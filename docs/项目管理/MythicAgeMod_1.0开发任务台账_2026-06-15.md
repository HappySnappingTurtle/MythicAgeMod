# MythicAgeMod 1.0 开发任务台账（2026-06-15）

生成时间：2026-06-15 17:35 CST

本文档是 `MythicAgeMod_1.0重整开发总纲_2026-06-15.md` 的执行台账。它不表示 1.0 已完成，只用于把当前缺口拆成可执行、可验收、可回归的任务。

## 0. 总状态

- 当前版本状态：`NOT READY`
- 当前 release blocking gaps：4
- 当前 1.0 定义：五文明基础可玩版本，不是“文件齐全版本”
- 当前最大 P0：生物视觉和设计正确性，尤其是 `pegasus`、`hydra`
- 当前全生物设计先行状态：`approved_for_modeling=0/30`；28 个有设计包但未人工签收，2 个设计拒绝阻断
- 当前 NBT 状态：发布验收包和真实重启周期通过；新增长期状态必须继续补 schema、probe 和真实重启回归

## 1. 状态定义

| 状态 | 含义 |
| --- | --- |
| `blocked_by_design_review` | 设计未签收，禁止建模、贴图、动作和 Java 同步。 |
| `planned` | 已确认要做，但尚未进入实现。 |
| `in_progress` | 已进入实现或验收材料准备。 |
| `static_pass_runtime_pending` | 静态 gate 通过，但缺实机/人工验收。 |
| `manual_acceptance_pending` | 材料齐全，但还需要人工判定美术、动作、听感或玩法。 |
| `verified` | 代码、资源、人工验收和回归命令均通过。 |

## 2. P0 任务：设计与视觉基线

| ID | 任务 | 当前证据 | 依赖 | 退出标准 |
| --- | --- | --- | --- | --- |
| P0-ART-001 | 审查并签收天马 V6 可建模规格 | `pegasus` 旧设计已拒绝；当前实现不能作为造型依据；V6 候选依据为 `pegasus_v6_modeling_spec_contract.md`、`pegasus_v6_modeling_spec_sheet.png`、`pegasus_hydra_v6_modeling_spec_package.json` 和当前设计正确性 gate | 用户确认原版成年马式横向身体、非大头细长马头、二段斜向颈、肩后翼根、骑乘空间和纯白结构阴影 | `design_status=approved_for_modeling`、`design_correctness_verdict=approved_for_modeling`、`v6_modeling_spec_status=approved_for_modeling`、`modeling_allowed=true` |
| P0-ART-002 | 审查并签收九头蛇 V6 可建模规格 | `hydra` 旧设计已拒绝；当前 `.bbmodel`/runtime 为旧五头依据，不能作为 1.0 造型依据；V6 候选依据为 `hydra_v6_modeling_spec_contract.md`、`hydra_v6_modeling_spec_sheet.png`、`pegasus_hydra_v6_modeling_spec_package.json` 和当前设计正确性 gate | 用户确认默认九头、九个 socket、头-颈-肩实体连接、低伏长身和非大头细节表达 | `design_status=approved_for_modeling`、`design_correctness_verdict=approved_for_modeling`、`v6_modeling_spec_status=approved_for_modeling`、`modeling_allowed=true` |
| P0-ART-003 | 重建天马模型、UV、纹理和 runtime Java | 现有 V2 不可复用为造型依据 | P0-ART-001 | Blockbench 三视角和 runtime Java 均读成纯白马体、细长马头、分层翅膀、自主飞行和骑乘飞行 |
| P0-ART-004 | 重建九头蛇模型、UV、纹理和 runtime Java | 现有 V2 不可复用为造型依据 | P0-ART-002 | 九头扇形、低伏长身、每条颈根实体连接、每个头有眼嘴牙毒性细节 |
| P0-ART-005 | 修复 Minotaur 视觉质量合同阻断 | 已修复并验证：`visual_quality_contract_fail_count=0`，Minotaur `greek_mechanical_contract_ok=true`，牛头含角宽度 `15.5` > torso `12.0` | 已完成 | `creature_visual_acceptance_package.py audit` 不再报告 Minotaur quality blocker；仍需人工视觉验收 |
| P0-ART-006 | Greek 10 生物人工视觉复审 | Cyclops 为“修订待确认”，Pegasus/Hydra 设计拒绝，其余希腊项需人工复审 | P0-ART-001 至 P0-ART-005 | Greek 10 全部有“通过/返修/重写设计”的明确结论 |
| P0-ART-007 | 30 生物全量人工视觉复审 | 30 个 review board 已生成；新增 `human_visual_acceptance_ledger.json` 和 `creature_human_visual_acceptance_ledger.md` 作为人工判定台账；默认无任何生物自动通过 | Greek 复审先稳定 | 30 生物人工视觉台账全部为 `approved`，且 checklist 无 `pending` / `fail` |
| P0-ART-008 | 维护 30 生物美术重做队列 | `creature_art_rework_queue.json` 当前将 30 个生物拆为 P0/P1：设计阻断 2、待用户接受 1、希腊复审 7、非希腊升级 20 | P0-ART-001 至 P0-ART-007 | `creature_art_rework_queue.py audit` 通过，且所有队列项退出 gate 均满足 |
| P0-ART-009 | 维护 30 生物设计先行签收门禁 | `creature_design_first_gate.json` 当前为 `approved_for_modeling=0/30`、设计阻断 2、人工签收待办 28 | 所有生物设计包、概念图和人工审查 | `creature_design_first_gate.py audit` 通过；30 个生物均有正式 `approved_for_modeling` 状态后才允许将模型/纹理/动作/runtime 证据计为 1.0 完成 |

