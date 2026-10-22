package io.github.happysnappingturtle.mythicage.client;

import io.github.happysnappingturtle.mythicage.summon.SkyEyePayload;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/**
 * 天眼标记（T14 §11.4）：实体 id → 到这一刻（客户端世界时间）为止显示描边。只在收到包的主人客户端上有效，
 * 由 {@code MinecraftGlowMixin} 读取。
 */
public final class ClientSkyEye {
    private static final Int2LongOpenHashMap UNTIL = new Int2LongOpenHashMap();

    private ClientSkyEye() {
    }

    public static void accept(SkyEyePayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        long until = mc.level.getGameTime() + payload.ticks();
        for (int id : payload.entityIds()) {
            UNTIL.put(id, until);
        }
    }

    public static boolean marked(Entity e) {
        if (UNTIL.isEmpty()) {
            return false;
        }
        long until = UNTIL.getOrDefault(e.getId(), Long.MIN_VALUE);
        if (until == Long.MIN_VALUE) {
            return false;
        }
        if (e.level().getGameTime() >= until || !e.isAlive()) {
            UNTIL.remove(e.getId());
            return false;
        }
        return true;
    }

    /** 换世界、断线时清空。 */
    public static void clear() {
        UNTIL.clear();
    }
}
