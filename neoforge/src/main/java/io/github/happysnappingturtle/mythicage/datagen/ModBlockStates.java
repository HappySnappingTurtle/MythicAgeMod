package io.github.happysnappingturtle.mythicage.datagen;

import io.github.happysnappingturtle.mythicage.content.BlockModelSpec;
import io.github.happysnappingturtle.mythicage.content.Content;
import io.github.happysnappingturtle.mythicage.content.ContentEntry;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
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
                case BlockModelSpec.Log l -> axisBlock((RotatedPillarBlock) block, l.side(), l.end());
                case BlockModelSpec.Fence f -> {
                    fenceBlock((FenceBlock) block, textureOf(f.base().get()));
                    models().fenceInventory(e.id() + "_inventory", textureOf(f.base().get()));
                }
                case BlockModelSpec.FenceGate f -> fenceGateBlock((FenceGateBlock) block, textureOf(f.base().get()));
                case BlockModelSpec.Button b -> {
                    buttonBlock((ButtonBlock) block, textureOf(b.base().get()));
                    models().buttonInventory(e.id() + "_inventory", textureOf(b.base().get()));
                }
                case BlockModelSpec.PressurePlate p -> pressurePlateBlock((PressurePlateBlock) block, textureOf(p.base().get()));
                case BlockModelSpec.Door d -> doorBlockWithRenderType((DoorBlock) block, d.bottom(), d.top(), "cutout");
                case BlockModelSpec.Trapdoor t -> trapdoorBlockWithRenderType((TrapDoorBlock) block, t.texture(), true, "cutout");
                case BlockModelSpec.Leaves l -> leaves(e, block, l);
                case BlockModelSpec.Cross c -> simpleBlock(block, models().cross(e.id(), c.texture()).renderType("cutout"));
                case BlockModelSpec.Potted p -> simpleBlock(block, models()
                        .withExistingParent(e.id(), mcLoc("block/flower_pot_cross"))
                        .texture("plant", p.plant()).renderType("cutout"));
                case BlockModelSpec.Sign sg -> signBlock((StandingSignBlock) block, (WallSignBlock) sg.wall().get(), sg.particle());
                case BlockModelSpec.HangingSign hs -> hangingSignBlock((CeilingHangingSignBlock) block,
                        (WallHangingSignBlock) hs.wall().get(), hs.particle());
                case BlockModelSpec.GeneratedBy g -> {
                }
                case BlockModelSpec.Trellis t -> trellis(e, block);
                case BlockModelSpec.FacingToggle f -> {
                    ModelFile off = models().getExistingFile(modLoc("block/" + e.id()));
                    ModelFile on = models().getExistingFile(modLoc("block/" + e.id() + "_" + f.onSuffix()));
                    horizontalBlock(block, state -> state.getValue(f.toggle()) ? on : off);
                }
                case BlockModelSpec.FacingManual f ->
                        horizontalBlock(block, models().getExistingFile(modLoc("block/" + e.id())));
                case BlockModelSpec.Manual m -> {
                }
            }
        }
    }

    /** 葡萄架：stage 0 为空架子（block/<id>），1–4 为 block/<id>_stage0..3；横向轴为 Z 时旋转 90°。 */
    private void trellis(ContentEntry e, Block block) {
        var stage = io.github.happysnappingturtle.mythicage.content.greek.agri.GrapeTrellisBlock.STAGE;
        var axis = io.github.happysnappingturtle.mythicage.content.greek.agri.GrapeTrellisBlock.AXIS;
        ModelFile[] models = new ModelFile[5];
        for (int i = 0; i <= 4; i++) {
            String tex = i == 0 ? e.id() : e.id() + "_stage" + (i - 1);
            models[i] = models().withExistingParent(i == 0 ? e.id() : e.id() + "_" + i, modLoc("block/template_trellis"))
                    .texture("all", modLoc("block/" + tex)).renderType("cutout");
        }
        getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(models[state.getValue(stage)])
                .rotationY(state.getValue(axis) == net.minecraft.core.Direction.Axis.Z ? 90 : 0).build());
    }

    /** 树叶：每个属性取值一个模型；0 为纯树叶，其余为树叶 + 叠层（模板 template_leaves_overlay）。 */
    private void leaves(ContentEntry e, Block block, BlockModelSpec.Leaves l) {
        ModelFile[] models = new ModelFile[l.overlays().size()];
        models[0] = models().withExistingParent(e.id(), mcLoc("block/leaves")).texture("all", l.texture())
                .renderType("cutout_mipped");
        for (int i = 1; i < models.length; i++) {
            models[i] = models().withExistingParent(e.id() + "_" + i, modLoc("block/template_leaves_overlay"))
                    .texture("all", l.texture()).texture("overlay", l.overlays().get(i)).renderType("cutout_mipped");
        }
        getVariantBuilder(block).forAllStatesExcept(state -> ConfiguredModel.builder()
                .modelFile(models[state.getValue(l.property())]).build(),
                LeavesBlock.DISTANCE, LeavesBlock.PERSISTENT, LeavesBlock.WATERLOGGED);
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
