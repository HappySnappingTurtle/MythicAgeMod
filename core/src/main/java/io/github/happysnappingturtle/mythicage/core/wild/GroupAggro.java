package io.github.happysnappingturtle.mythicage.core.wild;

/**
 * 群居中立生物的仇恨归属（T15 设计文档 §2）：谁打了群里的一只，全群找谁算账。
 */
public final class GroupAggro {
    /** 伤害来源的种类（由调用方按实体判断）。 */
    public enum Source { PLAYER, PLAYER_SUMMON, PLAYER_PROJECTILE, OTHER_MOB, NONE }

    private GroupAggro() {
    }

    /** 这次伤害要不要让全群仇恨（玩家、玩家的召唤单位、玩家的投射物都归到玩家）。 */
    public static boolean alertsGroup(Source s) {
        return s == Source.PLAYER || s == Source.PLAYER_SUMMON || s == Source.PLAYER_PROJECTILE;
    }

    /** 被打的那一只要不要反击（别的生物打它：只有它自己反击）。 */
    public static boolean retaliates(Source s) {
        return s != Source.NONE;
    }
}
