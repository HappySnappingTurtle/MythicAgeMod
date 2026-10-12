---
creature_id: hydra
creature_name: Hydra / 九头蛇
design_version: v5_approval_candidate
design_status: candidate_needs_user_review
modeling_allowed: false
previous_assets_status: rejected_wrong_head_count_and_disconnected
approval_sheet: docs/preview_assets/greek_creatures/design_approval/v5/hydra_v5_approval_candidate_sheet.png
---

# Hydra / 九头蛇 V5 审批候选设计合同

## 结论

当前五头/头墙式模型不正确，不能继续修。V5 重新定义九头蛇的可建模设计：低伏长身、默认九头、九个 socket、九条连续颈路径和每个头的面部细节。

## 造型目标

压迫感来自低伏长身、长尾、九头错层、颈根连接和毒性鳞片，不来自臃肿身体或大头堆叠。

## 关键比例与拓扑

- 默认头数：`9`。
- 主身体长度为身体高度的 `3.0` 到 `4.0` 倍。
- 主身体高度不超过整体视觉高度的 `0.32`。
- 每个头宽为肩带宽度的 `0.12` 到 `0.18`。
- 每个头必须有 `neck_root -> neck_mid -> neck_tip -> head_core` 连续路径。
- 每个 `neck_root` 必须与命名 `neck_socket` 和 `shoulder_saddle` 几何重叠。
- 每个 `neck_tip` 必须与 `head_core` 几何重叠。

## Blockbench 结构

- `root/body/slender_body/chest_keel/shoulder_saddle/neck_socket_01_09/spine_crests_01_08/tail_base/tail_mid/tail_tip`
- `root/necks/head_01_center` 到 `head_09_rear_right`
- 每个 head group 必须含 `neck_root/neck_mid/neck_tip/head_core/upper_jaw/lower_jaw/left_eye/right_eye/left_fang/right_fang/horns_or_poison_glands`

## 纹理与 UV

每个头都有独立 `head_front_01_09`、`jaw_side_01_09` 和 `neck_scale_01_09` 纹理区。肩带必须有 `neck_socket_band` 阴影区，用于证明颈从身体长出。

## 否决条件

- 少于九头但没有先改设计包。
- 头和身体悬空或只靠命名暗示连接。
- 多头像插在背上的柱子。
- 身体臃肿、头墙化、单头缺少眼嘴獠牙等特征。
