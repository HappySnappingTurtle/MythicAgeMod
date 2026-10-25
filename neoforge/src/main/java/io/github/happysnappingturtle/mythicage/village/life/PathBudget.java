package io.github.happysnappingturtle.mythicage.village.life;

/**
 * 全服每刻新寻路的上限（V1 §11、T19 §1.10 J5）：居民每次算路先领一个名额，本刻用完了就推迟到下一刻再算。
 * 节庆游行几十人同时出发也不会在同一刻挤满寻路（TC-T19-124）。每刻开始由调度器清零。
 */
public final class PathBudget {
    public static final int PER_TICK = 8;
    private static int used;
    private static int peak;

    private PathBudget() {
    }

    public static void reset() {
        used = 0;
    }

    /** 领一个名额；没有了返回 false。 */
    public static boolean take() {
        if (used >= PER_TICK) {
            return false;
        }
        used++;
        peak = Math.max(peak, used);
        return true;
    }

    /** 只供 GameTest：自上次清零以来单刻最多用了几个名额。 */
    public static int peakForTest() {
        return peak;
    }

    public static void clearPeakForTest() {
        peak = 0;
    }
}
