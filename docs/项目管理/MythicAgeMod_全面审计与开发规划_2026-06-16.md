# MythicAgeMod 全面审计与开发规划（2026-06-16）

生成时间：2026-06-16
本文档替代旧版审计文档，作为当前开发阶段的最新执行基线。

---

## 一、项目总览

MythicAgeMod 是基于 Fabric 的 Minecraft 1.20.1 模组，主题为五大文明神话生物与玩法系统。目标是 1.0 五文明基础可玩版本。

| 维度 | 当前状态 |
|------|----------|
| 构建 | `./gradlew build --offline` 通过 |
| 发布门禁 | NOT_READY（4 个阻断缺口） |
| 设计签收 | 0/30 生物获得 `approved_for_modeling` |
| 设计阻断 | Pegasus、Hydra 被明确拒绝 |
| 人工视觉验收 | 0/30 生物通过 |

---

## 二、已完成内容审计

### 2.1 实体注册层（完成度：高）

已注册 43 个实体类型：30 个文明生物 + 11 个神殿守护者 + 1 个召唤实体（Walking Tree）+ 1 个投射物（Meteor）。

| 文明 | 生物（10/5/5/5/5） | 注册状态 |
|------|---------------------|----------|
| Greek | Minotaur, Cyclops, Centaur, Hydra, Medusa, Nemean Lion, Manticore, Chimera, Colossus, Pegasus | 全部注册 |
| Egyptian | Anubis Warrior, Scarab Guardian, Mummy Soldier, Sphinx Sentinel, Sun Falcon | 全部注册 |
| Nordic | Frost Giant, Draugr Warrior, Valkyrie, Fenrir Wolf, Dwarf Berserker | 全部注册 |
| Chinese | Azure Dragon, Qilin Guardian, Jiangshi, Nine-tailed Fox, Stone Lion Guardian | 全部注册 |
| Atlantean | Crystal Leviathan, Orichalcum Golem, Siren Priestess, Tidal Guardian, Abyssal Ray | 全部注册 |

每个生物有独立的 Entity 类、Model 类、Renderer 类、ModelLayer 注册和专属纹理目录。1:1:1 映射完整。

### 2.2 模型复杂度（已实现，质量未验收）

| 复杂度等级 | 生物 | ModelPart 数量 |
|------------|------|----------------|
| 超高（65+） | Centaur, Hydra, Medusa, Manticore, Chimera | 人马弓箭、多头蛇、蛇发、蝎尾、三头 |
| 很高（40-60） | Minotaur, Colossus, Nemean Lion, Cyclops, Pegasus, Walking Tree, Frost Giant, Sphinx Sentinel, Crystal Leviathan, Azure Dragon | 详细面部、武器、鬃毛、翅膀等 |
| 高（30-40） | Valkyrie, Fenrir Wolf, Dwarf Berserker, Nine-tailed Fox, Qilin Guardian, Orichalcum Golem, Siren Priestess | 九尾、盔甲、武器等 |
| 中（15-30） | Scarab Guardian, Mummy Soldier, Sun Falcon, Draugr Warrior, Jiangshi, Stone Lion Guardian, Tidal Guardian, Abyssal Ray | 基础结构化 |
| 低（8-15） | Anubis Warrior, Temple Guardian | 简单人形 |

### 2.3 玩法系统（代码实现完成度：高）

