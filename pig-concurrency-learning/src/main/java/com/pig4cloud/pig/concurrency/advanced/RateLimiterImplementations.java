package com.pig4cloud.pig.concurrency.advanced;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 限流器实现
 * 
 * 学习要点：
 * 1. 令牌桶算法（Token Bucket）
 * 2. 漏桶算法（Leaky Bucket）
 * 3. 滑动窗口算法
 * 4. 固定窗口计数器
 */
@Slf4j
public class RateLimiterImplementations {

    /**
     * 1. 固定窗口计数器
     * 简单但在窗口边界可能出现突刺流量
     */
    static class FixedWindowRateLimiter {
        private final int maxRequests;
        private final long windowSizeMillis;
        private final AtomicInteger counter = new AtomicInteger(0);
        private volatile long windowStart;

        public FixedWindowRateLimiter(int maxRequests, long windowSizeMillis) {
            this.maxRequests = maxRequests;
            this.windowSizeMillis = windowSizeMillis;
            this.windowStart = System.currentTimeMillis();
        }

        public synchronized boolean tryAcquire() {
            long now = System.currentTimeMillis();
            
            // 检查是否需要重置窗口
            if (now - windowStart >= windowSizeMillis) {
                counter.set(0);
                windowStart = now;
                log.info("窗口重置");
            }
            
            if (counter.get() < maxRequests) {
                counter.incrementAndGet();
                log.info("请求通过，当前计数: {}/{}", counter.get(), maxRequests);
                return true;
            }
            
            log.warn("请求被限流，当前计数: {}/{}", counter.get(), maxRequests);
            return false;
        }
    }

    /**
     * 2. 滑动窗口算法
     * 更平滑的限流，避免固定窗口的突刺问题
     */
    static class SlidingWindowRateLimiter {
        private final int maxRequests;
        private final long windowSizeMillis;
        private final ConcurrentLinkedQueue<Long> requestTimestamps = new ConcurrentLinkedQueue<>();

        public SlidingWindowRateLimiter(int maxRequests, long windowSizeMillis) {
            this.maxRequests = maxRequests;
            this.windowSizeMillis = windowSizeMillis;
        }

        public synchronized boolean tryAcquire() {
            long now = System.currentTimeMillis();
            long windowStart = now - windowSizeMillis;
            
            // 移除窗口外的时间戳
            while (!requestTimestamps.isEmpty() && requestTimestamps.peek() < windowStart) {
                requestTimestamps.poll();
            }
            
            if (requestTimestamps.size() < maxRequests) {
                requestTimestamps.offer(now);
                log.info("请求通过，当前窗口内请求数: {}/{}", requestTimestamps.size(), maxRequests);
                return true;
            }
            
            log.warn("请求被限流，当前窗口内请求数: {}/{}", requestTimestamps.size(), maxRequests);
            return false;
        }
    }

    /**
     * 3. 令牌桶算法
     * 允许突发流量，平滑处理请求
     */
    static class TokenBucketRateLimiter {
        private final int capacity; // 桶容量
        private final double refillRate; // 令牌生成速率（每秒）
        private final AtomicLong availableTokens;
        private volatile long lastRefillTime;

        public TokenBucketRateLimiter(int capacity, double refillRate) {
            this.capacity = capacity;
            this.refillRate = refillRate;
            this.availableTokens = new AtomicLong(capacity);
            this.lastRefillTime = System.nanoTime();
        }

        public synchronized boolean tryAcquire() {
            refill();
            
            long tokens = availableTokens.get();
            if (tokens > 0) {
                availableTokens.decrementAndGet();
                log.info("获取令牌成功，剩余令牌: {}/{}", availableTokens.get(), capacity);
                return true;
            }
            
            log.warn("令牌不足，请求被限流，当前令牌: 0/{}", capacity);
            return false;
        }

        public synchronized boolean tryAcquire(int tokens) {
            refill();
            
            long available = availableTokens.get();
            if (available >= tokens) {
                availableTokens.addAndGet(-tokens);
                log.info("获取 {} 个令牌成功，剩余令牌: {}/{}", tokens, availableTokens.get(), capacity);
                return true;
            }
            
            log.warn("令牌不足，请求被限流，需要: {}, 当前: {}", tokens, available);
            return false;
        }

        private void refill() {
            long now = System.nanoTime();
            long timePassed = now - lastRefillTime;
            
            // 计算应该补充的令牌数
            double tokensToAdd = (timePassed / 1_000_000_000.0) * refillRate;
            
            if (tokensToAdd > 0) {
                long newTokens = Math.min(capacity, 
                    availableTokens.get() + (long) tokensToAdd);
                availableTokens.set(newTokens);
                lastRefillTime = now;
            }
        }
    }

    /**
     * 4. 漏桶算法
     * 强制固定速率输出，平滑流量
     */
    static class LeakyBucketRateLimiter {
        private final int capacity;
        private final double leakRate; // 漏出速率（每秒）
        private final AtomicInteger water = new AtomicInteger(0);
        private volatile long lastLeakTime;
        private final ScheduledExecutorService scheduler;

