# MythicAgeMod 1.0 生物视觉人工验收包

本文档由 `tools/creature_visual_acceptance_package.py audit` 生成。自动证据只证明设计包、Blockbench、UV、贴图、预览和 runtime model gate 已准备好；它不替代人工审美验收。

## 总览

- 生物数量：30
- 自动证据通过：False
- 自动证据包通过：False
- 头身比例质量门通过：True
- 设计/Blockbench 细节优先门通过：True (30/30)
- 头身比例质量门待补数量：0
- 头身比例/细节审美预警数量：0
- 运行时 Java 头身比例门通过：False
- 运行时 Java 细节优先门通过：False (29/30)
- 运行时 Java 头身比例待补数量：1
- 1.0 视觉质量契约通过：False
- 视觉质量契约通过数量：29/30
- 视觉质量契约待补数量：1
- 设计拒绝阻断数量：2
- 人工视觉验收通过：False
- 需要人工验收：30
- 视觉 review board：30 / 30
- Review board 总览图：`docs/preview_assets/creature_visual_acceptance/visual_review_board_contact_sheet.png`
- Greek 返修待用户接受：1
- 待人工形体审查：27
- 状态统计：`{"design_rejected_requires_v6_modeling_spec_approval": 2, "pending_human_shape_review": 27, "revision_implemented_pending_user_acceptance": 1}`
- 质量契约统计：`{"needs_detail_contract_refinement": 1, "quality_contract_passed": 29}`
- 质量规则：1.0 creatures must declare target scale, three-view silhouette, signature features, reject-if rules, at least 10 design parts, at least 24 Blockbench elements, at least 8 named Blockbench detail elements, at least 18 runtime named parts, at least 8 runtime detail UV tokens, focal details, signature structures, Blockbench preview triad, and UV texture-fit evidence before human acceptance.
- 比例规则：Heads must remain proportionate to the creature body archetype; readability must come from explicit eyes, mouth, jaws, horns, mane, weapon, wing, tail, and UV detail, not abnormal head enlargement. Creature-specific/user-feedback checks are blockers; broad archetype checks are review warnings.
- 细节优先规则：A proportionate head is required but not sufficient: each creature must preserve enough named face and signature parts that reducing head scale does not reduce eye, mouth, snout, horn, wing, tail, weapon, or mythic-feature readability.
- 运行时比例规则：Runtime Java model cuboids must match the same head/body proportion rule, including visual face volume from snout/jaw/mouth/brow/cheek parts; Blockbench/design evidence is not enough if the in-game model still renders an oversized or under-detailed head.

## 文明验收材料

### greek

- Visual report：`docs/preview_assets/greek_creatures/visual_report.json`
- Blockbench contact sheet：`docs/preview_assets/greek_creatures/blockbench_validation_contact_sheet.png`
- Concept contact sheet：`docs/preview_assets/greek_creatures/concept_design_contact_sheet.png`
- 生物数量：10
- 自动证据通过数量：9
- 视觉质量契约通过数量：9
- 状态统计：`{"design_rejected_requires_v6_modeling_spec_approval": 2, "pending_human_shape_review": 7, "revision_implemented_pending_user_acceptance": 1}`
- 质量契约统计：`{"needs_detail_contract_refinement": 1, "quality_contract_passed": 9}`

### egyptian

- Visual report：`docs/preview_assets/egyptian_creatures/visual_report.json`
- Blockbench contact sheet：`docs/preview_assets/egyptian_creatures/blockbench_validation_contact_sheet.png`
- Concept contact sheet：`docs/preview_assets/egyptian_creatures/concept_design_contact_sheet.png`
- 生物数量：5
- 自动证据通过数量：5
- 视觉质量契约通过数量：5
- 状态统计：`{"pending_human_shape_review": 5}`
- 质量契约统计：`{"quality_contract_passed": 5}`

### nordic

- Visual report：`docs/preview_assets/nordic_creatures/visual_report.json`
- Blockbench contact sheet：`docs/preview_assets/nordic_creatures/blockbench_validation_contact_sheet.png`
- Concept contact sheet：`docs/preview_assets/nordic_creatures/concept_design_contact_sheet.png`
- 生物数量：5
- 自动证据通过数量：5
- 视觉质量契约通过数量：5
- 状态统计：`{"pending_human_shape_review": 5}`
- 质量契约统计：`{"quality_contract_passed": 5}`

