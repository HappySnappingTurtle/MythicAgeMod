---
creature_id: abyssal_ray
creature_name: Abyssal Ray / 深渊鳐
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

# Abyssal Ray / 深渊鳐 设计包

## 目标体量
- Entity ID: `mythic_age:abyssal_ray`
- 碰撞盒目标：`2.6w x 0.9h`
- 视觉高度目标：约 `1.2` 格
- 比例说明：深海飞鳐，宽大三角翼、长尾刺、发光腹纹和头部眼点必须清楚。

## 轮廓语言
- 正面：薄而宽的翼展和双眼。
- 侧面：扁平身体、尾刺和下垂鳍边。
- 45 度：长尾和发光背纹。

## 概念设计图
- 文件：`docs/美术资源/atlantean_creatures/concepts/abyssal_ray_concept.png`
- 用途：这是建模前的造型合同；Blockbench 和 MC 截图只验证是否实现该设计，不负责重新发明造型。
- 三视角：正面、侧面、45 度必须同时能识别该生物。

## V2 造型冻结
- 触发依据：用户实测反馈指出当前模型存在体量、精细度、纹理空白和神话特征不可读问题；测试只验证设计落地，不再承担造型试错。
- 正面必须读到薄而宽的深海鳐翼展、头部眼点和扁平身体，不得像普通鱼。
- 侧面必须读到长尾和尾刺，尾刺必须是独立尖细结构。
- 青色发光腹纹、亮眼、深蓝背部和翼尖必须分区可读，宽翼是第一轮廓。

## 核心识别特征
- 巨大翼展
- 扁平鳐身
- 长尾刺
- 发光腹纹

## 部件清单
- `ray_body`
- `head_lobe`
- `left_eye`
- `right_eye`
- `mouth_slit`
- `left_wing`
- `right_wing`
- `left_wing_tip`
- `right_wing_tip`
- `glow_belly`
- `tail`
- `tail_spine`
- `back_ridge`
- `left_gill_mark`
- `right_gill_mark`
- `left_wing_rib_a`
- `left_wing_rib_b`
- `left_wing_rib_c`
- `right_wing_rib_a`
- `right_wing_rib_b`
- `right_wing_rib_c`
- `glow_rune_front`
- `glow_rune_center`
- `glow_rune_back`
- `tail_barb_left`
- `tail_barb_right`

## Blockbench 层级
- `root/body`
- `root/wings`
- `root/tail`
- `root/glow`

## 建模硬性要求
- wings 必须宽
- tail_spine 必须长
- eyes 独立高对比

## UV 计划
- wings: dark blue gradients
- eyes: cyan dots
- belly: rune glow
- tail: bright spine

## 纹理细节
- 主色：`#183f5a`
- 阴影：`#071923`
- 强调色：`#7de7ff`
- 细节规则：深蓝背部、青色发光腹部符纹、亮眼和细长尾刺。

## 动画约束
- 飞/游时双翼缓慢拍动；攻击时尾刺甩动。

## 返工触发条件
- 像普通鱼
- 翼展不明显
- 没有尾刺

## 验收口径
- 先验收概念设计图，再进入 Blockbench 建模。
- 必须提供正面、侧面、45 度三视图，且三视图来自已挂载贴图后的模型预览。
- 必须提供 UV 模板和贴图贴合报告，关键 UV 区域不得透明、空白或越界。
- 如三视图或贴合报告失败，优先修 .bbmodel、UV 或贴图。
