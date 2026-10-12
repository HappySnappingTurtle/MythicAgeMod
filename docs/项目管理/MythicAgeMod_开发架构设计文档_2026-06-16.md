# MythicAgeMod 开发架构设计文档

版本：1.0-draft | 日期：2026-06-16
配套文档：`MythicAgeMod_全面审计与开发规划_2026-06-16.md`

---

## 一、技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 游戏引擎 | Minecraft | 1.20.1 |
| 模组框架 | Fabric Loader | 0.14.21 |
| API | Fabric API | 0.83.0+1.20.1 |
| 映射 | Yarn | 1.20.1+build.1 |
| 构建 | Gradle + Fabric Loom | 8.2 / 1.3.8 |
| 语言 | Java 17 |  |
| 外部工具 | Python 3（审计/生成管线）、PIL（图像生成）、Blockbench（模型编辑） |  |

---

## 二、模组入口与初始化

### 2.1 入口点

```
fabric.mod.json
├── main: com.example.mythic_age.MythicAgeMod          (服务端+通用)
├── client: com.example.mythic_age.MythicAgeModClient   (客户端)
└── mixins: mythic_age.mixins.json                      (Mixin 配置)
```

### 2.2 服务端初始化顺序（MythicAgeMod.onInitialize）

```
 1. ModItems.registerModItems()              — 295+ 物品（法术书、圣物、证书、刷怪蛋等）
 2. ModBlocks.registerModBlocks()            — 神殿方块、雕像、祭坛、资源堆
 3. ModBlockEntities.registerBlockEntities() — 9 种方块实体（祭坛、雕像基座、圣物台等）
 4. ModEntities.registerModEntities()        — 43 个实体类型 + 属性 + 刷怪规则
 5. ModEntityTypes.registerEntityTypes()     — 投射物实体（Meteor）
 6. ModSounds.registerModSounds()            — 149+ 音效事件
 7. ModParticles.registerModParticles()      — 3 种粒子（MYTHIC_FLAME / DIVINE_SPARKLE / SHOCKWAVE）
 8. ModItemGroups.registerItemGroups()       — 创造模式标签页
 9. ModLootTableModifiers.modifyLootTables() — 战利品表注入
10. ModCombatEvents.register()               — 击杀事件（北欧证书产出）
11. MythicVillageTypes.initialize()          — 五文明村庄类型定义
12. VillageGenerationEventHandler.register() — 村庄生成事件监听
13. ModNetworking.registerNetworkPackets()   — 服务端网络包处理
14. ValidationCommand.register()             — 调试验证命令
15. ServerTickEvents.START_WORLD_TICK →       — 每 tick 驱动：
    ├── MythicAgeWorldState.tick()              世界状态（地震/闪电/天气）
    ├── GodPowerCertificateManager.tickEgyptianStatues()  埃及雕像产出
    └── GodPowerTaskScheduler.tick()            延迟任务调度器
```

### 2.3 客户端初始化顺序（MythicAgeModClient.onInitializeClient）

```
 1. ModClientParticles.registerClientParticles()     — 粒子工厂注册
 2. EntityRendererRegistry.register() × 33           — 生物渲染器
 3. EntityModelLayerRegistry.registerModelLayer() × 33 — 模型层数据
 4. Temple Guardian 变体渲染器 × 11                  — 标准 + 自定义守护者
 5. ModNetworkClient.registerClientNetworkHandlers() — 客户端网络包处理
 6. VisualProbeServer.startIfEnabled()               — 可选调试截图服务
```

---

## 三、包结构总览

