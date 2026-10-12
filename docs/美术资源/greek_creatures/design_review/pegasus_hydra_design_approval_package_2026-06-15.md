# Pegasus / Hydra V3 设计审批包（2026-06-15）

本文档用于建模前审批。它不表示设计已通过；只有用户确认蓝图、比例/拓扑锁定表和设计图后，才允许重新进入 Blockbench、纹理、动作和 Java runtime。

- 当前状态：`approval_pending`
- 阻断数量：2

## 审批项

| 生物 | 状态 | 蓝图 | 细化锁定表 | 细化设计板 | PNG 设计图 | 旧/新对照板 | 下一步 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `pegasus` | `approval_pending` | `docs/美术资源/greek_creatures/design_blueprints/pegasus_v3_blueprint.md` | `docs/美术资源/greek_creatures/design_sheets/pegasus_v3_proportion_lock.md` | `docs/preview_assets/greek_creatures/design_approval/detail_boards/pegasus_v3_proportion_detail_board.png` | `docs/preview_assets/greek_creatures/design_approval/pegasus_v3_design_sheet.png` | `docs/preview_assets/greek_creatures/design_approval/pegasus_v3_design_approval_board.png` | `user_review_blueprint_detail_lock_png_sheet_and_approval_board` |
| `hydra` | `approval_pending` | `docs/美术资源/greek_creatures/design_blueprints/hydra_v3_blueprint.md` | `docs/美术资源/greek_creatures/design_sheets/hydra_v3_topology_lock.md` | `docs/preview_assets/greek_creatures/design_approval/detail_boards/hydra_v3_topology_detail_board.png` | `docs/preview_assets/greek_creatures/design_approval/hydra_v3_design_sheet.png` | `docs/preview_assets/greek_creatures/design_approval/hydra_v3_design_approval_board.png` | `user_review_blueprint_detail_lock_png_sheet_and_approval_board` |

## 设计图预览

### Pegasus / 天马

![pegasus design sheet](../../preview_assets/greek_creatures/design_approval/pegasus_v3_design_sheet.png)

![pegasus approval board](../../preview_assets/greek_creatures/design_approval/pegasus_v3_design_approval_board.png)

- 设计正确性结论：`direction_correct_but_detail_insufficient`
- 细化锁定状态：`draft_requires_user_approval`
- 细化设计板状态：`draft_requires_user_approval`
- 细化锁定表：`docs/美术资源/greek_creatures/design_sheets/pegasus_v3_proportion_lock.md`
- 细化设计板：`docs/preview_assets/greek_creatures/design_approval/detail_boards/pegasus_v3_proportion_detail_board.png`

![pegasus detail board](../../preview_assets/greek_creatures/design_approval/detail_boards/pegasus_v3_proportion_detail_board.png)

必须确认：

- [ ] Side view reads as a white horse before it reads as a winged unit.
- [ ] Head is small, elongated, and forward of the chest; never a toy-like cube.
- [ ] Neck slopes from the withers; no vertical tower neck.
- [ ] Layered wings attach behind shoulders and do not hide head or rider space.
- [ ] Pure white texture plan still has eyes, nostrils, mouth, mane, hooves, and grey planes.

细化锁定必须确认：

- [ ] Original-horse-like body proportion is locked before modeling.
- [ ] Head, muzzle, and neck anchors are approved as small elongated horse anatomy.
- [ ] Wing root and rider space are approved without hiding the head or neck.

旧设计拒绝原因：

- Old preview has vertical tower head/neck.
- Wing reads as flat horizontal strips.
- Horse body/neck/head relationship is not established.

### Hydra / 九头蛇

![hydra design sheet](../../preview_assets/greek_creatures/design_approval/hydra_v3_design_sheet.png)

![hydra approval board](../../preview_assets/greek_creatures/design_approval/hydra_v3_design_approval_board.png)

- 设计正确性结论：`direction_correct_but_detail_insufficient`
- 细化锁定状态：`draft_requires_user_approval`
- 细化设计板状态：`draft_requires_user_approval`
- 细化锁定表：`docs/美术资源/greek_creatures/design_sheets/hydra_v3_topology_lock.md`
- 细化设计板：`docs/preview_assets/greek_creatures/design_approval/detail_boards/hydra_v3_topology_detail_board.png`

![hydra detail board](../../preview_assets/greek_creatures/design_approval/detail_boards/hydra_v3_topology_detail_board.png)

必须确认：

- [ ] Default is nine heads unless the design is explicitly changed before modeling.
- [ ] Every neck has root, mid, tip, and overlaps a visible shoulder socket.
- [ ] Body is low and long; mass cannot become a front-facing cube.
- [ ] Each head has eyes, jaw split, mouth line, fang, and crest or poison gland.
- [ ] 45 degree view shows staggered heads, not a single block wall.

细化锁定必须确认：

- [ ] Nine named sockets and nine neck roots are approved before modeling.
- [ ] Each head-neck-body connection is approved as real overlapping volume.
- [ ] Low long body and tail remain primary mass, not a front block.

旧设计拒绝原因：

- Old preview reads as a blocky head wall.
- Head-neck-body connections are not structurally proven.
- Front body mass is too dominant for a low long hydra.

