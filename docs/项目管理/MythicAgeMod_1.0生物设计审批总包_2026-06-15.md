# MythicAgeMod 1.0 生物设计审批总包（2026-06-15）

本文档由 `tools/creature_design_approval_review_package.py audit` 生成。它是设计审批入口，不是批准结果。生产锁仍然有效，任何生物未写入 `approved_for_modeling=true` 前，都不得进入最终模型、UV、纹理、Java runtime 或动作验收。

## 总览

- 审批包状态：`review_package_ready_designs_not_approved`
- 生物数量：30
- 已批准建模：0
- 待审批：30
- 批次统计：`{"B0_DESIGN_BLOCKER_REDRAW": 2, "B1_GREEK_VERTICAL_SLICE_REVIEW": 8, "B2_NON_GREEK_DESIGN_APPROVAL": 20}`
- 文明统计：`{"atlantean": 5, "chinese": 5, "egyptian": 5, "greek": 10, "nordic": 5}`
- 锁统计：`{"approval_lock_manual_review_required": 28, "hard_lock_rejected_design": 2}`
- 缺失审查图片：0
- 联系图：`docs/preview_assets/creature_design_approval_review/creature_design_approval_contact_sheet.png`

![creature design approval contact sheet](../preview_assets/creature_design_approval_review/creature_design_approval_contact_sheet.png)

## 审批规则

- 测试不负责发明造型，只验证设计是否落地。
- 审批只发生在设计包、候选图、视觉审查板都被人工确认之后。
- 审批通过后，必须显式写回设计 front matter：`design_status=approved_for_modeling`、`modeling_allowed=true` 等字段。
- 未批准前，不能生成最终 `.bbmodel`、最终 UV、最终纹理、Java runtime 同步或最终动作证据。

## 批次清单

### B0_DESIGN_BLOCKER_REDRAW

| 生物 | 锁级别 | 设计包 | 主要审查图 | 下一步 |
| --- | --- | --- | --- | --- |
| `greek:pegasus` | `hard_lock_rejected_design` | `docs/美术资源/greek_creatures/design/pegasus.md` | `docs/preview_assets/greek_creatures/design_approval/v6/pegasus_v6_modeling_spec_sheet.png` | `review_latest_candidate_contract_and_sheet_before_any_modeling` |
| `greek:hydra` | `hard_lock_rejected_design` | `docs/美术资源/greek_creatures/design/hydra.md` | `docs/preview_assets/greek_creatures/design_approval/v6/hydra_v6_modeling_spec_sheet.png` | `review_latest_candidate_contract_and_sheet_before_any_modeling` |

### B1_GREEK_VERTICAL_SLICE_REVIEW

| 生物 | 锁级别 | 设计包 | 主要审查图 | 下一步 |
| --- | --- | --- | --- | --- |
| `greek:centaur` | `approval_lock_manual_review_required` | `docs/美术资源/greek_creatures/design/centaur.md` | `docs/美术资源/greek_creatures/concepts/centaur_concept.png` | `human_review_design_package_and_record_explicit_approval` |
| `greek:chimera` | `approval_lock_manual_review_required` | `docs/美术资源/greek_creatures/design/chimera.md` | `docs/美术资源/greek_creatures/concepts/chimera_concept.png` | `human_review_design_package_and_record_explicit_approval` |
| `greek:colossus` | `approval_lock_manual_review_required` | `docs/美术资源/greek_creatures/design/colossus.md` | `docs/美术资源/greek_creatures/concepts/colossus_concept.png` | `human_review_design_package_and_record_explicit_approval` |
| `greek:cyclops` | `approval_lock_manual_review_required` | `docs/美术资源/greek_creatures/design/cyclops.md` | `docs/美术资源/greek_creatures/concepts/cyclops_concept.png` | `human_review_design_package_and_record_explicit_approval` |
| `greek:manticore` | `approval_lock_manual_review_required` | `docs/美术资源/greek_creatures/design/manticore.md` | `docs/美术资源/greek_creatures/concepts/manticore_concept.png` | `human_review_design_package_and_record_explicit_approval` |
| `greek:medusa` | `approval_lock_manual_review_required` | `docs/美术资源/greek_creatures/design/medusa.md` | `docs/美术资源/greek_creatures/concepts/medusa_concept.png` | `human_review_design_package_and_record_explicit_approval` |
| `greek:minotaur` | `approval_lock_manual_review_required` | `docs/美术资源/greek_creatures/design/minotaur.md` | `docs/美术资源/greek_creatures/concepts/minotaur_concept.png` | `human_review_design_package_and_record_explicit_approval` |
| `greek:nemean_lion` | `approval_lock_manual_review_required` | `docs/美术资源/greek_creatures/design/nemean_lion.md` | `docs/美术资源/greek_creatures/concepts/nemean_lion_concept.png` | `human_review_design_package_and_record_explicit_approval` |

### B2_NON_GREEK_DESIGN_APPROVAL

