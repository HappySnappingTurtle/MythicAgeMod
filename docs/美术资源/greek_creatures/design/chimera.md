---
creature_id: chimera
creature_name: Chimera / 奇美拉
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

# Chimera / 奇美拉 设计包

## 目标体量
- Entity ID: `mythic_age:chimera`
- 碰撞盒目标：`2.3w x 2.25h`
- 视觉高度目标：约 `2.4` 格
- 比例说明：必须是大型混合兽，多头和背部翼/火焰轮廓比普通四足兽更复杂。

## 轮廓语言
- 正面：狮头主脸，侧面羊头和蛇尾形成复合轮廓。
- 侧面：长身、羊头、蛇尾和背部翼/鬃线可见。
- 45 度：翼、蛇尾和多头后侧连接关系清楚。

## 概念设计图
- 文件：`docs/美术资源/greek_creatures/concepts/chimera_concept.png`
- 用途：这是建模前的造型合同；Blockbench 和 MC 截图只验证是否实现该设计，不负责重新发明造型。
- 三视角：正面、侧面、45 度必须同时能识别该生物。

## V2 造型冻结
- 触发依据：用户实测反馈指出当前模型存在体量、精细度、纹理空白和神话特征不可读问题；测试只验证设计落地，不再承担造型试错。
- 三重身份必须同时可读：狮头是主攻击脸，羊头带角和胡须从侧面探出，尾端蛇头从尾段自然过渡。
- 狮头不得退化成普通四足兽头；必须有鬃毛环、眼睛、鼻口线和獠牙。
- 羊头不得贴成小方块，角、眼睛、鼻吻和胡须要突出；蛇头必须有上下颚、双眼和舌头。
- 背部翼/火焰脊强调火性轮廓，但不能遮住三头结构或让模型变成单纯飞兽。

## 核心识别特征
- 狮头主脸、侧探羊头、尾端蛇头三重身份必须同时可读
- 狮头必须有鬃毛、双眼、鼻口线和獠牙，不能只是普通方块兽头
- 羊头必须有独立颈部、鼻吻、双眼、双角和胡须，不能贴成身体侧面小块
- 蛇尾必须从尾段过渡到蛇头，并有上下颚、眼睛和舌头
- 翼膜/翼骨/翼爪和肩部火焰脊形成火性轮廓，但不能遮住三头结构

## 部件清单
- `body`
- `chest`
- `shoulder_fire_crest`
- `lion_head`
- `lion_mane_core`
- `lion_mane_top`
- `lion_mane_left`
- `lion_mane_right`
- `lion_snout`
- `lion_nose_mouth`
- `lion_left_eye`
- `lion_right_eye`
- `lion_left_fang`
- `lion_right_fang`
- `goat_neck`
- `goat_head`
- `goat_snout`
- `goat_left_eye`
- `goat_right_eye`
- `goat_left_horn`
- `goat_right_horn`
- `goat_beard`
- `tail_base`
- `tail_rise`
- `tail_tip`
- `serpent_head`
- `serpent_upper_jaw`
- `serpent_lower_jaw`
- `serpent_left_eye`
- `serpent_right_eye`
- `snake_tongue`
- `left_wing_membrane`
- `left_wing_bone`
- `left_wing_claw`
- `right_wing_membrane`
- `right_wing_bone`
- `right_wing_claw`
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
- `root/body/shoulder_fire_crest`
- `root/heads/lion`
- `root/heads/goat`
- `root/tail/serpent`
- `root/wings/left_wing`
- `root/wings/right_wing`
- `root/legs/paws`

## 建模硬性要求
- lion_head、goat_head、serpent_head 必须独立且分别有面部细节 ModelPart
- lion_mane_core/top/left/right 必须包住狮头形成主脸优先级
- goat_left_horn/goat_right_horn、goat_beard、goat_snout 必须突出侧头身份
- tail_base/tail_rise/tail_tip 必须连接到 serpent_head，不能让蛇头像单独漂浮部件
- left/right_wing 必须拆出 membrane、bone、claw，翼膜颜色区别主体

## UV 计划
- lion_head_front: lion_left_eye/lion_right_eye/lion_nose_mouth/lion_fangs fixed on front face
- lion_mane_*: radial dark mane slabs around the lion head
- goat_head/goat_snout: separate pale goat face with goat_left_eye/goat_right_eye and cream horns
- serpent_head/serpent_*_jaw: dark green snake face with yellow eyes and forked tongue
- body/chest/shoulder_fire_crest: scorched brown body with ember ridge
- wing_membrane/wing_bone/wing_claw: ember membrane, dark bones, hooked claws
- leg/claws: paws and claws use distinct dark nail UV strips

## 纹理细节
- 主色：`#4a2c22`
- 阴影：`#2a1d19`
- 强调色：`#d65a31`
- 细节规则：深棕狮身，暗鬃毛包住主狮头；狮眼金黄、鼻口深色、獠牙浅色；羊头偏灰褐且有浅色角和胡须；蛇尾头深绿，蛇眼金黄，上下颚和红舌分离；翼膜带火焰橙纹，爪尖深色。

## 动画约束
- 狮头作为主攻击方向随视角转动；羊头较小幅度错相摆动；蛇尾随尾部甩动并独立张颚吐舌；翼只小幅开合保持轮廓稳定。

## 返工触发条件
- 只有一个兽头，看不出复合怪
- 狮头没有明确眼睛、鼻口或獠牙
- 羊角、羊胡须或羊眼缺失
- 蛇尾没有上下颚、蛇眼或舌头
- 翼部只是一整块色块，缺少翼骨/翼爪分层

## 验收口径
- 先验收概念设计图，再进入 Blockbench 建模。
- 必须提供正面、侧面、45 度三视图，且三视图来自已挂载贴图后的模型预览。
- 必须提供 UV 模板和贴图贴合报告，关键 UV 区域不得透明、空白或越界。
- 如三视图或贴合报告失败，优先修 .bbmodel、UV 或贴图。
