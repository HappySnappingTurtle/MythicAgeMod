# 神话时代Mod - 技术实现详解

## 1. 核心系统架构

### 1.1 模组初始化流程

#### 主模组类 (MythicAgeMod.java)
```java
public class MythicAgeMod implements ModInitializer {
    public static final String MOD_ID = "mythic_age";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    @Override
    public void onInitialize() {
        // 1. 注册物品
        ModItems.registerModItems();
        
        // 2. 注册方块
        ModBlocks.registerModBlocks();
        
        // 3. 注册实体
        ModEntities.registerModEntities();
        
        // 4. 注册世界生成
        MythicVillageTypes.registerVillageTypes();
        
        // 5. 注册事件监听器
        registerEventListeners();
    }
}
```

**技术要点：**
- 使用Fabric的ModInitializer接口
- 按依赖顺序进行注册
- 统一的日志管理

#### 客户端初始化 (MythicAgeModClient.java)
```java
public class MythicAgeModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // 注册实体渲染器
        EntityRendererRegistry.register(ModEntities.TEMPLE_GUARDIAN, 
            TempleGuardianEntityRenderer::new);
        
        // 注册方块渲染器
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.STATUE_BASE, 
            RenderLayer.getCutout());
    }
}
```

**技术要点：**
- 客户端和服务端分离
- 渲染器注册机制
- 渲染层管理

### 1.2 注册系统详解

#### 物品注册系统
```java
public class ModItems {
    public static final Item GOD_POWER_CERTIFICATE_GREEK = Registry.register(
        Registry.ITEM,
        new Identifier(MythicAgeMod.MOD_ID, "god_power_certificate_greek"),
        new GodPowerCertificateItem(new FabricItemSettings())
    );
    
    public static void registerModItems() {
        // 使用Registry.register进行注册
        // 每个物品都有唯一的Identifier
    }
}
```

**底层原理：**
- Minecraft使用Registry系统管理所有游戏对象
- Identifier提供命名空间隔离
- FabricItemSettings提供物品配置

## 2. 实体系统深度解析

### 2.1 神殿守护者实体架构

#### 继承层次结构
```
Entity (Minecraft基础实体)
├── MobEntity (生物实体)
    ├── PathAwareEntity (路径感知实体)
        ├── VillagerEntity (村民实体)
            └── TempleGuardianEntity (神殿守护者)
```

#### 核心属性管理
```java
public class TempleGuardianEntity extends VillagerEntity {
    // 1. 神话类型 - 决定行为和外观
    private final GodPowerCertificateItem.MythologyType mythologyType;
    
    // 2. 神殿位置 - 用于AI行为
    private BlockPos templePos;
    
    // 3. 生成计时器 - 控制神力证明生成
    private int certificateGenerationTimer = 0;
    
    // 4. 文明标识 - 用于状态管理
    private String civilization = "greek";
}
```

#### AI行为系统
```java
@Override
protected void initGoals() {
    // 优先级系统：数字越小优先级越高
    this.goalSelector.add(0, new SwimGoal(this));           // 游泳
    this.goalSelector.add(1, new EscapeDangerGoal(this, 1.5D)); // 逃跑
    this.goalSelector.add(2, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F)); // 看向玩家
    this.goalSelector.add(3, new WanderAroundPointOfInterestGoal(this, 0.6F, false)); // 漫游
    this.goalSelector.add(4, new WanderAroundGoal(this, 0.6F)); // 随机移动
    this.goalSelector.add(5, new LookAroundGoal(this));     // 环顾四周
}
```

**技术要点：**
- 使用Goal系统实现AI行为
- 优先级决定行为选择
- 可组合的行为模式

### 2.2 神力证明生成机制

#### 计时器系统
```java
@Override
public void tick() {
    super.tick();
    
    if (!this.getWorld().isClient && this.isAlive()) {
        // 每tick增加计时器
        certificateGenerationTimer++;
        
        // 检查是否达到生成间隔
        if (certificateGenerationTimer >= CERTIFICATE_GENERATION_INTERVAL) {
            generateCertificatesForNearbyVillagers();
            certificateGenerationTimer = 0; // 重置计时器
        }
    }
}
```

**技术要点：**
- 使用tick()方法实现周期性任务
- 服务端和客户端分离
- 计时器重置机制

