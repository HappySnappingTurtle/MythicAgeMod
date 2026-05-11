# 陨石法术完整实现文档

## 概述
本文档记录了MythicAge模组中陨石法术的完整实现，包括音效系统、粒子效果、语言本地化等所有相关功能。

## 实现的功能

### 1. 音效系统
- **音效注册**: 在`ModSounds.java`中注册了三个陨石相关音效事件
  - `ENTITY_METEOR_FALLING`: 陨石坠落音效
  - `ENTITY_METEOR_WARNING`: 陨石警告音效  
  - `ENTITY_METEOR_IMPACT`: 陨石撞击音效

- **音效配置**: 在`sounds.json`中配置了音效文件路径和字幕
  - 每个音效都有对应的字幕定义
  - 支持多个音效变体（falling和impact各有2个变体）

- **音效文件**: 位于`/sounds/entity/meteor/`目录
  - `falling1.ogg`, `falling2.ogg`: 坠落音效
  - `warning1.ogg`: 警告音效
  - `impact1.ogg`, `impact2.ogg`: 撞击音效

### 2. 实体实现增强
在`MeteorEntity.java`中实现了完整的音效播放逻辑：

- **构造函数**: 播放坠落音效
- **tick方法**: 
  - 距离目标30格时播放警告音效
  - 距离目标20格时播放持续警告
  - 增强的粒子效果（火焰、熔岩、龙息、烟花粒子）
- **explode方法**: 播放撞击音效和额外爆炸音效
- **NBT序列化**: 保存音效播放状态

### 3. 语言本地化
支持三种语言的字幕：

#### 中文 (zh_cn.json)
```json
"subtitles.mythic_age.entity.meteor.falling": "陨石坠落",
"subtitles.mythic_age.entity.meteor.warning": "陨石警告", 
"subtitles.mythic_age.entity.meteor.impact": "陨石撞击"
```

#### 英文 (en_us.json)
```json
"subtitles.mythic_age.entity.meteor.falling": "Meteor falling",
"subtitles.mythic_age.entity.meteor.warning": "Meteor warning",
"subtitles.mythic_age.entity.meteor.impact": "Meteor impact"
```

#### 日文 (ja_jp.json)
```json
"subtitles.mythic_age.entity.meteor.falling": "隕石の落下",
"subtitles.mythic_age.entity.meteor.warning": "隕石の警告",
"subtitles.mythic_age.entity.meteor.impact": "隕石の衝撃"
```

### 4. 粒子效果纹理
- **火焰粒子**: `flame_particle.png` - 用于陨石尾迹效果
- **冲击波粒子**: `shockwave_particle.png` - 用于撞击冲击波效果

### 5. 视觉效果增强
- **坠落阶段**: 
  - 火焰和熔岩粒子尾迹
  - 龙息粒子增加神秘感
  - 烟花粒子增加华丽效果
- **警告阶段**: 
  - 目标区域火焰粒子标记
  - 音效警告系统
- **撞击阶段**:
  - 大范围爆炸粒子
  - 冲击波环形效果
  - 增强的环境破坏

## 文件结构

```
src/main/resources/assets/mythic_age/
├── sounds/
│   └── entity/
│       └── meteor/
│           ├── falling1.ogg
│           ├── falling2.ogg
│           ├── warning1.ogg
│           ├── impact1.ogg
│           └── impact2.ogg
├── textures/
│   └── particle/
│       ├── flame_particle.png
│       └── shockwave_particle.png
├── lang/
│   ├── zh_cn.json (中文字幕)
│   ├── en_us.json (英文字幕)
│   └── ja_jp.json (日文字幕)
└── sounds.json (音效配置)

src/main/java/com/example/mythic_age/
├── sound/
│   └── ModSounds.java (音效注册)
└── entity/
    └── MeteorEntity.java (实体实现)
```

## 技术特性

### 音效播放优化
- 服务端音效播放避免客户端重复
- 音效播放状态持久化
- 距离感应的音效触发

### 粒子效果优化
- 服务端粒子生成频率控制
- 多层次粒子效果组合
- 性能优化的粒子密度

### 伤害和环境效果
- 增强的爆炸伤害 (35点基础伤害)
- 状态效果: 凋零III + 缓慢III
- 更大的环境破坏范围
- 智能的岩浆和火焰生成

## 使用说明
1. 使用陨石法术书施放法术
2. 陨石将从天空坠落并播放相应音效
3. 接近目标时会有警告音效和粒子标记
4. 撞击时产生大范围爆炸效果和音效

## 开发注意事项
- 所有音效文件必须为OGG格式
- 粒子纹理建议使用PNG格式
- 音效播放状态需要在NBT中持久化
- 多语言支持需要同步更新所有语言文件

## 版本信息
- 实现日期: 2024年7月24日
- Minecraft版本: 1.20.1
- Forge版本: 47.2.0
- 模组版本: 1.0.0