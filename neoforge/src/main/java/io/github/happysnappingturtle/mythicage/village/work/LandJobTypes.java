package io.github.happysnappingturtle.mythicage.village.work;

import io.github.happysnappingturtle.mythicage.core.village.CivVillageData;
import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.core.village.resource.ResourcePoint;
import io.github.happysnappingturtle.mythicage.core.village.work.Job;
import io.github.happysnappingturtle.mythicage.core.village.work.Priority;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 地里的活（T19 设计文档 §5.1）：采果（{@code pick_fruit}）与伐登记林地（{@code fell_tree}）。
 */
public final class LandJobTypes {
    public static final String PICK = "pick_fruit", FELL = "fell_tree";

    private LandJobTypes() {
    }

    public static void registerAll() {
        JobType.register(new PickFruit());
        JobType.register(new FellTree());
    }

    /** 采果：锚点（果树干、葡萄架前的站位）附近有熟果才派单；结算时逐个确认仍然成熟才采。 */
    static final class PickFruit implements JobType {
        @Override
        public String id() {
            return PICK;
        }

        @Override
        public List<Job> candidates(VillageContext ctx) {
            if (!BasicJobTypes.working(ctx)) {
                return List.of();
            }
            boolean hungry = ctx.data().rations().belowFloor(ctx.state().stock);
            List<Job> out = new ArrayList<>();
            for (CivVillageData.FruitKind k : ctx.data().fruits().values()) {
                FruitHarvester h = FruitHarvester.REGISTRY.get(k.harvester());
                if (h == null || ctx.state().stock.free() < BasicJobTypes.sum(k.expected()) * 2) {
                    continue;
                }
                for (Pos a : ctx.anchors(k.id())) {
                    BlockPos ap = VillageContext.block(a);
                    if (!ctx.loaded(ap) || h.ripe(ctx.level(), ap).isEmpty()) {
                        continue;
                    }
                    out.add(new Job(ctx.nextJobId(), PICK, a, "fruit:" + ap.asLong(), k.id(),
                            BasicJobTypes.withHaul(ctx, a, k.anim(), k.ticks(), k.carry()), Map.of(),
                            k.expected(), k.professions(), hungry ? Priority.FOOD_SAFETY : Priority.SHORTAGE,
                            BasicJobTypes.need(ctx.state().stock, k.expected(), 1.2), true, Job.DEFAULT_TIMEOUT));
                }
            }
            return out;
        }

        /** 动作做完时采：逐个确认仍然成熟，采下的果子挂在本单上，搬回仓库结算时入库。 */
        @Override
        public void onActDone(VillageContext ctx, ResidentEntity resident, Job job, int step) {
            if (step != 1) {
                return;
            }
            CivVillageData.FruitKind k = ctx.data().fruits().get(job.ref());
            FruitHarvester h = k == null ? null : FruitHarvester.REGISTRY.get(k.harvester());
            BlockPos ap = VillageContext.block(job.target());
            if (h == null || !ctx.loaded(ap)) {
                return;
            }
            List<BlockPos> ripe = h.ripe(ctx.level(), ap);
            if (!ripe.isEmpty()) {
                BasicJobTypes.addPending(ctx, job, h.harvest(ctx.level(), ripe));
            }
        }

        @Override
        public boolean settle(VillageContext ctx, ResidentEntity resident, Job job) {
            return BasicJobTypes.depositPending(ctx, job);
        }
    }

    /** 伐登记林地里的一株：砍下相连的原木入库，补种树苗；资源点余量归零，树长回来再恢复。 */
    static final class FellTree implements JobType {
        @Override
        public String id() {
            return FELL;
        }

        @Override
        public List<Job> candidates(VillageContext ctx) {
            CivVillageData.Woodland w = ctx.data().woodland();
            if (w == null || !BasicJobTypes.working(ctx) || ctx.state().stock.free() < w.logsPerTree() * 2) {
                return List.of();
            }
            List<Job> out = new ArrayList<>();
            for (ResourcePoint p : ctx.state().points.values()) {
                if (!p.kind().equals(NaturalResources.WOODLAND) || p.remaining() <= 0) {
                    continue;
                }
                BlockPos b = VillageContext.block(p.pos());
                if (!ctx.loaded(b) || !ctx.level().getBlockState(b).is(BlockTags.LOGS)) {
                    continue;
                }
                out.add(new Job(ctx.nextJobId(), FELL, p.pos(), "point:" + p.id(), p.id(),
                        BasicJobTypes.withHaul(ctx, p.pos(), w.anim(), w.ticks(), "wood"),
                        Map.of(), Map.of(), w.professions(), Priority.SHORTAGE, 0.8, true, Job.DEFAULT_TIMEOUT));
            }
            return out;
        }

        /** 动作做完时伐倒：原木挂在本单上，搬回仓库结算时入库。 */
        @Override
        public void onActDone(VillageContext ctx, ResidentEntity resident, Job job, int step) {
            if (step != 1) {
                return;
            }
            ResourcePoint p = ctx.state().points.get(job.ref());
            if (p == null || p.remaining() <= 0) {
                return;
            }
            Map<String, Integer> logs = NaturalResources.fell(ctx.level(), VillageContext.block(p.pos()));
            ctx.state().points.put(p.id(), p.take(1));
            BasicJobTypes.addPending(ctx, job, logs);
        }

        @Override
        public boolean settle(VillageContext ctx, ResidentEntity resident, Job job) {
            return BasicJobTypes.depositPending(ctx, job);
        }
    }
}
