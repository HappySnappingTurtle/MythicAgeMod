package io.github.happysnappingturtle.mythicage.equipment;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 举盾时攻击（T17 §4.3，通用）：原版举盾期间攻击键被吞掉，客户端改发这个包；服务器校验后结算。
 * 长矛：伤害 = 攻击伤害 × thrust_multiplier（0.6），与普通攻击共用冷却（D8），格挡状态不中断。
 */
public final class ShieldAttack {
    /** 冷却比例低于它时不出手。 */
    public static final float MIN_STRENGTH = 0.9F;

    public record Payload(int entityId) implements CustomPacketPayload {
        public static final Type<Payload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "shield_attack"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Payload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, Payload::entityId, Payload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private ShieldAttack() {
    }

    /** 副手的盾正在格挡（与原版 isBlocking 相同：举起满 5 刻）。 */
    public static boolean shieldUp(Player p) {
        if (!p.isUsingItem() || p.getUsedItemHand() != InteractionHand.OFF_HAND) {
            return false;
        }
        ItemStack use = p.getUseItem();
        return use.canPerformAction(ItemAbilities.SHIELD_BLOCK) && p.isBlocking();
    }

    /** 主手拿着能举盾出手的武器，副手的盾举着。客户端用它决定是否改发刺击包。 */
    public static boolean ready(Player p) {
        return p.getMainHandItem().is(EquipmentTags.SHIELD_THRUST) && p.getMainHandItem().getItem() instanceof MeleeWeaponItem
                && shieldUp(p);
    }

    public static void handle(Payload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer p) {
                thrust(p, p.level().getEntity(payload.entityId()));
            }
        });
    }

    /** 服务器结算；返回是否出手（伪造或条件不满足时 false，什么都不做）。 */
    public static boolean thrust(ServerPlayer p, Entity target) {
        if (!ready(p) || !(target instanceof LivingEntity le) || !le.isAlive() || target == p
                || !p.canInteractWithEntity(target, 0) || !p.hasLineOfSight(target)) {
            return false;
        }
        if (p.getAttackStrengthScale(0.5F) < MIN_STRENGTH) {
            return false;
        }
        ItemStack weapon = p.getMainHandItem();
        double mult = ((MeleeWeaponItem) weapon.getItem()).stats().thrustMultiplier();
        if (mult <= 0) {
            return false;
        }
        float damage = (float) (p.getAttributeValue(Attributes.ATTACK_DAMAGE) * mult);
        // 盾后刺击使用普通动作；成功伤害才播放通用命中反馈，不改变原有伤害和窗口结算。
        var motion = ((MeleeWeaponItem) weapon.getItem()).stats().motion();
        WeaponMotionState.remember(p, weapon, motion);
        PacketDistributor.sendToPlayersTrackingEntity(p,
                new WeaponMotionState.Payload(p.getId(), motion.ordinal()));
        CounterEvents.thrusting(() -> {
            if (le.hurt(p.damageSources().playerAttack(p), damage)) {
                WeaponFeedback.hit(weapon, le, p);
            }
        });
        p.resetAttackStrengthTicker();
        weapon.hurtAndBreak(1, p, EquipmentSlot.MAINHAND);
        p.swing(InteractionHand.MAIN_HAND, true);
        return true;
    }
}
