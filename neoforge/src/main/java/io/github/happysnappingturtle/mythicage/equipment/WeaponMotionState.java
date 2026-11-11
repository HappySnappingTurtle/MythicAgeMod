package io.github.happysnappingturtle.mythicage.equipment;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.equipment.CriticalHitRules;
import io.github.happysnappingturtle.mythicage.core.equipment.WeaponMotion;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** 出手时锁定动作，冷却清零、落地或抬升都不能让同一次挥击中途换动作。 */
public final class WeaponMotionState {
    private record Swing(Item item, WeaponMotion motion, long tick) {
    }

    // 不保存实体的强引用；客户端与集成服务器可同时访问，各自的实体键互不混用。
    private static final Map<LivingEntity, Swing> SWINGS = Collections.synchronizedMap(new WeakHashMap<>());

    public record Payload(int entityId, int motion) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "weapon_motion"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Payload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Payload::entityId, ByteBufCodecs.VAR_INT, Payload::motion, Payload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private WeaponMotionState() {
    }

    public static boolean critical(Player p, boolean livingTarget) {
        return CriticalHitRules.isCritical(p.fallDistance, p.onGround(), p.onClimbable(), p.isInWater(),
                p.hasEffect(MobEffects.BLINDNESS), p.isPassenger(), p.isSprinting(), p.getAttackStrengthScale(0.5F), livingTarget);
    }

    public static void remember(LivingEntity entity, ItemStack stack, WeaponMotion motion) {
        SWINGS.put(entity, new Swing(stack.getItem(), motion, entity.level().getGameTime()));
    }

    /** 未经过实体攻击事件的挥手（挖掘、挥空、盾后刺击）不得复用上次暴击。 */
    public static void prepareSwing(LivingEntity entity, ItemStack stack) {
        Swing previous = SWINGS.get(entity);
        if (stack.getItem() instanceof MeleeWeaponItem weapon && (previous == null || previous.item() != stack.getItem()
                || entity.level().getGameTime() - previous.tick() > 1)) {
            remember(entity, stack, weapon.stats().motion());
        }
    }

    public static WeaponMotion current(LivingEntity entity, ItemStack stack) {
        if (!(stack.getItem() instanceof MeleeWeaponItem weapon)) {
            return WeaponMotion.SLASH;
        }
        Swing swing = SWINGS.get(entity);
        // 挥击期间保留状态（包括挖掘疲劳延长的动画）；闲置后不沿用上一次暴击。
        if (swing != null && swing.item() == stack.getItem()
                && (entity.swinging || entity.level().getGameTime() - swing.tick() <= 1)) {
            return swing.motion();
        }
        return weapon.stats().motion();
    }

    public static void onAttack(AttackEntityEvent event) {
        Player p = event.getEntity();
        if (p.getMainHandItem().getItem() instanceof MeleeWeaponItem weapon) {
            remember(p, p.getMainHandItem(), critical(p, event.getTarget() instanceof LivingEntity)
                    ? weapon.stats().critMotion() : weapon.stats().motion());
        }
    }

    /** 最终暴击结果允许其他模组修改；服务端声音及旁观者动作跟随真正结算的结果。 */
    public static void onCritical(CriticalHitEvent event) {
        Player p = event.getEntity();
        if (p.getMainHandItem().getItem() instanceof MeleeWeaponItem weapon) {
            WeaponMotion motion = event.isCriticalHit() ? weapon.stats().critMotion() : weapon.stats().motion();
            remember(p, p.getMainHandItem(), motion);
            if (!p.level().isClientSide()) {
                PacketDistributor.sendToPlayersTrackingEntity(p, new Payload(p.getId(), motion.ordinal()));
            }
        }
    }
}
