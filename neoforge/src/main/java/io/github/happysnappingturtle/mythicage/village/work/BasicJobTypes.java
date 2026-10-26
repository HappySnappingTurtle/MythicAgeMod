package io.github.happysnappingturtle.mythicage.village.work;

import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.village.CivVillageData;
import io.github.happysnappingturtle.mythicage.core.village.economy.VillageStock;
import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.core.village.recipe.VillageRecipe;
import io.github.happysnappingturtle.mythicage.core.village.resource.ResourcePoint;
import io.github.happysnappingturtle.mythicage.core.village.time.Schedule;
import io.github.happysnappingturtle.mythicage.core.village.work.Job;
import io.github.happysnappingturtle.mythicage.core.village.work.JobStep;
import io.github.happysnappingturtle.mythicage.core.village.work.Priority;
import io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import io.github.happysnappingturtle.mythicage.village.resident.Residents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 框架自带的工作单类型（T19 设计文档 §5.1）：采集点、加工（含无人值守段与取出）、收作物（含打谷）、傍晚聚会、回家。
 * 各类型只看登记的资源（建筑锚点、资源点），不扫描周围的方块，不碰玩家的田和房子（11 号 §6、§9）。
 * <p>
 * 取料类的活（采集点、收作物、采果、伐木）在<b>动作做完时</b>才改世界，取下的东西挂在本单的「途中物品」上，
 * 搬回谷仓（{@code storage_keeper} 锚点）结算时入库；半路被打断，东西作废（11 号 §6：不复制、不凭空产出）。
 */
public final class BasicJobTypes {
    public static final String GATHER = "gather_point", CRAFT = "craft", COLLECT = "collect", HARVEST = "harvest_crop",
            LEISURE = "leisure", GO_HOME = "go_home";
    /** 工作站锚点名。 */
    public static final String STATION_ANCHOR = "workstation";

    private BasicJobTypes() {
    }

    public static void registerAll() {
        JobType.register(new GatherPoint());
        JobType.register(new Craft());
        JobType.register(new Collect());
        JobType.register(new HarvestCrop());
        JobType.register(new Leisure());
        JobType.register(new GoHome());
        LandJobTypes.registerAll();
        JobType.register(new io.github.happysnappingturtle.mythicage.village.build.BuildJob());
        ResidentMigration.registerAll();
        io.github.happysnappingturtle.mythicage.village.life.VillageLife.registerAll();
        JobType.register(new io.github.happysnappingturtle.mythicage.village.soldier.SoldierTrain());
        JobType.register(new io.github.happysnappingturtle.mythicage.village.quest.RepairJob());
    }

    static boolean working(VillageContext ctx) {
        return Schedule.WORK.equals(ctx.segment());
    }

    static int sum(Map<String, Integer> m) {
        return m.values().stream().mapToInt(Integer::intValue).sum();
    }

    /** 产物里库存最少的那一样决定紧缺程度：越缺越先做。 */
    static double need(VillageStock stock, Map<String, Integer> outputs, double base) {
        int least = outputs.keySet().stream().mapToInt(stock::count).min().orElse(0);
        return base / (1 + least / 16.0);
    }

    /** 离 from 最近的谷仓管事位（搬回仓库的地方）；没有谷仓就原地交。 */
    public static Optional<Pos> storage(VillageContext ctx, Pos from) {
        return ctx.anchors(Residents.KEEPER_ANCHOR).stream()
                .min(Comparator.comparingLong(p -> (long) (p.x() - from.x()) * (p.x() - from.x())
                        + (long) (p.z() - from.z()) * (p.z() - from.z())));
    }

    /** 「走到 → 干活 → （搬回仓库）→ 结算」：干活那一段是第 1 步（onActDone 的 step = 1）。 */
    static List<JobStep> withHaul(VillageContext ctx, Pos at, String anim, int ticks, String carry) {
        List<JobStep> steps = new ArrayList<>();
        steps.add(JobStep.move());
        steps.add(JobStep.act(anim, ticks));
        storage(ctx, at).ifPresent(s -> steps.add(JobStep.moveTo(s, carry)));
        steps.add(JobStep.settle());
        return steps;
    }

