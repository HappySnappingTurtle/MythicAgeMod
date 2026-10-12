---
creature_id: pegasus
creature_name: Pegasus / 天马
design_version: v6_modeling_spec
design_status: modeling_spec_ready_for_user_review
modeling_allowed: false
previous_assets_status: rejected_malformed
approval_sheet: docs/preview_assets/greek_creatures/design_approval/v6/pegasus_v6_modeling_spec_sheet.png
supersedes: v5_approval_candidate
---

# Pegasus / 天马 V6 可建模规格合同

## 结论

当前天马 `.bbmodel`、贴图预览和 Java runtime 都不能继续作为修复基础。V6 不是建模许可，而是建模前必须签收的规格：先成立成年白马，再成立纯白飞行坐骑。

## Blockbench 尺寸锁

- `body_barrel`: 约 `10.5w x 7.2h x 22.0l`，视觉锚点是成年马横向躯干。
- `head_core`: 约 `3.2w x 3.8h x 4.4l`，头宽不超过身体宽度 `0.42`。
- `muzzle`: 约 `2.0w x 2.0h x 6.0l`，吻部必须前伸，不能用大方头代替马脸。
- `neck_lower` / `neck_upper`: 两段斜向连接鬐甲和头部，禁止单根竖直颈柱。
- `wing_root`: 位于肩后，不能盖住头颈或骑乘点。
- `primary_feathers_01_06`: 每侧至少 6 片可读主羽。

## 必须存在的 group

- `root/body/body_barrel/chest/rump/withers/saddle_space`
- `root/head_neck/neck_lower/neck_upper/head_core/muzzle/eyes/nostrils/mouth_line/ears/mane_strip`
- `root/wings/left_wing/shoulder_joint/upper_wing/secondary_wing/coverts/primary_feathers_01_06`
- `root/wings/right_wing/shoulder_joint/upper_wing/secondary_wing/coverts/primary_feathers_01_06`
- `root/legs/front_left/front_right/back_left/back_right/hooves`
- `root/tail/tail_base/tail_plume_segments`

## UV 与纹理锁

- 纯白身体不得是空白面；`body_side` 必须有浅灰分面。
- `head_front` 必须固定眼、鼻孔、鼻梁、嘴线 UV。
- `mane_strip`、`hoof_band`、`feather_edges` 必须独立分区。
- 禁止随机白灰面板作为最终贴图。

## Runtime 重建要求

- 当前 `PegasusModel.java` 不能作为比例源。
- 新 runtime 必须从 V6 `.bbmodel` 同步；保留自主飞行和骑乘飞行，但重新校验 `saddle_space` 与 `wing_root` 不互相遮挡。

## 否决条件

- 头部过大、头看不见、头颈像竖直白柱。
- 侧面不能先读成成年白马。
- 翼面像水平白板或遮挡马头。
- 骑乘位压住翼根。
