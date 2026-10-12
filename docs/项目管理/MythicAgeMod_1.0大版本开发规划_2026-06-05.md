# MythicAgeMod 1.0 大版本开发规划

> 2026-06-15 纠偏说明：本文档保留历史规划语境，其中关于 Hydra / Pegasus V2 已精修或造型冻结的表述已经过时。当前执行依据以后续 `MythicAgeMod_1.0发布执行依据_2026-06-15.md`、`MythicAgeMod_1.0开发任务台账_2026-06-15.md`、`pegasus_hydra_current_design_verdict_2026-06-15.md` 为准：天马和九头蛇旧实现均已被判定为不正确，V3 也仍是候选设计，未获用户签收前禁止继续建模。

生成日期：2026-06-05

> 状态提示（2026-06-11）：本文是 2026-06-05 阶段规划，部分“当前状态/缺口”已被后续实现和审计更新。后续执行以 `docs/项目管理/MythicAgeMod_1.0当前缺口与开发总纲_2026-06-11.md` 和 `docs/preview_assets/1_0_gap_audit/gap_report.json` 为最新依据。

本文档用于替代“项目接近发布”的旧口径，作为后续开发 1.0 大版本的执行依据。结论基于当前仓库事实、continuity 记录、现有项目文档、实体/资源/NBT 校验脚本和离线构建结果。

## 1. 调查范围与当前结论

### 1.1 已核对范围

- 实体注册：`src/main/java/com/example/mythic_age/entity/ModEntities.java`
- 物品和神力书注册：`src/main/java/com/example/mythic_age/item/ModItems.java`
- 神力实现入口：`src/main/java/com/example/mythic_age/item/SpellbookItem.java`
- 文明、村庄和结构生成：`src/main/java/com/example/mythic_age/worldgen/`
- 方块、模型、纹理和数据包资源：`src/main/resources/`
- 希腊生物设计与 Blockbench 视觉产物：`docs/美术资源/greek_creatures/`
- NBT 静态结构校验：`tools/entity_validation_pipeline.py`
- 希腊生物视觉流程校验：`tools/greek_creature_visual_pipeline.py`
- 旧项目管理文档：`docs/项目管理/`

### 1.2 当前基线验证

- `python3 tools/entity_validation_pipeline.py audit`：通过。
  - 该结果只代表实体注册清单和已纳入脚本的 NBT 字段静态读写对称。
  - 不能代表所有结构 NBT 文件齐全，也不能代表 MC 实机视觉已通过。
- `python3 tools/greek_creature_visual_pipeline.py audit`：通过。
  - 该结果只代表设计包、`.bbmodel`、UV 模板、纹理和预览文件存在且满足脚本检查。
  - 用户已实测指出模型美术质量不足，因此该检查不能作为 1.0 美术质量通过依据。
- `./gradlew build --offline`：通过。
  - 沙盒内首次因本机 Gradle wrapper 缓存锁文件权限失败。
  - 非沙盒执行后构建成功，说明当前代码基线可以编译。

### 1.3 总体判断

当前项目不是“从零开始”，但也不是“接近 1.0 发布”。仓库已经有多文明系统骨架、神力书、证书、神殿方块、守护者、希腊生物和验证工具；真正缺失的是：

- 1.0 产品范围没有被冻结。
- 希腊、埃及、北欧、中国、亚特兰蒂斯生物已进入 design-first 视觉链路和运行模型质量门，但美术质量仍未达 1.0。
- 五文明生物已经有工程可验收的 V2/runtime 契约，下一阶段的主要矛盾转为高精度美术、动作、音效、粒子表现和玩法实测。
- 结构 NBT 文件和数据包模板池路径已收口到 15 个 canonical 神殿；后续重点转为最终建筑审美和实机视觉验收。
- Atlantean 神力体系已补齐 10 个 1.0 法术书入口和独立首版图标；逐项玩法验收、平衡、最终图标 polish 和粒子全覆盖仍未完成。
- 视觉验收脚本偏向“文件存在”和“技术通过”，不能替代设计质量验收。
- NBT 校验已覆盖关键实体/方块实体、结构 NBT、证书目标表和源码级重启契约；真实 MC 存档重启、物品状态升级和证书经济实测仍是发布前风险。

## 2. 当前已实现内容

### 2.1 多文明基础框架

代码已定义 5 个文明：

- Greek
- Egyptian
- Chinese
- Atlantean
- Nordic

主要证据：

- `GodPowerCertificateItem.MythologyType` 定义 5 类神力证明。
- `MythicVillageTypes.VillageType` 定义 5 类神话村庄。
- `BiomeCivilizationMapper` 为 5 类文明配置了生物群系映射。
- `ModBlocks` 已注册五文明神殿方块、神像和神像分段方块。
- `ModEntities` 已注册五文明神殿守护者和五文明自定义神殿守护者。

当前问题：

- 这些框架多为基础设施，不等于 5 个文明已经可玩。
- 文明之间缺少统一内容矩阵、统一获取链路、统一验收标准。

### 2.2 希腊生物现状

当前已接入的希腊生物：

- `minotaur`
- `cyclops`
- `centaur`
- `hydra`
- `medusa`
- `nemean_lion`
- `manticore`
- `chimera`
- `colossus`
- `pegasus`

已有内容：

- 实体类、注册、属性、自然生成。
- Java renderer/model。
- 刷怪蛋和部分语言项。
- 程序化纹理。
- 当前希腊视觉管线覆盖 10 个希腊生物的设计包、概念图、`.bbmodel`、UV 模板、Blockbench 预览和 contact sheet，`minotaur` 已从旧 legacy model 纳入同一套设计先行验收。

当前问题：

