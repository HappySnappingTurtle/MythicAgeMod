# MythicAgeMod 架构改进设计文档

版本：1.0 | 日期：2026-06-16
前置文档：`MythicAgeMod_开发架构设计文档_2026-06-16.md`

本文档定义三项核心改进的详细设计：
1. **中国文明神力证明生成机制增强**（新增村民工作/交易触发）
2. **法术书系统重构**（SpellbookItem 拆分为 SpellRegistry + 独立 Spell 类）
3. **粒子系统升级**（自定义渲染管线 + 文明主题粒子）
4. **AI Goal 系统扩展**（生物签名技能专用 Goal）
5. **Java ↔ BBModel 同步工具通用化**

---

## 一、中国文明神力证明生成机制增强

### 1.1 设计背景

**当前状态：**
- `GodPowerCertificateManager.handleChinesePopulationGeneration()` 方法**已存在但从未被接入主循环**（孤立代码），公式为 `populationCount / 20` + 随机加成，1200 tick 间隔
- `CertificateGenerationManager.generateCertificatesForTemple()` 虽然支持中华类型，但其内部 `countCivilizedVillagersNearTemple()` 主要为希腊服务
- `MythicAgeMod.onInitialize()` 的 tick 事件中只调用了 `tickEgyptianStatues()`，**中华人口产出未接入**
- 实际中华证书唯一获取途径：通过守护者交易（12 纸→1 证书）

**关键 BUG：** `handleChinesePopulationGeneration()` 是孤立代码，需要修复接入。

**问题：** 中华文明的核心特征是"勤劳民众的集体繁荣"，单纯的人口计数不能体现"劳动创造神力"的文明内核。希腊用人口代表城邦公民、埃及用雕像代表法老神权、北欧用击杀代表战士荣耀——中国应该用**劳动与交易**来代表民间信仰。

**设计目标：** 中国证书通过三个渠道产生：

| 渠道 | 触发 | 设计依据 |
|------|------|----------|
| 人口基础 | 村民存在（保留现有） | 人丁兴旺 |
| 劳作产出 | 村民完成工作（耕种/合成/采集） | 辛勤劳作感动天地 |
| 商贸繁荣 | 玩家与村民完成交易 | 货通天下兴百业 |

### 1.2 详细机制设计

#### 1.2.1 人口基础产出（保留现有）

沿用 `CertificateGenerationManager.generateCertificatesForTemple()`，无需修改。

- 周期：2400 tick（2 分钟）
- 阶梯：1-2 人→1 证书 ... 20+ 人→15 证书
- 存储：最近的中国神像基座

#### 1.2.2 村民劳作产出（新增）

**触发条件：** 中华文明村民完成以下工作行为时，有概率产出证书。

| 工作行为 | Minecraft 内部行为 | 检测方式 | 基础概率 | 产出量 |
|----------|-------------------|----------|----------|--------|
| 农民收获 | 农田方块从 age=7 变为 age=0（村民 AI 收割） | 监听方块变化 + 附近村民检测 | 15% | 1 |
| 图书管理员工作 | 村民在讲台前工作（`WorkStationCompetitionTask`） | 村民 tick 时检测职业站点交互 | 10% | 1 |
| 武器匠/工具匠工作 | 村民在锻造台/砂轮前工作 | 同上 | 12% | 1 |
| 牧羊人剪毛 | 村民给羊剪毛（原版 AI） | 监听实体交互 | 20% | 1 |
| 渔夫钓鱼 | 村民在水边工作（桶式站点） | 村民 tick 检测 | 8% | 1 |

**冷却机制：**
- 每个中华村民的劳作产出有独立 1200 tick（1 分钟）冷却
- 通过 Mixin 在 VillagerEntity 上追加 `mythicAge$workCertificateCooldown` 字段
- 冷却结束前该村民的劳作不产出证书

**产出上限：**
- 每个中华神殿范围（32 格）内，劳作渠道每 2400 tick 最多产出 8 证书
- 防止大量村民集中劳作导致证书通胀

#### 1.2.3 玩家交易产出（新增）

**触发条件：** 玩家与位于中华神殿范围内的中华文明村民完成原版交易时，产出证书。

