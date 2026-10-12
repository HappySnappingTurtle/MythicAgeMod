# MythicAgeMod 1.0 当前缺口与开发总纲

生成日期：2026-06-11

本文档用于替代“只有希腊生物继续补模型”的狭窄口径。当前仓库已经形成五文明内容雏形，但 1.0 还没有达到可发布质量。后续开发必须同时覆盖代码、模型、动作、纹理、UI、美术、粒子、音效、NBT、结构和验证工具。

## 1. 调查结论

当前事实：

- 五文明均已进入工程骨架：Greek、Egyptian、Nordic、Chinese、Atlantean。
- 当前视觉 manifest 覆盖 30 个五文明生物：
  - Greek 10 个：`minotaur`、`cyclops`、`centaur`、`hydra`、`medusa`、`nemean_lion`、`manticore`、`chimera`、`colossus`、`pegasus`
  - Egyptian 5 个：`anubis_warrior`、`scarab_guardian`、`mummy_soldier`、`sphinx_sentinel`、`sun_falcon`
  - Nordic 5 个：`frost_giant`、`draugr_warrior`、`valkyrie`、`fenrir_wolf`、`dwarf_berserker`
  - Chinese 5 个：`azure_dragon`、`qilin_guardian`、`jiangshi`、`nine_tailed_fox`、`stone_lion_guardian`
  - Atlantean 5 个：`crystal_leviathan`、`orichalcum_golem`、`siren_priestess`、`tidal_guardian`、`abyssal_ray`
- `entity_validation` manifest 覆盖 43 个实体条目，包含 30 个五文明生物、`walking_tree`、守护者、自定义守护者和 `meteor`。
- 五文明 canonical 神殿结构 NBT 已达到 15/15 存在并可读。
- UI、粒子、结构、实体动作和 runtime model 都已有静态 gate。
- 30 生物动作验收包已生成，路径为 `docs/美术资源/entity_validation/creature_action_acceptance_package.md` 和 `docs/preview_assets/creature_action_acceptance/creature_action_acceptance_package.json`；静态动作证据已覆盖 idle、移动、攻击/特殊、受击、飞行/滑游/骑乘和多部件联动要求，但不替代动作观感验收。

但 1.0 仍未完成，原因是：

- 自动 gate 主要证明文件、token、静态契约存在，不能证明模型好看、动作可信、玩法闭环完整。
- 30 生物全量视觉人工验收包已生成，路径为 `docs/美术资源/entity_validation/creature_visual_acceptance_package.md` 和 `docs/preview_assets/creature_visual_acceptance/creature_visual_acceptance_package.json`；该包证明材料齐全，但不替代人工审美确认。
- 30 生物动作验收包静态通过，但 `human_motion_acceptance_ok=false`；后续仍需基于设计包检查动作节奏、关键部件参与、粒子遮挡和死亡/受击可读性。
- 所有生物 visual report 仍处于 `blockbench_passed_pending_human_shape_review`，尚未完成最终人工造型验收。
- NBT 静态 schema 当前覆盖 14 个 owner，并已补源码级 round-trip 与重启契约探针；NBT 发布验收包已生成，静态合约和运行中实体探针通过，真正发布前仍需要真实 MC 存档重启实测。
- 音频技术 gate 已升级为检查非空 OGG/Vorbis；牛头人 0 字节音频已修复为技术占位并记录在 `docs/preview_assets/audio_validation/audio_placeholder_repair_report.json`，但仍不算最终混音。
- 生物音效最终混音验收包已生成，路径为 `docs/美术资源/audio/creature_audio_final_mix_acceptance_package.md` 和 `docs/preview_assets/audio_validation/creature_audio_final_mix_acceptance_package.json`；截至 2026-06-15，replacement required=0，但 21 个 final-mix candidate creature 和 149 个事件仍需最终听感/响度验收。
- 非希腊生物已有程序化首版音效技术链路，但仍未达到最终文明主题混音质量。
- Atlantean 神力已补到 10 个 1.0 注册法术和独立首版图标，但仍需逐项玩法、平衡和最终图标 polish。
- 神力粒子 gate 已覆盖 52/52 个非神殿生成神力，并阻止 `SpellbookItem` 绕过统一粒子性能辅助；剩余问题是文明主题美术、压力测试和逐项玩法验收。
- 神力玩法验收包已生成，路径为 `docs/系统设计/MythicAgeMod_1.0_神力玩法验收包_2026-06-12.md` 和 `docs/preview_assets/god_power_gameplay/god_power_gameplay_acceptance_package.json`；52/52 个非神殿生成神力的注册、路由、冷却、UI 图鉴、主语言运行时名称和粒子矩阵静态通过，但 `gameplay_acceptance_ok=false`，仍需逐项人工 playtest。
- UI gate 已覆盖守护者交易界面和神力图鉴界面；真实分辨率、长文本溢出和最终文明主题美术仍需人工/截图验收。
- 结构 NBT 已通过文明主题材料、尺寸、功能点位和 template pool 静态 gate，但仍不等于最终建筑审美和实机视觉完成。

