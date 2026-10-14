package io.github.happysnappingturtle.mythicage.datagen;

import io.github.happysnappingturtle.mythicage.content.BlockModelSpec;
import io.github.happysnappingturtle.mythicage.content.Content;
import io.github.happysnappingturtle.mythicage.content.ContentEntry;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/** 方块状态与方块模型：按 {@link BlockModelSpec} 生成。 */
public final class ModBlockStates extends BlockStateProvider {
    /** 矿石叠层模型的父模型（手写，位于 models/block/template_ore_overlay.json）。 */
    static final String ORE_TEMPLATE = "block/template_ore_overlay";

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
                case BlockModelSpec.CubeAll c -> simpleBlock(block);
                case BlockModelSpec.OreOverlay o -> simpleBlock(block, models()
                        .withExistingParent(e.id(), modLoc(ORE_TEMPLATE))
                        .texture("base", o.baseTexture())
                        .texture("overlay", modLoc("block/" + e.id() + "_overlay"))
                        .texture("particle", o.baseTexture())
                        .renderType("cutout"));
                case BlockModelSpec.Slab s -> slabBlock((SlabBlock) block, blockTexture(s.base().get()),
                        blockTexture(s.base().get()));
                case BlockModelSpec.Stairs s -> stairsBlock((StairBlock) block, blockTexture(s.base().get()));
                case BlockModelSpec.Wall w -> {
                    wallBlock((WallBlock) block, blockTexture(w.base().get()));
                    models().wallInventory(e.id() + "_inventory", blockTexture(w.base().get()));
                }
                case BlockModelSpec.Pillar p -> logBlock((RotatedPillarBlock) block);
                case BlockModelSpec.Manual m -> {
                }
            }
        }
    }
}
