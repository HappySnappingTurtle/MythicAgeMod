package io.github.happysnappingturtle.mythicage.core.power;

/**
 * 复原的计算（T13 设计文档 §2.2；05 号 GP-GR-04）：5 秒分 5 次，每次补最大生命的 20%；
 * 友方玩家的装备只修一次，每件修「最大耐久 × 10%」（向上取整），封顶满耐久。
 */
public final class RestorationRules {
    public static final int PULSES = 5;
    public static final int PULSE_INTERVAL = 20;
    public static final double PULSE_FRACTION = 0.2;
    public static final double REPAIR_FRACTION = 0.1;

    private RestorationRules() {
    }

    /** 每次补多少生命。 */
    public static float pulse(float maxHealth) {
        return (float) (maxHealth * PULSE_FRACTION);
    }

    /** 修复后的损耗值（原版的 damage：0 为满耐久）。 */
    public static int repairedDamage(int maxDamage, int damage) {
        if (maxDamage <= 0) {
            return damage;
        }
        return Math.max(0, damage - (int) Math.ceil(maxDamage * REPAIR_FRACTION));
    }
}
