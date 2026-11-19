package io.github.happysnappingturtle.mythicage.core.lair;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T25a 阶段 ⑤：嵌坡巢穴选址与夜间封洞（TC-T25a-28、29、40～43）。 */
class LairSiteRulesTest {
    private static final int SEA = 62;

    @Test
    void tc40HillsideAccepted() {
        var f = LairSiteRules.floor(new int[]{70, 71, 70}, new int[]{76, 78, 77}, new int[]{82, 85, 83}, 6, false, SEA);
        assertEquals(70, f.orElseThrow(), "地面取洞口中位数");
        assertEquals(12, LairSiteRules.score(new int[]{82, 85, 83}, 70));
    }

    @Test
    void tc41FlatOrShallowRejected() {
        assertTrue(LairSiteRules.floor(new int[]{70, 70, 70}, new int[]{70, 71, 70}, new int[]{71, 70, 71}, 6, false, SEA).isEmpty(),
                "平地不行");
        assertTrue(LairSiteRules.floor(new int[]{70, 70, 70}, new int[]{76, 76, 76}, new int[]{75, 77, 77}, 6, false, SEA).isEmpty(),
                "洞底的山比洞顶低不行（要 ≥ 地面 + 洞高）");
        assertTrue(LairSiteRules.floor(new int[]{70, 70, 70}, new int[]{71, 74, 73}, new int[]{82, 82, 82}, 6, false, SEA).isEmpty(),
                "中段露天不行（最低 71 < 70 + 6 − 3）");
        assertTrue(LairSiteRules.floor(new int[]{70, 70, 70}, new int[]{73, 75, 74}, new int[]{82, 82, 82}, 6, false, SEA).isPresent(),
                "中段低 3 格还行（露出的由岩壳补）");
    }

    @Test
    void tc42FrontReliefWaterSea() {
        int[] mid = {78, 78, 78};
        int[] back = {85, 85, 85};
        assertTrue(LairSiteRules.floor(new int[]{70, 74, 70}, mid, back, 6, false, SEA).isEmpty(), "洞口起伏 4 不行");
        assertTrue(LairSiteRules.floor(new int[]{70, 70, 70}, mid, back, 6, true, SEA).isEmpty(), "洞口在水里不行");
        assertTrue(LairSiteRules.floor(new int[]{62, 62, 62}, new int[]{70, 70, 70}, new int[]{75, 75, 75}, 6, false, SEA).isEmpty(),
                "地面不高于海平面不行");
    }

    @Test
    void tc43bStepwiseMatchesWhole() {
        assertTrue(LairSiteRules.frontFloor(new int[]{70, 71, 70}, false, SEA).isPresent());
        assertFalse(LairSiteRules.backOk(new int[]{75, 90, 90}, 70, 6), "洞底最低处不够");
        assertTrue(LairSiteRules.backOk(new int[]{76, 90, 90}, 70, 6));
        assertFalse(LairSiteRules.midOk(new int[]{72}, 70, 6));
        assertTrue(LairSiteRules.midOk(new int[]{73}, 70, 6));
    }

    @Test
    void tc43EmptySamples() {
        assertTrue(LairSiteRules.floor(new int[0], new int[]{1}, new int[]{1}, 6, false, SEA).isEmpty());
    }

    @Test
    void tc28SealWindow() {
        assertFalse(LairRules.sealTime(12499));
        assertTrue(LairRules.sealTime(12500));
        assertTrue(LairRules.sealTime(22999));
        assertFalse(LairRules.sealTime(23000));
        assertTrue(LairRules.sealTime(24000L * 5 + 18000), "按一天内的刻数算");
        assertEquals(10500, LairRules.ticksUntilDawn(12500));
        assertEquals(0, LairRules.ticksUntilDawn(6000));
        assertTrue(LairRules.shouldSeal(13000, true, true, false));
        assertFalse(LairRules.shouldSeal(13000, true, false, false), "巨人不在洞里不封");
        assertFalse(LairRules.shouldSeal(13000, true, true, true), "今晚推开过不再封");
        assertFalse(LairRules.shouldSeal(13000, false, true, false), "区块没载入不封");
        assertEquals(LairRules.nightIndex(13000), LairRules.nightIndex(22999), "傍晚到天亮（同一个 24000 周期内）是同一晚");
        assertEquals(LairRules.nightIndex(13000) + 1, LairRules.nightIndex(24000 + 13000));
    }

    @Test
    void tc29PushHold() {
        int p = 0;
        long t = 100;
        p = LairRules.pushProgress(p, t - 100, t);
        assertEquals(0, p, "第一下从头计");
        for (int i = 0; i < 15; i++) {
            long next = t + 4;
            p = LairRules.pushProgress(p, t, next);
            t = next;
        }
        assertEquals(60, p, "每 4 刻一下，15 下 = 60 刻");
        assertTrue(p >= LairRules.PUSH_TICKS);
        assertEquals(0, LairRules.pushProgress(40, t, t + 11), "松手超过 10 刻从头计");
        assertEquals(0, LairRules.pushProgress(40, t, t - 1), "时间倒退从头计");
    }
}
