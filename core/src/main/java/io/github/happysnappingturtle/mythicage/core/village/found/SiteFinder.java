package io.github.happysnappingturtle.mythicage.core.village.found;

import io.github.happysnappingturtle.mythicage.core.village.gen.Box;

import java.util.List;
import java.util.Optional;

/**
 * 临时找地（T19 设计文档 §13.2，V0 §5.4，D6）：没有预留地块的村庄（原版立村、奠基、或生成时没留地块的建筑）扩建时，
 * 在已建范围外 {@link #MIN_RING}–{@link #MAX_RING} 格内找一块 {@code w × d} 的地：
 * 每一列都已加载、不是水、没有玩家方块，地面高差 ≤ {@link #MAX_RISE}，不压到已建范围（外扩 {@link #MARGIN} 格）。
 * 候选按「高差小、离村子近」排序取第一个。纯函数，地形由调用方采样。
 */
public final class SiteFinder {
    public static final int MIN_RING = 8, MAX_RING = 32, MAX_RISE = 2, MARGIN = 2, STEP = 2;

    private SiteFinder() {
    }

    /** 一列地形：已加载才有值。 */
    public record Column(int ground, boolean water, boolean playerBlock) {
    }

    @FunctionalInterface
    public interface Terrain {
        /** 没加载返回 null。 */
        Column at(int x, int z);
    }

    /** @param ground 选中地块的整平高度（取中位数） */
    public record Site(Box box, int ground, int rise) {
    }

    /** 已建范围的外接盒（水平）。 */
    static Box union(List<Box> built) {
        int x0 = Integer.MAX_VALUE, z0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, z1 = Integer.MIN_VALUE;
        int y0 = Integer.MAX_VALUE, y1 = Integer.MIN_VALUE;
        for (Box b : built) {
            x0 = Math.min(x0, b.minX());
            z0 = Math.min(z0, b.minZ());
            x1 = Math.max(x1, b.maxX());
            z1 = Math.max(z1, b.maxZ());
            y0 = Math.min(y0, b.minY());
            y1 = Math.max(y1, b.maxY());
        }
        return new Box(x0, y0, z0, x1, y1, z1);
    }

    public static Optional<Site> find(List<Box> built, int w, int d, Terrain t) {
        if (built.isEmpty() || w <= 0 || d <= 0) {
            return Optional.empty();
        }
        Box u = union(built);
        Site best = null;
        double bestScore = Double.MAX_VALUE;
        for (int x = u.minX() - MAX_RING - w; x <= u.maxX() + MAX_RING; x += STEP) {
            for (int z = u.minZ() - MAX_RING - d; z <= u.maxZ() + MAX_RING; z += STEP) {
                Box b = new Box(x, u.minY(), z, x + w - 1, u.maxY(), z + d - 1);
                double gap = gapToBuilt(b, u);
                if (gap < MIN_RING || gap > MAX_RING || overlaps(b, built)) {
                    continue;
                }
                Optional<Site> s = check(b, t);
                if (s.isEmpty()) {
                    continue;
                }
                double score = s.get().rise() * 10 + gap;
                if (score < bestScore) {
                    bestScore = score;
                    best = s.get();
                }
            }
        }
        return Optional.ofNullable(best);
    }

    /** 两个盒在水平面上的最近距离（相交为 0）。 */
    static double gapToBuilt(Box a, Box b) {
        int dx = Math.max(0, Math.max(b.minX() - a.maxX(), a.minX() - b.maxX()));
        int dz = Math.max(0, Math.max(b.minZ() - a.maxZ(), a.minZ() - b.maxZ()));
        return Math.sqrt((double) dx * dx + (double) dz * dz);
    }

    static boolean overlaps(Box b, List<Box> built) {
        return built.stream().anyMatch(o -> o.inflateXZ(MARGIN).intersectsXZ(b));
    }

    /** 整块地逐列检查；全部合格返回整平高度（中位数）与高差。 */
    static Optional<Site> check(Box b, Terrain t) {
        int n = b.sizeX() * b.sizeZ();
        int[] heights = new int[n];
        int i = 0;
        int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
        for (int x = b.minX(); x <= b.maxX(); x++) {
            for (int z = b.minZ(); z <= b.maxZ(); z++) {
                Column c = t.at(x, z);
                if (c == null || c.water() || c.playerBlock()) {
                    return Optional.empty();
                }
                heights[i++] = c.ground();
                lo = Math.min(lo, c.ground());
                hi = Math.max(hi, c.ground());
                if (hi - lo > MAX_RISE) {
                    return Optional.empty();
                }
            }
        }
        java.util.Arrays.sort(heights);
        int ground = heights[n / 2];
        return Optional.of(new Site(new Box(b.minX(), ground, b.minZ(), b.maxX(), ground, b.maxZ()), ground, hi - lo));
    }
}
