---
creature_id: tidal_guardian
creature_name: Tidal Guardian / 潮汐守卫
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

# Tidal Guardian / 潮汐守卫 设计包

## 目标体量
- Entity ID: `mythic_age:tidal_guardian`
- 碰撞盒目标：`2.0w x 1.4h`
- 视觉高度目标：约 `1.65` 格
- 比例说明：龟甲与蟹钳混合的神殿守卫，厚壳、双钳、四足和水晶甲片必须可读。

## 轮廓语言
- 正面：宽壳和巨大双钳。
- 侧面：低矮甲壳、短足和后尾鳍。
- 45 度：壳上水晶甲片。

## 概念设计图
- 文件：`docs/美术资源/atlantean_creatures/concepts/tidal_guardian_concept.png`
- 用途：这是建模前的造型合同；Blockbench 和 MC 截图只验证是否实现该设计，不负责重新发明造型。
- 三视角：正面、侧面、45 度必须同时能识别该生物。

## V2 造型冻结
- 触发依据：用户实测反馈指出当前模型存在体量、精细度、纹理空白和神话特征不可读问题；测试只验证设计落地，不再承担造型试错。
- 正面必须读到低矮宽壳、巨大双蟹钳、小亮眼和厚甲，不得像普通乌龟。
- 侧面必须体现短足横移体态、龟甲壳脊、水晶甲片和尾鳍。
- 双钳必须大于普通前肢，水晶壳片必须独立高亮，不能只靠纹理暗示。

## 核心识别特征
- 厚龟甲
- 蟹钳
- 水晶甲片
- 低矮守卫体量

## 部件清单
- `shell`
- `head`
- `left_eye`
- `right_eye`
- `beak_mouth`
- `lower_mandible`
- `left_claw`
- `right_claw`
- `front_left_leg`
- `front_right_leg`
- `back_left_leg`
- `back_right_leg`
- `crystal_shell_plates`
- `crystal_plate_1`
- `crystal_plate_2`
- `tail_fin`
- `shell_plate_front`
- `shell_plate_left`
- `shell_plate_right`
- `shell_plate_back`
- `left_claw_upper_tip`
- `left_claw_lower_tip`
- `right_claw_upper_tip`
- `right_claw_lower_tip`
- `front_left_foot_claws`
- `front_right_foot_claws`
- `back_left_foot_claws`
- `back_right_foot_claws`
- `crystal_plate_3`
- `crystal_plate_4`
- `tail_fin_tip`

## Blockbench 层级
- `root/shell`
- `root/head`
- `root/claws`
- `root/legs`
- `root/crystals`

## 建模硬性要求
- claws 必须大
- shell 宽过身体
- crystal plates 独立

## UV 计划
- shell: dark turtle plates
- claws: cyan edge
- eyes: bright dots
- crystals: glowing shell ridges

## 纹理细节
- 主色：`#2f5d62`
- 阴影：`#102225`
- 强调色：`#48d1cc`
- 细节规则：深青龟甲、青色水晶壳脊、钳尖高光和小而亮的眼睛。

## 动画约束
- 缓慢横移；攻击时双钳夹击；防御时甲壳稳定。

## 返工触发条件
- 像普通乌龟
- 没有双钳
- 水晶壳不明显

## 验收口径
- 先验收概念设计图，再进入 Blockbench 建模。
- 必须提供正面、侧面、45 度三视图，且三视图来自已挂载贴图后的模型预览。
- 必须提供 UV 模板和贴图贴合报告，关键 UV 区域不得透明、空白或越界。
- 如三视图或贴合报告失败，优先修 .bbmodel、UV 或贴图。