### chinese

- Visual report：`docs/preview_assets/chinese_creatures/visual_report.json`
- Blockbench contact sheet：`docs/preview_assets/chinese_creatures/blockbench_validation_contact_sheet.png`
- Concept contact sheet：`docs/preview_assets/chinese_creatures/concept_design_contact_sheet.png`
- 生物数量：5
- 自动证据通过数量：5
- 视觉质量契约通过数量：5
- 状态统计：`{"pending_human_shape_review": 5}`
- 质量契约统计：`{"quality_contract_passed": 5}`

### atlantean

- Visual report：`docs/preview_assets/atlantean_creatures/visual_report.json`
- Blockbench contact sheet：`docs/preview_assets/atlantean_creatures/blockbench_validation_contact_sheet.png`
- Concept contact sheet：`docs/preview_assets/atlantean_creatures/concept_design_contact_sheet.png`
- 生物数量：5
- 自动证据通过数量：5
- 视觉质量契约通过数量：5
- 状态统计：`{"pending_human_shape_review": 5}`
- 质量契约统计：`{"quality_contract_passed": 5}`

## 1.0 视觉质量契约清单

| 文明 | 生物 | 质量契约 | 设计部件 | BB 元素 | BB 细节 | Runtime 部件 | Runtime 细节 | 失败项 | 下一步 |
| --- | --- | --- | ---: | ---: | ---: | ---: | ---: | --- | --- |
| greek | `minotaur` | `quality_contract_passed` | 42 | 41 | 29 | 61 | 19 | - | await_human_shape_acceptance |
| greek | `cyclops` | `quality_contract_passed` | 26 | 51 | 31 | 68 | 17 | - | await_user_acceptance_of_revision |
| greek | `centaur` | `quality_contract_passed` | 48 | 49 | 28 | 78 | 28 | - | await_human_shape_acceptance |
| greek | `hydra` | `needs_detail_contract_refinement` | 21 | 93 | 67 | 151 | 22 | runtime_no_missing_parts；greek_mechanical_contract_ok | 先补设计拆解和命名部件 |
| greek | `medusa` | `quality_contract_passed` | 59 | 72 | 64 | 93 | 19 | - | await_human_shape_acceptance |
| greek | `nemean_lion` | `quality_contract_passed` | 36 | 36 | 33 | 56 | 20 | - | await_human_shape_acceptance |
| greek | `manticore` | `quality_contract_passed` | 40 | 40 | 38 | 61 | 19 | - | await_human_shape_acceptance |
| greek | `chimera` | `quality_contract_passed` | 45 | 45 | 41 | 64 | 20 | - | await_human_shape_acceptance |
| greek | `colossus` | `quality_contract_passed` | 43 | 42 | 31 | 60 | 17 | - | await_human_shape_acceptance |
| greek | `pegasus` | `quality_contract_passed` | 33 | 50 | 43 | 48 | 18 | - | await_v6_modeling_spec_approval_before_modeling |
| egyptian | `anubis_warrior` | `quality_contract_passed` | 21 | 28 | 20 | 36 | 10 | - | await_human_shape_acceptance |
| egyptian | `scarab_guardian` | `quality_contract_passed` | 28 | 26 | 25 | 34 | 8 | - | await_human_shape_acceptance |
| egyptian | `mummy_soldier` | `quality_contract_passed` | 30 | 28 | 26 | 39 | 11 | - | await_human_shape_acceptance |
| egyptian | `sphinx_sentinel` | `quality_contract_passed` | 31 | 29 | 26 | 39 | 10 | - | await_human_shape_acceptance |
| egyptian | `sun_falcon` | `quality_contract_passed` | 28 | 28 | 25 | 36 | 10 | - | await_human_shape_acceptance |
| nordic | `frost_giant` | `quality_contract_passed` | 30 | 37 | 32 | 47 | 10 | - | await_human_shape_acceptance |
| nordic | `draugr_warrior` | `quality_contract_passed` | 16 | 34 | 27 | 44 | 10 | - | await_human_shape_acceptance |
| nordic | `valkyrie` | `quality_contract_passed` | 35 | 37 | 31 | 42 | 8 | - | await_human_shape_acceptance |
| nordic | `fenrir_wolf` | `quality_contract_passed` | 36 | 38 | 32 | 42 | 8 | - | await_human_shape_acceptance |
| nordic | `dwarf_berserker` | `quality_contract_passed` | 26 | 36 | 27 | 52 | 15 | - | await_human_shape_acceptance |
| chinese | `azure_dragon` | `quality_contract_passed` | 41 | 41 | 33 | 52 | 12 | - | await_human_shape_acceptance |
| chinese | `qilin_guardian` | `quality_contract_passed` | 42 | 38 | 36 | 51 | 10 | - | await_human_shape_acceptance |
| chinese | `jiangshi` | `quality_contract_passed` | 17 | 27 | 21 | 41 | 14 | - | await_human_shape_acceptance |
| chinese | `nine_tailed_fox` | `quality_contract_passed` | 12 | 35 | 32 | 50 | 10 | - | await_human_shape_acceptance |
| chinese | `stone_lion_guardian` | `quality_contract_passed` | 21 | 36 | 32 | 58 | 18 | - | await_human_shape_acceptance |
| atlantean | `crystal_leviathan` | `quality_contract_passed` | 36 | 33 | 27 | 42 | 10 | - | await_human_shape_acceptance |
| atlantean | `orichalcum_golem` | `quality_contract_passed` | 24 | 42 | 29 | 46 | 15 | - | await_human_shape_acceptance |
| atlantean | `siren_priestess` | `quality_contract_passed` | 21 | 31 | 28 | 43 | 13 | - | await_human_shape_acceptance |
| atlantean | `tidal_guardian` | `quality_contract_passed` | 31 | 30 | 30 | 42 | 11 | - | await_human_shape_acceptance |
| atlantean | `abyssal_ray` | `quality_contract_passed` | 26 | 26 | 23 | 37 | 11 | - | await_human_shape_acceptance |

