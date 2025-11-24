package com.pig4cloud.pig.concurrency.classic;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 读者-写者问题
 * 
 * 问题描述：
 * 多个读者和写者共享数据
 * 读者之间不互斥，可以同时读
 * 写者与任何读者或写者都互斥
 * 
 * 学习要点：
 * 1. ReadWriteLock 的使用
 * 2. 读写锁的性能优势
 * 3. 读者优先 vs 写者优先策略
 * 4. 锁降级（写锁降级为读锁）
 */
@Slf4j
public class ReaderWriterProblem {

    /**
     * 使用 ReadWriteLock 实现的共享资源
     */
    static class SharedResource {
        private int value = 0;
        private final ReadWriteLock rwLock = new ReentrantReadWriteLock(true); // true 表示公平锁

        /**
         * 读操作：多个读者可以同时读
         */
        public int read() {
            rwLock.readLock().lock();
            try {
                log.info("读者 {} 开始读取，当前值: {}", Thread.currentThread().getName(), value);
                // 模拟读取耗时
                Thread.sleep(100);
                log.info("读者 {} 读取完成", Thread.currentThread().getName());
                return value;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return -1;
            } finally {
                rwLock.readLock().unlock();
            }
        }

        /**
         * 写操作：写者独占访问
         */
        public void write(int newValue) {
            rwLock.writeLock().lock();
            try {
                log.info("写者 {} 开始写入，新值: {}", Thread.currentThread().getName(), newValue);
                // 模拟写入耗时
                Thread.sleep(200);
                value = newValue;
                log.info("写者 {} 写入完成", Thread.currentThread().getName());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                rwLock.writeLock().unlock();
            }
        }

        /**
         * 锁降级示例：从写锁降级到读锁
         * 场景：更新数据后立即读取，避免其他写者插入
         */
        public int writeAndRead(int newValue) {
            rwLock.writeLock().lock();
            try {
                log.info("写者 {} 开始写入并读取，新值: {}", Thread.currentThread().getName(), newValue);
                value = newValue;
                
                // 在释放写锁之前获取读锁（锁降级）
                rwLock.readLock().lock();
                try {
                    log.info("写者 {} 降级为读者，读取值: {}", Thread.currentThread().getName(), value);
                    return value;
                } finally {
                    rwLock.writeLock().unlock(); // 释放写锁
                }
            } finally {
                rwLock.readLock().unlock(); // 释放读锁
            }
        }
    }

    /**
     * 读者线程
     */
    static class Reader implements Runnable {
        private final SharedResource resource;
        private final int readCount;

        public Reader(SharedResource resource, int readCount) {
            this.resource = resource;
            this.readCount = readCount;
        }

        @Override
        public void run() {
            for (int i = 0; i < readCount; i++) {
                resource.read();
                try {
                    Thread.sleep(50); // 读取间隔
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /**
     * 写者线程
     */
    static class Writer implements Runnable {
        private final SharedResource resource;
        private final int writeCount;

        public Writer(SharedResource resource, int writeCount) {
            this.resource = resource;
            this.writeCount = writeCount;
        }

        @Override
        public void run() {
            for (int i = 0; i < writeCount; i++) {
                resource.write(i * 10);
                try {
                    Thread.sleep(300); // 写入间隔
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        SharedResource resource = new SharedResource();
        
        // 创建多个读者和少数写者
        Thread reader1 = new Thread(new Reader(resource, 5), "Reader-1");
        Thread reader2 = new Thread(new Reader(resource, 5), "Reader-2");
        Thread reader3 = new Thread(new Reader(resource, 5), "Reader-3");
        Thread reader4 = new Thread(new Reader(resource, 5), "Reader-4");
        
        Thread writer1 = new Thread(new Writer(resource, 3), "Writer-1");
        Thread writer2 = new Thread(new Writer(resource, 3), "Writer-2");
        
        // 启动所有线程
        reader1.start();
        reader2.start();
        writer1.start();
        reader3.start();
        reader4.start();
        writer2.start();
        
        // 等待所有线程完成
        reader1.join();
        reader2.join();
        reader3.join();
        reader4.join();
        writer1.join();
        writer2.join();
        
        log.info("所有读者和写者已完成");
        
        // 测试锁降级
        log.info("\n========== 测试锁降级 ==========");
        resource.writeAndRead(999);
    }
}
