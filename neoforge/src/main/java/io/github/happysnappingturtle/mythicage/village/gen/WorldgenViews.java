package io.github.happysnappingturtle.mythicage.village.gen;

import io.github.happysnappingturtle.mythicage.core.village.gen.BiomeView;
import io.github.happysnappingturtle.mythicage.core.village.gen.TerrainSampler;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

/** 世界生成线程上的地形与群系采样（core 接口的 MC 实现）。 */
public final class WorldgenViews {
    private WorldgenViews() {
    }

    /**
     * 规划用的地形采样（T13b §2.3）：噪声生成器上用「不含锯齿的初始密度」二分估地表（每列约 6 次求值，约 0.15 ms；
     * 精确的 getBaseHeight 约 3.5 ms），低于海平面的视为水面；其它生成器（超平坦、GameTest）用精确高度。
     * 估算忽略洞穴与尖峰，误差由建筑与地块的整地工序吸收。
     */
    public static TerrainSampler terrain(ChunkGenerator gen, LevelHeightAccessor height, RandomState rs, int cx, int cz) {
        if (gen instanceof net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator) {
            return calibrated(gen, height, rs, cx, cz, biomes(gen, rs));
        }
        return exact(gen, height, rs);
    }

    /** 校准点：候选中心与周围 40 格的两点（实测：估算整体偏低约 9 格，用 3 个近点校准后误差 p50 1、p90 4 格）。 */
    private static final int[][] CALIBRATION = {{0, 0}, {40, 0}, {-20, -35}};

    /** 按水处理的群系：海洋、沼泽（浅水区地面估算不可靠）。河流群系比实际水面宽，交给地面估算判断。 */
    private static final java.util.List<String> WET = java.util.List.of("#minecraft:is_ocean",
            "minecraft:swamp", "minecraft:mangrove_swamp");
    /** 估算整体偏低的典型值（实测约 9 格），给较准扫描当起点。 */
    private static final int TYPICAL_BIAS = 9;

