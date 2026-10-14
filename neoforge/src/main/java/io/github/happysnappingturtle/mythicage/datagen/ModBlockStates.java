package io.github.happysnappingturtle.mythicage.datagen;

import io.github.happysnappingturtle.mythicage.content.BlockModelSpec;
import io.github.happysnappingturtle.mythicage.content.Content;
import io.github.happysnappingturtle.mythicage.content.ContentEntry;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/** 方块状态与方块模型：按 {@link BlockModelSpec} 生成。 */
public final class ModBlockStates extends BlockStateProvider {
    public ModBlockStates(PackOutput output, ExistingFileHelper files) {
        super(output, MythicAgeIds.MOD_ID, files);
    }

    @Override
    protected void registerStatesAndModels() {
        for (ContentEntry e : Content.entries()) {
            if (e.block().isEmpty()) {
                continue;
            }
            Block block = e.block().get().get();
            switch (e.model().orElseThrow()) {
                case BlockModelSpec.CubeAll c -> simpleBlock(block, models().cubeAll(e.id(), texture(e)));
                case BlockModelSpec.Slab s -> slab(e, (SlabBlock) block, s);
                case BlockModelSpec.Column c -> simpleBlock(block, models().cubeColumn(e.id(), texture(e), c.end()));
                case BlockModelSpec.Stairs s -> stairsBlock((StairBlock) block, textureOf(s.base().get()));
                case BlockModelSpec.Wall w -> {
                    wallBlock((WallBlock) block, textureOf(w.base().get()));
                    models().wallInventory(e.id() + "_inventory", textureOf(w.base().get()));
                }
                case BlockModelSpec.Pillar p -> logBlock((RotatedPillarBlock) block);
                case BlockModelSpec.Manual m -> {
                }
            }
        }
    }

    /** 台阶：没有专用侧面贴图时用原版写法；有时三种状态（下、上、双层）的侧面都换成专用贴图。 */
    private void slab(ContentEntry e, SlabBlock block, BlockModelSpec.Slab s) {
        ResourceLocation face = textureOf(s.base().get());
        if (s.side() == null) {
            slabBlock(block, modelOf(s.base().get()), face);
            return;
        }
        slabBlock(block,
                models().slab(e.id(), s.side(), face, face),
                models().slabTop(e.id() + "_top", s.side(), face, face),
                models().cubeBottomTop(e.id() + "_double", s.side(), face, face));
    }

    /** 条目的六面贴图：声明了 texture 就用它，否则 {@code mythic_age:block/<id>}。 */
    private ResourceLocation texture(ContentEntry e) {
        return e.texture().orElse(modLoc("block/" + e.id()));
    }

    /** 本模组方块的贴图（台阶、楼梯、墙沿用其 base 方块的贴图）。 */
    private ResourceLocation textureOf(Block block) {
        return texture(entryOf(block));
    }

    private ResourceLocation modelOf(Block block) {
        return modLoc("block/" + entryOf(block).id());
    }

    static ContentEntry entryOf(Block block) {
        return Content.entries().stream()
                .filter(e -> e.block().isPresent() && e.block().get().get() == block)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("形状方块的 base 不是本模组的内容条目：" + block));
    }
}
