package io.github.happysnappingturtle.mythicage.core.village.soldier;

/**
 * 士兵的敌我表（T20 设计文档 §5.4，01 号 §0.3）与拴绳：纯函数。
 * <p>
 * {@link #decide} 只回答「对这个对象该怎么做」；能不能开始追、追到哪里为止由 {@link #canEngage}、{@link #keepChasing} 决定。
 */
public final class SoldierTargetRules {
    /** 只对离岗位这么近的目标开始接敌（01 号 §0.3）。 */
    public static final double ENGAGE_RADIUS = 16;
    /** 追击时离岗位超过这么远就放弃（02 号卡 §2）。 */
    public static final double CHASE_RADIUS = 24;
    /** 中立生物打过居民之后这么久内算「正在攻击」（刻）。 */
    public static final long NEUTRAL_MEMORY = 100;
    /** PvP 开时，玩家打过居民或士兵之后这么久内会被反击（30 秒）。 */
    public static final long PLAYER_MEMORY = 600;

    private SoldierTargetRules() {
    }

    public enum Decision {
        /** 接敌。 */
        ENGAGE,
        /** 护送居民撤离：拦在中间，只在被挡路（它正在打这名士兵且在触及内）时还手。 */
        ESCORT,
        IGNORE
    }

    /** 对象的类别（由游戏侧判断）。 */
    public enum Kind {
        HOSTILE, ELITE, BOSS, NEUTRAL, ANIMAL,
        /** 玩家的召唤单位、宠物、被支配的生物。 */
        OWNED,
        PLAYER,
        /** 本村或他村的居民、士兵、祭司：永远不打。 */
        VILLAGE_FOLK,
        OTHER
    }

    /**
     * @param kind              类别
     * @param ticksSinceHarmed  它上一次打本村居民或士兵是多少刻前（没打过为 -1）
     * @param attackingSoldier  它此刻正以这名士兵为目标
     * @param inReach           在士兵的触及内
     * @param creative          创造 / 旁观模式的玩家
     * @param teammate          与本村协作者同队的玩家
     * @param pvp               服务器开着 PvP
     * @param ownerRetaliated   （召唤单位、宠物）它的主人在本村的 PvP 反击名单里
     * @param harmingResident   （召唤单位、宠物）它此刻正在打本村居民
     */
    public record Facts(Kind kind, long ticksSinceHarmed, boolean attackingSoldier, boolean inReach, boolean creative,
                        boolean teammate, boolean pvp, boolean ownerRetaliated, boolean harmingResident) {
        public static Facts of(Kind kind) {
            return new Facts(kind, -1, false, false, false, false, false, false, false);
        }

        public Facts harmed(long ago) {
            return new Facts(kind, ago, attackingSoldier, inReach, creative, teammate, pvp, ownerRetaliated, harmingResident);
        }

        public Facts attacking(boolean soldier, boolean reach) {
            return new Facts(kind, ticksSinceHarmed, soldier, reach, creative, teammate, pvp, ownerRetaliated, harmingResident);
        }

        public Facts player(boolean creative, boolean teammate, boolean pvp) {
            return new Facts(kind, ticksSinceHarmed, attackingSoldier, inReach, creative, teammate, pvp, ownerRetaliated,
                    harmingResident);
        }

        public Facts owned(boolean ownerRetaliated, boolean harmingResident) {
            return new Facts(kind, ticksSinceHarmed, attackingSoldier, inReach, creative, teammate, pvp, ownerRetaliated,
                    harmingResident);
        }

        boolean harmedWithin(long ticks) {
            return ticksSinceHarmed >= 0 && ticksSinceHarmed <= ticks;
        }
    }

    public static Decision decide(Facts f) {
        return switch (f.kind()) {
            case HOSTILE -> Decision.ENGAGE;
            case ELITE, BOSS -> f.attackingSoldier() && f.inReach() ? Decision.ENGAGE : Decision.ESCORT;
            case NEUTRAL -> f.harmedWithin(NEUTRAL_MEMORY) ? Decision.ENGAGE : Decision.IGNORE;
            case OWNED -> f.ownerRetaliated() ? Decision.ENGAGE : f.harmingResident() ? Decision.ESCORT : Decision.IGNORE;
            case PLAYER -> player(f);
            // 动物永远不打（01 号 §0.3）；它正在咬本村的人时只护送（拦在中间）
            case ANIMAL -> f.harmedWithin(NEUTRAL_MEMORY) ? Decision.ESCORT : Decision.IGNORE;
            case VILLAGE_FOLK, OTHER -> Decision.IGNORE;
        };
    }

    private static Decision player(Facts f) {
        if (f.creative() || f.teammate()) {
            return Decision.IGNORE;
        }
        boolean harmed = f.harmedWithin(PLAYER_MEMORY);
        if (!f.pvp()) {
            return harmed ? Decision.ESCORT : Decision.IGNORE;
        }
        return harmed ? Decision.ENGAGE : Decision.IGNORE;
    }

    /** 能不能开始接敌：目标在村庄已建范围内、离岗位 ≤ 16 格。 */
    public static boolean canEngage(double distanceFromPost, boolean inVillage) {
        return inVillage && distanceFromPost <= ENGAGE_RADIUS;
    }

    /** 追击中还追不追：目标仍在村里、离岗位 ≤ 24 格。 */
    public static boolean keepChasing(double distanceFromPost, boolean inVillage) {
        return inVillage && distanceFromPost <= CHASE_RADIUS;
    }
}