## 2. 1.0 定位

1.0 不是“希腊生物包”，也不是“代码能编译即可”。1.0 应该是一个五文明基础可玩版本：

- 玩家能在世界中遇到五文明神殿、守护者和神话生物。
- 每个文明都有清楚的神力证明获取方式。
- 玩家能通过神殿、守护者、神像、圣物或挑战解锁神力。
- 每个文明至少有一批一眼可辨识的神话单位。
- 生物、神力、建筑、UI、粒子、音效、文案和 NBT 存档形成闭环。

1.0 内容只有满足“设计、资源、代码、玩法、验证”五个层面，才算完成。

## 3. 主要缺口

### 3.1 代码与玩法

已有：

- 五文明实体注册、属性注册和自然生成入口。
- 30 个五文明生物实体和 `walking_tree` 召唤物。
- 12 个希腊神力、10 个中国神力、10 个北欧神力、10 个埃及神力、10 个亚特兰蒂斯 1.0 神力。
- 守护者交易网络包和自定义交易界面。

缺口：

- 五文明最小可玩闭环未冻结：每个文明的证书来源、消耗、交易、掉落、挑战和神力解锁关系还需要一张正式矩阵。
- 生物 AI 和技能已有静态合同，但缺少按单位逐项实测的玩法验收。
- 神力玩法验收包已确认 52 个非神殿生成神力具备静态注册、冷却、提示、图鉴、主语言名称和粒子矩阵；但还没有逐项人工验证目标选择、失败路径、强度、性能、消耗和战斗可读性。
- 需要统一配置项：生成率、神力冷却、证书产出、交易冷却、粒子密度和调试开关。

### 3.2 模型

已有：

- 五文明生物均有设计包、`.bbmodel`、纹理、UV 模板和 Blockbench 预览基础产物。
- 五文明 runtime model quality gate 均通过。
- 希腊 10 生物已进行了 V2 精修，其中 Hydra、Cyclops、Pegasus、Medusa、Colossus 等已针对用户反馈做过重点修正。

缺口：

- 自动检查仍不能替代最终美术验收。
- 希腊模型虽然比上一轮更细，但仍需要用户按设计图确认整体审美、比例和神话特征。
- 非希腊生物多数还处在“工程可读”层级，需要提升到“神话单位一眼可辨”的精细度。
- 后续不能从 Java 模型盲目反推造型，必须先冻结设计图和部件层级。

1.0 模型完成定义：

- 每个生物有设计图/设计包、Blockbench group 层级、Java runtime 部件映射。
- 正面能看清眼睛、嘴和核心神话特征。
- 侧面能看清体态、尾、翼、武器或多头结构。
- 与玩家和同文明单位的体量关系明确。

### 3.3 动作

已有：

- 31 个神话生物/召唤物有静态动作和粒子合同。
- 多数 Java 模型有 `setAngles` 基础动作。
- 天马具备自主飞行和骑乘飞行逻辑。
- 30 个 1.0 生物动作验收包已静态通过，能追踪到实体、AI goal、模型 `setAngles` 和粒子/音效挂点。

缺口：

- 动作多是程序摆动，缺少先验动作设计表。
- hurt、death、special、飞行、骑乘、多头攻击、尾刺、石化凝视等动作已有静态证据，但缺少动作预览或人工观察验收。
- 服务端技能状态和客户端模型动作之间的同步仍偏弱。

1.0 动作完成定义：

- 每个生物至少有 idle、walk/run、attack、special、hurt、death。
- 飞行单位额外有 takeoff、fly、land、riding_fly。
- 多部件单位必须定义分部动作，例如 Hydra 多头、Chimera 三头、Manticore 尾刺、Medusa 蛇发。

### 3.4 纹理

已有：

- 五文明生物都有运行时 PNG 纹理。
- UV/texture fit 检查能发现透明空白和 UV 越界。

缺口：

