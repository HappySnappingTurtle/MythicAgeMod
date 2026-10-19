package io.github.happysnappingturtle.mythicage.client.power;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.power.AuraPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/** 客户端记下的实体外观状态（T13 设计文档 §1.4）：实体 id → 外观名 → 开始 / 结束的本地游戏刻。 */
@EventBusSubscriber(modid = MythicAgeIds.MOD_ID, value = Dist.CLIENT)
public final class ClientAuras {
    /** {@code start} 是本客户端第一次收到的时刻（补发时也从收到算起）。 */
    public record State(long start, long end) {
        public long age(long now) {
            return now - start;
        }

        public long left(long now) {
            return end - now;
        }
    }

    private static final Map<Integer, Map<String, State>> ALL = new HashMap<>();

    private ClientAuras() {
    }

    public static void accept(AuraPayload p) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        if (p.remainingTicks() <= 0) {
            Map<String, State> m = ALL.get(p.entityId());
            if (m != null) {
                m.remove(p.aura());
                if (m.isEmpty()) {
                    ALL.remove(p.entityId());
                }
            }
            return;
        }
        long end = p.remainingTicks() == Integer.MAX_VALUE ? Long.MAX_VALUE : now + p.remainingTicks();
        ALL.computeIfAbsent(p.entityId(), k -> new HashMap<>())
                .merge(p.aura(), new State(now, end), (old, neu) -> new State(old.start(), neu.end()));
    }

    @Nullable
    public static State get(int entityId, String aura) {
        Map<String, State> m = ALL.get(entityId);
        return m == null ? null : m.get(aura);
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || ALL.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        // 瘟疫标记：头顶每 10 刻补一个暗红标记粒子
        if (now % 10 == 0 && !Minecraft.getInstance().isPaused()) {
            for (Map.Entry<Integer, Map<String, State>> e : ALL.entrySet()) {
                State s = e.getValue().get("plagued");
                if (s != null && s.end() > now) {
                    net.minecraft.world.entity.Entity ent = level.getEntity(e.getKey());
                    if (ent != null) {
                        level.addParticle(io.github.happysnappingturtle.mythicage.power.PowerRegistries.PLAGUE_MARK.get(),
                                ent.getX(), ent.getY() + ent.getBbHeight() + 0.35, ent.getZ(), 0, 0, 0);
                    }
                }
            }
        }
        if (now % 20 != 0) {
            return;
        }
        ALL.entrySet().removeIf(e -> {
            e.getValue().values().removeIf(s -> s.end() < now);
            return e.getValue().isEmpty() || level.getEntity(e.getKey()) == null;
        });
    }

    @SubscribeEvent
    static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ALL.clear();
    }
}
