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
        assertEquals(new WorldRulesConfig(true, true, true, new WorldRulesConfig.GodPowers(true)), r.value());
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
}
