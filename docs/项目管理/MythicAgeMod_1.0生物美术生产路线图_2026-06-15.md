# MythicAgeMod 1.0 生物美术生产路线图（2026-06-15）

本文档由 `tools/creature_art_production_roadmap.py audit` 生成。它把生物美术重做队列拆成可执行生产批次，确保先设计签收，再建模、UV、纹理、runtime 同步和动作捕获。

## 总览

- 路线图通过：`False`
- 生物数量：30
- 已人工批准：0
- 被阻断生物：3
- 设计阻断生物：2
- 批次统计：`{"B0_DESIGN_BLOCKER_REDRAW": 2, "B1_GREEK_VERTICAL_SLICE_REVIEW": 8, "B2_NON_GREEK_DESIGN_APPROVAL": 20}`
- 优先级统计：`{"P0-DESIGN-BLOCKER": 2, "P0-GREEK-REVIEW": 7, "P0-USER-ACCEPTANCE": 1, "P1-CIV-ART-UPGRADE": 20}`

## 批次

- `B0_DESIGN_BLOCKER_REDRAW`：Pegasus/Hydra 这类设计已拒绝生物，禁止建模，先重画/签收设计。
- `B1_GREEK_VERTICAL_SLICE_REVIEW`：希腊切片人工形体复审，决定 polish、局部返修或回设计。
- `B2_NON_GREEK_DESIGN_APPROVAL`：非希腊 20 生物先补正式设计签收。
- `B3_NON_GREEK_BLOCKBENCH_UV_TEXTURE`：非希腊通过设计后进入模型/UV/纹理生产。
- `B4_RUNTIME_SYNC_AND_MOTION_UNLOCK`：视觉批准后同步 Java runtime 并解锁动作捕获。

## B0 候选设计材料

- V6 可建模规格包状态：`modeling_spec_ready_for_user_review`
- V6 可建模规格包报告：`docs/preview_assets/greek_creatures/design_approval/v6/pegasus_hydra_v6_modeling_spec_package.json`
- V6 可建模规格包说明：`docs/美术资源/greek_creatures/design_review/pegasus_hydra_v6_modeling_spec_package_2026-06-15.md`
- V5 历史候选包：`docs/preview_assets/greek_creatures/design_approval/v5/pegasus_hydra_v5_design_approval_package.json`
- V4 历史候选包：`docs/preview_assets/greek_creatures/design_approval/v4/pegasus_hydra_v4_candidate_package.json`
- 候选包只用于用户审查和设计签收；存在候选图不代表可建模。

## 工序状态统计

| 工序 | 状态统计 |
| --- | --- |
| `design_package_approved` | `{"blocked": 2, "pending_manual_approval": 28}` |
| `final_design_sheet_approved` | `{"blocked": 2, "pending_manual_approval": 28}` |
| `blockbench_model_built_or_polished` | `{"pending": 30}` |
| `uv_template_locked` | `{"pending": 30}` |
| `texture_painted_from_uv` | `{"pending": 30}` |
| `blockbench_front_side_three_quarter_review` | `{"pending": 30}` |
| `java_runtime_model_synced` | `{"pending": 30}` |
| `visual_ledger_approved` | `{"pending": 2, "pending_shape_review": 27, "pending_user_acceptance": 1}` |
| `motion_capture_unblocked` | `{"blocked": 2, "blocked_by_art_or_design": 1, "waiting_for_visual_approval": 27}` |

## 生物批次清单

### B0_DESIGN_BLOCKER_REDRAW

| 文明 | 生物 | 优先级 | 人工决策 | 设计状态 | 阻断工序 | 下一步 |
| --- | --- | --- | --- | --- | --- | --- |
| greek | `hydra` | `P0-DESIGN-BLOCKER` | `blocked_by_design_rejection` | `blocked_current_design_rejected_v3_pending` | `design_package_approved, final_design_sheet_approved, motion_capture_unblocked` | `design_package_approved` |
| greek | `pegasus` | `P0-DESIGN-BLOCKER` | `blocked_by_design_rejection` | `blocked_current_design_rejected_v3_pending` | `design_package_approved, final_design_sheet_approved, motion_capture_unblocked` | `design_package_approved` |

### B1_GREEK_VERTICAL_SLICE_REVIEW

| 文明 | 生物 | 优先级 | 人工决策 | 设计状态 | 阻断工序 | 下一步 |
| --- | --- | --- | --- | --- | --- | --- |
| greek | `centaur` | `P0-GREEK-REVIEW` | `pending_shape_review` | `manual_design_approval_required` | `-` | `design_package_approved` |
| greek | `chimera` | `P0-GREEK-REVIEW` | `pending_shape_review` | `manual_design_approval_required` | `-` | `design_package_approved` |
| greek | `colossus` | `P0-GREEK-REVIEW` | `pending_shape_review` | `manual_design_approval_required` | `-` | `design_package_approved` |
| greek | `cyclops` | `P0-USER-ACCEPTANCE` | `pending_user_acceptance` | `manual_design_approval_required` | `motion_capture_unblocked` | `motion_capture_unblocked` |
| greek | `manticore` | `P0-GREEK-REVIEW` | `pending_shape_review` | `manual_design_approval_required` | `-` | `design_package_approved` |
| greek | `medusa` | `P0-GREEK-REVIEW` | `pending_shape_review` | `manual_design_approval_required` | `-` | `design_package_approved` |
| greek | `minotaur` | `P0-GREEK-REVIEW` | `pending_shape_review` | `manual_design_approval_required` | `-` | `design_package_approved` |
| greek | `nemean_lion` | `P0-GREEK-REVIEW` | `pending_shape_review` | `manual_design_approval_required` | `-` | `design_package_approved` |

