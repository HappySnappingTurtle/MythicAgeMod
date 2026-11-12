package io.github.happysnappingturtle.mythicage.core.village.build;

import com.google.gson.JsonParser;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.village.economy.VillageStock;
import io.github.happysnappingturtle.mythicage.core.village.gen.Box;
import io.github.happysnappingturtle.mythicage.core.village.gen.BuildingSpec;
import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.core.village.gen.Turn;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T19 单元测试 TC-T19-32～37（含 32a、32b、34a、36a）：施工的折算、切段、摆放、选项目、取消。 */
class BuildTest {
    private static final String STONE = "minecraft:stone";

    private static BuildSegments.Cell cell(int x, int y, int z, String block) {
        return new BuildSegments.Cell(new Pos(x, y, z), block, "", 0, false, 0);
    }

    private static BuildingSpec spec(String json) {
        return BuildingSpec.parse(JsonParser.parseString(json).getAsJsonObject());
    }

    private static BuildingSpec building(String id, String stage, int priority, String plot, String extra) {
        return spec("""
                {"id": "%s", "civ": "greek", "template": "t", "size": [9, 5, 9], "stage": "%s", "priority": %d,
                 "plot_kinds": ["%s"], "door": {"pos": [4, 0, 8], "facing": "south"}%s}"""
                .formatted(id, stage, priority, plot, extra));
    }

    private static ProjectPicker.Input input(Age stage, List<BuildingSpec> bs, List<ProjectPicker.Plot> plots,
                                             Set<String> built, List<ProjectPicker.ShrineRequest> shrines, Set<String> sat,
                                             Set<String> replaceable) {
        return new ProjectPicker.Input(stage, false, false, bs, plots, built, Set.of(), shrines, sat, replaceable);
    }

    // ───────────── 折算 ─────────────

    static BuildCosts costs() {
        return BuildCosts.parse(JsonParser.parseString("""
                {"free": ["minecraft:gravel", "*_leaves"],
                 "groups": {"planks": ["mythic_age:olive_planks", "minecraft:oak_planks"]},
                 "rules": [{"match": "*:marble_column*", "cost": {"mythic_age:marble_column": 0.25}},
                           {"match": "*:marble*", "cost": {"mythic_age:marble_ashlar": 0.125}},
                           {"match": "*:olive_*", "cost": {"#planks": 0.125}},
                           {"match": "minecraft:stone", "cost": {"minecraft:cobblestone": 1}}]}""").getAsJsonObject());
    }

