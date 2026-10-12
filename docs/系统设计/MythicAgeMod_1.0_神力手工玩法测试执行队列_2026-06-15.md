# MythicAgeMod 1.0 神力手工玩法测试执行队列（2026-06-15）

本文档由 `tools/god_power_manual_playtest_execution_queue.py audit` 生成。它把已有神力玩法验收包中的 52 个待测神力整理成可执行记录队列。

## 总览

- 状态：`manual_playtest_not_done`
- 神力数量：52
- 批次数量：8
- 场景模板总数：446
- 源最低场景预算：446
- 风险统计：`{"high": 18, "low": 11, "medium": 23}`
- 优先级统计：`{"1": 18, "2": 23, "3": 11}`
- 文明统计：`{"atlantean": 10, "chinese": 10, "egyptian": 10, "greek": 12, "nordic": 10}`
- 世界编辑/爆炸相关神力：18
- runtime smoke 全通过：`True`
- CSV 执行表：`docs/preview_assets/god_power_gameplay/manual_playtest_execution/god_power_manual_playtest_execution_queue.csv`
- 结果台账模板：`docs/preview_assets/god_power_gameplay/manual_playtest_execution/god_power_manual_playtest_result_ledger_template.json`

## 记录字段

- `tester`
- `date`
- `minecraft_version`
- `world_seed`
- `game_mode`
- `spell_item`
- `target_case`
- `expected_result`
- `actual_result`
- `cooldown_cost_ok`
- `feedback_ok`
- `particle_ok`
- `audio_ok`
- `cleanup_ok`
- `performance_ok`
- `pass_fail`
- `notes`
- `evidence_path`

## 批次执行顺序

| 批次 | 优先级 | 风险 | 神力数 | 最低场景 | 世界编辑项 | 神力 | 执行重点 |
| --- | ---: | --- | ---: | ---: | ---: | --- | --- |
| P1-high-1 | 1 | `high` | 6 | 72 | 6 | chinese:ABUNDANCE, chinese:EARTH_GOD_BLESSING, chinese:FLOOD, chinese:RECREATION, egyptian:ANUBIS_JUDGMENT, egyptian:DESERT_STORM | 目标选择和失败路径必须覆盖。；粒子/音效/UI 反馈必须可读。；高风险批次必须额外记录性能、存档和清理结果。 |
| P1-high-2 | 1 | `high` | 6 | 72 | 6 | egyptian:EYE_OF_HORUS, egyptian:ISIS_RESURRECTION, egyptian:LOCUST_SWARM, egyptian:METEOR, egyptian:PROSPERITY, egyptian:RA_RADIANCE | 目标选择和失败路径必须覆盖。；粒子/音效/UI 反馈必须可读。；高风险批次必须额外记录性能、存档和清理结果。 |
| P1-high-3 | 1 | `high` | 6 | 74 | 6 | egyptian:SUN_RAIN, egyptian:TORNADO, greek:EARTHQUAKE, nordic:FIMBULWINTER, nordic:FROST, nordic:RAGNAROK | 目标选择和失败路径必须覆盖。；粒子/音效/UI 反馈必须可读。；高风险批次必须额外记录性能、存档和清理结果。 |
| P2-medium-1 | 2 | `medium` | 8 | 66 | 0 | atlantean:ABYSSAL_CURRENT, atlantean:ABYSSAL_RIFT, atlantean:CHRONO_STASIS, atlantean:LEVIATHAN_CALL, atlantean:OCEANIC_WRATH, atlantean:ORICHALCUM_FORGE, atlantean:PEARL_HARVES... | 确认手感、平衡和反馈是否符合设计。；记录任何遮挡、误导或冷却/成本异常。 |
| P2-medium-2 | 2 | `medium` | 8 | 56 | 0 | chinese:CALL_TO_ARMS, chinese:CRANE_BLESSING, chinese:ENLIGHTENMENT, chinese:FIVE_THUNDER, greek:CURSE, greek:LIGHTNING_STORM, greek:LIGHTNING_STORM_HERA, greek:UNDERWORLD_ARMY | 确认手感、平衡和反馈是否符合设计。；记录任何遮挡、误导或冷却/成本异常。 |
| P2-medium-3 | 2 | `medium` | 7 | 51 | 0 | greek:UNDERWORLD_PASSAGE, nordic:FENRIR_HOWL, nordic:GREAT_HUNT, nordic:ODIN_EYE, nordic:THOR_WRATH, nordic:VALHALLA_CALL, nordic:WALKING_WOODS | 确认手感、平衡和反馈是否符合设计。；记录任何遮挡、误导或冷却/成本异常。 |
| P3-low-1 | 3 | `low` | 10 | 50 | 0 | atlantean:CRYSTAL_BARRIER, atlantean:SIREN_SONG, chinese:RAIN_PRAYER, chinese:TAIJI_SHIELD, greek:BRONZE, greek:CEASEFIRE, greek:LURE, greek:PESTILENCE, greek:PLENTY, greek:REST... | 确认手感、平衡和反馈是否符合设计。；记录任何遮挡、误导或冷却/成本异常。 |
| P3-low-2 | 3 | `low` | 1 | 5 | 0 | nordic:DWARVEN_MINE | 确认手感、平衡和反馈是否符合设计。；记录任何遮挡、误导或冷却/成本异常。 |

## 批次勾选单

### P1-high-1

