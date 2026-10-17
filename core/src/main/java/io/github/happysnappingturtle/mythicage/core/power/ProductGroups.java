package io.github.happysnappingturtle.mythicage.core.power;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 不属于神话单位的产物（总纲 §3.6）：每名玩家同一种神力的产物同时最多 1 组，再放时旧的一组提前退场。
 * 只做账：返回该退场的旧实体 id。
 */
public final class ProductGroups {
    private record Key(UUID owner, String power) {
    }

    private final Map<Key, List<String>> groups = new HashMap<>();

    /** 登记新的一组，返回被替换掉的旧组。 */
    public synchronized List<String> replace(UUID owner, String power, List<String> ids) {
        List<String> old = groups.put(new Key(owner, power), new ArrayList<>(ids));
        return old == null ? List.of() : old;
    }

    /** 重启后实体载入时补登记到现有组。 */
    public synchronized void restore(UUID owner, String power, String id) {
        groups.computeIfAbsent(new Key(owner, power), k -> new ArrayList<>()).add(id);
    }

    public synchronized void forget(String id) {
        groups.values().forEach(l -> l.remove(id));
        groups.values().removeIf(List::isEmpty);
    }

    public synchronized List<String> group(UUID owner, String power) {
        return List.copyOf(groups.getOrDefault(new Key(owner, power), List.of()));
    }

    public synchronized void clear() {
        groups.clear();
    }
}
