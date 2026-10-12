# MythicAgeMod 1.0 NBT 状态持久化目标

生成日期：2026-06-11

本文档定义 1.0 需要持久化的状态范围。当前静态 NBT schema、目标表、source round-trip、restart contract、运行中实体探针和真实 MC 重启周期已经形成一套发布验收证据；本文用于冻结长期状态、runtime-only 决策和后续升级触发条件。

## 1. 原则

- 只有跨存档、跨区块卸载、死亡/重进、服务器重启后仍需要保留的状态才写入 NBT 或数据组件。
- 短期动画 tick、纯客户端视觉插值、可从当前世界状态重新计算的值，不写入长期 NBT。
- 每个字段必须有写入、读取、默认值、版本迁移和测试样例。
- 对于 constructor-final 或由实体类型决定的值，可以写入用于审计，但必须明确是否 read-only 或 write-only by design。

## 2. 当前覆盖口径

当前静态 schema 已覆盖 14 个 owner：

- `WalkingTreeEntity`
- `CustomTempleGuardianEntity`
- `MeteorEntity`
- `VillagerAIEnhancementMixin`
- `AnimalLureTowerBlockEntity`
- `AbundanceAltarCoreBlockEntity`
- `RelicPedestalBlockEntity`
- `StatueBaseBlockEntity`
- `EgyptianStatuePedestalBlockEntity`
- `MythicAgeWorldState`
- `LightningStormManager`
- `EarthquakeManager`
- `GodPowerWeatherManager`
- `GodPowerCertificateManager`

当前 1.0 目标表结论：

- `covered`：21 项长期状态，必须继续保持 schema、probe 和真实重启证据为 green。
- `runtime_only`：48 项短期状态或可重建状态，不写入长期 NBT；只有当它们变成跨重启真相、经济防刷、召唤归属、长期 boss 阶段或玩家可变状态时才升级。
- `required`：0 项。
- `missing`：0 项。

这意味着 NBT 侧当前不是“缺失实现”，而是已经完成 1.0 的状态所有权分类；后续新增玩法必须先更新本目标表，再实现 schema、round-trip/restart probe 和默认值策略。

## 3. Schema 目标状态定义

| 状态 | 含义 |
| --- | --- |
| `required` | 1.0 必须持久化，并加入静态 schema、运行时 round-trip 或 world persistent state 验证。 |
| `runtime_only` | 不写入长期 NBT；只在当前 tick、当前施法、当前动画或可由实体/方块/世界状态重建的索引中存在。 |
| `covered` | 当前已有 schema 或等价验证。 |
| `missing` | 目标已明确但当前既未实现也未验证；发布前不能保留。 |

## 4. 生物状态目标

### 4.1 所有神话生物通用

| 字段 | 类型 | 状态 | 说明 |
| --- | --- | --- | --- |
| `MythicAge_Variant` | string | `runtime_only` | 1.0 多数生物没有可切换皮肤/材质变体；行走树等真正有变体的实体单独覆盖。 |
| `MythicAge_SpecialCooldown` | int | `runtime_only` | 当前五文明基类的 `specialCooldownTicks` 是短期技能冷却，不要求跨卸载保留。 |
| `MythicAge_AbilityPhase` | string | `runtime_only` | 现有生物技能为瞬时或短 tick 动画；若以后做长阶段 boss 技能再新增 owner 级字段。 |
| `MythicAge_LastAbilityGameTime` | long | `runtime_only` | 生物普攻/技能节奏不作为跨重启防刷依据；经济/交易防刷使用独立持久状态。 |
| `MythicAge_Summoner` | uuid | `covered` | 1.0 自定义长期召唤单位由 `WalkingTreeEntity.walkingTreeSummoner` 覆盖；原版可驯服召唤沿用 vanilla owner NBT；非长期原版临时召唤按 `runtime_only` 处理。 |
| `MythicAge_SourceCivilization` | string | `runtime_only` | 1.0 生物文明通常由实体类型推导；跨文明召唤掉落由召唤/经济系统记录来源。 |

### 4.2 Greek

