package io.github.happysnappingturtle.mythicage.village.build;

import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;

import io.github.happysnappingturtle.mythicage.core.village.build.*;
import io.github.happysnappingturtle.mythicage.core.village.time.Schedule;
import io.github.happysnappingturtle.mythicage.core.village.work.*;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import io.github.happysnappingturtle.mythicage.village.work.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/** A trip holds one segment: walk to storage, fetch, then walk/work one cell at a time. */
public final class BuildJob implements JobType {
    public static final String ID = "build";
    public static final String ANIM = "build_hammer";
    private static final Map<UUID, Trip> TRIPS = new HashMap<>();
    /**
     * 工人够得着的距离（格）。不搭脚手架：站在地面或已盖好的楼层上，8 格内直接放（同千年村庄的村民）；
     * 连续 {@link #FALLBACK_AFTER} 次找不到站位后放宽到 {@link #FALLBACK_REACH}；
     * 到 {@link #GIVE_UP_AFTER} 次仍不行才标「够不着」（GA-10 实机，2026-11-11 用户定取消脚手架）。
     */
    static final double REACH = 8.0;
    static final double FALLBACK_REACH = 12.0;
    static final int FALLBACK_AFTER = 6;
    static final int GIVE_UP_AFTER = 20;

    private static final class Trip {
        final VillageContext context;
        final BuildProject project;
        final BuildSite site;
        final int segment;
        final Map<String, Integer> cargo = new TreeMap<>();
        final BitSet deferred = new BitSet();
        final BitSet cargoCells = new BitSet();
        int cell = -1, worked, searchAfter, failedSearches;
        BlockPos stand;
        /** 走向站位途中的卡住检测：上次挪动时的位置与时刻。 */
        BlockPos lastPos;
        int lastMoved;
        /** 当前这一格连续等依赖的刻数。 */
        int waited;
        /** 诊断用：最近一次动作刻数。 */
        int lastActTicks;
        Trip(VillageContext context, BuildProject project, BuildSite site, int segment) {
            this.context = context; this.project = project; this.site = site; this.segment = segment;
            // Logical cargo only: all actual items stay in project escrow until the cell succeeds.
            int solids = 0;
            var cells = site.segments.get(segment).cells();
            for (int i = 0; i < cells.size(); i++) {
                var c = cells.get(i);
                if (project.cellPlaced(segment,i) || project.blockedCells.getOrDefault(segment,new BitSet()).get(i)
                        || project.missingCells.getOrDefault(segment,new BitSet()).get(i)) continue;
                // Legacy layouts may contain 17 solids to keep a door together. Keep their bitmap stable,
                // but finish them over multiple trips so current cargo limits still apply.
                if (!c.air() && solids >= context.data().construction().tripSize()) continue;
                cargoCells.set(i);
                if (!c.air()) { solids++; cargo.merge(c.block(), 1, Integer::sum); }
            }
        }
    }
    public static String diagnostic(ResidentEntity resident) {
        Trip trip = TRIPS.get(resident.getUUID());
        return "step=" + resident.step() + " tick=" + resident.tickCount + " path=" + resident.getNavigation().getPath()
                + (trip == null ? " no trip" : " segment=" + trip.segment + " cell=" + trip.cell + " stand=" + trip.stand + " work=" + trip.worked + " search=" + trip.searchAfter + " failed=" + trip.failedSearches
                        + " actTicks=" + trip.lastActTicks + (trip.cell < 0 ? "" : " target=" + targetOf(trip).toShortString()
                        + " dist=" + String.format("%.1f", Math.sqrt(resident.distanceToSqr(targetOf(trip).getCenter()))))
                        + " budgetLeft=" + io.github.happysnappingturtle.mythicage.village.life.PathBudget.left());
    }
    public static void clear() { TRIPS.clear(); }
    @Override public String id() { return ID; }
    static String ref(BuildProject p, int seg) { return p.id + ":" + seg; }
    static long refProject(String ref) { return Long.parseLong(ref.substring(0, ref.indexOf(':'))); }
    static int refSeg(String ref) { return Integer.parseInt(ref.substring(ref.indexOf(':') + 1)); }

