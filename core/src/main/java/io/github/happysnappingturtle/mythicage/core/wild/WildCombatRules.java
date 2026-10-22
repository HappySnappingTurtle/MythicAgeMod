package io.github.happysnappingturtle.mythicage.core.wild;

/**
 * 野外神话生物的战斗规则（纯计算）。
 */
public final class WildCombatRules {
    private WildCombatRules() {
    }

    /**
     * 正面铁壳（卡尔基诺斯，01_希腊 W4）：伤害来源在身体前方 arcDeg 扇形内时乘 (1 − reduction)。
     * 按水平方向向量计算，不随动画抖动。
     *
     * @param facingYawDeg 实体朝向（Minecraft 偏航角：0 = +Z，90 = −X）
     * @param dx           来源相对实体的 x
     * @param dz           来源相对实体的 z
     */
    public static double frontalMultiplier(double facingYawDeg, double dx, double dz, double arcDeg, double reduction) {
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1.0E-6) {
            return 1.0;
        }
        double yaw = Math.toRadians(facingYawDeg);
        double fx = -Math.sin(yaw);
        double fz = Math.cos(yaw);
        double cos = (fx * dx + fz * dz) / len;
        double half = Math.toRadians(arcDeg / 2.0);
        return cos >= Math.cos(half) - 1.0E-9 ? 1.0 - reduction : 1.0;
    }
}
