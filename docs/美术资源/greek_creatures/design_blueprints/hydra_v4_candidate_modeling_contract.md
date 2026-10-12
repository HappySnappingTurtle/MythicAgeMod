---
creature_id: hydra
creature_name: Hydra / 九头蛇
design_version: v4_candidate
design_status: candidate_needs_user_review
modeling_allowed: false
previous_v2_assets_status: rejected_disconnected
previous_v3_status: topology_direction_but_detail_insufficient
candidate_sheet: docs/preview_assets/greek_creatures/design_approval/v4/hydra_v4_final_candidate_sheet.png
design_gate_required: true
---

# Hydra / 九头蛇 V4 候选建模契约

## 当前结论

当前九头蛇设计未正确通过。旧 V2/当前模型存在头颈身体断开、身体臃肿和头部比例错误问题；V3 只说明拓扑方向，仍不足以直接建模。V4 仍然是候选契约，必须由用户确认后才允许进入 Blockbench。

## 设计目标

低伏长身的沼泽蛇怪，默认九头，九条颈从同一肩带/背甲区域生长。压迫感来自长身、长尾、九头扇形错层、毒性鳞片和每个蛇头的面部细节，不来自臃肿身体或巨大方头。

## 硬比例与拓扑

- 碰撞盒目标：`3.2w x 2.8h`。
- 视觉高度：`2.8` 到 `3.1` 格。
- 默认头数：`9`，除非设计包先被改写并重新审批。
- 主身体长度为身体高度的 `3.0` 到 `4.0` 倍。
- 主身体高度不超过整体视觉高度的 `0.32`。
- 每个头宽约为肩带总宽的 `0.12` 到 `0.18`。
- 中央头可略大，但不得超过相邻头的 `1.18` 倍。
- 每条颈必须有 `neck_root`、`neck_mid`、`neck_tip` 三段。
- `neck_root` 必须与对应 `neck_socket` 和 `shoulder_saddle` 几何重叠。
- `neck_tip` 必须与 `head_core` 几何重叠。

## 正确造型要求

- 45 度必须同时看到九头错层、九个 socket、低伏身体、长尾和背脊鳞。
- 正面必须是扇形九头，有间隙，不是一堵头墙。
- 侧面必须是低长身体和长尾，不是胖蜥蜴或大箱体。
- 每个头是楔形蛇头，必须有眼、上颚、下颚、嘴线、獠牙和角/毒腺之一。
- 细节必须来自独立部件和 UV，不允许通过放大头部解决。

## Blockbench 层级

- `root/body/slender_body`
- `root/body/chest_keel`
- `root/body/shoulder_saddle`
- `root/body/neck_socket_01`
- `root/body/neck_socket_02`
- `root/body/neck_socket_03`
- `root/body/neck_socket_04`
- `root/body/neck_socket_05`
- `root/body/neck_socket_06`
- `root/body/neck_socket_07`
- `root/body/neck_socket_08`
- `root/body/neck_socket_09`
- `root/body/spine_crests_01_08`
- `root/body/tail_base`
- `root/body/tail_mid`
- `root/body/tail_tip`
- `root/legs/front_left`
- `root/legs/front_right`
- `root/legs/back_left`
- `root/legs/back_right`
- `root/necks/head_01_center/neck_root/neck_mid/neck_tip/head_core/upper_jaw/lower_jaw/left_eye/right_eye/left_fang/right_fang/horns_or_poison_glands`
- `root/necks/head_02_inner_left/neck_root/neck_mid/neck_tip/head_core/upper_jaw/lower_jaw/left_eye/right_eye/left_fang/right_fang/horns_or_poison_glands`
- `root/necks/head_03_inner_right/neck_root/neck_mid/neck_tip/head_core/upper_jaw/lower_jaw/left_eye/right_eye/left_fang/right_fang/horns_or_poison_glands`
- `root/necks/head_04_mid_left/neck_root/neck_mid/neck_tip/head_core/upper_jaw/lower_jaw/left_eye/right_eye/left_fang/right_fang/horns_or_poison_glands`
- `root/necks/head_05_mid_right/neck_root/neck_mid/neck_tip/head_core/upper_jaw/lower_jaw/left_eye/right_eye/left_fang/right_fang/horns_or_poison_glands`
- `root/necks/head_06_outer_left/neck_root/neck_mid/neck_tip/head_core/upper_jaw/lower_jaw/left_eye/right_eye/left_fang/right_fang/horns_or_poison_glands`
- `root/necks/head_07_outer_right/neck_root/neck_mid/neck_tip/head_core/upper_jaw/lower_jaw/left_eye/right_eye/left_fang/right_fang/horns_or_poison_glands`
- `root/necks/head_08_rear_left/neck_root/neck_mid/neck_tip/head_core/upper_jaw/lower_jaw/left_eye/right_eye/left_fang/right_fang/horns_or_poison_glands`
- `root/necks/head_09_rear_right/neck_root/neck_mid/neck_tip/head_core/upper_jaw/lower_jaw/left_eye/right_eye/left_fang/right_fang/horns_or_poison_glands`

## UV 与纹理

- `head_front_01_09`: 每个头固定眼、眉、上颚、下颚、嘴线、獠牙位置。
- `jaw_side_01_09`: 蛇头楔形侧面和嘴角。
- `neck_scale_01_09`: 每条颈的鳞片方向必须顺着颈段。
- `neck_socket_band`: 九个颈根 socket 与肩带阴影，用于证明颈从身体长出。
- `body_side`: 深绿低伏身体，不允许无细节深绿大块。
- `spine_crests`: 从肩到尾连续的背脊鳞。
- `tail`: 尾巴逐段变细的鳞片。
- `legs_claws`: 四肢、爪和腹部阴影。

## 动画约束

- 待机：九头错相摆动，身体低伏不膨胀。
- 攻击：中央头和左右内侧头前探，外侧头维持扇形威胁。
- 任何动画状态下，`neck_root` 与 socket、`neck_tip` 与 head_core 不得断开。

## 设计否决条件

- 只有五头却没有先改设计包。
- 头和身体没有实体连接。
- 多头像插在背上的柱子。
- 身体臃肿、正面大箱体化。
- 头被放大到破坏整体比例。
- 头太小且缺少眼、嘴、獠牙等神话特征。
- 45 度视角糊成头墙。

## 建模许可

当前为 `candidate_needs_user_review`，`modeling_allowed=false`。用户确认本契约和 V4 图后，才能把状态改为 `approved_for_modeling` 并进入 `.bbmodel`、UV、纹理和 Java runtime 同步。
