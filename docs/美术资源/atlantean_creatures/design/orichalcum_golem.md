---
creature_id: orichalcum_golem
creature_name: Orichalcum Golem / 山铜魔像
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

# Orichalcum Golem / 山铜魔像 设计包

## 目标体量
- Entity ID: `mythic_age:orichalcum_golem`
- 碰撞盒目标：`1.7w x 3.2h`
- 视觉高度目标：约 `3.45` 格
- 比例说明：古代亚特兰蒂斯构装体，山铜装甲、发光核心、重拳和肩部晶柱必须突出。

## 轮廓语言
- 正面：宽肩、发光胸核、巨拳。
- 侧面：厚装甲身躯和背部晶柱。
- 45 度：背晶柱和山铜铆钉可读。

## 概念设计图
- 文件：`docs/美术资源/atlantean_creatures/concepts/orichalcum_golem_concept.png`
- 用途：这是建模前的造型合同；Blockbench 和 MC 截图只验证是否实现该设计，不负责重新发明造型。
- 三视角：正面、侧面、45 度必须同时能识别该生物。

## V2 造型冻结
- 触发依据：用户实测反馈指出当前模型存在体量、精细度、纹理空白和神话特征不可读问题；测试只验证设计落地，不再承担造型试错。
- 正面必须读到山铜构装体、宽肩、发光胸核、单眼缝和巨大重拳，不得像普通铁傀儡。
- 背面和侧面必须有肩背晶柱、厚装甲板和铆钉/装甲缝分层。
- 青色能量核心、暗金山铜装甲和水晶晶柱必须在 UV 上高对比分区。

## 核心识别特征
- 山铜装甲
- 发光胸核
- 巨拳
- 肩背晶柱

## 部件清单
- `helmet_head`
- `core_eye`
- `chest_core`
- `core_frame`
- `shoulder_plates`
- `back_crystal_array`
- `heavy_arms`
- `forearm_plates`
- `giant_fists`
- `orichalcum_legs`
- `knee_plates`
- `heavy_feet`
- `upper_torso_rivet_left`
- `upper_torso_rivet_right`
- `lower_rune_glyph`
- `core_vertical_glow`
- `left_shoulder_rivet`
- `right_shoulder_rivet`
- `left_fist_knuckle_a`
- `left_fist_knuckle_b`
- `right_fist_knuckle_a`
- `right_fist_knuckle_b`
- `left_foot_toe_plate`
- `right_foot_toe_plate`

## Blockbench 层级
- `root/head`
- `root/torso`
- `root/arms`
- `root/legs`
- `root/crystals`

## 建模硬性要求
- chest_core 独立高对比并带框
- arms 和 fists 远大于普通人形
- shoulder_plates 与 back_crystal_array 独立

## UV 计划
- chest: cyan power core
- arms: bronze plates
- head: single slit eye
- crystals: cyan facets

## 纹理细节
- 主色：`#9a6c38`
- 阴影：`#332116`
- 强调色：`#36d6c9`
- 细节规则：暗金山铜、青色能量核心、深色装甲缝和铆钉。

## 动画约束
- 沉重待机；行走时手臂低摆；攻击时巨拳蓄力下砸。

## 返工触发条件
- 像普通铁傀儡
- 没有核心
- 没有亚特兰蒂斯水晶特征

## 验收口径
- 先验收概念设计图，再进入 Blockbench 建模。
- 必须提供正面、侧面、45 度三视图，且三视图来自已挂载贴图后的模型预览。
- 必须提供 UV 模板和贴图贴合报告，关键 UV 区域不得透明、空白或越界。
- 如三视图或贴合报告失败，优先修 .bbmodel、UV 或贴图。
