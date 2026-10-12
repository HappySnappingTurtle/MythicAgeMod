# MythicAgeMod 1.0 模块范围与缺口矩阵（2026-06-15）

本文档由 `tools/mythic_age_1_0_scope_matrix.py audit` 生成。它把 1.0 从“希腊生物继续补模型”扩展回完整发布范围：代码、模型、动作、纹理、UI、美术、粒子、音效、NBT、结构和世界生成。

## 总览

- 1.0 就绪：`False`
- release blocking gaps：4
- blocking gap IDs：`["human_visual_acceptance_not_done", "creature_motion_acceptance_not_done", "creature_audio_art_final_pending", "god_power_playtest_acceptance_not_done"]`
- 模块数量：10
- 模块状态统计：`{"manual_acceptance_pending": 9, "blocked": 1}`
- 判定规则：1.0 is complete only when every module is verified and the gap audit reports ready_for_1_0=true.

## 模块矩阵

| 模块 | 状态 | 当前证据摘要 | 退出 gate |
| --- | --- | --- | --- |
| `release_management` 版本与发布管理 | `manual_acceptance_pending` | 1.0 ready=False; release blockers=4; blockers=['human_visual_acceptance_not_done', 'creature_motion_acceptance_not_done', 'creature_audio_art_final_pending', 'god_power_playtest_acceptance_not_done']; requirement trace ok=False; requirement statuses={'blocked': 3, 'manual_acceptance_pending': 6, 'static_pass_runtime_pending': 2, 'verified_with_watchlist': 1}; open requirements=11; blocked requirements=['creature_design_models_art', 'actions_animation_motion', 'release_management'] | tools/mythic_age_1_0_gap_audit.py audit 返回 ready_for_1_0=true，requirement trace 通过，且 ./gradlew build --offline 通过。 |
| `code_gameplay` 代码与玩法 | `manual_acceptance_pending` | entity_manifest=43; creatures=30; god powers=52; manual playtests=52; min scenarios=446; manual execution=manual_playtest_not_done batches=8 scenarios=446; certificate economy ok=True; creature gameplay static=True (30/30); creature gameplay release ready=0/30; creature gameplay blockers={'current_design_rejected_v6_spec_pending': 2, 'design_not_approved:blocked_current_design_rejected_v6_spec_pending': 2, 'design_not_approved:manual_design_approval_required': 28, 'human_visual_not_approved:blocked_by_design_rejection': 2, 'human_visual_not_approved:pending_shape_review': 27, 'human_visual_not_approved:pending_user_acceptance': 1, 'motion_blocked_by_art_or_design': 3} | god_power_gameplay_acceptance_ok=true，certificate_economy_ok=true，且 creature_gameplay_contract ok=true。 |
| `models_art` 模型与美术 | `blocked` | design approvals=0/30; design states={'blocked_current_design_rejected_v6_spec_pending': 2, 'manual_design_approval_required': 28}; visual boards=30/30; quality blockers=1; design rejections=2; Pegasus/Hydra design correctness=blocked_current_design_not_correct_and_v6_spec_not_approved; Pegasus/Hydra V6 modeling spec=modeling_spec_ready_for_user_review modeling_allowed=False; art rework unresolved=30; art rework priorities={'P0-DESIGN-BLOCKER': 2, 'P0-GREEK-REVIEW': 7, 'P0-USER-ACCEPTANCE': 1, 'P1-CIV-ART-UPGRADE': 20}; art production batches={'B0_DESIGN_BLOCKER_REDRAW': 2, 'B1_GREEK_VERTICAL_SLICE_REVIEW': 8, 'B2_NON_GREEK_DESIGN_APPROVAL': 20}; art production blocked=3; art production design blockers=2; design production lock=locked locked=30 unlocked=0 hard=2 approval=28; design approval review=review_package_ready_designs_not_approved pending=30 batches={'B0_DESIGN_BLOCKER_REDRAW': 2, 'B1_GREEK_VERTICAL_SLICE_REVIEW': 8, 'B2_NON_GREEK_DESIGN_APPROVAL': 20}; human decisions={'blocked_by_design_rejection': 2, 'pending_shape_review': 27, 'pending_user_acceptance': 1} | greek_design_first_gate、creature_visual_acceptance_package、creature_human_visual_acceptance_ledger 全部通过。 |
| `textures_uv` 纹理与 UV | `manual_acceptance_pending` | texture/UV quality contract ok=False; failures=1; manual review required=30 | texture-fit、UV 空白检查、人工视觉台账全部通过。 |
| `actions_animation` 动作与动画 | `manual_acceptance_pending` | captured modes=30/168; missing=138; frames=180; capture queue=138; batches=16; rework queued=138; blocked by art/design=15; blocked creatures=['greek:cyclops', 'greek:hydra', 'greek:pegasus']; rework states={'blocked_by_art_or_design': 15, 'capture_allowed_but_final_acceptance_waits_for_art': 123} | creature_human_motion_acceptance_ok=true 且 full-mode sequence coverage 通过。 |
| `ui_player_guidance` UI 与玩家引导 | `manual_acceptance_pending` | UI static ok=True; guide scope ok=True | UI 静态 gate 加实机分辨率/多语言验收通过。 |
| `particles_vfx` 粒子与特效 | `manual_acceptance_pending` | particle static=True; god power full matrix=True; gameplay manual playtests=52; manual execution scenarios=446 | god_power_particle_full_matrix_ok=true 且玩法/压力验收完成。 |
| `audio_feedback` 音效与反馈 | `manual_acceptance_pending` | events=149; manual listening=149; final mix candidates=21; loudness metrics available=False; missing tools=['ffprobe', 'ffmpeg', 'soxi', 'ogginfo'] | creature_audio_final_mix_acceptance_ok=true，响度/听感报告完整。 |
| `nbt_persistence` NBT 与持久化 | `manual_acceptance_pending` | schema owners=14; real restart cycle=True; required=0; runtime_only=48; state ownership={'covered': 21, 'runtime_only': 48} | nbt_release_acceptance_package.py audit 通过，并且新增 required 状态全部有重启回归。 |
| `structures_worldgen` 结构与世界生成 | `manual_acceptance_pending` | canonical structures=15/15; theme contracts=15/15; functional points=15/15 | structure_validation 通过，并有实机建筑审美/功能点位结论。 |