| 交易类型 | 概率 | 产出量 | 条件 |
|----------|------|--------|------|
| 普通交易（1 级） | 30% | 1 | 村民为中华文明 + 在神殿 32 格内 |
| 中级交易（2-3 级） | 45% | 1-2 | 同上 |
| 高级交易（4-5 级） | 60% | 2-3 | 同上 |
| 首次解锁新交易 | 100% | 3 | 村民升级时的首次新交易 |

**冷却机制：**
- 每个玩家与同一村民的交易产出冷却 600 tick（30 秒）
- 防止刷交易产出证书
- 冷却绑定 `playerUUID + villagerUUID` 对

**产出上限：**
- 每个玩家每 2400 tick 从交易渠道最多获得 10 证书
- 跨存档持久化（通过 MythicAgeWorldState）

### 1.3 实现方案

#### 1.3.1 新增类

```
com.example.mythic_age.system.ChineseCertificateManager
├── tickVillagerWork(ServerWorld)              — 每 tick 检查中华村民劳作
├── onVillagerTrade(PlayerEntity, VillagerEntity, TradeOffer) — 交易回调
├── canGenerateWorkCertificate(VillagerEntity) — 单村民冷却检查
├── canGenerateTradeCertificate(PlayerEntity, VillagerEntity) — 交易冷却检查
├── recordWorkGeneration(VillagerEntity, int)  — 记录劳作产出
├── recordTradeGeneration(PlayerEntity, VillagerEntity, int) — 记录交易产出
├── getTempleWorkCap(BlockPos)                 — 获取神殿范围内劳作上限
└── writePersistentState() / readPersistentState() — NBT 持久化
```

#### 1.3.2 Mixin 扩展

在 `VillagerAIEnhancementMixin` 中新增：

```java
// 新增字段
private int mythicAge$workCertificateCooldown = 0;

// 在 tick 注入中递减冷却
@Inject(method = "tick", at = @At("HEAD"))
private void onTick(CallbackInfo ci) {
    if (mythicAge$workCertificateCooldown > 0) {
        mythicAge$workCertificateCooldown--;
    }
    // ... 现有逻辑
}

// NBT 读写中增加冷却持久化
```

#### 1.3.3 交易事件监听

新增 Mixin 或事件监听拦截原版 `VillagerEntity.trade(TradeOffer)`:

```java
// 方案 A: 新增 Mixin（推荐，最精确）
@Mixin(VillagerEntity.class)
public class VillagerTradeMixin {
    @Inject(method = "trade", at = @At("TAIL"))
    private void afterTrade(TradeOffer offer, CallbackInfo ci) {
        VillagerEntity villager = (VillagerEntity)(Object)this;
        if (villager instanceof CivilizedVillager cv
            && cv.mythicAge$getCivilization() == MythologyType.CHINESE) {
            ChineseCertificateManager.getInstance()
                .onVillagerTrade(/* lastInteractingPlayer */, villager, offer);
        }
    }
}
```

#### 1.3.4 劳作检测

在 `ChineseCertificateManager.tickVillagerWork()` 中，每 100 tick 扫描一次中华神殿范围内的村民：

```java
public void tickVillagerWork(ServerWorld world) {
    if (world.getTime() % 100 != 0) return;

    for (BlockPos templePos : getChineseTemplePositions(world)) {
        List<VillagerEntity> workers = getChineseVillagersNearTemple(world, templePos);
        for (VillagerEntity villager : workers) {
            if (isWorking(villager) && canGenerateWorkCertificate(villager)) {
                float chance = getWorkChance(villager);
                if (world.random.nextFloat() < chance) {
                    generateWorkCertificate(world, templePos, villager, 1);
                }
            }
        }
    }
}

private boolean isWorking(VillagerEntity villager) {
    // 检查村民是否在职业站点附近且有职业
    return villager.getVillagerData().getProfession() != VillagerProfession.NONE
        && villager.getVillagerData().getProfession() != VillagerProfession.NITWIT
        && villager.getBrain().getOptionalRegisteredMemory(MemoryModuleType.JOB_SITE).isPresent();
}
```

#### 1.3.5 集成到主循环

**修复孤立代码 + 新增两渠道：**

