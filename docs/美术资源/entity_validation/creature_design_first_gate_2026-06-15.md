# MythicAgeMod 1.0 全生物设计先行门禁（2026-06-15）

本文档由 `tools/creature_design_first_gate.py audit` 生成。它检查 30 个 1.0 生物是否真的完成“设计先行审批”，而不只是在流水线里生成了设计包、概念图和预览。

## 总览

- 门禁通过：`False`
- 生物数量：30
- 正式批准可建模：0
- 设计拒绝/阻断：2
- 仅有设计包但未人工签收：28
- 章节不完整：0
- 资产证据不完整：0
- 状态统计：`{"blocked_current_design_rejected_v6_spec_pending": 2, "manual_design_approval_required": 28}`

## 判定规则

- A creature is design-first ready only when its design is formally approved for modeling. Generated design docs, concept art, Blockbench previews, and runtime model gates are evidence, not approval.
- `pegasus` 和 `hydra` 当前仍由 V6 可建模规格签收 gate 阻断；旧 V2/当前产物不能继续作为建模依据。
- 其余生物虽然有设计包和预览，但缺正式 `approved_for_modeling` 记录，仍需人工设计签收。

## 生物列表

| 文明 | 生物 | 状态 | 设计包 | 章节完整 | 资产完整 | 下一步 |
| --- | --- | --- | --- | --- | --- | --- |
| greek | `minotaur` | `manual_design_approval_required` | `docs/美术资源/greek_creatures/design/minotaur.md` | `True` | `True` | `human_review_design_package_and_record_approval` |
| greek | `cyclops` | `manual_design_approval_required` | `docs/美术资源/greek_creatures/design/cyclops.md` | `True` | `True` | `human_review_design_package_and_record_approval` |
| greek | `centaur` | `manual_design_approval_required` | `docs/美术资源/greek_creatures/design/centaur.md` | `True` | `True` | `human_review_design_package_and_record_approval` |
| greek | `hydra` | `blocked_current_design_rejected_v6_spec_pending` | `docs/美术资源/greek_creatures/design/hydra.md` | `True` | `True` | `approve_or_rewrite_design_before_modeling` |
| greek | `medusa` | `manual_design_approval_required` | `docs/美术资源/greek_creatures/design/medusa.md` | `True` | `True` | `human_review_design_package_and_record_approval` |
| greek | `nemean_lion` | `manual_design_approval_required` | `docs/美术资源/greek_creatures/design/nemean_lion.md` | `True` | `True` | `human_review_design_package_and_record_approval` |
| greek | `manticore` | `manual_design_approval_required` | `docs/美术资源/greek_creatures/design/manticore.md` | `True` | `True` | `human_review_design_package_and_record_approval` |
| greek | `chimera` | `manual_design_approval_required` | `docs/美术资源/greek_creatures/design/chimera.md` | `True` | `True` | `human_review_design_package_and_record_approval` |
| greek | `colossus` | `manual_design_approval_required` | `docs/美术资源/greek_creatures/design/colossus.md` | `True` | `True` | `human_review_design_package_and_record_approval` |
| greek | `pegasus` | `blocked_current_design_rejected_v6_spec_pending` | `docs/美术资源/greek_creatures/design/pegasus.md` | `True` | `True` | `approve_or_rewrite_design_before_modeling` |
| egyptian | `anubis_warrior` | `manual_design_approval_required` | `docs/美术资源/egyptian_creatures/design/anubis_warrior.md` | `True` | `True` | `human_review_design_package_and_record_approval` |
| egyptian | `scarab_guardian` | `manual_design_approval_required` | `docs/美术资源/egyptian_creatures/design/scarab_guardian.md` | `True` | `True` | `human_review_design_package_and_record_approval` |
| egyptian | `mummy_soldier` | `manual_design_approval_required` | `docs/美术资源/egyptian_creatures/design/mummy_soldier.md` | `True` | `True` | `human_review_design_package_and_record_approval` |
| egyptian | `sphinx_sentinel` | `manual_design_approval_required` | `docs/美术资源/egyptian_creatures/design/sphinx_sentinel.md` | `True` | `True` | `human_review_design_package_and_record_approval` |
| egyptian | `sun_falcon` | `manual_design_approval_required` | `docs/美术资源/egyptian_creatures/design/sun_falcon.md` | `True` | `True` | `human_review_design_package_and_record_approval` |
| nordic | `frost_giant` | `manual_design_approval_required` | `docs/美术资源/nordic_creatures/design/frost_giant.md` | `True` | `True` | `human_review_design_package_and_record_approval` |
| nordic | `draugr_warrior` | `manual_design_approval_required` | `docs/美术资源/nordic_creatures/design/draugr_warrior.md` | `True` | `True` | `human_review_design_package_and_record_approval` |
| nordic | `valkyrie` | `manual_design_approval_required` | `docs/美术资源/nordic_creatures/design/valkyrie.md` | `True` | `True` | `human_review_design_package_and_record_approval` |
| nordic | `fenrir_wolf` | `manual_design_approval_required` | `docs/美术资源/nordic_creatures/design/fenrir_wolf.md` | `True` | `True` | `human_review_design_package_and_record_approval` |
| nordic | `dwarf_berserker` | `manual_design_approval_required` | `docs/美术资源/nordic_creatures/design/dwarf_berserker.md` | `True` | `True` | `human_review_design_package_and_record_approval` |
| chinese | `azure_dragon` | `manual_design_approval_required` | `docs/美术资源/chinese_creatures/design/azure_dragon.md` | `True` | `True` | `human_review_design_package_and_record_approval` |
| chinese | `qilin_guardian` | `manual_design_approval_required` | `docs/美术资源/chinese_creatures/design/qilin_guardian.md` | `True` | `True` | `human_review_design_package_and_record_approval` |
| chinese | `jiangshi` | `manual_design_approval_required` | `docs/美术资源/chinese_creatures/design/jiangshi.md` | `True` | `True` | `human_review_design_package_and_record_approval` |
| chinese | `nine_tailed_fox` | `manual_design_approval_required` | `docs/美术资源/chinese_creatures/design/nine_tailed_fox.md` | `True` | `True` | `human_review_design_package_and_record_approval` |
| chinese | `stone_lion_guardian` | `manual_design_approval_required` | `docs/美术资源/chinese_creatures/design/stone_lion_guardian.md` | `True` | `True` | `human_review_design_package_and_record_approval` |
| atlantean | `crystal_leviathan` | `manual_design_approval_required` | `docs/美术资源/atlantean_creatures/design/crystal_leviathan.md` | `True` | `True` | `human_review_design_package_and_record_approval` |
| atlantean | `orichalcum_golem` | `manual_design_approval_required` | `docs/美术资源/atlantean_creatures/design/orichalcum_golem.md` | `True` | `True` | `human_review_design_package_and_record_approval` |
| atlantean | `siren_priestess` | `manual_design_approval_required` | `docs/美术资源/atlantean_creatures/design/siren_priestess.md` | `True` | `True` | `human_review_design_package_and_record_approval` |
| atlantean | `tidal_guardian` | `manual_design_approval_required` | `docs/美术资源/atlantean_creatures/design/tidal_guardian.md` | `True` | `True` | `human_review_design_package_and_record_approval` |
| atlantean | `abyssal_ray` | `manual_design_approval_required` | `docs/美术资源/atlantean_creatures/design/abyssal_ray.md` | `True` | `True` | `human_review_design_package_and_record_approval` |

