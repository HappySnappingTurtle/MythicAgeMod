package io.github.happysnappingturtle.mythicage.core.growth;

/**
 * 生长速率换算（T6b 设计文档 §10.3）。原版默认每个区块段每刻随机抽 3 个方块做随机刻，
 * 一个方块平均每 4096 / 3 ≈ 1365 刻得到一次随机刻。配置里写的是「期望时长」，这里换算成每次随机刻的推进几率。
 */
public final class GrowthRules {
    /** 默认 randomTickSpeed = 3 时，单个方块两次随机刻之间的平均间隔（刻）。 */
    public static final double TICKS_PER_RANDOM_TICK = 4096.0 / 3.0;

    private GrowthRules() {
    }

    /**
     * @param expectedTicksPerStage 每个阶段的期望时长（刻），必须为正
     * @return 每次随机刻推进 1 阶的几率，范围 (0, 1]
     */
    public static double chancePerRandomTick(long expectedTicksPerStage) {
        if (expectedTicksPerStage <= 0) {
            throw new IllegalArgumentException("期望时长必须为正：" + expectedTicksPerStage);
        }
        return Math.min(1.0, TICKS_PER_RANDOM_TICK / expectedTicksPerStage);
    }
}
