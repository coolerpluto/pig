# 快速入门指南

## 🎯 5分钟快速体验

### 1. 运行第一个示例：生产者-消费者

```bash
# 编译项目
cd /workspace/pig-concurrency-learning
mvn clean compile

# 运行生产者-消费者示例
mvn exec:java -Dexec.mainClass="com.pig4cloud.pig.concurrency.classic.ProducerConsumerProblem"
```

**预期输出**：
```
缓冲区已满，生产者 Producer-1 等待...
消费者 Consumer-1 消费了: 0, 当前缓冲区大小: 4
生产者 Producer-1 生产了: 5, 当前缓冲区大小: 5
...
```

### 2. 观察死锁现象

```bash
mvn exec:java -Dexec.mainClass="com.pig4cloud.pig.concurrency.problems.DeadlockLivelock"
```

**你将看到**：
- 两个线程相互等待对方释放锁
- 线程状态变为 BLOCKED
- 然后看到使用 tryLock 成功避免死锁

### 3. 测试限流器

```bash
mvn exec:java -Dexec.mainClass="com.pig4cloud.pig.concurrency.advanced.RateLimiterImplementations"
```

**你将体验到**：
- 固定窗口计数器的简单限流
- 令牌桶的突发流量处理
- 漏桶的平滑限流效果

---

## 📚 按场景学习

### 场景1: 我需要实现一个生产者消费者模式

**推荐阅读**: `ProducerConsumerProblem.java`

**关键代码**:
```java
Lock lock = new ReentrantLock();
Condition notFull = lock.newCondition();
Condition notEmpty = lock.newCondition();

// 生产者
lock.lock();
try {
    while (queue.isFull()) {
        notFull.await();
    }
    queue.add(item);
    notEmpty.signal();
} finally {
    lock.unlock();
}

// 消费者
lock.lock();
try {
    while (queue.isEmpty()) {
        notEmpty.await();
    }
    T item = queue.take();
    notFull.signal();
    return item;
} finally {
    lock.unlock();
}
```

---

### 场景2: 我的应用出现了死锁

**推荐阅读**: `DeadlockLivelock.java`

**诊断步骤**:
1. 获取线程 dump: `jstack <pid> > dump.txt`
2. 搜索 "deadlock": `grep -A 20 "deadlock" dump.txt`
3. 分析锁的持有和等待关系

**解决方案**:
1. **资源有序分配**: 总是按相同顺序获取锁
2. **使用 tryLock**: 设置超时时间
3. **减少锁的持有时间**: 缩小同步代码块
4. **使用并发工具类**: 如 Semaphore

---

### 场景3: 我需要实现一个本地缓存

**推荐阅读**: `CacheConsistency.java`

**选择合适的缓存实现**:

| 需求 | 推荐实现 |
|-----|---------|
| 简单缓存，不考虑并发 | NaiveCache |
| 避免缓存击穿 | OptimizedCache (FutureTask) |
| 需要过期时间 | ExpiringCache |
| 高并发热点数据 | MutexCache |

**示例**:
```java
// 使用 FutureTask 避免重复计算
OptimizedCache<String, User> cache = new OptimizedCache<>();
User user = cache.get(userId, () -> {
    // 这段代码只会被一个线程执行
    return userService.loadFromDB(userId);
});
```

---

### 场景4: 我需要限制 API 调用频率

**推荐阅读**: `RateLimiterImplementations.java`

**选择合适的限流算法**:

```java
// 1. 简单场景：固定窗口
FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(100, 1000);
if (limiter.tryAcquire()) {
    // 执行业务逻辑
}

// 2. 允许突发：令牌桶
TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(100, 10.0);
if (limiter.tryAcquire()) {
    // 执行业务逻辑
}

// 3. 流量整形：漏桶
LeakyBucketRateLimiter limiter = new LeakyBucketRateLimiter(100, 10.0);
if (limiter.tryAcquire()) {
    // 执行业务逻辑
}
```

---

### 场景5: 我需要调优线程池

**推荐阅读**: `ThreadPoolAdvanced.java`

**线程数计算**:
```java
int cpuCount = Runtime.getRuntime().availableProcessors();

// CPU 密集型任务
int cpuIntensiveThreads = cpuCount + 1;

// IO 密集型任务
int ioIntensiveThreads = cpuCount * 2;

// 创建线程池
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    cpuCount,              // 核心线程数
    cpuCount * 2,          // 最大线程数
    60L,                   // 空闲线程存活时间
    TimeUnit.SECONDS,
    new LinkedBlockingQueue<>(100),  // 有界队列
    new CustomThreadFactory("MyPool", false),
    new ThreadPoolExecutor.CallerRunsPolicy()
);
```

**监控线程池**:
```java
log.info("活跃线程数: {}", executor.getActiveCount());
log.info("队列大小: {}", executor.getQueue().size());
log.info("已完成任务数: {}", executor.getCompletedTaskCount());
```

---

## 🔧 实用工具类速查