### B2_NON_GREEK_DESIGN_APPROVAL

| 文明 | 生物 | 优先级 | 人工决策 | 设计状态 | 阻断工序 | 下一步 |
| --- | --- | --- | --- | --- | --- | --- |
| egyptian | `anubis_warrior` | `P1-CIV-ART-UPGRADE` | `pending_shape_review` | `manual_design_approval_required` | `-` | `design_package_approved` |
| egyptian | `mummy_soldier` | `P1-CIV-ART-UPGRADE` | `pending_shape_review` | `manual_design_approval_required` | `-` | `design_package_approved` |
| egyptian | `scarab_guardian` | `P1-CIV-ART-UPGRADE` | `pending_shape_review` | `manual_design_approval_required` | `-` | `design_package_approved` |
| egyptian | `sphinx_sentinel` | `P1-CIV-ART-UPGRADE` | `pending_shape_review` | `manual_design_approval_required` | `-` | `design_package_approved` |
| egyptian | `sun_falcon` | `P1-CIV-ART-UPGRADE` | `pending_shape_review` | `manual_design_approval_required` | `-` | `design_package_approved` |
| nordic | `draugr_warrior` | `P1-CIV-ART-UPGRADE` | `pending_shape_review` | `manual_design_approval_required` | `-` | `design_package_approved` |
| nordic | `dwarf_berserker` | `P1-CIV-ART-UPGRADE` | `pending_shape_review` | `manual_design_approval_required` | `-` | `design_package_approved` |
| nordic | `fenrir_wolf` | `P1-CIV-ART-UPGRADE` | `pending_shape_review` | `manual_design_approval_required` | `-` | `design_package_approved` |
| nordic | `frost_giant` | `P1-CIV-ART-UPGRADE` | `pending_shape_review` | `manual_design_approval_required` | `-` | `design_package_approved` |
| nordic | `valkyrie` | `P1-CIV-ART-UPGRADE` | `pending_shape_review` | `manual_design_approval_required` | `-` | `design_package_approved` |
| chinese | `azure_dragon` | `P1-CIV-ART-UPGRADE` | `pending_shape_review` | `manual_design_approval_required` | `-` | `design_package_approved` |
| chinese | `jiangshi` | `P1-CIV-ART-UPGRADE` | `pending_shape_review` | `manual_design_approval_required` | `-` | `design_package_approved` |
| chinese | `nine_tailed_fox` | `P1-CIV-ART-UPGRADE` | `pending_shape_review` | `manual_design_approval_required` | `-` | `design_package_approved` |
| chinese | `qilin_guardian` | `P1-CIV-ART-UPGRADE` | `pending_shape_review` | `manual_design_approval_required` | `-` | `design_package_approved` |
| chinese | `stone_lion_guardian` | `P1-CIV-ART-UPGRADE` | `pending_shape_review` | `manual_design_approval_required` | `-` | `design_package_approved` |
| atlantean | `abyssal_ray` | `P1-CIV-ART-UPGRADE` | `pending_shape_review` | `manual_design_approval_required` | `-` | `design_package_approved` |
| atlantean | `crystal_leviathan` | `P1-CIV-ART-UPGRADE` | `pending_shape_review` | `manual_design_approval_required` | `-` | `design_package_approved` |
| atlantean | `orichalcum_golem` | `P1-CIV-ART-UPGRADE` | `pending_shape_review` | `manual_design_approval_required` | `-` | `design_package_approved` |
| atlantean | `siren_priestess` | `P1-CIV-ART-UPGRADE` | `pending_shape_review` | `manual_design_approval_required` | `-` | `design_package_approved` |
| atlantean | `tidal_guardian` | `P1-CIV-ART-UPGRADE` | `pending_shape_review` | `manual_design_approval_required` | `-` | `design_package_approved` |

## 执行纪律

- 测试只验证设计是否落地，不负责发明造型。
- `design_package_approved` 与 `final_design_sheet_approved` 未通过时，不允许生成最终 `.bbmodel`、纹理、动作或 runtime model。
- 任何生物不得用大头弥补眼、嘴、牙、角、吻部、武器、翼、尾或多头细节。
- 纹理必须从 UV 计划绘制，不能用随机面板当最终资源。
- 动作捕获只在视觉 ledger approved 后进入最终验收；被设计阻断的动作证据不作为 1.0 发布依据。
