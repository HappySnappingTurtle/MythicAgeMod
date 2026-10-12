# MythicAgeMod 1.0 神力玩法验收包

本文档由 `tools/god_power_gameplay_acceptance_package.py audit` 生成。它把 52 个非神殿生成神力的静态实现覆盖和人工玩法验收分开记录：静态通过不等于已经可发布。

## 总览

- 神力数量：52
- 静态实现契约通过：True
- MC 运行期施放探针通过：True
- MC 运行期施放探针覆盖：52/52
- MC 运行期施放探针报告：`run/visual_probe/god_power_playtest_report.json`
- 玩法验收通过：False
- 需要人工逐项游玩验收：52
- 风险等级统计：`{"high": 18, "low": 11, "medium": 23}`
- 高风险玩法测试项：18
- Runtime smoke 探针覆盖：52/52
- 人工玩法测试队列：52
- 人工玩法测试批次：8
- 人工玩法最低场景预算：446
- 人工玩法队列按优先级：`{"1": 18, "2": 23, "3": 11}`
- 人工玩法队列按风险：`{"high": 18, "low": 11, "medium": 23}`
- 测试维度状态统计：`{"auto_passed": 52, "auto_passed_pending_visual_check": 52, "manual_required": 260, "manual_required_high_focus": 18, "not_applicable": 34}`
- 静态玩法 review board：52 / 52
- Review board 总览图：`docs/preview_assets/god_power_gameplay/gameplay_review_board_contact_sheet.png`
- 状态统计：`{"static_gameplay_contract_passed_pending_playtest": 52}`
- 风险统计：`{"large_world_edit_or_explosion": 18}`
- 世界编辑/爆炸安全审查：`{"manual_safety_review_required_count": 18, "status_counts": {"static_safety_evidence_recorded_pending_manual_review": 18}, "operation_counts": {"break_block": 1, "create_explosion": 1, "earthquake_manager": 1, "explosion_entity_or_damage": 1, "meteor_entity": 1, "set_block_state": 15}}`
- 运行时提示本地化：`{"key_count": 56, "all_lang_ok": true, "missing_by_lang": {}}`

## 文明汇总

| 文明 | 神力数 | 静态通过 | 待人工玩法验收 | 高风险 | 中风险 | 低风险 | 状态 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| greek | 12 | 12 | 12 | 1 | 5 | 6 | `{"static_gameplay_contract_passed_pending_playtest": 12}` |
| chinese | 10 | 10 | 10 | 4 | 4 | 2 | `{"static_gameplay_contract_passed_pending_playtest": 10}` |
| nordic | 10 | 10 | 10 | 3 | 6 | 1 | `{"static_gameplay_contract_passed_pending_playtest": 10}` |
| egyptian | 10 | 10 | 10 | 10 | 0 | 0 | `{"static_gameplay_contract_passed_pending_playtest": 10}` |
| atlantean | 10 | 10 | 10 | 0 | 8 | 2 | `{"static_gameplay_contract_passed_pending_playtest": 10}` |

## 人工玩法测试批次计划

批次按风险和测试价值排序；P1 优先覆盖世界编辑、爆炸、固定成功路径、持续管理器和高影响范围神力。

| 批次 | 优先级 | 风险 | 神力数 | 世界编辑项 | 最低场景 | 神力 | 批次验收重点 |
| --- | ---: | --- | ---: | ---: | ---: | --- | --- |
| P1-high-1 | 1 | `high` | 6 | 6 | 72 | chinese:ABUNDANCE, chinese:EARTH_GOD_BLESSING, chinese:FLOOD, chinese:RECREATION, egyptian:ANUBIS_JUDGMENT, egyptian:DESERT_STORM | 目标选择和失败路径必须覆盖。；粒子/音效/UI 反馈必须可读。；高风险批次必须额外记录性能、存档和清理结果。 |
| P1-high-2 | 1 | `high` | 6 | 6 | 72 | egyptian:EYE_OF_HORUS, egyptian:ISIS_RESURRECTION, egyptian:LOCUST_SWARM, egyptian:METEOR, egyptian:PROSPERITY, egyptian:RA_RADIANCE | 目标选择和失败路径必须覆盖。；粒子/音效/UI 反馈必须可读。；高风险批次必须额外记录性能、存档和清理结果。 |
| P1-high-3 | 1 | `high` | 6 | 6 | 74 | egyptian:SUN_RAIN, egyptian:TORNADO, greek:EARTHQUAKE, nordic:FIMBULWINTER, nordic:FROST, nordic:RAGNAROK | 目标选择和失败路径必须覆盖。；粒子/音效/UI 反馈必须可读。；高风险批次必须额外记录性能、存档和清理结果。 |
| P2-medium-1 | 2 | `medium` | 8 | 0 | 66 | atlantean:ABYSSAL_CURRENT, atlantean:ABYSSAL_RIFT, atlantean:CHRONO_STASIS, atlantean:LEVIATHAN_CALL, atlantean:OCEANIC_WRATH, atlantean:ORICHALCUM_FORGE, atlantean:PEARL_HARVES... | 确认手感、平衡和反馈是否符合设计。；记录任何遮挡、误导或冷却/成本异常。 |
| P2-medium-2 | 2 | `medium` | 8 | 0 | 56 | chinese:CALL_TO_ARMS, chinese:CRANE_BLESSING, chinese:ENLIGHTENMENT, chinese:FIVE_THUNDER, greek:CURSE, greek:LIGHTNING_STORM, greek:LIGHTNING_STORM_HERA, greek:UNDERWORLD_ARMY | 确认手感、平衡和反馈是否符合设计。；记录任何遮挡、误导或冷却/成本异常。 |
| P2-medium-3 | 2 | `medium` | 7 | 0 | 51 | greek:UNDERWORLD_PASSAGE, nordic:FENRIR_HOWL, nordic:GREAT_HUNT, nordic:ODIN_EYE, nordic:THOR_WRATH, nordic:VALHALLA_CALL, nordic:WALKING_WOODS | 确认手感、平衡和反馈是否符合设计。；记录任何遮挡、误导或冷却/成本异常。 |
| P3-low-1 | 3 | `low` | 10 | 0 | 50 | atlantean:CRYSTAL_BARRIER, atlantean:SIREN_SONG, chinese:RAIN_PRAYER, chinese:TAIJI_SHIELD, greek:BRONZE, greek:CEASEFIRE, greek:LURE, greek:PESTILENCE, greek:PLENTY, greek:REST... | 确认手感、平衡和反馈是否符合设计。；记录任何遮挡、误导或冷却/成本异常。 |
| P3-low-2 | 3 | `low` | 1 | 0 | 5 | nordic:DWARVEN_MINE | 确认手感、平衡和反馈是否符合设计。；记录任何遮挡、误导或冷却/成本异常。 |

