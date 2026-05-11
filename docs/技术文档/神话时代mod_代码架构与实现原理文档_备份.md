# 神话时代Mod - 代码架构与实现原理文档

## 项目概述

神话时代Mod是一个基于Minecraft 1.20.1 Fabric的模组，实现了多文明神话系统，包括神殿守护者、神力证明、魔法书交易、神像系统等核心功能。

## 核心架构

### 1. 项目结构
```
MythicAgeMod/
├── src/main/java/com/example/mythic_age/
│   ├── MythicAgeMod.java              # 主模组类
│   ├── MythicAgeModClient.java        # 客户端初始化
│   ├── entity/                        # 实体相关
│   ├── item/                          # 物品相关
│   ├── block/                         # 方块相关
│   ├── client/render/                 # 客户端渲染
│   ├── system/                        # 系统管理
│   ├── util/                          # 工具类
│   └── worldgen/                      # 世界生成
└── src/main/resources/
    ├── assets/mythic_age/             # 资源文件
    └── data/mythic_age/               # 数据文件
```

## 核心功能模块详解

### 1. 神殿守护者系统 (TempleGuardianEntity)

#### 功能概述
神殿守护者是模组的核心NPC，负责神力证明生成、交易服务和玩家引导。

#### 核心文件
- `src/main/java/com/example/mythic_age/entity/TempleGuardianEntity.java`

#### 实现原理

##### 1.1 实体继承结构
```java
public class TempleGuardianEntity extends VillagerEntity
```
- 继承自VillagerEntity，获得村民的基础功能
- 保持与村民的兼容性，同时扩展特殊功能

##### 1.2 神话类型系统
```java
private final GodPowerCertificateItem.MythologyType mythologyType;
```
- 支持5种文明：希腊、埃及、中国、亚特兰蒂斯、北欧
- 每种文明有独特的交易内容和行为模式

##### 1.3 神力证明生成机制
```java
private void generateCertificatesForNearbyVillagers() {
    // 每60秒检查一次
    if (certificateGenerationTimer >= CERTIFICATE_GENERATION_INTERVAL) {
        // 30%概率为附近村民生成神力证明
        if (serverWorld.random.nextFloat() < 0.3f) {
            ItemStack certificate = createCertificate();
            villager.getInventory().addStack(certificate);
        }
    }
}
```

**底层原理：**
- 使用计时器系统，每1200 ticks（60秒）执行一次
- 通过ServerWorld.getEntitiesByClass()查找附近村民
- 使用随机数控制生成概率，平衡游戏经济

##### 1.4 玩家交互逻辑
```java
@Override
public ActionResult interactMob(PlayerEntity player, Hand hand) {
    // 检查玩家是否已有该文明的神力指南
    boolean hasGuide = hasPlayerGuide(player);
    
    if (!hasGuide) {
        // 第一次交互：给予神力指南
        givePlayerGuide(player);
        return ActionResult.SUCCESS;
    } else {
        // 已有指南，直接打开交易界面
        return openTradingInterface(player);
    }
}
```

**底层原理：**
- 使用NBT数据跟踪玩家状态
- 通过物品NBT中的"civilization"字段识别文明类型
- 实现状态机模式：首次交互→给予指南→后续交互→交易界面

##### 1.5 交易系统实现
```java
private void initializeTrades() {
    TradeOfferList offers = new TradeOfferList();
    
    // 基础交易
    offers.add(new TradeOffer(
        new ItemStack(Items.EMERALD, 5),    // 输入
        certificate,                         // 输出
        10, 2, 0.05f                        // 最大使用次数, 经验, 价格倍数
    ));
    
    // 文明特色交易
    addCivilizationSpecificTrades(offers);
    this.setOffers(offers);
}
```

**底层原理：**
- 使用Minecraft原生的TradeOffer系统
- 支持动态价格调整和库存管理
- 通过反射和NBT实现复杂交易逻辑

### 2. 神力证明系统 (GodPowerCertificateItem)

#### 功能概述
神力证明是模组的经济基础，用于兑换魔法书和其他高级物品。

#### 核心文件
- `src/main/java/com/example/mythic_age/item/GodPowerCertificateItem.java`

#### 实现原理

##### 2.1 文明类型枚举
```java
public enum MythologyType {
    GREEK("greek", "希腊", Formatting.BLUE),
    EGYPTIAN("egyptian", "埃及", Formatting.GOLD),
    CHINESE("chinese", "中国", Formatting.RED),
    ATLANTEAN("atlantean", "亚特兰蒂斯", Formatting.AQUA),
    NORDIC("nordic", "北欧", Formatting.GRAY);
}
```

