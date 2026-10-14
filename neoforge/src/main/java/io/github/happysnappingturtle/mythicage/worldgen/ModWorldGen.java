package io.github.happysnappingturtle.mythicage.worldgen;

import io.github.happysnappingturtle.mythicage.content.ModTags;
import io.github.happysnappingturtle.mythicage.content.greek.GreekAgriculture;
import io.github.happysnappingturtle.mythicage.content.greek.GreekMaterials;
import net.minecraft.core.BlockPos;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraft.world.level.levelgen.placement.SurfaceWaterDepthFilter;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;
import java.util.function.Supplier;

/**
 * 世界生成（T6 设计文档 §5）。参数写进数据包 JSON，服主可以用数据包覆盖。
 * <ul>
 *   <li>锡石矿：希腊主群系，每区块 5 簇、每簇最多 7 块、Y 0–80（稀有度约等于原版青金石，01_希腊 §2）。</li>
 *   <li>希腊大理石：希腊主群系，每区块 2 处、每处最多 64 块、Y 40–120（近似「地表下 0–30 格」）。</li>
 * </ul>
 */
public final class ModWorldGen {
    /** 生成范围的群系标签（可被数据包覆盖）。 */
    public static final net.minecraft.tags.TagKey<Biome> OLIVE_COMMON_BIOMES = ModTags.biome("has_feature/olive_tree_common");
    public static final net.minecraft.tags.TagKey<Biome> OLIVE_SPARSE_BIOMES = ModTags.biome("has_feature/olive_tree_sparse");
    public static final net.minecraft.tags.TagKey<Biome> GRAPEVINE_BIOMES = ModTags.biome("has_feature/wild_grapevine");

    public static final int TIN_COUNT = 5;
    public static final int TIN_SIZE = 7;
    public static final int TIN_MIN_Y = 0;
    public static final int TIN_MAX_Y = 80;
    public static final int MARBLE_COUNT = 2;
    public static final int MARBLE_SIZE = 64;
    public static final int MARBLE_MIN_Y = 40;
    public static final int MARBLE_MAX_Y = 120;

    public static final ResourceKey<ConfiguredFeature<?, ?>> TIN_ORE_CONFIGURED = key(Registries.CONFIGURED_FEATURE, "ore_tin");
    public static final ResourceKey<ConfiguredFeature<?, ?>> MARBLE_CONFIGURED = key(Registries.CONFIGURED_FEATURE, "ore_marble");
    public static final ResourceKey<PlacedFeature> TIN_ORE_PLACED = key(Registries.PLACED_FEATURE, "ore_tin");
    public static final ResourceKey<PlacedFeature> MARBLE_PLACED = key(Registries.PLACED_FEATURE, "ore_marble");
    public static final ResourceKey<BiomeModifier> ADD_TIN_ORE = key(NeoForgeRegistries.Keys.BIOME_MODIFIERS, "add_ore_tin");
    public static final ResourceKey<BiomeModifier> ADD_MARBLE = key(NeoForgeRegistries.Keys.BIOME_MODIFIERS, "add_ore_marble");

    /** 橄榄树：平原、草甸等每 2 个区块约 1 棵；风袭丘陵每 4 个区块约 1 棵（T6b 设计文档 §3.3）。 */
    public static final int OLIVE_COMMON_RARITY = 2;
    public static final int OLIVE_SPARSE_RARITY = 4;
    /** 野葡萄藤：每 8 个区块约 1 片，每片 3–6 株（尝试 12 次）。 */
    public static final int GRAPEVINE_RARITY = 8;

    public static final ResourceKey<ConfiguredFeature<?, ?>> WILD_GRAPEVINE_CONFIGURED =
            key(Registries.CONFIGURED_FEATURE, "patch_wild_grapevine");
    public static final ResourceKey<PlacedFeature> OLIVE_TREE_COMMON = key(Registries.PLACED_FEATURE, "olive_tree_common");
    public static final ResourceKey<PlacedFeature> OLIVE_TREE_SPARSE = key(Registries.PLACED_FEATURE, "olive_tree_sparse");
    public static final ResourceKey<PlacedFeature> WILD_GRAPEVINE_PLACED = key(Registries.PLACED_FEATURE, "patch_wild_grapevine");
    public static final ResourceKey<BiomeModifier> ADD_OLIVE_COMMON = key(NeoForgeRegistries.Keys.BIOME_MODIFIERS, "add_olive_tree_common");
    public static final ResourceKey<BiomeModifier> ADD_OLIVE_SPARSE = key(NeoForgeRegistries.Keys.BIOME_MODIFIERS, "add_olive_tree_sparse");
    public static final ResourceKey<BiomeModifier> ADD_WILD_GRAPEVINE = key(NeoForgeRegistries.Keys.BIOME_MODIFIERS, "add_wild_grapevine");

    private ModWorldGen() {
    }

