package io.github.happysnappingturtle.mythicage.content.greek.temple;

import io.github.happysnappingturtle.mythicage.config.ModConfigs;
import io.github.happysnappingturtle.mythicage.core.config.world.WorldRulesConfig;
import io.github.happysnappingturtle.mythicage.core.greek.temple.TempleRules;
import io.github.happysnappingturtle.mythicage.core.scan.ScanQueue;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.shrine.ShrineKind;
import io.github.happysnappingturtle.mythicage.shrine.ShrineRegistry;
import io.github.happysnappingturtle.mythicage.shrine.ShrineService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
 * 希腊神庙判定的服务端调度（T8a 设计文档 §3.5、§4）：全服扫描队列（每刻预算）、置脏、兜底重扫、
 * 检视请求的等待与回发。神坛的通用职责（上限、间距、保护、无主移除、登记）在 {@link ShrineService}，
 * 这里只通过 {@link #HOOKS} 补上希腊规则：扫描体不得相交、放置提示、绑定后入队扫描。
 */
public final class TempleService {
    /** 检视时缓存超过这么多刻就重扫。 */
    public static final int INSPECT_MAX_AGE = 100;
    /** 兜底检查的间隔（真正的重扫间隔由配置决定）。 */
    private static final int FALLBACK_CHECK_INTERVAL = 200;
    /** 神坛间垂直方向的最小距离：扫描体高度。 */
    public static final int VERTICAL_SPACING = 12;

    private static MinecraftServer server;
    private static final ScanQueue<GlobalPos> QUEUE = new ScanQueue<>();
    private static final Map<GlobalPos, TempleScan> ACTIVE = new HashMap<>();
    private static final Map<GlobalPos, Set<UUID>> WAITING = new HashMap<>();
    private static int cellsLastTick;

    /** 希腊神坛的文明规则（T10b 设计文档 §3.3）。 */
    public static final ShrineKind.Hooks HOOKS = new ShrineKind.Hooks() {
        @Override
        public Optional<Component> placementError(ServerPlayer player, ServerLevel level, BlockPos pos, Direction facing,
                                                  ShrineRegistry registry) {
            return scanOverlap(registry, GlobalPos.of(level.dimension(), pos), facing);
        }

        @Override
        public void onBound(ServerLevel level, BlockPos pos, Optional<ServerPlayer> placer) {
            placer.ifPresent(p -> p.displayClientMessage(Component.translatable("mythic_age.altar.place.hint"), true));
            submit(level, pos);
        }

        @Override
        public void onRemoved(ServerLevel level, GlobalPos key) {
            QUEUE.cancel(key);
            ACTIVE.remove(key);
            WAITING.remove(key);
        }
    };

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
    }

    static WorldRulesConfig.TempleBudget config() {
        return ModConfigs.worldRules().current().greek().temple();
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

    static Direction facingAt(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return state.hasProperty(GreekAltarBlock.FACING) ? state.getValue(GreekAltarBlock.FACING) : Direction.NORTH;
    }

    public static void onRulesReloaded() {
        MinecraftServer s = server;
        if (s == null) {
            return;
        }
        s.execute(() -> {
            ACTIVE.clear();
            for (ShrineRegistry.Entry e : ShrineRegistry.get(s).ofKind(GreekTemple.ALTAR_KIND.id())) {
                ServerLevel level = s.getLevel(e.pos().dimension());
                if (level != null && level.isLoaded(e.pos().pos())) {
                    submit(level, e.pos().pos());
                }
            }
        });
    }

    // ───────────── 每刻 ─────────────

    public static void onServerTickPost(ServerTickEvent.Post event) {
        MinecraftServer s = event.getServer();
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
                .ifPresent(p -> be.refreshOwnerAge(MythData.get(p).civ(be.kind().civ()).age()));
    }

    private static void fallback(MinecraftServer s) {
        int interval = config().rescanIntervalTicks();
        int hash = TempleRulesLoader.greek().hash();
        for (ShrineRegistry.Entry e : ShrineRegistry.get(s).ofKind(GreekTemple.ALTAR_KIND.id())) {
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
        for (BlockPos altar : ShrineRegistry.get(server).near(GreekTemple.ALTAR_KIND.id(), level.dimension(), pos, reach)) {
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

    // ───────────── 希腊放置规则 ─────────────

    /** 与已有希腊神坛的扫描体相交（修订 01 §2.4）。 */
    static Optional<Component> scanOverlap(ShrineRegistry reg, GlobalPos pos, Direction facing) {
        TempleRules.Scan scan = rules().scan();
        BlockPos[] mine = TempleScan.bounds(pos.pos(), facing, scan);
        for (ShrineRegistry.Entry e : reg.ofKind(GreekTemple.ALTAR_KIND.id())) {
            if (!e.pos().dimension().equals(pos.dimension()) || e.pos().equals(pos)) {
                continue;
            }
            BlockPos[] other = TempleScan.bounds(e.pos().pos(), e.facing(), scan);
            if (mine[0].getX() <= other[1].getX() && other[0].getX() <= mine[1].getX()
                    && mine[0].getY() <= other[1].getY() && other[0].getY() <= mine[1].getY()
                    && mine[0].getZ() <= other[1].getZ() && other[0].getZ() <= mine[1].getZ()) {
                BlockPos c = e.pos().pos();
                return Optional.of(Component.translatable("mythic_age.shrine.place.spacing",
                        GreekTemple.GREEK_ALTAR.get().getName(),
                        ModConfigs.worldRules().current().shrine(GreekTemple.ALTAR_KIND.id()).spacing(),
                        c.getX(), c.getY(), c.getZ(), e.ownerName()));
            }
        }
        return Optional.empty();
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
