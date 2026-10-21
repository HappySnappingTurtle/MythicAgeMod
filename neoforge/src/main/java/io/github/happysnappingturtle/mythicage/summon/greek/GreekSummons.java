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

    public static final DeferredItem<SummonTokenItem> MINOTAUR_TOKEN = Content.item("summon_token_minotaur", GREEK,
                    p -> new SummonTokenItem("greek:minotaur", p))
            .properties(p -> p.rarity(Rarity.UNCOMMON))
            .lang("弥诺陶洛斯召唤符", "Minotaur Summoning Tablet", "ミノタウロス召喚の札")
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
    }
}
