package com.pig4cloud.pig.concurrency.classic;

import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 生产者-消费者问题
 * 经典并发场景：多个生产者和消费者共享有界缓冲区
 * 
 * 学习要点：
 * 1. Lock 和 Condition 的使用
 * 2. 线程间通信机制
 * 3. 避免虚假唤醒（spurious wakeup）
 */
@Slf4j
public class ProducerConsumerProblem {

    /**
     * 使用 Lock 和 Condition 实现的有界缓冲区
     */
    static class BoundedBuffer<T> {
        private final Queue<T> queue = new LinkedList<>();
        private final int capacity;
        private final Lock lock = new ReentrantLock();
        private final Condition notFull = lock.newCondition();
        private final Condition notEmpty = lock.newCondition();

        public BoundedBuffer(int capacity) {
            this.capacity = capacity;
        }

        /**
         * 生产者放入数据
         * 关键点：使用 while 而不是 if 来检查条件，防止虚假唤醒
         */
        public void put(T item) throws InterruptedException {
            lock.lock();
            try {
                // 使用 while 循环防止虚假唤醒
                while (queue.size() == capacity) {
                    log.info("缓冲区已满，生产者 {} 等待...", Thread.currentThread().getName());
                    notFull.await(); // 等待缓冲区非满
                }
                
                queue.offer(item);
                log.info("生产者 {} 生产了: {}, 当前缓冲区大小: {}", 
                    Thread.currentThread().getName(), item, queue.size());
                
                notEmpty.signal(); // 唤醒等待的消费者
            } finally {
                lock.unlock();
            }
        }

        /**
         * 消费者取出数据
         */
        public T take() throws InterruptedException {
            lock.lock();
            try {
                while (queue.isEmpty()) {
                    log.info("缓冲区为空，消费者 {} 等待...", Thread.currentThread().getName());
                    notEmpty.await(); // 等待缓冲区非空
                }
                
                T item = queue.poll();
                log.info("消费者 {} 消费了: {}, 当前缓冲区大小: {}", 
                    Thread.currentThread().getName(), item, queue.size());
                
                notFull.signal(); // 唤醒等待的生产者
                return item;
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * 生产者线程
     */
    static class Producer implements Runnable {
        private final BoundedBuffer<Integer> buffer;
        private final int produceCount;

        public Producer(BoundedBuffer<Integer> buffer, int produceCount) {
            this.buffer = buffer;
            this.produceCount = produceCount;
        }

        @Override
        public void run() {
            try {
                for (int i = 0; i < produceCount; i++) {
                    buffer.put(i);
                    Thread.sleep(100); // 模拟生产耗时
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("生产者被中断", e);
            }
        }
    }

    /**
     * 消费者线程
     */
    static class Consumer implements Runnable {
        private final BoundedBuffer<Integer> buffer;
        private final int consumeCount;

        public Consumer(BoundedBuffer<Integer> buffer, int consumeCount) {
            this.buffer = buffer;
            this.consumeCount = consumeCount;
        }

        @Override
        public void run() {
            try {
                for (int i = 0; i < consumeCount; i++) {
                    buffer.take();
                    Thread.sleep(150); // 模拟消费耗时
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("消费者被中断", e);
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        BoundedBuffer<Integer> buffer = new BoundedBuffer<>(5);
        
        // 创建 3 个生产者
        Thread producer1 = new Thread(new Producer(buffer, 10), "Producer-1");
        Thread producer2 = new Thread(new Producer(buffer, 10), "Producer-2");
        Thread producer3 = new Thread(new Producer(buffer, 10), "Producer-3");
        
        // 创建 2 个消费者
        Thread consumer1 = new Thread(new Consumer(buffer, 15), "Consumer-1");
        Thread consumer2 = new Thread(new Consumer(buffer, 15), "Consumer-2");
        
        // 启动所有线程
        producer1.start();
        producer2.start();
        producer3.start();
        consumer1.start();
        consumer2.start();
        
        // 等待所有线程完成
        producer1.join();
        producer2.join();
        producer3.join();
        consumer1.join();
        consumer2.join();
        
        log.info("所有生产者和消费者已完成");
    }
}