```
com.example.mythic_age/                          168 个 Java 文件
│
├── MythicAgeMod.java                            模组主入口（服务端）
├── MythicAgeModClient.java                      客户端入口
│
├── entity/                                      58 文件 — 实体层
│   ├── ModEntities.java                         实体注册中心（890 行）
│   ├── ModEntityTypes.java                      投射物类型注册
│   ├── GreekMythEntity.java                     希腊文明基类（abstract）
│   ├── EgyptianMythEntity.java                  埃及文明基类
│   ├── NordicMythEntity.java                    北欧文明基类
│   ├── ChineseMythEntity.java                   中华文明基类
│   ├── AtlanteanMythEntity.java                 亚特兰蒂斯文明基类
│   ├── CivilizedVillager.java                   文明化村民接口
│   ├── [30 个具体生物 Entity 类]
│   ├── TempleGuardianEntity.java                神殿守护者（基于 VillagerEntity）
│   ├── CustomTempleGuardianEntity.java          自定义交易守护者（基于 MobEntity）
│   ├── WalkingTreeEntity.java                   召唤实体
│   ├── MeteorEntity.java                        投射物
│   ├── WalkingTreeVariant.java                  行走树变体枚举
│   └── ai/goal/
│       ├── MinotaurChargeAttackGoal.java        牛头人冲锋 AI
│       └── VillagerPilgrimageGoal.java          村民朝圣 AI
│
├── client/                                      67 文件 — 客户端渲染层
│   ├── gui/
│   │   ├── CustomTempleGuardianTradeScreen.java 守护者交易 GUI
│   │   └── GodPowerGuideScreen.java             神力图鉴 GUI
│   ├── particle/
│   │   ├── ModClientParticles.java              粒子工厂注册
│   │   ├── MythicFlameParticle.java             自定义粒子实现
│   │   ├── DivineSparkleParticle.java
│   │   └── ShockwaveParticle.java
│   ├── render/
│   │   ├── MeteorEntityRenderer.java
│   │   └── entity/
│   │       ├── [33 个 EntityRenderer 类]
│   │       ├── TempleGuardianTextures.java      守护者纹理映射
│   │       └── model/
│   │           ├── ModModelLayers.java          模型层常量定义
│   │           └── [33 个 Model 类]
│   └── visual/
│       └── VisualProbeServer.java               调试用截图 HTTP 服务
│
├── item/                                        6 文件 — 物品层
│   ├── ModItems.java                            物品注册中心
│   ├── ModItemGroups.java                       创造标签页
│   ├── SpellbookItem.java                       法术书（4325 行，55+ 法术）
│   ├── RelicItem.java                           圣物（三层效果系统）
│   ├── GodPowerCertificateItem.java             证书物品
│   └── GodPowerGuideItem.java                   图鉴物品
│
├── block/                                       10 文件 — 方块层
│   ├── ModBlocks.java                           方块注册中心
│   ├── TempleBlock.java                         神殿装饰方块
│   ├── StatueBlock.java                         雕像方块（含证书交易定价矩阵）
│   ├── StatueBaseBlock.java                     雕像基座（证书存储）
│   ├── EgyptianStatuePedestalBlock.java         埃及雕像基座
│   ├── AbundanceAltarCoreBlock.java             丰饶祭坛核心
│   ├── RelicPedestalBlock.java                  圣物台
│   ├── AnimalLureTowerBlock.java                动物诱引塔
│   ├── MeatPileBlock.java                       肉堆
│   └── WoodPileBlock.java                       木材堆
│   └── entity/                                  6 文件 — 方块实体
│       ├── ModBlockEntities.java                方块实体注册中心
│       ├── AbundanceAltarCoreBlockEntity.java   祭坛（多方块验证 + 周期产出）
│       ├── EgyptianStatuePedestalBlockEntity.java 埃及雕像（分级证书产出）
│       ├── RelicPedestalBlockEntity.java        圣物激活
│       ├── StatueBaseBlockEntity.java           证书 NBT 存储
│       └── AnimalLureTowerBlockEntity.java      动物吸引
│
├── system/                                      2 文件 — 经济系统
│   ├── GodPowerCertificateManager.java          证书全局管理器（单例）
│   └── CertificateGenerationManager.java        证书产出逻辑
│
├── util/                                        6 文件 — 工具类
│   ├── GodPowerTaskScheduler.java               tick 延迟任务调度
│   ├── GodPowerWeatherManager.java              天气效果管理
│   ├── EarthquakeManager.java                   地震效果管理
│   ├── LightningStormManager.java               闪电风暴管理
│   ├── StructureGenerator.java                  结构生成工具
│   └── MultiBlockStructureUtil.java             多方块结构工具
│
├── world/                                       1 文件 — 世界持久化
│   └── MythicAgeWorldState.java                 PersistentState 实现
│
├── worldgen/                                    4 文件 — 世界生成
│   ├── MythicVillageTypes.java                  五文明村庄定义（权重/群系映射）
│   ├── MythicVillageProcessor.java              村庄生成处理器
│   ├── VillageManager.java                      村庄管理
│   └── BiomeCivilizationMapper.java             群系→文明映射
│
├── event/                                       3 文件 — 事件处理
│   ├── ModCombatEvents.java                     击杀事件（北欧证书）
│   ├── ModLootTableModifiers.java               战利品表注入
│   └── VillageGenerationEventHandler.java       村庄生成事件
│
├── network/                                     2 文件 — 网络通信
│   ├── ModNetworking.java                       服务端包注册与处理
│   └── ModNetworkClient.java                    客户端包注册与处理
│
├── sound/                                       1 文件
│   └── ModSounds.java                           音效事件注册
│
├── particle/                                    2 文件
│   ├── ModParticles.java                        粒子类型注册
│   └── MythicParticleEffects.java               粒子效果工具方法
│
├── config/                                      1 文件
│   └── ModConfig.java                           调试标志 + 性能参数
│
├── validation/                                  1 文件
│   └── ValidationCommand.java                   调试命令
│
└── mixin/                                       1 文件
    └── VillagerAIEnhancementMixin.java          村民 AI 增强
```

---

## 四、实体架构

### 4.1 继承层次

```
                    ┌─────────────────────────┐
                    │  net.minecraft Entity    │
                    └────────────┬────────────┘
                                 │
              ┌──────────────────┼──────────────────┐
              ▼                  ▼                   ▼
       HostileEntity       AnimalEntity         MobEntity
              │                  │                   │
    ┌─────────┼─────────┐       │                   │
    ▼         ▼         ▼       ▼                   ▼
 Greek   Egyptian   Nordic   Pegasus        CustomTemple
 Myth     Myth      Myth    Entity          GuardianEntity
 Entity   Entity    Entity  (被动飞行坐骑)    (自定义交易)
    │         │         │
    │         │         │     ┌──────────────┐
    ▼         ▼         ▼     ▼              ▼
 Chinese   Atlantean       VillagerEntity   WalkingTree
  Myth      Myth           (TempleGuardian)  Entity
  Entity    Entity                           (召唤生物)
```

**五文明基类共享结构（以 GreekMythEntity 为例）：**