| 系统 | 状态 | 关键类 | 备注 |
|------|------|--------|------|
| **神力系统** | 55+ 法术实现 | `SpellbookItem.java` | 5 文明各 8-12 个法术，有冷却、稀有度、射线目标、粒子和音效 |
| **证书经济** | 5 类型完整 | `GodPowerCertificateManager.java`, `CertificateGenerationManager.java` | 希腊=村民人口、埃及=雕像产出、中华=人口驱动、北欧=怪物击杀、亚特兰蒂斯=守护者交易 |
| **守护者交易** | 6 类型 + 自定义 GUI | `CustomTempleGuardianEntity.java`, `CustomTempleGuardianTradeScreen.java` | 物品交易、冷却、分页、文明类型追踪 |
| **神殿结构** | 15 个 canonical NBT | `StructureGenerator.java`, `MultiBlockStructureUtil.java` | 5 文明 x 3 尺寸，静态验证通过 |
| **方块实体** | 7 类型 | `AbundanceAltarCoreBlockEntity.java` 等 | 丰饶祭坛、埃及雕像基座、圣物基座、雕像基座、动物诱引塔 |
| **音效系统** | 149+ 事件 | `ModSounds.java` | 30 生物 x ~5 音效（ambient/hurt/death/step/attack） |
| **粒子系统** | 3 类型（骨架） | `ModParticles.java` | MYTHIC_FLAME, DIVINE_SPARKLE, SHOCKWAVE；多数效果仍用原版粒子 |
| **配置系统** | 6 调试标志 + 性能参数 | `ModConfig.java` | 粒子距离、最大数量、调试节流 |
| **物品/圣物** | 295+ 物品，15 圣物 | `ModItems.java` | 5 证书、12 法术书、5 文明圣物组 |
| **世界持久化** | NBT 14 schema owner | `MythicAgeWorldState.java` | 真实重启周期通过；48 个 runtime_only 状态 |
| **国际化** | 英文、中文、日文 | `lang/*.json` | 基础翻译完成 |

### 2.4 工具链与门禁基础设施（完成度：高）

已建立 40+ Python 工具脚本，覆盖：

| 类别 | 工具数量 | 关键脚本 |
|------|----------|----------|
| 视觉管线 | 6 | `greek/egyptian/nordic/chinese/atlantean_creature_visual_pipeline.py`, `greek_creature_visual_pipeline.py` |
| 设计门禁 | 7 | `creature_design_first_gate.py`, `creature_visual_acceptance_package.py`, `pegasus_hydra_design_correctness_gate.py` 等 |
| 模型质量 | 5 | `creature_proportion_quality_gate.py`, `*_runtime_model_quality_gate.py` |
| 动作/音效 | 5 | `creature_action_acceptance_package.py`, `creature_audio_final_mix_acceptance_package.py` 等 |
| NBT/结构 | 6 | `nbt_release_acceptance_package.py`, `structure_validation_pipeline.py` 等 |
| 发布审计 | 4 | `mythic_age_1_0_gap_audit.py`, `mythic_age_1_0_release_gate.py`, `mythic_age_1_0_scope_matrix.py` 等 |
| MCP 服务 | 2 | `blockbench_visual_mcp.py`, `mc_visual_probe_mcp.py` |

---

## 三、关键问题：Pegasus/Hydra V6 退化分析

### 3.1 版本演化与问题

| 版本 | 内容 | 问题 |
|------|------|------|
| **V2（当前 Java 实现）** | Pegasus: 竖直塔状头颈，方块头、臃肿前身。Hydra: 5 头实现，大块身体 | 基本视觉身份失败，被明确判定设计错误 |
| **V3** | 方向修正但细节不足 | 标记为 `direction_correct_but_detail_insufficient`，未获批准即跳过 |
| **V4** | 引入硬性解剖学规则 | **最完整的设计逻辑**：Pegasus 要求成年马比例（长:高 = 1.9-2.2x）、小细长马头（头宽 = 体宽 0.35-0.45）、二段斜颈、肩后翼根、骑乘空间。Hydra 要求 **9 头**、低长身体（高度 ≤ 总长 0.32）、每头 0.12-0.18 肩宽、3 段颈路径 |
| **V5** | 重申 V4 规则 | `candidate_needs_user_review`，`modeling_allowed=false`，用户未批准即继续 |
| **V6（当前）** | 声称是"建模规格" | 文档明确写 `modeling_allowed=false`，但实际 Java 代码仍是 V2 的 5 头实现，未遵循任何 V4 规则 |

### 3.2 核心退化点

**Pegasus 退化：**
- V4 明确要求 `neck_lower` + `neck_upper` 二段斜颈 → 当前 Java 只有单个 `neck` 元素
- V4 要求头宽 = 体宽 0.35-0.45（小细长马头） → 当前头部比例未受约束
- V4 要求成年马横向身体（长:高 1.9-2.2x） → 当前身体短胖
- V4 要求结构阴影（纯白不能空白） → 当前纹理为纯白无细节