## 阻断详情

### greek / minotaur

- 状态：`manual_design_approval_required`
- 原因：Design package exists, but it has not been formally approved for modeling.
- 设计包：`docs/美术资源/greek_creatures/design/minotaur.md`
- 审批缺口：`["design_status='needs_user_review'", "design_sheet_status='needs_user_review'", "design_approval_status='approval_pending'", "design_correctness_verdict='not_reviewed'", "design_detail_sheet_status='needs_user_review'", "design_detail_board_status='needs_user_review'", "modeling_allowed=false"]`
- 下一步：`human_review_design_package_and_record_approval`

### greek / cyclops

- 状态：`manual_design_approval_required`
- 原因：Design package exists, but it has not been formally approved for modeling.
- 设计包：`docs/美术资源/greek_creatures/design/cyclops.md`
- 审批缺口：`["design_status='needs_user_review'", "design_sheet_status='needs_user_review'", "design_approval_status='approval_pending'", "design_correctness_verdict='not_reviewed'", "design_detail_sheet_status='needs_user_review'", "design_detail_board_status='needs_user_review'", "modeling_allowed=false"]`
- 下一步：`human_review_design_package_and_record_approval`

### greek / centaur

- 状态：`manual_design_approval_required`
- 原因：Design package exists, but it has not been formally approved for modeling.
- 设计包：`docs/美术资源/greek_creatures/design/centaur.md`
- 审批缺口：`["design_status='needs_user_review'", "design_sheet_status='needs_user_review'", "design_approval_status='approval_pending'", "design_correctness_verdict='not_reviewed'", "design_detail_sheet_status='needs_user_review'", "design_detail_board_status='needs_user_review'", "modeling_allowed=false"]`
- 下一步：`human_review_design_package_and_record_approval`

