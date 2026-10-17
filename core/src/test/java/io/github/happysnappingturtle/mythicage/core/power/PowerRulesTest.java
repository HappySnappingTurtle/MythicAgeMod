package io.github.happysnappingturtle.mythicage.core.power;

import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T12 阶段 ①：目标分类与矩阵、神力数据、冷却、施放会话（TC-T12-01～34）。 */
class PowerRulesTest {
    static final String HIT = """
            {"schemaVersion":1,"id":"test_hit","civilization":"greek","form":"scroll","castTicks":10,"cooldownTicks":1200,
             "targetProfile":"HOSTILE_1","matrix":{"hostile":"affect","elite":"elite","boss":"boss","enemy_player":"pvp",
             "other_summon":"pvp_enemy","neutral_animal":"pets"}}""";

    static PowerSpec spec(String json) {
        return PowerSpec.parse(json, new ArrayList<>());
    }

    static TargetFacts.Builder f() {
        return TargetFacts.builder();
    }

    // ───────────── 分类（TC-T12-01、02、05） ─────────────

    @Test
    void tc01_twelveColumns() {
        assertEquals(TargetCategory.CASTER, TargetClassifier.classify(f().caster().build()));
        assertEquals(TargetCategory.OWN_VILLAGER, TargetClassifier.classify(f().villager(true).build()));
        assertEquals(TargetCategory.OWN_SOLDIER, TargetClassifier.classify(f().soldier(true).build()));
        assertEquals(TargetCategory.OTHER_VILLAGE, TargetClassifier.classify(f().villager(false).build()));
        assertEquals(TargetCategory.OTHER_VILLAGE, TargetClassifier.classify(f().soldier(false).build()));
        assertEquals(TargetCategory.OWN_SUMMON, TargetClassifier.classify(f().summon(true, false, false).build()));
        assertEquals(TargetCategory.OTHER_SUMMON, TargetClassifier.classify(f().summon(false, false, true).build()));
        assertEquals(TargetCategory.GUARDIAN, TargetClassifier.classify(f().guardian().build()));
        assertEquals(TargetCategory.NEUTRAL_ANIMAL, TargetClassifier.classify(f().build()));
        assertEquals(TargetCategory.HOSTILE, TargetClassifier.classify(f().hostile().build()));
        assertEquals(TargetCategory.ELITE, TargetClassifier.classify(f().elite().build()));
        assertEquals(TargetCategory.BOSS, TargetClassifier.classify(f().boss().build()));
        assertEquals(TargetCategory.ENEMY_PLAYER, TargetClassifier.classify(f().player(false, true).build()));
    }

    @Test
    void tc02_priority() {
        assertEquals(TargetCategory.ELITE, TargetClassifier.classify(f().elite().hostile().build()), "精英优先于敌对");
        assertEquals(TargetCategory.BOSS, TargetClassifier.classify(f().boss().summon(true, false, false).build()));
        assertEquals(TargetCategory.CASTER, TargetClassifier.classify(f().caster().player(false, true).build()));
    }

    // ───────────── 反应键（TC-T12-03、04、06、07） ─────────────

    @Test
    void tc03_enemyPlayer() {
        PowerSpec s = spec(HIT);
        assertEquals("none", TargetClassifier.reaction(s, f().player(false, false).build()), "PvP 关");
        assertEquals("none", TargetClassifier.reaction(s, f().player(true, true).build()), "同队");
        assertEquals("pvp", TargetClassifier.reaction(s, f().player(false, true).build()));
    }

    @Test
    void tc04_otherSummon() {
        PowerSpec s = spec(HIT);
        assertEquals("none", TargetClassifier.reaction(s, f().summon(false, true, false).pvp(true).build()), "队友的");
        assertEquals("none", TargetClassifier.reaction(s, f().summon(false, false, true).pvp(false).build()), "PvP 关");
        assertEquals("none", TargetClassifier.reaction(s, f().summon(false, false, false).pvp(true).build()), "主人不敌对");
        assertEquals("pvp_enemy", TargetClassifier.reaction(s, f().summon(false, false, true).pvp(true).build()));
    }

