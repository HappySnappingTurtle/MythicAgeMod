package com.example.mythic_age.block;

import com.example.mythic_age.MythicAgeMod;
import com.example.mythic_age.block.entity.ModBlockEntities;
import com.example.mythic_age.block.entity.RelicPedestalBlockEntity;
import com.example.mythic_age.item.GodPowerCertificateItem;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Mod方块注册类
 * 
 * 负责注册和管理神话时代Mod中的所有自定义方块。
 * 这个类是方块系统的入口点，确保所有方块都被正确注册到游戏中。
 * 
 * 当前包含的方块：
 * 1. 圣物基座 (Relic Pedestal)：
 *    - 神殿系统的核心方块
 *    - 用于放置和激活神话圣物
 *    - 支持多方块结构检测
 *    - 具有自定义的3D模型和交互逻辑
 * 
 * 方块特性：
 * - 基于石砖的材质和音效
 * - 非完全不透明，允许光线穿透
 * - 自动生成对应的物品形式
 * - 完整的注册表集成
 * 
 * 技术实现：
 * - 使用Fabric API的方块设置
 * - 自动注册方块物品
 * - 统一的命名空间管理
 * - 日志记录用于调试
 * 
 * 未来扩展：
 * - 可能添加更多神殿相关方块
 * - 装饰性的神话主题方块
 * - 特殊功能的仪式方块
 * 
 * @author MythicAge Development Team
 * @version 1.0.0
 * @since 1.20.1
 */
public class ModBlocks {

    /**
     * 圣物基座方块
     * 
     * 神殿系统的核心方块，用于放置神话圣物并激活其神殿效果。
     * 
     * 方块属性：
     * - 材质：基于石砖，坚固耐用
     * - 音效：石头音效组，符合古代建筑主题
     * - 透明度：非完全不透明，允许光线和视线穿透
     * - 交互性：支持右键放置和取出圣物
     * - 结构检测：自动检测周围的神殿结构
     */
    public static final Block RELIC_PEDESTAL = registerBlock("relic_pedestal",
            new RelicPedestalBlock(FabricBlockSettings.copyOf(Blocks.STONE_BRICKS)
                .sounds(BlockSoundGroup.STONE)
                .nonOpaque()));

    // ============ 希腊神庙建筑方块系统 ============
    
    /**
     * 希腊神庙台阶
     * 
     * 希腊神庙建筑的装饰性台阶方块，采用古典希腊建筑风格。
     * 基于石质台阶，具有希腊神庙特有的纹理和设计。
     */
    public static final Block TEMPLE_GREEK_STAIRS = registerBlock("temple_greek_stairs",
            new StairsBlock(Blocks.STONE_BRICKS.getDefaultState(), 
                FabricBlockSettings.copyOf(Blocks.STONE_BRICK_STAIRS)
                    .sounds(BlockSoundGroup.STONE)));
    
    /**
     * 希腊神庙砖
     * 
     * 希腊神庙建筑的主要构造方块，采用古典希腊风格的石砖设计。
     * 作为神庙建筑的基础材料，具有坚固性和美观性。
     */
    public static final Block TEMPLE_GREEK_BRICKS = registerBlock("temple_greek_bricks",
            new Block(FabricBlockSettings.copyOf(Blocks.STONE_BRICKS)
                .sounds(BlockSoundGroup.STONE)));
    
    /**
     * 希腊神庙半砖
     * 
     * 希腊神庙建筑的装饰性半砖方块，用于细节装饰和过渡。
     * 保持与神庙砖一致的风格，提供更多建筑设计选择。
     */
    public static final Block TEMPLE_GREEK_SLAB = registerBlock("temple_greek_slab",
            new SlabBlock(FabricBlockSettings.copyOf(Blocks.STONE_BRICK_SLAB)
                .sounds(BlockSoundGroup.STONE)));
    
    /**
     * 希腊神庙浮雕
     * 
     * 希腊神庙建筑的装饰性浮雕方块，刻有希腊神话图案。
     * 用于神庙的装饰和文化表达，展现古希腊艺术风格。
     */
    public static final Block TEMPLE_GREEK_RELIEF = registerBlock("temple_greek_relief",
            new Block(FabricBlockSettings.copyOf(Blocks.CHISELED_STONE_BRICKS)
                .sounds(BlockSoundGroup.STONE)
                .nonOpaque()));
    
    /**
     * 希腊神庙柱子
     * 
     * 希腊神庙建筑的核心装饰柱子，采用多利安柱式设计。
     * 原希腊神殿方块，现重新定义为装饰性柱子组件。
     */
    public static final Block TEMPLE_GREEK_PILLAR = registerBlock("temple_greek_pillar",
            new Block(FabricBlockSettings.copyOf(Blocks.QUARTZ_PILLAR)
                .sounds(BlockSoundGroup.STONE)
                .nonOpaque()));

