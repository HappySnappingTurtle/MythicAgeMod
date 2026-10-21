package io.github.happysnappingturtle.mythicage.content.greek;

import io.github.happysnappingturtle.mythicage.content.BlockModelSpec;
import io.github.happysnappingturtle.mythicage.content.Content;
import io.github.happysnappingturtle.mythicage.content.greek.architecture.FacingShapedBlock;
import io.github.happysnappingturtle.mythicage.content.LootSpec;
import io.github.happysnappingturtle.mythicage.content.greek.GreekMaterials;
import io.github.happysnappingturtle.mythicage.content.greek.village.LionSpoutBlock;
import io.github.happysnappingturtle.mythicage.content.greek.village.PedimentOrnamentBlock;
import io.github.happysnappingturtle.mythicage.content.greek.village.PithosBlock;
import io.github.happysnappingturtle.mythicage.content.greek.village.TallWorkstationBlock;
import io.github.happysnappingturtle.mythicage.content.greek.village.TreadingVatBlock;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.neoforge.registries.DeferredBlock;

import static io.github.happysnappingturtle.mythicage.core.player.Civilization.GREEK;

/**
 * 希腊村庄新方块（T13b 阶段 ③，规划篇 §4.2 两张表）。模型与贴图由 {@code tools/models/greek_village.py} 生成。
 * 第 1 批：墙面与货堆；第 3 批：村庄与圣物；第 2 批：工作站（陶窑、青铜熔炉两格高，踩葡萄池两格长，按实物比例）。
 */
public final class GreekVillageBlocks {
    // ───────────── 第 1 批：墙面与货堆 ─────────────

