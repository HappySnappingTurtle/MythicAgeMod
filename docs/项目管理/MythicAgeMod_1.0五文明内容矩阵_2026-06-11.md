# MythicAgeMod 1.0 五文明内容矩阵

生成日期：2026-06-11

本文档冻结 1.0 的最小可玩范围。它不是最终美术验收表，而是后续开发、排期和测试的内容边界：每个文明必须同时具备生物、神力、证书、结构、UI、音效、粒子、NBT 和验证闭环。

## 1. 完成状态定义

| 状态 | 含义 |
| --- | --- |
| `implemented` | 代码或资源已经接入，能被当前构建引用。 |
| `static_gate_passed` | 有脚本检查通过，但只代表静态契约成立。 |
| `needs_design_freeze` | 需要先冻结设计图、玩法表、NBT 状态或 UI 范围，再继续实现。 |
| `needs_art_polish` | 现有资产可运行，但未达到 1.0 美术质量。 |
| `needs_playtest` | 代码存在，但玩法、平衡、生成、交易或消耗尚未逐项验证。 |
| `missing` | 1.0 必需但当前缺失。 |

## 2. 1.0 最小可玩范围

| 维度 | 1.0 最低要求 |
| --- | --- |
| 文明 | Greek、Egyptian、Nordic、Chinese、Atlantean 五文明均进入可玩闭环。 |
| 生物 | Greek 10 个，其他四文明各 5 个；每个有设计包、模型、纹理、动作、音效、粒子、生成/召唤/兑换入口和文案。 |
| 神力 | Greek/Egyptian/Nordic/Chinese/Atlantean 均冻结 10+ 或 10 项核心矩阵；不允许长期停留在无设计的 MVP。 |
| 证书 | 每个文明有可重复、可解释、可测试的获取与消耗规则。 |
| 神殿结构 | 每文明 small/medium/large 三个 canonical NBT，含功能点位和建筑美术验收。 |
| UI | 守护者交易、神力详情、文明进度、神殿状态、证书余额或等价指南。 |
| NBT | 生物状态、神力持续状态、证书经济、交易冷却、神殿/神像/圣物状态、物品数据均有 schema 和 round-trip 验证。 |
| 验证 | 构建、资源 JSON、实体/NBT、结构、UI、粒子、runtime model、动作验收包、人工视觉验收。 |

## 3. 文明矩阵

### 3.1 Greek

| 模块 | 当前事实 | 1.0 要求 | 状态 | 下一步 |
| --- | --- | --- | --- | --- |
| 生物 roster | 10 个：Minotaur、Cyclops、Centaur、Hydra、Medusa、Nemean Lion、Manticore、Chimera、Colossus、Pegasus | 10 个全部达到高精度神话特征 | `needs_art_polish` | 人工确认三视角，针对比例、眼嘴、武器、翅膀、多头、蛇发逐项 polish |
| 神力 | 12 个希腊神力已注册，并纳入 52 项神力玩法验收包 | 每个有冷却、消耗、目标、粒子、音效、失败提示和玩法验收 | `needs_playtest` | 按 `god_power_gameplay_acceptance_package` 做逐项手感、平衡和失败路径验收 |
| 证书 | 希腊证明由村民朝圣/神殿聚集链路产生 | 规则文档、产出频率、防刷和 UI 提示 | `needs_playtest` | 验证世界 tick、村民 AI 和证书产出 |
| 神殿结构 | canonical small/medium/large 存在 | 建筑美术、功能点位、守护者/圣物/神像布局 | `static_gate_passed` | 清理旧结构后做建筑设计验收 |
| UI | 守护者交易 UI 和神力图鉴 UI 已过静态 gate | 希腊主题、证书余额、神力详情、神殿状态 | `static_gate_passed` | 做真实分辨率、长文本和最终美术验收 |
| 音效 | 希腊生物音效已过技术 gate，Minotaur 以 charge 作为 attack/special 豁免口径 | 每个生物 ambient/hurt/death/attack/special/step，字幕和方向设计明确 | `technical_gate_passed` | 做最终听感、响度和战斗可读性验收 |
| 粒子 | 神力静态矩阵已覆盖 12/12，生物技能有静态合同 | 奥林匹斯蓝金/青铜主题三段式效果 | `needs_art_polish` | 做文明主题粒子图集、三段式艺术验收和压力测试 |
| NBT | 多数希腊生物被标为 vanilla-only | 需要状态的技能冷却、飞行/骑乘、召唤归属等持久化 | `needs_design_freeze` | 先定义哪些状态必须持久化 |

