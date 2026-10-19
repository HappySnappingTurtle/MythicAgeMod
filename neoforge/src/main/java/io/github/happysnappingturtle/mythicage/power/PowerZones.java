package io.github.happysnappingturtle.mythicage.power;

import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.runtime.WorldClock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 神力的持续区域（T13 设计文档 §3.2）：瘟疫（5 分钟）、诱兽石（3 分钟）……世界级存档数据，崩溃或重启后继续到期限。
 * 每 20 刻调一次区域的逐刻逻辑（区域中心所在区块载入时）；到期调结束逻辑并移除。
 * 玩家开始追踪区域中心所在区块时补发区域表现（含剩余时长），重启后也能看到。
 */
public final class PowerZones extends SavedData {
    public static final String NAME = "mythic_age_power_zones";
    static final int INTERVAL = 20;

    /**
     * 一个区域。{@code marks} 是区域里需要单独标记的方块位置（如被封停的刷怪器），补发表现时一起发。
     */
    public record Zone(String castId, String kind, String powerId, ResourceKey<Level> dim, Vec3 center, double radius, long until,
                       UUID owner, List<BlockPos> marks) {
        public Zone {
            marks = List.copyOf(marks);
        }

        /** 水平半径内、竖直 ±16 格内。 */
        public boolean covers(Vec3 p) {
            double dx = p.x - center.x;
            double dz = p.z - center.z;
            return dx * dx + dz * dz <= radius * radius && Math.abs(p.y - center.y) <= 16;
        }

        public boolean covers(BlockPos p) {
            return covers(Vec3.atCenterOf(p));
        }

        public Zone withMarks(List<BlockPos> m) {
            return new Zone(castId, kind, powerId, dim, center, radius, until, owner, m);
        }
    }

    /** 一类区域的行为。 */
    public interface Kind {
        void tick(ServerLevel level, Zone zone, long now);

        default void end(ServerLevel level, Zone zone) {
        }
    }

    private static final Map<String, Kind> KINDS = new ConcurrentHashMap<>();
    private final Map<String, Zone> zones = new LinkedHashMap<>();

    public static void registerKind(String kind, Kind behavior) {
        KINDS.put(kind, behavior);
    }

