package io.github.happysnappingturtle.mythicage.core.greek.prayer;

import java.util.List;

/**
 * 信徒祈祷的产出公式（T10 设计文档 §5.1；01_希腊 §3.3）。纯函数，不依赖 Minecraft。
 * <pre>
 * 一次采样（Δt 刻）中，玩家全部神坛合计 n 名在祈祷且计入的信徒：
 *   ΔF_i = B/1200 × Δt × √min(n, 24) / n × n_i × T_i × W_i × (1 + R)
 * </pre>
 * √n 按玩家合计，多建神坛只增加祈祷位上限，不能绕过递减。
 */
public final class PrayerFormula {
    /** 基础祈祷率 B：恩宠 / 分钟。 */
    public static final double BASE_PER_MINUTE = 0.6;
    public static final int TICKS_PER_MINUTE = 1200;
    /** 全局祈祷人数上限 N_cap。 */
    public static final int N_CAP = 24;
    /** 圣物加成 R 的上限。 */
    public static final double R_MAX = 0.15;
    /** 露天祭坛下雨时的系数 W。 */
    public static final double RAIN_FACTOR = 0.5;
    /** 祈祷时段（dayTime 取模 24000）：[2000, 9000)。 */
    public static final int WINDOW_START = 2000;
    public static final int WINDOW_END = 9000;
    /** 满一个祈祷时段的基础日产：B × 时段分钟数 = 0.6 × 7000 / 1200 = 3.5。 */
    public static final double DAILY_BASE = BASE_PER_MINUTE * (WINDOW_END - WINDOW_START) / TICKS_PER_MINUTE;

    private PrayerFormula() {
    }

    /**
     * 一座神坛在一次采样中的状态。
     *
     * @param praying 正在祈祷且计入的信徒数 n_i
     * @param tier    神庙认定等级系数 T_i
     * @param weather 天气系数 W_i（露天祭坛下雨 0.5，否则 1）
     */
    public record Altar(int praying, double tier, double weather) {
    }

    public static double clampR(double r) {
        return Math.max(0, Math.min(R_MAX, r));
    }

    /** 祈祷时段判定；dayTime 可以超过 24000（取模），负数也按模处理。 */
    public static boolean inWindow(long dayTime) {
        long t = Math.floorMod(dayTime, 24000L);
        return t >= WINDOW_START && t < WINDOW_END;
    }

    /** 一次采样分给各神坛的产出，顺序与输入一致；n = 0 时全为 0。 */
    public static double[] distribute(List<Altar> altars, double r, long dtTicks) {
        double[] out = new double[altars.size()];
        int n = 0;
        for (Altar a : altars) {
            n += Math.max(0, a.praying());
        }
        if (n == 0 || dtTicks <= 0) {
            return out;
        }
        double perHead = BASE_PER_MINUTE / TICKS_PER_MINUTE * dtTicks * Math.sqrt(Math.min(n, N_CAP)) / n
                * (1 + clampR(r));
        for (int i = 0; i < out.length; i++) {
            Altar a = altars.get(i);
            out[i] = perHead * Math.max(0, a.praying()) * a.tier() * a.weather();
        }
        return out;
    }

    /** 整个祈祷时段的日产（01_希腊 §3.3 算例表）：3.5 × √min(n, 24) × T̄ × (1 + R)。 */
    public static double daily(int n, double tierAverage, double r) {
        return DAILY_BASE * Math.sqrt(Math.min(Math.max(0, n), N_CAP)) * tierAverage * (1 + clampR(r));
    }

    /**
     * 单座神坛的快照日产估算（T10 设计文档 §5.2，偏保守）：3.5 × √min(信徒数, 祈祷位, 24) × T × (1 + R)。
     * 检视界面与信仰面板的「预计日产」也用它。
     */
    public static double estimate(int believers, int slots, double tier, double r) {
        return daily(Math.min(Math.max(0, believers), Math.max(0, slots)), tier, r);
    }
}