- 用户实测指出整体模型过小、精细度不足、纹理有空白、眼睛/嘴/神话特征不正确。
- 已对九头蛇、独眼巨人、天马建立 V2 造型冻结，并重新生成 Blockbench 资产/贴图/预览，且同步 Java 运行模型关键部件。
- 已对半人马、尼米亚狮、蝎尾狮、奇美拉建立第二批 V2 造型冻结，并重新生成设计包、概念图、Blockbench 资产、UV、贴图和三视角预览，且同步 Java 运行模型关键部件。
- 已对牛头人、美杜莎、青铜巨像建立第三批 V2 造型冻结，并重新生成希腊 10 生物视觉流水线资产；实体 visual gate 现在要求牛头人出现在 Greek design manifest 中。
- 牛头人已从 `0.9 x 2.1` 调整为 `1.35 x 2.75` 大型迷宫怪体量，并强化牛头、双角、牛吻、眼嘴、蹄足、牛尾和长柄双刃斧。
- 九头蛇 V2 已转向“低伏长身、五个比例合理的蛇头扇形、长尾、多头压迫感”，细节必须来自独立眼、颌、獠牙、角/毒腺和头颈连接；仍需最终美术人工验收。
- 独眼巨人 V2 已强化清晰人形巨怪、单眼面部和多部件巨大石棒，但仍需最终石棒材质和攻击动作 polish。
- 天马 V2 已强化纯白马头、长颈、耳朵、鼻口、鬃毛、羽毛分段和骑乘空间，并保留自主飞行和骑乘飞行。
- 半人马 V2 已强化前置人身连接、头脸、双肩、弓弦、四蹄和尾端毛束，避免退化成普通马或骑手。
- 尼米亚狮 V2 已强化超大放射鬃毛、厚肩胸、短吻、獠牙、前爪和尾端毛球。
- 蝎尾狮 V2 已强化兽脸獠牙、分段膜翼/翼骨和高过背线的蝎尾毒刺。
- 奇美拉 V2 已强化狮头主脸、带角羊头/胡须、蛇头尾/上下颚/舌头和背部火焰/翼轮廓。
- 美杜莎 V2 已强化石化凝视眼睛、多束蛇发冠、前/侧/背蛇头端点和盘绕蛇尾。
- 青铜巨像 V2 已强化发光眼缝、冠饰/冠刺、胸甲、腰带、肩甲、臂缝和铜绿板缝。
- 用户已明确当前不需要再进行游戏内验证，因此本阶段只保留 Blockbench/UV/贴图/Java 运行模型/NBT/构建验证。
- 视觉 gate 已覆盖 V2 设计冻结、UV/贴图空白和运行模型关键部件，但仍不能替代最终人工美术验收。

结论：希腊生物是 1.0 的第一条 vertical slice，但当前只能算原型级或技术接入级，不能算 1.0 美术完成。

### 2.3 神力系统现状

当前 `SpellbookItem.SpellType` 已包含：

- 希腊神力：12 个。
- 中国神力：10 个。
- 北欧神力：10 个。
- 埃及神力：10 个。
- 亚特兰蒂斯神力：10 个。
- 神殿生成法术：埃及、中国、亚特兰蒂斯、北欧各 1 个。

当前缺口：

- 亚特兰蒂斯已从 6 个 MVP 扩展到 10 神力 1.0 矩阵；仍需逐项玩法验收、最终图标 polish 和美术粒子全覆盖。
- 神力实现存在大量代码路径，但需要逐项确认是否实际可玩、是否有资源、是否有冷却/消耗/提示/音效/粒子/服务器端安全校验。
- 神力证明获取规则分散在不同系统里，需要统一成 1.0 规则文档和测试矩阵。

### 2.4 村庄、神殿与结构现状

已有内容：

- 五文明世界生成结构 JSON：`data/mythic_age/worldgen/structure/*.json`
- 五文明 template pool JSON：`data/mythic_age/worldgen/template_pool/village/*/town_centers.json`
- 五文明神殿方块模型、blockstate 和纹理。
- `MythicVillageProcessor` 支持优先加载 NBT，缺失时回退代码生成。

关键缺口：

- 当前 canonical `.nbt` 结构文件已补齐：
  - `src/main/resources/data/mythic_age/structures/village/<civilization>/temple_small.nbt`
  - `src/main/resources/data/mythic_age/structures/village/<civilization>/temple_medium.nbt`
  - `src/main/resources/data/mythic_age/structures/village/<civilization>/temple_large.nbt`
- 历史非 canonical 结构文件已清理；后续执行只以 `structures/village/<civilization>/temple_<size>.nbt` 为准。
- template pool 指向的是 canonical 路径：
  - `mythic_age:village/<civilization>/temple_large`
  - `mythic_age:village/<civilization>/temple_medium`
  - `mythic_age:village/<civilization>/temple_small`
- `MythicVillageProcessor.getTempleNBTPath` 已给五文明返回 canonical small 结构路径。
- 当前结构是基础程序生成版，已解决“缺文件”和“路径不一致”问题，并通过文明主题材料、尺寸、功能点位和 template pool gate；建筑美术、空间观感和截图验收仍需后续精修。

结论：结构/NBT 的“文件缺失、路径不一致、功能点缺失”P0/P1 已有静态收口，但五文明神殿仍不能视为最终美术完成；后续重点转为建筑审美和实机视觉验收。

### 2.5 NBT 结构现状

需要区分两类 NBT：

1. 实体/方块实体 NBT 持久化字段。
2. Minecraft 结构文件 `.nbt`。

当前实体/方块实体 NBT 静态校验已覆盖：

- `WalkingTreeEntity`
- `CustomTempleGuardianEntity`
- `MeteorEntity`
- `VillagerAIEnhancementMixin`
- `AnimalLureTowerBlockEntity`
- `AbundanceAltarCoreBlockEntity`
- `RelicPedestalBlockEntity`
- `StatueBaseBlockEntity`
- `EgyptianStatuePedestalBlockEntity`

当前结构 NBT 状态：

- 五文明各尺寸神殿结构文件已达到 15/15。
- `tools/structure_validation_pipeline.py` 已检查结构尺寸、文明主题材料、功能方块位置、守护者生成点推导和路径一致性。
- 结构文件与 template pool 的自动一致性校验已纳入 `structure_report.json`。