**Hydra 退化：**
- V4 明确要求 **9 头** → 当前 Java 硬编码 **5 头**
```java
// HydraModel.java 中的硬编码 - 违反 V4 合同
private static final String[] NECK_NAMES = {
    "outer_left_neck", "left_neck", "center_neck", "right_neck", "outer_right_neck"
};  // 只有 5 个，V4 要求 9 个
```
- V4 要求头-颈-肩实体连接（socket 系统） → 当前无 socket 验证
- V4 要求低伏长身（高度 ≤ 总长 0.32） → 当前身体为大块方体
- V4 要求每个头有独立眼/嘴/牙/毒腺细节 → 当前细节不足

### 3.3 退化原因分析

1. **工具过早生成输出**：`pegasus_hydra_v6_modeling_spec_package.py` 自动生成了设计文档，但这些文档本身写着 `modeling_allowed=false`
2. **设计与实现脱节**：V4/V5 合同有明确解剖学要求，但 Java 代码从未实现这些比例
3. **门禁未强制执行**：文档标注 `modeling_allowed=false`，但建模仍然继续
4. **V6 名义升级实际降级**：V6 声称比 V5 更严格（增加了 Blockbench 比例框和 pivot 要求），但实际 Java 实现完全忽视了 V4 的硬性规则

### 3.4 修复方案

**Pegasus 重建路径：**
1. 以 V4 硬性规则为基线（而非 V6 文档）
2. 重写 `PegasusModel.java`：二段斜颈、成年马身体比例、小细长马头
3. 同步 `.bbmodel` 文件
4. 纹理增加结构灰面和等高线

**Hydra 重建路径：**
1. 以 V4 硬性规则为基线
2. 重写 `HydraModel.java`：9 头 socket 系统、低伏长身、3 段颈路径
3. 每头独立眼/嘴/牙/毒腺
4. 扇形布局而非线性排列

---

## 四、BlockBench MCP 使用方式说明

### 4.1 已有 MCP 服务器

项目中有两个 MCP 服务器实现：

| MCP 服务器 | 文件 | 用途 |
|-----------|------|------|
| `mythic-age-blockbench-visual-gate` | `tools/blockbench_visual_mcp.py` | 希腊生物 Blockbench 视觉门禁 |
| MC Visual Probe | `tools/mc_visual_probe_mcp.py` | Minecraft 运行时截图和 NBT 验证（当前禁用） |

### 4.2 Blockbench MCP 工具清单

`blockbench_visual_mcp.py` 暴露 4 个工具：

| 工具名 | 功能 | 输入 |
|--------|------|------|
| `generate_design_gate_assets` | 生成 9 个希腊生物的设计文档、bbmodel、UV、纹理预览和报告 | 无 |
| `audit_design_gate` | 审计每个生物的设计包完整性 | 无 |
| `list_creatures` | 列出所有生物的设计产物路径 | 无 |
| `get_creature_design` | 读取单个生物的设计包 | `creature_id: string` |

### 4.3 正确的建模工作流（来自 `Blockbench_MCP_建模工作流.md`）

```
阶段 0：冻结目标（每次迭代只允许 1-2 个目标）
    ↓
阶段 1：结构拆解（写结构件清单，明确层级归属）
    ↓
阶段 2：Java/Blockbench 同步建模（先改 Java，再同步 bbmodel）
    ↓
阶段 3：本地脚本审计（UV、尺寸、连通性、比例检查）
    ↓
阶段 4：独立 SubAgent 审计（硬门槛，不通过不进游戏）
    ↓
阶段 5：Blockbench 三视角审查 + 用户确认图
    ↓
阶段 6：编译与游戏验证（只验证本轮目标）
    ↓
阶段 7：记录结论
```

### 4.4 当前问题与改进建议

**问题：** 当前 MCP 只覆盖希腊 9 个生物，不覆盖其他 4 个文明的 20 个生物。

