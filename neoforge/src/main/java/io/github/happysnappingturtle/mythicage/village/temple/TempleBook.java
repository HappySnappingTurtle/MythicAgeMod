package io.github.happysnappingturtle.mythicage.village.temple;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 一个村子的神庙记录（GA-13 §2.2–§2.5，五文明通用）：供奉者、近 7 日在这里祈祷过的信徒、
 * 各供奉者今天献供的日子、各供奉者在这里供奉的圣物。挂在 {@code VillageEconomy.State.temple}，随村庄运行数据存档。
 */
public final class TempleBook {
    /** 近几日的信徒（预计日产按它算，同 T10）。 */
    public static final int RECENT_DAYS = 7;
    public static final int RECENT_LIMIT = 64;

    /** 怎么成为供奉者的。 */
    public enum Via {
        BLESSING, PROMOTION, RITE, MIGRATION, JOIN;

        static Via byName(String s) {
            for (Via v : values()) {
                if (v.name().equalsIgnoreCase(s)) {
                    return v;
                }
            }
            return JOIN;
        }
    }

    /** @param since 成为供奉者的 MC 日 */
    public record Devotee(long since, Via via) {
    }

    private final Map<UUID, Devotee> devotees = new LinkedHashMap<>();
    /** 信徒（居民或原版村民）→ 最近一次在这里入账的 MC 日；按时间先后。 */
    private final LinkedHashMap<UUID, Long> believers = new LinkedHashMap<>();
    /** 供奉者 → 献供的 MC 日（GA-4 规则：每人每村每天一次）。 */
    private final Map<UUID, Long> offered = new LinkedHashMap<>();
    /** 供奉者 → 在这里供奉的圣物（按放入先后）。 */
    private final Map<UUID, List<ItemStack>> relics = new LinkedHashMap<>();
    /** 完好度 0–1（GA-13 §2.6）；还没检查过为 1。 */
    private double integrity = 1;

    public double integrity() {
        return integrity;
    }

    public void setIntegrity(double v) {
        integrity = Double.isFinite(v) ? Math.max(0, Math.min(1, v)) : 1;
    }

    // ───────────── 供奉者 ─────────────

    public boolean isDevotee(UUID player) {
        return devotees.containsKey(player);
    }

    /** 加为供奉者；已经是时不改原来的记录，返回 false。 */
    public boolean addDevotee(UUID player, long day, Via via) {
        return devotees.putIfAbsent(player, new Devotee(day, via)) == null;
    }

    /** 不再供奉：去掉记录与当天献供；圣物交给调用方处理（返回取出的圣物）。 */
    public List<ItemStack> removeDevotee(UUID player) {
        devotees.remove(player);
        offered.remove(player);
        List<ItemStack> out = relics.remove(player);
        return out == null ? List.of() : out;
    }

    public Set<UUID> devotees() {
        return Collections.unmodifiableSet(devotees.keySet());
    }

    public Map<UUID, Devotee> devoteeView() {
        return Collections.unmodifiableMap(devotees);
    }

    // ───────────── 信徒 ─────────────

    public boolean prayedToday(UUID villager, long today) {
        Long d = believers.get(villager);
        return d != null && d == today;
    }

    /** 记一次入账；返回近 7 日信徒数是否变化。 */
    public boolean notePrayer(UUID villager, long today) {
        Long before = believers.remove(villager);
        believers.put(villager, today);
        boolean changed = before == null;
        changed |= prune(today);
        while (believers.size() > RECENT_LIMIT) {
            believers.remove(believers.keySet().iterator().next());
            changed = true;
        }
        return changed;
    }

    public int believersToday(long today) {
        int n = 0;
        for (long d : believers.values()) {
            if (d == today) {
                n++;
            }
        }
        return n;
    }

    /** 近 7 日（含今天）的信徒数。 */
    public int believers(long today) {
        prune(today);
        return believers.size();
    }

    private boolean prune(long today) {
        boolean removed = false;
        for (Iterator<Map.Entry<UUID, Long>> it = believers.entrySet().iterator(); it.hasNext(); ) {
            if (today - it.next().getValue() >= RECENT_DAYS) {
                it.remove();
                removed = true;
            }
        }
        return removed;
    }

    // ───────────── 献供 ─────────────

    public boolean offeredOn(UUID player, long day) {
        Long d = offered.get(player);
        return d != null && d == day;
    }

    public void markOffered(UUID player, long day) {
        offered.put(player, day);
    }

    // ───────────── 圣物 ─────────────

    public List<ItemStack> relics(UUID player) {
        List<ItemStack> l = relics.get(player);
        return l == null ? List.of() : Collections.unmodifiableList(l);
    }

    public void addRelic(UUID player, ItemStack stack) {
        relics.computeIfAbsent(player, k -> new ArrayList<>()).add(stack.copyWithCount(1));
    }

    public ItemStack removeRelic(UUID player, int index) {
        List<ItemStack> l = relics.get(player);
        if (l == null || index < 0 || index >= l.size()) {
            return ItemStack.EMPTY;
        }
        ItemStack s = l.remove(index);
        if (l.isEmpty()) {
            relics.remove(player);
        }
        return s;
    }

    /** 全部供奉者的圣物，按放入先后（展示用）。 */
    public List<ItemStack> allRelics() {
        List<ItemStack> out = new ArrayList<>();
        relics.values().forEach(out::addAll);
        return out;
    }

    // ───────────── 存取 ─────────────

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag t = new CompoundTag();
        ListTag dev = new ListTag();
        devotees.forEach((id, d) -> {
            CompoundTag c = new CompoundTag();
            c.putUUID("id", id);
            c.putLong("since", d.since());
            c.putString("via", d.via().name().toLowerCase(java.util.Locale.ROOT));
            Long o = offered.get(id);
            if (o != null) {
                c.putLong("offered", o);
            }
            List<ItemStack> rl = relics.get(id);
            if (rl != null && !rl.isEmpty()) {
                ListTag items = new ListTag();
                rl.forEach(s -> items.add(s.save(registries)));
                c.put("relics", items);
            }
            dev.add(c);
        });
        t.put("devotees", dev);
        ListTag bel = new ListTag();
        believers.forEach((id, day) -> {
            CompoundTag c = new CompoundTag();
            c.putUUID("id", id);
            c.putLong("day", day);
            bel.add(c);
        });
        t.put("believers", bel);
        t.putDouble("integrity", integrity);
        return t;
    }

    public static TempleBook load(CompoundTag t, HolderLookup.Provider registries) {
        TempleBook b = new TempleBook();
        for (Tag x : t.getList("devotees", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) x;
            if (!c.hasUUID("id")) {
                continue;
            }
            UUID id = c.getUUID("id");
            b.devotees.put(id, new Devotee(c.getLong("since"), Via.byName(c.getString("via"))));
            if (c.contains("offered")) {
                b.offered.put(id, c.getLong("offered"));
            }
            for (Tag i : c.getList("relics", Tag.TAG_COMPOUND)) {
                ItemStack.parse(registries, i).filter(s -> !s.isEmpty())
                        .ifPresent(s -> b.relics.computeIfAbsent(id, k -> new ArrayList<>()).add(s));
            }
        }
        for (Tag x : t.getList("believers", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) x;
            if (c.hasUUID("id")) {
                b.believers.put(c.getUUID("id"), c.getLong("day"));
            }
        }
        b.setIntegrity(t.contains("integrity") ? t.getDouble("integrity") : 1);
        return b;
    }
}