## 3. 1.0 版本定位

1.0 不应只是“希腊生物包”。1.0 应该是一个完整可玩的 MythicAgeMod 基础版本：

- 玩家能在世界中遇到不同文明的神话村庄或神殿。
- 每个文明都有自己的神力证明获取方式。
- 玩家能通过神殿、守护者、神像、圣物或挑战解锁神力。
- 每个文明至少有一批可辨识的神话生物或召唤单位。
- 神力、建筑、生物、掉落、交易和说明文案形成闭环。
- 美术资产以“设计先行、Blockbench 预览确认、再接入代码”为标准。

1.0 的核心体验：

1. 探索：发现不同文明区域、村庄、神殿和遗迹。
2. 收集：获得神力证明、圣物、特殊材料。
3. 兑换/解锁：通过守护者或结构功能方块解锁神力书和神话单位。
4. 战斗/召唤：与神话生物战斗，或使用文明神力召唤单位。
5. 建设：使用文明神殿方块、神像和祭坛搭建可交互结构。

## 4. 1.0 开发内容总包

1.0 开发必须同时覆盖代码、模型、动作、纹理、UI、美术、粒子效果、音效、文案和验证工具。后续不能只完成 Java 实体注册就视为一个内容完成，也不能只生成纹理或模型就视为完成。

### 4.1 代码系统

当前已有：

- Fabric/Fabric API 基础初始化。
- 实体、物品、方块、方块实体、音效、物品组注册。
- 多文明类型、神力证明、神殿守护者、村庄文明映射。
- 希腊生物实体链路、北欧行走树、陨石实体。
- 神力书 `SpellbookItem` 中的大量神力实现入口。
- 自定义守护者交易网络包。
- 静态 NBT 校验和 MC visual probe 入口。
- 静态动作/粒子合同门禁已接入 `tools/entity_validation_pipeline.py actions`，覆盖 31/31 个神话生物与召唤物。
- `/mythicage_validate nbt` 运行时实体 round-trip 探针已扩展到五文明新增生物、守护者、行走树和陨石。

1.0 代码交付必须包括：

- 五文明内容注册表，明确每个文明的神力、证书、神殿、守护者、生物、结构、资源和语言项。
- 每个神话生物的实体类、属性、AI、攻击/特殊技能、掉落、生成、召唤或兑换入口。
- 每个神力的服务端安全校验、冷却、消耗、目标选择、效果结算、粒子、音效和反馈消息。
- 证书经济链路：获取、存储、消耗、交易、掉落、防刷和可测试性。
- NBT 持久化：实体、方块实体、物品、交易冷却、神殿状态、结构功能方块。
- 资源和数据包一致性校验：blockstate、model、lang、sounds、loot table、structure pool。
- 配置项：生成率、难度、神力冷却、证书产出、粒子密度和调试开关。

### 4.2 模型

当前已有：

- 希腊 10 个当前生物已进入 design-first Blockbench 视觉流程，含设计包、概念图、`.bbmodel`、UV、贴图和三视角预览。
- 埃及、中国、北欧、亚特兰蒂斯各 5 个生物已有 design-first 视觉流水线、`.bbmodel`、UV、贴图和三视角预览基础版本。
- 希腊、埃及、北欧、中国、亚特兰蒂斯生物已有 Java runtime model quality gate；希腊 10 个、埃及 5 个、北欧 5 个、中国 5 个、亚特兰蒂斯 5 个已具备运行模型关键部件契约。
- 米诺陶、行走树、守护者和陨石相关渲染器。

当前缺口：

- 希腊模型已完成 10 生物 V2 设计冻结和程序化重做，但仍未达到最终人工美术验收。
- 埃及 5 生物已建立 V2 设计冻结和 runtime model quality gate，但仍需最终手工美术、动作和音效 polish。
- 北欧 5 生物已建立 V2 设计冻结和 runtime model quality gate；当前 contact sheet 表明霜巨人、芬里尔、女武神轮廓可读，但亡灵战士和矮人狂战士仍偏方块化，需要美术精修。
- 中国 5 生物已建立 V2 设计冻结和 runtime model quality gate；当前 contact sheet 表明青龙、麒麟、九尾狐、石狮轮廓可读，但九尾狐尾部仍偏条带化、石狮卷鬃偏纹样化、僵尸面部细节不足，需要美术精修。
- 亚特兰蒂斯 5 生物已建立 V2 设计冻结和 runtime model quality gate；当前 contact sheet 表明水晶利维坦、潮汐守卫、深渊鳐轮廓可读，但山铜魔像过于普通方块人、海妖祭司脸部/鳍冠较弱，需要美术精修。
- Java 模型和 Blockbench 设计之间的强制同步规则已覆盖五文明生物，但还需要扩展到后续新增高阶单位、召唤物和非生物实体。

1.0 模型交付必须包括：

- 每个生物的设计包、概念图、`.bbmodel`、Java 模型或导出同步方案。
- 正面、侧面、45 度预览。
- 体量表：碰撞盒、视觉高度、玩家对比、同文明单位对比。
- 部件层级表：group 名称必须和 Java 模型部件命名可追踪。
- 模型验收图：不能只看文件存在，必须人工确认神话特征可读。

### 4.3 动作与动画

当前已有：

- 多数 Java 模型通过 `setAngles` 做待机、移动和攻击摆动。
- 行走树有较明确的攻击动画进度同步。
- 天马实体已有自主飞行与骑乘飞行方向控制。
- 实体动作能力已有静态合同报告：`docs/preview_assets/entity_validation/action_particle_report.json`。

当前缺口：

- 动作多为基础程序动画，缺少按生物设计的动作表。
- 受击、死亡、模型级特殊技能动画、飞行、冲锋、多头攻击、尾刺、石化等仍缺少 Blockbench/运行时动画验收。
- 很多特殊技能只在逻辑和粒子上体现，模型动作没有充分配合。

1.0 动作交付必须包括：

