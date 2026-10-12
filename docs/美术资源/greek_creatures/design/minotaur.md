---
creature_id: minotaur
creature_name: Minotaur / 米诺陶
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

# Minotaur / 米诺陶 设计包

## 目标体量
- Entity ID: `mythic_age:minotaur`
- 碰撞盒目标：`1.35w x 2.75h`
- 视觉高度目标：约 `2.9` 格
- 比例说明：应明显高于玩家、宽肩厚胸，但低于独眼巨人；不能像普通僵尸换牛头。

## 轮廓语言
- 正面：巨大牛头、外展双角、宽肩人形躯干和战斧必须一眼可读。
- 侧面：牛吻前突、粗颈、厚胸、尾巴和长柄双刃斧形成冲锋怪轮廓。
- 45 度：宽背、牛尾、双角外廓和战斧背影仍能区分。

## 概念设计图
- 文件：`docs/美术资源/greek_creatures/concepts/minotaur_concept.png`
- 用途：这是建模前的造型合同；Blockbench 和 MC 截图只验证是否实现该设计，不负责重新发明造型。
- 三视角：正面、侧面、45 度必须同时能识别该生物。

## V2 造型冻结
- 触发依据：用户实测反馈指出当前模型存在体量、精细度、纹理空白和神话特征不可读问题；测试只验证设计落地，不再承担造型试错。
- 正面第一识别必须是牛头、外展双角、宽肩厚胸和长柄双刃斧，而不是普通人形怪。
- 牛吻、眉眼、嘴线和双角必须分区建模/贴图；角的外轮廓要超过头部宽度。
- 体量必须从旧版 0.9x2.1 提升为大型迷宫怪，约 1.35 格宽、2.75 格高，仍低于独眼巨人。
- 头部主体必须匹配宽肩厚胸比例；牛头识别来自牛吻、眉眼、鼻孔、嘴线、耳朵和外展双角，不能靠异常大头解决。
- 双刃斧必须有左右斧刃、中心套筒、长柄和配重；蹄足必须区别普通人脚。

## 核心识别特征
- 牛头人身必须清楚，不能只是普通人形头部贴牛纹
- 双角外展、牛吻前突、眉眼、鼻孔和嘴必须独立可读
- 宽肩厚胸、胸肌、腰带、粗臂、手指、蹄足和牛尾表现强壮迷宫怪
- 长柄双刃斧必须有左右斧刃、中心连接和尾端配重，不能只是一根棍子

## 部件清单
- `head`
- `muzzle`
- `heavy_brow`
- `left_eye`
- `right_eye`
- `left_nostril`
- `right_nostril`
- `mouth`
- `left_horn`
- `right_horn`
- `left_ear`
- `right_ear`
- `neck`
- `body`
- `chest_harness`
- `left_chest_muscle`
- `right_chest_muscle`
- `waist_belt`
- `loincloth_front`
- `left_arm`
- `left_forearm`
- `left_fist`
- `left_finger_a`
- `left_finger_b`
- `right_arm`
- `right_forearm`
- `right_fist`
- `right_finger_a`
- `right_finger_b`
- `labrys_handle`
- `labrys_left_blade`
- `labrys_right_blade`
- `labrys_center_socket`
- `labrys_counterweight`
- `tail`
- `tail_tuft`
- `left_leg`
- `left_hoof`
- `left_hoof_claw`
- `right_leg`
- `right_hoof`
- `right_hoof_claw`

## Blockbench 层级
- `root/head/face`
- `root/body/chest_and_belt`
- `root/arms/hands`
- `root/right_arm/labrys_axe`
- `root/legs/hooves`
- `root/body/tail`

## 建模硬性要求
- head、muzzle、heavy_brow、left/right_eye、left/right_nostril、mouth、left/right_horn、left/right_ear 必须在正面可读
- labrys_axe 必须拆成 handle、left_blade、right_blade、center_socket、counterweight，侧面和 45 度不能被身体遮没
- 胸肌、腰带、前垂布、手指、左右蹄和蹄尖必须独立建模或明确 UV
- 碰撞盒目标约 1.35w x 2.75h，视觉体量明显大于玩家

## UV 计划
- bull_head_front: heavy_brow, left_eye/right_eye, muzzle, nostrils and mouth fixed on front face
- horns/ears: pale outward horns and side ears exceeding head bounds
- torso_front: chest harness, left/right chest muscles, belt and loincloth
- hands: fists and fingers separated from arms
- hooves: each hoof and hoof claw uses dark UV strip
- labrys_axe: wooden handle, two metal blades, center socket and counterweight separated

## 纹理细节
- 主色：`#6f3f22`
- 阴影：`#2b1a12`
- 强调色：`#d6c08a`
- 细节规则：深棕牛头与人身，浅色大角带渐变，黑色眉眼、鼻孔和嘴线固定在牛吻上；胸前皮革束带、胸肌阴影、腰带和前垂布分区；手指、蹄部和蹄尖深色；双刃斧用灰金属刃、深木柄和中心铆接。

## 动画约束
- 待机头部低压；行走时粗臂、手指和腿摆动；冲锋时头角前压、身体前倾，战斧保持侧面和 45 度可读轮廓。

## 返工触发条件
- 看起来像普通僵尸或小型村民怪
- 双角、牛吻、鼻孔、嘴眼或耳朵不清楚
- 没有武器轮廓或武器细得不可读
- 斧头没有左右刃或中心连接
- 腿脚像普通人腿，没有蹄足和蹄尖

## 验收口径
- 先验收概念设计图，再进入 Blockbench 建模。
- 必须提供正面、侧面、45 度三视图，且三视图来自已挂载贴图后的模型预览。
- 必须提供 UV 模板和贴图贴合报告，关键 UV 区域不得透明、空白或越界。
- 如三视图或贴合报告失败，优先修 .bbmodel、UV 或贴图。
