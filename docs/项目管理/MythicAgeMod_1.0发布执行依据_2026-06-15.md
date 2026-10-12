# MythicAgeMod 1.0 发布执行依据（2026-06-15）

本文档是 2026-06-15 之后的执行依据，取代旧文档中关于 NBT P0、音频替换数和生物头身比例的过时判断。当前状态以 `docs/preview_assets/1_0_gap_audit/gap_report.json` 和 dashboard 为准。

补充主依据：`docs/项目管理/MythicAgeMod_1.0重整开发总纲_2026-06-15.md`。该文档明确区分“工程骨架存在”和“1.0 可发布完成”，并将 `pegasus`、`hydra` 设计拒绝列为前置 P0。

执行台账：`docs/项目管理/MythicAgeMod_1.0开发任务台账_2026-06-15.md`。后续开发以该台账拆分任务、依赖和退出标准。

模块范围矩阵：`docs/项目管理/MythicAgeMod_1.0模块范围与缺口矩阵_2026-06-15.md`。后续任何“1.0 已完成/某模块已完成”的判断，都必须同时对照该矩阵和 `gap_report.json`，不能只看单一静态 gate。

## 当前结论

- 1.0 仍为 `NOT READY`，当前 release blocking gaps 为 4 个。
- 五文明 30 个生物均有设计包、Blockbench/UV/纹理/preview、runtime model 和视觉 review board。
- 全生物设计先行门禁已新增：30 个生物当前 `approved_for_modeling=0/30`，其中 `pegasus`/`hydra` 为设计拒绝阻断，其他 28 个为“有生成型设计包但未人工签收”。后续不能把设计包存在等同于设计通过。
- 生物头身比例与“细节优先”自动门已通过 30/30；runtime Java 同步通过 30/30。
- 2026-06-15 用户复审新增结论：`pegasus` 和 `hydra` 当前 V2 设计已被拒绝。自动比例/UV/runtime gate 的通过只代表机械检查通过，不代表造型设计正确；二者必须先通过 V6 可建模规格合同、规格图、设计正确性 gate 和人工签收，才允许重新建模。
- 2026-06-15 设计复核补充：旧 `pegasus`、旧 `hydra` 不能作为动作、贴图、Java runtime 或 Blockbench 继续迭代基线；动作队列必须读取 `creature_motion_rework_queue.json`，其中设计阻断项先等 V6 规格签收和模型重建。
- NBT 发布验收包和真实重启周期已通过，后续作为回归 gate 保留，不再是当前 P0 blocker。
- NBT 状态所有权队列已生成：21 项长期状态 covered，48 项 runtime-only 决策，required/missing 为 0；后续新增长期玩法必须先改目标表再实现 schema/probe。
- 音频 replacement required=0，但 21 个 final-mix candidate creature 与 149 个音频事件仍需最终听感验收。

## 本次比例纠偏

用户指出部分模型存在“大头习惯”，这是正确反馈。后续所有生物必须遵守：

- 头部必须匹配身体比例，不得通过放大头块弥补识别度。
- 缩小头部时不能损失细节精度；眼、嘴、鼻/吻、耳、角、鬃毛、蛇发、翼、尾、武器和符号必须由独立部件或固定 UV 保证。
- Blockbench/设计管线和 runtime Java 模型必须同时通过比例门；只修其中一端不算完成。
- 四足/马形生物必须检查头颈前伸结构，不能出现竖直塔状头颈；多头生物必须检查 head/neck/body overlap 几何相交，不能只靠命名或贴图暗示连接。

已修正并重新通过比例门的重点项：

