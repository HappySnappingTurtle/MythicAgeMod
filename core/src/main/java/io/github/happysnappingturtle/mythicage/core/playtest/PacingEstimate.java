package io.github.happysnappingturtle.mythicage.core.playtest;

import io.github.happysnappingturtle.mythicage.core.greek.prayer.PrayerFormula;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

/**
 * 节奏估算（GA-3 设计文档 §6）：恩宠缺口要祈祷几个游戏日。只算祈祷产出，不算兑换花费、下雨和离线。
 */
public final class PacingEstimate {
    /** 一个游戏日的真实分钟数。 */
    public static final double MINUTES_PER_DAY = 20;

    /**
     * @param believers 祈祷信徒数（受神庙祈祷位与全局上限约束，由调用方给出）
     * @param tier      神庙系数 T
     */
    public record Row(int believers, int templeLevel, double tier, double daily, double days, double minutes) {
    }

    private PacingEstimate() {
    }

    /** 缺口 deficit 要几天；已满足为 0；信徒为 0 时不可达（空）。 */
    public static OptionalDouble days(double deficit, int believers, double tier, double relic) {
        if (deficit <= 0) {
            return OptionalDouble.of(0);
        }
        double daily = PrayerFormula.daily(believers, tier, relic);
        if (daily <= 0) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(deficit / daily);
    }

    /**
     * 估算表：每个信徒数 × 每级神庙一行（GA-4 起祈祷位不再限制信徒数）。
     *
     * @param levels 神庙等级 → {祈祷位, 系数}，下标即等级；只用系数
     */
    public static List<Row> table(double deficit, int[] believers, int[] levelIds, double[][] levels) {
        List<Row> out = new ArrayList<>();
        for (int level : levelIds) {
            double tier = levels[level][1];
            for (int n : believers) {
                int counted = n;
                double daily = PrayerFormula.daily(counted, tier, 0);
                double d = days(deficit, counted, tier, 0).orElse(Double.POSITIVE_INFINITY);
                out.add(new Row(counted, level, tier, daily, d, d * MINUTES_PER_DAY));
            }
        }
        return out;
    }
}
