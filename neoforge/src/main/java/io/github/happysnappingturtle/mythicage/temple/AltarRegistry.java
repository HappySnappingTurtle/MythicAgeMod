package io.github.happysnappingturtle.mythicage.temple;

import io.github.happysnappingturtle.mythicage.MythicAge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 全服希腊神坛登记（T8a 设计文档 §5），世界级 SavedData {@code mythic_age_altars}，挂在主世界：
 * 神坛位置 → 主人；主人 → 神坛数（数量上限）；按维度 + 区块的索引（置脏查询用，不保存）。
 */
public final class AltarRegistry extends SavedData {
    public static final String NAME = "mythic_age_altars";

    public record Entry(GlobalPos pos, UUID owner, String ownerName, net.minecraft.core.Direction facing) {
    }

    private final Map<GlobalPos, Entry> altars = new LinkedHashMap<>();
    /** (维度, 区块) → 扫描体覆盖该区块的神坛。 */
    private final Map<ResourceKey<Level>, Map<Long, List<BlockPos>>> chunkIndex = new HashMap<>();
    private int indexRadius = -1;

    public static AltarRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(AltarRegistry::new, AltarRegistry::load, null), NAME);
    }

    public void add(Entry e) {
        altars.put(e.pos(), e);
        invalidateIndex();
        setDirty();
    }

    public boolean remove(GlobalPos pos) {
        boolean removed = altars.remove(pos) != null;
        if (removed) {
            invalidateIndex();
            setDirty();
        }
        return removed;
    }

    public Optional<Entry> get(GlobalPos pos) {
        return Optional.ofNullable(altars.get(pos));
    }

    public List<Entry> all() {
        return List.copyOf(altars.values());
    }

    public long countOwned(UUID owner) {
        return altars.values().stream().filter(e -> e.owner().equals(owner)).count();
    }

    /**
     * 与 pos 冲突的神坛：同维度，且水平切比雪夫距离 < spacing（垂直距离 < verticalSpacing），
     * 或两者的扫描体相交（修订 01 §2.4）。
     */
    public Optional<Entry> conflict(GlobalPos pos, net.minecraft.core.Direction facing, int spacing, int verticalSpacing,
                                    io.github.happysnappingturtle.mythicage.core.temple.TempleRules.Scan scan) {
        BlockPos[] mine = TempleScan.bounds(pos.pos(), facing, scan);
        for (Entry e : altars.values()) {
            if (!e.pos().dimension().equals(pos.dimension()) || e.pos().equals(pos)) {
                continue;
            }
            BlockPos a = e.pos().pos();
            BlockPos b = pos.pos();
            int h = Math.max(Math.abs(a.getX() - b.getX()), Math.abs(a.getZ() - b.getZ()));
            if (h < spacing && Math.abs(a.getY() - b.getY()) < verticalSpacing) {
                return Optional.of(e);
            }
            BlockPos[] other = TempleScan.bounds(a, e.facing(), scan);
            if (mine[0].getX() <= other[1].getX() && other[0].getX() <= mine[1].getX()
                    && mine[0].getY() <= other[1].getY() && other[0].getY() <= mine[1].getY()
                    && mine[0].getZ() <= other[1].getZ() && other[0].getZ() <= mine[1].getZ()) {
                return Optional.of(e);
            }
        }
        return Optional.empty();
    }

    /** 扫描体（水平最大伸展 radius）可能覆盖 pos 所在区块的神坛候选，调用方再精确判断。 */
    public List<BlockPos> altarsNear(ResourceKey<Level> dim, BlockPos pos, int radius) {
        if (indexRadius != radius) {
            rebuildIndex(radius);
        }
        Map<Long, List<BlockPos>> byChunk = chunkIndex.get(dim);
        if (byChunk == null) {
            return List.of();
        }
        return byChunk.getOrDefault(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4), List.of());
    }

    private void invalidateIndex() {
        indexRadius = -1;
    }

    private void rebuildIndex(int radius) {
        chunkIndex.clear();
        for (Entry e : altars.values()) {
            BlockPos p = e.pos().pos();
            Map<Long, List<BlockPos>> byChunk = chunkIndex.computeIfAbsent(e.pos().dimension(), k -> new HashMap<>());
            for (int cx = (p.getX() - radius) >> 4; cx <= (p.getX() + radius) >> 4; cx++) {
                for (int cz = (p.getZ() - radius) >> 4; cz <= (p.getZ() + radius) >> 4; cz++) {
                    byChunk.computeIfAbsent(ChunkPos.asLong(cx, cz), k -> new ArrayList<>()).add(p);
                }
            }
        }
        indexRadius = radius;
    }

    public Set<GlobalPos> positions() {
        return altars.keySet();
    }

    // ───────────── 存取 ─────────────

    private static AltarRegistry load(CompoundTag tag, HolderLookup.Provider registries) {
        AltarRegistry r = new AltarRegistry();
        for (Tag t : tag.getList("altars", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            try {
                ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(c.getString("dim")));
                GlobalPos pos = GlobalPos.of(dim, BlockPos.of(c.getLong("pos")));
                net.minecraft.core.Direction facing = net.minecraft.core.Direction.byName(c.getString("facing"));
                r.altars.put(pos, new Entry(pos, c.getUUID("owner"), c.getString("ownerName"),
                        facing == null || facing.getAxis().isVertical() ? net.minecraft.core.Direction.NORTH : facing));
            } catch (RuntimeException e) {
                MythicAge.LOGGER.warn("跳过损坏的神坛登记：{}（{}）", c, e.getMessage());
            }
        }
        return r;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Entry e : altars.values()) {
            CompoundTag c = new CompoundTag();
            c.putString("dim", e.pos().dimension().location().toString());
            c.putLong("pos", e.pos().pos().asLong());
            c.putUUID("owner", e.owner());
            c.putString("ownerName", e.ownerName());
            c.putString("facing", e.facing().getSerializedName());
            list.add(c);
        }
        tag.put("altars", list);
        return tag;
    }
}
