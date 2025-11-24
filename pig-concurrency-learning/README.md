# 多线程并发高难度场景学习模块

## 📚 模块简介

本模块专注于多线程并发编程的高难度场景学习，涵盖了从经典并发问题到实际应用场景的全方位内容。通过大量的实战代码示例，帮助你深入理解 Java 并发编程的核心概念和高级技巧。

## 🎯 学习目标

1. **掌握经典并发问题**：生产者-消费者、哲学家就餐、读者-写者等
2. **理解并发问题本质**：死锁、活锁、饥饿的产生原因和解决方案
3. **精通并发工具类**：Lock、Semaphore、CountDownLatch、CyclicBarrier 等
4. **掌握高级并发场景**：缓存一致性、限流器、线程池调优等
5. **深入 AQS 原理**：自定义同步器的实现

## 📁 项目结构

```
pig-concurrency-learning/
├── src/main/java/com/pig4cloud/pig/concurrency/
│   ├── classic/                    # 经典并发问题
│   │   ├── ProducerConsumerProblem.java     # 生产者-消费者问题
│   │   ├── DiningPhilosophersProblem.java   # 哲学家就餐问题
│   │   └── ReaderWriterProblem.java         # 读者-写者问题
│   ├── problems/                   # 并发问题演示
│   │   ├── DeadlockLivelock.java            # 死锁和活锁
│   │   └── StarvationProblem.java           # 线程饥饿问题
│   └── advanced/                   # 高级并发场景
│       ├── CacheConsistency.java            # 缓存一致性
│       ├── RateLimiterImplementations.java  # 限流器实现
│       ├── ConcurrentToolsAdvanced.java     # 并发工具类高级用法
│       ├── AQSCustomLock.java               # 基于 AQS 自定义锁
│       └── ThreadPoolAdvanced.java          # 线程池高级用法
└── pom.xml
```

## 🔥 核心内容详解

### 1️⃣ 经典并发问题 (classic/)

#### 生产者-消费者问题
**文件**: `ProducerConsumerProblem.java`

**学习要点**:
- Lock 和 Condition 的使用
- 线程间通信机制
- 如何避免虚假唤醒（spurious wakeup）
- 为什么使用 while 而不是 if 检查条件

**运行方式**:
```bash
java com.pig4cloud.pig.concurrency.classic.ProducerConsumerProblem
```

**核心代码片段**:
```java
// 使用 while 循环防止虚假唤醒
while (queue.size() == capacity) {
    notFull.await(); // 等待缓冲区非满
}
queue.offer(item);
notEmpty.signal(); // 唤醒等待的消费者
```

---

#### 哲学家就餐问题
**文件**: `DiningPhilosophersProblem.java`

**学习要点**:
- 死锁的产生条件和预防
- 资源有序分配策略
- 信号量限制并发数

**两种解决方案**:
1. **资源有序分配**: 按编号顺序获取筷子，避免循环等待
2. **信号量限制**: 最多允许 N-1 个哲学家同时就餐

**运行方式**:
```bash
java com.pig4cloud.pig.concurrency.classic.DiningPhilosophersProblem
```

---

#### 读者-写者问题
**文件**: `ReaderWriterProblem.java`

**学习要点**:
- ReadWriteLock 的使用
- 读写锁的性能优势
- 锁降级（写锁降级为读锁）

**核心概念**:
- 读者之间不互斥，可以同时读
- 写者与任何读者或写者都互斥
- 锁降级保证更新后立即读取的一致性

**运行方式**:
```bash
java com.pig4cloud.pig.concurrency.classic.ReaderWriterProblem
```

---

### 2️⃣ 并发问题演示 (problems/)

#### 死锁和活锁
**文件**: `DeadlockLivelock.java`

**学习要点**:
- 死锁的四个必要条件
  1. 互斥：资源不能被共享
  2. 持有并等待：已持有资源的同时等待其他资源
  3. 不可剥夺：资源只能被持有者主动释放
  4. 循环等待：存在资源的循环等待链
- tryLock 带超时避免死锁
- 活锁的产生和解决（引入随机性）

**演示内容**:
1. 死锁场景演示
2. 使用 tryLock 解决死锁
3. 活锁场景演示
4. 引入随机性解决活锁

**运行方式**:
```bash
java com.pig4cloud.pig.concurrency.problems.DeadlockLivelock
```