    @Override public List<Job> candidates(VillageContext ctx) {
        BuildPerformance.begin();
        try { return candidatesMeasured(ctx); } finally { BuildPerformance.end(); }
    }

    private List<Job> candidatesMeasured(VillageContext ctx) {
        if (!Schedule.WORK.equals(ctx.segment())) return List.of();
        var project = ctx.state().activeProject();
        if (project.isEmpty() || project.get().state != BuildProject.State.BUILDING) return List.of();
        BuildProject p = project.get();
        var maybeSite = Construction.site(ctx, p);
        if (maybeSite.isEmpty()) return List.of();
        BuildSite site = maybeSite.get();
        Set<Integer> busy = new HashSet<>();
        for (Job job : ctx.rt().activeJobs()) if (ID.equals(job.type()) && refProject(job.ref()) == p.id) busy.add(refSeg(job.ref()));
        int slots = ctx.data().construction().maxBuilders() - busy.size();
        List<Job> out = new ArrayList<>();
        // Do not start a later phase until all cells in the demolition/late phase have completed.
        // 例外：当前阶段一格都放不了、也没人在干（依附在后一阶段的格上），就放行下一阶段，免得整座卡住（GA-11 实机）
        BuildSegments.Kind phase = null;
        Boolean late = null;
        boolean alive = false;
        for (var segment : site.segments) {
            if (p.placed.get(segment.index())) continue;
            boolean isLate = segment.cells().stream().anyMatch(BuildSegments.Cell::last);
            if (phase == null) { phase = segment.kind(); late = isLate; }
            if (phase != segment.kind() || late != isLate) {
                if (alive) break;
                phase = segment.kind(); late = isLate;
            }
            if (slots <= 0) break;
            if (busy.contains(segment.index())) { alive = true; continue; }
            if (Construction.nextCell(p, segment) < 0) continue;
            if (!Construction.ready(ctx,p,site,segment)) continue;
            alive = true;
            var storage = fetchPoint(ctx, p, site);
            var steps = List.of(JobStep.moveTo(storage, ""), JobStep.act("haul_put", ctx.data().construction().fetchTicks()),
                    JobStep.act(ANIM, 24000), JobStep.settle());
            out.add(new Job(ctx.nextJobId(), ID, storage, "build:" + p.id + ":" + segment.index(), ref(p, segment.index()),
                    steps, Map.of(), Map.of(), Set.of(), Priority.ACTIVE_BUILD, 1, true, 26000));
            slots--;
        }
        return out;
    }

    /**
     * 取料点：仓储点离工地在 {@code fetchRange} 以内就去仓储点；更远就到施工牌旁取（料先堆在工地），
     * 免得每一趟都跑一个来回（GA-10 实机：山坡上的谷仓离工地 60 多格，施工几乎停住）。
     */
    static Pos fetchPoint(VillageContext ctx, BuildProject p, BuildSite site) {
        Pos storage = BasicJobTypes.storage(ctx, p.origin).orElse(VillageContext.pos(ctx.center()));
        var box = site.box();
        long dx = storage.x() - (box.minX() + box.maxX()) / 2;
        long dz = storage.z() - (box.minZ() + box.maxZ()) / 2;
        long range = ctx.data().construction().fetchRange();
        if (dx * dx + dz * dz <= range * range) {
            return storage;
        }
        return SiteMarkers.sign(ctx, p).map(sign -> VillageContext.pos(awayFrom(ctx.level(), site, sign))).orElse(storage);
    }

    /** 施工牌外侧、背离工地两格的空地（站得住才用）；没有就用牌子本身的格。 */
    static BlockPos awayFrom(ServerLevel level, BuildSite site, BlockPos sign) {
        var box = site.box();
        int dx = Integer.signum(sign.getX() * 2 - (box.minX() + box.maxX()));
        int dz = Integer.signum(sign.getZ() * 2 - (box.minZ() + box.maxZ()));
        for (int step = 2; step >= 1; step--) {
            for (int dy = 1; dy >= -1; dy--) {
                BlockPos at = sign.offset(dx * step, dy, dz * step);
                if (level.isLoaded(at) && BuildSite.standable(level, at)) {
                    return at;
                }
            }
        }
        return sign;
    }

