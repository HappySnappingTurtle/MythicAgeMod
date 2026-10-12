# MythicAgeMod 1.0 完整开发 Checklist

日期：2026-06-16
用途：goal 模式下逐项执行，指导 1.0 全部剩余工作。
前置文档：`MythicAgeMod_全面审计与开发规划_2026-06-16.md`、`MythicAgeMod_架构改进设计文档_2026-06-16.md`

---

## Skill 与工具协作矩阵

### 可用 Skill + MCP 工具

| Skill/MCP | 能力 | 在本项目中的用途 |
|-----------|------|-----------------|
| **Blockbench MCP** (localhost:3000) | 直接操作 Blockbench：创建/修改几何、骨骼、纹理、动画、截图 | 生物模型重建、UV 映射、纹理绘制、动画创建 |
| **blockbench-modeling** | cube/mesh 几何、分组/骨骼层级、顶点编辑 | 30 生物模型结构重建 |
| **blockbench-animation** | 关键帧、骨骼绑定、walk cycle、战斗动画 | 168 个动作模式创建 |
| **blockbench-texturing** | 画笔、UV 映射、渐变、图层、形状绘制 | 30 生物纹理升级 |
| **blockbench-pbr-materials** | PBR 法线/高度贴图 | 高级材质效果（可选） |
| **blockbench-mcp-overview** | 全功能参考 | 查询可用工具 |
| **imagegen** | AI 图像生成/编辑 | 概念图、纹理参考、设计参考图生成 |
| **screenshot** | 桌面截图 | 游戏内截图验证 |
| **sync_creature_bbmodel.py** | Java ↔ BBModel 同步 | 建模后自动同步到 Java 运行时 |
| **blockbench_visual_mcp.py** | 项目内视觉门禁 MCP | 设计包/bbmodel/UV 审计 |

### 单生物建模 Workflow（7 阶段）

```
阶段 0: 冻结设计
  ├── 读取设计合同（V4 规则为基线）
  ├── [imagegen] 生成概念参考图（三视角）
  └── 用户确认设计意图

阶段 1: Blockbench 骨骼层级
  ├── [blockbench-modeling] add_group 创建完整骨骼树
  ├── [blockbench-modeling] list_outline 验证层级
  └── 设置每个 group 的 origin（pivot 点，用于动画）

阶段 2: 几何建模
  ├── [blockbench-modeling] place_cube 按设计合同放置所有方块
  ├── [blockbench-modeling] modify_cube 调整尺寸/位置/旋转
  ├── [Blockbench MCP] capture_screenshot 三视角截图
  └── 审计：比例检查（头/体比、长高比）

阶段 3: UV 与纹理
  ├── [blockbench-texturing] create_texture 创建纹理画布
  ├── [blockbench-texturing] auto_uv_mesh 或手动 set_mesh_uv
  ├── [blockbench-texturing] paint_with_brush / fill / gradient 绘制纹理
  ├── [blockbench-texturing] 文明色板约束检查
  └── [Blockbench MCP] capture_screenshot 纹理预览

阶段 4: 动画
  ├── [blockbench-animation] create_animation 创建 idle/walk/attack/hurt/death
  ├── [blockbench-animation] manage_keyframes 设置每个骨骼的关键帧
  ├── [blockbench-animation] animation_timeline 播放验证
  └── [Blockbench MCP] capture_screenshot 动画帧截图

阶段 5: 同步到 Java
  ├── [Blockbench MCP] 导出/保存 bbmodel
  ├── [sync_creature_bbmodel.py] sync <creature_id> → 更新 bbmodel
  ├── 手动更新 Java Model（getTexturedModelData）对齐
  ├── ./gradlew build --offline
  └── [blockbench_visual_mcp.py] audit 验证一致性

阶段 6: 游戏内验证
  ├── ./gradlew runClient
  ├── [screenshot] 游戏内截图
  └── 对比设计合同，判定通过/返修
```

---

## 已完成项汇总（架构改进阶段）

### ✅ 阶段 A1：中国证书系统（21/22）
- [x] 修复 `handleChinesePopulationGeneration()` 孤立 BUG
- [x] 创建 `ChineseCertificateManager`（劳作+交易两渠道）
- [x] 创建 `VillagerTradeMixin` 拦截原版交易
- [x] Mixin 扩展劳作冷却字段
- [x] MythicAgeWorldState NBT 持久化
- [x] 本地化 zh_cn + en_us
- [ ] 实机验证（需 runClient）

