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

    public void remove(UUID village) {
        Plan old = plans.remove(village);
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
            list.add(c);
        }
        tag.put("plans", list);
        return tag;
    }
}
