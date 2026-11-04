package io.github.happysnappingturtle.mythicage.playtest;

import io.github.happysnappingturtle.mythicage.content.greek.GreekMaterials;
import io.github.happysnappingturtle.mythicage.content.greek.temple.BrazierBlock;
import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekAltarBlock;
import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekAltarBlockEntity;
import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekTemple;
import io.github.happysnappingturtle.mythicage.content.greek.temple.StatueBlock;
import io.github.happysnappingturtle.mythicage.content.greek.temple.TempleService;
import io.github.happysnappingturtle.mythicage.core.greek.temple.TempleBlueprint;
import io.github.happysnappingturtle.mythicage.shrine.ShrineService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * 快速验证的神庙（GA-3 设计文档 §4.5）：在村庄附近找空地，按 {@link TempleBlueprint} 搭一级或二级神庙，
 * 祭坛走玩家放置的同一条绑定路径，等扫描出结果。
 */
public final class PlaytestTemple {
    /** 选址：离钟最近 12 格、最远 48 格，每 3 格试一个点。 */
    static final int MIN_RADIUS = 12;
    static final int MAX_RADIUS = 48;
    static final int STEP = 3;
    /** 占地内地面高差上限（地基最多往下垫 {@link #FOUNDATION_DEPTH} 格，高出的部分直接削平）。 */
    static final int MAX_SLOPE = 6;
    private static final int FOUNDATION_DEPTH = 6;
    private static final int FLAGS = Block.UPDATE_CLIENTS;

    private PlaytestTemple() {
    }

    /** 从 {@code around} 往外找一块能搭神庙的空地，返回祭坛位置；祭坛朝南。 */
    static Optional<BlockPos> findSite(ServerLevel level, BlockPos around, ServerPlayer owner) {
        List<int[]> spots = new ArrayList<>();
        for (int dx = -MAX_RADIUS; dx <= MAX_RADIUS; dx += STEP) {
            for (int dz = -MAX_RADIUS; dz <= MAX_RADIUS; dz += STEP) {
                int d2 = dx * dx + dz * dz;
                if (d2 >= MIN_RADIUS * MIN_RADIUS && d2 <= MAX_RADIUS * MAX_RADIUS) {
                    spots.add(new int[]{dx, dz, d2});
                }
            }
        }
        spots.sort(Comparator.comparingInt(s -> s[2]));
        int[] rejected = new int[Reject.values().length + 1];
        for (int[] s : spots) {
            int x = around.getX() + s[0];
            int z = around.getZ() + s[1];
            Reject[] why = new Reject[1];
            OptionalInt y = siteY(level, x, z, Integer.MAX_VALUE, why);
            if (y.isEmpty()) {
                rejected[why[0].ordinal()]++;
                continue;
            }
            BlockPos altar = new BlockPos(x, y.getAsInt(), z);
            if (ShrineService.placementError(owner, level, altar, Direction.SOUTH, GreekTemple.ALTAR_KIND).isEmpty()) {
                return Optional.of(altar);
            }
            rejected[Reject.values().length]++;
        }
        StringBuilder b = new StringBuilder();
        for (Reject r : Reject.values()) {
            b.append(r).append('=').append(rejected[r.ordinal()]).append(' ');
        }
        io.github.happysnappingturtle.mythicage.MythicAge.LOGGER.info("快速验证神庙选址失败：{} 处候选，{}PLACEMENT={}",
                spots.size(), b, rejected[Reject.values().length]);
        return Optional.empty();
    }

    /**
     * 以 (x, z) 为祭坛的占地是否全是自然地形、高差不超过 {@link #MAX_SLOPE}；是则返回祭坛站立高度。
     * 每一列从 min(高度图, fromY) 往下穿过草、树叶、原木找到地面（GameTest 场地顶上有屏障，要给 fromY）。
     */
    public static OptionalInt siteY(ServerLevel level, int x, int z, int fromY) {
        return siteY(level, x, z, fromY, new Reject[1]);
    }

    /** 选址被拒的原因（只记日志）。 */
    enum Reject { NOT_LOADED, GROUND, SLOPE, BLOCKED }