**改进方向：**
1. 将 `blockbench_visual_mcp.py` 扩展为通用版，支持所有 5 个文明的视觉管线
2. 为每个文明的 `*_creature_visual_pipeline.py` 提供统一 MCP 入口
3. 增加 V4 硬性比例规则的自动检查（当前审计只检查文件是否存在，不检查比例逻辑）
4. 增加 Java Model ↔ BBModel 一致性对比工具（当前只有 WalkingTree 有 `sync_walking_tree_bbmodel.py`）

---

## 五、未完成项分类清单

### 5.1 P0：设计阻断（必须最先解决）

| ID | 任务 | 阻断原因 | 退出标准 |
|----|------|----------|----------|
| P0-001 | Pegasus 设计重做 | V2 实现被拒绝，V4 规则未在 Java 中落地 | 以 V4 规则重写 Java Model，三视角确认成年马比例 |
| P0-002 | Hydra 设计重做 | V2 实现被拒绝（5头），V4 要求 9 头未在 Java 中落地 | 以 V4 规则重写为 9 头 socket 系统，低伏长身 |
| P0-003 | 30 生物设计签收 | 0/30 获得 `approved_for_modeling` | 每个生物有明确"通过/返修/重写"结论 |
| P0-004 | Minotaur gate 修复 | 视觉质量合同阻断（已标记修复但需确认） | `creature_visual_acceptance_package.py` 无 Minotaur blocker |

### 5.2 P1-A：生物美术升级

| 文明 | 生物数 | 主要问题 | 关键升级点 |
|------|--------|----------|------------|
| Greek | 10 | Pegasus/Hydra 设计错误，其余待复审 | 马体比例、9头系统、面部细节、武器可读性 |
| Egyptian | 5 | 工程首版感强，文明材质不够 | 胡狼头雕刻、圣甲虫甲壳光泽、木乃伊绷带层次、狮身人面比例、猎鹰翎羽 |
| Nordic | 5 | 轮廓和武器/盔甲细节不足 | 霜巨人冰晶体量、亡灵腐蚀层次、女武神盔甲翅膀、芬里尔狼体量、矮人武器和盔甲 |
| Chinese | 5 | 标志结构需高精度重做 | 青龙分段龙身、麒麟角鬃火焰、僵尸符箓、九尾狐九尾分层、石狮卷鬃 |
| Atlantean | 5 | 水晶/海洋材质和轮廓层次不足 | 水晶利维坦分段、山铜金属质感、海妖鳞尾、潮汐守卫水纹、深渊鳐发光 |

### 5.3 P1-B：动作与动画

| 任务 | 当前 | 目标 | 备注 |
|------|------|------|------|
| 动作序列补全 | 30/168 已捕获 | 168/168 | 缺 138 项：locomotion、attack、hurt、death、flight、swim、signature |
| Pegasus 飞行动作 | 不可用（模型被拒） | 自主飞行 + 骑乘飞行 + 翼拍 | 依赖 P0-001 |
| Hydra 多头动作 | 不可用（模型被拒） | 9 头错相 idle/attack/hurt | 依赖 P0-002 |
| 16 批动作捕获 | 未开始 | 最低 1020 帧 | 需 MC 截图探针 |

### 5.4 P1-C：神力玩法

| 任务 | 当前 | 目标 |
|------|------|------|
| 52 神力人工 playtest | 0/52 完成 | 52/52，最低 446 场景 |
| 18 高风险神力优先验收 | 未开始 | 大范围编辑、爆炸、天气类有性能和失败路径结论 |
| 证书经济实机平衡 | 静态链路通过 | 频率、防刷、交易价格跨存档闭环 |

### 5.5 P1-D：音效

| 任务 | 当前 | 目标 |
|------|------|------|
| 149 音频事件人工听感 | 技术可播放 | 文明主题、战斗可读性、循环和音量层级确认 |
| 21 final-mix candidate | 未完成 | 响度/循环/可读性判断 |
| 响度工具链 | 缺 ffprobe/ffmpeg/soxi/ogginfo | 安装或等价方案 |
| 神力/UI 音效 | 未审计 | cast/impact/end/交易成功/失败/证书提示 |

### 5.6 P1-E：粒子系统

