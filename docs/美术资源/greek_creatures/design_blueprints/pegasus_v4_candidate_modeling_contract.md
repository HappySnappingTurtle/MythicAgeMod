---
creature_id: pegasus
creature_name: Pegasus / 天马
design_version: v4_candidate
design_status: candidate_needs_user_review
modeling_allowed: false
previous_v2_assets_status: rejected_malformed
previous_v3_status: direction_correct_but_detail_insufficient
candidate_sheet: docs/preview_assets/greek_creatures/design_approval/v4/pegasus_v4_final_candidate_sheet.png
design_gate_required: true
---

# Pegasus / 天马 V4 候选建模契约

## 当前结论

当前天马设计未正确通过。旧 V2/当前模型已经畸形，不能继续在其上微调；V3 只给出方向，不能作为直接建模依据。V4 仍然是候选契约，必须由用户确认后才允许进入 Blockbench。

## 设计目标

先是一匹比例成立的成年白马，再是有双翼、能自主飞行、能载玩家飞行的神话坐骑。识别顺序固定为：白马身体 -> 小而细长的马头 -> 斜向颈部 -> 肩后双翼 -> 骑乘飞行空间。

## 硬比例

- 碰撞盒目标：`1.7w x 2.1h`。
- 视觉高度：`2.15` 到 `2.25` 格。
- 主躯干长度为躯干高度的 `1.9` 到 `2.2` 倍。
- 头部宽度为身体宽度的 `0.35` 到 `0.45`。
- 头部高度为胸腔高度的 `0.45` 到 `0.60`。
- 吻部长度为头部核心长度的 `0.55` 到 `0.75`。
- 翼根位于肩胛后方，不能长在腹部、背中部或臀部。
- 玩家骑乘点位于背中段，不能遮住头颈、翼根或主要羽片。

## 正确造型要求

- 侧面必须先读成成年马，不能是大头方块、白色鸟、白色鹿或玩具马。
- 头部必须小、细长、在胸腔前方；眼、鼻孔、嘴线和耳朵靠部件与 UV 表达，不靠放大头部。
- 颈部从鬐甲/肩胛向前上方斜出，至少 `neck_lower`、`neck_upper` 两段，禁止竖直塔状颈。
- 每侧翅膀至少包含 `shoulder_joint`、`upper_wing`、`secondary_wing`、`coverts`、`primary_feathers_01_06`。
- 纯白不是空白：白色大面必须有浅灰分面、鼻口线、鬃毛线、马蹄和羽片边界。

## Blockbench 层级

- `root/body/body_barrel`
- `root/body/chest`
- `root/body/rump`
- `root/body/withers`
- `root/body/saddle_space`
- `root/head_neck/neck_lower`
- `root/head_neck/neck_upper`
- `root/head_neck/mane_strip`
- `root/head_neck/head_core`
- `root/head_neck/muzzle`
- `root/head_neck/left_eye`
- `root/head_neck/right_eye`
- `root/head_neck/left_nostril`
- `root/head_neck/right_nostril`
- `root/head_neck/mouth_line`
- `root/head_neck/left_ear`
- `root/head_neck/right_ear`
- `root/wings/left_wing/shoulder_joint`
- `root/wings/left_wing/upper_wing`
- `root/wings/left_wing/secondary_wing`
- `root/wings/left_wing/coverts`
- `root/wings/left_wing/primary_feathers_01_06`
- `root/wings/right_wing/shoulder_joint`
- `root/wings/right_wing/upper_wing`
- `root/wings/right_wing/secondary_wing`
- `root/wings/right_wing/coverts`
- `root/wings/right_wing/primary_feathers_01_06`
- `root/legs/front_left/upper_leg/lower_leg/hoof`
- `root/legs/front_right/upper_leg/lower_leg/hoof`
- `root/legs/back_left/upper_leg/lower_leg/hoof`
- `root/legs/back_right/upper_leg/lower_leg/hoof`
- `root/tail/tail_base`
- `root/tail/tail_plume_segments`

## UV 与纹理

- `head_front`: 双眼、鼻梁、鼻孔、嘴线固定在马头面，不允许漂到颈部或翼面。
- `muzzle_side`: 细长吻部侧面，必须帮助侧面读成马脸。
- `neck_side`: 浅灰颈部肌理和鬃毛根，不允许整块白柱。
- `body_side`: 暖白主身体与浅灰胸/臀/腹分面。
- `wing_top`: 翼肩、次翼、覆羽。
- `primary_feathers`: 每侧至少六片主羽，每片有灰色边线。
- `legs_hooves`: 腿部关节和马蹄。
- `tail_mane`: 白到浅灰的鬃尾分段。

## 动画约束

- 自主飞行：翼肩驱动，上翼、次翼、主羽分层拍动，马体保持水平。
- 骑乘飞行：`saddle_space` 稳定，玩家位置不挡头颈和翼根，翼片不穿玩家。
- 待机/行走：头颈小幅呼吸和马步，不允许像人形大头单位。

## 设计否决条件

- 头太大，或靠放大头部换取眼嘴细节。
- 头太小或被翼遮挡，导致看不出头部。
- 颈部竖直堆高。
- 身体短胖，不像马。
- 翅膀只是白条或平板。
- 纯白贴图渲染后像透明/空白面。

## 建模许可

当前为 `candidate_needs_user_review`，`modeling_allowed=false`。用户确认本契约和 V4 图后，才能把状态改为 `approved_for_modeling` 并进入 `.bbmodel`、UV、纹理和 Java runtime 同步。