```java
public abstract class GreekMythEntity extends HostileEntity {
    private int specialCooldownTicks;

    // 共享行为
    void tick()                     — 管理技能冷却
    boolean tryAttack(Entity)       — 播放攻击音效（文明特定音高变体）
    boolean canUseSpecial()         — 技能冷却检查
    void setSpecialCooldown(int)    — 设置冷却
    void damageAndKnockback(...)    — 技能伤害 + 击退
    void spawnParticles(...)        — 粒子效果生成

    // 子类必须实现
    abstract SoundEvent getAttackSound();
}
```

**音高变体差异：**

| 基类 | 攻击音高 | 设计意图 |
|------|----------|----------|
| GreekMythEntity | 0.85F + random 0.25F | 宽音域，神话感 |
| AtlanteanMythEntity | 0.9F + random 0.18F | 窄音域，水下回响 |
| NordicMythEntity | 0.82F + random 0.18F | 低沉，战斗感 |
| EgyptianMythEntity | 同 Greek 模式 | 标准 |
| ChineseMythEntity | 同 Greek 模式 | 标准 |

### 4.2 实体注册模式

每个生物在 `ModEntities.java` 中注册三个维度：

```java
// 1. 实体类型定义（FabricEntityTypeBuilder）
public static final EntityType<MinotaurEntity> MINOTAUR =
    Registry.register(Registries.ENTITY_TYPE,
        new Identifier(MOD_ID, "minotaur"),
        FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, MinotaurEntity::new)
            .dimensions(EntityDimensions.fixed(1.35f, 2.75f))
            .trackRangeBlocks(8)
            .build());

// 2. 属性注册（FabricDefaultAttributeRegistry）
FabricDefaultAttributeRegistry.register(MINOTAUR, MinotaurEntity.createMinotaurAttributes());

// 3. 刷怪限制（SpawnRestriction）
SpawnRestriction.register(MINOTAUR,
    SpawnRestriction.Location.ON_GROUND,
    Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
    HostileEntity::canSpawnInDark);
```

### 4.3 实体尺寸与刷怪总表

| 文明 | 生物 | 碰撞体(宽×高) | 刷怪组 | 群系 | 权重 |
|------|------|---------------|--------|------|------|
| **Greek** | Minotaur | 1.35×2.75 | MONSTER | 平原/黑森林 | 6 |
| | Cyclops | 1.8×4.0 | MONSTER | 山地 | 8 |
| | Centaur | 1.9×3.0 | MONSTER | 稀树草原 | 12 |
| | Hydra | 3.2×2.8 | MONSTER | 沼泽 | 5 |
| | Medusa | 1.1×2.55 | MONSTER | 丛林 | 8 |
| | Nemean Lion | 1.9×1.8 | MONSTER | 稀树草原 | 8 |
| | Manticore | 2.0×2.05 | MONSTER | 恶地 | 7 |
| | Chimera | 2.3×2.25 | MONSTER | 丛林 | 5 |
| | Colossus | 1.7×4.2 | MONSTER | 山地 | 3 |
| | Pegasus | 1.7×2.1 | CREATURE | 山地 | 4 |
| **Egyptian** | Anubis Warrior | 0.9×2.3 | MONSTER | 沙漠/恶地 | 8 |
| | Scarab Guardian | 1.8×1.2 | MONSTER | 沙漠/恶地 | 7 |
| | Mummy Soldier | 0.8×2.1 | MONSTER | 沙漠 | 12 |
| | Sphinx Sentinel | 2.2×1.8 | MONSTER | 沙漠/恶地 | 5 |
| | Sun Falcon | 1.2×1.3 | MONSTER | 沙漠/草原 | 6 |
| **Nordic** | Frost Giant | 1.8×3.9 | MONSTER | 雪地 | 5 |
| | Draugr Warrior | 0.85×2.2 | MONSTER | 雪地/针叶林/深暗 | 10 |
| | Valkyrie | 0.9×2.35 | MONSTER | 雪山 | 6 |
| | Fenrir Wolf | 2.1×1.8 | MONSTER | 针叶林/黑森林 | 5 |
| | Dwarf Berserker | 0.95×1.45 | MONSTER | 山峰/针叶林 | 8 |
| **Chinese** | Azure Dragon | 1.6×2.2 | MONSTER | 丛林 | 4 |
| | Qilin Guardian | 1.5×2.0 | MONSTER | 多群系 | 6 |
| | Jiangshi | 0.8×2.1 | MONSTER | 沼泽/黑森林 | 10 |
| | Nine-tailed Fox | 1.2×1.4 | MONSTER | 樱花林/针叶林 | 7 |
| | Stone Lion Guardian | 1.6×1.7 | MONSTER | 山峰/丛林 | 6 |
| **Atlantean** | Crystal Leviathan | 2.4×2.4 | MONSTER | 海洋 | 3 |
| | Orichalcum Golem | 1.7×3.2 | MONSTER | 沙滩/海洋 | 5 |
| | Siren Priestess | 0.9×2.2 | MONSTER | 沙滩/海洋 | 7 |
| | Tidal Guardian | 2.0×1.4 | MONSTER | 沙滩/海洋 | 6 |
| | Abyssal Ray | 2.6×0.9 | MONSTER | 深海 | 5 |

### 4.4 AI 目标优先级模式

所有敌对生物遵循统一的 AI 目标优先级框架：