- [ ] 创建/加载 1.0 标准测试世界。
- [ ] 准备本批次资源、证明不足状态、冷却状态、目标实体和空目标区域。
- [ ] 执行本批次 6 个神力，共 72 个最低场景行。
- [ ] 对 6 个世界编辑/爆炸类神力执行区块重载、保存退出重进和清理验证。
- [ ] 填写 CSV 或 JSON 台账中的 `pass_fail`、`actual_result`、`evidence_path` 和备注。
- [ ] 失败项已登记 issue 或回到代码/设计/UI/粒子/音效/NBT 对应步骤。

### P1-high-2

- [ ] 创建/加载 1.0 标准测试世界。
- [ ] 准备本批次资源、证明不足状态、冷却状态、目标实体和空目标区域。
- [ ] 执行本批次 6 个神力，共 72 个最低场景行。
- [ ] 对 6 个世界编辑/爆炸类神力执行区块重载、保存退出重进和清理验证。
- [ ] 填写 CSV 或 JSON 台账中的 `pass_fail`、`actual_result`、`evidence_path` 和备注。
- [ ] 失败项已登记 issue 或回到代码/设计/UI/粒子/音效/NBT 对应步骤。

### P1-high-3

- [ ] 创建/加载 1.0 标准测试世界。
- [ ] 准备本批次资源、证明不足状态、冷却状态、目标实体和空目标区域。
- [ ] 执行本批次 6 个神力，共 74 个最低场景行。
- [ ] 对 6 个世界编辑/爆炸类神力执行区块重载、保存退出重进和清理验证。
- [ ] 填写 CSV 或 JSON 台账中的 `pass_fail`、`actual_result`、`evidence_path` 和备注。
- [ ] 失败项已登记 issue 或回到代码/设计/UI/粒子/音效/NBT 对应步骤。

### P2-medium-1

- [ ] 创建/加载 1.0 标准测试世界。
- [ ] 准备本批次资源、证明不足状态、冷却状态、目标实体和空目标区域。
- [ ] 执行本批次 8 个神力，共 66 个最低场景行。
- [ ] 对 0 个世界编辑/爆炸类神力执行区块重载、保存退出重进和清理验证。
- [ ] 填写 CSV 或 JSON 台账中的 `pass_fail`、`actual_result`、`evidence_path` 和备注。
- [ ] 失败项已登记 issue 或回到代码/设计/UI/粒子/音效/NBT 对应步骤。

### P2-medium-2

- [ ] 创建/加载 1.0 标准测试世界。
- [ ] 准备本批次资源、证明不足状态、冷却状态、目标实体和空目标区域。
- [ ] 执行本批次 8 个神力，共 56 个最低场景行。
- [ ] 对 0 个世界编辑/爆炸类神力执行区块重载、保存退出重进和清理验证。
- [ ] 填写 CSV 或 JSON 台账中的 `pass_fail`、`actual_result`、`evidence_path` 和备注。
- [ ] 失败项已登记 issue 或回到代码/设计/UI/粒子/音效/NBT 对应步骤。

### P2-medium-3

- [ ] 创建/加载 1.0 标准测试世界。
- [ ] 准备本批次资源、证明不足状态、冷却状态、目标实体和空目标区域。
- [ ] 执行本批次 7 个神力，共 51 个最低场景行。
- [ ] 对 0 个世界编辑/爆炸类神力执行区块重载、保存退出重进和清理验证。
- [ ] 填写 CSV 或 JSON 台账中的 `pass_fail`、`actual_result`、`evidence_path` 和备注。
- [ ] 失败项已登记 issue 或回到代码/设计/UI/粒子/音效/NBT 对应步骤。

### P3-low-1

- [ ] 创建/加载 1.0 标准测试世界。
- [ ] 准备本批次资源、证明不足状态、冷却状态、目标实体和空目标区域。
- [ ] 执行本批次 10 个神力，共 50 个最低场景行。
- [ ] 对 0 个世界编辑/爆炸类神力执行区块重载、保存退出重进和清理验证。
- [ ] 填写 CSV 或 JSON 台账中的 `pass_fail`、`actual_result`、`evidence_path` 和备注。
- [ ] 失败项已登记 issue 或回到代码/设计/UI/粒子/音效/NBT 对应步骤。

### P3-low-2

- [ ] 创建/加载 1.0 标准测试世界。
- [ ] 准备本批次资源、证明不足状态、冷却状态、目标实体和空目标区域。
- [ ] 执行本批次 1 个神力，共 5 个最低场景行。
- [ ] 对 0 个世界编辑/爆炸类神力执行区块重载、保存退出重进和清理验证。
- [ ] 填写 CSV 或 JSON 台账中的 `pass_fail`、`actual_result`、`evidence_path` 和备注。
- [ ] 失败项已登记 issue 或回到代码/设计/UI/粒子/音效/NBT 对应步骤。


## 单神力执行模板

### chinese:ABUNDANCE

- 优先级：`1`
- 风险：`high`
- 目标模式：`caster_position`
- 作用范围：`["buff_or_debuff", "world_edit"]`
- 世界操作：`["set_block_state"]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/chinese/abundance_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |
| `protected_block_safety` 保护/核心方块安全 | 不破坏基岩、传送门、神殿核心或不可破坏目标。 |
| `chunk_reload_cleanup` 离开区块并重载 | 持续效果、临时方块、任务或实体不会异常残留。 |
| `save_restart_cleanup` 保存退出后重进 | 跨存档状态符合 NBT 目标表，runtime_only 状态不错误持久化。 |
| `balance_repeat_1` 战斗节奏/平衡复测 1 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_2` 战斗节奏/平衡复测 2 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_3` 战斗节奏/平衡复测 3 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_4` 战斗节奏/平衡复测 4 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### chinese:EARTH_GOD_BLESSING

