package io.github.happysnappingturtle.mythicage.core.config.conduct;

import io.github.happysnappingturtle.mythicage.core.config.ConfigIssue;
import io.github.happysnappingturtle.mythicage.core.config.ConfigLoadResult;
import io.github.happysnappingturtle.mythicage.core.config.ConfigLoader;
import io.github.happysnappingturtle.mythicage.core.config.conduct.ConductConfig.Events;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConductSchemaTest {
    private static final ConductSchema S = ConductSchema.INSTANCE;

    private static ConfigLoadResult<ConductConfig> load(String json) {
        return ConfigLoader.load(S, json, null);
    }

    @Test
    void tc_t3_50_defaultsMatchContract02() {
        ConductConfig c = S.defaults();
        assertTrue(c.enabled());
        assertEquals(-100, c.range().min());
        assertEquals(100, c.range().max());
        assertEquals(0, c.initial());
        assertEquals(2, c.regression().points());
        assertEquals(24000, c.regression().intervalOnlineTicks());
        assertEquals(12, c.positiveDailyCap());
        assertEquals(200, c.incidentWindowTicks());
        assertEquals(new Events(-2, -12, -3, -15, -2, -10, -1, -4, 6, 3, 2), c.events());
        assertTrue(c.selfDefenseExempt());
        assertFalse(c.pvpCountsAsConduct());
        assertEquals(c, load("{}").value());
    }

    @Test
    void tc_t3_51_rangeMustStraddleZero() {
        ConfigLoadResult<ConductConfig> r = load("{\"range\":{\"min\":10}}");
        assertEquals(S.defaults().range(), r.value().range());
        assertEquals(List.of("range"), r.issues().stream().map(ConfigIssue::path).toList());
    }

    @Test
    void tc_t3_52_initialOutsideRangeFallsBack() {
        ConfigLoadResult<ConductConfig> r = load("{\"initial\":150}");
        assertEquals(0, r.value().initial());
        assertEquals(1, r.issues().size());
    }

    @Test
    void tc_t3_53_negativeEventWrittenPositiveFallsBack() {
        ConfigLoadResult<ConductConfig> r = load("{\"events\":{\"harmVillager\":2}}");
        assertEquals(-2, r.value().events().harmVillager());
        assertEquals("events.harmVillager", r.issues().get(0).path());
    }

    @Test
    void tc_t3_54_killLighterThanHarmRevertsBoth() {
        ConfigLoadResult<ConductConfig> r = load("{\"events\":{\"harmVillager\":-5,\"killVillagerTotal\":-1}}");
        assertEquals(-2, r.value().events().harmVillager());
        assertEquals(-12, r.value().events().killVillagerTotal());
        assertEquals(1, r.issues().size());
        assertEquals("events.killVillagerTotal", r.issues().get(0).path());
    }

    @Test
    void tc_t3_55_outsideVillageAnimalKeyIsUnknownAndAbsent() {
        ConfigLoadResult<ConductConfig> r = load("{\"events\":{\"harmAnimalOutsideVillage\":-1}}");
        assertEquals(S.defaults(), r.value());
        assertEquals(List.of("events.harmAnimalOutsideVillage"), r.issues().stream().map(ConfigIssue::path).toList());
        // 硬边界：配置类型中根本不存在村外动物字段
        assertTrue(Arrays.stream(Events.class.getRecordComponents())
                .noneMatch(c -> c.getName().toLowerCase().contains("outside")));
    }

    @Test
    void tc_t3_56_canBeDisabled() {
        ConfigLoadResult<ConductConfig> r = load("{\"enabled\":false}");
        assertFalse(r.value().enabled());
        assertTrue(r.issues().isEmpty());
    }

    @Test
    void validCustomValuesAreKept() {
        ConfigLoadResult<ConductConfig> r = load("{\"range\":{\"min\":-50,\"max\":80},\"initial\":-10,"
                + "\"events\":{\"harmGuardian\":-4,\"killGuardianTotal\":-20}}");
        assertTrue(r.issues().isEmpty(), r.issues().toString());
        assertEquals(-50, r.value().range().min());
        assertEquals(-10, r.value().initial());
        assertEquals(-20, r.value().events().killGuardianTotal());
    }

    @Test
    void defaultFileRoundTrips() {
        ConfigLoadResult<ConductConfig> r = load(ConfigLoader.defaultFileContent(S));
        assertEquals(S.defaults(), r.value());
        assertTrue(r.issues().isEmpty(), r.issues().toString());
    }
}
