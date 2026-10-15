package io.github.happysnappingturtle.mythicage.temple;

import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.config.ModConfigs;
import io.github.happysnappingturtle.mythicage.core.config.world.WorldRulesConfig;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.temple.ScanQueue;
import io.github.happysnappingturtle.mythicage.core.temple.TempleRules;
import io.github.happysnappingturtle.mythicage.player.MythData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 神庙判定的服务端调度（T8a 设计文档 §3.5、§4）：全服扫描队列（每刻预算）、置脏、兜底重扫、
 * 检视请求的等待与回发、放置规则、无主神坛清理。
 */
public final class TempleService {
    /** 检视时缓存超过这么多刻就重扫。 */
    public static final int INSPECT_MAX_AGE = 100;
    /** 兜底检查的间隔（真正的重扫间隔由配置决定）。 */
    private static final int FALLBACK_CHECK_INTERVAL = 200;
    /** 神坛间垂直方向的最小距离：扫描体高度。 */
    private static final int VERTICAL_SPACING = 12;

    private static MinecraftServer server;
    private static final ScanQueue<GlobalPos> QUEUE = new ScanQueue<>();
    private static final Map<GlobalPos, TempleScan> ACTIVE = new HashMap<>();
    private static final Map<GlobalPos, Set<UUID>> WAITING = new HashMap<>();
    private static final Map<GlobalPos, Long> OWNER_CHECK = new HashMap<>();
    private static int cellsLastTick;

    private TempleService() {
    }

    public static void onServerStarted(ServerStartedEvent event) {
        server = event.getServer();
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        server = null;
        for (GlobalPos p : new ArrayList<>(ACTIVE.keySet())) {
            QUEUE.cancel(p);
        }
        ACTIVE.clear();
        WAITING.clear();
        OWNER_CHECK.clear();
    }

    static WorldRulesConfig.Temples config() {
        return ModConfigs.worldRules().current().temples();
    }

    public static TempleRules rules() {
        return TempleRulesLoader.greek().rules();
    }

    /** 测试与检视用：上一刻处理的格数。 */
    public static int cellsLastTick() {
        return cellsLastTick;
    }

    public static boolean pending(ServerLevel level, BlockPos pos) {
        return QUEUE.contains(GlobalPos.of(level.dimension(), pos));
    }

    // ───────────── 入队 ─────────────

    /** 置脏：入队（已在队列中则从头开始）。 */
    public static void submit(ServerLevel level, BlockPos pos) {
        GlobalPos key = GlobalPos.of(level.dimension(), pos.immutable());
        TempleRules rules = rules();
        TempleScan scan = ACTIVE.get(key);
        if (scan == null || scan.volume() != rules.scan().volume()) {
            ACTIVE.put(key, new TempleScan(level, pos.immutable(), facingAt(level, pos), rules));
        }
        QUEUE.submit(key, rules.scan().volume());
    }

    static net.minecraft.core.Direction facingAt(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return state.hasProperty(GreekAltarBlock.FACING) ? state.getValue(GreekAltarBlock.FACING)
                : net.minecraft.core.Direction.NORTH;
    }

    public static void onRulesReloaded() {
        MinecraftServer s = server;
        if (s == null) {
            return;
        }
        s.execute(() -> {
            ACTIVE.clear();
            for (AltarRegistry.Entry e : AltarRegistry.get(s).all()) {
                ServerLevel level = s.getLevel(e.pos().dimension());
                if (level != null && level.isLoaded(e.pos().pos())) {
                    submit(level, e.pos().pos());
                }
            }
        });
    }

    static void onAltarLoaded(ServerLevel level, GreekAltarBlockEntity be) {
        if (be.owner().isEmpty()) {
            // 没有玩家的放置（命令、结构）：给绑定留出同一刻，下一刻仍无主就移除
            OWNER_CHECK.put(GlobalPos.of(level.dimension(), be.getBlockPos()), level.getGameTime() + 1);
            return;
        }
        level.getServer().execute(() -> {
            if (level.getBlockEntity(be.getBlockPos()) == be) {
                submit(level, be.getBlockPos());
            }
        });
    }

    // ───────────── 每刻 ─────────────

