package io.github.happysnappingturtle.mythicage.core.village.soldier;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 兵装的耐久、退还与修复（T20 设计文档 §4.5，01 号 §0.5）：纯函数。
 */
public final class KitRules {
    private KitRules() {
    }

    /** 退回村库时变成什么。 */
    public enum Returned {
        /** 原样退回（完整的退完整、简易的退简易）。 */
        SAME,
        /** 退回破损兵装（只有完整兵装）。 */
        BROKEN,
        /** 报废，不入库（简易兵装损坏，D4）。 */
        SCRAP,
        /** 身上没有兵装。 */
        NOTHING
    }

    /** 耐久用完了没有（被命中 {@code wear} 次，上限 {@code durability}）。 */
    public static boolean exhausted(int wear, int durability) {
        return wear >= durability;
    }

    /** 降回或换装时退回什么：磨损 &lt; 一半原样退回，≥ 一半完整的退破损、简易的报废。 */
    public static Returned onReturn(SoldierProfile.Tier tier, int wear, int durability) {
        if (tier == SoldierProfile.Tier.NONE) {
            return Returned.NOTHING;
        }
        if (wear * 2 < durability) {
            return Returned.SAME;
        }
        return tier == SoldierProfile.Tier.FULL ? Returned.BROKEN : Returned.SCRAP;
    }

    /** 阵亡：完整兵装退破损，简易报废（01 号 §0.5「阵亡后退回村库成为破损件」，D4）。 */
    public static Returned onDeath(SoldierProfile.Tier tier) {
        return switch (tier) {
            case FULL -> Returned.BROKEN;
            case SIMPLE -> Returned.SCRAP;
            case NONE -> Returned.NOTHING;
        };
    }

    /** 修复消耗原材料的一半（各项向上取整，01 号 §0.5）。 */
    public static Map<String, Integer> repairCost(Map<String, Integer> fullCost) {
        Map<String, Integer> out = new LinkedHashMap<>();
        fullCost.forEach((k, v) -> {
            if (v > 0) {
                out.put(k, (v + 1) / 2);
            }
        });
        return out;
    }
}
