# MythicAgeMod 开发执行 Checklist

日期：2026-06-16
用途：goal 模式下逐项执行，每完成一项打勾并 build 验证。
前置文档：`MythicAgeMod_架构改进设计文档_2026-06-16.md`

---

## 阶段 1：中国文明证书系统修复与增强

### 1.1 BUG 修复：接入孤立的人口产出

- [x] **1.1.1** 在 `GodPowerCertificateManager` 中新增 `tickChinesePopulation(ServerWorld)` 公共方法，内部遍历中华神殿位置并调用已有的 `handleChinesePopulationGeneration()`
- [x] **1.1.2** 在 `MythicAgeMod.onInitialize()` 的 `ServerTickEvents.START_WORLD_TICK` 回调中追加 `GodPowerCertificateManager.getInstance().tickChinesePopulation(world)`
- [x] **1.1.3** `./gradlew build --offline` 通过
- [ ] **1.1.4** 验证：进入中华村庄，等待 2 分钟，观察是否有证书产出日志或消息（需实机验证）

### 1.2 新增：ChineseCertificateManager 核心类

- [x] **1.2.1** 创建 `com.example.mythic_age.system.ChineseCertificateManager` 单例类
- [x] **1.2.2** 实现 `tickVillagerWork(ServerWorld)` — 每 100 tick 扫描中华神殿范围内的中华村民，检测是否在职业站点附近工作
- [x] **1.2.3** 实现 `isWorking(VillagerEntity)` — 检查村民有职业 + 有 JOB_SITE 记忆 + 非 NONE/NITWIT
- [x] **1.2.4** 实现 `canGenerateWorkCertificate(VillagerEntity)` — 单村民 1200 tick 冷却检查
- [x] **1.2.5** 实现 `getTempleWorkCap(BlockPos)` — 每神殿 2400 tick 内劳作上限 8 证书
- [x] **1.2.6** 实现 `generateWorkCertificate(ServerWorld, BlockPos templePos, VillagerEntity, int amount)` — 存入最近中华神像基座，无基座则掉落
- [x] **1.2.7** 实现 `onVillagerTrade(PlayerEntity, VillagerEntity, TradeOffer)` — 交易产出入口
- [x] **1.2.8** 实现交易等级概率：1 级 30%/1 个，2-3 级 45%/1-2 个，4-5 级 60%/2-3 个
- [x] **1.2.9** 实现 `canGenerateTradeCertificate(PlayerEntity, VillagerEntity)` — 同一对 600 tick 冷却 + 每玩家 2400 tick 内上限 10 个
- [x] **1.2.10** 实现 `writePersistentState()` / `readPersistentState()` — 冷却和上限数据 NBT 持久化
- [x] **1.2.11** `./gradlew build --offline` 通过

### 1.3 新增：VillagerTradeMixin

- [x] **1.3.1** 创建 `com.example.mythic_age.mixin.VillagerTradeMixin`，目标 `VillagerEntity`
- [x] **1.3.2** 注入 `trade(TradeOffer)` 方法的 `@At("TAIL")`，在交易完成后调用 `ChineseCertificateManager.getInstance().onVillagerTrade()`
- [x] **1.3.3** 通过 `MerchantEntity.getCustomer()` 获取当前交易玩家
- [x] **1.3.4** 在 `mythic_age.mixins.json` 中添加 `VillagerTradeMixin`
- [x] **1.3.5** `./gradlew build --offline` 通过

### 1.4 Mixin 扩展：村民劳作冷却

- [x] **1.4.1** 在 `VillagerAIEnhancementMixin` 中新增字段 `mythicAge$workCertificateCooldown`
- [x] **1.4.2** 在已有 `onTick` 注入中添加冷却递减 `if (mythicAge$workCertificateCooldown > 0) mythicAge$workCertificateCooldown--`
- [x] **1.4.3** 在 `CivilizedVillager` 接口中新增 `mythicAge$getWorkCooldown()` / `mythicAge$setWorkCooldown(int)`
- [x] **1.4.4** 在 `writeCustomDataToNbt` / `readCustomDataFromNbt` 注入中追加冷却字段持久化
- [x] **1.4.5** `./gradlew build --offline` 通过

### 1.5 集成到主循环

