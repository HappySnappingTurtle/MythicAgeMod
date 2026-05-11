# 多方块结构搭建与NBT文件生成指南

## 📖 概述

本指南将详细说明如何在Minecraft中搭建多方块结构，并将其保存为`.nbt`文件用于神话时代Mod的结构生成系统。

## 🛠️ 前置准备

### 1. 开启创造模式
```
/gamemode creative
```

### 2. 获取结构方块
```
/give @s structure_block
```

### 3. 开启结构方块GUI显示
```
/gamerule showStructureBlocks true
```

## 🏗️ 结构搭建流程

### 第一步：规划建筑区域

#### 1.1 选择平坦区域
- 找一个足够大的平坦区域（建议至少30×30格）
- 确保周围没有其他建筑干扰

#### 1.2 标记建筑基准点
- 在建筑中心位置放置一个显眼的方块（如金块）作为基准点
- 这将是NBT文件的原点(0,0,0)

### 第二步：搭建神庙结构

#### 2.1 按照设计文档搭建
参考《神庙结构设计与NBT文件需求文档.md》中的规格：

**埃及神庙示例：**
- 尺寸：11×7×9（小型）
- 使用方块：
  - `temple_egyptian_bricks`（主体）
  - `temple_egyptian_stairs`（台阶）
  - `temple_egyptian_slab`（装饰）
  - `temple_egyptian_pillar`（柱子）
  - `temple_egyptian_relief`（浮雕）

#### 2.2 建筑要点
- **入口朝向**：建议朝南（Z轴正方向）
- **地基处理**：确保底部平整，可以包含地面装饰
- **内部空间**：预留足够的内部空间放置NPC和宝箱
- **对称美观**：注意建筑的对称性和美观度

### 第三步：放置结构方块

#### 3.1 确定结构边界
- 计算结构的最小包围盒
- 记录XYZ三个维度的最大范围

#### 3.2 放置角点结构方块
```
在结构的西北下角（最小x,y,z坐标）放置结构方块
```

#### 3.3 设置结构方块参数
右键点击结构方块，设置以下参数：
- **模式**：保存（Save）
- **结构名称**：`egyptian_temple_small`（按规范命名）
- **相对位置**：`0 0 0`
- **结构大小**：根据实际尺寸设置，如`11 7 9`

## 💾 NBT文件生成与保存

### 第四步：生成NBT文件

#### 4.1 点击保存按钮
在结构方块GUI中点击"保存"按钮

#### 4.2 查找生成的文件
NBT文件会保存在：
```
存档文件夹/generated/minecraft/structures/egyptian_temple_small.nbt
```

### 第五步：文件管理

#### 5.1 复制到项目目录
将生成的`.nbt`文件复制到项目的指定位置：
```
src/main/resources/data/mythic_age/structures/
```

#### 5.2 文件命名规范
```
{文明}_{建筑类型}_{尺寸}.nbt

示例：
- egyptian_temple_small.nbt
- egyptian_temple_medium.nbt
- egyptian_temple_large.nbt
- chinese_temple_small.nbt
- atlantean_temple_medium.nbt
- nordic_temple_large.nbt
```

## 📁 项目结构组织

### 目录结构
```
src/main/resources/data/mythic_age/structures/
├── temple/
│   ├── egyptian/
│   │   ├── egyptian_temple_small.nbt
│   │   ├── egyptian_temple_medium.nbt
│   │   └── egyptian_temple_large.nbt
│   ├── chinese/
│   │   ├── chinese_temple_small.nbt
│   │   ├── chinese_temple_medium.nbt
│   │   └── chinese_temple_large.nbt
│   ├── atlantean/
│   │   ├── atlantean_temple_small.nbt
│   │   ├── atlantean_temple_medium.nbt
│   │   └── atlantean_temple_large.nbt
│   └── nordic/
│       ├── nordic_temple_small.nbt
│       ├── nordic_temple_medium.nbt
│       └── nordic_temple_large.nbt
└── village/
    ├── egyptian/
    ├── chinese/
    ├── atlantean/
    └── nordic/
```

