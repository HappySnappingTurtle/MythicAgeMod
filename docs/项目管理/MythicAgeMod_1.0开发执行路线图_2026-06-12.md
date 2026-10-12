# MythicAgeMod 1.0 开发执行路线图

生成日期：2026-06-12

> 2026-06-15 更新：本文中关于 NBT 真实重启仍为 P0、21 个音频必须替换、以及旧生物头身比例口径的判断已被最新 gap audit 和 `docs/项目管理/MythicAgeMod_1.0发布执行依据_2026-06-15.md` 取代。后续执行以 2026-06-15 文档和 `docs/preview_assets/1_0_gap_audit/gap_report.json` 为准。

本文档把 1.0 总纲、五文明内容矩阵、美术 Bible、NBT 目标表和当前 gap audit 收束为后续开发执行依据。它不是发布说明，也不是“当前已经完成”的声明。当前 1.0 状态仍为 `NOT READY`。

当前静态证据入口：

- 总览 dashboard：`docs/preview_assets/1_0_gap_audit/release_acceptance_dashboard.html`
- 当前 gap report：`docs/preview_assets/1_0_gap_audit/gap_report.json`
- 生物视觉验收包：`docs/preview_assets/creature_visual_acceptance/creature_visual_acceptance_package.json`
- 生物动作验收包：`docs/preview_assets/creature_action_acceptance/creature_action_acceptance_package.json`
- 生物音频最终混音验收包：`docs/preview_assets/audio_validation/creature_audio_final_mix_acceptance_package.json`
- 神力玩法验收包：`docs/preview_assets/god_power_gameplay/god_power_gameplay_acceptance_package.json`
- NBT 发布验收包：`docs/preview_assets/nbt_roundtrip_validation/nbt_release_acceptance_package.json`

## 1. 当前真实状态

静态证据证明：

- 1.0 生物 roster 已扩展到 5 文明 30 个生物，不再只是希腊 10 个。
- 52 个非神殿生成神力已有静态玩法验收板。
- 15 个 canonical 神殿结构存在并通过结构静态 gate。
- 14 个 NBT owner 有 schema 或等价验证证据。
- 守护者交易 UI 和神力图鉴 UI 已通过静态 UI gate。
- 自定义粒子和神力粒子矩阵已有静态 gate。
- 149 个生物音频事件已有本地试听入口。

但 1.0 仍未完成：

- 生物模型仍没有通过最终人工视觉验收；希腊模型仍需以更高精度重审，非希腊生物多数只是工程可读。
- 动作只是静态合同通过，受击、死亡、特殊、飞行、骑乘和多部件联动没有完成观感验收。
- NBT 发布验收包、真实重启周期和运行时实体探针已通过；后续作为回归 gate 保持，不再是当前 P0 blocker。
- 52 个神力没有完成逐项玩法、失败路径、平衡和性能验收。
- 音频当前 replacement required=0，但 21 个 final-mix candidate creature 和 149 个事件仍需要最终听感/响度验收。
- UI、结构、粒子通过的是静态下限，不等于最终美术、实机可读性和玩家体验完成。

## 2. 1.0 完成定义

1.0 的目标是五文明基础可玩版本。每个文明必须形成闭环：

1. 生物：有高辨识度模型、纹理、动作、音效、粒子、生成或召唤入口。
2. 神力：有解锁来源、证书消耗、冷却、目标选择、失败反馈、粒子、音效、UI 说明。
3. 结构：有 small/medium/large canonical 神殿，功能点位和文明建筑语言明确。
4. 证书经济：有可重复、可解释、可测试的来源、消耗、防刷和 UI 提示。
5. NBT：所有长期状态有 schema、round-trip、重启周期验证和默认值策略。
6. 验收：静态 gate、Blockbench/资源预览、人工视觉/动作/听感、玩法测试和构建均通过。

不能用以下证据替代完成：

- 不能用 `.bbmodel` 存在替代“模型好看”。
- 不能用 runtime model gate 替代“眼睛、嘴、武器、翅膀、多头、尾巴可读”；头部比例门通过也不等于人工审美通过。
- 不能用 `setAngles` 存在替代“动作可信”。
- 不能用 PNG 存在替代“UV 正确和纹理无占位感”。
- 不能用神力注册替代“玩法可用、失败路径正确、平衡合理”。
- 不能用源码级 NBT round-trip 替代真实存档重启 verify。

## 3. 模块开发范围

### 3.1 代码与玩法

必须完成：