    @Test
    void tc32a_costRulesAndCumulativeRounding() {
        BuildCosts c = costs();
        assertEquals(Map.of("mythic_age:marble_column", 0.25), c.cost("mythic_age:marble_column_base"));
        assertEquals(Map.of("mythic_age:marble_ashlar", 0.125), c.cost("mythic_age:marble_cornice"));
        assertEquals(Map.of(), c.cost("minecraft:gravel"));
        assertEquals(Map.of(), c.cost("mythic_age:olive_leaves"), "free 先于规则");
        assertFalse(c.covers("minecraft:diamond_block"));
        assertTrue(c.covers("minecraft:gravel"));
        assertEquals(List.of("mythic_age:olive_planks", "minecraft:oak_planks"), c.items("#planks"));
        // 10 格大理石：账单 2；前 8 格扣 1，后 2 格扣 1
        List<String> ten = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ten.add("mythic_age:marble_cornice");
        }
        BuildProject p = new BuildProject(1, "b", "p", "", "", new Pos(0, 0, 0), Turn.R0, null, null, 0);
        BuildCosts.add(p.planned, c.sum(ten));
        assertEquals(Map.of("mythic_age:marble_ashlar", 2), p.bill());
        VillageStock stock = new VillageStock(100);
        stock.deposit("mythic_age:marble_ashlar", 5);
        p.stockUp(stock, c);
        assertEquals(3, stock.count("mythic_age:marble_ashlar"), "只托管账单的量");
        assertTrue(p.missing().isEmpty());
        Map<String, Integer> first = p.pay(stock, c.sum(ten.subList(0, 8)));
        assertEquals(Map.of("mythic_age:marble_ashlar", 1), first);
        Map<String, Integer> second = p.pay(stock, c.sum(ten.subList(8, 10)));
        assertEquals(Map.of("mythic_age:marble_ashlar", 1), second);
        assertFalse(stock.hasEscrow(p.escrowId()), "扣完托管清空");
        // 物品组：橄榄木板不够时用橡木板
        BuildProject q = new BuildProject(2, "b", "p", "", "", new Pos(0, 0, 0), Turn.R0, null, null, 0);
        q.planned.put("#planks", 3.0);
        VillageStock s2 = new VillageStock(100);
        s2.deposit("mythic_age:olive_planks", 1);
        s2.deposit("minecraft:oak_planks", 5);
        q.stockUp(s2, c);
        assertEquals(Map.of("mythic_age:olive_planks", 1, "minecraft:oak_planks", 2), q.held.get("#planks"));
        assertEquals(3, s2.count("minecraft:oak_planks"));
    }

    // ───────────── 切段 ─────────────

    @Test
    void tc32_fortyBlocksThreeSegmentsBottomUp() {
        List<BuildSegments.Cell> cells = new ArrayList<>();
        for (int y = 2; y >= 0; y--) {
            for (int x = 0; x < (y == 2 ? 8 : 16); x++) {
                cells.add(cell(x, y, 0, STONE));
            }
        }
        List<BuildSegments.Segment> segs = BuildSegments.cut(cells, BuildSegments.MAX);
        assertEquals(List.of(16, 16, 8), segs.stream().map(BuildSegments.Segment::solid).toList());
        assertTrue(segs.get(0).cells().stream().allMatch(c -> c.pos().y() == 0));
        assertTrue(segs.get(2).cells().stream().allMatch(c -> c.pos().y() == 2));
        assertEquals(List.of(0, 1, 2), segs.stream().map(BuildSegments.Segment::index).toList());
    }

    @Test
    void tc32b_doorHalvesTogetherAndAirTail() {
        List<BuildSegments.Cell> cells = new ArrayList<>();
        for (int x = 0; x < 15; x++) {
            cells.add(cell(x, 0, 0, STONE));
        }
        cells.add(new BuildSegments.Cell(new Pos(15, 0, 0), "minecraft:oak_door", "", 7, false, 0));
        cells.add(new BuildSegments.Cell(new Pos(15, 1, 0), "minecraft:oak_door", "", 7, false, 0));
        cells.add(cell(0, 1, 0, STONE));
        cells.add(cell(0, 2, 0, BuildSegments.AIR));
        cells.add(cell(1, 2, 0, BuildSegments.AIR));
        List<BuildSegments.Segment> segs = BuildSegments.cut(cells, BuildSegments.MAX);
        assertEquals(2, segs.size(), "只有空气的尾段并进前一段");
        assertEquals(15, segs.get(0).solid(), "下一对门会超出 tripSize，留到下一趟");
        assertTrue(segs.get(1).cells().stream().filter(c -> c.link() == 7).count() == 2);
        assertEquals(3, segs.get(1).solid());
        assertEquals(5, segs.get(1).cells().size());
        assertEquals(17, BuildSegments.legacyCut(cells, BuildSegments.MAX).getFirst().solid(), "旧布局重建保留原来的17格段");
    }

    @Test
    void tc33_replaceDemolishThenPlaceLastAtEnd() {
        List<BuildSegments.Cell> old = new ArrayList<>();
        List<BuildSegments.Cell> nu = new ArrayList<>();
        // 重合 20 格
        for (int x = 0; x < 20; x++) {
            old.add(cell(x, 0, 0, STONE));
            nu.add(cell(x, 0, 0, STONE));
        }
        // 旧有新无 10 格（其中新蓝图写成空气 5 格、没写 5 格）
        for (int x = 0; x < 10; x++) {
            old.add(cell(x, 1, 0, "minecraft:oak_planks"));
            if (x < 5) {
                nu.add(cell(x, 1, 0, BuildSegments.AIR));
            }
        }
        // 新有旧无 30 格，其中 2 格在神像旁（最后）
        for (int x = 0; x < 30; x++) {
            nu.add(cell(x, 0, 1, "minecraft:quartz_block"));
        }
        // 旧模板地面以下的地基新模板没写：不拆
        old.add(cell(0, -1, 0, "minecraft:gravel"));
        Pos statue = new Pos(29, 0, 1);
        List<BuildSegments.Segment> segs = BuildSegments.replace(old, nu, 0,
                p -> Math.abs(p.x() - statue.x()) <= 1 && p.z() == statue.z() && p.y() == 0, BuildSegments.MAX);
        int demolished = segs.stream().filter(s -> s.kind() == BuildSegments.Kind.DEMOLISH).mapToInt(BuildSegments.Segment::solid).sum();
        int placedN = segs.stream().filter(s -> s.kind() == BuildSegments.Kind.PLACE).mapToInt(BuildSegments.Segment::solid).sum();
        assertEquals(10, demolished);
        assertEquals(30, placedN);
        assertEquals(BuildSegments.Kind.DEMOLISH, segs.get(0).kind(), "先拆");
        BuildSegments.Segment lastSeg = segs.get(segs.size() - 1);
        assertEquals(BuildSegments.Kind.PLACE, lastSeg.kind());
        assertTrue(lastSeg.cells().stream().allMatch(BuildSegments.Cell::last), "神像旁的格子在最后一段");
        assertEquals(2, lastSeg.solid());
        assertTrue(segs.stream().flatMap(s -> s.cells().stream()).noneMatch(c -> c.pos().y() == -1), "地基不拆");
    }

    // ───────────── 摆放 ─────────────

    @Test
    void tc34a_placeOnPlotRotatedAndCentered() {
        BuildingSpec s = spec("""
                {"id": "greek:storehouse", "civ": "greek", "template": "t", "size": [11, 9, 9], "floor_y": 1,
                 "door": {"pos": [10, 1, 4], "facing": "east"}}""");
        Box plot = new Box(100, 64, 200, 110, 64, 210);
        BuildPlacement.Placed p = BuildPlacement.onPlot(s, plot, "south");
        assertEquals(Turn.R90, p.turn());
        assertEquals(9, p.box().sizeX());
        assertEquals(11, p.box().sizeZ());
        assertEquals(101, p.box().minX(), "居中：11 − 9 = 2，每边 1");
        assertEquals(200, p.box().minZ());
        assertEquals(63, p.box().minY(), "地面层 = 地块 y");
        assertTrue(BuildPlacement.fits(13, 13, Turn.R0, 11, 11));
        assertFalse(BuildPlacement.fits(14, 14, Turn.R0, 11, 11));
        assertTrue(BuildPlacement.fits(9, 13, Turn.R90, 11, 7), "转 90° 后 13 × 9 放 11 × 7");
    }

    @Test
    void tc34a_replaceAlignsFramesAndLocates() {
        BuildingSpec old = spec("""
                {"id": "greek:acropolis_archaic", "civ": "greek", "template": "t", "size": [20, 11, 24], "frame": [9, 0, -11],
                 "anchors": {"ritual": [[20, 0, 18]], "guardian": [[4, 1, 18]]}}""");
        BuildingSpec next = spec("""
                {"id": "greek:acropolis_classical", "civ": "greek", "template": "t", "size": [24, 13, 24], "frame": [5, 0, -11],
                 "anchors": {"ritual": [[24, 0, 18]]}}""");
        Pos origin = new Pos(1000, 70, 500);
        Box box = io.github.happysnappingturtle.mythicage.core.village.gen.TemplateTransform.worldBox(origin, 20, 11, 24, Turn.R0);
        var found = BuildPlacement.locate(old, Map.of("ritual", List.of(new Pos(1020, 70, 518))), List.of(new Box(0, 0, 0, 1, 1, 1), box));
        assertTrue(found.isPresent());
        assertEquals(origin, found.get().origin());
        BuildPlacement.Placed n = BuildPlacement.replacing(next, old, found.get());
        assertEquals(new Pos(996, 70, 500), n.origin(), "新 frame 向西 4 格");
        assertEquals(BuildPlacement.worldAnchors(old, found.get()).get("ritual"), BuildPlacement.worldAnchors(next, n).get("ritual"),
                "仪式位不动");
        assertTrue(BuildPlacement.locate(old, Map.of("ritual", List.of(new Pos(1021, 70, 518))), List.of(box)).isEmpty());
    }

    // ───────────── 选项目 ─────────────

    private static final List<ProjectPicker.Plot> PLOTS = List.of(
            new ProjectPicker.Plot("classical_workshop_1", "workshop", Age.CLASSICAL, 9, 9, "south"),
            new ProjectPicker.Plot("classical_storage_1", "storage", Age.CLASSICAL, 11, 9, "east"),
            new ProjectPicker.Plot("classical_shrine_1", "shrine", Age.CLASSICAL, 11, 11, "east"),
            new ProjectPicker.Plot("heroic_shrine_1", "shrine", Age.HEROIC, 11, 11, "east"));

    private static List<BuildingSpec> greek() {
        return List.of(building("greek:stoneworks", "classical", 10, "workshop", ""),
                building("greek:storehouse", "classical", 20, "storage", ""),
                building("greek:winery", "classical", 60, "workshop", ", \"requires\": [\"anchor:trellis\"]"),
                building("greek:acropolis_classical", "classical", 50, "temple_ext",
                        ", \"replace\": [\"greek:acropolis_archaic\"]"),
                building("greek:shrine_athena", "classical", 0, "shrine", ", \"god\": \"athena\""),
                building("greek:shrine", "classical", 0, "shrine", ", \"god\": \"*\""));
    }

    @Test
    void tc34_busyOrArchaicPicksNothing() {
        var in = input(Age.CLASSICAL, greek(), PLOTS, Set.of(), List.of(), Set.of(), Set.of());
        assertTrue(ProjectPicker.pick(in).isPresent());
        assertTrue(ProjectPicker.pick(new ProjectPicker.Input(Age.CLASSICAL, true, false, greek(), PLOTS, Set.of(), Set.of(),
                List.of(), Set.of(), Set.of())).isEmpty(), "已有进行中的项目");
        assertTrue(ProjectPicker.pick(input(Age.ARCHAIC, greek(), PLOTS, Set.of(), List.of(), Set.of(), Set.of())).isEmpty(),
                "远古（D7）");
    }

    @Test
    void tc35_foodShortPicksNothing() {
        assertTrue(ProjectPicker.pick(new ProjectPicker.Input(Age.CLASSICAL, false, true, greek(), PLOTS, Set.of(), Set.of(),
                List.of(), Set.of(), Set.of())).isEmpty());
    }

    @Test
    void tc36_shrineFirst() {
        var in = input(Age.CLASSICAL, greek(), PLOTS, Set.of(),
                List.of(new ProjectPicker.ShrineRequest(Age.CLASSICAL, "athena")), Set.of(), Set.of());
        var p = ProjectPicker.pick(in).orElseThrow();
        assertEquals("greek:shrine_athena", p.building());
        assertEquals("classical_shrine_1", p.plot());
        assertEquals("athena", p.god());
        // 没有专门祠的神用通用祠，英雄神建在英雄地块
        var h = ProjectPicker.pick(input(Age.HEROIC, greek(), PLOTS, Set.of(),
                List.of(new ProjectPicker.ShrineRequest(Age.HEROIC, "dionysus")), Set.of(), Set.of())).orElseThrow();
        assertEquals("greek:shrine", h.building());
        assertEquals("heroic_shrine_1", h.plot());
        assertEquals("dionysus", h.god());
        // 已经有祠：不再插队
        var none = ProjectPicker.pick(new ProjectPicker.Input(Age.CLASSICAL, false, false, greek(), PLOTS, Set.of(),
                Set.of("athena"), List.of(new ProjectPicker.ShrineRequest(Age.CLASSICAL, "athena")), Set.of(), Set.of()));
        assertEquals("greek:stoneworks", none.orElseThrow().building());
    }

    @Test
    void tc36a_priorityUniqueRequiresReplace() {
        assertEquals("greek:stoneworks", ProjectPicker.pick(input(Age.CLASSICAL, greek(), PLOTS, Set.of(), List.of(), Set.of(),
                Set.of())).orElseThrow().building());
        // 石作坊建好了（工坊地块也用掉了）→ 大仓库
        List<ProjectPicker.Plot> rest = PLOTS.subList(1, PLOTS.size());
        assertEquals("greek:storehouse", ProjectPicker.pick(input(Age.CLASSICAL, greek(), rest, Set.of("greek:stoneworks"),
                List.of(), Set.of(), Set.of())).orElseThrow().building());
        // 大仓库也建好：没有旧神庙可替换、没有葡萄园 → 什么都不选
        List<ProjectPicker.Plot> shrinesOnly = PLOTS.subList(2, PLOTS.size());
        assertTrue(ProjectPicker.pick(input(Age.CLASSICAL, greek(), shrinesOnly, Set.of("greek:stoneworks", "greek:storehouse"),
                List.of(), Set.of(), Set.of())).isEmpty());
        // 有远古神庙（主神变体）→ 原地替换，不占地块
        var temple = ProjectPicker.pick(input(Age.CLASSICAL, greek(), shrinesOnly, Set.of("greek:stoneworks", "greek:storehouse"),
                List.of(), Set.of(), Set.of("greek:acropolis_archaic_zeus"))).orElseThrow();
        assertEquals("greek:acropolis_classical", temple.building());
        assertEquals("", temple.plot());
        assertEquals("greek:acropolis_archaic_zeus", temple.replaces());
        // 葡萄酒坊要有葡萄园；工坊地块空着时才能建
        var winery = ProjectPicker.pick(input(Age.CLASSICAL, greek(), PLOTS,
                Set.of("greek:stoneworks", "greek:storehouse", "greek:acropolis_classical"), List.of(), Set.of("anchor:trellis"),
                Set.of())).orElseThrow();
        assertEquals("greek:winery", winery.building());
    }

    // ───────────── 取消 ─────────────

    @Test
    void tc37_cancelReturnsOnlyRemainingEscrow() {
        BuildCosts c = costs();
        BuildProject p = new BuildProject(3, "b", "p", "", "", new Pos(0, 0, 0), Turn.R0, null, null, 0);
        List<String> blocks = new ArrayList<>();
        for (int i = 0; i < 52; i++) {
            blocks.add(STONE);
        }
        BuildCosts.add(p.planned, c.sum(blocks));
        VillageStock stock = new VillageStock(1000);
        stock.deposit("minecraft:cobblestone", 60);
        p.stockUp(stock, c);
        assertEquals(8, stock.count("minecraft:cobblestone"));
        p.pay(stock, c.sum(blocks.subList(0, 16)));
        p.pay(stock, c.sum(blocks.subList(16, 32)));
        Map<String, Integer> back = p.cancel(stock);
        assertEquals(Map.of("minecraft:cobblestone", 20), back);
        assertEquals(28, stock.count("minecraft:cobblestone"));
        assertEquals(BuildProject.State.CANCELLED, p.state);
        assertFalse(stock.hasEscrow(p.escrowId()));
    }

    @Test
    void refundFloorsCumulatively() {
        BuildCosts c = costs();
        BuildProject p = new BuildProject(4, "b", "p", "", "", new Pos(0, 0, 0), Turn.R0, null, null, 0);
        VillageStock stock = new VillageStock(0);
        for (int i = 0; i < 7; i++) {
            assertTrue(p.refund(stock, c, c.cost("mythic_age:marble_cornice")).isEmpty());
        }
        assertEquals(Map.of("mythic_age:marble_ashlar", 1), p.refund(stock, c, c.cost("mythic_age:marble_cornice")));
        assertEquals(1, stock.count("mythic_age:marble_ashlar"), "退回不受容量限制");
        assertEquals(Map.of("mythic_age:olive_planks", 1), p.refund(stock, c, Map.of("#planks", 1.0)), "物品组退回组里第一样");
    }

    @Test
    void repairAddsCostAndReturnsToStocking() {
        BuildCosts c = costs();
        BuildProject p = new BuildProject(5, "b", "p", "", "", new Pos(0, 0, 0), Turn.R0, null, null, 0);
        p.segments = 2;
        BuildCosts.add(p.planned, Map.of("minecraft:cobblestone", 4.0));
        VillageStock stock = new VillageStock(100);
        stock.deposit("minecraft:cobblestone", 10);
        p.stockUp(stock, c);
        p.state = BuildProject.State.BUILDING;
        assertTrue(p.canPay(Map.of("minecraft:cobblestone", 2.0)));
        p.pay(stock, Map.of("minecraft:cobblestone", 2.0));
        p.placed.set(0);
        p.pay(stock, Map.of("minecraft:cobblestone", 2.0));
        p.placed.set(1);
        assertTrue(p.complete());
        p.unplace(Map.of(1, Map.of("minecraft:cobblestone", 2.0)));
        assertEquals(BuildProject.State.STOCKING, p.state);
        assertFalse(p.complete());
        assertEquals(Map.of("minecraft:cobblestone", 2), p.missing());
        assertFalse(p.canPay(Map.of("minecraft:cobblestone", 2.0)));
        p.stockUp(stock, c);
        assertTrue(p.canPay(Map.of("minecraft:cobblestone", 2.0)));
    }
}
