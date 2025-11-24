package com.pig4cloud.pig.concurrency.problems;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 线程饥饿问题演示
 * 
 * 饥饿：线程因无法获取所需资源而长时间无法执行
 * 
 * 常见原因：
 * 1. 高优先级线程占用资源
 * 2. 锁的不公平竞争
 * 3. 线程持有锁的时间过长
 * 
 * 学习要点：
 * 1. 公平锁 vs 非公平锁
 * 2. 线程优先级对调度的影响
 * 3. 如何避免饥饿
 */
@Slf4j
public class StarvationProblem {

    /**
     * 演示非公平锁可能导致的饥饿
     */
    static class UnfairLockStarvation {
        private final Lock unfairLock = new ReentrantLock(false); // 非公平锁
        private int counter = 0;

        public void accessResource(int workLoad) {
            unfairLock.lock();
            try {
                long startTime = System.currentTimeMillis();
                log.info("{} 获得锁，开始工作 (工作量: {})", 
                    Thread.currentThread().getName(), workLoad);
                
                // 模拟工作
                for (int i = 0; i < workLoad; i++) {
                    counter++;
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                
                long duration = System.currentTimeMillis() - startTime;
                log.info("{} 完成工作，耗时: {} ms", 
                    Thread.currentThread().getName(), duration);
            } finally {
                unfairLock.unlock();
            }
        }

        public void demonstrate() throws InterruptedException {
            log.info("========== 非公平锁演示 ==========");
            
            // 创建一个低工作量的线程（模拟"饥饿"线程）
            Thread starvedThread = new Thread(() -> {
                long startTime = System.currentTimeMillis();
                accessResource(10);
                long waitTime = System.currentTimeMillis() - startTime;
                log.warn("饥饿线程总等待时间: {} ms", waitTime);
            }, "Starved-Thread");
            
            starvedThread.start();
            Thread.sleep(50); // 确保饥饿线程先启动
            
            // 创建多个高工作量的线程，持续竞争锁
            Thread[] threads = new Thread[5];
            for (int i = 0; i < threads.length; i++) {
                final int threadNum = i;
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < 3; j++) {
                        accessResource(100); // 高工作量
                    }
                }, "Worker-" + threadNum);
                threads[i].start();
            }
            
            starvedThread.join();
            for (Thread t : threads) {
                t.join();
            }
        }
    }

    /**
     * 使用公平锁解决饥饿问题
     */
    static class FairLockSolution {
        private final Lock fairLock = new ReentrantLock(true); // 公平锁
        private int counter = 0;

        public void accessResource(int workLoad) {
            fairLock.lock();
            try {
                long startTime = System.currentTimeMillis();
                log.info("{} 获得锁，开始工作 (工作量: {})", 
                    Thread.currentThread().getName(), workLoad);
                
                // 模拟工作
                for (int i = 0; i < workLoad; i++) {
                    counter++;
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                
                long duration = System.currentTimeMillis() - startTime;
                log.info("{} 完成工作，耗时: {} ms", 
                    Thread.currentThread().getName(), duration);
            } finally {
                fairLock.unlock();
            }
        }

        public void demonstrate() throws InterruptedException {
            log.info("========== 公平锁演示 ==========");
            
            Thread normalThread = new Thread(() -> {
                long startTime = System.currentTimeMillis();
                accessResource(10);
                long waitTime = System.currentTimeMillis() - startTime;
                log.info("普通线程总等待时间: {} ms", waitTime);
            }, "Normal-Thread");
            
            normalThread.start();
            Thread.sleep(50);
            
            Thread[] threads = new Thread[5];
            for (int i = 0; i < threads.length; i++) {
                final int threadNum = i;
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < 3; j++) {
                        accessResource(100);
                    }
                }, "Worker-" + threadNum);
                threads[i].start();
            }
            
            normalThread.join();
            for (Thread t : threads) {
                t.join();
            }
        }
    }

    /**
     * 线程优先级导致的饥饿演示
     */
    static class PriorityStarvation {
        private final Object lock = new Object();
        private int counter = 0;

        public void work(String taskName, int iterations) {
            for (int i = 0; i < iterations; i++) {
                synchronized (lock) {
                    counter++;
                    if (i % 10 == 0) {
                        log.info("{} (优先级: {}) 执行第 {} 次", 
                            taskName, 
                            Thread.currentThread().getPriority(), 
                            i);
                    }
                }
                
                // 短暂让出CPU，给其他线程机会
                Thread.yield();
            }
            log.info("{} 完成所有工作", taskName);
        }

        public void demonstrate() throws InterruptedException {
            log.info("========== 线程优先级演示 ==========");
            
            // 低优先级线程
            Thread lowPriorityThread = new Thread(() -> 
                work("Low-Priority-Thread", 100), "Low-Priority"
            );
            lowPriorityThread.setPriority(Thread.MIN_PRIORITY);
            
            // 高优先级线程
            Thread[] highPriorityThreads = new Thread[3];
            for (int i = 0; i < highPriorityThreads.length; i++) {
                final int num = i;
                highPriorityThreads[i] = new Thread(() -> 
                    work("High-Priority-Thread-" + num, 100), 
                    "High-Priority-" + num
                );
                highPriorityThreads[i].setPriority(Thread.MAX_PRIORITY);
            }
            
            // 启动所有线程
            lowPriorityThread.start();
            for (Thread t : highPriorityThreads) {
                t.start();
            }
            
            // 等待完成
            lowPriorityThread.join();
            for (Thread t : highPriorityThreads) {
                t.join();
            }
        }
    }

    /**
     * 避免饥饿的最佳实践
     */
    static class BestPractices {
        private final Lock lock = new ReentrantLock(true); // 使用公平锁

        public void demonstrateBestPractices() {
            log.info("========== 避免饥饿的最佳实践 ==========");
            log.info("1. 使用公平锁（ReentrantLock(true)）");
            log.info("2. 避免无限期持有锁");
            log.info("3. 适当使用 Thread.yield() 让出 CPU");
            log.info("4. 避免使用线程优先级进行控制");
            log.info("5. 使用限时等待（tryLock with timeout）");
            log.info("6. 合理设计锁的粒度，减少持有时间");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // 1. 演示非公平锁可能导致的饥饿
        UnfairLockStarvation unfairDemo = new UnfairLockStarvation();
        unfairDemo.demonstrate();
        
        Thread.sleep(1000);
        
        // 2. 使用公平锁解决饥饿
        FairLockSolution fairDemo = new FairLockSolution();
        fairDemo.demonstrate();
        
        Thread.sleep(1000);
        
        // 3. 线程优先级导致的饥饿
        PriorityStarvation priorityDemo = new PriorityStarvation();
        priorityDemo.demonstrate();
        
        Thread.sleep(1000);
        
        // 4. 最佳实践
        BestPractices bestPractices = new BestPractices();
        bestPractices.demonstrateBestPractices();
    }
}