| 实体 | 字段 | 类型 | 状态 | 说明 |
| --- | --- | --- | --- | --- |
| `minotaur` | `ChargeCooldown` | int | `runtime_only` | 冲锋冷却由 goal 本地 tick 控制，卸载后重置可接受。 |
| `cyclops` | `StoneThrowCooldown` | int | `runtime_only` | 投石冷却不承担经济、防刷或召唤归属。 |
| `centaur` | `RangedVolleyCooldown` | int | `runtime_only` | 连射节奏只影响当前战斗。 |
| `hydra` | `ActiveHeadCount` | int | `runtime_only` | 当前 1.0 视觉要求多头清晰；断头/再生若升级为真实玩法再转 required。 |
| `hydra` | `RegenerationTicks` | int | `runtime_only` | 当前不作为跨存档生命阶段。 |
| `medusa` | `PetrifiedTargets` | list/compound | `runtime_only` | 石化持续交给原版/自定义状态效果的实体 NBT，不在美杜莎实体保存目标列表。 |
| `nemean_lion` | `PounceCooldown` | int | `runtime_only` | 扑击冷却只影响当前战斗。 |
| `manticore` | `VenomCharges` | int | `runtime_only` | 毒刺节奏只影响当前战斗；若做有限弹药再新增 item/entity 字段。 |
| `chimera` | `FlameBreathCooldown` | int | `runtime_only` | 喷火冷却只影响当前战斗。 |
| `colossus` | `CoreHeat` | int | `runtime_only` | 青铜核心蓄力是动画/技能节奏，不作为长期状态。 |
| `pegasus` | `IsTamed` | boolean | `runtime_only` | 1.0 目标是可骑乘飞行，不冻结驯服系统。 |
| `pegasus` | `Owner` | uuid | `runtime_only` | 当前骑乘无需 owner；若以后加入驯服/召唤天马再新增 required 字段。 |
| `pegasus` | `FlightEnergy` | int | `runtime_only` | 当前骑乘飞行无耐力条；飞行能力由 movement 实时计算。 |

### 4.3 Egyptian

| 实体 | 字段 | 类型 | 状态 | 说明 |
| --- | --- | --- | --- | --- |
| `anubis_warrior` | `JudgementMarkTicks` | int | `runtime_only` | 审判标记作为当前战斗状态或状态效果处理。 |
| `scarab_guardian` | `Burrowed` | boolean | `runtime_only` | 潜地/甲壳防御若成为可跨卸载姿态，再升级为实体字段。 |
| `mummy_soldier` | `CurseStacks` | int | `runtime_only` | 诅咒叠层归目标状态效果持久化，不保存在施法者。 |
| `sphinx_sentinel` | `RiddleCooldown` | int | `runtime_only` | 谜题凝视或控制冷却只影响当前战斗。 |
| `sun_falcon` | `SolarCharge` | int | `runtime_only` | 太阳俯冲蓄力是动作阶段。 |

### 4.4 Nordic

| 实体 | 字段 | 类型 | 状态 | 说明 |
| --- | --- | --- | --- | --- |
| `frost_giant` | `FrostQuakeCooldown` | int | `runtime_only` | 冰震冷却只影响当前战斗。 |
| `draugr_warrior` | `ShieldBashCooldown` | int | `runtime_only` | 盾击冷却只影响当前战斗。 |
| `valkyrie` | `SpearDiveCooldown` | int | `runtime_only` | 飞矛/俯冲冷却只影响当前战斗。 |
| `fenrir_wolf` | `DreadHowlCooldown` | int | `runtime_only` | 恐惧嚎叫冷却只影响当前战斗。 |
| `dwarf_berserker` | `BerserkTicks` | int | `runtime_only` | 狂暴状态如作用于自身，可由状态效果或短 tick 动画处理。 |
| `walking_tree` | `walkingTreeVariant` | string | `covered` | 已覆盖。 |
| `walking_tree` | `walkingTreeLifetimeTicks` | int | `covered` | 已覆盖。 |
| `walking_tree` | `walkingTreeSummoner` | uuid | `covered` | 已覆盖。 |
| `walking_tree` | `guardPosX/Y/Z` | int | `covered` | 已覆盖。 |

### 4.5 Chinese

| 实体 | 字段 | 类型 | 状态 | 说明 |
| --- | --- | --- | --- | --- |
| `azure_dragon` | `DragonBreathCooldown` | int | `runtime_only` | 龙息冷却只影响当前战斗。 |
| `azure_dragon` | `FlightPhase` | string | `runtime_only` | 飞行 AI 阶段由当前位置和目标实时计算；长期迁徙/巢穴系统另建字段。 |
| `qilin_guardian` | `BlessingCooldown` | int | `runtime_only` | 瑞兽祝福或踏击冷却只影响当前战斗。 |
| `jiangshi` | `TalismanBindTicks` | int | `runtime_only` | 符箓束缚归状态效果或当前战斗 tick。 |
| `nine_tailed_fox` | `GlamourCooldown` | int | `runtime_only` | 魅惑技能冷却只影响当前战斗。 |
| `stone_lion_guardian` | `StoneSuppressionCooldown` | int | `runtime_only` | 石爪压制冷却只影响当前战斗。 |