## 人工玩法测试执行队列

| 优先级 | 文明 | 神力 | 风险 | 最低场景 | 目标 | 范围 | 世界操作 | 验收要点 |
| ---: | --- | --- | --- | ---: | --- | --- | --- | --- |
| 1 | chinese | `ABUNDANCE` | `high` | 12 | `caster_position` | `buff_or_debuff,world_edit` | `set_block_state` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 1 | chinese | `EARTH_GOD_BLESSING` | `high` | 12 | `caster_position` | `buff_or_debuff,world_edit,resource_generation` | `set_block_state` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 1 | chinese | `FLOOD` | `high` | 12 | `target_block_or_crosshair` | `damage,buff_or_debuff,world_edit,resource_generation` | `set_block_state` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 1 | chinese | `RECREATION` | `high` | 12 | `target_block_or_crosshair` | `buff_or_debuff,summon_or_spawn,world_edit,resource_generation` | `set_block_state` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 1 | egyptian | `ANUBIS_JUDGMENT` | `high` | 12 | `target_block_or_crosshair` | `damage,buff_or_debuff,summon_or_spawn,world_edit,resource_generation` | `set_block_state` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 1 | egyptian | `DESERT_STORM` | `high` | 12 | `target_block_or_crosshair` | `damage,buff_or_debuff,world_edit,resource_generation` | `set_block_state` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 1 | egyptian | `EYE_OF_HORUS` | `high` | 12 | `target_block_or_crosshair` | `buff_or_debuff,summon_or_spawn,world_edit,resource_generation` | `set_block_state` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 1 | egyptian | `ISIS_RESURRECTION` | `high` | 12 | `target_block_or_crosshair` | `buff_or_debuff,summon_or_spawn,world_edit,resource_generation` | `set_block_state` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 1 | egyptian | `LOCUST_SWARM` | `high` | 12 | `target_block_or_crosshair` | `damage,buff_or_debuff,summon_or_spawn,world_edit,resource_generation` | `set_block_state` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 1 | egyptian | `METEOR` | `high` | 12 | `target_block_or_crosshair` | `buff_or_debuff` | `meteor_entity` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 1 | egyptian | `PROSPERITY` | `high` | 12 | `target_block_or_crosshair` | `buff_or_debuff,summon_or_spawn,world_edit,resource_generation` | `set_block_state` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 1 | egyptian | `RA_RADIANCE` | `high` | 12 | `target_block_or_crosshair` | `damage,buff_or_debuff,world_edit,resource_generation` | `set_block_state` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 1 | egyptian | `SUN_RAIN` | `high` | 12 | `target_block_or_crosshair` | `buff_or_debuff,world_edit,resource_generation` | `set_block_state` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 1 | egyptian | `TORNADO` | `high` | 14 | `target_block_or_crosshair` | `damage,buff_or_debuff,world_edit,teleport_or_mobility,resource_generation` | `set_block_state,break_block` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 1 | greek | `EARTHQUAKE` | `high` | 12 | `caster_position` | `weather_or_persistent_manager` | `earthquake_manager` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 1 | nordic | `FIMBULWINTER` | `high` | 12 | `target_block_or_crosshair` | `damage,buff_or_debuff,world_edit,weather_or_persistent_manager,resource_generation` | `set_block_state` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 1 | nordic | `FROST` | `high` | 12 | `target_block_or_crosshair` | `damage,buff_or_debuff,world_edit,resource_generation` | `set_block_state` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 1 | nordic | `RAGNAROK` | `high` | 12 | `target_block_or_crosshair` | `damage,buff_or_debuff,summon_or_spawn` | `create_explosion,explosion_entity_or_damage` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 2 | atlantean | `ABYSSAL_CURRENT` | `medium` | 9 | `target_block_or_crosshair` | `buff_or_debuff,teleport_or_mobility` | `none` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 2 | atlantean | `ABYSSAL_RIFT` | `medium` | 9 | `target_block_or_crosshair` | `damage,buff_or_debuff,teleport_or_mobility` | `none` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 2 | atlantean | `CHRONO_STASIS` | `medium` | 9 | `target_block_or_crosshair` | `damage,buff_or_debuff,teleport_or_mobility` | `none` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 2 | atlantean | `LEVIATHAN_CALL` | `medium` | 9 | `target_block_or_crosshair` | `damage,buff_or_debuff,teleport_or_mobility` | `none` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 2 | atlantean | `OCEANIC_WRATH` | `medium` | 7 | `target_block_or_crosshair` | `damage,buff_or_debuff,summon_or_spawn,weather_or_persistent_manager` | `none` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 2 | atlantean | `ORICHALCUM_FORGE` | `medium` | 7 | `target_block_or_crosshair` | `buff_or_debuff,summon_or_spawn,resource_generation` | `none` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 2 | atlantean | `PEARL_HARVEST` | `medium` | 7 | `target_block_or_crosshair` | `buff_or_debuff,summon_or_spawn,resource_generation` | `none` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 2 | atlantean | `TIDAL_SURGE` | `medium` | 9 | `target_block_or_crosshair` | `damage,buff_or_debuff,teleport_or_mobility` | `none` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 2 | chinese | `CALL_TO_ARMS` | `medium` | 7 | `caster_position` | `buff_or_debuff,summon_or_spawn` | `none` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 2 | chinese | `CRANE_BLESSING` | `medium` | 7 | `target_block_or_crosshair` | `buff_or_debuff,summon_or_spawn` | `none` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 2 | chinese | `ENLIGHTENMENT` | `medium` | 7 | `target_block_or_crosshair` | `buff_or_debuff,summon_or_spawn` | `none` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 2 | chinese | `FIVE_THUNDER` | `medium` | 7 | `caster_centered_area` | `damage,buff_or_debuff,summon_or_spawn` | `none` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 2 | greek | `CURSE` | `medium` | 7 | `caster_centered_area` | `summon_or_spawn` | `none` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 2 | greek | `LIGHTNING_STORM` | `medium` | 7 | `caster_position` | `summon_or_spawn,weather_or_persistent_manager` | `none` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 2 | greek | `LIGHTNING_STORM_HERA` | `medium` | 7 | `caster_position` | `summon_or_spawn` | `none` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 2 | greek | `UNDERWORLD_ARMY` | `medium` | 7 | `caster_position` | `summon_or_spawn` | `none` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 2 | greek | `UNDERWORLD_PASSAGE` | `medium` | 9 | `caster_position` | `teleport_or_mobility` | `none` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 2 | nordic | `FENRIR_HOWL` | `medium` | 7 | `target_block_or_crosshair` | `buff_or_debuff,summon_or_spawn` | `none` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 2 | nordic | `GREAT_HUNT` | `medium` | 7 | `caster_position` | `buff_or_debuff,summon_or_spawn` | `none` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 2 | nordic | `ODIN_EYE` | `medium` | 7 | `target_block_or_crosshair` | `buff_or_debuff,resource_generation` | `none` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 2 | nordic | `THOR_WRATH` | `medium` | 7 | `target_block_or_crosshair` | `damage,buff_or_debuff,summon_or_spawn` | `none` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 2 | nordic | `VALHALLA_CALL` | `medium` | 7 | `target_block_or_crosshair` | `buff_or_debuff,summon_or_spawn,resource_generation` | `none` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 2 | nordic | `WALKING_WOODS` | `medium` | 7 | `caster_position` | `buff_or_debuff,summon_or_spawn` | `none` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 3 | atlantean | `CRYSTAL_BARRIER` | `low` | 5 | `caster_centered_area` | `buff_or_debuff` | `none` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 3 | atlantean | `SIREN_SONG` | `low` | 5 | `target_block_or_crosshair` | `buff_or_debuff` | `none` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 3 | chinese | `RAIN_PRAYER` | `low` | 5 | `target_block_or_crosshair` | `buff_or_debuff` | `none` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 3 | chinese | `TAIJI_SHIELD` | `low` | 5 | `caster_position` | `buff_or_debuff` | `none` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 3 | greek | `BRONZE` | `low` | 5 | `caster_centered_area` | `buff_or_debuff` | `none` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 3 | greek | `CEASEFIRE` | `low` | 5 | `caster_centered_area` | `buff_or_debuff` | `none` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 3 | greek | `LURE` | `low` | 5 | `caster_position` | `unknown` | `none` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 3 | greek | `PESTILENCE` | `low` | 5 | `caster_centered_area` | `buff_or_debuff` | `none` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 3 | greek | `PLENTY` | `low` | 5 | `caster_position` | `unknown` | `none` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 3 | greek | `RESTORATION` | `low` | 5 | `caster_centered_area` | `unknown` | `none` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |
| 3 | nordic | `DWARVEN_MINE` | `low` | 5 | `target_block_or_crosshair` | `buff_or_debuff` | `none` | 分别测试准星命中、空地、近距、远距、实体和无效目标。；失败目标必须给出清楚反馈，不能误扣冷却或消耗。；记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。；同文明同类型神力之间不能出现明显成本和强度倒挂。 |

