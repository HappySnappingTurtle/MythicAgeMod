#!/bin/bash

# 神话时代 Mod 编译脚本
# 使用可配置的 Gradle 版本进行编译

# Gradle 路径可通过 GRADLE_HOME 或 GRADLE_BIN 覆盖
GRADLE_HOME="${GRADLE_HOME:-}"
if [ -n "$GRADLE_HOME" ]; then
    GRADLE_BIN="${GRADLE_BIN:-$GRADLE_HOME/bin/gradle}"
else
    GRADLE_BIN="${GRADLE_BIN:-gradle}"
fi

# 检查 Gradle 是否存在
if ! command -v "$GRADLE_BIN" >/dev/null 2>&1; then
    echo "❌ 错误: 找不到 Gradle 可执行文件: $GRADLE_BIN"
    exit 1
fi

echo "🔧 使用 Gradle 编译神话时代 Mod..."
echo "📍 Gradle 命令: $GRADLE_BIN"
echo ""

# 显示 Gradle 版本信息
echo "📋 Gradle 版本信息:"
"$GRADLE_BIN" --version
echo ""

# 执行编译
echo "🚀 开始编译..."
"$GRADLE_BIN" "$@"

# 检查编译结果
if [ $? -eq 0 ]; then
    echo ""
    echo "✅ 编译成功完成!"
    echo "📦 生成的文件位于: build/libs/"
else
    echo ""
    echo "❌ 编译失败!"
    exit 1
fi 
