package com.pig4cloud.pig.concurrency.advanced;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * 基于 AQS (AbstractQueuedSynchronizer) 自定义锁
 * 
 * 学习要点：
 * 1. AQS 的工作原理
 * 2. 独占锁和共享锁的实现
 * 3. state 状态管理
 * 4. CLH 队列的使用
 */
@Slf4j
public class AQSCustomLock {

    /**
     * 自定义互斥锁（独占锁）
     */
    static class CustomMutexLock implements Lock {
        
        /**
         * AQS 同步器
         * state = 0: 未锁定
         * state = 1: 已锁定
         */
        private static class Sync extends AbstractQueuedSynchronizer {
            
            /**
             * 尝试获取锁
             */
            @Override
            protected boolean tryAcquire(int arg) {
                // 使用 CAS 将 state 从 0 改为 1
                if (compareAndSetState(0, 1)) {
                    // 设置当前线程为独占线程
                    setExclusiveOwnerThread(Thread.currentThread());
                    log.debug("{} 获取锁成功", Thread.currentThread().getName());
                    return true;
                }
                log.debug("{} 获取锁失败，进入等待队列", Thread.currentThread().getName());
                return false;
            }
            
            /**
             * 尝试释放锁
             */
            @Override
            protected boolean tryRelease(int arg) {
                if (getState() == 0) {
                    throw new IllegalMonitorStateException();
                }
                
                // 清除独占线程
                setExclusiveOwnerThread(null);
                // 释放锁，设置 state 为 0
                setState(0);
                log.debug("{} 释放锁", Thread.currentThread().getName());
                return true;
            }
            
            /**
             * 是否被独占
             */
            @Override
            protected boolean isHeldExclusively() {
                return getState() == 1;
            }
            
            Condition newCondition() {
                return new ConditionObject();
            }
        }
        
        private final Sync sync = new Sync();
        
        @Override
        public void lock() {
            sync.acquire(1);
        }
        
        @Override
        public void lockInterruptibly() throws InterruptedException {
            sync.acquireInterruptibly(1);
        }
        
        @Override
        public boolean tryLock() {
            return sync.tryAcquire(1);
        }
        
        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            return sync.tryAcquireNanos(1, unit.toNanos(time));
        }
        
        @Override
        public void unlock() {
            sync.release(1);
        }
        