## 单项清单

| 文明 | 神力 | 静态契约 | 风险等级 | Runtime probe | 冷却秒 | 证明成本 | 目标/范围 | 风险 | 首要人工测试重点 | Review board |
| --- | --- | --- | --- | --- | ---: | ---: | --- | --- | --- | --- |
| greek | `LIGHTNING_STORM` | `True` | `medium` | `ok=True 85ms` | 6.0 | 3 | `caster_position` | `none` | 目标模式 `caster_position` 在准星、空地、实体、远近距离和失败目标下是否直观。 | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/greek/lightning_storm_gameplay_review.png` |
| greek | `RESTORATION` | `True` | `low` | `ok=True 237ms` | 9.0 | 2 | `caster_centered_area` | `none` | 目标模式 `caster_centered_area` 在准星、空地、实体、远近距离和失败目标下是否直观。 | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/greek/restoration_gameplay_review.png` |
| greek | `LURE` | `True` | `low` | `ok=True 1751ms` | 5.0 | 2 | `caster_position` | `none` | 目标模式 `caster_position` 在准星、空地、实体、远近距离和失败目标下是否直观。 | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/greek/lure_gameplay_review.png` |
| greek | `UNDERWORLD_ARMY` | `True` | `medium` | `ok=True 659ms` | 15.0 | 5 | `caster_position` | `none` | 目标模式 `caster_position` 在准星、空地、实体、远近距离和失败目标下是否直观。 | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/greek/underworld_army_gameplay_review.png` |
| greek | `PESTILENCE` | `True` | `low` | `ok=True 772ms` | 8.0 | 3 | `caster_centered_area` | `none` | 目标模式 `caster_centered_area` 在准星、空地、实体、远近距离和失败目标下是否直观。 | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/greek/pestilence_gameplay_review.png` |
| greek | `CURSE` | `True` | `medium` | `ok=True 503ms` | 12.0 | 4 | `caster_centered_area` | `none` | 目标模式 `caster_centered_area` 在准星、空地、实体、远近距离和失败目标下是否直观。 | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/greek/curse_gameplay_review.png` |
| greek | `EARTHQUAKE` | `True` | `high` | `ok=True 1563ms` | 20.0 | 5 | `caster_position` | `large_world_edit_or_explosion` | 优先复核风险标记：large_world_edit_or_explosion | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/greek/earthquake_gameplay_review.png` |
| greek | `CEASEFIRE` | `True` | `low` | `ok=True 151ms` | 25.0 | 3 | `caster_centered_area` | `none` | 目标模式 `caster_centered_area` 在准星、空地、实体、远近距离和失败目标下是否直观。 | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/greek/ceasefire_gameplay_review.png` |
| greek | `BRONZE` | `True` | `low` | `ok=True 130ms` | 10.0 | 3 | `caster_centered_area` | `none` | 目标模式 `caster_centered_area` 在准星、空地、实体、远近距离和失败目标下是否直观。 | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/greek/bronze_gameplay_review.png` |
| greek | `UNDERWORLD_PASSAGE` | `True` | `medium` | `ok=True 50ms` | 15.0 | 7 | `caster_position` | `none` | 目标模式 `caster_position` 在准星、空地、实体、远近距离和失败目标下是否直观。 | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/greek/underworld_passage_gameplay_review.png` |
| greek | `LIGHTNING_STORM_HERA` | `True` | `medium` | `ok=True 437ms` | 18.0 | 4 | `caster_position` | `none` | 目标模式 `caster_position` 在准星、空地、实体、远近距离和失败目标下是否直观。 | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/greek/lightning_storm_hera_gameplay_review.png` |
| greek | `PLENTY` | `True` | `low` | `ok=True 64ms` | 30.0 | 5 | `caster_position` | `none` | 目标模式 `caster_position` 在准星、空地、实体、远近距离和失败目标下是否直观。 | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/greek/plenty_gameplay_review.png` |
| chinese | `RAIN_PRAYER` | `True` | `low` | `ok=True 28ms` | 8.0 | 2 | `target_block_or_crosshair` | `none` | 目标模式 `target_block_or_crosshair` 在准星、空地、实体、远近距离和失败目标下是否直观。 | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/chinese/rain_prayer_gameplay_review.png` |
| chinese | `EARTH_GOD_BLESSING` | `True` | `high` | `ok=True 30ms` | 10.0 | 3 | `caster_position` | `large_world_edit_or_explosion` | 优先复核风险标记：large_world_edit_or_explosion | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/chinese/earth_god_blessing_gameplay_review.png` |
| chinese | `ENLIGHTENMENT` | `True` | `medium` | `ok=True 118ms` | 6.0 | 4 | `target_block_or_crosshair` | `none` | 目标模式 `target_block_or_crosshair` 在准星、空地、实体、远近距离和失败目标下是否直观。 | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/chinese/enlightenment_gameplay_review.png` |
| chinese | `ABUNDANCE` | `True` | `high` | `ok=True 29ms` | 10.0 | 5 | `caster_position` | `large_world_edit_or_explosion` | 优先复核风险标记：large_world_edit_or_explosion | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/chinese/abundance_gameplay_review.png` |
| chinese | `FIVE_THUNDER` | `True` | `medium` | `ok=True 9ms` | 12.0 | 4 | `caster_centered_area` | `none` | 目标模式 `caster_centered_area` 在准星、空地、实体、远近距离和失败目标下是否直观。 | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/chinese/five_thunder_gameplay_review.png` |
| chinese | `CRANE_BLESSING` | `True` | `medium` | `ok=True 47ms` | 15.0 | 5 | `target_block_or_crosshair` | `none` | 目标模式 `target_block_or_crosshair` 在准星、空地、实体、远近距离和失败目标下是否直观。 | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/chinese/crane_blessing_gameplay_review.png` |
| chinese | `FLOOD` | `True` | `high` | `ok=True 20ms` | 20.0 | 6 | `target_block_or_crosshair` | `large_world_edit_or_explosion` | 优先复核风险标记：large_world_edit_or_explosion | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/chinese/flood_gameplay_review.png` |
| chinese | `CALL_TO_ARMS` | `True` | `medium` | `ok=True 268ms` | 18.0 | 6 | `caster_position` | `none` | 目标模式 `caster_position` 在准星、空地、实体、远近距离和失败目标下是否直观。 | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/chinese/call_to_arms_gameplay_review.png` |
| chinese | `TAIJI_SHIELD` | `True` | `low` | `ok=True 24ms` | 25.0 | 7 | `caster_position` | `none` | 目标模式 `caster_position` 在准星、空地、实体、远近距离和失败目标下是否直观。 | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/chinese/taiji_shield_gameplay_review.png` |
| chinese | `RECREATION` | `True` | `high` | `ok=True 95ms` | 30.0 | 8 | `target_block_or_crosshair` | `large_world_edit_or_explosion` | 优先复核风险标记：large_world_edit_or_explosion | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/chinese/recreation_gameplay_review.png` |
| nordic | `FENRIR_HOWL` | `True` | `medium` | `ok=True 56ms` | 12.0 | 2 | `target_block_or_crosshair` | `none` | 目标模式 `target_block_or_crosshair` 在准星、空地、实体、远近距离和失败目标下是否直观。 | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/nordic/fenrir_howl_gameplay_review.png` |
| nordic | `ODIN_EYE` | `True` | `medium` | `ok=True 123ms` | 18.0 | 3 | `target_block_or_crosshair` | `none` | 目标模式 `target_block_or_crosshair` 在准星、空地、实体、远近距离和失败目标下是否直观。 | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/nordic/odin_eye_gameplay_review.png` |
| nordic | `GREAT_HUNT` | `True` | `medium` | `ok=True 259ms` | 10.0 | 4 | `caster_position` | `none` | 目标模式 `caster_position` 在准星、空地、实体、远近距离和失败目标下是否直观。 | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/nordic/great_hunt_gameplay_review.png` |
| nordic | `DWARVEN_MINE` | `True` | `low` | `ok=True 113ms` | 12.0 | 5 | `target_block_or_crosshair` | `none` | 目标模式 `target_block_or_crosshair` 在准星、空地、实体、远近距离和失败目标下是否直观。 | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/nordic/dwarven_mine_gameplay_review.png` |
| nordic | `THOR_WRATH` | `True` | `medium` | `ok=True 242ms` | 15.0 | 4 | `target_block_or_crosshair` | `none` | 目标模式 `target_block_or_crosshair` 在准星、空地、实体、远近距离和失败目标下是否直观。 | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/nordic/thor_wrath_gameplay_review.png` |
| nordic | `VALHALLA_CALL` | `True` | `medium` | `ok=True 305ms` | 18.0 | 5 | `target_block_or_crosshair` | `none` | 目标模式 `target_block_or_crosshair` 在准星、空地、实体、远近距离和失败目标下是否直观。 | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/nordic/valhalla_call_gameplay_review.png` |
| nordic | `FROST` | `True` | `high` | `ok=True 113ms` | 16.0 | 6 | `target_block_or_crosshair` | `large_world_edit_or_explosion` | 优先复核风险标记：large_world_edit_or_explosion | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/nordic/frost_gameplay_review.png` |
| nordic | `WALKING_WOODS` | `True` | `medium` | `ok=True 250ms` | 20.0 | 6 | `caster_position` | `none` | 目标模式 `caster_position` 在准星、空地、实体、远近距离和失败目标下是否直观。 | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/nordic/walking_woods_gameplay_review.png` |
| nordic | `RAGNAROK` | `True` | `high` | `ok=True 658ms` | 25.0 | 7 | `target_block_or_crosshair` | `large_world_edit_or_explosion` | 优先复核风险标记：large_world_edit_or_explosion | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/nordic/ragnarok_gameplay_review.png` |
| nordic | `FIMBULWINTER` | `True` | `high` | `ok=True 88ms` | 30.0 | 8 | `target_block_or_crosshair` | `large_world_edit_or_explosion` | 优先复核风险标记：large_world_edit_or_explosion | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/nordic/fimbulwinter_gameplay_review.png` |
| egyptian | `RA_RADIANCE` | `True` | `high` | `ok=True 6ms` | 10.0 | 2 | `target_block_or_crosshair` | `large_world_edit_or_explosion` | 优先复核风险标记：large_world_edit_or_explosion | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/egyptian/ra_radiance_gameplay_review.png` |
| egyptian | `DESERT_STORM` | `True` | `high` | `ok=True 313ms` | 8.0 | 3 | `target_block_or_crosshair` | `large_world_edit_or_explosion` | 优先复核风险标记：large_world_edit_or_explosion | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/egyptian/desert_storm_gameplay_review.png` |
| egyptian | `SUN_RAIN` | `True` | `high` | `ok=True 132ms` | 8.0 | 4 | `target_block_or_crosshair` | `large_world_edit_or_explosion` | 优先复核风险标记：large_world_edit_or_explosion | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/egyptian/sun_rain_gameplay_review.png` |
| egyptian | `PROSPERITY` | `True` | `high` | `ok=True 124ms` | 12.0 | 4 | `target_block_or_crosshair` | `large_world_edit_or_explosion` | 优先复核风险标记：large_world_edit_or_explosion | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/egyptian/prosperity_gameplay_review.png` |
| egyptian | `EYE_OF_HORUS` | `True` | `high` | `ok=True 62ms` | 15.0 | 3 | `target_block_or_crosshair` | `large_world_edit_or_explosion` | 优先复核风险标记：large_world_edit_or_explosion | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/egyptian/eye_of_horus_gameplay_review.png` |
| egyptian | `ANUBIS_JUDGMENT` | `True` | `high` | `ok=True 119ms` | 18.0 | 5 | `target_block_or_crosshair` | `large_world_edit_or_explosion` | 优先复核风险标记：large_world_edit_or_explosion | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/egyptian/anubis_judgment_gameplay_review.png` |
| egyptian | `LOCUST_SWARM` | `True` | `high` | `ok=True 99ms` | 20.0 | 5 | `target_block_or_crosshair` | `large_world_edit_or_explosion` | 优先复核风险标记：large_world_edit_or_explosion | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/egyptian/locust_swarm_gameplay_review.png` |
| egyptian | `TORNADO` | `True` | `high` | `ok=True 113ms` | 22.0 | 6 | `target_block_or_crosshair` | `large_world_edit_or_explosion` | 优先复核风险标记：large_world_edit_or_explosion | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/egyptian/tornado_gameplay_review.png` |
| egyptian | `ISIS_RESURRECTION` | `True` | `high` | `ok=True 117ms` | 25.0 | 7 | `target_block_or_crosshair` | `large_world_edit_or_explosion` | 优先复核风险标记：large_world_edit_or_explosion | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/egyptian/isis_resurrection_gameplay_review.png` |
| egyptian | `METEOR` | `True` | `high` | `ok=True 170ms` | 30.0 | 8 | `target_block_or_crosshair` | `large_world_edit_or_explosion` | 优先复核风险标记：large_world_edit_or_explosion | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/egyptian/meteor_gameplay_review.png` |
| atlantean | `TIDAL_SURGE` | `True` | `medium` | `ok=True 119ms` | 9.0 | 2 | `target_block_or_crosshair` | `none` | 目标模式 `target_block_or_crosshair` 在准星、空地、实体、远近距离和失败目标下是否直观。 | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/atlantean/tidal_surge_gameplay_review.png` |
| atlantean | `ABYSSAL_CURRENT` | `True` | `medium` | `ok=True 104ms` | 10.0 | 3 | `target_block_or_crosshair` | `none` | 目标模式 `target_block_or_crosshair` 在准星、空地、实体、远近距离和失败目标下是否直观。 | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/atlantean/abyssal_current_gameplay_review.png` |
| atlantean | `CRYSTAL_BARRIER` | `True` | `low` | `ok=True 135ms` | 11.0 | 4 | `caster_centered_area` | `none` | 目标模式 `caster_centered_area` 在准星、空地、实体、远近距离和失败目标下是否直观。 | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/atlantean/crystal_barrier_gameplay_review.png` |
| atlantean | `PEARL_HARVEST` | `True` | `medium` | `ok=True 81ms` | 13.0 | 4 | `target_block_or_crosshair` | `none` | 目标模式 `target_block_or_crosshair` 在准星、空地、实体、远近距离和失败目标下是否直观。 | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/atlantean/pearl_harvest_gameplay_review.png` |
| atlantean | `ORICHALCUM_FORGE` | `True` | `medium` | `ok=True 104ms` | 14.0 | 5 | `target_block_or_crosshair` | `none` | 目标模式 `target_block_or_crosshair` 在准星、空地、实体、远近距离和失败目标下是否直观。 | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/atlantean/orichalcum_forge_gameplay_review.png` |
| atlantean | `SIREN_SONG` | `True` | `low` | `ok=True 100ms` | 16.0 | 6 | `target_block_or_crosshair` | `none` | 目标模式 `target_block_or_crosshair` 在准星、空地、实体、远近距离和失败目标下是否直观。 | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/atlantean/siren_song_gameplay_review.png` |
| atlantean | `LEVIATHAN_CALL` | `True` | `medium` | `ok=True 1013ms` | 20.0 | 7 | `target_block_or_crosshair` | `none` | 目标模式 `target_block_or_crosshair` 在准星、空地、实体、远近距离和失败目标下是否直观。 | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/atlantean/leviathan_call_gameplay_review.png` |
| atlantean | `ABYSSAL_RIFT` | `True` | `medium` | `ok=True 106ms` | 22.0 | 7 | `target_block_or_crosshair` | `none` | 目标模式 `target_block_or_crosshair` 在准星、空地、实体、远近距离和失败目标下是否直观。 | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/atlantean/abyssal_rift_gameplay_review.png` |
| atlantean | `CHRONO_STASIS` | `True` | `medium` | `ok=True 77ms` | 27.0 | 8 | `target_block_or_crosshair` | `none` | 目标模式 `target_block_or_crosshair` 在准星、空地、实体、远近距离和失败目标下是否直观。 | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/atlantean/chrono_stasis_gameplay_review.png` |
| atlantean | `OCEANIC_WRATH` | `True` | `medium` | `ok=True 328ms` | 30.0 | 8 | `target_block_or_crosshair` | `none` | 目标模式 `target_block_or_crosshair` 在准星、空地、实体、远近距离和失败目标下是否直观。 | `docs/preview_assets/god_power_gameplay/gameplay_review_boards/atlantean/oceanic_wrath_gameplay_review.png` |

## 高风险玩法测试优先级

| 文明 | 神力 | 风险标记 | 效果范围 | 世界操作 | 测试重点 |
| --- | --- | --- | --- | --- | --- |
| greek | `EARTHQUAKE` | `large_world_edit_or_explosion` | `weather_or_persistent_manager` | `earthquake_manager` | 优先复核风险标记：large_world_edit_or_explosion；目标模式 `caster_position` 在准星、空地、实体、远近距离和失败目标下是否直观。；冷却、证明消耗、影响范围、持续时间、召唤数量或伤害强度是否与同文明神力和战斗节奏匹配。；成功/失败提示、音效、粒子启动/持续/结束阶段是否可读，是否遮挡战斗或误导玩家。 |
| chinese | `EARTH_GOD_BLESSING` | `large_world_edit_or_explosion` | `buff_or_debuff,world_edit,resource_generation` | `set_block_state` | 优先复核风险标记：large_world_edit_or_explosion；目标模式 `caster_position` 在准星、空地、实体、远近距离和失败目标下是否直观。；冷却、证明消耗、影响范围、持续时间、召唤数量或伤害强度是否与同文明神力和战斗节奏匹配。；成功/失败提示、音效、粒子启动/持续/结束阶段是否可读，是否遮挡战斗或误导玩家。 |
| chinese | `ABUNDANCE` | `large_world_edit_or_explosion` | `buff_or_debuff,world_edit` | `set_block_state` | 优先复核风险标记：large_world_edit_or_explosion；目标模式 `caster_position` 在准星、空地、实体、远近距离和失败目标下是否直观。；冷却、证明消耗、影响范围、持续时间、召唤数量或伤害强度是否与同文明神力和战斗节奏匹配。；成功/失败提示、音效、粒子启动/持续/结束阶段是否可读，是否遮挡战斗或误导玩家。 |
| chinese | `FLOOD` | `large_world_edit_or_explosion` | `damage,buff_or_debuff,world_edit,resource_generation` | `set_block_state` | 优先复核风险标记：large_world_edit_or_explosion；目标模式 `target_block_or_crosshair` 在准星、空地、实体、远近距离和失败目标下是否直观。；冷却、证明消耗、影响范围、持续时间、召唤数量或伤害强度是否与同文明神力和战斗节奏匹配。；成功/失败提示、音效、粒子启动/持续/结束阶段是否可读，是否遮挡战斗或误导玩家。 |
| chinese | `RECREATION` | `large_world_edit_or_explosion` | `buff_or_debuff,summon_or_spawn,world_edit,resource_generation` | `set_block_state` | 优先复核风险标记：large_world_edit_or_explosion；目标模式 `target_block_or_crosshair` 在准星、空地、实体、远近距离和失败目标下是否直观。；冷却、证明消耗、影响范围、持续时间、召唤数量或伤害强度是否与同文明神力和战斗节奏匹配。；成功/失败提示、音效、粒子启动/持续/结束阶段是否可读，是否遮挡战斗或误导玩家。 |
| nordic | `FROST` | `large_world_edit_or_explosion` | `damage,buff_or_debuff,world_edit,resource_generation` | `set_block_state` | 优先复核风险标记：large_world_edit_or_explosion；目标模式 `target_block_or_crosshair` 在准星、空地、实体、远近距离和失败目标下是否直观。；冷却、证明消耗、影响范围、持续时间、召唤数量或伤害强度是否与同文明神力和战斗节奏匹配。；成功/失败提示、音效、粒子启动/持续/结束阶段是否可读，是否遮挡战斗或误导玩家。 |
| nordic | `RAGNAROK` | `large_world_edit_or_explosion` | `damage,buff_or_debuff,summon_or_spawn` | `create_explosion,explosion_entity_or_damage` | 优先复核风险标记：large_world_edit_or_explosion；目标模式 `target_block_or_crosshair` 在准星、空地、实体、远近距离和失败目标下是否直观。；冷却、证明消耗、影响范围、持续时间、召唤数量或伤害强度是否与同文明神力和战斗节奏匹配。；成功/失败提示、音效、粒子启动/持续/结束阶段是否可读，是否遮挡战斗或误导玩家。 |
| nordic | `FIMBULWINTER` | `large_world_edit_or_explosion` | `damage,buff_or_debuff,world_edit,weather_or_persistent_manager,resource_generation` | `set_block_state` | 优先复核风险标记：large_world_edit_or_explosion；目标模式 `target_block_or_crosshair` 在准星、空地、实体、远近距离和失败目标下是否直观。；冷却、证明消耗、影响范围、持续时间、召唤数量或伤害强度是否与同文明神力和战斗节奏匹配。；成功/失败提示、音效、粒子启动/持续/结束阶段是否可读，是否遮挡战斗或误导玩家。 |
| egyptian | `RA_RADIANCE` | `large_world_edit_or_explosion` | `damage,buff_or_debuff,world_edit,resource_generation` | `set_block_state` | 优先复核风险标记：large_world_edit_or_explosion；目标模式 `target_block_or_crosshair` 在准星、空地、实体、远近距离和失败目标下是否直观。；冷却、证明消耗、影响范围、持续时间、召唤数量或伤害强度是否与同文明神力和战斗节奏匹配。；成功/失败提示、音效、粒子启动/持续/结束阶段是否可读，是否遮挡战斗或误导玩家。 |
| egyptian | `DESERT_STORM` | `large_world_edit_or_explosion` | `damage,buff_or_debuff,world_edit,resource_generation` | `set_block_state` | 优先复核风险标记：large_world_edit_or_explosion；目标模式 `target_block_or_crosshair` 在准星、空地、实体、远近距离和失败目标下是否直观。；冷却、证明消耗、影响范围、持续时间、召唤数量或伤害强度是否与同文明神力和战斗节奏匹配。；成功/失败提示、音效、粒子启动/持续/结束阶段是否可读，是否遮挡战斗或误导玩家。 |
| egyptian | `SUN_RAIN` | `large_world_edit_or_explosion` | `buff_or_debuff,world_edit,resource_generation` | `set_block_state` | 优先复核风险标记：large_world_edit_or_explosion；目标模式 `target_block_or_crosshair` 在准星、空地、实体、远近距离和失败目标下是否直观。；冷却、证明消耗、影响范围、持续时间、召唤数量或伤害强度是否与同文明神力和战斗节奏匹配。；成功/失败提示、音效、粒子启动/持续/结束阶段是否可读，是否遮挡战斗或误导玩家。 |
| egyptian | `PROSPERITY` | `large_world_edit_or_explosion` | `buff_or_debuff,summon_or_spawn,world_edit,resource_generation` | `set_block_state` | 优先复核风险标记：large_world_edit_or_explosion；目标模式 `target_block_or_crosshair` 在准星、空地、实体、远近距离和失败目标下是否直观。；冷却、证明消耗、影响范围、持续时间、召唤数量或伤害强度是否与同文明神力和战斗节奏匹配。；成功/失败提示、音效、粒子启动/持续/结束阶段是否可读，是否遮挡战斗或误导玩家。 |
| egyptian | `EYE_OF_HORUS` | `large_world_edit_or_explosion` | `buff_or_debuff,summon_or_spawn,world_edit,resource_generation` | `set_block_state` | 优先复核风险标记：large_world_edit_or_explosion；目标模式 `target_block_or_crosshair` 在准星、空地、实体、远近距离和失败目标下是否直观。；冷却、证明消耗、影响范围、持续时间、召唤数量或伤害强度是否与同文明神力和战斗节奏匹配。；成功/失败提示、音效、粒子启动/持续/结束阶段是否可读，是否遮挡战斗或误导玩家。 |
| egyptian | `ANUBIS_JUDGMENT` | `large_world_edit_or_explosion` | `damage,buff_or_debuff,summon_or_spawn,world_edit,resource_generation` | `set_block_state` | 优先复核风险标记：large_world_edit_or_explosion；目标模式 `target_block_or_crosshair` 在准星、空地、实体、远近距离和失败目标下是否直观。；冷却、证明消耗、影响范围、持续时间、召唤数量或伤害强度是否与同文明神力和战斗节奏匹配。；成功/失败提示、音效、粒子启动/持续/结束阶段是否可读，是否遮挡战斗或误导玩家。 |
| egyptian | `LOCUST_SWARM` | `large_world_edit_or_explosion` | `damage,buff_or_debuff,summon_or_spawn,world_edit,resource_generation` | `set_block_state` | 优先复核风险标记：large_world_edit_or_explosion；目标模式 `target_block_or_crosshair` 在准星、空地、实体、远近距离和失败目标下是否直观。；冷却、证明消耗、影响范围、持续时间、召唤数量或伤害强度是否与同文明神力和战斗节奏匹配。；成功/失败提示、音效、粒子启动/持续/结束阶段是否可读，是否遮挡战斗或误导玩家。 |
| egyptian | `TORNADO` | `large_world_edit_or_explosion` | `damage,buff_or_debuff,world_edit,teleport_or_mobility,resource_generation` | `set_block_state,break_block` | 优先复核风险标记：large_world_edit_or_explosion；目标模式 `target_block_or_crosshair` 在准星、空地、实体、远近距离和失败目标下是否直观。；冷却、证明消耗、影响范围、持续时间、召唤数量或伤害强度是否与同文明神力和战斗节奏匹配。；成功/失败提示、音效、粒子启动/持续/结束阶段是否可读，是否遮挡战斗或误导玩家。 |
| egyptian | `ISIS_RESURRECTION` | `large_world_edit_or_explosion` | `buff_or_debuff,summon_or_spawn,world_edit,resource_generation` | `set_block_state` | 优先复核风险标记：large_world_edit_or_explosion；目标模式 `target_block_or_crosshair` 在准星、空地、实体、远近距离和失败目标下是否直观。；冷却、证明消耗、影响范围、持续时间、召唤数量或伤害强度是否与同文明神力和战斗节奏匹配。；成功/失败提示、音效、粒子启动/持续/结束阶段是否可读，是否遮挡战斗或误导玩家。 |
| egyptian | `METEOR` | `large_world_edit_or_explosion` | `buff_or_debuff` | `meteor_entity` | 优先复核风险标记：large_world_edit_or_explosion；目标模式 `target_block_or_crosshair` 在准星、空地、实体、远近距离和失败目标下是否直观。；冷却、证明消耗、影响范围、持续时间、召唤数量或伤害强度是否与同文明神力和战斗节奏匹配。；成功/失败提示、音效、粒子启动/持续/结束阶段是否可读，是否遮挡战斗或误导玩家。 |

## 世界编辑/爆炸安全审查

| 文明 | 神力 | 操作 | 静态安全状态 | 保护证据 |
| --- | --- | --- | --- | --- |
| greek | `EARTHQUAKE` | `earthquake_manager` | `static_safety_evidence_recorded_pending_manual_review` | `server_side_guard_present,thread_free,persistent_manager_used,bounded_area_indicators` |
| chinese | `EARTH_GOD_BLESSING` | `set_block_state` | `static_safety_evidence_recorded_pending_manual_review` | `server_side_guard_present,thread_free,bounded_area_indicators` |
| chinese | `ABUNDANCE` | `set_block_state` | `static_safety_evidence_recorded_pending_manual_review` | `server_side_guard_present,thread_free,bounded_area_indicators` |
| chinese | `FLOOD` | `set_block_state` | `static_safety_evidence_recorded_pending_manual_review` | `server_side_guard_present,thread_free,bounded_area_indicators` |
| chinese | `RECREATION` | `set_block_state` | `static_safety_evidence_recorded_pending_manual_review` | `server_side_guard_present,thread_free,bounded_area_indicators` |
| nordic | `FROST` | `set_block_state` | `static_safety_evidence_recorded_pending_manual_review` | `server_side_guard_present,thread_free,bounded_area_indicators` |
| nordic | `RAGNAROK` | `create_explosion,explosion_entity_or_damage` | `static_safety_evidence_recorded_pending_manual_review` | `server_side_guard_present,thread_free,bounded_area_indicators` |
| nordic | `FIMBULWINTER` | `set_block_state` | `static_safety_evidence_recorded_pending_manual_review` | `server_side_guard_present,thread_free,persistent_manager_used,bounded_area_indicators` |
| egyptian | `RA_RADIANCE` | `set_block_state` | `static_safety_evidence_recorded_pending_manual_review` | `server_side_guard_present,thread_free,bounded_area_indicators` |
| egyptian | `DESERT_STORM` | `set_block_state` | `static_safety_evidence_recorded_pending_manual_review` | `server_side_guard_present,thread_free,bounded_area_indicators` |
| egyptian | `SUN_RAIN` | `set_block_state` | `static_safety_evidence_recorded_pending_manual_review` | `server_side_guard_present,thread_free,bounded_area_indicators` |
| egyptian | `PROSPERITY` | `set_block_state` | `static_safety_evidence_recorded_pending_manual_review` | `server_side_guard_present,thread_free,bounded_area_indicators` |
| egyptian | `EYE_OF_HORUS` | `set_block_state` | `static_safety_evidence_recorded_pending_manual_review` | `server_side_guard_present,thread_free,preflight_space_or_bounds_check,bounded_area_indicators` |
| egyptian | `ANUBIS_JUDGMENT` | `set_block_state` | `static_safety_evidence_recorded_pending_manual_review` | `server_side_guard_present,thread_free,bounded_area_indicators` |
| egyptian | `LOCUST_SWARM` | `set_block_state` | `static_safety_evidence_recorded_pending_manual_review` | `server_side_guard_present,thread_free,bounded_area_indicators` |
| egyptian | `TORNADO` | `set_block_state,break_block` | `static_safety_evidence_recorded_pending_manual_review` | `server_side_guard_present,thread_free,hard_block_protection,bounded_area_indicators` |
| egyptian | `ISIS_RESURRECTION` | `set_block_state` | `static_safety_evidence_recorded_pending_manual_review` | `server_side_guard_present,thread_free,bounded_area_indicators` |
| egyptian | `METEOR` | `meteor_entity` | `static_safety_evidence_recorded_pending_manual_review` | `server_side_guard_present,thread_free,tick_scheduler_used,bounded_area_indicators` |

## 验收规则

- `static_contract_ok=true` 只说明注册、施法路由、冷却/提示、神力图鉴、主语言 tooltip 和粒子矩阵存在。
- `runtime_smoke_playtest_ok=true` 只说明 52 个神力在受控 MC 运行期探针中能完成一次服务端施放。
- `gameplay_acceptance_ok=false` 表示 52 个神力仍未经过逐项实机手感、平衡、失败路径、性能和可读性验收。
- `void_cast_always_success` 表示该神力路由调用 void 方法后固定成功，需要人工确认无目标、无敌人或不可执行场景下是否应该失败。
- 若后续再次出现 `threaded_delayed_effect`，必须迁移到服务端 tick 调度或持久化管理器。
- `large_world_edit_or_explosion` 需要额外检查服务端 tick 安全、存档安全和性能。
- 若 `supported_language_release_gap` 再次出现，发布前必须补齐所有声明支持语言的名称和 tooltip。
