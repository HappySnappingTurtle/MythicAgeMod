package io.github.happysnappingturtle.mythicage.village.life;

import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.core.village.life.LifeData;
import io.github.happysnappingturtle.mythicage.core.village.life.LifeRules;
import io.github.happysnappingturtle.mythicage.core.village.work.Job;
import io.github.happysnappingturtle.mythicage.core.village.work.JobStep;
import io.github.happysnappingturtle.mythicage.core.village.work.Priority;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import io.github.happysnappingturtle.mythicage.village.work.JobType;
import io.github.happysnappingturtle.mythicage.village.work.VillageContext;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 装饰性活动（T19 设计文档 §1.10 J2）：打水、集市闲逛、圣橡躬身、推船、扫墓、问卜前沐浴……全由数据描述。
 * 优先级「装饰」：有生产需要时让位；缺粮时不排。每段派几人按村庄、日子、时段派生（{@link LifeRules}），每人每段一次。
 * 打水：排队的人先闲聊（第 k 个等 40k 刻），装满水罐顶在头上走回家放下。
 */
final class LifeActivityJob implements JobType {
    @Override
    public String id() {
        return VillageLife.LIFE;
    }

    @Override
    public List<Job> candidates(VillageContext ctx) {
        int tod = ctx.timeOfDay();
        List<Job> out = new ArrayList<>();
        List<ResidentEntity> free = null;
        for (LifeData.Activity a : ctx.life().activities()) {
            if (!VillageLife.applies(ctx, a.patron(), a.special(), a.minAge())) {
                continue;
            }
            Optional<LifeData.Window> w = a.windowAt(tod);
            if (w.isEmpty() || tod > w.get().to() - a.act().ticks() - 200) {
                continue;
            }
            List<Pos> anchors = ctx.anchors(a.anchor());
            if (anchors.isEmpty()) {
                continue;
            }
            String prefix = "life:" + a.id() + ":" + w.get().from() + ":";
            boolean everyone = a.specialMax() > 0 && !a.special().isEmpty() && !a.special().startsWith("!");
            int target = everyone ? a.specialMax()
                    : LifeRules.people(a, ctx.state().roster.size(), anchors.size(),
                    LifeRules.seed(ctx.village().id(), ctx.today(), w.get().from(), a.id()));
            int already = ctx.rt().countDone(prefix, ctx.today());
            if (already >= target) {
                continue;
            }
            if (free == null) {
                free = VillageLife.idleMembers(ctx);
            }
            List<ResidentEntity> pool = new ArrayList<>();
            for (ResidentEntity r : free) {
                if ((a.professions().isEmpty() || a.professions().contains(r.profession()))
                        && !ctx.rt().done(prefix + r.getUUID(), ctx.today())) {
                    pool.add(r);
                }
            }
            BlockPos first = VillageContext.block(anchors.get(0));
            boolean female = a.prefer().equals("female");
            pool.sort(Comparator.<ResidentEntity>comparingInt(r -> female && !r.female() ? 1 : 0)
                    .thenComparingDouble(r -> r.distanceToSqr(first.getX(), first.getY(), first.getZ())));
            for (int i = 0; i < pool.size() && already + i < target; i++) {
                ResidentEntity r = pool.get(i);
                int queue = already + i;
                Pos at = anchors.stream().min(Comparator.comparingDouble(p ->
                        r.distanceToSqr(p.x(), p.y(), p.z()))).orElse(anchors.get(0));
                List<JobStep> steps = new ArrayList<>();
                steps.add(JobStep.moveTo(at, a.carry()));
                if (!a.queueAct().isEmpty() && queue > 0) {
                    steps.add(JobStep.act(a.queueAct(), 40 * queue));
                }
                steps.add(JobStep.act(a.act().anim(), a.act().ticks()));
                if (!a.carryHome().isEmpty() && r.home() != null) {
                    steps.add(JobStep.moveTo(VillageContext.pos(r.home()), a.carryHome()));
                    steps.add(JobStep.act("haul_put", 20));
                }
                steps.add(JobStep.settle());
                out.add(new Job(ctx.nextJobId(), id(), at, prefix + r.getUUID(), prefix, steps, Map.of(), Map.of(), Set.of(),
                        Priority.DECOR, 1, false, 2400, r.getUUID()));
                free.remove(r);
            }
        }
        return out;
    }

    /** 派出去就算这一段做过了（走不到、被打断也不再补派——纯表现）。 */
    @Override
    public Optional<Map<String, Integer>> escrow(VillageContext ctx, Job job) {
        ctx.rt().markDone(job.targetKey(), ctx.today());
        return Optional.of(Map.of());
    }

    @Override
    public boolean settle(VillageContext ctx, ResidentEntity resident, Job job) {
        return true;
    }
}
