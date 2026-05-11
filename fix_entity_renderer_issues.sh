#!/bin/bash

# 神话时代Mod - 实体渲染器问题修复脚本
# 解决退出世界时卡住的问题

echo "🔧 开始修复神话时代Mod的实体渲染器问题..."

# 项目根目录
PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
RESOURCES_DIR="$PROJECT_ROOT/src/main/resources/assets/mythic_age"

# 1. 检查并创建缺失的纹理文件
echo "📁 检查纹理文件..."

TEXTURE_DIR="$RESOURCES_DIR/textures/item"
if [ ! -d "$TEXTURE_DIR" ]; then
    mkdir -p "$TEXTURE_DIR"
    echo "✅ 创建纹理目录: $TEXTURE_DIR"
fi

# 检查缺失的法术书纹理
MISSING_TEXTURES=("spellbook_dwarf_mine.png" "spellbook_giant_eagle.png")
for texture in "${MISSING_TEXTURES[@]}"; do
    if [ ! -f "$TEXTURE_DIR/$texture" ]; then
        echo "❌ 缺失纹理文件: $texture"
        # 创建一个占位符纹理文件（16x16像素的透明PNG）
        # 这里我们复制一个已存在的法术书纹理作为模板
        if [ -f "$TEXTURE_DIR/spellbook_odin_eye.png" ]; then
            cp "$TEXTURE_DIR/spellbook_odin_eye.png" "$TEXTURE_DIR/$texture"
            echo "✅ 创建占位符纹理: $texture"
        fi
    else
        echo "✅ 纹理文件存在: $texture"
    fi
done

# 2. 检查并创建缺失的音效文件
echo "🔊 检查音效文件..."

SOUNDS_DIR="$RESOURCES_DIR/sounds/entity/minotaur"
if [ ! -d "$SOUNDS_DIR" ]; then
    mkdir -p "$SOUNDS_DIR"
    echo "✅ 创建音效目录: $SOUNDS_DIR"
fi

# 创建缺失的米诺陶音效文件（空文件作为占位符）
MISSING_SOUNDS=("ambient.ogg" "hurt.ogg" "death.ogg" "step.ogg" "charge.ogg")
for sound in "${MISSING_SOUNDS[@]}"; do
    if [ ! -f "$SOUNDS_DIR/$sound" ]; then
        echo "❌ 缺失音效文件: $sound"
        # 创建一个空的音效文件作为占位符
        touch "$SOUNDS_DIR/$sound"
        echo "✅ 创建占位符音效: $sound"
    else
        echo "✅ 音效文件存在: $sound"
    fi
done

# 3. 检查并创建缺失的模型文件
echo "🎭 检查模型文件..."

MODELS_DIR="$RESOURCES_DIR/models"
ITEM_MODELS_DIR="$MODELS_DIR/item"
BLOCK_MODELS_DIR="$MODELS_DIR/block"

# 创建模型目录
for dir in "$MODELS_DIR" "$ITEM_MODELS_DIR" "$BLOCK_MODELS_DIR"; do
    if [ ! -d "$dir" ]; then
        mkdir -p "$dir"
        echo "✅ 创建模型目录: $dir"
    fi
done

# 创建基础的物品模型文件
MISSING_ITEM_MODELS=(
    "statue_greek_body.json"
    "statue_nordic_base.json"
    "temple_nordic_stairs.json"
    "statue_atlantean_legs.json"
    "statue_nordic_head.json"
    "statue_nordic.json"
    "statue_chinese.json"
    "temple_chinese_slab.json"
    "temple_nordic_bricks.json"
    "statue_egyptian.json"
    "relic_pedestal.json"
)