### 4.6 Atlantean

| 实体 | 字段 | 类型 | 状态 | 说明 |
| --- | --- | --- | --- | --- |
| `crystal_leviathan` | `PrismSurgeCooldown` | int | `runtime_only` | 棱镜潮涌冷却只影响当前战斗。 |
| `orichalcum_golem` | `CorePulseCharge` | int | `runtime_only` | 山铜核心蓄力是技能阶段。 |
| `siren_priestess` | `HymnTicks` | int | `runtime_only` | 海妖歌声持续交给当前施法 tick 或目标状态效果。 |
| `tidal_guardian` | `ClawBreakerCooldown` | int | `runtime_only` | 破甲钳击冷却只影响当前战斗。 |
| `abyssal_ray` | `AbyssalStingCooldown` | int | `runtime_only` | 深渊尾刺冷却只影响当前战斗。 |

## 5. 神力持续状态目标

| 神力/系统 | 字段 | 类型 | 状态 | 说明 |
| --- | --- | --- | --- | --- |
| Lightning Storm | `dimension`, `center`, `remainingTicks`, `lightningTimer` | world persistent state | `covered` | 已接入 `MythicAgeWorldState`，保存 active storm 列表、维度、中心、已过/剩余 tick 和闪电计时器。 |
| Earthquake | `dimension`, `center`, `remainingTicks`, `intervalTimer`, `yaw` | world persistent state | `covered` | 已接入 `MythicAgeWorldState`，保存 active earthquake 列表、维度、震中、已过/剩余 tick、间隔计时器和方向。 |
| Ceasefire | `affectedEntities`, `remainingTicks` | status effect/entity NBT | `runtime_only` | 当前通过原版缓慢/虚弱和清 target 生效，长期状态由目标状态效果保存。 |
| Bronze | `affectedEntities`, `remainingTicks` | status effect/entity NBT | `runtime_only` | 当前使用原版抗性/力量状态效果，不另建神力状态。 |
| Underworld Passage | `portalA`, `portalB`, `expiresAt` | world persistent state | `runtime_only` | 当前实现是短距瞬移和粒子，没有持久传送门；若恢复双向门玩法再转 required。 |
| Walking Woods | `summoner`, `treeIds`, `expiresAt` | entity NBT + runtime index | `covered` | 行走树自身 owner/lifetime/guardPos 已覆盖；manager 的 treeIds 可从实体重建。 |
| Fimbulwinter | `dimension`, `center`, `radius`, `remainingTicks` | world persistent state | `covered` | 已接入 `GodPowerWeatherManager`，保存持续天气神力的维度、中心、范围、持续时间和剩余 tick。 |
| Crystal Barrier | `affectedEntities`, `remainingTicks` | status effect/entity NBT | `runtime_only` | 当前实现只给队友吸收/抗性/水下呼吸并播放粒子，没有临时屏障方块需要恢复或清理。 |
| Abyssal Rift | `center`, `remainingTicks`, `owner` | status/world state | `runtime_only` | 当前实现是瞬时伤害、拉拽和状态效果，没有持续裂隙实体。 |
| Oceanic Wrath | `dimension`, `center`, `radius`, `remainingTicks` | world persistent state | `covered` | 已接入 `GodPowerWeatherManager`，保存亚特兰蒂斯终极天气神力的持续状态。 |

## 6. 证书、交易和经济状态

