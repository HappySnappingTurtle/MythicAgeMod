---
creature_id: fenrir_wolf
creature_name: Fenrir Wolf / 芬里尔巨狼
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

# Fenrir Wolf / 芬里尔巨狼 设计包

## 目标体量
- Entity ID: `mythic_age:fenrir_wolf`
- 碰撞盒目标：`2.1w x 1.8h`
- 视觉高度目标：约 `1.95` 格
- 比例说明：巨型黑狼，锁链、红眼和强壮口鼻必须读出；狼头要与长身厚颈匹配，不能做成卡通大头。

## 轮廓语言
- 正面：比例合理的狼头、红眼、锁链项圈。
- 侧面：长背、厚颈、蓬尾、低伏捕食姿态。
- 45 度：背毛和尾巴清楚。

## 概念设计图
- 文件：`docs/美术资源/nordic_creatures/concepts/fenrir_wolf_concept.png`
- 用途：这是建模前的造型合同；Blockbench 和 MC 截图只验证是否实现该设计，不负责重新发明造型。
- 三视角：正面、侧面、45 度必须同时能识别该生物。

## V2 造型冻结
- 触发依据：用户实测反馈指出当前模型存在体量、精细度、纹理空白和神话特征不可读问题；测试只验证设计落地，不再承担造型试错。
- 正面和侧面必须读到巨狼体量、红眼、巨大上下颚和锁链项圈，不得像普通狼。
- 嘴部上颚、下颚和獠牙必须分离，锁链项圈必须独立且高对比。
- 低伏长身、背毛和蓬尾要形成芬里尔捕食轮廓。

## 核心识别特征
- 巨狼
- 红眼
- 锁链
- 强壮下颚

## 部件清单
- `wolf_body`
- `back_fur`
- `wolf_head`
- `left_eye`
- `right_eye`
- `upper_jaw`
- `lower_jaw`
- `fangs`
- `chain_collar`
- `front_left_leg`
- `front_right_leg`
- `back_left_leg`
- `back_right_leg`
- `tail`
- `tail_tip`
- `shoulder_hump`
- `left_ear`
- `right_ear`
- `left_brow`
- `right_brow`
- `left_lower_fang`
- `right_lower_fang`
- `chain_link_left`
- `chain_link_right`
- `chain_link_center`
- `front_left_paw`
- `front_right_paw`
- `back_left_paw`
- `back_right_paw`
- `front_left_claws`
- `front_right_claws`
- `back_left_claws`
- `back_right_claws`
- `back_fur_a`
- `back_fur_b`
- `back_fur_c`

## Blockbench 层级
- `root/body`
- `root/head`
- `root/legs`
- `root/tail`
- `root/chains`

## 建模硬性要求
- chain_collar 独立
- jaws 明确
- tail 蓬大
- wolf_head 宽度不得接近躯干宽度，细节靠眼、耳、上下颚、獠牙和锁链

## UV 计划
- head_front: red eyes and jaws
- chain: iron links
- body_side: black fur highlights

## 纹理细节
- 主色：`#1d2028`
- 阴影：`#08090d`
- 强调色：`#b02a2a`
- 细节规则：黑灰毛、红眼、铁链和白牙；口鼻前突但宽度受身体比例约束。

## 动画约束
- 潜行式行走；扑咬时头前冲；尾部低摆。

## 返工触发条件
- 像普通狼
- 没有锁链
- 眼睛不可读

## 验收口径
- 先验收概念设计图，再进入 Blockbench 建模。
- 必须提供正面、侧面、45 度三视图，且三视图来自已挂载贴图后的模型预览。
- 必须提供 UV 模板和贴图贴合报告，关键 UV 区域不得透明、空白或越界。
- 如三视图或贴合报告失败，优先修 .bbmodel、UV 或贴图。