- [x] **1.5.1** 在 `MythicAgeMod.onInitialize()` 的 tick 回调中追加 `ChineseCertificateManager.getInstance().tickVillagerWork(world)`
- [x] **1.5.2** 将 `ChineseCertificateManager` 的冷却/上限状态接入 `MythicAgeWorldState` 的 NBT 序列化
- [x] **1.5.3** `./gradlew build --offline` 通过

### 1.6 本地化

- [x] **1.6.1** 在 `en_us.json` / `zh_cn.json` 中添加劳作/交易证书消息的翻译键（ja_jp 后续补充）
- [x] **1.6.2** `./gradlew build --offline` 通过

### 1.7 集成验证

- [ ] **1.7.1** 人口产出：中华村庄有村民时，2 分钟后神像基座有证书
- [ ] **1.7.2** 劳作产出：中华农民在农田收割后概率出现劳作证书消息
- [ ] **1.7.3** 交易产出：与中华村民交易后概率获得证书到背包
- [ ] **1.7.4** 冷却生效：快速反复交易不会持续产出
- [ ] **1.7.5** 上限生效：大量交易后不再产出直到 2400 tick 重置
- [ ] **1.7.6** 重启持久化：存档退出再进入后，冷却和上限状态正确恢复
- [ ] **1.7.7** 非中华村民交易不触发证书产出

---

## 阶段 2：法术书系统重构

### 2.1 基础设施

- [x] **2.1.1** 创建 `item/spell/SpellCategory.java` 枚举
- [ ] **2.1.2** SpellType 枚举暂保留在 SpellbookItem 中（避免 breaking change），后续清理阶段再移出
- [x] **2.1.3** 创建 `item/spell/Spell.java` 接口
- [x] **2.1.4** 创建 `item/spell/AbstractSpell.java` 基类
- [x] **2.1.5** 创建 `item/spell/SpellRegistry.java`
- [x] **2.1.6** `./gradlew build --offline` 通过

### 2.2 法术迁移（按文明逐批，每批 build 验证）

#### Greek（12 个法术）

- [x] **2.2.1** 迁移 `castLightningStorm()` → `spell/greek/LightningStormSpell.java`
- [x] **2.2.2** 迁移 `castRestoration()` → `spell/greek/RestorationSpell.java`
- [x] **2.2.3** 迁移剩余 Greek 法术：UnderworldArmy, Pestilence, Curse, Earthquake, Ceasefire, Bronze, UnderworldPassage, LightningStormHera 完整迁移；Lure 和 Plenty 为代理占位（依赖复杂 helper 方法，后续完整迁移）
- [x] **2.2.4** 在 `SpellRegistry.init()` 中注册 12 个 Greek 法术
- [x] **2.2.5** SpellbookItem.castGodPower() 已改为 registry-first + fallback 分发模式
- [x] **2.2.6** `./gradlew build --offline` 通过
- [ ] **2.2.7** 验证：使用希腊法术书效果与迁移前一致（需实机验证）

#### Egyptian（10 个法术）

- [x] **2.2.8** 迁移 10 个 Egyptian 法术到 `spell/egyptian/` 包
- [x] **2.2.9** 注册 + build 通过

#### Nordic（10 个法术）

- [x] **2.2.10** 迁移 10 个 Nordic 法术到 `spell/nordic/` 包（WalkingWoods 完整迁移含内部类）
- [x] **2.2.11** 注册 + build 通过

#### Chinese（10 个法术）

- [x] **2.2.12** 迁移 10 个 Chinese 法术到 `spell/chinese/` 包
- [x] **2.2.13** 注册 + build 通过

#### Atlantean（10 个法术）

- [x] **2.2.14** 迁移 10 个 Atlantean 法术到 `spell/atlantean/` 包
- [x] **2.2.15** 注册 + build 通过

#### 神殿生成（4 个）

- [x] **2.2.16** 迁移 4 个 temple generation 法术到独立 Spell 类（GenerateEgyptianTempleSpell 等），在 SpellRegistry 中注册
- [x] **2.2.17** build 通过

### 2.3 清理 SpellbookItem

