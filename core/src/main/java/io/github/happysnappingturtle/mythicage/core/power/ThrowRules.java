package io.github.happysnappingturtle.mythicage.core.power;

/**
 * 投掷型神力的飞行（T13 设计文档 §3.1，D2）：提交时已消耗，投掷物按固定抛物线飞到锁定落点，不会落空。
 * 服务端与客户端用同一个公式算飞行刻数与轨迹，两边不必同步实体。
 */
public final class ThrowRules {
    private ThrowRules() {
    }

    /** 飞行刻数：每刻约 1.6 格，至少 6 刻、至多 20 刻。 */
    public static int flightTicks(double distance) {
        return (int) Math.max(6, Math.min(20, Math.ceil(distance / 1.6)));
    }

    /** 抛物线顶点比两端连线高出多少格（距离越远越高）。 */
    public static double apex(double distance) {
        return 1.0 + distance * 0.18;
    }

    /** 第 t（0–1）时刻相对起点的高度偏移（叠加在两端直线插值上）。 */
    public static double arc(double distance, double t) {
        return 4 * apex(distance) * t * (1 - t);
    }
}