- 优先级：`1`
- 风险：`high`
- 目标模式：`caster_position`
- 作用范围：`["buff_or_debuff", "world_edit", "resource_generation"]`
- 世界操作：`["set_block_state"]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/chinese/earth_god_blessing_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |
| `protected_block_safety` 保护/核心方块安全 | 不破坏基岩、传送门、神殿核心或不可破坏目标。 |
| `chunk_reload_cleanup` 离开区块并重载 | 持续效果、临时方块、任务或实体不会异常残留。 |
| `save_restart_cleanup` 保存退出后重进 | 跨存档状态符合 NBT 目标表，runtime_only 状态不错误持久化。 |
| `balance_repeat_1` 战斗节奏/平衡复测 1 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_2` 战斗节奏/平衡复测 2 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_3` 战斗节奏/平衡复测 3 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_4` 战斗节奏/平衡复测 4 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### chinese:FLOOD

- 优先级：`1`
- 风险：`high`
- 目标模式：`target_block_or_crosshair`
- 作用范围：`["damage", "buff_or_debuff", "world_edit", "resource_generation"]`
- 世界操作：`["set_block_state"]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/chinese/flood_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |
| `protected_block_safety` 保护/核心方块安全 | 不破坏基岩、传送门、神殿核心或不可破坏目标。 |
| `chunk_reload_cleanup` 离开区块并重载 | 持续效果、临时方块、任务或实体不会异常残留。 |
| `save_restart_cleanup` 保存退出后重进 | 跨存档状态符合 NBT 目标表，runtime_only 状态不错误持久化。 |
| `balance_repeat_1` 战斗节奏/平衡复测 1 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_2` 战斗节奏/平衡复测 2 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_3` 战斗节奏/平衡复测 3 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_4` 战斗节奏/平衡复测 4 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### chinese:RECREATION

- 优先级：`1`
- 风险：`high`
- 目标模式：`target_block_or_crosshair`
- 作用范围：`["buff_or_debuff", "summon_or_spawn", "world_edit", "resource_generation"]`
- 世界操作：`["set_block_state"]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/chinese/recreation_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |
| `protected_block_safety` 保护/核心方块安全 | 不破坏基岩、传送门、神殿核心或不可破坏目标。 |
| `chunk_reload_cleanup` 离开区块并重载 | 持续效果、临时方块、任务或实体不会异常残留。 |
| `save_restart_cleanup` 保存退出后重进 | 跨存档状态符合 NBT 目标表，runtime_only 状态不错误持久化。 |
| `spawn_cap_or_cleanup` 召唤上限与清理 | 召唤数量、持续时间、阵营和消失逻辑符合设计。 |
| `balance_repeat_1` 战斗节奏/平衡复测 1 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_2` 战斗节奏/平衡复测 2 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_3` 战斗节奏/平衡复测 3 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### egyptian:ANUBIS_JUDGMENT

- 优先级：`1`
- 风险：`high`
- 目标模式：`target_block_or_crosshair`
- 作用范围：`["damage", "buff_or_debuff", "summon_or_spawn", "world_edit", "resource_generation"]`
- 世界操作：`["set_block_state"]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/egyptian/anubis_judgment_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |
| `protected_block_safety` 保护/核心方块安全 | 不破坏基岩、传送门、神殿核心或不可破坏目标。 |
| `chunk_reload_cleanup` 离开区块并重载 | 持续效果、临时方块、任务或实体不会异常残留。 |
| `save_restart_cleanup` 保存退出后重进 | 跨存档状态符合 NBT 目标表，runtime_only 状态不错误持久化。 |
| `spawn_cap_or_cleanup` 召唤上限与清理 | 召唤数量、持续时间、阵营和消失逻辑符合设计。 |
| `balance_repeat_1` 战斗节奏/平衡复测 1 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_2` 战斗节奏/平衡复测 2 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_3` 战斗节奏/平衡复测 3 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### egyptian:DESERT_STORM

- 优先级：`1`
- 风险：`high`
- 目标模式：`target_block_or_crosshair`
- 作用范围：`["damage", "buff_or_debuff", "world_edit", "resource_generation"]`
- 世界操作：`["set_block_state"]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/egyptian/desert_storm_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |
| `protected_block_safety` 保护/核心方块安全 | 不破坏基岩、传送门、神殿核心或不可破坏目标。 |
| `chunk_reload_cleanup` 离开区块并重载 | 持续效果、临时方块、任务或实体不会异常残留。 |
| `save_restart_cleanup` 保存退出后重进 | 跨存档状态符合 NBT 目标表，runtime_only 状态不错误持久化。 |
| `balance_repeat_1` 战斗节奏/平衡复测 1 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_2` 战斗节奏/平衡复测 2 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_3` 战斗节奏/平衡复测 3 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_4` 战斗节奏/平衡复测 4 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### egyptian:EYE_OF_HORUS