## 头身比例质量门

- 报告：`docs/preview_assets/creature_visual_acceptance/creature_proportion_quality_report.json`
- 通过：True
- 覆盖生物：30/30
- 待补数量：0
- 审美预警数量：0
- 细节优先通过：True (30/30)
- 规则：头部必须符合身体比例，神话特征可读性来自独立眼、嘴、颌、角、鬃毛、武器、翼、尾和固定 UV 细节，而不是异常放大头部。

## 设计拒绝阻断项

| 文明 | 生物 | 状态 | 冻结记录 | 正确性审查 | V6 规格合同 | V6 规格图 | 下一步 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| greek | `hydra` | `design_rejected_requires_v6_modeling_spec_approval` | `docs/美术资源/greek_creatures/design_review/pegasus_hydra_design_rejection_2026-06-15.md` | `docs/美术资源/greek_creatures/design_review/pegasus_hydra_design_correctness_audit_2026-06-15.md` | `docs/美术资源/greek_creatures/design_blueprints/hydra_v6_modeling_spec_contract.md` | `docs/preview_assets/greek_creatures/design_approval/v6/hydra_v6_modeling_spec_sheet.png` | `await_v6_modeling_spec_approval_before_modeling` |
| greek | `pegasus` | `design_rejected_requires_v6_modeling_spec_approval` | `docs/美术资源/greek_creatures/design_review/pegasus_hydra_design_rejection_2026-06-15.md` | `docs/美术资源/greek_creatures/design_review/pegasus_hydra_design_correctness_audit_2026-06-15.md` | `docs/美术资源/greek_creatures/design_blueprints/pegasus_v6_modeling_spec_contract.md` | `docs/preview_assets/greek_creatures/design_approval/v6/pegasus_v6_modeling_spec_sheet.png` | `await_v6_modeling_spec_approval_before_modeling` |

## 运行时 Java 头身比例门

- 报告：`docs/preview_assets/creature_visual_acceptance/creature_runtime_proportion_quality_report.json`
- 通过：False
- 覆盖生物：30/30
- 待补数量：1
- 细节优先通过：False (29/30)
- 规则：游戏内 Java 模型也必须符合头身比例；不能只让 Blockbench/设计包正确而实机仍保留旧大头模型。

