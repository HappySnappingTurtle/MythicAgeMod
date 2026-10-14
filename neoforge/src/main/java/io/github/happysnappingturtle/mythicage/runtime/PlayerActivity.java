package io.github.happysnappingturtle.mythicage.runtime;

import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig;
import io.github.happysnappingturtle.mythicage.core.runtime.ActivityRules;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.bus.api.IEventBus;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;
import java.util.UUID;

/**
 * 玩家活跃采集（T5 设计文档 §2）。有效输入：移动 ≥1 格、视角变化 ≥1°、与方块或实体交互、
 * 打开容器、聊天或命令；登录也算一次。记录只在内存中，下线即清除（下线视为不活跃）。
 */
public final class PlayerActivity {
    /** 每隔多少 tick 检查一次位移与视角。 */
    private static final int MOVE_CHECK_INTERVAL = 10;

    private static final class Entry {
        ServerPlayer player;
        long lastActiveTick;
        double x;
        double y;
        double z;
        float yRot;
        float xRot;
    }

    private static final Map<UUID, Entry> ENTRIES = new HashMap<>();

    private PlayerActivity() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(PlayerActivity::onLoggedIn);
        bus.addListener(PlayerActivity::onLoggedOut);
        bus.addListener(PlayerActivity::onRespawn);
        bus.addListener(PlayerActivity::onPlayerTick);
        bus.addListener((PlayerInteractEvent.RightClickBlock e) -> mark(e.getEntity()));
        bus.addListener((PlayerInteractEvent.RightClickItem e) -> mark(e.getEntity()));
        bus.addListener((PlayerInteractEvent.LeftClickBlock e) -> mark(e.getEntity()));
        bus.addListener((PlayerInteractEvent.EntityInteract e) -> mark(e.getEntity()));
        bus.addListener((AttackEntityEvent e) -> mark(e.getEntity()));
        bus.addListener((BlockEvent.BreakEvent e) -> mark(e.getPlayer()));
        bus.addListener((PlayerContainerEvent.Open e) -> mark(e.getEntity()));
        bus.addListener((ServerChatEvent e) -> mark(e.getPlayer()));
        bus.addListener((CommandEvent e) -> mark(e.getParseResults().getContext().getSource().getPlayer()));
    }

    /** 记一次有效输入（同时把玩家登记为在线）。 */
    public static synchronized void markActive(ServerPlayer player) {
        entry(player).lastActiveTick = WorldClock.now(player.server);
    }

    /** 供 GameTest 模拟挂机：直接设定最近一次输入的时刻。 */
    public static synchronized void setLastActiveTick(ServerPlayer player, long tick) {
        entry(player).lastActiveTick = tick;
    }

    public static synchronized OptionalLong lastActiveTick(UUID id) {
        Entry e = ENTRIES.get(id);
        return e == null ? OptionalLong.empty() : OptionalLong.of(e.lastActiveTick);
    }

    /** 在线的玩家实例：优先取玩家列表；不在列表中但已登记且未断线的（如假玩家）也算在线。 */
    public static synchronized ServerPlayer onlinePlayer(MinecraftServer server, UUID id) {
        ServerPlayer listed = server.getPlayerList().getPlayer(id);
        if (listed != null) {
            return listed;
        }
        Entry e = ENTRIES.get(id);
        return e != null && !e.player.hasDisconnected() ? e.player : null;
    }

    /** 主人是否活跃：在线，且距最近一次输入不足宽限期。 */
    public static synchronized boolean isActive(MinecraftServer server, UUID id, long now, RuntimeConfig.Idle idle) {
        ServerPlayer player = onlinePlayer(server, id);
        Entry e = ENTRIES.get(id);
        long last = e == null ? Long.MIN_VALUE / 2 : e.lastActiveTick;
        return ActivityRules.isActive(player != null && e != null, now, last, idle);
    }

    /** 清除记录（下线、测试结束）。 */
    public static synchronized void forget(UUID id) {
        ENTRIES.remove(id);
    }

    public static synchronized void clear() {
        ENTRIES.clear();
    }

    private static void mark(Player player) {
        if (player instanceof ServerPlayer sp && !sp.level().isClientSide) {
            markActive(sp);
        }
    }

    private static Entry entry(ServerPlayer player) {
        Entry e = ENTRIES.computeIfAbsent(player.getUUID(), k -> {
            Entry n = new Entry();
            n.x = player.getX();
            n.y = player.getY();
            n.z = player.getZ();
            n.yRot = player.getYRot();
            n.xRot = player.getXRot();
            return n;
        });
        e.player = player;
        return e;
    }

    private static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        mark(event.getEntity());
    }

    private static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        forget(event.getEntity().getUUID());
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        mark(event.getEntity());
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % MOVE_CHECK_INTERVAL != 0) {
            return;
        }
        synchronized (PlayerActivity.class) {
            Entry e = entry(player);
            boolean moved = player.distanceToSqr(e.x, e.y, e.z) >= 1.0;
            boolean turned = Math.abs(player.getYRot() - e.yRot) >= 1.0f || Math.abs(player.getXRot() - e.xRot) >= 1.0f;
            if (moved || turned) {
                e.x = player.getX();
                e.y = player.getY();
                e.z = player.getZ();
                e.yRot = player.getYRot();
                e.xRot = player.getXRot();
                e.lastActiveTick = WorldClock.now(player.server);
            }
        }
    }
}
