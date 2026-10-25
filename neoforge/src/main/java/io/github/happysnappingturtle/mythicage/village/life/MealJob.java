package io.github.happysnappingturtle.mythicage.village.life;

import io.github.happysnappingturtle.mythicage.core.village.life.LifeData;
import io.github.happysnappingturtle.mythicage.core.village.time.Schedule;
import io.github.happysnappingturtle.mythicage.core.village.work.Job;
import io.github.happysnappingturtle.mythicage.core.village.work.JobStep;
import io.github.happysnappingturtle.mythicage.core.village.work.Priority;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import io.github.happysnappingturtle.mythicage.village.work.JobType;
import io.github.happysnappingturtle.mythicage.village.work.VillageContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 三餐（T19 设计文档 §1.10 J1，V3-希腊-居民 §2.1、§5.2）：黎明与晚饭在家——先向灶台奠酒、再吃；午饭在原地坐下吃。
 * 纯表现：不扣物品（晚饭的口粮由调度器统一扣）、不产恩宠。不在家的人先由「回家」把人送回去。每人每餐一次。
 */
final class MealJob implements JobType {
    @Override
    public String id() {
        return VillageLife.MEAL;
    }

    @Override
    public List<Job> candidates(VillageContext ctx) {
        LifeData.Meals meals = ctx.life().meals();
        String seg = ctx.segment();
        List<LifeData.Act> acts;
        boolean atHome = true;
        if (Schedule.DAWN.equals(seg)) {
            acts = meals.dawn();
        } else if (Schedule.SUPPER.equals(seg)) {
            acts = meals.supper();
        } else if (Schedule.LUNCH.equals(seg)) {
            acts = meals.lunch();
            atHome = false;
        } else {
            return List.of();
        }
        if (acts.isEmpty()) {
            return List.of();
        }
        List<Job> out = new ArrayList<>();
        for (ResidentEntity r : VillageLife.idleMembers(ctx)) {
            if (ctx.rt().done(key(seg, r), ctx.today())) {
                continue;
            }
            if (atHome && (r.home() == null || !r.reached(r.home()))) {
                continue;
            }
            List<JobStep> steps = new ArrayList<>();
            int total = 0;
            for (LifeData.Act a : acts) {
                steps.add(JobStep.act(a.anim(), a.ticks()));
                total += a.ticks();
            }
            steps.add(JobStep.settle());
            out.add(new Job(ctx.nextJobId(), id(), VillageLife.pos(atHome ? r.home() : r.blockPosition()), key(seg, r), seg, steps,
                    Map.of(), Map.of(), Set.of(), Priority.ROUTINE, 1, false, total + 200, r.getUUID()));
        }
        return out;
    }

    static String key(String seg, ResidentEntity r) {
        return "meal:" + seg + ":" + r.getUUID();
    }

    @Override
    public boolean settle(VillageContext ctx, ResidentEntity resident, Job job) {
        ctx.rt().markDone(key(job.ref(), resident), ctx.today());
        return true;
    }
}
