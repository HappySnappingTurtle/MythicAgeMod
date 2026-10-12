---
creature_id: pegasus
creature_name: Pegasus / 天马
design_version: v5_approval_candidate
design_status: candidate_needs_user_review
modeling_allowed: false
previous_assets_status: rejected_malformed
approval_sheet: docs/preview_assets/greek_creatures/design_approval/v5/pegasus_v5_approval_candidate_sheet.png
---

# Pegasus / 天马 V5 审批候选设计合同

## 结论

当前模型和旧设计均不正确。V5 不是建模许可，而是新的可审批设计图和合同。只有用户明确签收后，才能把对应设计包改为 `approved_for_modeling` 与 `modeling_allowed=true`。

## 造型目标

先是一匹正常成年白马，再是长出双翼的神话坐骑。识别顺序固定为：成年马体 -> 小而细长马头 -> 斜向颈部 -> 肩后双翼 -> 可载人飞行空间。

## 关键比例

- 主躯干长度为躯干高度的 `1.9` 到 `2.2` 倍。
- 头宽为身体宽度的 `0.35` 到 `0.45`，不允许大头化。
- 头高为胸腔高度的 `0.45` 到 `0.60`。
- 吻部长度大于头核心长度的 `0.55`。
- 颈部必须从鬐甲/肩胛斜向前上方伸出。
- 翼根必须在肩后；骑乘点不得遮挡头颈或翼根。

## Blockbench 结构

- `root/body/body_barrel/chest/rump/withers/saddle_space`
- `root/head_neck/neck_lower/neck_upper/mane_strip/head_core/muzzle/eyes/nostrils/mouth_line/ears`
- `root/wings/left_wing` 和 `root/wings/right_wing`，每侧含 `shoulder_joint/upper_wing/secondary_wing/coverts/primary_feathers_01_06`
- `root/legs/front_left/front_right/back_left/back_right/hoof`
- `root/tail/tail_base/tail_plume_segments`

## 纹理与 UV

纯白不是空白。头、鼻孔、嘴线、眼、鬃毛、马蹄、羽片边缘必须有固定 UV 区和浅灰分面。禁止使用随机白灰面板作为最终纹理。

## 否决条件

- 头太大、头看不见、颈部竖直、身体短胖。
- 翅膀像白条或平板，缺少羽片层级。
- 骑乘点挡住翼根或头颈。
- 白色贴图在 Blockbench 或 MC 中像透明/空白面。
