package io.github.happysnappingturtle.mythicage.village;

import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.core.village.gen.BuiltArea;
import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.core.village.gen.SpaceIndex;
import io.github.happysnappingturtle.mythicage.village.gen.VillageInfoPiece;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 村庄规划（V0 §4.4，SavedData {@code mythic_age_village_plans}）：按村庄 id 存结构起点、布局、锚点、已建范围。
 * 地块与施工项目在阶段 ② / T19 加入。另维护两个内存索引：结构起点 → 村庄、按维度的空间索引。
 */
public final class VillagePlans extends SavedData {
    public static final String NAME = "mythic_age_village_plans";

    /**
     * @param start     结构起点所在区块（ChunkPos.asLong），用于去重
     * @param populated 祭司与初始居民是否已生成
     */
    public record Plan(UUID village, ResourceKey<Level> dim, String structure, long start, VillageInfoPiece.Info info,
                       boolean populated) {
        public BuiltArea area() {
            return new BuiltArea(info.built());
        }

        public List<Pos> anchors(String name) {
            return info.anchors().getOrDefault(name, List.of());
        }

        Plan withPopulated() {
            return new Plan(village, dim, structure, start, info, true);
        }
    }

    private final Map<UUID, Plan> plans = new LinkedHashMap<>();
    /** 被玩家占用的预留地块：村庄 → 地块 id → 冲突坐标（V0 §5.3）。 */
    private final Map<UUID, Map<String, net.minecraft.core.BlockPos>> blocked = new HashMap<>();
    /** 道路（GA-12 §4，村庄地图用）：登记时从道路片段读出；没有这一项表示还没读到（老村子），空表表示没有路。 */
    private final Map<UUID, List<io.github.happysnappingturtle.mythicage.core.village.gen.VillagePlan.Path>> roads = new HashMap<>();
    private final Map<String, UUID> byStart = new HashMap<>();
    private final Map<ResourceKey<Level>, SpaceIndex<UUID>> space = new HashMap<>();

