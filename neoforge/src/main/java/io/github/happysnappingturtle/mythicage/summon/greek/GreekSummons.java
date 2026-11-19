package io.github.happysnappingturtle.mythicage.summon.greek;

import io.github.happysnappingturtle.mythicage.content.Content;
import io.github.happysnappingturtle.mythicage.registry.ModRegistries;
import io.github.happysnappingturtle.mythicage.summon.KerykeionItem;
import io.github.happysnappingturtle.mythicage.summon.SummonTokenItem;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

import static io.github.happysnappingturtle.mythicage.core.player.Civilization.GREEK;

/**
 * 希腊神话单位（T14）：召唤版实体、召唤符、号令之物「传令杖」。单位卡与数值在数据包
 * {@code summon_unit/greek/*.json}、{@code entity_stats/greek/summon_*.json}。
 */
public final class GreekSummons {
    /** 召唤版弥诺陶洛斯：碰撞与 T7 弥诺陶洛斯相同；MISC 类别，不占自然生成的生物上限。 */
    public static final DeferredHolder<EntityType<?>, EntityType<SummonMinotaurEntity>> MINOTAUR =
            ModRegistries.ENTITY_TYPES.register("summon_minotaur",
                    () -> EntityType.Builder.of(SummonMinotaurEntity::new, MobCategory.MISC)
                            .sized(1.1F, 2.5F)
                            .eyeHeight(2.2F)
                            .clientTrackingRange(10)
                            .build("mythic_age:summon_minotaur"));

    /** 召唤版天马（T14 §11.4）：碰撞 0.9 × 1.5（翅膀不进碰撞）；骑手坐在鬐甲后方。 */
    public static final DeferredHolder<EntityType<?>, EntityType<SummonPegasusEntity>> PEGASUS =
            ModRegistries.ENTITY_TYPES.register("summon_pegasus",
                    () -> EntityType.Builder.of(SummonPegasusEntity::new, MobCategory.MISC)
                            .sized(0.9F, 1.5F)
                            .eyeHeight(1.35F)
                            .passengerAttachments(new net.minecraft.world.phys.Vec3(0, 1.3, -0.1))
                            .clientTrackingRange(10)
                            .build("mythic_age:summon_pegasus"));

    public static final DeferredItem<SummonTokenItem> PEGASUS_TOKEN = Content.item("summon_token_pegasus", GREEK,
                    p -> new SummonTokenItem("greek:pegasus", p))
            .properties(p -> p.rarity(Rarity.UNCOMMON))
            .lang("天马召唤符", "Pegasus Summoning Tablet", "ペガサス召喚の札")
            .register();

    /** 召唤版半人马（T14 §11.5）：碰撞 1.1 × 2.5。 */
    public static final DeferredHolder<EntityType<?>, EntityType<SummonCentaurEntity>> CENTAUR =
            ModRegistries.ENTITY_TYPES.register("summon_centaur",
                    () -> EntityType.Builder.of(SummonCentaurEntity::new, MobCategory.MISC)
                            .sized(1.1F, 2.5F)
                            .eyeHeight(2.2F)
                            .clientTrackingRange(10)
                            .build("mythic_age:summon_centaur"));

    /** 半人马掷出的松树干。 */
    public static final DeferredHolder<EntityType<?>, EntityType<CentaurPineEntity>> CENTAUR_PINE =
            ModRegistries.ENTITY_TYPES.register("centaur_pine",
                    () -> EntityType.Builder.<CentaurPineEntity>of(CentaurPineEntity::new, MobCategory.MISC)
                            .sized(0.7F, 0.7F)
                            .clientTrackingRange(6)
                            .updateInterval(1)
                            .build("mythic_age:centaur_pine"));

    public static final DeferredItem<SummonTokenItem> CENTAUR_TOKEN = Content.item("summon_token_centaur", GREEK,
                    p -> new SummonTokenItem("greek:centaur", p))
            .properties(p -> p.rarity(Rarity.UNCOMMON))
            .lang("半人马召唤符", "Centaur Summoning Tablet", "ケンタウロス召喚の札")
            .register();

    public static final DeferredItem<SummonTokenItem> MINOTAUR_TOKEN = Content.item("summon_token_minotaur", GREEK,
                    p -> new SummonTokenItem("greek:minotaur", p))
            .properties(p -> p.rarity(Rarity.UNCOMMON))
            .lang("弥诺陶洛斯召唤符", "Minotaur Summoning Tablet", "ミノタウロス召喚の札")
            .register();

    /** 召唤版独眼巨人（T25a §3）：碰撞 1.8 × 3.8（RIG-CYCLOPS）。 */
    public static final DeferredHolder<EntityType<?>, EntityType<SummonCyclopsEntity>> CYCLOPS =
            ModRegistries.ENTITY_TYPES.register("summon_cyclops",
                    () -> EntityType.Builder.of(SummonCyclopsEntity::new, MobCategory.MISC)
                            .sized(1.8F, 3.8F)
                            .eyeHeight(3.35F)
                            .clientTrackingRange(10)
                            .build("mythic_age:summon_cyclops"));

    public static final DeferredItem<SummonTokenItem> CYCLOPS_TOKEN = Content.item("summon_token_cyclops", GREEK,
                    p -> new SummonTokenItem("greek:cyclops", p))
            .properties(p -> p.rarity(Rarity.UNCOMMON))
            .lang("独眼巨人召唤符", "Cyclops Summoning Tablet", "キュクロプス召喚の札")
            .register();

    public static final DeferredItem<KerykeionItem> KERYKEION = Content.item("kerykeion", GREEK, KerykeionItem::new)
            .properties(p -> p.rarity(Rarity.UNCOMMON))
            .lang("传令杖", "Kerykeion", "ケリュケイオン")
            .register();

    private GreekSummons() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityAttributeCreationEvent e) ->
                e.put(MINOTAUR.get(), SummonMinotaurEntity.createSummonAttributes().build()));
        modBus.addListener((EntityAttributeCreationEvent e) ->
                e.put(PEGASUS.get(), SummonPegasusEntity.createSummonAttributes().build()));
        modBus.addListener((EntityAttributeCreationEvent e) ->
                e.put(CENTAUR.get(), SummonCentaurEntity.createSummonAttributes().build()));
        modBus.addListener((EntityAttributeCreationEvent e) ->
                e.put(CYCLOPS.get(), SummonCyclopsEntity.createAttributes().build()));
    }
}
