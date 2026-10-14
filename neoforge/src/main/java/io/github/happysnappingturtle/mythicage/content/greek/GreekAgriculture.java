package io.github.happysnappingturtle.mythicage.content.greek;

import io.github.happysnappingturtle.mythicage.content.BlockModelSpec;
import io.github.happysnappingturtle.mythicage.content.Content;
import io.github.happysnappingturtle.mythicage.content.LootSpec;
import io.github.happysnappingturtle.mythicage.content.ModTags;
import io.github.happysnappingturtle.mythicage.content.Recolor;
import io.github.happysnappingturtle.mythicage.content.greek.agri.GrapeTrellisBlock;
import io.github.happysnappingturtle.mythicage.content.greek.agri.OilLampBlock;
import io.github.happysnappingturtle.mythicage.content.greek.agri.OliveLeavesBlock;
import io.github.happysnappingturtle.mythicage.content.greek.agri.OlivePressBlock;
import io.github.happysnappingturtle.mythicage.content.greek.agri.OlivePressBlockEntity;
import io.github.happysnappingturtle.mythicage.content.greek.agri.OlivePressMenu;
import io.github.happysnappingturtle.mythicage.content.greek.agri.WildGrapevineBlock;
import io.github.happysnappingturtle.mythicage.content.wood.ModBoats;
import io.github.happysnappingturtle.mythicage.content.wood.ModWoodTypes;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.registry.ModRegistries;
import io.github.happysnappingturtle.mythicage.worldgen.OliveTreeFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.List;
import java.util.Optional;

import static io.github.happysnappingturtle.mythicage.core.player.Civilization.GREEK;

/**
 * 希腊农业：橄榄树与橄榄木、葡萄、橄榄压榨器、陶油灯（T6b 设计文档 §2–§7）。声明顺序即创造模式物品栏顺序。
 * 木材族中照样板的形态（台阶、楼梯、栅栏、栅栏门、门、活板门、按钮、压力板）在 GreekWood 中声明（codex 按交接单编写）。
 */
public final class GreekAgriculture {
    public static final TagKey<Block> OLIVE_LOGS = ModTags.block("olive_logs");
    public static final TagKey<Item> OLIVE_LOGS_ITEM = ModTags.item("olive_logs");

    // ---- 世界生成用的树形 Feature 与 configured feature 键（树苗共用） ----