## 详细缺口

### 版本与发布管理 (`release_management`)

- 状态：`manual_acceptance_pending`
- 当前证据：1.0 ready=False; release blockers=4; blockers=['human_visual_acceptance_not_done', 'creature_motion_acceptance_not_done', 'creature_audio_art_final_pending', 'god_power_playtest_acceptance_not_done']; requirement trace ok=False; requirement statuses={'blocked': 3, 'manual_acceptance_pending': 6, 'static_pass_runtime_pending': 2, 'verified_with_watchlist': 1}; open requirements=11; blocked requirements=['creature_design_models_art', 'actions_animation_motion', 'release_management']
- 必须完成：

  - release blocking gaps 清零
  - 需求追踪表全部变为 verified 或 verified_with_watchlist
  - 版本号、README、玩家指南、CHANGELOG 和 dashboard 对齐
  - 所有模块 gate 与人工验收均有可复验记录

- 证据：

  - `docs/preview_assets/1_0_gap_audit/gap_report.json`
  - `docs/preview_assets/1_0_gap_audit/release_acceptance_dashboard.html`
  - `docs/项目管理/MythicAgeMod_1.0需求追踪与开发缺口_2026-06-15.md`

- 退出 gate：tools/mythic_age_1_0_gap_audit.py audit 返回 ready_for_1_0=true，requirement trace 通过，且 ./gradlew build --offline 通过。

### 代码与玩法 (`code_gameplay`)

- 状态：`manual_acceptance_pending`
- 当前证据：entity_manifest=43; creatures=30; god powers=52; manual playtests=52; min scenarios=446; manual execution=manual_playtest_not_done batches=8 scenarios=446; certificate economy ok=True; creature gameplay static=True (30/30); creature gameplay release ready=0/30; creature gameplay blockers={'current_design_rejected_v6_spec_pending': 2, 'design_not_approved:blocked_current_design_rejected_v6_spec_pending': 2, 'design_not_approved:manual_design_approval_required': 28, 'human_visual_not_approved:blocked_by_design_rejection': 2, 'human_visual_not_approved:pending_shape_review': 27, 'human_visual_not_approved:pending_user_acceptance': 1, 'motion_blocked_by_art_or_design': 3}
- 必须完成：

  - 52 个神力完成人工玩法、目标选择、失败路径、冷却、消耗和性能验收
  - 按神力手工玩法测试执行队列填完 CSV 场景记录并回写通过结论
  - 18 个高风险神力优先做压力、回滚/降级和跨存档语义确认
  - 30 生物静态玩法闭环已可审计；发布前仍需设计、视觉、动作和实机行为验收全部通过
  - 证书经济从静态链路进入实机频率、防刷和价格平衡

- 证据：

  - `docs/preview_assets/god_power_gameplay/god_power_gameplay_acceptance_package.json`
  - `docs/preview_assets/god_power_gameplay/manual_playtest_execution/god_power_manual_playtest_execution_queue.json`
  - `docs/系统设计/MythicAgeMod_1.0_神力手工玩法测试执行队列_2026-06-15.md`
  - `docs/preview_assets/god_power_gameplay/manual_playtest_execution/god_power_manual_playtest_execution_queue.csv`
  - `docs/preview_assets/certificate_economy/certificate_economy_report.json`
  - `docs/preview_assets/creature_gameplay_contract/creature_gameplay_contract.json`
  - `docs/系统设计/MythicAgeMod_1.0生物玩法闭环契约_2026-06-15.md`

