package io.github.happysnappingturtle.mythicage.core.village.soldier;

import java.util.Optional;

/**
 * 投矛兵后撤一步要退多远、用多久（GA-1）：按自己和追兵的速度算，退完正好拉开 {@code gain} 格。
 * 以前后撤跑到 30 米/秒，退 3 格一眨眼就到；改成正常跑速后，要把追兵这段时间跟上的路程算进去。
 */
public record Backstep(double distance, int ticks) {
    /** 起步加速大约要的刻数。 */
    public static final int ACCEL_TICKS = 4;

    /**
     * @param gain        要多拉开的格数
     * @param ownMps      自己后撤的米/秒
     * @param chaserMps   追兵的米/秒（站着不动的给 0）
     * @param maxDistance 一步最远退多少格
     * @return 跑不过追兵（或参数无效）时为空，调用方就近出手
     */
    public static Optional<Backstep> plan(double gain, double ownMps, double chaserMps, double maxDistance) {
        if (gain <= 0 || ownMps <= 0 || chaserMps < 0 || ownMps <= chaserMps || maxDistance <= 0) {
            return Optional.empty();
        }
        double seconds = gain / (ownMps - chaserMps);
        double distance = Math.min(maxDistance, ownMps * seconds);
        return Optional.of(new Backstep(distance, (int) Math.ceil(distance / ownMps * 20) + ACCEL_TICKS));
    }
}