#### 实体查询系统
```java
private void generateCertificatesForNearbyVillagers() {
    ServerWorld serverWorld = (ServerWorld) this.getWorld();
    
    // 使用AABB查询附近实体
    serverWorld.getEntitiesByClass(
        VillagerEntity.class, 
        this.getBoundingBox().expand(TEMPLE_RANGE),
        villager -> villager != this && villager.isAlive()
    ).forEach(villager -> {
        // 处理每个村民
        if (serverWorld.random.nextFloat() < 0.3f) {
            // 30%概率生成神力证明
            ItemStack certificate = createCertificate();
            villager.getInventory().addStack(certificate);
        }
    });
}
```

**技术要点：**
- 使用AABB (Axis-Aligned Bounding Box) 进行空间查询
- 实体过滤条件
- 随机数控制生成概率

## 3. 物品系统技术实现

### 3.1 神力证明物品

#### NBT数据管理
```java
public class GodPowerCertificateItem extends Item {
    @Override
    public void appendTooltip(ItemStack stack, World world, 
                             List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        
        // 从NBT读取神话类型
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains("mythology_type")) {
            String typeName = nbt.getString("mythology_type");
            MythologyType type = MythologyType.valueOf(typeName.toUpperCase());
            
            // 添加文明信息到工具提示
            tooltip.add(Text.literal("文明: " + type.getChineseName())
                .formatted(type.getColor()));
        }
    }
}
```

**技术要点：**
- NBT (Named Binary Tag) 存储物品数据
- 工具提示动态生成
- 类型安全的数据访问

#### 使用事件处理
```java
@Override
public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
    ItemStack itemStack = user.getStackInHand(hand);
    
    if (!world.isClient) {
        // 服务端逻辑
        showCertificateInfo(user, itemStack);
        
        // 播放使用音效
        world.playSound(null, user.getX(), user.getY(), user.getZ(), 
            SoundEvents.ENTITY_VILLAGER_YES, SoundCategory.PLAYERS, 1.0F, 1.0F);
    }
    
    return TypedActionResult.success(itemStack);
}
```

**技术要点：**
- TypedActionResult处理物品使用结果
- 客户端和服务端逻辑分离
- 音效和粒子效果

### 3.2 神力指南系统

#### 动态书籍生成
```java
private ItemStack createGuideBook(String civilization) {
    // 创建原版书物品
    ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
    NbtCompound nbt = book.getOrCreateNbt();
    
    // 设置书籍元数据
    nbt.putString("title", getBookTitle(civilization));
    nbt.putString("author", getBookAuthor(civilization));
    nbt.putInt("generation", 0);
    nbt.putBoolean("resolved", true);
    
    // 生成页面内容
    NbtList pages = new NbtList();
    addCivilizationPages(pages, civilization);
    nbt.put("pages", pages);
    
    return book;
}
```

**技术要点：**
- 使用原版WrittenBookItem
- JSON格式的富文本
- NBTList管理多页面

#### JSON文本格式
```java
private void addGreekPages(NbtList pages) {
    // 使用JSON格式定义富文本
    String page1 = """
    {"text":"§b§l希腊神力指南\\n\\n","extra":[
    {"text":"§e欢迎来到希腊神殿！\\n\\n","color":"yellow"},
    {"text":"§f作为智慧与力量的象征，希腊神明将赐予您强大的神力。\\n\\n","color":"white"},
    {"text":"§a主要内容：\\n","color":"green"},
    {"text":"1. 神力证明获取\\n","color":"white"},
    {"text":"2. 法术书兑换\\n","color":"white"},
    {"text":"3. 神殿守护者服务\\n","color":"white"},
    {"text":"4. 神像祭祀\\n","color":"white"}
    ]}""";
    
    pages.add(NbtString.of(page1));
}
```

**技术要点：**
- JSON格式支持颜色和格式化
- 转义字符处理
- 多语言支持

## 4. 交易系统深度实现

### 4.1 交易Offer系统

#### 基础交易创建
```java
private void initializeTrades() {
    TradeOfferList offers = new TradeOfferList();
    
    // 创建基础交易
    ItemStack certificate = createCertificate();
    TradeOffer basicTrade = new TradeOffer(
        new ItemStack(Items.EMERALD, 5),    // 输入物品
        certificate,                         // 输出物品
        10,                                  // 最大使用次数
        2,                                   // 村民经验
        0.05f                                // 价格倍数
    );
    
    offers.add(basicTrade);
    this.setOffers(offers);
}
```

**技术要点：**
- TradeOffer包含输入、输出、限制条件
- 动态价格调整机制
- 经验值系统

