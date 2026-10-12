# Pegasus / Hydra V6 可建模规格包（2026-06-15）

V6 把用户指出的天马头大、九头蛇断颈和大头化问题转成建模前硬规格。它不是建模放行结果；只有用户确认并写回 `approved_for_modeling` 后，才能开始 `.bbmodel`、UV、贴图和 Java runtime 重建。

- 状态：`modeling_spec_ready_for_user_review`
- 结论：`not_approved_for_modeling`
- 建模允许：`False`

## 审批材料

| 生物 | V6 合同 | V6 规格图 | 当前结论 |
| --- | --- | --- | --- |
| Pegasus / 天马 | `docs/美术资源/greek_creatures/design_blueprints/pegasus_v6_modeling_spec_contract.md` | `docs/preview_assets/greek_creatures/design_approval/v6/pegasus_v6_modeling_spec_sheet.png` | `modeling_spec_concrete_but_unapproved` |
| Hydra / 九头蛇 | `docs/美术资源/greek_creatures/design_blueprints/hydra_v6_modeling_spec_contract.md` | `docs/preview_assets/greek_creatures/design_approval/v6/hydra_v6_modeling_spec_sheet.png` | `modeling_spec_concrete_but_unapproved` |

## 规格图

### Pegasus / 天马

![pegasus V6 modeling spec](../../preview_assets/greek_creatures/design_approval/v6/pegasus_v6_modeling_spec_sheet.png)

必须先确认：

- [ ] adult horse side silhouette and body length are accepted
- [ ] head_core and muzzle dimensions are accepted as small horse head
- [ ] two-segment sloped neck and visible withers connection are accepted
- [ ] wing roots, saddle_space, and six feather layers are accepted
- [ ] white UV zones include eyes, nostrils, mouth, mane, hooves, and feather edges

本轮已锁定：

- `head_must_be_smaller_than_current_and_horse_like`
- `must_reference_adult_minecraft_horse_proportions_before_wings`
- `flight_and_ridden_flight_space_locked_before_modeling`
- `details_must_come_from_geometry_and_uv_not_head_scale`

### Hydra / 九头蛇

![hydra V6 modeling spec](../../preview_assets/greek_creatures/design_approval/v6/hydra_v6_modeling_spec_sheet.png)

必须先确认：

- [ ] exactly nine heads and nine sockets are accepted
- [ ] each neck path has root, mid, tip, and head_core overlap
- [ ] low long body and shoulder_saddle socket band are accepted
- [ ] each head has jaw, eye, mouth, fang, and horn or poison gland
- [ ] Java runtime rebuild from five heads to nine heads is required

本轮已锁定：

- `current_five_head_blockbench_and_java_models_are_rejected`
- `nine_heads_nine_sockets_nine_connected_neck_paths_locked`
- `each_head_must_connect_to_body_via_socket_and_overlap_geometry`
- `low_long_body_and_head_detail_locked_without_oversized_heads`

## 禁止事项

- 不允许基于当前 V2 `.bbmodel` 或当前 Java runtime 微调天马/九头蛇。
- 不允许用放大头部换取眼睛、嘴、獠牙、吻部、角或毒腺细节。
- 不允许把 V6 规格图当成自动建模许可；它必须先被用户签收。
- 不允许测试阶段重新发明造型；测试只验证已批准设计是否落地。