    private static <T> ResourceKey<T> key(ResourceKey<? extends net.minecraft.core.Registry<T>> registry, String path) {
        return ResourceKey.create(registry, ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, path));
    }

    public static RegistrySetBuilder builder() {
        return new RegistrySetBuilder()
                .add(Registries.CONFIGURED_FEATURE, ModWorldGen::configured)
                .add(Registries.PLACED_FEATURE, ModWorldGen::placed)
                .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ModWorldGen::biomeModifiers);
    }

    private static void configured(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
        ctx.register(TIN_ORE_CONFIGURED, stoneOre(GreekMaterials.TIN_ORE, TIN_SIZE));
        ctx.register(MARBLE_CONFIGURED, stoneOre(GreekMaterials.MARBLE, MARBLE_SIZE));
        ctx.register(GreekAgriculture.OLIVE_TREE_CONFIGURED, GreekAgriculture.oliveTreeConfigured());
        ctx.register(WILD_GRAPEVINE_CONFIGURED, new ConfiguredFeature<>(Feature.RANDOM_PATCH,
                FeatureUtils.simplePatchConfiguration(Feature.SIMPLE_BLOCK,
                        new SimpleBlockConfiguration(BlockStateProvider.simple(GreekAgriculture.WILD_GRAPEVINE.get())),
                        List.of(Blocks.GRASS_BLOCK), 12)));
    }

    private static ConfiguredFeature<?, ?> stoneOre(Supplier<? extends Block> block, int size) {
        return new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(List.of(OreConfiguration.target(
                new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES), block.get().defaultBlockState())), size));
    }

    private static void placed(BootstrapContext<PlacedFeature> ctx) {
        HolderGetter<ConfiguredFeature<?, ?>> cf = ctx.lookup(Registries.CONFIGURED_FEATURE);
        ctx.register(TIN_ORE_PLACED, new PlacedFeature(cf.getOrThrow(TIN_ORE_CONFIGURED), List.of(
                CountPlacement.of(TIN_COUNT), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(TIN_MIN_Y), VerticalAnchor.absolute(TIN_MAX_Y)),
                BiomeFilter.biome())));
        ctx.register(MARBLE_PLACED, new PlacedFeature(cf.getOrThrow(MARBLE_CONFIGURED), List.of(
                CountPlacement.of(MARBLE_COUNT), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(MARBLE_MIN_Y), VerticalAnchor.absolute(MARBLE_MAX_Y)),
                BiomeFilter.biome())));
        ctx.register(OLIVE_TREE_COMMON, oliveTree(cf, OLIVE_COMMON_RARITY));
        ctx.register(OLIVE_TREE_SPARSE, oliveTree(cf, OLIVE_SPARSE_RARITY));
        ctx.register(WILD_GRAPEVINE_PLACED, new PlacedFeature(cf.getOrThrow(WILD_GRAPEVINE_CONFIGURED), List.of(
                RarityFilter.onAverageOnceEvery(GRAPEVINE_RARITY), InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP_WORLD_SURFACE, BiomeFilter.biome())));
    }

    private static PlacedFeature oliveTree(HolderGetter<ConfiguredFeature<?, ?>> cf, int rarity) {
        return new PlacedFeature(cf.getOrThrow(GreekAgriculture.OLIVE_TREE_CONFIGURED), List.of(
                RarityFilter.onAverageOnceEvery(rarity), InSquarePlacement.spread(),
                SurfaceWaterDepthFilter.forMaxDepth(0), PlacementUtils.HEIGHTMAP_OCEAN_FLOOR,
                BlockPredicateFilter.forPredicate(BlockPredicate.wouldSurvive(
                        GreekAgriculture.OLIVE_SAPLING.get().defaultBlockState(), BlockPos.ZERO)),
                BiomeFilter.biome()));
    }

    private static void biomeModifiers(BootstrapContext<BiomeModifier> ctx) {
        HolderGetter<Biome> biomes = ctx.lookup(Registries.BIOME);
        HolderGetter<PlacedFeature> placed = ctx.lookup(Registries.PLACED_FEATURE);
        HolderSet<Biome> greek = biomes.getOrThrow(ModTags.region(Civilization.GREEK));
        ctx.register(ADD_TIN_ORE, new BiomeModifiers.AddFeaturesBiomeModifier(greek,
                HolderSet.direct(placed.getOrThrow(TIN_ORE_PLACED)), GenerationStep.Decoration.UNDERGROUND_ORES));
        ctx.register(ADD_MARBLE, new BiomeModifiers.AddFeaturesBiomeModifier(greek,
                HolderSet.direct(placed.getOrThrow(MARBLE_PLACED)), GenerationStep.Decoration.UNDERGROUND_ORES));
        ctx.register(ADD_OLIVE_COMMON, new BiomeModifiers.AddFeaturesBiomeModifier(biomes.getOrThrow(OLIVE_COMMON_BIOMES),
                HolderSet.direct(placed.getOrThrow(OLIVE_TREE_COMMON)), GenerationStep.Decoration.VEGETAL_DECORATION));
        ctx.register(ADD_OLIVE_SPARSE, new BiomeModifiers.AddFeaturesBiomeModifier(biomes.getOrThrow(OLIVE_SPARSE_BIOMES),
                HolderSet.direct(placed.getOrThrow(OLIVE_TREE_SPARSE)), GenerationStep.Decoration.VEGETAL_DECORATION));
        ctx.register(ADD_WILD_GRAPEVINE, new BiomeModifiers.AddFeaturesBiomeModifier(biomes.getOrThrow(GRAPEVINE_BIOMES),
                HolderSet.direct(placed.getOrThrow(WILD_GRAPEVINE_PLACED)), GenerationStep.Decoration.VEGETAL_DECORATION));
    }
}