- 每个生物至少定义 idle、walk/run、attack、special、hurt、death。
- 飞行单位额外定义 takeoff、fly、land、riding_fly。
- 多头/多部件单位定义分部动作，例如 Hydra V3 九头错相攻击、Chimera 三头联动、Manticore 尾刺攻击；旧五头 Hydra 只能作为历史 V2 口径，不能作为 1.0 验收依据。
- 关键动作必须有服务端或客户端状态同步方案，不能只依赖 `handSwingProgress` 猜测。
- Blockbench/预览阶段先确认动作意图，再进入 Java 调参。

### 4.4 纹理

当前已有：

- 五文明神殿方块纹理。
- 大量神力书、神力证明、圣物和埃及神像物品纹理。
- 希腊生物实体纹理。
- 两张粒子纹理：`flame_particle.png`、`shockwave_particle.png`。

当前缺口：

- 用户实测反馈生物纹理存在空白、透明、错位和关键面不正确。
- 当前生物纹理仍偏程序生成/占位感，眼睛、嘴、鬃毛、翅膀、鳞片、蛇发等关键纹理不够明确。
- 纹理源文件、调色板、UV 对应关系和人工验收记录不足。

1.0 纹理交付必须包括：

- 每个模型有 UV 模板、完成纹理、纹理 fit 预览。
- 关键面必须标注：眼、嘴、角、爪、尾、翅膀、多头、武器、符文。
- 每个文明建立主色、辅色、金属/石材/布料/鳞片材质规则。
- 不允许通用随机面板纹理作为最终资源。
- 纹理必须在 Blockbench 三视角中确认无空白、无错位、无透明破面。

### 4.5 UI

当前已有：

- `CustomTempleGuardianTradeScreen` 自定义守护者交易界面。
- 自定义交易网络包：打开交易界面、执行交易。
- `GodPowerGuideItem` 通过 `OPEN_GOD_POWER_GUIDE_SCREEN` 网络入口打开 `GodPowerGuideScreen`，不再把指南物品替换为原版成书。
- `GodPowerGuideScreen` 覆盖概览、神力图鉴、神殿状态和证书余额四个页签，按五文明显示证书来源和法术书矩阵。
- `textures/gui/temple_guardian_trade.png` 已接入交易 Screen，界面具备交易列表、输入/输出详情、库存计数、冷却状态、禁用原因、tooltip 和滚动列表。
- UI 静态验收工具：`tools/ui_validation_pipeline.py`，当前范围为 `static_guardian_trade_and_god_power_guide_ui`，报告路径 `docs/preview_assets/ui_validation/ui_report.json`。

当前缺口：

- 交易界面已有基础状态驱动布局，但文明视觉主题仍偏弱，尚未达到最终美术 UI。
- 交易界面展示的是当前所需物品库存计数；证书余额已在神力图鉴中显示，但交易界面仍可进一步强化直接余额提示。
- 神力图鉴已有静态实现，仍需真实分辨率和最终美术验收。
- 小窗口、默认窗口、高分辨率下的真实渲染仍需手工或截图验收。

1.0 UI 交付必须包括：

- 五文明守护者交易 UI：文明主题背景、神力列表、输入/输出、证书余额、冷却状态、禁用原因。
- 神力详情 UI 或指南升级：神力来源、效果、冷却、消耗、解锁方式、文明分类。
- 神殿/神像/圣物状态反馈：产出倒计时、有效结构、存储数量、缺失部件。
- UI 纹理资源、按钮状态、悬停 tooltip、错误提示、多语言适配。
- UI 自动/手工验收：小窗口、默认窗口、高分辨率下文字不溢出。

### 4.6 美术与风格

当前已有：

- 希腊生物概念图 contact sheet。
- 神殿、神力书、圣物、守护者等分散美术资源。

当前缺口：

- 缺少全 mod 统一美术 bible。
- 五文明之间的视觉语言没有冻结。
- 生物、建筑、UI、粒子、图标之间没有同一套风格规则。

1.0 美术交付必须包括：

- 全局美术 bible：Minecraft/Fabric 像素风、文明区分、神话特征、禁止事项。
- 五文明视觉板：
  - Greek：大理石、青铜、橄榄绿、奥林匹斯纹样。
  - Egyptian：砂岩、黄金、青金石、太阳和象形符号。
  - Chinese：朱红、金、玉色、云纹、龙纹、符箓。
  - Nordic：冷灰石、深木、冰蓝、卢恩符文、兽骨/铁器。
  - Atlantean：海晶石、青蓝发光、水晶、失落科技几何纹样。
- 生物概念图、UI mockup、粒子效果参考、图标风格规范。
- 所有最终资产保留可追踪源文件或生成脚本说明。

### 4.7 粒子效果

当前已有：

- 大量神力、生物技能和方块实体使用原版 `ParticleTypes`。
- 陨石、地震、奇美拉火焰、美杜莎石化、九头蛇毒雾等已有基础粒子表现。
- 存在 `textures/particle/flame_particle.png` 和 `textures/particle/shockwave_particle.png`。
- 生物技能粒子矩阵已有静态门禁，当前覆盖 31/31 个神话生物与召唤物，报告路径为 `docs/preview_assets/entity_validation/action_particle_report.json`。
- 已补第一批自定义粒子注册链：`ModParticles` + `ModClientParticles` + `assets/mythic_age/particles/*.json`，覆盖奇美拉 `mythic_flame`、天马 `divine_sparkle`、青铜巨像 `shockwave`。
- 已新增粒子资源门禁 `tools/particle_validation_pipeline.py`，报告路径为 `docs/preview_assets/particle_validation/particle_report.json`。
- 已新增神力粒子性能基础设施：`ModConfig` 粒子开关/密度/距离/单次上限 + `MythicParticleEffects` 统一发射辅助。
- 已新增神力粒子矩阵门禁 `tools/god_power_particle_validation_pipeline.py`，报告路径为 `docs/preview_assets/particle_validation/god_power_particle_report.json`。
- 神力粒子矩阵门禁已覆盖 52/52 个非神殿生成神力；`SpellbookItem` 已禁止直接绕过 `MythicParticleEffects`，持续型神力 manager 也纳入检查。