| 任务 | 当前 | 目标 |
|------|------|------|
| 自定义粒子实现 | 3 个骨架类型（MYTHIC_FLAME, DIVINE_SPARKLE, SHOCKWAVE） | 每个神力有文明主题的三段式粒子（cast/active/end） |
| 高风险神力压力测试 | 未开始 | 粒子数量、范围、降级策略和帧率影响有记录 |
| 生物技能粒子 | 未逐项审查 | 不遮挡关键读形（眼/嘴/武器/翼根/尾刺/多头） |

### 5.7 P2：UI、结构、发布卫生

| 任务 | 当前 | 目标 |
|------|------|------|
| 守护者交易 UI 实机验收 | 静态通过 | 文明、余额、价格、冷却、失败原因可读 |
| 神力图鉴 UI 实机验收 | 静态通过 | 文明、效果、冷却、消耗、获得方式、风险 |
| 分辨率/GUI scale/长文本 | 未验收 | 不同分辨率和 scale 下不重叠不截断 |
| 15 神殿建筑审美 | NBT 静态通过 | 文明差异明确、地形融合可读 |
| 玩家指南/CHANGELOG | README 已降级 | 与当前版本一致的玩家指南 |

### 5.8 持续项：NBT 与持久化

| 任务 | 当前 | 目标 |
|------|------|------|
| NBT 真实重启回归 | 通过 | 每次新增状态后重跑 |
| 48 个 runtime_only 状态 | 短期状态决策 | 任一升级为 required 时补 schema |
| 神力持续状态持久化 | 未复审 | 天气/世界编辑类跨存档语义明确 |

---

## 六、实施规划

### 阶段 M0：设计基线冻结（当前 → 完成设计签收）

**目标**：修复 Pegasus/Hydra 设计逻辑，建立 30 生物设计签收机制。

**步骤**：
1. **回退 V6，以 V4 规则为基线**
   - Pegasus：成年马比例（长:高 1.9-2.2x）、小细长马头、二段斜颈、肩后翼根
   - Hydra：9 头 socket 系统、低伏长身（高度 ≤ 总长 0.32）、3 段颈路径
2. **生成正确的设计合同**
   - 基于 V4 规则重写 `pegasus_modeling_contract.md` 和 `hydra_modeling_contract.md`
   - 包含 Blockbench 比例框、pivot 点、UV zone 和拒绝条件
3. **用户审查并签收**
   - 设计图三视角确认
   - `approved_for_modeling=true` 后方可进入建模
4. **建模工作流**（遵循 BlockBench MCP 工作流）
   - 冻结目标 → 结构拆解 → Java/BBModel 同步 → 本地审计 → SubAgent 审计 → 三视角审查 → 编译验证

**退出标准**：
- `tools/creature_design_first_gate.py audit` 中 Pegasus 和 Hydra 为 `approved_for_modeling`
- `tools/pegasus_hydra_design_correctness_gate.py audit` 为 PASS

### 阶段 M1：Greek 视觉 Vertical Slice

**目标**：Greek 10 生物全部通过视觉验收。

**步骤**：
1. 按 V4 规则重建 Pegasus Model + 纹理
2. 按 V4 规则重建 Hydra Model + 纹理（9 头）
3. 确认 Cyclops 巨大人形和石棒
4. Greek 10 生物逐个人工视觉复审
5. 返修或重做不合格项

**退出标准**：
- Greek 10 生物全部 `approved` 或有明确返修计划
- `tools/creature_visual_acceptance_package.py audit` 无 Greek design rejection blocker

### 阶段 M2：五文明生物美术升级

**目标**：20 个非希腊生物达到 1.0 美术标准。

**执行顺序**：每个文明一批
1. Egyptian（5 生物）→ 设计蓝图 → BBModel → UV → 纹理 → Java → 视觉报告
2. Nordic（5 生物）
3. Chinese（5 生物）
4. Atlantean（5 生物）

**退出标准**：30 生物全部 `approved_for_modeling` 且人工视觉验收通过

### 阶段 M3：动作与实机观感

**目标**：138 项缺失动作补全。

**步骤**：
1. 按 16 批动作捕获计划执行
2. 每批输出连续帧、触发条件和人工结论
3. 优先 Pegasus 飞行和 Hydra 多头动作（依赖 M1）

**退出标准**：`creature_human_motion_acceptance_ok=true`

