# 神话时代 Mod - 国际化 (i18n) 指南

## 概述

本文档说明了如何为神话时代 Mod 添加和维护多语言支持。目前支持的语言包括：

- 🇺🇸 **英语 (en_us)** - 默认语言
- 🇨🇳 **简体中文 (zh_cn)** - 完整翻译
- 🇯🇵 **日语 (ja_jp)** - 基础翻译

## 文件结构

语言文件位于：`src/main/resources/assets/mythic_age/lang/`

```
lang/
├── en_us.json    # 英语（美国）
├── zh_cn.json    # 简体中文
└── ja_jp.json    # 日语
```

## 翻译键分类

### 1. 物品名称和描述
```json
{
  "item.mythic_age.spellbook_lightning_storm": "闪电风暴法术书",
  "item.mythic_age.relic_ankh_of_ra": "拉神安卡",
  "item.mythic_age.relic_ankh_of_ra.tooltip": "生命的象征，据说能赋予坚韧不拔的力量。"
}
```

### 2. 法术书系统
```json
{
  "spell.lightning_storm": "闪电风暴",
  "mythic_age.spellbook.cooldown": "冷却时间：%s秒",
  "mythic_age.spellbook.type.offensive": "攻击法术",
  "mythic_age.spellbook.cast_success": "成功施放了%s！",
  "mythic_age.spellbook.cast_failed": "法术施放失败！"
}
```

### 3. 圣物系统
```json
{
  "mythic_age.relic.ankh_of_ra.held_effect": "手持效果：再生 I",
  "mythic_age.relic.ankh_of_ra.temple_effect": "神殿效果：为附近的玩家提供持续再生",
  "mythic_age.relic.golden_fleece.passive_effect": "被动效果：每10秒获得1点经验"
}
```

### 4. 神殿系统
```json
{
  "mythic_age.temple.activated": "神殿已激活！圣物的力量开始显现。",
  "mythic_age.temple.deactivated": "神殿已失效。圣物的力量消散了。",
  "mythic_age.temple.structure_incomplete": "神殿结构不完整，无法激活圣物的力量。"
}
```

### 5. 用户界面
```json
{
  "itemgroup.mythic_age": "神话时代",
  "block.mythic_age.relic_pedestal": "圣物基座"
}
```

## 添加新语言支持

### 步骤 1：创建语言文件
在 `src/main/resources/assets/mythic_age/lang/` 目录下创建新的语言文件，文件名格式为 `语言代码_地区代码.json`。

常见语言代码：
- `fr_fr` - 法语（法国）
- `de_de` - 德语（德国）
- `es_es` - 西班牙语（西班牙）
- `ru_ru` - 俄语（俄国）
- `ko_kr` - 韩语（韩国）
- `pt_br` - 葡萄牙语（巴西）

### 步骤 2：复制翻译键
从 `en_us.json` 复制所有翻译键到新的语言文件中。

### 步骤 3：翻译内容
翻译所有值，保持键名不变。注意：
- 保留格式化占位符（如 `%s`）
- 保持颜色代码和格式化标记
- 考虑文化差异和游戏术语

### 步骤 4：测试
在游戏中切换语言设置，验证翻译是否正确显示。

## 翻译指南

### 1. 神话主题术语
保持神话色彩和史诗感：
- **英语**: "Divine Blessing" → **中文**: "神明祝福"
- **英语**: "Temple Aura" → **中文**: "神殿光环"
- **英语**: "Relic Power" → **中文**: "圣物之力"

### 2. 游戏机制术语
使用玩家熟悉的游戏术语：
- **英语**: "Cooldown" → **中文**: "冷却时间"
- **英语**: "Passive Effect" → **中文**: "被动效果"
- **英语**: "Temple Effect" → **中文**: "神殿效果"

### 3. 状态效果
使用 Minecraft 官方翻译：
- **英语**: "Regeneration" → **中文**: "再生"
- **英语**: "Strength" → **中文**: "力量"
- **英语**: "Haste" → **中文**: "急迫"

### 4. 格式化占位符
保留所有格式化占位符：
```json
{
  "mythic_age.spellbook.cast_success": "Successfully cast %s!",
  "mythic_age.spellbook.cast_success": "成功施放了%s！"
}
```

## 维护和更新

### 添加新翻译键时：
1. 首先在 `en_us.json` 中添加英文版本
2. 在所有其他语言文件中添加对应的翻译键
3. 如果暂时没有翻译，可以先使用英文作为占位符
4. 在代码中使用 `Text.translatable("key")` 而不是硬编码文本

### 翻译质量检查：
- 确保所有翻译键都存在于所有语言文件中
- 检查格式化占位符是否正确保留
- 验证游戏中的显示效果
- 考虑文本长度对UI的影响

## 代码中的国际化实践

### 正确的做法：
```java
// 使用翻译键
player.sendMessage(Text.translatable("mythic_age.temple.activated"), false);

// 带参数的翻译
user.sendMessage(Text.translatable("item.mythic_age.spellbook.cast_success", 
    Text.translatable("spell." + spellType.name().toLowerCase())), false);
```

### 错误的做法：
```java
// 硬编码英文文本
player.sendMessage(Text.literal("Temple activated!"), false);
```

## 贡献翻译

如果您想为 Mod 贡献翻译：

1. Fork 项目仓库
2. 创建或更新对应的语言文件
3. 确保翻译质量和一致性
4. 提交 Pull Request

### 翻译质量标准：
- ✅ 准确性：翻译意思正确
- ✅ 一致性：术语翻译统一
- ✅ 流畅性：符合目标语言习惯
- ✅ 完整性：所有键都有翻译
- ✅ 格式：保留所有格式化标记

## 未来扩展

计划支持的语言：
- 🇫🇷 法语 (fr_fr)
- 🇩🇪 德语 (de_de)
- 🇪🇸 西班牙语 (es_es)
- 🇷🇺 俄语 (ru_ru)
- 🇰🇷 韩语 (ko_kr)

欢迎社区贡献更多语言的翻译！