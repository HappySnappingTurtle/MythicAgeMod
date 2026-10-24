package io.github.happysnappingturtle.mythicage.village.economy;

import io.github.happysnappingturtle.mythicage.core.village.build.BuildProject;
import io.github.happysnappingturtle.mythicage.core.village.gen.Box;
import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.core.village.gen.Turn;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;

import java.util.BitSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/** 施工项目、已建建筑、落成通告的存档格式（村庄运行数据的一部分）。 */
final class ProjectNbt {
    private ProjectNbt() {
    }

    static CompoundTag write(BuildProject p) {
        CompoundTag t = new CompoundTag();
        t.putLong("id", p.id);
        t.putString("building", p.building);
        t.putString("plot", p.plot);
        t.putString("god", p.god);
        t.putString("replaces", p.replaces);
        t.putLong("origin", pack(p.origin));
        t.putString("turn", p.turn.name());
        if (p.oldOrigin != null) {
            t.putLong("oldOrigin", pack(p.oldOrigin));
            t.putString("oldTurn", p.oldTurn.name());
        }
        t.putLong("startDay", p.startDay);
        t.putString("state", p.state.name());
        t.putInt("segments", p.segments);
        t.putLongArray("placed", p.placed.toLongArray());
        t.put("planned", doubles(p.planned));
        t.put("spent", doubles(p.spent));
        t.put("returned", doubles(p.returned));
        t.put("consumed", ints(p.consumed));
        t.put("returnedGiven", ints(p.returnedGiven));
        CompoundTag held = new CompoundTag();
        p.held.forEach((k, m) -> held.put(k, ints(m)));
        t.put("held", held);
        if (p.blockedAt != null) {
            t.putLong("blockedAt", pack(p.blockedAt));
        }
        t.putString("blockedReason", p.blockedReason);
        return t;
    }

    static BuildProject read(CompoundTag t) {
        BuildProject p = new BuildProject(t.getLong("id"), t.getString("building"), t.getString("plot"), t.getString("god"),
                t.getString("replaces"), unpack(t.getLong("origin")), Turn.valueOf(t.getString("turn")),
                t.contains("oldOrigin") ? unpack(t.getLong("oldOrigin")) : null,
                t.contains("oldTurn") ? Turn.valueOf(t.getString("oldTurn")) : null, t.getLong("startDay"));
        p.state = BuildProject.State.valueOf(t.getString("state"));
        p.segments = t.getInt("segments");
        p.placed.or(BitSet.valueOf(t.getLongArray("placed")));
        readDoubles(t.getCompound("planned"), p.planned);
        readDoubles(t.getCompound("spent"), p.spent);
        readDoubles(t.getCompound("returned"), p.returned);
        readInts(t.getCompound("consumed"), p.consumed);
        readInts(t.getCompound("returnedGiven"), p.returnedGiven);
        CompoundTag held = t.getCompound("held");
        for (String k : held.getAllKeys()) {
            Map<String, Integer> m = new TreeMap<>();
            readInts(held.getCompound(k), m);
            p.held.put(k, m);
        }
        p.blockedAt = t.contains("blockedAt") ? unpack(t.getLong("blockedAt")) : null;
        p.blockedReason = t.getString("blockedReason");
        return p;
    }

    static CompoundTag writeBuilt(VillageEconomy.Built b) {
        CompoundTag t = new CompoundTag();
        t.putString("building", b.building());
        t.putLong("origin", pack(b.origin()));
        t.putString("turn", b.turn().name());
        Box x = b.box();
        t.putIntArray("box", new int[]{x.minX(), x.minY(), x.minZ(), x.maxX(), x.maxY(), x.maxZ()});
        t.putString("plot", b.plot());
        t.putString("god", b.god());
        t.putLong("day", b.day());
        return t;
    }

    static VillageEconomy.Built readBuilt(CompoundTag t) {
        int[] v = t.getIntArray("box");
        return new VillageEconomy.Built(t.getString("building"), unpack(t.getLong("origin")), Turn.valueOf(t.getString("turn")),
                new Box(v[0], v[1], v[2], v[3], v[4], v[5]), t.getString("plot"), t.getString("god"), t.getLong("day"));
    }

    static CompoundTag writeNotice(VillageEconomy.Notice n) {
        CompoundTag t = new CompoundTag();
        t.putLong("day", n.day());
        t.putString("building", n.building());
        t.putString("god", n.god());
        ListTag sent = new ListTag();
        n.sent().forEach(u -> sent.add(NbtUtils.createUUID(u)));
        t.put("sent", sent);
        return t;
    }

    static VillageEconomy.Notice readNotice(CompoundTag t) {
        Set<UUID> sent = new LinkedHashSet<>();
        t.getList("sent", Tag.TAG_INT_ARRAY).forEach(u -> sent.add(NbtUtils.loadUUID(u)));
        return new VillageEconomy.Notice(t.getLong("day"), t.getString("building"), t.getString("god"), sent);
    }

    private static CompoundTag doubles(Map<String, Double> m) {
        CompoundTag t = new CompoundTag();
        m.forEach(t::putDouble);
        return t;
    }

    private static CompoundTag ints(Map<String, Integer> m) {
        CompoundTag t = new CompoundTag();
        m.forEach(t::putInt);
        return t;
    }

    private static void readDoubles(CompoundTag t, Map<String, Double> into) {
        t.getAllKeys().forEach(k -> into.put(k, t.getDouble(k)));
    }

    private static void readInts(CompoundTag t, Map<String, Integer> into) {
        t.getAllKeys().forEach(k -> into.put(k, t.getInt(k)));
    }

    private static long pack(Pos p) {
        return BlockPos.asLong(p.x(), p.y(), p.z());
    }

    private static Pos unpack(long l) {
        return new Pos(BlockPos.getX(l), BlockPos.getY(l), BlockPos.getZ(l));
    }
}
