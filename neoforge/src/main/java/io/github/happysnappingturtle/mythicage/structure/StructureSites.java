package io.github.happysnappingturtle.mythicage.structure;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashSet;
import java.util.Set;

/** 已登记过的结构（T22 §3.3）：键 = 维度 | 结构 id | 起点区块。登记一次后不再生成守卫（被打碎的哨兵不重生）。 */
public final class StructureSites extends SavedData {
    public static final String NAME = "mythic_age_structure_sites";

    private final Set<String> registered = new LinkedHashSet<>();

    public static StructureSites get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(StructureSites::new, StructureSites::read, null), NAME);
    }

    public boolean registered(String key) {
        return registered.contains(key);
    }

    public void mark(String key) {
        if (registered.add(key)) {
            setDirty();
        }
    }

    static StructureSites read(CompoundTag tag, HolderLookup.Provider registries) {
        StructureSites s = new StructureSites();
        for (Tag t : tag.getList("registered", Tag.TAG_STRING)) {
            s.registered.add(t.getAsString());
        }
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag l = new ListTag();
        registered.forEach(k -> l.add(StringTag.valueOf(k)));
        tag.put("registered", l);
        return tag;
    }

    /** 只供 GameTest。 */
    public void forgetForTest(String key) {
        if (registered.remove(key)) {
            setDirty();
        }
    }
}
