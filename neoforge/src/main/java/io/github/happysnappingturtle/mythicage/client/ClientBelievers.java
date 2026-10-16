package io.github.happysnappingturtle.mythicage.client;

import io.github.happysnappingturtle.mythicage.content.greek.prayer.BelieverStatePayload;
import it.unimi.dsi.fastutil.ints.Int2ByteOpenHashMap;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

/**
 * 客户端：村民的信仰状态（T10 D2），按实体 id 保存。只用于渲染橄榄枝饰与跪姿，不参与任何判定。
 */
public final class ClientBelievers {
    private static final Int2ByteOpenHashMap FLAGS = new Int2ByteOpenHashMap();

    private ClientBelievers() {
    }

    public static void accept(BelieverStatePayload payload) {
        if (payload.flags() == 0) {
            FLAGS.remove(payload.entityId());
        } else {
            FLAGS.put(payload.entityId(), payload.flags());
        }
    }

    public static boolean believer(Entity e) {
        return (FLAGS.get(e.getId()) & BelieverStatePayload.BELIEVER) != 0;
    }

    public static boolean praying(Entity e) {
        return (FLAGS.get(e.getId()) & BelieverStatePayload.PRAYING) != 0;
    }

    public static void onLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            FLAGS.remove(event.getEntity().getId());
        }
    }

    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        FLAGS.clear();
    }
}