- 优先级：`1`
- 风险：`high`
- 目标模式：`target_block_or_crosshair`
- 作用范围：`["buff_or_debuff", "summon_or_spawn", "world_edit", "resource_generation"]`
- 世界操作：`["set_block_state"]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/egyptian/eye_of_horus_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |
| `protected_block_safety` 保护/核心方块安全 | 不破坏基岩、传送门、神殿核心或不可破坏目标。 |
| `chunk_reload_cleanup` 离开区块并重载 | 持续效果、临时方块、任务或实体不会异常残留。 |
| `save_restart_cleanup` 保存退出后重进 | 跨存档状态符合 NBT 目标表，runtime_only 状态不错误持久化。 |
| `spawn_cap_or_cleanup` 召唤上限与清理 | 召唤数量、持续时间、阵营和消失逻辑符合设计。 |
| `balance_repeat_1` 战斗节奏/平衡复测 1 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_2` 战斗节奏/平衡复测 2 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_3` 战斗节奏/平衡复测 3 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### egyptian:ISIS_RESURRECTION

- 优先级：`1`
- 风险：`high`
- 目标模式：`target_block_or_crosshair`
- 作用范围：`["buff_or_debuff", "summon_or_spawn", "world_edit", "resource_generation"]`
- 世界操作：`["set_block_state"]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/egyptian/isis_resurrection_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |
| `protected_block_safety` 保护/核心方块安全 | 不破坏基岩、传送门、神殿核心或不可破坏目标。 |
| `chunk_reload_cleanup` 离开区块并重载 | 持续效果、临时方块、任务或实体不会异常残留。 |
| `save_restart_cleanup` 保存退出后重进 | 跨存档状态符合 NBT 目标表，runtime_only 状态不错误持久化。 |
| `spawn_cap_or_cleanup` 召唤上限与清理 | 召唤数量、持续时间、阵营和消失逻辑符合设计。 |
| `balance_repeat_1` 战斗节奏/平衡复测 1 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_2` 战斗节奏/平衡复测 2 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_3` 战斗节奏/平衡复测 3 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### egyptian:LOCUST_SWARM

- 优先级：`1`
- 风险：`high`
- 目标模式：`target_block_or_crosshair`
- 作用范围：`["damage", "buff_or_debuff", "summon_or_spawn", "world_edit", "resource_generation"]`
- 世界操作：`["set_block_state"]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/egyptian/locust_swarm_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |
| `protected_block_safety` 保护/核心方块安全 | 不破坏基岩、传送门、神殿核心或不可破坏目标。 |
| `chunk_reload_cleanup` 离开区块并重载 | 持续效果、临时方块、任务或实体不会异常残留。 |
| `save_restart_cleanup` 保存退出后重进 | 跨存档状态符合 NBT 目标表，runtime_only 状态不错误持久化。 |
| `spawn_cap_or_cleanup` 召唤上限与清理 | 召唤数量、持续时间、阵营和消失逻辑符合设计。 |
| `balance_repeat_1` 战斗节奏/平衡复测 1 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_2` 战斗节奏/平衡复测 2 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_3` 战斗节奏/平衡复测 3 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### egyptian:METEOR

- 优先级：`1`
- 风险：`high`
- 目标模式：`target_block_or_crosshair`
- 作用范围：`["buff_or_debuff"]`
- 世界操作：`["meteor_entity"]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/egyptian/meteor_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |
| `protected_block_safety` 保护/核心方块安全 | 不破坏基岩、传送门、神殿核心或不可破坏目标。 |
| `chunk_reload_cleanup` 离开区块并重载 | 持续效果、临时方块、任务或实体不会异常残留。 |
| `save_restart_cleanup` 保存退出后重进 | 跨存档状态符合 NBT 目标表，runtime_only 状态不错误持久化。 |
| `balance_repeat_1` 战斗节奏/平衡复测 1 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_2` 战斗节奏/平衡复测 2 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_3` 战斗节奏/平衡复测 3 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_4` 战斗节奏/平衡复测 4 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### egyptian:PROSPERITY

- 优先级：`1`
- 风险：`high`
- 目标模式：`target_block_or_crosshair`
- 作用范围：`["buff_or_debuff", "summon_or_spawn", "world_edit", "resource_generation"]`
- 世界操作：`["set_block_state"]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/egyptian/prosperity_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |
| `protected_block_safety` 保护/核心方块安全 | 不破坏基岩、传送门、神殿核心或不可破坏目标。 |
| `chunk_reload_cleanup` 离开区块并重载 | 持续效果、临时方块、任务或实体不会异常残留。 |
| `save_restart_cleanup` 保存退出后重进 | 跨存档状态符合 NBT 目标表，runtime_only 状态不错误持久化。 |
| `spawn_cap_or_cleanup` 召唤上限与清理 | 召唤数量、持续时间、阵营和消失逻辑符合设计。 |
| `balance_repeat_1` 战斗节奏/平衡复测 1 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_2` 战斗节奏/平衡复测 2 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_3` 战斗节奏/平衡复测 3 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### egyptian:RA_RADIANCE

- 优先级：`1`
- 风险：`high`
- 目标模式：`target_block_or_crosshair`
- 作用范围：`["damage", "buff_or_debuff", "world_edit", "resource_generation"]`
- 世界操作：`["set_block_state"]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/egyptian/ra_radiance_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |
| `protected_block_safety` 保护/核心方块安全 | 不破坏基岩、传送门、神殿核心或不可破坏目标。 |
| `chunk_reload_cleanup` 离开区块并重载 | 持续效果、临时方块、任务或实体不会异常残留。 |
| `save_restart_cleanup` 保存退出后重进 | 跨存档状态符合 NBT 目标表，runtime_only 状态不错误持久化。 |
| `balance_repeat_1` 战斗节奏/平衡复测 1 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_2` 战斗节奏/平衡复测 2 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_3` 战斗节奏/平衡复测 3 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_4` 战斗节奏/平衡复测 4 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### egyptian:SUN_RAIN

