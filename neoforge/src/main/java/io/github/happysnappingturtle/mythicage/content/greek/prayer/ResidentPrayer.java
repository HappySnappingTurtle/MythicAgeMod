package io.github.happysnappingturtle.mythicage.content.greek.prayer;

import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekAltarBlockEntity;
import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekTemple;
import io.github.happysnappingturtle.mythicage.content.greek.temple.TempleService;
import io.github.happysnappingturtle.mythicage.core.greek.prayer.BelieverRules;
import io.github.happysnappingturtle.mythicage.core.greek.prayer.PrayerFormula;
import io.github.happysnappingturtle.mythicage.core.greek.temple.TempleRules;
import io.github.happysnappingturtle.mythicage.core.village.time.Schedule;
import io.github.happysnappingturtle.mythicage.core.village.work.Job;
import io.github.happysnappingturtle.mythicage.core.village.work.JobStep;
import io.github.happysnappingturtle.mythicage.core.village.work.Priority;
import io.github.happysnappingturtle.mythicage.guardian.MythDays;
import io.github.happysnappingturtle.mythicage.shrine.ShrineRegistry;
import io.github.happysnappingturtle.mythicage.village.VillageService;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import io.github.happysnappingturtle.mythicage.village.work.JobType;
import io.github.happysnappingturtle.mythicage.village.work.VillageContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 居民祈祷（T19 设计文档 §11.2、§1.6 H5；V3-希腊-居民 §5.1）：希腊行为模块在祈祷窗口里提供的工作单 {@value #ID}。
 * 神坛选择、祈祷位预留、√n 递减、「同一天只为一位主人」「离家 ≥ 8 格才计入」全部沿用 T10（{@link PrayerService}、
 * {@link BelieverRules}），与原版村民信徒用同一张祈祷位表、同一套采样结算。
 * <ul>
 *   <li>每人每天一班 {@link #SHIFT} 刻（原版信徒整个窗口都跪着；居民还要干活，按班轮换，祈祷位一直有人）；</li>
 *   <li>只派空闲的居民（烧窑、施工段上的人本来就不空闲）；优先级「缺口生产」、权重 {@link #WEIGHT}，缺粮时让位给口粮链；</li>
 *   <li>姿势：站立、双手上举（古希腊人祈祷的常见姿势）；女性居民有时跪下（同一资料）。祈祷记录用原版信徒同一个附件。</li>
 * </ul>
 */
public final class ResidentPrayer implements JobType {
    public static final String ID = "pray";
    public static final int SHIFT = 2400;
    public static final double WEIGHT = 10;
    public static final String STAND = "pray", KNEEL = "pray_kneel";
    private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1.0F, 0.82F, 0.35F), 0.9F);

    @Override
    public String id() {
        return ID;
    }

    /** 由希腊行为模块提供（{@code VillageBehavior.jobs}），这里不出候选。 */
    @Override
    public List<Job> candidates(VillageContext ctx) {
        return List.of();
    }

    /** ref = 神坛维度|坐标|祈祷位|主人。 */
    static String ref(GlobalPos altar, BlockPos slot, UUID owner) {
        return altar.dimension().location() + "|" + altar.pos().asLong() + "|" + slot.asLong() + "|" + owner;
    }

    static GlobalPos refAltar(VillageContext ctx, String ref) {
        return refAltar(ctx.level(), ref);
    }

    static GlobalPos refAltar(ServerLevel level, String ref) {
        String[] p = ref.split("\\|");
        return GlobalPos.of(level.dimension(), BlockPos.of(Long.parseLong(p[1])));
    }

    static BlockPos refSlot(String ref) {
        return BlockPos.of(Long.parseLong(ref.split("\\|")[2]));
    }

    static UUID refOwner(String ref) {
        return UUID.fromString(ref.split("\\|")[3]);
    }

    /** 这一刻可派的祈祷单：每个空闲、今天还没祈祷过的居民一份，各自先占一个祈祷位（没派出去的 40 刻后自动过期）。 */
    public static List<Job> jobs(VillageContext ctx) {
        ServerLevel level = ctx.level();
        if (!Schedule.WORK.equals(ctx.segment()) || !PrayerFormula.inWindow(level.getDayTime())) {
            return List.of();
        }
        long today = MythDays.today(level.getServer());
        long now = level.getGameTime();
        int remaining = (int) Math.min(SHIFT, PrayerFormula.WINDOW_END - Math.floorMod(level.getDayTime(), 24000L));
        if (remaining < 200) {
            return List.of();
        }
        List<ShrineRegistry.Entry> altars = ShrineRegistry.get(level.getServer()).ofKind(GreekTemple.ALTAR_KIND.id()).stream()
                .filter(e -> e.pos().dimension().equals(level.dimension())).toList();
        if (altars.isEmpty()) {
            return List.of();
        }
        TempleRules rules = TempleService.rules();
        BlockPos bell = ctx.center();
        List<Job> out = new ArrayList<>();
        for (ResidentEntity r : ctx.residents()) {
            if (!r.idle() || r.inTransit() || r.home() == null || !ctx.state().roster.containsKey(r.getUUID())) {
                continue;
            }
            PrayerRecord rec = r.getData(GreekPrayer.RECORD);
            if (rec.prayedOn(today)) {
                continue;
            }
            List<BelieverRules.Candidate<GlobalPos>> cands = new ArrayList<>();
            for (ShrineRegistry.Entry e : altars) {
                BlockPos p = e.pos().pos();
                boolean inVillage = horizontal(bell.getX() + 0.5, bell.getZ() + 0.5, p) <= VillageService.RANGE;
                cands.add(new BelieverRules.Candidate<>(e.pos(), e.owner(), inVillage, horizontal(r.getX(), r.getZ(), p)));
            }
            for (BelieverRules.Candidate<GlobalPos> c : BelieverRules.order(cands)) {
                if (!BelieverRules.canPrayFor(rec.lastPrayDay(), rec.lastPrayOwner(), today, c.owner())) {
                    continue;
                }
                GreekAltarBlockEntity be = PrayerService.altarAt(level.getServer(), c.altar());
                if (be == null || !be.hasResult()) {
                    continue;
                }
                List<BlockPos> slots = new ArrayList<>(be.prayerSlots(rules));
                slots.sort(Comparator.comparingDouble(p -> p.distToCenterSqr(r.position())));
                Optional<BlockPos> s = PrayerService.SLOTS.claim(c.altar(), slots, r.getUUID(), now);
                if (s.isEmpty()) {
                    continue;
                }
                String anim = r.female() && Math.floorMod(r.getUUID().hashCode(), 2) == 0 ? KNEEL : STAND;
                out.add(new Job(ctx.nextJobId(), ID, VillageContext.pos(s.get()), "pray:" + r.getUUID(),
                        ref(c.altar(), s.get(), c.owner()), List.of(JobStep.move(), JobStep.act(anim, remaining), JobStep.settle()),
                        Map.of(), Map.of(), Set.of(), Priority.SHORTAGE, WEIGHT, false, Job.DEFAULT_TIMEOUT + remaining,
                        r.getUUID()));
                break;
            }
        }
        return out;
    }

    private static double horizontal(double x, double z, BlockPos p) {
        double dx = x - (p.getX() + 0.5);
        double dz = z - (p.getZ() + 0.5);
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** 派单时续一次预留；位置已被别人占了就不派。作废时不用释放：预留没人报到，40 刻后自动过期。 */
    @Override
    public Optional<Map<String, Integer>> escrow(VillageContext ctx, Job job) {
        GlobalPos altar = refAltar(ctx, job.ref());
        BlockPos slot = refSlot(job.ref());
        GreekAltarBlockEntity be = PrayerService.altarAt(ctx.level().getServer(), altar);
        if (be == null) {
            return Optional.empty();
        }
        // 传入全部祈祷位：预留表会清掉位置不在列表里的预留（只传自己那一格会把别人的预留全清掉）
        List<BlockPos> slots = new ArrayList<>(be.prayerSlots(TempleService.rules()));
        slots.remove(slot);
        slots.add(0, slot);
        Optional<BlockPos> got = PrayerService.SLOTS.claim(altar, slots, job.assignee(), ctx.level().getGameTime());
        return got.isPresent() && got.get().equals(slot) ? Optional.of(Map.of()) : Optional.empty();
    }

    /** 走向祈祷位的路上每 20 刻报到一次续预留（同原版信徒）；预留没了就不去了。 */
    @Override
    public void onWalkTick(ServerLevel level, ResidentEntity r, Job job, int step) {
        if (!PrayerService.SLOTS.report(refAltar(level, job.ref()), r.getUUID(), level.getGameTime(), false, false, false)) {
            r.abortJob(false);
        }
    }

    /**
     * 跪拜中：第 1 刻算到达（离家 ≥ 8 格才计入，记下今天为谁祈祷），之后每 20 刻向神坛报到；
     * 预留被收回（神坛拆了、过期）就停下。始终面向神坛。
     */
    @Override
    public void onActTick(ServerLevel level, ResidentEntity r, Job job, int step, int ticks) {
        GlobalPos altar = refAltar(level, job.ref());
        BlockPos slot = refSlot(job.ref());
        long now = level.getGameTime();
        boolean counted = r.home() != null
                && BelieverRules.pilgrimageOk(r.home().getX() + 0.5, r.home().getZ() + 0.5, slot.getX() + 0.5, slot.getZ() + 0.5);
        if (ticks == 1) {
            if (!PrayerService.SLOTS.report(altar, r.getUUID(), now, true, counted, !counted)) {
                r.abortJob(false);
                return;
            }
            if (counted) {
                long today = MythDays.today(level.getServer());
                r.getData(GreekPrayer.RECORD).record(today, refOwner(job.ref()), altar);
                GreekAltarBlockEntity be = PrayerService.altarAt(level.getServer(), altar);
                if (be != null && be.notePrayer(r.getUUID(), today)) {
                    PrayerService.refreshEstimate(be);
                }
            }
        } else if (ticks % PrayerSlots.REPORT_INTERVAL == 0
                && !PrayerService.SLOTS.report(altar, r.getUUID(), now, true, counted, !counted)) {
            r.abortJob(false);
            return;
        }
        double ax = altar.pos().getX() + 0.5 - r.getX();
        double az = altar.pos().getZ() + 0.5 - r.getZ();
        float yaw = (float) (Mth.atan2(az, ax) * Mth.RAD_TO_DEG) - 90.0F;
        r.setYRot(yaw);
        r.setYHeadRot(yaw);
        r.setYBodyRot(yaw);
        if (counted && now % 40 == r.getId() % 40) {
            level.sendParticles(GOLD, r.getX(), r.getY() + 1.1, r.getZ(), 3, 0.3, 0.3, 0.3, 0);
        }
    }

    @Override
    public boolean settle(VillageContext ctx, ResidentEntity resident, Job job) {
        PrayerService.SLOTS.release(refAltar(ctx, job.ref()), resident.getUUID());
        return true;
    }

}
