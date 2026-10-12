---
creature_id: valkyrie
creature_name: Valkyrie / 女武神
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

# Valkyrie / 女武神 设计包

## 目标体量
- Entity ID: `mythic_age:valkyrie`
- 碰撞盒目标：`0.9w x 2.35h`
- 视觉高度目标：约 `2.55` 格
- 比例说明：精英飞行人形，翼、翼盔、长矛和金属甲必须明确；头部保持成人形比例，不能靠大头读身份。

## 轮廓语言
- 正面：双翼、翼盔和长矛竖线可读。
- 侧面：披风、翼厚度和矛尖明显。
- 45 度：两翼和披风分离。

## 概念设计图
- 文件：`docs/美术资源/nordic_creatures/concepts/valkyrie_concept.png`
- 用途：这是建模前的造型合同；Blockbench 和 MC 截图只验证是否实现该设计，不负责重新发明造型。
- 三视角：正面、侧面、45 度必须同时能识别该生物。

## V2 造型冻结
- 触发依据：用户实测反馈指出当前模型存在体量、精细度、纹理空白和神话特征不可读问题；测试只验证设计落地，不再承担造型试错。
- 正面必须读到翼盔、银金甲、双翼和长矛，不得像普通人形飞行单位，也不得用大头替代翼盔细节。
- 翼羽必须分段；头盔小翼与背翼不能混淆，长矛要形成清晰竖线和刺击方向。
- 飞行单位需要保持披风、翼根、腿部和矛臂结构可读。

## 核心识别特征
- 翼盔
- 双翼
- 长矛
- 银金甲
- 比例合理的头盔脸部

## 部件清单
- `head`
- `face`
- `winged_helmet`
- `helmet_left_wing`
- `helmet_right_wing`
- `armor`
- `armor_plate`
- `cape`
- `left_wing`
- `right_wing`
- `wing_feather_strips`
- `left_arm`
- `spear_arm`
- `spear_shaft`
- `spear_tip`
- `left_leg`
- `right_leg`
- `mouth_line`
- `helmet_crest`
- `left_helmet_feather_tip`
- `right_helmet_feather_tip`
- `left_wing_primary_a`
- `left_wing_primary_b`
- `left_wing_primary_c`
- `right_wing_primary_a`
- `right_wing_primary_b`
- `right_wing_primary_c`
- `left_bracer`
- `spear_bracer`
- `spear_blade`
- `spear_counterweight`
- `left_boot`
- `right_boot`
- `cape_left_rip`
- `cape_right_rip`

## Blockbench 层级
- `root/head`
- `root/body`
- `root/wings`
- `root/weapon`
- `root/legs`

## 建模硬性要求
- wings 分段
- spear 独立
- helmet wings 独立或高对比
- 基础头块宽度不得超过胸甲宽度约七成，识别由翼盔和矛完成

## UV 计划
- helmet: gold wings
- armor: silver plates
- wings: pale feathers
- spear: bright tip

## 纹理细节
- 主色：`#d8d7cf`
- 阴影：`#5a6270`
- 强调色：`#d7b65a`
- 细节规则：银甲、金色翼盔、浅色羽翼、红暗披风和亮矛尖。

## 动画约束
- 待机轻拍翼；飞行翼大幅摆动；攻击时长矛前刺。

## 返工触发条件
- 像普通村民
- 没有翼
- 没有长矛

## 验收口径
- 先验收概念设计图，再进入 Blockbench 建模。
- 必须提供正面、侧面、45 度三视图，且三视图来自已挂载贴图后的模型预览。
- 必须提供 UV 模板和贴图贴合报告，关键 UV 区域不得透明、空白或越界。
- 如三视图或贴合报告失败，优先修 .bbmodel、UV 或贴图。