当前缺口：

- 粒子效果主要是原版粒子拼接，缺少文明主题和神力辨识度。
- 粒子密度、距离裁剪、低配配置和多人服务器性能已有基础入口，但尚未做运行时配置文件、玩家可调 UI 和压力测试。
- 自定义粒子目前只覆盖少量代表技能；神力矩阵已有静态全量覆盖和性能 gate，但仍需逐项替换原版粒子拼接并增加文明专属纹理图集。
- 亚特兰蒂斯当前已有证书、神殿、守护者、生物和 10 个 1.0 神力 spellbook；1.0 仍需补逐项玩法验收、最终图标 polish 和更高辨识度粒子。

1.0 粒子交付必须包括：

- 神力粒子矩阵：每个神力至少定义启动、持续、命中/结束三段视觉。
- 生物技能粒子矩阵：毒、火、石化、冲锋、雷击、冰冻、神圣光环等。
- 文明粒子调色板：Greek 蓝金雷光，Egyptian 金砂太阳，Chinese 红金符箓，Nordic 冰蓝雪雾，Atlantean 青蓝水晶。
- 自定义粒子资源和注册方案：需要时添加 particle JSON、纹理、客户端工厂和网络触发。
- 性能 gate：粒子数量、范围、距离裁剪、配置开关、服务端 tick 影响。

### 4.8 音效与反馈

当前已有：

- 希腊生物和陨石有较完整 OGG 音效文件。
- `sounds.json` 已登记 30 个 1.0 生物 ambient、hurt、death、attack、special/charge/fly 等事件。
- 非希腊 20 个生物已有程序化首版 OGG、`ModSounds` 常量、实体引用和三语字幕。
- 已新增 30 生物音效方向设计包：`docs/美术资源/audio/creature_audio_art_direction_2026-06-12.json`。
- `tools/audio_validation_pipeline.py` 已覆盖音频文件、`sounds.json`、`ModSounds`、实体引用、字幕和音效方向。

当前缺口：

- 非希腊 20 个生物仍是程序化首版音效，发布前需要最终文明主题混音或替换。
- 神力音效和 UI 反馈需要逐项审计。
- 最终音效需要响度、循环/尾音、战斗可读性和文明主题听感验收。

1.0 音效交付必须包括：

- 每个生物 ambient、hurt、death、step、attack、special。
- 每个神力 cast、loop/impact、end 或失败反馈。
- UI 点击、交易成功、交易失败、解锁、证书产出提示。
- `sounds.json` 和语言字幕一致性校验。
- 生物音效方向设计包和最终混音验收记录。

### 4.9 验证与交付口径

任何 1.0 内容必须同时通过以下口径才算完成：

- 代码可编译。
- 资源 JSON 可解析。
- 模型和纹理通过设计包人工验收。
- 动作矩阵覆盖该单位的所有核心行为。
- UI 文本不溢出，状态反馈明确。
- 粒子和音效与技能/文明特征匹配。
- NBT、结构、交易和存档相关字段有静态或运行时验证。

## 5. 1.0 内容矩阵

### 5.1 Greek

当前状态：代码接入最多，但美术质量未达标。

1.0 必须完成：

- 重做或精修 10 个希腊生物的设计包、概念图、Blockbench 模型、UV 和纹理。
- 保留并强化当前 12 个希腊神力。
- 希腊神殿至少完成 small/medium/large 三个结构 NBT，或将 template pool 调整为真实存在的结构。
- 米诺陶纳入同一套设计先行视觉流程，不保留 legacy 例外。
- 神殿兑换、自然生成、刷怪蛋、战斗掉落和文案完整。

优先重做顺序：

1. Hydra
2. Cyclops
3. Pegasus
4. Chimera
5. Manticore
6. Nemean Lion
7. Centaur
8. Medusa
9. Colossus
10. Minotaur

### 5.2 Egyptian

当前状态：神力、证书、神像、神殿方块基础较多；5 个埃及生物已有设计包、Blockbench 资产、纹理、实体链路和 V2/runtime model gate；结构 NBT 基础文件已补齐但仍需建筑美术和功能布局精修。

1.0 必须完成：

- 神殿结构 NBT：small/medium/large。
- 至少 5 个埃及神话生物设计、模型、纹理和实体链路；当前 Anubis Warrior、Scarab Guardian、Mummy Soldier、Sphinx Sentinel、Sun Falcon 已完成基础链路和 V2 设计冻结。
- 埃及神像生产证明链路完整测试。
- 埃及神力书逐项实测，补齐模型、纹理、音效、粒子和说明。

候选生物方向，最终以设计包冻结为准：

- Anubite
- Sphinx
- Scarab
- Wadjet
- Mummy
- Phoenix 或其它埃及主题高阶单位

### 5.3 Chinese

当前状态：中国神力书和文明框架存在；Azure Dragon、Qilin Guardian、Jiangshi、Nine-tailed Fox、Stone Lion Guardian 已完成基础实体链路、design-first 视觉流水线、V2 造型冻结和 runtime model quality gate。结构 NBT 仍缺 small/medium/large，神力证明人口产出链路仍需实测，生物美术仍需从程序化模型进入 1.0 级人工精修。

1.0 必须完成：

- 神殿结构 NBT：small/medium/large。
- 至少 5 个中国神话生物或单位设计、模型、纹理和实体链路；当前 5 个已完成基础链路和 V2/runtime gate，后续重点转为高精度模型、动作、音效和纹理精修。
- 中国神力证明人口产出链路需要确认是否被世界 tick 或村庄系统真实调用。
- 中国神力书逐项实测，补齐模型、纹理、音效、粒子和说明。

候选生物方向，最终以设计包冻结为准：

