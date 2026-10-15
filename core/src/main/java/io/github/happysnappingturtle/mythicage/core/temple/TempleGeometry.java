package io.github.happysnappingturtle.mythicage.core.temple;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 扫描体内与位置有关的统计（T8a 设计文档 §3.2、§3.4，修订 01 §2.2）：露天祭坛、柱围屋顶、台基、可达地面、祈祷位。
 * 坐标都是扫描体局部坐标：+z 为神坛朝向（指向神庙），神坛位于 (halfWidth, below, back)。
 */
public final class TempleGeometry {
    /** 祈祷位离神坛的水平切比雪夫距离范围。 */
    public static final int SLOT_MIN_DIST = 1;
    public static final int SLOT_MAX_DIST = 4;
    /** 祈祷位相对神坛的高差范围。 */
    public static final int SLOT_MAX_DY = 1;
    /** 屋顶从柱脚最低处往上多少格开始算（留出头顶空间，台基本身不算屋顶）。 */
    public static final int ROOF_CLEARANCE = 2;

    private final TempleRules.Scan scan;
    private final int ax;
    private final int ay;
    private final int az;

    public TempleGeometry(TempleRules.Scan scan) {
        this.scan = scan;
        this.ax = scan.halfWidth();
        this.ay = scan.below();
        this.az = scan.back();
    }

    public int[] altar() {
        return new int[]{ax, ay, az};
    }

    public Grid3 newGrid() {
        return new Grid3(scan.sizeX(), scan.sizeY(), scan.sizeZ());
    }

    // ───────────── 露天祭坛 ─────────────

    /** 神坛正上方直到扫描体顶部都没有遮盖。 */
    public boolean openAltar(Grid3 cover) {
        for (int y = ay + 1; y < scan.sizeY(); y++) {
            if (cover.get(ax, y, az)) {
                return false;
            }
        }
        return true;
    }

    /** 神坛正上方第一处遮盖的局部坐标；没有时为 null。 */
    public int[] altarCover(Grid3 cover) {
        for (int y = ay + 1; y < scan.sizeY(); y++) {
            if (cover.get(ax, y, az)) {
                return new int[]{ax, y, az};
            }
        }
        return null;
    }

    // ───────────── 柱围屋顶与台基 ─────────────

    /**
     * 已计数柱子的外包矩形与柱脚最低处。
     *
     * @param minX     包含
     * @param maxX     包含
     * @param baseY    已计数柱子中最低的柱脚
     */
    public record ColumnBox(int minX, int maxX, int minZ, int maxZ, int baseY) {
        public int width() {
            return maxX - minX + 1;
        }

        public int depth() {
            return maxZ - minZ + 1;
        }
    }

    /** 没有已计数的柱子时为 null。 */
    public static ColumnBox columnBox(ColumnCounter.Result columns) {
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        int baseY = Integer.MAX_VALUE;
        for (ColumnCounter.Group g : columns.groups()) {
            if (!g.counted()) {
                continue;
            }
            for (int[] c : g.cells()) {
                minX = Math.min(minX, c[0]);
                maxX = Math.max(maxX, c[0]);
                minZ = Math.min(minZ, c[2]);
                maxZ = Math.max(maxZ, c[2]);
            }
            baseY = Math.min(baseY, g.base()[1]);
        }
        return baseY == Integer.MAX_VALUE ? null : new ColumnBox(minX, maxX, minZ, maxZ, baseY);
    }

    public record Roof(int covered, int area, List<int[]> uncovered) {
        public Roof {
            uncovered = List.copyOf(uncovered);
        }
    }

    /**
     * 柱围屋顶：矩形内每一列在「柱脚最低处 + {@link #ROOF_CLEARANCE}」以上有遮盖就算有顶；柱身本身不算遮盖。
     * 矩形任一边小于 minSide 时面积记 0（没有围出空间）。
     */
    public Roof roof(Grid3 cover, Grid3 columns, ColumnBox box, TempleRules.Roof rule) {
        if (box == null || box.width() < rule.minSide() || box.depth() < rule.minSide()) {
            return new Roof(0, 0, List.of());
        }
        int covered = 0;
        List<int[]> uncovered = new ArrayList<>();
        for (int z = box.minZ(); z <= box.maxZ(); z++) {
            for (int x = box.minX(); x <= box.maxX(); x++) {
                boolean hit = false;
                for (int y = box.baseY() + ROOF_CLEARANCE; y < scan.sizeY() && !hit; y++) {
                    hit = cover.get(x, y, z) && !columns.get(x, y, z);
                }
                if (hit) {
                    covered++;
                } else {
                    uncovered.add(new int[]{x, box.baseY(), z});
                }
            }
        }
        return new Roof(covered, box.width() * box.depth(), uncovered);
    }

    /** 柱脚高出神坛所在地面至少 1 格的已计数柱子数。 */
    public int raisedColumns(ColumnCounter.Result columns) {
        int n = 0;
        for (ColumnCounter.Group g : columns.groups()) {
            if (g.counted() && g.base()[1] >= ay + 1) {
                n++;
            }
        }
        return n;
    }