        public LeakyBucketRateLimiter(int capacity, double leakRate) {
            this.capacity = capacity;
            this.leakRate = leakRate;
            this.lastLeakTime = System.nanoTime();
            
            // 定期漏水
            this.scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(this::leak, 0, 100, TimeUnit.MILLISECONDS);
        }

        public synchronized boolean tryAcquire() {
            leak();
            
            if (water.get() < capacity) {
                water.incrementAndGet();
                log.info("请求加入桶，当前水位: {}/{}", water.get(), capacity);
                return true;
            }
            
            log.warn("桶已满，请求被限流，当前水位: {}/{}", water.get(), capacity);
            return false;
        }

        private synchronized void leak() {
            long now = System.nanoTime();
            long timePassed = now - lastLeakTime;
            
            // 计算应该漏出的水量
            double waterToLeak = (timePassed / 1_000_000_000.0) * leakRate;
            
            if (waterToLeak > 0) {
                int leaked = (int) Math.min(water.get(), waterToLeak);
                if (leaked > 0) {
                    water.addAndGet(-leaked);
                    log.debug("漏出 {} 单位水，剩余: {}/{}", leaked, water.get(), capacity);
                }
                lastLeakTime = now;
            }
        }

        public void shutdown() {
            scheduler.shutdown();
        }
    }

    /**
     * 5. 分布式限流器（基于 Redis 的滑动窗口）
     * 这里用 ConcurrentHashMap 模拟 Redis
     */
    static class DistributedRateLimiter {
        private final ConcurrentHashMap<String, ConcurrentLinkedQueue<Long>> userRequests = new ConcurrentHashMap<>();
        private final int maxRequests;
        private final long windowSizeMillis;

        public DistributedRateLimiter(int maxRequests, long windowSizeMillis) {
            this.maxRequests = maxRequests;
            this.windowSizeMillis = windowSizeMillis;
        }

        public boolean tryAcquire(String userId) {
            long now = System.currentTimeMillis();
            long windowStart = now - windowSizeMillis;
            
            ConcurrentLinkedQueue<Long> requests = userRequests.computeIfAbsent(
                userId, k -> new ConcurrentLinkedQueue<>()
            );
            
            synchronized (requests) {
                // 清理过期请求
                while (!requests.isEmpty() && requests.peek() < windowStart) {
                    requests.poll();
                }
                
                if (requests.size() < maxRequests) {
                    requests.offer(now);
                    log.info("用户 {} 请求通过，窗口内请求数: {}/{}", 
                        userId, requests.size(), maxRequests);
                    return true;
                }
                
                log.warn("用户 {} 被限流，窗口内请求数: {}/{}", 
                    userId, requests.size(), maxRequests);
                return false;
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        log.info("========== 1. 固定窗口计数器 ==========");
        testFixedWindow();
        
        Thread.sleep(1000);
        
        log.info("\n========== 2. 滑动窗口算法 ==========");
        testSlidingWindow();
        
        Thread.sleep(1000);
        
        log.info("\n========== 3. 令牌桶算法 ==========");
        testTokenBucket();
        
        Thread.sleep(1000);
        
        log.info("\n========== 4. 漏桶算法 ==========");
        testLeakyBucket();
        
        Thread.sleep(1000);
        
        log.info("\n========== 5. 分布式限流 ==========");
        testDistributed();
    }

    private static void testFixedWindow() throws InterruptedException {
        FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(5, 1000);
        
        // 发送 10 个请求
        for (int i = 0; i < 10; i++) {
            limiter.tryAcquire();
            Thread.sleep(100);
        }
    }

    private static void testSlidingWindow() throws InterruptedException {
        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(5, 1000);
        
        for (int i = 0; i < 10; i++) {
            limiter.tryAcquire();
            Thread.sleep(150);
        }
    }

    private static void testTokenBucket() throws InterruptedException {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(10, 2.0);
        
        // 快速消耗令牌
        for (int i = 0; i < 5; i++) {
            limiter.tryAcquire();
        }
        
        log.info("等待令牌补充...");
        Thread.sleep(2000);
        
        // 再次尝试
        for (int i = 0; i < 5; i++) {
            limiter.tryAcquire();
        }
    }

    private static void testLeakyBucket() throws InterruptedException {
        LeakyBucketRateLimiter limiter = new LeakyBucketRateLimiter(5, 1.0);
        
        // 快速填充
        for (int i = 0; i < 8; i++) {
            limiter.tryAcquire();
            Thread.sleep(100);
        }
        
        Thread.sleep(2000);
        limiter.shutdown();
    }

    private static void testDistributed() throws InterruptedException {
        DistributedRateLimiter limiter = new DistributedRateLimiter(3, 1000);
        
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(10);
        
        // 模拟多个用户并发请求
        for (int i = 0; i < 10; i++) {
            final String userId = "user" + (i % 2); // 2个用户
            executor.submit(() -> {
                try {
                    limiter.tryAcquire(userId);
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
    }
}
