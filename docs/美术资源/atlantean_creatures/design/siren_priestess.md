---
creature_id: siren_priestess
creature_name: Siren Priestess / 海妖祭司
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

# Siren Priestess / 海妖祭司 设计包

## 目标体量
- Entity ID: `mythic_age:siren_priestess`
- 碰撞盒目标：`0.9w x 2.2h`
- 视觉高度目标：约 `2.35` 格
- 比例说明：亚特兰蒂斯海妖施法者，人形上身、鱼尾、鳍冠和水晶权杖必须清楚。

## 轮廓语言
- 正面：人形头冠、双臂和权杖。
- 侧面：鱼尾下身和背鳍。
- 45 度：鳍冠和长尾可读。

## 概念设计图
- 文件：`docs/美术资源/atlantean_creatures/concepts/siren_priestess_concept.png`
- 用途：这是建模前的造型合同；Blockbench 和 MC 截图只验证是否实现该设计，不负责重新发明造型。
- 三视角：正面、侧面、45 度必须同时能识别该生物。

## V2 造型冻结
- 触发依据：用户实测反馈指出当前模型存在体量、精细度、纹理空白和神话特征不可读问题；测试只验证设计落地，不再承担造型试错。
- 正面必须读到海妖祭司的人形上身、发光眼、鳍冠、双臂和水晶权杖；头部保持人形比例，不能靠放大头部表现细节。
- 下身必须是鱼尾结构，包含尾基、尾尖和尾鳍，不能退化成人腿或裙摆。
- 权杖晶体、鳍冠、青蓝鳞片和海浪纹长袍必须分区可读，头部宽度不得超过上身宽度的 82%。

## 核心识别特征
- 海妖人形上身
- 鱼尾
- 鳍冠
- 水晶权杖

## 部件清单
- `face`
- `glowing_eyes`
- `mouth`
- `fin_crown`
- `crown_spire`
- `hair_fins`
- `torso`
- `wave_chest_ornament`
- `arms`
- `crystal_staff`
- `fish_tail`
- `tail_scales`
- `wide_tail_fin`
- `left_ear_fin`
- `right_ear_fin`
- `left_wrist_fin`
- `right_wrist_fin`
- `staff_rune_band`
- `tail_scale_band_3`
- `tail_fin_left_edge`
- `tail_fin_right_edge`

## Blockbench 层级
- `root/head`
- `root/torso`
- `root/arms`
- `root/tail`
- `root/staff`

## 建模硬性要求
- fish_tail 不能做成人腿且必须有鳞片条
- staff 独立并带顶部晶体
- face 必须有眼睛、口鼻和鳍冠侧翼

## UV 计划
- face: pale eyes
- tail: teal scales
- staff: cyan crystal
- crown: pale fin

## 纹理细节
- 主色：`#2c8f9e`
- 阴影：`#0f303e`
- 强调色：`#d7f7ff`
- 细节规则：青蓝鱼鳞、白蓝眼睛、半透明鳍冠、水晶权杖和海浪纹长袍。

## 动画约束
- 悬浮漂游；施法时权杖抬起；尾巴缓慢摆动。

## 返工触发条件
- 像普通女巫
- 没有鱼尾
- 没有权杖

## 验收口径
- 先验收概念设计图，再进入 Blockbench 建模。
- 必须提供正面、侧面、45 度三视图，且三视图来自已挂载贴图后的模型预览。
- 必须提供 UV 模板和贴图贴合报告，关键 UV 区域不得透明、空白或越界。
- 如三视图或贴合报告失败，优先修 .bbmodel、UV 或贴图。
