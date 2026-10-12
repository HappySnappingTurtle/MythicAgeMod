# MythicAgeMod 1.0 生物玩法闭环契约（2026-06-15）

本文档由 `tools/creature_gameplay_contract_audit.py audit` 生成。它检查 30 个 1.0 生物是否不仅有模型/纹理，还具备代码玩法闭环：注册、属性、自然生成、AI、特殊能力、掉落、声音、渲染、模型、纹理、刷怪蛋和发布验收依赖。

## 总览

- 发布玩法闭环通过：`False`
- 静态代码契约通过：`True`
- 生物数量：30
- 静态代码契约通过数：30
- 发布玩法 ready 数：0
- 静态失败统计：`{}`
- 发布阻断统计：`{"current_design_rejected_v6_spec_pending": 2, "design_not_approved:blocked_current_design_rejected_v6_spec_pending": 2, "design_not_approved:manual_design_approval_required": 28, "human_visual_not_approved:blocked_by_design_rejection": 2, "human_visual_not_approved:pending_shape_review": 27, "human_visual_not_approved:pending_user_acceptance": 1, "motion_blocked_by_art_or_design": 3}`
- 判定规则：A creature is 1.0 gameplay-ready only when static code/resource contracts, design approval, human visual approval, and motion readiness all pass.

## 文明摘要

| 文明 | 生物数 | 静态通过 | 发布 ready | 静态失败 | 发布阻断 |
| --- | --- | --- | --- | --- | --- |
| `greek` | 10 | 10 | 0 | `{}` | `{"design_not_approved:manual_design_approval_required": 8, "human_visual_not_approved:pending_shape_review": 7, "human_visual_not_approved:pending_user_acceptance": 1, "motion_blocked_by_art_or_design": 3, "design_not_approved:blocked_current_design_rejected_v6_spec_pending": 2, "human_visual_not_approved:blocked_by_design_rejection": 2, "current_design_rejected_v6_spec_pending": 2}` |
| `egyptian` | 5 | 5 | 0 | `{}` | `{"design_not_approved:manual_design_approval_required": 5, "human_visual_not_approved:pending_shape_review": 5}` |
| `nordic` | 5 | 5 | 0 | `{}` | `{"design_not_approved:manual_design_approval_required": 5, "human_visual_not_approved:pending_shape_review": 5}` |
| `chinese` | 5 | 5 | 0 | `{}` | `{"design_not_approved:manual_design_approval_required": 5, "human_visual_not_approved:pending_shape_review": 5}` |
| `atlantean` | 5 | 5 | 0 | `{}` | `{"design_not_approved:manual_design_approval_required": 5, "human_visual_not_approved:pending_shape_review": 5}` |

## 生物矩阵

