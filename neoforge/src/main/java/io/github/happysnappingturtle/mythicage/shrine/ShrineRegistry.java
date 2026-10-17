package io.github.happysnappingturtle.mythicage.shrine;

import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
 * 全服神庙登记（T8a 设计文档 §5；T10b 设计文档 §3.2 加入文明与种类）。世界级 SavedData，挂在主世界；
 * 名称沿用 {@code mythic_age_altars}，T8a 的旧记录没有文明与种类，读取时视为 {@code greek / greek:altar}。
 * <p>
 * 位置 → 条目；主人 → 各种类的数量（上限）；按维度 + 区块的索引（只在内存里，供置脏查询）。
 */
public final class ShrineRegistry extends SavedData {
    public static final String NAME = "mythic_age_altars";
    /** T8a 记录缺省的种类。 */
    static final String LEGACY_KIND = "greek:altar";

    public record Entry(GlobalPos pos, UUID owner, String ownerName, Direction facing, Civilization civ, String kind) {
    }

    private final Map<GlobalPos, Entry> shrines = new LinkedHashMap<>();
    /** 各神庙最近一次认定的结构等级（不受主人时代封顶；T11b）。主人不在附近时晋升条件读它。 */
    private final Map<GlobalPos, Integer> levels = new HashMap<>();
    /** 种类 → (维度, 区块) → 该种类中半径 indexRadius 内覆盖该区块的神庙。 */
    private final Map<String, Map<ResourceKey<Level>, Map<Long, List<BlockPos>>>> chunkIndex = new HashMap<>();
    private final Map<String, Integer> indexRadius = new HashMap<>();

