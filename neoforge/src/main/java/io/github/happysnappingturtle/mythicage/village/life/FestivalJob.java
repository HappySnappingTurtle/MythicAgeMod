package io.github.happysnappingturtle.mythicage.village.life;

import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.core.village.life.FestivalRules;
import io.github.happysnappingturtle.mythicage.core.village.life.LifeData;
import io.github.happysnappingturtle.mythicage.core.village.work.Job;
import io.github.happysnappingturtle.mythicage.core.village.work.JobStep;
import io.github.happysnappingturtle.mythicage.core.village.work.Priority;
import io.github.happysnappingturtle.mythicage.guardian.GuardianEntity;
import io.github.happysnappingturtle.mythicage.guardian.GuardianGesture;
import io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import io.github.happysnappingturtle.mythicage.village.work.JobType;
import io.github.happysnappingturtle.mythicage.village.work.VillageContext;
import io.github.happysnappingturtle.mythicage.village.work.VillageScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.StairBlock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 节庆（T19 设计文档 §1.10 J5，V3-希腊-居民 §5.5）：按数据里的段派工作单。
 * <ul>
 *   <li>{@code relay} 火炬接力（前夜）：路线均分成几程，后一棒站在交接点等前一棒跑到才出发；最后一棒到达点燃祭坛；</li>
 *   <li>{@code procession} 游行：提前 {@link VillageLife#LEAD} 刻在广场排成两列纵队，按排每 {@value #ROW_GAP} 刻放行；
 *       走到祭坛前围成半圈（{@code altar}）或绕村一圈回广场（{@code loop}）；领头者到达祭坛时祭司奠酒、消耗祭品；</li>
 *   <li>{@code games} 赛跑：一轮几人分道起跑，其余人在终点旁欢呼；{@code theater}：演员在歌队场、观众坐看台；
 *       {@code gather}：广场内圈跳舞、外圈坐着吃（宴饮按人数消耗葡萄酒）。</li>
 * </ul>
 * 引用 {@code ref} = 节日|种类|序号|附加；目标键是每人一份。派单时记为已派（不补派——纯表现）。
 */
final class FestivalJob implements JobType {
    static final int PER_ROW = 2, ROW_GAP = 20;
    private static final Map<BlockPos, List<BlockPos>> SEATS = new HashMap<>();
    private static final Map<BlockPos, Long> SEATS_DAY = new HashMap<>();

    @Override
    public String id() {
        return VillageLife.FESTIVAL;
    }

    static int until(int tod, int t) {
        return Math.floorMod(t - tod, 24000);
    }

    @Override
    public List<Job> candidates(VillageContext ctx) {
        Optional<FestivalRules.Active> a = VillageLife.festivalSoon(ctx, VillageLife.LEAD);
        if (a.isEmpty()) {
            return List.of();
        }
        FestivalRules.Active act = a.get();
        LifeData.Phase p = act.phase();
        boolean started = VillageLife.festivalNow(ctx).filter(n -> n.phase().equals(p)).isPresent();
        List<ResidentEntity> free = VillageLife.idleMembers(ctx);
        String fid = act.festival().id();
        return switch (p.kind()) {
            case "relay" -> relay(ctx, fid, p, free);
            case "procession" -> procession(ctx, fid, p, free);
            case "games" -> started ? games(ctx, fid, p, free) : List.of();
            case "theater" -> started ? theater(ctx, fid, p, free) : List.of();
            case "gather" -> started ? gather(ctx, fid, p, p.at(), p.acts(), free) : List.of();
            default -> List.of();
        };
    }

    private Job job(VillageContext ctx, ResidentEntity r, Pos target, String key, String ref, List<JobStep> steps, double weight,
                    int timeout) {
        return new Job(ctx.nextJobId(), id(), target, key, ref, steps, Map.of(), Map.of(), Set.of(), Priority.ROUTINE, weight,
                false, timeout, r.getUUID());
    }

    private static Comparator<ResidentEntity> nearest(BlockPos p) {
        return Comparator.comparingDouble(r -> r.distanceToSqr(p.getX() + 0.5, p.getY(), p.getZ() + 0.5));
    }

    // ───────────── 火炬接力 ─────────────

    /** 接力的交接点：起点到终点均分 {@code n} 程。 */
    static List<BlockPos> legs(VillageContext ctx, LifeData.Phase p) {
        if (p.track().size() < 2) {
            return List.of();
        }
        Optional<BlockPos> s = VillageLife.point(ctx, p.track().get(0));
        Optional<BlockPos> e = VillageLife.point(ctx, p.track().get(1));
        if (s.isEmpty() || e.isEmpty()) {
            return List.of();
        }
        int n = Math.max(1, p.count());
        List<BlockPos> out = new ArrayList<>();
        for (int k = 0; k <= n; k++) {
            double t = (double) k / n;
            BlockPos a = s.get();
            BlockPos b = e.get();
            out.add(VillageLife.ground(ctx.level(), (int) Math.round(a.getX() + (b.getX() - a.getX()) * t),
                    (int) Math.round(a.getZ() + (b.getZ() - a.getZ()) * t), (int) Math.round(a.getY() + (b.getY() - a.getY()) * t)));
        }
        return out;
    }

    private List<Job> relay(VillageContext ctx, String fid, LifeData.Phase p, List<ResidentEntity> free) {
        List<BlockPos> legs = legs(ctx, p);
        if (legs.isEmpty()) {
            return List.of();
        }
        int n = legs.size() - 1;
        int left = until(ctx.timeOfDay(), p.to());
        List<Job> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            String key = "fest:relay:" + fid + ":" + i;
            if (ctx.rt().done(key, ctx.today()) || free.isEmpty()) {
                continue;
            }
            BlockPos from = legs.get(i);
            ResidentEntity r = free.stream().min(Comparator.<ResidentEntity>comparingInt(x -> x.female() ? 1 : 0)
                    .thenComparing(nearest(from))).orElseThrow();
            free.remove(r);
            List<JobStep> steps = List.of(JobStep.moveTo(VillageLife.pos(from), ""), JobStep.act("idle", left + VillageLife.LEAD),
                    JobStep.runTo(VillageLife.pos(legs.get(i + 1)), p.leader()), JobStep.act("cheer", 40), JobStep.settle());
            out.add(job(ctx, r, VillageLife.pos(from), key, fid + "|relay|" + i + "|" + n, steps, 10, left + VillageLife.LEAD + 2400));
        }
        return out;
    }

    // ───────────── 游行 ─────────────

    /** 广场上的纵队位置：第 0 排在集合点，往远离终点的方向排，每排 2 人。 */
    static BlockPos slot(ServerLevel level, BlockPos gather, double dx, double dz, int idx) {
        int row = idx / PER_ROW;
        double side = idx % PER_ROW == 0 ? -0.9 : 0.9;
        double x = gather.getX() + 0.5 - dx * row * 1.6 - dz * side;
        double z = gather.getZ() + 0.5 - dz * row * 1.6 + dx * side;
        return VillageLife.ground(level, (int) Math.floor(x), (int) Math.floor(z), gather.getY());
    }

    /** 祭坛前的半圈：领头者在正前方 2 格，其余人每圈 7 个、向外扩。 */
    static BlockPos altarSlot(ServerLevel level, BlockPos altar, double ux, double uz, int idx) {
        double base = Math.atan2(uz, ux);
        if (idx == 0) {
            return VillageLife.ring(level, altar, 2, base);
        }
        int k = idx - 1;
        return VillageLife.ring(level, altar, 3 + (k / 7) * 1.3, base + ((k % 7) - 3) * 0.35);
    }

    /** 绕村路线：四个方向上各取一处最远的院门（没有就是住居），按方位排好，最后回到集合点。 */
    static List<BlockPos> loop(VillageContext ctx, BlockPos gather) {
        List<Pos> pts = new ArrayList<>(ctx.anchors("yard_gate"));
        if (pts.isEmpty()) {
            pts.addAll(ctx.anchors("home"));
        }
        BlockPos[] best = new BlockPos[4];
        double[] bestD = new double[4];
        for (Pos q : pts) {
            double dx = q.x() - gather.getX();
            double dz = q.z() - gather.getZ();
            double d = dx * dx + dz * dz;
            if (d > 48 * 48 || d < 36) {
                continue;
            }
            int quad = (int) Math.floorMod((long) Math.floor((Math.atan2(dz, dx) + Math.PI) / (Math.PI / 2)), 4L);
            if (d > bestD[quad]) {
                bestD[quad] = d;
                best[quad] = VillageContext.block(q);
            }
        }
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos b : best) {
            if (b != null) {
                out.add(b);
            }
        }
        return out;
    }

    private List<Job> procession(VillageContext ctx, String fid, LifeData.Phase p, List<ResidentEntity> free) {
        int tod = ctx.timeOfDay();
        if (p.contains(tod) && until(tod, p.to()) < 600) {
            return List.of();
        }
        ServerLevel level = ctx.level();
        BlockPos gather = VillageLife.point(ctx, p.at()).orElse(ctx.center());
        boolean loop = p.route().equals("loop");
        List<BlockPos> route = loop ? loop(ctx, gather) : List.of();
        BlockPos end = loop ? (route.isEmpty() ? gather : route.get(0)) : VillageLife.altar(ctx);
        double dx = end.getX() - gather.getX();
        double dz = end.getZ() - gather.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1) {
            dx = 0;
            dz = 1;
        } else {
            dx /= len;
            dz /= len;
        }
        int base = ctx.state().dailyCount(ctx.today(), "proc:" + fid);
        List<ResidentEntity> order = new ArrayList<>();
        for (ResidentEntity r : free) {
            if (!ctx.rt().done(procKey(fid, r), ctx.today())) {
                order.add(r);
            }
        }
        order.sort(nearest(gather));
        boolean leader = !p.leader().isEmpty();
        if (base == 0 && leader) {
            order.stream().min(Comparator.<ResidentEntity>comparingInt(r -> r.female() ? 0 : 1).thenComparing(nearest(gather)))
                    .ifPresent(l -> {
                        order.remove(l);
                        order.add(0, l);
                    });
        }
        int wait = until(tod, p.to()) + VillageLife.LEAD;
        List<Job> out = new ArrayList<>();
        for (int k = 0; k < order.size(); k++) {
            ResidentEntity r = order.get(k);
            int idx = base + k;
            int release = p.from() + FestivalRules.release(idx + 1, PER_ROW, ROW_GAP).get(idx);
            String carry = leader && idx == 0 ? p.leader() : carryAt(p, leader ? idx - 1 : idx);
            BlockPos at = slot(level, gather, dx, dz, idx);
            List<JobStep> steps = new ArrayList<>();
            steps.add(JobStep.moveTo(VillageLife.pos(at), ""));
            steps.add(JobStep.act("idle", wait));
            if (loop) {
                for (BlockPos w : route) {
                    steps.add(JobStep.moveTo(VillageLife.pos(VillageLife.ground(level, w)), carry));
                }
                steps.add(JobStep.moveTo(VillageLife.pos(VillageLife.ring(level, gather, 3 + (idx / 10) * 1.5, idx * 0.6)), carry));
                steps.add(JobStep.act("cheer", 300));
            } else {
                steps.add(JobStep.moveTo(VillageLife.pos(altarSlot(level, end, -dx, -dz, idx)), carry));
                steps.add(JobStep.act("pray", 300));
            }
            steps.add(JobStep.settle());
            out.add(job(ctx, r, VillageLife.pos(at), procKey(fid, r), fid + "|procession|" + idx + "|" + release, steps,
                    idx == 0 ? 20 : 10, wait + 4800));
        }
        return out;
    }

    static String procKey(String fid, ResidentEntity r) {
        return "fest:proc:" + fid + ":" + r.getUUID();
    }

    static String carryAt(LifeData.Phase p, int i) {
        return i >= 0 && i < p.carries().size() ? p.carries().get(i) : "";
    }

    // ───────────── 竞技、剧场、广场 ─────────────

    private List<Job> games(VillageContext ctx, String fid, LifeData.Phase p, List<ResidentEntity> free) {
        List<BlockPos> track = trackPoints(ctx, p.track());
        if (track.isEmpty()) {
            track = trackPoints(ctx, p.fallback());
        }
        if (track.isEmpty()) {
            return List.of();
        }
        ServerLevel level = ctx.level();
        BlockPos start = track.get(0);
        BlockPos finish = track.get(1);
        double dx = finish.getX() - start.getX();
        double dz = finish.getZ() - start.getZ();
        double len = Math.max(1, Math.sqrt(dx * dx + dz * dz));
        dx /= len;
        dz /= len;
        int left = until(ctx.timeOfDay(), p.to());
        List<Job> out = new ArrayList<>();
        boolean heatOn = ctx.rt().activeJobs().stream().anyMatch(j -> j.type().equals(id()) && j.ref().contains("|run|"));
        if (!heatOn && left > 900 && free.size() >= 2) {
            int n = Math.min(Math.max(2, p.count()), free.size());
            List<ResidentEntity> runners = free.stream().sorted(Comparator.<ResidentEntity>comparingInt(r -> r.female() ? 1 : 0)
                    .thenComparing(nearest(start))).limit(n).toList();
            long startAt = level.getGameTime() + 400;
            for (int j = 0; j < runners.size(); j++) {
                ResidentEntity r = runners.get(j);
                double lane = (j - (runners.size() - 1) / 2.0) * 1.2;
                BlockPos a = VillageLife.ground(level, (int) Math.floor(start.getX() + 0.5 - dz * lane),
                        (int) Math.floor(start.getZ() + 0.5 + dx * lane), start.getY());
                BlockPos b = VillageLife.ground(level, (int) Math.floor(finish.getX() + 0.5 - dz * lane),
                        (int) Math.floor(finish.getZ() + 0.5 + dx * lane), finish.getY());
                List<JobStep> steps = List.of(JobStep.moveTo(VillageLife.pos(a), ""), JobStep.act("idle", 800),
                        JobStep.runTo(VillageLife.pos(b), ""), JobStep.act("cheer", 60), JobStep.settle());
                out.add(job(ctx, r, VillageLife.pos(a), "fest:run:" + startAt + ":" + r.getUUID(), fid + "|run|" + j + "|" + startAt,
                        steps, 12, 3000));
                free.remove(r);
            }
        }
        String act = p.acts().isEmpty() ? "cheer" : p.acts().get(0);
        int slot = ctx.timeOfDay() / 1000;
        double side = Math.atan2(dx, -dz);
        int k = 0;
        for (ResidentEntity r : free) {
            String key = "fest:watch:" + fid + ":" + slot + ":" + r.getUUID();
            if (k >= 16 || ctx.rt().done(key, ctx.today())) {
                continue;
            }
            double ang = side + (k % 2 == 0 ? 0 : Math.PI) + ((k / 2) % 5 - 2) * 0.3;
            BlockPos at = VillageLife.ring(level, finish, 3.5 + (k / 10) * 1.2, ang);
            out.add(job(ctx, r, VillageLife.pos(at), key, fid + "|watch|" + k, List.of(JobStep.moveTo(VillageLife.pos(at), ""),
                    JobStep.act(act, Math.min(left, 600)), JobStep.settle()), 5, 1800));
            k++;
        }
        return out;
    }

    static List<BlockPos> trackPoints(VillageContext ctx, List<String> names) {
        if (names.size() < 2) {
            return List.of();
        }
        List<BlockPos> out = new ArrayList<>();
        for (String n : names.subList(0, 2)) {
            Optional<BlockPos> b = n.equals("center") ? VillageLife.point(ctx, n)
                    : ctx.anchors(n).stream().findFirst().map(VillageContext::block);
            if (b.isEmpty()) {
                return List.of();
            }
            out.add(b.get());
        }
        return out;
    }

    /** 看台座位：歌队场周围 4.5–12.5 格内、上面能站人的楼梯方块（人站在楼梯上就像坐着）。每天扫一次。 */
    static List<BlockPos> seats(ServerLevel level, BlockPos orch, long day) {
        Long d = SEATS_DAY.get(orch);
        if (d != null && d == day) {
            return SEATS.get(orch);
        }
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos q : BlockPos.betweenClosed(orch.offset(-12, -1, -12), orch.offset(12, 8, 12))) {
            double h = Math.hypot(q.getX() - orch.getX(), q.getZ() - orch.getZ());
            if (h < 4.5 || h > 12.5 || !level.isLoaded(q) || !(level.getBlockState(q).getBlock() instanceof StairBlock)) {
                continue;
            }
            BlockPos up = q.above();
            if (VillageLife.standable(level, up)) {
                out.add(up.immutable());
            }
        }
        out.sort(Comparator.comparingDouble(q -> q.distSqr(orch)));
        SEATS.put(orch, out);
        SEATS_DAY.put(orch, day);
        return out;
    }

    private List<Job> theater(VillageContext ctx, String fid, LifeData.Phase p, List<ResidentEntity> free) {
        Optional<BlockPos> orch = ctx.anchors(p.at()).stream().findFirst().map(VillageContext::block);
        if (orch.isEmpty()) {
            // 没有剧场：改为广场跳舞（V3 §5.5）
            return gather(ctx, fid, p, "center", List.of("dance"), free);
        }
        List<BlockPos> seats = seats(ctx.level(), orch.get(), ctx.today());
        String prefix = "fest:theater:" + fid + ":";
        int base = ctx.rt().countDone(prefix, ctx.today());
        int left = Math.max(100, until(ctx.timeOfDay(), p.to()));
        String watch = p.acts().isEmpty() ? "sit_watch" : p.acts().get(0);
        String perform = p.acts().size() < 2 ? "perform" : p.acts().get(1);
        int actors = Math.max(0, p.count());
        List<Job> out = new ArrayList<>();
        List<ResidentEntity> order = new ArrayList<>(free);
        order.sort(nearest(orch.get()));
        int k = 0;
        for (ResidentEntity r : order) {
            if (ctx.rt().done(prefix + r.getUUID(), ctx.today())) {
                continue;
            }
            int idx = base + k;
            BlockPos at;
            String anim;
            if (idx < actors) {
                at = VillageLife.ring(ctx.level(), orch.get(), 2, idx * 2 * Math.PI / Math.max(1, actors));
                anim = perform;
            } else if (idx - actors < seats.size()) {
                at = seats.get(idx - actors);
                anim = watch;
            } else {
                break;
            }
            out.add(job(ctx, r, VillageLife.pos(at), prefix + r.getUUID(), fid + "|theater|" + idx,
                    List.of(JobStep.moveTo(VillageLife.pos(at), ""), JobStep.act(anim, left), JobStep.settle()), 8, left + 2400));
            k++;
        }
        return out;
    }

    private List<Job> gather(VillageContext ctx, String fid, LifeData.Phase p, String atName, List<String> acts,
                             List<ResidentEntity> free) {
        BlockPos center = VillageLife.point(ctx, atName).orElse(ctx.center());
        String prefix = "fest:gather:" + fid + ":";
        int base = ctx.rt().countDone(prefix, ctx.today());
        int left = Math.max(100, until(ctx.timeOfDay(), p.to()));
        List<String> a = acts.isEmpty() ? List.of("dance") : acts;
        List<Job> out = new ArrayList<>();
        List<ResidentEntity> order = new ArrayList<>(free);
        order.sort(nearest(center));
        int k = 0;
        for (ResidentEntity r : order) {
            if (ctx.rt().done(prefix + r.getUUID(), ctx.today())) {
                continue;
            }
            int idx = base + k;
            boolean outer = a.size() >= 2 && idx % 3 == 2;
            BlockPos at = outer ? VillageLife.ring(ctx.level(), center, 6, idx * 0.35)
                    : VillageLife.ring(ctx.level(), center, 3 + (idx / 18) * 1.2, idx * (Math.PI / 6));
            out.add(job(ctx, r, VillageLife.pos(at), prefix + r.getUUID(), fid + "|gather|" + idx,
                    List.of(JobStep.moveTo(VillageLife.pos(at), ""), JobStep.act(outer ? a.get(1) : a.get(0), left), JobStep.settle()),
                    8, left + 2400));
            k++;
        }
        return out;
    }

    // ───────────── 派单、动作中、结算 ─────────────

    /** 派出去就记为已派；游行记下用到的最大序号（下一轮从后面接着排）。 */
    @Override
    public Optional<Map<String, Integer>> escrow(VillageContext ctx, Job job) {
        ctx.rt().markDone(job.targetKey(), ctx.today());
        String[] ref = job.ref().split("\\|");
        if (ref[1].equals("procession")) {
            String key = "proc:" + ref[0];
            int idx = Integer.parseInt(ref[2]);
            int cur = ctx.state().dailyCount(ctx.today(), key);
            if (idx + 1 > cur) {
                ctx.state().addDaily(ctx.today(), key, idx + 1 - cur);
            }
        }
        return Optional.of(Map.of());
    }

    @Override
    public void onActTick(ServerLevel level, ResidentEntity r, Job job, int step, int ticks) {
        String[] ref = job.ref().split("\\|");
        String kind = ref[1];
        boolean waiting = step == 1 && (kind.equals("relay") || kind.equals("procession") || kind.equals("run"));
        if (waiting) {
            if (ticks % 5 != 0) {
                return;
            }
            if (kind.equals("run")) {
                if (level.getGameTime() >= Long.parseLong(ref[3])) {
                    r.finishAct();
                }
                return;
            }
            Optional<VillageContext> co = r.village().flatMap(v -> VillageScheduler.contextOf(level, v));
            if (co.isEmpty()) {
                return;
            }
            VillageContext ctx = co.get();
            Optional<FestivalRules.Active> now = VillageLife.festivalNow(ctx)
                    .filter(a -> a.festival().id().equals(ref[0]) && a.phase().kind().equals(kind));
            if (now.isEmpty()) {
                return;
            }
            if (kind.equals("relay") ? ctx.state().dailyCount(ctx.today(), "relay:" + ref[0]) >= Integer.parseInt(ref[2])
                    : ctx.timeOfDay() >= Integer.parseInt(ref[3])) {
                r.finishAct();
            }
            return;
        }
        if (ticks != 1) {
            return;
        }
        boolean lastAct = step == job.steps().size() - 2;
        if (kind.equals("relay") && lastAct) {
            r.village().flatMap(v -> VillageScheduler.contextOf(level, v)).ifPresent(ctx -> {
                ctx.state().addDaily(ctx.today(), "relay:" + ref[0], 1);
                if (Integer.parseInt(ref[2]) == Integer.parseInt(ref[3]) - 1) {
                    ctx.state().addDaily(ctx.today(), "lit:" + ref[0], 1);
                    VillageLife.blaze(level, VillageLife.altar(ctx));
                    ctx.state().log(ctx.today(), "mythic_age.village.log.relay_lit", "@mythic_age.festival." + ref[0]);
                }
                VillageEconomy.get(level.getServer()).setDirty();
            });
        } else if (kind.equals("procession") && lastAct && ref[2].equals("0")) {
            r.village().flatMap(v -> VillageScheduler.contextOf(level, v)).ifPresent(ctx -> offering(ctx, ref[0]));
        }
    }

    /** 领头者到达祭坛：祭司奠酒、圣火大亮，消耗村库一份祭品（依次找有的），一个节日一次。 */
    static void offering(VillageContext ctx, String fid) {
        String key = "offer:" + fid;
        if (ctx.state().dailyCount(ctx.today(), key) > 0) {
            return;
        }
        LifeData.Festival f = ctx.life().festivals().get(fid);
        if (f == null) {
            return;
        }
        ctx.state().addDaily(ctx.today(), key, 1);
        List<String> items = f.phases().stream().filter(ph -> ph.kind().equals("procession")).findFirst()
                .map(LifeData.Phase::offering).orElse(List.of());
        Optional<String> item = FestivalRules.offering(items, ctx.state().stock::count);
        item.ifPresent(i -> ctx.state().stock.take(i, 1, 0));
        BlockPos altar = VillageLife.altar(ctx);
        VillageLife.blaze(ctx.level(), altar);
        VillageLife.priest(ctx).ifPresent(g -> g.playGesture(GuardianGesture.LIBATION_POUR, 80));
        ctx.state().log(ctx.today(), item.isPresent() ? "mythic_age.village.log.festival_offering" : "mythic_age.village.log.festival_no_offering",
                "@mythic_age.festival." + fid, item.map(i -> "@" + net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .get(net.minecraft.resources.ResourceLocation.parse(i)).getDescriptionId()).orElse(""));
        VillageEconomy.get(ctx.level().getServer()).setDirty();
    }

    @Override
    public boolean settle(VillageContext ctx, ResidentEntity resident, Job job) {
        return true;
    }

    // ───────────── 每次调度：一次性事件 ─────────────

    static void tick(VillageContext ctx) {
        Optional<LifeData.Festival> today = VillageLife.festivalToday(ctx);
        today.ifPresent(f -> {
            titles(ctx, f);
            if (ctx.timeOfDay() < VillageLife.FESTIVAL_END) {
                BlockPos a = VillageLife.altar(ctx);
                ctx.level().sendParticles(ParticleTypes.FLAME, a.getX() + 0.5, a.getY() + 1.1, a.getZ() + 0.5, 2, 0.15, 0.1, 0.15, 0.005);
            }
        });
        Optional<FestivalRules.Active> now = VillageLife.festivalNow(ctx);
        if (now.isEmpty()) {
            return;
        }
        String fid = now.get().festival().id();
        LifeData.Phase p = now.get().phase();
        long gt = ctx.level().getGameTime();
        int tod = ctx.timeOfDay();
        switch (p.kind()) {
            case "gather" -> feast(ctx, fid, p);
            case "procession" -> {
                if (!p.route().equals("loop") && tod >= p.from() + ROW_GAP) {
                    priestTo(ctx, "priest:proc:" + fid, VillageLife.altar(ctx), gt + until(tod, p.to()));
                }
            }
            case "theater" -> ctx.anchors(p.at()).stream().findFirst().map(VillageContext::block)
                    .ifPresent(o -> priestTo(ctx, "priest:theater:" + fid, o, gt + until(tod, p.to())));
            default -> {
            }
        }
    }

    /** 祭司走到某处奠酒并留到段末（一个节日一次）。 */
    static void priestTo(VillageContext ctx, String key, BlockPos at, long until) {
        if (ctx.state().dailyCount(ctx.today(), key) > 0) {
            return;
        }
        Optional<GuardianEntity> g = VillageLife.priest(ctx);
        if (g.isEmpty()) {
            return;
        }
        if (g.get().errand() != null) {
            g.get().endErrand();
        }
        ctx.state().addDaily(ctx.today(), key, 1);
        g.get().startErrand(new GuardianEntity.Errand(key, List.of(VillageLife.ground(ctx.level(), at)), GuardianGesture.LIBATION_POUR,
                80, until, null));
    }

    /** 宴饮开始时按人数消耗一次（每 {@code per} 人 1 份，不足有多少用多少）。 */
    static void feast(VillageContext ctx, String fid, LifeData.Phase p) {
        String key = "feast:" + fid;
        if (p.consume().isEmpty() || ctx.state().dailyCount(ctx.today(), key) > 0) {
            return;
        }
        ctx.state().addDaily(ctx.today(), key, 1);
        int n = FestivalRules.feast(ctx.state().roster.size(), p.per(), ctx.state().stock.count(p.consume()));
        if (n > 0) {
            ctx.state().stock.take(p.consume(), n, 0);
        }
        ctx.state().log(ctx.today(), "mythic_age.village.log.feast", "@mythic_age.festival." + fid, String.valueOf(n));
        VillageEconomy.get(ctx.level().getServer()).setDirty();
    }

    /** 节日当天在村里（钟 48 格内）的玩家看到一次标题。 */
    static void titles(VillageContext ctx, LifeData.Festival f) {
        BlockPos c = ctx.center();
        for (ServerPlayer p : ctx.level().players()) {
            if (p.isSpectator() || p.distanceToSqr(c.getX() + 0.5, c.getY(), c.getZ() + 0.5) > 48 * 48) {
                continue;
            }
            String key = "title:" + p.getUUID();
            if (ctx.rt().done(key, ctx.today())) {
                continue;
            }
            ctx.rt().markDone(key, ctx.today());
            p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
            p.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("mythic_age.festival." + f.id())));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("mythic_age.festival." + f.id() + ".sub")));
        }
    }
}
