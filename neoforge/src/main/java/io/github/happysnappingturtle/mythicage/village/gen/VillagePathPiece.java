package io.github.happysnappingturtle.mythicage.village.gen;

import io.github.happysnappingturtle.mythicage.structure.TerrainOps;

import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.tags.BlockTags;
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

import java.util.ArrayList;
import java.util.List;

/**
 * 一段道路（T13b §4.2 步 7）：沿折线逐格贴着实际地表铺路——草、土铺成土径，沙地铺砾石，主路中线夹多边形砌石；
 * 只换自然地面，不碰建筑方块与水面（水面由桥片段负责）。
 */
public final class VillagePathPiece extends StructurePiece {
    private final int width;
    private final List<Pos> points;

    public VillagePathPiece(int width, List<Pos> points) {
        super(ModStructures.PATH_PIECE.get(), 0, box(width, points));
        this.width = width;
        this.points = List.copyOf(points);
    }

    public VillagePathPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModStructures.PATH_PIECE.get(), tag);
        this.width = tag.getInt("W");
        List<Pos> pts = new ArrayList<>();
        int[] a = tag.getIntArray("P");
        for (int i = 0; i + 2 < a.length; i += 3) {
            pts.add(new Pos(a[i], a[i + 1], a[i + 2]));
        }
        this.points = pts;
    }

    public io.github.happysnappingturtle.mythicage.core.village.gen.VillagePlan.Path path() {
        return new io.github.happysnappingturtle.mythicage.core.village.gen.VillagePlan.Path(width, points);
    }

    private static BoundingBox box(int width, List<Pos> pts) {
        int r = width / 2;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (Pos p : pts) {
            minX = Math.min(minX, p.x() - r);
            maxX = Math.max(maxX, p.x() + r);
            minZ = Math.min(minZ, p.z() - r);
            maxZ = Math.max(maxZ, p.z() + r);
            minY = Math.min(minY, p.y());
            maxY = Math.max(maxY, p.y());
        }
        return new BoundingBox(minX, minY - 12, minZ, maxX, maxY + 12, maxZ);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("W", width);
        int[] a = new int[points.size() * 3];
        for (int i = 0; i < points.size(); i++) {
            a[i * 3] = points.get(i).x();
            a[i * 3 + 1] = points.get(i).y();
            a[i * 3 + 2] = points.get(i).z();
        }
        tag.put("P", new IntArrayTag(a));
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int lo = -(width - 1) / 2;
        int hi = width / 2;
        for (int i = 0; i + 1 < points.size(); i++) {
            Pos a = points.get(i);
            Pos b = points.get(i + 1);
            int dx = Integer.signum(b.x() - a.x());
            int dz = Integer.signum(b.z() - a.z());
            int len = Math.max(Math.abs(b.x() - a.x()), Math.abs(b.z() - a.z()));
            for (int k = 0; k <= len; k++) {
                int cx = a.x() + dx * k;
                int cz = a.z() + dz * k;
                for (int o = lo; o <= hi; o++) {
                    int x = cx + (dx == 0 ? o : 0);
                    int z = cz + (dz == 0 ? o : 0);
                    if (chunkBox.isInside(x, chunkBox.minY(), z)) {
                        pave(level, p, x, z, o == 0 && width >= 3);
                    }
                }
            }
        }
    }

    private static void pave(WorldGenLevel level, BlockPos.MutableBlockPos p, int x, int z, boolean center) {
        int y = TerrainOps.top(level, x, z);
        BlockState s = level.getBlockState(p.set(x, y, z));
        if (s.canBeReplaced() && s.getFluidState().isEmpty() && !s.isAir()) {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), 2); // 草丛、花
            y--;
            s = level.getBlockState(p.set(x, y, z));
        }
        if (!s.is(BlockTags.DIRT) && !s.is(BlockTags.SAND) && !s.is(Blocks.GRAVEL) && !s.is(Blocks.SNOW_BLOCK)) {
            return;
        }
        BlockState road;
        if (center && Math.floorMod(x * 13 + z * 7, 3) == 0) {
            road = TerrainOps.polygonal(); // 主路零星铺石
        } else if (s.is(BlockTags.SAND) || Math.floorMod(x * 31 + z * 17, 5) == 0) {
            road = Blocks.GRAVEL.defaultBlockState();
        } else {
            road = Blocks.DIRT_PATH.defaultBlockState();
        }
        level.setBlock(p.set(x, y, z), road, 2);
        BlockState above = level.getBlockState(p.set(x, y + 1, z));
        if (above.canBeReplaced() && above.getFluidState().isEmpty()) {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        }
    }
}