    public static ShrineRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(ShrineRegistry::new, ShrineRegistry::load, null), NAME);
    }

    public void add(Entry e) {
        // 新登记（或重新绑定）从 0 级算起，等下一次认定写回
        levels.remove(e.pos());
        shrines.put(e.pos(), e);
        invalidateIndex();
        setDirty();
    }

    public boolean remove(GlobalPos pos) {
        levels.remove(pos);
        boolean removed = shrines.remove(pos) != null;
        if (removed) {
            invalidateIndex();
            setDirty();
        }
        return removed;
    }

    /** 最近一次认定的结构等级；从未认定为 0。 */
    public int level(GlobalPos pos) {
        return levels.getOrDefault(pos, 0);
    }

    /** 记下认定的结构等级；没有该登记或数值未变时不做任何事。返回是否变化。 */
    public boolean setLevel(GlobalPos pos, int level) {
        if (!shrines.containsKey(pos) || level(pos) == level) {
            return false;
        }
        levels.put(pos, level);
        setDirty();
        return true;
    }

    public Optional<Entry> get(GlobalPos pos) {
        return Optional.ofNullable(shrines.get(pos));
    }

    public List<Entry> all() {
        return List.copyOf(shrines.values());
    }

    public List<Entry> ofKind(String kind) {
        return shrines.values().stream().filter(e -> e.kind().equals(kind)).toList();
    }

    public List<Entry> ownedBy(UUID owner, Civilization civ) {
        return shrines.values().stream().filter(e -> e.owner().equals(owner) && e.civ() == civ).toList();
    }

    /** 某玩家拥有的某种神庙数（数量上限按种类计，T10b 设计文档 §3.2）。 */
    public long countOwned(UUID owner, String kind) {
        return shrines.values().stream().filter(e -> e.owner().equals(owner) && e.kind().equals(kind)).count();
    }

    /**
     * 与 pos 太近的同种神庙：同维度，水平切比雪夫距离 &lt; spacing，且垂直距离 &lt; verticalSpacing。
     * 不同种类的神庙互不影响。
     */
    public Optional<Entry> tooClose(GlobalPos pos, String kind, int spacing, int verticalSpacing) {
        for (Entry e : shrines.values()) {
            if (!e.kind().equals(kind) || !e.pos().dimension().equals(pos.dimension()) || e.pos().equals(pos)) {
                continue;
            }
            BlockPos a = e.pos().pos();
            BlockPos b = pos.pos();
            int h = Math.max(Math.abs(a.getX() - b.getX()), Math.abs(a.getZ() - b.getZ()));
            if (h < spacing && Math.abs(a.getY() - b.getY()) < verticalSpacing) {
                return Optional.of(e);
            }
        }
        return Optional.empty();
    }

    /** 某种神庙中，水平伸展 radius 内可能覆盖 pos 所在区块的候选，调用方再精确判断。 */
    public List<BlockPos> near(String kind, ResourceKey<Level> dim, BlockPos pos, int radius) {
        if (indexRadius.getOrDefault(kind, -1) != radius) {
            rebuildIndex(kind, radius);
        }
        Map<Long, List<BlockPos>> byChunk = chunkIndex.getOrDefault(kind, Map.of()).get(dim);
        if (byChunk == null) {
            return List.of();
        }
        return byChunk.getOrDefault(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4), List.of());
    }

    private void invalidateIndex() {
        indexRadius.clear();
        chunkIndex.clear();
    }

    private void rebuildIndex(String kind, int radius) {
        Map<ResourceKey<Level>, Map<Long, List<BlockPos>>> index = new HashMap<>();
        for (Entry e : ofKind(kind)) {
            BlockPos p = e.pos().pos();
            Map<Long, List<BlockPos>> byChunk = index.computeIfAbsent(e.pos().dimension(), k -> new HashMap<>());
            for (int cx = (p.getX() - radius) >> 4; cx <= (p.getX() + radius) >> 4; cx++) {
                for (int cz = (p.getZ() - radius) >> 4; cz <= (p.getZ() + radius) >> 4; cz++) {
                    byChunk.computeIfAbsent(ChunkPos.asLong(cx, cz), k -> new ArrayList<>()).add(p);
                }
            }
        }
        chunkIndex.put(kind, index);
        indexRadius.put(kind, radius);
    }

    public Set<GlobalPos> positions() {
        return shrines.keySet();
    }

    // ───────────── 存取 ─────────────

    /** 读取存档；供 GameTest 验证旧格式迁移。 */
    public static ShrineRegistry load(CompoundTag tag, HolderLookup.Provider registries) {
        ShrineRegistry r = new ShrineRegistry();
        for (Tag t : tag.getList("altars", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            try {
                ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(c.getString("dim")));
                GlobalPos pos = GlobalPos.of(dim, BlockPos.of(c.getLong("pos")));
                Direction facing = Direction.byName(c.getString("facing"));
                String kind = c.contains("kind") ? c.getString("kind") : LEGACY_KIND;
                Civilization civ = Civilization.byId(kind.substring(0, Math.max(0, kind.indexOf(':'))))
                        .orElseThrow(() -> new IllegalArgumentException("未知文明的神庙种类 " + kind));
                r.shrines.put(pos, new Entry(pos, c.getUUID("owner"), c.getString("ownerName"),
                        facing == null || facing.getAxis().isVertical() ? Direction.NORTH : facing, civ, kind));
                if (c.getInt("level") > 0) {
                    r.levels.put(pos, c.getInt("level"));
                }
            } catch (RuntimeException e) {
                MythicAge.LOGGER.warn("跳过损坏的神庙登记：{}（{}）", c, e.getMessage());
            }
        }
        return r;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Entry e : shrines.values()) {
            CompoundTag c = new CompoundTag();
            c.putString("dim", e.pos().dimension().location().toString());
            c.putLong("pos", e.pos().pos().asLong());
            c.putUUID("owner", e.owner());
            c.putString("ownerName", e.ownerName());
            c.putString("facing", e.facing().getSerializedName());
            c.putString("kind", e.kind());
            c.putInt("level", level(e.pos()));
            list.add(c);
        }
        tag.put("altars", list);
        return tag;
    }
}
