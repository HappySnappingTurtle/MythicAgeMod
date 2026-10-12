# MythicAgeMod 1.0 五文明证书经济矩阵

生成日期：2026-06-12

本文档冻结 1.0 证书经济的当前事实、缺口和后续实现口径。它基于当前源码调查，不把“方法存在”写成“玩法已完成”。证书经济连接代码、神力、交易、UI、NBT、结构和玩法验收，是五文明从“资源存在”走向“基础可玩闭环”的核心系统。

当前证据入口：

- 证书物品：`src/main/java/com/example/mythic_age/item/GodPowerCertificateItem.java`
- 证书注册：`src/main/java/com/example/mythic_age/item/ModItems.java`
- 证书产出管理：`src/main/java/com/example/mythic_age/system/GodPowerCertificateManager.java`
- 神殿村民产出：`src/main/java/com/example/mythic_age/system/CertificateGenerationManager.java`
- 守护者交易：`src/main/java/com/example/mythic_age/entity/CustomTempleGuardianEntity.java`
- 原版村民式守护者交易：`src/main/java/com/example/mythic_age/entity/TempleGuardianEntity.java`
- 神像基座存储：`src/main/java/com/example/mythic_age/block/StatueBaseBlock.java`
- 神像基座 NBT：`src/main/java/com/example/mythic_age/block/entity/StatueBaseBlockEntity.java`
- 埃及神像基座：`src/main/java/com/example/mythic_age/block/EgyptianStatuePedestalBlock.java`
- 埃及神像基座 NBT：`src/main/java/com/example/mythic_age/block/entity/EgyptianStatuePedestalBlockEntity.java`
- 世界持久状态：`src/main/java/com/example/mythic_age/world/MythicAgeWorldState.java`
- 神力图鉴 UI：`src/main/java/com/example/mythic_age/client/gui/GodPowerGuideScreen.java`
- 语言项：`src/main/resources/assets/mythic_age/lang/zh_cn.json`、`src/main/resources/assets/mythic_age/lang/en_us.json`

## 1. 当前事实

已经具备：

- 五种证书物品均已注册：Greek、Egyptian、Chinese、Atlantean、Nordic。
- `GodPowerCertificateItem.MythologyType` 已定义五文明和 tooltip。
- `GodPowerGuideScreen` 已按五文明列出证书余额、证书来源和神力价格矩阵。
- `StatueBaseBlockEntity` 持久化 `StoredCertificates`、`LastUpdateTime`、`MythologyType`。
- `CustomTempleGuardianEntity` 和 `TempleGuardianEntity` 都能按文明创建对应证书并生成交易。
- `GodPowerCertificateManager` 已接入北欧击杀掉落、中国人口产出、埃及神像产出和 world persistent state。
- `MythicAgeWorldState` 已保存 `GodPowerCertificates`，用于文明产出防刷窗口。

仍不完整：

- `CustomTempleGuardianEntity` 的交易矩阵与 `GodPowerGuideScreen` 的五文明神力矩阵不一致。
- 埃及、中国、北欧在图鉴中各有 10 个神力，但守护者交易没有完整兑换这些神力。
- `StatueBlock` 仍是旧希腊兑换口径；非希腊显示 `coming_soon`，不能作为 1.0 五文明兑换入口。
- `CustomTempleGuardianEntity.generateCertificatesForNearbyVillagers()` 当前只允许 Greek 走神殿村民产出。
- Atlantean 的证书来源当前主要是 prismarine shard 与守护者交易；“海洋遗迹/探索”仍是设计口径，缺少专门代码入口和防刷状态。
- 中国人口产出方法存在，但需要确认实际调用链和村庄人口统计入口。
- 埃及同时存在 `GodPowerCertificateManager.tickEgyptianStatues()` 和 `EgyptianStatuePedestalBlockEntity.tick()` 两条神像产出路径，后续需要统一真相源，避免重复产出。

## 2. 1.0 证书经济完成定义

每个文明必须满足：

1. 证书来源明确：玩家知道从哪里、按什么规则、多久获得。
2. 证书产出可测试：有静态入口、运行时 probe 或 playtest case。
3. 证书存储持久化：基座、世界防刷窗口、交易冷却等长期状态能跨重启保存。
4. 证书消耗完整：所有 1.0 神力都有对应兑换入口，价格与图鉴一致。
5. UI 显示一致：图鉴、守护者交易、基座提示和 tooltip 的来源/价格不冲突。
6. 防刷明确：产出间隔、掉落概率、交易冷却和可重复获取规则可解释。

