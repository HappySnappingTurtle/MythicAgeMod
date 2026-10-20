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

    public static TerrainSampler terrain(ChunkGenerator gen, LevelHeightAccessor height, RandomState rs) {
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
