package io.github.happysnappingturtle.mythicage.core.power;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T13：希腊神力的纯规则（TC-T13-01～10）。 */
class GreekPowerRulesTest {

    // ───────────── 雷击（TC-T13-01） ─────────────

    @Test
    void tc01_bolt() {
        assertTrue(BoltRules.resolve(60, false, false, false).kill(), "最大生命恰 60 即死");
        BoltRules.Outcome o = BoltRules.resolve(61, false, false, false);
        assertFalse(o.kill());
        assertEquals(40, o.damage(), "61 只受 40");
        assertEquals(new BoltRules.Outcome(false, 40), BoltRules.resolve(50, false, true, false), "精英不即死");
        assertEquals(new BoltRules.Outcome(false, 40), BoltRules.resolve(300, true, false, false), "Boss 40");
        assertEquals(new BoltRules.Outcome(false, 40), BoltRules.resolve(20, true, false, false), "Boss 免疫即死");
        assertEquals(new BoltRules.Outcome(false, 12), BoltRules.resolve(20, false, false, true), "玩家 12");
    }

    // ───────────── 复原（TC-T13-09） ─────────────

    @Test
    void tc09_restoration() {
        assertEquals(4, RestorationRules.pulse(20), 1e-6);
        assertEquals(20, RestorationRules.pulse(20) * RestorationRules.PULSES, 1e-6, "5 次补满");
        assertEquals(0, RestorationRules.repairedDamage(100, 5), "剩 95% → 满，不溢出");
        assertEquals(89, RestorationRules.repairedDamage(100, 99), "剩 1 点 → 修上限的 10%");
        assertEquals(240 - 25, RestorationRules.repairedDamage(250, 240), "10% 向上取整");
        assertEquals(0, RestorationRules.repairedDamage(100, 0), "满耐久不变");
    }

    // ───────────── 友方扩展（TC-T13-10） ─────────────

    static PowerSpec spec(String profile) {
        return PowerSpec.parse("""
                {"schemaVersion":1,"id":"t","civilization":"greek","form":"scroll","targetProfile":"%s",
                 "matrix":{"caster":"heal","own_summon":"heal_summon","enemy_player":"pvp","other_summon":"enemy_summon"}}"""
                .formatted(profile), new ArrayList<>());
    }

    @Test
    void tc10_allyArea() {
        TargetFacts mate = TargetFacts.builder().player(true, true).build();
        TargetFacts foe = TargetFacts.builder().player(false, true).build();
        TargetFacts mateSummon = TargetFacts.builder().pvp(true).summon(false, true, false).build();
        PowerSpec ally = spec("ALLY_AREA");
        PowerSpec hostile = spec("HOSTILE_1");
        assertEquals("heal", TargetClassifier.reaction(ally, mate), "友方入口：队友按施法者列");
        assertEquals("none", TargetClassifier.reaction(hostile, mate), "伤害入口：队友仍为 none");
        assertEquals("heal_summon", TargetClassifier.reaction(ally, mateSummon), "队友召唤按自己召唤列");
        assertEquals("none", TargetClassifier.reaction(hostile, mateSummon));
        assertEquals("pvp", TargetClassifier.reaction(ally, foe), "非队友玩家照常取敌对玩家列");
        assertEquals("pvp", TargetClassifier.reaction(hostile, foe));
    }
}
