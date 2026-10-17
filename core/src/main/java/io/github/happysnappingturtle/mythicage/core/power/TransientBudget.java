package io.github.happysnappingturtle.mythicage.core.power;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 临时实体预算（决策 R-CAP；05 号 §2）：每名玩家 ≤ perPlayer、全服 ≤ global；超出时最早登记的先退场
 * （先按个人上限、再按全服上限），不拒绝施放、不部分生成；寿命 > 300 秒（6000 刻）的不是临时实体。
 * 只做账：返回该退场的实体 id，由服务端移除。
 */
public final class TransientBudget {
    public static final long MAX_LIFETIME_TICKS = 6000;

    /** @param seq 登记序号（越小越早） */
    public record Entry(String id, UUID owner, long seq, long expiresAt) {
    }

    private final Map<String, Entry> entries = new LinkedHashMap<>();
    private long nextSeq;

    /**
     * 登记一批（同一次施放的）临时实体。
     *
     * @return 因超额需要退场的旧实体 id（不含本批）
     * @throws IllegalArgumentException 寿命超过 300 秒
     */
    public synchronized List<String> register(UUID owner, List<String> ids, long now, long lifetimeTicks, int perPlayer,
                                              int global) {
        if (lifetimeTicks > MAX_LIFETIME_TICKS || lifetimeTicks <= 0) {
            throw new IllegalArgumentException("临时实体寿命应在 (0, 6000] 刻：" + lifetimeTicks);
        }
        for (String id : ids) {
            entries.put(id, new Entry(id, owner, nextSeq++, now + lifetimeTicks));
        }
        List<String> evicted = new ArrayList<>();
        List<Entry> mine = entries.values().stream().filter(e -> e.owner().equals(owner))
                .sorted(Comparator.comparingLong(Entry::seq)).toList();
        for (int i = 0; i < mine.size() - perPlayer; i++) {
            evicted.add(mine.get(i).id());
        }
        evicted.forEach(entries::remove);
        List<Entry> all = entries.values().stream().sorted(Comparator.comparingLong(Entry::seq)).toList();
        for (int i = 0; i < all.size() - global; i++) {
            evicted.add(all.get(i).id());
            entries.remove(all.get(i).id());
        }
        return evicted;
    }

    /** 服务器重启后实体重新载入时补登记（保持原序号先后：按到期时刻近似）。 */
    public synchronized void restore(String id, UUID owner, long expiresAt) {
        entries.putIfAbsent(id, new Entry(id, owner, nextSeq++, expiresAt));
    }

    /** 到期的实体 id（并从账里移除）。 */
    public synchronized List<String> expire(long now) {
        List<String> out = new ArrayList<>();
        entries.values().removeIf(e -> {
            if (e.expiresAt() <= now) {
                out.add(e.id());
                return true;
            }
            return false;
        });
        return out;
    }

    public synchronized void forget(String id) {
        entries.remove(id);
    }

    public synchronized int count(UUID owner) {
        return (int) entries.values().stream().filter(e -> e.owner().equals(owner)).count();
    }

    public synchronized int count() {
        return entries.size();
    }

    public synchronized boolean contains(String id) {
        return entries.containsKey(id);
    }

    public synchronized void clear() {
        entries.clear();
    }
}