    static OptionalInt siteY(ServerLevel level, int x, int z, int fromY, Reject[] why) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        int[][] heights = new int[TempleBlueprint.MAX_DX - TempleBlueprint.MIN_DX + 1][TempleBlueprint.MAX_DZ - TempleBlueprint.MIN_DZ + 1];
        for (int dx = TempleBlueprint.MIN_DX; dx <= TempleBlueprint.MAX_DX; dx++) {
            for (int dz = TempleBlueprint.MIN_DZ; dz <= TempleBlueprint.MAX_DZ; dz++) {
                BlockPos col = new BlockPos(x + dx, 0, z + dz);
                if (!level.isLoaded(col)) {
                    why[0] = Reject.NOT_LOADED;
                    return OptionalInt.empty();
                }
                int h = groundY(level, col.getX(), col.getZ(),
                        Math.min(fromY, level.getHeight(Heightmap.Types.MOTION_BLOCKING, col.getX(), col.getZ())));
                BlockState ground = level.getBlockState(new BlockPos(col.getX(), h - 1, col.getZ()));
                if (!naturalGround(ground)) {
                    why[0] = Reject.GROUND;
                    return OptionalInt.empty();
                }
                heights[dx - TempleBlueprint.MIN_DX][dz - TempleBlueprint.MIN_DZ] = h;
                min = Math.min(min, h);
                max = Math.max(max, h);
                if (max - min > MAX_SLOPE) {
                    why[0] = Reject.SLOPE;
                    return OptionalInt.empty();
                }
            }
        }
        int y = heights[-TempleBlueprint.MIN_DX][-TempleBlueprint.MIN_DZ];
        // 上方要清空的范围里只能有空气、草花、树叶和原木（不压房子、路、栅栏）
        for (int dx = TempleBlueprint.MIN_DX; dx <= TempleBlueprint.MAX_DX; dx++) {
            for (int dz = TempleBlueprint.MIN_DZ; dz <= TempleBlueprint.MAX_DZ; dz++) {
                int h = heights[dx - TempleBlueprint.MIN_DX][dz - TempleBlueprint.MIN_DZ];
                for (int yy = h; yy <= y + TempleBlueprint.MAX_DY; yy++) {
                    if (!clearable(level.getBlockState(new BlockPos(x + dx, yy, z + dz)))) {
                        why[0] = Reject.BLOCKED;
                        return OptionalInt.empty();
                    }
                }
            }
        }
        return OptionalInt.of(y);
    }

    /** 从 fromY 往下，第一个不能清掉的方块上面一格。 */
    static int groundY(ServerLevel level, int x, int z, int fromY) {
        int y = fromY;
        while (y > level.getMinBuildHeight() && clearable(level.getBlockState(new BlockPos(x, y - 1, z)))) {
            y--;
        }
        return y;
    }

    static boolean naturalGround(BlockState s) {
        return s.is(BlockTags.DIRT) || s.is(BlockTags.SAND) || s.is(BlockTags.BASE_STONE_OVERWORLD)
                || s.is(Blocks.GRAVEL) || s.is(Blocks.SNOW_BLOCK) || s.is(Blocks.CLAY)
                // 已经搭过的试玩神庙原址可以重搭
                || s.is(GreekMaterials.SMOOTH_MARBLE.get());
    }

    static boolean clearable(BlockState s) {
        return s.isAir() || (s.canBeReplaced() && s.getFluidState().isEmpty()) || s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS);
    }

    /**
     * 在 altar 处搭指定等级的神庙并绑定给 owner：先清空占地上方、把地面垫实，再放方块，最后放祭坛并提交扫描。
     * 调用方随后用 {@link #scanned} 等结果。
     */
    public static void build(ServerLevel level, BlockPos altar, int templeLevel, ServerPlayer owner) {
        for (int dx = TempleBlueprint.MIN_DX; dx <= TempleBlueprint.MAX_DX; dx++) {
            for (int dz = TempleBlueprint.MIN_DZ; dz <= TempleBlueprint.MAX_DZ; dz++) {
                for (int dy = 0; dy <= TempleBlueprint.MAX_DY; dy++) {
                    BlockPos p = altar.offset(dx, dy, dz);
                    if (!level.getBlockState(p).isAir()) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), FLAGS);
                    }
                }
                for (int dy = -1; dy >= -FOUNDATION_DEPTH; dy--) {
                    BlockPos p = altar.offset(dx, dy, dz);
                    if (level.getBlockState(p).isSolidRender(level, p)) {
                        break;
                    }
                    level.setBlock(p, Blocks.STONE.defaultBlockState(), FLAGS);
                }
            }
        }
        BlockState marble = GreekMaterials.SMOOTH_MARBLE.get().defaultBlockState();
        BlockState statue = GreekTemple.GREEK_STATUE.get().defaultBlockState().setValue(StatueBlock.FACING, Direction.NORTH);
        for (TempleBlueprint.Block b : TempleBlueprint.of(templeLevel)) {
            BlockPos p = altar.offset(b.dx(), b.dy(), b.dz());
            BlockState s = switch (b.part()) {
                case PLATFORM, FLOOR, ROOF -> marble;
                case PILLAR -> GreekTemple.MARBLE_PILLAR.get().defaultBlockState();
                case BRAZIER -> GreekTemple.GREEK_BRAZIER.get().defaultBlockState().setValue(BrazierBlock.LIT, true);
                case STATUE_LOWER -> statue.setValue(StatueBlock.HALF, DoubleBlockHalf.LOWER);
                case STATUE_UPPER -> statue.setValue(StatueBlock.HALF, DoubleBlockHalf.UPPER);
                case PEDIMENT -> GreekTemple.GREEK_PEDIMENT.get().defaultBlockState();
                case ALTAR -> null;
            };
            if (s != null) {
                level.setBlock(p, s, FLAGS);
            }
        }
        level.setBlock(altar, GreekTemple.GREEK_ALTAR.get().defaultBlockState().setValue(GreekAltarBlock.FACING, Direction.SOUTH),
                Block.UPDATE_ALL);
        if (level.getBlockEntity(altar) instanceof GreekAltarBlockEntity be) {
            ShrineService.onPlaced(level, altar, owner, be, Direction.SOUTH);
        }
        io.github.happysnappingturtle.mythicage.MythicAge.LOGGER.info("快速验证：在 {} 搭了 {} 级神庙", altar.toShortString(), templeLevel);
    }

    /** 扫描已出结果时返回认定等级。 */
    public static OptionalInt scanned(ServerLevel level, BlockPos altar) {
        if (level.getBlockEntity(altar) instanceof GreekAltarBlockEntity be && be.hasResult() && !TempleService.pending(level, altar)) {
            return OptionalInt.of(be.verdict(TempleService.rules()).grantedLevel());
        }
        return OptionalInt.empty();
    }
}