```
优先级 0: SwimGoal              — 水中生存
优先级 1: 特殊攻击 Goal          — MinotaurChargeAttackGoal 等
优先级 2: MeleeAttackGoal       — 近战（或 ProjectileAttackGoal）
优先级 5: WanderAroundFarGoal   — 远距离游荡
优先级 6: LookAtEntityGoal      — 注视玩家
优先级 7: LookAroundGoal        — 环顾四周

目标选择器：
优先级 1: RevengeGoal           — 反击攻击者
优先级 2: ActiveTargetGoal      — 主动追踪玩家
```

**自定义 AI Goal：**

| Goal | 生物 | 行为 |
|------|------|------|
| `MinotaurChargeAttackGoal` | Minotaur | 4-16 格距离冲锋，1.2x 速度，20 tick 持续，100 tick 冷却 |
| `VillagerPilgrimageGoal` | CivilizedVillager | 村民朝向神殿移动 |

### 4.5 特殊能力系统

生物特殊技能通过文明基类的 `canUseSpecial()` + `setSpecialCooldown()` 管理：

| 生物 | 技能 | 冷却(tick) | 范围 | 效果 |
|------|------|-----------|------|------|
| Hydra | 毒液横扫 | 120 | 4.2D AoE | 5.0F 伤害 + Poison I 100t |
| Tidal Guardian | 巨爪碎击 | 125 | 2.8D AoE | 6.5F 伤害 + Slowness II 80t |
| Medusa | 石化凝视 | ~100 | 视线内 | 减速/石化 |
| Cyclops | 投石 | ~80 | 远程 | 投射物伤害 |
| Centaur | 弓箭射击 | 持续 | 远程 | RangedAttackMob 接口 |

---

## 五、渲染架构

### 5.1 三层绑定

每个可见实体由三个类协同工作：

```
Entity 类          →  行为、属性、AI
    ↓ 绑定 via ModEntities
Model 类           →  几何结构、UV、动画（setAngles）
    ↓ 绑定 via ModModelLayers
Renderer 类        →  纹理绑定、缩放、特殊渲染
```

**注册链路：**

```java
// ModModelLayers.java — 声明模型层标识
public static final EntityModelLayer MINOTAUR =
    new EntityModelLayer(new Identifier("mythic_age", "minotaur"), "main");

// MythicAgeModClient — 注册模型数据提供者
EntityModelLayerRegistry.registerModelLayer(
    ModModelLayers.MINOTAUR, MinotaurModel::getTexturedModelData);

// MythicAgeModClient — 注册渲染器（自动获取模型层）
EntityRendererRegistry.register(
    ModEntities.MINOTAUR, MinotaurEntityRenderer::new);
```

### 5.2 Model 类结构

所有模型类继承 `SinglePartEntityModel<T>` 并实现统一模式：

```java
public class MinotaurModel extends SinglePartEntityModel<MinotaurEntity> {

    // 1. 字段：缓存所有 ModelPart 引用
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;
    // ... 57 个字段（Minotaur）

    // 2. 构造器：从 root ModelPart 递归获取子部件
    public MinotaurModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        // ...
    }

    // 3. 静态工厂：定义几何结构、UV 映射、纹理尺寸
    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        ModelPartData head = root.addChild("head",
            ModelPartBuilder.create()
                .uv(0, 0).cuboid(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F),
            ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        // 子部件嵌套
        head.addChild("left_horn", ...);

        return TexturedModelData.of(modelData, 128, 128); // 纹理尺寸
    }

    // 4. 动画：每帧调用
    @Override
    public void setAngles(MinotaurEntity entity, float limbAngle,
                          float limbDistance, float animationProgress,
                          float headYaw, float headPitch) {
        // 头部跟随视角
        this.head.pitch = headPitch * 0.017453292F;
        this.head.yaw = headYaw * 0.017453292F;

        // 四肢行走动画
        this.leftLeg.pitch = MathHelper.cos(limbAngle * 0.6662F) * 1.4F * limbDistance;
        this.rightLeg.pitch = MathHelper.cos(limbAngle * 0.6662F + PI) * 1.4F * limbDistance;

        // 受击抖动
        if (entity.hurtTime > 0) { ... }
    }

    // 5. 渲染器入口
    @Override
    public ModelPart getPart() { return this.root; }
}
```

### 5.3 模型复杂度分级

| 级别 | ModelPart 数 | 代表生物 | 特征 |
|------|-------------|----------|------|
| Ultra（65+） | 65-70 | Centaur | 双体（马+人）、弓箭系统、箭袋、面部细节 |
| Ultra（60+） | 59-65 | Medusa, Manticore, Chimera | 蛇发系统/翅膀+蝎尾/三头 |
| Very High（40-60） | 50-57 | Minotaur, Hydra, Nemean Lion, Colossus | 多头数组/鬃毛系统/盔甲细节 |
| High（30-40） | 35-45 | Pegasus, Valkyrie, Fenrir Wolf, Nine-tailed Fox | 翅膀+骑乘/九尾分层 |
| Medium（15-30） | 20-28 | Draugr Warrior, Jiangshi, Sun Falcon | 基础人形+武器或翅膀 |
| Low（8-15） | 8-15 | Anubis Warrior, Temple Guardian | 简单人形 |

### 5.4 Renderer 标准模式

```java
public class MinotaurEntityRenderer
        extends MobEntityRenderer<MinotaurEntity, MinotaurModel> {

    private static final Identifier TEXTURE =
        new Identifier("mythic_age", "textures/entity/minotaur/minotaur.png");

    public MinotaurEntityRenderer(EntityRendererFactory.Context ctx) {
        super(ctx,
              new MinotaurModel(ctx.getPart(ModModelLayers.MINOTAUR)),
              0.95f);  // shadow radius
    }

    @Override
    public Identifier getTexture(MinotaurEntity entity) {
        return TEXTURE;
    }

    // 可选：缩放
    @Override
    protected void scale(MinotaurEntity entity, MatrixStack matrices, float amount) {
        matrices.scale(1.18F, 1.18F, 1.18F);
    }
}
```

