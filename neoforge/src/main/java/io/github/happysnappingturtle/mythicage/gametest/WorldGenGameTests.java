package io.github.happysnappingturtle.mythicage.gametest;

import com.mojang.serialization.JsonOps;
import io.github.happysnappingturtle.mythicage.content.ModTags;
import io.github.happysnappingturtle.mythicage.content.greek.GreekMaterials;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.content.greek.worldgen.GreekWorldGen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;

/** T6 世界生成的集成测试（T6 测试用例文档 §4）。 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WorldGenGameTests {
    private static final String BATCH = "worldgen";

    private WorldGenGameTests() {
    }

    /** TC-T6-40：希腊地域标签包含总纲 §4.1 的 8 个主群系，不含沙漠。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void greek_region_tag(GameTestHelper helper) {
        Registry<Biome> biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        HolderSet.Named<Biome> tag = biomes.getTag(ModTags.region(Civilization.GREEK)).orElseThrow();
        List<ResourceKey<Biome>> expected = List.of(Biomes.MEADOW, Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS,
                Biomes.STONY_SHORE, Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.WINDSWEPT_FOREST,
                Biomes.SWAMP);
        for (ResourceKey<Biome> key : expected) {
            helper.assertTrue(tag.contains(biomes.getHolderOrThrow(key)), "region/greek 应包含 " + key.location());
        }
        helper.assertTrue(tag.size() == expected.size(), "region/greek 应恰好 8 个群系，实际 " + tag.size());
        helper.assertTrue(!tag.contains(biomes.getHolderOrThrow(Biomes.DESERT)), "region/greek 不应包含沙漠");
        helper.succeed();
    }

    /** TC-T6-41：placed feature 存在且高度范围正确。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void placed_features(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        Registry<PlacedFeature> placed = access.registryOrThrow(Registries.PLACED_FEATURE);
        RegistryOps<com.google.gson.JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, access);
        record Case(ResourceKey<PlacedFeature> key, int min, int max) {
        }
        for (Case c : List.of(new Case(GreekWorldGen.TIN_ORE_PLACED, 0, 80), new Case(GreekWorldGen.MARBLE_PLACED, 40, 120))) {
            PlacedFeature pf = placed.getOrThrow(c.key());
            String json = PlacedFeature.DIRECT_CODEC.encodeStart(ops, pf).getOrThrow().toString();
            helper.assertTrue(json.contains("\"min_inclusive\":{\"absolute\":" + c.min() + "}")
                    && json.contains("\"max_inclusive\":{\"absolute\":" + c.max() + "}"),
                    c.key().location() + " 高度范围应为 " + c.min() + "–" + c.max() + "：" + json);
        }
        helper.succeed();
    }

    /** TC-T6-42：biome modifier 只作用于 #mythic_age:region/greek。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void biome_modifiers(GameTestHelper helper) {
        Registry<BiomeModifier> mods = helper.getLevel().registryAccess().registryOrThrow(NeoForgeRegistries.Keys.BIOME_MODIFIERS);
        for (var key : List.of(GreekWorldGen.ADD_TIN_ORE, GreekWorldGen.ADD_MARBLE)) {
            BiomeModifier m = mods.getOrThrow(key);
            helper.assertTrue(m instanceof BiomeModifiers.AddFeaturesBiomeModifier, key.location() + " 类型");
            var biomes = ((BiomeModifiers.AddFeaturesBiomeModifier) m).biomes();
            helper.assertTrue(biomes.unwrapKey().filter(k -> k.equals(ModTags.region(Civilization.GREEK))).isPresent(),
                    key.location() + " 应只作用于 region/greek，实际 " + biomes);
        }
        helper.succeed();
    }

    /** TC-T6-43：直接运行锡石矿的 configured feature：石头被替换为锡石矿，空气不被替换。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void tin_feature_replaces_stone_only(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                for (int y = 0; y < 2; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
                }
                helper.setBlock(new BlockPos(x, 2, z), Blocks.AIR);
            }
        }
        ConfiguredFeature<?, ?> feature = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .getOrThrow(GreekWorldGen.TIN_ORE_CONFIGURED);
        boolean found = false;
        for (int seed = 0; seed < 20 && !found; seed++) {
            feature.place(level, level.getChunkSource().getGenerator(), RandomSource.create(seed),
                    helper.absolutePos(new BlockPos(1, 0, 1)));
            for (int x = 0; x < 3 && !found; x++) {
                for (int z = 0; z < 3 && !found; z++) {
                    for (int y = 0; y < 2 && !found; y++) {
                        found = helper.getBlockState(new BlockPos(x, y, z)).is(GreekMaterials.TIN_ORE.get());
                    }
                }
            }
        }
        helper.assertTrue(found, "运行 20 次后应至少替换出 1 块锡石矿");
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                helper.assertBlockPresent(Blocks.AIR, new BlockPos(x, 2, z));
            }
        }
        helper.succeed();
    }
}
