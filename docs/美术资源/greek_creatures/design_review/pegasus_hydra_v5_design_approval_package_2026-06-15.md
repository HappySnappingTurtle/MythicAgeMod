# Pegasus / Hydra V5 设计审批候选包（2026-06-15）

V5 用于纠正当前天马和九头蛇畸形问题。它是设计审批材料，不是建模放行结果。

- 状态：`candidate_needs_user_review`
- 结论：`not_approved_for_modeling`
- 建模允许：`False`

## 审批材料

| 生物 | V5 合同 | V5 设计图 | 审批结论 |
| --- | --- | --- | --- |
| Pegasus / 天马 | `docs/美术资源/greek_creatures/design_blueprints/pegasus_v5_approval_candidate_contract.md` | `docs/preview_assets/greek_creatures/design_approval/v5/pegasus_v5_approval_candidate_sheet.png` | `candidate_needs_user_review` |
| Hydra / 九头蛇 | `docs/美术资源/greek_creatures/design_blueprints/hydra_v5_approval_candidate_contract.md` | `docs/preview_assets/greek_creatures/design_approval/v5/hydra_v5_approval_candidate_sheet.png` | `candidate_needs_user_review` |

## 设计图

### Pegasus / 天马

![pegasus V5 approval candidate](../../preview_assets/greek_creatures/design_approval/v5/pegasus_v5_approval_candidate_sheet.png)

必须先确认：

- [ ] side view reads as adult pure-white horse first
- [ ] head is small elongated horse head and not hidden by wings
- [ ] neck slopes from withers and does not become a vertical tower
- [ ] wing roots sit behind shoulders with layered feathers
- [ ] pure-white texture plan has fixed face, hoof, mane, and feather UV zones

本轮针对用户反馈锁定：

- `head_must_not_be_oversized`
- `must_reference_adult_horse_body_proportions`
- `head_neck_wing_root_rider_slot_must_be_visible_before_modeling`
- `self_flight_and_ridden_flight_must_share_clear_saddle_space`

### Hydra / 九头蛇

![hydra V5 approval candidate](../../preview_assets/greek_creatures/design_approval/v5/hydra_v5_approval_candidate_sheet.png)

必须先确认：

- [ ] exactly nine heads are visible unless design package is revised
- [ ] nine sockets and nine neck roots are accepted
- [ ] each neck path connects root, mid, tip, and head core
- [ ] side view reads as low long body with long tail
- [ ] each head has eye, mouth line, jaw, fang, and horn or poison gland

本轮针对用户反馈锁定：

- `current_five_head_model_is_rejected`
- `default_head_count_is_nine`
- `each_head_must_connect_to_body_via_neck_root_socket`
- `body_must_be_low_long_not_bulky`

## 禁止事项

- 不允许基于当前 `.bbmodel` 修天马或九头蛇。
- 不允许用放大头部换取眼睛、嘴、獠牙等细节。
- 不允许测试阶段重新发明造型；测试只能验证已批准设计是否落地。
- 未审批前，不允许进入最终 Blockbench、UV、纹理、Java runtime 或动作同步。
