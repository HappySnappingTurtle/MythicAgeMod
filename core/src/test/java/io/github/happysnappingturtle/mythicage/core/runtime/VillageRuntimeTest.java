package io.github.happysnappingturtle.mythicage.core.runtime;

import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig;
import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig.Construction;
import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig.PublicMode;
import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig.PublicResources;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VillageRuntimeTest {
    private static final PublicResources PUBLIC =
            Fixtures.preset(RuntimeConfig.Preset.E).village().publicResources();
    private static final Construction CONSTRUCTION =
            Fixtures.preset(RuntimeConfig.Preset.E).village().construction();

    @Test
    void tc_t5_30_publicCatchUpIsCapped() {
        assertEquals(72000, PublicCatchUp.ticksToSimulate(0, 100000, false, PUBLIC));
        assertEquals(5000, PublicCatchUp.ticksToSimulate(0, 5000, false, PUBLIC));
    }

    @Test
    void tc_t5_31_publicLoadedOnly() {
        PublicResources p = new PublicResources(PublicMode.LOADED_ONLY, 128, false, 72000, 1.0);
        assertEquals(0, PublicCatchUp.ticksToSimulate(0, 100000, true, p));
    }

    @Test
    void publicRequireOwnerOnline() {
        PublicResources p = new PublicResources(PublicMode.LOGICAL_WHEN_NOT_REAL, 128, true, 72000, 1.0);
        assertEquals(0, PublicCatchUp.ticksToSimulate(0, 100000, false, p));
        assertEquals(72000, PublicCatchUp.ticksToSimulate(0, 100000, true, p));
        assertEquals(0, PublicCatchUp.ticksToSimulate(100, 50, true, p));
    }

    @Test
    void tc_t5_32_realRunRadiusBoundary() {
        assertTrue(VillageRunCondition.realRunning(true, 128.0, 128));
        assertFalse(VillageRunCondition.realRunning(true, 128.01, 128));
        assertFalse(VillageRunCondition.realRunning(false, 1, 128));
    }

    @Test
    void tc_t5_33_constructionBankCapped() {
        assertEquals(24000, ConstructionBank.accrue(0, 30000, CONSTRUCTION));
        assertEquals(24000, ConstructionBank.accrue(20000, Long.MAX_VALUE, CONSTRUCTION));
        assertEquals(100, ConstructionBank.accrue(100, -5, CONSTRUCTION));
    }

    @Test
    void tc_t5_34_constructionBankSpend() {
        var first = ConstructionBank.spend(100, 80, CONSTRUCTION);
        assertEquals(80, first.extraTicks());
        assertEquals(20, first.remaining());
        var second = ConstructionBank.spend(first.remaining(), 80, CONSTRUCTION);
        assertEquals(20, second.extraTicks());
        assertEquals(0, second.remaining());
    }

    @Test
    void constructionBankDisabledByZeroCap() {
        Construction c = new Construction(false, 0, 2.0);
        assertEquals(0, ConstructionBank.accrue(0, 30000, c));
    }
}