- Qinglong / 青龙
- White Tiger / 白虎
- Vermilion Bird / 朱雀
- Qilin / 麒麟
- Terracotta Warrior / 兵马俑守护者
- Jiangshi / 僵尸

### 5.4 Nordic

当前状态：北欧神力和行走树已有较多实现；Frost Giant、Draugr Warrior、Valkyrie、Fenrir Wolf、Dwarf Berserker 已完成基础实体链路、design-first 视觉流水线、V2 造型冻结和 runtime model quality gate。结构 NBT 仍缺 small/medium/large，生物美术仍需从程序化模型进入 1.0 级人工精修。

1.0 必须完成：

- 神殿结构 NBT：small/medium/large。
- 行走树纳入正式视觉验收，不作为孤立召唤物处理。
- 至少 5 个北欧神话生物设计、模型、纹理和实体链路；当前 5 个已完成基础链路和 V2/runtime gate，后续重点转为高精度模型、动作、音效和纹理精修。
- 北欧击杀掉落证明规则完整测试。
- 北欧神力书逐项实测，补齐模型、纹理、音效、粒子和说明。

候选生物方向，最终以设计包冻结为准：

- Troll
- Frost Giant
- Valkyrie
- Fenrir Wolf
- Draugr
- Kraken 或其它北欧主题高阶单位

### 5.5 Atlantean

当前状态：五文明框架中仍偏弱。已有证书、守护者、神殿方块、结构入口、10 个 1.0 神力书和独立首版图标；Crystal Leviathan、Orichalcum Golem、Siren Priestess、Tidal Guardian、Abyssal Ray 已完成基础实体链路、design-first 视觉流水线、V2 造型冻结和 runtime model quality gate。但逐项玩法验收、美术资源、神殿结构和神话生物主内容还未达到 1.0。

1.0 必须完成：

- 冻结亚特兰蒂斯文明定位：海洋、失落科技、水晶、古代机械。
- 将已实现的 6 个亚特兰蒂斯 MVP 神力书验收稳定，并后续扩展/冻结到 10 个。
- 神殿结构 NBT：small/medium/large。
- 至少 5 个亚特兰蒂斯神话生物或机械单位设计、模型、纹理和实体链路；当前 5 个已完成基础链路和 V2/runtime gate，后续重点转为高精度模型、动作、音效和纹理精修。
- 明确亚特兰蒂斯神力证明获取方式，避免只靠守护者交易。

候选生物方向，最终以设计包冻结为准：

- Automaton
- Promethean
- Behemoth
- Satyr
- Lampades
- Argus 或其它失落文明主题单位

## 6. 设计与美术执行标准

### 6.1 生物设计先行 gate

每个生物必须先有设计包，再允许进入建模：

- 文件路径：`docs/美术资源/<civilization>_creatures/design/<creature>.md`
- 必填内容：
  - 目标体量：碰撞盒、视觉高度、相对玩家比例。
  - 轮廓语言：正面、侧面、45 度可读特征。
  - 部件清单：头、眼、嘴、角、鬃毛、翅膀、尾巴、武器、腿、爪等。
  - Blockbench group 层级。
  - UV 规划：关键面和纹理区域。
  - 纹理细节：主色、阴影、眼睛、嘴、特殊符号。
  - 动画约束：待机、移动、攻击、飞行、骑乘等。
  - 验收截图清单。

### 6.2 视觉质量 gate

脚本检查不能替代人工视觉验收。1.0 生物必须满足：

- 体量明显，不再出现除青铜巨人外普遍偏小的问题。
- 正面可见眼睛、嘴和核心神话特征。
- 侧面能读出生物结构，不只有一团躯干。
- 纹理无空白、透明、错位或关键面缺失。
- 武器、翅膀、多头、蛇发、尾刺、鬃毛等特征必须落在正确位置。
- Blockbench 预览必须先通过，再允许接入 Java 模型。
- 用户已明确“不需要再在游戏内验证”，因此当前阶段不把 MC 截图作为必跑项。

### 6.3 希腊生物重做重点

- Hydra：旧五头 V2 已被拒绝；1.0 默认回到 V3 九头、低伏长身、九个 socket 和头-颈-肩实体连接设计，任何五头简化都必须先走设计变更。
- Cyclops：完整人形巨怪，单眼、眉骨、嘴、手脚和巨大石棒必须清楚。
- Pegasus：纯白主色，明确马头、鬃毛、四腿、羽翼；自主飞行和骑乘飞行能力必须保留。
- Chimera：狮身、羊头、蛇尾至少三类特征可读。
- Manticore：人面或兽面、翼、蝎尾、尖牙和尾刺必须可读。
- Nemean Lion：狮头、鬃毛、爪、强壮前肢要明确。
- Centaur：人上身和马身比例必须清晰，不能像普通四足怪叠小人。
- Medusa：蛇发、面部、蛇形下身或造型特征保持清晰。
- Colossus：作为青铜巨像，体量和金属块面要保持优势。
- Minotaur：牛头、人形身体和战斗武器/角要纳入新流程。

## 7. NBT 与结构文件执行标准

### 7.1 结构 NBT 必须补齐

1.0 推荐结构目标已生成基础版：

- Greek：small、medium、large
- Egyptian：small、medium、large
- Chinese：small、medium、large
- Atlantean：small、medium、large
- Nordic：small、medium、large

当前 15 个 canonical NBT 均存在，并由 `tools/generate_canonical_temple_structures.py` 可重复生成。后续不能把这批基础结构当作最终美术交付，还需要按文明建筑设计包精修。

### 7.2 结构路径必须统一

当前路径口径已开始统一：

- template pool 使用 `mythic_age:village/<civilization>/temple_<size>`
- `MythicVillageProcessor` 使用 `structures/village/<civilization>/temple_small.nbt`

主路径采用 Minecraft 数据包常规路径：

```text
src/main/resources/data/mythic_age/structures/village/<civilization>/temple_<size>.nbt
```

### 7.3 结构审计工具

现有 `entity_validation_pipeline.py` 不覆盖结构 NBT。当前已新增独立结构审计工具：

