package io.github.happysnappingturtle.mythicage.structure;

import io.github.happysnappingturtle.mythicage.MythicAge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 生成即登记（T22 §3.3，同 {@code VillageRegistration}）：
 * <ol>
 *   <li>区块载入时发现带 {@link StructureInfoPiece} 的结构起点 → 放进待登记队列（载入事件里不改存档、不生成实体）；</li>
 *   <li>服务器刻里在主线程处理：锚点所在区块都加载了才登记，交给这种结构的 {@link SiteHandler}（生成守卫、设好宝库），
 *       记进 {@link StructureSites}，以后再载入不再处理；锚点还没加载的放回等待表，每 {@value #RETRY_TICKS} 刻再看。</li>
 * </ol>
 * 每刻最多处理 {@value #PER_TICK} 处。
 */
public final class StructureRegistration {
    public static final int PER_TICK = 4;
    public static final int RETRY_TICKS = 20;

    /** 一种结构登记时做的事（各文明的结构在 init 里注册）。 */
    public interface SiteHandler {
        void register(ServerLevel level, StructureInfoPiece.Info info);
    }

    private record Pending(ResourceKey<Level> dim, StructureStart start) {
    }

    private static final Map<String, SiteHandler> HANDLERS = new ConcurrentHashMap<>();
    private static final ConcurrentLinkedQueue<Pending> PENDING = new ConcurrentLinkedQueue<>();
    private static final List<Pending> WAITING = new ArrayList<>();

    private StructureRegistration() {
    }

    public static void handler(String kind, SiteHandler handler) {
        HANDLERS.put(kind, handler);
    }

    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
            for (StructureStart start : chunk.getAllStarts().values()) {
                if (start.isValid() && info(start).isPresent()) {
                    PENDING.add(new Pending(level.dimension(), start));
                }
            }
        }
    }

    public static void onServerTickPost(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        drain(server, PER_TICK);
        if (server.getTickCount() % RETRY_TICKS == 0 && !WAITING.isEmpty()) {
            PENDING.addAll(WAITING);
            WAITING.clear();
        }
    }

    /** 处理待登记队列，最多 {@code max} 处（GameTest 也直接调用）。 */
    public static void drain(MinecraftServer server, int max) {
        Pending p;
        int n = 0;
        while (n < max && (p = PENDING.poll()) != null) {
            ServerLevel level = server.getLevel(p.dim());
            if (level == null) {
                continue;
            }
            if (register(level, p.start()) == Result.WAITING) {
                WAITING.add(p);
            }
            n++;
        }
    }

    public enum Result { REGISTERED, ALREADY, WAITING, NONE }

    /** 登记一处结构（GameTest 直接调）。锚点所在区块没加载完时返回 WAITING，什么都不做。 */
    public static Result register(ServerLevel level, StructureStart start) {
        Optional<StructureInfoPiece.Info> info = info(start);
        if (info.isEmpty()) {
            return Result.NONE;
        }
        String key = key(level, start);
        StructureSites sites = StructureSites.get(level.getServer());
        if (sites.registered(key)) {
            return Result.ALREADY;
        }
        for (List<BlockPos> pts : info.get().anchors().values()) {
            for (BlockPos a : pts) {
                if (!level.isLoaded(a)) {
                    return Result.WAITING;
                }
            }
        }
        SiteHandler h = HANDLERS.get(info.get().kind());
        if (h != null) {
            h.register(level, info.get());
        } else {
            MythicAge.LOGGER.warn("结构种类 {} 没有登记处理，只记下已登记", info.get().kind());
        }
        sites.mark(key);
        return Result.REGISTERED;
    }

    public static String key(ServerLevel level, StructureStart start) {
        String id = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(start.getStructure()) + "";
        return level.dimension().location() + "|" + id + "|" + start.getChunkPos().toLong();
    }

    public static Optional<StructureInfoPiece.Info> info(StructureStart start) {
        for (StructurePiece piece : start.getPieces()) {
            if (piece instanceof StructureInfoPiece i) {
                return Optional.of(i.info());
            }
        }
        return Optional.empty();
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        PENDING.clear();
        WAITING.clear();
    }

    /** 只供 GameTest：等待表里有几处。 */
    public static int waitingForTest() {
        return WAITING.size();
    }
}
