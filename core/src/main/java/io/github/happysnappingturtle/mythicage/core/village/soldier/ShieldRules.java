package io.github.happysnappingturtle.mythicage.core.village.soldier;

import io.github.happysnappingturtle.mythicage.core.wild.WildCombatRules;

/**
 * 盾（T20 设计文档 §8.3，02 号卡 §3）：只有处于明确的举盾状态才减伤；一次最多举 {@code maxHold} 刻，放下后至少隔
 * {@code gap} 刻才能再举；举起与放下的过渡期不减伤。纯函数。
 */
public final class ShieldRules {
    private ShieldRules() {
    }

    /** 放下于 {@code loweredAt} 之后，此刻能否再举（放下后的 {@code gap} 刻都是间隙）。 */
    public static boolean canRaise(SoldierProfile.Shield s, long now, long loweredAt) {
        return now - loweredAt > s.gap();
    }

    /** 举了这么久必须放下。 */
    public static boolean mustLower(SoldierProfile.Shield s, long now, long raisedAt) {
        return now - raisedAt >= s.maxHold();
    }

    /** 举起的过渡做完了没有（过渡期不减伤）。 */
    public static boolean effective(SoldierProfile.Shield s, long now, long raisedAt) {
        return now - raisedAt >= s.raise();
    }

    /**
     * 伤害倍率：举盾生效、来源在正前方扇形内时 ×(1 − 减伤)，否则 ×1。
     *
     * @param dx 来源相对士兵的 x
     * @param dz 来源相对士兵的 z
     */
    public static double multiplier(SoldierProfile.Shield s, boolean blocking, double facingYawDeg, double dx, double dz) {
        if (!blocking) {
            return 1.0;
        }
        return WildCombatRules.frontalMultiplier(facingYawDeg, dx, dz, s.arc(), s.reduction());
    }
}