#### 文明特定交易
```java
private void addCivilizationSpecificTrades(TradeOfferList offers) {
    ItemStack certificate = createCertificate();
    
    switch (mythologyType) {
        case GREEK -> {
            // 希腊魔法书交易
            offers.add(new TradeOffer(
                certificate.copyWithCount(2),  // 2个希腊神力证明
                new ItemStack(ModItems.SPELLBOOK_RESTORATION),
                3, 10, 0.1f
            ));
            
            // 高级法术书（限量）
            if (Math.random() < 0.3) {
                offers.add(new TradeOffer(
                    certificate.copyWithCount(5),
                    new ItemStack(ModItems.SPELLBOOK_EARTHQUAKE),
                    1, 30, 0.3f
                ));
            }
        }
        // ... 其他文明
    }
}
```

**技术要点：**
- 条件性交易生成
- 随机性交易内容
- 文明特色平衡

### 4.2 交易界面管理

#### 界面打开逻辑
```java
private ActionResult openTradingInterface(PlayerEntity player) {
    // 确保交易列表已初始化
    if (this.getOffers().isEmpty()) {
        initializeTrades();
    }
    
    // 设置交易状态
    this.setCustomer(player);
    this.setVillagerData(this.getVillagerData().withProfession(VillagerProfession.NONE));
    
    // 发送交易数据到客户端
    if (player instanceof ServerPlayerEntity serverPlayer) {
        this.sendOffers(serverPlayer, this.getDisplayName(), 1);
        return ActionResult.CONSUME;
    }
    
    return ActionResult.SUCCESS;
}
```

**技术要点：**
- 客户端和服务端同步
- 交易状态管理
- 界面数据传递

## 5. 多方块结构系统

### 5.1 结构检测算法

#### 垂直结构验证
```java
public static boolean isCompleteStructure(World world, BlockPos pos) {
    // 定义4层垂直结构
    BlockPos[] structurePositions = {
        pos,                    // 基座 (0层)
        pos.up(),              // 腿部 (1层)
        pos.up(2),             // 身体 (2层)
        pos.up(3)              // 头部 (3层)
    };
    
    // 验证每层方块
    for (int i = 0; i < structurePositions.length; i++) {
        BlockPos checkPos = structurePositions[i];
        BlockState state = world.getBlockState(checkPos);
        
        if (!isValidStatueBlock(state, i)) {
            return false;
        }
    }
    
    return true;
}
```

**技术要点：**
- 相对位置计算
- 方块状态验证
- 层次结构管理

#### 方块类型验证
```java
private static boolean isValidStatueBlock(BlockState state, int layer) {
    Block block = state.getBlock();
    
    return switch (layer) {
        case 0 -> block instanceof StatueBaseBlock;           // 基座
        case 1 -> block instanceof StatueLegsBlock;           // 腿部
        case 2 -> block instanceof StatueBodyBlock;           // 身体
        case 3 -> block instanceof StatueHeadBlock;           // 头部
        default -> false;
    };
}
```

**技术要点：**
- 类型安全检查
- 层次化验证
- 扩展性设计

### 5.2 自动储存系统

#### 周期性检查机制
```java
@Override
public void tick() {
    super.tick();
    
    if (!this.getWorld().isClient) {
        // 每200 ticks (10秒) 检查一次
        if (this.getWorld().getTime() % 200 == 0) {
            collectNearbyCertificates();
        }
    }
}
```

#### 物品收集算法
```java
private void collectNearbyCertificates() {
    // 查询附近2格范围内的物品实体
    List<ItemEntity> nearbyItems = this.getWorld().getEntitiesByClass(
        ItemEntity.class, 
        this.getBoundingBox().expand(2.0),
        item -> {
            ItemStack stack = item.getStack();
            return stack.getItem() instanceof GodPowerCertificateItem 
                && this.storedCertificates.size() < MAX_STORAGE;
        }
    );
    
    // 收集符合条件的物品
    for (ItemEntity itemEntity : nearbyItems) {
        ItemStack stack = itemEntity.getStack().copy();
        this.storedCertificates.add(stack);
        itemEntity.discard(); // 移除物品实体
        
        // 播放收集音效
        this.getWorld().playSound(null, this.getPos(), 
            SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 0.5F, 1.0F);
    }
}
```

**技术要点：**
- 空间查询优化
- 容量限制管理
- 音效反馈

## 6. 魔法书系统实现

