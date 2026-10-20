package io.github.happysnappingturtle.mythicage.core.village.gen;

import java.util.List;
import java.util.Locale;

/**
 * 模板旋转，与 MC 的 {@code Rotation} 一一对应（绕模板原点，俯视顺时针）：
 * R90 = CLOCKWISE_90：(x, z) → (−z, x)；R180：(−x, −z)；R270 = COUNTERCLOCKWISE_90：(z, −x)。
 */
public enum Turn {
    R0, R90, R180, R270;

    private static final List<String> FACINGS = List.of("north", "east", "south", "west");

    public int rx(int x, int z) {
        return switch (this) {
            case R0 -> x;
            case R90 -> -z;
            case R180 -> -x;
            case R270 -> z;
        };
    }

    public int rz(int x, int z) {
        return switch (this) {
            case R0 -> z;
            case R90 -> x;
            case R180 -> -z;
            case R270 -> -x;
        };
    }

    public Pos apply(Pos p) {
        return new Pos(rx(p.x(), p.z()), p.y(), rz(p.x(), p.z()));
    }

    /** 朝向（north / east / south / west）旋转后的朝向；俯视顺时针。 */
    public String rotateFacing(String facing) {
        int i = FACINGS.indexOf(facing.toLowerCase(Locale.ROOT));
        if (i < 0) {
            throw new IllegalArgumentException("未知朝向：" + facing);
        }
        return FACINGS.get((i + ordinal()) % 4);
    }

    /** 把朝向 from 转到 to 需要的旋转。 */
    public static Turn between(String from, String to) {
        int a = FACINGS.indexOf(from.toLowerCase(Locale.ROOT));
        int b = FACINGS.indexOf(to.toLowerCase(Locale.ROOT));
        if (a < 0 || b < 0) {
            throw new IllegalArgumentException("未知朝向：" + from + " / " + to);
        }
        return values()[Math.floorMod(b - a, 4)];
    }

    public Turn then(Turn other) {
        return values()[(ordinal() + other.ordinal()) % 4];
    }
}