    @Test
    void tc05_villagersNeverHurtByDefault() {
        PowerSpec s = spec(HIT);
        assertEquals("none", TargetClassifier.reaction(s, f().villager(true).build()));
        assertEquals("none", TargetClassifier.reaction(s, f().villager(false).build()));
        assertEquals("affect", TargetClassifier.reaction(s, f().hostile().build()));
    }

    @Test
    void tc06_freeForAll() {
        PowerSpec s = spec("""
                {"schemaVersion":1,"id":"test_ffa","civilization":"atlantean","form":"scroll","targetProfile":"FREE_FOR_ALL",
                 "matrix":{"caster":"affect","own_villager":"affect","enemy_player":"affect","other_summon":"affect",
                 "neutral_animal":"affect"}}""");
        assertEquals("affect", TargetClassifier.reaction(s, f().caster().build()));
        assertEquals("affect", TargetClassifier.reaction(s, f().villager(true).build()));
        assertEquals("affect", TargetClassifier.reaction(s, f().player(true, false).build()), "失控型不过滤");
        assertEquals("affect", TargetClassifier.reaction(s, f().pet().build()));
    }

    @Test
    void tc07_pets() {
        PowerSpec affect = spec(HIT.replace("\"neutral_animal\":\"pets\"", "\"neutral_animal\":\"affect\""));
        assertEquals("none", TargetClassifier.reaction(affect, f().pet().build()), "affect 不作用于宠物");
        assertEquals("affect", TargetClassifier.reaction(affect, f().build()));
        assertEquals("pets", TargetClassifier.reaction(spec(HIT), f().pet().build()), "写 pets 键时作用");
    }

    // ───────────── 数据（TC-T12-10～12） ─────────────

    @Test
    void tc10_parse() {
        PowerSpec s = spec(HIT);
        assertEquals("test_hit", s.id());
        assertEquals(Civilization.GREEK, s.civ());
        assertEquals(10, s.castTicks());
        assertEquals(1200, s.cooldownTicks());
        assertEquals(32, s.range());
        assertEquals("none", s.reaction(TargetCategory.GUARDIAN), "缺省列为 none");
        assertEquals(4.5, spec(HIT.replace("\"scroll\"", "\"placeable\"")).range());
        assertEquals(24, spec(HIT.replace("\"scroll\"", "\"throwable\"")).range());
        assertEquals(10, spec(HIT.replace("\"castTicks\":10", "\"castTicks\":10,\"range\":10")).range());
    }

    @Test
    void tc11_errorsCarryPath() {
        assertPath(HIT.replace("\"hostile\":", "\"monsters\":"), "$.matrix.monsters");
        assertPath(HIT.replace("HOSTILE_1", "ENEMIES"), "$.targetProfile");
        assertPath(HIT.replace("\"castTicks\":10", "\"castTicks\":61"), "$.castTicks");
        assertPath(HIT.replace("\"cooldownTicks\":1200", "\"cooldownTicks\":-1"), "$.cooldownTicks");
        assertPath(HIT.replace("\"greek\"", "\"roman\""), "$.civilization");
        assertPath(HIT.replace("\"scroll\"", "\"wand\""), "$.form");
    }

    private static void assertPath(String json, String path) {
        PowerSpec.PowerSpecException e = assertThrows(PowerSpec.PowerSpecException.class, () -> spec(json));
        assertTrue(e.getMessage().startsWith(path), e.getMessage());
    }

    @Test
    void tc12_unknownFieldWarns() {
        List<String> w = new ArrayList<>();
        PowerSpec.parse(HIT.replace("\"castTicks\":10", "\"castTicks\":10,\"color\":\"red\""), w);
        assertEquals(1, w.size());
    }

    // ───────────── 冷却（TC-T12-20～22） ─────────────