| Owner | 字段 | 类型 | 状态 | 说明 |
| --- | --- | --- | --- | --- |
| `StatueBaseBlockEntity` | `StoredCertificates` | int | `covered` | 已覆盖。 |
| `StatueBaseBlockEntity` | `LastUpdateTime` | long | `covered` | 已覆盖。 |
| `CustomTempleGuardianEntity` | `tradeCooldowns` | compound | `covered` | 已覆盖。 |
| `CustomTempleGuardianEntity` | `tradeOffers` | compound | `covered` | 已覆盖。 |
| `GodPowerCertificateItem` | `MythologyType` | item id/component | `runtime_only` | 当前由不同 item 类型表达文明，不需要额外组件。 |
| `GodPowerCertificateManager` | `CivilizationProductionState` | world persistent state | `covered` | 已接入 `MythicAgeWorldState`，按文明、维度、来源位置记录最近产出 tick 和数量；埃及方块实体仍以自身 `productionTick` 为真相。 |
| `GodPowerCertificateManager` | `EgyptianStatueRuntimeIndex` | blockpos map | `runtime_only` | 当前 manager map 应从 `EgyptianStatuePedestalBlockEntity` 重建，不能成为唯一真相。 |
| 神力书 item | `UnlockedBy`, `Charges`, `BoundCivilization` | component | `runtime_only` | 1.0 当前以独立 spellbook item 表达能力；不冻结绑定/次数/升级系统。 |
| 圣物 item | `RelicPower`, `BoundOwner`, `DurabilityState` | component | `runtime_only` | 1.0 当前圣物能力由 item 类型/神殿放置状态表达；有耐久/绑定后再新增 required。 |

## 7. 方块实体和结构状态

| Owner | 字段 | 类型 | 状态 | 说明 |
| --- | --- | --- | --- | --- |
| `RelicPedestalBlockEntity` | `RelicItem` | compound | `covered` | 已覆盖。 |
| `RelicPedestalBlockEntity` | `IsActiveTemple` | boolean | `covered` | 已覆盖。 |
| `EgyptianStatuePedestalBlockEntity` | `hasStatue`, `productionTick`, `statueType` | mixed | `covered` | 已覆盖。 |
| `AbundanceAltarCoreBlockEntity` | `NextGenerationTime` | long | `covered` | 已覆盖。 |
| 神殿结构功能点 | `GuardianSpawn`, `RelicPedestal`, `StatueBase`, `Entrance`, `Bounds` | marker/metadata | `covered` | `structure_validation_pipeline.py` 已从 canonical NBT 推导并验证 15/15 神殿的功能点位。 |
| canonical NBT | `size`, `palette`, `blocks`, `entities`, `DataVersion` | nbt | `covered` | 结构解析已覆盖基础合法性。 |

## 8. 验证与回归计划

### P0

- 保持 `entity_validation_pipeline.py nbt`、`nbt_target_validation_pipeline.py audit`、`nbt_roundtrip_probe_pipeline.py audit`、`nbt_restart_contract_probe.py audit` 和 `nbt_release_acceptance_package.py audit` 全部通过。
- `covered` 字段必须继续纳入 schema owner、source round-trip 或 restart contract。
- `required`、`missing`、`candidate`、`todo`、`tbd`、`undecided` 发布前必须为 0。
- 真实 MC 重启证据必须保留 `run/visual_probe/nbt_restart_prepare_report.json` 与 `run/visual_probe/nbt_restart_verify_report.json`，且 prepare/verify 来自不同 JVM process id。
- `MythicAgeWorldState` 继续作为长期 world persistent state 的统一 owner，覆盖持续神力和证书产出窗口。

### P1

- 当前已补 `/mythicage_validate nbt_restart_prepare` 和 `/mythicage_validate nbt_restart_verify`：
  - `prepare` 在固定探针区域写入自定义神殿守护者、神像基座、圣物基座、埃及神像基座、丰饶祭坛核心，并强制保存。
  - 重启同一世界后执行 `verify`，读取同一 UUID/坐标并生成 `run/visual_probe/nbt_restart_verify_report.json`。
  - `tools/mc_visual_probe_mcp.py` 已封装 `run_runtime_nbt_restart_prepare`、`run_runtime_nbt_restart_verify`；只有 verify 报告存在且 `failed=0`，1.0 gap audit 才能关闭真实重启 blocker。
- 若后续将五文明生物冷却、天马驯服/耐力、神力书 item data、圣物绑定、证书 item 来源等从 `runtime_only` 升级为长期真相，必须先改第 4-7 节目标表，再补代码和 probe。

### P2

- 为长期 world persistent state 建立版本号和迁移策略。
- 对旧存档缺字段场景做默认值测试。
- 发布前导出 NBT schema changelog。

## 9. 当前决策

- 不能把“多数生物 vanilla_only”直接当作 1.0 合格。它只能说明当前没有自定义 NBT 要求。
- 生物普通技能冷却统一不持久化；召唤归属、长期神力、证书经济和结构功能点才是 1.0 NBT 主战场。
- 结构 NBT 已可读，不代表结构状态完整；功能点位必须进入后续 gate。
