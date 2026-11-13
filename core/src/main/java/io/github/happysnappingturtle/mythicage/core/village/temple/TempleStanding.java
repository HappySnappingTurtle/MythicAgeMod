package io.github.happysnappingturtle.mythicage.core.village.temple;

/**
 * 村庄神庙的系数与晋升门槛（GA-13 §2.6，五文明通用）：系数按完好度打折、不低于三成；
 * 晋升「神庙 ≥ N 级」还要求完好度至少八成。
 */
public final class TempleStanding {
    /** 完好度修正的下限：神庙再破也按三成算。 */
    public static final double INTEGRITY_FLOOR = 0.3;
    /** 晋升认可的最低完好度。 */
    public static final double PROMOTION_INTEGRITY = 0.8;

    private TempleStanding() {
    }

    /** 完好度夹到 [0, 1]；非有限值按完好算（还没检查过）。 */
    public static double clampIntegrity(double integrity) {
        return Double.isFinite(integrity) ? Math.max(0, Math.min(1, integrity)) : 1;
    }

    /** {@code T(level) × max(0.3, integrity)}。 */
    public static double coefficient(double tierCoefficient, double integrity) {
        return tierCoefficient * Math.max(INTEGRITY_FLOOR, clampIntegrity(integrity));
    }

    /** 晋升条件「神庙 ≥ need 级」：等级够且完好度 ≥ 0.8。 */
    public static boolean meets(int level, double integrity, int need) {
        return level >= need && clampIntegrity(integrity) >= PROMOTION_INTEGRITY;
    }
}
