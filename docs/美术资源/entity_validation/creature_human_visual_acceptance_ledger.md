# MythicAgeMod 1.0 生物人工视觉验收台账

本文档由 `tools/creature_human_visual_acceptance_ledger.py audit` 生成。人工字段以 JSON 台账为准，重新生成时会按 `civilization + id` 保留已有人工结论。

## 总览

- 生物数量：30
- 人工视觉验收通过：False
- 已批准：0
- 仍需人工处理：30
- 因设计拒绝阻断：2
- 待形体审查：27
- 待用户接受修订：1
- 要求返修：0
- 要求重写设计：0
- checklist 待判定项：270
- checklist 失败项：0
- 决策统计：`{"blocked_by_design_rejection": 2, "pending_shape_review": 27, "pending_user_acceptance": 1}`

## 填写规则

- `approved`：形体、比例、头脸、神话特征、纹理和 runtime 表现都可接受。
- `revision_required`：设计方向可保留，但模型、贴图、比例、细节或 runtime 同步需要返修。
- `design_rewrite_required`：设计本身不成立，必须回到设计包。
- `blocked_by_design_rejection`：设计已被拒绝，禁止建模和验收。
- `pending_shape_review` / `pending_user_acceptance`：尚未形成最终人工结论。

## 台账

| 文明 | 生物 | 决策 | 源状态 | Review board | 下一步 |
| --- | --- | --- | --- | --- | --- |
| `greek` | `minotaur` | `pending_shape_review` | `pending_human_shape_review` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/minotaur_visual_review.png` | `human_review_visual_board_and_fill_ledger_decision` |
| `greek` | `cyclops` | `pending_user_acceptance` | `revision_implemented_pending_user_acceptance` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/cyclops_visual_review.png` | `user_review_revision_board_and_accept_or_request_changes` |
| `greek` | `centaur` | `pending_shape_review` | `pending_human_shape_review` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/centaur_visual_review.png` | `human_review_visual_board_and_fill_ledger_decision` |
| `greek` | `hydra` | `blocked_by_design_rejection` | `design_rejected_requires_v6_modeling_spec_approval` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/hydra_visual_review.png` | `approve_or_rewrite_v6_modeling_spec_before_modeling` |
| `greek` | `medusa` | `pending_shape_review` | `pending_human_shape_review` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/medusa_visual_review.png` | `human_review_visual_board_and_fill_ledger_decision` |
| `greek` | `nemean_lion` | `pending_shape_review` | `pending_human_shape_review` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/nemean_lion_visual_review.png` | `human_review_visual_board_and_fill_ledger_decision` |
| `greek` | `manticore` | `pending_shape_review` | `pending_human_shape_review` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/manticore_visual_review.png` | `human_review_visual_board_and_fill_ledger_decision` |
| `greek` | `chimera` | `pending_shape_review` | `pending_human_shape_review` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/chimera_visual_review.png` | `human_review_visual_board_and_fill_ledger_decision` |
| `greek` | `colossus` | `pending_shape_review` | `pending_human_shape_review` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/colossus_visual_review.png` | `human_review_visual_board_and_fill_ledger_decision` |
| `greek` | `pegasus` | `blocked_by_design_rejection` | `design_rejected_requires_v6_modeling_spec_approval` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/pegasus_visual_review.png` | `approve_or_rewrite_v6_modeling_spec_before_modeling` |
| `egyptian` | `anubis_warrior` | `pending_shape_review` | `pending_human_shape_review` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/egyptian/anubis_warrior_visual_review.png` | `human_review_visual_board_and_fill_ledger_decision` |
| `egyptian` | `scarab_guardian` | `pending_shape_review` | `pending_human_shape_review` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/egyptian/scarab_guardian_visual_review.png` | `human_review_visual_board_and_fill_ledger_decision` |
| `egyptian` | `mummy_soldier` | `pending_shape_review` | `pending_human_shape_review` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/egyptian/mummy_soldier_visual_review.png` | `human_review_visual_board_and_fill_ledger_decision` |
| `egyptian` | `sphinx_sentinel` | `pending_shape_review` | `pending_human_shape_review` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/egyptian/sphinx_sentinel_visual_review.png` | `human_review_visual_board_and_fill_ledger_decision` |
| `egyptian` | `sun_falcon` | `pending_shape_review` | `pending_human_shape_review` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/egyptian/sun_falcon_visual_review.png` | `human_review_visual_board_and_fill_ledger_decision` |
| `nordic` | `frost_giant` | `pending_shape_review` | `pending_human_shape_review` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/nordic/frost_giant_visual_review.png` | `human_review_visual_board_and_fill_ledger_decision` |
| `nordic` | `draugr_warrior` | `pending_shape_review` | `pending_human_shape_review` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/nordic/draugr_warrior_visual_review.png` | `human_review_visual_board_and_fill_ledger_decision` |
| `nordic` | `valkyrie` | `pending_shape_review` | `pending_human_shape_review` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/nordic/valkyrie_visual_review.png` | `human_review_visual_board_and_fill_ledger_decision` |
| `nordic` | `fenrir_wolf` | `pending_shape_review` | `pending_human_shape_review` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/nordic/fenrir_wolf_visual_review.png` | `human_review_visual_board_and_fill_ledger_decision` |
| `nordic` | `dwarf_berserker` | `pending_shape_review` | `pending_human_shape_review` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/nordic/dwarf_berserker_visual_review.png` | `human_review_visual_board_and_fill_ledger_decision` |
| `chinese` | `azure_dragon` | `pending_shape_review` | `pending_human_shape_review` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/chinese/azure_dragon_visual_review.png` | `human_review_visual_board_and_fill_ledger_decision` |
| `chinese` | `qilin_guardian` | `pending_shape_review` | `pending_human_shape_review` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/chinese/qilin_guardian_visual_review.png` | `human_review_visual_board_and_fill_ledger_decision` |
| `chinese` | `jiangshi` | `pending_shape_review` | `pending_human_shape_review` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/chinese/jiangshi_visual_review.png` | `human_review_visual_board_and_fill_ledger_decision` |
| `chinese` | `nine_tailed_fox` | `pending_shape_review` | `pending_human_shape_review` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/chinese/nine_tailed_fox_visual_review.png` | `human_review_visual_board_and_fill_ledger_decision` |
| `chinese` | `stone_lion_guardian` | `pending_shape_review` | `pending_human_shape_review` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/chinese/stone_lion_guardian_visual_review.png` | `human_review_visual_board_and_fill_ledger_decision` |
| `atlantean` | `crystal_leviathan` | `pending_shape_review` | `pending_human_shape_review` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/atlantean/crystal_leviathan_visual_review.png` | `human_review_visual_board_and_fill_ledger_decision` |
| `atlantean` | `orichalcum_golem` | `pending_shape_review` | `pending_human_shape_review` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/atlantean/orichalcum_golem_visual_review.png` | `human_review_visual_board_and_fill_ledger_decision` |
| `atlantean` | `siren_priestess` | `pending_shape_review` | `pending_human_shape_review` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/atlantean/siren_priestess_visual_review.png` | `human_review_visual_board_and_fill_ledger_decision` |
| `atlantean` | `tidal_guardian` | `pending_shape_review` | `pending_human_shape_review` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/atlantean/tidal_guardian_visual_review.png` | `human_review_visual_board_and_fill_ledger_decision` |
| `atlantean` | `abyssal_ray` | `pending_shape_review` | `pending_human_shape_review` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/atlantean/abyssal_ray_visual_review.png` | `human_review_visual_board_and_fill_ledger_decision` |

