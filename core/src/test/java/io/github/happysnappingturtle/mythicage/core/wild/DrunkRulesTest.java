package io.github.happysnappingturtle.mythicage.core.wild;

import io.github.happysnappingturtle.mythicage.core.wild.DrunkRules.Phase;
import io.github.happysnappingturtle.mythicage.core.wild.DrunkRules.State;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T25a 独眼巨人灌醉（TC-T25a-34～36）。 */
class DrunkRulesTest {
    @Test
    void tc34FullCycle() {
        State s = State.FRESH;
        assertEquals(Phase.SOBER, DrunkRules.notice(s, 100, 17 * 17).phase(), "16 格外闻不到");
        s = DrunkRules.notice(s, 100, 10 * 10);
        assertEquals(Phase.APPROACH, s.phase());
        s = DrunkRules.arrive(s, 140);
        assertEquals(Phase.DRINK, s.phase());
        State before = s;
        s = DrunkRules.tick(s, 179);
        assertEquals(Phase.DRINK, s.phase(), "喝满 2 秒前");
        s = DrunkRules.tick(s, 180);
        assertEquals(Phase.DRUNK, s.phase());
        assertTrue(DrunkRules.finishedDrinking(before, s));
        assertEquals(3.0, DrunkRules.damageWindow(s), 1e-9, "醉倒伤害 ×3");
        s = DrunkRules.tick(s, 339);
        assertEquals(Phase.DRUNK, s.phase());
        s = DrunkRules.tick(s, 340);
        assertEquals(Phase.SOBER, s.phase(), "醉 8 秒后醒");
        assertEquals(340, s.wokeAt());
        assertEquals(1.0, DrunkRules.damageWindow(s), 1e-9);
    }

    @Test
    void tc35NoInterestRightAfterWaking() {
        State woke = new State(Phase.SOBER, 340, 340);
        assertEquals(Phase.SOBER, DrunkRules.notice(woke, 939, 4).phase(), "醒后 30 秒内不理");
        assertEquals(Phase.APPROACH, DrunkRules.notice(woke, 940, 4).phase());
    }

    @Test
    void tc36HurtWhileDrinking() {
        State drinking = new State(Phase.DRINK, 140, Long.MIN_VALUE / 4);
        State s = DrunkRules.hurt(drinking, 150);
        assertEquals(Phase.SOBER, s.phase(), "中断，不醉倒");
        assertEquals(Phase.APPROACH, DrunkRules.notice(s, 151, 4).phase(), "没算醒过，酒还在就能再去喝");
        State drunk = new State(Phase.DRUNK, 180, 0);
        assertEquals(Phase.DRUNK, DrunkRules.hurt(drunk, 200).phase(), "醉倒中挨打照睡");
        assertFalse(DrunkRules.finishedDrinking(drinking, s));
        assertEquals(Phase.SOBER, DrunkRules.bowlGone(new State(Phase.APPROACH, 100, 0), 120).phase(), "酒碗没了就放弃");
    }
}
