package io.github.happysnappingturtle.mythicage.core.village.gen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 坡地建筑的门前通道（GA-14）：建筑切进山坡、外圈砌了挡土墙时，从门口往外开一段每列升 1 格的台阶，直到与外面地表齐平。
 * <p>
 * 生成时每个区块各自整地、读不到别的区块的可靠高度，所以按列独立判断：门外第 j 列（j = 1…{@link #MAX_STEPS}，
 * 从占地外第一列算起）台阶顶面应在「地面 + j」；这一列地表比它高（{@link #needsStair}）就在 {@link #stairY} 放一级朝外上升的台阶，
 * 否则不动。纯函数，五文明共用。
 */
public final class DoorAccess {
    public static final int MAX_STEPS = 5;

    /** 通道里的一列：世界 x、z，第几级（从 1 起），台阶朝向（= 门朝外的方向，台阶向这边升高）。 */
    public record Column(int x, int z, int step, String facing) {
    }

    private DoorAccess() {
    }

    /** 门在世界里的位置（门下半格）、朝外方向。 */
    public record Door(Pos pos, String facing, int width) {
    }

    /** 建筑数据的门换到世界坐标（按放置时的原点与转向）。 */
    public static Door door(BuildingSpec spec, Pos origin, Turn turn) {
        return new Door(TemplateTransform.toWorld(origin, spec.door(), turn), turn.rotateFacing(spec.doorFacing()), spec.doorWidth());
    }

    /**
     * 通道各列：沿门的朝外方向，从占地包围盒外的第一列起，共 {@code steps} 级；每级 {@code width} 列，第二列起在朝向的顺时针一侧。
     */
    public static List<Column> corridor(Box box, Door door, int steps) {
        int[] out = step(door.facing());
        int[] lat = step(Turn.R90.rotateFacing(door.facing()));
        int k0 = 1;
        while (box.containsXZ(door.pos().x() + out[0] * k0, door.pos().z() + out[1] * k0) && k0 < 64) {
            k0++;
        }
        List<Column> cols = new ArrayList<>();
        for (int j = 1; j <= steps; j++) {
            int k = k0 + j - 1;
            for (int w = 0; w < door.width(); w++) {
                cols.add(new Column(door.pos().x() + out[0] * k + lat[0] * w, door.pos().z() + out[1] * k + lat[1] * w, j,
                        door.facing().toLowerCase(Locale.ROOT)));
            }
        }
        return cols;
    }

    /** 这一列的地表（最上面一个实心方块的 y）比台阶线高：要台阶。floor 是门内地面上第一格的 y（站立高度）。 */
    public static boolean needsStair(int top, int floor, int step) {
        return top >= floor + step;
    }

    /** 第 j 级台阶方块的 y（站在它上面的高度是 floor + j）。 */
    public static int stairY(int floor, int step) {
        return floor + step - 1;
    }

    static int[] step(String facing) {
        return switch (facing.toLowerCase(Locale.ROOT)) {
            case "north" -> new int[]{0, -1};
            case "south" -> new int[]{0, 1};
            case "east" -> new int[]{1, 0};
            case "west" -> new int[]{-1, 0};
            default -> throw new IllegalArgumentException("未知朝向：" + facing);
        };
    }
}