| 生物 | 锁级别 | 设计包 | 主要审查图 | 下一步 |
| --- | --- | --- | --- | --- |
| `egyptian:anubis_warrior` | `approval_lock_manual_review_required` | `docs/美术资源/egyptian_creatures/design/anubis_warrior.md` | `docs/美术资源/egyptian_creatures/concepts/anubis_warrior_concept.png` | `human_review_design_package_and_record_explicit_approval` |
| `egyptian:mummy_soldier` | `approval_lock_manual_review_required` | `docs/美术资源/egyptian_creatures/design/mummy_soldier.md` | `docs/美术资源/egyptian_creatures/concepts/mummy_soldier_concept.png` | `human_review_design_package_and_record_explicit_approval` |
| `egyptian:scarab_guardian` | `approval_lock_manual_review_required` | `docs/美术资源/egyptian_creatures/design/scarab_guardian.md` | `docs/美术资源/egyptian_creatures/concepts/scarab_guardian_concept.png` | `human_review_design_package_and_record_explicit_approval` |
| `egyptian:sphinx_sentinel` | `approval_lock_manual_review_required` | `docs/美术资源/egyptian_creatures/design/sphinx_sentinel.md` | `docs/美术资源/egyptian_creatures/concepts/sphinx_sentinel_concept.png` | `human_review_design_package_and_record_explicit_approval` |
| `egyptian:sun_falcon` | `approval_lock_manual_review_required` | `docs/美术资源/egyptian_creatures/design/sun_falcon.md` | `docs/美术资源/egyptian_creatures/concepts/sun_falcon_concept.png` | `human_review_design_package_and_record_explicit_approval` |
| `nordic:draugr_warrior` | `approval_lock_manual_review_required` | `docs/美术资源/nordic_creatures/design/draugr_warrior.md` | `docs/美术资源/nordic_creatures/concepts/draugr_warrior_concept.png` | `human_review_design_package_and_record_explicit_approval` |
| `nordic:dwarf_berserker` | `approval_lock_manual_review_required` | `docs/美术资源/nordic_creatures/design/dwarf_berserker.md` | `docs/美术资源/nordic_creatures/concepts/dwarf_berserker_concept.png` | `human_review_design_package_and_record_explicit_approval` |
| `nordic:fenrir_wolf` | `approval_lock_manual_review_required` | `docs/美术资源/nordic_creatures/design/fenrir_wolf.md` | `docs/美术资源/nordic_creatures/concepts/fenrir_wolf_concept.png` | `human_review_design_package_and_record_explicit_approval` |
| `nordic:frost_giant` | `approval_lock_manual_review_required` | `docs/美术资源/nordic_creatures/design/frost_giant.md` | `docs/美术资源/nordic_creatures/concepts/frost_giant_concept.png` | `human_review_design_package_and_record_explicit_approval` |
| `nordic:valkyrie` | `approval_lock_manual_review_required` | `docs/美术资源/nordic_creatures/design/valkyrie.md` | `docs/美术资源/nordic_creatures/concepts/valkyrie_concept.png` | `human_review_design_package_and_record_explicit_approval` |
| `chinese:azure_dragon` | `approval_lock_manual_review_required` | `docs/美术资源/chinese_creatures/design/azure_dragon.md` | `docs/美术资源/chinese_creatures/concepts/azure_dragon_concept.png` | `human_review_design_package_and_record_explicit_approval` |
| `chinese:jiangshi` | `approval_lock_manual_review_required` | `docs/美术资源/chinese_creatures/design/jiangshi.md` | `docs/美术资源/chinese_creatures/concepts/jiangshi_concept.png` | `human_review_design_package_and_record_explicit_approval` |
| `chinese:nine_tailed_fox` | `approval_lock_manual_review_required` | `docs/美术资源/chinese_creatures/design/nine_tailed_fox.md` | `docs/美术资源/chinese_creatures/concepts/nine_tailed_fox_concept.png` | `human_review_design_package_and_record_explicit_approval` |
| `chinese:qilin_guardian` | `approval_lock_manual_review_required` | `docs/美术资源/chinese_creatures/design/qilin_guardian.md` | `docs/美术资源/chinese_creatures/concepts/qilin_guardian_concept.png` | `human_review_design_package_and_record_explicit_approval` |
| `chinese:stone_lion_guardian` | `approval_lock_manual_review_required` | `docs/美术资源/chinese_creatures/design/stone_lion_guardian.md` | `docs/美术资源/chinese_creatures/concepts/stone_lion_guardian_concept.png` | `human_review_design_package_and_record_explicit_approval` |
| `atlantean:abyssal_ray` | `approval_lock_manual_review_required` | `docs/美术资源/atlantean_creatures/design/abyssal_ray.md` | `docs/美术资源/atlantean_creatures/concepts/abyssal_ray_concept.png` | `human_review_design_package_and_record_explicit_approval` |
| `atlantean:crystal_leviathan` | `approval_lock_manual_review_required` | `docs/美术资源/atlantean_creatures/design/crystal_leviathan.md` | `docs/美术资源/atlantean_creatures/concepts/crystal_leviathan_concept.png` | `human_review_design_package_and_record_explicit_approval` |
| `atlantean:orichalcum_golem` | `approval_lock_manual_review_required` | `docs/美术资源/atlantean_creatures/design/orichalcum_golem.md` | `docs/美术资源/atlantean_creatures/concepts/orichalcum_golem_concept.png` | `human_review_design_package_and_record_explicit_approval` |
| `atlantean:siren_priestess` | `approval_lock_manual_review_required` | `docs/美术资源/atlantean_creatures/design/siren_priestess.md` | `docs/美术资源/atlantean_creatures/concepts/siren_priestess_concept.png` | `human_review_design_package_and_record_explicit_approval` |
| `atlantean:tidal_guardian` | `approval_lock_manual_review_required` | `docs/美术资源/atlantean_creatures/design/tidal_guardian.md` | `docs/美术资源/atlantean_creatures/concepts/tidal_guardian_concept.png` | `human_review_design_package_and_record_explicit_approval` |

## 审查问题

### greek:pegasus

- 批次：`B0_DESIGN_BLOCKER_REDRAW`
- 锁级别：`hard_lock_rejected_design`
- 设计包：`docs/美术资源/greek_creatures/design/pegasus.md`
- 概念图：`docs/美术资源/greek_creatures/concepts/pegasus_concept.png`
- 视觉审查板：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/pegasus_visual_review.png`
- 最新候选契约：`docs/美术资源/greek_creatures/design_blueprints/pegasus_v6_modeling_spec_contract.md`
- 最新候选图：`docs/preview_assets/greek_creatures/design_approval/v6/pegasus_v6_modeling_spec_sheet.png`

必须确认：
- [ ] adult horse side silhouette and body length are accepted
- [ ] head_core and muzzle dimensions are accepted as small horse head
- [ ] two-segment sloped neck and visible withers connection are accepted
- [ ] wing roots, saddle_space, and six feather layers are accepted
- [ ] white UV zones include eyes, nostrils, mouth, mane, hooves, and feather edges

审批通过后必须写回：
- `design_status=approved_for_modeling`
- `design_sheet_status=approved_for_modeling`
- `design_approval_status=approved_for_modeling`
- `design_correctness_verdict=approved_for_modeling`
- `design_detail_sheet_status=approved_for_modeling`
- `design_detail_board_status=approved_for_modeling`
- `modeling_allowed=true`

### greek:hydra

- 批次：`B0_DESIGN_BLOCKER_REDRAW`
- 锁级别：`hard_lock_rejected_design`
- 设计包：`docs/美术资源/greek_creatures/design/hydra.md`
- 概念图：`docs/美术资源/greek_creatures/concepts/hydra_concept.png`
- 视觉审查板：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/hydra_visual_review.png`
- 最新候选契约：`docs/美术资源/greek_creatures/design_blueprints/hydra_v6_modeling_spec_contract.md`
- 最新候选图：`docs/preview_assets/greek_creatures/design_approval/v6/hydra_v6_modeling_spec_sheet.png`

