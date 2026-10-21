package io.github.happysnappingturtle.mythicage.core.summon;

import io.github.happysnappingturtle.mythicage.core.power.TargetCategory;
import io.github.happysnappingturtle.mythicage.core.power.TargetClassifier;
import io.github.happysnappingturtle.mythicage.core.power.TargetFacts;

/**
 * 召唤单位能打谁（T14 设计文档 §4.1，按 T12 的目标矩阵；事实以「主人」为施法者收集）：
 * 敌对生物、精英、Boss；PvP 开且不同队的玩家；敌对玩家的召唤单位。永不打主人、主人的村民与士兵、队友、
 * 自己人的召唤单位、祭司、中立动物、别村的居民。非战斗单位不打任何目标。
 */
public final class SummonTargetRules {
    private SummonTargetRules() {
    }

    public static boolean canAttack(TargetFacts f, boolean combat) {
        if (!combat) {
            return false;
        }
        TargetCategory c = TargetClassifier.classify(f);
        return switch (c) {
            case HOSTILE, ELITE, BOSS -> true;
            case ENEMY_PLAYER -> f.pvpEnabled() && !f.teammate();
            case OTHER_SUMMON -> f.summonOwnerHostile();
            default -> false;
        };
    }
}
