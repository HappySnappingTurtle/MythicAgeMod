package io.github.happysnappingturtle.mythicage.core.structure;

import java.util.Arrays;
import java.util.OptionalInt;

/**
 * 遗迹选址（T22 §4.5、§5.5）：占地上 3 × 3 采样的地表高度（地面上第一个空格的 y）。纯函数。
 * 中心在水里（地表比水底高）不要；起伏（最高 − 最低）超过上限不要；合格时地面取 9 个样本的中位数。
 */
public final class SiteRules {
    private SiteRules() {
    }

    /** 合格就返回地面高度（地面上第一层的 y），否则为空。 */
    public static OptionalInt floor(int[] surface, boolean centerWet, int maxRelief) {
        if (surface.length == 0 || centerWet || relief(surface) > maxRelief) {
            return OptionalInt.empty();
        }
        int[] s = surface.clone();
        Arrays.sort(s);
        return OptionalInt.of(s[s.length / 2]);
    }

    public static int relief(int[] surface) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int h : surface) {
            min = Math.min(min, h);
            max = Math.max(max, h);
        }
        return max - min;
    }

    /** 中心那一格在水里：地表（含水面）比水底高。 */
    public static boolean wet(int surfaceTop, int oceanFloorTop) {
        return surfaceTop > oceanFloorTop;
    }

    /** 迷宫地面高度（§5.3）：地表 − depth，夹在 {@value #MAZE_MIN_Y} 与海平面 − 8 之间。 */
    public static int mazeFloor(int surface, int depth, int seaLevel) {
        return Math.max(MAZE_MIN_Y, Math.min(surface - depth, seaLevel - 8));
    }

    public static final int MAZE_MIN_Y = -40;
}