    // ============ 埃及神殿建筑系统 ============
    
    /**
     * 埃及神殿砖
     * 埃及神殿的基础建筑材料，采用黄色砂岩风格，体现埃及沙漠建筑特色
     */
    public static final Block TEMPLE_EGYPTIAN_BRICKS = registerBlock("temple_egyptian_bricks",
            new Block(FabricBlockSettings.copyOf(Blocks.SANDSTONE)
                .sounds(BlockSoundGroup.STONE)
                .strength(1.5f)));
    
    /**
     * 埃及神殿台阶
     * 埃及神殿的台阶方块，用于建造金字塔式的阶梯结构
     */
    public static final Block TEMPLE_EGYPTIAN_STAIRS = registerBlock("temple_egyptian_stairs",
            new StairsBlock(TEMPLE_EGYPTIAN_BRICKS.getDefaultState(),
                FabricBlockSettings.copyOf(Blocks.SANDSTONE_STAIRS)
                    .sounds(BlockSoundGroup.STONE)
                    .strength(1.5f)));
    
    /**
     * 埃及神殿半砖
     * 埃及神殿的半砖方块，用于精细的建筑装饰和层次感
     */
    public static final Block TEMPLE_EGYPTIAN_SLAB = registerBlock("temple_egyptian_slab",
            new SlabBlock(FabricBlockSettings.copyOf(Blocks.SANDSTONE_SLAB)
                .sounds(BlockSoundGroup.STONE)
                .strength(1.5f)));
    
    /**
     * 埃及神殿浮雕
     * 埃及神殿的装饰方块，雕刻有象形文字和法老图案
     */
    public static final Block TEMPLE_EGYPTIAN_RELIEF = registerBlock("temple_egyptian_relief",
            new Block(FabricBlockSettings.copyOf(Blocks.CHISELED_SANDSTONE)
                .sounds(BlockSoundGroup.STONE)
                .strength(1.5f)));
    
    /**
     * 埃及神殿柱子
     * 埃及神殿的支撑柱，采用古埃及柱式设计，顶部有莲花装饰
     */
    public static final Block TEMPLE_EGYPTIAN_PILLAR = registerBlock("temple_egyptian_pillar",
            new PillarBlock(FabricBlockSettings.copyOf(Blocks.SANDSTONE)
                .sounds(BlockSoundGroup.STONE)
                .strength(1.5f)));

    // ============ 中国神殿建筑系统 ============
    
    /**
     * 中国神殿砖
     * 中国神殿的基础建筑材料，采用红色陶瓦风格，体现传统建筑特色
     */
    public static final Block TEMPLE_CHINESE_BRICKS = registerBlock("temple_chinese_bricks",
            new Block(FabricBlockSettings.copyOf(Blocks.RED_TERRACOTTA)
                .sounds(BlockSoundGroup.STONE)
                .strength(1.5f)));
    
    /**
     * 中国神殿台阶
     * 中国神殿的台阶方块，用于建造传统的庙宇阶梯
     */
    public static final Block TEMPLE_CHINESE_STAIRS = registerBlock("temple_chinese_stairs",
            new StairsBlock(TEMPLE_CHINESE_BRICKS.getDefaultState(),
                FabricBlockSettings.copyOf(Blocks.RED_TERRACOTTA)
                    .sounds(BlockSoundGroup.STONE)
                    .strength(1.5f)));
    
    /**
     * 中国神殿半砖
     * 中国神殿的半砖方块，用于建筑的精细装饰
     */
    public static final Block TEMPLE_CHINESE_SLAB = registerBlock("temple_chinese_slab",
            new SlabBlock(FabricBlockSettings.copyOf(Blocks.RED_TERRACOTTA)
                .sounds(BlockSoundGroup.STONE)
                .strength(1.5f)));
    
    /**
     * 中国神殿浮雕
     * 中国神殿的装饰方块，雕刻有龙纹和云纹图案
     */
    public static final Block TEMPLE_CHINESE_RELIEF = registerBlock("temple_chinese_relief",
            new Block(FabricBlockSettings.copyOf(Blocks.RED_TERRACOTTA)
                .sounds(BlockSoundGroup.STONE)
                .strength(1.5f)));
    
    /**
     * 中国神殿柱子
     * 中国神殿的支撑柱，采用传统的圆柱设计，雕刻有盘龙图案
     */
    public static final Block TEMPLE_CHINESE_PILLAR = registerBlock("temple_chinese_pillar",
            new PillarBlock(FabricBlockSettings.copyOf(Blocks.RED_TERRACOTTA)
                .sounds(BlockSoundGroup.STONE)
                .strength(1.5f)));

    // ============ 亚特兰蒂斯神殿建筑系统 ============
    
