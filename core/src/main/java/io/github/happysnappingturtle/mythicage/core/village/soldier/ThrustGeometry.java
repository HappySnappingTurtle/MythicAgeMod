package io.github.happysnappingturtle.mythicage.core.village.soldier;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 近战胶囊判定（T20 设计文档 §8.1，02 号卡 §3）：纯几何。
 * <p>
 * 从士兵脚下位置沿水平朝向，取 [{@link SoldierProfile.Melee#START}, min(触及, 方块截断)] 的线段，半径 {@code radius}；
 * 垂直方向是离地 {@code height ± halfHeight} 的一段。目标的碰撞箱与这个胶囊相交就算命中。
 */
public final class ThrustGeometry {
    private ThrustGeometry() {
    }

    /** 轴对齐盒（世界坐标）。 */
    public record Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
    }

    /**
     * @param x       士兵脚下 x
     * @param y       士兵脚下 y
     * @param z       士兵脚下 z
     * @param yawDeg  朝向（Minecraft 偏航角：0 = +Z，90 = −X）
     * @param blocked 方块截断距离（没有截断传 {@code Double.MAX_VALUE}）
     * @return 命中时返回目标沿线段的距离（用来比谁更近）；不中返回 empty
     */
    public static Optional<Double> hit(double x, double y, double z, double yawDeg, SoldierProfile.Melee m, double blocked,
                                       Box target) {
        double end = Math.min(m.reach(), blocked);
        if (end <= SoldierProfile.Melee.START) {
            return Optional.empty();
        }
        double lo = y + m.height() - m.halfHeight();
        double hi = y + m.height() + m.halfHeight();
        if (target.maxY() < lo || target.minY() > hi) {
            return Optional.empty();
        }
        double yaw = Math.toRadians(yawDeg);
        double fx = -Math.sin(yaw);
        double fz = Math.cos(yaw);
        double ax = x + fx * SoldierProfile.Melee.START;
        double az = z + fz * SoldierProfile.Melee.START;
        double bx = x + fx * end;
        double bz = z + fz * end;
        // 线段到矩形（水平投影）的最近距离：在线段上细分取样，足够精确（触及只有 2–4 格）
        int steps = 32;
        double best = Double.MAX_VALUE;
        double bestAlong = 0;
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double px = ax + (bx - ax) * t;
            double pz = az + (bz - az) * t;
            double dx = Math.max(Math.max(target.minX() - px, 0), px - target.maxX());
            double dz = Math.max(Math.max(target.minZ() - pz, 0), pz - target.maxZ());
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d < best - 1.0E-9) {
                best = d;
                bestAlong = SoldierProfile.Melee.START + (end - SoldierProfile.Melee.START) * t;
            }
        }
        return best <= m.radius() + 1.0E-9 ? Optional.of(bestAlong) : Optional.empty();
    }

    /** 胶囊里的一个对象：沿线段的距离、能不能打。 */
    public record Hit(Object who, double along, boolean attackable) {
    }

    /** 一次出手的结果：命中谁，或因友方挡在前面而收手。 */
    public sealed interface Outcome permits Strike, Withhold, Miss {
    }

    public record Strike(Object who) implements Outcome {
    }

    /** 友方挡在比可打目标更近的地方：收手（02 号卡「友方挡在中间时收手 / 重定位」）。 */
    public record Withhold(Object blocker) implements Outcome {
    }

    public record Miss() implements Outcome {
    }

    /** 从胶囊里的所有对象中决定：最近的一个可打 → 打它；最近的一个不可打（且后面有可打的）→ 收手；都没有 → 落空。 */
    public static Outcome resolve(List<Hit> hits) {
        Optional<Hit> first = hits.stream().min(Comparator.comparingDouble(Hit::along));
        if (first.isEmpty()) {
            return new Miss();
        }
        if (first.get().attackable()) {
            return new Strike(first.get().who());
        }
        return hits.stream().anyMatch(Hit::attackable) ? new Withhold(first.get().who()) : new Miss();
    }
}
