package io.github.happysnappingturtle.mythicage.core.playtest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 站点用时与里程碑去重（TC-GA3-20～25）。 */
class MilestoneClockTest {
    @Test
    void tc20StartDone() {
        MilestoneClock c = new MilestoneClock();
        assertTrue(c.start("temple", 1_000, 100).isEmpty());
        MilestoneClock.Ended e = c.done(613_500, 12_340).orElseThrow();
        assertEquals("temple", e.station());
        assertEquals(612, e.seconds());
        assertEquals(12_240, e.ticks());
        assertFalse(e.switched());
        assertTrue(c.station().isEmpty());
    }

    @Test
    void tc21Switch() {
        MilestoneClock c = new MilestoneClock();
        c.start("temple", 0, 0);
        MilestoneClock.Ended e = c.start("powers", 60_000, 1200).orElseThrow();
        assertEquals("temple", e.station());
        assertTrue(e.switched());
        assertEquals("powers", c.station().orElseThrow());
        // 准备下一站期间：当前站已结束，事件不记
        assertTrue(c.interrupt(70_000, 1300).orElseThrow().switched());
        assertFalse(c.milestone("blessed"));
        assertTrue(c.interrupt(71_000, 1301).isEmpty());
    }

    @Test
    void tc22FirstOnly() {
        MilestoneClock c = new MilestoneClock();
        c.start("temple", 0, 0);
        assertTrue(c.milestone("favor_gained"));
        assertFalse(c.milestone("favor_gained"));
        assertTrue(c.milestone("temple_level:1"));
        // 换站后重新计
        c.start("powers", 1, 1);
        assertTrue(c.milestone("favor_gained"));
    }

    @Test
    void tc23ClockBack() {
        MilestoneClock c = new MilestoneClock();
        c.start("temple", 10_000, 500);
        MilestoneClock.Ended e = c.done(5_000, 100).orElseThrow();
        assertEquals(0, e.seconds());
        assertEquals(0, e.ticks());
    }

    @Test
    void tc24DoneWithoutStart() {
        MilestoneClock c = new MilestoneClock();
        assertTrue(c.done(1, 1).isEmpty());
        assertFalse(c.milestone("blessed"), "不在站里不记");
    }

    @Test
    void tc25Format() {
        assertEquals("59 秒", MilestoneClock.format(59));
        assertEquals("10 分 12 秒", MilestoneClock.format(612));
        assertEquals("1 时 1 分 40 秒", MilestoneClock.format(3700));
        assertEquals("0 秒", MilestoneClock.format(-5));
    }
}
