package io.github.happysnappingturtle.mythicage.village;

import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.core.player.Age;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 村庄登记（T10a 设计文档 §2.1）：以聚会钟为唯一键。主世界 SavedData，维度写在钟的 GlobalPos 里。
 * 文明与主神一经登记不变；唯一的例外是登记时该文明还没有主神权重、主神为空的村庄，
 * 之后有了权重会按同一坐标补抽一次（T10b，19 号 M2）。钟连续 7 个 MC 日不在时注销。
 */
public final class VillageRegistry extends SavedData {
    public static final String NAME = "mythic_age_villages";
    public static final int EXPIRE_DAYS = 7;

    public enum Origin {
        VANILLA, GENERATED, FOUNDED;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        static Origin byId(String id) {
            for (Origin o : values()) {
                if (o.id().equals(id)) {
                    return o;
                }
            }
            return VANILLA;
        }
    }

    /**
     * @param civ              文明 id；中立为空串
     * @param patron           主神 id；中立或该文明没有主神权重时为空串
     * @param missingSinceDay  钟不在的起始 MC 日；钟在时为 −1
     * @param stage            村庄信仰阶段（T11a）：有文明的村庄登记时为远古，中立为 NONE；只升不降
     * @param shrines          各时代立过祠的次神（T11a）
     * @param baseline         远古基线人口：首次升到古典时的成员数；−1 = 未记录（T11a 阶段 ③）
     * @param lastMigrationDay 上次吸纳移民的 MC 日；−1 = 从未
     */
    public record Village(UUID id, GlobalPos bell, String civ, String patron, Origin origin, @Nullable UUID founder,
                          String vanillaType, long missingSinceDay, Age stage, Map<Age, List<String>> shrines,
                          int baseline, long lastMigrationDay) {
        public Village {
            EnumMap<Age, List<String>> copy = new EnumMap<>(Age.class);
            shrines.forEach((a, l) -> copy.put(a, List.copyOf(l)));
            shrines = java.util.Collections.unmodifiableMap(copy);
        }

        /** T10a 的构造方式：阶段按文明给出缺省值，没有祠。 */
        public Village(UUID id, GlobalPos bell, String civ, String patron, Origin origin, @Nullable UUID founder,
                       String vanillaType, long missingSinceDay) {
            this(id, bell, civ, patron, origin, founder, vanillaType, missingSinceDay, defaultStage(civ), Map.of(), -1, -1);
        }

        public static Age defaultStage(String civ) {
            return civ.isEmpty() ? Age.NONE : Age.ARCHAIC;
        }

        public boolean neutral() {
            return civ.isEmpty();
        }

        /** 村庄锚点（T18）：位置就是 {@link #bell()}，种类由来源推出。 */
        public VillageAnchor anchor() {
            return new VillageAnchor(bell, VillageAnchor.kindOf(origin));
        }

        public boolean hasShrine(String god) {
            return shrines.values().stream().anyMatch(l -> l.contains(god));
        }

        /** 全部立过祠的次神。 */
        public java.util.Set<String> shrineGods() {
            java.util.Set<String> out = new java.util.LinkedHashSet<>();
            shrines.values().forEach(out::addAll);
            return out;
        }

        Village withMissingSince(long day) {
            return new Village(id, bell, civ, patron, origin, founder, vanillaType, day, stage, shrines, baseline,
                    lastMigrationDay);
        }

        Village withPatron(String p) {
            return new Village(id, bell, civ, p, origin, founder, vanillaType, missingSinceDay, stage, shrines, baseline,
                    lastMigrationDay);
        }

        /** 阶段取最高；为某时代的次神立祠（已有则不重复）。 */
        public Village withRite(Age newStage, Age shrineAge, String god) {
            Age st = newStage.atLeast(stage) ? newStage : stage;
            EnumMap<Age, List<String>> sh = new EnumMap<>(Age.class);
            shrines.forEach((a, l) -> sh.put(a, new ArrayList<>(l)));
            if (!god.isEmpty() && shrineAge != Age.NONE) {
                List<String> l = sh.computeIfAbsent(shrineAge, k -> new ArrayList<>());
                if (!l.contains(god)) {
                    l.add(god);
                }
            }
            return new Village(id, bell, civ, patron, origin, founder, vanillaType, missingSinceDay, st, sh, baseline,
                    lastMigrationDay);
        }

        public Village withStage(Age newStage) {
            return new Village(id, bell, civ, patron, origin, founder, vanillaType, missingSinceDay, newStage, shrines,
                    baseline, lastMigrationDay);
        }

        public Village withPopulation(int newBaseline, long migrationDay) {
            return new Village(id, bell, civ, patron, origin, founder, vanillaType, missingSinceDay, stage, shrines,
                    newBaseline, migrationDay);
        }
    }