##### 2.2 物品使用逻辑
```java
@Override
public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
    if (!world.isClient) {
        // 显示神力证明信息
        showCertificateInfo(user, itemStack);
        return TypedActionResult.success(itemStack);
    }
    return TypedActionResult.success(itemStack);
}
```

**底层原理：**
- 使用TypedActionResult处理物品使用事件
- 区分客户端和服务端逻辑
- 通过NBT存储物品状态信息

### 3. 神力指南系统 (GodPowerGuideItem)

#### 功能概述
神力指南为玩家提供详细的游戏说明和文明背景知识。

#### 核心文件
- `src/main/java/com/example/mythic_age/item/GodPowerGuideItem.java`

#### 实现原理

##### 3.1 动态书籍生成
```java
private ItemStack createGuideBook(String civilization) {
    ItemStack book = new ItemStack(net.minecraft.item.Items.WRITTEN_BOOK);
    NbtCompound nbt = book.getOrCreateNbt();
    
    // 根据文明类型设置内容
    switch (civilization) {
        case "greek" -> addGreekPages(pages);
        case "egyptian" -> addEgyptianPages(pages);
        // ... 其他文明
    }
    
    nbt.put("pages", pages);
    return book;
}
```

**底层原理：**
- 使用Minecraft原生的WrittenBookItem
- 通过JSON格式的文本实现富文本显示
- 支持颜色、格式化和多页面内容

##### 3.2 文明特定内容
```java
private void addGreekPages(NbtList pages) {
    String page1 = """
    {"text":"§b§l希腊神力指南\\n\\n","extra":[
    {"text":"§e欢迎来到希腊神殿！\\n\\n","color":"yellow"},
    {"text":"§f作为智慧与力量的象征...","color":"white"}
    ]}""";
    pages.add(NbtString.of(page1));
}
```

### 4. 神像基座系统 (StatueBaseBlock)

#### 功能概述
神像基座是多方块结构的核心，支持神力证明自动储存和玩家交互。

#### 核心文件
- `src/main/java/com/example/mythic_age/block/StatueBaseBlock.java`
- `src/main/java/com/example/mythic_age/block/entity/StatueBaseBlockEntity.java`

#### 实现原理

##### 4.1 多方块结构检测
```java
public static boolean isCompleteStructure(World world, BlockPos pos) {
    // 检查4层垂直结构：基座→腿部→身体→头部
    BlockPos[] structurePositions = {
        pos,                    // 基座
        pos.up(),              // 腿部
        pos.up(2),             // 身体
        pos.up(3)              // 头部
    };
    
    for (BlockPos checkPos : structurePositions) {
        if (!isValidStatueBlock(world, checkPos)) {
            return false;
        }
    }
    return true;
}
```

**底层原理：**
- 使用BlockPos计算相对位置
- 通过BlockState检查方块类型
- 实现结构完整性验证

##### 4.2 自动储存机制
```java
public void tick() {
    if (!this.getWorld().isClient && this.getWorld().getTime() % 200 == 0) {
        // 每10秒检查一次附近的神力证明
        List<ItemEntity> nearbyItems = this.getWorld().getEntitiesByClass(
            ItemEntity.class, 
            this.getBoundingBox().expand(2.0),
            item -> item.getStack().getItem() instanceof GodPowerCertificateItem
        );
        
        for (ItemEntity itemEntity : nearbyItems) {
            if (this.storedCertificates.size() < MAX_STORAGE) {
                this.storedCertificates.add(itemEntity.getStack().copy());
                itemEntity.discard();
            }
        }
    }
}
```

**底层原理：**
- 使用BlockEntity的tick()方法实现周期性检查
- 通过Entity查询系统查找附近物品
- 使用NBT存储数据，支持区块卸载

### 5. 魔法书系统

#### 功能概述
魔法书是模组的高级功能，提供各种强大的法术效果。

#### 核心文件
- `src/main/java/com/example/mythic_age/item/` (各种魔法书类)

#### 实现原理

##### 5.1 法术效果实现
```java
public class LightningStormManager {
    public static void castLightningStorm(World world, BlockPos center, int radius) {
        for (int i = 0; i < 10; i++) {
            BlockPos targetPos = center.add(
                world.random.nextInt(radius * 2) - radius,
                0,
                world.random.nextInt(radius * 2) - radius
            );
            
            // 创建闪电效果
            LightningEntity lightning = EntityType.LIGHTNING_BOLT.create(world);
            lightning.setPosition(targetPos.getX(), targetPos.getY(), targetPos.getZ());
            world.spawnEntity(lightning);
        }
    }
}
```

