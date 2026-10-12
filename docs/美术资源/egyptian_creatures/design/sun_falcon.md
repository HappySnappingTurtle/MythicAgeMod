---
creature_id: sun_falcon
creature_name: Sun Falcon / 太阳隼
design_status: needs_user_review
design_sheet_status: needs_user_review
design_approval_status: approval_pending
design_correctness_verdict: not_reviewed
design_detail_sheet_status: needs_user_review
design_detail_board_status: needs_user_review
modeling_allowed: false
design_package_status: generated_reference_needs_human_approval
approval_basis: human_design_review_required_before_model_uv_texture_animation_or_runtime_sync
current_v2_assets_status: generated_reference_only
blocking_reason: design package exists but has not been formally approved for 1.0 modeling
---

# Sun Falcon / 太阳隼 设计包

## 目标体量
- Entity ID: `mythic_age:sun_falcon`
- 碰撞盒目标：`1.2w x 1.3h`
- 视觉高度目标：约 `1.45` 格
- 比例说明：中型飞行单位，隼头、展开翼和太阳圆盘必须明确。

## 轮廓语言
- 正面：双翼横展、头顶太阳盘。
- 侧面：尖喙、翼羽和尾羽明显。
- 45 度：翼羽分段和尾羽可读。

## 概念设计图
- 文件：`docs/美术资源/egyptian_creatures/concepts/sun_falcon_concept.png`
- 用途：这是建模前的造型合同；Blockbench 和 MC 截图只验证是否实现该设计，不负责重新发明造型。
- 三视角：正面、侧面、45 度必须同时能识别该生物。

## V2 造型冻结
- 触发依据：用户实测反馈指出当前模型存在体量、精细度、纹理空白和神话特征不可读问题；测试只验证设计落地，不再承担造型试错。
- 正面第一识别必须是太阳圆盘、尖喙隼头和横展双翼，不能只是普通鸟。
- 头部主体必须服从飞鸟身体比例；太阳盘必须独立高亮，头部眼睛和尖喙在侧面必须清楚。
- 翼羽至少三段分层，尾羽独立，飞行单位轮廓必须成立。

## 核心识别特征
- 太阳盘
- 隼头尖喙
- 分段翼羽

## 部件清单
- `falcon_body`
- `belly`
- `head`
- `beak`
- `left_eye`
- `right_eye`
- `sun_disk`
- `sun_disk_top_ray`
- `sun_disk_bottom_ray`
- `left_wing`
- `left_feathers`
- `right_wing`
- `right_feathers`
- `tail`
- `tail_tip`
- `beak_tip`
- `left_brow`
- `right_brow`
- `sun_disk_left_ray`
- `sun_disk_right_ray`
- `left_primary_feather_a`
- `left_primary_feather_b`
- `left_primary_feather_c`
- `right_primary_feather_a`
- `right_primary_feather_b`
- `right_primary_feather_c`
- `left_talon`
- `right_talon`

## Blockbench 层级
- `root/body`
- `root/head`
- `root/wings`
- `root/tail`

## 建模硬性要求
- sun_disk 独立
- wing_feathers 分段
- beak 高对比

## UV 计划
- head_front: falcon eyes
- sun_disk: gold disk
- wing_feathers: brown-gold bands

## 纹理细节
- 主色：`#8f4b22`
- 阴影：`#2d1a12`
- 强调色：`#f2c14e`
- 细节规则：褐金羽毛、金色太阳盘、黑色隼眼和浅色尖喙。

## 动画约束
- 待机轻拍翼；飞行大幅拍翼；俯冲攻击时头和喙前压。

## 返工触发条件
- 像普通鸟
- 没有太阳盘
- 翼没有羽片

## 验收口径
- 先验收概念设计图，再进入 Blockbench 建模。
- 必须提供正面、侧面、45 度三视图，且三视图来自已挂载贴图后的模型预览。
- 必须提供 UV 模板和贴图贴合报告，关键 UV 区域不得透明、空白或越界。
- 如三视图或贴合报告失败，优先修 .bbmodel、UV 或贴图。
