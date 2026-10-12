# MythicAgeMod 1.0 生物设计生产锁（2026-06-15）

本文档由 `tools/creature_design_production_lock.py audit` 生成。它把设计先行原则转成生产准入锁：设计未批准时，不允许进入最终模型、UV、纹理、Java runtime、视觉台账或动作捕获。

## 总览

- 生产锁状态：`locked`
- 生物数量：30
- 已解锁：0
- 锁定：30
- 设计拒绝硬锁：2
- 人工审批锁：28
- 锁统计：`{"approval_lock_manual_review_required": 28, "hard_lock_rejected_design": 2}`
- 工序锁统计：`{"blockbench_model_built_or_polished": 30, "uv_template_locked": 30, "texture_painted_from_uv": 30, "blockbench_front_side_three_quarter_review": 30, "java_runtime_model_synced": 30, "visual_ledger_approved": 30, "motion_capture_unblocked": 30}`
- 判定规则：No creature may advance final Blockbench, UV, texture, runtime Java model, visual ledger, or motion capture until approved_for_modeling=true is recorded in the design-first gate.
- 设计审批总包：`docs/项目管理/MythicAgeMod_1.0生物设计审批总包_2026-06-15.md`
- 设计审批联系图：`docs/preview_assets/creature_design_approval_review/creature_design_approval_contact_sheet.png`

## 锁定清单