必须确认：
- [ ] exactly nine heads and nine sockets are accepted
- [ ] each neck path has root, mid, tip, and head_core overlap
- [ ] low long body and shoulder_saddle socket band are accepted
- [ ] each head has jaw, eye, mouth, fang, and horn or poison gland
- [ ] Java runtime rebuild from five heads to nine heads is required

审批通过后必须写回：
- `design_status=approved_for_modeling`
- `design_sheet_status=approved_for_modeling`
- `design_approval_status=approved_for_modeling`
- `design_correctness_verdict=approved_for_modeling`
- `design_detail_sheet_status=approved_for_modeling`
- `design_detail_board_status=approved_for_modeling`
- `modeling_allowed=true`

### greek:centaur

- 批次：`B1_GREEK_VERTICAL_SLICE_REVIEW`
- 锁级别：`approval_lock_manual_review_required`
- 设计包：`docs/美术资源/greek_creatures/design/centaur.md`
- 概念图：`docs/美术资源/greek_creatures/concepts/centaur_concept.png`
- 视觉审查板：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/centaur_visual_review.png`

必须确认：
- [ ] The silhouette is identifiable without reading the label.
- [ ] Proportion matches the creature role; details are not achieved by oversized heads.
- [ ] Eyes, mouth, weapon/wing/tail/multiple-head features have explicit geometry or UV zones.
- [ ] The design can be built in Blockbench without inventing new major parts.
- [ ] Texture plan avoids blank-looking faces and random panel noise.
- [ ] Animation constraints preserve the silhouette and key connections.

审批通过后必须写回：
- `design_status=approved_for_modeling`
- `design_sheet_status=approved_for_modeling`
- `design_approval_status=approved_for_modeling`
- `design_correctness_verdict=approved_for_modeling`
- `design_detail_sheet_status=approved_for_modeling`
- `design_detail_board_status=approved_for_modeling`
- `modeling_allowed=true`

### greek:chimera

- 批次：`B1_GREEK_VERTICAL_SLICE_REVIEW`
- 锁级别：`approval_lock_manual_review_required`
- 设计包：`docs/美术资源/greek_creatures/design/chimera.md`
- 概念图：`docs/美术资源/greek_creatures/concepts/chimera_concept.png`
- 视觉审查板：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/chimera_visual_review.png`

必须确认：
- [ ] The silhouette is identifiable without reading the label.
- [ ] Proportion matches the creature role; details are not achieved by oversized heads.
- [ ] Eyes, mouth, weapon/wing/tail/multiple-head features have explicit geometry or UV zones.
- [ ] The design can be built in Blockbench without inventing new major parts.
- [ ] Texture plan avoids blank-looking faces and random panel noise.
- [ ] Animation constraints preserve the silhouette and key connections.

审批通过后必须写回：
- `design_status=approved_for_modeling`
- `design_sheet_status=approved_for_modeling`
- `design_approval_status=approved_for_modeling`
- `design_correctness_verdict=approved_for_modeling`
- `design_detail_sheet_status=approved_for_modeling`
- `design_detail_board_status=approved_for_modeling`
- `modeling_allowed=true`

### greek:colossus

- 批次：`B1_GREEK_VERTICAL_SLICE_REVIEW`
- 锁级别：`approval_lock_manual_review_required`
- 设计包：`docs/美术资源/greek_creatures/design/colossus.md`
- 概念图：`docs/美术资源/greek_creatures/concepts/colossus_concept.png`
- 视觉审查板：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/colossus_visual_review.png`

必须确认：
- [ ] The silhouette is identifiable without reading the label.
- [ ] Proportion matches the creature role; details are not achieved by oversized heads.
- [ ] Eyes, mouth, weapon/wing/tail/multiple-head features have explicit geometry or UV zones.
- [ ] The design can be built in Blockbench without inventing new major parts.
- [ ] Texture plan avoids blank-looking faces and random panel noise.
- [ ] Animation constraints preserve the silhouette and key connections.

审批通过后必须写回：
- `design_status=approved_for_modeling`
- `design_sheet_status=approved_for_modeling`
- `design_approval_status=approved_for_modeling`
- `design_correctness_verdict=approved_for_modeling`
- `design_detail_sheet_status=approved_for_modeling`
- `design_detail_board_status=approved_for_modeling`
- `modeling_allowed=true`

### greek:cyclops

- 批次：`B1_GREEK_VERTICAL_SLICE_REVIEW`
- 锁级别：`approval_lock_manual_review_required`
- 设计包：`docs/美术资源/greek_creatures/design/cyclops.md`
- 概念图：`docs/美术资源/greek_creatures/concepts/cyclops_concept.png`
- 视觉审查板：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/cyclops_visual_review.png`

必须确认：
- [ ] The silhouette is identifiable without reading the label.
- [ ] Proportion matches the creature role; details are not achieved by oversized heads.
- [ ] Eyes, mouth, weapon/wing/tail/multiple-head features have explicit geometry or UV zones.
- [ ] The design can be built in Blockbench without inventing new major parts.
- [ ] Texture plan avoids blank-looking faces and random panel noise.
- [ ] Animation constraints preserve the silhouette and key connections.

审批通过后必须写回：
- `design_status=approved_for_modeling`
- `design_sheet_status=approved_for_modeling`
- `design_approval_status=approved_for_modeling`
- `design_correctness_verdict=approved_for_modeling`
- `design_detail_sheet_status=approved_for_modeling`
- `design_detail_board_status=approved_for_modeling`
- `modeling_allowed=true`