    /**
     * 亚特兰蒂斯神殿砖
     * 亚特兰蒂斯神殿的基础建筑材料，采用青色海晶石风格，体现海洋文明特色
     */
    public static final Block TEMPLE_ATLANTEAN_BRICKS = registerBlock("temple_atlantean_bricks",
            new Block(FabricBlockSettings.copyOf(Blocks.PRISMARINE_BRICKS)
                .sounds(BlockSoundGroup.STONE)
                .strength(1.5f)
                .luminance(3)));
    
    /**
     * 亚特兰蒂斯神殿台阶
     * 亚特兰蒂斯神殿的台阶方块，具有轻微发光效果
     */
    public static final Block TEMPLE_ATLANTEAN_STAIRS = registerBlock("temple_atlantean_stairs",
            new StairsBlock(TEMPLE_ATLANTEAN_BRICKS.getDefaultState(),
                FabricBlockSettings.copyOf(Blocks.PRISMARINE_BRICK_STAIRS)
                    .sounds(BlockSoundGroup.STONE)
                    .strength(1.5f)
                    .luminance(3)));
    
    /**
     * 亚特兰蒂斯神殿半砖
     * 亚特兰蒂斯神殿的半砖方块，带有海洋光泽
     */
    public static final Block TEMPLE_ATLANTEAN_SLAB = registerBlock("temple_atlantean_slab",
            new SlabBlock(FabricBlockSettings.copyOf(Blocks.PRISMARINE_BRICK_SLAB)
                .sounds(BlockSoundGroup.STONE)
                .strength(1.5f)
                .luminance(3)));
    
    /**
     * 亚特兰蒂斯神殿浮雕
     * 亚特兰蒂斯神殿的装饰方块，雕刻有几何水纹和海洋符号
     */
    public static final Block TEMPLE_ATLANTEAN_RELIEF = registerBlock("temple_atlantean_relief",
            new Block(FabricBlockSettings.copyOf(Blocks.DARK_PRISMARINE)
                .sounds(BlockSoundGroup.STONE)
                .strength(1.5f)
                .luminance(5)));
    
    /**
     * 亚特兰蒂斯神殿柱子
     * 亚特兰蒂斯神殿的支撑柱，采用水晶柱式设计，散发神秘光芒
     */
    public static final Block TEMPLE_ATLANTEAN_PILLAR = registerBlock("temple_atlantean_pillar",
            new PillarBlock(FabricBlockSettings.copyOf(Blocks.PRISMARINE)
                .sounds(BlockSoundGroup.STONE)
                .strength(1.5f)
                .luminance(4)));

    // ============ 北欧神殿建筑系统 ============
    
    /**
     * 北欧神殿砖
     * 北欧神殿的基础建筑材料，采用深灰石砖风格，体现维京建筑特色
     */
    public static final Block TEMPLE_NORDIC_BRICKS = registerBlock("temple_nordic_bricks",
            new Block(FabricBlockSettings.copyOf(Blocks.COBBLESTONE)
                .sounds(BlockSoundGroup.STONE)
                .strength(2.0f)));
    
    /**
     * 北欧神殿台阶
     * 北欧神殿的台阶方块，采用粗犷的石质设计
     */
    public static final Block TEMPLE_NORDIC_STAIRS = registerBlock("temple_nordic_stairs",
            new StairsBlock(TEMPLE_NORDIC_BRICKS.getDefaultState(),
                FabricBlockSettings.copyOf(Blocks.COBBLESTONE_STAIRS)
                    .sounds(BlockSoundGroup.STONE)
                    .strength(2.0f)));
    
    /**
     * 北欧神殿半砖
     * 北欧神殿的半砖方块，保持粗犷的维京风格
     */
    public static final Block TEMPLE_NORDIC_SLAB = registerBlock("temple_nordic_slab",
            new SlabBlock(FabricBlockSettings.copyOf(Blocks.COBBLESTONE_SLAB)
                .sounds(BlockSoundGroup.STONE)
                .strength(2.0f)));
    
    /**
     * 北欧神殿浮雕
     * 北欧神殿的装饰方块，雕刻有卢恩符文和战士图案
     */
    public static final Block TEMPLE_NORDIC_RELIEF = registerBlock("temple_nordic_relief",
            new Block(FabricBlockSettings.copyOf(Blocks.MOSSY_COBBLESTONE)
                .sounds(BlockSoundGroup.STONE)
                .strength(2.0f)));
    
    /**
     * 北欧神殿柱子
     * 北欧神殿的支撑柱，采用坚固的石柱设计，雕刻有世界树纹样
     */
    public static final Block TEMPLE_NORDIC_PILLAR = registerBlock("temple_nordic_pillar",
            new PillarBlock(FabricBlockSettings.copyOf(Blocks.STONE)
                .sounds(BlockSoundGroup.STONE)
                .strength(2.0f)));

    // ============ 丰饶法术资源方块系统 ============
    