在 `MythicAgeMod.onInitialize()` 的 ServerTickEvents 中添加：

```java
ServerTickEvents.START_WORLD_TICK.register(world -> {
    // 现有逻辑...
    MythicAgeWorldState.get(world.getServer()).tick(world);
    GodPowerCertificateManager.getInstance().tickEgyptianStatues(world);
    GodPowerTaskScheduler.tick(world);

    // [BUG FIX] 接入中华人口产出（原有方法从未被调用）
    GodPowerCertificateManager.getInstance().tickChinesePopulation(world);

    // [新增] 中华村民劳作产出
    ChineseCertificateManager.getInstance().tickVillagerWork(world);
});
```

需要在 `GodPowerCertificateManager` 中新增 `tickChinesePopulation(ServerWorld)` 方法，调用已有的 `handleChinesePopulationGeneration()`，遍历中华神殿位置执行人口检测。

### 1.4 数据流

```
中国文明证书产出三渠道：

渠道 1: 人口基础（保留）
  神殿范围内中华村民 → 人口阶梯表 → 神像基座

渠道 2: 村民劳作（新增）
  中华村民 tick → 检测职业站点交互 → 概率判定 → 冷却检查 → 上限检查 → 神像基座
  │
  └── 冷却: 每村民 1200 tick / 每神殿 2400 tick 内最多 8 个

渠道 3: 玩家交易（新增）
  玩家与中华村民交易 → Mixin 拦截 trade() → 等级概率判定 → 冷却检查 → 上限检查
  │                                                                    ↓
  └── 冷却: 同一对 600 tick / 每玩家 2400 tick 内最多 10 个       直接进入玩家背包
```

### 1.5 防刷设计

| 维度 | 限制 | 理由 |
|------|------|------|
| 单村民劳作冷却 | 1200 tick | 防止一个村民反复触发 |
| 神殿劳作上限 | 8/2400 tick | 防止大量村民农场 |
| 玩家交易冷却 | 600 tick/对 | 防止反复点击同一村民 |
| 玩家交易上限 | 10/2400 tick | 防止多村民轮流交易 |
| 跨重启持久化 | MythicAgeWorldState | 防止重启重置冷却 |

### 1.6 本地化文案

```json
{
  "mythic_age.certificate.chinese_work": "§6[天工开物] %s的辛勤劳作感动天地，产生了 %d 个中华神力证明",
  "mythic_age.certificate.chinese_trade": "§6[货通天下] 与%s的交易兴旺了百业，获得 %d 个中华神力证明",
  "mythic_age.certificate.chinese_first_trade": "§e[开市大吉] %s解锁了新的交易，天地嘉奖 3 个中华神力证明"
}
```

---

## 二、法术书系统重构

### 2.1 设计背景

**当前问题：** `SpellbookItem.java` 单文件 **4325 行**，包含 **68 个 SpellType 枚举值**（52 战斗法术 + 12 同名 + 4 神殿生成）、**54 个 castXxx() 方法**、一个 56 case 的巨型 switch 分发器 `castGodPower()`。ModItems.java 中注册了 **56 个法术书物品**。新增法术需要修改枚举、switch、cast 方法、tooltip、ModItems 共 5 处。

此外，88 处粒子调用中 **~90% 使用原版 ParticleTypes**，仅 9 处使用自定义 ModParticles（10%）。

**目标：** 拆分为注册表模式，每个法术独立类，支持按文明分包。

### 2.2 架构设计

