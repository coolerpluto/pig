package com.pig4cloud.pig.concurrency.advanced;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池高级用法
 * 
 * 学习要点：
 * 1. 线程池核心参数详解
 * 2. 拒绝策略的使用场景
 * 3. 自定义线程池
 * 4. 线程池监控
 * 5. 优雅关闭
 */
@Slf4j
public class ThreadPoolAdvanced {

    /**
     * 自定义线程工厂
     */
    static class CustomThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;
        private final boolean daemon;

        public CustomThreadFactory(String namePrefix, boolean daemon) {
            this.namePrefix = namePrefix;
            this.daemon = daemon;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, namePrefix + "-" + threadNumber.getAndIncrement());
            thread.setDaemon(daemon);
            log.info("创建线程: {}, 是否守护线程: {}", thread.getName(), daemon);
            return thread;
        }
    }

    /**
     * 自定义拒绝策略
     */
    static class CustomRejectedHandler implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            log.warn("任务被拒绝: {}, 活跃线程: {}, 队列大小: {}, 最大线程数: {}", 
                r.toString(),
                executor.getActiveCount(),
                executor.getQueue().size(),
                executor.getMaximumPoolSize());
            
            // 自定义处理逻辑：记录到数据库、发送告警等
            // 这里演示：尝试在调用者线程中执行
            if (!executor.isShutdown()) {
                try {
                    executor.getQueue().offer(r, 100, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("尝试重新入队失败", e);
                }
            }
        }
    }

    /**
     * 可监控的线程池
     */
    static class MonitoredThreadPool extends ThreadPoolExecutor {
        
        public MonitoredThreadPool(int corePoolSize, int maximumPoolSize, 
                                  long keepAliveTime, TimeUnit unit,
                                  BlockingQueue<Runnable> workQueue) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                new CustomThreadFactory("Monitor-Pool", false),
                new CustomRejectedHandler());
        }

        @Override
        protected void beforeExecute(Thread t, Runnable r) {
            super.beforeExecute(t, r);
            log.debug("任务开始执行: 线程={}, 任务={}", t.getName(), r.toString());
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);
            if (t != null) {
                log.error("任务执行异常: 任务={}", r.toString(), t);
            } else {
                log.debug("任务执行完成: 任务={}", r.toString());
            }
        }

        @Override
        protected void terminated() {
            super.terminated();
            log.info("线程池已终止");
        }

        /**
         * 打印线程池状态
         */
        public void printStatus() {
            log.info("========== 线程池状态 ==========");
            log.info("核心线程数: {}", getCorePoolSize());
            log.info("最大线程数: {}", getMaximumPoolSize());
            log.info("当前线程数: {}", getPoolSize());
            log.info("活跃线程数: {}", getActiveCount());
            log.info("队列大小: {}", getQueue().size());
            log.info("已完成任务数: {}", getCompletedTaskCount());
            log.info("总任务数: {}", getTaskCount());
            log.info("===============================");
        }
    }

    /**
     * 演示不同拒绝策略
     */
    static class RejectionPolicyDemo {
        public static void demonstrate() throws InterruptedException {
            log.info("========== 1. AbortPolicy（默认策略，抛出异常） ==========");
            testAbortPolicy();
            
            Thread.sleep(500);
            
            log.info("\n========== 2. CallerRunsPolicy（调用者运行） ==========");
            testCallerRunsPolicy();
            
            Thread.sleep(500);
            
            log.info("\n========== 3. DiscardPolicy（丢弃任务） ==========");
            testDiscardPolicy();
            
            Thread.sleep(500);
            
            log.info("\n========== 4. DiscardOldestPolicy（丢弃最老任务） ==========");
            testDiscardOldestPolicy();
        }

        private static void testAbortPolicy() {
            ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(1),
                new ThreadPoolExecutor.AbortPolicy()
            );

            try {
                for (int i = 0; i < 3; i++) {
                    final int taskId = i;
                    executor.execute(() -> {
                        log.info("执行任务 {}", taskId);
                        sleep(1000);
                    });
                }
            } catch (RejectedExecutionException e) {
                log.error("任务被拒绝: {}", e.getMessage());
            } finally {
                executor.shutdown();
            }
        }

        private static void testCallerRunsPolicy() throws InterruptedException {
            ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(1),
                new ThreadPoolExecutor.CallerRunsPolicy()
            );

            for (int i = 0; i < 3; i++) {
                final int taskId = i;
                executor.execute(() -> {
                    log.info("执行任务 {} by {}", taskId, Thread.currentThread().getName());
                    sleep(500);
                });
            }
            
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }

        private static void testDiscardPolicy() throws InterruptedException {
            ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(1),
                new ThreadPoolExecutor.DiscardPolicy()
            );

            for (int i = 0; i < 5; i++) {
                final int taskId = i;
                executor.execute(() -> {
                    log.info("执行任务 {}", taskId);
                    sleep(500);
                });
            }
            
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }

        private static void testDiscardOldestPolicy() throws InterruptedException {
            ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(2),
                new ThreadPoolExecutor.DiscardOldestPolicy()
            );

            for (int i = 0; i < 5; i++) {
                final int taskId = i;
                executor.execute(() -> {
                    log.info("执行任务 {}", taskId);
                    sleep(500);
                });
            }
            
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }

        private static void sleep(long millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 线程池参数调优示例
     */
    static class ThreadPoolTuning {
        public static void demonstrate() throws InterruptedException {
            log.info("========== 线程池参数调优 ==========");
            
            // CPU 密集型任务：线程数 = CPU 核心数 + 1
            int cpuCount = Runtime.getRuntime().availableProcessors();
            log.info("CPU 核心数: {}", cpuCount);
            log.info("CPU 密集型推荐线程数: {}", cpuCount + 1);
            
            // IO 密集型任务：线程数 = CPU 核心数 * 2 或更多
            log.info("IO 密集型推荐线程数: {}", cpuCount * 2);
            
            // 创建合适的线程池
            MonitoredThreadPool executor = new MonitoredThreadPool(
                cpuCount,           // 核心线程数
                cpuCount * 2,       // 最大线程数
                60L,                // 空闲线程存活时间
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100)  // 有界队列
            );
            
            // 提交任务
            for (int i = 0; i < 20; i++) {
                final int taskId = i;
                executor.execute(() -> {
                    log.info("执行任务 {}", taskId);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            
            // 监控线程池状态
            Thread.sleep(500);
            executor.printStatus();
            
            // 优雅关闭
            log.info("开始关闭线程池...");
            executor.shutdown(); // 不再接受新任务
            
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("线程池未在指定时间内关闭，强制关闭");
                executor.shutdownNow(); // 中断正在执行的任务
            }
            
            executor.printStatus();
        }
    }

    /**
     * 动态调整线程池
     */
    static class DynamicThreadPool {
        public static void demonstrate() throws InterruptedException {
            log.info("========== 动态调整线程池 ==========");
            
            MonitoredThreadPool executor = new MonitoredThreadPool(
                2, 10, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(50)
            );
            
            // 提交一批任务
            for (int i = 0; i < 5; i++) {
                final int taskId = i;
                executor.execute(() -> {
                    log.info("执行任务 {}", taskId);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            
            Thread.sleep(500);
            executor.printStatus();
            
            // 动态调整核心线程数
            log.info("增加核心线程数到 5");
            executor.setCorePoolSize(5);
            
            // 动态调整最大线程数
            log.info("增加最大线程数到 20");
            executor.setMaximumPoolSize(20);
            
            // 提交更多任务
            for (int i = 5; i < 15; i++) {
                final int taskId = i;
                executor.execute(() -> {
                    log.info("执行任务 {}", taskId);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            
            Thread.sleep(500);
            executor.printStatus();
            
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        RejectionPolicyDemo.demonstrate();
        Thread.sleep(1000);
        
        ThreadPoolTuning.demonstrate();
        Thread.sleep(1000);
        
        DynamicThreadPool.demonstrate();
    }
}
