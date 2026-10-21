package io.github.happysnappingturtle.mythicage.village.gen;

import io.github.happysnappingturtle.mythicage.content.greek.GreekMaterials;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/** 地形工序共用的小工具（V0 §3.7，T13b §4.3）。只在调用方给定的区块范围内写方块。 */
final class TerrainOps {
    /** 地基、整平最多向下补多少格。 */
    static final int MAX_FILL = 12;
    /** 建筑顶上的山体最多削多少格。 */
    static final int MAX_CUT = 10;

    private TerrainOps() {
    }

    /** 能被地基 / 填土覆盖的格：空气、可替换的植物、液体。 */
    static boolean fillable(BlockState s) {
        return s.isAir() || s.canBeReplaced() || !s.getFluidState().isEmpty();
    }

    /** 自然地面（可以铺路、削平的）：土、草、石、沙、砾、雪。 */
    static boolean natural(BlockState s) {
        return s.is(BlockTags.DIRT) || s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(BlockTags.SAND)
                || s.is(Blocks.GRAVEL) || s.is(Blocks.SNOW_BLOCK) || s.is(Blocks.CLAY) || s.is(BlockTags.TERRACOTTA);
    }

    /** 可以清掉的自然物：自然地面 + 树木 + 植物。 */
    static boolean clearable(BlockState s) {
        return natural(s) || s.is(BlockTags.LOGS) || s.is(BlockTags.LEAVES) || s.canBeReplaced();
    }

    /** 地表最上面一个方块的 y（含水面与树叶）。 */
    static int top(WorldGenLevel level, int x, int z) {
        return level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
    }

    /** 从 y 往下补到实心地面（≤ MAX_FILL 格）；top 是最上面一格用的方块。 */
    static void fillDown(WorldGenLevel level, BlockPos.MutableBlockPos p, int x, int y, int z, BlockState topState,
                         BlockState body) {
        for (int yy = y; yy >= y - MAX_FILL; yy--) {
            BlockState s = level.getBlockState(p.set(x, yy, z));
            if (!fillable(s)) {
                return;
            }
            level.setBlock(p, yy == y ? topState : body, 2);
        }
    }

    static BlockState polygonal() {
        return GreekMaterials.POLYGONAL_MARBLE.get().defaultBlockState();
    }

    /** 预留地块四角的界石（T13b 阶段 ③ 换装：原先用大理石墙柱替身）。 */
    static BlockState marker() {
        return io.github.happysnappingturtle.mythicage.content.greek.GreekVillageBlocks.BOUNDARY_STONE.get().defaultBlockState();
    }

    static BlockState coping() {
        return GreekMaterials.MARBLE_ASHLAR.get().defaultBlockState();
    }

    /**
     * 占地外一圈（距离 1..margin）：高处比地面高 ≥ 2 格时在距离 1 处砌挡土墙（≤ maxWall 格高，方石压顶）；
     * 低处按 1:1 培土成缓坡。floor 是地面上第一格的 y。
     */
    static void ring(WorldGenLevel level, net.minecraft.world.level.levelgen.structure.BoundingBox chunkBox,
                     int minX, int minZ, int maxX, int maxZ, int floor, int margin, int maxWall) {
        ring(level, chunkBox, minX, minZ, maxX, maxZ, floor, margin, maxWall, true);
    }

    /** walls = false（预留地块）：高处不砌墙，按 1:1 削成缓坡。 */
    static void ring(WorldGenLevel level, net.minecraft.world.level.levelgen.structure.BoundingBox chunkBox,
                     int minX, int minZ, int maxX, int maxZ, int floor, int margin, int maxWall, boolean walls) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        for (int x = Math.max(minX - margin, chunkBox.minX()); x <= Math.min(maxX + margin, chunkBox.maxX()); x++) {
            for (int z = Math.max(minZ - margin, chunkBox.minZ()); z <= Math.min(maxZ + margin, chunkBox.maxZ()); z++) {
                int dist = Math.max(Math.max(minX - x, x - maxX), Math.max(minZ - z, z - maxZ));
                if (dist <= 0) {
                    continue;
                }
                int top = top(level, x, z);
                BlockState topState = level.getBlockState(p.set(x, top, z));
                if (!topState.getFluidState().isEmpty()) {
                    continue;
                }
                if (!walls && top > floor - 1 + dist && natural(topState)) {
                    int target = floor - 1 + dist;
                    for (int y = top; y > target && y > top - MAX_CUT; y--) {
                        BlockState s = level.getBlockState(p.set(x, y, z));
                        if (clearable(s) && s.getFluidState().isEmpty()) {
                            level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                    BlockState t = level.getBlockState(p.set(x, target, z));
                    if (natural(t) && !t.is(Blocks.GRASS_BLOCK)) {
                        level.setBlock(p, grass, 2); // 坡面露出的石头铺草
                    }
                } else if (walls && dist == 1 && top >= floor + 1 && natural(topState)) {
                    int h = Math.min(top, floor + maxWall - 1);
                    for (int y = floor; y < h; y++) {
                        level.setBlock(p.set(x, y, z), polygonal(), 2);
                    }
                    level.setBlock(p.set(x, h, z), coping(), 2);
                } else if (top < floor - 1 - dist) {
                    int target = floor - 1 - dist;
                    for (int y = top + 1; y <= target; y++) {
                        BlockState s = level.getBlockState(p.set(x, y, z));
                        if (!fillable(s)) {
                            break;
                        }
                        level.setBlock(p, y == target ? grass : dirt, 2);
                    }
                }
            }
        }
    }
}