- 程序生成纹理仍有占位感。
- 关键面艺术表达不足：眼睛、嘴、牙、鳞片、鬃毛、羽毛、蛇发、武器材质、符文等仍需要人工绘制级 polish。
- 缺少统一调色板和材质规则。

1.0 纹理完成定义：

- 每个关键面在 UV 模板中标注位置。
- 纹理在 Blockbench 三视角确认无空白、无透明破面、无错位。
- 每个文明有主色、辅色、材质语言和禁用风格。

### 3.5 UI

已有：

- `CustomTempleGuardianTradeScreen` 已有交易列表、输入/输出、库存计数、冷却状态、禁用原因、tooltip 和多语言 key。
- `GodPowerGuideScreen` 已提供概览、神力图鉴、神殿状态、证书余额四个页签，并通过 `GodPowerGuideItem` 的网络入口打开。
- `tools/ui_validation_pipeline.py audit` 已扩展到 `static_guardian_trade_and_god_power_guide_ui` 并通过。

缺口：

- 交易 UI 的文明主题仍弱。
- 还没有真实分辨率、长文本溢出、键鼠/手柄可用性和最终美术验收。

1.0 UI 完成定义：

- 五文明守护者交易 UI 显示文明主题、证书余额、需求材料、冷却、失败原因。
- 神力详情 UI 或指南说明来源、效果、冷却、消耗、解锁方式。
- 神殿/神像/圣物状态反馈能告诉玩家缺什么、还要等多久、存了多少。

### 3.6 美术

已有：

- 生物、方块、神力书、圣物、神像和部分 concept/contact sheet 分散存在。

缺口：

- 缺少全局美术 bible。
- 五文明视觉语言没有统一冻结。
- UI、图标、粒子、生物、建筑之间风格不完全一致。

1.0 美术 bible 必须冻结：

- Greek：大理石、青铜、橄榄绿、奥林匹斯纹样。
- Egyptian：砂岩、黄金、青金石、太阳、象形符号。
- Chinese：朱红、金、玉色、云纹、龙纹、符箓。
- Nordic：冷灰石、深木、冰蓝、卢恩符文、兽骨和铁器。
- Atlantean：海晶石、青蓝发光、水晶、失落科技几何纹样。

### 3.7 粒子

已有：

- 自定义粒子注册链：`mythic_flame`、`divine_sparkle`、`shockwave`。
- 神力粒子性能辅助：粒子开关、密度、距离、单次上限。
- `particle_validation_pipeline.py` 和 `god_power_particle_validation_pipeline.py` 通过。
- 神力粒子静态矩阵已覆盖 52/52 个非神殿生成神力；`SpellbookItem` 不再直接调用 `spawnParticles`，统一经过 `MythicParticleEffects`。
- `tools/god_power_gameplay_acceptance_package.py audit` 已把 52 个神力的静态玩法契约和人工 playtest 待办聚合到同一验收包。

缺口：

- 自定义粒子只覆盖一小部分代表技能。
- 神力粒子仍主要由原版粒子拼接，缺少最终文明主题和专属图集。
- 缺少每个文明的艺术级三段式效果验收：启动、持续、命中/结束。
- 神力人工玩法验收仍未完成，尤其是 `void_cast_always_success`、`no_explicit_failure_path`、`threaded_delayed_effect` 和大范围世界编辑类风险需要逐项确认。

1.0 粒子完成定义：

- 每个神力至少定义启动、持续、结束/命中三段视觉。
- 每个核心生物技能有主题粒子。
- 粒子受性能配置控制，并有压力测试口径。

### 3.8 音效

已有：

- 希腊生物和陨石音效较完整。
- `sounds.json` 已登记 30 个 1.0 生物的 ambient、hurt、death、attack、special/charge/fly 等事件。
- 非希腊 20 个生物已有程序化首版 OGG、Java 引用和三语字幕。
- `docs/美术资源/audio/creature_audio_art_direction_2026-06-12.json` 已为 30 个生物定义文明音色、事件意图和最终验收标准。
- `tools/audio_validation_pipeline.py` 已覆盖非空 OGG/Vorbis、`sounds.json`、`ModSounds`、实体引用、字幕和音效方向。
- 牛头人 0 字节 OGG 已修复为技术占位，保证运行时不再引用空音频。
- 生物音效最终混音验收包已聚合 30 生物、149 个事件的 OGG 元数据、时长和人工听感问题。

缺口：

