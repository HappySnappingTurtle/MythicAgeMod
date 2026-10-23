package io.github.happysnappingturtle.mythicage.core.relic;

import java.util.Collection;

/** 圣物减伤的判定与合成（06 号 RE-GR-02 边界用例 1、3）。 */
public final class DamageRules {
    private DamageRules() {
    }

    /**
     * 「近战」只认直接攻击与横扫：直接实体就是攻击者本身且是活的生物；弹射物、爆炸、荆棘反伤都不算。
     *
     * @param directIsAttacker 伤害源的直接实体与归属实体是同一个（没有直接实体时为 false）
     * @param directIsLiving   直接实体是活的生物
     */
    public static boolean isMelee(boolean directIsAttacker, boolean directIsLiving, boolean projectile, boolean explosion,
                                  boolean thorns) {
        return directIsAttacker && directIsLiving && !projectile && !explosion && !thorns;
    }

    /** 先把各个百分比减伤相乘，再减固定值，最后不低于 0。 */
    public static float reduce(float amount, Collection<Double> percents, float flat) {
        double after = amount * EffectGroups.remaining(percents) - Math.max(0, flat);
        return (float) Math.max(0, after);
    }
}
