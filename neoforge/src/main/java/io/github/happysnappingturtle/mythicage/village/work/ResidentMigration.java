package io.github.happysnappingturtle.mythicage.village.work;

import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.rite.MigrationRules;
import io.github.happysnappingturtle.mythicage.core.rite.VillageStageTable;
import io.github.happysnappingturtle.mythicage.core.village.CivVillageData;
import io.github.happysnappingturtle.mythicage.core.village.build.BuildPlacement;
import io.github.happysnappingturtle.mythicage.core.village.gen.BuildingSpec;
import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.core.village.migrate.MigrationPlanner;
import io.github.happysnappingturtle.mythicage.core.village.work.Job;
import io.github.happysnappingturtle.mythicage.core.village.work.JobStep;
import io.github.happysnappingturtle.mythicage.core.village.work.Priority;
import io.github.happysnappingturtle.mythicage.rite.RiteTables;
import io.github.happysnappingturtle.mythicage.village.VillagePopulation;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy;
import io.github.happysnappingturtle.mythicage.village.gen.VillageBuildings;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import io.github.happysnappingturtle.mythicage.village.resident.Residents;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 文明村庄的移民（T19 设计文档 §10.2、§1.6 H1–H4）：取代 T11a 往文明村庄里放原版无业村民的做法。
 * <ul>
 *   <li>古典起；目标人数 = 基线 + 各阶段人口加成（{@code village_stage}）；要有空床（名册没人占的 {@code home} 锚点）；
 *       每 {@code migrationDays} 最多 1 人；</li>
 *   <li>在村口外 8–12 格生成「在途居民」，只做一件事——走到分给他的床位（工作单 {@value #ARRIVE}）；走到才登记进名册；</li>
 *   <li>路上被杀（{@link #onDied}）、{@link #GIVE_UP} 刻内没走到就作废，下一个移民日再来；同一时间每村最多 1 名在途居民；</li>
 *   <li>职业按空缺岗位（{@link MigrationPlanner}）。</li>
 * </ul>
 */
public final class ResidentMigration {
    public static final String ARRIVE = "arrive";
    public static final int SPAWN_MIN = 8;
    public static final int SPAWN_MAX = 12;
    /** 20 分钟没走到就作废。 */
    public static final long GIVE_UP = 24000;
    private static final int SPOT_TRIES = 24;

    private ResidentMigration() {
    }

    public static void registerAll() {
        JobType.register(new Arrive());
    }

    /** 每次调度调用：处理在途移民的作废、记基线、按判定生成新移民。返回本次的判定（GameTest 看）。 */
    public static MigrationRules.Decision step(VillageContext ctx) {
        VillageEconomy.State s = ctx.state();
        if (s.migrant != null) {
            if (ctx.now() - s.migrantSince > GIVE_UP) {
                ctx.residents().stream().filter(r -> r.getUUID().equals(s.migrant)).findFirst().ifPresent(ResidentEntity::discard);
                s.log(ctx.today(), "mythic_age.village.log.migrant_lost");
                s.migrant = null;
            } else {
                return MigrationRules.Decision.WAITING;
            }
        }
        Optional<Civilization> civ = Civilization.byId(ctx.village().civ());
        if (civ.isEmpty()) {
            return MigrationRules.Decision.INACTIVE;
        }
        VillageRegistry.Village v = recordBaseline(ctx);
        VillageStageTable table = RiteTables.stages(civ.get());
        int members = s.roster.size();
        int target = table.populationTarget(v.baseline() >= 0 ? v.baseline() : members, v.stage());
        List<BlockPos> free = Residents.freeHomes(ctx.plan(), s);
        MigrationRules.Decision d = MigrationRules.decide(new MigrationRules.Input(v.stage(), members, target, free.size(),
                ctx.today(), v.lastMigrationDay(), table.migrationInterval(v.stage()), true));
        VillagePopulation.setNeedsHousing(v.id(), d == MigrationRules.Decision.NO_BED);
        if (d != MigrationRules.Decision.MIGRATE) {
            return d;
        }
        Optional<ResidentEntity> migrant = spawn(ctx, free);
        if (migrant.isEmpty()) {
            return MigrationRules.Decision.WAITING;
        }
        VillageRegistry.get(ctx.level().getServer()).update(v.withPopulation(v.baseline(), ctx.today()));
        return d;
    }

    /** 在途居民死了：作废，下一个移民日再来。 */
    public static void onDied(VillageEconomy.State s, ResidentEntity r, long day) {
        if (r.getUUID().equals(s.migrant)) {
            s.migrant = null;
            s.log(day, "mythic_age.village.log.migrant_lost");
        }
    }

    /** 开发命令：不看移民日与人口目标，有空床就直接来一名在途移民。 */
    public static Optional<ResidentEntity> forceForCommand(VillageContext ctx) {
        if (ctx.state().migrant != null) {
            return Optional.empty();
        }
        return spawn(ctx, Residents.freeHomes(ctx.plan(), ctx.state()));
    }

    /** 首次到古典（或之后第一次处理）时记下名册人数作为基线（沿用 T11a 的基线字段）。 */
    static VillageRegistry.Village recordBaseline(VillageContext ctx) {
        VillageRegistry.Village v = VillageRegistry.get(ctx.level().getServer()).byId(ctx.village().id()).orElse(ctx.village());
        // 基线 0：本框架之前（T11a）按原版村民数记下的，居民村庄不可能在 0 人时升古典，当作没记过重记
        boolean stale = v.baseline() == 0 && !ctx.state().roster.isEmpty();
        if ((v.baseline() >= 0 && !stale) || !v.stage().atLeast(Age.CLASSICAL)) {
            return v;
        }
        VillageRegistry.Village updated = v.withPopulation(ctx.state().roster.size(), v.lastMigrationDay());
        VillageRegistry.get(ctx.level().getServer()).update(updated);
        return updated;
    }

    /** 在村口外 8–12 格找一块已加载、干燥、能站的地面生成在途居民；床位取离村口最近的空床。 */
    static Optional<ResidentEntity> spawn(VillageContext ctx, List<BlockPos> free) {
        List<Pos> entrances = ctx.anchors("entrance");
        BlockPos base = entrances.isEmpty() ? ctx.center() : VillageContext.block(entrances.get(0));
        BlockPos bed = free.stream().min(Comparator.comparingDouble(b -> b.distSqr(base))).orElse(null);
        if (bed == null) {
            return Optional.empty();
        }
        Optional<BlockPos> at = spot(ctx, base, ctx.level().getRandom());
        if (at.isEmpty()) {
            return Optional.empty();
        }
        Map<String, Integer> have = new LinkedHashMap<>();
        ctx.state().roster.values().forEach(m -> have.merge(m.profession(), 1, Integer::sum));
        String prof = MigrationPlanner.profession(jobs(ctx), have, ctx.data().residents().fallback());
        Optional<ResidentEntity> r = Residents.spawnMigrant(ctx.level(), ctx.village(), ctx.data(), prof, bed, at.get());
        r.ifPresent(e -> {
            e.setMigrant(true, ctx.now());
            ctx.state().migrant = e.getUUID();
            ctx.state().migrantSince = ctx.now();
            ctx.state().log(ctx.today(), "mythic_age.village.log.migrant_coming", "@" + professionKey(prof));
            VillageEconomy.get(ctx.level().getServer()).setDirty();
            io.github.happysnappingturtle.mythicage.village.quest.QuestService.onMigrantSpawned(ctx, e);
        });
        return r;
    }

    static String professionKey(String profession) {
        return "mythic_age.profession." + profession.replace(':', '.');
    }

    static Optional<BlockPos> spot(VillageContext ctx, BlockPos base, RandomSource random) {
        for (int i = 0; i < SPOT_TRIES; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double r = SPAWN_MIN + random.nextDouble() * (SPAWN_MAX - SPAWN_MIN);
            int x = base.getX() + Mth.floor(Math.cos(angle) * r);
            int z = base.getZ() + Mth.floor(Math.sin(angle) * r);
            if (!ctx.loaded(new BlockPos(x, base.getY(), z))) {
                continue;
            }
            BlockPos pos = new BlockPos(x, ctx.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            BlockPos below = pos.below();
            if (ctx.level().getFluidState(pos).isEmpty() && ctx.level().getFluidState(below).isEmpty()
                    && !ctx.level().getBlockState(below).getCollisionShape(ctx.level(), below).isEmpty()
                    && ctx.level().getBlockState(pos).getCollisionShape(ctx.level(), pos).isEmpty()
                    && ctx.level().getBlockState(pos.above()).getCollisionShape(ctx.level(), pos.above()).isEmpty()) {
                return Optional.of(pos);
            }
        }
        return Optional.empty();
    }

    /**
     * 本村的岗位（§1.6 H1）：已建建筑的 {@code jobs} 之和——生成的建筑按锚点认出来，施工建成的直接查；职业开放阶段没到的不算。
     * 每 MC 日算一次。
     */
    public static Map<String, Integer> jobs(VillageContext ctx) {
        Map<String, Integer> cached = ctx.rt().jobsCache(ctx.today());
        if (cached != null) {
            return cached;
        }
        Map<String, Integer> out = new LinkedHashMap<>();
        Map<String, BuildingSpec> specs = VillageBuildings.all();
        for (BuildingSpec s : specs.values()) {
            if (!s.generated() || !s.civ().equals(ctx.village().civ()) || s.jobs().isEmpty()) {
                continue;
            }
            int n = BuildPlacement.locateAll(s, ctx.plan().info().anchors(), ctx.plan().info().built()).size();
            s.jobs().forEach((p, c) -> out.merge(p, c * n, Integer::sum));
        }
        for (VillageEconomy.Built b : ctx.state().constructed) {
            BuildingSpec s = specs.get(b.building());
            if (s != null) {
                s.jobs().forEach((p, c) -> out.merge(p, c, Integer::sum));
            }
        }
        out.keySet().removeIf(p -> {
            CivVillageData.Profession prof = ctx.data().professions().get(p);
            return prof != null && !BasicJobTypes.stageOpen(ctx, prof.stage());
        });
        out.values().removeIf(c -> c <= 0);
        ctx.rt().putJobs(ctx.today(), out);
        return out;
    }

    /** 在途居民走向床位的工作单（只本人做）。 */
    static Job arriveJob(VillageContext ctx, ResidentEntity r) {
        BlockPos home = r.home() == null ? ctx.center() : r.home();
        return new Job(ctx.nextJobId(), ARRIVE, VillageContext.pos(home), "arrive:" + r.getUUID(), "",
                List.of(JobStep.move(), JobStep.settle()), Map.of(), Map.of(), Set.of(), Priority.ROUTINE, 0, false,
                (int) GIVE_UP, r.getUUID());
    }

    /** 走到了：登记进名册（职业、床位），算到人口里。 */
    static final class Arrive implements JobType {
        @Override
        public String id() {
            return ARRIVE;
        }

        @Override
        public List<Job> candidates(VillageContext ctx) {
            return List.of();
        }

        @Override
        public boolean settle(VillageContext ctx, ResidentEntity resident, Job job) {
            if (!resident.inTransit() || resident.home() == null) {
                return true;
            }
            VillageEconomy.State s = ctx.state();
            UUID id = resident.getUUID();
            s.roster.put(id, new VillageEconomy.Member(id, resident.profession(), resident.home().immutable(), false));
            resident.setMigrant(false, 0);
            if (id.equals(s.migrant)) {
                s.migrant = null;
            }
            s.log(ctx.today(), "mythic_age.village.log.migrant_arrived",
                    resident.getCustomName() == null ? "" : resident.getCustomName().getString(), "@" + professionKey(resident.profession()));
            VillageEconomy.get(ctx.level().getServer()).setDirty();
            io.github.happysnappingturtle.mythicage.village.quest.QuestService.onMigrantArrived(ctx, id);
            return true;
        }
    }
}
