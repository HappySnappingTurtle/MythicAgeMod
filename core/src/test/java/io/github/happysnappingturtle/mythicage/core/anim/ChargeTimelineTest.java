package io.github.happysnappingturtle.mythicage.core.anim;

import io.github.happysnappingturtle.mythicage.core.anim.ChargeTimeline.Phase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChargeTimelineTest {
    @Test
    void tc06WindupTwentyTicksLocksAtTen() {
        ChargeTimeline t = new ChargeTimeline();
        assertTrue(t.canTurn());
        t.start(10);
        for (int i = 0; i < 20; i++) {
            assertEquals(Phase.WINDUP, t.phase(), "第 " + i + " 刻应为蓄力");
            assertEquals(i < 10, t.canTurn(), "第 " + i + " 刻的转向");
            assertFalse(t.moving());
            t.advance();
        }
        assertEquals(Phase.RUN, t.phase());
        assertTrue(t.moving());
        assertFalse(t.canTurn());
    }

    @Test
    void tc07RunEndsAtPlannedTicksThenRecover16() {
        ChargeTimeline t = new ChargeTimeline();
        t.start(7);
        advance(t, 20);
        int run = 0;
        while (t.phase() == Phase.RUN) {
            run++;
            t.advance();
        }
        assertEquals(7, run);
        assertEquals(Phase.RECOVER, t.phase());
        int recover = 0;
        while (t.phase() == Phase.RECOVER) {
            assertFalse(t.canTurn());
            recover++;
            t.advance();
        }
        assertEquals(16, recover);
        assertEquals(Phase.NONE, t.phase());
        assertTrue(t.canTurn());
    }

    @Test
    void tc07CollisionStopsAtAnyRunTick() {
        for (int at = 0; at < 20; at++) {
            ChargeTimeline t = new ChargeTimeline();
            t.start(20);
            advance(t, 20 + at);
            assertEquals(Phase.RUN, t.phase());
            t.stopRun();
            assertEquals(Phase.RECOVER, t.phase(), "冲刺第 " + at + " 刻碰撞应进入收势");
            assertEquals(0, t.tickInPhase());
            advance(t, 16);
            assertEquals(Phase.NONE, t.phase());
        }
    }

    @Test
    void stopRunOutsideRunIsIgnored() {
        ChargeTimeline t = new ChargeTimeline();
        t.stopRun();
        assertEquals(Phase.NONE, t.phase());
        t.start(5);
        t.stopRun();
        assertEquals(Phase.WINDUP, t.phase());
    }

    @Test
    void tc08ZeroDistanceSkipsRun() {
        ChargeTimeline t = new ChargeTimeline();
        t.start(ChargeTimeline.runTicksFor(0, 0.8));
        advance(t, 19);
        assertEquals(Phase.WINDUP, t.phase());
        t.advance();
        assertEquals(Phase.RECOVER, t.phase());
        advance(t, 16);
        assertEquals(Phase.NONE, t.phase());
    }

    @Test
    void runTicksForClampsAndRounds() {
        assertEquals(0, ChargeTimeline.runTicksFor(0, 0.8));
        assertEquals(0, ChargeTimeline.runTicksFor(-3, 0.8));
        assertEquals(10, ChargeTimeline.runTicksFor(8, 0.8));
        assertEquals(11, ChargeTimeline.runTicksFor(8.1, 0.8));
        assertEquals(20, ChargeTimeline.runTicksFor(100, 0.8));
        assertThrows(IllegalArgumentException.class, () -> ChargeTimeline.runTicksFor(5, 0));
        assertThrows(IllegalArgumentException.class, () -> new ChargeTimeline().start(-1));
    }

    @Test
    void cancelReturnsToNone() {
        ChargeTimeline t = new ChargeTimeline();
        t.start(5);
        advance(t, 22);
        t.cancel();
        assertEquals(Phase.NONE, t.phase());
        assertFalse(t.active());
    }

    private static void advance(ChargeTimeline t, int n) {
        for (int i = 0; i < n; i++) {
            t.advance();
        }
    }
}