    public static VillagePlans get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(VillagePlans::new, VillagePlans::read, null), NAME);
    }

    private static String startKey(ResourceKey<Level> dim, String structure, long start) {
        return dim.location() + "|" + structure + "|" + start;
    }

    public void put(Plan p) {
        remove(p.village());
        plans.put(p.village(), p);
        byStart.put(startKey(p.dim(), p.structure(), p.start()), p.village());
        space.computeIfAbsent(p.dim(), k -> new SpaceIndex<>()).put(p.village(), p.area(), p.info().center());
        setDirty();
    }

    /** 施工落成（T19 §8.6）：换上新的规划信息（锚点、已建范围），空间索引随之更新；占地记录保留。 */
    public void updateInfo(UUID village, VillageInfoPiece.Info info) {
        Plan old = plans.get(village);
        if (old == null) {
            return;
        }
        Map<String, net.minecraft.core.BlockPos> keep = blocked.get(village);
        List<io.github.happysnappingturtle.mythicage.core.village.gen.VillagePlan.Path> keepRoads = roads.get(village);
        put(new Plan(old.village(), old.dim(), old.structure(), old.start(), info, old.populated()));
        if (keep != null) {
            blocked.put(village, keep);
        }
        if (keepRoads != null) {
            roads.put(village, keepRoads);
        }
    }

    public void remove(UUID village) {
        Plan old = plans.remove(village);
        blocked.remove(village);
        roads.remove(village);
        if (old != null) {
            byStart.remove(startKey(old.dim(), old.structure(), old.start()));
            SpaceIndex<UUID> idx = space.get(old.dim());
            if (idx != null) {
                idx.remove(village);
            }
            setDirty();
        }
    }

    public Optional<Plan> byVillage(UUID village) {
        return Optional.ofNullable(plans.get(village));
    }

    public Optional<UUID> byStart(ResourceKey<Level> dim, String structure, long start) {
        return Optional.ofNullable(byStart.get(startKey(dim, structure, start)));
    }

    public boolean has(UUID village) {
        return plans.containsKey(village);
    }

    /** 已建范围内的村庄。 */
    public Optional<UUID> at(ResourceKey<Level> dim, int x, int z) {
        SpaceIndex<UUID> idx = space.get(dim);
        return idx == null ? Optional.empty() : idx.at(x, z);
    }

    public boolean within(UUID village, int x, int z, int extra) {
        Plan p = plans.get(village);
        return p != null && space.get(p.dim()).within(village, x, z, extra);
    }

    /** 记下一块地块的占用情况：conflict 为空表示已清空、恢复 reserved。返回状态是否有变化。 */
    public boolean setBlocked(UUID village, String plot, Optional<net.minecraft.core.BlockPos> conflict) {
        Map<String, net.minecraft.core.BlockPos> m = blocked.computeIfAbsent(village, k -> new HashMap<>());
        net.minecraft.core.BlockPos old = conflict.isPresent() ? m.put(plot, conflict.get()) : m.remove(plot);
        boolean changed = !java.util.Objects.equals(old, conflict.orElse(null));
        if (changed) {
            setDirty();
        }
        return changed;
    }

    public Map<String, net.minecraft.core.BlockPos> blocked(UUID village) {
        return Map.copyOf(blocked.getOrDefault(village, Map.of()));
    }

    /** 道路；还没读到时为空（与「没有路」的空表不同）。 */
    public Optional<List<io.github.happysnappingturtle.mythicage.core.village.gen.VillagePlan.Path>> roads(UUID village) {
        return Optional.ofNullable(roads.get(village));
    }

    public void setRoads(UUID village, List<io.github.happysnappingturtle.mythicage.core.village.gen.VillagePlan.Path> paths) {
        if (plans.containsKey(village)) {
            roads.put(village, List.copyOf(paths));
            setDirty();
        }
    }

    public List<Plan> all() {
        return new ArrayList<>(plans.values());
    }

    public void markPopulatedPublic(UUID village) {
        markPopulated(village);
    }

    void markPopulated(UUID village) {
        Plan p = plans.get(village);
        if (p != null && !p.populated()) {
            plans.put(village, p.withPopulated());
            setDirty();
        }
    }

    // ───────────── 存取 ─────────────

    public static VillagePlans read(CompoundTag tag, HolderLookup.Provider registries) {
        VillagePlans r = new VillagePlans();
        for (Tag t : tag.getList("plans", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            try {
                r.put(new Plan(c.getUUID("village"),
                        ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(c.getString("dim"))),
                        c.getString("structure"), c.getLong("start"), VillageInfoPiece.read(c.getCompound("info")),
                        c.getBoolean("populated")));
                if (c.contains("roads", Tag.TAG_LIST)) {
                    List<io.github.happysnappingturtle.mythicage.core.village.gen.VillagePlan.Path> paths = new ArrayList<>();
                    for (Tag rt : c.getList("roads", Tag.TAG_COMPOUND)) {
                        CompoundTag rc = (CompoundTag) rt;
                        List<Pos> pts = new ArrayList<>();
                        for (long l : rc.getLongArray("p")) {
                            pts.add(VillageInfoPiece.unpack(l));
                        }
                        paths.add(new io.github.happysnappingturtle.mythicage.core.village.gen.VillagePlan.Path(rc.getInt("w"), pts));
                    }
                    r.roads.put(c.getUUID("village"), List.copyOf(paths));
                }
                CompoundTag bl = c.getCompound("blocked");
                for (String k : bl.getAllKeys()) {
                    r.blocked.computeIfAbsent(c.getUUID("village"), x -> new HashMap<>())
                            .put(k, net.minecraft.core.BlockPos.of(bl.getLong(k)));
                }
            } catch (RuntimeException e) {
                MythicAge.LOGGER.warn("跳过损坏的村庄规划：{}（{}）", c, e.getMessage());
            }
        }
        r.setDirty(false);
        return r;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Plan p : plans.values()) {
            CompoundTag c = new CompoundTag();
            c.putUUID("village", p.village());
            c.putString("dim", p.dim().location().toString());
            c.putString("structure", p.structure());
            c.putLong("start", p.start());
            c.put("info", VillageInfoPiece.write(p.info()));
            c.putBoolean("populated", p.populated());
            List<io.github.happysnappingturtle.mythicage.core.village.gen.VillagePlan.Path> rs = roads.get(p.village());
            if (rs != null) {
                ListTag rl = new ListTag();
                for (var path : rs) {
                    CompoundTag rc = new CompoundTag();
                    rc.putInt("w", path.width());
                    rc.putLongArray("p", path.points().stream().mapToLong(VillageInfoPiece::pack).toArray());
                    rl.add(rc);
                }
                c.put("roads", rl);
            }
            CompoundTag bl = new CompoundTag();
            blocked.getOrDefault(p.village(), Map.of()).forEach((k, v) -> bl.putLong(k, v.asLong()));
            c.put("blocked", bl);
            list.add(c);
        }
        tag.put("plans", list);
        return tag;
    }
}
