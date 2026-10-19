package io.github.happysnappingturtle.mythicage.core.power;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TC-T13-05～08：诱兽石候选、哨兵寿命与站位。 */
class LureSentinelRulesTest {
    @Test
    void tc05EquinesFirstThenDistance() {
        List<LureRules.Candidate<String>> c = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            c.add(new LureRules.Candidate<>("cow" + i, false, i));
        }
        c.add(new LureRules.Candidate<>("horse1", true, 40));
        c.add(new LureRules.Candidate<>("horse2", true, 45));
        c.add(new LureRules.Candidate<>("horse3", true, 47));
        c.add(new LureRules.Candidate<>("donkey1", true, 30));
        c.add(new LureRules.Candidate<>("donkey2", true, 46));
        List<String> got = LureRules.pick(c, 0, LureRules.CAP);
        assertEquals(24, got.size());
        assertTrue(got.containsAll(List.of("horse1", "horse2", "horse3", "donkey1", "donkey2")), "马驴全在");
        assertEquals("donkey1", got.getFirst(), "马驴之间也按距离");
        for (int i = 0; i < 19; i++) {
            assertTrue(got.contains("cow" + i), "最近的 19 头牛");
        }
        assertTrue(!got.contains("cow19"), "第 20 头牛起排不上");
    }

    @Test
    void tc06FullMeansNoNewPicks() {
        List<LureRules.Candidate<String>> c = List.of(new LureRules.Candidate<>("a", true, 1));
        assertEquals(List.of(), LureRules.pick(c, 24, LureRules.CAP));
        assertEquals(List.of("a"), LureRules.pick(c, 23, LureRules.CAP));
    }

    @Test
    void chickensFollowTheClock() {
        assertEquals(0, LureRules.chickensDue(599));
        assertEquals(1, LureRules.chickensDue(600));
        assertEquals(6, LureRules.chickensDue(3600));
        assertEquals(6, LureRules.chickensDue(99999), "封顶 6");
    }

    @Test
    void tc07Lifetime() {
        assertEquals(72000, SentinelRules.lifetime(31));
        assertEquals(72000, SentinelRules.lifetime(32));
        assertEquals(24000, SentinelRules.lifetime(33));
        assertEquals(24000, SentinelRules.lifetime(Double.POSITIVE_INFINITY), "别人的神坛不算：调用方传无穷远");
    }

    @Test
    void tc08CornersAndHeights() {
        assertEquals(4, SentinelRules.CORNERS.size());
        for (int[] c : SentinelRules.CORNERS) {
            assertEquals(2, Math.abs(c[0]));
            assertEquals(2, Math.abs(c[1]));
        }
        assertEquals(List.of(0, 1, -1, 2, -2), SentinelRules.HEIGHTS);
    }
}