    public static void onServerTickPost(ServerTickEvent.Post event) {
        MinecraftServer s = event.getServer();
        checkOwnerless(s);
        int budget = config().scanCellsPerTick();
        int[] used = {0};
        List<GlobalPos> done = QUEUE.tick(budget, (key, start, count) -> {
            TempleScan scan = ACTIVE.get(key);
            if (scan != null) {
                scan.scan(start, count);
            }
            used[0] += count;
        });
        cellsLastTick = used[0];
        for (GlobalPos key : done) {
            complete(s, key);
        }
        if (s.getTickCount() % FALLBACK_CHECK_INTERVAL == 0) {
            fallback(s);
        }
    }

    private static void checkOwnerless(MinecraftServer s) {
        if (OWNER_CHECK.isEmpty()) {
            return;
        }
        var it = OWNER_CHECK.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            ServerLevel level = s.getLevel(e.getKey().dimension());
            if (level == null || level.getGameTime() < e.getValue()) {
                continue;
            }
            it.remove();
            BlockPos pos = e.getKey().pos();
            if (level.getBlockEntity(pos) instanceof GreekAltarBlockEntity be) {
                if (be.owner().isEmpty()) {
                    MythicAge.LOGGER.info("移除没有主人的神坛 {}（只能由玩家放置）", pos);
                    level.destroyBlock(pos, true);
                } else {
                    submit(level, pos);
                }
            }
        }
    }

    private static void complete(MinecraftServer s, GlobalPos key) {
        TempleScan scan = ACTIVE.remove(key);
        ServerLevel level = s.getLevel(key.dimension());
        if (scan == null || level == null || !(level.getBlockEntity(key.pos()) instanceof GreekAltarBlockEntity be)) {
            WAITING.remove(key);
            return;
        }
        refreshOwnerAge(s, be);
        if (scan.unloaded()) {
            be.markUnverified(level.getGameTime());
        } else {
            TempleRulesLoader.Loaded loaded = TempleRulesLoader.greek();
            int maxSlots = loaded.rules().tiers().stream().mapToInt(TempleRules.Tier::prayerSlots).max().orElse(0);
            be.applyScan(scan.finish(maxSlots), level.getGameTime(), loaded.hash());
        }
        Set<UUID> waiting = WAITING.remove(key);
        if (waiting != null) {
            for (UUID id : waiting) {
                ServerPlayer p = s.getPlayerList().getPlayer(id);
                if (p != null) {
                    send(p, be);
                }
            }
        }
    }

    private static void refreshOwnerAge(MinecraftServer s, GreekAltarBlockEntity be) {
        be.owner().map(id -> s.getPlayerList().getPlayer(id))
                .ifPresent(p -> be.refreshOwnerAge(MythData.get(p).civ(Civilization.GREEK).age()));
    }

    private static void fallback(MinecraftServer s) {
        int interval = config().rescanIntervalTicks();
        int hash = TempleRulesLoader.greek().hash();
        for (AltarRegistry.Entry e : AltarRegistry.get(s).all()) {
            ServerLevel level = s.getLevel(e.pos().dimension());
            if (level == null || !level.isLoaded(e.pos().pos())) {
                continue;
            }
            if (level.getBlockEntity(e.pos().pos()) instanceof GreekAltarBlockEntity be
                    && (be.rulesHash() != hash || level.getGameTime() - be.lastScanTick() >= interval)
                    && !QUEUE.contains(e.pos())) {
                submit(level, e.pos().pos());
            }
        }
    }

    // ───────────── 置脏 ─────────────

    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || server == null) {
            return;
        }
        BlockPos pos = event.getPos();
        TempleRules.Scan scan = rules().scan();
        int reach = Math.max(scan.halfWidth(), Math.max(scan.front(), scan.back()));
        for (BlockPos altar : AltarRegistry.get(server).altarsNear(level.dimension(), pos, reach)) {
            if (pos.equals(altar) || !level.isLoaded(altar)) {
                continue;
            }
            BlockPos[] b = TempleScan.bounds(altar, facingAt(level, altar), scan);
            if (pos.getX() >= b[0].getX() && pos.getX() <= b[1].getX() && pos.getY() >= b[0].getY()
                    && pos.getY() <= b[1].getY() && pos.getZ() >= b[0].getZ() && pos.getZ() <= b[1].getZ()) {
                submit(level, altar);
            }
        }
    }

    // ───────────── 放置与保护 ─────────────

    /** 放置前的检查：数量上限、间距与扫描体不相交。返回拒绝原因。 */
    public static Optional<Component> placementError(ServerPlayer player, ServerLevel level, BlockPos pos,
                                                     net.minecraft.core.Direction facing) {
        WorldRulesConfig.Temples cfg = config();
        AltarRegistry reg = AltarRegistry.get(level.getServer());
        Age age = MythData.get(player).civ(Civilization.GREEK).age();
        int limit = cfg.altarLimit(age.id());
        if (reg.countOwned(player.getUUID()) >= limit) {
            return Optional.of(limit == 0
                    ? Component.translatable("mythic_age.altar.place.no_age")
                    : Component.translatable("mythic_age.altar.place.limit", limit));
        }
        Optional<AltarRegistry.Entry> conflict = reg.conflict(GlobalPos.of(level.dimension(), pos), facing,
                cfg.altarSpacing(), VERTICAL_SPACING, rules().scan());
        if (conflict.isPresent()) {
            BlockPos c = conflict.get().pos().pos();
            return Optional.of(Component.translatable("mythic_age.altar.place.spacing", cfg.altarSpacing(),
                    c.getX(), c.getY(), c.getZ(), conflict.get().ownerName()));
        }
        return Optional.empty();
    }

    public static void onPlaced(ServerLevel level, BlockPos pos, ServerPlayer player, GreekAltarBlockEntity be) {
        be.bind(player.getUUID(), player.getGameProfile().getName(), MythData.get(player).civ(Civilization.GREEK).age());
        AltarRegistry.get(level.getServer()).add(new AltarRegistry.Entry(GlobalPos.of(level.dimension(), pos.immutable()),
                player.getUUID(), player.getGameProfile().getName(), facingAt(level, pos)));
        OWNER_CHECK.remove(GlobalPos.of(level.dimension(), pos));
        player.displayClientMessage(Component.translatable("mythic_age.altar.place.hint"), true);
        submit(level, pos);
    }

    /** 测试与管理用：直接绑定主人（不经过放置检查）。 */
    public static void bindForTest(ServerLevel level, BlockPos pos, UUID owner, String name, Age age) {
        if (level.getBlockEntity(pos) instanceof GreekAltarBlockEntity be) {
            be.bind(owner, name, age);
            AltarRegistry.get(level.getServer()).add(new AltarRegistry.Entry(GlobalPos.of(level.dimension(), pos.immutable()),
                    owner, name, facingAt(level, pos)));
            OWNER_CHECK.remove(GlobalPos.of(level.dimension(), pos));
            submit(level, pos);
        }
    }

    public static void onRemoved(Level level, BlockPos pos) {
        if (level instanceof ServerLevel server) {
            GlobalPos key = GlobalPos.of(server.dimension(), pos);
            AltarRegistry.get(server.getServer()).remove(key);
            QUEUE.cancel(key);
            ACTIVE.remove(key);
            WAITING.remove(key);
            OWNER_CHECK.remove(key);
        }
    }

    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(level.getBlockEntity(event.getPos()) instanceof GreekAltarBlockEntity be)
                || !config().protectAltar()) {
            return;
        }
        var player = event.getPlayer();
        boolean owner = be.owner().map(id -> id.equals(player.getUUID())).orElse(true);
        boolean admin = player.isCreative() && player.hasPermissions(2);
        if (!owner && !admin) {
            event.setCanceled(true);
            player.displayClientMessage(Component.translatable("mythic_age.altar.protected", be.ownerName()), true);
        }
    }

    // ───────────── 检视 ─────────────

    public static void inspect(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof GreekAltarBlockEntity be)) {
            return;
        }
        GlobalPos key = GlobalPos.of(level.dimension(), pos.immutable());
        boolean stale = !be.hasResult() || be.unverified() || level.getGameTime() - be.lastScanTick() > INSPECT_MAX_AGE
                || be.highlights() == null || QUEUE.contains(key);
        if (stale) {
            if (!QUEUE.contains(key)) {
                submit(level, pos);
            }
            WAITING.computeIfAbsent(key, k -> new HashSet<>()).add(player.getUUID());
        } else {
            refreshOwnerAge(level.getServer(), be);
            send(player, be);
        }
    }

    static void send(ServerPlayer player, GreekAltarBlockEntity be) {
        PacketDistributor.sendToPlayer(player, AltarSurveyPayload.of(be, player, rules()));
    }
}
