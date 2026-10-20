package io.github.happysnappingturtle.mythicage.village.gen;

import io.github.happysnappingturtle.mythicage.core.village.gen.Box;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * 预留地块（V0 §5.2）：生成时把地整平到地块高度（高的削、低的垫，树一并清掉），四角立不显眼的界桩
 * （阶段 ③ 换成界石方块）。未建成前不算村里。
 */
public final class VillagePlotPiece extends StructurePiece {
    public static final int MARGIN = 3;
    public static final int MAX_WALL = 4;
    private final Box plot;

    public VillagePlotPiece(Box plot) {
        super(ModStructures.PLOT_PIECE.get(), 0, new BoundingBox(plot.minX() - MARGIN, plot.minY() - TerrainOps.MAX_FILL,
                plot.minZ() - MARGIN, plot.maxX() + MARGIN, plot.minY() + TerrainOps.MAX_CUT, plot.maxZ() + MARGIN));
        this.plot = plot;
    }

    public VillagePlotPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModStructures.PLOT_PIECE.get(), tag);
        int[] v = tag.getIntArray("Plot");
        this.plot = new Box(v[0], v[1], v[2], v[3], v[4], v[5]);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putIntArray("Plot", new int[]{plot.minX(), plot.minY(), plot.minZ(), plot.maxX(), plot.maxY(), plot.maxZ()});
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        int ground = plot.minY(); // 地面上第一格
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        for (int x = Math.max(plot.minX(), chunkBox.minX()); x <= Math.min(plot.maxX(), chunkBox.maxX()); x++) {
            for (int z = Math.max(plot.minZ(), chunkBox.minZ()); z <= Math.min(plot.maxZ(), chunkBox.maxZ()); z++) {
                for (int y = ground; y <= ground + TerrainOps.MAX_CUT; y++) {
                    BlockState s = level.getBlockState(p.set(x, y, z));
                    if (s.isAir()) {
                        break;
                    }
                    if (TerrainOps.clearable(s) && s.getFluidState().isEmpty()) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
                BlockState g = level.getBlockState(p.set(x, ground - 1, z));
                if (TerrainOps.fillable(g)) {
                    TerrainOps.fillDown(level, p, x, ground - 1, z, grass, dirt);
                } else if (TerrainOps.natural(g) && !g.is(Blocks.GRASS_BLOCK)) {
                    level.setBlock(p, grass, 2); // 削出来的石头、砾石地面铺成草地
                }
                boolean corner = (x == plot.minX() || x == plot.maxX()) && (z == plot.minZ() || z == plot.maxZ());
                if (corner) {
                    level.setBlock(p.set(x, ground, z), TerrainOps.marker(), 2);
                }
            }
        }
        TerrainOps.ring(level, chunkBox, plot.minX(), plot.minZ(), plot.maxX(), plot.maxZ(), ground, MARGIN, MAX_WALL, false);
    }
}