### greek:manticore

- 批次：`B1_GREEK_VERTICAL_SLICE_REVIEW`
- 锁级别：`approval_lock_manual_review_required`
- 设计包：`docs/美术资源/greek_creatures/design/manticore.md`
- 概念图：`docs/美术资源/greek_creatures/concepts/manticore_concept.png`
- 视觉审查板：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/manticore_visual_review.png`

必须确认：
- [ ] The silhouette is identifiable without reading the label.
- [ ] Proportion matches the creature role; details are not achieved by oversized heads.
- [ ] Eyes, mouth, weapon/wing/tail/multiple-head features have explicit geometry or UV zones.
- [ ] The design can be built in Blockbench without inventing new major parts.
- [ ] Texture plan avoids blank-looking faces and random panel noise.
- [ ] Animation constraints preserve the silhouette and key connections.

审批通过后必须写回：
- `design_status=approved_for_modeling`
- `design_sheet_status=approved_for_modeling`
- `design_approval_status=approved_for_modeling`
- `design_correctness_verdict=approved_for_modeling`
- `design_detail_sheet_status=approved_for_modeling`
- `design_detail_board_status=approved_for_modeling`
- `modeling_allowed=true`

### greek:medusa

- 批次：`B1_GREEK_VERTICAL_SLICE_REVIEW`
- 锁级别：`approval_lock_manual_review_required`
- 设计包：`docs/美术资源/greek_creatures/design/medusa.md`
- 概念图：`docs/美术资源/greek_creatures/concepts/medusa_concept.png`
- 视觉审查板：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/medusa_visual_review.png`

必须确认：
- [ ] The silhouette is identifiable without reading the label.
- [ ] Proportion matches the creature role; details are not achieved by oversized heads.
- [ ] Eyes, mouth, weapon/wing/tail/multiple-head features have explicit geometry or UV zones.
- [ ] The design can be built in Blockbench without inventing new major parts.
- [ ] Texture plan avoids blank-looking faces and random panel noise.
- [ ] Animation constraints preserve the silhouette and key connections.

审批通过后必须写回：
- `design_status=approved_for_modeling`
- `design_sheet_status=approved_for_modeling`
- `design_approval_status=approved_for_modeling`
- `design_correctness_verdict=approved_for_modeling`
- `design_detail_sheet_status=approved_for_modeling`
- `design_detail_board_status=approved_for_modeling`
- `modeling_allowed=true`

### greek:minotaur

- 批次：`B1_GREEK_VERTICAL_SLICE_REVIEW`
- 锁级别：`approval_lock_manual_review_required`
- 设计包：`docs/美术资源/greek_creatures/design/minotaur.md`
- 概念图：`docs/美术资源/greek_creatures/concepts/minotaur_concept.png`
- 视觉审查板：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/minotaur_visual_review.png`

必须确认：
- [ ] The silhouette is identifiable without reading the label.
- [ ] Proportion matches the creature role; details are not achieved by oversized heads.
- [ ] Eyes, mouth, weapon/wing/tail/multiple-head features have explicit geometry or UV zones.
- [ ] The design can be built in Blockbench without inventing new major parts.
- [ ] Texture plan avoids blank-looking faces and random panel noise.
- [ ] Animation constraints preserve the silhouette and key connections.

审批通过后必须写回：
- `design_status=approved_for_modeling`
- `design_sheet_status=approved_for_modeling`
- `design_approval_status=approved_for_modeling`
- `design_correctness_verdict=approved_for_modeling`
- `design_detail_sheet_status=approved_for_modeling`
- `design_detail_board_status=approved_for_modeling`
- `modeling_allowed=true`

### greek:nemean_lion

- 批次：`B1_GREEK_VERTICAL_SLICE_REVIEW`
- 锁级别：`approval_lock_manual_review_required`
- 设计包：`docs/美术资源/greek_creatures/design/nemean_lion.md`
- 概念图：`docs/美术资源/greek_creatures/concepts/nemean_lion_concept.png`
- 视觉审查板：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/nemean_lion_visual_review.png`

必须确认：
- [ ] The silhouette is identifiable without reading the label.
- [ ] Proportion matches the creature role; details are not achieved by oversized heads.
- [ ] Eyes, mouth, weapon/wing/tail/multiple-head features have explicit geometry or UV zones.
- [ ] The design can be built in Blockbench without inventing new major parts.
- [ ] Texture plan avoids blank-looking faces and random panel noise.
- [ ] Animation constraints preserve the silhouette and key connections.

审批通过后必须写回：
- `design_status=approved_for_modeling`
- `design_sheet_status=approved_for_modeling`
- `design_approval_status=approved_for_modeling`
- `design_correctness_verdict=approved_for_modeling`
- `design_detail_sheet_status=approved_for_modeling`
- `design_detail_board_status=approved_for_modeling`
- `modeling_allowed=true`

### egyptian:anubis_warrior

- 批次：`B2_NON_GREEK_DESIGN_APPROVAL`
- 锁级别：`approval_lock_manual_review_required`
- 设计包：`docs/美术资源/egyptian_creatures/design/anubis_warrior.md`
- 概念图：`docs/美术资源/egyptian_creatures/concepts/anubis_warrior_concept.png`
- 视觉审查板：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/egyptian/anubis_warrior_visual_review.png`

必须确认：
- [ ] The silhouette is identifiable without reading the label.
- [ ] Proportion matches the creature role; details are not achieved by oversized heads.
- [ ] Eyes, mouth, weapon/wing/tail/multiple-head features have explicit geometry or UV zones.
- [ ] The design can be built in Blockbench without inventing new major parts.
- [ ] Texture plan avoids blank-looking faces and random panel noise.
- [ ] Animation constraints preserve the silhouette and key connections.

审批通过后必须写回：
- `design_status=approved_for_modeling`
- `design_sheet_status=approved_for_modeling`
- `design_approval_status=approved_for_modeling`
- `design_correctness_verdict=approved_for_modeling`
- `design_detail_sheet_status=approved_for_modeling`
- `design_detail_board_status=approved_for_modeling`
- `modeling_allowed=true`

### egyptian:mummy_soldier

- 批次：`B2_NON_GREEK_DESIGN_APPROVAL`
- 锁级别：`approval_lock_manual_review_required`
- 设计包：`docs/美术资源/egyptian_creatures/design/mummy_soldier.md`
- 概念图：`docs/美术资源/egyptian_creatures/concepts/mummy_soldier_concept.png`
- 视觉审查板：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/egyptian/mummy_soldier_visual_review.png`

