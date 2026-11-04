package io.github.happysnappingturtle.mythicage.playtest;

import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.boss.TrialArena;
import io.github.happysnappingturtle.mythicage.boss.TrialArenas;
import io.github.happysnappingturtle.mythicage.core.boss.ArenaPhase;
import io.github.happysnappingturtle.mythicage.core.playtest.PlaytestStations;
import io.github.happysnappingturtle.mythicage.core.playtest.PlaytestStations.Spot;
import io.github.happysnappingturtle.mythicage.guardian.GuardianEntity;
import io.github.happysnappingturtle.mythicage.structure.LabyrinthMap;
import io.github.happysnappingturtle.mythicage.structure.StructureRegistration;
import io.github.happysnappingturtle.mythicage.village.VillageGuide;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 进站的准备任务（GA-3 设计文档 §4.1）：找结构、等区块与村庄登记要跨刻，每次进站是一个挂在服务器 tick 上的状态机。
 * 每名玩家同时只有一个任务；下线取消；超时报出卡在哪一步。
 */
public final class PlaytestJobs {
    public enum Step { LOCATE, TELEPORT, WAIT_READY, APPLY, BUILD, DONE }

    /** 要先找结构时 60 秒，已有锚点时 30 秒。 */
    static final int TIMEOUT_LOCATE = 1200;
    static final int TIMEOUT = 600;
    /** 村庄站先传到村庄中心以南这么远，等村庄载入。 */
    static final int EDGE_DISTANCE = 40;
    /** 起步站：出村界后再退多少格；最多往南找多远。 */
    static final int OUTSIDE_MARGIN = 16;
    static final int OUTSIDE_MAX = 160;
    /** 找钟、祭司、竞技场的范围。 */
    static final int NEAR = 96;
    static final int ARENA_NEAR = 160;

    static final class Job {
        final UUID player;
        final PlaytestStations.Station station;
        Step step = Step.LOCATE;
        final long started;
        int timeout = TIMEOUT;
        boolean searching;
        BlockPos target;
        UUID village;
        BlockPos bell;
        String arena;
        BlockPos temple;
        List<String> missing = List.of();
        int quests = -1;
        boolean firstEntry;

        Job(UUID player, PlaytestStations.Station station, long started) {
            this.player = player;
            this.station = station;
            this.started = started;
        }
    }

    private static final Map<UUID, Job> JOBS = new ConcurrentHashMap<>();

    private PlaytestJobs() {
    }

    public static boolean running(UUID player) {
        return JOBS.containsKey(player);
    }

    /** 开始进站；上一站还在准备时返回 false。 */
    public static boolean begin(ServerPlayer p, PlaytestStations.Station s) {
        Job job = new Job(p.getUUID(), s, p.server.getTickCount());
        if (JOBS.putIfAbsent(p.getUUID(), job) != null) {
            return false;
        }
        PlaytestLog.interrupt(p);
        p.sendSystemMessage(Component.translatable("commands.mythic_age.playtest.preparing", s.number(),
                Component.translatable(s.nameKey())));
        return true;
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        JOBS.remove(e.getEntity().getUUID());
    }

    public static void onServerTickPost(ServerTickEvent.Post e) {
        if (JOBS.isEmpty()) {
            return;
        }
        MinecraftServer server = e.getServer();
        for (Job job : List.copyOf(JOBS.values())) {
            ServerPlayer p = server.getPlayerList().getPlayer(job.player);
            if (p == null) {
                JOBS.remove(job.player);
                continue;
            }
            try {
                tick(server, p, job);
            } catch (RuntimeException ex) {
                MythicAge.LOGGER.error("快速验证准备出错：第 {} 站 {}", job.station.number(), job.step, ex);
                fail(p, job, Component.literal(ex.toString()));
            }
        }
    }

