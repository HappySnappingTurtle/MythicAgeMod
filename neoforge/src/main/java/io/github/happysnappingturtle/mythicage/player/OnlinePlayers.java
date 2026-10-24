package io.github.happysnappingturtle.mythicage.player;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按 UUID 找在线玩家（召唤单位、圣物、善恶值的责任人都要用）。GameTest 的 FakePlayer 不在玩家列表里，登记后按「在线」处理。
 */
public final class OnlinePlayers {
    private static final Map<UUID, ServerPlayer> TEST = new ConcurrentHashMap<>();

    private OnlinePlayers() {
    }

    @Nullable
    public static ServerPlayer find(MinecraftServer server, @Nullable UUID id) {
        if (id == null) {
            return null;
        }
        ServerPlayer p = server.getPlayerList().getPlayer(id);
        return p != null ? p : TEST.get(id);
    }

    /** 这个维度里的在线玩家（含 GameTest 登记的 FakePlayer）。 */
    public static java.util.List<ServerPlayer> in(net.minecraft.server.level.ServerLevel level) {
        java.util.List<ServerPlayer> out = new java.util.ArrayList<>(level.players());
        TEST.values().stream().filter(p -> p.level() == level && !out.contains(p)).forEach(out::add);
        return out;
    }

    public static void forTest(ServerPlayer player, boolean online) {
        if (online) {
            TEST.put(player.getUUID(), player);
        } else {
            TEST.remove(player.getUUID());
        }
    }
}
