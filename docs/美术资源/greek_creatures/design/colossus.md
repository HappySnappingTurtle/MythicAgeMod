---
creature_id: colossus
creature_name: Colossus / 青铜巨像
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

# Colossus / 青铜巨像 设计包

## 目标体量
- Entity ID: `mythic_age:colossus`
- 碰撞盒目标：`1.7w x 4.2h`
- 视觉高度目标：约 `4.5` 格
- 比例说明：已具备大体量，重点是青铜/神像质感和面部结构正确。

## 轮廓语言
- 正面：巨大神像头冠、发光眼缝、面甲、宽肩、分层胸甲和柱状腿必须一眼可读。
- 侧面：厚重胸腔、前后胸甲厚度、巨大拳头和大脚像可移动青铜神像。
- 45 度：背部青铜板、肩臂、肘膝关节缝和柱状腿在背面仍有神像感。

## 概念设计图
- 文件：`docs/美术资源/greek_creatures/concepts/colossus_concept.png`
- 用途：这是建模前的造型合同；Blockbench 和 MC 截图只验证是否实现该设计，不负责重新发明造型。
- 三视角：正面、侧面、45 度必须同时能识别该生物。

## V2 造型冻结
- 触发依据：用户实测反馈指出当前模型存在体量、精细度、纹理空白和神话特征不可读问题；测试只验证设计落地，不再承担造型试错。
- 正面必须读到青铜神像巨人、发光眼缝、冠饰和胸甲板缝，而不是普通棕色巨人。
- 躯干和四肢必须有青铜板、腰带、关节缝与铜绿点缀，不能大面积空白色块。
- 体量仍作为希腊最高压迫感单位，视觉高度约 4.5 格，脚掌和柱状腿要支撑神像重量。
- 脸、胸、手、脚的细节必须是命名结构和固定 UV 区，不再只靠通用铜色纹理暗示。

## 核心识别特征
- 青铜神像巨人，体量显著高于其他生物
- 头冠、面甲、发光眼缝、嘴缝和颊板必须明显，不允许脸部空白铜块
- 胸甲上板/下板、中心宝石、左右板缝和背板共同形成神像铠甲
- 手臂和腿像雕像柱体，但必须有肘环、膝环、拳头、手指、大脚和脚趾板
- 铜绿只作为关节/缝隙点缀，不改变青铜主体

## 部件清单
- `head`
- `face_mask`
- `eye_slit`
- `mouth_groove`
- `left_cheek_plate`
- `right_cheek_plate`
- `crown_band`
- `crown_center_spike`
- `crown_spike_left`
- `crown_spike_right`
- `torso_core`
- `chest_plate_upper`
- `chest_plate_lower`
- `chest_center_gem`
- `left_chest_seam`
- `right_chest_seam`
- `back_plate`
- `bronze_belt`
- `left_shoulder_plate`
- `right_shoulder_plate`
- `left_arm`
- `left_elbow_ring`
- `left_forearm`
- `left_fist`
- `left_finger_a`
- `left_finger_b`
- `right_arm`
- `right_elbow_ring`
- `right_forearm`
- `right_fist`
- `right_finger_a`
- `right_finger_b`
- `left_leg`
- `left_knee_ring`
- `left_shin_plate`
- `left_foot`
- `left_toe_plate`
- `right_leg`
- `right_knee_ring`
- `right_shin_plate`
- `right_foot`
- `right_toe_plate`
- `oxidized_cracks`

## Blockbench 层级
- `root/head/{face_mask,eye_slit,mouth_groove,cheek_plates,crown_band,crown_spikes}`
- `root/body/{torso_core,chest_plates,chest_center_gem,chest_seams,back_plate,bronze_belt}`
- `root/body/shoulders/{left_shoulder_plate,right_shoulder_plate}`
- `root/arms/{upper_arm,elbow_ring,forearm,fist,fingers}`
- `root/legs/{leg,knee_ring,shin_plate,foot,toe_plate}`

## 建模硬性要求
- head、crown、face_mask、eye_slit、torso_core、chest_plates、arms、legs 分组明确
- 脸部必须有眼缝、嘴缝和左右颊板，不是空白铜块
- 胸甲必须拆出 upper/lower/center_gem/left_seam/right_seam/back_plate
- 每条手臂必须拆出 elbow_ring、forearm、fist、finger_a、finger_b
- 每条腿必须拆出 knee_ring、shin_plate、foot、toe_plate

## UV 计划
- face_front: face mask, glowing eye slit, mouth groove, cheek plates and crown spikes occupy fixed islands
- torso_front: upper/lower chest plates, center gem and left/right seams separate large bronze surfaces
- body_back: back plate and oxidized cracks prevent blank rear silhouette
- arms: upper arm, elbow ring, forearm, fist and fingers use separate bronze/verdigris UV regions
- legs: knee rings, shin plates, massive feet and toe plates use separate dark joint/bronze UV regions

## 纹理细节
- 主色：`#7b5a3a`
- 阴影：`#362b27`
- 强调色：`#2dba9d`
- 细节规则：青铜主体分深浅板块；发光眼缝为青绿色白高光；面甲和嘴缝深色；胸甲上下板、中心宝石、肩甲、肘膝环、手指、脚趾均有深裂缝和少量铜绿，不允许大面积空白铜板。

## 动画约束
- 步态缓慢重；行走时腿和手臂摆幅小但惯性重；攻击时右臂大幅砸下、左臂平衡，胸体微前倾；死亡时整体倾倒且头冠随身体下沉。

## 返工触发条件
- 像普通石巨人而非青铜神像
- 脸部没有眼缝、嘴缝、面甲或冠饰
- 躯干大片空白无胸甲板缝/背板
- 手脚没有拳头、手指、大脚和脚趾板导致像普通柱状肢体

## 验收口径
- 先验收概念设计图，再进入 Blockbench 建模。
- 必须提供正面、侧面、45 度三视图，且三视图来自已挂载贴图后的模型预览。
- 必须提供 UV 模板和贴图贴合报告，关键 UV 区域不得透明、空白或越界。
- 如三视图或贴合报告失败，优先修 .bbmodel、UV 或贴图。