    @Override public void onActDone(VillageContext ctx, ResidentEntity resident, Job job, int step) {
        BuildPerformance.begin();
        try { fetched(ctx,resident,job,step); } finally { BuildPerformance.end(); }
    }

    private void fetched(VillageContext ctx, ResidentEntity resident, Job job, int step) {
        if (step != 1) return;
        ctx.state().activeProject().filter(p -> p.id == refProject(job.ref())).ifPresent(p -> Construction.site(ctx, p).ifPresent(site -> {
            TRIPS.put(resident.getUUID(), new Trip(ctx, p, site, refSeg(job.ref())));
            resident.setCarrying("basket");
        }));
    }

    @Override public void onActTick(ServerLevel level, ResidentEntity resident, Job job, int step, int ticks) {
        BuildPerformance.begin();
        try { tickTrip(level, resident, job, step, ticks); } finally { BuildPerformance.end(); }
    }

    private void tickTrip(ServerLevel level, ResidentEntity resident, Job job, int step, int ticks) {
        if (step != 2) return;
        Trip trip = TRIPS.get(resident.getUUID());
        if (trip == null || trip.project.state != BuildProject.State.BUILDING) { end(resident); return; }
        trip.lastActTicks = ticks;
        if (!VillageScheduler.isRunning(trip.context)) return;
        if (trip.stand != null && ticks % 100 == 0 && resident.getNavigation().isDone()) {
            trip.stand = null;
        }
        var segment = trip.site.segments.get(trip.segment);
        if (trip.cell < 0) {
            for (int i = 0; i < segment.cells().size(); i++) {
                if (trip.cargoCells.get(i) && !trip.deferred.get(i) && !trip.project.cellPlaced(segment.index(), i)
                        && !trip.project.blockedCells.getOrDefault(segment.index(), new BitSet()).get(i)
                        && !trip.project.missingCells.getOrDefault(segment.index(), new BitSet()).get(i)) { trip.cell = i; break; }
            }
        }
        if (trip.cell < 0) { end(resident); return; }
        var cell = segment.cells().get(trip.cell);
        var target = VillageContext.block(cell.pos());
        if (!level.isLoaded(target)) return;
        var state = trip.site.state(segment, cell);
        boolean alreadyEmpty = cell.air() && level.getBlockState(target).isAir();
        ItemStack hand = new ItemStack(state.getBlock());
        if (!ItemStack.isSameItemSameComponents(hand, resident.getMainHandItem())) resident.setItemSlot(EquipmentSlot.MAINHAND, hand);
        // 够取距离：平时 8 格；连续多次找不到站位后放宽到 12 格，由手上这名工人补上（GA-10 实机：取消脚手架，2026-11-11 用户定）
        double reach = trip.failedSearches >= FALLBACK_AFTER ? FALLBACK_REACH : REACH;
        if (!alreadyEmpty && resident.distanceToSqr(target.getX() + .5, target.getY() + .5, target.getZ() + .5) > reach * reach) {
            resident.playAnim("");
            trip.worked = 0;
            // 这一刻寻路名额不够就下一刻再找，不算一次失败（GA-10 实机：名额被农夫占满，空搜 20 次就把整片格子误判成够不着）
            // 超过 100 刻没搜过就强制搜一次（只多算一条直达路径），免得名额一直被别人占满、施工饿死
            boolean starved = ticks - trip.searchAfter > 100;
            if (ticks >= trip.searchAfter && trip.stand == null
                    && (starved || io.github.happysnappingturtle.mythicage.village.life.PathBudget.left() >= 2)) {
                trip.searchAfter = ticks + 40;
                trip.failedSearches++;
                trip.stand = trip.site.standFor(level, target, resident, starved).orElse(null);
            }
            if (trip.stand != null) {
                // 走向站位时 60 刻几乎没挪动（挤在墙角、互相顶住）：放弃这个站位重新找
                if (trip.lastPos == null || !trip.lastPos.closerThan(resident.blockPosition(), 1)) {
                    trip.lastPos = resident.blockPosition();
                    trip.lastMoved = ticks;
                } else if (ticks - trip.lastMoved > 60) {
                    resident.getNavigation().stop();
                    trip.stand = null;
                    trip.searchAfter = ticks;
                    trip.lastPos = null;
                }
            }
            if (ticks % 20 == 0 && io.github.happysnappingturtle.mythicage.village.life.PathBudget.take()) {
                if (trip.stand != null) {
                    resident.getNavigation().moveTo(trip.stand.getX() + .5, trip.stand.getY(), trip.stand.getZ() + .5, .65);
                } else if (trip.failedSearches >= FALLBACK_AFTER) {
                    // 找不到站位：尽量往目标那边走，走近了就能在放宽的距离内补上
                    resident.getNavigation().moveTo(target.getX() + .5, target.getY(), target.getZ() + .5, .65);
                }
            }
            if (trip.stand == null && trip.failedSearches >= GIVE_UP_AFTER) {
                BuildProject.mark(trip.project.blockedCells, trip.segment, trip.cell, true);
                trip.project.blockedReason = "unreachable";
                Construction.exhausted(trip.context, trip.project, trip.site);
                trip.cell = -1; trip.failedSearches = 0;
            }
            return;
        }
        resident.getNavigation().stop();
        resident.playAnim(alreadyEmpty ? "" : ANIM);
        resident.getLookControl().setLookAt(target.getX() + .5, target.getY() + .5, target.getZ() + .5);
        if (!alreadyEmpty && ++trip.worked < BuildPacing.ticks(trip.context.data().construction(), trip.context.state().constructionBank,
                Construction.speed(trip.context.village().id()))) return;
        Construction.Result result = Construction.dropCell(trip.context, trip.project, trip.segment, trip.cell);
        if (result == Construction.Result.WAIT) {
            // 依赖的方块可能在别人那一段：先等；等超过 200 刻就跳过这一格去放别的，免得整趟卡到超时被作废（GA-10 实机）
            if (++trip.waited > 200) {
                trip.deferred.set(trip.cell);
                trip.cell = -1; trip.worked = 0; trip.waited = 0;
            }
            return;
        }
        trip.waited = 0;
        if (result == Construction.Result.DEFERRED) trip.deferred.set(trip.cell);
        if (result == Construction.Result.PLACED) {
            if (!alreadyEmpty) resident.swing(InteractionHand.MAIN_HAND);
            trip.cargo.computeIfPresent(cell.block(), (k, n) -> n > 1 ? n - 1 : null);
            if (!alreadyEmpty) trip.context.state().constructionBank = BuildPacing.spendBank(trip.context.data().construction(), trip.context.state().constructionBank);
        }
        trip.cell = -1; trip.worked = 0; trip.stand = null; trip.failedSearches = 0;
        if (Construction.nextCell(trip.project, segment) < 0 || !trip.project.active()) end(resident);
    }

    private static BlockPos targetOf(Trip trip) {
        return VillageContext.block(trip.site.segments.get(trip.segment).cells().get(trip.cell).pos());
    }

    private static void end(ResidentEntity resident) {
        Trip trip = TRIPS.remove(resident.getUUID());
        resident.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        resident.setCarrying(""); resident.playAnim(""); resident.finishAct();
    }
    @Override public boolean settle(VillageContext ctx, ResidentEntity resident, Job job) { end(resident); return true; }
    @Override public void onAbort(VillageScheduler.Runtime rt, Job job) {
        TRIPS.entrySet().removeIf(e -> {
            Trip trip = e.getValue();
            if (trip.context.rt() != rt || trip.project.id != refProject(job.ref()) || trip.segment != refSeg(job.ref())) return false;
            var entity = trip.context.level().getEntity(e.getKey());
            if (entity instanceof ResidentEntity r) { r.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY); r.setCarrying(""); }
            return true;
        });
    }
    /** Timing is evaluated per cell at execution, never prepaid for the whole trip. */
    public static Job withBankedTicks(VillageContext ctx, Job job) { return job; }
}
