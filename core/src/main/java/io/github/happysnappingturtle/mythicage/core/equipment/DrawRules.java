package io.github.happysnappingturtle.mythicage.core.equipment;

/** 拉弓力度（EQ-GR-12）：原版 {@code BowItem.getPowerForTime} 的公式，把满弓的 20 刻换成 fullTicks。 */
public final class DrawRules {
    public static final int VANILLA_FULL_TICKS = 20;

    private DrawRules() {
    }

    public static float power(int useTicks, int fullTicks) {
        float f = (float) useTicks / fullTicks;
        f = (f * f + f * 2.0F) / 3.0F;
        return Math.min(f, 1.0F);
    }
}
