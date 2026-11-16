package io.github.happysnappingturtle.mythicage.core.village.map;

import io.github.happysnappingturtle.mythicage.core.village.gen.Box;
import io.github.happysnappingturtle.mythicage.core.village.gen.Outline;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * 村庄地图的数据（GA-12 §4）：服务端按村庄规划生成，<b>不读世界方块</b>，所以不受区块是否加载影响。
 * 坐标都是方块坐标（x、z）；编码时减去村中心，配合之字形变长整数，一个坐标多半只占 1–2 字节。五文明通用：类别与配色在
 * {@link MapPalette}，这里只存建筑 id。
 *
 * @param centerX 村中心（聚会钟）
 * @param border  村界：合并后的轴向线段
 * @param roads   道路折线
 * @param temple  神庙祭坛位置；没有神庙为空
 */
public record VillageMapData(int centerX, int centerZ, List<Segment> border, List<Road> roads, List<Building> buildings,
                             List<Plot> plots, Optional<Point> temple) {
    /** 一张图编码后的上限（§4）。 */
    public static final int MAX_BYTES = 16 * 1024;
    /** 超限时省略的「小型装饰物」：占地小于这么多格、且不在施工中、不是神庙的建筑。 */
    public static final int SMALL_AREA = 16;
    /** 道路只留主干时的最小宽度。 */
    public static final int TRUNK_WIDTH = 3;
    private static final int VERSION = 1;

    public VillageMapData {
        border = List.copyOf(border);
        roads = List.copyOf(roads);
        buildings = List.copyOf(buildings);
        plots = List.copyOf(plots);
    }

    public static final VillageMapData EMPTY = new VillageMapData(0, 0, List.of(), List.of(), List.of(), List.of(), Optional.empty());

    /** 水平矩形，含两端。 */
    public record Rect(int minX, int minZ, int maxX, int maxZ) {
        public Rect {
            if (minX > maxX || minZ > maxZ) {
                throw new IllegalArgumentException("矩形最小角大于最大角");
            }
        }

        public static Rect of(Box b) {
            return new Rect(b.minX(), b.minZ(), b.maxX(), b.maxZ());
        }

        public boolean contains(int x, int z) {
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        }

        public long area() {
            return (long) (maxX - minX + 1) * (maxZ - minZ + 1);
        }

        public Rect union(Rect o) {
            return new Rect(Math.min(minX, o.minX), Math.min(minZ, o.minZ), Math.max(maxX, o.maxX), Math.max(maxZ, o.maxZ));
        }
    }

    public record Point(int x, int z) {
    }

    /** 轴向线段：两端是格线上的点（格 (x, z) 的西北角是点 (x, z)）。 */
    public record Segment(int x1, int z1, int x2, int z2) {
    }

    public record Road(int width, List<Point> points) {
        public Road {
            points = List.copyOf(points);
        }
    }

    /** 地图上能点选的东西：建筑或空地块。 */
    public sealed interface Feature permits Building, Plot {
        Rect box();
    }

    /**
     * 一座建筑（或一个在建的新项目）。
     *
     * @param building 建筑 id（新建项目为要盖的建筑）
     * @param god      祠的次神；没有为空串
     * @param tier     等级（1 起）；没有等级链的为 0
     * @param progress 施工进度百分比；不在施工为 −1
     * @param target   升级中：升级成的建筑 id；否则为空串
     */
    public record Building(String building, String god, int tier, Rect box, int progress, String target) implements Feature {
        /** 在一张图里唯一、跨刷新不变：建筑 id + 西北角。新项目落成后键不变，只是进度变回 −1。 */
        public String key() {
            return building + "@" + box.minX() + "," + box.minZ();
        }

        public boolean underConstruction() {
            return progress >= 0;
        }
    }

    /** 空地块：kind 是地块类别（market、shrine……）。 */
    public record Plot(String id, String kind, Rect box) implements Feature {
    }

    /** 全部内容的外包围（地图初次打开时按它定缩放）；什么都没有时是村中心一格。 */
    public Rect extent() {
        Rect r = new Rect(centerX, centerZ, centerX, centerZ);
        for (Segment s : border) {
            r = r.union(new Rect(Math.min(s.x1, s.x2), Math.min(s.z1, s.z2), Math.max(s.x1, s.x2), Math.max(s.z1, s.z2)));
        }
        for (Building b : buildings) {
            r = r.union(b.box);
        }
        for (Plot p : plots) {
            r = r.union(p.box);
        }
        return r;
    }

    public Optional<Building> building(String key) {
        return buildings.stream().filter(b -> b.key().equals(key)).findFirst();
    }

    // ───────────── 生成辅助 ─────────────

    /** 把村界的单位边合并成线段：同一行（列）上首尾相接的单位边连成一段。 */
    public static List<Segment> segments(List<Outline.Edge> edges) {
        Map<Integer, List<Integer>> rows = new TreeMap<>();
        Map<Integer, List<Integer>> cols = new TreeMap<>();
        for (Outline.Edge e : edges) {
            if (e.alongX()) {
                rows.computeIfAbsent(e.z(), k -> new ArrayList<>()).add(e.x());
            } else {
                cols.computeIfAbsent(e.x(), k -> new ArrayList<>()).add(e.z());
            }
        }
        List<Segment> out = new ArrayList<>();
        rows.forEach((z, xs) -> runs(xs, (a, b) -> out.add(new Segment(a, z, b, z))));
        cols.forEach((x, zs) -> runs(zs, (a, b) -> out.add(new Segment(x, a, x, b))));
        return out;
    }

    private interface RunSink {
        void run(int from, int to);
    }

    private static void runs(List<Integer> starts, RunSink sink) {
        List<Integer> s = new ArrayList<>(new java.util.TreeSet<>(starts));
        int i = 0;
        while (i < s.size()) {
            int a = s.get(i);
            int b = a + 1;
            i++;
            while (i < s.size() && s.get(i) == b) {
                b++;
                i++;
            }
            sink.run(a, b);
        }
    }

    /** 去掉折线上的重复点与共线的中间点（前后同向；折返的点保留）。 */
    public static Road simplify(Road r) {
        List<Point> p = r.points();
        if (p.size() <= 2) {
            return r;
        }
        List<Point> out = new ArrayList<>();
        out.add(p.get(0));
        for (int i = 1; i < p.size() - 1; i++) {
            Point a = out.get(out.size() - 1);
            Point b = p.get(i);
            Point c = p.get(i + 1);
            if (b.equals(a)) {
                continue;
            }
            long cross = (long) (b.x - a.x) * (c.z - b.z) - (long) (b.z - a.z) * (c.x - b.x);
            long dot = (long) (b.x - a.x) * (c.x - b.x) + (long) (b.z - a.z) * (c.z - b.z);
            if (cross != 0 || dot <= 0) {
                out.add(b);
            }
        }
        Point last = p.get(p.size() - 1);
        if (!last.equals(out.get(out.size() - 1)) || out.size() == 1) {
            out.add(last);
        }
        return new Road(r.width(), out);
    }

    // ───────────── 大小控制 ─────────────

    /**
     * 编码后不超过 limit 字节的版本（§4）：依次「道路去共线点」→「省略小型装饰物」→「道路只留主干」→「去掉空地块」→
     * 「村界只画外包围」→「从小到大去掉建筑」，每步之后量一次，够了就停。施工中的建筑与神庙最后才动。
     */
    public VillageMapData fit(int limit) {
        if (encode().length <= limit) {
            return this;
        }
        VillageMapData d = new VillageMapData(centerX, centerZ, border, roads.stream().map(VillageMapData::simplify).toList(),
                buildings, plots, temple);
        if (d.encode().length <= limit) {
            return d;
        }
        d = new VillageMapData(centerX, centerZ, border, d.roads, d.buildings.stream()
                .filter(b -> b.underConstruction() || b.box.area() >= SMALL_AREA).toList(), plots, temple);
        if (d.encode().length <= limit) {
            return d;
        }
        d = new VillageMapData(centerX, centerZ, border, d.roads.stream().filter(r -> r.width() >= TRUNK_WIDTH).toList(),
                d.buildings, plots, temple);
        if (d.encode().length <= limit) {
            return d;
        }
        d = new VillageMapData(centerX, centerZ, border, d.roads, d.buildings, List.of(), temple);
        if (d.encode().length <= limit) {
            return d;
        }
        List<Segment> hull = hull(border);
        d = new VillageMapData(centerX, centerZ, hull, d.roads, d.buildings, List.of(), temple);
        List<Building> keep = new ArrayList<>(d.buildings);
        keep.sort(Comparator.comparing((Building b) -> b.underConstruction()).thenComparingLong(b -> b.box.area()));
        while (!keep.isEmpty() && d.encode().length > limit) {
            keep.remove(0);
            List<Building> k = keep;
            d = new VillageMapData(centerX, centerZ, hull, d.roads,
                    buildings.stream().filter(k::contains).toList(), List.of(), temple);
        }
        if (d.encode().length > limit) {
            d = new VillageMapData(centerX, centerZ, List.of(), List.of(), List.of(), List.of(), temple);
        }
        return d;
    }

    /** 村界线段的外包围矩形（四条边）。 */
    static List<Segment> hull(List<Segment> border) {
        if (border.isEmpty()) {
            return border;
        }
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (Segment s : border) {
            minX = Math.min(minX, Math.min(s.x1, s.x2));
            maxX = Math.max(maxX, Math.max(s.x1, s.x2));
            minZ = Math.min(minZ, Math.min(s.z1, s.z2));
            maxZ = Math.max(maxZ, Math.max(s.z1, s.z2));
        }
        return List.of(new Segment(minX, minZ, maxX, minZ), new Segment(minX, maxZ, maxX, maxZ),
                new Segment(minX, minZ, minX, maxZ), new Segment(maxX, minZ, maxX, maxZ));
    }

    // ───────────── 编码 ─────────────

    public byte[] encode() {
        MapBytes.Writer w = new MapBytes.Writer();
        w.varInt(VERSION);
        w.sInt(centerX);
        w.sInt(centerZ);
        Strings strings = Strings.of(buildings, plots);
        strings.write(w);
        writeBody(w, strings);
        return w.bytes();
    }

    public static VillageMapData decode(byte[] bytes) {
        MapBytes.Reader r = new MapBytes.Reader(bytes);
        int version = r.varInt();
        if (version != VERSION) {
            throw new IllegalArgumentException("村庄地图数据版本不对：" + version);
        }
        int cx = r.sInt();
        int cz = r.sInt();
        List<String> strings = Strings.read(r);
        return readBody(r, cx, cz, strings);
    }

    private void writeBody(MapBytes.Writer w, Strings strings) {
        writeSegments(w, border, centerX, centerZ);
        writeRoads(w, roads, centerX, centerZ);
        w.varInt(buildings.size());
        buildings.forEach(b -> writeBuilding(w, b, strings, centerX, centerZ));
        writePlots(w, plots, strings, centerX, centerZ);
        writeTemple(w, temple, centerX, centerZ);
    }

    private static VillageMapData readBody(MapBytes.Reader r, int cx, int cz, List<String> strings) {
        List<Segment> border = readSegments(r, cx, cz);
        List<Road> roads = readRoads(r, cx, cz);
        int n = r.varInt();
        List<Building> buildings = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            buildings.add(readBuilding(r, strings, cx, cz));
        }
        List<Plot> plots = readPlots(r, strings, cx, cz);
        return new VillageMapData(cx, cz, border, roads, buildings, plots, readTemple(r, cx, cz));
    }

    /** 重复的字符串（建筑 id、地块类别）只写一次，用下标引用。 */
    record Strings(List<String> list, Map<String, Integer> index) {
        static Strings of(List<Building> buildings, List<Plot> plots) {
            Map<String, Integer> idx = new LinkedHashMap<>();
            for (Building b : buildings) {
                idx.putIfAbsent(b.building, idx.size());
                idx.putIfAbsent(b.god, idx.size());
                idx.putIfAbsent(b.target, idx.size());
            }
            for (Plot p : plots) {
                idx.putIfAbsent(p.kind, idx.size());
            }
            return new Strings(List.copyOf(idx.keySet()), idx);
        }

        void write(MapBytes.Writer w) {
            w.varInt(list.size());
            list.forEach(w::str);
        }

        static List<String> read(MapBytes.Reader r) {
            int n = r.varInt();
            List<String> out = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                out.add(r.str());
            }
            return out;
        }

        int of(String s) {
            return index.get(s);
        }
    }

    static String str(List<String> strings, int i) {
        if (i < 0 || i >= strings.size()) {
            throw new IllegalArgumentException("字符串下标越界：" + i);
        }
        return strings.get(i);
    }

    static void writeRect(MapBytes.Writer w, Rect r, int cx, int cz) {
        w.sInt(r.minX - cx);
        w.sInt(r.minZ - cz);
        w.varInt(r.maxX - r.minX);
        w.varInt(r.maxZ - r.minZ);
    }

    static Rect readRect(MapBytes.Reader r, int cx, int cz) {
        int x = r.sInt() + cx;
        int z = r.sInt() + cz;
        return new Rect(x, z, x + r.varInt(), z + r.varInt());
    }

    static void writeSegments(MapBytes.Writer w, List<Segment> segs, int cx, int cz) {
        w.varInt(segs.size());
        for (Segment s : segs) {
            w.sInt(s.x1 - cx);
            w.sInt(s.z1 - cz);
            w.sInt(s.x2 - s.x1);
            w.sInt(s.z2 - s.z1);
        }
    }

    static List<Segment> readSegments(MapBytes.Reader r, int cx, int cz) {
        int n = r.varInt();
        List<Segment> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            int x = r.sInt() + cx;
            int z = r.sInt() + cz;
            out.add(new Segment(x, z, x + r.sInt(), z + r.sInt()));
        }
        return out;
    }

    static void writeRoads(MapBytes.Writer w, List<Road> roads, int cx, int cz) {
        w.varInt(roads.size());
        for (Road road : roads) {
            w.varInt(road.width());
            w.varInt(road.points().size());
            int px = cx;
            int pz = cz;
            for (Point p : road.points()) {
                w.sInt(p.x - px);
                w.sInt(p.z - pz);
                px = p.x;
                pz = p.z;
            }
        }
    }

    static List<Road> readRoads(MapBytes.Reader r, int cx, int cz) {
        int n = r.varInt();
        List<Road> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            int width = r.varInt();
            int m = r.varInt();
            List<Point> pts = new ArrayList<>(m);
            int px = cx;
            int pz = cz;
            for (int j = 0; j < m; j++) {
                px += r.sInt();
                pz += r.sInt();
                pts.add(new Point(px, pz));
            }
            out.add(new Road(width, pts));
        }
        return out;
    }

    static void writeBuilding(MapBytes.Writer w, Building b, Strings strings, int cx, int cz) {
        w.varInt(strings.of(b.building));
        w.varInt(strings.of(b.god));
        w.varInt(b.tier);
        writeRect(w, b.box, cx, cz);
        w.varInt(b.progress + 1);
        w.varInt(strings.of(b.target));
    }

    static Building readBuilding(MapBytes.Reader r, List<String> strings, int cx, int cz) {
        String building = str(strings, r.varInt());
        String god = str(strings, r.varInt());
        int tier = r.varInt();
        Rect box = readRect(r, cx, cz);
        int progress = r.varInt() - 1;
        return new Building(building, god, tier, box, progress, str(strings, r.varInt()));
    }

    static void writePlots(MapBytes.Writer w, List<Plot> plots, Strings strings, int cx, int cz) {
        w.varInt(plots.size());
        for (Plot p : plots) {
            w.str(p.id);
            w.varInt(strings.of(p.kind));
            writeRect(w, p.box, cx, cz);
        }
    }

    static List<Plot> readPlots(MapBytes.Reader r, List<String> strings, int cx, int cz) {
        int n = r.varInt();
        List<Plot> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            String id = r.str();
            String kind = str(strings, r.varInt());
            out.add(new Plot(id, kind, readRect(r, cx, cz)));
        }
        return out;
    }

    static void writeTemple(MapBytes.Writer w, Optional<Point> temple, int cx, int cz) {
        w.bool(temple.isPresent());
        temple.ifPresent(t -> {
            w.sInt(t.x - cx);
            w.sInt(t.z - cz);
        });
    }

    static Optional<Point> readTemple(MapBytes.Reader r, int cx, int cz) {
        return r.bool() ? Optional.of(new Point(r.sInt() + cx, r.sInt() + cz)) : Optional.empty();
    }
}