- 冻结五文明证书经济矩阵：来源、掉落、交易消耗、冷却、防刷、UI 提示。
- 给每个文明建立最小可玩闭环任务表：神殿发现、守护者互动、证书获取、神力解锁、神力使用。
- 对 52 个神力逐项补玩法验收结果：目标合法性、失败提示、冷却、消耗、粒子遮挡、性能风险。
- 建立配置项：生成率、神力冷却、证书产出、交易冷却、粒子密度、调试开关。

产物：

- `docs/系统设计/MythicAgeMod_1.0_五文明证书经济矩阵.md`
- `docs/系统设计/MythicAgeMod_1.0_神力逐项玩法验收结果.md`
- 对应代码配置和 validation pipeline 更新。

### 3.2 模型

必须完成：

- 30 个生物逐个设计冻结：体量、轮廓、部件层级、Blockbench group、Java runtime 映射。
- 希腊 10 个作为质量样板重新审美验收，尤其是 Hydra、Cyclops、Pegasus、Medusa、Chimera、Manticore。
- 非希腊 20 个提升到“神话单位一眼可辨”，不能只靠颜色区分文明。
- 巨型、飞行、多头、多尾、武器单位必须增加独立部件，不允许用单一臃肿主体表达。
- 所有生物禁止通过放大头部弥补细节不足；头部必须匹配身体比例，细节精度来自独立眼、嘴、鼻/吻、耳、角、鬃毛、翅膀、尾巴、武器和固定 UV。

关键造型目标：

- Hydra：低伏长身，头颈比身体更抢眼，多头扇形清楚，避免身大头小。
- Cyclops：巨大人形、中央独眼、厚眉骨、嘴、巨大石棒。
- Pegasus：纯白主色，马头、耳、鼻口、鬃毛、羽翼、骑乘背线都要清楚。
- Medusa：凝视眼、嘴、蛇发端点和蛇尾轮廓可读。
- Egyptian：胡狼吻、甲虫壳、绷带、狮身人面、太阳猎鹰翼。
- Nordic：冰须/冰刺、翼盔、狼牙、亡灵盾、矮人武器。
- Chinese：龙须角爪、麒麟角鬃、僵尸符箓、九尾展开、石狮卷鬃。
- Atlantean：水晶背棘、山铜核心、海妖面部/鳍冠、潮汐甲壳、深渊鳐翼面。

产物：

- `docs/美术资源/<civ>_creatures/design/<creature>.md`
- `docs/美术资源/<civ>_creatures/blockbench/<creature>.bbmodel`
- `docs/preview_assets/<civ>_creatures/<creature>/front.png`
- `docs/preview_assets/<civ>_creatures/<creature>/side.png`
- `docs/preview_assets/<civ>_creatures/<creature>/three_quarter.png`

### 3.3 动作

必须完成：

- 每个生物至少定义 idle、walk/run、attack、special、hurt、death。
- 飞行/骑乘单位定义 takeoff、fly、land、riding_fly。
- 多部件单位定义分部动作节奏：Hydra 多头、Chimera 三头、Manticore 尾刺、Medusa 蛇发、Azure Dragon 身段、Crystal Leviathan 背棘。
- 服务端技能状态和客户端模型动作之间建立明确触发关系。

产物：

- 每个生物设计包中的动作表。
- `docs/preview_assets/creature_action_acceptance/` 下的动作 review board 更新。
- 源码中实体状态、模型动画和粒子/音效挂点一致。

### 3.4 纹理与 UV

必须完成：

- 30 个生物使用 UV 驱动纹理，不允许最终资源停留在随机程序面板。
- 眼睛、嘴、鼻/吻、角、爪、武器、符号、翼纹、尾端必须固定 UV 区。
- 每个文明建立 8-12 色主调色板，并区分材质亮面、暗面和边缘色。
- Blockbench 三视角和 texture fit 同时确认无空白、透明破面和关键面错位。

产物：

- UV 模板图。
- 完成纹理图。
- texture fit 报告。
- 纹理审美验收记录。

### 3.5 UI

必须完成：

- 守护者交易 UI 展示文明主题、证书余额、需求材料、冷却、失败原因。
- 神力图鉴展示文明、来源、消耗、冷却、效果、失败路径。
- 神殿/神像/圣物状态展示有效/无效、缺失部件、产出倒计时、存储量。
- 做真实分辨率、长文本、多语言和键鼠/手柄可用性验收。

产物：

- `tools/ui_validation_pipeline.py` 继续覆盖静态结构。
- UI 预览截图或浏览式资源板。
- 多语言 key 检查报告。

### 3.6 美术

