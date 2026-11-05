package io.github.happysnappingturtle.mythicage.core.greek.prayer;

/**
 * 信徒祈祷的产出公式（T10 设计文档 §5；GA-4 改为按「今日信徒」计）。纯函数，不依赖 Minecraft。
 * <pre>
 * 一天：F_day = 3.5 × √min(N, 24) × T × W × (1 + R) × (1 + O) × M
 * 第 N 名今日信徒祈祷满 {@link #PRAY_TICKS} 刻时入一笔：
 *   ΔF = 3.5 × T × W × (1 + R) × (1 + O) × M × (√min(N, 24) − √min(N − 1, 24))
 * </pre>
 * N 按玩家全部神坛合计，所以多建神坛不能绕过递减；一天各笔加起来正好是 F_day。
 */
public final class PrayerFormula {
    /** 基础祈祷率 B：恩宠 / 分钟。 */
    public static final double BASE_PER_MINUTE = 0.6;
    public static final int TICKS_PER_MINUTE = 1200;
    /** 全局今日信徒上限 N_cap。 */
    public static final int N_CAP = 24;
    /** 圣物加成 R 的上限。 */
    public static final double R_MAX = 0.15;
    /** 露天祭坛下雨时的系数 W。 */
    public static final double RAIN_FACTOR = 0.5;
    /** 祈祷时段（dayTime 取模 24000）：[2000, 9000)。 */
    public static final int WINDOW_START = 2000;
    public static final int WINDOW_END = 9000;
    /** 一次祈祷要满这么多刻才算一名今日信徒（GA-4）。 */
    public static final int PRAY_TICKS = 600;
    /** 一天的基数：B × 时段分钟数 = 0.6 × 7000 / 1200 = 3.5。 */
    public static final double DAILY_BASE = BASE_PER_MINUTE * (WINDOW_END - WINDOW_START) / TICKS_PER_MINUTE;

    private PrayerFormula() {
    }

    public static double clampR(double r) {
        return Math.max(0, Math.min(R_MAX, r));
    }

    /** 祈祷时段判定；dayTime 可以超过 24000（取模），负数也按模处理。 */
    public static boolean inWindow(long dayTime) {
        long t = Math.floorMod(dayTime, 24000L);
        return t >= WINDOW_START && t < WINDOW_END;
    }

    private static double root(int n) {
        return Math.sqrt(Math.min(Math.max(0, n), N_CAP));
    }

    /**
     * 第 n 名今日信徒（这位主人各神坛合计，已含这一位）入账的一笔。n ≤ 0 时为 0；第 25 名起为 0。
     *
     * @param tier     这座神坛的神庙系数 T
     * @param weather  天气系数 W
     * @param r        圣物加成（夹到 [0, 0.15]）
     * @param offering 当天这座神坛的供奉加成 O（没献为 0）
     * @param mult     服务器倍率 M
     */
    public static double credit(int n, double tier, double weather, double r, double offering, double mult) {
        if (n <= 0) {
            return 0;
        }
        return DAILY_BASE * tier * weather * (1 + clampR(r)) * (1 + Math.max(0, offering)) * Math.max(0, mult)
                * (root(n) - root(n - 1));
    }

    /** 一天的产出：3.5 × √min(n, 24) × T × (1 + R)（晴天、不献供、倍率 1；01_希腊 §3.3 算例表）。 */
    public static double daily(int n, double tierAverage, double r) {
        return daily(n, tierAverage, r, 0, 1);
    }

    /** 一天的产出，含供奉加成与倍率。 */
    public static double daily(int n, double tierAverage, double r, double offering, double mult) {
        return DAILY_BASE * root(n) * tierAverage * (1 + clampR(r)) * (1 + Math.max(0, offering)) * Math.max(0, mult);
    }

    /**
     * 单座神坛的预计日产（检视界面与信仰面板）：按近 7 日信徒数，不再受祈祷位限制（GA-4）。
     */
    public static double estimate(int believers, double tier, double r, double mult) {
        return daily(believers, tier, r, 0, mult);
    }
}
