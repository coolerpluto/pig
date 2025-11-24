# 多线程并发学习路线图

## 📊 学习阶段划分

```
Level 1: 基础入门 (1-2周)
    ↓
Level 2: 经典问题 (2-3周)
    ↓
Level 3: 高级应用 (3-4周)
    ↓
Level 4: 深入原理 (4-6周)
    ↓
Level 5: 实战项目 (持续)
```

---

## 🎯 Level 1: 基础入门 (1-2周)

### 学习目标
- 理解线程的基本概念
- 掌握线程的创建和启动
- 了解线程安全问题

### 必学内容

#### 1. 线程基础
```java
// 创建线程的三种方式
// 方式1: 继承 Thread
class MyThread extends Thread {
    public void run() {
        System.out.println("Hello from thread");
    }
}

// 方式2: 实现 Runnable
class MyRunnable implements Runnable {
    public void run() {
        System.out.println("Hello from runnable");
    }
}

// 方式3: 使用 Lambda
new Thread(() -> System.out.println("Hello")).start();
```

#### 2. synchronized 关键字
```java
// 同步方法
public synchronized void method() {
    // 线程安全的代码
}

// 同步代码块
public void method() {
    synchronized (this) {
        // 线程安全的代码
    }
}
```

#### 3. volatile 关键字
```java
// 保证可见性
private volatile boolean flag = true;

public void stop() {
    flag = false;  // 立即对其他线程可见
}
```

### 练习项目
- [ ] 创建10个线程并发执行
- [ ] 实现一个计数器，测试线程安全问题
- [ ] 使用 synchronized 解决计数器的线程安全问题

### 推荐资源
- Java 官方文档：Concurrency
- 视频教程：《尚硅谷 Java 多线程》

---

## 🎯 Level 2: 经典问题 (2-3周)

### 学习目标
- 理解并发编程的经典问题
- 掌握 Lock 和 Condition 的使用
- 学会使用等待/通知机制

### 必学内容

#### 1. 生产者-消费者问题
**文件**: `ProducerConsumerProblem.java`

**核心知识点**:
- Lock 和 Condition 的配合使用
- 为什么要用 while 而不是 if 检查条件
- 如何避免虚假唤醒

**练习**:
- [ ] 运行示例代码
- [ ] 修改缓冲区大小，观察行为变化
- [ ] 增加生产者和消费者数量
- [ ] 尝试用 BlockingQueue 重新实现

#### 2. 哲学家就餐问题
**文件**: `DiningPhilosophersProblem.java`

**核心知识点**:
- 死锁的四个必要条件
- 资源有序分配策略
- 信号量的使用

**练习**:
- [ ] 理解两种解决方案的原理
- [ ] 尝试实现第三种方案：奇偶数哲学家策略
- [ ] 模拟死锁场景并分析

#### 3. 读者-写者问题
**文件**: `ReaderWriterProblem.java`

**核心知识点**:
- ReadWriteLock 的使用
- 读写锁的性能优势
- 锁降级的应用

**练习**:
- [ ] 对比 synchronized 和 ReadWriteLock 的性能
- [ ] 实现读者优先策略
- [ ] 实现写者优先策略

### 阶段测试
完成以下任务检验学习效果：
1. 不看代码，独立实现生产者-消费者模式
2. 解释死锁的四个必要条件，并举例说明
3. 说出至少3种避免死锁的方法

---

## 🎯 Level 3: 高级应用 (3-4周)

### 学习目标
- 掌握并发工具类的使用
- 理解线程池的原理和使用
- 学会缓存一致性和限流的实现

### 必学内容

#### 1. 并发工具类
**文件**: `ConcurrentToolsAdvanced.java`

**学习顺序**:
```
CountDownLatch (最简单)
    ↓
Semaphore (控制并发数)
    ↓
CyclicBarrier (可重用的屏障)
    ↓
Phaser (更灵活的屏障)
    ↓
Exchanger (线程间交换数据)
    ↓
CompletableFuture (异步编程)
```

**练习项目**:
- [ ] 使用 CountDownLatch 实现并行加载多个资源
- [ ] 使用 Semaphore 实现数据库连接池
- [ ] 使用 CyclicBarrier 实现多线程协作计算
- [ ] 使用 CompletableFuture 重构回调地狱代码

#### 2. 线程池
**文件**: `ThreadPoolAdvanced.java`

