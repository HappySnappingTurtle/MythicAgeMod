package io.github.happysnappingturtle.mythicage.core.village.map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.happysnappingturtle.mythicage.core.village.gen.Box;
import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.core.village.gen.VillagePlan;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** GA-12 村庄地图数据（TC-GA12-01～07）。 */
class VillageMapDataTest {
    private static final Pos CENTER = new Pos(-1500, 70, -2160);

    private static Box box(int x, int z, int w, int d) {
        return new Box(x, 70, z, x + w - 1, 75, z + d - 1);
    }

    /** 一个像样的测试村：卫城、4 座小屋、谷仓、陶坊、橄榄园，两条路，两个空地块。 */
    private static VillageMapBuilder.Input plan(List<VillageMapBuilder.Project> projects) {
        List<VillageMapBuilder.Placed> b = List.of(
                new VillageMapBuilder.Placed("greek:acropolis_archaic", "", 1, box(-1510, -2190, 20, 24)),
                new VillageMapBuilder.Placed("greek:house_small", "", 1, box(-1530, -2160, 9, 10)),
                new VillageMapBuilder.Placed("greek:house_small", "", 1, box(-1518, -2160, 9, 10)),
                new VillageMapBuilder.Placed("greek:house_small", "", 1, box(-1530, -2146, 9, 10)),
                new VillageMapBuilder.Placed("greek:house_small", "", 1, box(-1518, -2146, 9, 10)),
                new VillageMapBuilder.Placed("greek:granary", "", 1, box(-1490, -2160, 11, 9)),
                new VillageMapBuilder.Placed("greek:pottery", "", 0, box(-1490, -2145, 11, 9)),
                new VillageMapBuilder.Placed("greek:olive_grove", "", 0, box(-1470, -2190, 16, 16)));
        List<Box> zone = b.stream().map(p -> p.box().inflateXZ(4)).toList();
        List<VillagePlan.Path> paths = List.of(
                new VillagePlan.Path(3, List.of(new Pos(-1535, 70, -2163), new Pos(-1500, 70, -2163), new Pos(-1465, 70, -2163))),
                new VillagePlan.Path(2, List.of(new Pos(-1500, 70, -2163), new Pos(-1500, 70, -2140))));
        List<VillagePlan.Plot> plots = List.of(
                new VillagePlan.Plot("classical_market_1", "market", "classical", box(-1470, -2150, 15, 15), "north"),
                new VillagePlan.Plot("classical_shrine_1", "shrine", "classical", box(-1540, -2190, 11, 11), "east"));
        return new VillageMapBuilder.Input(CENTER, zone, paths, b, List.of(box(-1450, -2140, 5, 5)), projects, plots,
                Optional.of(new Pos(-1500, 72, -2180)));
    }

    @Test
    void tc01BuildFromPlan() {
        VillageMapData d = VillageMapBuilder.build(plan(List.of()));
        assertFalse(d.border().isEmpty(), "村界");
        assertEquals(2, d.roads().size(), "道路");
        assertEquals(3, d.roads().get(0).width());
        assertEquals(9, d.buildings().size(), "8 座认得出的 + 1 座认不出的");
        VillageMapData.Building acro = d.buildings().get(0);
        assertEquals("greek:acropolis_archaic", acro.building());
        assertEquals(1, acro.tier());
        assertEquals(new VillageMapData.Rect(-1510, -2190, -1491, -2167), acro.box());
        assertFalse(acro.underConstruction());
        assertEquals("", d.buildings().get(8).building(), "认不出的建筑 id 为空");
        assertEquals(2, d.plots().size());
        assertEquals("market", d.plots().get(0).kind());
        assertEquals(Optional.of(new VillageMapData.Point(-1500, -2180)), d.temple());
        // 村界线段都是轴向的
        d.border().forEach(s -> assertTrue(s.x1() == s.x2() || s.z1() == s.z2()));
        // 编码往返不变
        assertEquals(d, VillageMapData.decode(d.encode()));
    }