- 牛头人技术占位和非希腊 20 个程序首版仍需最终混音或替换为真实设计音效，合计 21 个最终音频待办。
- 神力 cast/impact/end 音效需要逐项审计和最终听感验收；当前玩法验收包只确认统一施法成功音效存在，不等于每项神力已有最终专属音效。
- UI 点击、交易成功/失败、证书产出提示音不足。

1.0 音效完成定义：

- 每个生物有 ambient、hurt、death、attack、special；有移动音的单位补 step/fly/swim。
- 每个神力有 cast 和 impact，持续型神力补 loop/end。
- `sounds.json`、音频文件和字幕 key 一致。
- 每个生物有音效方向设计包，最终 OGG 通过响度、可读性和文明主题听感验收。

### 3.9 NBT 与结构

已有：

- 实体/方块实体/world state NBT 静态 schema 覆盖 14 个 owner。
- `/mythicage_validate nbt` 运行时 round-trip 探针已存在。
- `/mythicage_validate nbt_restart` 和 VisualProbe `/validate_nbt_restart` 重启契约入口已存在。
- `/mythicage_validate nbt_restart_prepare` 与 `nbt_restart_verify` 已存在，可执行 prepare -> 保存退出 -> 重启同一世界 -> verify 的真实存档重启周期。
- `tools/mc_visual_probe_mcp.py` 已封装 `run_runtime_nbt_restart_validation`、`run_runtime_nbt_restart_prepare`、`run_runtime_nbt_restart_verify`，并在每次运行前清理旧报告，避免误读历史结果。
- `tools/nbt_target_validation_pipeline.py audit`、`tools/nbt_roundtrip_probe_pipeline.py audit`、`tools/nbt_restart_contract_probe.py audit` 均通过。
- `tools/nbt_release_acceptance_package.py audit` 已生成 NBT 发布验收包，路径为 `docs/系统设计/MythicAgeMod_1.0_NBT发布验收包_2026-06-12.md` 和 `docs/preview_assets/nbt_roundtrip_validation/nbt_release_acceptance_package.json`。
- 五文明 canonical 神殿结构 NBT 15/15 存在并可读。
- `tools/structure_validation_pipeline.py audit` 通过，并覆盖文明主题材料、尺寸、功能点位、template pool 引用和数据包污染检查。

缺口：

- NBT 目标表中仍有 48 个 `runtime_only` 状态，表示当前 1.0 不冻结为持久化要求；若后续玩法升级，需要先转入 `required` 并补 schema/探针。
- 当前已具备真实 MC 存档重启周期探针入口，但还未在当前阶段执行并产出 `run/visual_probe/nbt_restart_verify_report.json`。
- 旧 `structures/temple/greek/greek_temple_small.nbt` 和数据包 `.DS_Store` 污染已清理；后续不得重新引入非 canonical 结构路径。
- canonical 神殿 NBT 已具备静态功能布局契约，不代表最终建筑审美、地形融合和实机视觉完成。

1.0 NBT 完成定义：

- 所有需要跨存档保留的状态都有 schema。
- 静态 schema 和运行时 round-trip 同时覆盖。
- 真实 MC 存档重启周期必须存在 verify 报告且 `failed=0`；否则 `nbt_runtime_roundtrip_missing` 仍是 P0 blocker。
- 结构 NBT 检查尺寸、palette、文明主题材料、功能方块、守护者/圣物/神像点位和 template pool 引用。

## 4. 文明 1.0 内容矩阵

| 文明 | 当前已接入 | 1.0 未完成重点 |
| --- | --- | --- |
| Greek | 10 生物、12 神力、神殿、证书、守护者、音效较完整 | 人工美术验收、动作 polish、纹理手工化、玩法实测、模型最终确认 |
| Egyptian | 5 生物、10 神力、神像/神殿基础、结构 NBT | 生物美术精修、音效、神力逐项实测、神像证书链路验收 |
| Nordic | 5 生物、10 神力、行走树、战斗证明规则 | 生物美术精修、音效、行走树纳入统一验收、神力/证明链路实测 |
| Chinese | 5 生物、10 神力、文明神殿基础 | 生物美术精修、音效、人口/村庄证明链路确认、神力逐项实测 |
| Atlantean | 5 生物、10 个 1.0 神力、神殿/守护者基础 | 神力逐项玩法验收、独立图标 polish、粒子主题、生物音效、证明获取方式 |

## 5. 1.0 里程碑

### M0：现状冻结与旧口径纠偏

交付物：

- 本文档。
- `tools/mythic_age_1_0_gap_audit.py`
- `docs/preview_assets/1_0_gap_audit/gap_report.json`

