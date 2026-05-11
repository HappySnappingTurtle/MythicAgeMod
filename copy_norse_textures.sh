#!/bin/bash

# 北欧法术纹理文件复制脚本
# 使用方法: ./copy_norse_textures.sh [源目录路径]

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
SOURCE_DIR="${1:-./textures/items}"
TARGET_DIR="$PROJECT_ROOT/src/main/resources/assets/mythic_age/textures/item"

echo "=== 北欧法术纹理文件复制脚本 ==="
echo "源目录: $SOURCE_DIR"
echo "目标目录: $TARGET_DIR"
echo ""

# 检查源目录是否存在
if [ ! -d "$SOURCE_DIR" ]; then
    echo "❌ 错误: 源目录不存在: $SOURCE_DIR"
    echo "请确认路径是否正确，或者手动指定源目录路径："
    echo "例如: ./copy_norse_textures.sh /path/to/your/textures"
    exit 1
fi

# 检查目标目录是否存在
if [ ! -d "$TARGET_DIR" ]; then
    echo "❌ 错误: 目标目录不存在: $TARGET_DIR"
    exit 1
fi

echo "📁 正在扫描源目录中的北欧法术纹理文件..."

# 定义北欧法术书纹理文件列表
NORSE_SPELLBOOKS=(
    "spellbook_fenrir_howl.png"
    "spellbook_odin_eye.png" 
    "spellbook_great_hunt.png"
    "spellbook_dwarven_mine.png"
    "spellbook_frost.png"
    "spellbook_walking_woods.png"
    "spellbook_thor_wrath.png"
    "spellbook_valhalla_call.png"
    "spellbook_ragnarok.png"
    "spellbook_fimbulwinter.png"
)

COPIED_COUNT=0
MISSING_COUNT=0

for file in "${NORSE_SPELLBOOKS[@]}"; do
    SOURCE_FILE="$SOURCE_DIR/$file"
    TARGET_FILE="$TARGET_DIR/$file"
    
    if [ -f "$SOURCE_FILE" ]; then
        if [ -f "$TARGET_FILE" ]; then
            echo "🔄 替换 $file (覆盖已存在文件)"
        else
            echo "✅ 复制 $file"
        fi
        
        cp "$SOURCE_FILE" "$TARGET_FILE"
        if [ $? -eq 0 ]; then
            COPIED_COUNT=$((COPIED_COUNT + 1))
        else
            echo "❌ 复制失败 $file"
        fi
    else
        echo "⚠️  未找到 $file"
        MISSING_COUNT=$((MISSING_COUNT + 1))
    fi
done

echo ""
echo "=== 复制结果统计 ==="
echo "✅ 成功复制/替换: $COPIED_COUNT 个文件"
echo "❌ 缺失文件: $MISSING_COUNT 个文件"
echo ""

if [ $COPIED_COUNT -gt 0 ]; then
    echo "🎉 纹理文件复制完成！"
    echo "📍 文件位置: $TARGET_DIR"
    echo ""
    echo "下一步操作："
    echo "1. 运行 ./gradlew build 编译项目"
    echo "2. 运行 ./gradlew runClient 启动游戏测试"
fi