        @Override
        public Condition newCondition() {
            return sync.newCondition();
        }
    }

    /**
     * 自定义共享锁（读写锁的简化版）
     */
    static class CustomSharedLock {
        
        /**
         * 共享锁同步器
         * state 表示共享资源的数量
         */
        private static class Sync extends AbstractQueuedSynchronizer {
            private final int maxShared;
            
            Sync(int maxShared) {
                this.maxShared = maxShared;
                setState(maxShared); // 初始化可用资源数量
            }
            
            /**
             * 尝试获取共享锁
             */
            @Override
            protected int tryAcquireShared(int arg) {
                for (;;) {
                    int available = getState();
                    int remaining = available - arg;
                    
                    // 如果资源不足，返回负数
                    if (remaining < 0) {
                        log.debug("{} 资源不足，等待中...", Thread.currentThread().getName());
                        return remaining;
                    }
                    
                    // 使用 CAS 更新 state
                    if (compareAndSetState(available, remaining)) {
                        log.debug("{} 获取 {} 个资源，剩余: {}", 
                            Thread.currentThread().getName(), arg, remaining);
                        return remaining;
                    }
                }
            }
            
            /**
             * 尝试释放共享锁
             */
            @Override
            protected boolean tryReleaseShared(int arg) {
                for (;;) {
                    int current = getState();
                    int next = current + arg;
                    
                    if (next < current) { // 溢出检查
                        throw new Error("Maximum permit count exceeded");
                    }
                    
                    if (compareAndSetState(current, next)) {
                        log.debug("{} 释放 {} 个资源，当前可用: {}", 
                            Thread.currentThread().getName(), arg, next);
                        return true;
                    }
                }
            }
            
            int availablePermits() {
                return getState();
            }
        }
        
        private final Sync sync;
        
        public CustomSharedLock(int permits) {
            this.sync = new Sync(permits);
        }
        
        public void acquire(int permits) throws InterruptedException {
            if (permits < 0) throw new IllegalArgumentException();
            sync.acquireSharedInterruptibly(permits);
        }
        
        public void release(int permits) {
            if (permits < 0) throw new IllegalArgumentException();
            sync.releaseShared(permits);
        }
        
        public int availablePermits() {
            return sync.availablePermits();
        }
    }

    /**
     * 自定义可重入锁
     */
    static class CustomReentrantLock implements Lock {
        
        private static class Sync extends AbstractQueuedSynchronizer {
            
            /**
             * 尝试获取锁（支持重入）
             */
            @Override
            protected boolean tryAcquire(int arg) {
                Thread current = Thread.currentThread();
                int state = getState();
                
                if (state == 0) {
                    // 锁未被占用，尝试获取
                    if (compareAndSetState(0, arg)) {
                        setExclusiveOwnerThread(current);
                        log.debug("{} 首次获取锁", current.getName());
                        return true;
                    }
                } else if (getExclusiveOwnerThread() == current) {
                    // 当前线程已持有锁，重入
                    int nextState = state + arg;
                    if (nextState < 0) { // 溢出检查
                        throw new Error("Maximum lock count exceeded");
                    }
                    setState(nextState);
                    log.debug("{} 重入锁，重入次数: {}", current.getName(), nextState);
                    return true;
                }
                
                return false;
            }
            
            /**
             * 尝试释放锁（支持重入）
             */
            @Override
            protected boolean tryRelease(int arg) {
                int state = getState() - arg;
                
                if (Thread.currentThread() != getExclusiveOwnerThread()) {
                    throw new IllegalMonitorStateException();
                }
                
                boolean free = false;
                if (state == 0) {
                    // 完全释放锁
                    free = true;
                    setExclusiveOwnerThread(null);
                    log.debug("{} 完全释放锁", Thread.currentThread().getName());
                } else {
                    log.debug("{} 部分释放锁，剩余重入次数: {}", 
                        Thread.currentThread().getName(), state);
                }
                
                setState(state);
                return free;
            }
            
            @Override
            protected boolean isHeldExclusively() {
                return getExclusiveOwnerThread() == Thread.currentThread();
            }
            
            Condition newCondition() {
                return new ConditionObject();
            }
        }
        
        private final Sync sync = new Sync();
        
        @Override
        public void lock() {
            sync.acquire(1);
        }
        
        @Override
        public void lockInterruptibly() throws InterruptedException {
            sync.acquireInterruptibly(1);
        }
        
        @Override
        public boolean tryLock() {
            return sync.tryAcquire(1);
        }
        
        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            return sync.tryAcquireNanos(1, unit.toNanos(time));
        }
        
        @Override
        public void unlock() {
            sync.release(1);
        }
        
        @Override
        public Condition newCondition() {
            return sync.newCondition();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        log.info("========== 1. 测试自定义互斥锁 ==========");
        testCustomMutexLock();
        
        Thread.sleep(1000);
        
        log.info("\n========== 2. 测试自定义共享锁 ==========");
        testCustomSharedLock();
        
        Thread.sleep(1000);
        
        log.info("\n========== 3. 测试自定义可重入锁 ==========");
        testCustomReentrantLock();
    }

    private static void testCustomMutexLock() throws InterruptedException {
        CustomMutexLock lock = new CustomMutexLock();
        
        Runnable task = () -> {
            lock.lock();
            try {
                log.info("{} 进入临界区", Thread.currentThread().getName());
                Thread.sleep(500);
                log.info("{} 离开临界区", Thread.currentThread().getName());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        };
        
        Thread t1 = new Thread(task, "Thread-1");
        Thread t2 = new Thread(task, "Thread-2");
        Thread t3 = new Thread(task, "Thread-3");
        
        t1.start();
        t2.start();
        t3.start();
        
        t1.join();
        t2.join();
        t3.join();
    }

    private static void testCustomSharedLock() throws InterruptedException {
        CustomSharedLock lock = new CustomSharedLock(3);
        
        Runnable task = () -> {
            try {
                log.info("{} 尝试获取资源", Thread.currentThread().getName());
                lock.acquire(1);
                
                log.info("{} 获得资源，开始工作", Thread.currentThread().getName());
                Thread.sleep(1000);
                log.info("{} 工作完成", Thread.currentThread().getName());
                
                lock.release(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
        
        Thread[] threads = new Thread[5];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(task, "Worker-" + i);
            threads[i].start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
    }

    private static void testCustomReentrantLock() throws InterruptedException {
        CustomReentrantLock lock = new CustomReentrantLock();
        
        Runnable task = () -> {
            lock.lock();
            try {
                log.info("{} 第一次获取锁", Thread.currentThread().getName());
                
                // 重入
                lock.lock();
                try {
                    log.info("{} 第二次获取锁（重入）", Thread.currentThread().getName());
                    Thread.sleep(300);
                } finally {
                    lock.unlock();
                    log.info("{} 第二次释放锁", Thread.currentThread().getName());
                }
                
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
                log.info("{} 第一次释放锁", Thread.currentThread().getName());
            }
        };
        
        Thread t1 = new Thread(task, "Thread-1");
        Thread t2 = new Thread(task, "Thread-2");
        
        t1.start();
        t2.start();
        
        t1.join();
        t2.join();
    }
}
