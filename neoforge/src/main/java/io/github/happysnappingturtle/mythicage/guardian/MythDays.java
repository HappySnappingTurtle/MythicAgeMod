package io.github.happysnappingturtle.mythicage.guardian;

import io.github.happysnappingturtle.mythicage.core.guardian.GuardianRules;
import io.github.happysnappingturtle.mythicage.runtime.WorldClock;
import net.minecraft.server.MinecraftServer;

/**
 * MC 日序号 = 世界时钟（主世界 gameTime，单调）/ 24000。{@code /time set} 与睡觉不影响它。
 * 测试可以加一个偏移量来「推进到次日」，而不改动全服时钟。
 */
public final class MythDays {
    private static volatile long testOffset;

    private MythDays() {
    }

    public static long today(MinecraftServer server) {
        return GuardianRules.day(WorldClock.now(server)) + testOffset;
    }

    /** 只供 GameTest 使用。 */
    public static void advanceForTest(long days) {
        testOffset += days;
    }
}
