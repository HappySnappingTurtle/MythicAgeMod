package io.github.happysnappingturtle.mythicage.core.village.quest;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 一个村庄里每名玩家最近的贡献来源（T24 设计文档 §3.3）：每人最多 {@value #LIMIT} 条，新的挤掉旧的。
 * 只是给「善恶与贡献」界面看的记录；累计贡献仍是任务簿里的总数。
 */
public final class ContributionLog {
    public static final int LIMIT = 8;

    /** @param reason 来源：委托种类（supply、escort、rescue、repair）或 donate */
    public record Event(String reason, int points, long day) {
    }

    private final Map<UUID, Deque<Event>> events = new LinkedHashMap<>();

    /** 0 分不记（不刷屏）。 */
    public void add(UUID player, Event e) {
        if (e.points() <= 0) {
            return;
        }
        Deque<Event> d = events.computeIfAbsent(player, k -> new ArrayDeque<>());
        d.addFirst(e);
        while (d.size() > LIMIT) {
            d.removeLast();
        }
    }

    /** 最近 n 条，新的在前。 */
    public List<Event> recent(UUID player, int n) {
        Deque<Event> d = events.get(player);
        if (d == null || n <= 0) {
            return List.of();
        }
        List<Event> out = new ArrayList<>();
        for (Event e : d) {
            if (out.size() >= n) {
                break;
            }
            out.add(e);
        }
        return List.copyOf(out);
    }

    public Map<UUID, List<Event>> all() {
        Map<UUID, List<Event>> out = new LinkedHashMap<>();
        events.forEach((k, v) -> out.put(k, List.copyOf(v)));
        return out;
    }

    /** 读档用：按新 → 旧的顺序整体放回。 */
    public void restore(UUID player, List<Event> newestFirst) {
        Deque<Event> d = new ArrayDeque<>();
        for (Event e : newestFirst) {
            if (d.size() < LIMIT && e.points() > 0) {
                d.addLast(e);
            }
        }
        if (!d.isEmpty()) {
            events.put(player, d);
        }
    }
}
