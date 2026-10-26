package io.github.happysnappingturtle.mythicage.core.boss;

/**
 * 弥诺陶洛斯选招（T23 §6.2）。确定性：同样的局面选同样的招。
 * 近身且斧劈可用 → 斧劈；{@value #CHARGE_MIN}–{@value #CHARGE_MAX} 格、视线通且冲锋可用 → 阶段二连冲、阶段一冲锋；
 * 阶段二、≥ {@value #THROW_MIN} 格且掷石可用 → 掷石；其余追击。正在出招时不选新招。
 */
public final class MoveScheduler {
    public static final double AXE_RANGE = 4.0;
    public static final double CHARGE_MIN = 6.0;
    public static final double CHARGE_MAX = 16.0;
    public static final double THROW_MIN = 6.0;
    public static final int AXE_COOLDOWN = 60;
    public static final int CHARGE_COOLDOWN = 160;
    public static final int DOUBLE_CHARGE_COOLDOWN = 240;
    public static final int THROW_COOLDOWN = 160;

    public enum Move { NONE, AXE, CHARGE, DOUBLE_CHARGE, THROW, CHASE }

    /**
     * @param axeReady    斧劈冷却已过
     * @param chargeReady 冲锋 / 连冲冷却已过（两者共用）
     * @param throwReady  掷石冷却已过
     */
    public record Situation(boolean phaseTwo, double distance, boolean lineOfSight, boolean busy,
                            boolean axeReady, boolean chargeReady, boolean throwReady) {
    }

    private MoveScheduler() {
    }

    public static Move choose(Situation s) {
        if (s.busy()) {
            return Move.NONE;
        }
        if (s.distance() <= AXE_RANGE && s.axeReady()) {
            return Move.AXE;
        }
        if (s.distance() >= CHARGE_MIN && s.distance() <= CHARGE_MAX && s.lineOfSight() && s.chargeReady()) {
            return s.phaseTwo() ? Move.DOUBLE_CHARGE : Move.CHARGE;
        }
        if (s.phaseTwo() && s.distance() >= THROW_MIN && s.throwReady()) {
            return Move.THROW;
        }
        return Move.CHASE;
    }

    /** 出招后的冷却刻数。 */
    public static int cooldown(Move m) {
        return switch (m) {
            case AXE -> AXE_COOLDOWN;
            case CHARGE -> CHARGE_COOLDOWN;
            case DOUBLE_CHARGE -> DOUBLE_CHARGE_COOLDOWN;
            case THROW -> THROW_COOLDOWN;
            case NONE, CHASE -> 0;
        };
    }
}