| 文明 | 生物 | 静态代码 | 发布 ready | 设计状态 | 视觉结论 | 静态失败 | 发布阻断 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `greek` | `minotaur` | `True` | `False` | `manual_design_approval_required` | `pending_shape_review` | `[]` | `["design_not_approved:manual_design_approval_required", "human_visual_not_approved:pending_shape_review"]` |
| `greek` | `cyclops` | `True` | `False` | `manual_design_approval_required` | `pending_user_acceptance` | `[]` | `["design_not_approved:manual_design_approval_required", "human_visual_not_approved:pending_user_acceptance", "motion_blocked_by_art_or_design"]` |
| `greek` | `centaur` | `True` | `False` | `manual_design_approval_required` | `pending_shape_review` | `[]` | `["design_not_approved:manual_design_approval_required", "human_visual_not_approved:pending_shape_review"]` |
| `greek` | `hydra` | `True` | `False` | `blocked_current_design_rejected_v6_spec_pending` | `blocked_by_design_rejection` | `[]` | `["design_not_approved:blocked_current_design_rejected_v6_spec_pending", "human_visual_not_approved:blocked_by_design_rejection", "motion_blocked_by_art_or_design", "current_design_rejected_v6_spec_pending"]` |
| `greek` | `medusa` | `True` | `False` | `manual_design_approval_required` | `pending_shape_review` | `[]` | `["design_not_approved:manual_design_approval_required", "human_visual_not_approved:pending_shape_review"]` |
| `greek` | `nemean_lion` | `True` | `False` | `manual_design_approval_required` | `pending_shape_review` | `[]` | `["design_not_approved:manual_design_approval_required", "human_visual_not_approved:pending_shape_review"]` |
| `greek` | `manticore` | `True` | `False` | `manual_design_approval_required` | `pending_shape_review` | `[]` | `["design_not_approved:manual_design_approval_required", "human_visual_not_approved:pending_shape_review"]` |
| `greek` | `chimera` | `True` | `False` | `manual_design_approval_required` | `pending_shape_review` | `[]` | `["design_not_approved:manual_design_approval_required", "human_visual_not_approved:pending_shape_review"]` |
| `greek` | `colossus` | `True` | `False` | `manual_design_approval_required` | `pending_shape_review` | `[]` | `["design_not_approved:manual_design_approval_required", "human_visual_not_approved:pending_shape_review"]` |
| `greek` | `pegasus` | `True` | `False` | `blocked_current_design_rejected_v6_spec_pending` | `blocked_by_design_rejection` | `[]` | `["design_not_approved:blocked_current_design_rejected_v6_spec_pending", "human_visual_not_approved:blocked_by_design_rejection", "motion_blocked_by_art_or_design", "current_design_rejected_v6_spec_pending"]` |
| `egyptian` | `anubis_warrior` | `True` | `False` | `manual_design_approval_required` | `pending_shape_review` | `[]` | `["design_not_approved:manual_design_approval_required", "human_visual_not_approved:pending_shape_review"]` |
| `egyptian` | `scarab_guardian` | `True` | `False` | `manual_design_approval_required` | `pending_shape_review` | `[]` | `["design_not_approved:manual_design_approval_required", "human_visual_not_approved:pending_shape_review"]` |
| `egyptian` | `mummy_soldier` | `True` | `False` | `manual_design_approval_required` | `pending_shape_review` | `[]` | `["design_not_approved:manual_design_approval_required", "human_visual_not_approved:pending_shape_review"]` |
| `egyptian` | `sphinx_sentinel` | `True` | `False` | `manual_design_approval_required` | `pending_shape_review` | `[]` | `["design_not_approved:manual_design_approval_required", "human_visual_not_approved:pending_shape_review"]` |
| `egyptian` | `sun_falcon` | `True` | `False` | `manual_design_approval_required` | `pending_shape_review` | `[]` | `["design_not_approved:manual_design_approval_required", "human_visual_not_approved:pending_shape_review"]` |
| `nordic` | `frost_giant` | `True` | `False` | `manual_design_approval_required` | `pending_shape_review` | `[]` | `["design_not_approved:manual_design_approval_required", "human_visual_not_approved:pending_shape_review"]` |
| `nordic` | `draugr_warrior` | `True` | `False` | `manual_design_approval_required` | `pending_shape_review` | `[]` | `["design_not_approved:manual_design_approval_required", "human_visual_not_approved:pending_shape_review"]` |
| `nordic` | `valkyrie` | `True` | `False` | `manual_design_approval_required` | `pending_shape_review` | `[]` | `["design_not_approved:manual_design_approval_required", "human_visual_not_approved:pending_shape_review"]` |
| `nordic` | `fenrir_wolf` | `True` | `False` | `manual_design_approval_required` | `pending_shape_review` | `[]` | `["design_not_approved:manual_design_approval_required", "human_visual_not_approved:pending_shape_review"]` |
| `nordic` | `dwarf_berserker` | `True` | `False` | `manual_design_approval_required` | `pending_shape_review` | `[]` | `["design_not_approved:manual_design_approval_required", "human_visual_not_approved:pending_shape_review"]` |
| `chinese` | `azure_dragon` | `True` | `False` | `manual_design_approval_required` | `pending_shape_review` | `[]` | `["design_not_approved:manual_design_approval_required", "human_visual_not_approved:pending_shape_review"]` |
| `chinese` | `qilin_guardian` | `True` | `False` | `manual_design_approval_required` | `pending_shape_review` | `[]` | `["design_not_approved:manual_design_approval_required", "human_visual_not_approved:pending_shape_review"]` |
| `chinese` | `jiangshi` | `True` | `False` | `manual_design_approval_required` | `pending_shape_review` | `[]` | `["design_not_approved:manual_design_approval_required", "human_visual_not_approved:pending_shape_review"]` |
| `chinese` | `nine_tailed_fox` | `True` | `False` | `manual_design_approval_required` | `pending_shape_review` | `[]` | `["design_not_approved:manual_design_approval_required", "human_visual_not_approved:pending_shape_review"]` |
| `chinese` | `stone_lion_guardian` | `True` | `False` | `manual_design_approval_required` | `pending_shape_review` | `[]` | `["design_not_approved:manual_design_approval_required", "human_visual_not_approved:pending_shape_review"]` |
| `atlantean` | `crystal_leviathan` | `True` | `False` | `manual_design_approval_required` | `pending_shape_review` | `[]` | `["design_not_approved:manual_design_approval_required", "human_visual_not_approved:pending_shape_review"]` |
| `atlantean` | `orichalcum_golem` | `True` | `False` | `manual_design_approval_required` | `pending_shape_review` | `[]` | `["design_not_approved:manual_design_approval_required", "human_visual_not_approved:pending_shape_review"]` |
| `atlantean` | `siren_priestess` | `True` | `False` | `manual_design_approval_required` | `pending_shape_review` | `[]` | `["design_not_approved:manual_design_approval_required", "human_visual_not_approved:pending_shape_review"]` |
| `atlantean` | `tidal_guardian` | `True` | `False` | `manual_design_approval_required` | `pending_shape_review` | `[]` | `["design_not_approved:manual_design_approval_required", "human_visual_not_approved:pending_shape_review"]` |
| `atlantean` | `abyssal_ray` | `True` | `False` | `manual_design_approval_required` | `pending_shape_review` | `[]` | `["design_not_approved:manual_design_approval_required", "human_visual_not_approved:pending_shape_review"]` |

## 需要处理的静态缺口

## 退出标准

- 30 个生物 `static_code_contract_ok=true`。
- 30 个生物设计门禁为 `approved_for_modeling`。
- 30 个生物人工视觉结论为 `approved`。
- 动作/动画队列中不再有 art/design 阻断，并完成必要连续帧/clip 验收。
- Pegasus/Hydra 必须先签收 V6 可建模规格，再完成模型和动作重建；不能用当前 V2 代码功能抵消视觉设计失败。
