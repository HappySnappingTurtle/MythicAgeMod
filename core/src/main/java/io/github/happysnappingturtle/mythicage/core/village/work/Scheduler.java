package io.github.happysnappingturtle.mythicage.core.village.work;

import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * 村庄调度器（T19 设计文档 §5.3，V1 §3）：纯函数。输入一份快照，输出「哪个居民做哪份工作单」。
 * <ol>
 *   <li>警戒：所有居民只分到回屋（{@code go_home}，EMERGENCY）；</li>
 *   <li>候选工作单经行为模块改写或删除；雨天删掉户外工作；缺粮时不排新施工与装饰活动；
 *       今天在同一目标上失败满 {@link FailureBook#LIMIT} 次的不排；已有人在做的目标不排；</li>
 *   <li>按优先级、权重、id 排序，逐个分给「空闲、职业匹配、此刻可用、离目标最近」的居民；</li>
 *   <li>同时在走路的居民不超过 {@code maxWalking}（V1 §11：每村同时寻路 ≤ 4）。</li>
 * </ol>
 */
public final class Scheduler {
    public static final String GO_HOME = "go_home";
    public static final int DEFAULT_MAX_WALKING = 4;

    /** 居民在调度器眼里的样子。 */
    public record ResidentView(UUID id, String profession, Pos pos, Pos home, boolean idle, boolean walking) {
    }

    public record Assignment(UUID resident, Job job) {
    }

    /**
     * @param busyTargets 正在执行的工作单占着的目标键
     * @param available   行为模块的 availability：居民此刻能否被派单（为 null 时都可以）
     * @param modify      行为模块的 modify：改写或删除（返回 empty）一份工作单（为 null 时不改）
     */
    public record Snapshot(List<ResidentView> residents, List<Job> candidates, boolean alarm, boolean raining,
                           boolean foodShort, int maxWalking, Set<String> busyTargets, FailureBook failures, long day,
                           BiPredicate<ResidentView, Job> available, Function<Job, Optional<Job>> modify) {
        public Snapshot {
            residents = List.copyOf(residents);
            candidates = List.copyOf(candidates);
            busyTargets = Set.copyOf(busyTargets);
        }

        public static Snapshot of(List<ResidentView> residents, List<Job> candidates) {
            return new Snapshot(residents, candidates, false, false, false, DEFAULT_MAX_WALKING, Set.of(),
                    new FailureBook(), 0, null, null);
        }

        public Snapshot withAlarm(boolean a) {
            return new Snapshot(residents, candidates, a, raining, foodShort, maxWalking, busyTargets, failures, day,
                    available, modify);
        }

        public Snapshot withRaining(boolean r) {
            return new Snapshot(residents, candidates, alarm, r, foodShort, maxWalking, busyTargets, failures, day,
                    available, modify);
        }

        public Snapshot withFoodShort(boolean f) {
            return new Snapshot(residents, candidates, alarm, raining, f, maxWalking, busyTargets, failures, day,
                    available, modify);
        }

        public Snapshot withFailures(FailureBook f, long d) {
            return new Snapshot(residents, candidates, alarm, raining, foodShort, maxWalking, busyTargets, f, d,
                    available, modify);
        }

        public Snapshot withBehavior(BiPredicate<ResidentView, Job> avail, Function<Job, Optional<Job>> mod) {
            return new Snapshot(residents, candidates, alarm, raining, foodShort, maxWalking, busyTargets, failures, day,
                    avail, mod);
        }

        public Snapshot withBusy(Set<String> busy) {
            return new Snapshot(residents, candidates, alarm, raining, foodShort, maxWalking, busy, failures, day,
                    available, modify);
        }
    }

    private Scheduler() {
    }

    public static List<Assignment> plan(Snapshot s) {
        List<Assignment> out = new ArrayList<>();
        if (s.alarm()) {
            long id = -1;
            for (ResidentView r : s.residents()) {
                out.add(new Assignment(r.id(), new Job(id--, GO_HOME, r.home(), "home:" + r.id(), "",
                        List.of(JobStep.move()), java.util.Map.of(), java.util.Map.of(), Set.of(), Priority.EMERGENCY,
                        0, false, Job.DEFAULT_TIMEOUT)));
            }
            return out;
        }
        List<Job> jobs = new ArrayList<>();
        for (Job j : s.candidates()) {
            Optional<Job> m = s.modify() == null ? Optional.of(j) : s.modify().apply(j);
            if (m.isEmpty()) {
                continue;
            }
            Job k = m.get();
            if (s.raining() && k.outdoor()) {
                continue;
            }
            if (s.foodShort() && (k.priority() == Priority.NEW_BUILD || k.priority() == Priority.DECOR)) {
                continue;
            }
            if (s.failures() != null && s.failures().blocked(k.targetKey(), s.day())) {
                continue;
            }
            if (s.busyTargets().contains(k.targetKey())) {
                continue;
            }
            jobs.add(k);
        }
        jobs.sort(Comparator.comparing(Job::priority).thenComparing(Comparator.comparingDouble(Job::weight).reversed())
                .thenComparingLong(Job::id));
        Set<UUID> taken = new HashSet<>();
        Set<String> targets = new HashSet<>();
        int walking = (int) s.residents().stream().filter(ResidentView::walking).count();
        for (Job j : jobs) {
            if (!targets.add(j.targetKey())) {
                continue;
            }
            if (j.needsWalk() && walking >= s.maxWalking()) {
                targets.remove(j.targetKey());
                continue;
            }
            ResidentView best = null;
            long bestD = Long.MAX_VALUE;
            for (ResidentView r : s.residents()) {
                if (!r.idle() || taken.contains(r.id()) || !j.allows(r.id(), r.profession())) {
                    continue;
                }
                if (s.available() != null && !s.available().test(r, j)) {
                    continue;
                }
                long d = dist(r.pos(), j.target());
                if (d < bestD) {
                    bestD = d;
                    best = r;
                }
            }
            if (best == null) {
                targets.remove(j.targetKey());
                continue;
            }
            taken.add(best.id());
            if (j.needsWalk()) {
                walking++;
            }
            out.add(new Assignment(best.id(), j));
        }
        return out;
    }

    private static long dist(Pos a, Pos b) {
        if (a == null || b == null) {
            return Long.MAX_VALUE / 2;
        }
        long dx = a.x() - b.x();
        long dy = a.y() - b.y();
        long dz = a.z() - b.z();
        return dx * dx + dy * dy + dz * dz;
    }
}