    /** 把本单的途中物品入库。没有（重启丢了、早被作废）返回 false。 */
    public static boolean depositPending(VillageContext ctx, Job job) {
        Map<String, Integer> got = ctx.rt().pending().remove(job.id());
        if (got == null || got.isEmpty()) {
            return false;
        }
        got.forEach(ctx.state().stock::deposit);
        return true;
    }

    public static void addPending(VillageContext ctx, Job job, Map<String, Integer> items) {
        Map<String, Integer> m = ctx.rt().pending().computeIfAbsent(job.id(), k -> new LinkedHashMap<>());
        items.forEach((k, v) -> {
            if (v > 0) {
                m.merge(k, v, Integer::sum);
            }
        });
    }

    // ───────────── 采集点 ─────────────

    /** 从登记采集点取料：余量减少（NPC 与玩家共用同一份），取下的东西搬回仓库入库。 */
    static final class GatherPoint implements JobType {
        @Override
        public String id() {
            return GATHER;
        }

        @Override
        public List<Job> candidates(VillageContext ctx) {
            if (!working(ctx)) {
                return List.of();
            }
            List<Job> out = new ArrayList<>();
            for (ResourcePoint p : ctx.state().points.values()) {
                CivVillageData.PointKind k = ctx.data().points().get(p.kind());
                if (k == null || p.recover(ctx.today()).remaining() < k.take()
                        || ctx.state().stock.free() < sum(k.outputs()) || !ctx.loaded(VillageContext.block(p.pos()))) {
                    continue;
                }
                out.add(new Job(ctx.nextJobId(), GATHER, p.pos(), "point:" + p.id(), p.id(),
                        withHaul(ctx, p.pos(), k.anim(), k.ticks(), carryOf(p.kind())), Map.of(), k.outputs(),
                        k.professions(), Priority.SHORTAGE, need(ctx.state().stock, k.outputs(), 1), k.outdoor(),
                        Job.DEFAULT_TIMEOUT));
            }
            return out;
        }

        static String carryOf(String kind) {
            return switch (kind) {
                case "clay" -> "clay";
                case "quarry" -> "stone";
                case "prune" -> "wood";
                default -> "basket";
            };
        }

        @Override
        public void onActDone(VillageContext ctx, ResidentEntity resident, Job job, int step) {
            if (step != 1) {
                return;
            }
            ResourcePoint p = ctx.state().points.get(job.ref());
            CivVillageData.PointKind k = p == null ? null : ctx.data().points().get(p.kind());
            if (k == null) {
                return;
            }
            p = p.recover(ctx.today());
            if (p.remaining() < k.take()) {
                ctx.state().points.put(p.id(), p);
                return;
            }
            ResourcePoint after = p.take(k.take());
            ctx.state().points.put(p.id(), after);
            addPending(ctx, job, k.outputs());
            if (after.tier() != p.tier()) {
                PointVisuals.update(ctx, after);
            }
        }

        @Override
        public boolean settle(VillageContext ctx, ResidentEntity resident, Job job) {
            return depositPending(ctx, job);
        }
    }

    // ───────────── 加工 ─────────────

    /** 工作站方块 id。 */
    static String stationId(BlockState s) {
        return BuiltInRegistries.BLOCK.getKey(s.getBlock()).toString();
    }

    public static boolean stageOpen(VillageContext ctx, String stage) {
        return Age.byId(stage).map(a -> a == Age.ARCHAIC || ctx.village().stage().atLeast(a)).orElse(true);
    }

    /** 这一刻能做的工作站：方块工作站与 anchor: 虚拟工作站来自缓存，point: 虚拟工作站来自采集点。 */
    static List<Workstations.Station> stations(VillageContext ctx) {
        List<Workstations.Station> out = new ArrayList<>(ctx.rt().stations());
        Set<String> pointKinds = new HashSet<>();
        for (VillageRecipe r : ctx.data().recipes().values()) {
            if (r.station().startsWith(Workstations.POINT)) {
                pointKinds.add(r.station().substring(Workstations.POINT.length()));
            }
        }
        for (ResourcePoint p : ctx.state().points.values()) {
            if (pointKinds.contains(p.kind())) {
                out.add(new Workstations.Station(p.pos(), VillageContext.block(p.pos()), Workstations.POINT + p.kind()));
            }
        }
        return out;
    }

