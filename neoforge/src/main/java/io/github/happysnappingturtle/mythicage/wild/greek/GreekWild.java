package io.github.happysnappingturtle.mythicage.wild.greek;

import io.github.happysnappingturtle.mythicage.content.Content;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.content.BlockModelSpec;
import io.github.happysnappingturtle.mythicage.content.LootSpec;
import io.github.happysnappingturtle.mythicage.entity.centaur.WildCentaurEntity;
import io.github.happysnappingturtle.mythicage.entity.hamadryad.HamadryadEntity;
import io.github.happysnappingturtle.mythicage.entity.hamadryad.RootSpikeEntity;
import io.github.happysnappingturtle.mythicage.entity.harpy.HarpyEntity;
import io.github.happysnappingturtle.mythicage.entity.karkinos.KarkinosEntity;
import io.github.happysnappingturtle.mythicage.entity.stymphalian.BronzeFeatherEntity;
import io.github.happysnappingturtle.mythicage.entity.stymphalian.StymphalianBirdEntity;
import io.github.happysnappingturtle.mythicage.registry.ModRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

import static io.github.happysnappingturtle.mythicage.core.player.Civilization.GREEK;

/**
 * 希腊野外神话生物（T15）：实体、掉落物、小结构地物。生物卡在 {@code wild_creature/greek/*.json}，
 * 数值在 {@code entity_stats/greek/wild_*.json}。
 */
public final class GreekWild {
    /** 野外半人马（W1）：碰撞同召唤版；首领放大 1.1 倍。CREATURE 类别（同原版马）。 */
    public static final DeferredHolder<EntityType<?>, EntityType<WildCentaurEntity>> CENTAUR =
            ModRegistries.ENTITY_TYPES.register("centaur",
                    () -> EntityType.Builder.of(WildCentaurEntity::new, MobCategory.CREATURE)
                            .sized(1.1F, 2.5F)
                            .eyeHeight(2.2F)
                            .clientTrackingRange(10)
                            .build("mythic_age:centaur"));

    /** 半人马鬃弦：半人马的鬃毛搓成的弓弦（→ 希腊复合弓、半人马召唤符的原料之一，T17）。 */
    public static final DeferredItem<Item> CENTAUR_SINEW = Content.item("centaur_sinew", GREEK)
            .lang("半人马鬃弦", "Centaur Sinew", "ケンタウロスの鬣弦")
            .register();

