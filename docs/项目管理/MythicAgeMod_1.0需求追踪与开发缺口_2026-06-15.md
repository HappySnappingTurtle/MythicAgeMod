# MythicAgeMod 1.0 需求追踪与开发缺口（2026-06-15）

本文档由 `tools/mythic_age_1_0_requirement_trace.py audit` 生成。它把用户原始 1.0 目标映射到当前证据、缺口和下一步，防止只看单个 gate 就误判完成。

## 总览

- 需求追踪通过：`False`
- 1.0 就绪：`False`
- 需求数量：12
- 未完成需求：11
- 阻断需求：3
- 状态统计：`{"blocked": 3, "manual_acceptance_pending": 6, "static_pass_runtime_pending": 2, "verified_with_watchlist": 1}`
- release blockers：`["human_visual_acceptance_not_done", "creature_motion_acceptance_not_done", "creature_audio_art_final_pending", "god_power_playtest_acceptance_not_done"]`
- 判定规则：A 1.0 requirement is done only when current authoritative evidence proves the requested product surface is complete, not merely registered or scaffolded.

## 需求矩阵

| ID | 优先级 | 状态 | 用户目标 |
| --- | --- | --- | --- |
| `scope_five_civilizations` | `P0` | `static_pass_runtime_pending` | 1.0 必须是五文明基础可玩版本，不是只有希腊生物。 |
| `code_gameplay_systems` | `P1` | `manual_acceptance_pending` | 代码与玩法要形成五文明闭环：实体、AI、神力、证书经济、守护者交易和失败路径。 |
| `creature_design_models_art` | `P0` | `blocked` | 模型与美术必须达到 1.0 质量，不能只有不好看的希腊生物；天马和九头蛇不能继续沿用畸形设计。 |
| `non_greek_creature_completion` | `P1` | `manual_acceptance_pending` | 埃及、北欧、中国、亚特兰蒂斯不能只是占位，必须有各自文明生物成品级模型和美术。 |
| `textures_uv_materials` | `P0` | `manual_acceptance_pending` | 纹理必须 UV 驱动，眼、嘴、角、翼、尾、符号等关键细节不能空白、错位或随机。 |
| `actions_animation_motion` | `P1` | `blocked` | 动作要覆盖待机、移动、攻击、特殊、受击、死亡，以及飞行/骑乘/多头等特殊动作。 |
| `ui_player_guidance` | `P1` | `static_pass_runtime_pending` | UI 要让玩家看懂文明、证书、神力、交易、冷却、失败原因和进度。 |
| `particles_vfx` | `P1` | `manual_acceptance_pending` | 粒子效果要覆盖神力和生物技能，具有文明主题、三段式反馈和性能降级。 |
| `art_direction_structures_world` | `P1` | `manual_acceptance_pending` | 美术不仅是生物，还包括五文明建筑、方块、物品、UI 和粒子的统一视觉语言。 |
| `audio_feedback` | `P1` | `manual_acceptance_pending` | 音效和反馈要支撑生物、神力、UI、证书和战斗读秒，不只技术可播放。 |
| `nbt_persistence` | `P1` | `verified_with_watchlist` | NBT 结构要正确覆盖长期状态；新增持久化状态必须有 schema、probe 和真实重启回归。 |
| `release_management` | `P0` | `blocked` | 1.0 大版本要有统一的发布口径、回归命令、玩家指南和退出标准。 |

## 详细追踪

### scope_five_civilizations

- 优先级：`P0`
- 状态：`static_pass_runtime_pending`
- 是否满足 1.0：`False`
- 用户目标：1.0 必须是五文明基础可玩版本，不是只有希腊生物。

当前证据：
- civilizations=['greek', 'egyptian', 'nordic', 'chinese', 'atlantean']
- civilization_creature_count=30
- creature_roster={'greek': ['minotaur', 'cyclops', 'centaur', 'hydra', 'medusa', 'nemean_lion', 'manticore', 'chimera', 'colossus', 'pegasus'], 'egyptian': ['anubis_warrior', 'scarab_guardian', 'mummy_soldier', 'sphinx_sentinel', 'sun_falcon'], 'nordic': ['frost_giant', 'draugr_warrior', 'valkyrie', 'fenrir_wolf', 'dwarf_berserker'], 'chinese': ['azure_dragon', 'qilin_guardian', 'jiangshi', 'nine_tailed_fox', 'stone_lion_guardian'], 'atlantean': ['crystal_leviathan', 'orichalcum_golem', 'siren_priestess', 'tidal_guardian', 'abyssal_ray']}
- structure canonical=15/15
- god power review boards=52