**核心参数理解**:
```java
new ThreadPoolExecutor(
    corePoolSize,       // 核心线程数
    maximumPoolSize,    // 最大线程数
    keepAliveTime,      // 空闲线程存活时间
    unit,               // 时间单位
    workQueue,          // 任务队列
    threadFactory,      // 线程工厂
    handler             // 拒绝策略
)
```

**任务执行流程**:
```
提交任务
    ↓
核心线程未满？ → 是 → 创建核心线程执行
    ↓ 否
队列未满？ → 是 → 加入队列等待
    ↓ 否
最大线程数未达到？ → 是 → 创建非核心线程执行
    ↓ 否
执行拒绝策略
```

**练习**:
- [ ] 计算你的应用适合的线程数
- [ ] 实现自定义线程工厂
- [ ] 实现自定义拒绝策略
- [ ] 监控线程池状态并输出报表

#### 3. 缓存一致性
**文件**: `CacheConsistency.java`

**四种缓存实现对比**:

| 实现 | 特点 | 适用场景 |
|-----|------|---------|
| NaiveCache | 简单但有重复加载 | 学习用 |
| OptimizedCache | 使用 FutureTask | 生产环境 |
| ExpiringCache | 支持过期 | 需要 TTL |
| MutexCache | 防止击穿 | 热点数据 |

**练习**:
- [ ] 模拟缓存穿透场景并解决
- [ ] 实现 LRU 缓存淘汰策略
- [ ] 实现缓存预热功能

#### 4. 限流器
**文件**: `RateLimiterImplementations.java`

**算法对比**:
```
固定窗口 → 简单但不精确
    ↓
滑动窗口 → 更精确但内存占用高
    ↓
令牌桶 → 允许突发流量
    ↓
漏桶 → 流量平滑
```

**练习**:
- [ ] 实现每个算法并测试
- [ ] 对比不同算法的性能
- [ ] 实现分布式限流（基于 Redis）

### 阶段项目
实现一个完整的「文章访问系统」：
- 使用缓存存储热门文章
- 使用限流器控制访问频率
- 使用线程池处理异步任务
- 记录访问日志

---

## 🎯 Level 4: 深入原理 (4-6周)

### 学习目标
- 理解 AQS 的工作原理
- 掌握 JMM (Java Memory Model)
- 能够自定义同步器

### 必学内容

#### 1. AQS 原理
**文件**: `AQSCustomLock.java`

**核心概念**:
```
AQS (AbstractQueuedSynchronizer)
    ↓
state (同步状态)
    ↓
CLH 队列 (FIFO 等待队列)
    ↓
独占模式 vs 共享模式
```

**练习**:
- [ ] 阅读 ReentrantLock 源码
- [ ] 实现一个自定义的互斥锁
- [ ] 实现一个自定义的读写锁
- [ ] 实现一个自定义的信号量

#### 2. Java 内存模型 (JMM)

**核心概念**:
- 主内存和工作内存
- happens-before 规则
- volatile 的实现原理
- final 的语义

**happens-before 规则**:
1. 程序顺序规则
2. 监视器锁规则
3. volatile 变量规则
4. 线程启动规则
5. 线程终止规则
6. 线程中断规则
7. 对象终结规则
8. 传递性规则

**练习**:
- [ ] 分析 DCL (Double-Checked Locking) 问题
- [ ] 理解指令重排序的影响
- [ ] 使用 JOL 工具查看对象布局

#### 3. CAS 和原子类

**原理**:
```
Compare And Swap (CAS)
    ↓
乐观锁的实现基础
    ↓
ABA 问题
    ↓
解决方案：版本号或时间戳
```

**练习**:
- [ ] 使用 Unsafe 实现 CAS 操作
- [ ] 对比 AtomicInteger 和 LongAdder
- [ ] 实现一个无锁栈

#### 4. 并发容器

**常用容器**:
- ConcurrentHashMap
- CopyOnWriteArrayList
- ConcurrentLinkedQueue
- BlockingQueue 家族

**练习**:
- [ ] 分析 ConcurrentHashMap 的分段锁实现
- [ ] 理解 CopyOnWrite 的适用场景
- [ ] 实现一个有界的并发队列

### 深入研究项目
选择以下任一主题深入研究：
1. 阅读并分析 Doug Lea 的 AQS 论文
2. 研究 ForkJoinPool 的工作窃取算法
3. 分析 Disruptor 的无锁队列实现

---

## 🎯 Level 5: 实战项目 (持续)

### 项目1: 高并发秒杀系统