    public static final DeferredHolder<Feature<?>, CentaurCampFeature> CENTAUR_CAMP_FEATURE =
            ModRegistries.FEATURES.register("centaur_camp", () -> new CentaurCampFeature(NoneFeatureConfiguration.CODEC));
    public static final ResourceKey<ConfiguredFeature<?, ?>> CENTAUR_CAMP_CONFIGURED =
            ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "centaur_camp"));
    public static final ResourceKey<PlacedFeature> CENTAUR_CAMP_PLACED =
            ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "centaur_camp"));

    /** 斯廷法利斯怪鸟（W3）：敌对、群居、只随沼泽鸟巢生成；领头鸟放大 1.15 倍。 */
    public static final DeferredHolder<EntityType<?>, EntityType<StymphalianBirdEntity>> STYMPHALIAN_BIRD =
            ModRegistries.ENTITY_TYPES.register("stymphalian_bird",
                    () -> EntityType.Builder.of(StymphalianBirdEntity::new, MobCategory.MONSTER)
                            .sized(0.8F, 1.0F)
                            .eyeHeight(0.85F)
                            .clientTrackingRange(10)
                            .build("mythic_age:stymphalian_bird"));

    /** 怪鸟射出的青铜羽（弹射物）。 */
    public static final DeferredHolder<EntityType<?>, EntityType<BronzeFeatherEntity>> BRONZE_FEATHER_SHOT =
            ModRegistries.ENTITY_TYPES.register("bronze_feather",
                    () -> EntityType.Builder.<BronzeFeatherEntity>of(BronzeFeatherEntity::new, MobCategory.MISC)
                            .sized(0.3F, 0.3F)
                            .clientTrackingRange(6)
                            .updateInterval(2)
                            .build("mythic_age:bronze_feather"));

    /** 青铜羽毛：怪鸟掉落（→ 青铜箭、熔成青铜粒，T17）。 */
    public static final DeferredItem<Item> BRONZE_FEATHER = Content.item("bronze_feather", GREEK)
            .lang("青铜羽毛", "Bronze Feather", "青銅の羽根")
            .register();

    /** 青铜响板 Krotala：摇响惊散怪鸟（D5：2 青铜锭 + 1 皮革）。 */
    public static final DeferredItem<KrotalaItem> KROTALA = Content.item("krotala", GREEK, KrotalaItem::new)
            .properties(p -> p.stacksTo(1))
            .lang("青铜响板", "Krotala", "青銅のクロタラ")
            .register();

    /** 插地青铜羽：鸟巢附近泥地里的迹象，挖掉得 1 根青铜羽毛。 */
    public static final DeferredBlock<StuckFeatherBlock> STUCK_BRONZE_FEATHER = Content.block("stuck_bronze_feather", GREEK,
                    StuckFeatherBlock::new)
            .properties(() -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).noCollission().instabreak()
                    .sound(SoundType.CHAIN).noOcclusion().pushReaction(PushReaction.DESTROY))
            .model(BlockModelSpec.cross(ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "block/stuck_bronze_feather")))
            .noItem()
            .loot(LootSpec.other(() -> BRONZE_FEATHER.get()))
            .lang("插地青铜羽", "Stuck Bronze Feather", "地に刺さった青銅の羽根")
            .register();

    public static final DeferredHolder<Feature<?>, StymphalianNestFeature> STYMPHALIAN_NEST_FEATURE =
            ModRegistries.FEATURES.register("stymphalian_nest", () -> new StymphalianNestFeature(NoneFeatureConfiguration.CODEC));
    public static final ResourceKey<ConfiguredFeature<?, ?>> STYMPHALIAN_NEST_CONFIGURED =
            ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "stymphalian_nest"));
    public static final ResourceKey<PlacedFeature> STYMPHALIAN_NEST_PLACED =
            ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "stymphalian_nest"));

    /** 哈耳庇厄（W2）：敌对、群居 2–4、只随崖巢生成（风暴高地、爱琴海岸）。 */
    public static final DeferredHolder<EntityType<?>, EntityType<HarpyEntity>> HARPY =
            ModRegistries.ENTITY_TYPES.register("harpy",
                    () -> EntityType.Builder.of(HarpyEntity::new, MobCategory.MONSTER)
                            .sized(0.8F, 1.7F)
                            .eyeHeight(1.5F)
                            .clientTrackingRange(10)
                            .build("mythic_age:harpy"));

    /** 风暴之羽：哈耳庇厄掉落（→ 飞翼凉鞋、哈耳珀，T16 / T17）。 */
    public static final DeferredItem<Item> STORM_FEATHER = Content.item("storm_feather", GREEK)
            .lang("风暴之羽", "Storm Feather", "嵐の羽根")
            .register();

    public static final DeferredHolder<Feature<?>, HarpyNestFeature> HARPY_NEST_FEATURE =
            ModRegistries.FEATURES.register("harpy_nest", () -> new HarpyNestFeature(NoneFeatureConfiguration.CODEC));
    public static final ResourceKey<ConfiguredFeature<?, ?>> HARPY_NEST_CONFIGURED =
            ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "harpy_nest"));
    public static final ResourceKey<PlacedFeature> HARPY_NEST_PLACED =
            ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "harpy_nest"));

    /** 卡尔基诺斯（W4）：独居、领地型，随群系自然刷新（勒拿沼泽、爱琴海石岸）。 */
    public static final DeferredHolder<EntityType<?>, EntityType<KarkinosEntity>> KARKINOS =
            ModRegistries.ENTITY_TYPES.register("karkinos",
                    () -> EntityType.Builder.of(KarkinosEntity::new, MobCategory.MONSTER)
                            .sized(1.4F, 0.8F)
                            .eyeHeight(0.7F)
                            .clientTrackingRange(10)
                            .build("mythic_age:karkinos"));

    /** 巨蟹甲壳：卡尔基诺斯掉落（→ 抛光镜盾、青铜圆盾强化，T17）。 */
    public static final DeferredItem<Item> CRAB_SHELL = Content.item("crab_shell", GREEK)
            .lang("巨蟹甲壳", "Giant Crab Shell", "大蟹の甲羅")
            .register();

    /** 树仙女（W5）：中立、绑定本命橡木，只随圣林生成。 */
    public static final DeferredHolder<EntityType<?>, EntityType<HamadryadEntity>> HAMADRYAD =
            ModRegistries.ENTITY_TYPES.register("hamadryad",
                    () -> EntityType.Builder.of(HamadryadEntity::new, MobCategory.CREATURE)
                            .sized(0.6F, 1.8F)
                            .eyeHeight(1.6F)
                            .clientTrackingRange(10)
                            .build("mythic_age:hamadryad"));

    /** 根刺（树仙女的攻击）：裂纹 → 出刺 → 缩回。 */
    public static final DeferredHolder<EntityType<?>, EntityType<RootSpikeEntity>> ROOT_SPIKE =
            ModRegistries.ENTITY_TYPES.register("root_spike",
                    () -> EntityType.Builder.<RootSpikeEntity>of(RootSpikeEntity::new, MobCategory.MISC)
                            .sized(1.6F, 1.0F)
                            .clientTrackingRange(8)
                            .build("mythic_age:root_spike"));

    /** 本命橡木（D6）：原版橡木树皮压暗一档；掉落普通橡木原木（拿去放置不会变成别人的本命树）。不能去皮。 */
    public static final DeferredBlock<net.minecraft.world.level.block.RotatedPillarBlock> HAMADRYAD_OAK_LOG =
            Content.block("hamadryad_oak_log", GREEK, net.minecraft.world.level.block.RotatedPillarBlock::new)
                    .properties(() -> BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.OAK_LOG)
                            .mapColor(MapColor.COLOR_BROWN))
                    .model(new BlockModelSpec.Log(
                            io.github.happysnappingturtle.mythicage.content.Recolor.sprite("minecraft:block/oak_log", "hamadryad_oak_log"),
                            io.github.happysnappingturtle.mythicage.content.Recolor.sprite("minecraft:block/oak_log_top", "hamadryad_oak_log_top")))
                    .loot(LootSpec.other(() -> net.minecraft.world.item.Items.OAK_LOG))
                    .tags(net.minecraft.tags.BlockTags.LOGS_THAT_BURN, net.minecraft.tags.BlockTags.MINEABLE_WITH_AXE)
                    .lang("本命橡木", "Hamadryad Oak Log", "ハマドリュアスの樫の原木")
                    .register();

    /** 还愿彩带：圣林橡树下挂的花环与红白羊毛带。 */
    public static final DeferredBlock<VotiveRibbonBlock> VOTIVE_RIBBON = Content.block("votive_ribbon", GREEK, VotiveRibbonBlock::new)
            .properties(() -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).noCollission().instabreak()
                    .sound(SoundType.WOOL).noOcclusion().pushReaction(PushReaction.DESTROY))
            .model(BlockModelSpec.cross(ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "block/votive_ribbon")))
            .itemTexture(ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "block/votive_ribbon"))
            .lang("还愿彩带", "Votive Ribbon", "奉納のリボン")
            .register();

    /** 宁芙之露：树仙女的赠礼（→ 高级奠酒，T16）。 */
    public static final DeferredItem<Item> NYMPH_DEW = Content.item("nymph_dew", GREEK)
            .lang("宁芙之露", "Nymph's Dew", "ニンフの露")
            .register();

    public static final DeferredHolder<Feature<?>, HamadryadGroveFeature> HAMADRYAD_GROVE_FEATURE =
            ModRegistries.FEATURES.register("hamadryad_grove", () -> new HamadryadGroveFeature(NoneFeatureConfiguration.CODEC));
    public static final ResourceKey<ConfiguredFeature<?, ?>> HAMADRYAD_GROVE_CONFIGURED =
            ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "hamadryad_grove"));
    public static final ResourceKey<PlacedFeature> HAMADRYAD_GROVE_PLACED =
            ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "hamadryad_grove"));

    private GreekWild() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityAttributeCreationEvent e) -> {
            e.put(CENTAUR.get(), WildCentaurEntity.createWildAttributes().build());
            e.put(STYMPHALIAN_BIRD.get(), StymphalianBirdEntity.createAttributes().build());
            e.put(HARPY.get(), HarpyEntity.createAttributes().build());
            e.put(KARKINOS.get(), KarkinosEntity.createAttributes().build());
            e.put(HAMADRYAD.get(), HamadryadEntity.createAttributes().build());
        });
        // 自然刷新的野外神话生物：刷新条件由生物卡决定（村庄排除、区域密度），不要求黑暗
        modBus.addListener((net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent e) ->
                e.register(KARKINOS.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                        net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        io.github.happysnappingturtle.mythicage.wild.WildSpawning.natural("greek:karkinos"),
                        net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE));
    }
}
