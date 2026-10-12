# MythicAgeMod 1.0 重整开发总纲（2026-06-15）

生成时间：2026-06-15 16:41 CST

本文档用于重整 1.0 大版本开发口径。它取代“只有继续补希腊生物”或“文件齐全即接近发布”的旧判断，后续执行必须以当前工作树和最新 gate 结果为准。

配套执行台账：`docs/项目管理/MythicAgeMod_1.0开发任务台账_2026-06-15.md`。总纲定义方向，台账定义任务、依赖和退出标准。

## 1. 调查结论

当前项目不是只有希腊代码，但也绝不是 1.0 完成。

真实状态是：

- 代码层已经名义接入五文明：Greek、Egyptian、Nordic、Chinese、Atlantean。
- 实体注册层已有 30 个五文明生物、5 个文明守护者、5 个自定义守护者和 Walking Tree。
- 资源层已有五文明生物纹理、刷怪蛋、Blockbench 文件、设计文档、三视角预览和 canonical 神殿 NBT。
- 玩法层已有 52 个非神殿生成神力、五文明证书、守护者交易和神力图鉴静态链路。
- NBT 层已有 14 个 schema owner，并且 NBT 发布验收包当前通过真实重启周期。

但这些只能证明“工程骨架存在”。当前 1.0 的主要问题是：

- 希腊生物模型质量未达标，`pegasus` 和 `hydra` 已被明确判定为设计错误。
- 非希腊 20 个生物虽然有文件和预览，但大多仍是工程首版/占位美术，不是 1.0 可发布美术。
- 动作、音效、神力玩法、UI、粒子、建筑美术都没有完成最终人工验收。
- 自动 gate 曾过度表达“通过”，需要明确降级为“材料齐全或静态契约存在”，不能代表好看、可玩或发布完成。

## 2. 最新证据

| 模块 | 当前证据 | 结论 |
| --- | --- | --- |
| 1.0 总审计 | `docs/preview_assets/1_0_gap_audit/gap_report.json` | `ready_for_1_0=false`，release blocking gaps=4。 |
| 生物视觉 | `docs/preview_assets/creature_visual_acceptance/creature_visual_acceptance_package.json` | 当前 `ok=false`；设计拒绝 2 个，质量合同阻断 1 个，30 个均需人工视觉验收。 |
| 设计拒绝 | `docs/美术资源/greek_creatures/design_review/pegasus_hydra_design_rejection_2026-06-15.md` | `pegasus`、`hydra` V2 不能继续作为建模依据。 |
| 设计审批包 | `docs/美术资源/greek_creatures/design_review/pegasus_hydra_design_approval_package_2026-06-15.md` | PNG 设计图和旧/新对照板已生成；当前仍为 approval pending。 |
| 动作 | `docs/preview_assets/creature_action_acceptance/creature_action_acceptance_package.json` | 静态合同通过，但完整动作序列缺 138 项，人工动作验收未完成。 |
| 神力玩法 | `docs/preview_assets/god_power_gameplay/god_power_gameplay_acceptance_package.json` | 52 个神力静态通过，仍需 52 项人工 playtest，最低 446 场景。 |
| 音效 | `docs/preview_assets/audio_validation/creature_audio_final_mix_acceptance_package.json` | 149 个事件可听审，21 个候选生物仍需最终听感/响度验收。 |
| NBT | `docs/preview_assets/nbt_roundtrip_validation/nbt_release_acceptance_package.json` | 静态合约、runtime entity probe、真实重启周期当前通过；新增持久化状态必须先补 schema。 |
| 结构 | `docs/preview_assets/structure_validation/structure_report.json` | 15 个 canonical 神殿 NBT 静态通过，但建筑审美和实机场景融合未完成。 |
| UI | `docs/preview_assets/ui_validation/ui_report.json` | 守护者交易和神力图鉴静态通过；真实分辨率、长文本和文明主题未最终验收。 |
| 粒子 | `docs/preview_assets/particle_validation/god_power_particle_report.json` | 52 神力粒子矩阵静态通过；最终文明主题、压力和实机可读性未完成。 |

## 3. 1.0 产品定义

1.0 目标不是“生成一批实体文件”，而是五文明基础可玩版本：

