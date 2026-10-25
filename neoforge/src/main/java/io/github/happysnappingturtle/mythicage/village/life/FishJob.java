package io.github.happysnappingturtle.mythicage.village.life;

import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.core.village.life.LifeData;
import io.github.happysnappingturtle.mythicage.core.village.time.Schedule;
import io.github.happysnappingturtle.mythicage.core.village.work.Job;
import io.github.happysnappingturtle.mythicage.core.village.work.JobStep;
import io.github.happysnappingturtle.mythicage.core.village.work.Priority;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import io.github.happysnappingturtle.mythicage.village.work.BasicJobTypes;
import io.github.happysnappingturtle.mythicage.village.work.JobType;
import io.github.happysnappingturtle.mythicage.village.work.VillageContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 打鱼（T19 设计文档 §1.10 J7，V3-希腊-居民 §5.7，H9）：临海村的劳力在泊位撒网、收网，渔获搬回谷仓入库。
 * 每村每 MC 日最多结算 {@code daily} 次（派单时就把在做的算进去，不会超）；口粮链：缺粮时升为「恢复口粮」。户外活，下雨暂停。
 */
final class FishJob implements JobType {
    static final String COUNT = "fish";

    @Override
    public String id() {
        return VillageLife.FISH;
    }

    @Override
    public List<Job> candidates(VillageContext ctx) {
        LifeData.Fishing f = ctx.life().fishing();
        if (!f.enabled() || !Schedule.WORK.equals(ctx.segment()) || !VillageLife.applies(ctx, f.patron(), "", "")) {
            return List.of();
        }
        List<Pos> berths = ctx.anchors(f.anchor());
        long inProgress = ctx.rt().activeJobs().stream().filter(j -> j.type().equals(id())).count();
        int left = f.daily() - ctx.state().dailyCount(ctx.today(), COUNT) - (int) inProgress;
        if (berths.isEmpty() || left <= 0) {
            return List.of();
        }
        boolean hungry = ctx.data().rations().belowFloor(ctx.state().stock);
        List<Job> out = new ArrayList<>();
        for (int i = 0; i < berths.size() && i < left; i++) {
            Pos at = berths.get(i);
            List<JobStep> steps = new ArrayList<>();
            steps.add(JobStep.move());
            steps.add(JobStep.act("fish_cast", f.castTicks()));
            steps.add(JobStep.act("fish_reel", f.reelTicks()));
            BasicJobTypes.storage(ctx, at).ifPresent(s -> steps.add(JobStep.moveTo(s, "basket")));
            steps.add(JobStep.settle());
            out.add(new Job(ctx.nextJobId(), id(), at, "fish:" + i, "", steps, Map.of(), Map.of(), f.professions(),
                    hungry ? Priority.FOOD_SAFETY : Priority.SHORTAGE, 4, true, 2400));
        }
        return out;
    }

    /** 收完网：渔获挂在本单上，搬回谷仓才入库（途中打断就作废）。 */
    @Override
    public void onActDone(VillageContext ctx, ResidentEntity resident, Job job, int step) {
        if (step != 2) {
            return;
        }
        LifeData.Fishing f = ctx.life().fishing();
        String item = !f.alt().isEmpty() && ctx.level().random.nextDouble() < f.altChance() ? f.alt() : f.item();
        BasicJobTypes.addPending(ctx, job, Map.of(item, 1));
    }

    @Override
    public boolean settle(VillageContext ctx, ResidentEntity resident, Job job) {
        if (!BasicJobTypes.depositPending(ctx, job)) {
            return false;
        }
        ctx.state().addDaily(ctx.today(), COUNT, 1);
        return true;
    }
}
