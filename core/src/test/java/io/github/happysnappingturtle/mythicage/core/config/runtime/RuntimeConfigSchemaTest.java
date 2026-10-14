package io.github.happysnappingturtle.mythicage.core.config.runtime;

import io.github.happysnappingturtle.mythicage.core.config.ConfigIssue;
import io.github.happysnappingturtle.mythicage.core.config.ConfigLoadResult;
import io.github.happysnappingturtle.mythicage.core.config.ConfigLoader;
import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig.FavorMode;
import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig.OfflineClock;
import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig.Preset;
import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig.PublicMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeConfigSchemaTest {
    private static final RuntimeConfigSchema S = RuntimeConfigSchema.INSTANCE;

    /** 03 号文档 §13.4 中 E 预设的默认值 JSON（原样摘录，另加本实现约定的 preset 键）。 */
    private static final String DOC_DEFAULTS = """
            {
              "schemaVersion": 1,
              "preset": "E",
              "favor": {
                "mode": "online_snapshot",
                "idle": { "enabled": true, "graceTicks": 12000 },
                "snapshot": {
                  "rateFactor": 1.0,
                  "maxTicksSinceVisit": 120000,
                  "visitMinTicks": 600,
                  "verifyTicks": 24000,
                  "unverifiedFactor": 0.5
                },
                "offline": { "rate": 0.0, "maxBufferDays": 1.0, "clock": "server_ticks" },
                "catchUp": { "maxSimulationTicks": 24000 }
              },
              "norse": {
                "rested": { "enabled": false, "multiplier": 2.0, "maxBufferDays": 1.0, "bypassDailyCap": true }
              },
              "village": {
                "forceLoad": { "enabled": false },
                "public": {
                  "mode": "logical_when_not_real",
                  "realRunRadius": 128,
                  "requireOwnerOnline": false,
                  "maxCatchUpTicks": 72000,
                  "rateFactor": 1.0
                },
                "construction": { "simulateWhenUnloaded": false, "bankMaxTicks": 24000, "bankSpendMultiplier": 2.0 },
                "population": { "simulateWhenUnloaded": false }
              }
            }
            """;

    private static ConfigLoadResult<RuntimeConfig> load(String json) {
        return ConfigLoader.load(S, json, null);
    }

    @Test
    void tc_t3_30_defaultsMatchDesignDocument() {
        ConfigLoadResult<RuntimeConfig> fromDoc = load(DOC_DEFAULTS);
        assertTrue(fromDoc.issues().isEmpty(), fromDoc.issues().toString());
        assertEquals(S.defaults(), fromDoc.value());
        assertEquals(S.defaults(), load("{}").value());
    }

    @Test
    void tc_t3_31_presetF() {
        RuntimeConfig c = load("{\"preset\":\"F\"}").value();
        assertEquals(Preset.F, c.preset());
        assertEquals(0.5, c.favor().offline().rate());
        assertTrue(c.norse().rested().enabled());
        assertEquals(S.defaults().favor().mode(), c.favor().mode());
        assertEquals(S.defaults().village(), c.village());
    }

    @Test
    void tc_t3_32_presetC() {
        RuntimeConfig c = load("{\"preset\":\"C\"}").value();
        assertEquals(FavorMode.CAPPED_CATCHUP, c.favor().mode());
        assertEquals(24000, c.village().publicResources().maxCatchUpTicks());
        assertEquals(0, c.village().construction().bankMaxTicks());
    }

    @Test
    void tc_t3_33_presetsAandB() {
        RuntimeConfig a = load("{\"preset\":\"A\"}").value();
        assertEquals(FavorMode.LOADED_ONLY, a.favor().mode());
        assertEquals(PublicMode.LOADED_ONLY, a.village().publicResources().mode());
        assertFalse(a.village().forceLoad().enabled());

        RuntimeConfig b = load("{\"preset\":\"B\"}").value();
        assertEquals(FavorMode.LOADED_ONLY, b.favor().mode());
        assertEquals(PublicMode.LOADED_ONLY, b.village().publicResources().mode());
        assertTrue(b.village().forceLoad().enabled());
    }

    @Test
    void tc_t3_34_explicitKeyOverridesPreset() {
        RuntimeConfig c = load("{\"preset\":\"C\",\"favor\":{\"mode\":\"online_snapshot\"}}").value();
        assertEquals(FavorMode.ONLINE_SNAPSHOT, c.favor().mode());
        assertEquals(Preset.C, c.preset());
        assertEquals(24000, c.village().publicResources().maxCatchUpTicks());
        assertEquals(0, c.village().construction().bankMaxTicks());
    }

    @Test
    void tc_t3_35_unknownPresetFallsBackToE() {
        ConfigLoadResult<RuntimeConfig> r = load("{\"preset\":\"Z\"}");
        assertEquals(S.defaults(), r.value());
        assertEquals(1, r.issues().size());
        assertEquals("preset", r.issues().get(0).path());
    }

    @ParameterizedTest
    @CsvSource({"1199,12000,1", "1200,1200,0", "72000,72000,0", "72001,12000,1"})
    void tc_t3_36_graceTicksBoundaries(int written, int expected, int warnings) {
        ConfigLoadResult<RuntimeConfig> r = load("{\"favor\":{\"idle\":{\"graceTicks\":" + written + "}}}");
        assertEquals(expected, r.value().favor().idle().graceTicks());
        assertEquals(warnings, r.issues().size());
    }

    @Test
    void tc_t3_37_zeroCareWindowMeansUnlimited() {
        ConfigLoadResult<RuntimeConfig> r = load("{\"favor\":{\"snapshot\":{\"maxTicksSinceVisit\":0}}}");
        assertEquals(0, r.value().favor().snapshot().maxTicksSinceVisit());
        assertTrue(r.issues().isEmpty());
    }

    @Test
    void tc_t3_38_wallClockIsValid() {
        ConfigLoadResult<RuntimeConfig> r = load("{\"favor\":{\"offline\":{\"clock\":\"wall_clock\"}}}");
        assertEquals(OfflineClock.WALL_CLOCK, r.value().favor().offline().clock());
        assertTrue(r.issues().isEmpty());
    }

    @Test
    void tc_t3_39_constructionSimulationCannotBeEnabled() {
        ConfigLoadResult<RuntimeConfig> r = load("{\"village\":{\"construction\":{\"simulateWhenUnloaded\":true}}}");
        assertFalse(r.value().village().construction().simulateWhenUnloaded());
        assertEquals(1, r.issues().size());
        assertEquals(ConfigIssue.Severity.WARN, r.issues().get(0).severity());
        assertEquals("village.construction.simulateWhenUnloaded", r.issues().get(0).path());
    }

    @Test
    void everyPresetRoundTripsThroughJson() {
        for (Preset p : Preset.values()) {
            RuntimeConfig expected = RuntimeConfigSchema.preset(p);
            ConfigLoadResult<RuntimeConfig> r = load(ConfigLoader.toPrettyJson(S, expected));
            assertEquals(expected, r.value(), p.name());
            assertTrue(r.issues().isEmpty(), p + ": " + r.issues());
        }
    }
}