- 玩家能在世界中发现五文明神殿/村庄/守护者。
- 每个文明有稳定的证书获取、消耗和交易链路。
- 每个文明有至少 5 个一眼可辨识的神话生物，Greek 为 10 个。
- 每个生物有正确设计、模型、纹理、动作、音效、粒子/技能反馈、掉落/生成/召唤入口。
- 每个神力有冷却、消耗、目标选择、失败反馈、粒子、音效、UI 图鉴和玩法验收。
- 结构、方块实体、世界状态、证书经济和长期状态能跨存档保存。
- UI、图标、粒子、音效、建筑和生物遵守统一文明美术语言。

## 4. 当前必须承认的缺失

### 4.1 生物与模型

当前名义覆盖：

- Greek 10：`minotaur`、`cyclops`、`centaur`、`hydra`、`medusa`、`nemean_lion`、`manticore`、`chimera`、`colossus`、`pegasus`
- Egyptian 5：`anubis_warrior`、`scarab_guardian`、`mummy_soldier`、`sphinx_sentinel`、`sun_falcon`
- Nordic 5：`frost_giant`、`draugr_warrior`、`valkyrie`、`fenrir_wolf`、`dwarf_berserker`
- Chinese 5：`azure_dragon`、`qilin_guardian`、`jiangshi`、`nine_tailed_fox`、`stone_lion_guardian`
- Atlantean 5：`crystal_leviathan`、`orichalcum_golem`、`siren_priestess`、`tidal_guardian`、`abyssal_ray`

当前缺失：

- `pegasus`：V2 设计错误。必须先审查 `docs/美术资源/greek_creatures/design_blueprints/pegasus_v6_modeling_spec_contract.md`、`docs/preview_assets/greek_creatures/design_approval/v6/pegasus_v6_modeling_spec_sheet.png` 和 V6 可建模规格包，确认后重新建模。
- `hydra`：V2 设计错误。必须先审查 `docs/美术资源/greek_creatures/design_blueprints/hydra_v6_modeling_spec_contract.md`、`docs/preview_assets/greek_creatures/design_approval/v6/hydra_v6_modeling_spec_sheet.png` 和 V6 可建模规格包，确认头数、头颈连接和身体比例后重新建模。
- `minotaur`：视觉质量合同仍有 `greek_mechanical_contract_ok` 阻断，需要修复 gate 依据或补足实际机械合同。
- `cyclops`：仍是唯一“修订完成待用户确认”的希腊反馈项，必须人工确认巨大人形和石棒。
- 其他 27 个生物均为 `pending_human_shape_review`，不得标记美术完成。
- 非希腊 20 个生物从 contact sheet 看仍有明显工程首版感：面部、眼嘴、武器、翼/尾/鳞片、材质层次和文明特征都需要高精度重做或精修。

后续模型原则：

- 先设计蓝图，再 Blockbench，再 UV，再纹理，再 Java/runtime，同步顺序不可反。
- 测试只验证设计是否落地，不再负责试错造型。
- 任何“大头补细节”“大块方体代替结构”“纹理随机面板当最终资源”都不得进入 1.0。

### 4.2 纹理与美术

当前缺失：

- 多数纹理仍像程序生成或低精度像素块，不能代表最终文明材质。
- 眼睛、嘴、牙、角、鬃毛、羽毛、蛇发、鳞片、武器、符箓、符文、水晶等关键面需要 UV 驱动手工化。
- 五文明美术 bible 已存在，但尚未作为强 gate 反查每个模型、图标、粒子和 UI。

1.0 要求：

- 每个生物有设计图、三视角、UV 计划、最终纹理、Blockbench 截图和人工签收。
- 每个文明有固定色板、材质语言和禁用风格。
- 所有白色/深色大面必须有结构阴影，不得出现空白或黑块。

### 4.3 动作

当前缺失：

- 30 生物动作包静态通过，但只捕获了 30/168 个必需动作模式。
- 缺少 138 项动作序列，覆盖 locomotion、attack/special、hurt、death、flight/glide、riding_fly、swim/drift、signature multi-part motion。
- `pegasus` 和 `hydra` 动作必须等 V6 可建模规格签收并完成模型重建后重新做，不能基于畸形模型继续验收。

1.0 要求：

- 每个生物至少有 idle、移动、攻击/特殊、受击、死亡。
- 飞行/游泳/骑乘单位必须有对应动作。
- 多头、多尾、多翼、多武器单位必须有 signature 部件联动。

### 4.4 代码与玩法

当前已有：

- 30 生物实体和属性注册。
- 52 个非神殿生成神力进入静态玩法验收包。
- 证书经济、守护者交易和神力图鉴静态审计通过。

