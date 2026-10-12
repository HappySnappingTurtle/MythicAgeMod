# Pegasus / Hydra 当前设计正确性结论（2026-06-15）

## 结论

- 当前结论：`blocked_current_design_not_correct_and_v6_spec_not_approved`
- 是否允许建模：`False`
- 旧实现/当前 V2 不是正确设计，不能继续调模型尺寸当作修复。
- 当前 `.bbmodel` 和 Blockbench 预览只作为失败证据保留，不作为重建基础。
- 当前 Java runtime model 也不能作为同步目标：Pegasus runtime 属于错误视觉设计的实现，Hydra runtime 仍是五头。
- V3/V5 是方向材料；V6 已经转成可建模规格，但仍未被用户签收，不得作为建模许可。
- V6 规格包已把用户反馈转成 Blockbench 尺寸、层级、UV 和 runtime 重建门禁，但它仍不能自动放行建模。
- 自动比例 gate 通过不能替代设计正确性签收。

## 分项

| 生物 | 当前实现设计正确 | 候选可建模 | 最新候选设计判断 | 最新候选状态 | 当前问题 | 最新规格问题 | 下一步 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Pegasus / 天马 | `unknown_current_v2_assets_status` | `modeling_spec_ready_but_not_user_approved` | `modeling_spec_concrete_but_unapproved` | `modeling_spec_ready_for_user_review` | 旧实现/当前 V2 已畸形：马体比例不成立，头颈关系不稳定，不能继续调尺寸当作修复。 | 现有 V6 sheet 已经把方向转成可建模规格，但仍未被用户签收；未签收前不能生成 `.bbmodel`、UV、纹理或 runtime。 | `do_not_model_review_or_rewrite_v6_modeling_spec_first` |
| Hydra / 九头蛇 | `unknown_current_v2_assets_status` | `modeling_spec_ready_but_not_user_approved` | `modeling_spec_concrete_but_unapproved` | `modeling_spec_ready_for_user_review` | 旧实现/当前 V2 已畸形：头、颈、肩带和身体没有建立可靠实体连接。 | 现有 V6 sheet 已经把九头、socket 和连续颈路径转成可建模规格，但仍未被用户签收；未签收前不能生成 `.bbmodel`、UV、纹理或 runtime。 | `do_not_model_review_or_rewrite_v6_modeling_spec_first` |

## 必须先证明的设计点

### Pegasus / 天马

- 候选方向：先按原版成年马式横向马体成立白马，再加入双翼、自主飞行和骑乘飞行。
- 当前建模许可：`False`
- 审批失败项：`["design_status='modeling_spec_ready_for_user_review'", "design_sheet_status=None", "design_approval_status=None", "design_correctness_verdict=None", "design_detail_sheet_status=None", "design_detail_board_status=None", "modeling_allowed=false"]`
- V6 可建模规格合同：`docs/美术资源/greek_creatures/design_blueprints/pegasus_v6_modeling_spec_contract.md`
- 最新候选契约：`docs/美术资源/greek_creatures/design_blueprints/pegasus_v6_modeling_spec_contract.md`
- 最新候选设计图：`docs/preview_assets/greek_creatures/design_approval/v6/pegasus_v6_modeling_spec_sheet.png`
- 当前失败 `.bbmodel`：`docs/美术资源/blockbench/greek_creatures/pegasus.bbmodel`
- 当前失败预览：`docs/preview_assets/greek_creatures/pegasus/blockbench/side.png` / `docs/preview_assets/greek_creatures/pegasus/blockbench/three_quarter.png`
- 当前结构性结论：`rejected_visual_proportion_not_horse_like`
- Runtime 模型：`src/main/java/com/example/mythic_age/client/render/entity/model/PegasusModel.java`
- Runtime 结论：`blocked_runtime_model_is_implementation_of_rejected_visual_design`
- 本轮用户反馈：
  - 当前生成天马头部过大，不能继续用大头换取细节。
  - 天马必须先参考原版成年马体比例，再叠加纯白、双翼和骑乘飞行能力。
  - 当前实现看不出稳定马头/马颈/马身关系，必须回到设计阶段。
- 当前头宽/身体宽比例证据：`0.33`；这只能说明命名尺寸，不等于视觉上读成成年马。
- 当前命名部件缺失：`[]`
- Runtime 飞行动画标记：`True`；骑乘位标记：`True`。
- 当前视觉失败点：
  - 当前 Blockbench 侧面预览不能先读成成年白马；躯干、长颈、头和翼面像堆叠零件。
  - 头颈比例和锚点不稳定，头部可读性依赖局部放大/方块堆叠，而不是马式小头、细长吻部和斜向颈。
  - 翼面在视觉上压过马体结构，未证明肩后翼根、骑乘空间和头颈之间的正确空间关系。
- 建模前必须证明：
  - 侧面先读成白马，而不是白色方块或大头玩具马。
  - 头部细长且位于胸腔前方；吻部、眼、鼻孔、嘴线和耳由部件/UV 表达。
  - 颈部从鬐甲/肩胛斜向前上方伸出，不是竖直白柱。
  - 翼根在肩后，骑乘空间不遮挡头颈或翼根。
  - 设计图必须达到可建模精度，不能只用几何示意替代生物造型图。

### Hydra / 九头蛇

