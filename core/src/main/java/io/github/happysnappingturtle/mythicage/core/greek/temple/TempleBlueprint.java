package io.github.happysnappingturtle.mythicage.core.greek.temple;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 快速验证用的希腊神庙蓝图（GA-3 设计文档 §4.5）：一级（小神庙）与二级（前柱式）。
 * 坐标相对祭坛：祭坛在 (0, 0, 0)、朝南（+z），dy = 0 是祭坛所在的站立层，dy = -1 是地面。
 * 布局与 {@code TempleGameTests} 的一级、二级算例相同（柱围 9×9 在祭坛前方 4–12 格），
 * 只有楣石放在屋顶前沿，不悬在空中。
 */
public final class TempleBlueprint {
    public enum Part { PLATFORM, FLOOR, PILLAR, ROOF, BRAZIER, STATUE_LOWER, STATUE_UPPER, PEDIMENT, ALTAR }

    public record Block(int dx, int dy, int dz, Part part) {
    }

    /** 搭建前要清空、要垫实的范围（含两端）。 */
    public static final int MIN_DX = -7, MAX_DX = 7, MIN_DZ = -1, MAX_DZ = 16, MAX_DY = 9;

    private static final int[][] COLUMNS = {
            {-4, 4}, {4, 4}, {-4, 12}, {4, 12}, {0, 4}, {0, 12}, {-4, 8}, {4, 8}};
    private static final int[][] BRAZIERS = {{-2, 10}, {2, 10}};
    private static final int[] STATUE = {0, 10};

    private TempleBlueprint() {
    }

    /** 等级 1 或 2 的方块清单；同一坐标只出现一次（后放的覆盖先放的）。 */
    public static List<Block> of(int level) {
        if (level != 1 && level != 2) {
            throw new IllegalArgumentException("只支持一级、二级神庙：" + level);
        }
        boolean l2 = level == 2;
        int platform = l2 ? 1 : 0;
        int base = platform;
        Map<String, Block> out = new LinkedHashMap<>();
        if (l2) {
            for (int dx = -6; dx <= 6; dx++) {
                for (int dz = 2; dz <= 14; dz++) {
                    put(out, new Block(dx, 0, dz, Part.PLATFORM));
                }
            }
        } else {
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = 4; dz <= 12; dz++) {
                    put(out, new Block(dx, -1, dz, Part.FLOOR));
                }
            }
        }
        int columns = l2 ? 8 : 4;
        for (int i = 0; i < columns; i++) {
            for (int y = base; y < base + 3; y++) {
                put(out, new Block(COLUMNS[i][0], y, COLUMNS[i][1], Part.PILLAR));
            }
        }
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = 4; dz <= 12; dz++) {
                put(out, new Block(dx, base + 3, dz, Part.ROOF));
            }
        }
        int braziers = l2 ? 2 : 1;
        for (int i = 0; i < braziers; i++) {
            put(out, new Block(BRAZIERS[i][0], base, BRAZIERS[i][1], Part.BRAZIER));
        }
        if (l2) {
            put(out, new Block(STATUE[0], base, STATUE[1], Part.STATUE_LOWER));
            put(out, new Block(STATUE[0], base + 1, STATUE[1], Part.STATUE_UPPER));
            put(out, new Block(-1, base + 4, 4, Part.PEDIMENT));
            put(out, new Block(1, base + 4, 4, Part.PEDIMENT));
        }
        put(out, new Block(0, 0, 0, Part.ALTAR));
        return new ArrayList<>(out.values());
    }

    private static void put(Map<String, Block> out, Block b) {
        String k = b.dx() + "," + b.dy() + "," + b.dz();
        out.remove(k);
        out.put(k, b);
    }

    public static long count(List<Block> blocks, Part part) {
        return blocks.stream().filter(b -> b.part() == part).count();
    }
}