**纹理路径约定：** `textures/entity/{creature_id}/{creature_id}.png`
**纹理尺寸：** 128×128（希腊系列）

---

## 六、玩法系统架构

### 6.1 神力（法术书）系统

```
SpellbookItem.java（4325 行，55+ 法术）
│
├── SpellType 枚举                    — 55 种法术定义
│   ├── Greek: 12 种（LIGHTNING_STORM, RESTORATION, EARTHQUAKE...）
│   ├── Egyptian: 10 种（RA_RADIANCE, METEOR, ISIS_RESURRECTION...）
│   ├── Nordic: 10 种（RAGNAROK, WALKING_WOODS, FIMBULWINTER...）
│   ├── Chinese: 10 种（FIVE_THUNDER, RECREATION, FLOOD...）
│   └── Atlantean: 10+ 种（TIDAL_SURGE, CHRONO_STASIS...）
│
├── 施放管线
│   ├── use() → getTargetPos()       — 50 格射线检测目标位置
│   ├── castSpell(SpellType, pos)    — 分发到具体施放方法
│   ├── applyEffects()               — 实体/方块/状态效果
│   ├── spawnParticles()             — 粒子效果
│   ├── playSound()                  — 音效
│   └── GodPowerTaskScheduler.schedule() — 多 tick 延迟任务
│
├── 冷却系统
│   ├── 每个法术书实例独立冷却计时
│   ├── cooldownTicks: 100-600 tick（5-30 秒）
│   └── tooltip 显示剩余冷却
│
├── 分类系统（getSpellCategory）
│   ├── offensive   — 直接伤害
│   ├── healing     — 恢复
│   ├── utility     — 辅助/侦查
│   ├── summoning   — 召唤实体
│   ├── debuff      — 削弱
│   ├── buff        — 强化
│   ├── transport   — 传送
│   └── resource    — 资源生成
│
└── 复杂法术示例
    ├── Walking Woods — 唤醒附近树木为 WalkingTreeEntity
    │   └── 多阶段：检测树→破坏方块→生成实体
    ├── Dwarven Mine — 五阶段地形生成（山丘+矿石）
    │   └── 通过 GodPowerTaskScheduler 分 tick 执行
    └── Ragnarok — 大范围破坏+火焰+TNT+凋灵效果
```

### 6.2 证书经济系统

```
五文明证书产出路径各不相同：

Greek ─── 神殿附近村民人口
│         CertificateGenerationManager
│         └── 每 2400 tick 检查 32 格内村民数 → 查表产出
│             1-2人→1证书, 3-4→2, 5-7→3, 8-11→5, 12-15→7, 16-19→10, 20+→15
│
Egyptian ── 雕像基座自动产出
│           EgyptianStatuePedestalBlockEntity
│           └── 7 级雕像，每级不同产出速率
│               SLAVE(1/2min), CITIZEN(2), SOLDIER(3), QUEEN(5/1.5min),
│               KING(7), ROYAL_COUPLE(12/1min), PHARAOH(20/1min)
│
Chinese ─── 人口驱动（类似 Greek）
│           CertificateGenerationManager
│           └── 每 20 村民 = 1 基础证书 + 随机加成
│
Nordic ──── 怪物击杀掉落
│           ModCombatEvents → GodPowerCertificateManager.handleNordicKill()
│           └── 末影龙 100%(50), 凋灵 100%(30), 烈焰人 20%(3), 普通怪 3-5%(1)
│
Atlantean ── 守护者交易
             CustomTempleGuardianEntity
             └── 海晶碎片/棱镜石 → 证书交换

证书存储路径：
产出 → StatueBaseBlockEntity(NBT 存储) → 玩家潜行右键取出 → 背包
```

### 6.3 守护者交易系统

```
CustomTempleGuardianEntity（extends MobEntity）
│
├── 交易数据结构
│   ├── Map<ItemStack, ItemStack> tradeOffers    — 输入→输出映射
│   └── Map<String, Long> tradeCooldowns          — 物品冷却时间戳
│
├── 文明定价矩阵（StatueBlock.java 中定义）
│   ├── 2 证书: 基础法术（Restoration, Lure...）
│   ├── 3-4 证书: 中级法术
│   ├── 5-6 证书: 高级法术
│   ├── 7-8 证书: 史诗法术（Ragnarok, Meteor...）
│   └── 神话生物蛋: 特殊定价 + 1200 tick 冷却
│
├── 网络协议
│   ├── 服务端 → 客户端: OPEN_CUSTOM_TEMPLE_GUARDIAN_TRADE_SCREEN
│   │   └── PacketByteBuf: entityId + mythologyType + trades[]
│   └── 客户端 → 服务端: EXECUTE_CUSTOM_TEMPLE_GUARDIAN_TRADE
│       └── PacketByteBuf: entityId + inputStack + outputStack
│
└── GUI
    └── CustomTempleGuardianTradeScreen（294×188 像素）
        ├── 左面板: 交易列表（5 行可见，分页）
        └── 右面板: 交易详情 + 执行按钮
```

