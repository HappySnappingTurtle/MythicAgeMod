package io.github.happysnappingturtle.mythicage.player;

import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.player.FavorRules;
import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;
import io.github.happysnappingturtle.mythicage.core.player.tx.Transactions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端访问玩家神话数据的唯一入口（T4 设计文档 §4）。
 * 修改后把玩家标记为「需要同步」，在该服务端 tick 结束时统一发送，同一 tick 内多次修改只同步一次。
 */
public final class MythData {
    /** 需要同步的玩家（保存实例：重生后会被新实例覆盖；不在玩家列表中的假玩家也能同步）。 */
    private static final Map<UUID, ServerPlayer> DIRTY = new LinkedHashMap<>();
    /** 每个玩家的同步次数，只供 GameTest 验证同步去重。 */
    private static final Map<UUID, Integer> SYNC_COUNTS = new ConcurrentHashMap<>();

    private MythData() {
    }

    public static PlayerMythData get(ServerPlayer player) {
        return player.getData(ModAttachments.PLAYER_DATA);
    }

    /** 当前上限（含圣物加成）。 */
    public static long cap(ServerPlayer player, Civilization civ) {
        return FavorRules.cap(get(player).civ(civ).age(), CapBonusProvider.Holder.get().capBonus(player, civ));
    }

    /** 执行事务；提交成功时标记同步。 */
    public static Transactions.Result transact(ServerPlayer player, Transactions.Request request) {
        Transactions.Result result = Transactions.execute(get(player), request);
        if (result.committed()) {
            markDirty(player);
        }
        return result;
    }

    /** 直接修改数据后调用（运维命令、读档迁移等）：递增版本并标记同步。 */
    public static void changed(ServerPlayer player) {
        get(player).markChanged();
        markDirty(player);
    }

    public static synchronized void markDirty(ServerPlayer player) {
        DIRTY.put(player.getUUID(), player);
    }

    /** 供 GameTest 使用。 */
    public static synchronized boolean isDirty(UUID id) {
        return DIRTY.containsKey(id);
    }

    /** 供 GameTest 使用：该玩家累计同步次数。 */
    public static int syncCount(UUID id) {
        return SYNC_COUNTS.getOrDefault(id, 0);
    }

    public static void onServerTickPost(ServerTickEvent.Post event) {
        List<ServerPlayer> pending;
        synchronized (MythData.class) {
            if (DIRTY.isEmpty()) {
                return;
            }
            pending = new ArrayList<>(DIRTY.values());
            DIRTY.clear();
        }
        MinecraftServer server = event.getServer();
        for (ServerPlayer marked : pending) {
            // 优先用玩家列表中的当前实例；不在列表中时（如假玩家）用标记时的实例
            ServerPlayer player = server.getPlayerList().getPlayer(marked.getUUID());
            if (player == null && !marked.hasDisconnected()) {
                player = marked;
            }
            if (player != null) {
                sync(player);
            }
        }
    }

    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    static PlayerSyncPayload snapshot(ServerPlayer player) {
        PlayerMythData data = get(player);
        List<PlayerSyncPayload.CivEntry> civs = new ArrayList<>();
        for (Civilization civ : Civilization.values()) {
            PlayerMythData.CivState s = data.civ(civ);
            civs.add(new PlayerSyncPayload.CivEntry(civ, s.favor(), s.age(), cap(player, civ)));
        }
        return new PlayerSyncPayload(data.dataVersion(), civs);
    }

    private static void sync(ServerPlayer player) {
        SYNC_COUNTS.merge(player.getUUID(), 1, Integer::sum);
        if (player.connection != null) {
            PacketDistributor.sendToPlayer(player, snapshot(player));
        }
    }
}
