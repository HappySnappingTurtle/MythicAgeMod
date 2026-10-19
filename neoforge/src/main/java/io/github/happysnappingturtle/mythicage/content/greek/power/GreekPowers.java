package io.github.happysnappingturtle.mythicage.content.greek.power;

import io.github.happysnappingturtle.mythicage.content.Content;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.power.Auras;
import io.github.happysnappingturtle.mythicage.power.GodPowerItem;
import io.github.happysnappingturtle.mythicage.power.GodPowers;
import io.github.happysnappingturtle.mythicage.power.PowerRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

import static io.github.happysnappingturtle.mythicage.core.player.Civilization.GREEK;

/**
 * 希腊神力（T13）：道具、效果代码、药水效果、伤害类型。神力 id 与物品 id 相同（{@code gp_gr_*}）；数值在
 * {@code data/mythic_age/mythic_age/god_power/}。
 */
public final class GreekPowers {
    public static final String BOLT = "gp_gr_bolt";
    public static final String RESTORATION = "gp_gr_restoration";
    public static final String BRONZE = "gp_gr_bronze";
    public static final String PESTILENCE = "gp_gr_pestilence";
    public static final String CURSE = "gp_gr_curse";

    public static final DeferredItem<GodPowerItem> BOLT_ITEM = item(BOLT, "雷击", "Bolt", "雷撃");
    public static final DeferredItem<GodPowerItem> RESTORATION_ITEM = item(RESTORATION, "复原", "Restoration", "復元");
    public static final DeferredItem<GodPowerItem> BRONZE_ITEM = item(BRONZE, "青铜", "Bronze", "青銅");
    public static final DeferredItem<GodPowerItem> PESTILENCE_ITEM = item(PESTILENCE, "瘟疫", "Pestilence", "疫病");
    public static final DeferredItem<GodPowerItem> CURSE_ITEM = item(CURSE, "诅咒", "Curse", "呪い");

    public static final DeferredHolder<MobEffect, BronzeBodyEffect> BRONZE_BODY =
            PowerRegistries.EFFECTS.register("bronze_body", BronzeBodyEffect::new);
    public static final DeferredHolder<MobEffect, PlaguedEffect> PLAGUED =
            PowerRegistries.EFFECTS.register("plagued", PlaguedEffect::new);

    /** 被诅咒的猪（D8）：尺寸同原版猪。 */
    public static final DeferredHolder<net.minecraft.world.entity.EntityType<?>, net.minecraft.world.entity.EntityType<CursedPig>> CURSED_PIG =
            io.github.happysnappingturtle.mythicage.registry.ModRegistries.ENTITY_TYPES.register("cursed_pig",
                    () -> net.minecraft.world.entity.EntityType.Builder.of(CursedPig::new, net.minecraft.world.entity.MobCategory.CREATURE)
                            .sized(0.9F, 0.9F)
                            .eyeHeight(0.765F)
                            .clientTrackingRange(10)
                            .build("mythic_age:cursed_pig"));
    /** 瘟疫封禁的远程武器（拉弓、上弩、投三叉戟）。 */
    public static final TagKey<net.minecraft.world.item.Item> RANGED_WEAPONS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "ranged_weapons"));
    /** 被诅咒的生物（以后善恶系统据此不按「杀动物」计）。 */
    public static final TagKey<net.minecraft.world.entity.EntityType<?>> CURSED = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "cursed"));

    public static final ResourceKey<DamageType> DIVINE_BOLT =
            ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "divine_bolt"));
    /** 复原清除的效果（石化效果实现后加进来）。 */
    public static final TagKey<MobEffect> RESTORATION_CLEANSES =
            TagKey.create(Registries.MOB_EFFECT, ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "restoration_cleanses"));

    private GreekPowers() {
    }

    private static DeferredItem<GodPowerItem> item(String id, String zh, String en, String ja) {
        return Content.item(id, GREEK, p -> new GodPowerItem(id, p))
                .properties(p -> p.rarity(Rarity.UNCOMMON))
                .lang(zh, en, ja)
                .register();
    }

    /** 触发类加载（物品、效果注册项）并登记效果代码与事件。 */
    public static void init(net.neoforged.bus.api.IEventBus modBus, net.neoforged.bus.api.IEventBus game) {
        modBus.addListener((net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent e) ->
                e.put(CURSED_PIG.get(), net.minecraft.world.entity.animal.Pig.createAttributes().build()));
        game.addListener(BronzePower::onExpired);
        game.addListener(PestilencePower::onUseItemStart);
        game.addListener(PestilencePower::onEntityJoin);
        game.addListener(PestilencePower::onSpawnCheck);
        io.github.happysnappingturtle.mythicage.power.PowerZones.registerKind(PestilencePower.KIND, PestilencePower.ZONE);
        io.github.happysnappingturtle.mythicage.power.SpawnerStall.register((level, pos) ->
                io.github.happysnappingturtle.mythicage.power.PowerZones.covered(level, pos, PestilencePower.KIND));
        GodPowers.register(new PestilencePower());
        GodPowers.register(new CursePower());
        Auras.register(PLAGUED.getKey(), "plagued");
        GodPowers.register(new BoltPower());
        GodPowers.register(new RestorationPower());
        GodPowers.register(new BronzePower());
        Auras.register(BRONZE_BODY.getKey(), "bronze");
    }

    /** 在某处播放神力音效（所有附近玩家都听得到）。 */
    static void play(Level level, Vec3 at, String sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, PowerRegistries.sound(sound), SoundSource.PLAYERS, volume, pitch);
    }

    static void play(Entity e, String sound) {
        play(e.level(), e.position(), sound, 1f, 1f);
    }
}
