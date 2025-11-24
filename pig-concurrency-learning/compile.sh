#!/bin/bash

# 多线程并发学习模块编译脚本

echo "=========================================="
echo "  多线程并发学习模块 - 编译脚本"
echo "=========================================="
echo ""

# 创建输出目录
mkdir -p target/classes

# 查找所有 Java 源文件
SOURCE_DIR="src/main/java"
OUTPUT_DIR="target/classes"

echo "正在编译 Java 源文件..."
echo ""

# 使用 javac 编译（因为没有 Maven 依赖，可以直接编译）
find "$SOURCE_DIR" -name "*.java" | xargs javac -d "$OUTPUT_DIR" -encoding UTF-8

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ 编译成功！"
    echo ""
    echo "输出目录: $OUTPUT_DIR"
    echo ""
    echo "提示: 由于缺少依赖库（Lombok, SLF4J），某些功能可能无法正常工作。"
    echo "建议安装 Maven 后使用 'mvn clean compile' 进行完整编译。"
else
    echo ""
    echo "❌ 编译失败！"
    echo ""
    echo "可能的原因："
    echo "1. 缺少依赖库（Lombok, SLF4J）"
    echo "2. 代码语法错误"
    echo ""
    echo "解决方案："
    echo "1. 安装 Maven: sudo apt-get install maven"
    echo "2. 使用 Maven 编译: mvn clean compile"
fi

echo ""
echo "=========================================="
