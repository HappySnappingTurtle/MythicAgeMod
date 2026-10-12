---
creature_id: nemean_lion
creature_name: Nemean Lion / 尼米亚狮
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

# Nemean Lion / 尼米亚狮 设计包

## 目标体量
- Entity ID: `mythic_age:nemean_lion`
- 碰撞盒目标：`1.9w x 1.8h`
- 视觉高度目标：约 `1.9` 格
- 比例说明：要像超大型狮子，比狼和普通马更厚重；头脸与身体匹配，识别来自放射鬃毛、短吻、眼、獠牙和厚爪。

## 轮廓语言
- 正面：大鬃毛包围脸，肩部厚。
- 侧面：低伏强壮狮身，尾端毛球可见。
- 45 度：背部、尾端毛球和后腿体块可读。

## 概念设计图
- 文件：`docs/美术资源/greek_creatures/concepts/nemean_lion_concept.png`
- 用途：这是建模前的造型合同；Blockbench 和 MC 截图只验证是否实现该设计，不负责重新发明造型。
- 三视角：正面、侧面、45 度必须同时能识别该生物。

## V2 造型冻结
- 触发依据：用户实测反馈指出当前模型存在体量、精细度、纹理空白和神话特征不可读问题；测试只验证设计落地，不再承担造型试错。
- 正面第一识别必须是超大放射鬃毛包围脸，而不是方块黄兽或普通狼形。
- 肩背和前爪必须厚重，短吻部、眼睛、獠牙、爪尖都要在正面和 45 度可读。
- 尾端毛球和鬃毛边缘必须独立分块，侧面能读到大型狮身而非单一长方体。
- 背部硬质金色脊线表现神话中的不可伤皮毛，但不能替代狮身结构。

## 核心识别特征
- 超大型狮子，不是狼/猫科普通小体型
- 巨大放射状金色鬃毛必须超过头部边界并包住比例合理的头脸
- 强壮肩背、胸前厚块和背部硬质金色脊线体现不可伤神狮
- 短吻部、鼻口线、双眼、左右獠牙和每只爪尖都必须可读
- 尾端毛球必须分层存在，侧面不能像普通细尾

## 部件清单
- `body`
- `shoulder`
- `chest_mass`
- `back_armor_ridge`
- `head`
- `snout`
- `nose_mouth`
- `left_eye`
- `right_eye`
- `left_fang`
- `right_fang`
- `mane_core`
- `mane_top`
- `mane_bottom`
- `mane_left`
- `mane_right`
- `mane_outer_top`
- `mane_outer_left`
- `mane_outer_right`
- `mane_chin`
- `tail`
- `tail_tuft_core`
- `tail_tuft_top`
- `tail_tuft_bottom`
- `front_left_leg`
- `front_left_paw`
- `front_left_claws`
- `front_right_leg`
- `front_right_paw`
- `front_right_claws`
- `back_left_leg`
- `back_left_paw`
- `back_left_claws`
- `back_right_leg`
- `back_right_paw`
- `back_right_claws`

## Blockbench 层级
- `root/body`
- `root/body/shoulder`
- `root/head/face`
- `root/mane/radial_layers`
- `root/tail/tuft`
- `root/legs/paws_and_claws`

## 建模硬性要求
- mane_core/top/bottom/left/right/outer_* 必须比 head 更大，形成正面第一识别点；head 本体不能做成大头替代鬃毛
- left_eye/right_eye、left_fang/right_fang、nose_mouth 必须独立 ModelPart
- front/back paws 和 claws 必须拆到每只脚，不能合并成一条横杆
- shoulder、chest_mass、back_armor_ridge 必须让前半身比腰背更厚重
- tail_tuft_core/top/bottom 必须独立分块

## UV 计划
- mane_*: bright radial mane slabs surrounding and exceeding head bounds
- face_front: left_eye/right_eye/nose_mouth/snout/left_fang/right_fang fixed on front face
- body/shoulder/chest_mass/back_armor_ridge: heavy tawny body with invulnerable golden ridge accents
- front/back paws: each paw and claw strip gets distinct dark UV
- tail_tuft_*: separate tuft core/top/bottom so side view reads as lion tail, not plain rod

## 纹理细节
- 主色：`#c89032`
- 阴影：`#65461d`
- 强调色：`#f3d27a`
- 细节规则：金棕身体，鬃毛使用更亮金色和放射纹，肩背有深阴影和硬质金色脊线；眼睛深色带金色高光，吻部浅色并有鼻口线，左右獠牙浅色，四足爪尖深色，尾端毛球分层。

## 动画约束
- 行走四足步态；扑击时身体前倾且前爪下压；鬃毛作为整体随头部轻微跟随但外层保持放射轮廓；尾端毛球随尾部摆动。

## 返工触发条件
- 看起来像普通狮子或狼
- 没有大鬃毛或尾端毛球
- 爪、眼、吻部或左右獠牙不可读
- 前爪不厚或四爪没有深色爪尖
- 鬃毛没有外层放射轮廓

## 验收口径
- 先验收概念设计图，再进入 Blockbench 建模。
- 必须提供正面、侧面、45 度三视图，且三视图来自已挂载贴图后的模型预览。
- 必须提供 UV 模板和贴图贴合报告，关键 UV 区域不得透明、空白或越界。
- 如三视图或贴合报告失败，优先修 .bbmodel、UV 或贴图。