---

#### 线程饥饿问题
**文件**: `StarvationProblem.java`

**学习要点**:
- 饥饿的产生原因
- 公平锁 vs 非公平锁
- 线程优先级的影响
- 避免饥饿的最佳实践

**演示对比**:
- 非公平锁导致的饥饿
- 公平锁解决饥饿
- 线程优先级的影响

**运行方式**:
```bash
java com.pig4cloud.pig.concurrency.problems.StarvationProblem
```

---

### 3️⃣ 高级并发场景 (advanced/)

#### 缓存一致性
**文件**: `CacheConsistency.java`

**学习要点**:
- 缓存穿透、击穿、雪崩
- 双重检查锁定（Double-Checked Locking）
- 使用 FutureTask 避免重复计算
- 缓存过期策略

**四种缓存实现**:
1. **朴素缓存**: 存在重复加载问题
2. **优化缓存**: 使用 FutureTask 避免重复加载
3. **过期缓存**: 支持 TTL 的缓存
4. **互斥锁缓存**: 防止缓存击穿

**运行方式**:
```bash
java com.pig4cloud.pig.concurrency.advanced.CacheConsistency
```

---

#### 限流器实现
**文件**: `RateLimiterImplementations.java`

**学习要点**:
- 固定窗口计数器
- 滑动窗口算法
- 令牌桶算法
- 漏桶算法
- 分布式限流

**五种限流算法对比**:

| 算法 | 优点 | 缺点 | 适用场景 |
|-----|------|------|---------|
| 固定窗口 | 简单高效 | 边界突刺流量 | 简单限流 |
| 滑动窗口 | 更平滑 | 内存占用高 | 精确限流 |
| 令牌桶 | 允许突发 | 实现复杂 | API 限流 |
| 漏桶 | 流量平滑 | 无法应对突发 | 流量整形 |
| 分布式 | 全局限流 | 需要中间件 | 微服务 |

**运行方式**:
```bash
java com.pig4cloud.pig.concurrency.advanced.RateLimiterImplementations
```

---

#### 并发工具类高级用法
**文件**: `ConcurrentToolsAdvanced.java`

**学习要点**:
- CountDownLatch: 等待多个线程完成
- CyclicBarrier: 线程间相互等待
- Semaphore: 信号量控制并发数
- Phaser: 更灵活的屏障
- Exchanger: 线程间数据交换
- CompletableFuture: 异步编程
- LongAdder: 高性能计数器

**七个实战示例**:
1. CountDownLatch 实现系统启动协调
2. CyclicBarrier 实现多阶段并行计算
3. Semaphore 实现数据库连接池
4. Phaser 实现动态多阶段任务
5. Exchanger 实现生产者消费者数据交换
6. CompletableFuture 实现异步任务编排
7. LongAdder vs AtomicInteger 性能对比

**运行方式**:
```bash
java com.pig4cloud.pig.concurrency.advanced.ConcurrentToolsAdvanced
```

---

#### 基于 AQS 自定义锁
**文件**: `AQSCustomLock.java`

**学习要点**:
- AQS (AbstractQueuedSynchronizer) 工作原理
- state 状态管理
- CLH 队列的使用
- 独占锁和共享锁的实现

**三种自定义锁**:
1. **CustomMutexLock**: 简单互斥锁
2. **CustomSharedLock**: 共享锁（类似 Semaphore）
3. **CustomReentrantLock**: 可重入锁

**核心代码**:
```java
@Override
protected boolean tryAcquire(int arg) {
    // 使用 CAS 将 state 从 0 改为 1
    if (compareAndSetState(0, 1)) {
        setExclusiveOwnerThread(Thread.currentThread());
        return true;
    }
    return false;
}
```

**运行方式**:
```bash
java com.pig4cloud.pig.concurrency.advanced.AQSCustomLock
```

---

#### 线程池高级用法
**文件**: `ThreadPoolAdvanced.java`

**学习要点**:
- 线程池核心参数详解
- 四种拒绝策略的使用场景
- 自定义线程工厂和拒绝策略
- 线程池监控
- 动态调整线程池参数
- 优雅关闭线程池

**四种拒绝策略**:
1. **AbortPolicy**: 抛出异常（默认）
2. **CallerRunsPolicy**: 调用者线程执行
3. **DiscardPolicy**: 丢弃任务
4. **DiscardOldestPolicy**: 丢弃最老任务