```
item/
├── SpellbookItem.java              — 通用法术书物品（瘦身为 ~200 行）
│   ├── use() → SpellRegistry.get(spellType).cast(...)
│   ├── appendTooltip() → spell.getTooltip(...)
│   └── 冷却管理（保留现有逻辑）
│
├── spell/
│   ├── Spell.java                  — 法术接口
│   │   ├── getId(): Identifier
│   │   ├── getType(): SpellType
│   │   ├── getCivilization(): MythologyType
│   │   ├── getCategory(): SpellCategory
│   │   ├── cast(ServerWorld, PlayerEntity, BlockPos): void
│   │   ├── getTooltipLines(): List<Text>
│   │   └── getDefaultCooldown(): int
│   │
│   ├── AbstractSpell.java          — 基类（通用目标选择、粒子、音效）
│   │   ├── getTargetPos(PlayerEntity, int range): BlockPos
│   │   ├── spawnCastParticles(ServerWorld, BlockPos)
│   │   ├── playCastSound(ServerWorld, BlockPos)
│   │   └── sendMessage(PlayerEntity, String key, Object... args)
│   │
│   ├── SpellRegistry.java          — 注册表
│   │   ├── register(Spell): void
│   │   ├── get(SpellType): Spell
│   │   ├── getByCivilization(MythologyType): List<Spell>
│   │   └── getAll(): Collection<Spell>
│   │
│   ├── SpellType.java              — 枚举（从 SpellbookItem 移出）
│   ├── SpellCategory.java          — 枚举（offensive/healing/utility/...）
│   │
│   ├── greek/                      — 希腊法术包
│   │   ├── LightningStormSpell.java
│   │   ├── RestorationSpell.java
│   │   ├── EarthquakeSpell.java
│   │   ├── UnderworldArmySpell.java
│   │   └── ... (12 个)
│   │
│   ├── egyptian/                   — 埃及法术包
│   │   ├── RaRadianceSpell.java
│   │   ├── MeteorSpell.java
│   │   └── ... (10 个)
│   │
│   ├── nordic/                     — 北欧法术包
│   │   ├── RagnarokSpell.java
│   │   ├── WalkingWoodsSpell.java
│   │   └── ... (10 个)
│   │
│   ├── chinese/                    — 中华法术包
│   │   ├── FiveThunderSpell.java
│   │   ├── RecreationSpell.java
│   │   └── ... (10 个)
│   │
│   └── atlantean/                  — 亚特兰蒂斯法术包
│       ├── TidalSurgeSpell.java
│       ├── ChronoStasisSpell.java
│       └── ... (10 个)
```

### 2.3 核心接口

```java
public interface Spell {
    Identifier getId();
    SpellType getType();
    MythologyType getCivilization();
    SpellCategory getCategory();
    int getDefaultCooldown();
    Rarity getRarity();

    // 施放
    void cast(ServerWorld world, PlayerEntity caster, BlockPos targetPos);

    // UI
    List<Text> getTooltipLines(ItemStack stack);
    Text getDescription();
}
```

```java
public abstract class AbstractSpell implements Spell {
    protected final SpellType type;
    protected final MythologyType civilization;
    protected final SpellCategory category;
    protected final int cooldown;

    protected BlockPos getTargetPos(PlayerEntity caster, int range) {
        // 从 SpellbookItem 移入的射线检测逻辑
    }

    protected void sendMessage(PlayerEntity caster, String key, Object... args) {
        // 通用消息发送
    }

    protected void scheduleTask(ServerWorld world, int delay, Runnable task) {
        GodPowerTaskScheduler.schedule(world, delay, task);
    }
}
```

### 2.4 注册表模式

```java
public class SpellRegistry {
    private static final Map<SpellType, Spell> SPELLS = new LinkedHashMap<>();

    public static void register(Spell spell) {
        SPELLS.put(spell.getType(), spell);
    }

    public static Spell get(SpellType type) {
        return SPELLS.get(type);
    }

    public static void init() {
        // Greek
        register(new LightningStormSpell());
        register(new RestorationSpell());
        // ... 55+ 法术
    }
}
```

### 2.5 SpellbookItem 瘦身后

```java
public class SpellbookItem extends Item {
    private final SpellType spellType;
    private final int cooldownTicks;

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (world instanceof ServerWorld serverWorld) {
            Spell spell = SpellRegistry.get(this.spellType);
            BlockPos target = spell instanceof AbstractSpell abs
                ? abs.getTargetPos(user, 50) : user.getBlockPos();
            spell.cast(serverWorld, user, target);
            user.getItemCooldownManager().set(this, cooldownTicks);
        }
        return TypedActionResult.success(user.getStackInHand(hand));
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext ctx) {
        Spell spell = SpellRegistry.get(this.spellType);
        tooltip.addAll(spell.getTooltipLines(stack));
    }
}
```

### 2.6 迁移策略

