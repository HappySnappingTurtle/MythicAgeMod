package io.github.happysnappingturtle.mythicage.village.gen;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * 一座村庄建筑（T13b §2.1）：放模板，再把底面下方的空隙向下补地基（≤ {@link #MAX_FOUNDATION} 格，V0 §3.7 foundation）。
 */
public final class VillageBuildingPiece extends TemplateStructurePiece {
    public static final int MAX_FOUNDATION = 12;

    public VillageBuildingPiece(StructureTemplateManager manager, String template, BlockPos origin, Rotation rotation) {
        super(ModStructures.BUILDING_PIECE.get(), 0, manager, ResourceLocation.parse(template), template,
                settings(rotation), origin);
    }

    public VillageBuildingPiece(StructureTemplateManager manager, CompoundTag tag) {
        super(ModStructures.BUILDING_PIECE.get(), tag, manager, rl -> settings(Rotation.valueOf(tag.getString("Rot"))));
    }

    private static StructurePlaceSettings settings(Rotation rotation) {
        return new StructurePlaceSettings().setRotation(rotation);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
        tag.putString("Rot", placeSettings.getRotation().name());
    }

    @Override
    protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        super.postProcess(level, structures, generator, random, chunkBox, chunkPos, pivot);
        BoundingBox b = getBoundingBox();
        int y0 = b.minY();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = Math.max(b.minX(), chunkBox.minX()); x <= Math.min(b.maxX(), chunkBox.maxX()); x++) {
            for (int z = Math.max(b.minZ(), chunkBox.minZ()); z <= Math.min(b.maxZ(), chunkBox.maxZ()); z++) {
                BlockState bottom = level.getBlockState(p.set(x, y0, z));
                if (bottom.isAir() || !bottom.isCollisionShapeFullBlock(level, p)) {
                    continue;
                }
                for (int y = y0 - 1; y >= y0 - MAX_FOUNDATION; y--) {
                    BlockState s = level.getBlockState(p.set(x, y, z));
                    if (!s.isAir() && !s.canBeReplaced() && s.getFluidState().isEmpty()) {
                        break;
                    }
                    level.setBlock(p, Blocks.DIRT.defaultBlockState(), 2);
                }
            }
        }
    }
}