- 退出 gate：god_power_gameplay_acceptance_ok=true，certificate_economy_ok=true，且 creature_gameplay_contract ok=true。

### 模型与美术 (`models_art`)

- 状态：`blocked`
- 当前证据：design approvals=0/30; design states={'blocked_current_design_rejected_v6_spec_pending': 2, 'manual_design_approval_required': 28}; visual boards=30/30; quality blockers=1; design rejections=2; Pegasus/Hydra design correctness=blocked_current_design_not_correct_and_v6_spec_not_approved; Pegasus/Hydra V6 modeling spec=modeling_spec_ready_for_user_review modeling_allowed=False; art rework unresolved=30; art rework priorities={'P0-DESIGN-BLOCKER': 2, 'P0-GREEK-REVIEW': 7, 'P0-USER-ACCEPTANCE': 1, 'P1-CIV-ART-UPGRADE': 20}; art production batches={'B0_DESIGN_BLOCKER_REDRAW': 2, 'B1_GREEK_VERTICAL_SLICE_REVIEW': 8, 'B2_NON_GREEK_DESIGN_APPROVAL': 20}; art production blocked=3; art production design blockers=2; design production lock=locked locked=30 unlocked=0 hard=2 approval=28; design approval review=review_package_ready_designs_not_approved pending=30 batches={'B0_DESIGN_BLOCKER_REDRAW': 2, 'B1_GREEK_VERTICAL_SLICE_REVIEW': 8, 'B2_NON_GREEK_DESIGN_APPROVAL': 20}; human decisions={'blocked_by_design_rejection': 2, 'pending_shape_review': 27, 'pending_user_acceptance': 1}
- 必须完成：

  - 30 个生物先补正式设计先行签收，不能把生成型设计包当作批准
  - 按生物设计审批总包完成 B0/B1/B2 审查顺序和写回字段
  - 生产锁解除前，不允许进入最终 Blockbench、UV、纹理、Java runtime 或动作验收
  - 按生物美术生产路线图执行 B0/B1/B2/B3/B4 批次
  - Pegasus/Hydra 先审查并签收 V6 可建模规格，再重建模型
  - Greek 10 生物人工视觉复审并形成通过/返修/重写设计结论
  - 非希腊 20 生物按文明重做或精修为 1.0 成品级美术
  - 所有模型禁止大头补细节，必须用部件层级和 UV 细节表达神话身份

- 证据：

  - `docs/preview_assets/creature_design_first_gate/creature_design_first_gate.json`
  - `docs/preview_assets/creature_visual_acceptance/creature_visual_acceptance_package.json`
  - `docs/preview_assets/creature_visual_acceptance/human_visual_acceptance_ledger.json`
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
  - `docs/preview_assets/creature_design_approval_review/creature_design_approval_contact_sheet.png`
  - `docs/preview_assets/greek_creatures/shape_acceptance_report.json`
  - `docs/preview_assets/greek_creatures/design_approval/detail_boards/pegasus_hydra_detail_design_boards.json`

- 退出 gate：greek_design_first_gate、creature_visual_acceptance_package、creature_human_visual_acceptance_ledger 全部通过。

### 纹理与 UV (`textures_uv`)

- 状态：`manual_acceptance_pending`
- 当前证据：texture/UV quality contract ok=False; failures=1; manual review required=30
- 必须完成：

  - 每个生物关键面固定 UV：眼、嘴、牙、角、翼、尾、武器、符号
  - 纯白、深色或大面积色块必须有结构阴影，不能像空白/透明破面
  - 禁止通用随机面板纹理作为最终资源
  - Blockbench 三视图和 texture-fit preview 由人工验收确认

- 证据：

  - `docs/preview_assets/creature_visual_acceptance/creature_visual_acceptance_package.json`

- 退出 gate：texture-fit、UV 空白检查、人工视觉台账全部通过。

### 动作与动画 (`actions_animation`)

- 状态：`manual_acceptance_pending`
- 当前证据：captured modes=30/168; missing=138; frames=180; capture queue=138; batches=16; rework queued=138; blocked by art/design=15; blocked creatures=['greek:cyclops', 'greek:hydra', 'greek:pegasus']; rework states={'blocked_by_art_or_design': 15, 'capture_allowed_but_final_acceptance_waits_for_art': 123}
- 必须完成：

  - 补齐 138 项缺失动作模式
  - Pegasus 需要自主飞行与骑乘飞行，且等设计通过后重做
  - Hydra 需要 V6 九头错相 idle/attack/hurt，且任何动作不断颈；旧五头动作证据不作为 1.0 依据
  - 每个生物 idle、移动、攻击/特殊、受击、死亡有人工结论