    private final Map<UUID, Village> byId = new LinkedHashMap<>();
    private final Map<GlobalPos, UUID> byBell = new LinkedHashMap<>();

    public static VillageRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(VillageRegistry::new, VillageRegistry::read, null), NAME);
    }

    public void add(Village v) {
        Village old = byId.put(v.id(), v);
        if (old != null) {
            byBell.remove(old.bell());
        }
        byBell.put(v.bell(), v.id());
        setDirty();
    }

    public Optional<Village> byBell(GlobalPos bell) {
        UUID id = byBell.get(bell);
        return id == null ? Optional.empty() : Optional.ofNullable(byId.get(id));
    }

    public Optional<Village> byId(UUID id) {
        return Optional.ofNullable(byId.get(id));
    }

    public boolean remove(GlobalPos bell) {
        UUID id = byBell.remove(bell);
        if (id != null) {
            byId.remove(id);
            setDirty();
            return true;
        }
        return false;
    }

    /** 用新的记录替换（仪式、管理命令、移民）。 */
    public void update(Village v) {
        if (byId.containsKey(v.id())) {
            add(v);
        }
    }

    /** 补抽主神（只对主神为空的村庄生效）。 */
    void setPatron(Village v, String patron) {
        if (v.patron().isEmpty() && !patron.isEmpty()) {
            add(v.withPatron(patron));
        }
    }

    void setMissingSince(Village v, long day) {
        if (v.missingSinceDay() != day) {
            add(v.withMissingSince(day));
        }
    }

    public List<Village> all() {
        return new ArrayList<>(byId.values());
    }

    public List<Village> in(ResourceKey<Level> dim) {
        return byId.values().stream().filter(v -> v.bell().dimension().equals(dim)).toList();
    }

    // ───────────── 存取 ─────────────

    public static VillageRegistry read(CompoundTag tag, HolderLookup.Provider registries) {
        VillageRegistry r = new VillageRegistry();
        for (Tag t : tag.getList("villages", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            try {
                ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(c.getString("dim")));
                String civ = c.getString("civ");
                Age stage = c.contains("stage") ? Age.byId(c.getString("stage")).orElse(Village.defaultStage(civ))
                        : Village.defaultStage(civ);
                EnumMap<Age, List<String>> shrines = new EnumMap<>(Age.class);
                CompoundTag sh = c.getCompound("shrines");
                for (String a : sh.getAllKeys()) {
                    Age.byId(a).ifPresent(age -> {
                        List<String> gods = new ArrayList<>();
                        for (Tag g : sh.getList(a, Tag.TAG_STRING)) {
                            gods.add(g.getAsString());
                        }
                        shrines.put(age, gods);
                    });
                }
                Village v = new Village(c.getUUID("id"), GlobalPos.of(dim, BlockPos.of(c.getLong("bell"))), civ,
                        c.getString("patron"), Origin.byId(c.getString("origin")),
                        c.hasUUID("founder") ? c.getUUID("founder") : null, c.getString("vanillaType"),
                        c.contains("missingSince") ? c.getLong("missingSince") : -1, stage, shrines,
                        c.contains("baseline") ? c.getInt("baseline") : -1,
                        c.contains("lastMigration") ? c.getLong("lastMigration") : -1);
                r.byId.put(v.id(), v);
                r.byBell.put(v.bell(), v.id());
            } catch (RuntimeException e) {
                MythicAge.LOGGER.warn("跳过损坏的村庄登记：{}（{}）", c, e.getMessage());
            }
        }
        return r;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Village v : byId.values()) {
            CompoundTag c = new CompoundTag();
            c.putUUID("id", v.id());
            c.putString("dim", v.bell().dimension().location().toString());
            c.putLong("bell", v.bell().pos().asLong());
            c.putString("civ", v.civ());
            c.putString("patron", v.patron());
            c.putString("origin", v.origin().id());
            if (v.founder() != null) {
                c.putUUID("founder", v.founder());
            }
            c.putString("vanillaType", v.vanillaType());
            if (v.missingSinceDay() >= 0) {
                c.putLong("missingSince", v.missingSinceDay());
            }
            c.putString("stage", v.stage().id());
            CompoundTag sh = new CompoundTag();
            v.shrines().forEach((a, gods) -> {
                ListTag l = new ListTag();
                gods.forEach(g -> l.add(net.minecraft.nbt.StringTag.valueOf(g)));
                sh.put(a.id(), l);
            });
            c.put("shrines", sh);
            c.putInt("baseline", v.baseline());
            c.putLong("lastMigration", v.lastMigrationDay());
            list.add(c);
        }
        tag.put("villages", list);
        return tag;
    }
}