- 优先级：`1`
- 风险：`high`
- 目标模式：`target_block_or_crosshair`
- 作用范围：`["buff_or_debuff", "world_edit", "resource_generation"]`
- 世界操作：`["set_block_state"]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/egyptian/sun_rain_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |
| `protected_block_safety` 保护/核心方块安全 | 不破坏基岩、传送门、神殿核心或不可破坏目标。 |
| `chunk_reload_cleanup` 离开区块并重载 | 持续效果、临时方块、任务或实体不会异常残留。 |
| `save_restart_cleanup` 保存退出后重进 | 跨存档状态符合 NBT 目标表，runtime_only 状态不错误持久化。 |
| `balance_repeat_1` 战斗节奏/平衡复测 1 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_2` 战斗节奏/平衡复测 2 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_3` 战斗节奏/平衡复测 3 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_4` 战斗节奏/平衡复测 4 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### egyptian:TORNADO

- 优先级：`1`
- 风险：`high`
- 目标模式：`target_block_or_crosshair`
- 作用范围：`["damage", "buff_or_debuff", "world_edit", "teleport_or_mobility", "resource_generation"]`
- 世界操作：`["set_block_state", "break_block"]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/egyptian/tornado_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |
| `protected_block_safety` 保护/核心方块安全 | 不破坏基岩、传送门、神殿核心或不可破坏目标。 |
| `chunk_reload_cleanup` 离开区块并重载 | 持续效果、临时方块、任务或实体不会异常残留。 |
| `save_restart_cleanup` 保存退出后重进 | 跨存档状态符合 NBT 目标表，runtime_only 状态不错误持久化。 |
| `obstructed_destination` 传送/位移目标被阻挡 | 不会卡墙、窒息或越界；失败时给出反馈。 |
| `vertical_or_water_edge` 高度差/水边/边界地形 | 目标选择和安全落点符合预期。 |
| `balance_repeat_1` 战斗节奏/平衡复测 1 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_2` 战斗节奏/平衡复测 2 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_3` 战斗节奏/平衡复测 3 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_4` 战斗节奏/平衡复测 4 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### greek:EARTHQUAKE

- 优先级：`1`
- 风险：`high`
- 目标模式：`caster_position`
- 作用范围：`["weather_or_persistent_manager"]`
- 世界操作：`["earthquake_manager"]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/greek/earthquake_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |
| `protected_block_safety` 保护/核心方块安全 | 不破坏基岩、传送门、神殿核心或不可破坏目标。 |
| `chunk_reload_cleanup` 离开区块并重载 | 持续效果、临时方块、任务或实体不会异常残留。 |
| `save_restart_cleanup` 保存退出后重进 | 跨存档状态符合 NBT 目标表，runtime_only 状态不错误持久化。 |
| `balance_repeat_1` 战斗节奏/平衡复测 1 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_2` 战斗节奏/平衡复测 2 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_3` 战斗节奏/平衡复测 3 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_4` 战斗节奏/平衡复测 4 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### nordic:FIMBULWINTER

- 优先级：`1`
- 风险：`high`
- 目标模式：`target_block_or_crosshair`
- 作用范围：`["damage", "buff_or_debuff", "world_edit", "weather_or_persistent_manager", "resource_generation"]`
- 世界操作：`["set_block_state"]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/nordic/fimbulwinter_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |
| `protected_block_safety` 保护/核心方块安全 | 不破坏基岩、传送门、神殿核心或不可破坏目标。 |
| `chunk_reload_cleanup` 离开区块并重载 | 持续效果、临时方块、任务或实体不会异常残留。 |
| `save_restart_cleanup` 保存退出后重进 | 跨存档状态符合 NBT 目标表，runtime_only 状态不错误持久化。 |
| `balance_repeat_1` 战斗节奏/平衡复测 1 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_2` 战斗节奏/平衡复测 2 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_3` 战斗节奏/平衡复测 3 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_4` 战斗节奏/平衡复测 4 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### nordic:FROST

- 优先级：`1`
- 风险：`high`
- 目标模式：`target_block_or_crosshair`
- 作用范围：`["damage", "buff_or_debuff", "world_edit", "resource_generation"]`
- 世界操作：`["set_block_state"]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/nordic/frost_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |
| `protected_block_safety` 保护/核心方块安全 | 不破坏基岩、传送门、神殿核心或不可破坏目标。 |
| `chunk_reload_cleanup` 离开区块并重载 | 持续效果、临时方块、任务或实体不会异常残留。 |
| `save_restart_cleanup` 保存退出后重进 | 跨存档状态符合 NBT 目标表，runtime_only 状态不错误持久化。 |
| `balance_repeat_1` 战斗节奏/平衡复测 1 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_2` 战斗节奏/平衡复测 2 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_3` 战斗节奏/平衡复测 3 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_4` 战斗节奏/平衡复测 4 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### nordic:RAGNAROK

