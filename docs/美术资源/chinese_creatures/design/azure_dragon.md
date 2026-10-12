---
creature_id: azure_dragon
creature_name: Azure Dragon / 青龙
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

# Azure Dragon / 青龙 设计包

## 目标体量
- Entity ID: `mythic_age:azure_dragon`
- 碰撞盒目标：`1.6w x 2.2h`
- 视觉高度目标：约 `2.45` 格
- 比例说明：长身飞行东方龙，龙角、龙须、鳞片、爪和云纹必须清楚。

## 轮廓语言
- 正面：龙头、双角、龙须和前爪一眼可见。
- 侧面：长蛇身、分段身躯、尾鳍和云气侧影明显。
- 45 度：背鳍、尾部和鳞片分段可读。

## 概念设计图
- 文件：`docs/美术资源/chinese_creatures/concepts/azure_dragon_concept.png`
- 用途：这是建模前的造型合同；Blockbench 和 MC 截图只验证是否实现该设计，不负责重新发明造型。
- 三视角：正面、侧面、45 度必须同时能识别该生物。

## V2 造型冻结
- 触发依据：用户实测反馈指出当前模型存在体量、精细度、纹理空白和神话特征不可读问题；测试只验证设计落地，不再承担造型试错。
- 正面必须读到东方龙头、双角、龙须、金色眼睛和前爪，不得像普通蛇或海蛇。
- 侧面必须由至少 4 段长身、腹鳞、背鳍和尾鳍组成连续但分节的无翼飞行轮廓。
- 龙须、龙角、爪和头部必须分层可读，青绿色鳞片和玉色腹鳞不得混成单色长条。

## 核心识别特征
- 东方龙长身
- 双角和龙须
- 青绿色鳞片
- 无翼飞行

## 部件清单
- `dragon_head`
- `snout`
- `left_horn`
- `right_horn`
- `left_whisker`
- `right_whisker`
- `left_eye`
- `right_eye`
- `body_1`
- `body_2`
- `body_3`
- `body_4`
- `belly_scales`
- `left_front_claw`
- `right_front_claw`
- `left_back_claw`
- `right_back_claw`
- `tail`
- `tail_fin`
- `dorsal_fin`
- `mouth_line`
- `left_brow`
- `right_brow`
- `left_horn_tip`
- `right_horn_tip`
- `left_whisker_tip`
- `right_whisker_tip`
- `body_1_belly_scale`
- `body_2_belly_scale`
- `body_3_belly_scale`
- `body_4_belly_scale`
- `dorsal_fin_front`
- `dorsal_fin_mid`
- `dorsal_fin_back`
- `left_front_talon`
- `right_front_talon`
- `left_back_talon`
- `right_back_talon`
- `tail_tip`
- `tail_fin_left`
- `tail_fin_right`

## Blockbench 层级
- `root/head`
- `root/body_segments`
- `root/claws`
- `root/tail`
- `root/fins`

## 建模硬性要求
- body_segments 必须分段
- horns 和 whiskers 独立
- 爪与头部不能糊成一块

## UV 计划
- head_front: gold eyes and whiskers
- body_segments: teal scale bands
- horns: gold tips
- tail: jade fin

## 纹理细节
- 主色：`#2f8f8a`
- 阴影：`#123b42`
- 强调色：`#d8b65a`
- 细节规则：青绿色鳞片、金色龙角和眼睛、浅玉腹鳞、红金云纹点缀；脸部必须有眼、须、吻部。

## 动画约束
- 待机悬浮盘旋；飞行时身体分段波动；攻击时龙头前探喷吐龙息。

## 返工触发条件
- 像普通蛇
- 没有龙角/龙须
- 身体没有分段

## 验收口径
- 先验收概念设计图，再进入 Blockbench 建模。
- 必须提供正面、侧面、45 度三视图，且三视图来自已挂载贴图后的模型预览。
- 必须提供 UV 模板和贴图贴合报告，关键 UV 区域不得透明、空白或越界。
- 如三视图或贴合报告失败，优先修 .bbmodel、UV 或贴图。
