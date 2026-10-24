package io.github.happysnappingturtle.mythicage.core.village.gen;

/**
 * 世界生成时的地形采样（T13b §2.3）：neoforge 用 {@code ChunkGenerator.getBaseHeight} 实现，测试用公式实现。
 * 两种高度都是「该列最上面一个方块之上的第一格」。
 */
public interface TerrainSampler {
    /** 含水面的地表（WORLD_SURFACE_WG）。 */
    int surface(int x, int z);

    /** 固体地面（OCEAN_FLOOR_WG）；与 {@link #surface} 不同说明这里是水面。 */
    int floor(int x, int z);

    default boolean water(int x, int z) {
        return surface(x, z) > floor(x, z);
    }

    /**
     * 落位复核用的判水（比 {@link #water} 准、也更贵）：世界生成时估算地面在海平面附近误差几格，湖岸会被估成陆地，
     * 这里对靠近海平面的点改用较准地面再判一次（T19 实机：卫城前半截压进湖里）。默认同 {@link #water}。
     */
    default boolean wet(int x, int z) {
        return water(x, z);
    }

    /**
     * 定位用的较准地面（地面上第一格）：每座建筑、每块地块落位时取一次。默认同 {@link #floor}；
     * 世界生成时比网格估算更准也更贵（约 0.35 ms）。
     */
    default int ground(int x, int z) {
        return floor(x, z);
    }
}
