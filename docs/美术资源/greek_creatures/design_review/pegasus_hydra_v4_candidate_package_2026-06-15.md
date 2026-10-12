# Pegasus / Hydra V4 候选设计包（2026-06-15）

本文档回答当前问题：天马和九头蛇的现有设计不正确，不能继续在畸形模型上调参。V4 是建模前候选契约，不是批准结果。

- 当前状态：`candidate_needs_user_review`
- 建模允许：`False`
- 结论：`not_approved_for_modeling`

## 审批材料

| 生物 | V4 候选契约 | V4 候选设计图 | 当前结论 |
| --- | --- | --- | --- |
| Pegasus / 天马 | `docs/美术资源/greek_creatures/design_blueprints/pegasus_v4_candidate_modeling_contract.md` | `docs/preview_assets/greek_creatures/design_approval/v4/pegasus_v4_final_candidate_sheet.png` | `candidate_needs_user_review` |
| Hydra / 九头蛇 | `docs/美术资源/greek_creatures/design_blueprints/hydra_v4_candidate_modeling_contract.md` | `docs/preview_assets/greek_creatures/design_approval/v4/hydra_v4_final_candidate_sheet.png` | `candidate_needs_user_review` |

## 设计图预览

### Pegasus / 天马

![pegasus V4 candidate sheet](../../preview_assets/greek_creatures/design_approval/v4/pegasus_v4_final_candidate_sheet.png)

该候选图必须由用户确认后才允许进入建模。它当前只证明“下一轮设计应如何约束”，不证明最终模型已经正确。

必须先确认：

- [ ] side silhouette reads as adult white horse
- [ ] head and muzzle ratios are accepted
- [ ] wing root and rider slot are accepted
- [ ] pure-white texture plan has fixed face and feather UV zones

已针对用户反馈新增的硬约束：

- `head_must_not_be_oversized`
- `must_follow_adult_horse_proportion`
- `must_keep_visible_head_muzzle_eyes_nostrils_mouth`
- `must_support_self_flight_and_ridden_flight`

### Hydra / 九头蛇

![hydra V4 candidate sheet](../../preview_assets/greek_creatures/design_approval/v4/hydra_v4_final_candidate_sheet.png)

该候选图必须由用户确认后才允许进入建模。它当前只证明“下一轮设计应如何约束”，不证明最终模型已经正确。

必须先确认：

- [ ] nine sockets and nine neck roots are accepted
- [ ] each neck root overlaps shoulder saddle
- [ ] each neck tip overlaps head core
- [ ] front and 45 degree views keep gaps instead of a head wall

已针对用户反馈新增的硬约束：

- `head_neck_body_connections_are_required`
- `default_head_count_is_nine`
- `body_must_be_low_and_long`
- `head_details_must_not_depend_on_oversized_heads`

## 建模门禁

- 当前 V2 模型与贴图不得继续作为修复依据。
- V3 只作为方向参考，不作为直接建模依据。
- V4 契约和设计图经用户确认后，才能把对应蓝图改为 `approved_for_modeling` 与 `modeling_allowed=true`。
- 未批准前，不允许生成新的最终 `.bbmodel`、最终纹理、动作或 Java runtime 同步。