1. 创建 `Spell` 接口和 `AbstractSpell` 基类
2. 创建 `SpellRegistry`
3. **逐个**将现有 `castXxx()` 方法提取为独立 Spell 类（每次提取后 build 验证）
4. 原 `SpellbookItem` 中的 switch 逐步缩小
5. 最终 `SpellbookItem` 仅保留物品行为，不含法术逻辑

---

## 三、粒子系统升级

### 3.1 设计背景

**当前状态：** 仅 3 种自定义粒子（MYTHIC_FLAME, DIVINE_SPARKLE, SHOCKWAVE），多数法术使用原版 ParticleTypes（EXPLOSION, SWEEP_ATTACK, CLOUD 等），无文明主题差异。

**目标：** 每个文明有统一的粒子视觉语言，每个法术有三段式粒子（cast/active/end）。

### 3.2 文明粒子主题

| 文明 | 主色调 | 粒子风格 | 代表元素 |
|------|--------|----------|----------|
| Greek | 金白 + 天蓝 | 神圣光柱、雷电弧线 | 闪电、光环 |
| Egyptian | 金沙 + 暗红 | 沙尘漩涡、太阳光线 | 沙粒、圣甲虫轨迹 |
| Nordic | 冰蓝 + 暗灰 | 冰晶碎片、符文轨迹 | 冰霜、北极光 |
| Chinese | 朱红 + 金黄 | 祥云、龙纹轨迹 | 云雾、火焰、符箓 |
| Atlantean | 青蓝 + 水晶白 | 水泡、水流轨迹 | 水波、棱镜折射 |

### 3.3 新增粒子类型

```java
// ModParticles.java 新增
public static final DefaultParticleType GREEK_DIVINE_LIGHT;     // 希腊神圣光
public static final DefaultParticleType GREEK_LIGHTNING_SPARK;  // 希腊雷电火花
public static final DefaultParticleType EGYPTIAN_SAND_SWIRL;    // 埃及沙尘
public static final DefaultParticleType EGYPTIAN_SUN_RAY;       // 埃及太阳光线
public static final DefaultParticleType NORDIC_ICE_SHARD;       // 北欧冰晶
public static final DefaultParticleType NORDIC_RUNE_GLOW;       // 北欧符文光
public static final DefaultParticleType CHINESE_CLOUD_WISP;     // 中华祥云
public static final DefaultParticleType CHINESE_DRAGON_TRAIL;   // 中华龙纹
public static final DefaultParticleType ATLANTEAN_BUBBLE;       // 亚特兰蒂斯水泡
public static final DefaultParticleType ATLANTEAN_PRISM;        // 亚特兰蒂斯棱镜
```

### 3.4 三段式粒子接口

```java
public interface SpellParticleEffect {
    // 施法起手（0.5 秒内）
    void playCast(ServerWorld world, BlockPos casterPos, BlockPos targetPos);

    // 效果持续期间
    void playActive(ServerWorld world, BlockPos targetPos, int ticksElapsed);

    // 效果结束
    void playEnd(ServerWorld world, BlockPos targetPos);
}
```

每个 Spell 类持有对应的 `SpellParticleEffect` 实现。

### 3.5 性能保护

- 沿用 `ModConfig.PARTICLE_MAX_RENDER_DISTANCE`（64 格）
- 沿用 `ModConfig.PARTICLE_FAR_DISTANCE_MULTIPLIER`（0.35）
- 沿用 `ModConfig.MAX_PARTICLES_PER_BURST`（160）
- 新增：`ModConfig.PARTICLE_LOD_LEVELS`（3 级距离衰减）

---

## 四、AI Goal 系统扩展

### 4.1 设计背景

**当前状态：** 仅 2 个自定义 AI Goal（MinotaurChargeAttackGoal、VillagerPilgrimageGoal），且 VillagerPilgrimageGoal **从未被注册使用**。其余 28 个生物共享通用 MeleeAttackGoal，所有签名技能（毒液横扫、石化凝视、投石、龙息等）都硬编码在 `tick()` 方法中，没有使用 Goal 系统。