**底层原理：**
- 使用Minecraft原生的LightningEntity
- 通过随机数生成法术效果位置
- 实现范围性法术效果

### 6. 世界生成系统

#### 功能概述
负责生成神话村庄和神殿结构。

#### 核心文件
- `src/main/java/com/example/mythic_age/worldgen/MythicVillageTypes.java`
- `src/main/resources/data/mythic_age/worldgen/`

#### 实现原理

##### 6.1 村庄类型注册
```java
public class MythicVillageTypes {
    public static void registerVillageTypes() {
        // 注册5种文明村庄
        Registry.register(Registry.VILLAGE_TYPE, 
            new Identifier(MythicAgeMod.MOD_ID, "greek"), 
            new VillageType("greek"));
    }
}
```

**底层原理：**
- 使用Minecraft的Registry系统
- 通过JSON文件定义结构池
- 实现生物群系相关的村庄生成

### 7. 客户端渲染系统

#### 功能概述
处理实体和方块的客户端渲染。

#### 核心文件
- `src/main/java/com/example/mythic_age/client/render/entity/TempleGuardianEntityRenderer.java`

#### 实现原理

##### 7.1 动态纹理系统
```java
@Override
public Identifier getTexture(VillagerEntity villagerEntity) {
    if (villagerEntity instanceof TempleGuardianEntity guardian) {
        GodPowerCertificateItem.MythologyType mythologyType = guardian.getMythologyType();
        
        // 根据文明类型返回对应纹理
        return switch (mythologyType) {
            case GREEK -> new Identifier(MythicAgeMod.MOD_ID, "textures/entity/temple_guardian_greek.png");
            case EGYPTIAN -> new Identifier(MythicAgeMod.MOD_ID, "textures/entity/temple_guardian_egyptian.png");
            // ... 其他文明
        };
    }
    return super.getTexture(villagerEntity);
}
```

**底层原理：**
- 继承VillagerEntityRenderer
- 使用Identifier系统管理纹理资源
- 实现动态纹理切换

### 8. 系统管理器

#### 功能概述
管理全局状态和系统配置。

#### 核心文件
- `src/main/java/com/example/mythic_age/system/GodPowerCertificateManager.java`

#### 实现原理

##### 8.1 全局状态管理
```java
public class GodPowerCertificateManager {
    private static final Map<UUID, PlayerCertificateData> playerData = new HashMap<>();
    
    public static void addCertificateToPlayer(UUID playerId, MythologyType type, int count) {
        PlayerCertificateData data = playerData.computeIfAbsent(playerId, 
            k -> new PlayerCertificateData());
        data.addCertificate(type, count);
    }
}
```

**底层原理：**
- 使用静态Map存储玩家数据
- 通过UUID标识玩家
- 实现数据持久化

## 技术架构特点

### 1. 模块化设计
- 每个功能模块独立封装
- 清晰的接口定义
- 易于扩展和维护

### 2. 事件驱动架构
- 使用Minecraft的事件系统
- 响应式编程模式
- 松耦合设计

### 3. 数据驱动
- 使用JSON配置文件
- 支持热重载
- 易于平衡调整

### 4. 性能优化
- 使用缓存机制
- 延迟加载
- 内存管理优化

## 编译和部署

### 编译脚本
```bash
./build-with-gradle8.sh clean build
```

### 部署流程
1. 编译生成JAR文件
2. 复制到Minecraft mods文件夹
3. 启动游戏测试功能

## 调试和测试

### 调试信息
- 添加了详细的调试输出
- 支持日志级别控制
- 便于问题定位

### 测试方法
- 单元测试覆盖核心逻辑
- 集成测试验证功能完整性
- 性能测试确保稳定性

## 扩展指南

### 添加新文明
1. 在MythologyType枚举中添加新类型
2. 创建对应的纹理和模型文件
3. 实现文明特定的交易内容
4. 添加世界生成配置

### 添加新魔法书
1. 创建新的魔法书物品类
2. 实现法术效果逻辑
3. 添加到交易系统中
4. 更新神力指南内容

## 总结

神话时代Mod采用了现代化的Minecraft模组开发架构，通过模块化设计、事件驱动编程和数据驱动配置，实现了一个功能完整、易于扩展的神话系统。核心功能包括神殿守护者、神力证明、魔法书交易、神像系统等，为玩家提供了丰富的游戏体验。