**线程池参数调优**:
- **CPU 密集型**: 线程数 = CPU 核心数 + 1
- **IO 密集型**: 线程数 = CPU 核心数 × 2

**运行方式**:
```bash
java com.pig4cloud.pig.concurrency.advanced.ThreadPoolAdvanced
```

---

## 🚀 快速开始

### 环境要求
- JDK 17+
- Maven 3.6+

### 编译项目
```bash
cd pig-concurrency-learning
mvn clean compile
```

### 运行示例
```bash
# 方式1: 使用 maven
mvn exec:java -Dexec.mainClass="com.pig4cloud.pig.concurrency.classic.ProducerConsumerProblem"

# 方式2: 编译后直接运行
mvn package
java -cp target/pig-concurrency-learning-1.0.0.jar com.pig4cloud.pig.concurrency.classic.ProducerConsumerProblem
```

---

## 📊 学习路径建议

### 初级阶段
1. 先学习经典并发问题 (classic/)
   - 理解基本的线程同步机制
   - 掌握 Lock 和 Condition 的使用

### 中级阶段
2. 学习并发问题演示 (problems/)
   - 了解死锁、活锁、饥饿的本质
   - 学会识别和避免这些问题

### 高级阶段
3. 学习高级并发场景 (advanced/)
   - 缓存一致性和限流器
   - 并发工具类的高级用法
   - 线程池调优

### 专家阶段
4. 深入 AQS 原理
   - 理解同步器的实现原理
   - 能够自定义同步器

---

## 💡 重要概念总结

### 线程安全的三大特性
1. **原子性**: 操作不可分割
2. **可见性**: 一个线程的修改对其他线程可见
3. **有序性**: 禁止指令重排序

### 实现线程安全的方法
1. **synchronized**: 内置锁
2. **Lock**: 显式锁（更灵活）
3. **volatile**: 保证可见性和有序性
4. **Atomic 类**: CAS 操作
5. **ThreadLocal**: 线程本地变量

### 锁的分类
- **乐观锁 vs 悲观锁**
- **公平锁 vs 非公平锁**
- **独占锁 vs 共享锁**
- **可重入锁 vs 不可重入锁**
- **自旋锁 vs 阻塞锁**

---

## 🎓 进阶资源推荐

### 书籍
1. 《Java 并发编程实战》- Brian Goetz
2. 《Java 并发编程的艺术》- 方腾飞
3. 《深入理解 Java 虚拟机》- 周志明

### 在线资源
1. JDK 并发包源码
2. Doug Lea 的论文
3. Java 官方文档

---

## 🔧 调试技巧

### 1. 线程 Dump 分析
```bash
# 获取线程 dump
jstack <pid> > thread_dump.txt

# 查找死锁
grep -A 10 "Found one Java-level deadlock" thread_dump.txt
```

### 2. 使用 JConsole 监控
```bash
jconsole
```

### 3. 使用 VisualVM 分析
```bash
jvisualvm
```

### 4. 添加 JVM 参数
```bash
# 打印锁信息
-XX:+PrintConcurrentLocks

# 打印 GC 日志
-XX:+PrintGCDetails
```

---

## ⚠️ 常见陷阱

### 1. Double-Checked Locking 陷阱
```java
// 错误写法（可能因为指令重排导致问题）
if (instance == null) {
    synchronized (Singleton.class) {
        if (instance == null) {
            instance = new Singleton(); // 非原子操作
        }
    }
}

// 正确写法（使用 volatile）
private volatile static Singleton instance;
```

### 2. 死锁的四个必要条件
必须同时满足才会死锁，破坏其中任意一个即可避免死锁。

### 3. ThreadLocal 内存泄漏
记得在使用完后调用 `remove()` 方法。

### 4. 线程池不要使用 Executors 创建
使用 `ThreadPoolExecutor` 手动创建，明确参数。

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

如果你有好的并发场景示例，欢迎贡献代码：
1. Fork 项目
2. 创建特性分支
3. 提交代码
4. 发起 Pull Request

---

## 📝 许可证

本项目采用 MIT 许可证。

---

## 📮 联系方式

如有问题，欢迎提 Issue 或联系作者。

---

## 🎉 致谢

感谢所有为 Java 并发编程做出贡献的开发者！

---

**祝学习愉快！🚀**

*持续更新中...*