    @Test
    void tc02UpgradeInProgress() {
        Box old = box(-1490, -2160, 11, 9);
        Box grown = new Box(-1490, 70, -2160, -1480, 82, -2152);
        VillageMapBuilder.Project up = new VillageMapBuilder.Project("greek:granary_ii", "", 2, grown, 60, Optional.of(old));
        VillageMapBuilder.Project fresh = new VillageMapBuilder.Project("greek:shrine_athena", "athena", 0,
                box(-1540, -2190, 11, 11), 12, Optional.empty());
        VillageMapData d = VillageMapBuilder.build(plan(List.of(up, fresh)));
        VillageMapData.Building g = d.buildings().stream().filter(b -> b.building().equals("greek:granary")).findFirst().orElseThrow();
        assertEquals(60, g.progress());
        assertEquals("greek:granary_ii", g.target());
        assertTrue(g.underConstruction());
        VillageMapData.Building s = d.buildings().stream().filter(b -> b.building().equals("greek:shrine_athena")).findFirst()
                .orElseThrow();
        assertEquals(12, s.progress());
        assertEquals("athena", s.god());
        assertEquals("", s.target());
        assertEquals(1, d.buildings().stream().filter(b -> b.building().startsWith("greek:granary")).count(), "升级不多画一座");
        assertEquals(d, VillageMapData.decode(d.encode()));
    }

