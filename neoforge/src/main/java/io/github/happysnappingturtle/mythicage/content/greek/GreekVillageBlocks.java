package io.github.happysnappingturtle.mythicage.content.greek;

import io.github.happysnappingturtle.mythicage.content.BlockModelSpec;
import io.github.happysnappingturtle.mythicage.content.Content;
import io.github.happysnappingturtle.mythicage.content.greek.architecture.FacingShapedBlock;
import io.github.happysnappingturtle.mythicage.content.LootSpec;
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
import net.neoforged.neoforge.registries.DeferredBlock;

import static io.github.happysnappingturtle.mythicage.core.player.Civilization.GREEK;

/**
 * 希腊村庄新方块（T13b 阶段 ③，规划篇 §4.2 两张表）。模型与贴图由 {@code tools/models/greek_village.py} 生成。
 * 第 1 批：墙面与货堆；第 2 批：工作站（陶窑、青铜熔炉两格高，踩葡萄池两格长，按实物比例）。
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
