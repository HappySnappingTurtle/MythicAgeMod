package io.github.happysnappingturtle.mythicage.village.gen;

import io.github.happysnappingturtle.mythicage.structure.TerrainTemplatePiece;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * 一座村庄建筑（T13b §2.1、§4.3）：放模板，再整地（{@link TerrainTemplatePiece.Terrain#FULL}）——
 * <ul>
 *   <li>占地每一列：地面下补土到实心地面（≤ 12 格），占地边缘的列用多边形砌石（低处露出的就是挡土墙）；</li>
 *   <li>模板顶上的山体削掉（≤ 10 格）；</li>
 *   <li>占地外一圈：高处比地面高 ≥ 2 格时砌挡土墙（≤ 5 格高，压顶方石）；低处按 1:1 培土成缓坡。</li>
 * </ul>
 * 桥、码头（shape = false）只把底层的实心方块往下补到水底（{@link TerrainTemplatePiece.Terrain#PILLARS}）。
 * 整地的实现在 {@link TerrainTemplatePiece}（T22 抽出，遗迹也用）。
 */
public final class VillageBuildingPiece extends TerrainTemplatePiece {
    public VillageBuildingPiece(StructureTemplateManager manager, String template, BlockPos origin, Rotation rotation,
                                int floorY, boolean shape) {
        super(ModStructures.BUILDING_PIECE.get(), manager, template, origin, rotation, floorY,
                shape ? Terrain.FULL : Terrain.PILLARS);
    }

    public VillageBuildingPiece(StructureTemplateManager manager, CompoundTag tag) {
        super(ModStructures.BUILDING_PIECE.get(), manager, tag);
    }

    /** T22 之前的存档只有 Shape（true 整地、false 只补桥墩）。 */
    @Override
    protected Terrain readTerrain(CompoundTag tag) {
        if (tag.contains("Terrain")) {
            return super.readTerrain(tag);
        }
        return !tag.contains("Shape") || tag.getBoolean("Shape") ? Terrain.FULL : Terrain.PILLARS;
    }
}
