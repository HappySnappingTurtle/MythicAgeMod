package io.github.happysnappingturtle.mythicage.core.village.life;

import java.util.UUID;

/**
 * 装饰性活动的派人规则（T19 设计文档 §1.10 J2）：人数与概率按「村庄、日子、时段」派生，同一段里多次规划结果相同。
 */
public final class LifeRules {
    private LifeRules() {
    }

    /** 村庄 + MC 日 + 时段起点派生的种子（splitmix64）。 */
    public static long seed(UUID village, long day, int windowFrom, String activity) {
        long z = village.getMostSignificantBits() ^ village.getLeastSignificantBits() ^ day * 0x9E3779B97F4A7C15L
                ^ ((long) windowFrom << 20) ^ activity.hashCode();
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** 0–1 之间的确定值。 */
    public static double unit(long seed) {
        return (seed >>> 11) * 0x1.0p-53;
    }

    /**
     * 这一段派几人：先按概率决定有没有，再在 [min, max] 里取，且不超过人口的 1/3（人口 ≥ 1 时至少 1）。
     * 没有锚点或没有人为 0。
     */
    public static int people(LifeData.Activity a, int population, int anchors, long seed) {
        if (population <= 0 || anchors <= 0 || a.max() <= 0) {
            return 0;
        }
        if (a.chance() < 1 && unit(seed) >= a.chance()) {
            return 0;
        }
        int span = Math.max(0, a.max() - a.min()) + 1;
        int n = a.min() + (int) Math.floorMod(seed >>> 7, (long) span);
        return Math.min(n, Math.max(1, population / 3));
    }
}
