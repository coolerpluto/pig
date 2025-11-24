package com.pig4cloud.pig.concurrency.classic;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 哲学家就餐问题
 * 
 * 问题描述：
 * 5个哲学家围坐在圆桌旁，每两个哲学家之间有一根筷子（共5根）
 * 哲学家只做两件事：思考和吃饭
 * 吃饭时需要同时拿到左右两根筷子
 * 
 * 学习要点：
 * 1. 死锁的产生条件和预防
 * 2. 资源有序分配策略
 * 3. Semaphore 的使用
 */
@Slf4j
public class DiningPhilosophersProblem {

    /**
     * 方案1：使用 Lock，按编号顺序获取筷子（避免死锁）
     */
    static class PhilosopherWithLock implements Runnable {
        private final int id;
        private final Lock leftChopstick;
        private final Lock rightChopstick;
        private final int eatCount;

        public PhilosopherWithLock(int id, Lock leftChopstick, Lock rightChopstick, int eatCount) {
            this.id = id;
            this.leftChopstick = leftChopstick;
            this.rightChopstick = rightChopstick;
            this.eatCount = eatCount;
        }

        @Override
        public void run() {
            try {
                for (int i = 0; i < eatCount; i++) {
                    think();
                    eat();
                }
                log.info("哲学家 {} 完成了 {} 次进餐", id, eatCount);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private void think() throws InterruptedException {
            log.info("哲学家 {} 正在思考", id);
            Thread.sleep((long) (Math.random() * 100));
        }

        private void eat() throws InterruptedException {
            // 关键：按顺序获取锁，避免死锁
            // 较小编号的筷子先拿，较大编号的筷子后拿
            Lock first = leftChopstick.hashCode() < rightChopstick.hashCode() ? leftChopstick : rightChopstick;
            Lock second = leftChopstick.hashCode() < rightChopstick.hashCode() ? rightChopstick : leftChopstick;
            
            first.lock();
            try {
                log.info("哲学家 {} 拿起了第一根筷子", id);
                second.lock();
                try {
                    log.info("哲学家 {} 拿起了第二根筷子，开始吃饭", id);
                    Thread.sleep((long) (Math.random() * 100));
                    log.info("哲学家 {} 吃完了", id);
                } finally {
                    second.unlock();
                    log.info("哲学家 {} 放下了第二根筷子", id);
                }
            } finally {
                first.unlock();
                log.info("哲学家 {} 放下了第一根筷子", id);
            }
        }
    }

    /**
     * 方案2：使用信号量限制同时就餐的哲学家数量
     * 最多允许4个哲学家同时拿筷子，保证至少有一个能拿到两根
     */
    static class PhilosopherWithSemaphore implements Runnable {
        private final int id;
        private final Semaphore semaphore;
        private final Lock leftChopstick;
        private final Lock rightChopstick;
        private final int eatCount;

        public PhilosopherWithSemaphore(int id, Semaphore semaphore, 
                                       Lock leftChopstick, Lock rightChopstick, int eatCount) {
            this.id = id;
            this.semaphore = semaphore;
            this.leftChopstick = leftChopstick;
            this.rightChopstick = rightChopstick;
            this.eatCount = eatCount;
        }

        @Override
        public void run() {
            try {
                for (int i = 0; i < eatCount; i++) {
                    think();
                    eat();
                }
                log.info("哲学家 {} 完成了 {} 次进餐", id, eatCount);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private void think() throws InterruptedException {
            log.info("哲学家 {} 正在思考", id);
            Thread.sleep((long) (Math.random() * 100));
        }

        private void eat() throws InterruptedException {
            // 先获取信号量许可，限制同时就餐人数
            semaphore.acquire();
            try {
                leftChopstick.lock();
                try {
                    log.info("哲学家 {} 拿起了左边的筷子", id);
                    rightChopstick.lock();
                    try {
                        log.info("哲学家 {} 拿起了右边的筷子，开始吃饭", id);
                        Thread.sleep((long) (Math.random() * 100));
                        log.info("哲学家 {} 吃完了", id);
                    } finally {
                        rightChopstick.unlock();
                    }
                } finally {
                    leftChopstick.unlock();
                }
            } finally {
                semaphore.release();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        log.info("========== 方案1：资源有序分配（按编号顺序获取锁） ==========");
        runWithOrderedLocks();
        
        Thread.sleep(1000);
        
        log.info("\n========== 方案2：限制同时就餐人数（信号量） ==========");
        runWithSemaphore();
    }

    private static void runWithOrderedLocks() throws InterruptedException {
        int philosopherCount = 5;
        int eatCount = 3;
        
        Lock[] chopsticks = new Lock[philosopherCount];
        for (int i = 0; i < philosopherCount; i++) {
            chopsticks[i] = new ReentrantLock();
        }
        
        Thread[] philosophers = new Thread[philosopherCount];
        for (int i = 0; i < philosopherCount; i++) {
            Lock leftChopstick = chopsticks[i];
            Lock rightChopstick = chopsticks[(i + 1) % philosopherCount];
            philosophers[i] = new Thread(
                new PhilosopherWithLock(i, leftChopstick, rightChopstick, eatCount),
                "Philosopher-" + i
            );
            philosophers[i].start();
        }
        
        for (Thread philosopher : philosophers) {
            philosopher.join();
        }
    }

    private static void runWithSemaphore() throws InterruptedException {
        int philosopherCount = 5;
        int eatCount = 3;
        
        // 最多允许4个哲学家同时拿筷子
        Semaphore semaphore = new Semaphore(philosopherCount - 1);
        
        Lock[] chopsticks = new Lock[philosopherCount];
        for (int i = 0; i < philosopherCount; i++) {
            chopsticks[i] = new ReentrantLock();
        }
        
        Thread[] philosophers = new Thread[philosopherCount];
        for (int i = 0; i < philosopherCount; i++) {
            Lock leftChopstick = chopsticks[i];
            Lock rightChopstick = chopsticks[(i + 1) % philosopherCount];
            philosophers[i] = new Thread(
                new PhilosopherWithSemaphore(i, semaphore, leftChopstick, rightChopstick, eatCount),
                "Philosopher-" + i
            );
            philosophers[i].start();
        }
        
        for (Thread philosopher : philosophers) {
            philosopher.join();
        }
    }
}