**技术要点**:
- Redis 分布式锁
- 消息队列削峰
- 限流降级
- 缓存预热

**实现步骤**:
1. 设计秒杀接口
2. 实现库存扣减（防止超卖）
3. 实现限流（保护系统）
4. 实现缓存（提升性能）
5. 压力测试和调优

### 项目2: 分布式任务调度系统

**技术要点**:
- 任务队列
- 线程池管理
- 任务重试机制
- 任务监控

**实现步骤**:
1. 设计任务模型
2. 实现任务调度器
3. 实现任务执行器
4. 实现任务监控和告警

### 项目3: 实时数据处理系统

**技术要点**:
- 流式处理
- 背压处理
- 并行计算
- 结果聚合

**实现步骤**:
1. 设计数据流架构
2. 实现数据接收和缓冲
3. 实现并行处理逻辑
4. 实现结果聚合和输出

---

## 📚 推荐阅读路线

### 书籍

#### 入门级
1. 《Java 核心技术 卷1》第14章：并发
2. 《Head First Java》多线程部分

#### 进阶级
3. **《Java 并发编程实战》** ⭐⭐⭐⭐⭐
   - 必读经典
   - 系统全面
   
4. **《Java 并发编程的艺术》** ⭐⭐⭐⭐⭐
   - 深入原理
   - 适合进阶

#### 高级
5. 《深入理解 Java 虚拟机》第12-13章
6. Doug Lea 的论文集

### 源码阅读顺序

```
ReentrantLock
    ↓
ReentrantReadWriteLock
    ↓
CountDownLatch
    ↓
Semaphore
    ↓
CyclicBarrier
    ↓
ThreadPoolExecutor
    ↓
ConcurrentHashMap
    ↓
ForkJoinPool
```

---

## 🎓 学习建议

### 1. 理论与实践结合
- 看完理论立即动手编码
- 不要只看不练
- 多做实验，观察现象

### 2. 循序渐进
- 不要跳级学习
- 扎实掌握每个阶段的内容
- 遇到困难不要放弃

### 3. 多读源码
- JDK 并发包源码是最好的教材
- 理解大师的设计思想
- 学习代码风格和注释

### 4. 总结输出
- 写技术博客
- 做技术分享
- 教别人是最好的学习方式

### 5. 关注实战
- 在项目中应用所学
- 解决实际问题
- 积累经验

---

## ✅ 学习检查清单

### Level 1 检查点
- [ ] 能够创建和启动线程
- [ ] 理解线程安全问题
- [ ] 能够使用 synchronized
- [ ] 理解 volatile 的作用

### Level 2 检查点
- [ ] 能够独立实现生产者-消费者
- [ ] 理解死锁的原因和预防
- [ ] 掌握 Lock 和 Condition 的使用
- [ ] 能够解决哲学家就餐问题

### Level 3 检查点
- [ ] 熟练使用并发工具类
- [ ] 能够正确配置线程池
- [ ] 理解缓存一致性问题
- [ ] 能够实现限流器

### Level 4 检查点
- [ ] 理解 AQS 的工作原理
- [ ] 能够自定义同步器
- [ ] 理解 JMM
- [ ] 掌握 happens-before 规则

### Level 5 检查点
- [ ] 完成至少一个实战项目
- [ ] 能够进行性能调优
- [ ] 能够解决生产环境问题
- [ ] 能够指导他人

---

## 🎯 学习时间规划建议

### 每天2小时的学习计划

**第1-2周**: Level 1 基础入门
- 每天1小时理论学习
- 每天1小时编码实践

**第3-5周**: Level 2 经典问题
- 每天30分钟复习前一天内容
- 每天1.5小时新内容学习和实践

**第6-9周**: Level 3 高级应用
- 每天分模块学习
- 周末做综合练习

**第10-15周**: Level 4 深入原理
- 阅读源码
- 做笔记和总结

**第16周+**: Level 5 实战项目
- 边学边用
- 持续积累

---

## 💪 学习激励

> "并发编程是 Java 开发者的必备技能"

> "掌握并发编程，你就掌握了高性能系统的钥匙"

> "每一个并发问题的解决，都是一次思维的升华"

**坚持就是胜利！加油！🚀**

---

## 📞 获取帮助

遇到问题时：
1. 查阅本项目的示例代码
2. 阅读 Java 官方文档
3. 搜索相关技术博客
4. 在技术论坛提问
5. 阅读相关书籍

**记住：没有笨问题，只有不问的问题！**
