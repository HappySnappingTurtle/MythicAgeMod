package io.github.happysnappingturtle.mythicage.core.wild;

/**
 * 野外神话生物的刷新规则（T15 设计文档 §1.1）：村庄排除与区域密度。纯计算，世界查询由调用方完成。
 */
public final class WildSpawnRules {
    private WildSpawnRules() {
    }

    /** 区域编号：按 area × area 的网格把 (x, z) 归到一个区域。 */
    public static long areaKey(int x, int z, int area) {
        long ax = Math.floorDiv(x, area);
        long az = Math.floorDiv(z, area);
        return (ax << 32) ^ (az & 0xffffffffL);
    }

    /** 离最近村庄中心的距离平方 nearestVillageSq（没有村庄时传 {@link Double#POSITIVE_INFINITY}）。 */
    public static boolean villageAllows(double nearestVillageSq, int avoid) {
        return avoid <= 0 || nearestVillageSq > (double) avoid * avoid;
    }

    /** 区域里已有 existingGroups 群，上限 max。 */
    public static boolean densityAllows(int existingGroups, int max) {
        return existingGroups < max;
    }
}
