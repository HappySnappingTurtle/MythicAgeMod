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
        assertEquals(new WorldRulesConfig(true, true, true, new WorldRulesConfig.GodPowers(true), S.defaults().recipes()),
                r.value());
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
}