缺口或弱证据：
- 五文明内容虽然有静态资源和注册证据，但人工玩法、美术、动作、粒子和结构实机验收未闭环。
- 当前 release blockers=['human_visual_acceptance_not_done', 'creature_motion_acceptance_not_done', 'creature_audio_art_final_pending', 'god_power_playtest_acceptance_not_done']

下一步：
- 保留五文明 30 生物、52 神力、15 canonical 神殿作为 1.0 范围，不再缩成希腊-only。
- 每个文明按同一套设计、模型、动作、纹理、UI/反馈、粒子、NBT 退出标准推进。

权威证据：
- `docs/preview_assets/1_0_gap_audit/gap_report.json`
- `docs/preview_assets/creature_design_first_gate/creature_design_first_gate.json`
- `docs/preview_assets/structure_validation/structure_report.json`
- `docs/preview_assets/god_power_gameplay/god_power_gameplay_acceptance_package.json`

### code_gameplay_systems

- 优先级：`P1`
- 状态：`manual_acceptance_pending`
- 是否满足 1.0：`False`
- 用户目标：代码与玩法要形成五文明闭环：实体、AI、神力、证书经济、守护者交易和失败路径。

当前证据：
- entity_manifest_count=43
- certificate_economy_ok=True
- guide_spell_total=52
- custom_guardian_spell_trade_total=56
- god_power_static_implementation_ok=True
- god_power_runtime_smoke=52/52
- god_power_manual_execution=manual_playtest_not_done batches=8 scenarios=446
- creature_gameplay_static_contract=True (30/30)
- creature_gameplay_release_ready=0/30

缺口或弱证据：
- 52 个神力仍需人工玩法验收。
- 30 生物静态代码闭环已经可审计，但设计/视觉/动作发布依赖未过，不能算玩法发布 ready。
- 证书经济通过静态矩阵，但实机频率、防刷和价格平衡还未作为发布验收闭环。

下一步：
- 先验收 18 个高风险神力的性能、失败路径、回滚/降级和跨存档语义。
- 按神力手工玩法测试执行队列逐批记录 52 个神力的场景结果。
- 为 30 生物建立生成/召唤/掉落/AI 玩法复查表。
- 用实机交易流程验证证书余额、冷却、防刷和 UI 提示。

权威证据：
- `docs/preview_assets/god_power_gameplay/god_power_gameplay_acceptance_package.json`
- `docs/preview_assets/god_power_gameplay/manual_playtest_execution/god_power_manual_playtest_execution_queue.json`
- `docs/系统设计/MythicAgeMod_1.0_神力手工玩法测试执行队列_2026-06-15.md`
- `docs/preview_assets/god_power_gameplay/manual_playtest_execution/god_power_manual_playtest_execution_queue.csv`
- `docs/preview_assets/certificate_economy/certificate_economy_report.json`
- `docs/美术资源/entity_validation/entity_manifest.json`
- `docs/preview_assets/creature_gameplay_contract/creature_gameplay_contract.json`
- `docs/系统设计/MythicAgeMod_1.0生物玩法闭环契约_2026-06-15.md`

### creature_design_models_art

- 优先级：`P0`
- 状态：`blocked`
- 是否满足 1.0：`False`
- 用户目标：模型与美术必须达到 1.0 质量，不能只有不好看的希腊生物；天马和九头蛇不能继续沿用畸形设计。

