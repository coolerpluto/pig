package com.pig4cloud.pig.concurrency.problems;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 死锁和活锁问题演示
 * 
 * 学习要点：
 * 1. 死锁的四个必要条件
 * 2. 死锁的检测和预防
 * 3. 活锁的产生和解决
 * 4. tryLock 的使用
 */
@Slf4j
public class DeadlockLivelock {

    /**
     * 死锁演示：两个线程相互等待对方持有的锁
     * 
     * 死锁四个必要条件：
     * 1. 互斥：资源不能被共享
     * 2. 持有并等待：已持有资源的同时等待其他资源
     * 3. 不可剥夺：资源只能被持有者主动释放
     * 4. 循环等待：存在资源的循环等待链
     */
    static class DeadlockDemo {
        private final Lock lock1 = new ReentrantLock();
        private final Lock lock2 = new ReentrantLock();

        public void method1() {
            lock1.lock();
            try {
                log.info("{} 获得了 lock1", Thread.currentThread().getName());
                // 模拟一些操作
                Thread.sleep(100);
                
                log.info("{} 尝试获取 lock2...", Thread.currentThread().getName());
                lock2.lock();
                try {
                    log.info("{} 获得了 lock2", Thread.currentThread().getName());
                } finally {
                    lock2.unlock();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock1.unlock();
            }
        }

        public void method2() {
            lock2.lock();
            try {
                log.info("{} 获得了 lock2", Thread.currentThread().getName());
                // 模拟一些操作
                Thread.sleep(100);
                
                log.info("{} 尝试获取 lock1...", Thread.currentThread().getName());
                lock1.lock();
                try {
                    log.info("{} 获得了 lock1", Thread.currentThread().getName());
                } finally {
                    lock1.unlock();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock2.unlock();
            }
        }

        public void demonstrateDeadlock() {
            Thread t1 = new Thread(() -> method1(), "Thread-1");
            Thread t2 = new Thread(() -> method2(), "Thread-2");
            
            t1.start();
            t2.start();
            
            try {
                // 等待一段时间，观察死锁
                Thread.sleep(3000);
                log.error("死锁发生！线程状态：");
                log.error("Thread-1 状态: {}", t1.getState());
                log.error("Thread-2 状态: {}", t2.getState());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 死锁解决方案1：使用 tryLock 带超时
     */
    static class DeadlockSolution1 {
        private final Lock lock1 = new ReentrantLock();
        private final Lock lock2 = new ReentrantLock();

        public void method1() {
            try {
                while (true) {
                    // 尝试获取 lock1
                    if (lock1.tryLock(100, TimeUnit.MILLISECONDS)) {
                        try {
                            log.info("{} 获得了 lock1", Thread.currentThread().getName());
                            
                            // 尝试获取 lock2
                            if (lock2.tryLock(100, TimeUnit.MILLISECONDS)) {
                                try {
                                    log.info("{} 获得了 lock2，执行业务逻辑", Thread.currentThread().getName());
                                    Thread.sleep(50);
                                    return; // 成功获取两个锁，执行完毕
                                } finally {
                                    lock2.unlock();
                                }
                            } else {
                                log.warn("{} 获取 lock2 失败，释放 lock1 并重试", Thread.currentThread().getName());
                            }
                        } finally {
                            lock1.unlock();
                        }
                    }
                    // 随机等待一段时间后重试，避免活锁
                    Thread.sleep((long) (Math.random() * 50));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        public void method2() {
            try {
                while (true) {
                    if (lock2.tryLock(100, TimeUnit.MILLISECONDS)) {
                        try {
                            log.info("{} 获得了 lock2", Thread.currentThread().getName());
                            
                            if (lock1.tryLock(100, TimeUnit.MILLISECONDS)) {
                                try {
                                    log.info("{} 获得了 lock1，执行业务逻辑", Thread.currentThread().getName());
                                    Thread.sleep(50);
                                    return;
                                } finally {
                                    lock1.unlock();
                                }
                            } else {
                                log.warn("{} 获取 lock1 失败，释放 lock2 并重试", Thread.currentThread().getName());
                            }
                        } finally {
                            lock2.unlock();
                        }
                    }
                    Thread.sleep((long) (Math.random() * 50));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 活锁演示：两个线程都在主动避让，但都无法前进
     */
    @AllArgsConstructor
    static class Spoon {
        private String owner;
        
        public synchronized void use(String userName) {
            log.info("{} 正在使用勺子", userName);
        }
        
        public synchronized void setOwner(String owner) {
            this.owner = owner;
        }
        
        public synchronized String getOwner() {
            return owner;
        }
    }

    @AllArgsConstructor
    static class Spouse implements Runnable {
        private final String name;
        private final Spoon spoon;
        private boolean isHungry = true;

        @Override
        public void run() {
            while (isHungry) {
                // 如果勺子的主人不是自己
                if (!spoon.getOwner().equals(name)) {
                    try {
                        // 礼貌地等待
                        log.info("{}: 亲爱的，你先用吧！", name);
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                } else {
                    // 使用勺子
                    spoon.use(name);
                    
                    // 把勺子让给对方（这里产生活锁）
                    String partnerName = name.equals("张三") ? "李四" : "张三";
                    spoon.setOwner(partnerName);
                    
                    isHungry = false;
                    log.info("{}: 我吃完了", name);
                }
            }
        }
    }

    /**
     * 活锁解决方案：引入随机性，打破对称性
     */
    @AllArgsConstructor
    static class SmartSpouse implements Runnable {
        private final String name;
        private final Spoon spoon;
        private boolean isHungry = true;

        @Override
        public void run() {
            while (isHungry) {
                if (!spoon.getOwner().equals(name)) {
                    try {
                        // 随机等待，打破对称性
                        long waitTime = (long) (Math.random() * 100);
                        log.info("{}: 等待 {} ms", name, waitTime);
                        Thread.sleep(waitTime);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                } else {
                    spoon.use(name);
                    String partnerName = name.equals("张三") ? "李四" : "张三";
                    spoon.setOwner(partnerName);
                    isHungry = false;
                    log.info("{}: 我吃完了", name);
                }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // 1. 演示死锁
        log.info("========== 演示死锁 ==========");
        DeadlockDemo deadlock = new DeadlockDemo();
        deadlock.demonstrateDeadlock();
        
        Thread.sleep(2000);
        
        // 2. 演示死锁解决方案
        log.info("\n========== 死锁解决方案：使用 tryLock ==========");
        DeadlockSolution1 solution = new DeadlockSolution1();
        Thread t3 = new Thread(() -> solution.method1(), "Thread-3");
        Thread t4 = new Thread(() -> solution.method2(), "Thread-4");
        t3.start();
        t4.start();
        t3.join();
        t4.join();
        
        Thread.sleep(1000);
        
        // 3. 演示活锁
        log.info("\n========== 演示活锁 ==========");
        Spoon spoon = new Spoon("张三");
        Thread husband = new Thread(new Spouse("张三", spoon), "Husband");
        Thread wife = new Thread(new Spouse("李四", spoon), "Wife");
        husband.start();
        wife.start();
        
        // 等待一段时间后中断，因为会一直活锁
        Thread.sleep(2000);
        husband.interrupt();
        wife.interrupt();
        
        Thread.sleep(500);
        
        // 4. 演示活锁解决方案
        log.info("\n========== 活锁解决方案：引入随机性 ==========");
        Spoon spoon2 = new Spoon("张三");
        Thread husband2 = new Thread(new SmartSpouse("张三", spoon2), "Smart-Husband");
        Thread wife2 = new Thread(new SmartSpouse("李四", spoon2), "Smart-Wife");
        husband2.start();
        wife2.start();
        husband2.join();
        wife2.join();
        
        log.info("所有演示完成");
    }
}