    public static final DeferredHolder<net.minecraft.world.level.levelgen.feature.Feature<?>, OliveTreeFeature> OLIVE_TREE_FEATURE =
            ModRegistries.FEATURES.register("olive_tree", OliveTreeFeature::new);
    public static final ResourceKey<ConfiguredFeature<?, ?>> OLIVE_TREE_CONFIGURED = ResourceKey.create(
            Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "olive_tree"));
    public static final TreeGrower OLIVE_GROWER = new TreeGrower("mythic_age:olive", Optional.empty(),
            Optional.of(OLIVE_TREE_CONFIGURED), Optional.empty());

    private static ResourceLocation mod(String path) {
        return ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, path);
    }

    private static final ResourceLocation LOG_SIDE = Recolor.sprite("minecraft:block/spruce_log", "olive_log");
    private static final ResourceLocation LOG_TOP = Recolor.sprite("minecraft:block/acacia_log_top", "olive_log_top");
    private static final ResourceLocation STRIPPED_SIDE =
            Recolor.sprite("minecraft:block/stripped_acacia_log", "stripped_olive_log");
    private static final ResourceLocation STRIPPED_TOP =
            Recolor.sprite("minecraft:block/stripped_acacia_log_top", "stripped_olive_log_top");
    public static final ResourceLocation PLANKS_TEXTURE = Recolor.sprite("minecraft:block/jungle_planks", "olive_planks");

    // ---- 橄榄木（原木、木头、去皮、木板；A 类换色） ----

    public static final DeferredBlock<RotatedPillarBlock> OLIVE_LOG = wood("olive_log", LOG_SIDE, LOG_TOP,
            "橄榄原木", "Olive Log", "オリーブの原木");
    public static final DeferredBlock<RotatedPillarBlock> OLIVE_WOOD = wood("olive_wood", LOG_SIDE, LOG_SIDE,
            "橄榄木", "Olive Wood", "オリーブの木");
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_OLIVE_LOG = wood("stripped_olive_log", STRIPPED_SIDE,
            STRIPPED_TOP, "去皮橄榄原木", "Stripped Olive Log", "樹皮を剥いだオリーブの原木");
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_OLIVE_WOOD = wood("stripped_olive_wood",
            STRIPPED_SIDE, STRIPPED_SIDE, "去皮橄榄木", "Stripped Olive Wood", "樹皮を剥いだオリーブの木");

    public static final DeferredBlock<Block> OLIVE_PLANKS = Content.block("olive_planks", GREEK)
            .properties(() -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).mapColor(MapColor.SAND))
            .texture(PLANKS_TEXTURE)
            .tags(BlockTags.PLANKS, BlockTags.MINEABLE_WITH_AXE)
            .itemTags(ItemTags.PLANKS)
            .lang("橄榄木板", "Olive Planks", "オリーブの板材")
            .register();

    // ---- 橄榄树：树叶（结果）、树苗、盆栽（C 类手绘） ----

    public static final DeferredBlock<OliveLeavesBlock> OLIVE_LEAVES = Content.block("olive_leaves", GREEK,
                    OliveLeavesBlock::new)
            .properties(() -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES))
            .model(new BlockModelSpec.Leaves(mod("block/olive_leaves"), OliveLeavesBlock.FRUIT,
                    java.util.Arrays.asList(null, mod("block/olive_leaves_fruit_unripe"),
                            mod("block/olive_leaves_fruit_ripe"))))
            .loot(LootSpec.custom(l -> l.fruitLeaves(GreekAgriculture.OLIVE_LEAVES.get(),
                    GreekAgriculture.OLIVE_SAPLING.get(), OliveLeavesBlock.FRUIT, OliveLeavesBlock.RIPE,
                    GreekAgriculture.OLIVE.get())))
            .tags(BlockTags.LEAVES, BlockTags.MINEABLE_WITH_HOE)
            .itemTags(ItemTags.LEAVES)
            .lang("橄榄树叶", "Olive Leaves", "オリーブの葉")
            .register();

    public static final DeferredBlock<SaplingBlock> OLIVE_SAPLING = Content.block("olive_sapling", GREEK,
                    p -> new SaplingBlock(OLIVE_GROWER, p))
            .properties(() -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING))
            .model(BlockModelSpec.cross(mod("block/olive_sapling")))
            .itemTexture(mod("block/olive_sapling"))
            .tags(BlockTags.SAPLINGS)
            .itemTags(ItemTags.SAPLINGS)
            .lang("橄榄树苗", "Olive Sapling", "オリーブの苗木")
            .register();

    public static final DeferredBlock<FlowerPotBlock> POTTED_OLIVE_SAPLING = Content.block("potted_olive_sapling",
                    GREEK, p -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT,
                            () -> GreekAgriculture.OLIVE_SAPLING.get(), p))
            .properties(() -> BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_OAK_SAPLING))
            .model(new BlockModelSpec.Potted(mod("block/olive_sapling")))
            .loot(LootSpec.potted(() -> GreekAgriculture.OLIVE_SAPLING.get()))
            .noItem()
            .tags(BlockTags.FLOWER_POTS)
            .lang("橄榄树苗盆栽", "Potted Olive Sapling", "オリーブの苗木の鉢植え")
            .register();

    // ---- 告示牌（A 类换色，signs 图集）与船 ----

    public static final DeferredBlock<StandingSignBlock> OLIVE_SIGN = Content.block("olive_sign", GREEK,
                    p -> new StandingSignBlock(ModWoodTypes.OLIVE, p))
            .properties(() -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SIGN))
            .model(new BlockModelSpec.Sign(PLANKS_TEXTURE, () -> GreekAgriculture.OLIVE_WALL_SIGN.get()))
            .item((b, p) -> new SignItem(p.stacksTo(16), b, GreekAgriculture.OLIVE_WALL_SIGN.get()))
            .itemTexture(Recolor.sprite("minecraft:item/oak_sign", "olive_sign"))
            .tags(BlockTags.STANDING_SIGNS)
            .itemTags(ItemTags.SIGNS)
            .lang("橄榄木告示牌", "Olive Sign", "オリーブの看板")
            .register();

    public static final DeferredBlock<WallSignBlock> OLIVE_WALL_SIGN = Content.block("olive_wall_sign", GREEK,
                    p -> new WallSignBlock(ModWoodTypes.OLIVE, p))
            .properties(() -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WALL_SIGN))
            .model(new BlockModelSpec.GeneratedBy("olive_sign"))
            .loot(LootSpec.other(() -> GreekAgriculture.OLIVE_SIGN.get()))
            .noItem()
            .tags(BlockTags.WALL_SIGNS)
            .lang("墙上的橄榄木告示牌", "Olive Wall Sign", "壁に付けたオリーブの看板")
            .register();

    public static final DeferredBlock<CeilingHangingSignBlock> OLIVE_HANGING_SIGN = Content.block(
                    "olive_hanging_sign", GREEK, p -> new CeilingHangingSignBlock(ModWoodTypes.OLIVE, p))
            .properties(() -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_HANGING_SIGN))
            .model(new BlockModelSpec.HangingSign(STRIPPED_SIDE, () -> GreekAgriculture.OLIVE_WALL_HANGING_SIGN.get()))
            .item((b, p) -> new HangingSignItem(b, GreekAgriculture.OLIVE_WALL_HANGING_SIGN.get(), p.stacksTo(16)))
            .itemTexture(Recolor.sprite("minecraft:item/oak_hanging_sign", "olive_hanging_sign"))
            .tags(BlockTags.CEILING_HANGING_SIGNS)
            .itemTags(ItemTags.HANGING_SIGNS)
            .lang("悬挂式橄榄木告示牌", "Olive Hanging Sign", "オリーブの吊り看板")
            .register();

    public static final DeferredBlock<WallHangingSignBlock> OLIVE_WALL_HANGING_SIGN = Content.block(
                    "olive_wall_hanging_sign", GREEK, p -> new WallHangingSignBlock(ModWoodTypes.OLIVE, p))
            .properties(() -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WALL_HANGING_SIGN))
            .model(new BlockModelSpec.GeneratedBy("olive_hanging_sign"))
            .loot(LootSpec.other(() -> GreekAgriculture.OLIVE_HANGING_SIGN.get()))
            .noItem()
            .tags(BlockTags.WALL_HANGING_SIGNS)
            .lang("墙上的悬挂式橄榄木告示牌", "Olive Wall Hanging Sign", "壁に付けたオリーブの吊り看板")
            .register();

    public static final DeferredItem<BoatItem> OLIVE_BOAT = Content.item("olive_boat", GREEK,
                    p -> new BoatItem(false, ModBoats.olive(), p.stacksTo(1)))
            .texture(Recolor.sprite("minecraft:item/oak_boat", "olive_boat"))
            .tags(ItemTags.BOATS)
            .lang("橄榄木船", "Olive Boat", "オリーブのボート")
            .register();

    public static final DeferredItem<BoatItem> OLIVE_CHEST_BOAT = Content.item("olive_chest_boat", GREEK,
                    p -> new BoatItem(true, ModBoats.olive(), p.stacksTo(1)))
            .texture(Recolor.sprite("minecraft:item/oak_chest_boat", "olive_chest_boat"))
            .tags(ItemTags.CHEST_BOATS)
            .lang("橄榄木运输船", "Olive Boat with Chest", "チェスト付きオリーブのボート")
            .register();

    // ---- 橄榄、葡萄（C 类手绘） ----

    public static final DeferredItem<Item> OLIVE = Content.item("olive", GREEK)
            .tags(ModTags.cItem("crops/olive"), ModTags.cItem("crops"))
            .lang("橄榄", "Olive", "オリーブの実")
            .register();

    public static final DeferredItem<Item> GRAPE = Content.item("grape", GREEK)
            .properties(p -> p.food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3f).build()))
            .tags(ModTags.cItem("crops/grape"), ModTags.cItem("crops"), ModTags.cItem("foods/berry"))
            .lang("葡萄", "Grapes", "ブドウ")
            .register();

    public static final DeferredItem<Item> GRAPE_CUTTING = Content.item("grape_cutting", GREEK)
            .tags(ModTags.cItem("seeds"))
            .lang("葡萄插条", "Grape Cutting", "ブドウの挿し木")
            .register();

    public static final DeferredBlock<WildGrapevineBlock> WILD_GRAPEVINE = Content.block("wild_grapevine", GREEK,
                    WildGrapevineBlock::new)
            .properties(() -> BlockBehaviour.Properties.ofFullCopy(Blocks.SWEET_BERRY_BUSH).randomTicks()
                    .noCollission().instabreak())
            .model(BlockModelSpec.cross(mod("block/wild_grapevine")))
            .itemTexture(mod("block/wild_grapevine"))
            .loot(LootSpec.custom(l -> l.bush(GreekAgriculture.WILD_GRAPEVINE.get(),
                    GreekAgriculture.GRAPE_CUTTING.get(), 1, 2, GreekAgriculture.GRAPE.get(), 1, 3)))
            .tags(BlockTags.SWORD_EFFICIENT, BlockTags.MINEABLE_WITH_HOE)
            .lang("野葡萄藤", "Wild Grapevine", "野生のブドウの蔓")
            .register();

    public static final DeferredBlock<GrapeTrellisBlock> GRAPE_TRELLIS = Content.block("grape_trellis", GREEK,
                    GrapeTrellisBlock::new)
            .properties(() -> BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(0.6f)
                    .sound(net.minecraft.world.level.block.SoundType.WOOD).noOcclusion().randomTicks()
                    .ignitedByLava().pushReaction(PushReaction.DESTROY))
            .model(new BlockModelSpec.Trellis())
            .itemTexture(mod("item/grape_trellis"))
            .loot(LootSpec.custom(l -> l.trellis(GreekAgriculture.GRAPE_TRELLIS.get(), GrapeTrellisBlock.PART,
                    GrapeTrellisBlock.Part.BOTTOM, GrapeTrellisBlock.STAGE, GrapeTrellisBlock.MATURE,
                    GreekAgriculture.GRAPE_CUTTING.get(), GreekAgriculture.GRAPE.get(), 2, 3)))
            .tags(BlockTags.MINEABLE_WITH_AXE)
            .lang("葡萄架", "Grape Trellis", "ブドウ棚")
            .register();

    // ---- 橄榄压榨器、橄榄油、陶油灯（C 类） ----

    public static final DeferredBlock<OlivePressBlock> OLIVE_PRESS = Content.block("olive_press", GREEK,
                    OlivePressBlock::new)
            .properties(() -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).mapColor(MapColor.QUARTZ)
                    .noOcclusion())
            .model(new BlockModelSpec.FacingToggle(OlivePressBlock.PRESSING, "pressing"))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE)
            .lang("橄榄压榨器", "Olive Press", "オリーブ圧搾機")
            .register();

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OlivePressBlockEntity>> OLIVE_PRESS_BE =
            ModRegistries.BLOCK_ENTITY_TYPES.register("olive_press",
                    () -> BlockEntityType.Builder.of(OlivePressBlockEntity::new, OLIVE_PRESS.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<OlivePressMenu>> OLIVE_PRESS_MENU =
            ModRegistries.MENU_TYPES.register("olive_press",
                    () -> new MenuType<>(OlivePressMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredItem<Item> OLIVE_OIL = Content.item("olive_oil", GREEK)
            .properties(p -> p.stacksTo(16))
            .tags(ModTags.cItem("foods/edible_oil"))
            .lang("橄榄油", "Olive Oil", "オリーブオイル")
            .register();

    public static final DeferredBlock<OilLampBlock> GREEK_OIL_LAMP = Content.block("greek_oil_lamp", GREEK,
                    OilLampBlock::new)
            .properties(() -> BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE).strength(0.3f)
                    .sound(net.minecraft.world.level.block.SoundType.DECORATED_POT).lightLevel(s -> 12)
                    .noOcclusion().pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY))
            .model(new BlockModelSpec.FacingManual())
            .itemTexture(mod("item/greek_oil_lamp"))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE)
            .lang("希腊陶油灯", "Greek Oil Lamp", "ギリシャの陶製オイルランプ")
            .register();

    private GreekAgriculture() {
    }

    private static DeferredBlock<RotatedPillarBlock> wood(String id, ResourceLocation side, ResourceLocation end,
                                                          String zh, String en, String ja) {
        return Content.block(id, GREEK, RotatedPillarBlock::new)
                .properties(() -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG).mapColor(MapColor.TERRACOTTA_GRAY))
                .model(new BlockModelSpec.Log(side, end))
                .tags(OLIVE_LOGS, BlockTags.LOGS_THAT_BURN, BlockTags.MINEABLE_WITH_AXE)
                .itemTags(OLIVE_LOGS_ITEM, ItemTags.LOGS_THAT_BURN)
                .lang(zh, en, ja)
                .register();
    }

    /** 用于世界生成数据：配置化的树形。 */
    public static ConfiguredFeature<?, ?> oliveTreeConfigured() {
        return new ConfiguredFeature<>(OLIVE_TREE_FEATURE.get(), NoneFeatureConfiguration.INSTANCE);
    }

    /** 斧头去皮的对应关系。 */
    public static List<java.util.Map.Entry<Block, Block>> strippables() {
        return List.of(java.util.Map.entry(OLIVE_LOG.get(), STRIPPED_OLIVE_LOG.get()),
                java.util.Map.entry(OLIVE_WOOD.get(), STRIPPED_OLIVE_WOOD.get()));
    }

    public static void init() {
        ModWoodTypes.init();
    }
}
