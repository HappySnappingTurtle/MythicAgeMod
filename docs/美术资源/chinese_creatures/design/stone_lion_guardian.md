---
creature_id: stone_lion_guardian
creature_name: Stone Lion Guardian / 石狮守卫
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

# Stone Lion Guardian / 石狮守卫 设计包

## 目标体量
- Entity ID: `mythic_age:stone_lion_guardian`
- 碰撞盒目标：`1.6w x 1.7h`
- 视觉高度目标：约 `1.85` 格
- 比例说明：神殿石狮，卷鬃、石质身体和爪下绣球必须可读。

## 轮廓语言
- 正面：比例合理的石狮头脸、卷鬃、宽胸、前爪绣球。
- 侧面：蹲坐石狮身和尾卷。
- 45 度：石尾和背部纹样。

## 概念设计图
- 文件：`docs/美术资源/chinese_creatures/concepts/stone_lion_guardian_concept.png`
- 用途：这是建模前的造型合同；Blockbench 和 MC 截图只验证是否实现该设计，不负责重新发明造型。
- 三视角：正面、侧面、45 度必须同时能识别该生物。

## V2 造型冻结
- 触发依据：用户实测反馈指出当前模型存在体量、精细度、纹理空白和神话特征不可读问题；测试只验证设计落地，不再承担造型试错。
- 正面必须读到神殿石狮的比例合理头脸、卷鬃、宽胸、厚前爪和爪下绣球；头本体不能和身体一样高，不得像普通大头狮子。
- 侧面必须体现蹲坐石狮体态、石质躯干、卷尾和厚重爪掌。
- 灰石纹、深色裂缝、金色眼点和红色绣球必须分区可读，绣球必须是独立模型部件。

## 核心识别特征
- 石狮
- 卷鬃
- 绣球
- 石质裂纹

## 部件清单
- `stone_body`
- `shoulder_plate`
- `stone_plinth`
- `head`
- `snout`
- `gold_eyes`
- `nose`
- `fangs`
- `curled_mane_top`
- `curled_mane_sides`
- `curled_mane_bottom`
- `mane_curls`
- `front_paws`
- `paw_pads`
- `claws`
- `back_legs`
- `embroidered_ball`
- `ball_ribbons`
- `ball_top_knot`
- `curled_tail`
- `tail_curl`

## Blockbench 层级
- `root/body`
- `root/head`
- `root/mane`
- `root/paws`
- `root/ball`
- `root/tail`

## 建模硬性要求
- curled_mane 必须拆成上/侧/下卷鬃和卷纹块
- ball 必须独立且有绣球带
- 前爪必须厚重并有爪尖
- tail 必须形成卷尾而不是普通尾巴

## UV 计划
- mane: carved curl blocks and dark groove lines
- body: grey stone cracks and shoulder/plinth layers
- face: gold eyes, dark nose and small fangs
- ball: red lacquer sphere with ribbon bands
- tail: curled stone tail with separated curl

## 纹理细节
- 主色：`#8f8a7a`
- 阴影：`#3f3b34`
- 强调色：`#b93a32`
- 细节规则：灰石纹、深色裂缝、分块卷鬃、金色眼点、深色鼻口、厚爪尖、红色绣球和卷尾必须分区清楚。

## 动画约束
- 低伏守卫；攻击时前爪压下；防御时身体稳定。

## 返工触发条件
- 像普通狮子
- 没有绣球
- 没有石纹

## 验收口径
- 先验收概念设计图，再进入 Blockbench 建模。
- 必须提供正面、侧面、45 度三视图，且三视图来自已挂载贴图后的模型预览。
- 必须提供 UV 模板和贴图贴合报告，关键 UV 区域不得透明、空白或越界。
- 如三视图或贴合报告失败，优先修 .bbmodel、UV 或贴图。
