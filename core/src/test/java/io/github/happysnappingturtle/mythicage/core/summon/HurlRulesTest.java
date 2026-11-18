package io.github.happysnappingturtle.mythicage.core.summon;

import io.github.happysnappingturtle.mythicage.core.summon.HurlRules.Outcome;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** T25a 独眼巨人抓投（TC-T25a-30～33）。 */
class HurlRulesTest {
    @Test
    void tc30OpenGround() {
        assertEquals(8.0, HurlRules.landing(d -> true), 1e-9);
    }

    @Test
    void tc31WallPullsBack() {
        // 5 格以外是墙：落在 5 格处
        assertEquals(5.0, HurlRules.landing(d -> d <= 5.0), 1e-9);
        // 一直找不到：原地
        assertEquals(0.0, HurlRules.landing(d -> false), 1e-9);
    }

    @Test
    void tc32MythicAndBossMiss() {
        assertEquals(Outcome.MISS, HurlRules.decide(true, false, false, 0, 0, 0.6, 1.9));
        assertEquals(Outcome.MISS, HurlRules.decide(false, true, false, 0, 0, 0.6, 1.9));
        assertEquals(100, HurlRules.cooldown(Outcome.MISS), "抓空冷却 5 秒");
    }

    @Test
    void tc33PlayerKnockback() {
        assertEquals(Outcome.KNOCKBACK, HurlRules.decide(false, false, true, 0, 0, 0.6, 1.8));
        assertEquals(400, HurlRules.cooldown(Outcome.KNOCKBACK));
    }

    @Test
    void grabZombieAndLimits() {
        assertEquals(Outcome.GRAB, HurlRules.decide(false, false, false, 100, 0, 0.6, 1.95));
        assertEquals(400, HurlRules.cooldown(Outcome.GRAB));
        assertEquals(Outcome.MISS, HurlRules.decide(false, false, false, 100, 200, 0.6, 1.95), "抓取免疫中");
        assertEquals(Outcome.MISS, HurlRules.decide(false, false, false, 100, 0, 1.4, 1.4), "太宽");
    }
}
