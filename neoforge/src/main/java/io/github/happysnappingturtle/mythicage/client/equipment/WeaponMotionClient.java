package io.github.happysnappingturtle.mythicage.client.equipment;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.equipment.WeaponMotion;
import io.github.happysnappingturtle.mythicage.equipment.MeleeWeaponItem;
import io.github.happysnappingturtle.mythicage.equipment.WeaponFeedback;
import io.github.happysnappingturtle.mythicage.equipment.WeaponMotionState;
import io.github.happysnappingturtle.mythicage.power.PowerRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;

/** 挥空在清空冷却之前记为普通动作；实体命中由两端 AttackEntityEvent 判定。 */
@EventBusSubscriber(modid = MythicAgeIds.MOD_ID, value = Dist.CLIENT)
public final class WeaponMotionClient {
    private WeaponMotionClient() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onInput(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (!event.isAttack() || !event.shouldSwingHand() || mc.player == null || mc.hitResult == null
                || mc.hitResult.getType() == HitResult.Type.ENTITY
                || !(mc.player.getMainHandItem().getItem() instanceof MeleeWeaponItem weapon)) {
            return;
        }
        WeaponMotion motion = weapon.stats().motion();
        WeaponMotionState.remember(mc.player, mc.player.getMainHandItem(), motion);
        if (mc.hitResult.getType() == HitResult.Type.MISS && motion == WeaponMotion.THRUST) {
            mc.player.playSound(PowerRegistries.sound(WeaponFeedback.MISS), 0.35F, 1);
        }
    }

    public static void accept(WeaponMotionState.Payload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && payload.motion() >= 0 && payload.motion() < WeaponMotion.values().length
                && mc.level.getEntity(payload.entityId()) instanceof LivingEntity entity) {
            WeaponMotionState.remember(entity, entity.getMainHandItem(), WeaponMotion.values()[payload.motion()]);
        }
    }
}