| 文明 | 生物 | runtime h/w | 说明 |
| --- | --- | --- | --- |
| Greek | `minotaur` | `0.6375/0.7250` | 缩小基础牛头，保留牛吻、角、耳、眼、鼻孔和嘴线。 |
| Greek | `medusa` | `0.6404/0.7262` | 缩小基础人脸，保留石化眼、瞳孔、嘴、鼻梁、耳和六条蛇发。 |
| Egyptian | `mummy_soldier` | `0.6667/0.7000` | 缩小缠布头块，保留眼洞、绷带条、嘴影和短矛。 |
| Chinese | `jiangshi` | `0.6667/0.7381` | 缩小基础头和官帽体积，保留符箓、青白脸、眼口和前伸袖臂。 |
| Nordic | `dwarf_berserker` | `0.6263/0.7264` | 缩小基础头盔脸，保留胡须、鼻眼口、圆盔、双辫和双斧。 |
| Greek | `pegasus` | design rejected | 用户复审判定当前天马已畸形；旧概念图和 Blockbench 预览为竖直塔状头颈，不能继续作为建模依据。必须先确认 `docs/美术资源/greek_creatures/design_blueprints/pegasus_v6_modeling_spec_contract.md`、`docs/preview_assets/greek_creatures/design_approval/v6/pegasus_v6_modeling_spec_sheet.png` 和 `docs/preview_assets/greek_creatures/design_approval/v6/pegasus_hydra_v6_modeling_spec_package.json`。 |
| Greek | `hydra` | design rejected | 用户复审判定当前九头蛇已畸形；旧概念图和 Blockbench 预览没有建立正确头-颈-肩-身体连接，当前 `.bbmodel`/runtime 仍是旧五头依据，不能继续作为建模依据。必须先确认 `docs/美术资源/greek_creatures/design_blueprints/hydra_v6_modeling_spec_contract.md`、`docs/preview_assets/greek_creatures/design_approval/v6/hydra_v6_modeling_spec_sheet.png` 和 `docs/preview_assets/greek_creatures/design_approval/v6/pegasus_hydra_v6_modeling_spec_package.json`。 |

## 2026-06-15 设计拒绝阻断

- 冻结记录：`docs/美术资源/greek_creatures/design_review/pegasus_hydra_design_rejection_2026-06-15.md`
- 设计正确性审查：`docs/美术资源/greek_creatures/design_review/pegasus_hydra_design_correctness_audit_2026-06-15.md`
- 当前设计正确性 gate：`docs/美术资源/greek_creatures/design_review/pegasus_hydra_current_design_verdict_2026-06-15.md`
- V6 可建模规格包：`docs/preview_assets/greek_creatures/design_approval/v6/pegasus_hydra_v6_modeling_spec_package.json`
- V6 规格说明：`docs/美术资源/greek_creatures/design_review/pegasus_hydra_v6_modeling_spec_package_2026-06-15.md`
- V6 规格图：`docs/preview_assets/greek_creatures/design_approval/v6/pegasus_v6_modeling_spec_sheet.png`、`docs/preview_assets/greek_creatures/design_approval/v6/hydra_v6_modeling_spec_sheet.png`
- V6 规格合同：`docs/美术资源/greek_creatures/design_blueprints/pegasus_v6_modeling_spec_contract.md`、`docs/美术资源/greek_creatures/design_blueprints/hydra_v6_modeling_spec_contract.md`
- 30 生物美术重做队列：`docs/项目管理/MythicAgeMod_1.0生物美术重做队列_2026-06-15.md`
- 全生物设计先行门禁：`docs/美术资源/entity_validation/creature_design_first_gate_2026-06-15.md`
- 30 生物动作/动画重做队列：`docs/美术资源/entity_validation/creature_motion_rework_queue_2026-06-15.md`
- 前置 gate：`tools/greek_design_first_gate.py audit`
- 当前阻断状态：
  - `pegasus`: 当前实现 rejected；V6 规格包 `status=modeling_spec_ready_for_user_review`，`modeling_allowed=false`
  - `hydra`: 当前实现 rejected；V6 规格包 `status=modeling_spec_ready_for_user_review`，`modeling_allowed=false`
- 在上述状态全部变为 `approved_for_modeling` 且 `modeling_allowed=true` 前，不允许重新生成 `.bbmodel`、同步 Java model 或把旧 V2 预览标成修复完成。

## 剩余发布阻塞