    public static PowerZones get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(PowerZones::new, PowerZones::load, null), NAME);
    }

    // ───────────── 增删查 ─────────────

    public static void add(MinecraftServer server, Zone zone) {
        PowerZones data = get(server);
        data.zones.put(zone.castId(), zone);
        data.setDirty();
    }

    public static void update(MinecraftServer server, Zone zone) {
        add(server, zone);
    }

    public static void remove(MinecraftServer server, String castId) {
        PowerZones data = get(server);
        if (data.zones.remove(castId) != null) {
            data.setDirty();
        }
    }

    public static List<Zone> all(MinecraftServer server) {
        return List.copyOf(get(server).zones.values());
    }

    /** 覆盖某点的某类区域（未到期的）。 */
    public static List<Zone> at(ServerLevel level, Vec3 p, String kind) {
        PowerZones data = get(level.getServer());
        if (data.zones.isEmpty()) {
            return List.of();
        }
        long now = WorldClock.now(level.getServer());
        List<Zone> out = new ArrayList<>();
        for (Zone z : data.zones.values()) {
            if (z.kind().equals(kind) && z.dim() == level.dimension() && z.until() > now && z.covers(p)) {
                out.add(z);
            }
        }
        return out;
    }

    public static boolean covered(ServerLevel level, BlockPos p, String kind) {
        return !at(level, Vec3.atCenterOf(p), kind).isEmpty();
    }

    // ───────────── 逐刻 ─────────────

    public static void onServerTickPost(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % INTERVAL != 0) {
            return;
        }
        PowerZones data = get(server);
        if (data.zones.isEmpty()) {
            return;
        }
        long now = WorldClock.now(server);
        for (Zone z : List.copyOf(data.zones.values())) {
            ServerLevel level = server.getLevel(z.dim());
            Kind k = KINDS.get(z.kind());
            if (level == null || k == null) {
                if (z.until() <= now) {
                    remove(server, z.castId());
                }
                continue;
            }
            try {
                if (z.until() <= now) {
                    remove(server, z.castId());
                    k.end(level, z);
                } else if (level.isLoaded(BlockPos.containing(z.center()))) {
                    k.tick(level, z, now);
                }
            } catch (RuntimeException e) {
                MythicAge.LOGGER.error("神力区域 {} 出错，已移除", z.castId(), e);
                remove(server, z.castId());
            }
        }
    }

    // ───────────── 补发表现 ─────────────

    public static void onChunkWatch(ChunkWatchEvent.Watch event) {
        replay(event.getPlayer(), event.getLevel(), event.getPos());
    }

    /** 给开始追踪 {@code pos} 的玩家补发该区块里的区域表现；返回补发的区域 castId。 */
    public static List<String> replay(ServerPlayer player, ServerLevel level, ChunkPos pos) {
        PowerZones data = get(level.getServer());
        if (data.zones.isEmpty()) {
            return List.of();
        }
        long now = WorldClock.now(level.getServer());
        List<String> out = new ArrayList<>();
        for (Zone z : data.zones.values()) {
            if (z.dim() != level.dimension() || z.until() <= now || !new ChunkPos(BlockPos.containing(z.center())).equals(pos)) {
                continue;
            }
            int left = (int) Math.min(Integer.MAX_VALUE, z.until() - now);
            PowerFx.sendTo(player, zonePayload(z, level, now, left));
            for (BlockPos m : z.marks()) {
                PowerFx.sendTo(player, markPayload(z, level, m, now, left));
            }
            out.add(z.castId());
        }
        return out;
    }

    /** 区域本身的表现：子事件 {@code zone:<半径>}（客户端不知道神力数据，半径随事件带过去）。 */
    public static PowerFxPayload zonePayload(Zone z, ServerLevel level, long now, int duration) {
        return new PowerFxPayload(z.castId(), z.powerId(), PowerFxPayload.Phase.HIT, -1, level.dimension().location(), z.center(),
                List.of(), now, z.castId().hashCode(), duration, "", "zone:" + z.radius());
    }

    /** 区域里某个标记方块（如被封停的刷怪器）的表现：子事件 {@code mark}，原点为方块中心。 */
    public static PowerFxPayload markPayload(Zone z, ServerLevel level, BlockPos m, long now, int duration) {
        return new PowerFxPayload(z.castId(), z.powerId(), PowerFxPayload.Phase.HIT, -1, level.dimension().location(),
                Vec3.atCenterOf(m), List.of(), now, m.asLong(), duration, "", "mark");
    }

    /** 区域表现的子事件里带的半径；不是区域事件时为空。 */
    @Nullable
    public static Double radiusOf(String tag) {
        if (!tag.startsWith("zone:")) {
            return null;
        }
        try {
            return Double.parseDouble(tag.substring(5));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ───────────── 存取 ─────────────

    public static PowerZones load(CompoundTag tag, HolderLookup.Provider registries) {
        PowerZones z = new PowerZones();
        for (Tag x : tag.getList("zones", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) x;
            try {
                List<BlockPos> marks = new ArrayList<>();
                for (long l : c.getLongArray("marks")) {
                    marks.add(BlockPos.of(l));
                }
                Zone zone = new Zone(c.getString("cast"), c.getString("kind"), c.getString("power"),
                        ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(c.getString("dim"))),
                        new Vec3(c.getDouble("x"), c.getDouble("y"), c.getDouble("z")), c.getDouble("radius"), c.getLong("until"),
                        c.getUUID("owner"), marks);
                z.zones.put(zone.castId(), zone);
            } catch (RuntimeException e) {
                MythicAge.LOGGER.warn("跳过损坏的神力区域：{}（{}）", c, e.getMessage());
            }
        }
        return z;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Zone z : zones.values()) {
            CompoundTag c = new CompoundTag();
            c.putString("cast", z.castId());
            c.putString("kind", z.kind());
            c.putString("power", z.powerId());
            c.putString("dim", z.dim().location().toString());
            c.putDouble("x", z.center().x);
            c.putDouble("y", z.center().y);
            c.putDouble("z", z.center().z);
            c.putDouble("radius", z.radius());
            c.putLong("until", z.until());
            c.putUUID("owner", z.owner());
            c.putLongArray("marks", z.marks().stream().mapToLong(BlockPos::asLong).toArray());
            list.add(c);
        }
        tag.put("zones", list);
        return tag;
    }

    // ───────────── 测试 ─────────────

    /** 只供 GameTest：存档往返后替换内存数据。 */
    public static void roundTripForTest(MinecraftServer server) {
        PowerZones data = get(server);
        PowerZones back = load(data.save(new CompoundTag(), server.registryAccess()), server.registryAccess());
        data.zones.clear();
        data.zones.putAll(back.zones);
    }

    /** 只供 GameTest：立即跑一轮逐刻与到期处理。 */
    public static void tickForTest(MinecraftServer server) {
        PowerZones data = get(server);
        long now = WorldClock.now(server);
        for (Zone z : List.copyOf(data.zones.values())) {
            ServerLevel level = server.getLevel(z.dim());
            Kind k = KINDS.get(z.kind());
            if (level == null || k == null) {
                continue;
            }
            if (z.until() <= now) {
                remove(server, z.castId());
                k.end(level, z);
            } else {
                k.tick(level, z, now);
            }
        }
    }
}
