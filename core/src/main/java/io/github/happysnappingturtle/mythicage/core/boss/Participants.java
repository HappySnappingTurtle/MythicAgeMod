package io.github.happysnappingturtle.mythicage.core.boss;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * 试炼参战者（T23 §3、§7.1，D7）：开战后进过大厅的玩家。Boss 死时合法 =（a）对它造成过伤害（本人、弹射物、召唤物、宠物、神力；
 * 士兵不算，由调用方决定 {@code own}），或（b）在大厅里累计 ≥ {@value #PRESENCE} 刻。创造 / 旁观由调用方排除。
 * 断线不清记录，本场结算前一直保留。
 */
public final class Participants {
    public static final int PRESENCE = 600;

    public record Entry(boolean damaged, int presentTicks) {
    }

    private final Map<UUID, Entry> entries = new LinkedHashMap<>();

    public void damage(UUID player, boolean own) {
        if (!own) {
            entries.putIfAbsent(player, new Entry(false, 0));
            return;
        }
        Entry e = entries.getOrDefault(player, new Entry(false, 0));
        entries.put(player, new Entry(true, e.presentTicks()));
    }

    public void present(UUID player) {
        Entry e = entries.getOrDefault(player, new Entry(false, 0));
        entries.put(player, new Entry(e.damaged(), e.presentTicks() + 1));
    }

    public boolean legal(UUID player) {
        Entry e = entries.get(player);
        return e != null && (e.damaged() || e.presentTicks() >= PRESENCE);
    }

    /** 合法参战者（按进场顺序）；excluded 为真的（创造、旁观）不算。 */
    public List<UUID> legal(Predicate<UUID> excluded) {
        List<UUID> out = new ArrayList<>();
        for (UUID id : entries.keySet()) {
            if (legal(id) && !excluded.test(id)) {
                out.add(id);
            }
        }
        return out;
    }

    public Map<UUID, Entry> entries() {
        return Map.copyOf(entries);
    }

    public void restore(UUID id, Entry e) {
        entries.put(id, e);
    }

    public void clear() {
        entries.clear();
    }
}