必须完成：

- 美术 Bible 成为所有资产制作前置 gate。
- 每个文明有统一材质、图标、粒子、建筑、生物和 UI 风格。
- 所有生物、图标、粒子、结构不得继续依赖“只换色”的占位逻辑。

产物：

- 五文明视觉板。
- 生物/建筑/UI/图标/粒子风格检查表。
- 人工美术验收记录。

### 3.7 粒子

必须完成：

- 每个神力至少定义启动、持续、命中/结束三段效果。
- 每个核心生物技能有主题粒子，粒子不能遮挡关键模型读形。
- 所有神力通过 `MythicParticleEffects` 的密度、距离和上限控制。
- 大范围世界编辑、爆炸、延迟线程类神力做性能和降级验收。

产物：

- 神力粒子矩阵。
- 生物技能粒子矩阵。
- 压力测试和低配降级记录。

### 3.8 音效

必须完成：

- 30 个生物完成 ambient、hurt、death、attack、special；飞行/水中/大型单位补 step/fly/swim。
- 当前音频不再有 replacement required 项；21 个 final-mix candidate creature 必须完成听感确认或最终混音。
- 每个神力有 cast 和 impact，持续型神力补 loop/end。
- UI 点击、交易成功/失败、证书产出补反馈音。

产物：

- 音效方向设计包。
- 最终 OGG、`sounds.json`、字幕 key 一致。
- 本地试听 index 和人工听感验收记录。

### 3.9 NBT 与结构

必须完成：

- 保持现有 `runtime_only` 决策，但任何升级为长期玩法的状态必须先转 `required`。
- 证书经济、交易冷却、神殿/神像/圣物、长期神力和召唤归属必须有 schema 和 probe。
- 真实 MC 存档重启周期已通过；后续新增长期状态时必须先补 schema、probe 和重启回归。
- 结构继续禁止非 canonical 路径和数据包污染。
- 神殿建筑需要最终审美、地形融合和功能点位人工验收。

产物：

- `docs/系统设计/MythicAgeMod_1.0_NBT状态持久化目标_2026-06-11.md` 更新。
- `docs/preview_assets/nbt_roundtrip_validation/nbt_release_acceptance_package.json`
- 真实重启 verify 报告：`run/visual_probe/nbt_restart_verify_report.json`

## 4. 优先级路线

### P0：不能发布的阻塞

1. 生物人工视觉验收未完成。

P0 完成标准：

- 30 个生物逐个有人工视觉结论，失败项回到模型/纹理实现。
- 人工视觉验收必须确认头部不大于身体比例，且没有因缩头损失眼、嘴、鼻/吻、耳、角、鬃毛、翼、尾等神话细节。

### P1：1.0 质量主线

1. 生物动作观感验收。
2. 52 个神力玩法验收。
3. 149 个音频事件最终听感验收，重点覆盖 21 个 final-mix candidate creature。
4. 五文明粒子和图标 polish。
5. UI 和结构人工美术验收。

P1 完成标准：

- 动作 review board 不再只是静态合同，必须有人工结论。
- 神力验收包记录逐项结果、风险和修复状态。
- 音频最终混音验收 `final_mix_acceptance_ok=true`。

### P2：发布整理

1. 版本号、README、CHANGELOG、玩家指南统一。
2. release checklist 明确构建、资源、NBT、视觉、玩法、音频和文档 gate。
3. 旧的“已完成/可发布”文档保持归档，不作为当前状态依据。

## 5. 执行顺序

1. 冻结证据口径：以 `gap_report.json` 和 dashboard 为唯一当前状态入口。
2. 先做希腊 10 生物重审和质量样板，确认模型精细度标准。
3. 按同一标准横向改非希腊 20 生物。
4. 补动作设计表和动作验收结论。
5. 补五文明证书经济矩阵和神力逐项玩法验收。
6. 在允许启动游戏后只执行必要回归、动作/玩法/音频实测；NBT 真实重启作为已通过回归项保留。
7. 做最终音效、粒子、UI、结构美术验收。
8. 收口 release hygiene、玩家文档和 1.0 checklist。

## 6. 当前禁止事项

- 不得把静态 gate 通过写成 1.0 完成。
- 不得在没有设计包的情况下继续堆模型。
- 不得用通用随机纹理作为最终资源。
- 不得把游戏内测试阶段当成造型试错阶段。
- 不得在新增长期状态时绕过 NBT schema、probe 和真实重启回归。
- 不得在用户要求“不启动游戏”时执行 MC 客户端或实机截图任务。
