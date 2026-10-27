package io.github.happysnappingturtle.mythicage.core.village.quest;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 贡献来源记录（TC-T24-07～10）。 */
class ContributionLogTest {
    static final UUID A = new UUID(0, 1);
    static final UUID B = new UUID(0, 2);

    @Test
    void tc07KeepsNewestEight() {
        ContributionLog log = new ContributionLog();
        for (int i = 1; i <= 10; i++) {
            log.add(A, new ContributionLog.Event("supply", i, i));
        }
        List<ContributionLog.Event> r = log.recent(A, 99);
        assertEquals(ContributionLog.LIMIT, r.size());
        assertEquals(10, r.get(0).points(), "新的在前");
        assertEquals(3, r.get(7).points());
    }

    @Test
    void tc08PlayersSeparate() {
        ContributionLog log = new ContributionLog();
        log.add(A, new ContributionLog.Event("supply", 3, 1));
        log.add(B, new ContributionLog.Event("escort", 10, 2));
        assertEquals("supply", log.recent(A, 8).get(0).reason());
        assertEquals("escort", log.recent(B, 8).get(0).reason());
        assertEquals(1, log.recent(A, 8).size());
    }

    @Test
    void tc09RecentBounds() {
        ContributionLog log = new ContributionLog();
        log.add(A, new ContributionLog.Event("donate", 2, 1));
        assertEquals(1, log.recent(A, 5).size());
        assertTrue(log.recent(A, 0).isEmpty());
        assertTrue(log.recent(B, 3).isEmpty());
    }

    @Test
    void tc10ZeroPointsIgnored() {
        ContributionLog log = new ContributionLog();
        log.add(A, new ContributionLog.Event("escort", 0, 1));
        assertTrue(log.recent(A, 8).isEmpty());
        assertTrue(log.all().isEmpty());
    }

    @Test
    void restoreRoundTrip() {
        ContributionLog log = new ContributionLog();
        log.add(A, new ContributionLog.Event("supply", 1, 1));
        log.add(A, new ContributionLog.Event("repair", 2, 2));
        ContributionLog back = new ContributionLog();
        back.restore(A, log.all().get(A));
        assertEquals(log.recent(A, 8), back.recent(A, 8));
    }
}
