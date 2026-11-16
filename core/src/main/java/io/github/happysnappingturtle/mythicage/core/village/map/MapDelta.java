package io.github.happysnappingturtle.mythicage.core.village.map;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 地图增量（GA-12 §4）：界面开着时每 20 刻比一次。建筑按 {@link VillageMapData.Building#key()} 增、删、改；
 * 村界、道路、空地块、神庙变了就整段重发（它们很少变，变的时候往往整段都变）。村中心变了不发增量，改发全量。
 *
 * @param changed 新增或改变了的建筑
 * @param removed 去掉的建筑键
 * @param border  村界变了时的新村界；没变为空
 */
public record MapDelta(List<VillageMapData.Building> changed, List<String> removed,
                       Optional<List<VillageMapData.Segment>> border, Optional<List<VillageMapData.Road>> roads,
                       Optional<List<VillageMapData.Plot>> plots, Optional<Optional<VillageMapData.Point>> temple) {
    public MapDelta {
        changed = List.copyOf(changed);
        removed = List.copyOf(removed);
    }

    public boolean isEmpty() {
        return changed.isEmpty() && removed.isEmpty() && border.isEmpty() && roads.isEmpty() && plots.isEmpty()
                && temple.isEmpty();
    }

    /** 从 old 到 now 的增量；村中心不同返回空（调用方改发全量）。 */
    public static Optional<MapDelta> diff(VillageMapData old, VillageMapData now) {
        if (old.centerX() != now.centerX() || old.centerZ() != now.centerZ()) {
            return Optional.empty();
        }
        Map<String, VillageMapData.Building> before = new LinkedHashMap<>();
        old.buildings().forEach(b -> before.put(b.key(), b));
        List<VillageMapData.Building> changed = new ArrayList<>();
        for (VillageMapData.Building b : now.buildings()) {
            if (!b.equals(before.get(b.key()))) {
                changed.add(b);
            }
        }
        Set<String> after = now.buildings().stream().map(VillageMapData.Building::key).collect(Collectors.toSet());
        List<String> removed = before.keySet().stream().filter(k -> !after.contains(k)).toList();
        return Optional.of(new MapDelta(changed, removed,
                old.border().equals(now.border()) ? Optional.empty() : Optional.of(now.border()),
                old.roads().equals(now.roads()) ? Optional.empty() : Optional.of(now.roads()),
                old.plots().equals(now.plots()) ? Optional.empty() : Optional.of(now.plots()),
                old.temple().equals(now.temple()) ? Optional.empty() : Optional.of(now.temple())));
    }

    /** 把增量用到 old 上：改了的建筑留在原位，新增的接在后面。 */
    public VillageMapData apply(VillageMapData old) {
        Map<String, VillageMapData.Building> update = new LinkedHashMap<>();
        changed.forEach(b -> update.put(b.key(), b));
        Set<String> gone = Set.copyOf(removed);
        List<VillageMapData.Building> out = new ArrayList<>();
        for (VillageMapData.Building b : old.buildings()) {
            if (gone.contains(b.key())) {
                continue;
            }
            VillageMapData.Building u = update.remove(b.key());
            out.add(u == null ? b : u);
        }
        out.addAll(update.values());
        return new VillageMapData(old.centerX(), old.centerZ(), border.orElse(old.border()), roads.orElse(old.roads()), out,
                plots.orElse(old.plots()), temple.orElse(old.temple()));
    }

    // ───────────── 编码 ─────────────

    public byte[] encode(int centerX, int centerZ) {
        MapBytes.Writer w = new MapBytes.Writer();
        VillageMapData.Strings strings = VillageMapData.Strings.of(changed, plots.orElse(List.of()));
        strings.write(w);
        w.varInt(changed.size());
        changed.forEach(b -> VillageMapData.writeBuilding(w, b, strings, centerX, centerZ));
        w.varInt(removed.size());
        removed.forEach(w::str);
        w.bool(border.isPresent());
        border.ifPresent(b -> VillageMapData.writeSegments(w, b, centerX, centerZ));
        w.bool(roads.isPresent());
        roads.ifPresent(r -> VillageMapData.writeRoads(w, r, centerX, centerZ));
        w.bool(plots.isPresent());
        plots.ifPresent(p -> VillageMapData.writePlots(w, p, strings, centerX, centerZ));
        w.bool(temple.isPresent());
        temple.ifPresent(t -> VillageMapData.writeTemple(w, t, centerX, centerZ));
        return w.bytes();
    }

    public static MapDelta decode(byte[] bytes, int centerX, int centerZ) {
        MapBytes.Reader r = new MapBytes.Reader(bytes);
        List<String> strings = VillageMapData.Strings.read(r);
        int n = r.varInt();
        List<VillageMapData.Building> changed = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            changed.add(VillageMapData.readBuilding(r, strings, centerX, centerZ));
        }
        int m = r.varInt();
        List<String> removed = new ArrayList<>(m);
        for (int i = 0; i < m; i++) {
            removed.add(r.str());
        }
        Optional<List<VillageMapData.Segment>> border = r.bool()
                ? Optional.of(VillageMapData.readSegments(r, centerX, centerZ)) : Optional.empty();
        Optional<List<VillageMapData.Road>> roads = r.bool()
                ? Optional.of(VillageMapData.readRoads(r, centerX, centerZ)) : Optional.empty();
        Optional<List<VillageMapData.Plot>> plots = r.bool()
                ? Optional.of(VillageMapData.readPlots(r, strings, centerX, centerZ)) : Optional.empty();
        Optional<Optional<VillageMapData.Point>> temple = r.bool()
                ? Optional.of(VillageMapData.readTemple(r, centerX, centerZ)) : Optional.empty();
        return new MapDelta(changed, removed, border, roads, plots, temple);
    }
}
