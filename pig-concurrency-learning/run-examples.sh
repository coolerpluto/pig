#!/bin/bash

# 多线程并发学习模块运行脚本

echo "=========================================="
echo "  多线程并发学习模块 - 示例运行器"
echo "=========================================="
echo ""

# 检查编译
if [ ! -d "target/classes" ]; then
    echo "❌ 未找到编译输出，请先运行 ./compile.sh"
    exit 1
fi

# 定义示例列表
declare -A examples=(
    ["1"]="生产者-消费者问题|com.pig4cloud.pig.concurrency.classic.ProducerConsumerProblem"
    ["2"]="哲学家就餐问题|com.pig4cloud.pig.concurrency.classic.DiningPhilosophersProblem"
    ["3"]="读者-写者问题|com.pig4cloud.pig.concurrency.classic.ReaderWriterProblem"
    ["4"]="死锁和活锁演示|com.pig4cloud.pig.concurrency.problems.DeadlockLivelock"
    ["5"]="线程饥饿问题|com.pig4cloud.pig.concurrency.problems.StarvationProblem"
    ["6"]="缓存一致性|com.pig4cloud.pig.concurrency.advanced.CacheConsistency"
    ["7"]="限流器实现|com.pig4cloud.pig.concurrency.advanced.RateLimiterImplementations"
    ["8"]="并发工具类高级用法|com.pig4cloud.pig.concurrency.advanced.ConcurrentToolsAdvanced"
    ["9"]="基于AQS自定义锁|com.pig4cloud.pig.concurrency.advanced.AQSCustomLock"
    ["10"]="线程池高级用法|com.pig4cloud.pig.concurrency.advanced.ThreadPoolAdvanced"
)

# 显示菜单
echo "请选择要运行的示例："
echo ""
echo "=== 经典并发问题 ==="
echo "1. 生产者-消费者问题"
echo "2. 哲学家就餐问题"
echo "3. 读者-写者问题"
echo ""
echo "=== 并发问题演示 ==="
echo "4. 死锁和活锁演示"
echo "5. 线程饥饿问题"
echo ""
echo "=== 高级并发场景 ==="
echo "6. 缓存一致性"
echo "7. 限流器实现"
echo "8. 并发工具类高级用法"
echo "9. 基于AQS自定义锁"
echo "10. 线程池高级用法"
echo ""
echo "0. 退出"
echo ""
echo -n "请输入选项 [0-10]: "

read choice

if [ "$choice" == "0" ]; then
    echo "再见！"
    exit 0
fi

if [ -n "${examples[$choice]}" ]; then
    IFS='|' read -r name class <<< "${examples[$choice]}"
    echo ""
    echo "=========================================="
    echo "  运行: $name"
    echo "=========================================="
    echo ""
    
    # 注意：由于缺少依赖，需要提示用户使用 Maven
    echo "⚠️  注意：本项目依赖 Lombok 和 SLF4J 库"
    echo "建议使用 Maven 运行："
    echo ""
    echo "mvn exec:java -Dexec.mainClass=\"$class\""
    echo ""
    echo "如果已安装 Maven，按回车继续..."
    read
    
    mvn exec:java -Dexec.mainClass="$class"
else
    echo "❌ 无效的选项！"
    exit 1
fi
