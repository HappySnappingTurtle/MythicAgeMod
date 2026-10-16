package io.github.happysnappingturtle.mythicage.core.greek.temple;

import io.github.happysnappingturtle.mythicage.core.scan.Grid3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * 柱计数（T8a 设计文档 §3.2）：柱方块按 6 连通分组，每组算一根柱；组内最长竖直连续段达到门槛才计数。
 * 同一根柱不重复计数，并排砌成的柱束或柱墙也只算一根。
 */
public final class ColumnCounter {
    private ColumnCounter() {
    }

    /**
     * @param cells     组内全部格子 [x, y, z]
     * @param longestRun 组内最长的竖直连续段
     * @param counted   是否达到门槛
     */
    public record Group(List<int[]> cells, int longestRun, boolean counted) {
        /** 组的代表位置：最低、再按 x、z 最小的格子。 */
        public int[] base() {
            int[] best = cells.getFirst();
            for (int[] c : cells) {
                if (c[1] < best[1] || c[1] == best[1] && (c[0] < best[0] || c[0] == best[0] && c[2] < best[2])) {
                    best = c;
                }
            }
            return best;
        }
    }

    public record Result(int count, List<Group> groups) {
        public Result {
            groups = List.copyOf(groups);
        }
    }

    private static final int[][] DIRS = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

    public static Result count(Grid3 columns, int minHeight) {
        Grid3 seen = new Grid3(columns.sizeX(), columns.sizeY(), columns.sizeZ());
        List<Group> groups = new ArrayList<>();
        int count = 0;
        for (int y = 0; y < columns.sizeY(); y++) {
            for (int z = 0; z < columns.sizeZ(); z++) {
                for (int x = 0; x < columns.sizeX(); x++) {
                    if (!columns.get(x, y, z) || seen.get(x, y, z)) {
                        continue;
                    }
                    List<int[]> cells = new ArrayList<>();
                    ArrayDeque<int[]> queue = new ArrayDeque<>();
                    queue.add(new int[]{x, y, z});
                    seen.set(x, y, z, true);
                    while (!queue.isEmpty()) {
                        int[] c = queue.poll();
                        cells.add(c);
                        for (int[] d : DIRS) {
                            int nx = c[0] + d[0];
                            int ny = c[1] + d[1];
                            int nz = c[2] + d[2];
                            if (columns.get(nx, ny, nz) && !seen.get(nx, ny, nz)) {
                                seen.set(nx, ny, nz, true);
                                queue.add(new int[]{nx, ny, nz});
                            }
                        }
                    }
                    int longest = longestRun(columns, cells);
                    boolean counted = longest >= minHeight;
                    if (counted) {
                        count++;
                    }
                    groups.add(new Group(cells, longest, counted));
                }
            }
        }
        return new Result(count, groups);
    }

    /** 组内各格向上数连续的柱方块，取最长；只从段的最低格开始数。 */
    private static int longestRun(Grid3 columns, List<int[]> cells) {
        int best = 0;
        for (int[] c : cells) {
            if (columns.get(c[0], c[1] - 1, c[2])) {
                continue;
            }
            int run = 0;
            while (columns.get(c[0], c[1] + run, c[2])) {
                run++;
            }
            best = Math.max(best, run);
        }
        return best;
    }
}
