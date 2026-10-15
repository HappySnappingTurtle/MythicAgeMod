package io.github.happysnappingturtle.mythicage.content.greek;

import io.github.happysnappingturtle.mythicage.content.BlockModelSpec;
import io.github.happysnappingturtle.mythicage.content.Content;
import io.github.happysnappingturtle.mythicage.content.greek.architecture.AxisBlock;
import io.github.happysnappingturtle.mythicage.content.greek.architecture.ColumnShaftBlock;
import io.github.happysnappingturtle.mythicage.content.greek.architecture.FacingShapedBlock;
import io.github.happysnappingturtle.mythicage.content.greek.architecture.MosaicBlock;
import io.github.happysnappingturtle.mythicage.content.greek.architecture.ShapedBlock;
import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekTemple;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.registries.DeferredBlock;

import static io.github.happysnappingturtle.mythicage.core.player.Civilization.GREEK;

/**
 * 希腊建筑套件（T8b 设计文档 §2）：柱础、柱身、三种柱头、额枋、三陇板饰带、檐口、赤陶屋面、瓦当、卵石马赛克。
 * 模型与贴图由 {@code tools/models/greek_architecture.py} 生成。柱础、柱身、柱头都算希腊柱，可组合成一根柱。
 */
public final class GreekArchitecture {
    private static final VoxelShape BASE_SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 4, 16), Block.box(1, 4, 1, 15, 8, 15),
            Block.box(2, 8, 2, 14, 16, 14));
    private static final VoxelShape DORIC_SHAPE = Shapes.or(Block.box(2, 0, 2, 14, 8, 14), Block.box(1, 8, 1, 15, 12, 15),
            Block.box(0, 12, 0, 16, 16, 16));
    private static final VoxelShape IONIC_Z = Shapes.or(Block.box(2, 0, 2, 14, 8, 14), Block.box(0, 8, 2, 16, 14, 14),
            Block.box(1, 14, 1, 15, 16, 15));
    private static final VoxelShape IONIC_X = Shapes.or(Block.box(2, 0, 2, 14, 8, 14), Block.box(2, 8, 0, 14, 14, 16),
            Block.box(1, 14, 1, 15, 16, 15));
    private static final VoxelShape RIDGE_Z = Shapes.or(Block.box(0, 0, 0, 16, 8, 16), Block.box(3, 8, 0, 13, 13, 16));
    private static final VoxelShape RIDGE_X = Shapes.or(Block.box(0, 0, 0, 16, 8, 16), Block.box(0, 8, 3, 16, 13, 13));
    private static final VoxelShape ANTEFIX = Block.box(0, 0, 0, 16, 9, 2);

    public static final DeferredBlock<ShapedBlock> MARBLE_COLUMN_BASE = Content.block("marble_column_base", GREEK,
                    p -> new ShapedBlock(p, BASE_SHAPE))
            .properties(GreekArchitecture::marble)
            .model(new BlockModelSpec.StateModels(s -> "", false, ""))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE, GreekTemple.GREEK_COLUMN, GreekMaterials.MARBLE_FAMILY)
            .itemTags(GreekMaterials.MARBLE_FAMILY_ITEM)
            .lang("柱础", "Column Base", "柱礎")
            .register();

    public static final DeferredBlock<ColumnShaftBlock> MARBLE_COLUMN = Content.block("marble_column", GREEK,
                    ColumnShaftBlock::new)
            .properties(GreekArchitecture::marble)
            .model(new BlockModelSpec.StateModels(s -> "_" + s.getValue(ColumnShaftBlock.PART).getSerializedName(), false,
                    "_single"))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE, GreekTemple.GREEK_COLUMN, GreekMaterials.MARBLE_FAMILY)
            .itemTags(GreekMaterials.MARBLE_FAMILY_ITEM)
            .lang("大理石柱身", "Marble Column Shaft", "大理石の柱身")
            .register();

    public static final DeferredBlock<ShapedBlock> DORIC_CAPITAL = Content.block("doric_capital", GREEK,
                    p -> new ShapedBlock(p, DORIC_SHAPE))
            .properties(GreekArchitecture::marble)
            .model(new BlockModelSpec.StateModels(s -> "", false, ""))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE, GreekTemple.GREEK_COLUMN, GreekMaterials.MARBLE_FAMILY)
            .itemTags(GreekMaterials.MARBLE_FAMILY_ITEM)
            .lang("多立克柱头", "Doric Capital", "ドーリア式柱頭")
            .register();

    public static final DeferredBlock<AxisBlock> IONIC_CAPITAL = Content.block("ionic_capital", GREEK,
                    p -> new AxisBlock(p, IONIC_X, IONIC_Z))
            .properties(GreekArchitecture::marble)
            .model(new BlockModelSpec.StateModels(s -> s.getValue(AxisBlock.AXIS) == Direction.Axis.X ? "_x" : "_z", false,
                    "_z"))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE, GreekTemple.GREEK_COLUMN, GreekMaterials.MARBLE_FAMILY)
            .itemTags(GreekMaterials.MARBLE_FAMILY_ITEM)
            .lang("爱奥尼柱头", "Ionic Capital", "イオニア式柱頭")
            .register();

    public static final DeferredBlock<ShapedBlock> CORINTHIAN_CAPITAL = Content.block("corinthian_capital", GREEK,
                    p -> new ShapedBlock(p, Shapes.block()))
            .properties(GreekArchitecture::marble)
            .model(new BlockModelSpec.StateModels(s -> "", false, ""))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE, GreekTemple.GREEK_COLUMN, GreekMaterials.MARBLE_FAMILY)
            .itemTags(GreekMaterials.MARBLE_FAMILY_ITEM)
            .lang("科林斯柱头", "Corinthian Capital", "コリント式柱頭")
            .register();

    public static final DeferredBlock<FacingShapedBlock> MARBLE_ARCHITRAVE = Content.block("marble_architrave", GREEK,
                    p -> new FacingShapedBlock(p, false, Shapes.block()))
            .properties(GreekArchitecture::marble)
            .model(new BlockModelSpec.StateModels(s -> "", true, ""))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE, GreekMaterials.MARBLE_FAMILY)
            .itemTags(GreekMaterials.MARBLE_FAMILY_ITEM)
            .lang("大理石额枋", "Marble Architrave", "大理石のアーキトレーブ")
            .register();

    public static final DeferredBlock<FacingShapedBlock> TRIGLYPH_FRIEZE = Content.block("triglyph_frieze", GREEK,
                    p -> new FacingShapedBlock(p, true, Shapes.block()))
            .properties(GreekArchitecture::marble)
            .model(new BlockModelSpec.StateModels(s -> s.getValue(FacingShapedBlock.ALT) ? "_metope" : "_triglyph", true,
                    "_triglyph"))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE, GreekMaterials.MARBLE_FAMILY)
            .itemTags(GreekMaterials.MARBLE_FAMILY_ITEM)
            .lang("三陇板饰带", "Triglyph Frieze", "トリグリフのフリーズ")
            .register();

    public static final DeferredBlock<StairBlock> MARBLE_CORNICE = Content.block("marble_cornice", GREEK,
                    p -> new StairBlock(GreekMaterials.SMOOTH_MARBLE.get().defaultBlockState(), p))
            .properties(GreekArchitecture::marble)
            .model(new BlockModelSpec.StairsTextured(mod("block/marble_cornice_side"), mod("block/marble_cornice_bottom"),
                    mod("block/smooth_marble")))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE, GreekMaterials.MARBLE_FAMILY)
            .itemTags(GreekMaterials.MARBLE_FAMILY_ITEM)
            .lang("大理石檐口", "Marble Cornice", "大理石のコーニス")
            .register();

    public static final DeferredBlock<Block> TERRACOTTA_ROOF_TILES = Content.block("terracotta_roof_tiles", GREEK)
            .properties(GreekArchitecture::terracotta)
            .tags(BlockTags.MINEABLE_WITH_PICKAXE)
            .lang("赤陶瓦", "Terracotta Roof Tiles", "テラコッタの屋根瓦")
            .register();

    public static final DeferredBlock<StairBlock> TERRACOTTA_ROOF_STAIRS = Content.block("terracotta_roof_stairs", GREEK,
                    p -> new StairBlock(TERRACOTTA_ROOF_TILES.get().defaultBlockState(), p))
            .properties(GreekArchitecture::terracotta)
            .model(new BlockModelSpec.StairsTextured(mod("block/terracotta_roof_side"), mod("block/terracotta_roof_under"),
                    mod("block/terracotta_roof_tiles")))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE)
            .lang("赤陶瓦坡面", "Terracotta Roof Slope", "テラコッタの屋根（傾斜）")
            .register();

    public static final DeferredBlock<SlabBlock> TERRACOTTA_ROOF_SLAB = Content.block("terracotta_roof_slab", GREEK,
                    SlabBlock::new)
            .properties(GreekArchitecture::terracotta)
            .model(BlockModelSpec.slab(TERRACOTTA_ROOF_TILES, mod("block/terracotta_roof_side")))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE)
            .lang("赤陶瓦平面", "Terracotta Roof Slab", "テラコッタの屋根（平）")
            .register();

    public static final DeferredBlock<AxisBlock> TERRACOTTA_RIDGE = Content.block("terracotta_ridge", GREEK,
                    p -> new AxisBlock(p, RIDGE_X, RIDGE_Z))
            .properties(() -> terracotta().noOcclusion())
            .model(new BlockModelSpec.StateModels(s -> s.getValue(AxisBlock.AXIS) == Direction.Axis.X ? "_x" : "_z", false,
                    "_z"))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE)
            .lang("屋脊瓦", "Terracotta Ridge", "テラコッタの棟瓦")
            .register();

    public static final DeferredBlock<FacingShapedBlock> TERRACOTTA_ANTEFIX = Content.block("terracotta_antefix", GREEK,
                    p -> new FacingShapedBlock(p, false, ANTEFIX))
            .properties(() -> terracotta().noOcclusion().noCollission().strength(0.5f))
            .model(new BlockModelSpec.StateModels(s -> "", true, ""))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE)
            .lang("瓦当", "Antefix", "軒先瓦")
            .register();

    public static final DeferredBlock<MosaicBlock> PEBBLE_MOSAIC = Content.block("pebble_mosaic", GREEK, MosaicBlock::new)
            .properties(() -> BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE).mapColor(MapColor.QUARTZ))
            .model(new BlockModelSpec.StateModels(s -> "_q" + s.getValue(MosaicBlock.QUADRANT), false, "_q0"))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE, GreekTemple.TEMPLE_FLOOR)
            .lang("卵石马赛克", "Pebble Mosaic", "小石のモザイク")
            .register();

    private GreekArchitecture() {
    }

    public static void init() {
    }

    private static BlockBehaviour.Properties marble() {
        return BlockBehaviour.Properties.ofFullCopy(GreekMaterials.MARBLE.get()).noOcclusion();
    }

    private static BlockBehaviour.Properties terracotta() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.TERRACOTTA).mapColor(MapColor.TERRACOTTA_ORANGE);
    }

    private static ResourceLocation mod(String path) {
        return ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, path);
    }
}