## 3. 五文明矩阵

| 文明 | 当前来源 | 当前消耗/兑换 | NBT/持久化 | 当前状态 | 1.0 缺口 |
| --- | --- | --- | --- | --- | --- |
| Greek | `CertificateGenerationManager.generateCertificatesForTemple()` 统计神殿附近 Greek 村民；`TempleGuardianEntity`/`CustomTempleGuardianEntity` 只对 Greek 自动触发该路径。 | `CustomTempleGuardianEntity` 提供 11 个 Greek 神力书和 9 个 Greek 生物蛋交易；`GodPowerGuideScreen` 列 12 个 Greek 神力。 | `StatueBaseBlockEntity.StoredCertificates`；`GodPowerCertificateManager.CivilizationProductionState` 防刷窗口。 | `needs_playtest` | 统一 Earthquake/Plenty 价格和图鉴价格；确认 Greek 12 神力是否全部可兑换；实测村民数量、基座存储、提取和交易消耗。 |
| Egyptian | `EgyptianStatuePedestalBlockEntity` 根据神像类型周期掉落 Egyptian 证书；`GodPowerCertificateManager.tickEgyptianStatues()` 也有管理器产出路径；守护者可用金锭换证书。 | `GodPowerGuideScreen` 列 10 个 Egyptian 神力；`CustomTempleGuardianEntity` 只提供埃及神殿生成书、金苹果和金锭换证书。 | `EgyptianStatuePedestalBlockEntity.hasStatue/productionTick/statueType`；world state 未作为埃及方块实体唯一真相。 | `needs_implementation` | 守护者交易必须补齐 10 个埃及神力书；统一神像产出真相源，避免 block entity tick 与 manager tick 双重产出；补神像产出防刷和 UI 说明。 |
| Chinese | `GodPowerCertificateManager.handleChinesePopulationGeneration()` 根据人口产出 Chinese 证书；守护者可用纸换证书。 | `GodPowerGuideScreen` 列 10 个 Chinese 神力；`CustomTempleGuardianEntity` 只提供中国神殿生成书、Plenty、食物和村民蛋。 | `GodPowerCertificateManager.CivilizationProductionState` 记录 `chinese_population` 产出窗口。 | `needs_implementation` | 确认人口产出调用链；守护者交易必须补齐 10 个中国神力书；Plenty 不应替代 Chinese 的 Abundance/Five Thunder 等神力矩阵。 |
| Nordic | `ModCombatEvents` 调用 `GodPowerCertificateManager.handleNordicKill()`；敌对怪物和玩家击杀按概率/数量掉落 Nordic 证书。 | `GodPowerGuideScreen` 列 10 个 Nordic 神力；`CustomTempleGuardianEntity` 只提供北欧神殿生成书和武器/护甲兑换。 | 击杀掉落本身不持久化；守护者交易冷却持久化；长期防刷主要靠掉落概率。 | `needs_implementation` | 守护者交易必须补齐 10 个北欧神力书；增加战斗掉落测试矩阵；确认 boss、PvP、普通怪掉落与平衡。 |
| Atlantean | 当前主要是守护者交易：`16 prismarine_shard -> 1 Atlantean certificate`；语言项宣称海洋遗迹/神殿交易。 | `CustomTempleGuardianEntity` 提供 10 个 Atlantean 神力书，与图鉴矩阵基本一致。 | 交易冷却持久化；没有专门海洋探索防刷状态。 | `needs_design_freeze` | 冻结“海洋遗迹/水下探索”真实来源，或明确 1.0 只采用海晶碎片交易；若新增探索来源，必须补 world state 防刷和 UI 文案。 |

## 4. 价格矩阵

