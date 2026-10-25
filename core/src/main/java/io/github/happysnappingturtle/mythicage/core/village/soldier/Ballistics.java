package io.github.happysnappingturtle.mythicage.core.village.soldier;

import java.util.Optional;

/**
 * 远程兵种的出手角度（T20 设计文档 §8.2）：按「每刻先移动、再减重力」的离散抛物线，用低抛的那一解。纯函数。
 * 重力为 0 时直线瞄准（亚特兰蒂斯标枪那类平直投射）。
 */
public final class Ballistics {
    private Ballistics() {
    }

    /**
     * @param dx 目标相对出手点的水平 x
     * @param dy 目标相对出手点的高度差
     * @param dz 目标相对出手点的水平 z
     * @return 初速度 {vx, vy, vz}；够不着返回 empty
     */
    public static Optional<double[]> launch(double dx, double dy, double dz, double speed, double gravity) {
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d < 1.0E-6) {
            return Optional.of(new double[]{0, dy >= 0 ? speed : -speed, 0});
        }
        if (gravity <= 0) {
            double len = Math.sqrt(d * d + dy * dy);
            return Optional.of(new double[]{dx / len * speed, dy / len * speed, dz / len * speed});
        }
        double v2 = speed * speed;
        double disc = v2 * v2 - gravity * (gravity * d * d + 2 * dy * v2);
        if (disc < 0) {
            return Optional.empty();
        }
        double tan = (v2 - Math.sqrt(disc)) / (gravity * d);
        double cos = 1 / Math.sqrt(1 + tan * tan);
        double sin = tan * cos;
        double h = speed * cos;
        return Optional.of(new double[]{dx / d * h, speed * sin, dz / d * h});
    }

    /** 按 {@link #launch} 的速度模拟 {@code ticks} 刻（不计空气阻力），返回落点高度最接近目标水平距离时的高度差（测试用）。 */
    public static double heightAt(double[] v, double distance, double gravity) {
        double x = 0;
        double y = 0;
        double vy = v[1];
        double h = Math.sqrt(v[0] * v[0] + v[2] * v[2]);
        for (int t = 0; t < 400 && x < distance; t++) {
            x += h;
            y += vy;
            vy -= gravity;
        }
        return y;
    }
}