    static String ref(VillageRecipe r, BlockPos block) {
        return r.id() + "|" + block.asLong();
    }

    static BlockPos refBlock(String ref) {
        return BlockPos.of(Long.parseLong(ref.substring(ref.lastIndexOf('|') + 1)));
    }

    static String refRecipe(String ref) {
        return ref.substring(0, ref.lastIndexOf('|'));
    }

    /**
     * 在工作站按配方加工：输入与燃料在派单时托管，最后一段结束时产物一次入库（无人值守段改为放进容器）。
     * 主产物已经够配方的库存目标就不做；挑「最缺」的那条配方。
     */
    static final class Craft implements JobType {
        @Override
        public String id() {
            return CRAFT;
        }

        @Override
        public List<Job> candidates(VillageContext ctx) {
            if (!working(ctx)) {
                return List.of();
            }
            List<Job> out = new ArrayList<>();
            VillageStock stock = ctx.state().stock;
            for (Workstations.Station st : stations(ctx)) {
                if (!ctx.loaded(st.block()) || ctx.state().vessels.containsKey(st.block())) {
                    continue;
                }
                VillageRecipe best = null;
                double bestW = -1;
                for (VillageRecipe r : ctx.data().recipes().values()) {
                    if (!r.station().equals(st.id()) || !stageOpen(ctx, r.stage())
                            || (r.stockTarget() > 0 && stock.count(r.mainOutput()) >= r.stockTarget())
                            || r.inputs().entrySet().stream().anyMatch(e -> stock.count(e.getKey()) < e.getValue())
                            || ctx.data().fuel().pick(stock, r.fuel()).isEmpty()
                            || stock.free() + sum(r.inputs()) < sum(r.outputs())) {
                        continue;
                    }
                    double w = need(stock, r.outputs(), r.weight());
                    if (w > bestW) {
                        bestW = w;
                        best = r;
                    }
                }
                if (best == null) {
                    continue;
                }
                List<JobStep> steps = new ArrayList<>();
                steps.add(JobStep.move());
                if (best.unattended()) {
                    steps.add(JobStep.act("haul_put", 20));
                } else {
                    best.stages().forEach(s -> steps.add(JobStep.act(s.anim(), s.ticks())));
                }
                steps.add(JobStep.settle());
                out.add(new Job(ctx.nextJobId(), CRAFT, st.stand(), st.key(), ref(best, st.block()), steps, best.inputs(),
                        best.outputs(), best.professions(), Priority.SHORTAGE, bestW, best.outdoor(),
                        Job.DEFAULT_TIMEOUT + best.totalTicks()));
            }
            return out;
        }

        @Override
        public Optional<Map<String, Integer>> escrow(VillageContext ctx, Job job) {
            VillageRecipe r = ctx.data().recipes().get(refRecipe(job.ref()));
            if (r == null) {
                return Optional.empty();
            }
            Optional<Map<String, Integer>> fuel = ctx.data().fuel().pick(ctx.state().stock, r.fuel());
            if (fuel.isEmpty()) {
                return Optional.empty();
            }
            Map<String, Integer> all = new LinkedHashMap<>(r.inputs());
            fuel.get().forEach((k, v) -> all.merge(k, v, Integer::sum));
            return Optional.of(all);
        }

        @Override
        public boolean settle(VillageContext ctx, ResidentEntity resident, Job job) {
            VillageRecipe r = ctx.data().recipes().get(refRecipe(job.ref()));
            BlockPos block = refBlock(job.ref());
            if (r == null || !Workstations.present(ctx, block, r.station())) {
                return false;
            }
            VillageStock stock = ctx.state().stock;
            if (r.unattended()) {
                VillageEconomy.Vessel v = new VillageEconomy.Vessel(block.immutable(), r.id(), ctx.today());
                stock.putEscrowUnchecked(v.escrowId(), stock.consume(job.escrowId()));
                ctx.state().vessels.put(v.pos(), v);
                return true;
            }
            stock.consume(job.escrowId());
            r.outputs().forEach(stock::deposit);
            return true;
        }
    }

