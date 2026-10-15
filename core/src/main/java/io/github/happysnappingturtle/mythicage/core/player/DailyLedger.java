package io.github.happysnappingturtle.mythicage.core.player;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 每日限购台账（T9 设计文档 §10）：键 → 当天的 MC 日序号与已购次数。读到旧日期视为 0。
 * 只保留最近写入的 {@link #CAPACITY} 个键，最旧的先淘汰。
 */
public final class DailyLedger {
    public static final int CAPACITY = 64;

    public record DailyCount(long day, int count) {
    }

    private final LinkedHashMap<String, DailyCount> entries = new LinkedHashMap<>();

    /** 今天已购次数。 */
    public int count(String key, long today) {
        DailyCount c = entries.get(key);
        return c != null && c.day() == today ? c.count() : 0;
    }

    /** 今天再记一次。 */
    public void record(String key, long today) {
        int next = count(key, today) + 1;
        entries.remove(key);
        entries.put(key, new DailyCount(today, next));
        Iterator<String> it = entries.keySet().iterator();
        while (entries.size() > CAPACITY && it.hasNext()) {
            it.next();
            it.remove();
        }
    }

    /** 存档读取用：按原样恢复一条。 */
    public void restore(String key, DailyCount count) {
        entries.put(key, count);
    }

    public Map<String, DailyCount> view() {
        return Collections.unmodifiableMap(entries);
    }

    void entriesFrom(DailyLedger other) {
        entries.clear();
        entries.putAll(other.entries);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DailyLedger d && entries.equals(d.entries);
    }

    @Override
    public int hashCode() {
        return entries.hashCode();
    }
}