    /**
     * 一堆肉方块
     * 
     * 丰饶法术生成的食物资源方块，被破坏时掉落24-36个随机肉类。
     * 包含各种肉类：牛肉、猪肉、鸡肉、羊肉、兔肉、鱼类等。
     */
    public static final Block MEAT_PILE = registerBlock("meat_pile",
            new MeatPileBlock(FabricBlockSettings.copyOf(Blocks.DRIED_KELP_BLOCK)
                .sounds(BlockSoundGroup.WET_GRASS)
                .strength(0.5f)));
    
    /**
     * 一堆木头方块
     * 
     * 丰饶法术生成的木材资源方块，被破坏时掉落24-36个随机木材。
     * 包含各种木材：橡木、桦木、云杉木、丛林木、金合欢木、深色橡木等。
     */
    public static final Block WOOD_PILE = registerBlock("wood_pile",
            new WoodPileBlock(FabricBlockSettings.copyOf(Blocks.OAK_PLANKS)
                .sounds(BlockSoundGroup.WOOD)
                .strength(1.0f)));

    // ============ 丰饶祭坛多方块结构系统 ============
    
    /**
     * 丰饶祭坛墙壁(不带符文) - C
     */
    public static final Block ABUNDANCE_ALTAR_WALL = registerBlock("abundance_altar_wall",
            new Block(FabricBlockSettings.copyOf(Blocks.QUARTZ_BLOCK)
                .sounds(BlockSoundGroup.STONE)
                .strength(2.0f)));
    
    /**
     * 丰饶祭坛楼梯 - 用于屋顶结构
     */
    public static final Block ABUNDANCE_ALTAR_STAIRS = registerBlock("abundance_altar_stairs",
            new StairsBlock(ABUNDANCE_ALTAR_WALL.getDefaultState(), FabricBlockSettings.copyOf(Blocks.QUARTZ_STAIRS)
                .sounds(BlockSoundGroup.STONE)
                .strength(2.0f)));

    /**
     * 丰饶祭坛核心
     */
    public static final Block ABUNDANCE_ALTAR_CORE = registerBlock("abundance_altar_core",
            new AbundanceAltarCoreBlock(FabricBlockSettings.copyOf(Blocks.QUARTZ_BLOCK)
                .sounds(BlockSoundGroup.STONE)
                .strength(2.0f)
                .nonOpaque()));
    
    /**
     * 丰饶祭坛柱子方块
     * 
     * 丰饶祭坛多方块结构的装饰性组件，构成祭坛的整体外观。
     */
    public static final Block ABUNDANCE_ALTAR_PILLAR = registerBlock("abundance_altar_pillar",
            new Block(FabricBlockSettings.copyOf(Blocks.QUARTZ_PILLAR)
                .sounds(BlockSoundGroup.STONE)
                .nonOpaque()));

    // ============ 新丰饶祭坛方块系统 ============
    
    /**
     * 丰饶祭坛砖(上下前后带金线) - A
     */
    public static final Block ABUNDANCE_ALTAR_BRICK_GOLD_H = registerBlock("abundance_altar_brick_gold_h",
            new Block(FabricBlockSettings.copyOf(Blocks.QUARTZ_BRICKS)
                .sounds(BlockSoundGroup.STONE)
                .strength(2.0f)));
    
    /**
     * 丰饶祭坛砖(东西向金线) - 用于南北边缘
     */
    public static final Block ABUNDANCE_ALTAR_BRICK_GOLD_EW = registerBlock("abundance_altar_brick_gold_ew",
            new Block(FabricBlockSettings.copyOf(Blocks.QUARTZ_BRICKS)
                .sounds(BlockSoundGroup.STONE)
                .strength(2.0f)));
    
    /**
     * 丰饶祭坛砖(南北向金线) - 用于东西边缘
     */
    public static final Block ABUNDANCE_ALTAR_BRICK_GOLD_NS = registerBlock("abundance_altar_brick_gold_ns",
            new Block(FabricBlockSettings.copyOf(Blocks.QUARTZ_BRICKS)
                .sounds(BlockSoundGroup.STONE)
                .strength(2.0f)));
    
    /**
     * 丰饶祭坛墙壁(下方带符文) - D
     */
    public static final Block ABUNDANCE_ALTAR_WALL_RUNE_BOTTOM = registerBlock("abundance_altar_wall_rune_bottom",
            new Block(FabricBlockSettings.copyOf(Blocks.QUARTZ_BLOCK)
                .sounds(BlockSoundGroup.STONE)
                .strength(2.0f)
                .luminance(5)));
    
    /**
     * 丰饶祭坛砖(前后左右带金线) - E
     */
    public static final Block ABUNDANCE_ALTAR_BRICK_GOLD_V = registerBlock("abundance_altar_brick_gold_v",
            new Block(FabricBlockSettings.copyOf(Blocks.QUARTZ_BRICKS)
                .sounds(BlockSoundGroup.STONE)
                .strength(2.0f)));
    