    @Test
    void tc20_sharedAndOwn() {
        PlayerMythData d = new PlayerMythData();
        CooldownRules.apply(d, Civilization.GREEK, "a", 1000, 600, 1200);
        assertTrue(CooldownRules.remaining(d, Civilization.GREEK, "a", 1100) > 0);
        assertEquals(500, CooldownRules.remaining(d, Civilization.GREEK, "b", 1100), "同文明 B 也在短冷却");
        assertEquals(0, CooldownRules.remaining(d, Civilization.GREEK, "b", 1600));
        assertEquals(600, CooldownRules.remaining(d, Civilization.GREEK, "a", 1600), "A 的长冷却还剩 600");
        assertEquals(0, CooldownRules.remaining(d, Civilization.GREEK, "a", 2200));
    }

    @Test
    void tc21_otherCivUnaffected() {
        PlayerMythData d = new PlayerMythData();
        CooldownRules.apply(d, Civilization.GREEK, "a", 1000, 600, 1200);
        assertEquals(0, CooldownRules.remaining(d, Civilization.EGYPTIAN, "e", 1100));
    }

    @Test
    void tc22_secondsRoundUp() {
        assertEquals(1, CooldownRules.remainingSeconds(1));
        assertEquals(1, CooldownRules.remainingSeconds(20));
        assertEquals(2, CooldownRules.remainingSeconds(21));
        assertEquals(0, CooldownRules.remainingSeconds(0));
    }

    @Test
    void tc22b_applyNeverShortens() {
        PlayerMythData d = new PlayerMythData();
        CooldownRules.apply(d, Civilization.GREEK, "a", 1000, 600, 3000);
        CooldownRules.apply(d, Civilization.GREEK, "a", 1100, 600, 100);
        assertEquals(2900, CooldownRules.remaining(d, Civilization.GREEK, "a", 1100));
    }

    // ───────────── 会话（TC-T12-30～34） ─────────────

    @Test
    void tc30_normalFlow() {
        CastSession s = CastSession.aim("c1", "a", 0, 200);
        assertEquals(CastSession.State.AIMING, s.state());
        assertTrue(s.confirm(5, 10));
        assertEquals(CastSession.Event.NONE, s.tick(14));
        assertEquals(0.9, s.progress(14), 1e-9);
        assertEquals(CastSession.Event.CHANNEL_DONE, s.tick(15));
        s.finish();
        assertEquals(PowerReason.COMMITTED, s.result());
    }

    @Test
    void tc31_aimTimeout() {
        CastSession s = CastSession.aim("c1", "a", 0, 200);
        assertEquals(CastSession.Event.NONE, s.tick(199));
        assertEquals(CastSession.Event.AIM_TIMEOUT, s.tick(200));
        assertEquals(PowerReason.AIM_TIMEOUT, s.result());
    }

    @Test
    void tc32_cancelKeepsFirstReason() {
        CastSession s = CastSession.aim("c1", "a", 0, 200);
        s.confirm(0, 20);
        s.cancel(PowerReason.INTERRUPTED_DAMAGE);
        s.cancel(PowerReason.INTERRUPTED_SWITCH);
        s.finish();
        assertEquals(PowerReason.INTERRUPTED_DAMAGE, s.result());
        assertEquals(CastSession.State.DONE, s.state());
    }

    @Test
    void tc34_duplicateConfirmIgnored() {
        CastSession s = CastSession.aim("c1", "a", 0, 200);
        assertTrue(s.confirm(0, 20));
        assertFalse(s.confirm(5, 20));
        assertEquals(CastSession.Event.CHANNEL_DONE, s.tick(20), "吟唱起点不被重复确认重置");
    }

    @Test
    void instantCast() {
        CastSession s = CastSession.aim("c1", "a", 0, 200);
        s.confirm(3, 0);
        assertEquals(CastSession.Event.CHANNEL_DONE, s.tick(3));
    }
}
