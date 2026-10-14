package io.github.happysnappingturtle.mythicage.content.greek;

import io.github.happysnappingturtle.mythicage.content.BlockModelSpec;
import io.github.happysnappingturtle.mythicage.content.Content;
import io.github.happysnappingturtle.mythicage.content.LootSpec;
import io.github.happysnappingturtle.mythicage.content.ModTags;
import io.github.happysnappingturtle.mythicage.content.Recolor;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.function.Function;

import static io.github.happysnappingturtle.mythicage.core.player.Civilization.GREEK;

/**
 * 希腊基础材料（08 号 §2；T6 设计文档 §6；《美术资产分类与希腊建筑套件设计》§4、§5.2）。
 * 声明顺序即创造模式物品栏顺序。贴图分三类：
 * <ul>
 *   <li>A 原版造型换色：{@code .texture(Recolor.sprite(...))}，规格为 tools/texgen/specs/greek/ 中的 recolor；</li>
 *   <li>B/C 手绘：不写 texture，贴图为 {@code mythic_age:block|item/<id>}，规格为同目录的 pixels 等模板。</li>
 * </ul>
 */
public final class GreekMaterials {
    /** 大理石族共用的方块标签与物品标签（T8a 神庙判定「大理石地面」会用到）。 */
    public static final TagKey<Block> MARBLE_FAMILY = ModTags.block("marble_family");
    public static final TagKey<Item> MARBLE_FAMILY_ITEM = ModTags.item("marble_family");

    // ---- 锡（远古）：锡石矿 → 粗锡 → 锡锭（A 类） ----