- 工具路径：`tools/structure_validation_pipeline.py`
- 报告路径：`docs/preview_assets/structure_validation/structure_report.json`
- 当前命令：`python3 tools/structure_validation_pipeline.py audit`

当前审计结果：

- 规范化 1.0 神殿结构：15/15 存在。
- 可解析且含关键方块的 canonical 结构：15/15。
- template pool 引用结构：0 缺失。
- `MythicVillageProcessor` 代码 NBT 路径：5/5 存在。
- 结构 gate：PASS。
- 旧希腊结构 `structures/temple/greek/greek_temple_small.nbt` 已确认无代码/数据引用并清理；数据包 `.DS_Store` 污染也已清理。

生成工具：

- `tools/minecraft_structure_nbt.py`
- `tools/generate_canonical_temple_structures.py`
- `docs/preview_assets/structure_validation/generated_temple_manifest.json`

执行约束：

- NBT 生成和结构审计必须串行执行，不能并行读写同一批二进制 `.nbt` 文件。
- 当前结构 gate 已检查 NBT 可读性、尺寸、palette、blocks、entities、DataVersion、文明神殿主材、台阶/半砖/柱/浮雕、圣物基座、对应文明神像分段方块、功能点位和 template pool 引用。

1.0 后续需要把该工具或截图流程扩展到更深层视觉质量检查：

- 检查实机光照、路径可达性、守护者站位和玩家动线。
- 检查建筑从远处和近处是否有足够文明辨识度。
- 检查结构不会与村庄道路、地形和其他 POI 产生明显穿插。
- 检查截图或人工验收结果是否符合建筑设计包。

## 8. MCP 与验证工具规划

### 8.1 已有工具能力

- `tools/greek_creature_visual_pipeline.py`：希腊生物设计包、Blockbench 产物和预览检查。
- `tools/entity_validation_pipeline.py`：实体清单、静态 NBT schema、动作/粒子合同检查。
- `tools/mc_visual_probe_mcp.py`：MC 客户端视觉探针入口。
- `VisualProbeServer`：客户端内 HTTP 探针，支持 `/open_world`、`/capture`、`/command`、`/validate_nbt`、`/validate_nbt_restart`。
- `ValidationCommand`：服务端 `/mythicage_validate nbt`、`nbt_restart`、`nbt_restart_prepare`、`nbt_restart_verify`。实体 round-trip 范围已包含五文明新增生物；真实重启周期通过 prepare -> 保存退出 -> 重启同一世界 -> verify 生成报告。

### 8.2 1.0 必须补强的 MCP/工具能力

- Blockbench 检查扩展到所有文明，不只 Greek。
- 结构 NBT audit 工具。
- 资源 JSON audit 工具，覆盖 blockstates、models、lang、sounds、loot_tables。
- NBT runtime probe 已有重启语义和真实重启周期入口；后续关闭 blocker 必须产出 `run/visual_probe/nbt_restart_verify_report.json` 且 `failed=0`。
- MC 截图工具若未来重新启用，必须先关闭菜单、确认 HUD/GUI 状态、等待世界 ready，再截图。

当前执行规则：

- 用户已明确“不需要再在游戏内进行验证”，所以当前 1.0 美术重做阶段只做设计包和 Blockbench 预览。
- MC MCP 仅在用户重新要求实机验证时启用。

## 9. 1.0 里程碑

### M0：基线收口

目标：稳定当前工程，不在不清楚的基础上继续堆内容。

交付物：

- 当前状态审计文档。
- 1.0 内容矩阵。
- 结构路径决策。
- 旧文档状态标记，避免继续引用“95% 发布就绪”的旧口径。
- `./gradlew build --offline` 通过。

退出标准：

- 当前缺口清单被确认。
- 后续所有任务都能映射到 1.0 内容矩阵。

### M1：1.0 设计冻结

目标：先定义做什么，再实现。

交付物：

- 五文明设计 bible。
- 五文明神力证明获取规则。
- 五文明神殿结构规格。
- 五文明生物 roster。
- 希腊 10 生物重做设计包。

退出标准：

- 每个 1.0 生物都有体量、轮廓、部件、UV、纹理和动画设计。
- 每个文明都有最小可玩闭环定义。

### M2：希腊 vertical slice 重做

目标：把希腊做成 1.0 质量样板。

交付物：

- 希腊 10 生物高质量 Blockbench 模型和纹理。
- Java 模型同步或导出策略统一。
- 希腊神殿 small/medium/large 结构 NBT。
- 希腊神力、证书、守护者、兑换、自然生成完整验收。

退出标准：

- 用户视觉验收通过。
- Blockbench 三视角通过。
- 构建和资源校验通过。

### M3：结构/NBT 系统收口

目标：解决所有文明神殿资源和结构加载缺口。

交付物：

- 五文明结构 NBT。
- template pool 路径统一。
- 结构 audit 工具。
- 结构生成测试文档。

退出标准：

- template pool 不再指向缺失结构。
- 所有结构 NBT 能被工具读取。

### M4：其它文明神力和证明链路

目标：把已有神力代码变成可测、可玩、可说明的系统。

交付物：

- Egyptian、Chinese、Nordic 神力逐项验收。
- Atlantean 6 个神力 MVP 已实现入口；继续完成完整矩阵、独立资源和玩法验收。
- 五文明证书获取规则统一测试。
- 神力书资源、tooltip、冷却、消耗、音效、粒子完整。

退出标准：

- 每个文明至少有一条可重复获取证书和消耗证书的闭环。

### M5：其它文明生物批次

目标：把 1.0 从希腊扩展为多文明神话生物体系。

交付物：

- Egyptian 至少 5 个生物。
- Chinese 至少 5 个生物。
- Nordic 至少 5 个生物。
- Atlantean 至少 5 个生物。
- 每个生物都走设计先行、Blockbench、UV、纹理、Java 接入、资源、掉落、生成和文案流程。

退出标准：

