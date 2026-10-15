package io.github.happysnappingturtle.mythicage.core.trade;

import java.util.List;

/**
 * 恩宠价格（T9 设计文档 §3）：最终价 = 基础价 × Π(1 − 折扣)，向上取整，下限 1。
 * 折扣按乘法叠加，所以永远不会降到 0；折扣来源（圣物等）由 T16 提供。
 */
public final class PriceCalculator {
    /** 浮点误差容差：20 × 0.9 这类结果不会因为 18.000000000000004 被进成 19。 */
    private static final double EPSILON = 1e-9;

    private PriceCalculator() {
    }

    /**
     * @param discounts 每项折扣 ∈ [0, 1)；超出范围的项被截断到 [0, 0.95]
     */
    public static long favor(long base, List<Double> discounts) {
        if (base <= 0) {
            throw new IllegalArgumentException("基础价必须为正：" + base);
        }
        double price = base;
        for (double d : discounts) {
            price *= 1 - Math.max(0, Math.min(0.95, d));
        }
        return Math.max(1, (long) Math.ceil(price - EPSILON));
    }
}