当前证据：
- design approvals=0/30
- design_status_counts={'blocked_current_design_rejected_v6_spec_pending': 2, 'manual_design_approval_required': 28}
- Pegasus/Hydra verdict=blocked_current_design_not_correct_and_v6_spec_not_approved
- Pegasus/Hydra V6 modeling spec=modeling_spec_ready_for_user_review verdict=not_approved_for_modeling modeling_allowed=False
- creature_gameplay_static_contract=True (30/30)
- art_rework_unresolved=30
- art_rework_priorities={'P0-DESIGN-BLOCKER': 2, 'P0-GREEK-REVIEW': 7, 'P0-USER-ACCEPTANCE': 1, 'P1-CIV-ART-UPGRADE': 20}
- art_production_batches={'B0_DESIGN_BLOCKER_REDRAW': 2, 'B1_GREEK_VERTICAL_SLICE_REVIEW': 8, 'B2_NON_GREEK_DESIGN_APPROVAL': 20}
- art_production_blocked=3
- design_production_lock=locked locked=30 unlocked=0
- design_approval_review=review_package_ready_designs_not_approved pending=30 batches={'B0_DESIGN_BLOCKER_REDRAW': 2, 'B1_GREEK_VERTICAL_SLICE_REVIEW': 8, 'B2_NON_GREEK_DESIGN_APPROVAL': 20}
- human_visual_decisions={'blocked_by_design_rejection': 2, 'pending_shape_review': 27, 'pending_user_acceptance': 1}

缺口或弱证据：
- 30 个生物当前没有任何一个正式 approved_for_modeling。
- Pegasus/Hydra 当前设计被拒绝，V6 可建模规格已生成但未签收，modeling_allowed=false。
- 生产锁已阻止 30 生物进入最终 Blockbench、UV、纹理、Java runtime 和动作生产。
- 非希腊 20 生物仍在 P1 美术升级队列，不能算最终 1.0 资产。
- 人工视觉台账仍未批准 30 生物的形体、比例、神话特征和纹理表现。

下一步：
- 按生物美术生产路线图执行 B0/B1/B2/B3/B4 批次，禁止跳过设计签收。
- 使用生物设计审批总包按 B0/B1/B2 顺序做人工设计审查。
- 先签收 Pegasus/Hydra V6 可建模规格：天马以原版成年马比例为主体，九头蛇以九头 socket 和真实颈身连接为硬约束。
- 按设计包重建被拒绝生物，再同步 Blockbench、UV、纹理和 Java runtime model。
- 逐文明完成 30 生物人工视觉验收，失败项回设计包而不是测试阶段试错。

权威证据：
- `docs/preview_assets/creature_design_first_gate/creature_design_first_gate.json`
- `docs/preview_assets/greek_creatures/design_correctness/pegasus_hydra_design_correctness_gate.json`
- `docs/preview_assets/greek_creatures/design_approval/v6/pegasus_hydra_v6_modeling_spec_package.json`
- `docs/美术资源/greek_creatures/design_review/pegasus_hydra_v6_modeling_spec_package_2026-06-15.md`
- `docs/preview_assets/creature_art_rework_queue/creature_art_rework_queue.json`
- `docs/preview_assets/creature_art_production_roadmap/creature_art_production_roadmap.json`
- `docs/项目管理/MythicAgeMod_1.0生物美术生产路线图_2026-06-15.md`
- `docs/preview_assets/creature_design_production_lock/creature_design_production_lock.json`
- `docs/项目管理/MythicAgeMod_1.0生物设计生产锁_2026-06-15.md`
- `docs/preview_assets/creature_design_approval_review/creature_design_approval_review_package.json`
- `docs/项目管理/MythicAgeMod_1.0生物设计审批总包_2026-06-15.md`
- `docs/preview_assets/creature_visual_acceptance/human_visual_acceptance_ledger.json`
- `docs/preview_assets/creature_gameplay_contract/creature_gameplay_contract.json`

### non_greek_creature_completion

- 优先级：`P1`
- 状态：`manual_acceptance_pending`
- 是否满足 1.0：`False`
- 用户目标：埃及、北欧、中国、亚特兰蒂斯不能只是占位，必须有各自文明生物成品级模型和美术。

