---
creature_id: manticore
creature_name: Manticore / 蝎尾狮
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

# Manticore / 蝎尾狮 设计包

## 目标体量
- Entity ID: `mythic_age:manticore`
- 碰撞盒目标：`2.0w x 2.05h`
- 视觉高度目标：约 `2.2` 格
- 比例说明：狮身应比尼米亚狮略长，翅膀和蝎尾扩大整体轮廓。

## 轮廓语言
- 正面：兽头、展开翅膀、宽胸。
- 侧面：长蝎尾高高翘起，尾刺明显。
- 45 度：翅膀、蝎尾和尾刺从背面清楚分离。

## 概念设计图
- 文件：`docs/美术资源/greek_creatures/concepts/manticore_concept.png`
- 用途：这是建模前的造型合同；Blockbench 和 MC 截图只验证是否实现该设计，不负责重新发明造型。
- 三视角：正面、侧面、45 度必须同时能识别该生物。

## V2 造型冻结
- 触发依据：用户实测反馈指出当前模型存在体量、精细度、纹理空白和神话特征不可读问题；测试只验证设计落地，不再承担造型试错。
- 正面和 45 度必须同时读到兽脸獠牙、展开膜翼和上扬蝎尾毒刺，三者缺一即返工。
- 蝎尾必须高过背线形成钩状轮廓，不能是一条水平尾巴；毒刺用绿色/毒性强调色独立分块。
- 翅膀必须有骨架、翼爪和膜翼分段，不能用单条平板横条替代。
- 头脸必须有双眼、鼻口线、左右獠牙和耳侧尖刺，让它比普通狮兽更凶险。

## 核心识别特征
- 狮身主体、分段膜翼、上扬蝎尾毒刺必须三者同时明显
- 脸部必须有双眼、鼻口线、左右獠牙、耳朵和左右 cheek_spike，不能只是普通兽头，也不能靠大头读凶险感
- 蝎尾必须由 base/rise/curve/hook/sting/tip 形成高过背线的钩状轮廓
- 翼膜、翼骨、翼爪必须分层，不能用单块平板代表翅膀
- 暗红兽身和绿色毒性翼膜/尾刺形成清楚对比

## 部件清单
- `body`
- `chest`
- `shoulder_ridge`
- `head`
- `snout`
- `nose_mouth`
- `left_eye`
- `right_eye`
- `left_fang`
- `right_fang`
- `left_ear`
- `right_ear`
- `left_cheek_spike`
- `right_cheek_spike`
- `left_wing_membrane_a`
- `left_wing_membrane_b`
- `left_wing_bone_front`
- `left_wing_bone_mid`
- `left_wing_bone_outer`
- `left_wing_claw`
- `right_wing_membrane_a`
- `right_wing_membrane_b`
- `right_wing_bone_front`
- `right_wing_bone_mid`
- `right_wing_bone_outer`
- `right_wing_claw`
- `tail_base`
- `tail_rise`
- `tail_curve`
- `tail_hook`
- `tail_sting`
- `tail_sting_tip`
- `front_left_leg`
- `front_left_claws`
- `front_right_leg`
- `front_right_claws`
- `back_left_leg`
- `back_left_claws`
- `back_right_leg`
- `back_right_claws`

## Blockbench 层级
- `root/body`
- `root/body/chest`
- `root/head/face`
- `root/wings/left_wing`
- `root/wings/right_wing`
- `root/tail/scorpion_segments`
- `root/legs/claws`

## 建模硬性要求
- left/right_wing 必须拆出 membrane_a、membrane_b、bone_front、bone_mid、bone_outer、wing_claw
- tail_base、tail_rise、tail_curve、tail_hook、tail_sting、tail_sting_tip 必须连续组成蝎尾
- head 必须拆出 snout、nose_mouth、left/right_eye、left/right_fang、left/right_ear、cheek_spikes；基础头宽不得接近躯干宽度
- front/back legs 必须有 claws，避免四足落地像无特征红色方块

## UV 计划
- face_front: left_eye/right_eye/nose_mouth/left_fang/right_fang fixed around snout
- ears/left_cheek_spike/right_cheek_spike: side silhouette accents around the head
- wing_membrane_*: dark poison-green membrane panels
- wing_bone_* and wing_claw: dark bone struts and hooked tips separated from membrane
- tail_base/tail_rise/tail_curve/tail_hook: raised segmented scorpion tail arc
- tail_sting/tail_sting_tip: poison green sting and bright venom tip
- leg/claws: red fur legs with dark claw UV strips

## 纹理细节
- 主色：`#8b3e3a`
- 阴影：`#2f242a`
- 强调色：`#407f4a`
- 细节规则：暗红狮身，胸肩有更深阴影；头部必须固定金黄眼、黑鼻口线和浅色獠牙；翼膜为暗毒绿并带分段纹理，翼骨/翼爪更深；蝎尾分节上扬，毒刺和毒刺尖使用亮绿色；四足爪尖深色。

## 动画约束
- 翼膜随左右翼小幅拍动但翼骨保持分段；蝎尾分节待机摆动，特殊攻击时 tail_hook 与 tail_sting 明显前压；头脸随视角转动，耳朵轻微抖动。

## 返工触发条件
- 没有翼或翼像贴片
- 尾刺不明显或没有毒性颜色
- 头脸没有眼睛、鼻口线或左右獠牙
- 蝎尾没有分节上扬或 hook/sting/tip 关系不清
- 翅膀缺少翼骨/翼膜/翼爪分层

## 验收口径
- 先验收概念设计图，再进入 Blockbench 建模。
- 必须提供正面、侧面、45 度三视图，且三视图来自已挂载贴图后的模型预览。
- 必须提供 UV 模板和贴图贴合报告，关键 UV 区域不得透明、空白或越界。
- 如三视图或贴合报告失败，优先修 .bbmodel、UV 或贴图。
