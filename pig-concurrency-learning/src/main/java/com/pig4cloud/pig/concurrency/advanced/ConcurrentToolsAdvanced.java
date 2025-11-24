package com.pig4cloud.pig.concurrency.advanced;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * 并发工具类高级用法
 * 
 * 学习要点：
 * 1. CountDownLatch - 等待多个线程完成
 * 2. CyclicBarrier - 线程间相互等待
 * 3. Semaphore - 信号量控制并发数
 * 4. Phaser - 更灵活的屏障
 * 5. Exchanger - 线程间数据交换
 * 6. CompletableFuture - 异步编程
 */
@Slf4j
public class ConcurrentToolsAdvanced {

    /**
     * 1. CountDownLatch 高级用法
     * 场景：主线程等待多个工作线程完成初始化
     */
    static class CountDownLatchDemo {
        public static void demonstrate() throws InterruptedException {
            log.info("========== CountDownLatch 示例：系统启动 ==========");
            
            int serviceCount = 5;
            CountDownLatch startSignal = new CountDownLatch(1); // 开始信号
            CountDownLatch doneSignal = new CountDownLatch(serviceCount); // 完成信号
            
            // 启动多个服务
            for (int i = 0; i < serviceCount; i++) {
                final int serviceId = i;
                new Thread(() -> {
                    try {
                        log.info("服务 {} 准备就绪，等待启动信号...", serviceId);
                        startSignal.await(); // 等待主线程发出启动信号
                        
                        log.info("服务 {} 开始初始化", serviceId);
                        Thread.sleep((long) (Math.random() * 1000));
                        log.info("服务 {} 初始化完成", serviceId);
                        
                        doneSignal.countDown();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }
            
            Thread.sleep(500);
            log.info("发送启动信号");
            startSignal.countDown(); // 发送启动信号
            
            log.info("等待所有服务初始化完成...");
            doneSignal.await(); // 等待所有服务完成
            log.info("所有服务启动完成！");
        }
    }

    /**
     * 2. CyclicBarrier 高级用法
     * 场景：多线程分阶段处理，每个阶段结束时同步
     */
    static class CyclicBarrierDemo {
        public static void demonstrate() throws InterruptedException {
            log.info("========== CyclicBarrier 示例：多阶段并行计算 ==========");
            
            int threadCount = 4;
            CyclicBarrier barrier = new CyclicBarrier(threadCount, () -> {
                log.info("====== 所有线程完成当前阶段，开始下一阶段 ======");
            });
            
            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                new Thread(() -> {
                    try {
                        for (int phase = 1; phase <= 3; phase++) {
                            log.info("线程 {} 执行阶段 {}", threadId, phase);
                            Thread.sleep((long) (Math.random() * 1000));
                            log.info("线程 {} 完成阶段 {}，等待其他线程...", threadId, phase);
                            
                            barrier.await(); // 等待所有线程完成当前阶段
                        }
                        log.info("线程 {} 完成所有阶段", threadId);
                    } catch (InterruptedException | BrokenBarrierException e) {
                        log.error("线程被中断", e);
                    }
                }).start();
            }
            
            Thread.sleep(5000);
        }
    }

    /**
     * 3. Semaphore 高级用法
     * 场景：数据库连接池
     */
    static class SemaphoreDemo {
        static class ConnectionPool {
            private final Semaphore semaphore;
            private final int poolSize;
            private final AtomicInteger activeConnections = new AtomicInteger(0);

            public ConnectionPool(int poolSize) {
                this.poolSize = poolSize;
                this.semaphore = new Semaphore(poolSize);
            }

            public void executeQuery(String queryName) throws InterruptedException {
                log.info("{} 尝试获取数据库连接...", queryName);
                semaphore.acquire();
                
                try {
                    int active = activeConnections.incrementAndGet();
                    log.info("{} 获得连接，当前活跃连接: {}/{}", 
                        queryName, active, poolSize);
                    
                    // 模拟查询执行
                    Thread.sleep((long) (Math.random() * 1000));
                    
                    log.info("{} 查询完成", queryName);
                } finally {
                    activeConnections.decrementAndGet();
                    semaphore.release();
                    log.info("{} 释放连接", queryName);
                }
            }
        }

        public static void demonstrate() throws InterruptedException {
            log.info("========== Semaphore 示例：数据库连接池 ==========");
            
            ConnectionPool pool = new ConnectionPool(3);
            ExecutorService executor = Executors.newFixedThreadPool(10);
            
            for (int i = 0; i < 10; i++) {
                final int queryId = i;
                executor.submit(() -> {
                    try {
                        pool.executeQuery("Query-" + queryId);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    /**
     * 4. Phaser 高级用法
     * 场景：动态调整参与者数量的多阶段任务
     */
    static class PhaserDemo {
        public static void demonstrate() throws InterruptedException {
            log.info("========== Phaser 示例：动态多阶段任务 ==========");
            
            Phaser phaser = new Phaser(1) { // 主线程作为初始参与者
                @Override
                protected boolean onAdvance(int phase, int registeredParties) {
                    log.info("====== 阶段 {} 完成，参与者数: {} ======", phase, registeredParties);
                    return phase >= 2 || registeredParties == 0; // 3个阶段后终止
                }
            };
            
            // 动态添加工作线程
            for (int i = 0; i < 3; i++) {
                final int threadId = i;
                phaser.register(); // 注册新参与者
                
                new Thread(() -> {
                    for (int phase = 0; phase < 3; phase++) {
                        log.info("线程 {} 执行阶段 {}", threadId, phase);
                        try {
                            Thread.sleep((long) (Math.random() * 500));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        phaser.arriveAndAwaitAdvance(); // 到达并等待
                    }
                    phaser.arriveAndDeregister(); // 完成所有阶段，注销
                    log.info("线程 {} 完成并退出", threadId);
                }).start();
            }
            
            // 主线程也参与各阶段
            for (int phase = 0; phase < 3; phase++) {
                phaser.arriveAndAwaitAdvance();
            }
            
            phaser.arriveAndDeregister();
            log.info("所有任务完成");
        }
    }

    /**
     * 5. Exchanger 高级用法
     * 场景：生产者和消费者交换数据
     */
    static class ExchangerDemo {
        public static void demonstrate() throws InterruptedException {
            log.info("========== Exchanger 示例：数据交换 ==========");
            
            Exchanger<String> exchanger = new Exchanger<>();
            
            // 生产者线程
            Thread producer = new Thread(() -> {
                try {
                    for (int i = 0; i < 3; i++) {
                        String data = "Data-" + i;
                        log.info("生产者准备数据: {}", data);
                        Thread.sleep(500);
                        
                        String received = exchanger.exchange(data);
                        log.info("生产者收到确认: {}", received);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "Producer");
            
            // 消费者线程
            Thread consumer = new Thread(() -> {
                try {
                    for (int i = 0; i < 3; i++) {
                        String received = exchanger.exchange("ACK-" + i);
                        log.info("消费者接收数据: {}", received);
                        Thread.sleep(300);
                        log.info("消费者处理完成");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "Consumer");
            
            producer.start();
            consumer.start();
            producer.join();
            consumer.join();
        }
    }

    /**
     * 6. CompletableFuture 高级用法
     * 场景：异步任务编排
     */
    static class CompletableFutureDemo {
        public static void demonstrate() throws Exception {
            log.info("========== CompletableFuture 示例：异步任务编排 ==========");
            
            // 异步任务链
            CompletableFuture<String> future = CompletableFuture
                .supplyAsync(() -> {
                    log.info("步骤1：查询用户信息");
                    sleep(500);
                    return "User123";
                })
                .thenApply(userId -> {
                    log.info("步骤2：根据用户ID {} 查询订单", userId);
                    sleep(500);
                    return "Order456";
                })
                .thenApply(orderId -> {
                    log.info("步骤3：根据订单ID {} 查询详情", orderId);
                    sleep(500);
                    return "OrderDetail: " + orderId;
                });
            
            log.info("结果: {}", future.get());
            
            // 并行执行多个任务
            log.info("\n并行执行多个任务：");
            CompletableFuture<String> task1 = CompletableFuture.supplyAsync(() -> {
                log.info("任务1开始");
                sleep(1000);
                return "Result1";
            });
            
            CompletableFuture<String> task2 = CompletableFuture.supplyAsync(() -> {
                log.info("任务2开始");
                sleep(800);
                return "Result2";
            });
            
            CompletableFuture<String> task3 = CompletableFuture.supplyAsync(() -> {
                log.info("任务3开始");
                sleep(600);
                return "Result3";
            });
            
            // 等待所有任务完成并合并结果
            CompletableFuture<String> allTasks = CompletableFuture
                .allOf(task1, task2, task3)
                .thenApply(v -> {
                    try {
                        return String.format("所有任务完成: [%s, %s, %s]", 
                            task1.get(), task2.get(), task3.get());
                    } catch (Exception e) {
                        return "Error";
                    }
                });
            
            log.info(allTasks.get());
            
            // 异常处理
            log.info("\n异常处理示例：");
            CompletableFuture<String> futureWithError = CompletableFuture
                .supplyAsync(() -> {
                    log.info("执行可能出错的任务");
                    if (Math.random() > 0.5) {
                        throw new RuntimeException("模拟错误");
                    }
                    return "Success";
                })
                .exceptionally(ex -> {
                    log.error("捕获异常: {}", ex.getMessage());
                    return "Default Value";
                })
                .thenApply(result -> {
                    log.info("继续处理: {}", result);
                    return result;
                });
            
            log.info("最终结果: {}", futureWithError.get());
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
     * 7. LongAdder 高性能计数器
     * 场景：高并发计数
     */
    static class LongAdderDemo {
        public static void demonstrate() throws InterruptedException {
            log.info("========== LongAdder 示例：高并发计数 ==========");
            
            LongAdder counter = new LongAdder();
            int threadCount = 10;
            int incrementsPerThread = 100000;
            
            CountDownLatch latch = new CountDownLatch(threadCount);
            long startTime = System.currentTimeMillis();
            
            for (int i = 0; i < threadCount; i++) {
                new Thread(() -> {
                    for (int j = 0; j < incrementsPerThread; j++) {
                        counter.increment();
                    }
                    latch.countDown();
                }).start();
            }
            
            latch.await();
            long duration = System.currentTimeMillis() - startTime;
            
            log.info("LongAdder 结果: {}, 耗时: {} ms", counter.sum(), duration);
            
            // 对比 AtomicInteger
            log.info("\n对比 AtomicInteger:");
            AtomicInteger atomicCounter = new AtomicInteger(0);
            CountDownLatch latch2 = new CountDownLatch(threadCount);
            long startTime2 = System.currentTimeMillis();
            
            for (int i = 0; i < threadCount; i++) {
                new Thread(() -> {
                    for (int j = 0; j < incrementsPerThread; j++) {
                        atomicCounter.incrementAndGet();
                    }
                    latch2.countDown();
                }).start();
            }
            
            latch2.await();
            long duration2 = System.currentTimeMillis() - startTime2;
            
            log.info("AtomicInteger 结果: {}, 耗时: {} ms", atomicCounter.get(), duration2);
            log.info("LongAdder 性能提升: {}%", 
                ((duration2 - duration) * 100.0 / duration2));
        }
    }

    public static void main(String[] args) throws Exception {
        CountDownLatchDemo.demonstrate();
        Thread.sleep(500);
        
        CyclicBarrierDemo.demonstrate();
        Thread.sleep(500);
        
        SemaphoreDemo.demonstrate();
        Thread.sleep(500);
        
        PhaserDemo.demonstrate();
        Thread.sleep(500);
        
        ExchangerDemo.demonstrate();
        Thread.sleep(500);
        
        CompletableFutureDemo.demonstrate();
        Thread.sleep(500);
        
        LongAdderDemo.demonstrate();
    }
}