- 证据：

  - `docs/preview_assets/creature_action_acceptance/creature_action_acceptance_package.json`
  - `docs/preview_assets/creature_motion_rework_queue/creature_motion_rework_queue.json`
  - `docs/preview_assets/creature_action_acceptance/minecraft_sequences/motion_sequence_manifest.json`

- 退出 gate：creature_human_motion_acceptance_ok=true 且 full-mode sequence coverage 通过。

### UI 与玩家引导 (`ui_player_guidance`)

- 状态：`manual_acceptance_pending`
- 当前证据：UI static ok=True; guide scope ok=True
- 必须完成：

  - 守护者交易 UI 实机验收：文明、余额、价格、材料、冷却、失败原因
  - 神力图鉴 UI 实机验收：效果、消耗、冷却、来源、风险和状态
  - GUI scale、长文本、多语言、低分辨率不重叠不截断
  - 神殿/证书/神像状态给玩家清楚反馈

- 证据：

  - `docs/preview_assets/ui_validation/ui_report.json`

- 退出 gate：UI 静态 gate 加实机分辨率/多语言验收通过。

### 粒子与特效 (`particles_vfx`)

- 状态：`manual_acceptance_pending`
- 当前证据：particle static=True; god power full matrix=True; gameplay manual playtests=52; manual execution scenarios=446
- 必须完成：

  - 52 神力 cast、active/impact、end 三段式实机可读性验收
  - 高风险大范围效果做性能压力和降级策略验证
  - 生物技能粒子不遮挡眼、嘴、武器、翼根、尾刺和多头结构
  - 粒子主题必须符合对应文明美术语言

- 证据：

  - `docs/preview_assets/particle_validation/particle_report.json`
  - `docs/preview_assets/particle_validation/god_power_particle_report.json`
  - `docs/preview_assets/god_power_gameplay/god_power_gameplay_acceptance_package.json`
  - `docs/preview_assets/god_power_gameplay/manual_playtest_execution/god_power_manual_playtest_execution_queue.json`

- 退出 gate：god_power_particle_full_matrix_ok=true 且玩法/压力验收完成。

### 音效与反馈 (`audio_feedback`)

- 状态：`manual_acceptance_pending`
- 当前证据：events=149; manual listening=149; final mix candidates=21; loudness metrics available=False; missing tools=['ffprobe', 'ffmpeg', 'soxi', 'ogginfo']
- 必须完成：

  - 149 个生物音频事件完成听感验收
  - 21 个 final-mix candidate 生物有最终通过/替换结论
  - 补齐响度/容器工具链或等价 MCP 能力
  - 神力和 UI 音效补 cast/impact/end、交易成功/失败、证书提示

- 证据：

  - `docs/preview_assets/audio_validation/creature_audio_final_mix_acceptance_package.json`
  - `docs/preview_assets/audio_validation/audio_report.json`

- 退出 gate：creature_audio_final_mix_acceptance_ok=true，响度/听感报告完整。

### NBT 与持久化 (`nbt_persistence`)

- 状态：`manual_acceptance_pending`
- 当前证据：schema owners=14; real restart cycle=True; required=0; runtime_only=48; state ownership={'covered': 21, 'runtime_only': 48}
- 必须完成：

  - 当前 NBT release package 作为回归保留
  - 任一新增长期状态先升为 required，再补 schema、round-trip 和真实重启验证
  - 复查持续神力、证书经济、结构/神殿状态的跨存档语义
  - 不要把 runtime_only 状态误判为发布持久化完成

- 证据：

  - `docs/preview_assets/nbt_roundtrip_validation/nbt_release_acceptance_package.json`
  - `docs/preview_assets/nbt_state_ownership/nbt_state_ownership_queue.json`
  - `docs/preview_assets/nbt_target_validation/nbt_target_report.json`
  - `docs/preview_assets/nbt_roundtrip_validation/nbt_restart_contract_report.json`

- 退出 gate：nbt_release_acceptance_package.py audit 通过，并且新增 required 状态全部有重启回归。

### 结构与世界生成 (`structures_worldgen`)

- 状态：`manual_acceptance_pending`
- 当前证据：canonical structures=15/15; theme contracts=15/15; functional points=15/15
- 必须完成：

  - 15 个 canonical 神殿实机加载并做建筑审美验收
  - 文明材料、体量、入口、守护者、祭坛、奖励点位可读
  - 世界生成频率、稀有度和玩家进程匹配
  - 数据包内无非 canonical 结构污染

- 证据：

  - `docs/preview_assets/structure_validation/structure_report.json`

- 退出 gate：structure_validation 通过，并有实机建筑审美/功能点位结论。

