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
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 框架自带的工作单类型（T19 设计文档 §5.1）：采集点、加工（含无人值守段与取出）、收作物、傍晚聚会、回家。
 * 各类型只看登记的资源（建筑锚点），不扫描周围的方块，不碰玩家的田和房子（11 号 §6、§9）。
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

    // ───────────── 采集点 ─────────────

    /** 从登记采集点取料：余量减少、产物入库；NPC 与玩家共用同一份余量。 */
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
                        Job.walkActSettle(k.anim(), k.ticks()), Map.of(), k.outputs(), k.professions(), Priority.SHORTAGE,
                        need(ctx.state().stock, k.outputs(), 1), k.outdoor(), Job.DEFAULT_TIMEOUT));
            }
            return out;
        }

        @Override
        public boolean settle(VillageContext ctx, ResidentEntity resident, Job job) {
            ResourcePoint p = ctx.state().points.get(job.ref());
            CivVillageData.PointKind k = p == null ? null : ctx.data().points().get(p.kind());
            if (k == null) {
                return false;
            }
            p = p.recover(ctx.today());
            if (p.remaining() < k.take()) {
                ctx.state().points.put(p.id(), p);
                return false;
            }
            ctx.state().points.put(p.id(), p.take(k.take()));
            k.outputs().forEach(ctx.state().stock::deposit);
            PointVisuals.update(ctx, ctx.state().points.get(p.id()));
            return true;
        }
    }

    // ───────────── 加工 ─────────────

    /** 工作站方块 id。 */
    static String stationId(BlockState s) {
        return BuiltInRegistries.BLOCK.getKey(s.getBlock()).toString();
    }

    static boolean stageOpen(VillageContext ctx, String stage) {
        return Age.byId(stage).map(a -> ctx.village().stage().atLeast(a)).orElse(true);
    }

    /** 在工作站按配方加工：输入与燃料在派单时托管，最后一段结束时产物一次入库（无人值守段改为放进酒瓮）。 */
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
            for (Pos sp : ctx.anchors(STATION_ANCHOR)) {
                BlockPos bp = VillageContext.block(sp);
                if (!ctx.loaded(bp) || ctx.state().vessels.containsKey(bp)) {
                    continue;
                }
                String station = stationId(ctx.level().getBlockState(bp));
                VillageRecipe best = null;
                double bestW = -1;
                for (VillageRecipe r : ctx.data().recipes().values()) {
                    if (!r.station().equals(station) || !stageOpen(ctx, r.stage())
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
                out.add(new Job(ctx.nextJobId(), CRAFT, sp, "station:" + bp.asLong(), best.id(), steps, best.inputs(),
                        best.outputs(), best.professions(), Priority.SHORTAGE, bestW, best.outdoor(),
                        Job.DEFAULT_TIMEOUT + best.totalTicks()));
            }
            return out;
        }

        @Override
        public Optional<Map<String, Integer>> escrow(VillageContext ctx, Job job) {
            VillageRecipe r = ctx.data().recipes().get(job.ref());
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
            VillageRecipe r = ctx.data().recipes().get(job.ref());
            BlockPos bp = VillageContext.block(job.target());
            if (r == null || !ctx.loaded(bp) || !stationId(ctx.level().getBlockState(bp)).equals(r.station())) {
                return false;
            }
            VillageStock stock = ctx.state().stock;
            if (r.unattended()) {
                VillageEconomy.Vessel v = new VillageEconomy.Vessel(bp.immutable(), r.id(), ctx.today());
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
     * 收登记农田里成熟的作物：每单最多 {@code batch} 格，结算时逐格确认仍然成熟才收，掉落入库，扣掉补种的种子，作物回到幼苗。
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
                    .forEach(j -> decode(j.ref()).forEach(b -> claimed.add(b.asLong())));
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

        private static Job job(VillageContext ctx, CivVillageData.CropKind k, List<BlockPos> batch, boolean hungry) {
            StringBuilder ref = new StringBuilder();
            for (BlockPos b : batch) {
                if (!ref.isEmpty()) {
                    ref.append(';');
                }
                ref.append(b.asLong());
            }
            return new Job(ctx.nextJobId(), HARVEST, VillageContext.pos(batch.get(0)), "crop:" + batch.get(0).asLong(),
                    ref.toString(), Job.walkActSettle(k.anim(), k.ticksPerCrop() * batch.size()), Map.of(), k.expected(),
                    k.professions(), hungry ? Priority.FOOD_SAFETY : Priority.SHORTAGE, 1.5, true, Job.DEFAULT_TIMEOUT);
        }

        @Override
        public boolean settle(VillageContext ctx, ResidentEntity resident, Job job) {
            CivVillageData.CropKind kind = null;
            Map<String, Integer> got = new LinkedHashMap<>();
            int harvested = 0;
            for (BlockPos b : decode(job.ref())) {
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
                return false;
            }
            for (CivVillageData.CropKind k : ctx.data().crops().values()) {
                if (job.targetKey().startsWith("crop:") && ctx.anchors(k.id()).contains(job.target())) {
                    kind = k;
                    break;
                }
            }
            if (kind != null && !kind.replant().isEmpty()) {
                int seeds = got.getOrDefault(kind.replant(), 0) - harvested;
                if (seeds > 0) {
                    got.put(kind.replant(), seeds);
                } else {
                    got.remove(kind.replant());
                }
            }
            got.forEach(ctx.state().stock::deposit);
            return true;
        }
    }

    // ───────────── 作息 ─────────────

    /** 傍晚到村中心附近聚会闲聊（装饰活动，有生产需要时让位）。 */
    static final class Leisure implements JobType {
        @Override
        public String id() {
            return LEISURE;
        }

        @Override
        public List<Job> candidates(VillageContext ctx) {
            if (!Schedule.EVENING.equals(ctx.segment())) {
                return List.of();
            }
            List<Job> out = new ArrayList<>();
            BlockPos c = ctx.center();
            for (ResidentEntity r : ctx.residents()) {
                if (!r.idle() || r.distanceToSqr(c.getX() + 0.5, c.getY(), c.getZ() + 0.5) < 36) {
                    continue;
                }
                int dx = Math.floorMod(r.getUUID().hashCode(), 7) - 3;
                int dz = Math.floorMod(r.getUUID().hashCode() >> 8, 7) - 3;
                out.add(new Job(ctx.nextJobId(), LEISURE, VillageContext.pos(c.offset(dx, 0, dz)), "leisure:" + r.getUUID(),
                        "", List.of(JobStep.move(), JobStep.act("chat", 200)), Map.of(), Map.of(), Set.of(), Priority.DECOR,
                        0, true, 600, r.getUUID()));
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
