---
creature_id: frost_giant
creature_name: Frost Giant / 霜巨人
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

# Frost Giant / 霜巨人 设计包

## 目标体量
- Entity ID: `mythic_age:frost_giant`
- 碰撞盒目标：`1.8w x 3.9h`
- 视觉高度目标：约 `4.05` 格
- 比例说明：高大寒霜巨人，比玩家高约两倍，宽肩、冰须和霜石棒必须清楚。

## 轮廓语言
- 正面：比例合理的巨人头脸、宽肩、冰须、冰蓝胸纹和巨大霜石棒一眼可读。
- 侧面：厚胸、前突鼻梁、长臂和石棒形成压迫侧影。
- 45 度：背部冰刺、宽肩和垂落兽皮披肩清楚分层。

## 概念设计图
- 文件：`docs/美术资源/nordic_creatures/concepts/frost_giant_concept.png`
- 用途：这是建模前的造型合同；Blockbench 和 MC 截图只验证是否实现该设计，不负责重新发明造型。
- 三视角：正面、侧面、45 度必须同时能识别该生物。

## V2 造型冻结
- 触发依据：用户实测反馈指出当前模型存在体量、精细度、纹理空白和神话特征不可读问题；测试只验证设计落地，不再承担造型试错。
- 正面第一识别必须是巨人体量、冰须、冰刺和右手巨大霜石棒，不得像普通大人形。
- 头脸必须有眉骨、发光眼和冰须分区；胸前冰蓝符文和兽皮披肩要与身体分层。
- 视觉高度约 4 格；长臂、宽肩、背刺和霜石棒共同支撑霜巨人的压迫感。

## 核心识别特征
- 巨人体量
- 冰须和冰刺
- 巨大霜石棒
- 蓝白寒霜配色

## 部件清单
- `giant_head`
- `brow`
- `eyes`
- `ice_beard`
- `torso`
- `fur_pelt`
- `arms`
- `frost_club`
- `legs`
- `ice_spikes`
- `nose_bridge`
- `mouth_line`
- `left_brow_ice`
- `right_brow_ice`
- `left_beard_strand`
- `center_beard_strand`
- `right_beard_strand`
- `left_shoulder_ice`
- `right_shoulder_ice`
- `left_bracer_ice`
- `right_bracer_ice`
- `club_crack_a`
- `club_crack_b`
- `club_ice_cap`
- `left_knee_ice`
- `right_knee_ice`
- `left_boot_spikes`
- `right_boot_spikes`
- `back_left_spike`
- `back_right_spike`

## Blockbench 层级
- `root/head`
- `root/body`
- `root/arms`
- `root/weapon`
- `root/legs`
- `root/spikes`

## 建模硬性要求
- frost_club 必须独立 weapon group
- ice_beard 必须有垂落分段
- 眼睛和眉骨必须高对比

## UV 计划
- head_front: glowing eyes and brow
- beard: white ice strips
- torso: blue runes
- club: frosted stone

## 纹理细节
- 主色：`#8fb8d8`
- 阴影：`#26394a`
- 强调色：`#e6f6ff`
- 细节规则：蓝灰皮肤、白色冰须、胸口浅蓝符文、兽皮深棕边缘、霜石棒带冰裂纹。

## 动画约束
- 待机呼吸重；行走长臂摆动；攻击时霜石棒下砸；特殊震地时身体前压、头部稳定。

## 返工触发条件
- 像普通大僵尸
- 没有冰须或冰刺
- 武器不可读

## 验收口径
- 先验收概念设计图，再进入 Blockbench 建模。
- 必须提供正面、侧面、45 度三视图，且三视图来自已挂载贴图后的模型预览。
- 必须提供 UV 模板和贴图贴合报告，关键 UV 区域不得透明、空白或越界。
- 如三视图或贴合报告失败，优先修 .bbmodel、UV 或贴图。