**具体现状：**
- Hydra: `tryPoisonSweep()` 在 tick() 中，120 tick 冷却
- Medusa: `tryPetrifyingGaze()` 在 tick() 中，130 tick 冷却
- Cyclops: `tryStoneThrow()` 在 tick() 中，140 tick 冷却
- Fenrir Wolf: `tryDreadHowl()` 在 tick() 中，120 tick 冷却
- Azure Dragon: `tryDragonBreath()` 在 tick() 中，95 tick 冷却（**唯一无 MeleeAttackGoal 的生物**）
- **22+ 个生物** 的特殊技能都在 tick() 中硬编码

**目标：** 为每个文明的标志性生物创建专用 AI Goal，优先覆盖有签名技能的生物。将 tick() 中的硬编码技能迁移到 Goal 系统，使 AI 行为可组合、可复用。

### 4.2 优先级清单（P0: 设计已有签名技能的生物）

| 生物 | 签名技能 | 新增 Goal | 优先级 |
|------|----------|----------|--------|
| Hydra | 毒液横扫 | `HydraPoisonSweepGoal` | P0 |
| Medusa | 石化凝视 | `MedusaStoneGazeGoal` | P0 |
| Cyclops | 投石 | `CyclopsStoneThrowGoal` | P0 |
| Pegasus | 飞行闪避 | `PegasusFlightEvadeGoal` | P0 |
| Chimera | 三头轮替攻击 | `ChimeraMultiHeadAttackGoal` | P1 |
| Centaur | 骑射 | `CentaurMountedArcheryGoal` | P1 |
| Nemean Lion | 猛扑 | `NemeanLionPounceGoal` | P1 |
| Fenrir Wolf | 嚎叫增益 | `FenrirHowlBuffGoal` | P1 |
| Azure Dragon | 龙息 | `AzureDragonBreathGoal` | P1 |
| Crystal Leviathan | 水晶尖刺 | `LeviathanCrystalSpikeGoal` | P2 |

### 4.3 Goal 基类

```java
public abstract class MythicSpecialAttackGoal extends Goal {
    protected final HostileEntity mob;
    protected final int cooldownTicks;
    protected final float range;
    protected int cooldownRemaining;

    @Override
    public boolean canStart() {
        LivingEntity target = mob.getTarget();
        return target != null
            && target.isAlive()
            && cooldownRemaining <= 0
            && mob.squaredDistanceTo(target) <= range * range;
    }

    @Override
    public void stop() {
        cooldownRemaining = cooldownTicks;
    }

    protected abstract void performAttack(LivingEntity target);
}
```

### 4.4 示例：HydraPoisonSweepGoal

```java
public class HydraPoisonSweepGoal extends MythicSpecialAttackGoal {
    public HydraPoisonSweepGoal(HydraEntity hydra) {
        super(hydra, 120, 5.5F);
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    protected void performAttack(LivingEntity target) {
        // AoE 伤害 + Poison
        Box aoe = mob.getBoundingBox().expand(4.2, 1.0, 4.2);
        for (LivingEntity victim : mob.getWorld().getEntitiesByClass(
                LivingEntity.class, aoe, e -> e != mob)) {
            victim.damage(mob.getDamageSources().mobAttack(mob), 5.0F);
            victim.addStatusEffect(new StatusEffectInstance(
                StatusEffects.POISON, 100, 0));
        }
        // 粒子 + 音效
        ((ServerWorld) mob.getWorld()).spawnParticles(
            ParticleTypes.SNEEZE, mob.getX(), mob.getY() + 1, mob.getZ(),
            32, 2.0, 0.5, 2.0, 0.02);
    }
}
```

---

## 五、Java ↔ BBModel 同步工具通用化

### 5.1 设计背景

**当前状态：** 仅 `sync_walking_tree_bbmodel.py` 支持 WalkingTree 的 Java→BBModel 同步。其余 30 个生物无自动同步。

**目标：** 通用同步工具，解析任意 Model.java 的 `getTexturedModelData()` 并生成/更新对应 `.bbmodel`。

### 5.2 工具设计