- [x] **2.3.1** 删除 `SpellbookItem` 中的 `castGodPower()` switch 和所有 `castXxx()` 方法（4334 行 → 170 行）
- [x] **2.3.2** `castGodPower()` 改为纯 `SpellRegistry.get(spellType).cast(...)` 调用，无 fallback
- [x] **2.3.3** `appendTooltip()` 保留，`getSpellCategory()` 改为 `SpellRegistry.get(spellType).getCategory().getId()`
- [x] **2.3.4** 替换 `getSpellCategory()` 为 `SpellRegistry.get(spellType).getCategory().getId()` 调用
- [x] **2.3.5** 确认 `SpellbookItem.java` 行数 170 < 300 ✓
- [x] **2.3.6** `./gradlew build --offline` 通过
- [ ] **2.3.7** 全量验证：每个文明至少测试 2 个法术效果正确（需实机验证）

### 2.4 SpellRegistry 初始化

- [x] **2.4.1** 已在 `MythicAgeMod.onInitialize()` 第 78 行调用 `SpellRegistry.init()`（在 `registerModItems()` 之前）
- [x] **2.4.2** `./gradlew build --offline` 通过

---

## 阶段 3：AI Goal 系统扩展

### 3.1 基础设施

- [x] **3.1.1** 创建 `entity/ai/goal/MythicSpecialAttackGoal.java` 抽象基类
- [x] **3.1.2** `./gradlew build --offline` 通过

### 3.2 P0 签名技能 Goal（4 个）

- [x] **3.2.1** 创建 `HydraPoisonSweepGoal`：从 `HydraEntity.tryPoisonSweep()` 提取逻辑
- [x] **3.2.2** 在 `HydraEntity.initGoals()` 注册优先级 1，删除 tick() 中的硬编码
- [x] **3.2.3** `./gradlew build --offline` 通过

- [x] **3.2.4** 创建 `MedusaStoneGazeGoal`：从 `MedusaEntity.tryPetrifyingGaze()` 提取
- [x] **3.2.5** 在 `MedusaEntity.initGoals()` 注册 + 删除 tick() 硬编码 + build 通过

- [x] **3.2.6** 创建 `CyclopsStoneThrowGoal`：从 `CyclopsEntity.tryStoneThrow()` 提取
- [x] **3.2.7** 在 `CyclopsEntity.initGoals()` 注册 + 删除 tick() 硬编码 + build 通过

- [ ] **3.2.8** PegasusFlightEvadeGoal：跳过，Pegasus 飞行是持续 tick 行为而非冷却技能，不适合 Goal 迁移。后续考虑 FlyWanderGoal 替代。
- [ ] **3.2.9** 跳过（依赖 3.2.8）

### 3.3 P1 签名技能 Goal（5 个）

- [x] **3.3.1** `ChimeraFlameBreathGoal`（火焰吐息 AoE）→ ChimeraEntity
- [x] **3.3.2** `NemeanLionPounceGoal`（猛扑 + 击退）→ NemeanLionEntity
- [x] **3.3.3** `FenrirHowlBuffGoal`（恐惧嚎叫 AoE + 自我加速）→ FenrirWolfEntity
- [x] **3.3.4** `AzureDragonBreathGoal`（龙息 + 浮空）→ AzureDragonEntity
- [ ] **3.3.5** CentaurMountedArcheryGoal 跳过（Centaur 使用 RangedAttackMob 接口，无 tick 硬编码技能）
- [x] **3.3.6** 每个 Goal build 通过
- [x] **3.3.7** 全量 build 通过

### 3.4 清理

- [x] **3.4.1** 所有 7 个迁移到 Goal 的技能已从 tick() 中移除（Hydra/Medusa/Cyclops/Chimera/NemeanLion/Fenrir/AzureDragon）
- [ ] **3.4.2** VillagerPilgrimageGoal 注册问题保留（需要在 Mixin 中为各文明村民添加）
- [x] **3.4.3** `./gradlew build --offline` 通过

---

## 阶段 4：粒子系统升级

### 4.1 新增粒子类型注册

- [x] **4.1.1** 在 `ModParticles.java` 中注册 10 种新粒子
- [x] **4.1.2** 在 `assets/mythic_age/particles/` 下创建 10 个粒子 JSON 配置
- [x] **4.1.3** `./gradlew build --offline` 通过

### 4.2 客户端粒子实现