- 优先级：`1`
- 风险：`high`
- 目标模式：`target_block_or_crosshair`
- 作用范围：`["damage", "buff_or_debuff", "summon_or_spawn"]`
- 世界操作：`["create_explosion", "explosion_entity_or_damage"]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/nordic/ragnarok_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |
| `protected_block_safety` 保护/核心方块安全 | 不破坏基岩、传送门、神殿核心或不可破坏目标。 |
| `chunk_reload_cleanup` 离开区块并重载 | 持续效果、临时方块、任务或实体不会异常残留。 |
| `save_restart_cleanup` 保存退出后重进 | 跨存档状态符合 NBT 目标表，runtime_only 状态不错误持久化。 |
| `spawn_cap_or_cleanup` 召唤上限与清理 | 召唤数量、持续时间、阵营和消失逻辑符合设计。 |
| `balance_repeat_1` 战斗节奏/平衡复测 1 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_2` 战斗节奏/平衡复测 2 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_3` 战斗节奏/平衡复测 3 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### atlantean:ABYSSAL_CURRENT

- 优先级：`2`
- 风险：`medium`
- 目标模式：`target_block_or_crosshair`
- 作用范围：`["buff_or_debuff", "teleport_or_mobility"]`
- 世界操作：`[]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/atlantean/abyssal_current_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |
| `obstructed_destination` 传送/位移目标被阻挡 | 不会卡墙、窒息或越界；失败时给出反馈。 |
| `vertical_or_water_edge` 高度差/水边/边界地形 | 目标选择和安全落点符合预期。 |
| `balance_repeat_1` 战斗节奏/平衡复测 1 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_2` 战斗节奏/平衡复测 2 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### atlantean:ABYSSAL_RIFT

- 优先级：`2`
- 风险：`medium`
- 目标模式：`target_block_or_crosshair`
- 作用范围：`["damage", "buff_or_debuff", "teleport_or_mobility"]`
- 世界操作：`[]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/atlantean/abyssal_rift_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |
| `obstructed_destination` 传送/位移目标被阻挡 | 不会卡墙、窒息或越界；失败时给出反馈。 |
| `vertical_or_water_edge` 高度差/水边/边界地形 | 目标选择和安全落点符合预期。 |
| `balance_repeat_1` 战斗节奏/平衡复测 1 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_2` 战斗节奏/平衡复测 2 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### atlantean:CHRONO_STASIS

- 优先级：`2`
- 风险：`medium`
- 目标模式：`target_block_or_crosshair`
- 作用范围：`["damage", "buff_or_debuff", "teleport_or_mobility"]`
- 世界操作：`[]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/atlantean/chrono_stasis_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |
| `obstructed_destination` 传送/位移目标被阻挡 | 不会卡墙、窒息或越界；失败时给出反馈。 |
| `vertical_or_water_edge` 高度差/水边/边界地形 | 目标选择和安全落点符合预期。 |
| `balance_repeat_1` 战斗节奏/平衡复测 1 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_2` 战斗节奏/平衡复测 2 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### atlantean:LEVIATHAN_CALL

- 优先级：`2`
- 风险：`medium`
- 目标模式：`target_block_or_crosshair`
- 作用范围：`["damage", "buff_or_debuff", "teleport_or_mobility"]`
- 世界操作：`[]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/atlantean/leviathan_call_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |
| `obstructed_destination` 传送/位移目标被阻挡 | 不会卡墙、窒息或越界；失败时给出反馈。 |
| `vertical_or_water_edge` 高度差/水边/边界地形 | 目标选择和安全落点符合预期。 |
| `balance_repeat_1` 战斗节奏/平衡复测 1 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_2` 战斗节奏/平衡复测 2 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### atlantean:OCEANIC_WRATH

- 优先级：`2`
- 风险：`medium`
- 目标模式：`target_block_or_crosshair`
- 作用范围：`["damage", "buff_or_debuff", "summon_or_spawn", "weather_or_persistent_manager"]`
- 世界操作：`[]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/atlantean/oceanic_wrath_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |
| `spawn_cap_or_cleanup` 召唤上限与清理 | 召唤数量、持续时间、阵营和消失逻辑符合设计。 |
| `balance_repeat_1` 战斗节奏/平衡复测 1 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### atlantean:ORICHALCUM_FORGE

- 优先级：`2`
- 风险：`medium`
- 目标模式：`target_block_or_crosshair`
- 作用范围：`["buff_or_debuff", "summon_or_spawn", "resource_generation"]`
- 世界操作：`[]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/atlantean/orichalcum_forge_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |
| `spawn_cap_or_cleanup` 召唤上限与清理 | 召唤数量、持续时间、阵营和消失逻辑符合设计。 |
| `balance_repeat_1` 战斗节奏/平衡复测 1 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### atlantean:PEARL_HARVEST

- 优先级：`2`
- 风险：`medium`
- 目标模式：`target_block_or_crosshair`
- 作用范围：`["buff_or_debuff", "summon_or_spawn", "resource_generation"]`
- 世界操作：`[]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/atlantean/pearl_harvest_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |
| `spawn_cap_or_cleanup` 召唤上限与清理 | 召唤数量、持续时间、阵营和消失逻辑符合设计。 |
| `balance_repeat_1` 战斗节奏/平衡复测 1 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### atlantean:TIDAL_SURGE

- 优先级：`2`
- 风险：`medium`
- 目标模式：`target_block_or_crosshair`
- 作用范围：`["damage", "buff_or_debuff", "teleport_or_mobility"]`
- 世界操作：`[]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/atlantean/tidal_surge_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |
| `obstructed_destination` 传送/位移目标被阻挡 | 不会卡墙、窒息或越界；失败时给出反馈。 |
| `vertical_or_water_edge` 高度差/水边/边界地形 | 目标选择和安全落点符合预期。 |
| `balance_repeat_1` 战斗节奏/平衡复测 1 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_2` 战斗节奏/平衡复测 2 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### chinese:CALL_TO_ARMS

