package io.github.happysnappingturtle.mythicage.power;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 外观状态的同步（T13 设计文档 §1.4）：登记过的药水效果被添加 / 刷新 / 移除 / 到期时，发给追踪该实体的玩家（含它自己）；
 * 玩家开始追踪某实体时补发。客户端据此画青铜外壳、瘟疫标记等。
 */
public final class Auras {
    private static final Map<ResourceKey<MobEffect>, String> BY_EFFECT = new ConcurrentHashMap<>();
    /** 只供 GameTest：最近发出的负载，最多 128 条。 */
    private static final Deque<AuraPayload> RECENT = new ConcurrentLinkedDeque<>();

    private Auras() {
    }

    /** 让一个药水效果带外观；{@code aura} 是客户端认的外观名。 */
    public static void register(ResourceKey<MobEffect> effect, String aura) {
        BY_EFFECT.put(effect, aura);
    }

    private static String auraOf(Holder<MobEffect> effect) {
        return effect.unwrapKey().map(BY_EFFECT::get).orElse(null);
    }

    public static void onAdded(MobEffectEvent.Added event) {
        MobEffectInstance e = event.getEffectInstance();
        String aura = auraOf(e.getEffect());
        if (aura != null && !event.getEntity().level().isClientSide()) {
            send(event.getEntity(), aura, e.isInfiniteDuration() ? Integer.MAX_VALUE : e.getDuration());
        }
    }

    public static void onRemoved(MobEffectEvent.Remove event) {
        if (event.getEffectInstance() != null) {
            String aura = auraOf(event.getEffectInstance().getEffect());
            if (aura != null && !event.getEntity().level().isClientSide()) {
                send(event.getEntity(), aura, 0);
            }
        }
    }

    public static void onExpired(MobEffectEvent.Expired event) {
        if (event.getEffectInstance() != null) {
            String aura = auraOf(event.getEffectInstance().getEffect());
            if (aura != null && !event.getEntity().level().isClientSide()) {
                send(event.getEntity(), aura, 0);
            }
        }
    }

    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof LivingEntity l && event.getEntity() instanceof ServerPlayer to) {
            for (AuraPayload p : current(l)) {
                record(p);
                PacketDistributor.sendToPlayer(to, p);
            }
        }
    }

    /** 实体当前全部外观状态（补发用）。 */
    static List<AuraPayload> current(LivingEntity l) {
        List<AuraPayload> out = new ArrayList<>();
        for (MobEffectInstance e : l.getActiveEffects()) {
            String aura = auraOf(e.getEffect());
            if (aura != null) {
                out.add(new AuraPayload(l.getId(), aura, e.isInfiniteDuration() ? Integer.MAX_VALUE : e.getDuration()));
            }
        }
        return out;
    }

    private static void send(LivingEntity e, String aura, int remaining) {
        AuraPayload p = new AuraPayload(e.getId(), aura, remaining);
        record(p);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(e, p);
    }

    private static void record(AuraPayload p) {
        RECENT.addLast(p);
        while (RECENT.size() > 128) {
            RECENT.pollFirst();
        }
    }

    /** 只供 GameTest：某实体最近发出的外观负载。 */
    public static List<AuraPayload> sentForTest(int entityId) {
        return RECENT.stream().filter(p -> p.entityId() == entityId).toList();
    }

    public static List<AuraPayload> currentForTest(LivingEntity l) {
        return current(l);
    }
}
