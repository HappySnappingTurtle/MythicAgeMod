package io.github.happysnappingturtle.mythicage.core.power;

import java.util.List;

/**
 * 哨兵石像的站位与寿命（T13 设计文档 §4.2；05 号 GP-GR-03）。
 */
public final class SentinelRules {
    /** 1 个 MC 日；在自己的神坛 32 格内为 3 个 MC 日。 */
    public static final long LIFETIME = 24000;
    public static final long LIFETIME_NEAR_SHRINE = 72000;
    public static final double SHRINE_RANGE = 32;
    /** 索敌与射程、射击间隔、拉弓时长、碎化时长、升起时长。 */
    public static final double RANGE = 16;
    public static final int SHOT_INTERVAL = 30;
    public static final int DRAW_TICKS = 12;
    public static final int CRUMBLE_TICKS = 20;
    public static final int RISE_TICKS = 20;
    public static final float ARROW_DAMAGE = 4;
    /** 四角：以落点为中心的 (±2, ±2)。 */
    public static final List<int[]> CORNERS = List.of(new int[]{2, 2}, new int[]{-2, 2}, new int[]{-2, -2}, new int[]{2, -2});
    /** 每个角在高度上的搜索顺序：0、+1、−1、+2、−2。 */
    public static final List<Integer> HEIGHTS = List.of(0, 1, -1, 2, -2);

    private SentinelRules() {
    }

    /**
     * 寿命。
     *
     * @param nearestOwnShrine 落点到施法者自己最近一座本文明神庙的距离；没有时传 {@link Double#POSITIVE_INFINITY}
     */
    public static long lifetime(double nearestOwnShrine) {
        return nearestOwnShrine <= SHRINE_RANGE ? LIFETIME_NEAR_SHRINE : LIFETIME;
    }
}
