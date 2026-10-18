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

    public static final DeferredItem<GodPowerItem> BOLT_ITEM = item(BOLT, "雷击", "Bolt", "雷撃");
    public static final DeferredItem<GodPowerItem> RESTORATION_ITEM = item(RESTORATION, "复原", "Restoration", "復元");
    public static final DeferredItem<GodPowerItem> BRONZE_ITEM = item(BRONZE, "青铜", "Bronze", "青銅");

    public static final DeferredHolder<MobEffect, BronzeBodyEffect> BRONZE_BODY =
            PowerRegistries.EFFECTS.register("bronze_body", BronzeBodyEffect::new);

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
    public static void init(net.neoforged.bus.api.IEventBus game) {
        game.addListener(BronzePower::onExpired);
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