退出标准：

- 后续讨论统一使用“自动 gate 通过但未达 1.0”的口径。
- 不再引用旧文档中“接近发布”“95% 完成”等过时判断。

### M1：设计冻结

交付物：

- 五文明美术 bible。
- 五文明 1.0 内容矩阵。
- 每个文明证书获取与消耗规则。
- 每个生物的设计包、动作表、NBT 状态需求表。
- 30 生物全量视觉人工验收包，逐项引用概念图、Blockbench 三视图、UV、贴图贴合、runtime model gate 和待人工确认问题。
- 神力矩阵、玩法验收口径和 UI 范围冻结。

退出标准：

- 所有开发项都有明确设计依据，不通过测试阶段临时发明造型。

### M2：Greek 质量样板

交付物：

- 希腊 10 生物三视角人工验收记录。
- 高精度模型/纹理/动作 polish。
- 希腊神力、守护者交易、证书、生成、掉落闭环实测。

退出标准：

- 用户确认希腊生物达到预期，不再出现比例小、特征不清、眼嘴错位、纹理空白等问题。

### M3：NBT 与结构收口

交付物：

- 按 NBT 目标表维护 schema；只有当物品、交易、神殿状态、神力持续状态或生物状态从 `runtime_only` 升级为 `required` 时，才新增 schema 和探针。
- 清理或迁移旧结构 NBT 和数据包污染。
- 五文明神殿结构设计包、功能点位和建筑美术验收；静态功能点已过 gate，剩余重点是最终建筑审美与实机视觉。

退出标准：

- 结构 gate 无 warning。
- 所有 1.0 持久化状态有静态和运行时验证。

### M4：非希腊文明内容补齐

交付物：

- Egyptian、Nordic、Chinese、Atlantean 生物美术、动作、音效、粒子 polish。
- 以 `god_power_gameplay_acceptance_package` 为清单完成五文明 52 个神力逐项玩法验收。
- Atlantean 神力矩阵已扩展到 10 项，后续重点转为玩法、图标和粒子 polish。

退出标准：

- 每个文明都有完整可玩闭环和一批可辨识神话单位。

### M5：UI、文案、玩家指南

交付物：

- 守护者交易 UI 最终美术。
- 神力图鉴/文明进度/神殿状态/证书余额指南已具备静态实现；发布前继续做真实分辨率和最终美术验收。
- 中文、英文、日文文案统一。

退出标准：

- UI 不溢出，状态反馈明确，玩家不用看源码也能理解系统。

### M6：发布候选

交付物：

- 全量构建。
- 全量资源 JSON 校验。
- 全量实体/结构/NBT/UI/粒子/audio gate。
- 玩家测试指南、CHANGELOG、release checklist。

退出标准：

- 没有 P0/P1 阻断项。
- 用户确认视觉和玩法质量达到 1.0。

## 6. 验证命令

当前不做 MC 实机截图，除非用户重新要求。

基础验证：

```bash
python3 tools/mythic_age_1_0_gap_audit.py audit
python3 tools/entity_validation_pipeline.py audit
python3 tools/structure_validation_pipeline.py audit
python3 tools/ui_validation_pipeline.py audit
python3 tools/particle_validation_pipeline.py audit
python3 tools/god_power_particle_validation_pipeline.py audit
python3 tools/god_power_gameplay_acceptance_package.py audit
python3 tools/greek_runtime_model_quality_gate.py audit
python3 tools/egyptian_runtime_model_quality_gate.py audit
python3 tools/nordic_runtime_model_quality_gate.py audit
python3 tools/chinese_runtime_model_quality_gate.py audit
python3 tools/atlantean_runtime_model_quality_gate.py audit
./gradlew build --offline
```

注意：

- `mythic_age_1_0_gap_audit.py` 输出 `NOT READY` 是预期行为，表示 1.0 缺口仍存在。
- `./gradlew build --offline` 在当前沙盒内可能因工作区外 Gradle wrapper 缓存锁权限失败，需要授权后运行。

## 7. 下一步主攻方向

1. 先完成 M0/M1，不继续无设计地堆新内容。
2. 用 gap audit 固化当前事实，防止旧文档误导。
3. 优先补 NBT schema 目标表和五文明美术 bible。
4. 以 Greek 为 1.0 质量样板做人工视觉验收和动作 polish。
5. 再横向补齐非希腊文明的音效、动作、粒子、UI 和玩法验收。