### 6.4 圣物系统

```
RelicItem（extends Item）
│
├── 三层效果架构
│   ├── 第一层: 手持效果（inventoryTick, selected=true）
│   │   └── 每 40 tick 刷新状态效果
│   │       例: 阿瑞斯头盔 → Strength I
│   │       例: 昆仑镜 → Night Vision + Glowing
│   │
│   ├── 第二层: 被动效果（inventoryTick, selected=false）
│   │   └── 每 200-600 tick 触发
│   │       例: 金羊毛 → +1 XP / 10 秒
│   │       例: 女娲石 → 修复盔甲 / 30 秒
│   │       例: 芙蕾雅项链 → 生成宝石 / 20 秒
│   │
│   └── 第三层: 神殿效果（放置在 RelicPedestal）
│       └── 区域增益（设计中，未完整实现）
│
└── 15 圣物 × 5 文明
    ├── Greek: 拉之权杖、阿瑞斯头盔、金羊毛、丰饶之角
    ├── Egyptian: 荷鲁斯之眼、伊西斯权杖、阿努比斯之杖、拉之圣甲虫
    ├── Chinese: 昆仑镜、女娲石、神农鼎、后羿弓
    └── Nordic: 奥丁之眼、芙蕾雅项链、洛基面具
```

---

## 七、世界生成与持久化

### 7.1 村庄生成

```
世界生成管线：
│
├── data/mythic_age/worldgen/
│   ├── structure/                    — 5 文明神殿结构定义 JSON
│   ├── structure_set/
│   │   └── mythic_villages.json      — 结构集配置
│   └── template_pool/village/
│       └── {civilization}/town_centers.json  — 模板池
│
├── Java 处理链：
│   ├── BiomeCivilizationMapper       — 群系→文明映射
│   ├── MythicVillageTypes            — 五文明权重分配
│   │   ├── BASE_MYTHIC_VILLAGE_CHANCE: 10%
│   │   ├── GREEK_WEIGHT: 30%
│   │   ├── EGYPTIAN_WEIGHT: 20%
│   │   ├── CHINESE_WEIGHT: 20%
│   │   ├── ATLANTEAN_WEIGHT: 15%
│   │   └── NORDIC_WEIGHT: 15%
│   ├── MythicVillageProcessor        — 村庄生成处理
│   └── VillageManager                — 村庄管理
│
└── NBT 模板（15 个）：
    data/mythic_age/structures/village/{civ}/temple_{small|medium|large}.nbt
```

### 7.2 世界状态持久化

```
MythicAgeWorldState extends PersistentState
│
├── 存储键: "mythic_age_world_state"
├── 版本: 1
│
├── 管理的子系统
│   ├── LightningStormManager     — 活跃闪电风暴列表
│   ├── EarthquakeManager         — 活跃地震列表
│   ├── GodPowerWeatherManager    — 神力天气效果
│   └── GodPowerCertificateManager — 证书产出状态
│
├── NBT 序列化
│   writeNbt() {
│       nbt.put("LightningStorms", ...)
│       nbt.put("Earthquakes", ...)
│       nbt.put("GodPowerWeather", ...)
│       nbt.put("GodPowerCertificates", ...)
│   }
│
└── Tick 驱动
    tick(ServerWorld) {
        earthquakeManager.tick(world)
        lightningStormManager.tick(world)
        godPowerWeatherManager.tick(world)
    }
```

### 7.3 NBT 状态覆盖

| 类别 | Schema Owner 数 | 状态 |
|------|-----------------|------|
| 世界级效果管理 | 4 | 通过 |
| 方块实体 | 5 | 通过 |
| 实体特殊状态 | 5+ | 部分 runtime_only |
| **总计** | **14 covered + 48 runtime_only** | 真实重启通过 |

---

## 八、网络通信

### 8.1 数据包定义

| 方向 | 包标识 | 用途 | 载荷 |
|------|--------|------|------|
| S→C | `open_custom_temple_guardian_trade_screen` | 打开交易 GUI | entityId, mythType, trades[] |
| C→S | `execute_custom_temple_guardian_trade` | 执行交易 | entityId, inputStack, outputStack |
| S→C | `open_god_power_guide_screen` | 打开图鉴 | civilization string |

### 8.2 交易执行验证

```
客户端发送交易请求
    ↓
服务端验证：
    1. 玩家距离 ≤ 36 格（6² × 距离检查）
    2. 实体存活且类型正确
    3. 输入物品匹配且数量足够
    4. 交易冷却已结束
    ↓
执行：移除输入 → 添加输出 → 设置冷却 → 发送消息
```

---

## 九、资源层结构

```
src/main/resources/
├── fabric.mod.json                           模组元数据
├── mythic_age.mixins.json                    Mixin 配置
│
├── assets/mythic_age/
│   ├── lang/
│   │   ├── en_us.json                        英文
│   │   ├── zh_cn.json                        中文
│   │   └── ja_jp.json                        日文
│   ├── textures/
│   │   ├── entity/                           30+ 生物纹理目录
│   │   │   ├── {creature_id}/{creature_id}.png
│   │   │   └── walking_tree/{wood_type}.png  8 种木材变体
│   │   ├── block/                            方块纹理
│   │   └── item/                             物品纹理
│   ├── models/
│   │   ├── block/                            方块模型 JSON
│   │   └── item/                             物品模型 JSON
│   ├── blockstates/                          70+ 方块状态 JSON
│   ├── sounds/
│   │   └── entity/{creature_id}/             每生物 4-6 个 OGG
│   ├── sounds.json                           音效事件注册
│   └── particles/                            3 个粒子定义 JSON
│
└── data/mythic_age/
    ├── structures/village/
    │   └── {civ}/temple_{size}.nbt           15 个神殿 NBT
    └── worldgen/
        ├── structure/                        5 个结构定义
        ├── structure_set/                    结构集配置
        └── template_pool/village/            5 个模板池
```

