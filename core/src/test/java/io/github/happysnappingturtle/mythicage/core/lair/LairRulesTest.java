package io.github.happysnappingturtle.mythicage.core.lair;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T25a 精英巢穴再入住（TC-T25a-20～27）。 */
class LairRulesTest {
    @Test
    void tc20PartialKillNotCleared() {
        assertEquals(LairRules.NOT_CLEARED, LairRules.afterDeath(1, LairRules.NOT_CLEARED, 10));
        assertFalse(LairRules.shouldRepopulate(LairRules.NOT_CLEARED, 100, true, false), "没清空就不入住、不补员");
    }

    @Test
    void tc21AllDeadRecordsDay() {
        assertEquals(10, LairRules.afterDeath(0, LairRules.NOT_CLEARED, 10));
        assertEquals(10, LairRules.afterDeath(0, 10, 12), "已经清空过的保持原日子");
    }

    @Test
    void tc22SevenDays() {
        assertFalse(LairRules.shouldRepopulate(10, 16, true, false), "第 6 天");
        assertTrue(LairRules.shouldRepopulate(10, 17, true, false), "第 7 天");
    }

    @Test
    void tc23PlayerNearbyDelays() {
        assertFalse(LairRules.shouldRepopulate(10, 17, true, true));
        assertTrue(LairRules.shouldRepopulate(10, 20, true, false), "玩家走后再入住");
    }

    @Test
    void tc24UnloadedWaits() {
        assertFalse(LairRules.shouldRepopulate(10, 30, false, false));
    }

    @Test
    void tc25Followers() {
        assertEquals(4, LairRules.followersToAdd(6, 2));
        assertEquals(0, LairRules.followersToAdd(6, 7), "多的不删");
        assertEquals(6, LairRules.followersToAdd(6, 0));
    }

    @Test
    void tc26Leash() {
        assertFalse(LairRules.mustReturn(48 * 48));
        assertTrue(LairRules.mustReturn(48.1 * 48.1));
    }

    @Test
    void tc27DayGoesBackwards() {
        assertEquals(5, LairRules.normalize(10, 5), "清空日不晚于今天");
        assertEquals(0, LairRules.vacantDays(10, 5), "不出现负数");
        assertFalse(LairRules.shouldRepopulate(10, 5, true, false), "不提前入住");
        // 倒退到第 5 天后，要从第 5 天重新数满 7 天：平台层把清空日写回 normalize 的结果
        long cleared = LairRules.normalize(10, 5);
        assertFalse(LairRules.shouldRepopulate(cleared, 11, true, false));
        assertTrue(LairRules.shouldRepopulate(cleared, 12, true, false));
        assertEquals(LairRules.NOT_CLEARED, LairRules.normalize(LairRules.NOT_CLEARED, 5));
    }
}
