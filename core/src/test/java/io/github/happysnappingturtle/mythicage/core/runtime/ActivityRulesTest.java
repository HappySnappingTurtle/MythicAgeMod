package io.github.happysnappingturtle.mythicage.core.runtime;

import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActivityRulesTest {
    private static final RuntimeConfig.Idle IDLE = new RuntimeConfig.Idle(true, 12000);

    @Test
    void tc_t5_01_withinGraceIsActive() {
        assertTrue(ActivityRules.isActive(true, 111999, 100000, IDLE));
    }

    @Test
    void tc_t5_02_graceBoundaryIsIdle() {
        assertFalse(ActivityRules.isActive(true, 112000, 100000, IDLE));
    }

    @Test
    void tc_t5_03_idleDisabledMeansOnlineIsActive() {
        assertTrue(ActivityRules.isActive(true, 200000, 100000, new RuntimeConfig.Idle(false, 12000)));
    }

    @Test
    void tc_t5_04_offlineIsNeverActive() {
        assertFalse(ActivityRules.isActive(false, 100000, 100000, IDLE));
    }
}
