package io.github.happysnappingturtle.mythicage.core.village.gen;

import java.util.ArrayList;
import java.util.List;

/** 规划校验（V0 §3.8）：出界、重叠、缺件。泡水与悬空在放置每座建筑时就判断（{@link PlanBuilder}）。 */
public final class PlanValidator {
    /** 原版结构范围上限（JigsawStructure.MAX_TOTAL_STRUCTURE_RANGE）。 */
    public static final int MAX_RANGE = 128;

    private PlanValidator() {
    }

    public static List<String> validate(VillagePlan plan, List<String> requiredAnchors) {
        List<String> problems = new ArrayList<>();
        Pos c = plan.center();
        long max = (long) MAX_RANGE * MAX_RANGE;
        List<VillagePlan.Placement> bs = plan.buildings();
        for (int i = 0; i < bs.size(); i++) {
            VillagePlan.Placement a = bs.get(i);
            if (a.box().farthestDistSq(c.x(), c.z()) > max) {
                problems.add("出界：" + a.building());
            }
            for (int j = i + 1; j < bs.size(); j++) {
                if (a.box().intersectsXZ(bs.get(j).box())) {
                    problems.add("重叠：" + a.building() + " / " + bs.get(j).building());
                }
            }
        }
        for (String r : requiredAnchors) {
            if (plan.anchors(r).isEmpty()) {
                problems.add("缺件：锚点 " + r);
            }
        }
        return problems;
    }
}