## 阻断项

### greek / minotaur

- 决策：`pending_shape_review`
- 源状态：`pending_human_shape_review`
- Review board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/minotaur_visual_review.png`
- 下一步：`human_review_visual_board_and_fill_ledger_decision`

### greek / cyclops

- 决策：`pending_user_acceptance`
- 源状态：`revision_implemented_pending_user_acceptance`
- Review board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/cyclops_visual_review.png`
- 下一步：`user_review_revision_board_and_accept_or_request_changes`

### greek / centaur

- 决策：`pending_shape_review`
- 源状态：`pending_human_shape_review`
- Review board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/centaur_visual_review.png`
- 下一步：`human_review_visual_board_and_fill_ledger_decision`

### greek / hydra

- 决策：`blocked_by_design_rejection`
- 源状态：`design_rejected_requires_v6_modeling_spec_approval`
- Review board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/hydra_visual_review.png`
- 下一步：`approve_or_rewrite_v6_modeling_spec_before_modeling`
- 设计拒绝报告：`docs/美术资源/greek_creatures/design_review/pegasus_hydra_design_rejection_2026-06-15.md`
- 设计正确性审查：`docs/美术资源/greek_creatures/design_review/pegasus_hydra_design_correctness_audit_2026-06-15.md`
- V6 可建模规格合同：`docs/美术资源/greek_creatures/design_blueprints/hydra_v6_modeling_spec_contract.md`