### CountDownLatch - 等待多个线程完成
```java
CountDownLatch latch = new CountDownLatch(3);

// 工作线程
new Thread(() -> {
    doWork();
    latch.countDown();
}).start();

// 主线程等待
latch.await();
System.out.println("所有任务完成");
```

### CyclicBarrier - 多线程相互等待
```java
CyclicBarrier barrier = new CyclicBarrier(3, () -> {
    System.out.println("所有线程到达屏障");
});

// 工作线程
new Thread(() -> {
    doPhase1();
    barrier.await();  // 等待其他线程
    doPhase2();
}).start();
```

### Semaphore - 控制并发数
```java
Semaphore semaphore = new Semaphore(3);  // 最多3个线程

semaphore.acquire();
try {
    // 访问受限资源
} finally {
    semaphore.release();
}
```

### CompletableFuture - 异步编程
```java
CompletableFuture.supplyAsync(() -> {
    return getUserId();
})
.thenApply(userId -> {
    return getOrdersByUserId(userId);
})
.thenAccept(orders -> {
    System.out.println("订单数: " + orders.size());
});
```

---

## 🐛 调试技巧

### 1. 打印线程信息
```java
Thread thread = Thread.currentThread();
log.info("线程名: {}, ID: {}, 状态: {}, 优先级: {}", 
    thread.getName(), 
    thread.getId(), 
    thread.getState(),
    thread.getPriority());
```

### 2. 检查锁的持有情况
```java
ReentrantLock lock = new ReentrantLock();
log.info("是否被锁定: {}", lock.isLocked());
log.info("当前线程是否持有锁: {}", lock.isHeldByCurrentThread());
log.info("等待线程数: {}", lock.getQueueLength());
```

### 3. 使用 ThreadMXBean 监控
```java
ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
long[] deadlockedThreads = threadMXBean.findDeadlockedThreads();
if (deadlockedThreads != null) {
    log.error("发现死锁！线程数: {}", deadlockedThreads.length);
}
```

---

## ⚡ 性能优化建议

### 1. 减少锁的粒度
```java
// 不推荐：锁整个方法
public synchronized void updateUser(User user) {
    validateUser(user);    // 不需要同步
    saveToDatabase(user);  // 需要同步
    sendNotification();    // 不需要同步
}

// 推荐：只锁必要的部分
public void updateUser(User user) {
    validateUser(user);
    synchronized (this) {
        saveToDatabase(user);
    }
    sendNotification();
}
```

### 2. 使用 ConcurrentHashMap
```java
// 不推荐
Map<String, User> users = Collections.synchronizedMap(new HashMap<>());

// 推荐
Map<String, User> users = new ConcurrentHashMap<>();
```

### 3. 使用 LongAdder 替代 AtomicLong
```java
// 高并发场景下，LongAdder 性能更好
LongAdder counter = new LongAdder();
counter.increment();
long sum = counter.sum();
```

### 4. 避免在循环中创建线程
```java
// 不推荐
for (int i = 0; i < 1000; i++) {
    new Thread(() -> doWork()).start();
}

// 推荐：使用线程池
ExecutorService executor = Executors.newFixedThreadPool(10);
for (int i = 0; i < 1000; i++) {
    executor.submit(() -> doWork());
}
executor.shutdown();
```

---

## 📝 最佳实践清单

- [ ] 优先使用并发工具类而不是 wait/notify
- [ ] 使用 ThreadPoolExecutor 手动创建线程池
- [ ] 为线程和线程池设置有意义的名称
- [ ] 正确处理 InterruptedException
- [ ] 使用 try-finally 确保锁被释放
- [ ] 避免在锁内部进行耗时操作
- [ ] 使用 volatile 保证可见性
- [ ] ThreadLocal 使用完记得 remove
- [ ] 使用有界队列防止内存溢出
- [ ] 定期监控线程池状态

---

## 🎓 下一步学习

完成快速入门后，建议按以下顺序深入学习：

1. **深入理解 AQS**: 阅读 `AQSCustomLock.java`
2. **学习 JMM**: 了解 Java 内存模型
3. **研究 JUC 源码**: CountDownLatch, ReentrantLock 等
4. **实战项目**: 将所学应用到实际项目中

---

## ❓ 常见问题

### Q: synchronized 和 Lock 有什么区别？
A: 
- synchronized 是关键字，Lock 是接口
- Lock 更灵活，支持尝试获取锁、超时、可中断
- Lock 需要手动释放，synchronized 自动释放
- Lock 可以实现公平锁

### Q: 什么时候使用 volatile？
A:
- 单纯的读写操作（如标志位）
- 不依赖当前值的写操作
- 不需要和其他变量保持不变约束

### Q: 如何选择线程池大小？
A:
- CPU 密集型：CPU 核心数 + 1
- IO 密集型：CPU 核心数 × 2
- 混合型：根据实际测试调整

### Q: CompletableFuture 什么时候用？
A:
- 需要异步执行任务
- 需要组合多个异步操作
- 需要更灵活的异常处理

---

**准备好了吗？开始你的并发编程之旅吧！🚀**
