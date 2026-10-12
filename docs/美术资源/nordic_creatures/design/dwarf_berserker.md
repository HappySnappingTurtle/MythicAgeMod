---
creature_id: dwarf_berserker
creature_name: Dwarf Berserker / 矮人狂战士
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

# Dwarf Berserker / 矮人狂战士 设计包

## 目标体量
- Entity ID: `mythic_age:dwarf_berserker`
- 碰撞盒目标：`0.95w x 1.45h`
- 视觉高度目标：约 `1.55` 格
- 比例说明：矮而宽，胡须、圆盔和双斧必须突出。

## 轮廓语言
- 正面：大胡须、宽肩、双斧。
- 侧面：短腿厚躯、斧头外扩。
- 45 度：背包/披肩和圆盔可见。

## 概念设计图
- 文件：`docs/美术资源/nordic_creatures/concepts/dwarf_berserker_concept.png`
- 用途：这是建模前的造型合同；Blockbench 和 MC 截图只验证是否实现该设计，不负责重新发明造型。
- 三视角：正面、侧面、45 度必须同时能识别该生物。

## V2 造型冻结
- 触发依据：用户实测反馈指出当前模型存在体量、精细度、纹理空白和神话特征不可读问题；测试只验证设计落地，不再承担造型试错。
- 正面必须读到矮宽比例、大胡须、圆盔和双斧，不得像缩小玩家。
- 胡须至少包含主体和双辫，腰带、盔缘和斧刃必须高对比分区。
- 短腿宽身和外扩双斧要在侧面与 45 度视角保持可读。

## 核心识别特征
- 矮宽比例
- 大胡须
- 双斧
- 圆盔

## 部件清单
- `head`
- `eyes`
- `nose`
- `mouth`
- `round_helmet`
- `helmet_crown`
- `helmet_rims`
- `helmet_nose_guard`
- `beard_mass`
- `beard_center`
- `braids`
- `torso`
- `shoulder_armor`
- `belt`
- `belt_buckle`
- `cloak_pack`
- `arms`
- `bracers`
- `left_axe_handle`
- `left_axe_blade`
- `left_axe_hook`
- `right_axe_handle`
- `right_axe_blade`
- `right_axe_hook`
- `short_legs`
- `boots`

## Blockbench 层级
- `root/head`
- `root/body`
- `root/arms`
- `root/weapons`
- `root/legs`

## 建模硬性要求
- 胡须主体、中心束和双辫必须独立
- 双斧必须拆成斧柄、斧刃和斧钩
- 短腿、宽肩和大靴子共同支撑矮宽比例

## UV 计划
- head_front: dark eyes, nose and mouth under helmet brim
- beard: orange main beard, center lock and two braids
- helmet: brass dome, side rim and nose guard
- axes: dark handles with separate iron blades/hooks
- belt: dark leather strip and bright buckle

## 纹理细节
- 主色：`#7b4c2a`
- 阴影：`#26160d`
- 强调色：`#c5a15c`
- 细节规则：棕橙胡须、双辫、铜盔盔缘、皮甲、铁斧分刃和金色腰扣；脸部眼鼻口必须在胡须上方可读。

## 动画约束
- 短步快跑；攻击时双斧交替；狂暴时身体前倾。

## 返工触发条件
- 像缩小玩家
- 没有胡须
- 没有双斧

## 验收口径
- 先验收概念设计图，再进入 Blockbench 建模。
- 必须提供正面、侧面、45 度三视图，且三视图来自已挂载贴图后的模型预览。
- 必须提供 UV 模板和贴图贴合报告，关键 UV 区域不得透明、空白或越界。
- 如三视图或贴合报告失败，优先修 .bbmodel、UV 或贴图。
