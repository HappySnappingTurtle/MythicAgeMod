---
creature_id: jiangshi
creature_name: Jiangshi / 僵尸
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

# Jiangshi / 僵尸 设计包

## 目标体量
- Entity ID: `mythic_age:jiangshi`
- 碰撞盒目标：`0.8w x 2.1h`
- 视觉高度目标：约 `2.15` 格
- 比例说明：中式僵尸，官帽、符箓、伸直手臂和青白脸必须清楚。

## 轮廓语言
- 正面：官帽、额头符、双臂前伸。
- 侧面：僵硬跳步和长袖轮廓。
- 45 度：长袍和帽尾可见。

## 概念设计图
- 文件：`docs/美术资源/chinese_creatures/concepts/jiangshi_concept.png`
- 用途：这是建模前的造型合同；Blockbench 和 MC 截图只验证是否实现该设计，不负责重新发明造型。
- 三视角：正面、侧面、45 度必须同时能识别该生物。

## V2 造型冻结
- 触发依据：用户实测反馈指出当前模型存在体量、精细度、纹理空白和神话特征不可读问题；测试只验证设计落地，不再承担造型试错。
- 正面必须读到中式官帽、额头黄符、青白脸和双臂前伸，不得像普通僵尸。
- 身体必须有深青官服、长袍前片和袖口，侧面要体现僵硬跳步和前伸手臂。
- 符箓必须是独立高对比面，红色符纹不能被脸部或帽子纹理吞掉。

## 核心识别特征
- 官帽
- 符箓
- 伸直手臂
- 青白脸

## 部件清单
- `head`
- `pale_face`
- `eyes`
- `mouth`
- `official_hat`
- `hat_wings`
- `hat_tail`
- `talisman_paper`
- `talisman_red_marks`
- `robe_body`
- `robe_front_panel`
- `robe_collar`
- `sleeves`
- `cuffs`
- `stiff_arms`
- `legs`
- `black_boots`

## Blockbench 层级
- `root/head`
- `root/body`
- `root/arms`
- `root/legs`

## 建模硬性要求
- talisman 必须独立且带红色符纹
- arms 前伸并有袖口
- robe 要有领口、前片和下摆分层

## UV 计划
- face_front: pale blue-white face with dark eyes and mouth
- talisman: yellow paper on forehead with three red symbol strokes
- hat: black official hat, side wings and rear tail
- robe: dark teal cloth, collar, front panel and cuffs

## 纹理细节
- 主色：`#46605f`
- 阴影：`#162323`
- 强调色：`#d6b64a`
- 细节规则：青白脸、深色眼口、黄符红纹、深青官服、黑官帽、长袖袖口和黑靴必须分区清楚。

## 动画约束
- 僵硬跳步；攻击时双臂前推；受击时头部轻晃。

## 返工触发条件
- 像普通僵尸
- 没有符
- 没有官帽

## 验收口径
- 先验收概念设计图，再进入 Blockbench 建模。
- 必须提供正面、侧面、45 度三视图，且三视图来自已挂载贴图后的模型预览。
- 必须提供 UV 模板和贴图贴合报告，关键 UV 区域不得透明、空白或越界。
- 如三视图或贴合报告失败，优先修 .bbmodel、UV 或贴图。
