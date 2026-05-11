# 神话时代 Mod - 核心功能单元测试计划与记录

本文档旨在规划和记录《神话时代》Mod 核心功能模块的单元测试与调优过程。由于无法直接在游戏环境中进行测试，此文档将侧重于代码逻辑审查、预期行为描述以及模拟测试场景。

## 测试阶段目标

1.  **功能验证：** 确保每个已实现的核心功能模块（神力法术书、神话生物、圣物系统、神殿交互）按设计预期工作。
2.  **交互检查：** 验证不同模块间的交互是否顺畅且符合逻辑。
3.  **稳定性评估：** 识别潜在的错误、崩溃点或性能问题。
4.  **代码调优：** 根据测试结果和代码审查，对现有代码进行优化和改进。

## 测试模块与测试点

### 1. 神力法术书模块 (Spellbook)

*   **测试项 1.1：法术书创建与注册**
    *   **验证：** `ModItems.SPELLBOOK_LIGHTNING_BOLT` 是否成功注册。
    *   **验证：** 法术书物品是否具有正确的名称、描述、材质和稀有度。
    *   **方法：** 代码审查 `ModItems.java`, `SpellbookItem.java`, `en_us.json`, `spellbook_lightning_bolt.json` (模型), `spellbook_lightning_bolt.png` (材质)。
*   **测试项 1.2：法术书使用 (闪电神力)**
    *   **验证：** 右键使用法术书是否能触发闪电效果（在 `SpellbookItem.use` 方法中模拟）。
    *   **验证：** 闪电是否在目标位置或玩家前方生成。
    *   **验证：** 是否有相应的音效和粒子效果提示。
    *   **方法：** 代码审查 `SpellbookItem.java` 中的 `use` 方法逻辑。
*   **测试项 1.3：冷却机制**
    *   **验证：** 使用法术书后，物品是否进入冷却状态。
    *   **验证：** 冷却时间是否与设定值一致 (例如，2秒/40 ticks)。
    *   **验证：** 冷却期间是否无法再次使用法术书。
    *   **验证：** 冷却结束后是否可以再次使用。
    *   **方法：** 代码审查 `SpellbookItem.java` 中与 `player.getItemCooldownManager()`相关的逻辑。

### 2. 神话生物模块 (米诺陶 Minotaur)

*   **测试项 2.1：米诺陶实体注册与属性**
    *   **验证：** `ModEntities.MINOTAUR` 是否成功注册。
    *   **验证：** 米诺陶是否具有正确的实体属性（生命值、攻击力、速度等）。
    *   **方法：** 代码审查 `ModEntities.java`, `MinotaurEntity.java` (特别是 `createMinotaurAttributes` 方法)。
*   **测试项 2.2：米诺陶 AI 行为**
    *   **验证：** 米诺陶是否能正确游荡。
    *   **验证：** 米诺陶是否能主动攻击玩家或敌对生物。
    *   **验证：** 目标选择逻辑是否合理。
    *   **方法：** 代码审查 `MinotaurEntity.java` 中的 `initGoals` 方法。
*   **测试项 2.3：米诺陶特殊能力 (冲锋)**
    *   **验证：** 米诺陶在特定条件下是否会触发冲锋行为。
    *   **验证：** 冲锋行为的视觉和伤害效果是否符合预期。
    *   **方法：** 代码审查 `MinotaurChargeAttackGoal.java` 和其在 `MinotaurEntity` 中的集成。
*   **测试项 2.4：音效与模型/材质**
    *   **验证：** 米诺陶的各种音效（环境音、受伤音、死亡音、脚步声、冲锋音）是否正确注册并能在对应事件触发（逻辑层面）。
    *   **验证：** 米诺陶的模型 (`MinotaurModel.java`) 和材质 (`minotaur.png`) 是否正确加载和渲染（逻辑层面，检查注册和路径）。
    *   **方法：** 代码审查 `ModSounds.java`, `sounds.json`, `MinotaurEntity.java` (音效调用), `MinotaurModel.java`, `MinotaurEntityRenderer.java`, `ModModelLayers.java`, `MythicAgeModClient.java` (渲染注册)。

### 3. 圣物物品模块 (Relics)