### ✅ 阶段 A2：法术书重构（21/21）— 2026-06-22 验证完成
- [x] Spell 接口 + AbstractSpell + SpellRegistry 基础设施
- [x] 56 个法术类文件（Greek 12 + Egyptian 11 + Chinese 11 + Nordic 11 + Atlantean 11）
- [x] registry-first + fallback 分发模式
- [x] Lure/Plenty/WalkingWoods 完整迁移（已有完整实现，非 stub）
- [x] SpellbookItem 最终瘦身（已无 castXxx 残留方法）
- [x] 神殿生成法术迁移（4 个 GenerateXxxTempleSpell 已注册）

### ✅ 阶段 A3：AI Goal 扩展（15/15）— 2026-06-22 补完
- [x] MythicSpecialAttackGoal 基类
- [x] P0: Hydra/Medusa/Cyclops 3 个签名技能 Goal
- [x] P1: Chimera/NemeanLion/Fenrir/AzureDragon 4 个签名技能 Goal
- [x] Pegasus 飞行 Goal — PegasusFlightGoal（3 阶段自主飞行：爬升/巡航/降落）
- [x] Centaur 骑射 Goal — CentaurRangedAttackGoal（5 箭齐射签名技能）

### ✅ 阶段 A4：粒子系统（11/11）— 2026-06-22 验证+补完
- [x] 10 种文明粒子注册 + JSON 配置
- [x] 客户端粒子工厂注册（复用基础工厂）
- [x] SpellParticleEffect 三段式接口
- [x] 独立文明粒子渲染类（5 个：Greek/Egyptian/Nordic/Chinese/Atlantean ParticleEffect）
- [x] 粒子纹理 PNG（13 个 PNG 已存在）
- [x] 法术粒子替换（AbstractSpell 模板方法自动注入 playCast/playEnd）
- [x] 文明默认粒子效果实现（5 个文明 ParticleEffect 均有完整 playCast/playActive/playEnd）

### ✅ 阶段 A5：BBModel 同步工具（7/10）
- [x] sync_creature_bbmodel.py + creature_model_map.json
- [x] 30 生物一致性审计可运行
- [ ] MCP 扩展（sync/audit 集成到 blockbench_visual_mcp.py）
- [ ] Blockbench 打开验证
- [ ] 详细 cuboid/pivot 级别解析

### ✅ 阶段 A6：Blockbench MCP + Skills 安装
- [x] blockbench MCP 注册到 `.claude/settings.json`
- [x] 6 个建模 skills 安装到 `~/.claude/skills/`
- [ ] Blockbench 端插件加载确认（需用户操作）

---

## 1.0 剩余开发 Checklist

### W0：设计阻断清理

#### W0.1 Pegasus 设计重做（使用 Blockbench MCP）
- [ ] 重写 `pegasus_modeling_contract.md`，以 V4 硬规则为基线：成年马体（L:H 1.9-2.2x）、小细长马头（头宽=体宽 0.35-0.45）、二段斜颈、肩后翼根
- [ ] [imagegen] 生成 Pegasus 三视角概念参考图
- [ ] 用户审查并签收设计合同 → `approved_for_modeling=true`
- [ ] [blockbench-modeling] 在 Blockbench 中重建 Pegasus 骨骼层级（head/neck_lower/neck_upper/body/wings/legs）
- [ ] [blockbench-modeling] 按 V4 比例放置所有 cube
- [ ] [Blockbench MCP] capture_screenshot 三视角截图
- [ ] 独立审计：比例检查通过
- [ ] [blockbench-texturing] 创建纯白+结构阴影纹理
- [ ] sync_creature_bbmodel.py sync pegasus
- [ ] 重写 PegasusModel.java 的 getTexturedModelData() 对齐新 bbmodel
- [ ] ./gradlew build --offline 通过