### P0 设计纪律

- 禁止用测试阶段临时改造型。
- 禁止从现有 Java model 盲目反推设计。
- 禁止用大头弥补眼嘴细节。
- 白色、深色或大面积纯色材质必须有结构阴影和固定 UV 细节。

## 3. P1 任务：非希腊 20 生物美术升级

| ID | 文明 | 当前问题 | 主要升级点 | 退出标准 |
| --- | --- | --- | --- | --- |
| P1-ART-EGY-001 | Egyptian | 工程首版感强，文明材质不够 | 胡狼头、圣甲虫甲壳、木乃伊绷带、狮身人面、太阳猎鹰羽翼 | 每个生物有设计蓝图、UV、最终纹理、三视角和人工通过 |
| P1-ART-NOR-001 | Nordic | 轮廓和武器/盔甲细节不足 | 霜巨人冰霜体量、亡灵战士、女武神、芬里尔、矮人武器盔甲 | 每个生物有设计蓝图、UV、最终纹理、三视角和人工通过 |
| P1-ART-CHI-001 | Chinese | 标志结构需要高精度重做 | 青龙分段龙身、麒麟角鬃、僵尸符箓、九尾狐九尾、石狮卷鬃 | 每个生物有设计蓝图、UV、最终纹理、三视角和人工通过 |
| P1-ART-ATL-001 | Atlantean | 水晶/海洋材质和轮廓层次不足 | 水晶利维坦、山铜魔像、海妖祭司、潮汐守卫、深渊鳐 | 每个生物有设计蓝图、UV、最终纹理、三视角和人工通过 |

## 4. P1 任务：动作与动画

| ID | 任务 | 当前证据 | 依赖 | 退出标准 |
| --- | --- | --- | --- | --- |
| P1-ACT-001 | 完成 138 项缺失动作序列 | `MC required motion modes covered=30/168`，缺 138 项 | P0 视觉稳定，尤其 Pegasus/Hydra | `creature_human_motion_acceptance_ok=true` |
| P1-ACT-002 | 执行 16 批动作捕获 | 当前计划 16 批，最低 1020 帧 | MC 截图/探针流程可用 | 每批有连续帧、触发条件和人工结论 |
| P1-ACT-003 | 天马自主飞行和骑乘飞行动作 | 当前模型设计拒绝，动作不能沿用 | P0-ART-003 | 自主飞行、骑乘飞行、翼拍、骑乘点稳定均可读 |
| P1-ACT-004 | 九头蛇九头错相动作 | 当前模型设计拒绝，旧五头动作不能沿用 | P0-ART-004 | 每条颈和头在 idle/attack/hurt 中保持实体连接；旧五头动作证据不作为 1.0 依据 |
| P1-ACT-005 | 维护动作重做队列与美术/设计依赖 | `creature_motion_rework_queue.json` 当前汇总 138 项待捕获动作，设计/美术阻断项单独标记 | P0/P1 美术队列 | `creature_motion_rework_queue.py audit` 通过，且所有动作任务都有连续帧、manifest 和人工 motion review note |

## 5. P1 任务：代码与玩法