| 文明 | 图鉴神力数量 | 守护者完整度 | 主要不一致 |
| --- | --- | --- | --- |
| Greek | 12 | 部分完整 | 图鉴中 `Plenty` 5 证书，`CustomTempleGuardianEntity` 没有 Greek Plenty；`Earthquake` 图鉴为 5，守护者为 6。 |
| Egyptian | 10 | 不完整 | 图鉴列 Ra Radiance、Desert Storm、Eye of Horus、Sun Rain、Prosperity、Anubis Judgment、Locust Swarm、Tornado、Isis Resurrection、Meteor；守护者未提供这些 10 项兑换。 |
| Chinese | 10 | 不完整 | 图鉴列 Rain Prayer、Earth God Blessing、Five Thunder、Enlightenment、Abundance、Crane Blessing、Flood、Call To Arms、Taiji Shield、Recreation；守护者未提供这些 10 项兑换，只提供 `Plenty`。 |
| Nordic | 10 | 不完整 | 图鉴列 Fenrir Howl、Odin Eye、Thor Wrath、Great Hunt、Dwarven Mine、Valhalla Call、Frost、Walking Woods、Ragnarok、Fimbulwinter；守护者未提供这些 10 项兑换。 |
| Atlantean | 10 | 基本完整 | 10 项神力与图鉴一致；仍需玩法、价格和平衡验收。 |

## 5. NBT 与防刷矩阵

| Owner | 字段 | 当前状态 | 1.0 要求 |
| --- | --- | --- | --- |
| `StatueBaseBlockEntity` | `StoredCertificates`、`LastUpdateTime`、`MythologyType` | 已写入/读取 NBT | 继续纳入真实重启 verify。 |
| `CustomTempleGuardianEntity` | `mythologyType`、`certificateGenerationTimer`、`templePos`、`tradeOffers`、`tradeCooldowns` | 已写入/读取 NBT | 交易矩阵修复后必须验证重启后交易和冷却仍一致。 |
| `EgyptianStatuePedestalBlockEntity` | `hasStatue`、`productionTick`、`statueType` | 已写入/读取 NBT | 明确它是埃及神像产出的唯一真相，或移除 manager 重复 tick 路径。 |
| `GodPowerCertificateManager` | `CivilizationProductionState` | 已进入 `MythicAgeWorldState` | 覆盖 Greek temple_villager、Chinese population、未来 Atlantean exploration 等防刷窗口。 |
| `ModCombatEvents`/Nordic kills | 击杀事件和掉落概率 | 无长期状态 | 1.0 可接受，但必须有掉落测试和平衡验收。 |

## 6. UI 与文案要求

必须保持一致：

- `GodPowerGuideScreen` 的神力价格必须等于守护者交易实际价格。
- 证书来源文案必须等于实际代码入口：
  - Greek：神殿附近同文明村民 + 完整神像基座。
  - Egyptian：埃及神像基座产出 + 金锭/仪式交易。
  - Chinese：村庄人口产出 + 纸张交易，前提是人口产出调用链确认。
  - Nordic：击杀敌对怪物或玩家。
  - Atlantean：若 1.0 不实现海洋遗迹防刷，则文案应写成海晶碎片/海洋资源交易。
- `StatueBaseBlock` 的提示和 `GodPowerCertificateItem` tooltip 不能继续使用泛化来源来掩盖文明差异。

## 7. 开发任务

### P0：证书消耗闭环

1. 为 `CustomTempleGuardianEntity` 补齐 Egyptian/Chinese/Nordic 的 10 神力书兑换。
2. 统一 `GodPowerGuideScreen` 价格和守护者交易价格。
3. 明确 `StatueBlock` 是否保留；若保留，必须改为五文明统一兑换入口，否则标记为 legacy display-only。

### P1：证书来源闭环

1. 确认 `handleChinesePopulationGeneration()` 的实际调用链；没有调用链则补村庄 tick/事件入口或调整 1.0 设计。
2. 统一埃及神像产出路径，避免重复产出。
3. 冻结 Atlantean 证书来源：海晶碎片交易，或新增海洋遗迹探索防刷系统。
4. 给 Nordic 击杀掉落增加静态/运行时验证用例。

### P1：NBT 与验证

1. 扩展 NBT release package，把证书经济列成独立小节。
2. 增加静态检查：图鉴神力价格 vs 守护者交易价格。
3. 在允许启动游戏后执行真实流程：
   - 产出证书。
   - 存入或获得证书。
   - 兑换神力书。
   - 保存退出。
   - 重启 verify 证书存储、交易冷却和世界防刷窗口。

## 8. 验收标准

1. `tools/certificate_economy_validation_pipeline.py audit` 通过。
2. 五文明守护者交易和神力图鉴价格完全一致。
3. 所有 52 个 1.0 神力都有证书消耗入口或明确不走证书经济的理由。
4. 证书产出和消耗有 UI 提示、失败提示和音效反馈。
5. NBT release acceptance 中真实重启 verify 覆盖证书存储、交易冷却和文明产出防刷状态。
