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
import net.minecraft.entity.passive.AnimalEntity;
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
     * - 碰撞箱：1.35格宽 x 2.75格高 (大型迷宫怪，明显高于玩家)
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
                    .dimensions(EntityDimensions.fixed(1.35f, 2.75f))
                    .build()
    );

    public static final EntityType<CyclopsEntity> CYCLOPS = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MythicAgeMod.MOD_ID, "cyclops"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, CyclopsEntity::new)
                    .dimensions(EntityDimensions.fixed(1.8f, 4.0f))
                    .build()
    );

    public static final EntityType<CentaurEntity> CENTAUR = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MythicAgeMod.MOD_ID, "centaur"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, CentaurEntity::new)
                    .dimensions(EntityDimensions.fixed(1.9f, 3.0f))
                    .build()
    );

    public static final EntityType<HydraEntity> HYDRA = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MythicAgeMod.MOD_ID, "hydra"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, HydraEntity::new)
                    .dimensions(EntityDimensions.fixed(3.5f, 4.0f))
                    .build()
    );

    public static final EntityType<MedusaEntity> MEDUSA = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MythicAgeMod.MOD_ID, "medusa"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, MedusaEntity::new)
                    .dimensions(EntityDimensions.fixed(1.1f, 2.55f))
                    .build()
    );

    public static final EntityType<NemeanLionEntity> NEMEAN_LION = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MythicAgeMod.MOD_ID, "nemean_lion"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, NemeanLionEntity::new)
                    .dimensions(EntityDimensions.fixed(1.9f, 1.8f))
                    .build()
    );

    public static final EntityType<ManticoreEntity> MANTICORE = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MythicAgeMod.MOD_ID, "manticore"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, ManticoreEntity::new)
                    .dimensions(EntityDimensions.fixed(2.0f, 2.05f))
                    .build()
    );

    public static final EntityType<ChimeraEntity> CHIMERA = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MythicAgeMod.MOD_ID, "chimera"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, ChimeraEntity::new)
                    .dimensions(EntityDimensions.fixed(2.3f, 2.25f))
                    .build()
    );

    public static final EntityType<ColossusEntity> COLOSSUS = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MythicAgeMod.MOD_ID, "colossus"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, ColossusEntity::new)
                    .dimensions(EntityDimensions.fixed(1.7f, 4.2f))
                    .build()
    );

    public static final EntityType<PegasusEntity> PEGASUS = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MythicAgeMod.MOD_ID, "pegasus"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, PegasusEntity::new)
                    .dimensions(EntityDimensions.fixed(1.7f, 2.1f))
                    .trackRangeBlocks(10)
                    .build()
    );

    public static final EntityType<AnubisWarriorEntity> ANUBIS_WARRIOR = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MythicAgeMod.MOD_ID, "anubis_warrior"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, AnubisWarriorEntity::new)
                    .dimensions(EntityDimensions.fixed(0.9f, 2.3f))
                    .build()
    );

    public static final EntityType<ScarabGuardianEntity> SCARAB_GUARDIAN = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MythicAgeMod.MOD_ID, "scarab_guardian"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, ScarabGuardianEntity::new)
                    .dimensions(EntityDimensions.fixed(1.8f, 1.2f))
                    .build()
    );

    public static final EntityType<MummySoldierEntity> MUMMY_SOLDIER = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MythicAgeMod.MOD_ID, "mummy_soldier"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, MummySoldierEntity::new)
                    .dimensions(EntityDimensions.fixed(0.8f, 2.1f))
                    .build()
    );

    public static final EntityType<SphinxSentinelEntity> SPHINX_SENTINEL = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MythicAgeMod.MOD_ID, "sphinx_sentinel"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, SphinxSentinelEntity::new)
                    .dimensions(EntityDimensions.fixed(2.2f, 1.8f))
                    .build()
    );

    public static final EntityType<SunFalconEntity> SUN_FALCON = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MythicAgeMod.MOD_ID, "sun_falcon"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, SunFalconEntity::new)
                    .dimensions(EntityDimensions.fixed(1.2f, 1.3f))
                    .build()
    );

    public static final EntityType<FrostGiantEntity> FROST_GIANT = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MythicAgeMod.MOD_ID, "frost_giant"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, FrostGiantEntity::new)
                    .dimensions(EntityDimensions.fixed(1.8f, 3.9f))
                    .build()
    );

    public static final EntityType<DraugrWarriorEntity> DRAUGR_WARRIOR = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MythicAgeMod.MOD_ID, "draugr_warrior"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, DraugrWarriorEntity::new)
                    .dimensions(EntityDimensions.fixed(0.85f, 2.2f))
                    .build()
    );

    public static final EntityType<ValkyrieEntity> VALKYRIE = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MythicAgeMod.MOD_ID, "valkyrie"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, ValkyrieEntity::new)
                    .dimensions(EntityDimensions.fixed(0.9f, 2.35f))
                    .trackRangeBlocks(10)
                    .build()
    );

    public static final EntityType<FenrirWolfEntity> FENRIR_WOLF = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MythicAgeMod.MOD_ID, "fenrir_wolf"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, FenrirWolfEntity::new)
                    .dimensions(EntityDimensions.fixed(2.1f, 1.8f))
                    .build()
    );

    public static final EntityType<DwarfBerserkerEntity> DWARF_BERSERKER = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MythicAgeMod.MOD_ID, "dwarf_berserker"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, DwarfBerserkerEntity::new)
                    .dimensions(EntityDimensions.fixed(0.95f, 1.45f))
                    .build()
    );

    public static final EntityType<AzureDragonEntity> AZURE_DRAGON = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MythicAgeMod.MOD_ID, "azure_dragon"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, AzureDragonEntity::new)
                    .dimensions(EntityDimensions.fixed(1.6f, 2.2f))
                    .build()
    );

    public static final EntityType<QilinGuardianEntity> QILIN_GUARDIAN = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MythicAgeMod.MOD_ID, "qilin_guardian"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, QilinGuardianEntity::new)
                    .dimensions(EntityDimensions.fixed(1.5f, 2.0f))
                    .build()
    );

    public static final EntityType<JiangshiEntity> JIANGSHI = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MythicAgeMod.MOD_ID, "jiangshi"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, JiangshiEntity::new)
                    .dimensions(EntityDimensions.fixed(0.8f, 2.1f))
                    .build()
    );

    public static final EntityType<NineTailedFoxEntity> NINE_TAILED_FOX = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MythicAgeMod.MOD_ID, "nine_tailed_fox"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, NineTailedFoxEntity::new)
                    .dimensions(EntityDimensions.fixed(1.2f, 1.4f))
                    .build()
    );

    public static final EntityType<StoneLionGuardianEntity> STONE_LION_GUARDIAN = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MythicAgeMod.MOD_ID, "stone_lion_guardian"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, StoneLionGuardianEntity::new)
                    .dimensions(EntityDimensions.fixed(1.6f, 1.7f))
                    .build()
    );

    public static final EntityType<CrystalLeviathanEntity> CRYSTAL_LEVIATHAN = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MythicAgeMod.MOD_ID, "crystal_leviathan"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, CrystalLeviathanEntity::new)
                    .dimensions(EntityDimensions.fixed(2.4f, 2.4f))
                    .build()
    );

    public static final EntityType<OrichalcumGolemEntity> ORICHALCUM_GOLEM = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MythicAgeMod.MOD_ID, "orichalcum_golem"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, OrichalcumGolemEntity::new)
                    .dimensions(EntityDimensions.fixed(1.7f, 3.2f))
                    .build()
    );

    public static final EntityType<SirenPriestessEntity> SIREN_PRIESTESS = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MythicAgeMod.MOD_ID, "siren_priestess"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, SirenPriestessEntity::new)
                    .dimensions(EntityDimensions.fixed(0.9f, 2.2f))
                    .build()
    );

    public static final EntityType<TidalGuardianEntity> TIDAL_GUARDIAN = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MythicAgeMod.MOD_ID, "tidal_guardian"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, TidalGuardianEntity::new)
                    .dimensions(EntityDimensions.fixed(2.0f, 1.4f))
                    .build()
    );

    public static final EntityType<AbyssalRayEntity> ABYSSAL_RAY = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MythicAgeMod.MOD_ID, "abyssal_ray"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, AbyssalRayEntity::new)
                    .dimensions(EntityDimensions.fixed(2.6f, 0.9f))
                    .trackRangeBlocks(10)
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
        FabricDefaultAttributeRegistry.register(NEMEAN_LION, NemeanLionEntity.createNemeanLionAttributes());
        FabricDefaultAttributeRegistry.register(MANTICORE, ManticoreEntity.createManticoreAttributes());
        FabricDefaultAttributeRegistry.register(CHIMERA, ChimeraEntity.createChimeraAttributes());
        FabricDefaultAttributeRegistry.register(COLOSSUS, ColossusEntity.createColossusAttributes());
        FabricDefaultAttributeRegistry.register(PEGASUS, PegasusEntity.createPegasusAttributes());
        FabricDefaultAttributeRegistry.register(ANUBIS_WARRIOR, AnubisWarriorEntity.createAnubisWarriorAttributes());
        FabricDefaultAttributeRegistry.register(SCARAB_GUARDIAN, ScarabGuardianEntity.createScarabGuardianAttributes());
        FabricDefaultAttributeRegistry.register(MUMMY_SOLDIER, MummySoldierEntity.createMummySoldierAttributes());
        FabricDefaultAttributeRegistry.register(SPHINX_SENTINEL, SphinxSentinelEntity.createSphinxSentinelAttributes());
        FabricDefaultAttributeRegistry.register(SUN_FALCON, SunFalconEntity.createSunFalconAttributes());
        FabricDefaultAttributeRegistry.register(FROST_GIANT, FrostGiantEntity.createFrostGiantAttributes());
        FabricDefaultAttributeRegistry.register(DRAUGR_WARRIOR, DraugrWarriorEntity.createDraugrWarriorAttributes());
        FabricDefaultAttributeRegistry.register(VALKYRIE, ValkyrieEntity.createValkyrieAttributes());
        FabricDefaultAttributeRegistry.register(FENRIR_WOLF, FenrirWolfEntity.createFenrirWolfAttributes());
        FabricDefaultAttributeRegistry.register(DWARF_BERSERKER, DwarfBerserkerEntity.createDwarfBerserkerAttributes());
        FabricDefaultAttributeRegistry.register(AZURE_DRAGON, AzureDragonEntity.createAzureDragonAttributes());
        FabricDefaultAttributeRegistry.register(QILIN_GUARDIAN, QilinGuardianEntity.createQilinGuardianAttributes());
        FabricDefaultAttributeRegistry.register(JIANGSHI, JiangshiEntity.createJiangshiAttributes());
        FabricDefaultAttributeRegistry.register(NINE_TAILED_FOX, NineTailedFoxEntity.createNineTailedFoxAttributes());
        FabricDefaultAttributeRegistry.register(STONE_LION_GUARDIAN, StoneLionGuardianEntity.createStoneLionGuardianAttributes());
        FabricDefaultAttributeRegistry.register(CRYSTAL_LEVIATHAN, CrystalLeviathanEntity.createCrystalLeviathanAttributes());
        FabricDefaultAttributeRegistry.register(ORICHALCUM_GOLEM, OrichalcumGolemEntity.createOrichalcumGolemAttributes());
        FabricDefaultAttributeRegistry.register(SIREN_PRIESTESS, SirenPriestessEntity.createSirenPriestessAttributes());
        FabricDefaultAttributeRegistry.register(TIDAL_GUARDIAN, TidalGuardianEntity.createTidalGuardianAttributes());
        FabricDefaultAttributeRegistry.register(ABYSSAL_RAY, AbyssalRayEntity.createAbyssalRayAttributes());
        
        // 注册行走树的默认属性
        FabricDefaultAttributeRegistry.register(WALKING_TREE, WalkingTreeEntity.createWalkingTreeAttributes());
        
        // 注册神殿守护者的默认属性 (使用村民属性)
        FabricDefaultAttributeRegistry.register(TEMPLE_GUARDIAN, TempleGuardianEntity.createVillagerAttributes());
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
        MythicAgeMod.LOGGER.info("- 9 Greek myth creatures: Cyclops, Centaur, Hydra, Medusa, Nemean Lion, Manticore, Chimera, Colossus, Pegasus");
        MythicAgeMod.LOGGER.info("- 5 Egyptian myth creatures: Anubis Warrior, Scarab Guardian, Mummy Soldier, Sphinx Sentinel, Sun Falcon");
        MythicAgeMod.LOGGER.info("- 5 Nordic myth creatures: Frost Giant, Draugr Warrior, Valkyrie, Fenrir Wolf, Dwarf Berserker");
        MythicAgeMod.LOGGER.info("- 5 Chinese myth creatures: Azure Dragon, Qilin Guardian, Jiangshi, Nine-tailed Fox, Stone Lion Guardian");
        MythicAgeMod.LOGGER.info("- 5 Atlantean myth creatures: Crystal Leviathan, Orichalcum Golem, Siren Priestess, Tidal Guardian, Abyssal Ray");
        MythicAgeMod.LOGGER.info("- 5 village temple guardians (20 HP, Villager type)");
        MythicAgeMod.LOGGER.info("- 5 custom temple guardians (30 HP, 0.5 Speed, custom trade system)");
        MythicAgeMod.LOGGER.info("- Enhanced vanilla villagers with civilization AI through Mixin");

        registerGreekMythSpawns();
        registerEgyptianMythSpawns();
        registerNordicMythSpawns();
        registerChineseMythSpawns();
        registerAtlanteanMythSpawns();
    }

    private static void registerGreekMythSpawns() {
        BiomeModifications.addSpawn(
                BiomeSelectors.includeByKey(BiomeKeys.PLAINS, BiomeKeys.DARK_FOREST),
                SpawnGroup.MONSTER,
                MINOTAUR,
                6,
                1,
                1
        );
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
        BiomeModifications.addSpawn(
                BiomeSelectors.tag(BiomeTags.IS_SAVANNA),
                SpawnGroup.MONSTER,
                NEMEAN_LION,
                8,
                1,
                1
        );
        BiomeModifications.addSpawn(
                BiomeSelectors.tag(BiomeTags.IS_BADLANDS),
                SpawnGroup.MONSTER,
                MANTICORE,
                7,
                1,
                1
        );
        BiomeModifications.addSpawn(
                BiomeSelectors.tag(BiomeTags.IS_JUNGLE),
                SpawnGroup.MONSTER,
                CHIMERA,
                5,
                1,
                1
        );
        BiomeModifications.addSpawn(
                BiomeSelectors.tag(BiomeTags.IS_MOUNTAIN),
                SpawnGroup.MONSTER,
                COLOSSUS,
                3,
                1,
                1
        );
        BiomeModifications.addSpawn(
                BiomeSelectors.tag(BiomeTags.IS_MOUNTAIN),
                SpawnGroup.CREATURE,
                PEGASUS,
                4,
                1,
                1
        );

        SpawnRestriction.register(MINOTAUR, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
        SpawnRestriction.register(CYCLOPS, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
        SpawnRestriction.register(CENTAUR, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
        SpawnRestriction.register(HYDRA, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
        SpawnRestriction.register(MEDUSA, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
        SpawnRestriction.register(NEMEAN_LION, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
        SpawnRestriction.register(MANTICORE, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
        SpawnRestriction.register(CHIMERA, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
        SpawnRestriction.register(COLOSSUS, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
        SpawnRestriction.register(PEGASUS, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, AnimalEntity::isValidNaturalSpawn);
    }

    private static void registerEgyptianMythSpawns() {
        BiomeModifications.addSpawn(
                BiomeSelectors.includeByKey(BiomeKeys.DESERT, BiomeKeys.BADLANDS, BiomeKeys.ERODED_BADLANDS, BiomeKeys.WOODED_BADLANDS),
                SpawnGroup.MONSTER,
                ANUBIS_WARRIOR,
                7,
                1,
                2
        );
        BiomeModifications.addSpawn(
                BiomeSelectors.includeByKey(BiomeKeys.DESERT, BiomeKeys.BADLANDS, BiomeKeys.ERODED_BADLANDS, BiomeKeys.WOODED_BADLANDS),
                SpawnGroup.MONSTER,
                SCARAB_GUARDIAN,
                9,
                1,
                2
        );
        BiomeModifications.addSpawn(
                BiomeSelectors.includeByKey(BiomeKeys.DESERT),
                SpawnGroup.MONSTER,
                MUMMY_SOLDIER,
                12,
                1,
                3
        );
        BiomeModifications.addSpawn(
                BiomeSelectors.includeByKey(BiomeKeys.DESERT, BiomeKeys.BADLANDS),
                SpawnGroup.MONSTER,
                SPHINX_SENTINEL,
                4,
                1,
                1
        );
        BiomeModifications.addSpawn(
                BiomeSelectors.includeByKey(BiomeKeys.DESERT, BiomeKeys.SAVANNA, BiomeKeys.SAVANNA_PLATEAU),
                SpawnGroup.MONSTER,
                SUN_FALCON,
                7,
                1,
                2
        );

        SpawnRestriction.register(ANUBIS_WARRIOR, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
        SpawnRestriction.register(SCARAB_GUARDIAN, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
        SpawnRestriction.register(MUMMY_SOLDIER, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
        SpawnRestriction.register(SPHINX_SENTINEL, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
        SpawnRestriction.register(SUN_FALCON, SpawnRestriction.Location.NO_RESTRICTIONS, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
    }

    private static void registerNordicMythSpawns() {
        BiomeModifications.addSpawn(
                BiomeSelectors.includeByKey(BiomeKeys.SNOWY_PLAINS, BiomeKeys.ICE_SPIKES, BiomeKeys.SNOWY_SLOPES, BiomeKeys.FROZEN_PEAKS, BiomeKeys.JAGGED_PEAKS),
                SpawnGroup.MONSTER,
                FROST_GIANT,
                5,
                1,
                1
        );
        BiomeModifications.addSpawn(
                BiomeSelectors.includeByKey(BiomeKeys.SNOWY_PLAINS, BiomeKeys.SNOWY_TAIGA, BiomeKeys.OLD_GROWTH_PINE_TAIGA, BiomeKeys.TAIGA, BiomeKeys.DEEP_DARK),
                SpawnGroup.MONSTER,
                DRAUGR_WARRIOR,
                12,
                1,
                2
        );
        BiomeModifications.addSpawn(
                BiomeSelectors.includeByKey(BiomeKeys.SNOWY_SLOPES, BiomeKeys.FROZEN_PEAKS, BiomeKeys.JAGGED_PEAKS, BiomeKeys.STONY_PEAKS),
                SpawnGroup.MONSTER,
                VALKYRIE,
                5,
                1,
                1
        );
        BiomeModifications.addSpawn(
                BiomeSelectors.includeByKey(BiomeKeys.SNOWY_TAIGA, BiomeKeys.OLD_GROWTH_PINE_TAIGA, BiomeKeys.TAIGA, BiomeKeys.DARK_FOREST, BiomeKeys.SNOWY_PLAINS),
                SpawnGroup.MONSTER,
                FENRIR_WOLF,
                6,
                1,
                1
        );
        BiomeModifications.addSpawn(
                BiomeSelectors.includeByKey(BiomeKeys.STONY_PEAKS, BiomeKeys.JAGGED_PEAKS, BiomeKeys.WINDSWEPT_HILLS, BiomeKeys.OLD_GROWTH_PINE_TAIGA, BiomeKeys.TAIGA),
                SpawnGroup.MONSTER,
                DWARF_BERSERKER,
                9,
                1,
                2
        );

        SpawnRestriction.register(FROST_GIANT, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
        SpawnRestriction.register(DRAUGR_WARRIOR, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
        SpawnRestriction.register(VALKYRIE, SpawnRestriction.Location.NO_RESTRICTIONS, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
        SpawnRestriction.register(FENRIR_WOLF, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
        SpawnRestriction.register(DWARF_BERSERKER, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
    }

    private static void registerChineseMythSpawns() {
        BiomeModifications.addSpawn(
                BiomeSelectors.includeByKey(BiomeKeys.JUNGLE, BiomeKeys.BAMBOO_JUNGLE, BiomeKeys.CHERRY_GROVE, BiomeKeys.WINDSWEPT_FOREST),
                SpawnGroup.MONSTER,
                AZURE_DRAGON,
                4,
                1,
                1
        );
        BiomeModifications.addSpawn(
                BiomeSelectors.includeByKey(BiomeKeys.CHERRY_GROVE, BiomeKeys.BAMBOO_JUNGLE, BiomeKeys.JUNGLE, BiomeKeys.WINDSWEPT_FOREST, BiomeKeys.MEADOW),
                SpawnGroup.MONSTER,
                QILIN_GUARDIAN,
                5,
                1,
                1
        );
        BiomeModifications.addSpawn(
                BiomeSelectors.includeByKey(BiomeKeys.SWAMP, createBiomeKey("mangrove_swamp"), BiomeKeys.DARK_FOREST, BiomeKeys.BAMBOO_JUNGLE),
                SpawnGroup.MONSTER,
                JIANGSHI,
                10,
                1,
                2
        );
        BiomeModifications.addSpawn(
                BiomeSelectors.includeByKey(BiomeKeys.CHERRY_GROVE, BiomeKeys.BAMBOO_JUNGLE, BiomeKeys.FLOWER_FOREST, BiomeKeys.TAIGA, BiomeKeys.OLD_GROWTH_PINE_TAIGA),
                SpawnGroup.MONSTER,
                NINE_TAILED_FOX,
                7,
                1,
                1
        );
        BiomeModifications.addSpawn(
                BiomeSelectors.includeByKey(BiomeKeys.STONY_PEAKS, BiomeKeys.WINDSWEPT_HILLS, BiomeKeys.WINDSWEPT_GRAVELLY_HILLS, BiomeKeys.JUNGLE, BiomeKeys.BAMBOO_JUNGLE),
                SpawnGroup.MONSTER,
                STONE_LION_GUARDIAN,
                5,
                1,
                1
        );

        SpawnRestriction.register(AZURE_DRAGON, SpawnRestriction.Location.NO_RESTRICTIONS, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
        SpawnRestriction.register(QILIN_GUARDIAN, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
        SpawnRestriction.register(JIANGSHI, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
        SpawnRestriction.register(NINE_TAILED_FOX, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
        SpawnRestriction.register(STONE_LION_GUARDIAN, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
    }

    private static void registerAtlanteanMythSpawns() {
        BiomeModifications.addSpawn(
                BiomeSelectors.includeByKey(BiomeKeys.OCEAN, BiomeKeys.DEEP_OCEAN, BiomeKeys.WARM_OCEAN, BiomeKeys.LUKEWARM_OCEAN, BiomeKeys.COLD_OCEAN),
                SpawnGroup.MONSTER,
                CRYSTAL_LEVIATHAN,
                4,
                1,
                1
        );
        BiomeModifications.addSpawn(
                BiomeSelectors.includeByKey(BiomeKeys.BEACH, BiomeKeys.STONY_SHORE, BiomeKeys.OCEAN, BiomeKeys.DEEP_OCEAN),
                SpawnGroup.MONSTER,
                ORICHALCUM_GOLEM,
                5,
                1,
                1
        );
        BiomeModifications.addSpawn(
                BiomeSelectors.includeByKey(BiomeKeys.BEACH, BiomeKeys.WARM_OCEAN, BiomeKeys.LUKEWARM_OCEAN, BiomeKeys.OCEAN),
                SpawnGroup.MONSTER,
                SIREN_PRIESTESS,
                7,
                1,
                2
        );
        BiomeModifications.addSpawn(
                BiomeSelectors.includeByKey(BiomeKeys.BEACH, BiomeKeys.STONY_SHORE, BiomeKeys.WARM_OCEAN, BiomeKeys.LUKEWARM_OCEAN),
                SpawnGroup.MONSTER,
                TIDAL_GUARDIAN,
                7,
                1,
                2
        );
        BiomeModifications.addSpawn(
                BiomeSelectors.includeByKey(BiomeKeys.DEEP_OCEAN, BiomeKeys.OCEAN, BiomeKeys.WARM_OCEAN, BiomeKeys.LUKEWARM_OCEAN, BiomeKeys.COLD_OCEAN),
                SpawnGroup.MONSTER,
                ABYSSAL_RAY,
                6,
                1,
                2
        );

        SpawnRestriction.register(CRYSTAL_LEVIATHAN, SpawnRestriction.Location.NO_RESTRICTIONS, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
        SpawnRestriction.register(ORICHALCUM_GOLEM, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
        SpawnRestriction.register(SIREN_PRIESTESS, SpawnRestriction.Location.NO_RESTRICTIONS, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
        SpawnRestriction.register(TIDAL_GUARDIAN, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
        SpawnRestriction.register(ABYSSAL_RAY, SpawnRestriction.Location.NO_RESTRICTIONS, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
    }

    private static RegistryKey<Biome> createBiomeKey(String name) {
        return RegistryKey.of(RegistryKeys.BIOME, new Identifier(name));
    }
}
