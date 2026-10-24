package io.github.happysnappingturtle.mythicage.core.conduct;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 事件账本（02 号 §2「同事件分级去重」、§4「跨重启避免重复升级」）：键 = 责任人 + 受害者 + 类别。
 * 窗口从该事件第一次计分算起；窗口内再伤害不计，致死只补到合计值。
 */
public final class IncidentLedger {
    /** 最多保留的条目（超出时丢最旧的）。 */
    public static final int CAPACITY = 64;

    public record Entry(int charged, long start) {
    }

    private final Map<String, Entry> entries = new LinkedHashMap<>();

    public static String key(String responsible, String victim, String category) {
        return responsible + "|" + victim + "|" + category;
    }

    /**
     * 结算一次伤害或致死，返回本次变化（≤ 0）。harm、killTotal 都是 ≤ 0 的分值。
     */
    public int charge(String key, boolean kill, long now, long window, int harm, int killTotal) {
        Entry e = entries.get(key);
        boolean open = e != null && now - e.start() <= window;
        int already = open ? e.charged() : 0;
        int target = kill ? Math.min(harm, killTotal) : harm;
        if (open && !kill) {
            return 0;
        }
        int delta = Math.min(0, target - already);
        if (delta == 0 && open) {
            return 0;
        }
        entries.remove(key);
        entries.put(key, new Entry(already + delta, open ? e.start() : now));
        while (entries.size() > CAPACITY) {
            entries.remove(entries.keySet().iterator().next());
        }
        return delta;
    }

    /** 这名责任人在窗口内是否对某个前缀（如某村庄的受害者）留下过罚分。 */
    public boolean chargedRecently(String responsible, long now, long window) {
        for (Map.Entry<String, Entry> e : entries.entrySet()) {
            if (e.getKey().startsWith(responsible + "|") && now - e.getValue().start() <= window && e.getValue().charged() < 0) {
                return true;
            }
        }
        return false;
    }

    /** 清掉窗口外的条目（写存档前调用）。 */
    public void prune(long now, long window) {
        entries.values().removeIf(e -> now - e.start() > window);
    }

    public Map<String, Entry> view() {
        return Collections.unmodifiableMap(entries);
    }

    public void restore(String key, Entry entry) {
        entries.put(key, entry);
    }

    public void copyFrom(IncidentLedger other) {
        entries.clear();
        entries.putAll(other.entries);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof IncidentLedger l && entries.equals(l.entries);
    }

    @Override
    public int hashCode() {
        return entries.hashCode();
    }
}