```
tools/sync_creature_bbmodel.py
│
├── 子命令
│   ├── sync <creature_id>            — 同步单个生物
│   ├── sync-all                      — 同步全部
│   ├── diff <creature_id>            — 对比差异（不修改）
│   └── audit                         — 检查所有 Java↔BBModel 一致性
│
├── 核心逻辑
│   ├── parse_java_model(path)        — 解析 getTexturedModelData() 中的
│   │   │                               addChild/cuboid/pivot/uv 调用
│   │   └── 返回: ModelTree（层级、尺寸、pivot、UV 映射）
│   │
│   ├── parse_bbmodel(path)           — 解析 .bbmodel JSON
│   │   └── 返回: BBModelTree（outliner、elements、UV）
│   │
│   ├── diff_trees(java, bbmodel)     — 对比两棵树
│   │   └── 返回: 新增/删除/尺寸变化/pivot 变化/UV 变化列表
│   │
│   └── write_bbmodel(java_tree, path) — 从 Java 树重建 bbmodel
│       └── 保留 UUID 稳定性（已有 element 保留 UUID）
│
├── 配置
│   └── creature_model_map.json       — creature_id → Java Model 路径映射
│
└── 输出
    ├── docs/美术资源/blockbench/{civ}_creatures/{creature_id}.bbmodel
    └── 同步报告 JSON
```

### 5.3 creature_model_map.json 示例

```json
{
  "minotaur": {
    "java_model": "src/main/java/.../model/MinotaurModel.java",
    "bbmodel": "docs/美术资源/blockbench/greek_creatures/minotaur.bbmodel",
    "texture_size": [128, 128]
  },
  "hydra": {
    "java_model": "src/main/java/.../model/HydraModel.java",
    "bbmodel": "docs/美术资源/blockbench/greek_creatures/hydra.bbmodel",
    "texture_size": [128, 128]
  }
  // ... 30 个生物
}
```

### 5.4 扩展 BlockBench MCP

在 `blockbench_visual_mcp.py` 中新增工具：

```python
TOOLS.append({
    "name": "sync_java_bbmodel",
    "description": "Sync a creature's Java model to its BBModel file",
    "inputSchema": {
        "type": "object",
        "properties": {
            "creature_id": {"type": "string"},
            "dry_run": {"type": "boolean", "default": True}
        },
        "required": ["creature_id"]
    }
})

TOOLS.append({
    "name": "audit_java_bbmodel_consistency",
    "description": "Check Java ↔ BBModel consistency for all creatures",
    "inputSchema": {"type": "object", "properties": {}}
})
```

---

## 六、实施优先级与依赖

```
阶段 1（可立即开始，无依赖）:
├── 中国证书增强: ChineseCertificateManager + Mixin 扩展 + 交易监听
├── Java↔BBModel 同步工具: sync_creature_bbmodel.py + creature_model_map.json
└── AI Goal 基类: MythicSpecialAttackGoal

阶段 2（阶段 1 完成后）:
├── 法术书重构: Spell 接口 + SpellRegistry + 逐个迁移（可分多次提交）
├── P0 AI Goal: Hydra/Medusa/Cyclops/Pegasus 4 个签名技能 Goal
└── 粒子系统: 10 种文明粒子类型注册 + 客户端渲染

阶段 3（阶段 2 完成后）:
├── 法术三段式粒子: 每个 Spell 绑定 SpellParticleEffect
├── P1 AI Goal: Chimera/Centaur/NemeanLion/Fenrir/AzureDragon
└── BlockBench MCP 扩展: sync 和 audit 工具集成
```

---

## 七、验收标准

| 改进项 | 验收标准 |
|--------|----------|
| 中国证书增强 | 村民工作时可观察到证书产出消息；玩家交易后背包获得证书；冷却和上限正常工作；跨重启持久化 |
| 法术书重构 | 55+ 法术全部迁移到独立类；`SpellbookItem.java` < 300 行；`./gradlew build` 通过；所有法术效果不变 |
| 粒子系统 | 10 种新粒子可在游戏中看到；每个文明的法术有视觉差异；性能参数生效 |
| AI Goal | P0 4 个生物有可观察的签名技能战斗行为；冷却正常；不影响现有 AI 优先级 |
| 同步工具 | `python3 tools/sync_creature_bbmodel.py audit` 报告 30 生物一致性；sync 后 BBModel 可在 Blockbench 中正常打开 |