### 3.2 Egyptian

| 模块 | 当前事实 | 1.0 要求 | 状态 | 下一步 |
| --- | --- | --- | --- | --- |
| 生物 roster | Anubis Warrior、Scarab Guardian、Mummy Soldier、Sphinx Sentinel、Sun Falcon 已接入 | 五个单位都具备埃及轮廓、动作、音效、粒子和玩法 | `needs_art_polish` | 强化胡狼头、圣甲虫甲壳、木乃伊绷带、狮身人面、太阳猎鹰轮廓 |
| 神力 | 10 个埃及神力已注册，并纳入 52 项神力玩法验收包；其中 10 项存在 `void_cast_always_success` 风险 | 每个神力有太阳/沙暴/复活/审判主题效果和验收 | `needs_playtest` | 按验收包重点确认无目标/无敌人场景的失败反馈、沙暴/龙卷/陨石性能和复活边界 |
| 证书 | 埃及神像/基座链路存在 | 神像产出、拆卸、存档、UI 提示和防刷 | `needs_playtest` | 验证 `EgyptianStatuePedestalBlockEntity` 和证书管理 |
| 神殿结构 | canonical small/medium/large 存在 | 砂岩、黄金、青金石、太阳和象形符号建筑语言 | `static_gate_passed` | 做建筑美术设计包和功能点位检查 |
| UI | 共用守护者交易 UI；神力图鉴显示埃及证书来源和法术书 | 埃及主题、神像/证书产出说明 | `static_gate_passed` | 做真实分辨率和文明主题美术验收 |
| 音效 | 5 个埃及生物已有程序化首版 OGG、字幕和音效方向 | 每个生物完整音效并体现金砂、太阳、墓室、胡狼/圣甲虫特征 | `needs_final_mix` | 按音效方向替换或混音为最终文明主题资源 |
| 粒子 | 神力静态矩阵已覆盖 10/10，生物技能有静态合同 | 金砂、太阳光、灵魂审判、复活心形粒子 | `needs_art_polish` | 补埃及专属粒子调色板和实机可读性验收 |
| NBT | 神像基座 schema 已覆盖，生物状态未覆盖 | 神像、证书、神力持续状态、生物特殊状态 | `needs_design_freeze` | 扩展 schema 目标到神力和生物 |

### 3.3 Nordic

| 模块 | 当前事实 | 1.0 要求 | 状态 | 下一步 |
| --- | --- | --- | --- | --- |
| 生物 roster | Frost Giant、Draugr Warrior、Valkyrie、Fenrir Wolf、Dwarf Berserker 已接入 | 五个单位都具备北欧神话轮廓和动作 | `needs_art_polish` | 强化冰霜巨人、亡灵战士、女武神飞行、芬里尔狼、矮人武器 |
| 神力 | 10 个北欧神力已注册，Walking Woods 有行走树，并纳入 52 项神力玩法验收包 | 每个神力完成冰、狼嚎、瓦尔哈拉、诸神黄昏等主题验收 | `needs_playtest` | 按验收包验证召唤、矿坑、冰冻、世界编辑和持续天气类神力的安全与平衡 |
| 证书 | 北欧证明通过战斗事件产出 | 掉落概率、击杀类型、防刷和玩家提示 | `needs_playtest` | 验证战斗事件和掉落规则 |
| 神殿结构 | canonical small/medium/large 存在 | 冷灰石、深木、冰蓝、卢恩符文建筑语言 | `static_gate_passed` | 建筑美术和功能点位 polish |
| UI | 共用守护者交易 UI；神力图鉴显示北欧战斗证明来源 | 北欧主题、战斗证明来源说明 | `static_gate_passed` | 做真实分辨率和文明主题美术验收 |
| 音效 | 5 个北欧生物已有程序化首版 OGG、字幕和音效方向；行走树仍需纳入 1.0 音效口径 | 生物、行走树、神力、UI 音效 | `needs_final_mix` | 按冰裂、铁器、骨响、卢恩方向做最终混音，并补行走树口径 |
| 粒子 | 神力静态矩阵已覆盖 10/10，生物技能有静态合同 | 冰蓝、雪雾、符文、雷神冲击 | `needs_art_polish` | 补北欧专属粒子图集、三段式艺术验收和压力测试 |
| NBT | 行走树 schema 已覆盖，北欧生物状态未覆盖 | 行走树、召唤归属、守卫点、神力持续状态 | `needs_design_freeze` | 扩展召唤物和神力状态 schema |