    /** 30 人村庄：约 25 座建筑、6 条路、8 个空地块。 */
    static VillageMapData thirtyResidents() {
        Random r = new Random(30);
        List<VillageMapBuilder.Placed> b = new ArrayList<>();
        String[] kinds = {"greek:house_small", "greek:house_court", "greek:granary_ii", "greek:pottery", "greek:forge",
                "greek:wheat_field", "greek:vineyard", "greek:shrine_athena", "greek:stoa", "greek:krene"};
        for (int i = 0; i < 25; i++) {
            int x = CENTER.x() - 60 + (i % 5) * 24;
            int z = CENTER.z() - 60 + (i / 5) * 24;
            b.add(new VillageMapBuilder.Placed(kinds[i % kinds.length], i % 7 == 0 ? "athena" : "", 1 + r.nextInt(2),
                    box(x, z, 9 + r.nextInt(8), 9 + r.nextInt(8))));
        }
        b.add(new VillageMapBuilder.Placed("greek:acropolis_heroic_zeus", "", 3, box(CENTER.x() - 14, CENTER.z() - 90, 29, 26)));
        List<VillagePlan.Path> paths = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            List<Pos> pts = new ArrayList<>();
            for (int j = 0; j < 12; j++) {
                pts.add(new Pos(CENTER.x() - 70 + j * 12, 70, CENTER.z() - 60 + i * 24 + r.nextInt(3)));
            }
            paths.add(new VillagePlan.Path(i == 0 ? 3 : 2, pts));
        }
        List<VillagePlan.Plot> plots = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            plots.add(new VillagePlan.Plot("heroic_plot_" + i, i % 2 == 0 ? "house" : "workshop", "heroic",
                    box(CENTER.x() + 70 + (i % 2) * 16, CENTER.z() - 60 + (i / 2) * 18, 13, 13), "west"));
        }
        List<Box> zone = b.stream().map(p -> p.box().inflateXZ(6)).toList();
        return VillageMapBuilder.build(new VillageMapBuilder.Input(CENTER, zone, paths, b, List.of(), List.of(), plots,
                Optional.of(new Pos(CENTER.x(), 72, CENTER.z() - 78))));
    }

    @Test
    void tc03ThirtyResidentVillageUnderLimit() {
        VillageMapData d = thirtyResidents();
        int size = d.encode().length;
        assertTrue(size <= VillageMapData.MAX_BYTES, "30 人村庄 " + size + " 字节");
        assertTrue(size < 4096, "实际应远小于上限：" + size);
        assertEquals(d, d.fit(VillageMapData.MAX_BYTES), "没超限时原样返回");
    }

    @Test
    void tc04HugeVillageFitsAfterTrimming() {
        Random r = new Random(4);
        List<VillageMapBuilder.Placed> b = new ArrayList<>();
        for (int i = 0; i < 1500; i++) {
            int w = i % 3 == 0 ? 3 : 9 + r.nextInt(10);
            b.add(new VillageMapBuilder.Placed("greek:custom_building_with_long_id_" + (i % 40), "", 1,
                    box(CENTER.x() - 600 + (i % 40) * 30, CENTER.z() - 600 + (i / 40) * 30, w, w)));
        }
        List<VillagePlan.Path> paths = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            List<Pos> pts = new ArrayList<>();
            for (int j = 0; j < 200; j++) {
                // 大部分是共线的中间点
                pts.add(new Pos(CENTER.x() - 600 + j * 6, 70, CENTER.z() - 600 + i * 20 + (j % 50 == 0 ? 1 : 0)));
            }
            paths.add(new VillagePlan.Path(i % 4 == 0 ? 3 : 2, pts));
        }
        List<Box> zone = b.stream().map(p -> p.box().inflateXZ(3)).toList();
        VillageMapData big = VillageMapBuilder.build(new VillageMapBuilder.Input(CENTER, zone, paths, b, List.of(), List.of(
                new VillageMapBuilder.Project("greek:stoa", "", 0, box(CENTER.x() + 700, CENTER.z(), 23, 9), 40, Optional.empty())),
                List.of(), Optional.of(new Pos(CENTER.x(), 70, CENTER.z()))));
        assertTrue(big.encode().length > VillageMapData.MAX_BYTES, "构造的村子要先超限");
        VillageMapData fit = big.fit(VillageMapData.MAX_BYTES);
        assertTrue(fit.encode().length <= VillageMapData.MAX_BYTES, "压缩后 " + fit.encode().length);
        assertTrue(fit.buildings().stream().anyMatch(VillageMapData.Building::underConstruction), "在建的不省略");
        assertTrue(fit.buildings().stream().noneMatch(x -> x.box().area() < VillageMapData.SMALL_AREA), "小物件省略了");
        assertTrue(fit.temple().isPresent());
        // 道路去共线点
        VillageMapData.Road straight = VillageMapData.simplify(new VillageMapData.Road(2, List.of(new VillageMapData.Point(0, 0),
                new VillageMapData.Point(5, 0), new VillageMapData.Point(10, 0), new VillageMapData.Point(10, 7))));
        assertEquals(List.of(new VillageMapData.Point(0, 0), new VillageMapData.Point(10, 0), new VillageMapData.Point(10, 7)),
                straight.points());
        VillageMapData.Road back = VillageMapData.simplify(new VillageMapData.Road(2, List.of(new VillageMapData.Point(0, 0),
                new VillageMapData.Point(10, 0), new VillageMapData.Point(4, 0))));
        assertEquals(3, back.points().size(), "折返点保留");
    }

    @Test
    void tc05DeltaOnlyCarriesChangedProgress() {
        Box old = box(-1490, -2160, 11, 9);
        Box grown = new Box(-1490, 70, -2160, -1480, 82, -2152);
        VillageMapData a = VillageMapBuilder.build(plan(List.of(
                new VillageMapBuilder.Project("greek:granary_ii", "", 2, grown, 60, Optional.of(old)))));
        VillageMapData b = VillageMapBuilder.build(plan(List.of(
                new VillageMapBuilder.Project("greek:granary_ii", "", 2, grown, 62, Optional.of(old)))));
        MapDelta delta = MapDelta.diff(a, b).orElseThrow();
        assertEquals(1, delta.changed().size(), "只发这一项");
        assertEquals(62, delta.changed().get(0).progress());
        assertTrue(delta.removed().isEmpty());
        assertTrue(delta.border().isEmpty() && delta.roads().isEmpty() && delta.plots().isEmpty() && delta.temple().isEmpty());
        assertTrue(delta.encode(a.centerX(), a.centerZ()).length < 64, "增量很小");
        MapDelta round = MapDelta.decode(delta.encode(a.centerX(), a.centerZ()), a.centerX(), a.centerZ());
        assertEquals(delta, round);
        assertEquals(b, round.apply(a));
        assertTrue(MapDelta.diff(b, b).orElseThrow().isEmpty(), "没变就是空增量");
        // 落成：旧谷仓去掉、新谷仓加上、地块不变
        VillageMapBuilder.Input done = plan(List.of());
        List<VillageMapBuilder.Placed> after = new ArrayList<>(done.buildings());
        after.set(5, new VillageMapBuilder.Placed("greek:granary_ii", "", 2, grown));
        VillageMapData c = VillageMapBuilder.build(new VillageMapBuilder.Input(done.center(), done.zone(), done.paths(), after,
                done.unknown(), List.of(), done.freePlots(), done.temple()));
        MapDelta d2 = MapDelta.diff(b, c).orElseThrow();
        assertEquals(List.of("greek:granary@-1490,-2160"), d2.removed());
        assertEquals(1, d2.changed().size());
        assertEquals(c.buildings().stream().map(VillageMapData.Building::key).sorted().toList(),
                d2.apply(b).buildings().stream().map(VillageMapData.Building::key).sorted().toList());
        // 村中心变了：不发增量
        VillageMapData moved = new VillageMapData(c.centerX() + 1, c.centerZ(), c.border(), c.roads(), c.buildings(), c.plots(),
                c.temple());
        assertTrue(MapDelta.diff(c, moved).isEmpty());
    }

    @Test
    void tc06ScreenToBlockAndPick() {
        VillageMapData d = VillageMapBuilder.build(plan(List.of()));
        for (double scale : new double[]{0.5, 4}) {
            MapView v = new MapView(100, 50, 240, 180, -1500, -2160, scale);
            assertEquals(scale, v.scale());
            // 视口中心对应视口中心方块
            assertEquals(-1500, v.blockX(100 + 120), 1e-9);
            assertEquals(-2160, v.blockZ(50 + 90), 1e-9);
            // 往返
            assertEquals(-1517.25, v.blockX(v.screenX(-1517.25)), 1e-9);
            // 平移后再换算
            v.pan(40, -20);
            assertEquals(-1500 - 40 / scale, v.blockX(220), 1e-9);
            assertEquals(-2160 + 20 / scale, v.blockZ(140), 1e-9);
        }
        // 缩放夹在 0.5–4
        assertEquals(MapView.MAX_SCALE, new MapView(0, 0, 10, 10, 0, 0, 99).scale());
        assertEquals(MapView.MIN_SCALE, new MapView(0, 0, 10, 10, 0, 0, 0.01).scale());
        // 以鼠标为不动点缩放
        MapView v = new MapView(0, 0, 200, 200, -1500, -2160, 1);
        double bx = v.blockX(30);
        double bz = v.blockZ(170);
        v.zoom(3, 30, 170);
        assertEquals(bx, v.blockX(30), 1e-9);
        assertEquals(bz, v.blockZ(170), 1e-9);
        assertTrue(v.scale() > 1.9);
        // 点到谷仓（-1490..-1480, -2160..-2152）
        MapView p = new MapView(0, 0, 400, 400, -1500, -2160, 4);
        double sx = p.screenX(-1485 + 0.5);
        double sy = p.screenY(-2156 + 0.5);
        assertEquals("greek:granary", ((VillageMapData.Building) p.pick(d, sx, sy).orElseThrow()).building());
        // 格子边界：x = -1491 是谷仓西边外一格
        assertTrue(p.pick(d, p.screenX(-1491 + 0.99), sy).isEmpty());
        assertTrue(p.pick(d, p.screenX(-1490 + 0.01), sy).isPresent());
        // 大小盒重叠时取小的
        VillageMapData overlap = new VillageMapData(0, 0, List.of(), List.of(), List.of(
                new VillageMapData.Building("greek:acropolis_classical", "", 2, new VillageMapData.Rect(0, 0, 30, 30), -1, ""),
                new VillageMapData.Building("greek:shrine_athena", "athena", 0, new VillageMapData.Rect(5, 5, 9, 9), -1, "")),
                List.of(), Optional.empty());
        MapView o = new MapView(0, 0, 100, 100, 15, 15, 2);
        assertEquals("greek:shrine_athena",
                ((VillageMapData.Building) o.pick(overlap, o.screenX(7), o.screenY(7)).orElseThrow()).building());
        // 空地块
        assertTrue(p.pick(d, p.screenX(-1465), p.screenY(-2145)).orElseThrow() instanceof VillageMapData.Plot);
        // 视口外不命中
        assertTrue(p.pick(d, -5, 10).isEmpty());
        // 适配：整个村子放得下
        MapView fit = MapView.fit(0, 0, 260, 200, d.extent());
        VillageMapData.Rect e = d.extent();
        assertTrue(fit.screenX(e.minX()) >= 0 && fit.screenX(e.maxX() + 1) <= 260);
        assertTrue(fit.screenY(e.minZ()) >= 0 && fit.screenY(e.maxZ() + 1) <= 200);
    }

    @Test
    void tc07Palette() throws IOException {
        Path root = Path.of(System.getProperty("mythicage.rootDir", ".."));
        Path file = root.resolve("neoforge/src/main/resources/assets/mythic_age/village_map/greek.json");
        List<String> warnings = new ArrayList<>();
        MapPalette p = MapPalette.parse(JsonParser.parseString(Files.readString(file)).getAsJsonObject(), warnings::add);
        assertTrue(warnings.isEmpty(), "希腊配色表完整：" + warnings);
        MapPalette.REQUIRED.forEach(c -> assertTrue(p.categories().containsKey(c), c));
        assertEquals("temple", p.categoryOf("greek:acropolis_heroic_zeus"));
        assertEquals("dwelling", p.categoryOf("greek:house_small_ii"));
        assertEquals("storage", p.categoryOf("greek:granary_ii"));
        assertEquals("shrine", p.categoryOf("greek:shrine_athena"));
        assertEquals(MapPalette.FALLBACK, p.categoryOf("egypt:unknown"));
        assertEquals(0xFFD4A72C, p.style("temple").fill());
        // 符号互不相同（不只靠颜色）
        assertEquals(MapPalette.REQUIRED.size(), MapPalette.REQUIRED.stream().map(c -> p.style(c).symbol()).distinct().count());
        // 空地块按种类归类（地图上画淡色符号，不写字）：希腊的十种都有；没登记的为空
        for (String kind : List.of("temple_ext", "house", "workshop", "storage", "shrine", "altar", "market", "theater", "training",
                "feature")) {
            assertTrue(p.plotCategory(kind).filter(MapPalette.REQUIRED::contains).isPresent(), "空地块 " + kind);
        }
        assertEquals("shrine", p.plotCategory("shrine").orElseThrow());
        assertTrue(p.plotCategory("egypt_unknown").isEmpty());
        // 缺一个类别：默认色 + 警告
        JsonObject o = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        o.getAsJsonObject("categories").remove("farm");
        List<String> w2 = new ArrayList<>();
        MapPalette q = MapPalette.parse(o, w2::add);
        assertEquals(MapPalette.DEFAULT, q.style("farm"));
        assertEquals(MapPalette.DEFAULT, q.styleOf("greek:wheat_field"));
        assertTrue(w2.stream().anyMatch(s -> s.contains("farm")), "给警告：" + w2);
        // 写错的颜色
        List<String> w3 = new ArrayList<>();
        MapPalette.parse(JsonParser.parseString("{\"categories\":{\"temple\":{\"fill\":\"gold\"}}}").getAsJsonObject(), w3::add);
        assertTrue(w3.stream().anyMatch(s -> s.contains("temple")));
    }
}
