package io.github.happysnappingturtle.mythicage.power;

import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.runtime.WorldClock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiPredicate;

/**
 * 神力的临时方块（T12 设计文档 §2.6；05 号 §4）：记下原方块，到期复原；区块卸载时立即复原（直接改区块，
 * 免得临时方块被存进存档）；复原时那一格已经不是我们放的方块（被玩家改过）就不覆盖。
 * 不替换方块实体（容器等）；受保护的位置（Boss 竞技场，T23 登记）拒绝。
 * {@code godPowers.terrainEdits} 打开且神力卡声明了永久分支时，直接放永久方块、不登记。
 * 登记存成世界级存档数据，崩溃或重启后仍能复原。
 */
public final class TempBlocks extends SavedData {
    public static final String NAME = "mythic_age_temp_blocks";
    private static final int CHECK_INTERVAL = 10;

    public enum Result { PLACED, PERMANENT, REFUSED_BLOCK_ENTITY, REFUSED_PROTECTED }

    /** 不允许放临时方块的位置（Boss 竞技场等，由各系统登记）。 */
    public static final List<BiPredicate<ServerLevel, BlockPos>> PROTECTED = new CopyOnWriteArrayList<>();

    public record Entry(GlobalPos pos, BlockState placed, BlockState original, long until, UUID owner) {
    }

    private final Map<GlobalPos, Entry> entries = new LinkedHashMap<>();

