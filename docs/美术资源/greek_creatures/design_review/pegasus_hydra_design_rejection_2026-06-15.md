# Pegasus / Hydra 设计审查冻结记录（2026-06-15）

## 结论

当前已生成的 `pegasus` 和 `hydra` 不能继续作为建模、贴图、Blockbench 验收或 MC 验收依据。

原因不是单个参数偏差，而是设计阶段没有先固定正确造型，导致后续模型、贴图和截图都在错误轮廓上迭代。自动比例门和 UV 门只能证明文件存在、命名结构存在、贴图非空，不能证明生物设计正确。

## 事实依据

| 生物 | 已观察产物 | 设计问题 | 处理结论 |
| --- | --- | --- | --- |
| `pegasus` | `docs/美术资源/greek_creatures/concepts/pegasus_concept.png`、`docs/preview_assets/greek_creatures/pegasus/blockbench/side.png` | 旧概念图和模型都呈现竖直塔状头颈；头颈比例不接近原版马；飞行翼和骑乘空间没有建立在正确马体上。 | V2 设计拒绝；先按 V3 蓝图审查，不得继续直接调模型。 |
| `hydra` | `docs/美术资源/greek_creatures/concepts/hydra_concept.png`、`docs/preview_assets/greek_creatures/hydra/blockbench/side.png` | 旧设计仍偏大块头/大躯干；头、颈、肩部连接关系不成立；侧面读成竖直柱和长尾拼接。 | V2 设计拒绝；先按 V3 蓝图审查，不得继续直接调模型。 |

## 工作审视报告

### 原定目标

先完成希腊神话生物的设计包，再基于设计包生成 Blockbench、UV、贴图和预览。测试只验证设计落地，不负责一轮轮发明造型。

### 完成情况

- [x] 自动生成了设计包、Blockbench 预览、UV 和机械比例检查。
- [ ] 未完成正确设计确认：`pegasus` 和 `hydra` 的概念设计本身没有通过用户复审。
- [ ] 未完成流程阻断：旧 gate 仍把二者写成“修订完成待用户确认”，没有表达“设计未通过”。

### 发现的问题

| 严重程度 | 问题描述 | 根本原因 | 改进建议 |
| --- | --- | --- | --- |
| 必须改正 | `pegasus` 旧设计和预览是竖直塔状头颈，无法作为天马设计依据。 | 过早进入模型调参，用命名部件和机械比例代替真实轮廓审查。 | 以原版马比例为基础重写 V3 蓝图，用户确认前禁止建模。 |
| 必须改正 | `hydra` 旧设计没有稳定的头-颈-肩连接，身体与多头比例不成立。 | 多头结构只检查“存在”和局部 overlap，没有先设计正确的低伏蛇形身体与颈根拓扑。 | 重写 V3 蓝图，先固定头数、颈根、肩带、身体、尾巴比例，再建模。 |
| 必须改正 | 工具报告把错误产物标成 `revision_implemented_pending_user_acceptance`。 | 验收状态没有吸收用户复审结论，自动 gate 的语义过强。 | 新增设计先行 gate，并把二者标为 `design_rejected_requires_v3_blueprint_approval`。 |

### 下次重点关注

- 先审查设计图和蓝图，不直接动 `.bbmodel` 或 Java model。
- 自动检查只负责底线，不得把机械通过等同于造型正确。
- 对用户指出的畸形项，状态必须从“待确认”改为“明确不通过”。

## 新的阻断规则

- `pegasus` 只允许在 `docs/美术资源/greek_creatures/design_blueprints/pegasus_v3_blueprint.md` 被确认后重新建模。
- `hydra` 只允许在 `docs/美术资源/greek_creatures/design_blueprints/hydra_v3_blueprint.md` 被确认后重新建模。
- 当前 V2 `.bbmodel`、Java model、贴图和预览都只能作为反例材料，不可作为后续同步源。
