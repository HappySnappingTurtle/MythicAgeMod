---
creature_id: qilin_guardian
creature_name: Qilin Guardian / 麒麟守卫
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

# Qilin Guardian / 麒麟守卫 设计包

## 目标体量
- Entity ID: `mythic_age:qilin_guardian`
- 碰撞盒目标：`1.5w x 2.0h`
- 视觉高度目标：约 `2.25` 格
- 比例说明：祥瑞兽，鹿角、鬃毛、鳞甲和蹄足必须明确。

## 轮廓语言
- 正面：鹿角、鬃毛和宽胸。
- 侧面：马鹿身、鳞片和尾鬃。
- 45 度：背鬃和尾巴可读。

## 概念设计图
- 文件：`docs/美术资源/chinese_creatures/concepts/qilin_guardian_concept.png`
- 用途：这是建模前的造型合同；Blockbench 和 MC 截图只验证是否实现该设计，不负责重新发明造型。
- 三视角：正面、侧面、45 度必须同时能识别该生物。

## V2 造型冻结
- 触发依据：用户实测反馈指出当前模型存在体量、精细度、纹理空白和神话特征不可读问题；测试只验证设计落地，不再承担造型试错。
- 正面必须读到麒麟鹿角、明亮眼睛、鬃毛和宽胸，不得像普通马或鹿。
- 头部主体必须匹配马鹿式四足身体比例；侧面必须有背部鬃毛、鳞甲板、蹄足和尾鬃分区。
- 祥瑞朱红、金色鹿角、玉色眼睛和红金鳞甲必须在 UV 中各自落位。

## 核心识别特征
- 麒麟鹿角
- 鳞甲
- 蹄足
- 祥瑞配色

## 部件清单
- `head`
- `snout`
- `left_eye`
- `right_eye`
- `left_antler`
- `right_antler`
- `left_antler_tip`
- `right_antler_tip`
- `mane`
- `scaled_body`
- `scale_plate`
- `front_left_leg`
- `front_right_leg`
- `back_left_leg`
- `back_right_leg`
- `hooves`
- `tail`
- `tail_plume`
- `mouth_line`
- `left_antler_branch_a`
- `left_antler_branch_b`
- `right_antler_branch_a`
- `right_antler_branch_b`
- `scale_plate_front`
- `scale_plate_mid`
- `scale_plate_back`
- `left_side_scale_front`
- `right_side_scale_front`
- `left_side_scale_mid`
- `right_side_scale_mid`
- `left_side_scale_back`
- `right_side_scale_back`
- `mane_front`
- `mane_mid`
- `mane_back`
- `front_left_hoof`
- `front_right_hoof`
- `back_left_hoof`
- `back_right_hoof`
- `chest_fur`
- `tail_plume_upper`
- `tail_plume_lower`

## Blockbench 层级
- `root/head`
- `root/body`
- `root/legs`
- `root/tail`

## 建模硬性要求
- antlers 独立
- mane 独立
- 四足和躯干分离

## UV 计划
- head_front: bright eyes
- antlers: gold
- body_side: red-gold scales

## 纹理细节
- 主色：`#b84a36`
- 阴影：`#3a1812`
- 强调色：`#d9b84a`
- 细节规则：朱红鳞甲、金色角、深色鬃毛和玉色眼睛。

## 动画约束
- 优雅行走；攻击时前蹄踏地；守护技能时头部稳定。

## 返工触发条件
- 像普通马
- 没有角
- 没有鳞片

## 验收口径
- 先验收概念设计图，再进入 Blockbench 建模。
- 必须提供正面、侧面、45 度三视图，且三视图来自已挂载贴图后的模型预览。
- 必须提供 UV 模板和贴图贴合报告，关键 UV 区域不得透明、空白或越界。
- 如三视图或贴合报告失败，优先修 .bbmodel、UV 或贴图。