- 每个文明至少有一批一眼可辨识的神话单位。

### M6：1.0 发布候选

目标：从开发完成进入发布质量。

交付物：

- 全量构建。
- 全量资源 audit。
- 全量 NBT audit。
- 全量玩法 smoke test。
- 平衡性表。
- 玩家说明文档。
- CHANGELOG 和 release checklist。

退出标准：

- 没有 P0/P1 阻断问题。
- 用户确认 1.0 内容和视觉质量达到预期。

## 10. 模块化工作分解

后续每个文明、每个生物、每个神力都按同一套分解执行，避免再次出现“代码有了但模型不好看”“模型有了但动作不对”“纹理有了但 UI/粒子没有”的半成品状态。

| 模块 | 当前状态 | 1.0 交付物 | 验收方式 |
| --- | --- | --- | --- |
| 代码 | 多文明框架和希腊生物链路已存在 | 实体、AI、神力、交易、证书、结构、配置、验证工具 | 构建、静态审计、运行时 probe |
| 模型 | 希腊 10 个、埃及 5 个、北欧 5 个、中国 5 个和亚特兰蒂斯 5 个已接入 V2/运行模型契约；整体仍未达到最终美术质量 | 全生物设计包、概念图、Blockbench 模型、Java 同步 | Blockbench 三视角、runtime model gate 和人工验收 |
| 动作 | 基础 `setAngles` 为主 | 待机、移动、攻击、特殊、受击、死亡；飞行单位额外飞行/骑乘 | 动作矩阵和预览验收 |
| 纹理 | 多数物品和方块已有；生物纹理质量不足 | UV 模板、完成纹理、关键面标注、无空白错位 | 纹理 fit 报告和预览图 |
| UI | 守护者交易 Screen 原型存在 | 文明主题交易 UI、神力详情、证书余额、冷却和错误状态 | 不同分辨率手工验收 |
| 美术 | 分散资源存在，缺少统一 bible | 五文明视觉板、图标规范、建筑/生物/粒子风格规范 | 美术 bible 审核 |
| 粒子 | 主要使用原版粒子，两张粒子纹理 | 神力/生物技能粒子矩阵、自定义粒子资源和性能配置 | 粒子效果预览和性能 gate |
| 音效 | 希腊生物较完整，非希腊缺失 | 生物、神力、UI、证书产出音效和字幕 | `sounds.json` 审计和听感验收 |
| NBT/结构 | 实体/方块实体 NBT、结构 NBT 和路径一致性已有静态 gate；真实存档重启仍待实测 | 五文明结构 NBT、物品/实体/方块实体 NBT、结构路径一致 | NBT audit、结构 audit、真实 MC 重启 probe |
| 文案 | 多语言已有但分散 | 中文/英文/日文名称、tooltip、指南、错误提示 | lang JSON 校验和 UI 走查 |

推荐每个内容包使用以下完成定义：

1. 设计包完成并通过人工确认。
2. 模型、纹理、动作预览通过。
3. Java/资源接入完成。
4. UI、粒子、音效、文案完成。
5. NBT/交易/生成/掉落/配置测试完成。
6. 构建和对应 audit 通过。

## 11. 近期执行任务清单

### P0

1. 确认 1.0 内容矩阵，冻结五文明最小可玩范围。
2. 标记旧 `PROJECT_STATUS_FINAL.md` 为历史文档，避免误判完成度。
3. 新增结构 NBT audit 工具。
4. 统一结构 NBT 路径和 template pool 引用。
5. 重做希腊 10 生物设计包，先从 Hydra、Cyclops、Pegasus 开始。

### P1

1. 按新设计包重做 Hydra、Cyclops、Pegasus 的 Blockbench 模型和纹理。
2. 把米诺陶纳入 Greek visual pipeline。
3. 建立所有文明通用 creature visual pipeline。
4. 梳理神力证明获取链路，确认哪些方法已真实接入 tick/event。
5. 为 Atlantean 神力体系补设计和 MVP 实现计划。

### P2

1. 为 Egyptian、Chinese、Nordic、Atlantean 分别冻结生物 roster。
2. 补齐五文明神殿结构 NBT。
3. 逐项验证神力书资源和实现。
4. 扩展 NBT runtime probe 到物品、交易和结构功能方块。
5. 编写玩家可读的 1.0 指南。

## 12. 风险与决策点

### 12.1 主要风险

- 旧文档过度乐观：有文档声称项目 95% 接近发布，但当前用户实测和仓库证据不支持该结论。
- 结构路径不一致：template pool 和代码加载路径并存，容易导致世界生成缺结构。
- 视觉 gate 标准过低：脚本通过不代表模型好看或特征正确。
- 神力实现数量多：代码路径多，但逐项可玩性和资源完整度需要重新审计。
- 文明扩展面过大：如果同时铺开五文明生物，容易再次产出低质量模型。

### 12.2 必须先确认的决策

1. 1.0 是否必须五文明全部有生物，还是 Greek 完整加其它文明神力/神殿为主。
2. 每个非希腊文明 1.0 生物数量是 3 个、5 个，还是更多。
3. 结构 NBT 是否坚持 small/medium/large 三尺寸。
4. Atlantean 已扩展到 10 神力；仍需决策 1.0 是否要求逐项实机玩法验收后才进入 release candidate。
5. MC 实机验证是否继续暂停，仅用 Blockbench 和静态工具推进。

## 13. 推荐主攻方向

当前最合理的 1.0 开发顺序：

1. 先完成 M0/M1，不继续无设计地堆实体。
2. 以 Greek 重做作为质量样板，修复用户已指出的模型问题。
3. 同步补结构 NBT audit，因为这是系统级 P0 缺口。
4. 在 Greek 样板通过后，再批量扩展其它文明生物。
5. 最后才进入 MC 实机截图和发布候选验证。

这样做的原因很直接：项目已经有很多框架，真正缺的是可控的内容生产流程和稳定的验收口径。先把希腊做成标杆，再横向复制到其它文明，成功率最高。
