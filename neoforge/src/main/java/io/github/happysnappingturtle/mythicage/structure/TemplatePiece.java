package io.github.happysnappingturtle.mythicage.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/** 遗迹、迷宫模块用的模板件（T22 §3.2）：整地方式由调用方定。 */
public final class TemplatePiece extends TerrainTemplatePiece {
    public TemplatePiece(StructureTemplateManager manager, String template, BlockPos origin, Rotation rotation, int floorY,
                         Terrain terrain) {
        super(MythStructures.TEMPLATE_PIECE.get(), manager, template, origin, rotation, floorY, terrain);
    }

    public TemplatePiece(StructureTemplateManager manager, CompoundTag tag) {
        super(MythStructures.TEMPLATE_PIECE.get(), manager, tag);
    }
}