    public static final DeferredBlock<Block> TIN_ORE = Content.block("tin_ore", GREEK)
            .properties(() -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE))
            .texture(Recolor.sprite("minecraft:block/iron_ore", "tin_ore"))
            .loot(LootSpec.ore(() -> GreekMaterials.RAW_TIN.get()))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_IRON_TOOL, Tags.Blocks.ORES,
                    ModTags.cBlock("ores/tin"), Tags.Blocks.ORES_IN_GROUND_STONE, Tags.Blocks.ORE_RATES_SINGULAR)
            .itemTags(Tags.Items.ORES, ModTags.cItem("ores/tin"), Tags.Items.ORES_IN_GROUND_STONE,
                    Tags.Items.ORE_RATES_SINGULAR)
            .lang("锡石矿", "Tin Ore", "錫鉱石")
            .register();

    public static final DeferredItem<Item> RAW_TIN = Content.item("raw_tin", GREEK)
            .texture(Recolor.sprite("minecraft:item/raw_iron", "raw_tin"))
            .tags(Tags.Items.RAW_MATERIALS, ModTags.cItem("raw_materials/tin"))
            .lang("粗锡", "Raw Tin", "錫の原石")
            .register();

    public static final DeferredItem<Item> TIN_INGOT = Content.item("tin_ingot", GREEK)
            .texture(Recolor.sprite("minecraft:item/iron_ingot", "tin_ingot"))
            .tags(Tags.Items.INGOTS, ModTags.cItem("ingots/tin"))
            .lang("锡锭", "Tin Ingot", "錫インゴット")
            .register();

    // ---- 青铜（古典解锁）：铜锭 3 + 锡锭 1 → 青铜混合物 4 → 熔炼为青铜锭 ----

    /** B 类：「块 + 粉末」——一小堆铜色粉末，旁边一颗铜粒、一颗锡粒。 */
    public static final DeferredItem<Item> BRONZE_MIX = Content.item("bronze_mix", GREEK)
            .lang("青铜混合物", "Bronze Mix", "青銅の混合物")
            .register();

    public static final DeferredItem<Item> BRONZE_INGOT = Content.item("bronze_ingot", GREEK)
            .texture(Recolor.sprite("minecraft:item/iron_ingot", "bronze_ingot"))
            .tags(Tags.Items.INGOTS, ModTags.cItem("ingots/bronze"))
            .lang("青铜锭", "Bronze Ingot", "青銅インゴット")
            .register();

    // ---- 陶器与器物（远古，C 类） ----

    public static final DeferredItem<Item> RAW_ROOF_TILE = Content.item("raw_roof_tile", GREEK)
            .lang("陶坯构件", "Raw Roof Tile", "素焼き前の瓦")
            .register();

    public static final DeferredItem<Item> CERAMIC_COMPONENT = Content.item("ceramic_component", GREEK)
            .lang("陶制构件", "Ceramic Component", "陶器部材")
            .register();

    /** 奠酒：细颈陶酒壶（oinochoe）。用于皈依与晋升，按用途消耗。 */
    public static final DeferredItem<Item> LIBATION = Content.item("libation", GREEK)
            .properties(p -> p.stacksTo(16))
            .lang("奠酒", "Libation", "献酒")
            .register();

    /** 木盘：葡萄果盘的容器，只作容器，不能放置。 */
    public static final DeferredItem<Item> WOODEN_PLATTER = Content.item("wooden_platter", GREEK)
            .lang("木盘", "Wooden Platter", "木の皿")
            .register();

    // ---- 希腊大理石建筑套件（C 类，设计 §5.2 的 T6 部分） ----

    /** 原石：天然面，世界生成于希腊地域。 */
    public static final DeferredBlock<Block> MARBLE = Content.block("marble", GREEK)
            .properties(() -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).mapColor(MapColor.QUARTZ))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE, ModTags.block("marble"), MARBLE_FAMILY)
            .itemTags(ModTags.item("marble"), MARBLE_FAMILY_ITEM)
            .lang("希腊大理石", "Greek Marble", "ギリシャ大理石")
            .register();

    public static final DeferredBlock<SlabBlock> MARBLE_SLAB = shape("marble_slab", SlabBlock::new,
            BlockModelSpec.slab(MARBLE), "大理石台阶", "Marble Slab", "大理石のハーフブロック");

    public static final DeferredBlock<StairBlock> MARBLE_STAIRS = shape("marble_stairs",
            p -> new StairBlock(MARBLE.get().defaultBlockState(), p), BlockModelSpec.stairs(MARBLE),
            "大理石楼梯", "Marble Stairs", "大理石の階段");

    public static final DeferredBlock<WallBlock> MARBLE_WALL = shape("marble_wall", WallBlock::new,
            BlockModelSpec.wall(MARBLE), "大理石墙", "Marble Wall", "大理石の塀");

    /** 平滑大理石：原石烧制所得，干净的整块石板面，城镇主墙面与地面。 */
    public static final DeferredBlock<Block> SMOOTH_MARBLE = cube("smooth_marble",
            "平滑大理石", "Smooth Marble", "なめらかな大理石");

    /** 平滑大理石台阶：侧面用专用贴图，表现两块石板的叠缝。 */
    public static final DeferredBlock<SlabBlock> SMOOTH_MARBLE_SLAB = shape("smooth_marble_slab", SlabBlock::new,
            BlockModelSpec.slab(SMOOTH_MARBLE, modBlock("smooth_marble_slab_side")),
            "平滑大理石台阶", "Smooth Marble Slab", "なめらかな大理石のハーフブロック");

    /** 大理石方石：顺砌的等高长条石，错缝；神庙墙体与正式建筑。 */
    public static final DeferredBlock<Block> MARBLE_ASHLAR = cube("marble_ashlar",
            "大理石方石", "Marble Ashlar", "大理石の切石");

    public static final DeferredBlock<SlabBlock> MARBLE_ASHLAR_SLAB = shape("marble_ashlar_slab", SlabBlock::new,
            BlockModelSpec.slab(MARBLE_ASHLAR), "大理石方石台阶", "Marble Ashlar Slab", "大理石の切石のハーフブロック");

    public static final DeferredBlock<StairBlock> MARBLE_ASHLAR_STAIRS = shape("marble_ashlar_stairs",
            p -> new StairBlock(MARBLE_ASHLAR.get().defaultBlockState(), p), BlockModelSpec.stairs(MARBLE_ASHLAR),
            "大理石方石楼梯", "Marble Ashlar Stairs", "大理石の切石の階段");

    public static final DeferredBlock<WallBlock> MARBLE_ASHLAR_WALL = shape("marble_ashlar_wall", WallBlock::new,
            BlockModelSpec.wall(MARBLE_ASHLAR), "大理石方石墙", "Marble Ashlar Wall", "大理石の切石の塀");

    /** 多边形砌石：不规则多边形石块紧密拼合（德尔斐、迈锡尼式）；卫城基座、挡土墙。 */
    public static final DeferredBlock<Block> POLYGONAL_MARBLE = cube("polygonal_marble",
            "多边形砌石", "Polygonal Marble Masonry", "大理石の多角形積み");

    public static final DeferredBlock<WallBlock> POLYGONAL_MARBLE_WALL = shape("polygonal_marble_wall",
            WallBlock::new, BlockModelSpec.wall(POLYGONAL_MARBLE),
            "多边形砌石墙", "Polygonal Marble Wall", "大理石の多角形積みの塀");

    /** 磨光大理石：光洁面与柔和纹理；室内地面、台面、祭坛平台。 */
    public static final DeferredBlock<Block> POLISHED_MARBLE = cube("polished_marble",
            "磨光大理石", "Polished Marble", "磨かれた大理石");

    public static final DeferredBlock<SlabBlock> POLISHED_MARBLE_SLAB = shape("polished_marble_slab",
            SlabBlock::new, BlockModelSpec.slab(POLISHED_MARBLE),
            "磨光大理石台阶", "Polished Marble Slab", "磨かれた大理石のハーフブロック");

    public static final DeferredBlock<StairBlock> POLISHED_MARBLE_STAIRS = shape("polished_marble_stairs",
            p -> new StairBlock(POLISHED_MARBLE.get().defaultBlockState(), p), BlockModelSpec.stairs(POLISHED_MARBLE),
            "磨光大理石楼梯", "Polished Marble Stairs", "磨かれた大理石の階段");

    /** 回纹饰带：侧面为连续回纹，上下为平滑大理石；墙腰线、檐下饰带。原物有彩绘，分爱琴蓝与赤陶红两色。 */
    public static final DeferredBlock<Block> BLUE_MEANDER_MARBLE = frieze("blue_meander_marble",
            "回纹饰带（爱琴蓝）", "Aegean Blue Meander Frieze", "雷文の帯飾り（エーゲ海の青）");

    public static final DeferredBlock<Block> RED_MEANDER_MARBLE = frieze("red_meander_marble",
            "回纹饰带（赤陶红）", "Terracotta Red Meander Frieze", "雷文の帯飾り（テラコッタの赤）");

    private GreekMaterials() {
    }

    private static ResourceLocation modBlock(String path) {
        return ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "block/" + path);
    }

    /** 大理石族的整块方块：属性同大理石，六面一张手绘贴图。 */
    private static DeferredBlock<Block> cube(String id, String zh, String en, String ja) {
        return Content.block(id, GREEK)
                .properties(() -> BlockBehaviour.Properties.ofFullCopy(MARBLE.get()))
                .tags(BlockTags.MINEABLE_WITH_PICKAXE, MARBLE_FAMILY)
                .itemTags(MARBLE_FAMILY_ITEM)
                .lang(zh, en, ja)
                .register();
    }

    private static DeferredBlock<Block> frieze(String id, String zh, String en, String ja) {
        return Content.block(id, GREEK)
                .properties(() -> BlockBehaviour.Properties.ofFullCopy(MARBLE.get()))
                .model(BlockModelSpec.column(modBlock("smooth_marble")))
                .tags(BlockTags.MINEABLE_WITH_PICKAXE, MARBLE_FAMILY)
                .itemTags(MARBLE_FAMILY_ITEM)
                .lang(zh, en, ja)
                .register();
    }

    /** 大理石族的台阶、楼梯、墙：属性同大理石；形状标签与台阶战利品由框架自动补上。 */
    private static <B extends Block> DeferredBlock<B> shape(String id, Function<BlockBehaviour.Properties, B> factory,
                                                           BlockModelSpec model, String zh, String en, String ja) {
        return Content.block(id, GREEK, factory)
                .properties(() -> BlockBehaviour.Properties.ofFullCopy(MARBLE.get()))
                .model(model)
                .tags(BlockTags.MINEABLE_WITH_PICKAXE, MARBLE_FAMILY)
                .itemTags(MARBLE_FAMILY_ITEM)
                .lang(zh, en, ja)
                .register();
    }

    /** 触发类加载，使上面的条目在注册事件之前完成声明。 */
    public static void init() {
    }
}
