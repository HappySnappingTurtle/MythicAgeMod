package io.github.happysnappingturtle.mythicage.content.greek.village.soldier;

import io.github.happysnappingturtle.mythicage.registry.ModRegistries;
import io.github.happysnappingturtle.mythicage.core.village.soldier.SoldierProfile;
import io.github.happysnappingturtle.mythicage.village.soldier.RangedAttacks;
import io.github.happysnappingturtle.mythicage.village.soldier.SoldierAbility;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * 希腊士兵的接入（T20 设计文档 §9）：两个独有机制、投矛实体与它的发射。数据在 {@code village_profession/greek/hoplite.json}、
 * {@code javelineer.json} 与 {@code village_civ/greek.json} 的 {@code soldiers} 段。
 */
public final class GreekSoldiers {
    public static final DeferredHolder<EntityType<?>, EntityType<JavelinEntity>> JAVELIN =
            ModRegistries.ENTITY_TYPES.register("javelin",
                    () -> EntityType.Builder.<JavelinEntity>of(JavelinEntity::new, MobCategory.MISC)
                            .sized(0.4F, 0.4F)
                            .clientTrackingRange(6)
                            .updateInterval(1)
                            .build("mythic_age:javelin"));

    private GreekSoldiers() {
    }

    public static void init() {
        SoldierAbility.register(PrayerGuard.ID, new PrayerGuard());
        SoldierAbility.register(BrazierSignal.ID, new BrazierSignal());
        io.github.happysnappingturtle.mythicage.village.soldier.VillageLandmarks.register(BrazierSignal.LANDMARK);
        RangedAttacks.Launcher javelin = (level, soldier, village, item, damage, from, velocity, profile) -> {
            JavelinEntity j = JAVELIN.get().create(level);
            j.setOwner(soldier);
            j.moveTo(from.x, from.y, from.z, soldier.getYRot(), 0);
            j.setDeltaMovement(velocity);
            SoldierProfile.Ranged r = profile.ranged();
            j.configure(village, item, damage, r.gravity(), r.waterDrag(), profile.ammo().lifetime());
            level.addFreshEntity(j);
            return j;
        };
        RangedAttacks.register("mythic_age:javelin", javelin);
        RangedAttacks.register("mythic_age:rough_javelin", javelin);
    }
}
