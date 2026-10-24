package io.github.happysnappingturtle.mythicage.core.village.work;

import java.util.HashMap;
import java.util.Map;

/**
 * 目标失败计数（T19 设计文档 §5.2）：同一目标当日失败 {@link #LIMIT} 次后不再派单，第二天清零。避免死循环寻路。
 * 只存在内存里，重启清空无妨。
 */
public final class FailureBook {
    public static final int LIMIT = 3;

    private record Entry(long day, int count) {
    }

    private final Map<String, Entry> entries = new HashMap<>();

    public void fail(String target, long day) {
        Entry e = entries.get(target);
        entries.put(target, e == null || e.day() != day ? new Entry(day, 1) : new Entry(day, e.count() + 1));
    }

    public int count(String target, long day) {
        Entry e = entries.get(target);
        return e == null || e.day() != day ? 0 : e.count();
    }

    public boolean blocked(String target, long day) {
        return count(target, day) >= LIMIT;
    }

    public void clear() {
        entries.clear();
    }
}
