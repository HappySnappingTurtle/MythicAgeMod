---
creature_id: medusa
creature_name: Medusa / 美杜莎
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

# Medusa / 美杜莎 设计包

## 目标体量
- Entity ID: `mythic_age:medusa`
- 碰撞盒目标：`1.1w x 2.55h`
- 视觉高度目标：约 `2.7` 格
- 比例说明：必须略高于玩家；蛇发冠和盘绕蛇尾扩大轮廓，基础头脸保持人形比例，不能像普通绿色人形也不能靠大头识别。

## 轮廓语言
- 正面：石化凝视双眼、女性头脸、多条蛇发冠和人形上身先被读到，下方转入蛇尾。
- 侧面：蛇发束离开头部轮廓，尾巴盘绕并向后滑出，身体不是普通双腿。
- 45 度：后蛇发、背部发束和蛇尾鳞片在背面仍能分层，不只是绿色柱体。

## 概念设计图
- 文件：`docs/美术资源/greek_creatures/concepts/medusa_concept.png`
- 用途：这是建模前的造型合同；Blockbench 和 MC 截图只验证是否实现该设计，不负责重新发明造型。
- 三视角：正面、侧面、45 度必须同时能识别该生物。

## V2 造型冻结
- 触发依据：用户实测反馈指出当前模型存在体量、精细度、纹理空白和神话特征不可读问题；测试只验证设计落地，不再承担造型试错。
- 正面必须先读到石化凝视眼睛、女性头脸和多条蛇发冠；不能只是绿色人形，也不能用过大的头块代替蛇发分束。
- 蛇发至少六束，前、左、右、背部都要有蛇头端点，侧面能看出蛇束离开头部轮廓。
- 下半身必须是盘绕蛇尾/滑行蛇身，不能读成普通双腿或单根柱体。
- 每个蛇头端点必须有固定 UV 的眼、牙、舌细节；测试只验证这些设计是否落地，不负责临场补造型。

## 核心识别特征
- 人形上身、女性头脸、蛇发冠、蛇形下身必须同时可读
- 蛇发不是普通头发贴图，必须有六条独立蛇束和六个蛇头端点
- 脸部必须有石化凝视双眼、瞳孔、鼻梁和嘴，眼部需要强对比；头块宽度必须服从躯干比例
- 每个蛇头必须至少具备眼、牙、舌的固定 UV 细节
- 下半身蛇尾要有盘绕/滑行趋势，而不是普通双腿或单根柱体

## 部件清单
- `head`
- `face_plate`
- `stone_gaze_aura`
- `left_eye`
- `right_eye`
- `left_pupil`
- `right_pupil`
- `nose_bridge`
- `mouth`
- `left_ear`
- `right_ear`
- `hair_crown`
- `hair_back`
- `front_snake_body`
- `front_snake_head`
- `front_snake_eyes`
- `front_snake_fangs`
- `front_snake_tongue`
- `left_snake_body`
- `left_snake_head`
- `left_snake_eyes`
- `left_snake_fangs`
- `left_snake_tongue`
- `right_snake_body`
- `right_snake_head`
- `right_snake_eyes`
- `right_snake_fangs`
- `right_snake_tongue`
- `back_snake_body`
- `back_snake_head`
- `back_snake_eyes`
- `back_snake_fangs`
- `back_snake_tongue`
- `left_outer_snake_body`
- `left_outer_snake_head`
- `left_outer_snake_eyes`
- `left_outer_snake_fangs`
- `left_outer_snake_tongue`
- `right_outer_snake_body`
- `right_outer_snake_head`
- `right_outer_snake_eyes`
- `right_outer_snake_fangs`
- `right_outer_snake_tongue`
- `torso`
- `robe_front`
- `waist_sash`
- `left_arm`
- `left_forearm`
- `left_hand`
- `right_arm`
- `right_forearm`
- `right_hand`
- `coiled_tail`
- `tail_base`
- `tail_mid`
- `tail_coil_outer`
- `tail_coil_inner`
- `tail_tip`
- `tail_tip_spike`

## Blockbench 层级
- `root/torso/{robe_front,waist_sash,shoulders,arms}`
- `root/head/{face_plate,stone_gaze_aura,eyes,pupils,nose_bridge,mouth,ears}`
- `root/head/snake_hair/{hair_crown,hair_back,six_snake_bodies}`
- `root/head/snake_hair/<snake>/{snake_head,eyes,fangs,tongue}`
- `root/coiled_tail/{tail_base,tail_mid,tail_coil_outer,tail_coil_inner,tail_tip,tail_tip_spike}`

## 建模硬性要求
- head、face_details、snake_hair、torso、arms、coiled_tail 分组固定；head 基础体积不能替代蛇发和五官细节
- 蛇发束必须为 front、left、right、back、left_outer、right_outer 六条，正面和侧面都能看见
- 每条蛇发必须拆出 body、head、eyes、fangs、tongue，不能只用一条管子代表
- 尾部必须拆出 base、mid、outer coil、inner coil、tip、tip spike，鳞片 UV 必须有连续条纹

## UV 计划
- face_front: pale face plate, high-contrast left/right eyes, dark pupils, nose bridge, mouth and stone gaze aura use fixed UV islands
- six snake heads: each snake head has explicit yellow eyes, ivory fangs and red tongue islands; snakes are not generic hair strips
- snake bodies/hair crown: dark green banded scales with lighter ridge lines
- torso/robe: gray-green robe plate, waist sash and pale arms separated from snake tail
- coiled_tail: tail_base, tail_mid, outer coil, inner coil, tail_tip and spike carry repeated scale bands

## 纹理细节
- 主色：`#2c5b46`
- 阴影：`#153428`
- 强调色：`#b8b8b8`
- 细节规则：脸部偏灰白，石化凝视眼睛为高对比浅绿/白色并有深色瞳孔；六条蛇发为深绿鳞片，每个蛇头有黄眼、白牙和红舌；躯干袍甲为灰绿，腰带偏暗；蛇尾使用连续鳞片条纹和更暗的尾尖。

## 动画约束
- 六条蛇发错相摆动，蛇头端点轻微点头；尾部滑行动画带盘绕摆动；石化凝视/攻击时头部稳定面向目标、石化眼光层显示、前蛇发压低。

## 返工触发条件
- 看起来只是绿色人形
- 蛇发与头部糊成一块，或缺少六个蛇头端点
- 脸部五官、瞳孔或石化凝视层不可读
- 蛇头没有眼、牙、舌，导致像普通发束
- 下半身读成双腿或单根尾柱

## 验收口径
- 先验收概念设计图，再进入 Blockbench 建模。
- 必须提供正面、侧面、45 度三视图，且三视图来自已挂载贴图后的模型预览。
- 必须提供 UV 模板和贴图贴合报告，关键 UV 区域不得透明、空白或越界。
- 如三视图或贴合报告失败，优先修 .bbmodel、UV 或贴图。
