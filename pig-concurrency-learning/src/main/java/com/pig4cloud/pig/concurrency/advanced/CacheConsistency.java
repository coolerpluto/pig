package com.pig4cloud.pig.concurrency.advanced;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 缓存一致性问题
 * 
 * 学习要点：
 * 1. 缓存穿透、击穿、雪崩
 * 2. 双重检查锁定（Double-Checked Locking）
 * 3. 缓存更新策略
 * 4. 使用 FutureTask 避免重复计算
 */
@Slf4j
public class CacheConsistency {

    /**
     * 基础缓存实现（存在并发问题）
     */
    static class NaiveCache<K, V> {
        private final ConcurrentHashMap<K, V> cache = new ConcurrentHashMap<>();
        
        public V get(K key, Callable<V> loader) {
            V value = cache.get(key);
            if (value == null) {
                try {
                    log.info("缓存未命中，从数据源加载: {}", key);
                    // 问题：多个线程可能同时加载相同的数据
                    value = loader.call();
                    cache.put(key, value);
                } catch (Exception e) {
                    log.error("加载数据失败", e);
                }
            } else {
                log.info("缓存命中: {}", key);
            }
            return value;
        }
    }

    /**
     * 使用 FutureTask 优化的缓存（避免重复计算）
     */
    static class OptimizedCache<K, V> {
        private final ConcurrentHashMap<K, Future<V>> cache = new ConcurrentHashMap<>();
        
        public V get(K key, Callable<V> loader) throws Exception {
            Future<V> future = cache.get(key);
            
            if (future == null) {
                FutureTask<V> task = new FutureTask<>(loader);
                // putIfAbsent 保证只有一个线程创建 FutureTask
                future = cache.putIfAbsent(key, task);
                
                if (future == null) {
                    // 当前线程负责加载数据
                    future = task;
                    log.info("{} 开始加载数据: {}", Thread.currentThread().getName(), key);
                    task.run();
                } else {
                    log.info("{} 等待其他线程加载数据: {}", Thread.currentThread().getName(), key);
                }
            } else {
                log.info("{} 缓存命中: {}", Thread.currentThread().getName(), key);
            }
            
            try {
                return future.get();
            } catch (InterruptedException | ExecutionException e) {
                cache.remove(key); // 加载失败，移除缓存
                throw new Exception("加载数据失败", e);
            }
        }
        
        public void invalidate(K key) {
            cache.remove(key);
            log.info("缓存失效: {}", key);
        }
    }

    /**
     * 支持过期时间的缓存
     */
    @Data
    @AllArgsConstructor
    static class CacheEntry<V> {
        private V value;
        private long expireTime;
        
        public boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }
    }

    static class ExpiringCache<K, V> {
        private final ConcurrentHashMap<K, CacheEntry<V>> cache = new ConcurrentHashMap<>();
        private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
        private final long ttlMillis;
        
        public ExpiringCache(long ttlMillis) {
            this.ttlMillis = ttlMillis;
        }
        
        public V get(K key, Callable<V> loader) throws Exception {
            // 先用读锁检查
            rwLock.readLock().lock();
            try {
                CacheEntry<V> entry = cache.get(key);
                if (entry != null && !entry.isExpired()) {
                    log.info("缓存命中且未过期: {}", key);
                    return entry.getValue();
                }
            } finally {
                rwLock.readLock().unlock();
            }
            
            // 缓存未命中或已过期，使用写锁加载数据
            rwLock.writeLock().lock();
            try {
                // 双重检查
                CacheEntry<V> entry = cache.get(key);
                if (entry != null && !entry.isExpired()) {
                    return entry.getValue();
                }
                
                log.info("加载数据并更新缓存: {}", key);
                V value = loader.call();
                long expireTime = System.currentTimeMillis() + ttlMillis;
                cache.put(key, new CacheEntry<>(value, expireTime));
                return value;
            } finally {
                rwLock.writeLock().unlock();
            }
        }
    }

    /**
     * 防止缓存击穿的互斥锁方案
     */
    static class MutexCache<K, V> {
        private final ConcurrentHashMap<K, V> cache = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<K, Object> locks = new ConcurrentHashMap<>();
        
        public V get(K key, Callable<V> loader) throws Exception {
            V value = cache.get(key);
            if (value != null) {
                return value;
            }
            
            // 获取该 key 对应的锁
            Object lock = locks.computeIfAbsent(key, k -> new Object());
            
            synchronized (lock) {
                // 双重检查
                value = cache.get(key);
                if (value != null) {
                    return value;
                }
                
                log.info("{} 加载数据: {}", Thread.currentThread().getName(), key);
                value = loader.call();
                cache.put(key, value);
                
                // 清理锁对象，避免内存泄漏
                locks.remove(key);
                return value;
            }
        }
    }

    /**
     * 模拟数据加载器
     */
    static class DataLoader {
        public static String loadFromDatabase(String key) {
            log.info("从数据库加载: {}", key);
            try {
                // 模拟数据库查询延迟
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "Value-" + key;
        }
    }

    public static void main(String[] args) throws Exception {
        log.info("========== 1. 朴素缓存（存在重复加载问题） ==========");
        testNaiveCache();
        
        Thread.sleep(2000);
        
        log.info("\n========== 2. 优化缓存（使用 FutureTask） ==========");
        testOptimizedCache();
        
        Thread.sleep(2000);
        
        log.info("\n========== 3. 支持过期的缓存 ==========");
        testExpiringCache();
        
        Thread.sleep(2000);
        
        log.info("\n========== 4. 防击穿的互斥锁缓存 ==========");
        testMutexCache();
    }

    private static void testNaiveCache() throws InterruptedException {
        NaiveCache<String, String> cache = new NaiveCache<>();
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(5);
        
        // 多个线程同时请求相同的数据
        for (int i = 0; i < 5; i++) {
            executor.submit(() -> {
                try {
                    cache.get("key1", () -> DataLoader.loadFromDatabase("key1"));
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
    }

    private static void testOptimizedCache() throws InterruptedException {
        OptimizedCache<String, String> cache = new OptimizedCache<>();
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(5);
        
        // 多个线程同时请求相同的数据
        for (int i = 0; i < 5; i++) {
            executor.submit(() -> {
                try {
                    cache.get("key1", () -> DataLoader.loadFromDatabase("key1"));
                } catch (Exception e) {
                    log.error("获取缓存失败", e);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
    }

    private static void testExpiringCache() throws Exception {
        ExpiringCache<String, String> cache = new ExpiringCache<>(2000); // 2秒过期
        
        // 第一次获取
        String value1 = cache.get("key1", () -> DataLoader.loadFromDatabase("key1"));
        log.info("第一次获取: {}", value1);
        
        // 立即再次获取（命中缓存）
        String value2 = cache.get("key1", () -> DataLoader.loadFromDatabase("key1"));
        log.info("第二次获取: {}", value2);
        
        // 等待过期
        log.info("等待缓存过期...");
        Thread.sleep(2500);
        
        // 过期后获取（重新加载）
        String value3 = cache.get("key1", () -> DataLoader.loadFromDatabase("key1"));
        log.info("过期后获取: {}", value3);
    }

    private static void testMutexCache() throws InterruptedException {
        MutexCache<String, String> cache = new MutexCache<>();
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(10);
        
        // 模拟热点数据的高并发访问
        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                try {
                    cache.get("hotKey", () -> DataLoader.loadFromDatabase("hotKey"));
                } catch (Exception e) {
                    log.error("获取缓存失败", e);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
    }
}
