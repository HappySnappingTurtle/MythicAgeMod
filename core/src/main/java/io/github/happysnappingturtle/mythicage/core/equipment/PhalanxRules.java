package io.github.happysnappingturtle.mythicage.core.equipment;

/** 方阵（EQ-GR-08）：每名友方 +perAlly 护甲，最多 max。 */
public final class PhalanxRules {
    private PhalanxRules() {
    }

    public static int bonus(int allies, int perAlly, int max) {
        return Math.max(0, Math.min(allies * perAlly, max));
    }
}