| ID | 任务 | 当前证据 | 依赖 | 退出标准 |
| --- | --- | --- | --- | --- |
| P1-GP-001 | 52 个神力人工玩法验收 | 静态合同通过；52 项 manual playtest；最低 446 场景 | 可启动 MC 测试环境 | `god_power_gameplay_acceptance_ok=true` |
| P1-GP-002 | 优先验收 18 个高风险神力 | 大范围世界编辑、爆炸、天气/持续管理类风险最高 | P1-GP-001 队列 | 高风险项有性能、失败路径和回滚/降级结论 |
| P1-CODE-001 | 生物 AI、掉落、生成和召唤入口复查 | 30 生物已注册，但不等于玩法完成 | 视觉和动作稳定后复查 | 每个生物有生成来源、战斗定位、掉落或召唤入口说明 |
| P1-CERT-001 | 证书经济实机平衡 | 静态链路通过，频率/防刷/价格未最终验收 | 神殿、守护者、UI 均稳定 | 获取、消耗、冷却、交易、防刷跨存档闭环通过 |

## 6. P1 任务：粒子与特效

| ID | 任务 | 当前证据 | 依赖 | 退出标准 |
| --- | --- | --- | --- | --- |
| P1-FX-001 | 52 神力三段式粒子验收 | 粒子矩阵静态通过，实机可读性未完成 | P1-GP-001 | 每个神力有 cast、active/impact、end 三段式结论 |
| P1-FX-002 | 高风险神力压力和降级验证 | 大范围效果可能影响性能 | P1-GP-002 | 粒子数量、范围、降级策略和帧率影响有记录 |
| P1-FX-003 | 生物技能粒子绑定 | 生物技能粒子未逐项最终审查 | P0/P1 生物视觉动作稳定 | 粒子不遮挡眼睛、嘴、武器、翼根、尾刺、多头 |

## 7. P1 任务：音效

| ID | 任务 | 当前证据 | 依赖 | 退出标准 |
| --- | --- | --- | --- | --- |
| P1-AUD-001 | 完成 149 个生物音频事件人工听感 | 技术可播放，人工听感未完成 | 可试听索引和 review board | `creature_audio_final_mix_acceptance_ok=true` |
| P1-AUD-002 | 处理 21 个 final-mix candidate | 21 个候选生物仍需最终确认 | P1-AUD-001 | 候选资源通过响度/循环/战斗可读性判断 |
| P1-AUD-003 | 补齐或替代响度工具链 | 缺 `ffprobe`、`ffmpeg`、`soxi`、`ogginfo` | 本机工具或等价 MCP | 音量、峰值、循环和容器指标有可复验报告 |
| P1-AUD-004 | 神力和 UI 音效审计 | 当前重点偏生物音频 | 神力/UI 稳定 | cast、impact、end、交易成功/失败、证书提示都有音效结论 |

## 8. P1/P2 任务：UI 与玩家引导

| ID | 任务 | 当前证据 | 依赖 | 退出标准 |
| --- | --- | --- | --- | --- |
| P1-UI-001 | 守护者交易 UI 实机验收 | 静态 UI gate 通过 | 证书经济稳定 | 显示文明、余额、价格、材料、冷却和失败原因 |
| P1-UI-002 | 神力图鉴 UI 实机验收 | 静态 UI gate 通过 | 神力玩法稳定 | 显示文明、效果、冷却、消耗、获得方式、风险和状态 |
| P1-UI-003 | 分辨率、GUI scale、长文本、多语言验收 | 未最终验收 | UI 文案冻结 | 不同 GUI scale 和语言下不重叠、不截断 |
| P2-DOC-001 | 玩家指南和发布说明 | README 已降级为开发快照，仍需 1.0 玩家指南 | 玩法和 UI 稳定 | README、CHANGELOG、玩家指南与当前版本一致 |

## 9. P1/P2 任务：结构、世界生成与建筑美术

| ID | 任务 | 当前证据 | 依赖 | 退出标准 |
| --- | --- | --- | --- | --- |
| P1-STRUCT-001 | 15 个 canonical 神殿建筑审美验收 | NBT 文件静态通过 | 可实机加载结构 | small/medium/large 在地形中可读，文明差异明确 |
| P1-STRUCT-002 | 神殿功能点位验收 | 结构文件存在不等于功能可读 | 证书、守护者、UI 稳定 | 祭坛、守护者、入口、战斗空间和奖励点位清楚 |
| P2-WORLD-001 | 世界生成频率和平衡 | 静态处理器存在，实机分布未最终验收 | 结构和证书经济稳定 | 五文明结构分布、稀有度和玩家进程匹配 |

## 10. P1 任务：NBT 与持久化