### greek / hydra

- 状态：`blocked_current_design_rejected_v6_spec_pending`
- 原因：Current V2/reference design is rejected; V6 modeling spec exists but is not approved for modeling.
- 设计包：`docs/美术资源/greek_creatures/design/hydra.md`
- 审批缺口：`["design_status='modeling_spec_ready_for_user_review'", "design_sheet_status=None", "design_approval_status=None", "design_correctness_verdict=None", "design_detail_sheet_status=None", "design_detail_board_status=None", "modeling_allowed=false"]`
- V6 可建模规格合同：`docs/美术资源/greek_creatures/design_blueprints/hydra_v6_modeling_spec_contract.md`
- 下一步：`approve_or_rewrite_design_before_modeling`

### greek / medusa

- 状态：`manual_design_approval_required`
- 原因：Design package exists, but it has not been formally approved for modeling.
- 设计包：`docs/美术资源/greek_creatures/design/medusa.md`
- 审批缺口：`["design_status='needs_user_review'", "design_sheet_status='needs_user_review'", "design_approval_status='approval_pending'", "design_correctness_verdict='not_reviewed'", "design_detail_sheet_status='needs_user_review'", "design_detail_board_status='needs_user_review'", "modeling_allowed=false"]`
- 下一步：`human_review_design_package_and_record_approval`

### greek / nemean_lion

- 状态：`manual_design_approval_required`
- 原因：Design package exists, but it has not been formally approved for modeling.
- 设计包：`docs/美术资源/greek_creatures/design/nemean_lion.md`
- 审批缺口：`["design_status='needs_user_review'", "design_sheet_status='needs_user_review'", "design_approval_status='approval_pending'", "design_correctness_verdict='not_reviewed'", "design_detail_sheet_status='needs_user_review'", "design_detail_board_status='needs_user_review'", "modeling_allowed=false"]`
- 下一步：`human_review_design_package_and_record_approval`

### greek / manticore

- 状态：`manual_design_approval_required`
- 原因：Design package exists, but it has not been formally approved for modeling.
- 设计包：`docs/美术资源/greek_creatures/design/manticore.md`
- 审批缺口：`["design_status='needs_user_review'", "design_sheet_status='needs_user_review'", "design_approval_status='approval_pending'", "design_correctness_verdict='not_reviewed'", "design_detail_sheet_status='needs_user_review'", "design_detail_board_status='needs_user_review'", "modeling_allowed=false"]`
- 下一步：`human_review_design_package_and_record_approval`

### greek / chimera

- 状态：`manual_design_approval_required`
- 原因：Design package exists, but it has not been formally approved for modeling.
- 设计包：`docs/美术资源/greek_creatures/design/chimera.md`
- 审批缺口：`["design_status='needs_user_review'", "design_sheet_status='needs_user_review'", "design_approval_status='approval_pending'", "design_correctness_verdict='not_reviewed'", "design_detail_sheet_status='needs_user_review'", "design_detail_board_status='needs_user_review'", "modeling_allowed=false"]`
- 下一步：`human_review_design_package_and_record_approval`

### greek / colossus

- 状态：`manual_design_approval_required`
- 原因：Design package exists, but it has not been formally approved for modeling.
- 设计包：`docs/美术资源/greek_creatures/design/colossus.md`
- 审批缺口：`["design_status='needs_user_review'", "design_sheet_status='needs_user_review'", "design_approval_status='approval_pending'", "design_correctness_verdict='not_reviewed'", "design_detail_sheet_status='needs_user_review'", "design_detail_board_status='needs_user_review'", "modeling_allowed=false"]`
- 下一步：`human_review_design_package_and_record_approval`

### greek / pegasus