| 优先级 | 阻塞项 | 当前证据 | 退出标准 |
| --- | --- | --- | --- |
| P0 | 生物设计签收和人工视觉验收未完成 | 30 个 review board 已生成，但 design-first approval=0/30，human visual acceptance=false | 30 个生物先完成设计签收，再逐个给出人工通过/返修结论，重点检查比例、神话特征、眼嘴和纹理空白。 |
| P1 | 生物动作观感验收未完成 | motion capture queue=138，计划 16 批，最低 1020 帧；动作重做队列会把设计/美术阻断项先冻结 | idle/locomotion/attack/special/hurt/death/fly/riding 等必需模式均有可读截图或序列结论，且设计阻断生物必须先完成 V6 规格签收和模型重建。 |
| P1 | 生物音频最终听感未完成 | 149 事件，15 批，140.172 秒；final-mix candidate=21 | OGG 技术指标、响度工具或人工听感验收通过，`final_mix_acceptance_ok=true`。 |
| P1 | 52 个神力玩法验收未完成 | 8 批，最低 446 场景；high risk=18 | 目标选择、失败路径、冷却、消耗、性能、粒子可读性和战斗影响逐项通过。 |

## 模块完成范围

- 代码与玩法：五文明证书经济、守护者交易、神力解锁、神力失败路径和配置项必须形成闭环。
- 模型：30 个生物不再允许“大头补细节”，必须靠部件层级和 UV 细节表达神话身份。
- 动作：静态 `setAngles` 不等于完成，必须完成动作观感验收。
- 纹理：禁止随机面板纹理作为最终资源，眼、嘴、角、符号、翼纹、尾端必须落在明确 UV 区。
- UI：守护者交易和神力图鉴已过静态 gate，但仍需真实分辨率、长文本和文明主题检查。
- 粒子：52/52 神力粒子矩阵已过静态 gate，剩余是实机可读性和压力/降级验收。
- NBT：当前通过；新增长期状态必须先补 schema、probe 和真实重启回归。

## 执行顺序

1. 先做 30 生物人工视觉验收，重点确认头身比例和细节精度。
2. 失败项回到对应设计包、Blockbench、纹理或 runtime Java 模型；不能用测试阶段临时改造型。
3. 进行 16 批动作捕捉验收。
4. 进行 15 批音频听感验收，并补齐响度工具或等价 MCP 能力。
5. 进行 8 批神力玩法验收。
6. 每批后重新运行 gap audit；只有 release blocking gaps=0 且 `./gradlew build --offline` 通过，才进入 1.0 发布整理。

## 回归命令

```bash
/Users/hongyuwu/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3 -B tools/creature_proportion_quality_gate.py audit
/Users/hongyuwu/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3 -B tools/creature_runtime_proportion_quality_gate.py audit
/Users/hongyuwu/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3 -B tools/pegasus_hydra_v6_modeling_spec_package.py audit
/Users/hongyuwu/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3 -B tools/pegasus_hydra_design_correctness_gate.py audit
/Users/hongyuwu/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3 -B tools/creature_art_rework_queue.py audit
/Users/hongyuwu/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3 -B tools/creature_design_first_gate.py audit
/Users/hongyuwu/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3 -B tools/greek_design_first_gate.py audit
/Users/hongyuwu/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3 -B tools/greek_design_approval_package.py audit
/Users/hongyuwu/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3 -B tools/creature_visual_acceptance_package.py audit
/Users/hongyuwu/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3 -B tools/creature_action_acceptance_package.py audit
/Users/hongyuwu/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3 -B tools/creature_motion_rework_queue.py audit
/Users/hongyuwu/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3 -B tools/god_power_gameplay_acceptance_package.py audit
/Users/hongyuwu/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3 -B tools/creature_audio_final_mix_acceptance_package.py audit
/Users/hongyuwu/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3 -B tools/nbt_release_acceptance_package.py audit
/Users/hongyuwu/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3 -B tools/nbt_state_ownership_queue.py audit
/Users/hongyuwu/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3 -B tools/mythic_age_1_0_gap_audit.py audit
/Users/hongyuwu/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3 -B tools/mythic_age_1_0_scope_matrix.py audit
./gradlew build --offline
```