### 阶段 M4：神力、粒子与音效

**目标**：52 神力 playtest + 三段式粒子 + 音效听感。

**步骤**：
1. 52 神力 playtest（优先 18 高风险）
2. 每神力补 cast/active/end 三段式粒子
3. 149 音频事件人工听感确认
4. 神力/UI 音效审计

**退出标准**：
- `god_power_gameplay_acceptance_ok=true`
- `creature_audio_final_mix_acceptance_ok=true`
- 粒子实机/压力验收完成

### 阶段 M5：UI、结构与 NBT 回归

**目标**：真实分辨率验收 + 神殿审美 + 持久化回归。

### 阶段 M6：1.0 发布候选

**退出标准**：
- `tools/mythic_age_1_0_gap_audit.py audit` → release blocking gaps = 0
- `./gradlew build --offline` 通过
- 所有人工验收项有签收记录

---

## 七、BlockBench MCP 建模规范（适用于所有生物）

### 7.1 通用建模原则

1. **设计先行**：任何建模必须先有审批通过的设计合同
2. **Java 是真值**：先改 Java Model，再同步 BBModel
3. **分阶段执行**：结构 → 几何 → UV → 纹理 → 动画，不混合
4. **双重审计**：本地脚本 + 独立 SubAgent，两者都通过才进游戏
5. **单一目标**：每轮迭代只解决 1-2 个问题

### 7.2 扩展 MCP 覆盖范围

当前 `blockbench_visual_mcp.py` 只覆盖 Greek 9 生物。需要扩展为统一入口：

```
blockbench_visual_mcp.py
  ├── generate_design_gate_assets(civilization?)  # 按文明生成
  ├── audit_design_gate(civilization?)             # 按文明审计
  ├── list_creatures(civilization?)                 # 按文明列出
  ├── get_creature_design(creature_id)             # 读取设计包
  ├── check_proportion_rules(creature_id)          # 新增：V4 硬性比例检查
  └── compare_java_bbmodel(creature_id)            # 新增：Java ↔ BBModel 一致性
```

### 7.3 需要新增的审计能力

| 审计项 | 当前状态 | 目标 |
|--------|----------|------|
| V4 硬性比例规则 | 不检查 | 自动校验头/体比、长高比、socket 数量 |
| Java ↔ BBModel 一致性 | 只有 WalkingTree 有 | 30 生物通用的同步对比 |
| 纹理空白/纯色检测 | 只检查 opacity | 检测大面积无结构阴影区域 |
| 动画连续帧验证 | 不存在 | setAngles 输出连续帧并检查连通性 |

---

## 八、必跑回归命令

```bash
# 设计门禁
python3 tools/creature_design_first_gate.py audit
python3 tools/pegasus_hydra_design_correctness_gate.py audit

# 视觉验收
python3 tools/creature_visual_acceptance_package.py audit
python3 tools/creature_human_visual_acceptance_ledger.py audit

# 动作验收
python3 tools/creature_action_acceptance_package.py audit
python3 tools/creature_motion_rework_queue.py audit

# 玩法验收
python3 tools/god_power_gameplay_acceptance_package.py audit

# 音效验收
python3 tools/creature_audio_final_mix_acceptance_package.py audit

# NBT/结构
python3 tools/nbt_release_acceptance_package.py audit
python3 tools/nbt_state_ownership_queue.py audit

# 总审计
python3 tools/mythic_age_1_0_gap_audit.py audit
python3 tools/mythic_age_1_0_scope_matrix.py audit
python3 tools/mythic_age_1_0_release_gate.py audit

# 构建
./gradlew build --offline
```

---

## 九、一句话判断

MythicAgeMod 的工程骨架和玩法代码实现度高（55+ 法术、30 生物注册、证书经济、守护者交易全链路可运行），但 **设计逻辑断裂**（Pegasus/Hydra V6 比 V4 退化、Java 代码不遵循设计合同）和 **人工验收缺失**（0/30 视觉签收、0/52 神力 playtest、0/138 动作捕获）是 1.0 发布的核心阻断。下一步必须以 V4 硬性规则为设计基线、重建 Pegasus 和 Hydra，再推进全生物视觉签收。
