package com.example.mythic_age.entity;

import com.example.mythic_age.MythicAgeMod;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.util.Identifier;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;

/**
 * Mod实体注册类
 * 
 * 负责注册和管理神话时代Mod中的所有自定义实体。
 * 这个类是实体系统的入口点，确保所有神话生物都被正确注册到游戏中。
 * 
 * 当前包含的实体：
 * 1. 米诺陶 (Minotaur)：
 *    - 希腊神话中的牛头人身怪物
 *    - 强大的敌对生物，具有冲锋攻击能力
 *    - 高生命值和攻击力，适合作为中后期挑战
 *    - 完整的AI行为系统和音效
 * 
 * 实体特性：
 * - 属于怪物生成组，会在黑暗中自然生成
 * - 固定的碰撞箱尺寸：0.9x2.1方块
 * - 完整的属性配置和AI行为
 * - 自定义的3D模型和动画
 * 
 * 技术实现：
 * - 使用Fabric API的实体构建器
 * - 自动注册实体属性
 * - 统一的命名空间管理
 * - 完整的客户端-服务器同步
 * 
 * 未来扩展：
 * - 更多希腊神话生物（如独眼巨人、美杜莎）
 * - 埃及神话生物（如阿努比斯战士、木乃伊）
 * - 北欧神话生物（如霜巨人、瓦尔基里）
 * - 中性或友好的神话生物
 * 
 * 平衡性考虑：
 * - 每个实体都有独特的能力和弱点
 * - 适当的生成条件和稀有度
 * - 合理的掉落物和经验值
 * - 与现有游戏内容的协调
 * 
 * @author MythicAge Development Team
 * @version 1.0.0
 * @since 1.20.1
 */
public class ModEntities {
    