必须确认：
- [ ] The silhouette is identifiable without reading the label.
- [ ] Proportion matches the creature role; details are not achieved by oversized heads.
- [ ] Eyes, mouth, weapon/wing/tail/multiple-head features have explicit geometry or UV zones.
- [ ] The design can be built in Blockbench without inventing new major parts.
- [ ] Texture plan avoids blank-looking faces and random panel noise.
- [ ] Animation constraints preserve the silhouette and key connections.

审批通过后必须写回：
- `design_status=approved_for_modeling`
- `design_sheet_status=approved_for_modeling`
- `design_approval_status=approved_for_modeling`
- `design_correctness_verdict=approved_for_modeling`
- `design_detail_sheet_status=approved_for_modeling`
- `design_detail_board_status=approved_for_modeling`
- `modeling_allowed=true`

### egyptian:scarab_guardian

- 批次：`B2_NON_GREEK_DESIGN_APPROVAL`
- 锁级别：`approval_lock_manual_review_required`
- 设计包：`docs/美术资源/egyptian_creatures/design/scarab_guardian.md`
- 概念图：`docs/美术资源/egyptian_creatures/concepts/scarab_guardian_concept.png`
- 视觉审查板：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/egyptian/scarab_guardian_visual_review.png`

必须确认：
- [ ] The silhouette is identifiable without reading the label.
- [ ] Proportion matches the creature role; details are not achieved by oversized heads.
- [ ] Eyes, mouth, weapon/wing/tail/multiple-head features have explicit geometry or UV zones.
- [ ] The design can be built in Blockbench without inventing new major parts.
- [ ] Texture plan avoids blank-looking faces and random panel noise.
- [ ] Animation constraints preserve the silhouette and key connections.

审批通过后必须写回：
- `design_status=approved_for_modeling`
- `design_sheet_status=approved_for_modeling`
- `design_approval_status=approved_for_modeling`
- `design_correctness_verdict=approved_for_modeling`
- `design_detail_sheet_status=approved_for_modeling`
- `design_detail_board_status=approved_for_modeling`
- `modeling_allowed=true`

### egyptian:sphinx_sentinel

- 批次：`B2_NON_GREEK_DESIGN_APPROVAL`
- 锁级别：`approval_lock_manual_review_required`
- 设计包：`docs/美术资源/egyptian_creatures/design/sphinx_sentinel.md`
- 概念图：`docs/美术资源/egyptian_creatures/concepts/sphinx_sentinel_concept.png`
- 视觉审查板：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/egyptian/sphinx_sentinel_visual_review.png`

必须确认：
- [ ] The silhouette is identifiable without reading the label.
- [ ] Proportion matches the creature role; details are not achieved by oversized heads.
- [ ] Eyes, mouth, weapon/wing/tail/multiple-head features have explicit geometry or UV zones.
- [ ] The design can be built in Blockbench without inventing new major parts.
- [ ] Texture plan avoids blank-looking faces and random panel noise.
- [ ] Animation constraints preserve the silhouette and key connections.

审批通过后必须写回：
- `design_status=approved_for_modeling`
- `design_sheet_status=approved_for_modeling`
- `design_approval_status=approved_for_modeling`
- `design_correctness_verdict=approved_for_modeling`
- `design_detail_sheet_status=approved_for_modeling`
- `design_detail_board_status=approved_for_modeling`
- `modeling_allowed=true`

### egyptian:sun_falcon

- 批次：`B2_NON_GREEK_DESIGN_APPROVAL`
- 锁级别：`approval_lock_manual_review_required`
- 设计包：`docs/美术资源/egyptian_creatures/design/sun_falcon.md`
- 概念图：`docs/美术资源/egyptian_creatures/concepts/sun_falcon_concept.png`
- 视觉审查板：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/egyptian/sun_falcon_visual_review.png`

必须确认：
- [ ] The silhouette is identifiable without reading the label.
- [ ] Proportion matches the creature role; details are not achieved by oversized heads.
- [ ] Eyes, mouth, weapon/wing/tail/multiple-head features have explicit geometry or UV zones.
- [ ] The design can be built in Blockbench without inventing new major parts.
- [ ] Texture plan avoids blank-looking faces and random panel noise.
- [ ] Animation constraints preserve the silhouette and key connections.

审批通过后必须写回：
- `design_status=approved_for_modeling`
- `design_sheet_status=approved_for_modeling`
- `design_approval_status=approved_for_modeling`
- `design_correctness_verdict=approved_for_modeling`
- `design_detail_sheet_status=approved_for_modeling`
- `design_detail_board_status=approved_for_modeling`
- `modeling_allowed=true`

### nordic:draugr_warrior

- 批次：`B2_NON_GREEK_DESIGN_APPROVAL`
- 锁级别：`approval_lock_manual_review_required`
- 设计包：`docs/美术资源/nordic_creatures/design/draugr_warrior.md`
- 概念图：`docs/美术资源/nordic_creatures/concepts/draugr_warrior_concept.png`
- 视觉审查板：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/nordic/draugr_warrior_visual_review.png`

必须确认：
- [ ] The silhouette is identifiable without reading the label.
- [ ] Proportion matches the creature role; details are not achieved by oversized heads.
- [ ] Eyes, mouth, weapon/wing/tail/multiple-head features have explicit geometry or UV zones.
- [ ] The design can be built in Blockbench without inventing new major parts.
- [ ] Texture plan avoids blank-looking faces and random panel noise.
- [ ] Animation constraints preserve the silhouette and key connections.

