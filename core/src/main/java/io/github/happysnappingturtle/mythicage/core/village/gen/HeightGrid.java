package io.github.happysnappingturtle.mythicage.core.village.gen;

import java.util.Arrays;

/**
 * 规划用的高度网格（T13b §4.1）：以 (cx, cz) 为中心、半径 radius、间距 step 采样一次，之后的查询都用最近的网格点，
 * 不再碰地形采样器。半径 60、间距 6 时 21 × 21 = 441 个点、882 次采样（设计 D5 预算内）。
 */
public final class HeightGrid {
    private final int cx;
    private final int cz;
    private final int radius;
    private final int step;
    private final int n;
    private final int[] surface;
    private final boolean[] water;

    public HeightGrid(TerrainSampler t, int cx, int cz, int radius, int step) {
        this.cx = cx;
        this.cz = cz;
        this.radius = radius;
        this.step = step;
        this.n = 2 * (radius / step) + 1;
        this.surface = new int[n * n];
        this.water = new boolean[n * n];
        int half = radius / step;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                int x = cx + (i - half) * step;
                int z = cz + (j - half) * step;
                int s = t.surface(x, z);
                surface[i * n + j] = s;
                water[i * n + j] = s > t.floor(x, z);
            }
        }
    }

    private int index(int x, int z) {
        int half = radius / step;
        int i = Math.clamp(Math.round((float) (x - cx) / step) + half, 0, n - 1);
        int j = Math.clamp(Math.round((float) (z - cz) / step) + half, 0, n - 1);
        return i * n + j;
    }

    public boolean inside(int x, int z) {
        return Math.abs(x - cx) <= radius && Math.abs(z - cz) <= radius;
    }

    /** 地表上第一格（含水面）。 */
    public int surface(int x, int z) {
        return surface[index(x, z)];
    }

    public boolean water(int x, int z) {
        return water[index(x, z)];
    }

    public int centerX() {
        return cx;
    }

    public int centerZ() {
        return cz;
    }

    public int radius() {
        return radius;
    }

    public int step() {
        return step;
    }

    /** 一块地上的采样（四角 + 内部网格点）。 */
    public record Footprint(int ground, int relief, boolean wet, int min, int max) {
    }

    public Footprint footprint(Box b) {
        int[] xs = sampleAxis(b.minX(), b.maxX());
        int[] zs = sampleAxis(b.minZ(), b.maxZ());
        int[] hs = new int[xs.length * zs.length];
        boolean wet = false;
        int k = 0;
        for (int x : xs) {
            for (int z : zs) {
                hs[k++] = surface(x, z);
                wet |= water(x, z);
            }
        }
        Arrays.sort(hs);
        return new Footprint(hs[hs.length / 2], hs[hs.length - 1] - hs[0], wet, hs[0], hs[hs.length - 1]);
    }

    private int[] sampleAxis(int lo, int hi) {
        int count = Math.max(2, (hi - lo) / step + 2);
        int[] out = new int[count];
        for (int i = 0; i < count; i++) {
            out[i] = lo + (int) Math.round((double) (hi - lo) * i / (count - 1));
        }
        return out;
    }

    /** 区域内的网格点全是水面。 */
    public boolean allWater(Box b) {
        for (int x : sampleAxis(b.minX(), b.maxX())) {
            for (int z : sampleAxis(b.minZ(), b.maxZ())) {
                if (!water(x, z)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** 半径 r 内的水面网格点数。 */
    public int waterNear(int x, int z, int r) {
        int c = 0;
        for (int dx = -r; dx <= r; dx += step) {
            for (int dz = -r; dz <= r; dz += step) {
                if (inside(x + dx, z + dz) && water(x + dx, z + dz)) {
                    c++;
                }
            }
        }
        return c;
    }

    /** 区域内水面格的比例（判断临海）。 */
    public double waterFraction() {
        int w = 0;
        for (boolean b : water) {
            w += b ? 1 : 0;
        }
        return (double) w / water.length;
    }
}
