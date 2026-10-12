---
creature_id: pegasus
creature_name: Pegasus / 天马
design_status: needs_user_review
design_sheet_status: v3_candidate_needs_user_review
design_approval_status: v3_approval_pending
design_correctness_verdict: current_design_rejected_v3_pending
design_detail_sheet_status: v3_proportion_lock_pending
design_detail_board_status: v3_detail_board_pending
modeling_allowed: false
design_package_status: generated_reference_needs_human_approval
approval_basis: human_design_review_required_before_model_uv_texture_animation_or_runtime_sync
current_v2_assets_status: rejected
blocking_reason: current Pegasus model/design is malformed and cannot be used as modeling basis
v3_blueprint: docs/美术资源/greek_creatures/design_blueprints/pegasus_v3_blueprint.md
---

# Pegasus / 天马 设计包

## 目标体量
- Entity ID: `mythic_age:pegasus`
- 碰撞盒目标：`1.7w x 2.1h`
- 视觉高度目标：约 `2.15` 格
- 比例说明：纯白大型飞行坐骑，应比普通马更神圣、更修长。

## 轮廓语言
- 正面：白马头和长颈必须先被看见，双翼从肩部展开，头不能被身体/翼遮没。
- 侧面：马体比例清晰，头颈前伸，翅膀不能像薄片乱贴，骑乘空间合理。
- 45 度：双翼、尾巴和后腿从背面清楚，白色层次不丢失。

## 当前设计状态
- 状态：`rejected_v2_reference_only`。
- 当前 V2/旧实现已被用户判定畸形，不能作为 `.bbmodel`、Java model、贴图、动作或预览的继续建模依据。
- 可建模依据必须改用 `docs/美术资源/greek_creatures/design_blueprints/pegasus_v3_blueprint.md`，并等待比例锁定表、细化设计板和设计正确性 gate 全部批准。

## 概念设计图
- 文件：`docs/美术资源/greek_creatures/concepts/pegasus_concept.png`
- 用途：这是建模前的造型合同；Blockbench 和 MC 截图只验证是否实现该设计，不负责重新发明造型。
- 三视角：正面、侧面、45 度必须同时能识别该生物。

## V2 造型冻结
- 触发依据：用户实测反馈指出当前模型存在体量、精细度、纹理空白和神话特征不可读问题；测试只验证设计落地，不再承担造型试错。
- 正面必须先读到比例正常的白马头、细长颈和深色眼点；头宽控制在马身主体宽度三分之一到二分之一之间，头高控制在身体高度约三分之二以内，横向翅膀不能压过头颈轮廓。
- 侧面头、耳、鼻孔、鼻梁、口线、鬃毛必须在翅膀前方清楚可读，背部骑乘空间不得遮住翼根。
- 纯白不是纯空白，所有大面必须有浅灰分面线，羽片至少四段分层。
- 飞行能力必须在模型和动画双重可读：翼肩、主翼、次翼、覆羽和独立羽片都要存在。
- 载人飞行能力必须保留背部骑乘空间，身体飞行时稳定，翼根不被玩家坐点遮挡。

## 核心识别特征
- 纯白天马，第一眼必须是白马头、长颈、双翼，而不是一团白色方块
- 头部参考原版马的细长比例，头宽约为马身宽度三分之一到二分之一之间，必须有左右耳、左右眼、鼻梁、鼻孔、口线和分段鬃毛，不能靠大头或被翅膀/身体遮挡
- 翅膀必须有翼肩、主翼、次翼、覆羽和多片独立羽毛，具备自身飞行和载人飞行的视觉可信度
- 背部保留骑乘空间，翅根位于肩部两侧
- 全身必须是纯白/浅灰分面，避免白色贴图渲染为空白块

## 部件清单
- `body`
- `chest`
- `saddle_space`
- `neck`
- `neck_front_plane`
- `head`
- `snout`
- `nose_bridge`
- `left_cheek_shadow`
- `right_cheek_shadow`
- `left_nostril`
- `right_nostril`
- `nose_mouth`
- `left_eye`
- `right_eye`
- `left_ear`
- `right_ear`
- `mane`
- `mane_crest`
- `left_wing_shoulder`
- `left_wing_primary`
- `left_wing_secondary`
- `left_wing_coverts`
- `left_feathers`
- `right_wing_shoulder`
- `right_wing_primary`
- `right_wing_secondary`
- `right_wing_coverts`
- `right_feathers`
- `tail_base`
- `tail_plume`
- `four_legs`
- `hooves`

## Blockbench 层级
- `root/body`
- `root/head`
- `root/head/mane`
- `root/wings`
- `root/wings/feathers`
- `root/tail`
- `root/legs`

## 建模硬性要求
- head、snout、ears、neck、mane 必须按马体比例清楚可读；头宽应保持在马身主体宽度约三分之一到二分之一，头高不得超过身体高度约三分之二，禁止通过异常放大头部来弥补细节不足
- left_eye/right_eye、left_ear/right_ear、left/right_nostril、nose_bridge、nose_mouth 必须独立或有明确 UV 区
- wing_primary、wing_secondary、wing_coverts 和 wing_feathers 必须分层，侧面能看出羽片
- 身体纯白但通过浅灰阴影分面，避免渲染成空白
- hasPassengers 骑乘稳定和 isFlying 翼拍动画必须在运行模型中继续存在

## UV 计划
- head_front: horse-sized head, dark left/right eyes, snout plane, nostrils, nose bridge and mouth line
- neck_side: long white neck with shallow gray front plane and raised mane
- body_side: pure white coat shadows and clearly reserved saddle/rider space
- wing_top: shoulder block, primary plane, secondary plane and gray coverts
- wing_feathers: at least five separated feather slabs per side with gray band lines
- tail: white plume with layered shadow strips

## 纹理细节
- 主色：`#f7f7f0`
- 阴影：`#c8cdd0`
- 强调色：`#ffffff`
- 细节规则：严格白/浅灰体系，不使用金色或蓝色装饰；左右眼为小型深色高对比点，鼻口用浅灰线区分。大面积白色必须有浅灰分面线；每片羽毛要有灰线和白色高光，骑乘空间只用轻微阴影标示而不变成鞍具。

## 动画约束
- 待机轻拍翅；飞行时双翼大幅拍动；骑乘时身体稳定，玩家坐点不遮挡翅根。

## 返工触发条件
- 看不出头部、头太大像玩具马、头宽超过马身一半，头颈融合成一整块白柱，或头被翼/身体吃掉
- 翅膀像薄片，没有羽毛结构
- 白色贴图缺少浅灰分面导致空白感
- 缺少可审计的左右眼/左右耳/鼻口或骑乘空间

## 验收口径
- 先验收概念设计图，再进入 Blockbench 建模。
- 必须提供正面、侧面、45 度三视图，且三视图来自已挂载贴图后的模型预览。
- 必须提供 UV 模板和贴图贴合报告，关键 UV 区域不得透明、空白或越界。
- 如三视图或贴合报告失败，优先修 .bbmodel、UV 或贴图。
