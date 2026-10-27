package io.github.happysnappingturtle.mythicage.core.conduct;

import java.util.ArrayList;
import java.util.List;

/**
 * 善恶值的显示规则（T24 设计文档 §3.2）：只给「善恶与贡献」界面用的五档名，按配置范围的比例划分，
 * <b>不参与任何游戏判定</b>（判定由消费方用 {@link ConductRules#band} 给自己的阈值）。
 */
public final class ConductDisplay {
    /** 最近事件最多显示几条。 */
    public static final int RECENT = 10;

    private ConductDisplay() {
    }

    public enum Tier {
        INFAMOUS, DISREPUTABLE, ORDINARY, UPRIGHT, VIRTUOUS;

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /**
     * 正半边：≥ max 的一半为仁德，≥ max 的 15%（至少 1）为正直；负半边对称；其余为平常。
     * min 必须 &lt; 0 &lt; max（配置已保证）。
     */
    public static Tier tier(int score, int min, int max) {
        if (score > 0) {
            if (score >= Math.max(1, (max + 1) / 2)) {
                return Tier.VIRTUOUS;
            }
            return score >= threshold(max) ? Tier.UPRIGHT : Tier.ORDINARY;
        }
        if (score < 0) {
            int s = -score;
            int m = -min;
            if (s >= Math.max(1, (m + 1) / 2)) {
                return Tier.INFAMOUS;
            }
            return s >= threshold(m) ? Tier.DISREPUTABLE : Tier.ORDINARY;
        }
        return Tier.ORDINARY;
    }

    private static int threshold(int extent) {
        return Math.max(1, (int) Math.ceil(extent * 0.15));
    }

    /** 从旧到新的列表 → 最新的 n 条，新的在前。 */
    public static <T> List<T> newestFirst(List<T> oldToNew, int n) {
        List<T> out = new ArrayList<>();
        for (int i = oldToNew.size() - 1; i >= 0 && out.size() < n; i--) {
            out.add(oldToNew.get(i));
        }
        return List.copyOf(out);
    }
}