#### W0.2 Hydra 设计重做（使用 Blockbench MCP）
- [ ] 重写 `hydra_modeling_contract.md`，以 V4 硬规则为基线：9 头 socket 系统、低伏长身（高度 ≤ 总长 0.32）、3 段颈路径、扇形头布局
- [ ] [imagegen] 生成 Hydra 三视角概念参考图
- [ ] 用户审查并签收设计合同 → `approved_for_modeling=true`
- [ ] [blockbench-modeling] 重建 Hydra 骨骼（body/9×neck/9×head/tail/4×leg）
- [ ] [blockbench-modeling] 9 头扇形布局 cube
- [ ] [Blockbench MCP] capture_screenshot 三视角截图
- [ ] 独立审计：9 头 socket 连通性检查
- [ ] [blockbench-texturing] 深绿鳞片+毒腺细节纹理
- [ ] 重写 HydraModel.java → 9 头数组
- [ ] 更新 HydraPoisonSweepGoal 适配 9 头
- [ ] ./gradlew build --offline 通过

#### W0.3 全生物设计签收
- [ ] Cyclops 人工确认（巨大人形+石棒）
- [ ] 其余 7 个 Greek 生物逐个人工视觉复审
- [ ] 设计先行 gate 更新：`creature_design_first_gate.py audit`

### W1：Greek 视觉 Vertical Slice

#### W1.1 Greek 10 生物模型 Polish（每个生物走 7 阶段 workflow）
- [ ] Minotaur：面部细节（牛头角宽>躯干、鼻孔、嘴巴）
- [ ] Cyclops：确认巨大体量、单眼可读、石棒细节
- [ ] Centaur：人马连接、弓箭可读、四蹄分明
- [ ] Medusa：蛇发 6 蛇可读、面部细节、蛇尾分段
- [ ] Nemean Lion：鬃毛>头部、利爪、尾巴末端
- [ ] Manticore：翅膀骨架可读、蝎尾毒刺、人面
- [ ] Chimera：三头可识别（狮/羊/蛇）、翅膀
- [ ] Colossus：盔甲板、面罩、巨人体量
- [ ] Pegasus（W0.1 已重建）
- [ ] Hydra（W0.2 已重建）
- [ ] `creature_visual_acceptance_package.py audit` 无 Greek blocker

#### W1.2 Greek 纹理升级（每个生物）
- [ ] [blockbench-texturing] 128x128 纹理重绘
- [ ] 文明色板：白色大理石、青铜金、橄榄绿
- [ ] 结构阴影：无空白/黑块大面
- [ ] [Blockbench MCP] capture_screenshot 纹理预览

### W2：非希腊 20 生物美术升级

#### W2.1 Egyptian 5 生物
- [ ] anubis_warrior：胡狼头雕刻、弯刀
- [ ] scarab_guardian：甲壳光泽、六足
- [ ] mummy_soldier：绷带层次、眼缝
- [ ] sphinx_sentinel：狮身人面比例
- [ ] sun_falcon：翎羽、太阳纹

#### W2.2 Nordic 5 生物
- [ ] frost_giant：冰晶体量、冰霜盔甲
- [ ] draugr_warrior：腐蚀层次、破甲
- [ ] valkyrie：头盔翅膀、盔甲、长矛
- [ ] fenrir_wolf：巨狼体量、锁链
- [ ] dwarf_berserker：矮壮体量、战斧、盔甲

#### W2.3 Chinese 5 生物
- [ ] azure_dragon：分段龙身、龙须、鹿角
- [ ] qilin_guardian：角+鬃+火焰、鳞片
- [ ] jiangshi：符箓、僵硬姿态、官服
- [ ] nine_tailed_fox：9 尾分层、狐面
- [ ] stone_lion_guardian：卷鬃、绣球、石质

#### W2.4 Atlantean 5 生物
- [ ] crystal_leviathan：水晶分段、发光
- [ ] orichalcum_golem：金属关节、山铜质感
- [ ] siren_priestess：鳞尾、人面、权杖
- [ ] tidal_guardian：蟹壳、水晶、巨爪
- [ ] abyssal_ray：扁平体、生物发光

### W3：动作与动画

#### W3.1 基础动作创建（使用 blockbench-animation）
- [ ] 30 生物 × idle 动画
- [ ] 30 生物 × walk/move 动画
- [ ] 30 生物 × attack 动画
- [ ] 30 生物 × hurt 动画
- [ ] 30 生物 × death 动画

