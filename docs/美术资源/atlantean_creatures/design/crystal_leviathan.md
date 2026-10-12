---
creature_id: crystal_leviathan
creature_name: Crystal Leviathan / 水晶利维坦
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

# Crystal Leviathan / 水晶利维坦 设计包

## 目标体量
- Entity ID: `mythic_age:crystal_leviathan`
- 碰撞盒目标：`2.4w x 2.4h`
- 视觉高度目标：约 `2.65` 格
- 比例说明：亚特兰蒂斯海渊巨兽，长身、水晶背脊、鳍翼和棱镜眼必须明确。

## 轮廓语言
- 正面：宽阔龙首、棱镜双眼、侧鳍和下颚须鳍可读。
- 侧面：长蛇身、分段躯体、水晶背刺和尾鳍形成海兽侧影。
- 45 度：背部晶簇、尾鳍和发光鳞带一眼可见。

## 概念设计图
- 文件：`docs/美术资源/atlantean_creatures/concepts/crystal_leviathan_concept.png`
- 用途：这是建模前的造型合同；Blockbench 和 MC 截图只验证是否实现该设计，不负责重新发明造型。
- 三视角：正面、侧面、45 度必须同时能识别该生物。

## V2 造型冻结
- 触发依据：用户实测反馈指出当前模型存在体量、精细度、纹理空白和神话特征不可读问题；测试只验证设计落地，不再承担造型试错。
- 正面必须读到比例适中的海兽头、棱镜眼、下颚和须鳍，不得像普通蛇或青龙换色。
- 侧面必须由分段长身、水晶背刺、侧鳍和尾扇组成海渊巨兽轮廓，体量主要来自长身分段，头部保持从属于身体的比例。
- 水晶背刺和发光鳞带必须是独立高亮结构，头部、身体和尾扇不能糊成单条管状体，头部宽度不得超过身体主段宽度的 92%。

## 核心识别特征
- 长身海兽
- 水晶背刺
- 棱镜眼
- 鳍翼和尾扇

## 部件清单
- `crystal_head`
- `snout`
- `prism_eyes`
- `lower_jaw`
- `mandible_fins`
- `left_mandible_fin`
- `right_mandible_fin`
- `segmented_body`
- `body_1`
- `body_2`
- `body_3`
- `body_4`
- `glow_belly`
- `dorsal_crystals`
- `dorsal_crystal_1`
- `dorsal_crystal_2`
- `dorsal_crystal_3`
- `side_fins`
- `left_side_fin`
- `right_side_fin`
- `tail`
- `tail_fan`
- `head_crown_crystal`
- `mouth_line`
- `left_gill_slits`
- `right_gill_slits`
- `body_1_glow_scale`
- `body_2_glow_scale`
- `body_3_glow_scale`
- `body_4_glow_scale`
- `dorsal_crystal_4`
- `left_side_fin_rib`
- `right_side_fin_rib`
- `tail_fan_left_rib`
- `tail_fan_right_rib`
- `tail_tip_crystal`

## Blockbench 层级
- `root/head`
- `root/body_segments`
- `root/fins`
- `root/crystals`
- `root/tail`

## 建模硬性要求
- body_segments 必须分段
- dorsal_crystals 独立成簇
- head 与 tail_fan 轮廓不能糊成一条蛇

## UV 计划
- head_front: cyan prism eyes and jaw line
- body_segments: dark teal scale bands
- dorsal_crystals: bright cyan facets
- tail_fan: wave pattern

## 纹理细节
- 主色：`#1b6f7a`
- 阴影：`#092b35`
- 强调色：`#72e6f2`
- 细节规则：深海青蓝主色、亮青水晶棱面、浅蓝发光鳞带、棱镜眼和下颚暗线；不得出现空白透明面。

## 动画约束
- 待机时悬浮游弋；移动时分段身体横向波动；攻击时头部前探并触发棱镜潮汐冲击。

## 返工触发条件
- 像普通蛇
- 没有水晶背刺
- 没有清楚的头和眼睛
- 尾鳍不明显

## 验收口径
- 先验收概念设计图，再进入 Blockbench 建模。
- 必须提供正面、侧面、45 度三视图，且三视图来自已挂载贴图后的模型预览。
- 必须提供 UV 模板和贴图贴合报告，关键 UV 区域不得透明、空白或越界。
- 如三视图或贴合报告失败，优先修 .bbmodel、UV 或贴图。