审批通过后必须写回：
- `design_status=approved_for_modeling`
- `design_sheet_status=approved_for_modeling`
- `design_approval_status=approved_for_modeling`
- `design_correctness_verdict=approved_for_modeling`
- `design_detail_sheet_status=approved_for_modeling`
- `design_detail_board_status=approved_for_modeling`
- `modeling_allowed=true`

### nordic:dwarf_berserker

- 批次：`B2_NON_GREEK_DESIGN_APPROVAL`
- 锁级别：`approval_lock_manual_review_required`
- 设计包：`docs/美术资源/nordic_creatures/design/dwarf_berserker.md`
- 概念图：`docs/美术资源/nordic_creatures/concepts/dwarf_berserker_concept.png`
- 视觉审查板：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/nordic/dwarf_berserker_visual_review.png`

必须确认：
- [ ] The silhouette is identifiable without reading the label.
- [ ] Proportion matches the creature role; details are not achieved by oversized heads.
- [ ] Eyes, mouth, weapon/wing/tail/multiple-head features have explicit geometry or UV zones.
- [ ] The design can be built in Blockbench without inventing new major parts.
- [ ] Texture plan avoids blank-looking faces and random panel noise.
- [ ] Animation constraints preserve the silhouette and key connections.

审批通过后必须写回：
- `design_status=approved_for_modeling`
- `design_sheet_status=approved_for_modeling`
- `design_approval_status=approved_for_modeling`
- `design_correctness_verdict=approved_for_modeling`
- `design_detail_sheet_status=approved_for_modeling`
- `design_detail_board_status=approved_for_modeling`
- `modeling_allowed=true`

### nordic:fenrir_wolf

- 批次：`B2_NON_GREEK_DESIGN_APPROVAL`
- 锁级别：`approval_lock_manual_review_required`
- 设计包：`docs/美术资源/nordic_creatures/design/fenrir_wolf.md`
- 概念图：`docs/美术资源/nordic_creatures/concepts/fenrir_wolf_concept.png`
- 视觉审查板：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/nordic/fenrir_wolf_visual_review.png`

必须确认：
- [ ] The silhouette is identifiable without reading the label.
- [ ] Proportion matches the creature role; details are not achieved by oversized heads.
- [ ] Eyes, mouth, weapon/wing/tail/multiple-head features have explicit geometry or UV zones.
- [ ] The design can be built in Blockbench without inventing new major parts.
- [ ] Texture plan avoids blank-looking faces and random panel noise.
- [ ] Animation constraints preserve the silhouette and key connections.

审批通过后必须写回：
- `design_status=approved_for_modeling`
- `design_sheet_status=approved_for_modeling`
- `design_approval_status=approved_for_modeling`
- `design_correctness_verdict=approved_for_modeling`
- `design_detail_sheet_status=approved_for_modeling`
- `design_detail_board_status=approved_for_modeling`
- `modeling_allowed=true`

### nordic:frost_giant

- 批次：`B2_NON_GREEK_DESIGN_APPROVAL`
- 锁级别：`approval_lock_manual_review_required`
- 设计包：`docs/美术资源/nordic_creatures/design/frost_giant.md`
- 概念图：`docs/美术资源/nordic_creatures/concepts/frost_giant_concept.png`
- 视觉审查板：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/nordic/frost_giant_visual_review.png`

必须确认：
- [ ] The silhouette is identifiable without reading the label.
- [ ] Proportion matches the creature role; details are not achieved by oversized heads.
- [ ] Eyes, mouth, weapon/wing/tail/multiple-head features have explicit geometry or UV zones.
- [ ] The design can be built in Blockbench without inventing new major parts.
- [ ] Texture plan avoids blank-looking faces and random panel noise.
- [ ] Animation constraints preserve the silhouette and key connections.

审批通过后必须写回：
- `design_status=approved_for_modeling`
- `design_sheet_status=approved_for_modeling`
- `design_approval_status=approved_for_modeling`
- `design_correctness_verdict=approved_for_modeling`
- `design_detail_sheet_status=approved_for_modeling`
- `design_detail_board_status=approved_for_modeling`
- `modeling_allowed=true`

### nordic:valkyrie

- 批次：`B2_NON_GREEK_DESIGN_APPROVAL`
- 锁级别：`approval_lock_manual_review_required`
- 设计包：`docs/美术资源/nordic_creatures/design/valkyrie.md`
- 概念图：`docs/美术资源/nordic_creatures/concepts/valkyrie_concept.png`
- 视觉审查板：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/nordic/valkyrie_visual_review.png`

必须确认：
- [ ] The silhouette is identifiable without reading the label.
- [ ] Proportion matches the creature role; details are not achieved by oversized heads.
- [ ] Eyes, mouth, weapon/wing/tail/multiple-head features have explicit geometry or UV zones.
- [ ] The design can be built in Blockbench without inventing new major parts.
- [ ] Texture plan avoids blank-looking faces and random panel noise.
- [ ] Animation constraints preserve the silhouette and key connections.

审批通过后必须写回：
- `design_status=approved_for_modeling`
- `design_sheet_status=approved_for_modeling`
- `design_approval_status=approved_for_modeling`
- `design_correctness_verdict=approved_for_modeling`
- `design_detail_sheet_status=approved_for_modeling`
- `design_detail_board_status=approved_for_modeling`
- `modeling_allowed=true`

### chinese:azure_dragon