#### W3.2 特殊动作
- [ ] Pegasus：自主飞行 + 骑乘飞行 + 翼拍
- [ ] Hydra：9 头错相 idle/attack/hurt
- [ ] Centaur：弓箭射击动作
- [ ] 飞行生物（Valkyrie/Sun Falcon）：飞行循环
- [ ] 水生生物（Crystal Leviathan/Siren/Tidal/Abyssal）：游泳

#### W3.3 动画同步到 Java
- [ ] 每个动画创建后同步到对应 Model.java 的 setAngles()
- [ ] ./gradlew build --offline 通过
- [ ] 游戏内动画连续帧验证

### W4：神力、粒子与音效

#### W4.1 神力 Playtest
- [ ] 18 个高风险神力优先验收（Ragnarok/Earthquake/Fimbulwinter/Meteor 等）
- [ ] 34 个常规神力 playtest
- [ ] 每个神力记录：效果正确/性能正常/失败路径有反馈

#### W4.2 文明粒子实现
- [ ] 10 种文明粒子独立渲染类（颜色/生命周期/物理差异）
- [ ] 粒子纹理 PNG 创建（可用 imagegen 生成参考）
- [ ] 每个文明的法术替换为文明主题粒子
- [ ] 三段式粒子效果绑定到 Spell 类

#### W4.3 音效
- [ ] 安装或替代 ffprobe/ffmpeg 响度工具
- [ ] 149 个生物音频事件人工听感验收
- [ ] 21 个 final-mix candidate 确认
- [ ] 神力 cast/impact/end 音效验证

### W5：UI、结构与 NBT

#### W5.1 UI 验收
- [ ] 守护者交易 UI 实机验收
- [ ] 神力图鉴 UI 实机验收
- [ ] 分辨率/GUI scale/长文本验收

#### W5.2 结构验收
- [ ] 15 个神殿建筑审美实机验收
- [ ] 功能点位可读性验收

#### W5.3 NBT 回归
- [ ] NBT 真实重启回归
- [ ] 新增中国证书交易/劳作状态持久化验证
- [ ] 48 个 runtime_only 状态逐项确认

### W6：发布候选

- [ ] `tools/mythic_age_1_0_gap_audit.py audit` → release blocking gaps = 0
- [ ] `tools/creature_design_first_gate.py audit` → 30/30 approved
- [ ] `tools/creature_visual_acceptance_package.py audit` → 通过
- [ ] `tools/creature_action_acceptance_package.py audit` → 通过
- [ ] `tools/god_power_gameplay_acceptance_package.py audit` → 通过
- [ ] `tools/creature_audio_final_mix_acceptance_package.py audit` → 通过
- [ ] `./gradlew build --offline` → BUILD SUCCESSFUL
- [ ] README + CHANGELOG + 版本号更新为 1.0.0

---

## 执行顺序与依赖

```
W0 (设计阻断) ──→ W1 (Greek vertical slice) ──→ W2 (非希腊美术)
                                                       │
                                                       ▼
                                              W3 (动作动画) ──→ W4 (神力/粒子/音效)
                                                                       │
                                                                       ▼
                                                              W5 (UI/结构/NBT)
                                                                       │
                                                                       ▼
                                                              W6 (发布候选)
```

**W0 必须最先完成**：Pegasus 和 Hydra 设计重做是全链路阻断。
**W1-W2 可部分并行**：Greek 先做 vertical slice 验证流程，再横向推 20 个非希腊。
**W3 依赖 W1-W2**：模型稳定后才做动画。
**W4-W5 可部分并行**：神力 playtest 不依赖动画完成。

---

## Blockbench MCP 启动前提

开始 W0 前，需要用户执行：

1. **打开 Blockbench 桌面版**
2. **File > Plugins > Load Plugin from URL** → `https://jasonjgardner.github.io/blockbench-mcp-plugin/mcp.js`
3. **确认插件面板显示 MCP Server 已启动**（端口 3000）
4. 回到 Claude Code，确认 `blockbench` MCP 状态变为 connected

一旦连接建立，即可开始使用 blockbench-modeling/texturing/animation skills 通过 MCP 直接操作 Blockbench。
