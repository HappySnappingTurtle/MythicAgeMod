package io.github.happysnappingturtle.mythicage.content.greek;

import io.github.happysnappingturtle.mythicage.content.BlockModelSpec;
import io.github.happysnappingturtle.mythicage.content.Content;
import io.github.happysnappingturtle.mythicage.content.LootSpec;
import io.github.happysnappingturtle.mythicage.content.Recolor;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.registry.ModRegistries;
import io.github.happysnappingturtle.mythicage.worldgen.GreekTreeShapes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.Arrays;
import java.util.Optional;

import static io.github.happysnappingturtle.mythicage.core.player.Civilization.GREEK;

/**
 * 柏树与月桂（T13b 阶段 ③ 第 4 批，规划篇 §4.2 补充表）：原木、树叶、树苗、盆栽；不做木板等木材族。
 * 柏树用于哈迪斯墓园与园林，月桂用于神谕所内室与园林；两者都在希腊地域少量自然生成（{@code ModWorldGen}）。
 * 树叶、树苗贴图与原木换色由 {@code tools/models/greek_trees.py} 生成。
 */
public final class GreekTrees {
    private GreekTrees() {
    }

    private static ResourceLocation mod(String path) {
        return ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, path);
    }

    // ---- 树形（世界生成与树苗共用） ----

    public static final DeferredHolder<Feature<?>, GreekTreeShapes.Cypress> CYPRESS_TREE_FEATURE =
            ModRegistries.FEATURES.register("cypress_tree",
                    () -> new GreekTreeShapes.Cypress(() -> GreekTrees.CYPRESS_LOG.get(), () -> GreekTrees.CYPRESS_LEAVES.get()));
    public static final DeferredHolder<Feature<?>, GreekTreeShapes.Laurel> LAUREL_TREE_FEATURE =
            ModRegistries.FEATURES.register("laurel_tree",
                    () -> new GreekTreeShapes.Laurel(() -> GreekTrees.LAUREL_LOG.get(), () -> GreekTrees.LAUREL_LEAVES.get()));
    public static final ResourceKey<ConfiguredFeature<?, ?>> CYPRESS_TREE_CONFIGURED =
            ResourceKey.create(Registries.CONFIGURED_FEATURE, mod("cypress_tree"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> LAUREL_TREE_CONFIGURED =
            ResourceKey.create(Registries.CONFIGURED_FEATURE, mod("laurel_tree"));
    public static final TreeGrower CYPRESS_GROWER = new TreeGrower("mythic_age:cypress", Optional.empty(),
            Optional.of(CYPRESS_TREE_CONFIGURED), Optional.empty());
    public static final TreeGrower LAUREL_GROWER = new TreeGrower("mythic_age:laurel", Optional.empty(),
            Optional.of(LAUREL_TREE_CONFIGURED), Optional.empty());

    // ---- 柏树 ----

    public static final DeferredBlock<RotatedPillarBlock> CYPRESS_LOG = log("cypress_log", MapColor.COLOR_BROWN,
            "柏树原木", "Cypress Log", "糸杉の原木");
    public static final DeferredBlock<LeavesBlock> CYPRESS_LEAVES = leaves("cypress_leaves", "柏树树叶", "Cypress Leaves",
            "糸杉の葉");
    public static final DeferredBlock<SaplingBlock> CYPRESS_SAPLING = sapling("cypress_sapling", CYPRESS_GROWER,
            "柏树树苗", "Cypress Sapling", "糸杉の苗木");
    public static final DeferredBlock<FlowerPotBlock> POTTED_CYPRESS_SAPLING = potted("potted_cypress_sapling",
            CYPRESS_SAPLING, "柏树树苗盆栽", "Potted Cypress Sapling", "糸杉の苗木の鉢植え");

    // ---- 月桂 ----

    public static final DeferredBlock<RotatedPillarBlock> LAUREL_LOG = log("laurel_log", MapColor.STONE,
            "月桂原木", "Laurel Log", "月桂樹の原木");
    public static final DeferredBlock<LeavesBlock> LAUREL_LEAVES = leaves("laurel_leaves", "月桂树叶", "Laurel Leaves",
            "月桂樹の葉");
    public static final DeferredBlock<SaplingBlock> LAUREL_SAPLING = sapling("laurel_sapling", LAUREL_GROWER,
            "月桂树苗", "Laurel Sapling", "月桂樹の苗木");
    public static final DeferredBlock<FlowerPotBlock> POTTED_LAUREL_SAPLING = potted("potted_laurel_sapling",
            LAUREL_SAPLING, "月桂树苗盆栽", "Potted Laurel Sapling", "月桂樹の苗木の鉢植え");

    private static DeferredBlock<RotatedPillarBlock> log(String id, MapColor color, String zh, String en, String ja) {
        return Content.block(id, GREEK, RotatedPillarBlock::new)
                .properties(() -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG).mapColor(color))
                .model(new BlockModelSpec.Log(Recolor.sprite(id.startsWith("cypress") ? "minecraft:block/spruce_log"
                        : "minecraft:block/birch_log", id), Recolor.sprite(id.startsWith("cypress")
                        ? "minecraft:block/spruce_log_top" : "minecraft:block/birch_log_top", id + "_top")))
                .tags(BlockTags.LOGS_THAT_BURN, BlockTags.MINEABLE_WITH_AXE)
                .itemTags(ItemTags.LOGS_THAT_BURN)
                .lang(zh, en, ja)
                .register();
    }

    private static DeferredBlock<LeavesBlock> leaves(String id, String zh, String en, String ja) {
        String sapling = id.replace("_leaves", "_sapling");
        return Content.block(id, GREEK, LeavesBlock::new)
                .properties(() -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES))
                .model(new BlockModelSpec.Leaves(mod("block/" + id), null, Arrays.asList((ResourceLocation) null)))
                .loot(LootSpec.custom(l -> l.leaves(net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(mod(id)),
                        net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(mod(sapling)))))
                .tags(BlockTags.LEAVES, BlockTags.MINEABLE_WITH_HOE)
                .itemTags(ItemTags.LEAVES)
                .lang(zh, en, ja)
                .register();
    }

    private static DeferredBlock<SaplingBlock> sapling(String id, TreeGrower grower, String zh, String en, String ja) {
        return Content.block(id, GREEK, p -> new SaplingBlock(grower, p))
                .properties(() -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING))
                .model(BlockModelSpec.cross(mod("block/" + id)))
                .itemTexture(mod("block/" + id))
                .tags(BlockTags.SAPLINGS)
                .itemTags(ItemTags.SAPLINGS)
                .lang(zh, en, ja)
                .register();
    }

    private static DeferredBlock<FlowerPotBlock> potted(String id, DeferredBlock<SaplingBlock> plant, String zh, String en,
                                                        String ja) {
        return Content.block(id, GREEK, p -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, plant, p))
                .properties(() -> BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_OAK_SAPLING))
                .model(new BlockModelSpec.Potted(mod("block/" + plant.getId().getPath())))
                .loot(LootSpec.potted(plant))
                .noItem()
                .tags(BlockTags.FLOWER_POTS)
                .lang(zh, en, ja)
                .register();
    }

    public static ConfiguredFeature<?, ?> cypressConfigured() {
        return new ConfiguredFeature<>(CYPRESS_TREE_FEATURE.get(), NoneFeatureConfiguration.INSTANCE);
    }

    public static ConfiguredFeature<?, ?> laurelConfigured() {
        return new ConfiguredFeature<>(LAUREL_TREE_FEATURE.get(), NoneFeatureConfiguration.INSTANCE);
    }

    public static void init() {
    }
}