    static void tick(MinecraftServer server, ServerPlayer p, Job job) {
        int limit = timeoutForTest > 0 ? timeoutForTest : job.timeout;
        if (server.getTickCount() - job.started > limit) {
            fail(p, job, Component.translatable("commands.mythic_age.playtest.reason.timeout",
                    Component.translatable("commands.mythic_age.playtest.step." + job.step.name().toLowerCase(java.util.Locale.ROOT))));
            return;
        }
        ServerLevel ow = server.overworld();
        PlaytestState st = PlaytestState.get(server);
        PlaytestStations.Spec spec = job.station.spec();
        switch (job.step) {
            case LOCATE -> locate(ow, st, p, job);
            case TELEPORT -> {
                BlockPos at = spec.spot() == Spot.VILLAGE_EDGE ? job.target.south(EDGE_DISTANCE) : job.target.south(4);
                teleport(p, ow, surface(ow, at), 180);
                job.step = Step.WAIT_READY;
            }
            case WAIT_READY -> {
                if ((server.getTickCount() - job.started) % 20 == 0 && ready(ow, job)) {
                    job.step = Step.APPLY;
                }
            }
            case APPLY -> apply(ow, st, p, job);
            case BUILD -> build(ow, st, p, job);
            case DONE -> finish(st, p, job);
        }
    }

    // ───────────── 各步 ─────────────

    private static void locate(ServerLevel ow, PlaytestState st, ServerPlayer p, Job job) {
        Spot spot = job.station.spec().spot();
        if (spot.village()) {
            if (st.villageCenter != null) {
                job.target = st.villageCenter;
                job.step = Step.TELEPORT;
            } else if (!job.searching) {
                job.searching = true;
                job.timeout = TIMEOUT_LOCATE;
                VillageGuide.searchAny(ow, ow.getSharedSpawnPos(), PlaytestSetup.CIV, found -> {
                    if (JOBS.get(job.player) != job) {
                        return;
                    }
                    if (found.isEmpty()) {
                        fail(p, job, Component.translatable("commands.mythic_age.playtest.reason.no_village"));
                        return;
                    }
                    st.setVillage(found.get().center());
                    job.target = found.get().center();
                    job.step = Step.TELEPORT;
                });
            }
            return;
        }
        if (st.labyrinthEntrance != null) {
            job.target = st.labyrinthEntrance;
            job.step = Step.TELEPORT;
            return;
        }
        job.timeout = TIMEOUT_LOCATE;
        Optional<BlockPos> found = LabyrinthMap.find(ow, ow.getSharedSpawnPos());
        if (found.isEmpty()) {
            fail(p, job, Component.translatable("commands.mythic_age.playtest.reason.no_labyrinth"));
            return;
        }
        BlockPos entrance = entranceOf(ow, found.get());
        st.setLabyrinth(entrance);
        job.target = entrance;
        job.step = Step.TELEPORT;
    }

    /** 迷宫的地表入口（结构信息里的 entrance 锚点）；读不到时用找到的位置。 */
    static BlockPos entranceOf(ServerLevel level, BlockPos found) {
        ChunkPos c = new ChunkPos(found);
        ChunkAccess chunk = level.getChunk(c.x, c.z, ChunkStatus.STRUCTURE_STARTS);
        for (Holder<Structure> h : level.registryAccess().registryOrThrow(Registries.STRUCTURE).getTagOrEmpty(LabyrinthMap.TAG)) {
            StructureStart start = level.structureManager().getStartForStructure(SectionPos.bottomOf(chunk), h.value(), chunk);
            if (start != null && start.isValid()) {
                return StructureRegistration.info(start).flatMap(i -> i.anchors("entrance").stream().findFirst())
                        .orElse(start.getBoundingBox().getCenter());
            }
        }
        return found;
    }

    private static boolean ready(ServerLevel ow, Job job) {
        Spot spot = job.station.spec().spot();
        if (spot.village()) {
            Optional<VillageRegistry.Village> v = VillageRegistry.get(ow.getServer()).in(ow.dimension()).stream()
                    .filter(x -> x.civ().equals(PlaytestSetup.CIV.id()))
                    .filter(x -> x.bell().pos().distSqr(job.target) <= (double) NEAR * NEAR)
                    .min(Comparator.comparingDouble(x -> x.bell().pos().distSqr(job.target)));
            if (v.isEmpty()) {
                return false;
            }
            job.village = v.get().id();
            job.bell = v.get().bell().pos();
            return spot != Spot.VILLAGE_PRIEST || guardian(ow, job.bell).isPresent();
        }
        Optional<TrialArena> a = arena(ow, job.target);
        a.ifPresent(x -> job.arena = x.id());
        return a.isPresent();
    }

