package io.github.happysnappingturtle.mythicage.core.village.work;

import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T19 单元测试 TC-T19-07～14：调度器。 */
class SchedulerTest {
    private static final String POTTER = "greek:potter", LABORER = "greek:laborer";

    private static Scheduler.ResidentView r(String prof, int x) {
        return new Scheduler.ResidentView(UUID.randomUUID(), prof, new Pos(x, 0, 0), new Pos(0, 0, 0), true, false);
    }

    private static Job job(long id, String type, Priority p, Set<String> prof, int x, boolean outdoor) {
        return new Job(id, type, new Pos(x, 0, 0), type + ":" + id, "", Job.walkActSettle("a", 20), Map.of(), Map.of(),
                prof, p, 1, outdoor, Job.DEFAULT_TIMEOUT);
    }

    @Test
    void tc07_alarmOnlyGoHome() {
        List<Scheduler.ResidentView> rs = List.of(r(POTTER, 0), r(LABORER, 3));
        var out = Scheduler.plan(Scheduler.Snapshot.of(rs, List.of(job(1, "craft", Priority.SHORTAGE, Set.of(), 0, false)))
                .withAlarm(true));
        assertEquals(2, out.size());
        assertTrue(out.stream().allMatch(a -> a.job().type().equals(Scheduler.GO_HOME)
                && a.job().priority() == Priority.EMERGENCY));
    }

    @Test
    void tc08_foodShortOrder() {
        List<Scheduler.ResidentView> rs = List.of(r(LABORER, 0), r(LABORER, 0), r(LABORER, 0));
        List<Job> js = List.of(job(1, "leisure", Priority.DECOR, Set.of(), 0, false),
                job(2, "build", Priority.ACTIVE_BUILD, Set.of(), 0, false),
                job(3, "harvest_crop", Priority.FOOD_SAFETY, Set.of(), 0, false));
        var out = Scheduler.plan(Scheduler.Snapshot.of(rs, js).withFoodShort(true));
        assertEquals(List.of(3L, 2L), out.stream().map(a -> a.job().id()).toList());
    }

    @Test
    void tc09_professionMatch() {
        Scheduler.ResidentView potter = r(POTTER, 10);
        Scheduler.ResidentView lab = r(LABORER, 0);
        var out = Scheduler.plan(Scheduler.Snapshot.of(List.of(lab, potter),
                List.of(job(1, "craft", Priority.SHORTAGE, Set.of(POTTER), 0, false))));
        assertEquals(1, out.size());
        assertEquals(potter.id(), out.get(0).resident());
    }

    @Test
    void tc10_nearestWins() {
        Scheduler.ResidentView far = r(LABORER, 12);
        Scheduler.ResidentView near = r(LABORER, 5);
        var out = Scheduler.plan(Scheduler.Snapshot.of(List.of(far, near),
                List.of(job(1, "haul", Priority.SHORTAGE, Set.of(), 0, false))));
        assertEquals(near.id(), out.get(0).resident());
    }

    @Test
    void tc11_walkingCap() {
        List<Scheduler.ResidentView> rs = new ArrayList<>();
        List<Job> js = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            rs.add(r(LABORER, i));
            js.add(job(i + 1, "gather_point", Priority.SHORTAGE, Set.of(), i, true));
        }
        assertEquals(4, Scheduler.plan(Scheduler.Snapshot.of(rs, js)).size());
    }

    @Test
    void tc12_availabilityRespected() {
        Scheduler.ResidentView a = r(LABORER, 0);
        Scheduler.ResidentView b = r(LABORER, 9);
        var out = Scheduler.plan(Scheduler.Snapshot.of(List.of(a, b),
                        List.of(job(1, "haul", Priority.SHORTAGE, Set.of(), 0, false)))
                .withBehavior((res, j) -> !res.id().equals(a.id()), null));
        assertEquals(b.id(), out.get(0).resident());
    }

    @Test
    void tc13_rainDropsOutdoor() {
        List<Scheduler.ResidentView> rs = List.of(r(LABORER, 0), r(POTTER, 0));
        List<Job> js = List.of(job(1, "harvest_crop", Priority.SHORTAGE, Set.of(), 0, true),
                job(2, "craft", Priority.SHORTAGE, Set.of(POTTER), 0, false));
        var out = Scheduler.plan(Scheduler.Snapshot.of(rs, js).withRaining(true));
        assertEquals(List.of(2L), out.stream().map(a -> a.job().id()).toList());
        // 行为模块也可以删
        var out2 = Scheduler.plan(Scheduler.Snapshot.of(rs, js)
                .withBehavior(null, j -> j.outdoor() ? Optional.empty() : Optional.of(j)));
        assertEquals(List.of(2L), out2.stream().map(a -> a.job().id()).toList());
    }

    @Test
    void tc14_failureBookBlocksForTheDay() {
        FailureBook fb = new FailureBook();
        Job j = job(1, "gather_point", Priority.SHORTAGE, Set.of(), 0, false);
        for (int i = 0; i < FailureBook.LIMIT; i++) {
            fb.fail(j.targetKey(), 7);
        }
        List<Scheduler.ResidentView> rs = List.of(r(LABORER, 0));
        assertEquals(0, Scheduler.plan(Scheduler.Snapshot.of(rs, List.of(j)).withFailures(fb, 7)).size());
        assertEquals(1, Scheduler.plan(Scheduler.Snapshot.of(rs, List.of(j)).withFailures(fb, 8)).size());
    }

    @Test
    void busyTargetsNotDoubled() {
        Job j = job(1, "gather_point", Priority.SHORTAGE, Set.of(), 0, false);
        List<Scheduler.ResidentView> rs = List.of(r(LABORER, 0), r(LABORER, 1));
        assertEquals(0, Scheduler.plan(Scheduler.Snapshot.of(rs, List.of(j)).withBusy(Set.of(j.targetKey()))).size());
        assertEquals(1, Scheduler.plan(Scheduler.Snapshot.of(rs, List.of(j, j.withId(2)))).size(), "同一目标只派一人");
    }
}
