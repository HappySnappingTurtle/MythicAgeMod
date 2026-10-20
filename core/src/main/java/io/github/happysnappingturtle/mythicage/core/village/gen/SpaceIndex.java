package io.github.happysnappingturtle.mythicage.core.village.gen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 「某点在哪个村」的查询（V0 §5.1）：每个村把已建范围覆盖的区块登记进索引，查询时先按区块筛候选，
 * 再精确判包围盒；落在两村重叠处时取中心较近的那座。
 */
public final class SpaceIndex<K> {
    private record Entry<K>(K id, BuiltArea area, Pos center) {
    }

    private final Map<K, Entry<K>> entries = new LinkedHashMap<>();
    private final Map<Long, List<K>> byChunk = new HashMap<>();

    public void put(K id, BuiltArea area, Pos center) {
        remove(id);
        entries.put(id, new Entry<>(id, area, center));
        for (long c : area.chunks()) {
            byChunk.computeIfAbsent(c, k -> new ArrayList<>()).add(id);
        }
    }

    public void remove(K id) {
        Entry<K> old = entries.remove(id);
        if (old != null) {
            for (long c : old.area().chunks()) {
                List<K> l = byChunk.get(c);
                if (l != null) {
                    l.remove(id);
                    if (l.isEmpty()) {
                        byChunk.remove(c);
                    }
                }
            }
        }
    }

    public void clear() {
        entries.clear();
        byChunk.clear();
    }

    public Optional<K> at(int x, int z) {
        List<K> cands = byChunk.get(BuiltArea.chunkKey(x >> 4, z >> 4));
        if (cands == null) {
            return Optional.empty();
        }
        K best = null;
        long bestD = Long.MAX_VALUE;
        for (K id : cands) {
            Entry<K> e = entries.get(id);
            if (e.area().contains(x, z)) {
                long d = e.center().horizontalDistSq(x, z);
                if (d < bestD) {
                    bestD = d;
                    best = id;
                }
            }
        }
        return Optional.ofNullable(best);
    }

    /** 该村已建范围再外扩 extra 格内（离开判定）。 */
    public boolean within(K id, int x, int z, int extra) {
        Entry<K> e = entries.get(id);
        return e != null && e.area().within(x, z, extra);
    }

    public boolean has(K id) {
        return entries.containsKey(id);
    }
}
