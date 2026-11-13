package io.github.happysnappingturtle.mythicage.structure;

import io.github.happysnappingturtle.mythicage.content.greek.GreekMaterials;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/** 地形工序共用的小工具（V0 §3.7，T13b §4.3；T22 起村庄与遗迹共用）。只在调用方给定的区块范围内写方块。 */
public final class TerrainOps {
    /** 地基、整平最多向下补多少格。 */
    public static final int MAX_FILL = 12;
    /** 建筑顶上的山体最多削多少格。 */
    public static final int MAX_CUT = 10;

    private TerrainOps() {
    }

    /** 能被地基 / 填土覆盖的格：空气、可替换的植物、液体。 */
    public static boolean fillable(BlockState s) {
        return s.isAir() || s.canBeReplaced() || !s.getFluidState().isEmpty();
    }

    /** 自然地面（可以铺路、削平的）：土、草、石、沙、砾、雪。 */
    public static boolean natural(BlockState s) {
        return s.is(BlockTags.DIRT) || s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(BlockTags.SAND)
                || s.is(Blocks.GRAVEL) || s.is(Blocks.SNOW_BLOCK) || s.is(Blocks.CLAY) || s.is(BlockTags.TERRACOTTA);
    }

    /** 可以清掉的自然物：自然地面 + 树木 + 植物。 */
    public static boolean clearable(BlockState s) {
        return natural(s) || s.is(BlockTags.LOGS) || s.is(BlockTags.LEAVES) || s.canBeReplaced();
    }

    /** 地表最上面一个方块的 y（含水面与树叶）。 */
    public static int top(WorldGenLevel level, int x, int z) {
        return level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
    }

    /** 从 y 往下补到实心地面（≤ MAX_FILL 格）；top 是最上面一格用的方块。 */
    public static void fillDown(WorldGenLevel level, BlockPos.MutableBlockPos p, int x, int y, int z, BlockState topState,
                         BlockState body) {
        for (int yy = y; yy >= y - MAX_FILL; yy--) {
            BlockState s = level.getBlockState(p.set(x, yy, z));
            if (!fillable(s)) {
                return;
            }
            level.setBlock(p, yy == y ? topState : body, 2);
        }
    }

    public static BlockState polygonal() {
        return GreekMaterials.POLYGONAL_MARBLE.get().defaultBlockState();
    }

    /** 预留地块四角的界石（T13b 阶段 ③ 换装：原先用大理石墙柱替身）。 */
    public static BlockState marker() {
        return io.github.happysnappingturtle.mythicage.content.greek.GreekVillageBlocks.BOUNDARY_STONE.get().defaultBlockState();
    }

    public static BlockState coping() {
        return GreekMaterials.MARBLE_ASHLAR.get().defaultBlockState();
    }

    /** 整地砌的挡土墙（多边形砌石、方石压顶）：门前通道可以拆。 */
    public static boolean villageWall(BlockState s) {
        return s.is(GreekMaterials.POLYGONAL_MARBLE.get()) || s.is(GreekMaterials.MARBLE_ASHLAR.get());
    }

    /** 门前通道能清的方块（生成时）：自然方块 + 挡土墙。 */
    public static boolean doorClearable(BlockState s) {
        return clearable(s) || villageWall(s);
    }

    /**
     * 门前一列的地表：从最高一级台阶的净空顶（地面 + 台阶数 + 3）往下找第一个实心方块。不读高度图：运行时世界里的
     * {@code WORLD_SURFACE_WG} 是第一次读时临时算的、之后不更新。更高的山体留在上面（门前成了一段带顶的通道）。
     */
    public static int surface(net.minecraft.world.level.BlockGetter level, int x, int z, int floor) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = floor + io.github.happysnappingturtle.mythicage.core.village.gen.DoorAccess.MAX_STEPS + 3; y >= floor - 2; y--) {
            BlockState s = level.getBlockState(p.set(x, y, z));
            if (!s.isAir() && !s.canBeReplaced()) {
                return y;
            }
        }
        return floor - 2;
    }

    /**
     * 门前台阶（GA-14）：逐列按 {@link io.github.happysnappingturtle.mythicage.core.village.gen.DoorAccess} 判断，要台阶的列放一级
     * 朝外上升的方石台阶、上面清空到地表（至少 2 格净空）。{@code canClear} 判断某格能不能拆；{@code limit} 不为空时只写这个范围里的列。
     *
     * @param dryRun 只检查不改
     * @return 要放（或放了）的台阶数；有一列要拆的格拆不了：检查时返回 −1，正式时跳过那一列
     */
    public static int doorSteps(WorldGenLevel level, net.minecraft.world.level.levelgen.structure.BoundingBox limit,
                                java.util.List<io.github.happysnappingturtle.mythicage.core.village.gen.DoorAccess.Column> cols, int floor,
                                java.util.function.Predicate<BlockPos> canClear, boolean dryRun) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int n = 0;
        for (var c : cols) {
            if (limit != null && !limit.isInside(c.x(), limit.minY(), c.z())) {
                continue;
            }
            int top = surface(level, c.x(), c.z(), floor);
            if (!io.github.happysnappingturtle.mythicage.core.village.gen.DoorAccess.needsStair(top, floor, c.step())) {
                continue;
            }
            int sy = io.github.happysnappingturtle.mythicage.core.village.gen.DoorAccess.stairY(floor, c.step());
            int upTo = Math.max(top, sy + 2);
            boolean ok = true;
            for (int y = sy; y <= upTo && ok; y++) {
                BlockState s = level.getBlockState(p.set(c.x(), y, c.z()));
                ok = s.isAir() || canClear.test(p.immutable());
            }
            if (!ok) {
                if (dryRun) {
                    return -1;
                }
                continue;
            }
            n++;
            if (dryRun) {
                continue;
            }
            level.setBlock(p.set(c.x(), sy, c.z()), GreekMaterials.MARBLE_ASHLAR_STAIRS.get().defaultBlockState()
                    .setValue(net.minecraft.world.level.block.StairBlock.FACING, net.minecraft.core.Direction.byName(c.facing())), 2);
            for (int y = sy + 1; y <= upTo; y++) {
                if (!level.getBlockState(p.set(c.x(), y, c.z())).isAir()) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        return n;
    }

    /**
     * 占地外一圈（距离 1..margin）：高处比地面高 ≥ 2 格时在距离 1 处砌挡土墙（≤ maxWall 格高，方石压顶）；
     * 低处按 1:1 培土成缓坡。floor 是地面上第一格的 y。
     */
    public static void ring(WorldGenLevel level, net.minecraft.world.level.levelgen.structure.BoundingBox chunkBox,
                     int minX, int minZ, int maxX, int maxZ, int floor, int margin, int maxWall) {
        ring(level, chunkBox, minX, minZ, maxX, maxZ, floor, margin, maxWall, true);
    }

    /** walls = false（预留地块）：高处不砌墙，按 1:1 削成缓坡。 */
    public static void ring(WorldGenLevel level, net.minecraft.world.level.levelgen.structure.BoundingBox chunkBox,
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
