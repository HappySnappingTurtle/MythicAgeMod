package io.github.happysnappingturtle.mythicage.core.relic;

/**
 * 圣物 ⑫ 赫菲斯托斯之砧的佩戴效果（RE-GR-12，T25a §7）：铁砧修理与附魔合并的经验消耗 −30%（效果组 {@code anvil_xp_cost}）。
 * 「过于昂贵」按折前价格判：原版生存模式 ≥ {@value #TOO_EXPENSIVE} 级就不让做，折扣不能把 45 级的操作压到 40 以下（跨册 06 边界用例）。
 */
public final class AnvilDiscount {
    public static final int TOO_EXPENSIVE = 40;

    private AnvilDiscount() {
    }

    /** 原版是不是「过于昂贵」。 */
    public static boolean tooExpensive(int originalCost) {
        return originalCost >= TOO_EXPENSIVE;
    }

    /**
     * 折后经验：floor(原价 × (1 − 折扣))，至少 1（原价 0 仍是 0）；原价已经过于昂贵的不折（保持过于昂贵）。
     *
     * @param discount 0–1，同组取最大后的值（0.3 = −30%）
     */
    public static int cost(int originalCost, double discount) {
        if (originalCost <= 0 || discount <= 0 || tooExpensive(originalCost)) {
            return originalCost;
        }
        // 加一点容差：30 × 0.7 在浮点里是 20.999…，不能因此少算 1 级
        int after = (int) Math.floor(originalCost * (1 - Math.min(1, discount)) + 1e-9);
        return Math.max(1, after);
    }
}