| ID | 任务 | 当前证据 | 依赖 | 退出标准 |
| --- | --- | --- | --- | --- |
| P1-NBT-001 | 保持 NBT 真实重启回归 | NBT 发布验收包当前通过 | 每次新增持久化状态后重跑 | `nbt_release_acceptance_package.py audit` 通过 |
| P1-NBT-002 | 维护 NBT 状态所有权队列 | `nbt_state_ownership_queue.json` 当前为 21 covered / 48 runtime_only / 0 required / 0 missing | 任意新增长期状态、物品绑定、交易防刷或持续神力 | `nbt_state_ownership_queue.py audit` 通过，runtime-only 升级触发器无遗漏 |
| P1-NBT-003 | 新增长期状态先升 schema | 仍有 48 个 `runtime_only` 状态，但它们不是缺失，而是短期状态决策 | 新玩法设计 | 任一长期状态先在目标文档中标为 `required`，再补 round-trip |
| P1-NBT-004 | 神力持续状态持久化复审 | 高风险神力含持续天气/世界编辑类 | P1-GP-002 | 持续状态跨存档语义明确，必要项可恢复或安全结束 |
| P1-NBT-005 | 证书经济和结构状态回归 | 当前证书/结构静态通过 | P1-CERT-001、P1-STRUCT-002 | 余额、交易冷却、神殿状态跨重启正确 |

## 11. 执行波次

| 波次 | 主攻方向 | 必须先完成 | 验收命令 |
| --- | --- | --- | --- |
| W0 | 设计阻断清理 | P0-ART-001、P0-ART-002 | `tools/pegasus_hydra_v6_modeling_spec_package.py audit` + `tools/pegasus_hydra_design_correctness_gate.py audit` |
| W1 | 全生物设计签收与 Greek 视觉 vertical slice | P0-ART-003 至 P0-ART-009 | `tools/creature_design_first_gate.py audit` + `tools/creature_visual_acceptance_package.py audit` |
| W2 | 非希腊 20 生物美术升级 | P1-ART-EGY/NOR/CHI/ATL | `tools/creature_visual_acceptance_package.py audit` |
| W3 | 动作和连续帧 | P1-ACT-001 至 P1-ACT-004 | `tools/creature_action_acceptance_package.py audit` |
| W4 | 神力、粒子、音效 | P1-GP、P1-FX、P1-AUD | gameplay、particle、audio acceptance |
| W5 | UI、结构、NBT、世界生成 | P1-UI、P1-STRUCT、P1-NBT | UI、structure、NBT acceptance |
| W6 | 发布候选 | release blockers 为 0 | `tools/mythic_age_1_0_gap_audit.py audit` + `./gradlew build --offline` |

## 12. 必跑回归命令

```bash
/Users/hongyuwu/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3 -B tools/greek_design_first_gate.py audit
/Users/hongyuwu/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3 -B tools/greek_design_approval_package.py audit
/Users/hongyuwu/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3 -B tools/pegasus_hydra_v6_modeling_spec_package.py audit
/Users/hongyuwu/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3 -B tools/pegasus_hydra_design_correctness_gate.py audit
/Users/hongyuwu/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3 -B tools/creature_design_first_gate.py audit
/Users/hongyuwu/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3 -B tools/creature_visual_acceptance_package.py audit
/Users/hongyuwu/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3 -B tools/creature_human_visual_acceptance_ledger.py audit
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

## 13. 退出标准

1. `tools/greek_design_first_gate.py audit` 通过。
2. `tools/creature_design_first_gate.py audit` 通过，30 个生物都有正式设计签收。
3. `tools/creature_visual_acceptance_package.py audit` 无 design rejection blocker 和 visual quality contract blocker。
4. 30 生物人工视觉验收完成，`tools/creature_human_visual_acceptance_ledger.py audit` 通过。
5. 138 项缺失动作模式补齐，动作人工验收完成。
6. 52 神力玩法验收完成，18 个高风险神力有性能和失败路径结论。
7. 149 个音频事件人工听感完成，21 个候选生物 final mix 结论明确。
8. UI、粒子、结构和世界生成完成实机验收。
9. 新增长期状态全部有 NBT schema、round-trip 和真实重启回归。
10. `tools/mythic_age_1_0_gap_audit.py audit` 显示 `ready_for_1_0=true` 且 release blocking gaps 为 0。
11. `tools/mythic_age_1_0_scope_matrix.py audit` 显示所有模块满足 1.0 退出 gate。
12. `./gradlew build --offline` 通过。