| 生物 | 锁级别 | 批次 | 锁定工序 | 下一步 |
| --- | --- | --- | --- | --- |
| `atlantean:abyssal_ray` | `approval_lock_manual_review_required` | `B2_NON_GREEK_DESIGN_APPROVAL` | `blockbench_model_built_or_polished, uv_template_locked, texture_painted_from_uv, blockbench_front_side_three_quarter_review, java_runtime_model_synced, visual_ledger_approved, motion_capture_unblocked` | `human_review_design_package_and_record_explicit_approval` |
| `atlantean:crystal_leviathan` | `approval_lock_manual_review_required` | `B2_NON_GREEK_DESIGN_APPROVAL` | `blockbench_model_built_or_polished, uv_template_locked, texture_painted_from_uv, blockbench_front_side_three_quarter_review, java_runtime_model_synced, visual_ledger_approved, motion_capture_unblocked` | `human_review_design_package_and_record_explicit_approval` |
| `atlantean:orichalcum_golem` | `approval_lock_manual_review_required` | `B2_NON_GREEK_DESIGN_APPROVAL` | `blockbench_model_built_or_polished, uv_template_locked, texture_painted_from_uv, blockbench_front_side_three_quarter_review, java_runtime_model_synced, visual_ledger_approved, motion_capture_unblocked` | `human_review_design_package_and_record_explicit_approval` |
| `atlantean:siren_priestess` | `approval_lock_manual_review_required` | `B2_NON_GREEK_DESIGN_APPROVAL` | `blockbench_model_built_or_polished, uv_template_locked, texture_painted_from_uv, blockbench_front_side_three_quarter_review, java_runtime_model_synced, visual_ledger_approved, motion_capture_unblocked` | `human_review_design_package_and_record_explicit_approval` |
| `atlantean:tidal_guardian` | `approval_lock_manual_review_required` | `B2_NON_GREEK_DESIGN_APPROVAL` | `blockbench_model_built_or_polished, uv_template_locked, texture_painted_from_uv, blockbench_front_side_three_quarter_review, java_runtime_model_synced, visual_ledger_approved, motion_capture_unblocked` | `human_review_design_package_and_record_explicit_approval` |
| `chinese:azure_dragon` | `approval_lock_manual_review_required` | `B2_NON_GREEK_DESIGN_APPROVAL` | `blockbench_model_built_or_polished, uv_template_locked, texture_painted_from_uv, blockbench_front_side_three_quarter_review, java_runtime_model_synced, visual_ledger_approved, motion_capture_unblocked` | `human_review_design_package_and_record_explicit_approval` |
| `chinese:jiangshi` | `approval_lock_manual_review_required` | `B2_NON_GREEK_DESIGN_APPROVAL` | `blockbench_model_built_or_polished, uv_template_locked, texture_painted_from_uv, blockbench_front_side_three_quarter_review, java_runtime_model_synced, visual_ledger_approved, motion_capture_unblocked` | `human_review_design_package_and_record_explicit_approval` |
| `chinese:nine_tailed_fox` | `approval_lock_manual_review_required` | `B2_NON_GREEK_DESIGN_APPROVAL` | `blockbench_model_built_or_polished, uv_template_locked, texture_painted_from_uv, blockbench_front_side_three_quarter_review, java_runtime_model_synced, visual_ledger_approved, motion_capture_unblocked` | `human_review_design_package_and_record_explicit_approval` |
| `chinese:qilin_guardian` | `approval_lock_manual_review_required` | `B2_NON_GREEK_DESIGN_APPROVAL` | `blockbench_model_built_or_polished, uv_template_locked, texture_painted_from_uv, blockbench_front_side_three_quarter_review, java_runtime_model_synced, visual_ledger_approved, motion_capture_unblocked` | `human_review_design_package_and_record_explicit_approval` |
| `chinese:stone_lion_guardian` | `approval_lock_manual_review_required` | `B2_NON_GREEK_DESIGN_APPROVAL` | `blockbench_model_built_or_polished, uv_template_locked, texture_painted_from_uv, blockbench_front_side_three_quarter_review, java_runtime_model_synced, visual_ledger_approved, motion_capture_unblocked` | `human_review_design_package_and_record_explicit_approval` |
| `egyptian:anubis_warrior` | `approval_lock_manual_review_required` | `B2_NON_GREEK_DESIGN_APPROVAL` | `blockbench_model_built_or_polished, uv_template_locked, texture_painted_from_uv, blockbench_front_side_three_quarter_review, java_runtime_model_synced, visual_ledger_approved, motion_capture_unblocked` | `human_review_design_package_and_record_explicit_approval` |
| `egyptian:mummy_soldier` | `approval_lock_manual_review_required` | `B2_NON_GREEK_DESIGN_APPROVAL` | `blockbench_model_built_or_polished, uv_template_locked, texture_painted_from_uv, blockbench_front_side_three_quarter_review, java_runtime_model_synced, visual_ledger_approved, motion_capture_unblocked` | `human_review_design_package_and_record_explicit_approval` |
| `egyptian:scarab_guardian` | `approval_lock_manual_review_required` | `B2_NON_GREEK_DESIGN_APPROVAL` | `blockbench_model_built_or_polished, uv_template_locked, texture_painted_from_uv, blockbench_front_side_three_quarter_review, java_runtime_model_synced, visual_ledger_approved, motion_capture_unblocked` | `human_review_design_package_and_record_explicit_approval` |
| `egyptian:sphinx_sentinel` | `approval_lock_manual_review_required` | `B2_NON_GREEK_DESIGN_APPROVAL` | `blockbench_model_built_or_polished, uv_template_locked, texture_painted_from_uv, blockbench_front_side_three_quarter_review, java_runtime_model_synced, visual_ledger_approved, motion_capture_unblocked` | `human_review_design_package_and_record_explicit_approval` |
| `egyptian:sun_falcon` | `approval_lock_manual_review_required` | `B2_NON_GREEK_DESIGN_APPROVAL` | `blockbench_model_built_or_polished, uv_template_locked, texture_painted_from_uv, blockbench_front_side_three_quarter_review, java_runtime_model_synced, visual_ledger_approved, motion_capture_unblocked` | `human_review_design_package_and_record_explicit_approval` |
| `greek:centaur` | `approval_lock_manual_review_required` | `B1_GREEK_VERTICAL_SLICE_REVIEW` | `blockbench_model_built_or_polished, uv_template_locked, texture_painted_from_uv, blockbench_front_side_three_quarter_review, java_runtime_model_synced, visual_ledger_approved, motion_capture_unblocked` | `human_review_design_package_and_record_explicit_approval` |
| `greek:chimera` | `approval_lock_manual_review_required` | `B1_GREEK_VERTICAL_SLICE_REVIEW` | `blockbench_model_built_or_polished, uv_template_locked, texture_painted_from_uv, blockbench_front_side_three_quarter_review, java_runtime_model_synced, visual_ledger_approved, motion_capture_unblocked` | `human_review_design_package_and_record_explicit_approval` |
| `greek:colossus` | `approval_lock_manual_review_required` | `B1_GREEK_VERTICAL_SLICE_REVIEW` | `blockbench_model_built_or_polished, uv_template_locked, texture_painted_from_uv, blockbench_front_side_three_quarter_review, java_runtime_model_synced, visual_ledger_approved, motion_capture_unblocked` | `human_review_design_package_and_record_explicit_approval` |
| `greek:cyclops` | `approval_lock_manual_review_required` | `B1_GREEK_VERTICAL_SLICE_REVIEW` | `blockbench_model_built_or_polished, uv_template_locked, texture_painted_from_uv, blockbench_front_side_three_quarter_review, java_runtime_model_synced, visual_ledger_approved, motion_capture_unblocked` | `human_review_design_package_and_record_explicit_approval` |
| `greek:manticore` | `approval_lock_manual_review_required` | `B1_GREEK_VERTICAL_SLICE_REVIEW` | `blockbench_model_built_or_polished, uv_template_locked, texture_painted_from_uv, blockbench_front_side_three_quarter_review, java_runtime_model_synced, visual_ledger_approved, motion_capture_unblocked` | `human_review_design_package_and_record_explicit_approval` |
| `greek:medusa` | `approval_lock_manual_review_required` | `B1_GREEK_VERTICAL_SLICE_REVIEW` | `blockbench_model_built_or_polished, uv_template_locked, texture_painted_from_uv, blockbench_front_side_three_quarter_review, java_runtime_model_synced, visual_ledger_approved, motion_capture_unblocked` | `human_review_design_package_and_record_explicit_approval` |
| `greek:minotaur` | `approval_lock_manual_review_required` | `B1_GREEK_VERTICAL_SLICE_REVIEW` | `blockbench_model_built_or_polished, uv_template_locked, texture_painted_from_uv, blockbench_front_side_three_quarter_review, java_runtime_model_synced, visual_ledger_approved, motion_capture_unblocked` | `human_review_design_package_and_record_explicit_approval` |
| `greek:nemean_lion` | `approval_lock_manual_review_required` | `B1_GREEK_VERTICAL_SLICE_REVIEW` | `blockbench_model_built_or_polished, uv_template_locked, texture_painted_from_uv, blockbench_front_side_three_quarter_review, java_runtime_model_synced, visual_ledger_approved, motion_capture_unblocked` | `human_review_design_package_and_record_explicit_approval` |
| `nordic:draugr_warrior` | `approval_lock_manual_review_required` | `B2_NON_GREEK_DESIGN_APPROVAL` | `blockbench_model_built_or_polished, uv_template_locked, texture_painted_from_uv, blockbench_front_side_three_quarter_review, java_runtime_model_synced, visual_ledger_approved, motion_capture_unblocked` | `human_review_design_package_and_record_explicit_approval` |
| `nordic:dwarf_berserker` | `approval_lock_manual_review_required` | `B2_NON_GREEK_DESIGN_APPROVAL` | `blockbench_model_built_or_polished, uv_template_locked, texture_painted_from_uv, blockbench_front_side_three_quarter_review, java_runtime_model_synced, visual_ledger_approved, motion_capture_unblocked` | `human_review_design_package_and_record_explicit_approval` |
| `nordic:fenrir_wolf` | `approval_lock_manual_review_required` | `B2_NON_GREEK_DESIGN_APPROVAL` | `blockbench_model_built_or_polished, uv_template_locked, texture_painted_from_uv, blockbench_front_side_three_quarter_review, java_runtime_model_synced, visual_ledger_approved, motion_capture_unblocked` | `human_review_design_package_and_record_explicit_approval` |
| `nordic:frost_giant` | `approval_lock_manual_review_required` | `B2_NON_GREEK_DESIGN_APPROVAL` | `blockbench_model_built_or_polished, uv_template_locked, texture_painted_from_uv, blockbench_front_side_three_quarter_review, java_runtime_model_synced, visual_ledger_approved, motion_capture_unblocked` | `human_review_design_package_and_record_explicit_approval` |
| `nordic:valkyrie` | `approval_lock_manual_review_required` | `B2_NON_GREEK_DESIGN_APPROVAL` | `blockbench_model_built_or_polished, uv_template_locked, texture_painted_from_uv, blockbench_front_side_three_quarter_review, java_runtime_model_synced, visual_ledger_approved, motion_capture_unblocked` | `human_review_design_package_and_record_explicit_approval` |
| `greek:hydra` | `hard_lock_rejected_design` | `B0_DESIGN_BLOCKER_REDRAW` | `blockbench_model_built_or_polished, uv_template_locked, texture_painted_from_uv, blockbench_front_side_three_quarter_review, java_runtime_model_synced, visual_ledger_approved, motion_capture_unblocked` | `review_latest_candidate_contract_and_sheet_before_any_modeling` |
| `greek:pegasus` | `hard_lock_rejected_design` | `B0_DESIGN_BLOCKER_REDRAW` | `blockbench_model_built_or_polished, uv_template_locked, texture_painted_from_uv, blockbench_front_side_three_quarter_review, java_runtime_model_synced, visual_ledger_approved, motion_capture_unblocked` | `review_latest_candidate_contract_and_sheet_before_any_modeling` |