    /**
     * 米诺陶实体类型
     * 
     * 希腊神话中的传奇怪物，牛头人身的强大敌对生物。
     * 
     * 实体规格：
     * - 生成组：MONSTER (怪物组，会在黑暗环境中生成)
     * - 碰撞箱：0.9格宽 x 2.1格高 (比玩家稍宽稍高)
     * - 工厂方法：MinotaurEntity::new (创建实体实例)
     * 
     * 游戏特性：
     * - 主动攻击玩家
     * - 具有冲锋攻击能力
     * - 高生命值和攻击力
     * - 完整的音效系统
     * - 智能的AI行为
     */
    public static final EntityType<MinotaurEntity> MINOTAUR = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MythicAgeMod.MOD_ID, "minotaur"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, MinotaurEntity::new)
                    .dimensions(EntityDimensions.fixed(0.9f, 2.1f)) // 宽0.9格，高2.1格
                    .build()
    );

    public static final EntityType<CyclopsEntity> CYCLOPS = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MythicAgeMod.MOD_ID, "cyclops"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, CyclopsEntity::new)
                    .dimensions(EntityDimensions.fixed(1.35f, 3.2f))
                    .build()
    );

    public static final EntityType<CentaurEntity> CENTAUR = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MythicAgeMod.MOD_ID, "centaur"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, CentaurEntity::new)
                    .dimensions(EntityDimensions.fixed(1.2f, 2.45f))
                    .build()
    );

    public static final EntityType<HydraEntity> HYDRA = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MythicAgeMod.MOD_ID, "hydra"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, HydraEntity::new)
                    .dimensions(EntityDimensions.fixed(2.4f, 2.35f))
                    .build()
    );

    public static final EntityType<MedusaEntity> MEDUSA = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MythicAgeMod.MOD_ID, "medusa"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, MedusaEntity::new)
                    .dimensions(EntityDimensions.fixed(0.75f, 2.05f))
                    .build()
    );

    // ============ 神话村庄实体系统 ============
    
    /**
     * 希腊神殿守护者实体类型
     * 
     * 希腊神话村庄中的特殊村民，负责管理希腊神殿和提供希腊神力相关服务。
     */
    public static final EntityType<TempleGuardianEntity> TEMPLE_GUARDIAN_GREEK = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MythicAgeMod.MOD_ID, "temple_guardian_greek"),
            FabricEntityTypeBuilder.create(SpawnGroup.MISC, (EntityType<TempleGuardianEntity> type, World world) -> 
                    new TempleGuardianEntity(type, world, com.example.mythic_age.item.GodPowerCertificateItem.MythologyType.GREEK, null))
                    .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
                    .build()
    );

    /**
     * 埃及神殿守护者实体类型
     * 
     * 埃及神话村庄中的特殊村民，负责管理埃及神殿和提供埃及神力相关服务。
     */
    public static final EntityType<TempleGuardianEntity> TEMPLE_GUARDIAN_EGYPTIAN = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MythicAgeMod.MOD_ID, "temple_guardian_egyptian"),
            FabricEntityTypeBuilder.create(SpawnGroup.MISC, (EntityType<TempleGuardianEntity> type, World world) -> 
                    new TempleGuardianEntity(type, world, com.example.mythic_age.item.GodPowerCertificateItem.MythologyType.EGYPTIAN, null))
                    .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
                    .build()
    );

    /**
     * 中国神殿守护者实体类型
     * 
     * 中国神话村庄中的特殊村民，负责管理中国神殿和提供中国神力相关服务。
     */
    public static final EntityType<TempleGuardianEntity> TEMPLE_GUARDIAN_CHINESE = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MythicAgeMod.MOD_ID, "temple_guardian_chinese"),
            FabricEntityTypeBuilder.create(SpawnGroup.MISC, (EntityType<TempleGuardianEntity> type, World world) -> 
                    new TempleGuardianEntity(type, world, com.example.mythic_age.item.GodPowerCertificateItem.MythologyType.CHINESE, null))
                    .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
                    .build()
    );

    /**
     * 亚特兰蒂斯神殿守护者实体类型
     * 
     * 亚特兰蒂斯神话村庄中的特殊村民，负责管理亚特兰蒂斯神殿和提供亚特兰蒂斯神力相关服务。
     */
    public static final EntityType<TempleGuardianEntity> TEMPLE_GUARDIAN_ATLANTEAN = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MythicAgeMod.MOD_ID, "temple_guardian_atlantean"),
            FabricEntityTypeBuilder.create(SpawnGroup.MISC, (EntityType<TempleGuardianEntity> type, World world) -> 
                    new TempleGuardianEntity(type, world, com.example.mythic_age.item.GodPowerCertificateItem.MythologyType.ATLANTEAN, null))
                    .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
                    .build()
    );

    /**
     * 北欧神殿守护者实体类型
     * 
     * 北欧神话村庄中的特殊村民，负责管理北欧神殿和提供北欧神力相关服务。
     */
    public static final EntityType<TempleGuardianEntity> TEMPLE_GUARDIAN_NORDIC = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MythicAgeMod.MOD_ID, "temple_guardian_nordic"),
            FabricEntityTypeBuilder.create(SpawnGroup.MISC, (EntityType<TempleGuardianEntity> type, World world) -> 
                    new TempleGuardianEntity(type, world, com.example.mythic_age.item.GodPowerCertificateItem.MythologyType.NORDIC, null))
                    .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
                    .build()
    );

    /**
     * 通用神殿守护者实体类型 (保留兼容性)
     * 
     * @deprecated 使用特定文明的神殿守护者类型
     */
    @Deprecated
    public static final EntityType<TempleGuardianEntity> TEMPLE_GUARDIAN = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MythicAgeMod.MOD_ID, "temple_guardian"),
            FabricEntityTypeBuilder.create(SpawnGroup.MISC, (EntityType<TempleGuardianEntity> type, World world) -> 
                    new TempleGuardianEntity(type, world))
                    .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
                    .build()
    );

    // ============ 自定义神殿守护者系统 ============
    
    /**
     * 自定义希腊神殿守护者实体类型
     * 
     * 这是一个专门设计的神殿守护者，不继承自VillagerEntity，
     * 具有完全自定义的交易系统，解决原版村民交易的限制问题。
     */
    public static final EntityType<CustomTempleGuardianEntity> CUSTOM_TEMPLE_GUARDIAN_GREEK = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MythicAgeMod.MOD_ID, "custom_temple_guardian_greek"),
            FabricEntityTypeBuilder.create(SpawnGroup.MISC, (EntityType<CustomTempleGuardianEntity> type, World world) -> 
                    new CustomTempleGuardianEntity(type, world, com.example.mythic_age.item.GodPowerCertificateItem.MythologyType.GREEK, null))
                    .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
                    .build()
    );

    /**
     * 自定义埃及神殿守护者实体类型
     */
    public static final EntityType<CustomTempleGuardianEntity> CUSTOM_TEMPLE_GUARDIAN_EGYPTIAN = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MythicAgeMod.MOD_ID, "custom_temple_guardian_egyptian"),
            FabricEntityTypeBuilder.create(SpawnGroup.MISC, (EntityType<CustomTempleGuardianEntity> type, World world) -> 
                    new CustomTempleGuardianEntity(type, world, com.example.mythic_age.item.GodPowerCertificateItem.MythologyType.EGYPTIAN, null))
                    .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
                    .build()
    );

    /**
     * 自定义中国神殿守护者实体类型
     */
    public static final EntityType<CustomTempleGuardianEntity> CUSTOM_TEMPLE_GUARDIAN_CHINESE = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MythicAgeMod.MOD_ID, "custom_temple_guardian_chinese"),
            FabricEntityTypeBuilder.create(SpawnGroup.MISC, (EntityType<CustomTempleGuardianEntity> type, World world) -> 
                    new CustomTempleGuardianEntity(type, world, com.example.mythic_age.item.GodPowerCertificateItem.MythologyType.CHINESE, null))
                    .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
                    .build()
    );

    /**
     * 自定义亚特兰蒂斯神殿守护者实体类型
     */
    public static final EntityType<CustomTempleGuardianEntity> CUSTOM_TEMPLE_GUARDIAN_ATLANTEAN = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MythicAgeMod.MOD_ID, "custom_temple_guardian_atlantean"),
            FabricEntityTypeBuilder.create(SpawnGroup.MISC, (EntityType<CustomTempleGuardianEntity> type, World world) -> 
                    new CustomTempleGuardianEntity(type, world, com.example.mythic_age.item.GodPowerCertificateItem.MythologyType.ATLANTEAN, null))
                    .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
                    .build()
    );

    /**
     * 自定义北欧神殿守护者实体类型
     */
    public static final EntityType<CustomTempleGuardianEntity> CUSTOM_TEMPLE_GUARDIAN_NORDIC = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MythicAgeMod.MOD_ID, "custom_temple_guardian_nordic"),
            FabricEntityTypeBuilder.create(SpawnGroup.MISC, (EntityType<CustomTempleGuardianEntity> type, World world) -> 
                    new CustomTempleGuardianEntity(type, world, com.example.mythic_age.item.GodPowerCertificateItem.MythologyType.NORDIC, null))
                    .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
                    .build()
    );

    // ============ 召唤生物实体系统 ============
    
    /**
     * 行走树实体类型
     * 
     * 由北欧神力"行走的森林"召唤的特殊怪物，具有以下特性：
     * - 固守召唤位置附近的小范围区域
     * - 拥有较高的生命值(60 HP)
     * - 受到火焰伤害时伤害翻倍
     * - 移动速度较慢但攻击力强
     */
    public static final EntityType<WalkingTreeEntity> WALKING_TREE = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MythicAgeMod.MOD_ID, "walking_tree"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, WalkingTreeEntity::new)
                    .dimensions(EntityDimensions.fixed(2.1f, 4.8f)) // 目标体量约4-5格高，底部约2x2
                    .build()
    );

    // ============ 实体注册方法 ============
    
    /**
     * 实体注册主方法
     * 
     * 在Mod初始化时调用，确保所有自定义实体被正确注册到游戏中。
     * 这个方法不仅注册实体类型，还注册实体的默认属性。
     * 
     * 注册过程：
     * 1. 实体类型注册：定义实体的基本信息和工厂方法
     * 2. 属性注册：设置实体的生命值、攻击力、移动速度等属性
     * 3. 客户端渲染：在客户端注册实体的3D模型和渲染器
     * 
     * 当前注册的实体：
     * - 米诺陶：强大的神话怪物，具有独特的冲锋攻击
     * - 神殿守护者：神话村庄的特殊村民，提供神力服务
     * - 自定义神殿守护者：具有完全自定义交易系统的守护者
     * 
     * 村民文明系统：
     * 通过Mixin为原版村民添加文明属性和AI行为，无需注册新实体类型
     * 
     * 属性配置：
     * - 米诺陶：40 HP, 8 ATK, 0.25 Speed, 35 Follow Range
     * - 神殿守护者：20 HP, 村民属性, 0.5 Speed, 10 Follow Range
     * - 自定义神殿守护者：30 HP, 0.5 Speed, 自定义交易系统
     */
    public static void registerModEntities() {
        MythicAgeMod.LOGGER.info("Registering Mod Entities for " + MythicAgeMod.MOD_ID);
        
        // 注册米诺陶的默认属性
        FabricDefaultAttributeRegistry.register(MINOTAUR, MinotaurEntity.createMinotaurAttributes());
        FabricDefaultAttributeRegistry.register(CYCLOPS, CyclopsEntity.createCyclopsAttributes());
        FabricDefaultAttributeRegistry.register(CENTAUR, CentaurEntity.createCentaurAttributes());
        FabricDefaultAttributeRegistry.register(HYDRA, HydraEntity.createHydraAttributes());
        FabricDefaultAttributeRegistry.register(MEDUSA, MedusaEntity.createMedusaAttributes());
        
        // 注册行走树的默认属性
        FabricDefaultAttributeRegistry.register(WALKING_TREE, WalkingTreeEntity.createWalkingTreeAttributes());
        
        // 注册神殿守护者的默认属性 (使用村民属性)
        FabricDefaultAttributeRegistry.register(TEMPLE_GUARDIAN_GREEK, TempleGuardianEntity.createVillagerAttributes());
        FabricDefaultAttributeRegistry.register(TEMPLE_GUARDIAN_EGYPTIAN, TempleGuardianEntity.createVillagerAttributes());
        FabricDefaultAttributeRegistry.register(TEMPLE_GUARDIAN_CHINESE, TempleGuardianEntity.createVillagerAttributes());
        FabricDefaultAttributeRegistry.register(TEMPLE_GUARDIAN_ATLANTEAN, TempleGuardianEntity.createVillagerAttributes());
        FabricDefaultAttributeRegistry.register(TEMPLE_GUARDIAN_NORDIC, TempleGuardianEntity.createVillagerAttributes());
        
        // 注册自定义神殿守护者的默认属性
        FabricDefaultAttributeRegistry.register(CUSTOM_TEMPLE_GUARDIAN_GREEK, CustomTempleGuardianEntity.createMobAttributes());
        FabricDefaultAttributeRegistry.register(CUSTOM_TEMPLE_GUARDIAN_EGYPTIAN, CustomTempleGuardianEntity.createMobAttributes());
        FabricDefaultAttributeRegistry.register(CUSTOM_TEMPLE_GUARDIAN_CHINESE, CustomTempleGuardianEntity.createMobAttributes());
        FabricDefaultAttributeRegistry.register(CUSTOM_TEMPLE_GUARDIAN_ATLANTEAN, CustomTempleGuardianEntity.createMobAttributes());
        FabricDefaultAttributeRegistry.register(CUSTOM_TEMPLE_GUARDIAN_NORDIC, CustomTempleGuardianEntity.createMobAttributes());
        
        MythicAgeMod.LOGGER.info("Successfully registered entities:");
        MythicAgeMod.LOGGER.info("- 1 mythic creature: Minotaur (40 HP, 8 ATK, 0.25 Speed)");
        MythicAgeMod.LOGGER.info("- 4 Greek myth creatures: Cyclops, Centaur, Hydra, Medusa");
        MythicAgeMod.LOGGER.info("- 5 village temple guardians (20 HP, Villager type)");
        MythicAgeMod.LOGGER.info("- 5 custom temple guardians (30 HP, 0.5 Speed, custom trade system)");
        MythicAgeMod.LOGGER.info("- Enhanced vanilla villagers with civilization AI through Mixin");

        registerGreekMythSpawns();
    }

    private static void registerGreekMythSpawns() {
        BiomeModifications.addSpawn(
                BiomeSelectors.tag(BiomeTags.IS_MOUNTAIN),
                SpawnGroup.MONSTER,
                CYCLOPS,
                8,
                1,
                1
        );
        BiomeModifications.addSpawn(
                BiomeSelectors.tag(BiomeTags.IS_SAVANNA),
                SpawnGroup.MONSTER,
                CENTAUR,
                12,
                1,
                2
        );
        BiomeModifications.addSpawn(
                BiomeSelectors.includeByKey(BiomeKeys.SWAMP, createBiomeKey("mangrove_swamp")),
                SpawnGroup.MONSTER,
                HYDRA,
                5,
                1,
                1
        );
        BiomeModifications.addSpawn(
                BiomeSelectors.tag(BiomeTags.IS_JUNGLE),
                SpawnGroup.MONSTER,
                MEDUSA,
                8,
                1,
                1
        );

        SpawnRestriction.register(CYCLOPS, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
        SpawnRestriction.register(CENTAUR, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
        SpawnRestriction.register(HYDRA, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
        SpawnRestriction.register(MEDUSA, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
    }

    private static RegistryKey<Biome> createBiomeKey(String name) {
        return RegistryKey.of(RegistryKeys.BIOME, new Identifier(name));
    }
}