    public static final DeferredBlock<Block> PLASTER = Content.block("plaster", GREEK)
            .properties(GreekVillageBlocks::plaster)
            .model(new BlockModelSpec.StateModels(s -> "", false, ""))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE)
            .lang("白灰墙", "Lime Plaster", "漆喰壁")
            .register();

    public static final DeferredBlock<Block> PLASTER_DADO = Content.block("plaster_dado", GREEK)
            .properties(GreekVillageBlocks::plaster)
            .model(new BlockModelSpec.StateModels(s -> "", false, ""))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE)
            .lang("赭红墙裙白灰墙", "Plaster with Ochre Dado", "赭色腰壁の漆喰壁")
            .register();

    public static final DeferredBlock<Block> MUDBRICK = Content.block("mudbrick", GREEK)
            .properties(() -> BlockBehaviour.Properties.ofFullCopy(Blocks.MUD_BRICKS).mapColor(MapColor.TERRACOTTA_BROWN))
            .model(new BlockModelSpec.StateModels(s -> "", false, ""))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE)
            .lang("日晒泥砖", "Sun-dried Mudbrick", "日干し煉瓦")
            .register();

    public static final DeferredBlock<PithosBlock> PITHOS = Content.block("pithos", GREEK, PithosBlock::new)
            .properties(GreekVillageBlocks::pottery)
            .model(new BlockModelSpec.StateModels(s -> "_" + s.getValue(PithosBlock.FILL).getSerializedName(), false, "_full"))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE)
            .lang("陶瓮", "Pithos", "ピトス")
            .register();

    public static final DeferredBlock<FacingShapedBlock> AMPHORA_STACK = Content.block("amphora_stack", GREEK,
                    p -> new FacingShapedBlock(p, false, Block.box(0, 0, 0, 16, 10, 16)))
            .properties(GreekVillageBlocks::pottery)
            .model(new BlockModelSpec.StateModels(s -> "", true, ""))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE)
            .lang("双耳瓶堆", "Amphora Stack", "アンフォラの山")
            .register();

    // ───────────── 第 2 批：工作站（外观；工作单在 T19） ─────────────

    public static final DeferredBlock<FacingShapedBlock> POTTERY_WHEEL = Content.block("pottery_wheel", GREEK,
                    p -> new FacingShapedBlock(p, false, Block.box(0.5, 0, 0.5, 15.5, 12.5, 15.5)))
            .properties(() -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion())
            .model(new BlockModelSpec.StateModels(s -> "", true, ""))
            .tags(BlockTags.MINEABLE_WITH_AXE)
            .lang("陶轮", "Potter's Wheel", "ろくろ")
            .register();

    public static final DeferredBlock<TallWorkstationBlock> POTTERY_KILN = Content.block("pottery_kiln", GREEK,
                    p -> new TallWorkstationBlock(p, Block.box(0.5, 0, 0.5, 15.5, 16, 15.5), Block.box(1.5, 0, 1.5, 14.5, 12.5, 14.5)))
            .properties(() -> kilnLike())
            .model(new BlockModelSpec.StateModels(GreekVillageBlocks::tallSuffix, true, "_item"))
            .loot(LootSpec.door())
            .tags(BlockTags.MINEABLE_WITH_PICKAXE)
            .lang("陶窑", "Pottery Kiln", "陶窯")
            .register();

    public static final DeferredBlock<FacingShapedBlock> MASON_BENCH = Content.block("mason_bench", GREEK,
                    p -> new FacingShapedBlock(p, false, Block.box(0.5, 0, 1, 15.5, 15.5, 15)))
            .properties(() -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).noOcclusion())
            .model(new BlockModelSpec.StateModels(s -> "", true, ""))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE)
            .lang("石作台", "Mason's Bench", "石工台")
            .register();

    public static final DeferredBlock<TallWorkstationBlock> BRONZE_FURNACE = Content.block("bronze_furnace", GREEK,
                    p -> new TallWorkstationBlock(p, Block.box(1, 0, 0.5, 15, 16, 16), Block.box(4.5, 0, 3.5, 11.5, 15, 11)))
            .properties(() -> kilnLike())
            .model(new BlockModelSpec.StateModels(GreekVillageBlocks::tallSuffix, true, "_item"))
            .loot(LootSpec.door())
            .tags(BlockTags.MINEABLE_WITH_PICKAXE)
            .lang("青铜熔炉", "Bronze Furnace", "青銅炉")
            .register();

    public static final DeferredBlock<FacingShapedBlock> GREEK_ANVIL = Content.block("greek_anvil", GREEK,
                    p -> new FacingShapedBlock(p, false, Block.box(1.5, 0, 1.5, 14.5, 16, 14.5)))
            .properties(() -> BlockBehaviour.Properties.ofFullCopy(Blocks.ANVIL).noOcclusion())
            .model(new BlockModelSpec.StateModels(s -> "", true, ""))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE)
            .lang("希腊式砧", "Greek Anvil", "ギリシャの金床")
            .register();

    public static final DeferredBlock<TreadingVatBlock> TREADING_VAT = Content.block("treading_vat", GREEK,
                    TreadingVatBlock::new)
            .properties(() -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).noOcclusion())
            .model(new BlockModelSpec.StateModels(
                    s -> s.getValue(TreadingVatBlock.PART) == BedPart.FOOT ? "_front" : "_back", true, "_item"))
            .loot(LootSpec.custom(l -> l.onlyWhen(GreekVillageBlocks.TREADING_VAT.get(), TreadingVatBlock.PART, BedPart.FOOT)))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE)
            .lang("踩葡萄池", "Treading Vat", "ぶどう踏み槽")
            .register();

    // ───────────── 第 3 批：村庄与圣物（尺寸按实物，比 1 格高的往上伸出、碰撞箱同高，同栅栏） ─────────────

    /** 装饰祭坛：外观同奥林匹斯祭坛，但没有主人、不登记、没有功能（村庄神庙前的露天祭坛）。 */
    public static final DeferredBlock<FacingShapedBlock> VILLAGE_ALTAR = Content.block("village_altar", GREEK,
                    p -> new FacingShapedBlock(p, false, Shapes.or(Block.box(0, 0, 0, 16, 3, 16),
                            Block.box(1, 3, 1, 15, 12, 15), Block.box(0, 12, 0, 16, 15, 16))))
            .properties(GreekVillageBlocks::carved)
            .model(new BlockModelSpec.StateModels(s -> "", true, ""))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE, GreekMaterials.MARBLE_FAMILY)
            .itemTags(GreekMaterials.MARBLE_FAMILY_ITEM)
            .lang("村庄祭坛", "Village Altar", "村の祭壇")
            .register();

    public static final DeferredBlock<FacingShapedBlock> BOUNDARY_STONE = Content.block("boundary_stone", GREEK,
                    p -> new FacingShapedBlock(p, false, Block.box(4, 0, 4, 12, 15.2, 12)))
            .properties(GreekVillageBlocks::carved)
            .model(new BlockModelSpec.StateModels(s -> "", true, ""))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE, GreekMaterials.MARBLE_FAMILY)
            .itemTags(GreekMaterials.MARBLE_FAMILY_ITEM)
            .lang("界石", "Boundary Stone", "境界石")
            .register();

    public static final DeferredBlock<Block> THRESHING_FLOOR = Content.block("threshing_floor", GREEK)
            .properties(() -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).mapColor(MapColor.TERRACOTTA_LIGHT_GRAY))
            .model(new BlockModelSpec.StateModels(s -> "", false, ""))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE)
            .lang("打谷场铺石", "Threshing Floor Paving", "脱穀場の敷石")
            .register();

    public static final DeferredBlock<FacingShapedBlock> BRONZE_TRIPOD = Content.block("bronze_tripod", GREEK,
                    p -> new FacingShapedBlock(p, false, Block.box(0.5, 0, 0.5, 15.5, 13.5, 15.5)))
            .properties(GreekVillageBlocks::bronze)
            .model(new BlockModelSpec.StateModels(s -> "", true, ""))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE)
            .lang("三足铜鼎", "Bronze Tripod Cauldron", "青銅の三脚鼎")
            .register();

    public static final DeferredBlock<FacingShapedBlock> ORACLE_TRIPOD = Content.block("oracle_tripod", GREEK,
                    p -> new FacingShapedBlock(p, false, Block.box(2, 0, 2, 14, 18.6, 14)))
            .properties(GreekVillageBlocks::bronze)
            .model(new BlockModelSpec.StateModels(s -> "", true, ""))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE)
            .lang("神谕三足鼎", "Oracle Tripod", "神託の三脚台")
            .register();

    public static final DeferredBlock<FacingShapedBlock> OMPHALOS = Content.block("omphalos", GREEK,
                    p -> new FacingShapedBlock(p, false, Block.box(1.5, 0, 1.5, 14.5, 15.2, 14.5)))
            .properties(GreekVillageBlocks::carved)
            .model(new BlockModelSpec.StateModels(s -> "", true, ""))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE, GreekMaterials.MARBLE_FAMILY)
            .itemTags(GreekMaterials.MARBLE_FAMILY_ITEM)
            .lang("肚脐石", "Omphalos", "オンファロス")
            .register();

    public static final DeferredBlock<LionSpoutBlock> LION_SPOUT = Content.block("lion_spout", GREEK, LionSpoutBlock::new)
            .properties(() -> BlockBehaviour.Properties.ofFullCopy(io.github.happysnappingturtle.mythicage.content.greek.GreekMaterials.SMOOTH_MARBLE.get()))
            .model(new BlockModelSpec.StateModels(s -> "", true, ""))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE, GreekMaterials.MARBLE_FAMILY)
            .itemTags(GreekMaterials.MARBLE_FAMILY_ITEM)
            .lang("狮头出水口", "Lion-head Spout", "獅子頭の吐水口")
            .register();

    public static final DeferredBlock<FacingShapedBlock> HERM_HEAD = Content.block("herm_head", GREEK,
                    p -> new FacingShapedBlock(p, false, Block.box(2, 0, 2, 14, 14, 14)))
            .properties(GreekVillageBlocks::carved)
            .model(new BlockModelSpec.StateModels(s -> "", true, ""))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE, GreekMaterials.MARBLE_FAMILY)
            .itemTags(GreekMaterials.MARBLE_FAMILY_ITEM)
            .lang("界柱胸像", "Herm Head", "ヘルマ柱頭")
            .register();

    public static final DeferredBlock<FacingShapedBlock> GRAVE_RELIEF = Content.block("grave_relief", GREEK,
                    p -> new FacingShapedBlock(p, false, Block.box(0, 0, 0, 16, 16, 16)))
            .properties(GreekVillageBlocks::carved)
            .model(new BlockModelSpec.StateModels(s -> "", true, ""))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE, GreekMaterials.MARBLE_FAMILY)
            .itemTags(GreekMaterials.MARBLE_FAMILY_ITEM)
            .lang("墓碑浮雕", "Grave Relief", "墓碑レリーフ")
            .register();

    public static final DeferredBlock<FacingShapedBlock> MARBLE_LEKYTHOS = Content.block("marble_lekythos", GREEK,
                    p -> new FacingShapedBlock(p, false, Block.box(3.5, 0, 3.5, 12.5, 22, 12.5)))
            .properties(GreekVillageBlocks::carved)
            .model(new BlockModelSpec.StateModels(s -> "", true, ""))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE, GreekMaterials.MARBLE_FAMILY)
            .itemTags(GreekMaterials.MARBLE_FAMILY_ITEM)
            .lang("大理石油瓶", "Marble Lekythos", "大理石のレキュトス")
            .register();

    // ───────────── 第 5 批：三角楣饰（规划篇 §5，挂在山花正面；底行横向连放 3 格，三叉戟中间那格上面再叠 1 格放叉头） ─────────────

    public static final DeferredBlock<PedimentOrnamentBlock> PEDIMENT_THUNDERBOLT = Content.block("pediment_thunderbolt",
                    GREEK, p -> new PedimentOrnamentBlock(p, false))
            .properties(GreekVillageBlocks::ornament)
            .model(new BlockModelSpec.StateModels(s -> "_" + s.getValue(PedimentOrnamentBlock.PART).getSerializedName(),
                    true, "_single"))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE)
            .lang("宙斯雷霆楣饰", "Thunderbolt Pediment Relief", "雷霆のペディメント装飾")
            .register();

    public static final DeferredBlock<PedimentOrnamentBlock> PEDIMENT_TRIDENT = Content.block("pediment_trident",
                    GREEK, p -> new PedimentOrnamentBlock(p, true))
            .properties(GreekVillageBlocks::ornament)
            .model(new BlockModelSpec.StateModels(s -> "_" + s.getValue(PedimentOrnamentBlock.PART).getSerializedName(),
                    true, "_single"))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE)
            .lang("三叉戟海浪楣饰", "Trident and Waves Pediment Relief", "三叉戟と波のペディメント装飾")
            .register();

    private static BlockBehaviour.Properties ornament() {
        return BlockBehaviour.Properties.ofFullCopy(io.github.happysnappingturtle.mythicage.content.greek.GreekMaterials.SMOOTH_MARBLE.get())
                .noOcclusion().noCollission();
    }

    private static BlockBehaviour.Properties carved() {
        return BlockBehaviour.Properties.ofFullCopy(io.github.happysnappingturtle.mythicage.content.greek.GreekMaterials.SMOOTH_MARBLE.get())
                .noOcclusion();
    }

    private static BlockBehaviour.Properties bronze() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(3.0f, 6.0f)
                .requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion();
    }

    /** 两格高工作站的模型后缀：_lower / _upper，生火时再加 _lit。 */
    private static String tallSuffix(net.minecraft.world.level.block.state.BlockState s) {
        return (s.getValue(TallWorkstationBlock.HALF) == DoubleBlockHalf.LOWER ? "_lower" : "_upper")
                + (s.getValue(TallWorkstationBlock.LIT) ? "_lit" : "");
    }

    private static BlockBehaviour.Properties kilnLike() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.MUD_BRICKS).mapColor(MapColor.TERRACOTTA_BROWN).noOcclusion()
                .lightLevel(TallWorkstationBlock::light);
    }

    private GreekVillageBlocks() {
    }

    public static void init() {
    }

    private static BlockBehaviour.Properties plaster() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.CALCITE).mapColor(MapColor.QUARTZ);
    }

    private static BlockBehaviour.Properties pottery() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.DECORATED_POT).mapColor(MapColor.TERRACOTTA_ORANGE)
                .sound(SoundType.DECORATED_POT).noOcclusion();
    }
}