## 🔧 代码集成

### 第六步：在代码中引用NBT结构

#### 6.1 注册结构文件
在对应的结构配置JSON文件中引用：
```json
{
  "type": "minecraft:temple",
  "spawn_overrides": {},
  "terrain_adaptation": "beard_thin",
  "biomes": "#minecraft:has_structure/desert_pyramid",
  "step": "surface_structures",
  "start_pool": "mythic_age:temple/egyptian/egyptian_temple_pool",
  "size": 3
}
```

#### 6.2 创建模板池文件
在`template_pool`目录中创建对应的JSON配置：
```json
{
  "name": "mythic_age:temple/egyptian/egyptian_temple_pool",
  "fallback": "minecraft:empty",
  "elements": [
    {
      "weight": 1,
      "element": {
        "element_type": "minecraft:single_pool_element",
        "projection": "rigid",
        "location": "mythic_age:temple/egyptian/egyptian_temple_small",
        "processors": "minecraft:empty"
      }
    }
  ]
}
```

## ✅ 测试与验证

### 第七步：测试结构生成

#### 7.1 使用结构生成命令测试
```
/place structure mythic_age:egyptian_temple_small ~ ~ ~
```

#### 7.2 检查生成效果
- 结构是否完整生成
- 方块是否正确放置
- 朝向是否符合预期
- 内部空间是否合理

#### 7.3 调试常见问题
- **结构不生成**：检查NBT文件路径和命名
- **方块缺失**：确认所有使用的方块都已注册
- **位置偏移**：调整结构方块的相对位置设置

## 📋 建筑规范清单

### 各文明神庙要求

#### 🏜️ 埃及神庙
- **风格**：金字塔式阶梯结构
- **主色调**：沙漠黄、金色装饰
- **特征元素**：方尖碑、狮身人面像、象形文字浮雕
- **尺寸范围**：11×7×9 到 21×11×13

#### 🏮 中国神庙  
- **风格**：传统汉式建筑
- **主色调**：朱红色、金色装饰
- **特征元素**：飞檐、斗拱、龙雕装饰
- **尺寸范围**：13×8×10 到 23×12×14

#### 🌊 亚特兰蒂斯神庙
- **风格**：海洋科技融合
- **主色调**：深蓝、青绿、白色
- **特征元素**：水晶装饰、几何图案、水道系统
- **尺寸范围**：15×6×12 到 25×10×16

#### ⚔️ 北欧神庙
- **风格**：维京长屋风格
- **主色调**：深灰、棕色、银色
- **特征元素**：尖顶、木石结合、符文装饰
- **尺寸范围**：9×6×8 到 17×10×12

## 🔄 批量生成工作流

### 高效工作方案
1. **第一阶段**：搭建4个小型神庙（每个文明1个）
2. **第二阶段**：基于小型版本扩建中型神庙
3. **第三阶段**：创建大型豪华版本神庙
4. **第四阶段**：优化细节和装饰元素

### 时间估算
- 小型神庙：每个约30-45分钟
- 中型神庙：每个约60-90分钟  
- 大型神庙：每个约120-180分钟
- **总计**：约12-20小时完成所有神庙结构

## 💡 高级技巧

### 优化建议
1. **使用WorldEdit**：大幅提升建筑效率
2. **复制变形**：基于一个基础结构创建变体
3. **分层保存**：将复杂结构分层保存便于修改
4. **版本管理**：保留多个版本便于对比选择

### 质量保证
- **对称性检查**：确保建筑左右对称
- **比例协调**：各部分比例要协调美观
- **细节丰富**：添加足够的装饰细节
- **功能性**：确保内部空间实用合理

---

📝 **注意事项**：
- 建筑时要考虑玩家的游戏体验
- 确保所有使用的方块都在Mod中已定义
- 定期测试NBT文件的生成效果
- 保持统一的建筑风格和质量标准