当前证据：
- non_greek_art_upgrade_count=20
- art_production_batches={'B0_DESIGN_BLOCKER_REDRAW': 2, 'B1_GREEK_VERTICAL_SLICE_REVIEW': 8, 'B2_NON_GREEK_DESIGN_APPROVAL': 20}
- design_production_lock_counts={'blockbench_model_built_or_polished': 30, 'uv_template_locked': 30, 'texture_painted_from_uv': 30, 'blockbench_front_side_three_quarter_review': 30, 'java_runtime_model_synced': 30, 'visual_ledger_approved': 30, 'motion_capture_unblocked': 30}
- runtime_reports={'greek': 'docs/preview_assets/greek_creatures/runtime_model_quality_report.json', 'egyptian': 'docs/preview_assets/egyptian_creatures/runtime_model_quality_report.json', 'nordic': 'docs/preview_assets/nordic_creatures/runtime_model_quality_report.json', 'chinese': 'docs/preview_assets/chinese_creatures/runtime_model_quality_report.json', 'atlantean': 'docs/preview_assets/atlantean_creatures/runtime_model_quality_report.json'}
- visual_reports={'greek': 'docs/preview_assets/greek_creatures/visual_report.json', 'egyptian': 'docs/preview_assets/egyptian_creatures/visual_report.json', 'nordic': 'docs/preview_assets/nordic_creatures/visual_report.json', 'chinese': 'docs/preview_assets/chinese_creatures/visual_report.json', 'atlantean': 'docs/preview_assets/atlantean_creatures/visual_report.json'}

缺口或弱证据：
- 非希腊 20 生物有设计包、预览和 runtime 契约，但缺正式设计签收和人工视觉通过。
- 文明材质、轮廓、武器/符号、神话特征仍需从工程首版提升到成品级。

下一步：
- 先把 20 个非希腊生物推进 B2 设计批准，再进入 B3 Blockbench/UV/纹理生产。
- 按 Egyptian/Nordic/Chinese/Atlantean 四批做设计签收和美术重做。
- 每批要求概念图、Blockbench 三视图、UV、贴图、runtime 模型和人工结论一致。

权威证据：
- `docs/preview_assets/creature_art_rework_queue/creature_art_rework_queue.json`
- `docs/preview_assets/creature_art_production_roadmap/creature_art_production_roadmap.json`
- `docs/项目管理/MythicAgeMod_1.0生物美术生产路线图_2026-06-15.md`
- `docs/preview_assets/creature_design_production_lock/creature_design_production_lock.json`
- `docs/preview_assets/creature_visual_acceptance/creature_visual_acceptance_package.json`

### textures_uv_materials

- 优先级：`P0`
- 状态：`manual_acceptance_pending`
- 是否满足 1.0：`False`
- 用户目标：纹理必须 UV 驱动，眼、嘴、角、翼、尾、符号等关键细节不能空白、错位或随机。

当前证据：
- visual_quality_contract_ok=False
- visual_quality_failures=1
- manual_visual_review_required=30
- texture/UV evidence report=docs/preview_assets/creature_visual_acceptance/creature_visual_acceptance_package.json

缺口或弱证据：
- 自动 texture-fit 只能检查空白/越界，不能证明眼嘴和神话特征美术正确。
- 纯白、深色和大面积单色资产仍需人工检查结构阴影和材质层次。
- Pegasus/Hydra 设计未通过前，其纹理不能作为最终依据。

下一步：
- 每个生物保留 UV 模板和关键面清单，纹理绘制必须绑定设计包。
- 人工验收重点检查白色空白感、深色细节丢失、面部五官、翼羽、尾刺、多头连接阴影。

权威证据：
- `docs/preview_assets/creature_visual_acceptance/creature_visual_acceptance_package.json`
- `docs/preview_assets/creature_design_first_gate/creature_design_first_gate.json`

### actions_animation_motion

- 优先级：`P1`
- 状态：`blocked`
- 是否满足 1.0：`False`
- 用户目标：动作要覆盖待机、移动、攻击、特殊、受击、死亡，以及飞行/骑乘/多头等特殊动作。

当前证据：
- required motion modes covered=30/168
- missing_motion_modes=138
- motion_rework_queued=138
- motion_rework_blocked=15
- blocked_creatures=['greek:cyclops', 'greek:hydra', 'greek:pegasus']
- minimum_frame_budget=1020

