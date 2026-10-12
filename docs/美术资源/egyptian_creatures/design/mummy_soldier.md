---
creature_id: mummy_soldier
creature_name: Mummy Soldier / 木乃伊士兵
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

# Mummy Soldier / 木乃伊士兵 设计包

## 目标体量
- Entity ID: `mythic_age:mummy_soldier`
- 碰撞盒目标：`0.8w x 2.1h`
- 视觉高度目标：约 `2.15` 格
- 比例说明：玩家体型敌人，但绷带、空眼和短矛必须可读。

## 轮廓语言
- 正面：缠布头脸、空眼、短矛。
- 侧面：前倾慢行，武器侧影明显。
- 45 度：绷带层次和披布可见。

## 概念设计图
- 文件：`docs/美术资源/egyptian_creatures/concepts/mummy_soldier_concept.png`
- 用途：这是建模前的造型合同；Blockbench 和 MC 截图只验证是否实现该设计，不负责重新发明造型。
- 三视角：正面、侧面、45 度必须同时能识别该生物。

## V2 造型冻结
- 触发依据：用户实测反馈指出当前模型存在体量、精细度、纹理空白和神话特征不可读问题；测试只验证设计落地，不再承担造型试错。
- 正面必须读到深色空眼、层层绷带和短矛士兵身份，不得像普通僵尸。
- 头、胸、手臂、腿部的绷带必须有多条独立带状区，不能只靠单一砂色贴图。
- 短矛必须作为独立武器轮廓，攻击方向正侧面都要可读。

## 核心识别特征
- 绷带层次
- 黑眼洞
- 短矛士兵身份

## 部件清单
- `wrapped_head`
- `head_bandage_upper`
- `head_bandage_lower`
- `left_eye`
- `right_eye`
- `torso`
- `bandage_chest`
- `bandage_mid`
- `bandage_low`
- `left_arm`
- `right_arm`
- `arm_bandages`
- `spear_shaft`
- `spear_tip`
- `left_leg`
- `right_leg`
- `leg_bandages`
- `head_bandage_mid`
- `head_bandage_diagonal`
- `mouth_shadow`
- `left_cheek_wrap`
- `right_cheek_wrap`
- `waist_wrap`
- `left_arm_bandage_upper`
- `right_arm_bandage_upper`
- `left_arm_bandage_lower`
- `right_arm_bandage_lower`
- `left_leg_bandage`
- `right_leg_bandage`
- `spear_tassel`

## Blockbench 层级
- `root/head`
- `root/body`
- `root/arms`
- `root/weapon`
- `root/legs`

## 建模硬性要求
- bandages 必须多区块
- spear 独立
- 眼洞高对比

## UV 计划
- head_front: dark eye holes
- bandages: diagonal linen strips
- spear: bronze tip

## 纹理细节
- 主色：`#b8a77e`
- 阴影：`#4a3f2b`
- 强调色：`#d7caa1`
- 细节规则：砂色绷带、深色眼洞、青铜短矛和褐色腐朽阴影。

## 动画约束
- 待机摇晃；行走拖步；攻击时短矛前刺。

## 返工触发条件
- 像普通僵尸
- 没有绷带
- 没有武器

## 验收口径
- 先验收概念设计图，再进入 Blockbench 建模。
- 必须提供正面、侧面、45 度三视图，且三视图来自已挂载贴图后的模型预览。
- 必须提供 UV 模板和贴图贴合报告，关键 UV 区域不得透明、空白或越界。
- 如三视图或贴合报告失败，优先修 .bbmodel、UV 或贴图。
