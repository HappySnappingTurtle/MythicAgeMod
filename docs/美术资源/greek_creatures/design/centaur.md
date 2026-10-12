---
creature_id: centaur
creature_name: Centaur / 半人马
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

# Centaur / 半人马 设计包

## 目标体量
- Entity ID: `mythic_age:centaur`
- 碰撞盒目标：`1.9w x 3.0h`
- 视觉高度目标：约 `3.15` 格
- 比例说明：必须比玩家更高、更长；马身承担主体体量，人形战士上身从前肩长出并高出马肩。

## 轮廓语言
- 正面：人类战士上身、头脸、双肩和弓先被读到，四蹄左右分开支撑。
- 侧面：长马身、前置人身连接、背后箭袋、尾巴和竖向弓弦形成清楚轮廓。
- 45 度：马臀、尾巴、后腿、人身背部与箭袋分层清楚。

## 概念设计图
- 文件：`docs/美术资源/greek_creatures/concepts/centaur_concept.png`
- 用途：这是建模前的造型合同；Blockbench 和 MC 截图只验证是否实现该设计，不负责重新发明造型。
- 三视角：正面、侧面、45 度必须同时能识别该生物。

## V2 造型冻结
- 触发依据：用户实测反馈指出当前模型存在体量、精细度、纹理空白和神话特征不可读问题；测试只验证设计落地，不再承担造型试错。
- 正面必须先读到人类战士上身、头脸、双肩和弓；马身只能作为下半身支撑，不得像马背方块。
- 人身从马肩前段长出，腰部连接、胸肩、头脸和双臂必须独立可读，侧面弓弦形成清楚长弧轮廓。
- 侧面必须显示长马身、四蹄步态、后尾、背部箭袋和前置人身连接，整体不是普通马或骑手。
- 所有脸部、武器和四蹄细节必须是命名结构和固定 UV 区，不再依赖随机纹理面。

## 核心识别特征
- 马身加前置人形战士上身的清楚拼接，不允许像普通马背上长方块
- 人形头脸必须有双眼、鼻梁、嘴线、耳朵和深色头发
- 弓、弓弦、箭和背部箭袋共同构成半人马弓手识别点
- 四条马腿、四个独立马蹄和尾巴要在正面/侧面都清楚读出
- 人身腰部必须包在马肩前段，而不是坐在马背中央

## 部件清单
- `horse_body`
- `horse_chest`
- `horse_rump`
- `horse_shoulder_blend`
- `human_waist`
- `human_torso`
- `human_shoulders`
- `human_chest_strap`
- `head`
- `left_eye`
- `right_eye`
- `nose_bridge`
- `mouth`
- `left_ear`
- `right_ear`
- `hair_crown`
- `hair_back`
- `mane_neck`
- `left_arm`
- `left_forearm`
- `left_hand`
- `right_arm`
- `right_forearm`
- `right_hand`
- `left_finger_a`
- `left_finger_b`
- `right_finger_a`
- `right_finger_b`
- `bow_upper`
- `bow_lower`
- `bow_grip`
- `bow_string`
- `arrow`
- `arrow_tip`
- `quiver`
- `quiver_strap`
- `quiver_arrow_a`
- `quiver_arrow_b`
- `tail`
- `tail_tuft`
- `front_left_leg`
- `front_left_hoof`
- `front_right_leg`
- `front_right_hoof`
- `back_left_leg`
- `back_left_hoof`
- `back_right_leg`
- `back_right_hoof`

## Blockbench 层级
- `root/horse_body/{horse_chest,horse_rump,horse_shoulder_blend,tail}`
- `root/human_torso/{human_waist,human_shoulders,human_chest_strap,head,arms,quiver}`
- `root/human_torso/head/{eyes,nose_bridge,mouth,ears,hair}`
- `root/human_torso/arms/{forearms,hands,fingers,bow,arrow}`
- `root/legs/{front_left,front_right,back_left,back_right}/{leg,hoof}`

## 建模硬性要求
- horse_body、human_torso、head、bow、quiver、tail、四腿四蹄必须分组明确
- 人形腰部必须从马肩前段长出，不能放在马身中央偏后
- 头脸必须使用独立眼睛、鼻梁、嘴线、耳朵、头发部件，不允许只靠一个头部贴图暗示
- 弓必须由 bow_upper、bow_lower、bow_grip、bow_string 组成，在侧面形成细长竖向轮廓
- 四个 hoof 必须是独立部件，不能用 front_hooves/back_hooves 合并条带

## UV 计划
- human_head_front: left/right eyes, nose bridge, mouth and ears occupy fixed face UV islands
- hair_crown/hair_back/mane_neck: dark hair bands separate from skin and horse coat
- horse_body_side: warm brown coat with darker belly and shoulder shading
- equipment: bow limbs, bow string, arrow tip, quiver and diagonal strap each occupy named UV islands
- legs/hooves: every leg and hoof has its own dark hoof UV block; no shared front/back hoof strip

## 纹理细节
- 主色：`#8b5a2b`
- 阴影：`#3f2614`
- 强调色：`#d9b382`
- 细节规则：马身暖棕并带腹部/肩部阴影；人身为肤色，头脸固定双眼、鼻梁、嘴线和耳朵；深色头发从头顶延伸到颈后鬃毛；弓为深木色、弓弦近黑、箭头金属灰；箭袋与胸带为深皮革；四个马蹄独立深色块。

## 动画约束
- 马腿四足错相步态，四蹄随对应腿摆动；人身保持前置稳定；头部跟随视线；远程攻击时右手拉弦、左手稳弓、弓弦和箭发生轻微拉伸/显隐；尾巴持续轻摆。

## 返工触发条件
- 看起来只是无头骑手或普通马
- 人脸没有独立双眼、鼻梁和嘴线
- 弓、箭、箭袋任一缺失导致战士身份不可读
- 四足步态部件或四个马蹄无法从躯干区分

## 验收口径
- 先验收概念设计图，再进入 Blockbench 建模。
- 必须提供正面、侧面、45 度三视图，且三视图来自已挂载贴图后的模型预览。
- 必须提供 UV 模板和贴图贴合报告，关键 UV 区域不得透明、空白或越界。
- 如三视图或贴合报告失败，优先修 .bbmodel、UV 或贴图。