    private static void apply(ServerLevel ow, PlaytestState st, ServerPlayer p, Job job) {
        PlaytestStations.Spec spec = job.station.spec();
        // 先站到位：晋升要在村里做，才会同时推进该村
        switch (spec.spot()) {
            case VILLAGE_EDGE -> teleport(p, ow, outside(ow, job.bell), 180);
            case VILLAGE_PRIEST -> guardian(ow, job.bell).ifPresent(g -> standBy(p, ow, g));
            case VILLAGE_KEEPER -> keeper(ow, job.village).ifPresentOrElse(k -> standBy(p, ow, k),
                    () -> teleport(p, ow, beside(ow, job.bell), 180));
            default -> {
            }
        }
        job.firstEntry = PlaytestSetup.reset(p, st);
        Optional<TrialArena> arena = Optional.ofNullable(job.arena).flatMap(id -> TrialArenas.get(ow.getServer()).byId(id));
        // 上一站留下的战斗先复原，进站时竞技场是空场
        arena.filter(a -> a.phase() != ArenaPhase.IDLE).ifPresent(a -> a.reset(ow));
        job.missing = PlaytestSetup.apply(p, spec);
        if (spec.quest() && job.village != null) {
            job.quests = PlaytestSetup.ensureQuest(ow, job.village);
        }
        if (spec.spot() == Spot.ARENA_GATE) {
            arena.ifPresent(a -> {
                BlockPos g = a.gate().size() > 2 ? a.gate().get(2).south(3) : a.bossOrigin();
                teleport(p, ow, g, 180);
            });
        }
        if (spec.dayTime() >= 0) {
            long now = ow.getDayTime();
            ow.setDayTime(now - Math.floorMod(now, 24000L) + spec.dayTime());
        }
        ow.setWeatherParameters(12000, 0, false, false);
        job.step = Step.BUILD;
    }

    private static void build(ServerLevel ow, PlaytestState st, ServerPlayer p, Job job) {
        int need = job.station.spec().temple();
        if (need <= 0) {
            job.step = Step.DONE;
            return;
        }
        if (job.temple == null) {
            BlockPos site = st.templeSite;
            if (site == null) {
                Optional<BlockPos> found = PlaytestTemple.findSite(ow, job.bell, p);
                if (found.isEmpty()) {
                    fail(p, job, Component.translatable("commands.mythic_age.playtest.reason.no_site"));
                    return;
                }
                site = found.get();
                st.setTempleSite(site);
            }
            PlaytestTemple.build(ow, site, need, p);
            job.temple = site;
            return;
        }
        OptionalInt level = PlaytestTemple.scanned(ow, job.temple);
        if (level.isEmpty()) {
            return;
        }
        if (level.getAsInt() < need) {
            fail(p, job, Component.translatable("commands.mythic_age.playtest.reason.temple_level", level.getAsInt(), need));
            return;
        }
        job.step = Step.DONE;
    }

    private static void finish(PlaytestState st, ServerPlayer p, Job job) {
        JOBS.remove(job.player);
        PlaytestStations.Station s = job.station;
        PlaytestLog.start(p, s);
        if (job.firstEntry) {
            p.sendSystemMessage(Component.translatable("commands.mythic_age.playtest.first_warning").withStyle(ChatFormatting.GOLD));
        }
        p.sendSystemMessage(Component.translatable("commands.mythic_age.playtest.ready", s.number(),
                Component.translatable(s.nameKey()), s.minutes()).withStyle(ChatFormatting.GREEN));
        p.sendSystemMessage(Component.translatable("commands.mythic_age.playtest.watch", Component.translatable(s.watchKey())));
        if (!job.missing.isEmpty()) {
            p.sendSystemMessage(Component.translatable("commands.mythic_age.playtest.reason.items", String.join(", ", job.missing))
                    .withStyle(ChatFormatting.RED));
        }
        if (job.quests == 0) {
            p.sendSystemMessage(Component.translatable("commands.mythic_age.playtest.quest_none").withStyle(ChatFormatting.RED));
        }
        p.sendSystemMessage(Component.translatable("commands.mythic_age.playtest.hint").withStyle(ChatFormatting.GRAY));
    }