    /** 柱围矩形内、高出地面的位置有可达的站立格（台基可以走上去）。 */
    public boolean platformReachable(Grid3 reachable, ColumnBox box) {
        if (box == null || box.baseY() < ay + 1) {
            return false;
        }
        for (int z = box.minZ(); z <= box.maxZ(); z++) {
            for (int x = box.minX(); x <= box.maxX(); x++) {
                for (int y = ay + 1; y < scan.sizeY(); y++) {
                    if (reachable.get(x, y, z)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // ───────────── 地面 ─────────────

    /** 能走到的大理石地面：floor 中上方一格是可达站立格的格子（台基顶面算，屋顶不算）。 */
    public int reachableFloor(Grid3 floor, Grid3 reachable) {
        int n = 0;
        for (int y = 0; y < scan.sizeY() - 1; y++) {
            for (int z = 0; z < scan.sizeZ(); z++) {
                for (int x = 0; x < scan.sizeX(); x++) {
                    if (floor.get(x, y, z) && reachable.get(x, y + 1, z)) {
                        n++;
                    }
                }
            }
        }
        return n;
    }

    // ───────────── 站立与祈祷位 ─────────────

    /**
     * 可站立格：脚下格子有完整上表面，自身与头顶两格可通过（村民高 1.95）。
     * 最底层没有「脚下」、最顶层没有「头顶」，都不可站立。
     */
    public Grid3 standable(Grid3 sturdyTop, Grid3 passable) {
        Grid3 out = newGrid();
        for (int y = 1; y < scan.sizeY() - 1; y++) {
            for (int z = 0; z < scan.sizeZ(); z++) {
                for (int x = 0; x < scan.sizeX(); x++) {
                    if (sturdyTop.get(x, y - 1, z) && passable.get(x, y, z) && passable.get(x, y + 1, z)) {
                        out.set(x, y, z, true);
                    }
                }
            }
        }
        return out;
    }

    private int dist(int x, int z) {
        return Math.max(Math.abs(x - ax), Math.abs(z - az));
    }

    private boolean edge(int x, int z) {
        return x == 0 || z == 0 || x == scan.sizeX() - 1 || z == scan.sizeZ() - 1;
    }

    /**
     * 从扫描体水平边缘上的可站立格出发，在可站立格上做 BFS：四个水平方向，高差不超过 1。返回可达格。
     */
    public Grid3 reachable(Grid3 standable) {
        Grid3 seen = newGrid();
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        for (int y = 0; y < scan.sizeY(); y++) {
            for (int z = 0; z < scan.sizeZ(); z++) {
                for (int x = 0; x < scan.sizeX(); x++) {
                    if (standable.get(x, y, z) && edge(x, z)) {
                        seen.set(x, y, z, true);
                        queue.add(new int[]{x, y, z});
                    }
                }
            }
        }
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!queue.isEmpty()) {
            int[] c = queue.poll();
            for (int[] d : dirs) {
                for (int dy = -1; dy <= 1; dy++) {
                    int nx = c[0] + d[0];
                    int ny = c[1] + dy;
                    int nz = c[2] + d[1];
                    if (standable.get(nx, ny, nz) && !seen.get(nx, ny, nz)) {
                        seen.set(nx, ny, nz, true);
                        queue.add(new int[]{nx, ny, nz});
                    }
                }
            }
        }
        return seen;
    }

    /**
     * 选祈祷位：可达格中，水平距离 1–4、高差 ≤1、不是神坛本身；按距离、再按方位角排序，
     * 依次选取，任意两位的水平切比雪夫距离 ≥2（互不相邻），最多 limit 个。结果是确定的。
     */
    public List<int[]> prayerSlots(Grid3 reachable, int limit) {
        List<int[]> candidates = new ArrayList<>();
        for (int y = ay - SLOT_MAX_DY; y <= ay + SLOT_MAX_DY; y++) {
            for (int z = az - SLOT_MAX_DIST; z <= az + SLOT_MAX_DIST; z++) {
                for (int x = ax - SLOT_MAX_DIST; x <= ax + SLOT_MAX_DIST; x++) {
                    int d = dist(x, z);
                    if (d >= SLOT_MIN_DIST && d <= SLOT_MAX_DIST && reachable.get(x, y, z)) {
                        candidates.add(new int[]{x, y, z});
                    }
                }
            }
        }
        candidates.sort(Comparator
                .comparingInt((int[] c) -> (c[0] - ax) * (c[0] - ax) + (c[2] - az) * (c[2] - az))
                .thenComparingDouble(c -> Math.atan2(c[2] - az, c[0] - ax))
                .thenComparingInt(c -> Math.abs(c[1] - ay))
                .thenComparingInt(c -> c[1]));
        List<int[]> chosen = new ArrayList<>();
        for (int[] c : candidates) {
            if (chosen.size() >= limit) {
                break;
            }
            boolean clear = true;
            for (int[] o : chosen) {
                if (Math.max(Math.abs(o[0] - c[0]), Math.abs(o[2] - c[2])) < 2) {
                    clear = false;
                    break;
                }
            }
            if (clear) {
                chosen.add(c);
            }
        }
        return chosen;
    }
}