### 3.4 Chinese

| 模块 | 当前事实 | 1.0 要求 | 状态 | 下一步 |
| --- | --- | --- | --- | --- |
| 生物 roster | Azure Dragon、Qilin Guardian、Jiangshi、Nine-tailed Fox、Stone Lion Guardian 已接入 | 五个单位体现中国神话特征和动作 | `needs_art_polish` | 强化龙须/爪/躯段、麒麟角鬃、僵尸符箓、九尾、石狮卷鬃 |
| 神力 | 10 个中国神力已注册，并纳入 52 项神力玩法验收包 | 祈雨、土地、五雷、太极、再造等效果可玩 | `needs_playtest` | 按验收包验证农业/资源/召唤/护盾/世界修复类神力的目标选择和反馈 |
| 证书 | 中国证明链路需要确认真实接入 | 清楚的村庄/人口/神殿产出规则 | `needs_design_freeze` | 冻结并验证证书获取方式 |
| 神殿结构 | canonical small/medium/large 存在 | 朱红、金、玉色、云纹、龙纹、符箓建筑语言 | `static_gate_passed` | 建筑美术和功能点位 polish |
| UI | 共用守护者交易 UI；神力图鉴显示中国人口/村庄产出说明 | 中国主题、人口/神殿产出说明 | `static_gate_passed` | 做真实分辨率和文明主题美术验收 |
| 音效 | 5 个中国生物已有程序化首版 OGG、字幕和音效方向 | 生物、神力、UI、证书产出音效 | `needs_final_mix` | 按玉铃、云气、符箓、龙息方向替换最终资源 |
| 粒子 | 神力静态矩阵已覆盖 10/10，生物技能有静态合同 | 红金符箓、云气、雷纹、玉色护盾 | `needs_art_polish` | 补中国专属粒子调色板、符箓/云气图集和实机可读性验收 |
| NBT | 生物状态和证书链路 schema 未覆盖 | 飞行、魅惑、符箓束缚、神力持续和证书状态 | `needs_design_freeze` | 扩展中国状态 schema |

### 3.5 Atlantean

| 模块 | 当前事实 | 1.0 要求 | 状态 | 下一步 |
| --- | --- | --- | --- | --- |
| 生物 roster | Crystal Leviathan、Orichalcum Golem、Siren Priestess、Tidal Guardian、Abyssal Ray 已接入 | 五个单位体现海洋、水晶、失落科技 | `needs_art_polish` | 强化水晶背棘、山铜核心、海妖脸/鳍冠、潮汐甲壳、深渊鳐轮廓 |
| 神力 | 10 个 1.0 神力已注册：潮汐奔流、深流跃迁、水晶屏障、珍珠丰收、山铜熔炉、海妖之歌、利维坦召唤、深渊裂隙、时滞静域、海洋震怒，并纳入 52 项神力玩法验收包 | 每个神力完成冷却、消耗、提示、粒子、音效和玩法验收 | `needs_playtest` | 逐项验证 10 神力手感、平衡、失败反馈和海洋/水晶主题可读性 |
| 证书 | 证书/守护者基础存在，获取方式未冻结 | 清楚的海洋/遗迹/神殿获取闭环 | `needs_design_freeze` | 冻结 Atlantean 证书来源 |
| 神殿结构 | canonical small/medium/large 存在 | 青蓝发光、水晶、失落科技几何建筑语言 | `static_gate_passed` | 建筑美术和功能点位 polish |
| UI | 共用守护者交易 UI；神力图鉴显示亚特兰蒂斯 10 神力矩阵和证书来源 | 亚特兰蒂斯主题、10 神力说明 | `static_gate_passed` | 做真实分辨率和文明主题美术验收 |
| 音效 | 5 个亚特兰蒂斯生物已有程序化首版 OGG、字幕和音效方向 | 海洋、水晶、机械、深渊主题音效 | `needs_final_mix` | 按水压、水晶、山铜、深渊脉冲方向替换最终资源 |
| 粒子 | 神力静态矩阵已覆盖 10/10，生物技能有静态合同 | 青蓝水晶、水泡、冲击波、深渊裂隙三段式效果 | `needs_art_polish` | 补专属粒子图集、最终调色和逐项玩法验收 |
| NBT | 生物状态、神力持续、证书来源未覆盖 | 水下/漂浮 AI、神力屏障/裂隙、证书状态持久化 | `needs_design_freeze` | 扩展 Atlantean 状态 schema |