### greek / medusa

- 决策：`pending_shape_review`
- 源状态：`pending_human_shape_review`
- Review board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/medusa_visual_review.png`
- 下一步：`human_review_visual_board_and_fill_ledger_decision`

### greek / nemean_lion

- 决策：`pending_shape_review`
- 源状态：`pending_human_shape_review`
- Review board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/nemean_lion_visual_review.png`
- 下一步：`human_review_visual_board_and_fill_ledger_decision`

### greek / manticore

- 决策：`pending_shape_review`
- 源状态：`pending_human_shape_review`
- Review board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/manticore_visual_review.png`
- 下一步：`human_review_visual_board_and_fill_ledger_decision`

### greek / chimera

- 决策：`pending_shape_review`
- 源状态：`pending_human_shape_review`
- Review board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/chimera_visual_review.png`
- 下一步：`human_review_visual_board_and_fill_ledger_decision`

### greek / colossus

- 决策：`pending_shape_review`
- 源状态：`pending_human_shape_review`
- Review board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/colossus_visual_review.png`
- 下一步：`human_review_visual_board_and_fill_ledger_decision`

### greek / pegasus

- 决策：`blocked_by_design_rejection`
- 源状态：`design_rejected_requires_v6_modeling_spec_approval`
- Review board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/pegasus_visual_review.png`
- 下一步：`approve_or_rewrite_v6_modeling_spec_before_modeling`
- 设计拒绝报告：`docs/美术资源/greek_creatures/design_review/pegasus_hydra_design_rejection_2026-06-15.md`
- 设计正确性审查：`docs/美术资源/greek_creatures/design_review/pegasus_hydra_design_correctness_audit_2026-06-15.md`
- V6 可建模规格合同：`docs/美术资源/greek_creatures/design_blueprints/pegasus_v6_modeling_spec_contract.md`

### egyptian / anubis_warrior

- 决策：`pending_shape_review`
- 源状态：`pending_human_shape_review`
- Review board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/egyptian/anubis_warrior_visual_review.png`
- 下一步：`human_review_visual_board_and_fill_ledger_decision`

### egyptian / scarab_guardian

- 决策：`pending_shape_review`
- 源状态：`pending_human_shape_review`
- Review board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/egyptian/scarab_guardian_visual_review.png`
- 下一步：`human_review_visual_board_and_fill_ledger_decision`

### egyptian / mummy_soldier

- 决策：`pending_shape_review`
- 源状态：`pending_human_shape_review`
- Review board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/egyptian/mummy_soldier_visual_review.png`
- 下一步：`human_review_visual_board_and_fill_ledger_decision`

### egyptian / sphinx_sentinel

- 决策：`pending_shape_review`
- 源状态：`pending_human_shape_review`
- Review board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/egyptian/sphinx_sentinel_visual_review.png`
- 下一步：`human_review_visual_board_and_fill_ledger_decision`

### egyptian / sun_falcon