### 6.1 法术效果管理器

#### 闪电风暴实现
```java
public class LightningStormManager {
    public static void castLightningStorm(World world, BlockPos center, int radius) {
        // 生成多个闪电点
        for (int i = 0; i < 10; i++) {
            // 随机选择目标位置
            BlockPos targetPos = center.add(
                world.random.nextInt(radius * 2) - radius,
                0,
                world.random.nextInt(radius * 2) - radius
            );
            
            // 创建闪电实体
            LightningEntity lightning = EntityType.LIGHTNING_BOLT.create(world);
            lightning.setPosition(targetPos.getX(), targetPos.getY(), targetPos.getZ());
            world.spawnEntity(lightning);
            
            // 添加粒子效果
            spawnLightningParticles(world, targetPos);
        }
    }
    
    private static void spawnLightningParticles(World world, BlockPos pos) {
        // 创建闪电粒子效果
        for (int i = 0; i < 20; i++) {
            double x = pos.getX() + world.random.nextDouble();
            double y = pos.getY() + world.random.nextDouble() * 10;
            double z = pos.getZ() + world.random.nextDouble();
            
            world.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0, 0, 0);
        }
    }
}
```

**技术要点：**
- 随机位置生成
- 实体创建和管理
- 粒子效果系统

### 6.2 地震法术实现
```java
public class EarthquakeManager {
    public static void castEarthquake(World world, BlockPos center, int radius) {
        // 创建地震波
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double distance = Math.sqrt(x * x + z * z);
                if (distance <= radius) {
                    BlockPos targetPos = center.add(x, 0, z);
                    
                    // 根据距离计算破坏强度
                    float intensity = 1.0f - (float)(distance / radius);
                    
                    // 破坏方块
                    if (world.random.nextFloat() < intensity * 0.3f) {
                        BlockState state = world.getBlockState(targetPos);
                        if (state.getHardness(world, targetPos) < 3.0f) {
                            world.breakBlock(targetPos, true);
                        }
                    }
                    
                    // 添加震动效果
                    addShakeEffect(world, targetPos, intensity);
                }
            }
        }
    }
}
```

**技术要点：**
- 距离计算算法
- 强度衰减机制
- 方块破坏逻辑

## 7. 世界生成系统

### 7.1 村庄类型注册

#### 注册机制
```java
public class MythicVillageTypes {
    public static void registerVillageTypes() {
        // 注册希腊村庄
        Registry.register(Registry.VILLAGE_TYPE, 
            new Identifier(MythicAgeMod.MOD_ID, "greek"), 
            new VillageType("greek"));
        
        // 注册埃及村庄
        Registry.register(Registry.VILLAGE_TYPE, 
            new Identifier(MythicAgeMod.MOD_ID, "egyptian"), 
            new VillageType("egyptian"));
        
        // ... 其他文明
    }
}
```

**技术要点：**
- Registry系统使用
- 命名空间管理
- 类型安全注册

### 7.2 结构池配置

#### JSON配置文件
```json
{
  "type": "minecraft:jigsaw",
  "start_pool": "mythic_age:village/greek/town_centers",
  "size": 6,
  "max_distance_from_center": 80,
  "biomes": "#minecraft:has_structure/village_plains",
  "step": "surface_structures",
  "terrain_adaptation": "beard_thin",
  "start_height": {
    "absolute": 0
  },
  "project_start_to_heightmap": "WORLD_SURFACE_WG",
  "spawn_overrides": {
    "creature": {
      "bounding_box": "piece",
      "spawns": []
    }
  }
}
```

**技术要点：**
- Jigsaw结构系统
- 生物群系关联
- 地形适应算法

## 8. 客户端渲染系统

### 8.1 实体渲染器

#### 动态纹理系统
```java
public class TempleGuardianEntityRenderer extends VillagerEntityRenderer {
    @Override
    public Identifier getTexture(VillagerEntity villagerEntity) {
        if (villagerEntity instanceof TempleGuardianEntity guardian) {
            GodPowerCertificateItem.MythologyType mythologyType = guardian.getMythologyType();
            
            // 根据文明类型返回对应纹理
            return switch (mythologyType) {
                case GREEK -> new Identifier(MythicAgeMod.MOD_ID, 
                    "textures/entity/temple_guardian_greek.png");
                case EGYPTIAN -> new Identifier(MythicAgeMod.MOD_ID, 
                    "textures/entity/temple_guardian_egyptian.png");
                case CHINESE -> new Identifier(MythicAgeMod.MOD_ID, 
                    "textures/entity/temple_guardian_chinese.png");
                case ATLANTEAN -> new Identifier(MythicAgeMod.MOD_ID, 
                    "textures/entity/temple_guardian_atlantean.png");
                case NORDIC -> new Identifier(MythicAgeMod.MOD_ID, 
                    "textures/entity/temple_guardian_nordic.png");
            };
        }
        
        // 回退到默认村民纹理
        return super.getTexture(villagerEntity);
    }
}
```

