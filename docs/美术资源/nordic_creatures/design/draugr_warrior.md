---
creature_id: draugr_warrior
creature_name: Draugr Warrior / 亡灵战士
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

# Draugr Warrior / 亡灵战士 设计包

## 目标体量
- Entity ID: `mythic_age:draugr_warrior`
- 碰撞盒目标：`0.85w x 2.2h`
- 视觉高度目标：约 `2.25` 格
- 比例说明：玩家体型亡灵战士，但必须有北欧头盔、圆盾和斧。

## 轮廓语言
- 正面：破损圆盾、斧头和角盔清楚。
- 侧面：前倾拖步，斧刃和盾厚度可读。
- 45 度：破披风和头盔后沿可见。

## 概念设计图
- 文件：`docs/美术资源/nordic_creatures/concepts/draugr_warrior_concept.png`
- 用途：这是建模前的造型合同；Blockbench 和 MC 截图只验证是否实现该设计，不负责重新发明造型。
- 三视角：正面、侧面、45 度必须同时能识别该生物。

## V2 造型冻结
- 触发依据：用户实测反馈指出当前模型存在体量、精细度、纹理空白和神话特征不可读问题；测试只验证设计落地，不再承担造型试错。
- 正面必须读到北欧亡灵、角盔或铁盔、破圆盾和战斧，不得像普通僵尸。
- 空眼、破披风、圆盾裂纹/中心扣和斧刃必须分区可读。
- 拖步姿态和盾斧双侧轮廓要在侧面与 45 度视角成立。

## 核心识别特征
- 北欧亡灵
- 圆盾
- 斧头
- 角盔/铁盔

## 部件清单
- `helmet`
- `helmet_horns`
- `empty_eyes`
- `mouth`
- `torso`
- `rib_marks`
- `belt`
- `round_shield`
- `shield_cracks`
- `axe`
- `axe_blade`
- `arms`
- `bracers`
- `legs`
- `boots`
- `ragged_cape`

## Blockbench 层级
- `root/head`
- `root/body`
- `root/arms`
- `root/weapons`
- `root/legs`

## 建模硬性要求
- shield 和 axe 独立且大轮廓可读
- 眼洞、口线和肋骨痕高对比
- 披风破片与身体分离

## UV 计划
- head_front: hollow eyes
- shield: cracked circle
- axe: iron blade
- cape: torn dark cloth

## 纹理细节
- 主色：`#4f5a5b`
- 阴影：`#171f22`
- 强调色：`#9aa7a8`
- 细节规则：灰尸皮、铁盔、破圆盾、铁斧和暗色披风；眼窝必须黑而亮边。

## 动画约束
- 拖步行走；攻击时斧头横劈；防御时盾臂保持在身体前侧。

## 返工触发条件
- 像普通僵尸
- 没有盾或斧
- 没有北欧头盔

## 验收口径
- 先验收概念设计图，再进入 Blockbench 建模。
- 必须提供正面、侧面、45 度三视图，且三视图来自已挂载贴图后的模型预览。
- 必须提供 UV 模板和贴图贴合报告，关键 UV 区域不得透明、空白或越界。
- 如三视图或贴合报告失败，优先修 .bbmodel、UV 或贴图。