当前缺失：

- 52 个神力全部还需要人工玩法验收。
- 高风险神力 18 个，主要是大范围世界编辑、爆炸、天气/持续管理类效果。
- 证书获取虽然静态闭环，但各文明真实产出频率、防刷、交易价格、玩家引导还未完成实机平衡。
- 生物 AI 与技能需要逐个确认：是否有目标选择、攻击节奏、特殊技能冷却、掉落和生成控制。

1.0 要求：

- 神力不能只“能施放”，还必须有失败路径、清楚反馈、合理强度和性能上限。
- 证书经济必须形成“获取 -> UI 可见 -> 交易消耗 -> 冷却/存档 -> 防刷”的完整闭环。

### 4.5 UI

当前已有：

- `CustomTempleGuardianTradeScreen`
- `GodPowerGuideScreen`
- `GodPowerGuideItem`
- 静态 UI gate 通过

当前缺失：

- 五文明主题皮肤和图标 polish 不足。
- 长文本、多语言、低分辨率、不同 GUI scale、键鼠/手柄路径未验收。
- 玩家仍需要更清楚知道：证书怎么来、神力怎么解锁、神殿当前缺什么。

1.0 要求：

- 守护者交易 UI 显示文明、证书余额、神力价格、材料需求、冷却和失败原因。
- 神力图鉴显示文明、效果、冷却、消耗、获得方式、风险说明和状态。
- 神殿/神像/圣物状态有可读反馈。

### 4.6 粒子

当前已有：

- 自定义粒子基础链路。
- 神力粒子矩阵静态通过。
- 性能辅助和统一粒子入口已存在。

当前缺失：

- 多数效果仍是原版粒子组合或初版特效，不是最终文明主题美术。
- 缺少每个神力的启动、持续、命中/结束三段式实机验收。
- 大范围神力需要压力测试和降级策略验证。

1.0 要求：

- 每个神力至少有三段式效果。
- 每个核心生物技能有主题粒子。
- 粒子不遮挡眼睛、嘴、武器、翼根、尾刺、多头等关键读形。

### 4.7 音效

当前已有：

- 149 个生物音频事件可试听。
- OGG 容器技术检查通过。
- 30 生物有 final mix review board。

当前缺失：

- 21 个候选生物仍需最终听感确认。
- 缺少响度工具：`ffprobe`、`ffmpeg`、`soxi`、`ogginfo`。
- 生物音效还需要确认文明主题、战斗可读性、循环和音量层级。
- 神力 cast/impact/end、UI 点击、交易成功/失败、证书产出提示音仍需完整审计。

### 4.8 NBT 与结构

当前 NBT：

- 14 个 schema owner 通过。
- 真实 prepare -> save/quit -> restart same world -> verify 周期当前通过。
- 仍有 48 个 `runtime_only` 状态，不是 1.0 持久化要求。

后续约束：

- 只要新增长期状态，必须先把目标从 `runtime_only` 或未定义提升为 `required`，再补 schema、round-trip 和真实重启验证。
- 不能因为 NBT 当前通过，就跳过新玩法状态的持久化设计。

当前结构：

- 五文明 small/medium/large 共 15 个 canonical NBT 存在并静态通过。

结构缺失：

- 建筑审美、地形融合、功能点位实机可读性和文明差异仍未最终验收。
- 结构不能只以“文件存在”作为完成标准。

## 5. 1.0 P0/P1/P2 开发优先级

### P0：设计与视觉基线重建

1. 审查并确认 `pegasus_v6_modeling_spec_contract.md`、`pegasus_v6_modeling_spec_sheet.png` 和 V6 可建模规格包。
2. 审查并确认 `hydra_v6_modeling_spec_contract.md`、`hydra_v6_modeling_spec_sheet.png` 和 V6 可建模规格包。
3. 按签收后的 V6 规格重新建模、UV、纹理、runtime Java，同步动作需求。
4. 修复 `minotaur` 的视觉质量合同阻断。
5. 建立人工视觉验收表，30 生物逐个判定：通过、返修、设计重写。

退出标准：

- `tools/greek_design_first_gate.py audit` 通过。
- `tools/creature_visual_acceptance_package.py audit` 不再有 design rejection blocker 和 visual quality contract blocker。
- 30 生物人工视觉验收有明确结果。

### P1：动作、神力玩法、音效与粒子