- 批次：`B2_NON_GREEK_DESIGN_APPROVAL`
- 锁级别：`approval_lock_manual_review_required`
- 设计包：`docs/美术资源/chinese_creatures/design/azure_dragon.md`
- 概念图：`docs/美术资源/chinese_creatures/concepts/azure_dragon_concept.png`
- 视觉审查板：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/chinese/azure_dragon_visual_review.png`

必须确认：
- [ ] The silhouette is identifiable without reading the label.
- [ ] Proportion matches the creature role; details are not achieved by oversized heads.
- [ ] Eyes, mouth, weapon/wing/tail/multiple-head features have explicit geometry or UV zones.
- [ ] The design can be built in Blockbench without inventing new major parts.
- [ ] Texture plan avoids blank-looking faces and random panel noise.
- [ ] Animation constraints preserve the silhouette and key connections.

审批通过后必须写回：
- `design_status=approved_for_modeling`
- `design_sheet_status=approved_for_modeling`
- `design_approval_status=approved_for_modeling`
- `design_correctness_verdict=approved_for_modeling`
- `design_detail_sheet_status=approved_for_modeling`
- `design_detail_board_status=approved_for_modeling`
- `modeling_allowed=true`

### chinese:jiangshi

- 批次：`B2_NON_GREEK_DESIGN_APPROVAL`
- 锁级别：`approval_lock_manual_review_required`
- 设计包：`docs/美术资源/chinese_creatures/design/jiangshi.md`
- 概念图：`docs/美术资源/chinese_creatures/concepts/jiangshi_concept.png`
- 视觉审查板：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/chinese/jiangshi_visual_review.png`

必须确认：
- [ ] The silhouette is identifiable without reading the label.
- [ ] Proportion matches the creature role; details are not achieved by oversized heads.
- [ ] Eyes, mouth, weapon/wing/tail/multiple-head features have explicit geometry or UV zones.
- [ ] The design can be built in Blockbench without inventing new major parts.
- [ ] Texture plan avoids blank-looking faces and random panel noise.
- [ ] Animation constraints preserve the silhouette and key connections.

审批通过后必须写回：
- `design_status=approved_for_modeling`
- `design_sheet_status=approved_for_modeling`
- `design_approval_status=approved_for_modeling`
- `design_correctness_verdict=approved_for_modeling`
- `design_detail_sheet_status=approved_for_modeling`
- `design_detail_board_status=approved_for_modeling`
- `modeling_allowed=true`

### chinese:nine_tailed_fox

- 批次：`B2_NON_GREEK_DESIGN_APPROVAL`
- 锁级别：`approval_lock_manual_review_required`
- 设计包：`docs/美术资源/chinese_creatures/design/nine_tailed_fox.md`
- 概念图：`docs/美术资源/chinese_creatures/concepts/nine_tailed_fox_concept.png`
- 视觉审查板：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/chinese/nine_tailed_fox_visual_review.png`

必须确认：
- [ ] The silhouette is identifiable without reading the label.
- [ ] Proportion matches the creature role; details are not achieved by oversized heads.
- [ ] Eyes, mouth, weapon/wing/tail/multiple-head features have explicit geometry or UV zones.
- [ ] The design can be built in Blockbench without inventing new major parts.
- [ ] Texture plan avoids blank-looking faces and random panel noise.
- [ ] Animation constraints preserve the silhouette and key connections.

审批通过后必须写回：
- `design_status=approved_for_modeling`
- `design_sheet_status=approved_for_modeling`
- `design_approval_status=approved_for_modeling`
- `design_correctness_verdict=approved_for_modeling`
- `design_detail_sheet_status=approved_for_modeling`
- `design_detail_board_status=approved_for_modeling`
- `modeling_allowed=true`

### chinese:qilin_guardian

- 批次：`B2_NON_GREEK_DESIGN_APPROVAL`
- 锁级别：`approval_lock_manual_review_required`
- 设计包：`docs/美术资源/chinese_creatures/design/qilin_guardian.md`
- 概念图：`docs/美术资源/chinese_creatures/concepts/qilin_guardian_concept.png`
- 视觉审查板：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/chinese/qilin_guardian_visual_review.png`

必须确认：
- [ ] The silhouette is identifiable without reading the label.
- [ ] Proportion matches the creature role; details are not achieved by oversized heads.
- [ ] Eyes, mouth, weapon/wing/tail/multiple-head features have explicit geometry or UV zones.
- [ ] The design can be built in Blockbench without inventing new major parts.
- [ ] Texture plan avoids blank-looking faces and random panel noise.
- [ ] Animation constraints preserve the silhouette and key connections.

审批通过后必须写回：
- `design_status=approved_for_modeling`
- `design_sheet_status=approved_for_modeling`
- `design_approval_status=approved_for_modeling`
- `design_correctness_verdict=approved_for_modeling`
- `design_detail_sheet_status=approved_for_modeling`
- `design_detail_board_status=approved_for_modeling`
- `modeling_allowed=true`

### chinese:stone_lion_guardian

- 批次：`B2_NON_GREEK_DESIGN_APPROVAL`
- 锁级别：`approval_lock_manual_review_required`
- 设计包：`docs/美术资源/chinese_creatures/design/stone_lion_guardian.md`
- 概念图：`docs/美术资源/chinese_creatures/concepts/stone_lion_guardian_concept.png`
- 视觉审查板：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/chinese/stone_lion_guardian_visual_review.png`

必须确认：
- [ ] The silhouette is identifiable without reading the label.
- [ ] Proportion matches the creature role; details are not achieved by oversized heads.
- [ ] Eyes, mouth, weapon/wing/tail/multiple-head features have explicit geometry or UV zones.
- [ ] The design can be built in Blockbench without inventing new major parts.
- [ ] Texture plan avoids blank-looking faces and random panel noise.
- [ ] Animation constraints preserve the silhouette and key connections.

审批通过后必须写回：
- `design_status=approved_for_modeling`
- `design_sheet_status=approved_for_modeling`
- `design_approval_status=approved_for_modeling`
- `design_correctness_verdict=approved_for_modeling`
- `design_detail_sheet_status=approved_for_modeling`
- `design_detail_board_status=approved_for_modeling`
- `modeling_allowed=true`

### atlantean:abyssal_ray

- 批次：`B2_NON_GREEK_DESIGN_APPROVAL`
- 锁级别：`approval_lock_manual_review_required`
- 设计包：`docs/美术资源/atlantean_creatures/design/abyssal_ray.md`
- 概念图：`docs/美术资源/atlantean_creatures/concepts/abyssal_ray_concept.png`
- 视觉审查板：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/atlantean/abyssal_ray_visual_review.png`

