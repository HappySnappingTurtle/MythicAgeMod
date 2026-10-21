package io.github.happysnappingturtle.mythicage.core.summon;

/** 侦察单位的重生时刻（总纲 B1、T14 设计文档 §5.4）：死亡后按 MC 日延迟，到时主人有神坛才重生，否则继续等。 */
public final class RespawnRules {
    public static final long DAY_TICKS = 24000L;

    private RespawnRules() {
    }

    public static long respawnAt(long deathTick, int delayDays) {
        return deathTick + delayDays * DAY_TICKS;
    }

    /** 现在能否重生：到时间且主人有神坛。 */
    public static boolean ready(long now, long respawnAt, boolean ownerHasShrine) {
        return now >= respawnAt && ownerHasShrine;
    }
}