- [x] **4.2.1** 文明粒子暂复用基础工厂（DivineSparkle/MythicFlame/Shockwave），后续可创建独立渲染类
- [x] **4.2.2** 在 `ModClientParticles.java` 中注册 10 个新粒子工厂（暂复用现有工厂）
- [x] **4.2.3** 创建 11 个粒子纹理 PNG（8x8 径向渐变，文明主题色）
- [x] **4.2.4** `./gradlew build --offline` 通过

### 4.3 三段式粒子接口

- [x] **4.3.1** 创建 `particle/SpellParticleEffect.java` 接口（playCast, playActive, playEnd）
- [x] **4.3.2** 创建 5 个文明默认粒子效果实现：GreekParticleEffect、EgyptianParticleEffect、NordicParticleEffect、ChineseParticleEffect、AtlanteanParticleEffect
- [x] **4.3.3** 在 `AbstractSpell` 基类中新增 `getParticleEffect()` 方法，按文明自动返回对应效果
- [x] **4.3.4** `./gradlew build --offline` 通过

### 4.4 法术粒子替换

- [ ] **4.4.1** 逐步将法术类中的原版 `ParticleTypes.xxx` 替换为文明主题粒子（优先替换高频出现的 CLOUD, CRIT, SOUL）
- [ ] **4.4.2** 在每个法术的 cast 方法中集成三段式粒子调用
- [ ] **4.4.3** `./gradlew build --offline` 通过
- [ ] **4.4.4** 进游戏验证：每个文明的法术有明显的视觉主题差异

---

## 阶段 5：Java ↔ BBModel 同步工具

### 5.1 通用同步脚本

- [x] **5.1.1** 创建 `tools/sync_creature_bbmodel.py`
- [x] **5.1.2** 实现 `parse_java_model(path)` — 用正则解析 addChild 层级
- [x] **5.1.3** 实现 `parse_bbmodel(path)` — 解析 bbmodel JSON outliner
- [x] **5.1.4** 实现 `diff_trees(java_tree, bbmodel_tree)` — 对比新增/删除
- [x] **5.1.5** 实现 `write_bbmodel(java_tree, output_path)` — 从 Java 树生成 bbmodel
- [x] **5.1.6** 创建 `tools/creature_model_map.json` — 30 生物映射
- [x] **5.1.7** `python3 tools/sync_creature_bbmodel.py audit` 可运行，报告 0/30 一致（预期：Java 和 BBModel 有历史漂移）

### 5.2 MCP 扩展

- [ ] **5.2.1** 在 `blockbench_visual_mcp.py` 中新增 `sync_java_bbmodel` 工具
- [ ] **5.2.2** 在 `blockbench_visual_mcp.py` 中新增 `audit_java_bbmodel_consistency` 工具
- [ ] **5.2.3** `python3 -m py_compile tools/blockbench_visual_mcp.py` 通过

### 5.3 验证

- [ ] **5.3.1** 选择 Minotaur 执行 `sync`，确认生成的 bbmodel 可在 Blockbench 中打开
- [ ] **5.3.2** 选择 Hydra 执行 `diff`，确认差异报告准确（当前 Java 5 头 vs 设计 9 头应有差异）
- [ ] **5.3.3** `audit` 报告所有 30 生物的同步状态

---

## 通用规则（每个阶段都适用）

### 构建验证
- [ ] 每完成一个标记为 `build` 的子项后立即执行 `./gradlew build --offline`
- [ ] 如果 build 失败，修复后再继续下一项，不跳过

### 代码规范
- [ ] 新增类遵循现有包结构约定
- [ ] 单例类使用 `getInstance()` 模式（参照 `GodPowerCertificateManager`）
- [ ] Mixin 字段命名带 `mythicAge$` 前缀
- [ ] NBT 持久化键命名带 `MythicAge_` 前缀
- [ ] 翻译键命名遵循 `mythic_age.{system}.{action}` 模式

### 不要做的事
- [ ] 不修改任何生物的视觉模型（Pegasus/Hydra 重建是独立任务）
- [ ] 不修改现有的希腊/埃及/北欧/亚特兰蒂斯证书机制
- [ ] 不新增 Minecraft 原版系统的 breaking change
- [ ] 法术迁移时不改变任何法术的数值、范围、冷却或效果

### 完成标志
当以上所有阶段的所有项均标记完成，且最终 `./gradlew build --offline` 通过时，本轮架构改进视为完成。