## 4. 跨文明共用任务

| 模块 | 任务 | 验收方式 |
| --- | --- | --- |
| 内容冻结 | 每个文明固定生物、神力、证书来源、结构、交易、UI 范围 | 本矩阵和后续 issue/任务都能映射到具体文明和模块 |
| 美术 bible | 五文明视觉语言、调色板、材质、图标、粒子、禁用事项 | `MythicAgeMod_1.0美术Bible_2026-06-11.md` |
| 生物视觉验收 | 30 生物设计包、概念图、Blockbench 三视图、UV、贴图贴合和 runtime model gate 聚合 | `tools/creature_visual_acceptance_package.py audit`、`docs/美术资源/entity_validation/creature_visual_acceptance_package.md` |
| 生物动作验收 | 30 生物 idle、移动、攻击/特殊、受击、飞行/滑游/骑乘和多部件联动静态证据聚合；人工动作观感仍待验收 | `tools/creature_action_acceptance_package.py audit`、`docs/美术资源/entity_validation/creature_action_acceptance_package.md` |
| 神力玩法验收 | 52 个非神殿生成神力的注册、施法路由、冷却/反馈、神力图鉴、主语言名称、粒子矩阵静态通过；人工玩法和平衡仍待验收 | `tools/god_power_gameplay_acceptance_package.py audit`、`docs/系统设计/MythicAgeMod_1.0_神力玩法验收包_2026-06-12.md` |
| NBT schema | 生物、神力、证书、交易、结构、物品状态目标；静态合约与运行中实体探针通过，真实 MC 重启周期仍待 verify | `MythicAgeMod_1.0_NBT状态持久化目标_2026-06-11.md`、`tools/nbt_release_acceptance_package.py audit` |
| 音效矩阵 | 生物音效技术 gate、字幕 gate、音效方向设计包和最终混音验收包；牛头人技术占位 + 非希腊 20 个程序首版仍需最终替换/混音 | `tools/audio_validation_pipeline.py`、`tools/creature_audio_final_mix_acceptance_package.py audit`、`creature_audio_art_direction_2026-06-12.json` |
| UI 范围 | 守护者交易、神力图鉴、证书余额、文明/神殿状态 | `tools/ui_validation_pipeline.py audit` 和后续真实截图验收 |
| 发布卫生 | 旧文档状态提示、版本号、README、CHANGELOG | release checklist |

## 5. 当前优先级

1. `P0`：基于全量生物视觉验收包完成人工视觉验收，并完成真实 NBT 存档重启周期验证。
2. `P1`：基于全量生物动作验收包完成人工动作观感验收，覆盖受击/死亡/特殊/飞行/骑乘/多部件联动。
3. `P1`：牛头人技术占位和非希腊 20 个程序首版音效最终混音、五文明 52 个神力逐项实测与最终图标/粒子 polish、结构美术/功能点位、UI 真实渲染与最终美术验收。
3. `P2`：版本号、旧文档、玩家指南和 release checklist 统一。

## 6. 下一步执行顺序

1. 使用美术 bible 反查所有生物设计包，确认文明风格是否一致。
2. 使用 NBT 目标表扩展 `entity_validation_pipeline.py` 或新增专门 schema gate。
3. 以 `creature_visual_acceptance_package.md` 为清单，先验收 Greek 10 生物，再横向验收其他 20 个文明生物。
4. 使用 `creature_action_acceptance_package.md` 对 30 生物做动作预览或人工观察验收，再横向补非希腊生物最终混音和动作 polish。
5. 对五文明 52 个神力做玩法、粒子、音效和证书消耗验收，优先处理验收包中的失败路径、线程延迟和大范围世界编辑风险。