## 硬锁详情

### greek:hydra

- 设计状态：`blocked_current_design_rejected_v6_spec_pending`
- 最新候选契约：`docs/美术资源/greek_creatures/design_blueprints/hydra_v6_modeling_spec_contract.md`
- 最新候选图：`docs/preview_assets/greek_creatures/design_approval/v6/hydra_v6_modeling_spec_sheet.png`
- 必须先确认：
  - exactly nine heads and nine sockets are accepted
  - each neck path has root, mid, tip, and head_core overlap
  - low long body and shoulder_saddle socket band are accepted
  - each head has jaw, eye, mouth, fang, and horn or poison gland
  - Java runtime rebuild from five heads to nine heads is required

- 禁止事项：
  - Do not generate or polish final .bbmodel from this design.
  - Do not lock final UV or paint final texture from this design.
  - Do not sync Java runtime model from this design.
  - Do not capture or accept final motion evidence from this design.
  - Do not treat generated previews, runtime screenshots, or static code contract as design approval.

### greek:pegasus

- 设计状态：`blocked_current_design_rejected_v6_spec_pending`
- 最新候选契约：`docs/美术资源/greek_creatures/design_blueprints/pegasus_v6_modeling_spec_contract.md`
- 最新候选图：`docs/preview_assets/greek_creatures/design_approval/v6/pegasus_v6_modeling_spec_sheet.png`
- 必须先确认：
  - adult horse side silhouette and body length are accepted
  - head_core and muzzle dimensions are accepted as small horse head
  - two-segment sloped neck and visible withers connection are accepted
  - wing roots, saddle_space, and six feather layers are accepted
  - white UV zones include eyes, nostrils, mouth, mane, hooves, and feather edges

- 禁止事项：
  - Do not generate or polish final .bbmodel from this design.
  - Do not lock final UV or paint final texture from this design.
  - Do not sync Java runtime model from this design.
  - Do not capture or accept final motion evidence from this design.
  - Do not treat generated previews, runtime screenshots, or static code contract as design approval.

## 退出条件

- 对每个生物，设计包 front matter 必须明确 `approved_for_modeling`。
- 对每个生物，`modeling_allowed=true` 必须与设计状态一致。
- Pegasus/Hydra 必须先通过专门设计正确性 gate。
- 生产锁 `ok=true` 后，才能进入最终 Blockbench/UV/纹理/Java runtime/动作验收。