缺口或弱证据：
- 138 项动作模式缺 MC 连续帧/clip 证据。
- Pegasus 飞行/骑乘飞行和 Hydra 九头错相动作被设计/美术阻断。
- 静态 setAngles 或 storyboard 不等于发布动作验收。

下一步：
- 先等设计阻断生物按 V6 签收规格完成模型重建，再捕获其飞行/多头动作。
- 按 16 批 motion capture 计划补足连续帧、触发条件和人工 motion review note。

权威证据：
- `docs/preview_assets/creature_action_acceptance/creature_action_acceptance_package.json`
- `docs/preview_assets/creature_motion_rework_queue/creature_motion_rework_queue.json`

### ui_player_guidance

- 优先级：`P1`
- 状态：`static_pass_runtime_pending`
- 是否满足 1.0：`False`
- 用户目标：UI 要让玩家看懂文明、证书、神力、交易、冷却、失败原因和进度。

当前证据：
- ui_static_gate_ok=True
- ui_guide_scope_ok=True

缺口或弱证据：
- 守护者交易 UI 和神力图鉴有静态 gate，但缺真实分辨率、GUI scale、长文本和多语言验收。
- 神殿状态、证书余额、失败原因、文明主题反馈还需要实机走查。

下一步：
- 用多个 GUI scale/分辨率走查交易 UI 与神力图鉴。
- 冻结 UI 文案后做多语言和长文本不重叠检查。

权威证据：
- `docs/preview_assets/ui_validation/ui_report.json`

### particles_vfx

- 优先级：`P1`
- 状态：`manual_acceptance_pending`
- 是否满足 1.0：`False`
- 用户目标：粒子效果要覆盖神力和生物技能，具有文明主题、三段式反馈和性能降级。

当前证据：
- particle_static_gate_ok=True
- god_power_particle_full_matrix_ok=True
- manual_playtest_required=52
- manual_execution_scenarios=446

缺口或弱证据：
- 52 神力静态粒子矩阵通过，但实机可读性和压力/降级未完成。
- 生物技能粒子还要验证不遮挡眼、嘴、武器、翼根、尾刺和多头结构。

下一步：
- 按神力批次记录 cast、active/impact、end 三段截图/结论。
- 高风险大范围粒子先做帧率和粒子数量降级策略验证。

权威证据：
- `docs/preview_assets/particle_validation/particle_report.json`
- `docs/preview_assets/particle_validation/god_power_particle_report.json`
- `docs/preview_assets/god_power_gameplay/god_power_gameplay_acceptance_package.json`
- `docs/preview_assets/god_power_gameplay/manual_playtest_execution/god_power_manual_playtest_execution_queue.json`

### art_direction_structures_world

- 优先级：`P1`
- 状态：`manual_acceptance_pending`
- 是否满足 1.0：`False`
- 用户目标：美术不仅是生物，还包括五文明建筑、方块、物品、UI 和粒子的统一视觉语言。

当前证据：
- m1_reference_docs_ok=False
- structure_quality_gate_ok=True
- structures=15/15
- theme_contracts=15/15

缺口或弱证据：
- 结构 gate 是静态质量，仍缺 15 个 canonical 神殿在地形中的建筑审美和功能点位验收。
- 物品、方块、UI、粒子和生物之间的文明主题一致性仍需人工 art-final 审查。

下一步：
- 做 15 个神殿 small/medium/large 实机加载审美验收。
- 按文明对齐材质、色彩、符号、建筑轮廓、UI 装饰和粒子语言。

权威证据：
- `docs/preview_assets/structure_validation/structure_report.json`
- `docs/项目管理/MythicAgeMod_1.0重整开发总纲_2026-06-15.md`

### audio_feedback

- 优先级：`P1`
- 状态：`manual_acceptance_pending`
- 是否满足 1.0：`False`
- 用户目标：音效和反馈要支撑生物、神力、UI、证书和战斗读秒，不只技术可播放。

当前证据：
- audio_technical_gate_ok=True
- creature_audio_events=149
- manual_listening_queue=149
- final_mix_candidates=21
- missing_tools=['ffprobe', 'ffmpeg', 'soxi', 'ogginfo']

