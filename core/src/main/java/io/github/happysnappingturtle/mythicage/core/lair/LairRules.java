package io.github.happysnappingturtle.mythicage.core.lair;

/**
 * 精英巢穴再入住（五文明通用，06 号 E1，T25a §6）：巢穴登记的精英全死才算清空；清空后空置 {@value #VACANT_DAYS} 个 MC 日；
 * 到期后区块载入、{@value #PLAYER_CLEAR} 格内没有玩家时重新入住（满血精英 + 补足随从）。圣物与图纸按玩家首开，不随入住重置。
 * 精英不出巢：离巢超过 {@value #LEASH} 格就回巢，回巢时回满血。
 */
public final class LairRules {
    public static final int VACANT_DAYS = 7;
    public static final int PLAYER_CLEAR = 32;
    public static final int LEASH = 48;
    /** 没清空。 */
    public static final long NOT_CLEARED = -1;

    private LairRules() {
    }

    /** 一只精英死后，巢穴的清空日：还有活的 → 不变（没清空就是 {@link #NOT_CLEARED}）；全死 → 今天（已经清空过的保持原日子）。 */
    public static long afterDeath(int aliveElites, long clearedDay, long today) {
        if (aliveElites > 0) {
            return NOT_CLEARED;
        }
        return clearedDay == NOT_CLEARED ? today : clearedDay;
    }

    /** 读档、改时间后日子倒退：清空日不能晚于今天（否则永远等不到，或算出负的空置天数）。 */
    public static long normalize(long clearedDay, long today) {
        return clearedDay == NOT_CLEARED ? NOT_CLEARED : Math.min(clearedDay, today);
    }

    /** 已经空置了几天（没清空为 0，不会是负数）。 */
    public static long vacantDays(long clearedDay, long today) {
        return clearedDay == NOT_CLEARED ? 0 : Math.max(0, today - clearedDay);
    }

    /** 该不该现在重新入住。 */
    public static boolean shouldRepopulate(long clearedDay, long today, boolean chunkLoaded, boolean playerNearby) {
        return clearedDay != NOT_CLEARED && vacantDays(normalize(clearedDay, today), today) >= VACANT_DAYS && chunkLoaded
                && !playerNearby;
    }

    /** 入住时补几只随从：登记 registered 只、现在 alive 只；多出来的（玩家赶来的）不删。 */
    public static int followersToAdd(int registered, int alive) {
        return Math.max(0, registered - alive);
    }

    /** 精英离巢太远，该回去了。 */
    public static boolean mustReturn(double distSqFromLair) {
        return distSqFromLair > (double) LEASH * LEASH;
    }
}