- 候选方向：低伏长身、默认九头扇形错层，每条颈从肩带 socket 生长并与头部重叠连接。
- 当前建模许可：`False`
- 审批失败项：`["design_status='modeling_spec_ready_for_user_review'", "design_sheet_status=None", "design_approval_status=None", "design_correctness_verdict=None", "design_detail_sheet_status=None", "design_detail_board_status=None", "modeling_allowed=false"]`
- V6 可建模规格合同：`docs/美术资源/greek_creatures/design_blueprints/hydra_v6_modeling_spec_contract.md`
- 最新候选契约：`docs/美术资源/greek_creatures/design_blueprints/hydra_v6_modeling_spec_contract.md`
- 最新候选设计图：`docs/preview_assets/greek_creatures/design_approval/v6/hydra_v6_modeling_spec_sheet.png`
- 当前失败 `.bbmodel`：`docs/美术资源/blockbench/greek_creatures/hydra.bbmodel`
- 当前失败预览：`docs/preview_assets/greek_creatures/hydra/blockbench/side.png` / `docs/preview_assets/greek_creatures/hydra/blockbench/three_quarter.png`
- 当前结构性结论：`rejected_wrong_head_count_and_connection_topology`
- Runtime 模型：`src/main/java/com/example/mythic_age/client/render/entity/model/HydraModel.java`
- Runtime 结论：`rejected_runtime_head_count_mismatch`
- 本轮用户反馈：
  - 当前九头蛇头和身体没有连接上，不能继续在旧结构上修补。
  - 当前实现头数和九头蛇设计不一致，五头 runtime/Blockbench 不能进入九头蛇流程。
  - 头部大小必须匹配身体比例，细节来自眼、嘴、颌、牙、角/毒腺部件和 UV，而不是放大头部。
- 当前 `.bbmodel` 头数：`5` / 设计要求 `9`。
- 当前头部命名：`["center_head", "left_head", "outer_left_head", "outer_right_head", "right_head"]`
- 当前 neck/body overlap 数：`5`；当前 neck/head overlap 数：`5`。
- Runtime 头数：`5` / 设计要求 `9`。
- Runtime 头部命名：`["outer_left_head", "left_head", "center_head", "right_head", "outer_right_head"]`
- 当前视觉失败点：
  - 当前 Blockbench 预览不是九头扇形蛇怪，而是五头/头墙式箱体堆叠。
  - 侧面没有证明每条颈从 shoulder_saddle 长出并连接到头，头颈身体连续性失败。
  - 压迫感来自臃肿大块和大头堆叠，不是低伏长身、长尾、九头错层和单头面部细节。
- 建模前必须证明：
  - 默认九头方案在设计阶段确定；如果要改五头，必须先改设计包。
  - 九个 neck_socket 位于 shoulder_saddle，且每个 neck_root 与 socket 几何重叠。
  - 每条颈至少 neck_root/neck_mid/neck_tip 三段，neck_tip 与 head_core 重叠。
  - 身体低而长，头部不靠放大获得细节，每个头有眼、颚、嘴线、牙或毒腺。
  - 设计图必须显示头颈身体的连续体积，不允许只有拓扑命名或分离柱状颈。

## 硬性门禁

只有同时满足以下条件，才允许重新生成 `.bbmodel`、纹理、动作或同步 Java model：

- `design_status=approved_for_modeling`
- `design_sheet_status=approved_for_modeling`
- `design_approval_status=approved_for_modeling`
- `design_correctness_verdict=approved_for_modeling`
- `design_detail_sheet_status=approved_for_modeling`
- `design_detail_board_status=approved_for_modeling`
- `modeling_allowed=true`
- `tools/greek_design_first_gate.py audit` 与本 gate 均通过

## 工作审视报告

### 原定目标

按设计先行方式确认天马和九头蛇是否正确，再决定是否进入建模。

### 完成情况

- [x] 已确认旧天马、旧九头蛇均不是正确设计。
- [x] 已确认 V6 当前只是可审查规格，不允许直接开工建模。
- [x] 已把机械比例 gate 与设计正确性 gate 区分开。
- [ ] 未完成最终设计签收；需要用户确认或要求重画设计图。

### 发现的问题

| 严重程度 | 问题描述 | 根本原因 | 改进建议 |
| --- | --- | --- | --- |
| 必须改正 | 天马旧实现没有先成立原版马式身体和头颈关系，导致头大/看不出头这两类问题反复出现。 | 过早从模型调参进入实现，没有先锁定侧面马体比例。 | 先签收马体比例、头颈锚点、翼根和骑乘空间，再建模。 |
| 必须改正 | 九头蛇旧实现没有先锁定九个 socket、颈段和头颈重叠关系，导致头和身体脱节。 | 多头拓扑没有在设计阶段拆到可建模粒度。 | 先签收九头拓扑、socket 排布和单头连接放大规则，再建模。 |
| 必须改正 | 当前 Hydra `.bbmodel` 只有 5 个 `_head` 部件，却被放进九头蛇流程。 | 设计审批未通过时仍推进了实现产物，流程 gate 没有挡在最前。 | 在本 gate 未通过前，禁止把当前 `.bbmodel`、贴图和 Java runtime 作为后续依据。 |
| 必须改正 | 当前 Pegasus `.bbmodel` 虽有头、吻部、眼、翼等命名部件，但预览不成立为成年马式天马。 | 把“有部件名”误当成“视觉造型正确”。 | 必须用三视图设计图先证明马体、头颈、翼根、骑乘位，再重建模型。 |
| 必须改正 | V6 规格仍未签收，不能当作自动建模许可。 | 过去把候选图误当作可生产依据。 | 只有用户确认 V6 后，才能写回 `approved_for_modeling` 并开始重建。 |
| 必须改正 | 之前的自动比例检查容易被误读为“设计正确”。 | 把机械阈值通过当成造型审批。 | 本 gate 必须作为 Pegasus/Hydra 的前置发布依据。 |

### 下次重点关注

- 不再基于当前 V2 `.bbmodel` 或 Java model 继续微调天马/九头蛇。
- 不用放大头部换取眼嘴可读性，细节必须来自部件和 UV。
- 设计图不签收，就不进入 Blockbench。
