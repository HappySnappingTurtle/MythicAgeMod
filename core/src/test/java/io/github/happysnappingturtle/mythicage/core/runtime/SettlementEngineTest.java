package io.github.happysnappingturtle.mythicage.core.runtime;

import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig.Favor;
import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig.FavorMode;
import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig.Offline;
import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig.OfflineClock;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.runtime.SettlementEngine.Conditions;
import io.github.happysnappingturtle.mythicage.core.runtime.SettlementEngine.Mode;
import io.github.happysnappingturtle.mythicage.core.runtime.SettlementEngine.Settlement;
import org.junit.jupiter.api.Test;

import static io.github.happysnappingturtle.mythicage.core.runtime.Fixtures.RATE;
import static io.github.happysnappingturtle.mythicage.core.runtime.Fixtures.verifiedLedger;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettlementEngineTest {
    private static final Conditions ACTIVE_AWAY = Conditions.away(true);
    private static final Conditions INACTIVE = Conditions.away(false);

    @Test
    void tc_t5_10_realRunningCreditsReportedYield() {
        ProducerLedger l = verifiedLedger("p");
        Settlement s = SettlementEngine.settle(l, 100, Conditions.real(3, false), Fixtures.e());
        assertEquals(Mode.REAL, s.mode());
        assertEquals(3, s.credited());
        assertEquals(100, l.lastSettledTick());
    }

    @Test
    void tc_t5_11_snapshotVerified() {
        ProducerLedger l = verifiedLedger("p");
        Settlement s = SettlementEngine.settle(l, 24000, ACTIVE_AWAY, Fixtures.e());
        assertEquals(Mode.SNAPSHOT, s.mode());
        assertEquals(48, s.credited());
    }

    @Test
    void tc_t5_12_snapshotUnverifiedIsDiscounted() {
        ProducerLedger l = new ProducerLedger(new ProducerId("p"), Fixtures.OWNER, Civilization.GREEK, RATE, 0);
        assertEquals(24, SettlementEngine.settle(l, 24000, ACTIVE_AWAY, Fixtures.e()).credited());
    }

    @Test
    void tc_t5_13_inactiveOwnerProducesNothingButTimeAdvances() {
        ProducerLedger l = verifiedLedger("p");
        Settlement s = SettlementEngine.settle(l, 24000, INACTIVE, Fixtures.e());
        assertEquals(Mode.NONE, s.mode());
        assertEquals(0, s.credited());
        assertEquals(24000, l.lastSettledTick());
        // 这段时间作废：之后恢复活跃也不会补发
        assertEquals(1, SettlementEngine.settle(l, 24500, ACTIVE_AWAY, Fixtures.e()).credited());
    }

    @Test
    void tc_t5_13b_inactiveWhileRealRunningStillNone() {
        ProducerLedger l = verifiedLedger("p");
        Settlement s = SettlementEngine.settle(l, 1000, new Conditions(true, false, false, 5), Fixtures.e());
        assertEquals(Mode.NONE, s.mode());
        assertEquals(0, s.credited());
    }

    @Test
    void tc_t5_14_visitPeriodExpired() {
        Favor e = Fixtures.e();
        ProducerLedger expired = verifiedLedger("p");
        expired.setLastSettledTick(120000);
        assertEquals(Mode.NONE, SettlementEngine.settle(expired, 120001, ACTIVE_AWAY, e).mode());
        ProducerLedger edge = verifiedLedger("q");
        edge.setLastSettledTick(119000);
        assertEquals(Mode.SNAPSHOT, SettlementEngine.settle(edge, 120000, ACTIVE_AWAY, e).mode());
    }

    @Test
    void tc_t5_15_unlimitedVisitPeriod() {
        ProducerLedger l = verifiedLedger("p");
        l.setLastSettledTick(10_000_000 - 500);
        Settlement s = SettlementEngine.settle(l, 10_000_000, ACTIVE_AWAY, Fixtures.withMaxTicksSinceVisit(Fixtures.e(), 0));
        assertEquals(Mode.SNAPSHOT, s.mode());
        assertEquals(1, s.credited());
    }

    @Test
    void tc_t5_16_invalidSnapshot() {
        ProducerLedger l = verifiedLedger("p");
        l.invalidate();
        assertEquals(Mode.NONE, SettlementEngine.settle(l, 24000, ACTIVE_AWAY, Fixtures.e()).mode());
    }

    @Test
    void tc_t5_17_sameTickTwiceCreditsOnce() {
        ProducerLedger l = verifiedLedger("p");
        assertEquals(48, SettlementEngine.settle(l, 24000, ACTIVE_AWAY, Fixtures.e()).credited());
        Settlement again = SettlementEngine.settle(l, 24000, ACTIVE_AWAY, Fixtures.e());
        assertEquals(0, again.interval());
        assertEquals(0, again.credited());
    }

    @Test
    void tc_t5_18_realThenSnapshotDoNotOverlap() {
        ProducerLedger l = verifiedLedger("p");
        SettlementEngine.settle(l, 12000, Conditions.real(0, false), Fixtures.e());
        Settlement s = SettlementEngine.settle(l, 24000, ACTIVE_AWAY, Fixtures.e());
        assertEquals(12000, s.interval());
        assertEquals(24, s.credited());
    }

    @Test
    void tc_t5_19_fractionalCarryAccumulates() {
        ProducerLedger l = verifiedLedger("p");
        long[] got = new long[5];
        for (int i = 0; i < 5; i++) {
            got[i] = SettlementEngine.settle(l, (i + 1) * 100L, ACTIVE_AWAY, Fixtures.e()).credited();
        }
        assertEquals("[0, 0, 0, 0, 1]", java.util.Arrays.toString(got));
        assertTrue(l.carry() < 1e-6);
    }

    @Test
    void tc_t5_20_ledgersAreIndependent() {
        ProducerLedger a = verifiedLedger("a");
        ProducerLedger b = verifiedLedger("b");
        assertEquals(48, SettlementEngine.settle(a, 24000, ACTIVE_AWAY, Fixtures.e()).credited());
        assertEquals(48, SettlementEngine.settle(b, 24000, ACTIVE_AWAY, Fixtures.e()).credited());
    }

    @Test
    void tc_t5_21_loadedOnlyIgnoresSnapshot() {
        ProducerLedger l = verifiedLedger("p");
        Favor a = Fixtures.withMode(Fixtures.e(), FavorMode.LOADED_ONLY);
        assertEquals(Mode.NONE, SettlementEngine.settle(l, 24000, ACTIVE_AWAY, a).mode());
        assertEquals(Mode.REAL, SettlementEngine.settle(l, 24100, Conditions.real(1, false), a).mode());
    }

    @Test
    void tc_t5_22_cappedCatchUp() {
        ProducerLedger l = verifiedLedger("p");
        Favor c = Fixtures.withMode(Fixtures.e(), FavorMode.CAPPED_CATCHUP);
        Settlement away = SettlementEngine.settle(l, 72000, ACTIVE_AWAY, c);
        assertEquals(0, away.credited());
        Settlement back = SettlementEngine.settle(l, 72020, Conditions.real(0, false), c);
        assertEquals(Mode.REAL, back.mode());
        assertEquals(48, back.credited());
        assertEquals(0, l.awayActiveTicks());
    }

    @Test
    void tc_t5_22b_cappedCatchUpCountsOnlyActiveTicks() {
        ProducerLedger l = verifiedLedger("p");
        Favor c = Fixtures.withMode(Fixtures.e(), FavorMode.CAPPED_CATCHUP);
        SettlementEngine.settle(l, 6000, ACTIVE_AWAY, c);
        SettlementEngine.settle(l, 60000, INACTIVE, c);
        assertEquals(12, SettlementEngine.settle(l, 60020, Conditions.real(0, false), c).credited());
    }

    @Test
    void tc_t5_23_offlineBufferAccruesAndCaps() {
        Favor f = Fixtures.f();
        ProducerLedger l = verifiedLedger("p");
        Settlement s = SettlementEngine.settle(l, 12000, INACTIVE, f);
        assertEquals(Mode.OFFLINE, s.mode());
        assertEquals(0, s.credited());
        assertEquals(12, l.offlineBuffer(), 1e-9);
        SettlementEngine.settle(l, 1_000_000, INACTIVE, Fixtures.withMaxTicksSinceVisit(f, 0));
        assertEquals(48, l.offlineBuffer(), 1e-9);
    }

    @Test
    void tc_t5_24_offlineBufferReleasedOnReturn() {
        Favor f = Fixtures.f();
        ProducerLedger l = verifiedLedger("p");
        SettlementEngine.settle(l, 12000, INACTIVE, f);
        Settlement s = SettlementEngine.settle(l, 12500, ACTIVE_AWAY, f);
        assertEquals(Mode.SNAPSHOT, s.mode());
        assertEquals(12 + 1, s.credited());
        assertEquals(0, l.offlineBuffer());
    }

    @Test
    void tc_t5_24b_bufferFrozenWhenOfflineRateIsZero() {
        ProducerLedger l = verifiedLedger("p");
        SettlementEngine.settle(l, 12000, INACTIVE, Fixtures.f());
        Settlement s = SettlementEngine.settle(l, 12500, ACTIVE_AWAY, Fixtures.e());
        assertEquals(1, s.credited());
        assertEquals(12, l.offlineBuffer(), 1e-9);
        Favor zeroRate = Fixtures.withOffline(Fixtures.f(), new Offline(0, 1, OfflineClock.SERVER_TICKS));
        assertEquals(Mode.NONE, SettlementEngine.settle(l, 13000, INACTIVE, zeroRate).mode());
    }

    @Test
    void tc_t5_25_visitRequiresMinimumStay() {
        Favor e = Fixtures.e();
        ProducerLedger l = verifiedLedger("p");
        l.setLastVisitTick(-5);
        SettlementEngine.settle(l, 599, Conditions.real(0, true), e);
        assertEquals(-5, l.lastVisitTick());
        SettlementEngine.settle(l, 600, Conditions.real(0, true), e);
        assertEquals(600, l.lastVisitTick());
    }

    @Test
    void tc_t5_25b_leavingResetsStay() {
        Favor e = Fixtures.e();
        ProducerLedger l = verifiedLedger("p");
        l.setLastVisitTick(-5);
        SettlementEngine.settle(l, 400, Conditions.real(0, true), e);
        SettlementEngine.settle(l, 420, Conditions.real(0, false), e);
        SettlementEngine.settle(l, 900, Conditions.real(0, true), e);
        assertEquals(-5, l.lastVisitTick());
        SettlementEngine.settle(l, 1020, Conditions.real(0, true), e);
        assertEquals(1020, l.lastVisitTick());
    }

    @Test
    void tc_t5_26_observationDayTakesLowerRate() {
        ProducerLedger l = new ProducerLedger(new ProducerId("p"), Fixtures.OWNER, Civilization.GREEK, 60, 0);
        assertFalse(l.verified());
        for (int t = 2000; t <= 24000; t += 2000) {
            SettlementEngine.settle(l, t, Conditions.real(40.0 / 12, false), Fixtures.e());
        }
        assertTrue(l.verified());
        assertEquals(40, l.snapshotRatePerDay(), 1e-9);
    }

    @Test
    void tc_t5_26b_observationNeverRaisesAboveEstimate() {
        ProducerLedger l = new ProducerLedger(new ProducerId("p"), Fixtures.OWNER, Civilization.GREEK, 60, 0);
        SettlementEngine.settle(l, 24000, Conditions.real(80, false), Fixtures.e());
        assertTrue(l.verified());
        assertEquals(60, l.snapshotRatePerDay(), 1e-9);
    }

    @Test
    void tc_t5_26c_interruptedStreakDoesNotVerify() {
        ProducerLedger l = new ProducerLedger(new ProducerId("p"), Fixtures.OWNER, Civilization.GREEK, 60, 0);
        SettlementEngine.settle(l, 20000, Conditions.real(30, false), Fixtures.e());
        SettlementEngine.settle(l, 20020, ACTIVE_AWAY, Fixtures.e());
        SettlementEngine.settle(l, 40000, Conditions.real(30, false), Fixtures.e());
        assertFalse(l.verified());
    }

    @Test
    void tc_t5_27_clockGoingBackwardsIsRejected() {
        ProducerLedger l = verifiedLedger("p");
        l.setLastSettledTick(5000);
        Settlement s = SettlementEngine.settle(l, 4000, ACTIVE_AWAY, Fixtures.e());
        assertEquals(0, s.credited());
        assertFalse(s.warning().isEmpty());
        assertEquals(5000, l.lastSettledTick());
    }

    @Test
    void rateFactorScalesSnapshot() {
        ProducerLedger l = verifiedLedger("p");
        assertEquals(24, SettlementEngine.settle(l, 24000, ACTIVE_AWAY, Fixtures.withRateFactor(Fixtures.e(), 0.5))
                .credited());
    }

    @Test
    void estimateUpdateNeverRaisesVerifiedSnapshot() {
        ProducerLedger l = verifiedLedger("p");
        l.recordObservation(30);
        l.setEstimatedRatePerDay(100);
        assertEquals(30, l.snapshotRatePerDay(), 1e-9);
        l.setEstimatedRatePerDay(20);
        assertEquals(20, l.snapshotRatePerDay(), 1e-9);
    }
}