for model in "${MISSING_ITEM_MODELS[@]}"; do
    if [ ! -f "$ITEM_MODELS_DIR/$model" ]; then
        echo "❌ 缺失物品模型: $model"
        # 创建基础的物品模型JSON
        cat > "$ITEM_MODELS_DIR/$model" << EOF
{
  "parent": "item/generated",
  "textures": {
    "layer0": "mythic_age:item/${model%.json}"
  }
}
EOF
        echo "✅ 创建基础物品模型: $model"
    else
        echo "✅ 物品模型存在: $model"
    fi
done

# 4. 检查并修复blockstates文件
echo "🧱 检查方块状态文件..."

BLOCKSTATES_DIR="$RESOURCES_DIR/blockstates"
if [ ! -d "$BLOCKSTATES_DIR" ]; then
    mkdir -p "$BLOCKSTATES_DIR"
    echo "✅ 创建方块状态目录: $BLOCKSTATES_DIR"
fi

# 创建基础的方块状态文件
MISSING_BLOCKSTATES=(
    "relic_pedestal.json"
    "temple_greek_stairs.json"
    "abundance_altar_stairs.json"
    "temple_egyptian.json"
    "temple_chinese.json"
    "temple_atlantean.json"
    "temple_nordic.json"
    "statue_greek.json"
    "statue_egyptian.json"
    "statue_chinese.json"
    "statue_atlantean.json"
    "statue_nordic.json"
)

for blockstate in "${MISSING_BLOCKSTATES[@]}"; do
    if [ ! -f "$BLOCKSTATES_DIR/$blockstate" ]; then
        echo "❌ 缺失方块状态: $blockstate"
        # 创建基础的方块状态JSON
        cat > "$BLOCKSTATES_DIR/$blockstate" << EOF
{
  "variants": {
    "": {
      "model": "mythic_age:block/${blockstate%.json}"
    }
  }
}
EOF
        echo "✅ 创建基础方块状态: $blockstate"
    else
        echo "✅ 方块状态存在: $blockstate"
    fi
done

# 5. 检查实体渲染器注册
echo "🎨 检查实体渲染器注册..."

CLIENT_FILE="$PROJECT_ROOT/src/main/java/com/example/mythic_age/MythicAgeModClient.java"
if [ -f "$CLIENT_FILE" ]; then
    echo "✅ 客户端文件存在: MythicAgeModClient.java"
    
    # 检查是否有未注册的实体渲染器
    if grep -q "EntityRendererRegistry.register" "$CLIENT_FILE"; then
        echo "✅ 发现实体渲染器注册代码"
    else
        echo "❌ 未发现实体渲染器注册代码"
    fi
else
    echo "❌ 客户端文件不存在: MythicAgeModClient.java"
fi

# 6. 清理构建缓存
echo "🧹 清理构建缓存..."
cd "$PROJECT_ROOT"
if [ -d "build" ]; then
    rm -rf build
    echo "✅ 清理build目录"
fi

if [ -d ".gradle" ]; then
    rm -rf .gradle/caches
    echo "✅ 清理Gradle缓存"
fi

# 7. 重新构建项目
echo "🔨 重新构建项目..."
./gradlew clean build --no-daemon

BUILD_EXIT_CODE=$?
if [ $BUILD_EXIT_CODE -eq 0 ]; then
    echo "✅ 项目构建成功"
else
    echo "❌ 项目构建失败，退出代码: $BUILD_EXIT_CODE"
fi

echo ""
echo "🎯 修复完成总结："
echo "✅ 创建了缺失的纹理文件占位符"
echo "✅ 创建了缺失的音效文件占位符"
echo "✅ 创建了缺失的模型文件"
echo "✅ 创建了缺失的方块状态文件"
echo "✅ 清理了构建缓存"
echo "✅ 重新构建了项目"
echo ""
echo "🚀 建议的后续步骤："
echo "1. 运行 ./gradlew runClient 测试游戏"
echo "2. 如果仍有问题，检查最新的日志文件"
echo "3. 为占位符文件添加实际的纹理和音效内容"
echo "4. 测试退出世界功能是否正常"

exit $BUILD_EXIT_CODE