缺口或弱证据：
- 149 个生物音频事件仍需人工听感验收。
- 21 个 final-mix candidate 需最终通过/替换结论。
- 响度/容器工具或等价 MCP 能力仍需补齐。

下一步：
- 先补响度/容器检测能力，再按 15 批完成试听和人工结论。
- 补神力 cast/impact/end、交易成功/失败、证书提示等 UI/玩法音效审计。

权威证据：
- `docs/preview_assets/audio_validation/creature_audio_final_mix_acceptance_package.json`
- `docs/preview_assets/audio_validation/audio_report.json`

### nbt_persistence

- 优先级：`P1`
- 状态：`verified_with_watchlist`
- 是否满足 1.0：`True`
- 用户目标：NBT 结构要正确覆盖长期状态；新增持久化状态必须有 schema、probe 和真实重启回归。

当前证据：
- nbt_release_acceptance_package_ok=True
- nbt_real_restart_cycle_ok=True
- nbt_state_ownership_status_counts={'covered': 21, 'runtime_only': 48}
- required=0
- runtime_only=48

缺口或弱证据：
- 当前 required/missing 为 0，因此不是当前 P0 blocker。
- 48 个 runtime_only 状态是明确决策，不代表未来长期状态自动完成。
- 任一新增长期神力、交易防刷、结构状态或实体状态都必须先升级目标表。

下一步：
- 保留 NBT 发布验收包和真实重启周期作为回归 gate。
- 开发新长期状态时先改 NBT 目标表，再补 schema、round-trip、restart contract 和真实重启验证。

权威证据：
- `docs/preview_assets/nbt_roundtrip_validation/nbt_release_acceptance_package.json`
- `docs/preview_assets/nbt_state_ownership/nbt_state_ownership_queue.json`
- `docs/preview_assets/nbt_target_validation/nbt_target_report.json`
- `docs/preview_assets/nbt_roundtrip_validation/nbt_restart_contract_report.json`

### release_management

- 优先级：`P0`
- 状态：`blocked`
- 是否满足 1.0：`False`
- 用户目标：1.0 大版本要有统一的发布口径、回归命令、玩家指南和退出标准。

当前证据：
- ready_for_1_0=False
- release_blocking_gaps=['human_visual_acceptance_not_done', 'creature_motion_acceptance_not_done', 'creature_audio_art_final_pending', 'god_power_playtest_acceptance_not_done']
- release_hygiene_ok=True
- scope_matrix=docs/项目管理/MythicAgeMod_1.0模块范围与缺口矩阵_2026-06-15.md

缺口或弱证据：
- release blocking gaps 未清零。
- 玩家指南、CHANGELOG、发布说明需要在玩法/UI/美术最终稳定后再冻结。

下一步：
- 每完成一个波次后重跑 gap audit、scope matrix、requirement trace 和 Gradle build。
- 只有 release blockers=0 且所有需求追踪项 done_for_1_0=true，才能进入 1.0 候选整理。

权威证据：
- `docs/preview_assets/1_0_gap_audit/gap_report.json`
- `docs/项目管理/MythicAgeMod_1.0模块范围与缺口矩阵_2026-06-15.md`

## 开发波次

- W0: Pegasus/Hydra V6 可建模规格审查，冻结设计先行规则。
- W1: 30 生物设计签收，Greek 视觉 vertical slice 作为质量样板。
- W2: 非希腊 20 生物美术升级和人工视觉验收。
- W3: 138 项动作连续帧/clip 补齐并做人工 motion 验收。
- W4: 52 神力玩法、粒子、音效和证书消耗验收。
- W5: UI、结构、世界生成、NBT 回归和玩家指南。
- W6: release blockers 清零后进入 1.0 候选整理。

## 退出口径

- 本报告所有需求项必须为 `verified` 或 `verified_with_watchlist`。
- `tools/mythic_age_1_0_gap_audit.py audit` 必须显示 `ready_for_1_0=true` 且 release blockers 为 0。
- `tools/mythic_age_1_0_scope_matrix.py audit` 必须显示所有模块满足退出 gate。
- `./gradlew build --offline` 必须通过。
