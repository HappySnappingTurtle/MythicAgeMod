package io.github.happysnappingturtle.mythicage.content.greek;

import io.github.happysnappingturtle.mythicage.content.BlockModelSpec;
import io.github.happysnappingturtle.mythicage.content.Content;
import io.github.happysnappingturtle.mythicage.content.greek.architecture.FacingShapedBlock;
import io.github.happysnappingturtle.mythicage.content.greek.village.PithosBlock;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;

import static io.github.happysnappingturtle.mythicage.core.player.Civilization.GREEK;

/**
 * 希腊村庄新方块（T13b 阶段 ③，规划篇 §4.2 两张表）。模型与贴图由 {@code tools/models/greek_village.py} 生成。
 * 第 1 批：墙面与货堆。
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
