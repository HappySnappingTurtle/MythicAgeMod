package io.github.happysnappingturtle.mythicage.worldgen;

import io.github.happysnappingturtle.mythicage.content.ModTags;
import io.github.happysnappingturtle.mythicage.content.greek.GreekMaterials;
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
    }

    private static void biomeModifiers(BootstrapContext<BiomeModifier> ctx) {
        HolderGetter<Biome> biomes = ctx.lookup(Registries.BIOME);
        HolderGetter<PlacedFeature> placed = ctx.lookup(Registries.PLACED_FEATURE);
        HolderSet<Biome> greek = biomes.getOrThrow(ModTags.region(Civilization.GREEK));
        ctx.register(ADD_TIN_ORE, new BiomeModifiers.AddFeaturesBiomeModifier(greek,
                HolderSet.direct(placed.getOrThrow(TIN_ORE_PLACED)), GenerationStep.Decoration.UNDERGROUND_ORES));
        ctx.register(ADD_MARBLE, new BiomeModifiers.AddFeaturesBiomeModifier(greek,
                HolderSet.direct(placed.getOrThrow(MARBLE_PLACED)), GenerationStep.Decoration.UNDERGROUND_ORES));
    }
}