**技术要点：**
- 继承渲染器系统
- 动态纹理选择
- 回退机制

### 8.2 方块渲染器

#### 渲染层管理
```java
public class ModBlocks {
    public static void registerBlockRenderers() {
        // 设置渲染层
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.STATUE_BASE, 
            RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.STATUE_HEAD, 
            RenderLayer.getCutout());
        
        // 注册方块实体渲染器
        BlockEntityRendererRegistry.register(ModBlockEntities.STATUE_BASE, 
            StatueBaseBlockEntityRenderer::new);
    }
}
```

**技术要点：**
- 渲染层分类
- 透明度处理
- 方块实体渲染

## 9. 数据持久化系统

### 9.1 NBT数据管理

#### 实体数据保存
```java
@Override
public void writeCustomDataToNbt(NbtCompound nbt) {
    super.writeCustomDataToNbt(nbt);
    
    // 保存神话类型
    nbt.putString("mythology_type", this.mythologyType.name());
    
    // 保存神殿位置
    if (this.templePos != null) {
        nbt.putLong("temple_pos", this.templePos.asLong());
    }
    
    // 保存计时器
    nbt.putInt("certificate_timer", this.certificateGenerationTimer);
    
    // 保存文明标识
    nbt.putString("civilization", this.civilization);
}

@Override
public void readCustomDataFromNbt(NbtCompound nbt) {
    super.readCustomDataFromNbt(nbt);
    
    // 读取神话类型
    if (nbt.contains("mythology_type")) {
        this.mythologyType = GodPowerCertificateItem.MythologyType.valueOf(
            nbt.getString("mythology_type"));
    }
    
    // 读取神殿位置
    if (nbt.contains("temple_pos")) {
        this.templePos = BlockPos.fromLong(nbt.getLong("temple_pos"));
    }
    
    // 读取计时器
    this.certificateGenerationTimer = nbt.getInt("certificate_timer");
    
    // 读取文明标识
    this.civilization = nbt.getString("civilization");
}
```

**技术要点：**
- NBT序列化机制
- 数据版本兼容
- 类型安全转换

### 9.2 方块实体数据

#### 神力证明存储
```java
public class StatueBaseBlockEntity extends BlockEntity {
    private final List<ItemStack> storedCertificates = new ArrayList<>();
    private static final int MAX_STORAGE = 64;
    
    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        
        // 保存神力证明列表
        NbtList certificateList = new NbtList();
        for (ItemStack stack : this.storedCertificates) {
            NbtCompound itemNbt = new NbtCompound();
            stack.writeNbt(itemNbt);
            certificateList.add(itemNbt);
        }
        nbt.put("stored_certificates", certificateList);
    }
    
    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        
        // 读取神力证明列表
        this.storedCertificates.clear();
        NbtList certificateList = nbt.getList("stored_certificates", NbtElement.COMPOUND_TYPE);
        for (NbtElement element : certificateList) {
            NbtCompound itemNbt = (NbtCompound) element;
            ItemStack stack = ItemStack.fromNbt(itemNbt);
            this.storedCertificates.add(stack);
        }
    }
}
```

**技术要点：**
- 列表序列化
- 容量管理
- 数据完整性

## 10. 性能优化技术

### 10.1 缓存机制

#### 纹理缓存
```java
public class TextureCache {
    private static final Map<Identifier, Identifier> textureCache = new HashMap<>();
    
    public static Identifier getTexture(Identifier baseTexture, MythologyType type) {
        String key = baseTexture.toString() + "_" + type.name().toLowerCase();
        return textureCache.computeIfAbsent(new Identifier(key), k -> {
            // 动态生成纹理路径
            return new Identifier(MythicAgeMod.MOD_ID, 
                "textures/entity/temple_guardian_" + type.name().toLowerCase() + ".png");
        });
    }
}
```

### 10.2 延迟加载

