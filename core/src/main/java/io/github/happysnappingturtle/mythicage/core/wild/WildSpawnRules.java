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

    /**
     * 小结构（营地、鸟巢……）的落点：按世界种子在每个 area × area 区域里固定挑出至多 max 个区块，只有这些区块会尝试生成，
     * 所以同一区域最多 max 处，同一个世界每次算出来都一样（生成线程上不用查已有结构）。
     * salt 区分不同生物，免得不同结构总挑同一个区块；area 按格数，不足一个区块的部分向上取整。
     */
    public static boolean isAreaSite(long seed, String salt, int chunkX, int chunkZ, int area, int max) {
        int cells = Math.max(1, (area + 15) / 16);
        long ax = Math.floorDiv(chunkX, cells);
        long az = Math.floorDiv(chunkZ, cells);
        long base = mix(seed ^ salt.hashCode()) ^ mix(ax * 0x9E3779B97F4A7C15L + az);
        for (int i = 0; i < max; i++) {
            long h = mix(base + i * 0xC2B2AE3D27D4EB4FL);
            if (ax * cells + Math.floorMod(h, cells) == chunkX && az * cells + Math.floorMod(h >>> 32, cells) == chunkZ) {
                return true;
            }
        }
        return false;
    }

    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