    /** 无人值守段到期后取出入库（任何居民都能做）。 */
    static final class Collect implements JobType {
        @Override
        public String id() {
            return COLLECT;
        }

        @Override
        public List<Job> candidates(VillageContext ctx) {
            if (!working(ctx)) {
                return List.of();
            }
            List<Job> out = new ArrayList<>();
            for (VillageEconomy.Vessel v : ctx.state().vessels.values()) {
                VillageRecipe r = ctx.data().recipes().get(v.recipe());
                if (r == null || ctx.today() < v.startDay() + r.unattendedDays() || !ctx.loaded(v.pos())
                        || ctx.state().stock.free() < sum(r.outputs())) {
                    continue;
                }
                out.add(new Job(ctx.nextJobId(), COLLECT, VillageContext.pos(v.pos()), "vessel:" + v.pos().asLong(),
                        v.recipe(), Job.walkActSettle("haul_pick", 20), Map.of(), r.outputs(), Set.of(), Priority.SHORTAGE,
                        2, false, Job.DEFAULT_TIMEOUT));
            }
            return out;
        }

        @Override
        public boolean settle(VillageContext ctx, ResidentEntity resident, Job job) {
            VillageEconomy.Vessel v = ctx.state().vessels.get(VillageContext.block(job.target()));
            VillageRecipe r = v == null ? null : ctx.data().recipes().get(v.recipe());
            if (r == null || ctx.today() < v.startDay() + r.unattendedDays()) {
                return false;
            }
            ctx.state().vessels.remove(v.pos());
            ctx.state().stock.consume(v.escrowId());
            r.outputs().forEach(ctx.state().stock::deposit);
            return true;
        }
    }

    // ───────────── 作物 ─────────────

    static List<BlockPos> decode(String ref) {
        List<BlockPos> out = new ArrayList<>();
        if (ref == null || ref.isEmpty()) {
            return out;
        }
        for (String s : ref.split(";")) {
            out.add(BlockPos.of(Long.parseLong(s)));
        }
        return out;
    }

    static boolean mature(BlockState s) {
        return s.getBlock() instanceof CropBlock c && c.isMaxAge(s);
    }

    /**
     * 收登记农田里成熟的作物：每单最多 {@code batch} 格。割完那一刻作物回到幼苗、掉落挂在本单上；
     * 有打谷场的作物（希腊小麦）接着搬到打谷场扬场，扬完才入库并扣掉补种的种子（V3-希腊-居民 §3.1）；半路打断就作废。
     * 同一成熟状态只结算一次（11 号 §6）。
     */
    static final class HarvestCrop implements JobType {
        @Override
        public String id() {
            return HARVEST;
        }

        @Override
        public List<Job> candidates(VillageContext ctx) {
            if (!working(ctx)) {
                return List.of();
            }
            Set<Long> claimed = new HashSet<>();
            ctx.rt().activeJobs().stream().filter(j -> j.type().equals(HARVEST))
                    .forEach(j -> decode(cropsOf(j.ref())).forEach(b -> claimed.add(b.asLong())));
            boolean hungry = ctx.data().rations().belowFloor(ctx.state().stock);
            List<Job> out = new ArrayList<>();
            for (CivVillageData.CropKind k : ctx.data().crops().values()) {
                List<BlockPos> batch = new ArrayList<>();
                for (Pos p : ctx.anchors(k.id())) {
                    BlockPos b = VillageContext.block(p);
                    if (claimed.contains(b.asLong()) || !ctx.loaded(b) || !mature(ctx.level().getBlockState(b))) {
                        continue;
                    }
                    batch.add(b);
                    if (batch.size() >= k.batch()) {
                        out.add(job(ctx, k, batch, hungry));
                        batch = new ArrayList<>();
                    }
                }
                if (!batch.isEmpty()) {
                    out.add(job(ctx, k, batch, hungry));
                }
            }
            return out;
        }

