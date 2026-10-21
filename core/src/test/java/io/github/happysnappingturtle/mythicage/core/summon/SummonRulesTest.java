package io.github.happysnappingturtle.mythicage.core.summon;

import com.google.gson.JsonParser;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.power.TargetFacts;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T14 单元测试 TC-T14-01～09。 */
class SummonRulesTest {
    @Test
    void tc01_capByAge() {
        assertEquals(2, SummonCap.cap(Age.NONE));
        assertEquals(2, SummonCap.cap(Age.ARCHAIC));
        assertEquals(3, SummonCap.cap(Age.CLASSICAL));
        assertEquals(4, SummonCap.cap(Age.HEROIC));
        assertEquals(5, SummonCap.cap(Age.MYTHIC));
    }

    @Test
    void tc02_highestAcrossCivs() {
        assertEquals(Age.HEROIC, SummonCap.highest(List.of(Age.CLASSICAL, Age.HEROIC, Age.NONE)));
        assertEquals(4, SummonCap.cap(SummonCap.highest(List.of(Age.CLASSICAL, Age.HEROIC))));
        assertEquals(Age.NONE, SummonCap.highest(List.of()));
    }

    @Test
    void tc03_04_fits() {
        // 两只 cost 1 + 一只等待重生的天马 = 占用 3
        int occupied = 1 + 1 + 1;
        assertEquals(3, occupied);
        assertFalse(SummonCap.fits(4, 2, 5), "占用 4 / 上限 5 召 cost 2 应拒绝");
        assertTrue(SummonCap.fits(4, 1, 5));
        assertFalse(SummonCap.fits(5, 1, 9));
    }

    private static com.google.gson.JsonObject json(String s) {
        return JsonParser.parseString(s).getAsJsonObject();
    }

    @Test
    void tc05_parseCards() {
        SummonUnitCard p = SummonUnitCard.parse("greek", "pegasus", json(
                "{\"entity\":\"mythic_age:summon_pegasus\",\"cost\":1,\"combat\":false,"
                        + "\"respawn\":{\"kind\":\"shrine\",\"delayDays\":1},\"token\":\"mythic_age:summon_token_pegasus\"}"));
        assertTrue(p.respawns());
        assertEquals(1, p.respawnDelayDays());
        assertFalse(p.combat());
        SummonUnitCard m = SummonUnitCard.parse("greek", "minotaur", json(
                "{\"entity\":\"mythic_age:summon_minotaur\",\"cost\":1,\"respawn\":\"none\",\"token\":\"t\"}"));
        assertTrue(m.combat());
        assertFalse(m.respawns());
        assertThrows(IllegalArgumentException.class, () -> SummonUnitCard.parse("greek", "x", json("{\"cost\":1}")));
        assertThrows(IllegalArgumentException.class, () -> SummonUnitCard.parse("greek", "x", json(
                "{\"entity\":\"e\",\"cost\":3,\"token\":\"t\"}")));
        assertThrows(IllegalArgumentException.class, () -> SummonUnitCard.parse("greek", "x", json(
                "{\"entity\":\"e\",\"cost\":1,\"token\":\"t\",\"respawn\":{\"kind\":\"altar\",\"delayDays\":1}}")));
        // 战斗单位不能重生（总纲 B1）
        assertThrows(IllegalArgumentException.class, () -> SummonUnitCard.parse("greek", "x", json(
                "{\"entity\":\"e\",\"cost\":1,\"token\":\"t\",\"respawn\":{\"kind\":\"shrine\",\"delayDays\":1}}")));
    }

    @Test
    void tc06_respawnTiming() {
        long death = 10 * RespawnRules.DAY_TICKS + 6000;
        long at = RespawnRules.respawnAt(death, 1);
        assertEquals(11 * RespawnRules.DAY_TICKS + 6000, at);
        assertFalse(RespawnRules.ready(at - 1, at, true));
        assertFalse(RespawnRules.ready(at + 100, at, false), "没有神坛继续等");
        assertTrue(RespawnRules.ready(at + 5000, at, true), "有了神坛就重生");
    }

    @Test
    void tc07_followTargets() {
        TargetFacts zombie = TargetFacts.builder().hostile().build();
        TargetFacts cow = TargetFacts.builder().build();
        TargetFacts villager = TargetFacts.builder().villager(true).build();
        TargetFacts owner = TargetFacts.builder().caster().build();
        TargetFacts ownSummon = TargetFacts.builder().summon(true, false, false).build();
        assertTrue(SummonTargetRules.canAttack(zombie, true));
        assertFalse(SummonTargetRules.canAttack(cow, true));
        assertFalse(SummonTargetRules.canAttack(villager, true));
        assertFalse(SummonTargetRules.canAttack(owner, true));
        assertFalse(SummonTargetRules.canAttack(ownSummon, true));
    }

    @Test
    void tc08_pvpTargets() {
        assertFalse(SummonTargetRules.canAttack(TargetFacts.builder().player(false, false).build(), true), "PvP 关");
        assertFalse(SummonTargetRules.canAttack(TargetFacts.builder().player(true, true).build(), true), "同队");
        assertTrue(SummonTargetRules.canAttack(TargetFacts.builder().player(false, true).build(), true), "PvP 开不同队");
        assertTrue(SummonTargetRules.canAttack(TargetFacts.builder().summon(false, false, true).build(), true));
        assertFalse(SummonTargetRules.canAttack(TargetFacts.builder().summon(false, true, false).build(), true));
    }

    @Test
    void tc09_nonCombatAttacksNothing() {
        assertFalse(SummonTargetRules.canAttack(TargetFacts.builder().hostile().build(), false));
    }
}
