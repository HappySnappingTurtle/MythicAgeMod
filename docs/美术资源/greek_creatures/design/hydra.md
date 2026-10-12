---
creature_id: hydra
creature_name: Hydra / 九头蛇
design_status: needs_user_review
design_sheet_status: v3_candidate_needs_user_review
design_approval_status: v3_approval_pending
design_correctness_verdict: current_design_rejected_v3_pending
design_detail_sheet_status: v3_topology_lock_pending
design_detail_board_status: v3_detail_board_pending
modeling_allowed: false
design_package_status: generated_reference_needs_human_approval
approval_basis: human_design_review_required_before_model_uv_texture_animation_or_runtime_sync
current_v2_assets_status: rejected
blocking_reason: current Hydra model/design lacks valid head-neck-shoulder-body topology
v3_blueprint: docs/美术资源/greek_creatures/design_blueprints/hydra_v3_blueprint.md
---

# Hydra / 九头蛇 设计包

## 目标体量
- Entity ID: `mythic_age:hydra`
- 碰撞盒目标：`3.2w x 2.8h`
- 视觉高度目标：约 `3.0` 格
- 比例说明：V3 当前未批准建模；目标是低伏长身、九头扇形错层和真实头-颈-肩连接，旧五头 V2 不再作为 1.0 建模依据。

## 轮廓语言
- 正面：V3 默认 9 个可读蛇头扇形展开，中央头最高，两侧头错层；如改 5 个主战头必须先做设计变更。
- 侧面：瘦长低伏身体、长尾、多个蛇颈从肩部前探，不能身子大头小。
- 45 度：背部鳞片、尾巴和 9 个颈部根部/错层颈影清楚分离，不糊成一团。

## 当前设计状态
- 状态：`rejected_v2_reference_only`。
- 当前 V2/旧五头实现已被用户判定畸形，不能作为 `.bbmodel`、Java model、贴图、动作或预览的继续建模依据。
- 可建模依据必须改用 `docs/美术资源/greek_creatures/design_blueprints/hydra_v3_blueprint.md`，并等待九头拓扑锁定表、细化设计板和设计正确性 gate 全部批准。

## 概念设计图
- 文件：`docs/美术资源/greek_creatures/concepts/hydra_concept.png`
- 用途：这是建模前的造型合同；Blockbench 和 MC 截图只验证是否实现该设计，不负责重新发明造型。
- 三视角：正面、侧面、45 度必须同时能识别该生物。

## V2 造型冻结
- 触发依据：用户实测反馈指出当前模型存在体量、精细度、纹理空白和神话特征不可读问题；测试只验证设计落地，不再承担造型试错。
- 旧五头 V2 和当前实现已被设计拒绝，不能继续作为 .bbmodel、Java model、贴图、动作或预览依据。
- V3 必须先经九头拓扑锁定表、细化设计板和用户签收；未签收前不得按本流水线继续生成最终模型。
- 正面必须先读到九个比例合理的蛇头与长颈扇形；压迫感来自数量、错层和颈部姿态，不来自头部异常放大。
- 九个头的脸部细节必须可审计：眼睛、上下颚、嘴线、獠牙、角或毒腺都不得只靠随机纹理暗示；每条颈根必须有 neck_root/body_overlap 与 shoulder_saddle 接触，每个头根必须有 head_overlap 与颈段接触。

## 核心识别特征
- V3 默认显性 9 头作为九头蛇识别基础；每个头必须比例合理，细节靠独立部件和固定 UV 保证
- 身体瘦长低伏，不能用一个臃肿大箱体撑满碰撞盒
- 每个头具备眼睛、上下颚、嘴线、獠牙和小角/毒腺
- 背部脊鳞从肩到尾形成沼泽毒兽轮廓
- 九条长颈从肩部 socket 扇形展开，每条颈根必须通过 neck_root/body_overlap 连接到 shoulder_saddle，每个头根必须通过 head_overlap 连到对应颈段，不能悬空、糊成一根粗柱或只剩中心头

## 部件清单
- `slender_body`
- `shoulder_saddle`
- `neck_bridge_sockets`
- `neck_anchor_blocks`
- `spine_crests`
- `long_tail`
- `tail_tip`
- `nine_neck_sockets`
- `nine_independent_necks`
- `nine_neck_body_overlap_segments`
- `nine_neck_head_overlap_segments`
- `nine_proportional_heads`
- `head_brows`
- `paired_eyes`
- `upper_jaws`
- `lower_jaws`
- `mouth_lines`
- `fangs`
- `horns`
- `poison_glands`
- `legs`
- `paws`

## Blockbench 层级
- `root/body`
- `root/body/tail`
- `root/body/spine`
- `root/necks`
- `root/necks/heads`
- `root/heads/face_details`
- `root/legs`

## 建模硬性要求
- 9 条 neck、9 个 neck_socket、9 个 body_overlap、9 个 head_overlap 和 9 个 head 必须是独立可审计结构，运行模型中头颈不得与身体断开
- 头部宽度必须与身体、颈部和整体视觉高度匹配；中央头只允许略大，不能用大头代替细节
- body 高度压低，尾巴拉长，正面以头扇形而非躯干方块为主视觉
- 每个 head 下必须有眼、上颚、下颚、牙、角/毒腺的明确子结构或同名 UV 区

## UV 计划
- each_head_front: proportional head plane, paired yellow eyes, brow band, dark mouth line, white fangs, horns and poison glands
- neck: repeated scale bands and bright toxic throat ridge
- body_side: low slender swamp scales with shoulder saddle and visible neck_bridge sockets under every neck root
- tail_tip: dark taper and scale bands
- spine_crests: brighter toxic ridge plates from shoulder to tail

## 纹理细节
- 主色：`#234a2e`
- 阴影：`#122317`
- 强调色：`#78b356`
- 细节规则：深绿鳞片，头部眼睛亮黄，口腔深色、獠牙白色、角/毒腺浅绿；每个头正面必须有眼睛、上下颚、嘴线、獠牙、角和毒腺。身体是低伏瘦长鳞片，不允许大块无细节深绿箱体。

## 动画约束
- 各头轻微错相摆动；行走时身体低伏；特殊攻击时中央头前探。

## 返工触发条件
- 只有 3 个头或头部太小
- 头部过大、压倒身体比例，或用大头替代眼/嘴/颌/角等细节
- 身体像圆胖蜥蜴，正面主要看到大躯干
- 眼睛、嘴、獠牙缺失或贴图错位
- 九头缺少上下颚/角/毒腺结构，导致像普通方块蛇头；或未经设计变更退回五头简化

## 验收口径
- 先验收概念设计图，再进入 Blockbench 建模。
- 必须提供正面、侧面、45 度三视图，且三视图来自已挂载贴图后的模型预览。
- 必须提供 UV 模板和贴图贴合报告，关键 UV 区域不得透明、空白或越界。
- 如三视图或贴合报告失败，优先修 .bbmodel、UV 或贴图。
