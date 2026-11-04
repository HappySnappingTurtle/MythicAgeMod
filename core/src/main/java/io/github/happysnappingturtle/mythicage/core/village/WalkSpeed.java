package io.github.happysnappingturtle.mythicage.core.village;

/**
 * 村里人（居民、士兵、祭司）的移动速度（GA-1）：按米/秒定目标，换算成寻路倍率。
 * <p>
 * 原版生物的前进输入等于 s = 属性 × 倍率（{@code Mob.setSpeed} 同时设 {@code zza}），加速度又乘一次 s，
 * 所以地面稳态速度和 s² 成正比，不能按「想要的值 / 属性」线性换算——以前这样写，居民走路快到 13 米/秒。
 */
public final class WalkSpeed {
    /** 普通方块（摩擦 0.6）上的系数：米/秒 = K × (属性 × 倍率)²。每刻加速度 0.98 s²，水平速度每刻乘 0.546。 */
    public static final double K = 20 * 0.98 / (1 - 0.6 * 0.91);

    /** 居民走路、士兵走路（原版村民走向目标约 2.7，闲逛约 1.7）。 */
    public static final double WALK = 2.0;
    /** 居民跑步（火炬接力、赛跑），略低于玩家疾跑 5.6。 */
    public static final double RUN = 5.0;
    /** 居民逃回家。 */
    public static final double FLEE = 4.0;
    /** 士兵追敌、拦截、后撤、回岗：比僵尸（约 2.3）快，比玩家走路（约 4.3）慢。 */
    public static final double SOLDIER_RUN = 3.5;
    /** 祭司跑腿、回到活动范围。 */
    public static final double ERRAND = 2.5;

    private WalkSpeed() {
    }

    /** 属性、倍率 → 地面稳态米/秒。 */
    public static double metersPerSecond(double attribute, double modifier) {
        double s = attribute * modifier;
        return K * s * s;
    }

    /** 想要的米/秒 → 应传给寻路的倍率。 */
    public static double modifier(double attribute, double metersPerSecond) {
        if (attribute <= 0 || metersPerSecond < 0) {
            throw new IllegalArgumentException("attribute=" + attribute + ", metersPerSecond=" + metersPerSecond);
        }
        return Math.sqrt(metersPerSecond / K) / attribute;
    }
}
