package io.github.happysnappingturtle.mythicage.core.runtime;

/** tick 换算常量。 */
public final class Ticks {
    /** 一个 MC 日。 */
    public static final long DAY = 24000;

    private Ticks() {
    }

    /** 按日产率换算 ticks 个 tick 的产出。 */
    public static double perDay(double ratePerDay, long ticks) {
        return ratePerDay * ticks / DAY;
    }
}