- 状态：`blocked_current_design_rejected_v6_spec_pending`
- 原因：Current V2/reference design is rejected; V6 modeling spec exists but is not approved for modeling.
- 设计包：`docs/美术资源/greek_creatures/design/pegasus.md`
- 审批缺口：`["design_status='modeling_spec_ready_for_user_review'", "design_sheet_status=None", "design_approval_status=None", "design_correctness_verdict=None", "design_detail_sheet_status=None", "design_detail_board_status=None", "modeling_allowed=false"]`
- V6 可建模规格合同：`docs/美术资源/greek_creatures/design_blueprints/pegasus_v6_modeling_spec_contract.md`
- 下一步：`approve_or_rewrite_design_before_modeling`

### egyptian / anubis_warrior

- 状态：`manual_design_approval_required`
- 原因：Design package exists, but it has not been formally approved for modeling.
- 设计包：`docs/美术资源/egyptian_creatures/design/anubis_warrior.md`
- 审批缺口：`["design_status='needs_user_review'", "design_sheet_status='needs_user_review'", "design_approval_status='approval_pending'", "design_correctness_verdict='not_reviewed'", "design_detail_sheet_status='needs_user_review'", "design_detail_board_status='needs_user_review'", "modeling_allowed=false"]`
- 下一步：`human_review_design_package_and_record_approval`

### egyptian / scarab_guardian

- 状态：`manual_design_approval_required`
- 原因：Design package exists, but it has not been formally approved for modeling.
- 设计包：`docs/美术资源/egyptian_creatures/design/scarab_guardian.md`
- 审批缺口：`["design_status='needs_user_review'", "design_sheet_status='needs_user_review'", "design_approval_status='approval_pending'", "design_correctness_verdict='not_reviewed'", "design_detail_sheet_status='needs_user_review'", "design_detail_board_status='needs_user_review'", "modeling_allowed=false"]`
- 下一步：`human_review_design_package_and_record_approval`

### egyptian / mummy_soldier

- 状态：`manual_design_approval_required`
- 原因：Design package exists, but it has not been formally approved for modeling.
- 设计包：`docs/美术资源/egyptian_creatures/design/mummy_soldier.md`
- 审批缺口：`["design_status='needs_user_review'", "design_sheet_status='needs_user_review'", "design_approval_status='approval_pending'", "design_correctness_verdict='not_reviewed'", "design_detail_sheet_status='needs_user_review'", "design_detail_board_status='needs_user_review'", "modeling_allowed=false"]`
- 下一步：`human_review_design_package_and_record_approval`

### egyptian / sphinx_sentinel

- 状态：`manual_design_approval_required`
- 原因：Design package exists, but it has not been formally approved for modeling.
- 设计包：`docs/美术资源/egyptian_creatures/design/sphinx_sentinel.md`
- 审批缺口：`["design_status='needs_user_review'", "design_sheet_status='needs_user_review'", "design_approval_status='approval_pending'", "design_correctness_verdict='not_reviewed'", "design_detail_sheet_status='needs_user_review'", "design_detail_board_status='needs_user_review'", "modeling_allowed=false"]`
- 下一步：`human_review_design_package_and_record_approval`

### egyptian / sun_falcon

- 状态：`manual_design_approval_required`
- 原因：Design package exists, but it has not been formally approved for modeling.
- 设计包：`docs/美术资源/egyptian_creatures/design/sun_falcon.md`
- 审批缺口：`["design_status='needs_user_review'", "design_sheet_status='needs_user_review'", "design_approval_status='approval_pending'", "design_correctness_verdict='not_reviewed'", "design_detail_sheet_status='needs_user_review'", "design_detail_board_status='needs_user_review'", "modeling_allowed=false"]`
- 下一步：`human_review_design_package_and_record_approval`

### nordic / frost_giant

- 状态：`manual_design_approval_required`
- 原因：Design package exists, but it has not been formally approved for modeling.
- 设计包：`docs/美术资源/nordic_creatures/design/frost_giant.md`
- 审批缺口：`["design_status='needs_user_review'", "design_sheet_status='needs_user_review'", "design_approval_status='approval_pending'", "design_correctness_verdict='not_reviewed'", "design_detail_sheet_status='needs_user_review'", "design_detail_board_status='needs_user_review'", "modeling_allowed=false"]`
- 下一步：`human_review_design_package_and_record_approval`

### nordic / draugr_warrior

