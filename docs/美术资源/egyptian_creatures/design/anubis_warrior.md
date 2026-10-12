---
creature_id: anubis_warrior
creature_name: Anubis Warrior / 阿努比斯战士
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

# Anubis Warrior / 阿努比斯战士 设计包

## 目标体量
- Entity ID: `mythic_age:anubis_warrior`
- 碰撞盒目标：`0.9w x 2.3h`
- 视觉高度目标：约 `2.45` 格
- 比例说明：比玩家略高，必须是胡狼头人形守卫；头部保持人形比例，识别来自长吻、长耳、眼睛和金饰，不靠大头。

## 轮廓语言
- 正面：胡狼长耳、长吻、金色胸饰和手持镰剑/权杖必须第一眼可见。
- 侧面：长吻前突，后脑耳朵高耸，武器形成清楚侧影。
- 45 度：背部披肩、耳朵和腿部护甲仍能读出埃及守卫身份。

## 概念设计图
- 文件：`docs/美术资源/egyptian_creatures/concepts/anubis_warrior_concept.png`
- 用途：这是建模前的造型合同；Blockbench 和 MC 截图只验证是否实现该设计，不负责重新发明造型。
- 三视角：正面、侧面、45 度必须同时能识别该生物。

## V2 造型冻结
- 触发依据：用户实测反馈指出当前模型存在体量、精细度、纹理空白和神话特征不可读问题；测试只验证设计落地，不再承担造型试错。
- 正面第一识别必须是胡狼头、长吻、长耳、金色胸饰和右手镰剑；不能用大头替代吻部和耳朵细节。
- 长吻、双耳、眼睛和嘴线必须分区建模/贴图；深色头部上眼睛必须高对比。
- 镰剑/权杖必须形成独立武器轮廓，侧面和 45 度不能被手臂遮没。

## 核心识别特征
- 胡狼头、长吻、长耳、人形身体和埃及金饰必须同时可读，头块宽度不能压过肩胸比例
- 右手镰剑/权杖必须作为独立武器轮廓
- 眼睛不能被深色头部淹没，胸饰不能与身体糊成一块
- 整体是神殿守卫，不是普通亡灵或村民

## 部件清单
- `jackal_head`
- `long_snout`
- `ears`
- `eyes`
- `torso`
- `gold_collar`
- `arms`
- `khopesh`
- `legs`
- `sandals`
- `mouth_line`
- `left_nostril`
- `right_nostril`
- `forehead_uraeus`
- `chest_scarab`
- `collar_center_gem`
- `left_bracer`
- `right_bracer`
- `khopesh_hook`
- `khopesh_counterweight`
- `belt_buckle`

## Blockbench 层级
- `root/head`
- `root/body`
- `root/arms`
- `root/weapon`
- `root/legs`

## 建模硬性要求
- jackal_head、snout、ears、gold_collar、khopesh 必须独立几何
- 头部高度和耳朵必须高于普通玩家头部轮廓，但基础头块宽度必须小于胸宽约四分之三
- 武器至少达到身体高度的 45%，侧面可见

## UV 计划
- head_front: gold eyes and long snout
- collar: gold bands
- weapon: bronze khopesh
- legs: linen skirt and sandals

## 纹理细节
- 主色：`#3b2b22`
- 阴影：`#17110d`
- 强调色：`#d8b45c`
- 细节规则：深胡狼头、砂色亚麻裙、金色胸饰和护腕，眼睛必须高对比，武器为青铜/金色弯刃。

## 动画约束
- 待机守卫姿态；行走长臂摆动；攻击时右手镰剑横扫；审判技能时头部稳定注视目标。

## 返工触发条件
- 看起来像普通僵尸
- 没有长吻/长耳
- 武器不可读或没有埃及金饰

## 验收口径
- 先验收概念设计图，再进入 Blockbench 建模。
- 必须提供正面、侧面、45 度三视图，且三视图来自已挂载贴图后的模型预览。
- 必须提供 UV 模板和贴图贴合报告，关键 UV 区域不得透明、空白或越界。
- 如三视图或贴合报告失败，优先修 .bbmodel、UV 或贴图。