*   **测试项 3.1：圣物物品创建与注册**
    *   **验证：** `RELIC_ANKH_OF_RA`, `RELIC_HELM_OF_ARES`, `RELIC_MJOLNIR_FRAGMENT` 是否成功注册。
    *   **验证：** 圣物物品是否具有正确的名称、描述、提示语、材质和稀有度。
    *   **方法：** 代码审查 `ModItems.java`, `RelicItem.java`, `en_us.json`, 相关模型和材质文件。
*   **测试项 3.2：圣物被动效果 (手持)**
    *   **验证：** 手持“拉之安卡”时，玩家是否获得生命恢复效果。
    *   **验证：** 手持“阿瑞斯之盔”时，玩家是否获得力量效果。
    *   **验证：** 效果的持续时间和等级是否正确。
    *   **方法：** 代码审查 `RelicItem.java` 中的 `inventoryTick` 方法。

### 4. 神殿与圣物基座模块 (Temple & Relic Pedestal)

*   **测试项 4.1：圣物基座方块与方块实体**
    *   **验证：** `RELIC_PEDESTAL` 方块及其方块物品是否成功注册。
    *   **验证：** `RelicPedestalBlockEntity` 是否成功注册。
    *   **验证：** 基座方块是否具有正确的模型和碰撞箱。
    *   **方法：** 代码审查 `ModBlocks.java`, `RelicPedestalBlock.java`, `ModBlockEntities.java`, `RelicPedestalBlockEntity.java`。
*   **测试项 4.2：圣物放置与取回**
    *   **验证：** 玩家能否将圣物物品放置在基座上。
    *   **验证：** 玩家能否从基座上取回圣物物品。
    *   **验证：** 放置和取回操作是否伴有正确的音效。
    *   **验证：** 基座方块实体是否正确存储和同步圣物 ItemStack。
    *   **方法：** 代码审查 `RelicPedestalBlock.java` 中的 `onUse` 方法和 `RelicPedestalBlockEntity.java` 中的 NBT 存储与同步逻辑。
*   **测试项 4.3：简单神殿结构检测**
    *   **验证：** `MultiBlockStructureUtil.checkForSimplePlatform` 是否能正确检测预设的简单平台结构（例如，基座下方3x3的石砖平台）。
    *   **验证：** `RelicPedestalBlockEntity` 在放置圣物或周期性检查时，是否调用结构检测逻辑。
    *   **方法：** 代码审查 `MultiBlockStructureUtil.java` 和 `RelicPedestalBlockEntity.java` 中的 `checkTempleStructure` 和 `tick` 方法。
*   **测试项 4.4：神殿激活与圣物效果 (基座)**
    *   **验证：** 当圣物放置在有效神殿结构的基座上时，`isActiveTemple` 状态是否正确更新。
    *   **验证：** 神殿激活时是否有相应的粒子和音效提示。
    *   **验证：** “拉之安卡”在激活的神殿中是否提供范围生命恢复效果。
    *   **验证：** “阿瑞斯之盔”在激活的神殿中是否提供范围力量效果。
    *   **验证：** 效果范围、持续时间和等级是否正确。
    *   **方法：** 代码审查 `RelicPedestalBlockEntity.java` 中的 `tick` 和 `checkTempleStructure` 方法。
*   **测试项 4.5：比较器输出**
    *   **验证：** 圣物基座在放置圣物后是否能输出红石信号。
    *   **方法：** 代码审查 `RelicPedestalBlock.java` 中的 `hasComparatorOutput` 和 `getComparatorOutput` 方法。

## 调优与问题修复记录

*(此部分将在模拟测试和代码审查过程中逐步填写)*

*   **问题/调优点 1：** (描述)
    *   **解决方案：** (描述)
    *   **状态：** (待处理/处理中/已解决)

## 后续测试计划

1.  **战利品表生成测试：** (在实现后)
    *   验证神力法术书是否按预期在村庄箱子中生成。
2.  **集成测试：**
    *   测试所有模块组合在一起时的整体表现和交互。
3.  **兼容性测试：** (如果可能)
    *   考虑与其他常见 Fabric Mod 的兼容性问题。