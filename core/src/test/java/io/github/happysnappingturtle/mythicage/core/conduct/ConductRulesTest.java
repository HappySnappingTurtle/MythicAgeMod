package io.github.happysnappingturtle.mythicage.core.conduct;

import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.github.happysnappingturtle.mythicage.core.conduct.ConductRules.Band.EVIL;
import static io.github.happysnappingturtle.mythicage.core.conduct.ConductRules.Band.GOOD;
import static io.github.happysnappingturtle.mythicage.core.conduct.ConductRules.Band.NEUTRAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T18 单元测试 TC-T18-01～12：善恶值规则与事件账本。 */
class ConductRulesTest {
    private static final long W = 200;

    @Test
    void tc01_clamp() {
        assertEquals(100, ConductRules.clamp(95 + 10, -100, 100));
        assertEquals(-100, ConductRules.clamp(-95 - 10, -100, 100));
    }

    @Test
    void tc02_regress() {
        assertEquals(8, ConductRules.regress(10, 2));
        assertEquals(0, ConductRules.regress(1, 2));
        assertEquals(0, ConductRules.regress(-1, 2));
        assertEquals(0, ConductRules.regress(0, 2));
        assertEquals(-8, ConductRules.regress(-10, 2));
    }

    @Test
    void tc03_positiveCap() {
        assertEquals(2, ConductRules.positiveAllowance(6, 10, 12));
        assertEquals(0, ConductRules.positiveAllowance(3, 12, 12));
        assertEquals(-5, ConductRules.positiveAllowance(-5, 12, 12), "负向不受上限");
    }

    @Test
    void tc04_harmThenKillEscalates() {
        IncidentLedger l = new IncidentLedger();
        String k = IncidentLedger.key("p", "v", "villager");
        assertEquals(-2, l.charge(k, false, 100, W, -2, -12));
        assertEquals(-10, l.charge(k, true, 150, W, -2, -12));
        assertEquals(0, l.charge(k, true, 160, W, -2, -12), "再杀不重复扣");
    }

    @Test
    void tc05_repeatedHarmOnce() {
        IncidentLedger l = new IncidentLedger();
        String k = IncidentLedger.key("p", "v", "villager");
        int total = 0;
        for (int i = 0; i < 5; i++) {
            total += l.charge(k, false, 100 + i * 10, W, -2, -12);
        }
        assertEquals(-2, total);
    }

    @Test
    void tc06_directKill() {
        assertEquals(-12, new IncidentLedger().charge("k", true, 0, W, -2, -12));
    }

    @Test
    void tc07_windowExpires() {
        IncidentLedger l = new IncidentLedger();
        l.charge("k", false, 100, W, -2, -12);
        assertEquals(-2, l.charge("k", false, 301, W, -2, -12), "201 刻后是新事件");
        assertEquals(0, l.charge("k", false, 300, W, -2, -12), "新事件窗口内");
    }

    @Test
    void tc08_independentKeys() {
        IncidentLedger l = new IncidentLedger();
        assertEquals(-2, l.charge(IncidentLedger.key("p1", "v", "villager"), false, 0, W, -2, -12));
        assertEquals(-2, l.charge(IncidentLedger.key("p2", "v", "villager"), false, 0, W, -2, -12));
        assertEquals(-2, l.charge(IncidentLedger.key("p1", "v2", "villager"), false, 0, W, -2, -12));
    }

    @Test
    void tc09_categoryValues() {
        IncidentLedger l = new IncidentLedger();
        assertEquals(-3, l.charge("g", false, 0, W, -3, -15));
        assertEquals(-12, l.charge("g", true, 1, W, -3, -15), "守护者合计 −15");
        assertEquals(-2, l.charge("d", false, 0, W, -2, -10));
        assertEquals(-8, l.charge("d", true, 1, W, -2, -10));
        assertEquals(-1, l.charge("a", false, 0, W, -1, -4));
        assertEquals(-3, l.charge("a", true, 1, W, -1, -4));
    }

    @Test
    void tc10_pruneAndRestore() {
        IncidentLedger l = new IncidentLedger();
        String old = IncidentLedger.key("p", "v0", "villager");
        String k = IncidentLedger.key("p", "v1", "villager");
        l.charge(old, false, 0, W, -2, -12);
        l.charge(k, false, 500, W, -2, -12);
        l.prune(600, W);
        assertFalse(l.view().containsKey(old));
        IncidentLedger back = new IncidentLedger();
        l.view().forEach(back::restore);
        assertEquals(-10, back.charge(k, true, 650, W, -2, -12), "读回后同窗口只补差额");
        assertTrue(back.chargedRecently("p", 650, W));
        assertFalse(back.chargedRecently("q", 650, W));
    }

    @Test
    void tc11_curedOnce() {
        ConductState s = new ConductState();
        UUID v = UUID.randomUUID();
        assertTrue(s.markCured(v));
        assertFalse(s.markCured(v));
    }

    @Test
    void tc12_band() {
        assertEquals(GOOD, ConductRules.band(25, 20, 20));
        assertEquals(NEUTRAL, ConductRules.band(0, 20, 20));
        assertEquals(EVIL, ConductRules.band(-25, 20, 20));
        assertEquals(GOOD, ConductRules.band(10, 10, 10));
        assertEquals(NEUTRAL, ConductRules.band(9, 10, 10));
        assertEquals(EVIL, ConductRules.band(-10, 10, 10));
    }

    @Test
    void stateCopyAndPositiveDay() {
        PlayerMythData d = new PlayerMythData();
        d.conduct().addPositive(3, 6);
        d.conduct().addPositive(3, 6);
        assertEquals(12, d.conduct().positiveGranted(3));
        assertEquals(0, d.conduct().positiveGranted(4), "换日归零");
        PlayerMythData c = d.copy();
        assertEquals(d, c);
        c.conduct().markCured(UUID.randomUUID());
        assertNotEquals(d, c);
    }
}