#### 交易系统延迟初始化
```java
public class TempleGuardianEntity extends VillagerEntity {
    private boolean tradesInitialized = false;
    
    private void ensureTradesInitialized() {
        if (!tradesInitialized) {
            initializeTrades();
            tradesInitialized = true;
        }
    }
    
    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ensureTradesInitialized();
        // ... 交互逻辑
    }
}
```

### 10.3 内存管理

#### 对象池模式
```java
public class ParticlePool {
    private static final Queue<ParticleEffect> particlePool = new ConcurrentLinkedQueue<>();
    
    public static ParticleEffect getParticle() {
        ParticleEffect particle = particlePool.poll();
        if (particle == null) {
            particle = new ParticleEffect();
        }
        return particle;
    }
    
    public static void returnParticle(ParticleEffect particle) {
        particle.reset();
        particlePool.offer(particle);
    }
}
```

## 11. 调试和测试系统

### 11.1 调试信息输出

#### 分级日志系统
```java
public class DebugLogger {
    public static void logInfo(String message) {
        MythicAgeMod.LOGGER.info("[神话时代] " + message);
    }
    
    public static void logWarning(String message) {
        MythicAgeMod.LOGGER.warn("[神话时代] " + message);
    }
    
    public static void logError(String message, Throwable throwable) {
        MythicAgeMod.LOGGER.error("[神话时代] " + message, throwable);
    }
    
    public static void sendPlayerMessage(PlayerEntity player, String message) {
        if (player instanceof ServerPlayerEntity) {
            player.sendMessage(Text.literal("§e[调试] " + message), false);
        }
    }
}
```

### 11.2 性能监控

#### 执行时间统计
```java
public class PerformanceMonitor {
    private static final Map<String, Long> startTimes = new HashMap<>();
    
    public static void startTimer(String operation) {
        startTimes.put(operation, System.nanoTime());
    }
    
    public static void endTimer(String operation) {
        Long startTime = startTimes.remove(operation);
        if (startTime != null) {
            long duration = System.nanoTime() - startTime;
            DebugLogger.logInfo(operation + " 耗时: " + duration / 1000000 + "ms");
        }
    }
}
```

## 12. 扩展性设计

### 12.1 插件化架构

#### 文明系统扩展
```java
public interface CivilizationProvider {
    MythologyType getMythologyType();
    String getCivilizationName();
    List<TradeOffer> getCivilizationTrades();
    void onPlayerInteract(PlayerEntity player);
}

public class CivilizationRegistry {
    private static final Map<String, CivilizationProvider> civilizations = new HashMap<>();
    
    public static void registerCivilization(String id, CivilizationProvider provider) {
        civilizations.put(id, provider);
    }
    
    public static CivilizationProvider getCivilization(String id) {
        return civilizations.get(id);
    }
}
```

### 12.2 配置系统

#### 动态配置加载
```java
public class ModConfig {
    private static ModConfig INSTANCE;
    private final JsonObject config;
    
    public static ModConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = loadConfig();
        }
        return INSTANCE;
    }
    
    private static ModConfig loadConfig() {
        // 从JSON文件加载配置
        // 支持热重载
        // 提供默认值
    }
    
    public int getCertificateGenerationInterval() {
        return config.get("certificate_generation_interval").getAsInt();
    }
    
    public float getCertificateGenerationChance() {
        return config.get("certificate_generation_chance").getAsFloat();
    }
}
```

## 总结

神话时代Mod采用了现代化的Minecraft模组开发技术栈，通过模块化设计、事件驱动架构、数据驱动配置和性能优化技术，实现了一个功能完整、易于扩展的神话系统。核心技术包括：

1. **实体系统**：继承Minecraft实体体系，实现AI行为和交互逻辑
2. **物品系统**：使用NBT数据管理，支持动态内容和状态跟踪
3. **交易系统**：基于Minecraft原生TradeOffer，实现复杂交易逻辑
4. **多方块结构**：空间查询和结构验证算法
5. **魔法书系统**：法术效果和粒子系统集成
6. **世界生成**：Registry系统和JSON配置
7. **渲染系统**：动态纹理和渲染层管理
8. **数据持久化**：NBT序列化和版本兼容
9. **性能优化**：缓存、延迟加载和对象池
10. **调试系统**：分级日志和性能监控
11. **扩展性**：插件化架构和配置系统

这些技术确保了模组的稳定性、性能和可维护性，为玩家提供了丰富的游戏体验。