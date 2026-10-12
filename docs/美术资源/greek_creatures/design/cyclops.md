---
creature_id: cyclops
creature_name: Cyclops / 独眼巨人
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

# Cyclops / 独眼巨人 设计包

## 目标体量
- Entity ID: `mythic_age:cyclops`
- 碰撞盒目标：`1.8w x 4.0h`
- 视觉高度目标：约 `4.15` 格
- 比例说明：比玩家高约 2 倍，肩宽接近两个玩家，不能像普通僵尸。

## 轮廓语言
- 正面：比例合理的独眼头脸、宽肩、粗臂、短粗腿形成高压迫感。
- 侧面：额头和鼻梁前突，胸腔厚，手臂垂到膝盖。
- 45 度：背部宽厚，双臂和短粗腿在背面仍有压迫感。

## 概念设计图
- 文件：`docs/美术资源/greek_creatures/concepts/cyclops_concept.png`
- 用途：这是建模前的造型合同；Blockbench 和 MC 截图只验证是否实现该设计，不负责重新发明造型。
- 三视角：正面、侧面、45 度必须同时能识别该生物。

## V2 造型冻结
- 触发依据：用户实测反馈指出当前模型存在体量、精细度、纹理空白和神话特征不可读问题；测试只验证设计落地，不再承担造型试错。
- 正面第一识别必须是单眼巨人头脸、宽肩厚胸和右手巨大石棒，而不是盾牌状侧块。
- 石棒由细长握柄、上端不规则巨石块和底部配重组成；巨石块必须高过肩线。
- 头部高度约占整体视觉高度 25%，但不能用异常大头替代面部结构；中央眼必须占头部正面宽度约 35%-45%。
- 面部必须包含瞳孔、鼻孔、牙齿和耳朵；四肢必须包含手指/脚趾，保证不是普通大号人形占位。
- 躯干必须有人形胸肌、腰带和兽皮裙层级，支撑独眼巨人作为神话时代大型人形单位的身份。

## 核心识别特征
- 巨大人形比例，宽肩厚胸，不是普通僵尸放大
- 头部正面只有一个居中的大眼，必须有瞳孔、眉骨、鼻梁、鼻孔、嘴和牙
- 单手持巨大粗糙石棒，石棒长度接近身体高度的一半以上，石块头高过肩线
- 手臂粗长、拳头有手指，腿短而重、脚掌有脚趾，整体像古希腊巨人单位
- 胸肌、腰带和兽皮裙提供人形身体结构，不能只是一个大矩形躯干

## 部件清单
- `head`
- `heavy_brow`
- `central_eye`
- `central_pupil`
- `nose_bridge`
- `nostrils`
- `mouth`
- `teeth`
- `ears`
- `torso`
- `chest_muscles`
- `waist_belt`
- `loincloth`
- `left_arm`
- `right_arm`
- `forearms`
- `fists`
- `fingers`
- `left_leg`
- `right_leg`
- `feet`
- `toes`
- `stone_club_handle`
- `stone_club_head`
- `stone_club_lumps`
- `stone_club_knob`

## Blockbench 层级
- `root/body`
- `root/head`
- `root/arms`
- `root/legs`
- `root/weapon`
- `root/weapon/stone_club`

## 建模硬性要求
- 石棒必须作为独立 weapon group 建模并挂在右手侧
- 眼睛、瞳孔、眉骨、鼻梁、鼻孔、嘴和牙必须是独立可读结构或明确 UV 区
- 身体高度约 4 格，头部应匹配宽肩厚胸比例；单眼细节必须清楚，但不能用异常大头替代面部结构
- 手指、脚趾、腰带、兽皮裙和不规则石棒块面必须进入 .bbmodel，而不是只靠贴图暗示

## UV 计划
- head_front: oversized single eye, dark pupil, brow, nose bridge, nostrils, mouth and teeth
- torso_front: broad chest muscles, waist belt and ragged loincloth
- arms: long muscular arms, fists and separated fingers
- legs: short heavy thighs, feet and separated toes
- weapon: stone club handle, large irregular stone head, side/top lumps and bottom knob

## 纹理细节
- 主色：`#7a6b5c`
- 阴影：`#3f342a`
- 强调色：`#f0e0b0`
- 细节规则：灰褐皮肤，中央大眼占头部正面核心区域并带深色瞳孔；眉骨、鼻孔、嘴线和牙齿必须高对比。胸肌用浅暗分区，腰带/兽皮为深皮革色；手指、脚趾和指甲独立；石棒必须是多块面粗糙灰石纹理并有裂纹。

## 动画约束
- 待机轻微呼吸；行走驱动双腿和长臂；攻击时石棒随主手大幅挥击，头部不变形。

## 返工触发条件
- 看起来只是无武器大人形
- 单眼被纹理噪声淹没或没有嘴部结构
- 石棒细得像棍子，或没有石质块面
- 没有耳朵、牙、手指/脚趾或腰部服饰，导致像粗糙占位模型

## 验收口径
- 先验收概念设计图，再进入 Blockbench 建模。
- 必须提供正面、侧面、45 度三视图，且三视图来自已挂载贴图后的模型预览。
- 必须提供 UV 模板和贴图贴合报告，关键 UV 区域不得透明、空白或越界。
- 如三视图或贴合报告失败，优先修 .bbmodel、UV 或贴图。