        /** ref = 作物种类 + "#" + 坐标串。 */
        static String cropsOf(String ref) {
            int i = ref.indexOf('#');
            return i < 0 ? ref : ref.substring(i + 1);
        }

        static String kindOf(String ref) {
            int i = ref.indexOf('#');
            return i < 0 ? "" : ref.substring(0, i);
        }

        private static Job job(VillageContext ctx, CivVillageData.CropKind k, List<BlockPos> batch, boolean hungry) {
            StringBuilder ref = new StringBuilder(k.id()).append('#');
            for (int i = 0; i < batch.size(); i++) {
                if (i > 0) {
                    ref.append(';');
                }
                ref.append(batch.get(i).asLong());
            }
            Pos first = VillageContext.pos(batch.get(0));
            List<JobStep> steps = new ArrayList<>();
            steps.add(JobStep.move());
            steps.add(JobStep.act(k.anim(), k.ticksPerCrop() * batch.size()));
            Optional<Pos> floor = k.threshed() ? nearest(ctx.anchors(k.thresh()), first) : Optional.empty();
            if (floor.isPresent()) {
                steps.add(JobStep.moveTo(floor.get(), "basket"));
                steps.add(JobStep.act(k.threshAnim(), k.threshTicks()));
            } else {
                storage(ctx, first).ifPresent(s -> steps.add(JobStep.moveTo(s, "basket")));
            }
            steps.add(JobStep.settle());
            return new Job(ctx.nextJobId(), HARVEST, first, "crop:" + batch.get(0).asLong(), ref.toString(), steps, Map.of(),
                    k.expected(), k.professions(), hungry ? Priority.FOOD_SAFETY : Priority.SHORTAGE, 1.5, true,
                    Job.DEFAULT_TIMEOUT + k.threshTicks());
        }

        static Optional<Pos> nearest(List<Pos> ps, Pos from) {
            return ps.stream().min(Comparator.comparingLong(p -> (long) (p.x() - from.x()) * (p.x() - from.x())
                    + (long) (p.z() - from.z()) * (p.z() - from.z())));
        }

        /** 割：第 1 步动作做完时，逐格确认仍然成熟才割，掉落挂在本单上，作物回到幼苗。 */
        @Override
        public void onActDone(VillageContext ctx, ResidentEntity resident, Job job, int step) {
            if (step != 1) {
                return;
            }
            CivVillageData.CropKind kind = ctx.data().crops().get(kindOf(job.ref()));
            Map<String, Integer> got = new LinkedHashMap<>();
            int harvested = 0;
            for (BlockPos b : decode(cropsOf(job.ref()))) {
                if (!ctx.loaded(b)) {
                    continue;
                }
                BlockState s = ctx.level().getBlockState(b);
                if (!mature(s)) {
                    continue;
                }
                for (ItemStack drop : Block.getDrops(s, ctx.level(), b, null)) {
                    got.merge(BuiltInRegistries.ITEM.getKey(drop.getItem()).toString(), drop.getCount(), Integer::sum);
                }
                ctx.level().setBlock(b, ((CropBlock) s.getBlock()).getStateForAge(0), Block.UPDATE_ALL);
                harvested++;
            }
            if (harvested == 0) {
                return;
            }
            if (kind != null && !kind.replant().isEmpty()) {
                int seeds = got.getOrDefault(kind.replant(), 0) - harvested;
                if (seeds > 0) {
                    got.put(kind.replant(), seeds);
                } else {
                    got.remove(kind.replant());
                }
            }
            addPending(ctx, job, got);
        }

        @Override
        public boolean settle(VillageContext ctx, ResidentEntity resident, Job job) {
            return depositPending(ctx, job);
        }
    }

    // ───────────── 作息 ─────────────