    public static TempBlocks get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(TempBlocks::new, TempBlocks::load, null), NAME);
    }

    /**
     * 放一个临时方块。
     *
     * @param permanentBranch 神力卡声明了永久分支（只有 terrainEdits 打开时才生效）
     */
    public static Result place(ServerLevel level, BlockPos pos, BlockState state, long durationTicks, UUID owner,
                               boolean permanentBranch) {
        if (level.getBlockEntity(pos) != null) {
            return Result.REFUSED_BLOCK_ENTITY;
        }
        for (BiPredicate<ServerLevel, BlockPos> p : PROTECTED) {
            if (p.test(level, pos)) {
                return Result.REFUSED_PROTECTED;
            }
        }
        if (permanentBranch && PowerService.config().terrainEdits()) {
            level.setBlock(pos, state, 3);
            return Result.PERMANENT;
        }
        TempBlocks data = get(level.getServer());
        GlobalPos key = GlobalPos.of(level.dimension(), pos.immutable());
        Entry old = data.entries.get(key);
        // 同一格再放：沿用最初记下的原方块
        BlockState original = old != null ? old.original() : level.getBlockState(pos);
        level.setBlock(pos, state, 3);
        data.entries.put(key, new Entry(key, state, original, WorldClock.now(level.getServer()) + durationTicks, owner));
        data.setDirty();
        return Result.PLACED;
    }

    /** A player has explicitly replaced this position, even with an identical state: relinquish ownership without editing it. */
    public static void forget(ServerLevel level, BlockPos pos) {
        TempBlocks data = get(level.getServer());
        Entry entry = data.entries.get(GlobalPos.of(level.dimension(), pos));
        if (entry != null) data.drop(entry);
    }

    /** 立即复原一个临时方块（同一玩家新放的诱兽石替换旧石等）。返回是否复原了登记里的方块。 */
    public static boolean restoreNow(ServerLevel level, BlockPos pos) {
        TempBlocks data = get(level.getServer());
        Entry e = data.entries.get(GlobalPos.of(level.dimension(), pos));
        if (e == null) {
            return false;
        }
        if (level.isLoaded(pos) && level.getBlockState(pos) == e.placed()) {
            level.setBlock(pos, e.original(), 3);
        }
        data.drop(e);
        return true;
    }

    public static void onServerTickPost(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % CHECK_INTERVAL != 0) {
            return;
        }
        TempBlocks data = get(server);
        if (data.entries.isEmpty()) {
            return;
        }
        long now = WorldClock.now(server);
        for (Entry e : List.copyOf(data.entries.values())) {
            if (e.until() > now) {
                continue;
            }
            ServerLevel level = server.getLevel(e.pos().dimension());
            if (level == null) {
                data.drop(e);
            } else if (level.isLoaded(e.pos().pos())) {
                if (level.getBlockState(e.pos().pos()) == e.placed()) {
                    level.setBlock(e.pos().pos(), e.original(), 3);
                }
                data.drop(e);
            }
            // 没载入的格：区块卸载时已经复原并销账；这里只剩极少数崩溃残留，载入时处理
        }
    }

    /** 区块卸载：这块区块里的临时方块立即复原（直接改区块，不触发邻居更新）。 */
    public static void onChunkUnload(ChunkEvent.Unload event) {
        restoreChunk(event.getLevel(), event.getChunk());
    }

    /** 区块载入：上次没来得及复原的（崩溃残留）立即复原。 */
    public static void onChunkLoad(ChunkEvent.Load event) {
        restoreChunk(event.getLevel(), event.getChunk());
    }

    private static void restoreChunk(net.minecraft.world.level.LevelAccessor la, ChunkAccess chunk) {
        if (!(la instanceof ServerLevel level) || !(chunk instanceof LevelChunk lc)) {
            return;
        }
        TempBlocks data = get(level.getServer());
        if (data.entries.isEmpty()) {
            return;
        }
        ChunkPos cp = chunk.getPos();
        for (Entry e : List.copyOf(data.entries.values())) {
            BlockPos p = e.pos().pos();
            if (e.pos().dimension() == level.dimension() && (p.getX() >> 4) == cp.x && (p.getZ() >> 4) == cp.z) {
                if (lc.getBlockState(p) == e.placed()) {
                    lc.setBlockState(p, e.original(), false);
                    lc.setUnsaved(true);
                }
                data.drop(e);
            }
        }
    }

    /** 关服前：所有已载入的临时方块复原。 */
    public static void onServerStopping(ServerStoppingEvent event) {
        MinecraftServer server = event.getServer();
        TempBlocks data = get(server);
        for (Entry e : List.copyOf(data.entries.values())) {
            ServerLevel level = server.getLevel(e.pos().dimension());
            if (level != null && level.isLoaded(e.pos().pos())) {
                if (level.getBlockState(e.pos().pos()) == e.placed()) {
                    level.setBlock(e.pos().pos(), e.original(), 3);
                }
                data.drop(e);
            }
        }
    }

    private void drop(Entry e) {
        entries.remove(e.pos());
        setDirty();
    }

    public List<Entry> entries() {
        return List.copyOf(entries.values());
    }

    // ───────────── 存取 ─────────────

    public static TempBlocks load(CompoundTag tag, HolderLookup.Provider registries) {
        TempBlocks t = new TempBlocks();
        var blocks = registries.lookupOrThrow(Registries.BLOCK);
        for (Tag x : tag.getList("blocks", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) x;
            try {
                ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(c.getString("dim")));
                GlobalPos pos = GlobalPos.of(dim, BlockPos.of(c.getLong("pos")));
                t.entries.put(pos, new Entry(pos, NbtUtils.readBlockState(blocks, c.getCompound("placed")),
                        NbtUtils.readBlockState(blocks, c.getCompound("original")), c.getLong("until"), c.getUUID("owner")));
            } catch (RuntimeException e) {
                MythicAge.LOGGER.warn("跳过损坏的临时方块登记：{}（{}）", c, e.getMessage());
            }
        }
        return t;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Entry e : entries.values()) {
            CompoundTag c = new CompoundTag();
            c.putString("dim", e.pos().dimension().location().toString());
            c.putLong("pos", e.pos().pos().asLong());
            c.put("placed", NbtUtils.writeBlockState(e.placed()));
            c.put("original", NbtUtils.writeBlockState(e.original()));
            c.putLong("until", e.until());
            c.putUUID("owner", e.owner());
            list.add(c);
        }
        tag.put("blocks", list);
        return tag;
    }

    /** 只供 GameTest：清空登记（不复原）。 */
    public static void clearForTest(MinecraftServer server) {
        get(server).entries.clear();
    }

    static List<Entry> snapshot(MinecraftServer server) {
        return new ArrayList<>(get(server).entries.values());
    }
}
