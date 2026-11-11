package io.github.happysnappingturtle.mythicage.core.equipment;

/** 原版 Player.attack 的暴击前提；冷却取出手前 getAttackStrengthScale(0.5)。 */
public final class CriticalHitRules {
    private CriticalHitRules() {
    }

    public static boolean isCritical(float fallDistance, boolean onGround, boolean climbing, boolean inWater,
                                     boolean blind, boolean passenger, boolean sprinting, float strength,
                                     boolean livingTarget) {
        return strength > 0.9F && fallDistance > 0 && !onGround && !climbing && !inWater && !blind
                && !passenger && !sprinting && livingTarget;
    }
}