必须确认：
- [ ] The silhouette is identifiable without reading the label.
- [ ] Proportion matches the creature role; details are not achieved by oversized heads.
- [ ] Eyes, mouth, weapon/wing/tail/multiple-head features have explicit geometry or UV zones.
- [ ] The design can be built in Blockbench without inventing new major parts.
- [ ] Texture plan avoids blank-looking faces and random panel noise.
- [ ] Animation constraints preserve the silhouette and key connections.

审批通过后必须写回：
- `design_status=approved_for_modeling`
- `design_sheet_status=approved_for_modeling`
- `design_approval_status=approved_for_modeling`
- `design_correctness_verdict=approved_for_modeling`
- `design_detail_sheet_status=approved_for_modeling`
- `design_detail_board_status=approved_for_modeling`
- `modeling_allowed=true`

### atlantean:crystal_leviathan

- 批次：`B2_NON_GREEK_DESIGN_APPROVAL`
- 锁级别：`approval_lock_manual_review_required`
- 设计包：`docs/美术资源/atlantean_creatures/design/crystal_leviathan.md`
- 概念图：`docs/美术资源/atlantean_creatures/concepts/crystal_leviathan_concept.png`
- 视觉审查板：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/atlantean/crystal_leviathan_visual_review.png`

必须确认：
- [ ] The silhouette is identifiable without reading the label.
- [ ] Proportion matches the creature role; details are not achieved by oversized heads.
- [ ] Eyes, mouth, weapon/wing/tail/multiple-head features have explicit geometry or UV zones.
- [ ] The design can be built in Blockbench without inventing new major parts.
- [ ] Texture plan avoids blank-looking faces and random panel noise.
- [ ] Animation constraints preserve the silhouette and key connections.

审批通过后必须写回：
- `design_status=approved_for_modeling`
- `design_sheet_status=approved_for_modeling`
- `design_approval_status=approved_for_modeling`
- `design_correctness_verdict=approved_for_modeling`
- `design_detail_sheet_status=approved_for_modeling`
- `design_detail_board_status=approved_for_modeling`
- `modeling_allowed=true`

### atlantean:orichalcum_golem

- 批次：`B2_NON_GREEK_DESIGN_APPROVAL`
- 锁级别：`approval_lock_manual_review_required`
- 设计包：`docs/美术资源/atlantean_creatures/design/orichalcum_golem.md`
- 概念图：`docs/美术资源/atlantean_creatures/concepts/orichalcum_golem_concept.png`
- 视觉审查板：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/atlantean/orichalcum_golem_visual_review.png`

必须确认：
- [ ] The silhouette is identifiable without reading the label.
- [ ] Proportion matches the creature role; details are not achieved by oversized heads.
- [ ] Eyes, mouth, weapon/wing/tail/multiple-head features have explicit geometry or UV zones.
- [ ] The design can be built in Blockbench without inventing new major parts.
- [ ] Texture plan avoids blank-looking faces and random panel noise.
- [ ] Animation constraints preserve the silhouette and key connections.

审批通过后必须写回：
- `design_status=approved_for_modeling`
- `design_sheet_status=approved_for_modeling`
- `design_approval_status=approved_for_modeling`
- `design_correctness_verdict=approved_for_modeling`
- `design_detail_sheet_status=approved_for_modeling`
- `design_detail_board_status=approved_for_modeling`
- `modeling_allowed=true`

### atlantean:siren_priestess

- 批次：`B2_NON_GREEK_DESIGN_APPROVAL`
- 锁级别：`approval_lock_manual_review_required`
- 设计包：`docs/美术资源/atlantean_creatures/design/siren_priestess.md`
- 概念图：`docs/美术资源/atlantean_creatures/concepts/siren_priestess_concept.png`
- 视觉审查板：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/atlantean/siren_priestess_visual_review.png`

必须确认：
- [ ] The silhouette is identifiable without reading the label.
- [ ] Proportion matches the creature role; details are not achieved by oversized heads.
- [ ] Eyes, mouth, weapon/wing/tail/multiple-head features have explicit geometry or UV zones.
- [ ] The design can be built in Blockbench without inventing new major parts.
- [ ] Texture plan avoids blank-looking faces and random panel noise.
- [ ] Animation constraints preserve the silhouette and key connections.

审批通过后必须写回：
- `design_status=approved_for_modeling`
- `design_sheet_status=approved_for_modeling`
- `design_approval_status=approved_for_modeling`
- `design_correctness_verdict=approved_for_modeling`
- `design_detail_sheet_status=approved_for_modeling`
- `design_detail_board_status=approved_for_modeling`
- `modeling_allowed=true`

### atlantean:tidal_guardian

- 批次：`B2_NON_GREEK_DESIGN_APPROVAL`
- 锁级别：`approval_lock_manual_review_required`
- 设计包：`docs/美术资源/atlantean_creatures/design/tidal_guardian.md`
- 概念图：`docs/美术资源/atlantean_creatures/concepts/tidal_guardian_concept.png`
- 视觉审查板：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/atlantean/tidal_guardian_visual_review.png`

必须确认：
- [ ] The silhouette is identifiable without reading the label.
- [ ] Proportion matches the creature role; details are not achieved by oversized heads.
- [ ] Eyes, mouth, weapon/wing/tail/multiple-head features have explicit geometry or UV zones.
- [ ] The design can be built in Blockbench without inventing new major parts.
- [ ] Texture plan avoids blank-looking faces and random panel noise.
- [ ] Animation constraints preserve the silhouette and key connections.

审批通过后必须写回：
- `design_status=approved_for_modeling`
- `design_sheet_status=approved_for_modeling`
- `design_approval_status=approved_for_modeling`
- `design_correctness_verdict=approved_for_modeling`
- `design_detail_sheet_status=approved_for_modeling`
- `design_detail_board_status=approved_for_modeling`
- `modeling_allowed=true`

## 退出口径

- 30 个生物全部通过设计审批后，生产锁才可能解除。
- Pegasus/Hydra 必须先解除 `hard_lock_rejected_design`。
- 非希腊 20 生物必须先完成设计审批，再进入 B3 模型/UV/纹理生产。
- 本审批总包通过不等于 1.0 完成；后续还需要模型、动作、纹理、UI、粒子、音效、NBT 和实机验收闭环。
