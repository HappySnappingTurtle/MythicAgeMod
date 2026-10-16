package io.github.happysnappingturtle.mythicage.core.config.world;

import io.github.happysnappingturtle.mythicage.core.config.ConfigLoadResult;
import io.github.happysnappingturtle.mythicage.core.config.ConfigLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldRulesSchemaTest {
    private static final WorldRulesSchema S = WorldRulesSchema.INSTANCE;

    @Test
    void tc_t3_40_defaults() {
        WorldRulesConfig c = S.defaults();
        assertTrue(c.beneficialConversions());
        assertTrue(c.mobBlockDamage());
        assertTrue(c.blockEntityStall());
        assertFalse(c.godPowers().terrainEdits());
        assertEquals(c, ConfigLoader.load(S, "{}", null).value());
    }

    @Test
    void tc_t3_41_onlyTerrainEditsChanges() {
        ConfigLoadResult<WorldRulesConfig> r = ConfigLoader.load(S, "{\"godPowers\":{\"terrainEdits\":true}}", null);
        WorldRulesConfig d = S.defaults();
        assertEquals(new WorldRulesConfig(true, true, true, new WorldRulesConfig.GodPowers(true), d.recipes(),
                d.shrines(), d.guardians(), d.greek()), r.value());
        assertTrue(r.issues().isEmpty());
    }

    @Test
    void tc_t3_42_stringBooleanIsRejected() {
        ConfigLoadResult<WorldRulesConfig> r = ConfigLoader.load(S, "{\"mobBlockDamage\":\"false\"}", null);
        assertTrue(r.value().mobBlockDamage());
        assertEquals(1, r.issues().size());
        assertEquals("mobBlockDamage", r.issues().get(0).path());
    }

    @Test
    void defaultFileRoundTrips() {
        ConfigLoadResult<WorldRulesConfig> r = ConfigLoader.load(S, ConfigLoader.defaultFileContent(S), null);
        assertEquals(S.defaults(), r.value());
        assertTrue(r.issues().isEmpty());
    }

    @Test
    void tc_t6_27c_ageGatedSwitches() {
        WorldRulesConfig d = S.defaults();
        assertTrue(d.recipes().ageGated().enabled());
        assertFalse(d.recipes().ageGated().allowAutomation());
        ConfigLoadResult<WorldRulesConfig> r = ConfigLoader.load(S,
                "{\"recipes\":{\"ageGated\":{\"enabled\":false,\"allowAutomation\":true}}}", null);
        assertFalse(r.value().recipes().ageGated().enabled());
        assertTrue(r.value().recipes().ageGated().allowAutomation());
        assertTrue(r.issues().isEmpty());
    }

    @Test
    void tc_t6_27d_badRecipesSectionFallsBack() {
        ConfigLoadResult<WorldRulesConfig> r = ConfigLoader.load(S, "{\"recipes\":{\"ageGated\":3}}", null);
        assertEquals(S.defaults().recipes(), r.value().recipes());
        assertFalse(r.issues().isEmpty());
    }

    @Test
    void tc_t6b_64_growthDefaultsAndOverrides() {
        WorldRulesConfig.Growth g = S.defaults().greek().growth();
        assertEquals(24000, g.oliveFruitTicks());
        assertEquals(12000, g.grapeStageTicks());
        assertEquals(0.5, g.boneMeal().grapeChance());
        assertFalse(g.boneMeal().oliveLeaves());
        ConfigLoadResult<WorldRulesConfig> r = ConfigLoader.load(S,
                "{\"greek\":{\"growth\":{\"grapeStageTicks\":6000,\"boneMeal\":{\"oliveLeaves\":true,\"grapeChance\":2}}}}", null);
        assertEquals(6000, r.value().greek().growth().grapeStageTicks());
        assertTrue(r.value().greek().growth().boneMeal().oliveLeaves());
        assertEquals(0.5, r.value().greek().growth().boneMeal().grapeChance(), "越界值回退为默认");
        assertEquals(1, r.issues().size());
    }

    @Test
    void tc_t8a_shrineDefaults() {
        WorldRulesConfig.Shrine t = S.defaults().shrine("greek:altar");
        assertEquals(1, t.limit("archaic"));
        assertEquals(2, t.limit("classical"));
        assertEquals(3, t.limit("mythic"));
        assertEquals(0, t.limit("none"));
        assertEquals(15, t.spacing());
        assertTrue(t.protect());
        assertEquals(2048, S.defaults().greek().temple().scanCellsPerTick());
        assertEquals(WorldRulesConfig.Shrine.FALLBACK, S.defaults().shrine("norse:horgr"), "未配置的种类用兜底值");
    }

    @Test
    void tc_t10b_06_onlyOneShrineFieldChanges() {
        ConfigLoadResult<WorldRulesConfig> r = ConfigLoader.load(S,
                "{\"shrines\":{\"greek\":{\"altar\":{\"limitByAge\":{\"archaic\":2}}}}}", null);
        WorldRulesConfig.Shrine a = r.value().shrine("greek:altar");
        assertEquals(new WorldRulesConfig.Shrine(2, 2, 3, 3, 15, true), a);
        assertTrue(r.issues().isEmpty(), () -> r.issues().toString());
    }

    @Test
    void tc_t10b_07_shrinesMergeByKind() {
        WorldRulesConfig base = S.defaults().withShrine("chinese:tudi", new WorldRulesConfig.Shrine(1, 2, 4, 6, 3, true));
        java.util.List<io.github.happysnappingturtle.mythicage.core.config.ConfigIssue> issues = new java.util.ArrayList<>();
        WorldRulesConfig v = S.read(io.github.happysnappingturtle.mythicage.core.config.ObjectReader.root(
                com.google.gson.JsonParser.parseString("{\"shrines\":{\"greek\":{\"altar\":{\"spacing\":20}}}}")
                        .getAsJsonObject(), issues), base);
        assertEquals(20, v.shrine("greek:altar").spacing());
        assertEquals(new WorldRulesConfig.Shrine(1, 2, 4, 6, 3, true), v.shrine("chinese:tudi"), "文件没写的种类保留默认");
        assertTrue(issues.isEmpty(), issues::toString);
    }

    @Test
    void tc_t10b_08_version1TemplesAreMigrated() {
        ConfigLoadResult<WorldRulesConfig> r = ConfigLoader.load(S, """
                {"schemaVersion":1,"temples":{"altarLimitByAge":{"archaic":2},"altarSpacing":20,"protectAltar":false,
                 "scanCellsPerTick":4096,"rescanIntervalTicks":12000}}""", null);
        assertEquals(new WorldRulesConfig.Shrine(2, 2, 3, 3, 20, false), r.value().shrine("greek:altar"));
        assertEquals(new WorldRulesConfig.TempleBudget(4096, 12000), r.value().greek().temple());
        assertEquals(1, r.issues().size(), () -> r.issues().toString());
        assertEquals("temples", r.issues().get(0).path());
        String out = ConfigLoader.toPrettyJson(S, r.value());
        assertFalse(out.contains("\"temples\""), "toJson 只输出新结构");
        assertTrue(out.contains("\"shrines\"") && out.contains("\"schemaVersion\": 2"));
    }

    @Test
    void tc_t10b_09_newStructureWinsAndOldGrowthMigrates() {
        ConfigLoadResult<WorldRulesConfig> r = ConfigLoader.load(S, """
                {"temples":{"altarSpacing":20},"shrines":{"greek":{"altar":{"spacing":30}}},
                 "growth":{"grapeStageTicks":6000}}""", null);
        assertEquals(30, r.value().shrine("greek:altar").spacing(), "新旧同时存在时以新结构为准");
        assertEquals(6000, r.value().greek().growth().grapeStageTicks(), "旧 growth 读作 greek.growth");
        java.util.List<String> paths = r.issues().stream().map(i -> i.path()).toList();
        assertEquals(java.util.List.of("temples", "growth"), paths);
    }

    @Test
    void tc_t10b_06b_badShrineKeys() {
        ConfigLoadResult<WorldRulesConfig> r = ConfigLoader.load(S,
                "{\"shrines\":{\"elves\":{\"tree\":{}},\"greek\":{\"Bad-Kind\":{}}}}", null);
        assertEquals(S.defaults().shrines(), r.value().shrines());
        java.util.List<String> paths = r.issues().stream().map(i -> i.path()).toList();
        assertTrue(paths.contains("shrines.elves") && paths.contains("shrines.greek.Bad-Kind"), paths::toString);
    }

    /** TC-T10-14（配置部分）：greek.prayer.restockLimit 默认 1，取 0–2，越界回退。 */
    @Test
    void tc_t10_14_prayerRestockLimit() {
        assertEquals(1, S.defaults().greek().prayer().restockLimit());
        assertEquals(0, ConfigLoader.load(S, "{\"greek\":{\"prayer\":{\"restockLimit\":0}}}", null)
                .value().greek().prayer().restockLimit());
        ConfigLoadResult<WorldRulesConfig> bad = ConfigLoader.load(S, "{\"greek\":{\"prayer\":{\"restockLimit\":3}}}", null);
        assertEquals(1, bad.value().greek().prayer().restockLimit());
        assertEquals(1, bad.issues().size());
    }
}