### 运行时比例待补清单

| 文明 | 生物 | 失败项 | 比例摘要 | 下一步 |
| --- | --- | --- | --- | --- |
| greek | `hydra` | head_parts_present；body_parts_present；required_head_count_matches_design_contract；required_neck_count_matches_design_contract；runtime_required_identity_detail_tokens_present；head_body_connection_parts_present；runtime_parent_child_structure_present；runtime_connection_overlap_intersects；runtime_detail_first_readability_not_head_scale | h=0.0, w=0.0, d=0.0; visual h=1.9022, w=0.6659, d=0.405; detail=121 | fix_runtime_java_model_head_body_proportion_before_visual_acceptance |

## 单体清单

| 文明 | 生物 | 状态 | 自动证据 | 设计比例门 | 运行时比例门 | 细节优先 | 视觉头部比例 h/w/d | 细节数 | 质量契约 | 下一步 | Review board | 关键验收点 |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: | --- | --- | --- | --- |
| greek | `minotaur` | `pending_human_shape_review` | `True` | `True` | `True` | `True` | `0.6375/0.725/1.6875` | 23 | `True` | `await_human_shape_acceptance` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/minotaur_visual_review.png` | 牛头人身必须清楚，不能只是普通人形头部贴牛纹；双角外展、牛吻前突、眉眼、鼻孔和嘴必须独立可读 |
| greek | `cyclops` | `revision_implemented_pending_user_acceptance` | `True` | `True` | `True` | `True` | `0.5907/0.7294/1.4545` | 25 | `True` | `await_user_acceptance_of_revision` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/cyclops_visual_review.png` | 巨大人形比例，宽肩厚胸，不是普通僵尸放大；头部正面只有一个居中的大眼，必须有瞳孔、眉骨、鼻梁、鼻孔、嘴和牙 |
| greek | `centaur` | `pending_human_shape_review` | `True` | `True` | `True` | `True` | `0.6602/0.5/0.2857` | 19 | `True` | `await_human_shape_acceptance` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/centaur_visual_review.png` | 马身加前置人形战士上身的清楚拼接，不允许像普通马背上长方块；人形头脸必须有双眼、鼻梁、嘴线、耳朵和深色头发 |
| greek | `hydra` | `design_rejected_requires_v6_modeling_spec_approval` | `False` | `True` | `False` | `False` | `1.9022/0.6659/0.405` | 121 | `False` | `await_v6_modeling_spec_approval_before_modeling` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/hydra_visual_review.png` | 显性 5 头作为 MC 可读形态，命名仍为九头蛇；每个头必须比例合理，细节靠独立部件和固定 UV 保证；身体瘦长低伏，不能用一个臃肿大箱体撑满碰撞盒 |
| greek | `medusa` | `pending_human_shape_review` | `True` | `True` | `True` | `True` | `0.6404/0.7262/1.4038` | 23 | `True` | `await_human_shape_acceptance` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/medusa_visual_review.png` | 人形上身、女性头脸、蛇发冠、蛇形下身必须同时可读；蛇发不是普通头发贴图，必须有六条独立蛇束和六个蛇头端点 |
| greek | `nemean_lion` | `pending_human_shape_review` | `True` | `True` | `True` | `True` | `0.7203/0.7583/0.5752` | 29 | `True` | `await_human_shape_acceptance` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/nemean_lion_visual_review.png` | 超大型狮子，不是狼/猫科普通小体型；巨大放射状金色鬃毛必须超过头部边界并包住比例合理的头脸 |
| greek | `manticore` | `pending_human_shape_review` | `True` | `True` | `True` | `True` | `0.7232/0.7368/0.5817` | 34 | `True` | `await_human_shape_acceptance` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/manticore_visual_review.png` | 狮身主体、分段膜翼、上扬蝎尾毒刺必须三者同时明显；脸部必须有双眼、鼻口线、左右獠牙、耳朵和左右 cheek_spike，不能只是普通兽头，也不能靠大头读凶险感 |
| greek | `chimera` | `pending_human_shape_review` | `True` | `True` | `True` | `True` | `0.6894/0.7647/0.5658` | 35 | `True` | `await_human_shape_acceptance` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/chimera_visual_review.png` | 狮头主脸、侧探羊头、尾端蛇头三重身份必须同时可读；狮头必须有鬃毛、双眼、鼻口线和獠牙，不能只是普通方块兽头 |
| greek | `colossus` | `pending_human_shape_review` | `True` | `True` | `True` | `True` | `0.3846/0.5652/1.0962` | 12 | `True` | `await_human_shape_acceptance` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/colossus_visual_review.png` | 青铜神像巨人，体量显著高于其他生物；头冠、面甲、发光眼缝、嘴缝和颊板必须明显，不允许脸部空白铜块 |
| greek | `pegasus` | `design_rejected_requires_v6_modeling_spec_approval` | `True` | `True` | `True` | `True` | `0.4891/0.3926/0.4862` | 17 | `True` | `await_v6_modeling_spec_approval_before_modeling` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/pegasus_visual_review.png` | 纯白天马，第一眼必须是白马头、长颈、双翼，而不是一团白色方块；头部参考原版马的细长比例，头宽约为马身宽度三分之一到二分之一之间，必须有左右耳、左右眼、鼻梁、鼻孔、口线和分段鬃毛，不能靠大头或被翅膀/身体遮挡 |
| egyptian | `anubis_warrior` | `pending_human_shape_review` | `True` | `True` | `True` | `True` | `0.4878/0.6771/2.0625` | 8 | `True` | `await_human_shape_acceptance` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/egyptian/anubis_warrior_visual_review.png` | 胡狼头、长吻、长耳、人形身体和埃及金饰必须同时可读，头块宽度不能压过肩胸比例；右手镰剑/权杖必须作为独立武器轮廓 |
| egyptian | `scarab_guardian` | `pending_human_shape_review` | `True` | `True` | `True` | `True` | `0.6875/0.625/0.3944` | 14 | `True` | `await_human_shape_acceptance` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/egyptian/scarab_guardian_visual_review.png` | 圣甲虫轮廓；金色背甲/太阳符号 |
| egyptian | `mummy_soldier` | `pending_human_shape_review` | `True` | `True` | `True` | `True` | `0.6667/0.7/1.9643` | 22 | `True` | `await_human_shape_acceptance` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/egyptian/mummy_soldier_visual_review.png` | 绷带层次；黑眼洞 |
| egyptian | `sphinx_sentinel` | `pending_human_shape_review` | `True` | `True` | `True` | `True` | `0.3644/0.5694/0.2815` | 17 | `True` | `await_human_shape_acceptance` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/egyptian/sphinx_sentinel_visual_review.png` | 狮身；人面 |
| egyptian | `sun_falcon` | `pending_human_shape_review` | `True` | `True` | `True` | `True` | `0.6279/0.6842/1.0463` | 16 | `True` | `await_human_shape_acceptance` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/egyptian/sun_falcon_visual_review.png` | 太阳盘；隼头尖喙 |
| nordic | `frost_giant` | `pending_human_shape_review` | `True` | `True` | `True` | `True` | `0.6111/0.7333/1.45` | 17 | `True` | `await_human_shape_acceptance` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/nordic/frost_giant_visual_review.png` | 巨人体量；冰须和冰刺 |
| nordic | `draugr_warrior` | `pending_human_shape_review` | `True` | `True` | `True` | `True` | `0.6452/0.7558/1.6538` | 20 | `True` | `await_human_shape_acceptance` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/nordic/draugr_warrior_visual_review.png` | 北欧亡灵；圆盾 |
| nordic | `valkyrie` | `pending_human_shape_review` | `True` | `True` | `True` | `True` | `0.5714/0.6122/1.1639` | 22 | `True` | `await_human_shape_acceptance` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/nordic/valkyrie_visual_review.png` | 翼盔；双翼 |
| nordic | `fenrir_wolf` | `pending_human_shape_review` | `True` | `True` | `True` | `True` | `0.681/0.6594/0.3533` | 20 | `True` | `await_human_shape_acceptance` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/nordic/fenrir_wolf_visual_review.png` | 巨狼；红眼 |
| nordic | `dwarf_berserker` | `pending_human_shape_review` | `True` | `True` | `True` | `True` | `0.6263/0.7264/1.4` | 19 | `True` | `await_human_shape_acceptance` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/nordic/dwarf_berserker_visual_review.png` | 矮宽比例；大胡须 |
| chinese | `azure_dragon` | `pending_human_shape_review` | `True` | `True` | `True` | `True` | `1.1667/1.0156/0.375` | 31 | `True` | `await_human_shape_acceptance` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/chinese/azure_dragon_visual_review.png` | 东方龙长身；双角和龙须 |
| chinese | `qilin_guardian` | `pending_human_shape_review` | `True` | `True` | `True` | `True` | `0.75/0.6604/0.4928` | 34 | `True` | `await_human_shape_acceptance` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/chinese/qilin_guardian_visual_review.png` | 麒麟鹿角；鳞甲 |
| chinese | `jiangshi` | `pending_human_shape_review` | `True` | `True` | `True` | `True` | `0.6667/0.7381/1.7391` | 13 | `True` | `await_human_shape_acceptance` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/chinese/jiangshi_visual_review.png` | 官帽；符箓 |
| chinese | `nine_tailed_fox` | `pending_human_shape_review` | `True` | `True` | `True` | `True` | `0.7838/0.6818/0.5955` | 32 | `True` | `await_human_shape_acceptance` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/chinese/nine_tailed_fox_visual_review.png` | 九条尾巴；狐脸尖耳 |
| chinese | `stone_lion_guardian` | `pending_human_shape_review` | `True` | `True` | `True` | `True` | `0.6852/0.7266/0.6962` | 32 | `True` | `await_human_shape_acceptance` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/chinese/stone_lion_guardian_visual_review.png` | 石狮；卷鬃 |
| atlantean | `crystal_leviathan` | `pending_human_shape_review` | `True` | `True` | `True` | `True` | `0.881/0.7609/0.3812` | 27 | `True` | `await_human_shape_acceptance` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/atlantean/crystal_leviathan_visual_review.png` | 长身海兽；水晶背刺 |
| atlantean | `orichalcum_golem` | `pending_human_shape_review` | `True` | `True` | `True` | `True` | `0.4327/0.6667/0.9057` | 15 | `True` | `await_human_shape_acceptance` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/atlantean/orichalcum_golem_visual_review.png` | 山铜装甲；发光胸核 |
| atlantean | `siren_priestess` | `pending_human_shape_review` | `True` | `True` | `True` | `True` | `0.569/0.7308/1.3229` | 23 | `True` | `await_human_shape_acceptance` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/atlantean/siren_priestess_visual_review.png` | 海妖人形上身；鱼尾 |
| atlantean | `tidal_guardian` | `pending_human_shape_review` | `True` | `True` | `True` | `True` | `0.663/0.4/0.3688` | 21 | `True` | `await_human_shape_acceptance` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/atlantean/tidal_guardian_visual_review.png` | 厚龟甲；蟹钳 |
| atlantean | `abyssal_ray` | `pending_human_shape_review` | `True` | `True` | `True` | `True` | `0.7619/0.6727/0.3571` | 24 | `True` | `await_human_shape_acceptance` | `docs/preview_assets/creature_visual_acceptance/visual_review_boards/atlantean/abyssal_ray_visual_review.png` | 巨大翼展；扁平鳐身 |

## 验收规则

- 先看概念图和设计包，再看 Blockbench 三视图；测试只验证设计是否落地，不负责临场重做造型。
- 每个生物至少看 `front`、`side`、`three_quarter` 三张 Blockbench 预览和 `texture_fit.png`。
- 有用户反馈且仍是修订待确认的 Greek 项：`cyclops`。
- `pegasus` 和 `hydra` 当前不是“待确认修订”，而是 `design_rejected_requires_v6_modeling_spec_approval`；必须先签收 V6 可建模规格，才能重新建模。
- 非 Greek 20 个生物虽然自动证据齐全，但仍停在 `pending_human_shape_review`，不能视为最终美术完成。
- `needs_detail_contract_refinement` 表示该生物还只是流程证据齐全，不满足 1.0 模型精细度；必须补结构件、五官/武器/翼/尾/鳞片等细节件和 runtime 命名部件后再进入人工签收。