    private static void fail(ServerPlayer p, Job job, Component reason) {
        if (JOBS.remove(job.player, job)) {
            PlaytestLog.failed(p, job.station, reason.getString());
            p.sendSystemMessage(Component.translatable("commands.mythic_age.playtest.failed", job.station.number(), reason)
                    .withStyle(ChatFormatting.RED));
        }
    }

    // ───────────── 位置 ─────────────

    /** 地面落脚点。先把这一块载入（必要时生成）：没载入的区块高度图读到的是世界底部。 */
    static BlockPos surface(ServerLevel level, BlockPos at) {
        level.getChunk(at.getX() >> 4, at.getZ() >> 4);
        return new BlockPos(at.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ()), at.getZ());
    }

    static void teleport(ServerPlayer p, ServerLevel level, BlockPos at, float yaw) {
        p.teleportTo(level, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yaw, 0);
    }

    /** 从钟往南走出村界，再退 {@link #OUTSIDE_MARGIN} 格；走 {@link #OUTSIDE_MAX} 格还在村里就停在那里。 */
    static BlockPos outside(ServerLevel level, BlockPos bell) {
        for (int d = 8; d <= OUTSIDE_MAX; d += 8) {
            BlockPos at = surface(level, bell.south(d));
            if (io.github.happysnappingturtle.mythicage.village.VillageSpace.villageAt(level, at).isEmpty()) {
                return surface(level, bell.south(d + OUTSIDE_MARGIN));
            }
        }
        return surface(level, bell.south(OUTSIDE_MAX));
    }

    private static final int[][] BESIDE = {{0, 2}, {2, 0}, {-2, 0}, {0, -2}, {1, 1}, {-1, 1}, {1, -1}, {-1, -1},
            {0, 1}, {1, 0}, {-1, 0}, {0, -1}};

    /** 站到实体旁边、和它同一层（不用高度图：祭司常在屋檐下），面朝它。 */
    static void standBy(ServerPlayer p, ServerLevel level, Entity e) {
        BlockPos at = beside(level, e.blockPosition());
        double dx = e.getX() - (at.getX() + 0.5);
        double dz = e.getZ() - (at.getZ() + 0.5);
        teleport(p, level, at, (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90);
    }

    static BlockPos beside(ServerLevel level, BlockPos foot) {
        for (int[] o : BESIDE) {
            for (int dy : new int[]{0, 1, -1}) {
                BlockPos c = foot.offset(o[0], dy, o[1]);
                if (standable(level, c)) {
                    return c;
                }
            }
        }
        return foot;
    }

    /** 脚下顶面结实，身体两格没有碰撞箱。 */
    static boolean standable(ServerLevel level, BlockPos c) {
        return level.getBlockState(c).getCollisionShape(level, c).isEmpty()
                && level.getBlockState(c.above()).getCollisionShape(level, c.above()).isEmpty()
                && level.getBlockState(c.below()).isFaceSturdy(level, c.below(), net.minecraft.core.Direction.UP);
    }

    static Optional<GuardianEntity> guardian(ServerLevel level, BlockPos bell) {
        return level.getEntitiesOfClass(GuardianEntity.class, new AABB(bell).inflate(64), g -> g.civ() == PlaytestSetup.CIV)
                .stream().min(Comparator.comparingDouble(g -> g.distanceToSqr(bell.getCenter())));
    }

    static Optional<Entity> keeper(ServerLevel level, UUID village) {
        return VillageEconomy.get(level.getServer()).state(village).keeper().map(m -> level.getEntity(m.id()));
    }

    static Optional<TrialArena> arena(ServerLevel level, BlockPos near) {
        return TrialArenas.get(level.getServer()).all().stream()
                .filter(a -> a.dimension().equals(level.dimension()))
                .filter(a -> a.bossOrigin().distSqr(near) <= (double) ARENA_NEAR * ARENA_NEAR)
                .min(Comparator.comparingDouble(a -> a.bossOrigin().distSqr(near)));
    }

    /** 只供 GameTest：把超时改短（≤ 0 恢复）。 */
    public static volatile int timeoutForTest = -1;

    /** 只供 GameTest：清掉卡住的任务。 */
    public static void clearForTest() {
        JOBS.clear();
    }

    /** 只供 GameTest：当前步骤。 */
    public static Optional<Step> stepForTest(UUID player) {
        return Optional.ofNullable(JOBS.get(player)).map(j -> j.step);
    }
}