- 状态：`manual_design_approval_required`
- 原因：Design package exists, but it has not been formally approved for modeling.
- 设计包：`docs/美术资源/nordic_creatures/design/draugr_warrior.md`
- 审批缺口：`["design_status='needs_user_review'", "design_sheet_status='needs_user_review'", "design_approval_status='approval_pending'", "design_correctness_verdict='not_reviewed'", "design_detail_sheet_status='needs_user_review'", "design_detail_board_status='needs_user_review'", "modeling_allowed=false"]`
- 下一步：`human_review_design_package_and_record_approval`

### nordic / valkyrie

- 状态：`manual_design_approval_required`
- 原因：Design package exists, but it has not been formally approved for modeling.
- 设计包：`docs/美术资源/nordic_creatures/design/valkyrie.md`
- 审批缺口：`["design_status='needs_user_review'", "design_sheet_status='needs_user_review'", "design_approval_status='approval_pending'", "design_correctness_verdict='not_reviewed'", "design_detail_sheet_status='needs_user_review'", "design_detail_board_status='needs_user_review'", "modeling_allowed=false"]`
- 下一步：`human_review_design_package_and_record_approval`

### nordic / fenrir_wolf

- 状态：`manual_design_approval_required`
- 原因：Design package exists, but it has not been formally approved for modeling.
- 设计包：`docs/美术资源/nordic_creatures/design/fenrir_wolf.md`
- 审批缺口：`["design_status='needs_user_review'", "design_sheet_status='needs_user_review'", "design_approval_status='approval_pending'", "design_correctness_verdict='not_reviewed'", "design_detail_sheet_status='needs_user_review'", "design_detail_board_status='needs_user_review'", "modeling_allowed=false"]`
- 下一步：`human_review_design_package_and_record_approval`

### nordic / dwarf_berserker

- 状态：`manual_design_approval_required`
- 原因：Design package exists, but it has not been formally approved for modeling.
- 设计包：`docs/美术资源/nordic_creatures/design/dwarf_berserker.md`
- 审批缺口：`["design_status='needs_user_review'", "design_sheet_status='needs_user_review'", "design_approval_status='approval_pending'", "design_correctness_verdict='not_reviewed'", "design_detail_sheet_status='needs_user_review'", "design_detail_board_status='needs_user_review'", "modeling_allowed=false"]`
- 下一步：`human_review_design_package_and_record_approval`

### chinese / azure_dragon

- 状态：`manual_design_approval_required`
- 原因：Design package exists, but it has not been formally approved for modeling.
- 设计包：`docs/美术资源/chinese_creatures/design/azure_dragon.md`
- 审批缺口：`["design_status='needs_user_review'", "design_sheet_status='needs_user_review'", "design_approval_status='approval_pending'", "design_correctness_verdict='not_reviewed'", "design_detail_sheet_status='needs_user_review'", "design_detail_board_status='needs_user_review'", "modeling_allowed=false"]`
- 下一步：`human_review_design_package_and_record_approval`

### chinese / qilin_guardian

- 状态：`manual_design_approval_required`
- 原因：Design package exists, but it has not been formally approved for modeling.
- 设计包：`docs/美术资源/chinese_creatures/design/qilin_guardian.md`
- 审批缺口：`["design_status='needs_user_review'", "design_sheet_status='needs_user_review'", "design_approval_status='approval_pending'", "design_correctness_verdict='not_reviewed'", "design_detail_sheet_status='needs_user_review'", "design_detail_board_status='needs_user_review'", "modeling_allowed=false"]`
- 下一步：`human_review_design_package_and_record_approval`

### chinese / jiangshi

- 状态：`manual_design_approval_required`
- 原因：Design package exists, but it has not been formally approved for modeling.
- 设计包：`docs/美术资源/chinese_creatures/design/jiangshi.md`
- 审批缺口：`["design_status='needs_user_review'", "design_sheet_status='needs_user_review'", "design_approval_status='approval_pending'", "design_correctness_verdict='not_reviewed'", "design_detail_sheet_status='needs_user_review'", "design_detail_board_status='needs_user_review'", "modeling_allowed=false"]`
- 下一步：`human_review_design_package_and_record_approval`

### chinese / nine_tailed_fox

- 状态：`manual_design_approval_required`
- 原因：Design package exists, but it has not been formally approved for modeling.
- 设计包：`docs/美术资源/chinese_creatures/design/nine_tailed_fox.md`
- 审批缺口：`["design_status='needs_user_review'", "design_sheet_status='needs_user_review'", "design_approval_status='approval_pending'", "design_correctness_verdict='not_reviewed'", "design_detail_sheet_status='needs_user_review'", "design_detail_board_status='needs_user_review'", "modeling_allowed=false"]`
- 下一步：`human_review_design_package_and_record_approval`