- 决策：`pending_shape_review`
- 源状态：`pending_human_shape_review`
- Review board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/egyptian/sun_falcon_visual_review.png`
- 下一步：`human_review_visual_board_and_fill_ledger_decision`

### nordic / frost_giant

- 决策：`pending_shape_review`
- 源状态：`pending_human_shape_review`
- Review board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/nordic/frost_giant_visual_review.png`
- 下一步：`human_review_visual_board_and_fill_ledger_decision`

### nordic / draugr_warrior

- 决策：`pending_shape_review`
- 源状态：`pending_human_shape_review`
- Review board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/nordic/draugr_warrior_visual_review.png`
- 下一步：`human_review_visual_board_and_fill_ledger_decision`

### nordic / valkyrie

- 决策：`pending_shape_review`
- 源状态：`pending_human_shape_review`
- Review board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/nordic/valkyrie_visual_review.png`
- 下一步：`human_review_visual_board_and_fill_ledger_decision`

### nordic / fenrir_wolf

- 决策：`pending_shape_review`
- 源状态：`pending_human_shape_review`
- Review board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/nordic/fenrir_wolf_visual_review.png`
- 下一步：`human_review_visual_board_and_fill_ledger_decision`

### nordic / dwarf_berserker

- 决策：`pending_shape_review`
- 源状态：`pending_human_shape_review`
- Review board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/nordic/dwarf_berserker_visual_review.png`
- 下一步：`human_review_visual_board_and_fill_ledger_decision`

### chinese / azure_dragon

- 决策：`pending_shape_review`
- 源状态：`pending_human_shape_review`
- Review board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/chinese/azure_dragon_visual_review.png`
- 下一步：`human_review_visual_board_and_fill_ledger_decision`

### chinese / qilin_guardian

- 决策：`pending_shape_review`
- 源状态：`pending_human_shape_review`
- Review board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/chinese/qilin_guardian_visual_review.png`
- 下一步：`human_review_visual_board_and_fill_ledger_decision`

### chinese / jiangshi

- 决策：`pending_shape_review`
- 源状态：`pending_human_shape_review`
- Review board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/chinese/jiangshi_visual_review.png`
- 下一步：`human_review_visual_board_and_fill_ledger_decision`

### chinese / nine_tailed_fox

- 决策：`pending_shape_review`
- 源状态：`pending_human_shape_review`
- Review board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/chinese/nine_tailed_fox_visual_review.png`
- 下一步：`human_review_visual_board_and_fill_ledger_decision`

### chinese / stone_lion_guardian

- 决策：`pending_shape_review`
- 源状态：`pending_human_shape_review`
- Review board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/chinese/stone_lion_guardian_visual_review.png`
- 下一步：`human_review_visual_board_and_fill_ledger_decision`

### atlantean / crystal_leviathan

- 决策：`pending_shape_review`
- 源状态：`pending_human_shape_review`
- Review board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/atlantean/crystal_leviathan_visual_review.png`
- 下一步：`human_review_visual_board_and_fill_ledger_decision`

### atlantean / orichalcum_golem

- 决策：`pending_shape_review`
- 源状态：`pending_human_shape_review`
- Review board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/atlantean/orichalcum_golem_visual_review.png`
- 下一步：`human_review_visual_board_and_fill_ledger_decision`

### atlantean / siren_priestess

- 决策：`pending_shape_review`
- 源状态：`pending_human_shape_review`
- Review board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/atlantean/siren_priestess_visual_review.png`
- 下一步：`human_review_visual_board_and_fill_ledger_decision`

### atlantean / tidal_guardian

- 决策：`pending_shape_review`
- 源状态：`pending_human_shape_review`
- Review board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/atlantean/tidal_guardian_visual_review.png`
- 下一步：`human_review_visual_board_and_fill_ledger_decision`

### atlantean / abyssal_ray

- 决策：`pending_shape_review`
- 源状态：`pending_human_shape_review`
- Review board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/atlantean/abyssal_ray_visual_review.png`
- 下一步：`human_review_visual_board_and_fill_ledger_decision`

