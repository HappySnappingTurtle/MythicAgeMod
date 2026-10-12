---
creature_id: hydra
creature_name: Hydra / 九头蛇
design_version: v6_modeling_spec
design_status: modeling_spec_ready_for_user_review
modeling_allowed: false
previous_assets_status: rejected_wrong_head_count_and_disconnected
approval_sheet: docs/preview_assets/greek_creatures/design_approval/v6/hydra_v6_modeling_spec_sheet.png
supersedes: v5_approval_candidate
---

# Hydra / 九头蛇 V6 可建模规格合同

## 结论

当前九头蛇 `.bbmodel` 和 Java runtime 都是五头实现，不满足九头蛇设计。V6 固定为九头、九个 socket、九条连续颈路径；任何减少头数都必须先改设计合同。

## Blockbench 拓扑锁

- 默认头数：`9`。
- `slender_body`: 约 `8w x 4h x 36l`，低伏长身，身体高度不超过整体视觉高度 `0.32`。
- `shoulder_saddle`: 必须承载 `neck_socket_01` 到 `neck_socket_09`。
- 每个 `neck_root` 必须与对应 `neck_socket` 几何重叠。
- 每条颈必须有 `neck_root -> neck_mid -> neck_tip -> head_core`。
- 每个 `neck_tip` 必须与 `head_core` 几何重叠。

## 必须存在的 group

- `root/body/slender_body/chest_keel/shoulder_saddle/neck_socket_01_09/spine_crests_01_08/tail_base/tail_mid/tail_tip`
- `root/necks/head_01_front_left` 到 `root/necks/head_09_rear_right`
- 每个 head group 必须含 `neck_root/neck_mid/neck_tip/head_core/upper_jaw/lower_jaw/left_eye/right_eye/left_fang/right_fang/horn_or_poison_gland`

## UV 与纹理锁

- `head_front_01` 到 `head_front_09` 独立分区，眼/嘴/牙不能靠随机纹理暗示。
- `neck_scale_01` 到 `neck_scale_09` 必须带方向性鳞片。
- `neck_socket_band` 必须在肩带处形成阴影，证明脖子从身体长出。

## Runtime 重建要求

- 当前 `HydraModel.java` 的 5 个 `HEAD_NAMES` / `NECK_NAMES` 必须作废。
- 新 runtime 必须有 9 个 neck/head 数组项，动画保持 root/socket 与 tip/head overlap。

## 否决条件

- 少于九头且没有先改设计包。
- 任一头漂浮、任一颈根不接 shoulder_saddle。
- 头墙化、身体臃肿、头靠放大获得细节。
- 单头缺眼、嘴、颌、牙、角/毒腺。