### chinese / stone_lion_guardian

- 状态：`manual_design_approval_required`
- 原因：Design package exists, but it has not been formally approved for modeling.
- 设计包：`docs/美术资源/chinese_creatures/design/stone_lion_guardian.md`
- 审批缺口：`["design_status='needs_user_review'", "design_sheet_status='needs_user_review'", "design_approval_status='approval_pending'", "design_correctness_verdict='not_reviewed'", "design_detail_sheet_status='needs_user_review'", "design_detail_board_status='needs_user_review'", "modeling_allowed=false"]`
- 下一步：`human_review_design_package_and_record_approval`

### atlantean / crystal_leviathan

- 状态：`manual_design_approval_required`
- 原因：Design package exists, but it has not been formally approved for modeling.
- 设计包：`docs/美术资源/atlantean_creatures/design/crystal_leviathan.md`
- 审批缺口：`["design_status='needs_user_review'", "design_sheet_status='needs_user_review'", "design_approval_status='approval_pending'", "design_correctness_verdict='not_reviewed'", "design_detail_sheet_status='needs_user_review'", "design_detail_board_status='needs_user_review'", "modeling_allowed=false"]`
- 下一步：`human_review_design_package_and_record_approval`

### atlantean / orichalcum_golem

- 状态：`manual_design_approval_required`
- 原因：Design package exists, but it has not been formally approved for modeling.
- 设计包：`docs/美术资源/atlantean_creatures/design/orichalcum_golem.md`
- 审批缺口：`["design_status='needs_user_review'", "design_sheet_status='needs_user_review'", "design_approval_status='approval_pending'", "design_correctness_verdict='not_reviewed'", "design_detail_sheet_status='needs_user_review'", "design_detail_board_status='needs_user_review'", "modeling_allowed=false"]`
- 下一步：`human_review_design_package_and_record_approval`

### atlantean / siren_priestess

- 状态：`manual_design_approval_required`
- 原因：Design package exists, but it has not been formally approved for modeling.
- 设计包：`docs/美术资源/atlantean_creatures/design/siren_priestess.md`
- 审批缺口：`["design_status='needs_user_review'", "design_sheet_status='needs_user_review'", "design_approval_status='approval_pending'", "design_correctness_verdict='not_reviewed'", "design_detail_sheet_status='needs_user_review'", "design_detail_board_status='needs_user_review'", "modeling_allowed=false"]`
- 下一步：`human_review_design_package_and_record_approval`

### atlantean / tidal_guardian

- 状态：`manual_design_approval_required`
- 原因：Design package exists, but it has not been formally approved for modeling.
- 设计包：`docs/美术资源/atlantean_creatures/design/tidal_guardian.md`
- 审批缺口：`["design_status='needs_user_review'", "design_sheet_status='needs_user_review'", "design_approval_status='approval_pending'", "design_correctness_verdict='not_reviewed'", "design_detail_sheet_status='needs_user_review'", "design_detail_board_status='needs_user_review'", "modeling_allowed=false"]`
- 下一步：`human_review_design_package_and_record_approval`

### atlantean / abyssal_ray

- 状态：`manual_design_approval_required`
- 原因：Design package exists, but it has not been formally approved for modeling.
- 设计包：`docs/美术资源/atlantean_creatures/design/abyssal_ray.md`
- 审批缺口：`["design_status='needs_user_review'", "design_sheet_status='needs_user_review'", "design_approval_status='approval_pending'", "design_correctness_verdict='not_reviewed'", "design_detail_sheet_status='needs_user_review'", "design_detail_board_status='needs_user_review'", "modeling_allowed=false"]`
- 下一步：`human_review_design_package_and_record_approval`

## 退出标准

- 30 个生物均有正式 `approved_for_modeling` 设计状态。
- 设计包章节完整，且概念图、`.bbmodel`、UV、贴图和三视图证据齐全。
- Pegasus/Hydra 的 V6 可建模规格合同、规格图、人工签收和设计正确性 gate 均通过。
- 只有通过本 gate 后，后续模型、动作、纹理和 runtime 同步才能作为 1.0 正式验收证据。