    /**
     * 丰饶祭坛砖(不带金线) - F
     */
    public static final Block ABUNDANCE_ALTAR_BRICK = registerBlock("abundance_altar_brick",
            new Block(FabricBlockSettings.copyOf(Blocks.QUARTZ_BRICKS)
                .sounds(BlockSoundGroup.STONE)
                .strength(2.0f)));
    
    /**
     * 丰饶祭坛半砖 - G
     */
    public static final Block ABUNDANCE_ALTAR_SLAB = registerBlock("abundance_altar_slab",
            new SlabBlock(FabricBlockSettings.copyOf(Blocks.QUARTZ_SLAB)
                .sounds(BlockSoundGroup.STONE)
                .strength(2.0f)));
    
    /**
     * 储藏室(矿物) - H
     */
    public static final Block ABUNDANCE_STORAGE_MINERAL = registerBlock("abundance_storage_mineral",
            new Block(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK)
                .sounds(BlockSoundGroup.METAL)
                .strength(2.0f)
                .luminance(3)));
    
    /**
     * 储藏室(食物) - I
     */
    public static final Block ABUNDANCE_STORAGE_FOOD = registerBlock("abundance_storage_food",
            new Block(FabricBlockSettings.copyOf(Blocks.HAY_BLOCK)
                .sounds(BlockSoundGroup.GRASS)
                .strength(2.0f)
                .luminance(3)));
    
    /**
     * 储藏室(木材) - J
     */
    public static final Block ABUNDANCE_STORAGE_WOOD = registerBlock("abundance_storage_wood",
            new Block(FabricBlockSettings.copyOf(Blocks.OAK_PLANKS)
                .sounds(BlockSoundGroup.WOOD)
                .strength(2.0f)
                .luminance(3)));
    
    /**
     * 丰饶祭坛墙壁(上方带符文) - K
     */
    public static final Block ABUNDANCE_ALTAR_WALL_RUNE_TOP = registerBlock("abundance_altar_wall_rune_top",
            new Block(FabricBlockSettings.copyOf(Blocks.QUARTZ_BLOCK)
                .sounds(BlockSoundGroup.STONE)
                .strength(2.0f)
                .luminance(5)));

    /**
     * 丰饶祭坛砖(上下转角金线) - Z
     * 支持水平旋转，用于四个不同方向的转角
     */
    public static final Block ABUNDANCE_ALTAR_BRICK_GOLD_CORNER = registerBlock("abundance_altar_brick_gold_corner",
            new HorizontalFacingBlock(FabricBlockSettings.copyOf(Blocks.QUARTZ_BRICKS)
                .sounds(BlockSoundGroup.STONE)
                .strength(2.0f)) {
                @Override
                protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
                    builder.add(FACING);
                }
                
                @Override
                public BlockState getPlacementState(ItemPlacementContext ctx) {
                    return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
                }
            });

    // ============ 埃及神像基座系统 ============
    
    /**
     * 埃及神像基座方块
     * 
     * 埃及神殿中的神像基座，用于放置不同等级的神像，周期性产出神力证明。
     * 支持7个等级的神像：奴隶、市民、士兵、王后、国王、国王王后、法老。
     */
    public static final Block EGYPTIAN_STATUE_PEDESTAL = registerBlock("egyptian_statue_pedestal",
            new EgyptianStatuePedestalBlock(FabricBlockSettings.copyOf(Blocks.SANDSTONE)
                .sounds(BlockSoundGroup.STONE)
                .nonOpaque()));

    // ============ 法术效果方块系统 ============
    
    /**
     * 动物招引塔方块
     * 
     * 动物招引法术生成的多方块结构的核心方块，替代钻石块使用。
     * 负责周期性吸引动物和促进草地生长，持续5分钟后自动消失。
     */
    public static final Block ANIMAL_LURE_TOWER = registerBlock("animal_lure_tower",
            new AnimalLureTowerBlock(FabricBlockSettings.copyOf(Blocks.DIAMOND_BLOCK)
                .sounds(BlockSoundGroup.METAL)
                .luminance(10)
                .nonOpaque()));

    // ============ 多文明神殿系统（保留单一方块以便对比）============
    
    /**
     * 埃及神殿方块（单一方块版本）
     * 埃及神话村庄的宗教中心建筑，具有埃及风格的装饰和功能。
     */
    public static final Block TEMPLE_EGYPTIAN = registerBlock("temple_egyptian",
            new TempleBlock(FabricBlockSettings.copyOf(Blocks.SANDSTONE)
                .sounds(BlockSoundGroup.STONE)
                .nonOpaque(), GodPowerCertificateItem.MythologyType.EGYPTIAN));
    
    /**
     * 中国神殿方块（单一方块版本）
     * 中国神话村庄的宗教中心建筑，体现东方古典建筑风格。
     */
    public static final Block TEMPLE_CHINESE = registerBlock("temple_chinese",
            new TempleBlock(FabricBlockSettings.copyOf(Blocks.RED_TERRACOTTA)
                .sounds(BlockSoundGroup.STONE)
                .nonOpaque(), GodPowerCertificateItem.MythologyType.CHINESE));
    
    /**
     * 亚特兰蒂斯神殿方块（单一方块版本）
     * 亚特兰蒂斯神话村庄的宗教中心建筑，融合古代科技和海洋元素。
     */
    public static final Block TEMPLE_ATLANTEAN = registerBlock("temple_atlantean",
            new TempleBlock(FabricBlockSettings.copyOf(Blocks.PRISMARINE_BRICKS)
                .sounds(BlockSoundGroup.STONE)
                .nonOpaque(), GodPowerCertificateItem.MythologyType.ATLANTEAN));
    
    /**
     * 北欧神殿方块（单一方块版本）
     * 北欧神话村庄的宗教中心建筑，体现维京战士文化和阿萨神族风格。
     */
    public static final Block TEMPLE_NORDIC = registerBlock("temple_nordic",
            new TempleBlock(FabricBlockSettings.copyOf(Blocks.STONE_BRICKS)
                .sounds(BlockSoundGroup.STONE)
                .nonOpaque(), GodPowerCertificateItem.MythologyType.NORDIC));

    // ============ 神像多方块结构系统 ============
    
    /**
     * 希腊神像方块
     * 希腊神殿中的核心多方块结构，用于兑换希腊神明的神力法术书。
     * 接受希腊神力证明，提供相应的神力法术书兑换服务。
     */
    public static final Block STATUE_GREEK = registerBlock("statue_greek",
            new StatueBlock(FabricBlockSettings.copyOf(Blocks.STONE_BRICKS)
                .sounds(BlockSoundGroup.STONE)
                .nonOpaque(), GodPowerCertificateItem.MythologyType.GREEK));
    
    /**
     * 埃及神像方块
     * 埃及神殿中的核心多方块结构，用于兑换埃及神明的神力法术书。
     */
    public static final Block STATUE_EGYPTIAN = registerBlock("statue_egyptian",
            new StatueBlock(FabricBlockSettings.copyOf(Blocks.SANDSTONE)
                .sounds(BlockSoundGroup.STONE)
                .nonOpaque(), GodPowerCertificateItem.MythologyType.EGYPTIAN));
    
    /**
     * 中国神像方块
     * 中国神殿中的核心多方块结构，用于兑换中国神明的神力法术书。
     */
    public static final Block STATUE_CHINESE = registerBlock("statue_chinese",
            new StatueBlock(FabricBlockSettings.copyOf(Blocks.RED_TERRACOTTA)
                .sounds(BlockSoundGroup.STONE)
                .nonOpaque(), GodPowerCertificateItem.MythologyType.CHINESE));
    
    /**
     * 亚特兰蒂斯神像方块
     * 亚特兰蒂斯神殿中的核心多方块结构，用于兑换亚特兰蒂斯神力法术书。
     */
    public static final Block STATUE_ATLANTEAN = registerBlock("statue_atlantean",
            new StatueBlock(FabricBlockSettings.copyOf(Blocks.PRISMARINE_BRICKS)
                .sounds(BlockSoundGroup.STONE)
                .nonOpaque(), GodPowerCertificateItem.MythologyType.ATLANTEAN));
    
    /**
     * 北欧神像方块
     * 北欧神殿中的核心多方块结构，用于兑换北欧神明的神力法术书。
     */
    public static final Block STATUE_NORDIC = registerBlock("statue_nordic",
            new StatueBlock(FabricBlockSettings.copyOf(Blocks.STONE_BRICKS)
                .sounds(BlockSoundGroup.STONE)
                .nonOpaque(), GodPowerCertificateItem.MythologyType.NORDIC));

    // ============ 多方块神像结构各部分 ============
    
    // === 希腊神像多方块结构 ===
    
    /**
     * 希腊神像基座
     * 4层神像结构的最底层，展现古典希腊建筑风格
     */
    public static final Block STATUE_GREEK_BASE = registerBlock("statue_greek_base",
            new StatueBaseBlock(FabricBlockSettings.copyOf(Blocks.STONE_BRICKS)
                .sounds(BlockSoundGroup.STONE)
                .strength(2.0f)
                .nonOpaque(), GodPowerCertificateItem.MythologyType.GREEK));
    
    /**
     * 希腊神像腿部
     * 4层神像结构的第二层，连接基座与身体
     */
    public static final Block STATUE_GREEK_LEGS = registerBlock("statue_greek_legs",
            new Block(FabricBlockSettings.copyOf(Blocks.STONE_BRICKS)
                .sounds(BlockSoundGroup.STONE)
                .strength(2.0f)));
    
    /**
     * 希腊神像身体
     * 4层神像结构的第三层，展现宙斯威严身姿
     */
    public static final Block STATUE_GREEK_BODY = registerBlock("statue_greek_body",
            new Block(FabricBlockSettings.copyOf(Blocks.STONE_BRICKS)
                .sounds(BlockSoundGroup.STONE)
                .strength(2.0f)));
    
    /**
     * 希腊神像头部
     * 4层神像结构的最顶层，宙斯威严面容与王冠
     */
    public static final Block STATUE_GREEK_HEAD = registerBlock("statue_greek_head",
            new Block(FabricBlockSettings.copyOf(Blocks.STONE_BRICKS)
                .sounds(BlockSoundGroup.STONE)
                .strength(2.0f)));

    // === 埃及神像多方块结构 ===
    
    /**
     * 埃及神像基座
     * 金字塔式基座，体现法老王朝建筑风格
     */
    public static final Block STATUE_EGYPTIAN_BASE = registerBlock("statue_egyptian_base",
            new StatueBaseBlock(FabricBlockSettings.copyOf(Blocks.SANDSTONE)
                .sounds(BlockSoundGroup.STONE)
                .strength(2.0f)
                .nonOpaque(), GodPowerCertificateItem.MythologyType.EGYPTIAN));
    
    /**
     * 埃及神像腿部
     * 法老神像的腿部结构，砂岩质感
     */
    public static final Block STATUE_EGYPTIAN_LEGS = registerBlock("statue_egyptian_legs",
            new Block(FabricBlockSettings.copyOf(Blocks.SANDSTONE)
                .sounds(BlockSoundGroup.STONE)
                .strength(2.0f)));
    
    /**
     * 埃及神像身体
     * 法老神像的躯干，交叉手臂持权杖
     */
    public static final Block STATUE_EGYPTIAN_BODY = registerBlock("statue_egyptian_body",
            new Block(FabricBlockSettings.copyOf(Blocks.SANDSTONE)
                .sounds(BlockSoundGroup.STONE)
                .strength(2.0f)));
    
    /**
     * 埃及神像头部
     * 法老神像的头部，黄金面具与神圣头饰
     */
    public static final Block STATUE_EGYPTIAN_HEAD = registerBlock("statue_egyptian_head",
            new Block(FabricBlockSettings.copyOf(Blocks.SANDSTONE)
                .sounds(BlockSoundGroup.STONE)
                .strength(2.0f)));

    // === 中国神像多方块结构 ===
    
    /**
     * 中国神像基座
     * 八角形仙神基座，祥云环绕，太极八卦装饰
     */
    public static final Block STATUE_CHINESE_BASE = registerBlock("statue_chinese_base",
            new StatueBaseBlock(FabricBlockSettings.copyOf(Blocks.RED_TERRACOTTA)
                .sounds(BlockSoundGroup.STONE)
                .strength(2.0f)
                .nonOpaque(), GodPowerCertificateItem.MythologyType.CHINESE));
    
    /**
     * 中国神像腿部
     * 仙神神像的腿部，飘逸道袍下摆
     */
    public static final Block STATUE_CHINESE_LEGS = registerBlock("statue_chinese_legs",
            new Block(FabricBlockSettings.copyOf(Blocks.RED_TERRACOTTA)
                .sounds(BlockSoundGroup.STONE)
                .strength(2.0f)));
    
    /**
     * 中国神像身体
     * 仙神神像的躯干，宽袖仙袍飘逸姿态
     */
    public static final Block STATUE_CHINESE_BODY = registerBlock("statue_chinese_body",
            new Block(FabricBlockSettings.copyOf(Blocks.RED_TERRACOTTA)
                .sounds(BlockSoundGroup.STONE)
                .strength(2.0f)));
    
    /**
     * 中国神像头部
     * 仙神神像的头部，东方神仙面容
     */
    public static final Block STATUE_CHINESE_HEAD = registerBlock("statue_chinese_head",
            new Block(FabricBlockSettings.copyOf(Blocks.RED_TERRACOTTA)
                .sounds(BlockSoundGroup.STONE)
                .strength(2.0f)));

    // === 亚特兰蒂斯神像多方块结构 ===
    
    /**
     * 亚特兰蒂斯神像基座
     * 海洋风格基座，体现深海文明美学
     */
    public static final Block STATUE_ATLANTEAN_BASE = registerBlock("statue_atlantean_base",
            new StatueBaseBlock(FabricBlockSettings.copyOf(Blocks.PRISMARINE_BRICKS)
                .sounds(BlockSoundGroup.STONE)
                .strength(2.0f)
                .nonOpaque(), GodPowerCertificateItem.MythologyType.ATLANTEAN));
    
    /**
     * 亚特兰蒂斯神像腿部
     * 海神神像的腿部，波浪般的流线造型
     */
    public static final Block STATUE_ATLANTEAN_LEGS = registerBlock("statue_atlantean_legs",
            new Block(FabricBlockSettings.copyOf(Blocks.PRISMARINE_BRICKS)
                .sounds(BlockSoundGroup.STONE)
                .strength(2.0f)));
    
    /**
     * 亚特兰蒂斯神像身体
     * 海神神像的躯干，手持三叉戟
     */
    public static final Block STATUE_ATLANTEAN_BODY = registerBlock("statue_atlantean_body",
            new Block(FabricBlockSettings.copyOf(Blocks.PRISMARINE_BRICKS)
                .sounds(BlockSoundGroup.STONE)
                .strength(2.0f)));
    
    /**
     * 亚特兰蒂斯神像头部
     * 海神神像的头部，珊瑚王冠装饰
     */
    public static final Block STATUE_ATLANTEAN_HEAD = registerBlock("statue_atlantean_head",
            new Block(FabricBlockSettings.copyOf(Blocks.PRISMARINE_BRICKS)
                .sounds(BlockSoundGroup.STONE)
                .strength(2.0f)));

    // === 北欧神像多方块结构 ===
    
    /**
     * 北欧神像基座
     * 维京风格基座，符文装饰与战士图腾
     */
    public static final Block STATUE_NORDIC_BASE = registerBlock("statue_nordic_base",
            new StatueBaseBlock(FabricBlockSettings.copyOf(Blocks.STONE_BRICKS)
                .sounds(BlockSoundGroup.STONE)
                .strength(2.0f)
                .nonOpaque(), GodPowerCertificateItem.MythologyType.NORDIC));
    
    /**
     * 北欧神像腿部
     * 战神神像的腿部，厚重战甲覆盖
     */
    public static final Block STATUE_NORDIC_LEGS = registerBlock("statue_nordic_legs",
            new Block(FabricBlockSettings.copyOf(Blocks.STONE_BRICKS)
                .sounds(BlockSoundGroup.STONE)
                .strength(2.0f)));
    
    /**
     * 北欧神像身体
     * 战神神像的躯干，手持战锤雷神之锤
     */
    public static final Block STATUE_NORDIC_BODY = registerBlock("statue_nordic_body",
            new Block(FabricBlockSettings.copyOf(Blocks.STONE_BRICKS)
                .sounds(BlockSoundGroup.STONE)
                .strength(2.0f)));
    
    /**
     * 北欧神像头部
     * 战神神像的头部，独眼奥丁威严面容
     */
    public static final Block STATUE_NORDIC_HEAD = registerBlock("statue_nordic_head",
            new Block(FabricBlockSettings.copyOf(Blocks.STONE_BRICKS)
                .sounds(BlockSoundGroup.STONE)
                .strength(2.0f)));

    /**
     * 方块注册辅助方法
     * 
     * 将方块注册到游戏中，并自动创建对应的物品形式。
     * 这确保方块既可以在世界中放置，也可以在背包中持有。
     * 
     * @param name 方块的注册名称，必须是小写字母和下划线
     * @param block 要注册的方块实例
     * @return 注册后的方块实例
     */
    private static Block registerBlock(String name, Block block) {
        registerBlockItem(name, block);
        return Registry.register(Registries.BLOCK, new Identifier(MythicAgeMod.MOD_ID, name), block);
    }

    /**
     * 方块物品注册辅助方法
     * 
     * 为方块创建对应的物品形式，使其可以在背包中存储和在创造模式中获取。
     * 使用标准的FabricItemSettings确保兼容性。
     * 
     * @param name 物品的注册名称，与方块名称相同
     * @param block 对应的方块实例
     * @return 注册后的方块物品实例
     */
    private static Item registerBlockItem(String name, Block block) {
        return Registry.register(Registries.ITEM, new Identifier(MythicAgeMod.MOD_ID, name),
                new BlockItem(block, new FabricItemSettings()));
    }

    /**
     * 方块注册主方法
     * 
     * 在Mod初始化时调用，确保所有自定义方块被正确注册到游戏中。
     * 由于使用了静态初始化器，方块在类加载时就会自动注册。
     * 
     * 注册的方块：
     * - 圣物基座：神殿系统的核心组件
     * 
     * 每个方块都会自动获得：
     * - 方块注册表条目
     * - 对应的物品形式
     * - 创造模式标签页中的位置
     */
    public static void registerModBlocks() {
        MythicAgeMod.LOGGER.info("Registering Mod Blocks for " + MythicAgeMod.MOD_ID);
        MythicAgeMod.LOGGER.info("Successfully registered 11 custom blocks: " +
            "1 Relic Pedestal, 5 Temple blocks, and 5 Statue blocks");
    }
}