---

## 十、外部工具链架构

### 10.1 Python 工具生态

```
tools/                                  40+ Python 脚本
│
├── 视觉管线（生成 + 审计）
│   ├── greek_creature_visual_pipeline.py     希腊生物视觉管线（source of truth）
│   ├── egyptian_creature_visual_pipeline.py  埃及
│   ├── nordic_creature_visual_pipeline.py    北欧
│   ├── chinese_creature_visual_pipeline.py   中华
│   └── atlantean_creature_visual_pipeline.py 亚特兰蒂斯
│
├── 设计门禁
│   ├── creature_design_first_gate.py         全生物设计签收门禁
│   ├── greek_design_first_gate.py            希腊设计门禁
│   ├── pegasus_hydra_design_correctness_gate.py
│   ├── creature_visual_acceptance_package.py 视觉验收包
│   └── creature_human_visual_acceptance_ledger.py 人工视觉台账
│
├── 模型质量
│   ├── creature_proportion_quality_gate.py   比例质量门禁
│   ├── greek_runtime_model_quality_gate.py   希腊运行时模型质量
│   ├── egyptian_runtime_model_quality_gate.py
│   ├── nordic/chinese/atlantean_runtime_model_quality_gate.py
│   └── audit_walking_tree_model.py           行走树专用审计
│
├── MCP 服务
│   ├── blockbench_visual_mcp.py              Blockbench 视觉门禁 MCP
│   │   └── 工具: generate_design_gate_assets / audit_design_gate /
│   │           list_creatures / get_creature_design
│   └── mc_visual_probe_mcp.py                MC 运行时截图 MCP
│
├── 发布审计
│   ├── mythic_age_1_0_gap_audit.py           1.0 缺口审计
│   ├── mythic_age_1_0_release_gate.py        发布门禁
│   ├── mythic_age_1_0_scope_matrix.py        范围矩阵
│   └── mythic_age_1_0_requirement_trace.py   需求追踪
│
└── 同步/生成工具
    ├── sync_walking_tree_bbmodel.py           Java↔BBModel 同步
    └── generate_canonical_temple_structures.py 神殿 NBT 生成
```

### 10.2 Blockbench MCP 工作流

```
设计合同签收
    ↓
阶段 0: 冻结本轮目标（仅 1-2 个）
    ↓
阶段 1: 结构拆解（写结构件清单）
    ↓
阶段 2: Java Model 修改（真值源）
    ↓   └── python3 tools/sync_{creature}_bbmodel.py
阶段 3: 本地脚本审计
    ↓   └── UV/尺寸/连通性/比例检查
阶段 4: 独立 SubAgent 审计（硬门槛）
    ↓
阶段 5: Blockbench 三视角审查
    ↓   └── 用户确认结构逻辑
阶段 6: ./gradlew build → 游戏验证
    ↓
阶段 7: 记录结论 → 下一轮
```

---

## 十一、系统间数据流

### 11.1 核心玩法循环

```
玩家探索世界
    │
    ├─→ 发现文明神殿
    │       │
    │       ├─→ 神殿中找到守护者
    │       │       │
    │       │       └─→ 交易: 证书 → 法术书/圣物/刷怪蛋
    │       │
    │       └─→ 神殿中找到雕像
    │               │
    │               └─→ 交易: 证书 → 法术书（定价矩阵）
    │
    ├─→ 获取证书（文明特定路径）
    │       ├── Greek: 村民人口自动产出
    │       ├── Egyptian: 雕像基座自动产出
    │       ├── Chinese: 人口驱动
    │       ├── Nordic: 击杀怪物掉落
    │       └── Atlantean: 守护者交易
    │
    ├─→ 使用法术书
    │       │
    │       └─→ SpellbookItem.use()
    │               ├── 射线检测目标
    │               ├── 执行效果（实体/方块/天气）
    │               ├── 粒子 + 音效
    │               └── 冷却
    │
    ├─→ 遭遇神话生物
    │       │
    │       └─→ 战斗/驯服/骑乘
    │               ├── 特殊技能（基类 canUseSpecial）
    │               └── 掉落物
    │
    └─→ 使用圣物
            └─→ 手持效果 / 被动效果 / 神殿放置效果
```

### 11.2 系统间依赖图

```
    ┌──────────────────────┐
    │   MythicAgeWorldState │ ← ServerTickEvents
    │   (PersistentState)   │
    └──────┬───────────────┘
           │ manages
    ┌──────┼──────────────────────┐
    │      │                      │
    ▼      ▼                      ▼
Lightning Earthquake  GodPower   GodPowerCertificate
Storm     Manager    Weather     Manager (singleton)
Manager              Manager         │
    │                  │              │
    └────────┬─────────┘              │
             ▼                        ▼
       SpellbookItem ◄──────── CertificateGeneration
       (55+ spells)             Manager
             │                        │
             │ spawns                 │ stores in
             ▼                        ▼
       ModEntities              StatueBaseBlock
       (30 creatures +           Entity (NBT)
        WalkingTree +                 │
        Meteor)                       │ extracted by
             │                        ▼
             │ rendered by        Player inventory
             ▼                        │
       EntityRenderer              uses for
       + Model                        │
       + ModelLayer                   ▼
                              CustomTempleGuardian
                              (trading) → GUI
```

