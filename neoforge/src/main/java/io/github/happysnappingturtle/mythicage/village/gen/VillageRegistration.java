package io.github.happysnappingturtle.mythicage.village.gen;

import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.guardian.GuardianRegistry;
import io.github.happysnappingturtle.mythicage.guardian.GuardianSpawner;
import io.github.happysnappingturtle.mythicage.village.VillagePlans;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 生成即登记（V0 §4.2，T13b §3.1、§3.4）：
 * <ol>
 *   <li>区块载入时发现文明村庄的结构起点 → 放进待登记队列（载入事件里不改存档、不生成实体）；</li>
 *   <li>下一个服务器刻在主线程登记：村庄登记（来源 GENERATED，键是聚会钟）+ 村庄规划；按结构起点去重；</li>
 *   <li>锚点所在区块都已加载后，在 {@code guardian} 锚点生成祭司、每个 {@code home} 锚点生成一名原版无业村民（T19 前的过渡）。</li>
 * </ol>
 */
public final class VillageRegistration {
    public static final int POPULATE_INTERVAL = 20;

    private record Pending(ResourceKey<Level> dim, StructureStart start) {
    }

    private static final ConcurrentLinkedQueue<Pending> PENDING = new ConcurrentLinkedQueue<>();

    private VillageRegistration() {
    }

    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
            scan(level, chunk);
        }
    }

    /** 把区块里文明村庄的结构起点放进待登记队列。 */
    public static void scan(ServerLevel level, LevelChunk chunk) {
        for (StructureStart start : chunk.getAllStarts().values()) {
            if (start.isValid() && start.getStructure() instanceof CivVillageStructure) {
                PENDING.add(new Pending(level.dimension(), start));
            }
        }
    }

    public static void onServerTickPost(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        drain(server);
        if (server.getTickCount() % POPULATE_INTERVAL == 0) {
            for (ServerLevel level : server.getAllLevels()) {
                populateDue(level);
            }
        }
    }

    /** 处理待登记队列（GameTest 也直接调用）。 */
    public static void drain(MinecraftServer server) {
        Pending p;
        while ((p = PENDING.poll()) != null) {
            ServerLevel level = server.getLevel(p.dim());
            if (level != null) {
                register(level, p.start());
            }
        }
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        PENDING.clear();
    }

    /** 按结构起点登记（已登记过就返回那座村庄）。不是文明村庄或缺信息片段时为空。 */
    public static Optional<VillageRegistry.Village> register(ServerLevel level, StructureStart start) {
        if (!(start.getStructure() instanceof CivVillageStructure)) {
            return Optional.empty();
        }
        String structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(start.getStructure()) + "";
        long chunk = start.getChunkPos().toLong();
        VillagePlans plans = VillagePlans.get(level.getServer());
        VillageRegistry reg = VillageRegistry.get(level.getServer());
        Optional<UUID> known = plans.byStart(level.dimension(), structure, chunk);
        if (known.isPresent()) {
            return reg.byId(known.get());
        }
        Optional<VillageInfoPiece.Info> info = info(start);
        if (info.isEmpty()) {
            MythicAge.LOGGER.warn("文明村庄 {} 缺少信息片段，跳过登记", start.getChunkPos());
            return Optional.empty();
        }
        VillageInfoPiece.Info i = info.get();
        GlobalPos bell = GlobalPos.of(level.dimension(), VillageInfoPiece.block(i.center()));
        VillageRegistry.Village v = reg.byBell(bell).orElseGet(() -> new VillageRegistry.Village(UUID.randomUUID(), bell,
                i.civ(), i.patron(), VillageRegistry.Origin.GENERATED, null, "", -1));
        reg.add(v);
        plans.put(new VillagePlans.Plan(v.id(), level.dimension(), structure, chunk, i, false));
        MythicAge.LOGGER.info("登记文明村庄：{} {}（{}）于 {}", i.civ(), i.patron(), structure, bell.pos());
        return Optional.of(v);
    }

    /** 兜底（V0 §4.2）：pos 在某座文明村庄的片段里时立即登记。 */
    public static Optional<VillageRegistry.Village> registerAt(ServerLevel level, BlockPos pos) {
        StructureStart start = level.structureManager().getStructureWithPieceAt(pos,
                h -> h.value() instanceof CivVillageStructure);
        return start.isValid() ? register(level, start) : Optional.empty();
    }

    static Optional<VillageInfoPiece.Info> info(StructureStart start) {
        for (StructurePiece piece : start.getPieces()) {
            if (piece instanceof VillageInfoPiece info) {
                return Optional.of(info.info());
            }
        }
        return Optional.empty();
    }

    // ───────────── 祭司与过渡期居民 ─────────────

    public static void populateDue(ServerLevel level) {
        VillagePlans plans = VillagePlans.get(level.getServer());
        for (VillagePlans.Plan p : plans.all()) {
            if (!p.populated() && p.dim().equals(level.dimension()) && anchorsLoaded(level, p)) {
                populate(level, p);
                plans.markPopulatedPublic(p.village());
            }
        }
    }

    private static boolean anchorsLoaded(ServerLevel level, VillagePlans.Plan p) {
        List<Pos> all = new ArrayList<>(p.anchors("guardian"));
        all.addAll(p.anchors("home"));
        all.add(p.info().center());
        return all.stream().allMatch(a -> level.isLoaded(VillageInfoPiece.block(a)));
    }

    /**
     * 生成祭司（若该钟还没有祭司）与每个床位一名居民（T19：有本框架数据的文明生成居民；没有数据的文明仍是过渡用的原版无业村民）。
     * 返回生成的人数。
     */
    public static int populate(ServerLevel level, VillagePlans.Plan p) {
        BlockPos bell = VillageInfoPiece.block(p.info().center());
        Optional<Civilization> civ = Civilization.byId(p.info().civ());
        List<Pos> guardian = p.anchors("guardian");
        if (civ.isPresent() && !guardian.isEmpty() && GuardianSpawner.hasGuardian(civ.get())
                && GuardianRegistry.get(level.getServer()).byBell(level.dimension(), bell).isEmpty()) {
            GuardianSpawner.spawnNew(level, VillageInfoPiece.block(guardian.get(0)), Optional.of(bell), civ.get());
        }
        Optional<VillageRegistry.Village> village = VillageRegistry.get(level.getServer()).byId(p.village());
        Optional<io.github.happysnappingturtle.mythicage.core.village.CivVillageData> data =
                io.github.happysnappingturtle.mythicage.village.work.VillageData.civ(p.info().civ());
        if (village.isPresent() && data.isPresent()) {
            return io.github.happysnappingturtle.mythicage.village.resident.Residents.spawnInitial(level, village.get(), p,
                    data.get());
        }
        int n = 0;
        for (Pos home : p.anchors("home")) {
            Villager v = EntityType.VILLAGER.create(level);
            if (v == null) {
                continue;
            }
            BlockPos at = VillageInfoPiece.block(home);
            v.moveTo(at.getX() + 0.5, at.getY() + 0.6, at.getZ() + 0.5, level.random.nextFloat() * 360, 0);
            v.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null);
            if (level.addFreshEntity(v)) {
                v.getBrain().setMemory(MemoryModuleType.MEETING_POINT, GlobalPos.of(level.dimension(), bell));
                n++;
            }
        }
        return n;
    }
}
