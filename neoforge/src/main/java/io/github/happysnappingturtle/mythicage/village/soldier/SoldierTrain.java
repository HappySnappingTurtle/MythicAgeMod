package io.github.happysnappingturtle.mythicage.village.soldier;

import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.core.village.time.Schedule;
import io.github.happysnappingturtle.mythicage.core.village.work.Job;
import io.github.happysnappingturtle.mythicage.core.village.work.Priority;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import io.github.happysnappingturtle.mythicage.village.work.JobType;
import io.github.happysnappingturtle.mythicage.village.work.VillageContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * 训练（T20 设计文档 §9.3，V3-希腊-居民 §7、H10）：村里有训练锚点（希腊：英雄起的体育场、角力场）时，不值勤的士兵
 * 在工作段的一半时间里去练刺、举盾（每 2400 刻轮换一次：练一段、帮一段农务）。装饰活动的优先级：缺粮时不排。
 */
public final class SoldierTrain implements JobType {
    public static final String ID = "soldier_train";
    public static final int TICKS = 400;
    /** 一段的长度（刻）：偶数段可以训练，奇数段帮农务。 */
    public static final int WINDOW = 2400;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public List<Job> candidates(VillageContext ctx) {
        String anchor = ctx.data().soldiers().training();
        if (anchor.isEmpty() || !Schedule.WORK.equals(ctx.segment()) || (ctx.timeOfDay() / WINDOW) % 2 != 0) {
            return List.of();
        }
        List<Pos> at = ctx.anchors(anchor);
        if (at.isEmpty()) {
            return List.of();
        }
        List<Job> out = new ArrayList<>();
        for (int i = 0; i < at.size(); i++) {
            out.add(new Job(ctx.nextJobId(), ID, at.get(i), "train:" + i, "", Job.walkActSettle("train", TICKS), Map.of(), Map.of(),
                    new HashSet<>(ctx.data().soldiers().order()), Priority.DECOR, 0.5, true, Job.DEFAULT_TIMEOUT));
        }
        return out;
    }

    @Override
    public boolean settle(VillageContext ctx, ResidentEntity resident, Job job) {
        return true;
    }
}