1. 完成 138 项缺失动作序列捕获，至少 1020 帧。
2. 对 52 个神力执行最低 446 场景 playtest，优先 18 个高风险神力。
3. 补齐最终音效工具链或等价响度验证，完成 149 个事件人工听感。
4. 对每个神力和核心生物技能补三段式粒子验收。

退出标准：

- `creature_human_motion_acceptance_ok=true`
- `god_power_gameplay_acceptance_ok=true`
- `creature_audio_final_mix_acceptance_ok=true`
- 粒子实机/压力验收完成

### P1：非希腊文明美术重做

埃及、北欧、中国、亚特兰蒂斯的 20 个生物不是“没有”，但当前不能按 1.0 成品处理。要按文明分批重做：

- Egyptian：胡狼头、圣甲虫甲壳、木乃伊绷带、狮身人面、太阳猎鹰。
- Nordic：霜巨人、亡灵战士、女武神、芬里尔、矮人武器和盔甲。
- Chinese：青龙分段龙身、麒麟角鬃、僵尸符箓、九尾狐九尾结构、石狮卷鬃。
- Atlantean：水晶利维坦、山铜魔像、海妖祭司、潮汐守卫、深渊鳐。

每个生物必须先出设计蓝图，再进入 Blockbench。

### P2：UI、结构、发布卫生

1. UI 做五文明主题、长文本、多语言和真实分辨率验收。
2. 15 个神殿结构做建筑审美和实机场景融合验收。
3. README、玩家指南、CHANGELOG、版本号、发布清单统一。
4. 把旧文档中“接近完成/已完成”的过时说法继续降级或归档。

## 6. 后续执行路线

### M0：口径冻结

- 本文档作为 2026-06-15 后的 1.0 重整主依据。
- `docs/项目管理/MythicAgeMod_1.0开发任务台账_2026-06-15.md` 作为后续执行拆解依据。
- 不再用“文件存在”“静态 gate 通过”表达 1.0 完成。
- 每次阶段结束重跑 `tools/mythic_age_1_0_gap_audit.py audit`。

### M1：Greek 视觉 vertical slice 重做

- 先签收 Pegasus/Hydra V6 可建模规格。
- 修 Minotaur gate。
- 重新出 Greek 10 生物视觉验收结论。
- Greek 通过后再横向推进非希腊 20 个。

### M2：五文明生物美术升级

- 每个文明一批，先设计蓝图，再模型和纹理。
- 每批输出：设计包、`.bbmodel`、UV、纹理、front/side/three-quarter、runtime model、视觉报告。

### M3：动作与实机观感

- 按 `motion_capture_plan_batches` 执行 16 批动作捕获。
- 动作验收不再只看 `setAngles`，必须看连续帧。

### M4：神力玩法与粒子

- 按 52 神力 playtest 队列执行。
- 优先大范围破坏、天气、爆炸、世界编辑和召唤类神力。
- 每项神力补粒子三段式和音效节点。

### M5：音效、UI、结构、NBT 回归

- 完成最终混音听感和响度验证。
- UI 和结构做真实截图验收。
- 新增状态补 NBT schema 和真实重启周期。

### M6：1.0 发布候选

- release blocking gaps 必须为 0。
- `./gradlew build --offline` 通过。
- 所有人工验收项有签收记录。

## 7. 回归命令

```bash
/Users/hongyuwu/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3 -B tools/greek_design_first_gate.py audit
/Users/hongyuwu/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3 -B tools/greek_design_approval_package.py audit
/Users/hongyuwu/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3 -B tools/creature_visual_acceptance_package.py audit
/Users/hongyuwu/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3 -B tools/creature_action_acceptance_package.py audit
/Users/hongyuwu/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3 -B tools/god_power_gameplay_acceptance_package.py audit
/Users/hongyuwu/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3 -B tools/creature_audio_final_mix_acceptance_package.py audit
/Users/hongyuwu/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3 -B tools/nbt_release_acceptance_package.py audit
/Users/hongyuwu/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3 -B tools/mythic_age_1_0_gap_audit.py audit
./gradlew build --offline
```

## 8. 当前一句话判断

MythicAgeMod 目前是“五文明工程骨架 + 大量静态验收材料 + 多个关键未通过的美术/动作/玩法验收项”，不是 1.0 可发布版本。后续 1.0 开发必须先修正设计和美术质量，再推进动作、神力、音效、UI、粒子和结构实机验收。