- 优先级：`2`
- 风险：`medium`
- 目标模式：`caster_position`
- 作用范围：`["buff_or_debuff", "summon_or_spawn"]`
- 世界操作：`[]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/chinese/call_to_arms_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |
| `spawn_cap_or_cleanup` 召唤上限与清理 | 召唤数量、持续时间、阵营和消失逻辑符合设计。 |
| `balance_repeat_1` 战斗节奏/平衡复测 1 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### chinese:CRANE_BLESSING

- 优先级：`2`
- 风险：`medium`
- 目标模式：`target_block_or_crosshair`
- 作用范围：`["buff_or_debuff", "summon_or_spawn"]`
- 世界操作：`[]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/chinese/crane_blessing_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |
| `spawn_cap_or_cleanup` 召唤上限与清理 | 召唤数量、持续时间、阵营和消失逻辑符合设计。 |
| `balance_repeat_1` 战斗节奏/平衡复测 1 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### chinese:ENLIGHTENMENT

- 优先级：`2`
- 风险：`medium`
- 目标模式：`target_block_or_crosshair`
- 作用范围：`["buff_or_debuff", "summon_or_spawn"]`
- 世界操作：`[]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/chinese/enlightenment_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |
| `spawn_cap_or_cleanup` 召唤上限与清理 | 召唤数量、持续时间、阵营和消失逻辑符合设计。 |
| `balance_repeat_1` 战斗节奏/平衡复测 1 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### chinese:FIVE_THUNDER

- 优先级：`2`
- 风险：`medium`
- 目标模式：`caster_centered_area`
- 作用范围：`["damage", "buff_or_debuff", "summon_or_spawn"]`
- 世界操作：`[]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/chinese/five_thunder_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |
| `spawn_cap_or_cleanup` 召唤上限与清理 | 召唤数量、持续时间、阵营和消失逻辑符合设计。 |
| `balance_repeat_1` 战斗节奏/平衡复测 1 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### greek:CURSE

- 优先级：`2`
- 风险：`medium`
- 目标模式：`caster_centered_area`
- 作用范围：`["summon_or_spawn"]`
- 世界操作：`[]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/greek/curse_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |
| `spawn_cap_or_cleanup` 召唤上限与清理 | 召唤数量、持续时间、阵营和消失逻辑符合设计。 |
| `balance_repeat_1` 战斗节奏/平衡复测 1 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### greek:LIGHTNING_STORM

- 优先级：`2`
- 风险：`medium`
- 目标模式：`caster_position`
- 作用范围：`["summon_or_spawn", "weather_or_persistent_manager"]`
- 世界操作：`[]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/greek/lightning_storm_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |
| `spawn_cap_or_cleanup` 召唤上限与清理 | 召唤数量、持续时间、阵营和消失逻辑符合设计。 |
| `balance_repeat_1` 战斗节奏/平衡复测 1 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### greek:LIGHTNING_STORM_HERA

- 优先级：`2`
- 风险：`medium`
- 目标模式：`caster_position`
- 作用范围：`["summon_or_spawn"]`
- 世界操作：`[]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/greek/lightning_storm_hera_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |
| `spawn_cap_or_cleanup` 召唤上限与清理 | 召唤数量、持续时间、阵营和消失逻辑符合设计。 |
| `balance_repeat_1` 战斗节奏/平衡复测 1 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### greek:UNDERWORLD_ARMY

- 优先级：`2`
- 风险：`medium`
- 目标模式：`caster_position`
- 作用范围：`["summon_or_spawn"]`
- 世界操作：`[]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/greek/underworld_army_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |
| `spawn_cap_or_cleanup` 召唤上限与清理 | 召唤数量、持续时间、阵营和消失逻辑符合设计。 |
| `balance_repeat_1` 战斗节奏/平衡复测 1 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### greek:UNDERWORLD_PASSAGE

- 优先级：`2`
- 风险：`medium`
- 目标模式：`caster_position`
- 作用范围：`["teleport_or_mobility"]`
- 世界操作：`[]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/greek/underworld_passage_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |
| `obstructed_destination` 传送/位移目标被阻挡 | 不会卡墙、窒息或越界；失败时给出反馈。 |
| `vertical_or_water_edge` 高度差/水边/边界地形 | 目标选择和安全落点符合预期。 |
| `balance_repeat_1` 战斗节奏/平衡复测 1 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_2` 战斗节奏/平衡复测 2 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### nordic:FENRIR_HOWL

- 优先级：`2`
- 风险：`medium`
- 目标模式：`target_block_or_crosshair`
- 作用范围：`["buff_or_debuff", "summon_or_spawn"]`
- 世界操作：`[]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/nordic/fenrir_howl_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |
| `spawn_cap_or_cleanup` 召唤上限与清理 | 召唤数量、持续时间、阵营和消失逻辑符合设计。 |
| `balance_repeat_1` 战斗节奏/平衡复测 1 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### nordic:GREAT_HUNT

- 优先级：`2`
- 风险：`medium`
- 目标模式：`caster_position`
- 作用范围：`["buff_or_debuff", "summon_or_spawn"]`
- 世界操作：`[]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/nordic/great_hunt_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |
| `spawn_cap_or_cleanup` 召唤上限与清理 | 召唤数量、持续时间、阵营和消失逻辑符合设计。 |
| `balance_repeat_1` 战斗节奏/平衡复测 1 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### nordic:ODIN_EYE