---

## 十二、配置与调试

### 12.1 ModConfig 参数

| 类别 | 参数 | 默认值 | 用途 |
|------|------|--------|------|
| 调试 | DEBUG_MODE | false | 通用调试日志 |
| 调试 | DEBUG_METEOR | false | 陨石投射物调试 |
| 调试 | DEBUG_TRADING | false | 交易系统调试 |
| 调试 | DEBUG_VILLAGE_GEN | false | 村庄生成调试 |
| 调试 | DEBUG_TEMPLE_GUARDIAN | false | 守护者调试 |
| 调试 | DEBUG_AI | false | AI 行为调试 |
| 性能 | MAX_METEORS_PER_SPELL | 5 | 每次施法最大陨石数 |
| 性能 | PARTICLE_DISTANCE_THRESHOLD | 32 | 粒子近距离阈值 |
| 性能 | PARTICLE_FAR_DISTANCE_MULTIPLIER | 0.35 | 远距离粒子密度缩放 |
| 性能 | PARTICLE_MAX_RENDER_DISTANCE | 64 | 粒子最大渲染距离 |
| 性能 | MAX_PARTICLES_PER_BURST | 160 | 单次爆发最大粒子数 |
| 日志 | 节流限制 | 10 msg/tick | 防止日志刷屏 |

### 12.2 调试工具

| 工具 | 入口 | 用途 |
|------|------|------|
| ValidationCommand | `/mythic_age validate` (op 2) | 运行时实体/NBT 验证 |
| VisualProbeServer | JVM flag `-Dmythic.visualProbe=true` | HTTP 截图+NBT 探针 |
| ModConfig.debugLog | 代码内调用 | 分类节流日志 |

---

## 十三、Mixin 使用

当前仅有一个 Mixin：

```java
// VillagerAIEnhancementMixin.java
// 目标: net.minecraft.entity.passive.VillagerEntity
// 作用: 增强原版村民 AI，使其感知文明属性
// 用途: 村民朝圣行为（VillagerPilgrimageGoal）
```

**Mixin 配置：**
- 兼容级别: JAVA_17
- 必需: true
- 无客户端/服务端分离 Mixin

---

## 十四、关键设计模式

| 模式 | 使用场景 | 实现 |
|------|----------|------|
| **枚举分发** | 法术类型、文明类型、雕像等级 | SpellType, MythologyType, EgyptianStatueType |
| **文明继承** | 实体基类按文明分层 | GreekMythEntity → MinotaurEntity |
| **单例+世界状态** | 全局管理器绑定世界生命周期 | GodPowerCertificateManager.getInstance() |
| **三层绑定** | Entity↔Model↔Renderer 解耦 | ModEntities + ModModelLayers + Renderer |
| **Tick 调度** | 延迟/多阶段法术效果 | GodPowerTaskScheduler.schedule(world, delay, task) |
| **NBT 持久化** | 方块实体/世界状态跨重启 | writeNbt/readNbt + PersistentState |
| **客户端-服务端分离** | 交易 GUI、图鉴打开 | ModNetworking + ModNetworkClient |
| **多方块验证** | 祭坛、雕像结构完整性 | MultiBlockStructureUtil.isStructureValid() |
| **MCP 服务** | 外部工具集成 | blockbench_visual_mcp.py（JSONRPC 2.0） |

---

## 十五、已知架构限制与改进方向

### 15.1 当前限制

| 问题 | 影响 | 严重度 |
|------|------|--------|
| SpellbookItem 单文件 4325 行 | 55+ 法术全在一个类中，维护困难 | 中 |
| 无法术注册表 | 法术通过枚举硬编码，无法动态扩展 | 中 |
| 粒子系统骨架化 | 仅 3 种自定义粒子，多数效果用原版粒子 | 高（影响视觉） |
| 自定义 AI Goal 仅 2 个 | 多数生物共享通用 AI，缺乏个性化行为 | 中 |
| 无合成配方 | 法术书/圣物无制作途径，只能交易/战利品 | 低（设计意图） |
| 无成就/进度系统 | 无玩家引导和进度追踪 | 低 |
| 证书经济无防刷 | 静态链路通过但无实机平衡验证 | 高（玩法） |
| Java ↔ BBModel 同步仅 WalkingTree | 其余 30 生物无自动同步工具 | 高（工作流） |

### 15.2 推荐改进（按优先级）

**P0: 设计流程基础设施**
1. 为每个文明生物建立 `sync_{creature}_bbmodel.py`（参考 `sync_walking_tree_bbmodel.py`）
2. 扩展 `blockbench_visual_mcp.py` 支持全 5 文明
3. 在模型审计脚本中加入 V4 硬性比例规则自动检查

**P1: 代码架构**
1. 将 SpellbookItem 拆分为 SpellRegistry + 独立 Spell 类
2. 为每种生物创建独立的 AI Goal（至少覆盖签名技能）
3. 实现自定义粒子渲染管线（替代原版粒子组合）

**P2: 玩法完善**
1. 添加证书防刷机制（冷却、上限、稀释）
2. 添加进度/成就系统引导玩家
3. 圣物神殿效果完整实现