    /**
     * 傍晚到村中心附近聚会闲聊、坐着歇脚（装饰活动，有生产需要时让位）。下雨时改去避雨处（文明数据 {@code life.meet.shelter}，
     * 希腊是柱廊铺面），没有避雨处就不聚（T19 §1.10 J3）。
     */
    public static final class Leisure implements JobType {
        @Override
        public String id() {
            return LEISURE;
        }

        /** 聚会地点：平时是聚会锚点（没有就是钟），下雨时是最近的避雨锚点；下雨又没有避雨处为 empty。 */
        public static Optional<BlockPos> place(VillageContext ctx, boolean raining) {
            io.github.happysnappingturtle.mythicage.core.village.life.LifeData.Meet m = ctx.life().meet();
            BlockPos c = ctx.center();
            if (raining) {
                return m.shelter().isEmpty() ? Optional.empty() : ctx.anchors(m.shelter()).stream().map(VillageContext::block)
                        .min(Comparator.comparingDouble(p -> p.distSqr(c)));
            }
            if (!m.at().isEmpty()) {
                return Optional.of(ctx.anchors(m.at()).stream().map(VillageContext::block)
                        .min(Comparator.comparingDouble(p -> p.distSqr(c))).orElse(c));
            }
            return Optional.of(c);
        }

        @Override
        public List<Job> candidates(VillageContext ctx) {
            if (!Schedule.EVENING.equals(ctx.segment())) {
                return List.of();
            }
            return jobs(ctx, ctx.raining());
        }

        public static List<Job> jobs(VillageContext ctx, boolean raining) {
            Optional<BlockPos> at = place(ctx, raining);
            if (at.isEmpty()) {
                return List.of();
            }
            BlockPos c = at.get();
            int spread = raining ? 2 : 3;
            List<Job> out = new ArrayList<>();
            for (ResidentEntity r : ctx.residents()) {
                if (!r.idle() || r.distanceToSqr(c.getX() + 0.5, c.getY(), c.getZ() + 0.5) < 36) {
                    continue;
                }
                int dx = Math.floorMod(r.getUUID().hashCode(), 2 * spread + 1) - spread;
                int dz = Math.floorMod(r.getUUID().hashCode() >> 8, 2 * spread + 1) - spread;
                // 三人里有一人坐着歇脚，其余站着聊
                String anim = Math.floorMod(r.getUUID().hashCode() >> 4, 3) == 0 ? "sit" : "chat";
                out.add(new Job(ctx.nextJobId(), LEISURE, VillageContext.pos(c.offset(dx, 0, dz)), "leisure:" + r.getUUID(),
                        "", List.of(JobStep.move(), JobStep.act(anim, 200)), Map.of(), Map.of(), Set.of(), Priority.DECOR,
                        0, false, 600, r.getUUID()));
            }
            return out;
        }

        @Override
        public boolean settle(VillageContext ctx, ResidentEntity resident, Job job) {
            return true;
        }
    }

    /** 晚饭、睡觉、黎明段：不在床位旁就走回去（只能本人做）。 */
    static final class GoHome implements JobType {
        @Override
        public String id() {
            return GO_HOME;
        }

        @Override
        public List<Job> candidates(VillageContext ctx) {
            String seg = ctx.segment();
            if (!Schedule.SUPPER.equals(seg) && !Schedule.SLEEP.equals(seg) && !Schedule.DAWN.equals(seg)) {
                return List.of();
            }
            List<Job> out = new ArrayList<>();
            for (ResidentEntity r : ctx.residents()) {
                if (!r.idle() || r.home() == null || r.reached(r.home())) {
                    continue;
                }
                out.add(new Job(ctx.nextJobId(), GO_HOME, VillageContext.pos(r.home()), "home:" + r.getUUID(), "",
                        List.of(JobStep.move(), JobStep.settle()), Map.of(), Map.of(), Set.of(), Priority.ROUTINE, 0, false,
                        Job.DEFAULT_TIMEOUT, r.getUUID()));
            }
            return out;
        }

        @Override
        public boolean settle(VillageContext ctx, ResidentEntity resident, Job job) {
            return true;
        }
    }
}