- 优先级：`2`
- 风险：`medium`
- 目标模式：`target_block_or_crosshair`
- 作用范围：`["buff_or_debuff", "resource_generation"]`
- 世界操作：`[]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/nordic/odin_eye_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |
| `balance_repeat_1` 战斗节奏/平衡复测 1 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |
| `balance_repeat_2` 战斗节奏/平衡复测 2 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### nordic:THOR_WRATH

- 优先级：`2`
- 风险：`medium`
- 目标模式：`target_block_or_crosshair`
- 作用范围：`["damage", "buff_or_debuff", "summon_or_spawn"]`
- 世界操作：`[]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/nordic/thor_wrath_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |
| `spawn_cap_or_cleanup` 召唤上限与清理 | 召唤数量、持续时间、阵营和消失逻辑符合设计。 |
| `balance_repeat_1` 战斗节奏/平衡复测 1 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### nordic:VALHALLA_CALL

- 优先级：`2`
- 风险：`medium`
- 目标模式：`target_block_or_crosshair`
- 作用范围：`["buff_or_debuff", "summon_or_spawn", "resource_generation"]`
- 世界操作：`[]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/nordic/valhalla_call_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |
| `spawn_cap_or_cleanup` 召唤上限与清理 | 召唤数量、持续时间、阵营和消失逻辑符合设计。 |
| `balance_repeat_1` 战斗节奏/平衡复测 1 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### nordic:WALKING_WOODS

- 优先级：`2`
- 风险：`medium`
- 目标模式：`caster_position`
- 作用范围：`["buff_or_debuff", "summon_or_spawn"]`
- 世界操作：`[]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/nordic/walking_woods_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |
| `spawn_cap_or_cleanup` 召唤上限与清理 | 召唤数量、持续时间、阵营和消失逻辑符合设计。 |
| `balance_repeat_1` 战斗节奏/平衡复测 1 | 记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### atlantean:CRYSTAL_BARRIER

- 优先级：`3`
- 风险：`low`
- 目标模式：`caster_centered_area`
- 作用范围：`["buff_or_debuff"]`
- 世界操作：`[]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/atlantean/crystal_barrier_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### atlantean:SIREN_SONG

- 优先级：`3`
- 风险：`low`
- 目标模式：`target_block_or_crosshair`
- 作用范围：`["buff_or_debuff"]`
- 世界操作：`[]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/atlantean/siren_song_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### chinese:RAIN_PRAYER

- 优先级：`3`
- 风险：`low`
- 目标模式：`target_block_or_crosshair`
- 作用范围：`["buff_or_debuff"]`
- 世界操作：`[]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/chinese/rain_prayer_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### chinese:TAIJI_SHIELD

- 优先级：`3`
- 风险：`low`
- 目标模式：`caster_position`
- 作用范围：`["buff_or_debuff"]`
- 世界操作：`[]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/chinese/taiji_shield_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### greek:BRONZE

- 优先级：`3`
- 风险：`low`
- 目标模式：`caster_centered_area`
- 作用范围：`["buff_or_debuff"]`
- 世界操作：`[]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/greek/bronze_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### greek:CEASEFIRE

- 优先级：`3`
- 风险：`low`
- 目标模式：`caster_centered_area`
- 作用范围：`["buff_or_debuff"]`
- 世界操作：`[]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/greek/ceasefire_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### greek:LURE

- 优先级：`3`
- 风险：`low`
- 目标模式：`caster_position`
- 作用范围：`["unknown"]`
- 世界操作：`[]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/greek/lure_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### greek:PESTILENCE

- 优先级：`3`
- 风险：`low`
- 目标模式：`caster_centered_area`
- 作用范围：`["buff_or_debuff"]`
- 世界操作：`[]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/greek/pestilence_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### greek:PLENTY

- 优先级：`3`
- 风险：`low`
- 目标模式：`caster_position`
- 作用范围：`["unknown"]`
- 世界操作：`[]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/greek/plenty_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### greek:RESTORATION

- 优先级：`3`
- 风险：`low`
- 目标模式：`caster_centered_area`
- 作用范围：`["unknown"]`
- 世界操作：`[]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/greek/restoration_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

### nordic:DWARVEN_MINE

- 优先级：`3`
- 风险：`low`
- 目标模式：`target_block_or_crosshair`
- 作用范围：`["buff_or_debuff"]`
- 世界操作：`[]`
- review board：`docs/preview_assets/god_power_gameplay/gameplay_review_boards/nordic/dwarven_mine_gameplay_review.png`

| 场景 | 预期 |
| --- | --- |
| `valid_near_target` 近距离有效目标 | 神力成功触发，反馈清楚，冷却和证明消耗正确。 |
| `valid_far_target` 远距离或边界目标 | 射程/范围边界符合设计；失败时不误扣资源。 |
| `empty_or_invalid_target` 空地/无效目标 | 失败路径有提示，不产生实际效果或错误冷却。 |
| `cooldown_active` 冷却中重复施放 | 不能再次施放；提示冷却，资源不重复扣除。 |
| `insufficient_certificate` 证明不足 | 施放失败且提示资源不足，不产生实际效果。 |

验收要点：
- 分别测试准星命中、空地、近距、远距、实体和无效目标。
- 失败目标必须给出清楚反馈，不能误扣冷却或消耗。
- 记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。
- 同文明同类型神力之间不能出现明显成本和强度倒挂。
- 粒子启动、持续、命中/结束阶段必须可读。
- 音效和提示必须说明成功/失败原因，不能遮挡战斗判断。
- 确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。
- 离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。

## 通过口径

- 每个场景行必须有 pass/fail、实际结果、证据路径和备注。
- 高风险神力必须额外记录性能、存档重启/区块重载和清理结果。
- 任何失败都不能直接豁免；必须回到代码、设计、UI、粒子、音效或 NBT 对应问题单。
- 全部神力通过并回写后，`god_power_playtest_acceptance_not_done` 才能关闭。
