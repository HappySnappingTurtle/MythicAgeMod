package io.github.happysnappingturtle.mythicage.core.village.soldier;

import io.github.happysnappingturtle.mythicage.core.wild.WildCombatRules;

/**
 * 拦截与护送的站位（T20 设计文档 §5.6、§9.1）：纯几何（水平面）。
 */
public final class InterceptRules {
    /** 站在离被护者多远的地方（不超过两者距离的一半）。 */
    public static final double OFFSET = 1.5;
    /** 投射物掩护：卫兵离被护者不超过这么远。 */
    public static final double COVER_DISTANCE = 3.0;

    private InterceptRules() {
    }

    /** 站位：从被护者 (rx, rz) 朝威胁 (tx, tz) 走 min(1.5, 距离一半)。返回 {x, z}。 */
    public static double[] stand(double rx, double rz, double tx, double tz) {
        double dx = tx - rx;
        double dz = tz - rz;
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d < 1.0E-6) {
            return new double[]{rx, rz};
        }
        double k = Math.min(OFFSET, d / 2) / d;
        return new double[]{rx + dx * k, rz + dz * k};
    }

    /**
     * 飞向被护者的投射物，卫兵挡不挡得住：卫兵正在举盾、离被护者 ≤ 3 格、射手在卫兵正前方扇形内。
     *
     * @param guardYaw 卫兵朝向
     */
    public static boolean covers(SoldierProfile.Shield s, boolean blocking, double guardYaw, double gx, double gz,
                                 double shooterX, double shooterZ, double rx, double rz) {
        if (!blocking) {
            return false;
        }
        double dr = Math.sqrt((gx - rx) * (gx - rx) + (gz - rz) * (gz - rz));
        if (dr > COVER_DISTANCE) {
            return false;
        }
        return WildCombatRules.frontalMultiplier(guardYaw, shooterX - gx, shooterZ - gz, s.arc(), s.reduction()) < 1.0;
    }
}