    static TerrainSampler calibrated(ChunkGenerator gen, LevelHeightAccessor height, RandomState rs, int cx, int cz,
                                     io.github.happysnappingturtle.mythicage.core.village.gen.BiomeView biomes) {
        TerrainSampler est = estimated(gen, height, rs);
        int sum = 0;
        for (int[] d : CALIBRATION) {
            int x = cx + d[0];
            int z = cz + d[1];
            sum += precise(gen, height, rs, x, z, est.floor(x, z) + TYPICAL_BIAS) - est.floor(x, z);
        }
        int offset = Math.round((float) sum / CALIBRATION.length);
        int sea = gen.getSeaLevel();
        java.util.Map<Long, Boolean> wetCache = new java.util.HashMap<>();
        return new TerrainSampler() {
            @Override
            public int surface(int x, int z) {
                int f = floor(x, z);
                // 估算误差在海平面附近最要紧：比海平面高不到 2 格的地、以及海洋 / 沼泽群系都按水处理
                boolean wet = f < sea + WET_MARGIN || wetCache.computeIfAbsent(net.minecraft.world.level.ChunkPos.asLong(x >> 2, z >> 2),
                        k -> {
                            Holder<Biome> b = gen.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(x),
                                    QuartPos.fromBlock(sea), QuartPos.fromBlock(z), rs.sampler());
                            return WET.stream().anyMatch(id -> matches(b, id));
                        });
                return wet ? Math.max(f + 1, sea) : f;
            }

            @Override
            public int floor(int x, int z) {
                return est.floor(x, z) + offset;
            }

            @Override
            public int ground(int x, int z) {
                return precise(gen, height, rs, x, z, floor(x, z));
            }
        };
    }

    /**
     * 较准的地面（地面上第一格）：从估算值上方 6 格往下，用最终密度（含锯齿与洞穴）按 2 格一步扫到实心，再细化 1 格。
     * 约 8 次求值。给定位时用（每座建筑、每块地块一次），网格仍用估算。
     */
    static int precise(ChunkGenerator gen, LevelHeightAccessor height, RandomState rs, int x, int z, int hint) {
        net.minecraft.world.level.levelgen.DensityFunction f = rs.router().finalDensity();
        java.util.function.IntPredicate solid = y -> f.compute(
                new net.minecraft.world.level.levelgen.DensityFunction.SinglePointContext(x, y, z)) > 0;
        int maxY = height.getMaxBuildHeight() - 1;
        int minY = height.getMinBuildHeight();
        int y = Math.min(hint + 6, maxY);
        while (y < maxY && solid.test(y)) {
            y += 6;
        }
        while (y > minY && !solid.test(y)) {
            y -= 2;
        }
        if (y + 1 <= maxY && solid.test(y + 1)) {
            y++;
        }
        return y + 1;
    }

    /** 估算地表比海平面高不到这么多格时按水处理。 */
    static final int WET_MARGIN = 2;

    /** 估算地表的阈值（与原版 NoiseChunk.computePreliminarySurfaceLevel 相同）。 */
    private static final double SOLID = 0.390625;

    static TerrainSampler estimated(ChunkGenerator gen, LevelHeightAccessor height, RandomState rs) {
        net.minecraft.world.level.levelgen.DensityFunction f = rs.router().initialDensityWithoutJaggedness();
        int sea = gen.getSeaLevel();
        int minY = height.getMinBuildHeight();
        int maxY = height.getMaxBuildHeight() - 1;
        java.util.Map<Long, Integer> cache = new java.util.HashMap<>();
        java.util.function.IntBinaryOperator ground = (x, z) -> cache.computeIfAbsent(
                net.minecraft.world.level.ChunkPos.asLong(x, z), k -> {
                    java.util.function.IntPredicate solid = y -> f.compute(
                            new net.minecraft.world.level.levelgen.DensityFunction.SinglePointContext(x, y, z)) > SOLID;
                    int lo = sea;
                    if (solid.test(lo)) {
                        while (lo + 8 <= maxY && solid.test(lo + 8)) {
                            lo += 8;
                        }
                    } else {
                        do {
                            lo -= 8;
                        } while (lo > minY && !solid.test(lo));
                    }
                    int hi = Math.min(lo + 8, maxY + 1);
                    while (hi - lo > 1) {
                        int mid = (lo + hi) >>> 1;
                        if (solid.test(mid)) {
                            lo = mid;
                        } else {
                            hi = mid;
                        }
                    }
                    return hi; // 地面上第一格
                });
        return new TerrainSampler() {
            @Override
            public int surface(int x, int z) {
                return Math.max(ground.applyAsInt(x, z), sea);
            }

            @Override
            public int floor(int x, int z) {
                return ground.applyAsInt(x, z);
            }
        };
    }

    static TerrainSampler exact(ChunkGenerator gen, LevelHeightAccessor height, RandomState rs) {
        return new TerrainSampler() {
            @Override
            public int surface(int x, int z) {
                return gen.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, height, rs);
            }

            @Override
            public int floor(int x, int z) {
                return gen.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, height, rs);
            }
        };
    }

    public static BiomeView biomes(ChunkGenerator gen, RandomState rs) {
        int y = QuartPos.fromBlock(gen.getSeaLevel());
        return (id, x, z) -> matches(gen.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(x), y, QuartPos.fromBlock(z),
                rs.sampler()), id);
    }

    /** 群系 id（{@code minecraft:plains}）或标签（{@code #minecraft:is_river}）。 */
    public static boolean matches(Holder<Biome> biome, String id) {
        if (id.startsWith("#")) {
            ResourceLocation rl = ResourceLocation.tryParse(id.substring(1));
            return rl != null && biome.is(TagKey.create(Registries.BIOME, rl));
        }
        return biome.unwrapKey().map(k -> k.location().toString().equals(id)).orElse(false);
    }
}
