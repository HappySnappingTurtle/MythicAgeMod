package io.github.happysnappingturtle.mythicage.wild.greek;

import io.github.happysnappingturtle.mythicage.content.Content;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.entity.centaur.WildCentaurEntity;
import io.github.happysnappingturtle.mythicage.registry.ModRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
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
    /** 营地：阿卡迪亚子区里平均每 200 个区块尝试 1 处（地面不平就放弃），约每 256×256 格一群。 */
    public static final int CAMP_RARITY = 200;

    private GreekWild() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityAttributeCreationEvent e) ->
                e.put(CENTAUR.get(), WildCentaurEntity.createWildAttributes().build()));
    }
}
