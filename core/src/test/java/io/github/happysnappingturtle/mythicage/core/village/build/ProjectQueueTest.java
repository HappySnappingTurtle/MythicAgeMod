package io.github.happysnappingturtle.mythicage.core.village.build;

import com.google.gson.JsonParser;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.village.gen.Box;
import io.github.happysnappingturtle.mythicage.core.village.gen.BuildingSpec;
import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.core.village.gen.TemplateTransform;
import io.github.happysnappingturtle.mythicage.core.village.gen.Turn;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** GA-11 单元测试 TC-GA11-01～12：候选列表、原位放置、项目队列、点名门槛。 */
class ProjectQueueTest {
    private static BuildingSpec spec(String id, int[] size, String stage, int priority, String plot, String extra) {
        return BuildingSpec.parse(JsonParser.parseString("""
                {"id": "%s", "civ": "greek", "template": "t", "size": [%d, %d, %d], "stage": "%s", "priority": %d,
                 "frame": [-1, -1, -1], "plot_kinds": ["%s"], "door": {"pos": [4, 1, 9], "facing": "south"}%s}"""
                .formatted(id, size[0], size[1], size[2], stage, priority, plot, extra)).getAsJsonObject());
    }

    private static final BuildingSpec HOUSE = spec("greek:house_small", new int[]{9, 7, 10}, "archaic", 100, "house",
            ", \"generated\": true");
    private static final BuildingSpec HOUSE_II = spec("greek:house_small_ii", new int[]{9, 11, 10}, "classical", 25, "house",
            ", \"unique\": false, \"replace\": [\"greek:house_small\"]");
    private static final BuildingSpec GRANARY = spec("greek:granary", new int[]{11, 9, 9}, "archaic", 100, "storage",
            ", \"generated\": true");
    private static final BuildingSpec GRANARY_II = spec("greek:granary_ii", new int[]{11, 12, 9}, "classical", 22, "storage",
            ", \"unique\": false, \"replace\": [\"greek:granary\"]");

    private static List<BuildingSpec> buildings() {
        return List.of(spec("greek:storehouse", new int[]{13, 9, 11}, "classical", 20, "storage", ""),
                GRANARY_II, HOUSE_II,
                spec("greek:house_court", new int[]{13, 9, 13}, "classical", 30, "house", ", \"unique\": false"),
                spec("greek:acropolis_classical", new int[]{24, 13, 24}, "classical", 50, "temple_ext",
                        ", \"replace\": [\"greek:acropolis_archaic\"]"),
                spec("greek:palaestra", new int[]{15, 6, 15}, "heroic", 10, "training", ""),
                spec("greek:winery", new int[]{9, 7, 9}, "classical", 60, "workshop", ", \"requires\": [\"anchor:trellis\"]"),
                spec("greek:shrine_athena", new int[]{11, 7, 11}, "classical", 0, "shrine", ", \"god\": \"athena\""));
    }

    private static final List<ProjectPicker.Plot> PLOTS = List.of(
            new ProjectPicker.Plot("classical_house_1", "house", Age.CLASSICAL, 15, 15, "south"),
            new ProjectPicker.Plot("classical_storage_1", "storage", Age.CLASSICAL, 15, 13, "east"),
            new ProjectPicker.Plot("classical_shrine_1", "shrine", Age.CLASSICAL, 11, 11, "east"));

    private static BuildPlacement.Placed at(BuildingSpec s, Pos origin, Turn t) {
        return new BuildPlacement.Placed(s.id(), origin, t, TemplateTransform.worldBox(origin, s.sizeX(), s.sizeY(), s.sizeZ(), t));
    }

    private static List<ProjectPicker.Existing> existing() {
        return List.of(new ProjectPicker.Existing("greek:house_small", at(HOUSE, new Pos(100, 64, 100), Turn.R0)),
                new ProjectPicker.Existing("greek:house_small", at(HOUSE, new Pos(120, 64, 100), Turn.R0)),
                new ProjectPicker.Existing("greek:granary", at(GRANARY, new Pos(140, 64, 100), Turn.R0)),
                new ProjectPicker.Existing("greek:acropolis_archaic_zeus", null));
    }

    private static ProjectPicker.Input input(Age stage, boolean busy, boolean food, List<ProjectPicker.Plot> plots,
                                             List<ProjectPicker.ShrineRequest> shrines, ProjectPicker.Fit fit) {
        return new ProjectPicker.Input(stage, busy, food, buildings(), plots, Set.of(), Set.of(), shrines, Set.of(), existing(),
                Set.of(), fit);
    }

    private static ProjectPicker.Input classical() {
        return input(Age.CLASSICAL, false, false, PLOTS, List.of(), ProjectPicker.Fit.ALWAYS);
    }

    private static Map<String, ProjectPicker.Candidate> byKey(List<ProjectPicker.Candidate> list) {
        return list.stream().collect(Collectors.toMap(ProjectPicker.Candidate::key, c -> c));
    }

    @Test
    void tc01_classicalVillageListsNewAndUpgrades() {
        var list = ProjectPicker.candidates(classical());
        assertEquals(List.of("greek:palaestra", "greek:storehouse", "greek:granary_ii", "greek:house_small_ii", "greek:house_court",
                "greek:acropolis_classical", "greek:winery"),
                list.stream().map(ProjectPicker.Candidate::key).toList(), "按优先级；英雄建筑与缺前置的也列出");
        var c = byKey(list);
        assertEquals(ProjectPicker.Kind.NEW, c.get("greek:storehouse").kind());
        assertEquals("classical_storage_1", c.get("greek:storehouse").pick().plot());
        assertEquals(ProjectPicker.Kind.UPGRADE, c.get("greek:house_small_ii").kind());
        assertEquals(2, c.get("greek:house_small_ii").targets(), "两座小屋");
        assertEquals(new Pos(100, 64, 100), c.get("greek:house_small_ii").pick().replaceAt(), "先升级排在前面的那座");
        assertEquals("greek:granary", c.get("greek:granary_ii").pick().replaces());
        assertEquals("greek:acropolis_archaic_zeus", c.get("greek:acropolis_classical").pick().replaces());
        assertNull(c.get("greek:acropolis_classical").pick().replaceAt(), "位置不明的旧建筑");
        assertEquals(ProjectPicker.STAGE, c.get("greek:palaestra").reason());
        assertEquals(ProjectPicker.REQUIRES, c.get("greek:winery").reason());
        assertEquals(List.of(0, 1, 2, 3, 4), list.stream().filter(ProjectPicker.Candidate::available)
                .map(ProjectPicker.Candidate::rank).toList(), "村子自己选的顺序");
        assertEquals(-1, c.get("greek:winery").rank());
    }

    @Test
    void tc02_archaicVillageNothingAvailable() {
        var list = ProjectPicker.candidates(input(Age.ARCHAIC, false, false, PLOTS, List.of(), ProjectPicker.Fit.ALWAYS));
        assertFalse(list.isEmpty());
        assertTrue(list.stream().noneMatch(ProjectPicker.Candidate::available));
        assertTrue(list.stream().allMatch(c -> c.reason().equals(ProjectPicker.STAGE)));
    }

    @Test
    void tc03_foodShortPausesEverything() {
        var c = byKey(ProjectPicker.candidates(input(Age.CLASSICAL, false, true, PLOTS, List.of(), ProjectPicker.Fit.ALWAYS)));
        assertTrue(c.values().stream().noneMatch(ProjectPicker.Candidate::available), "全部不可用");
        assertEquals(ProjectPicker.FOOD_SHORT, c.get("greek:storehouse").reason());
        assertEquals(ProjectPicker.FOOD_SHORT, c.get("greek:house_small_ii").reason());
        assertEquals(ProjectPicker.STAGE, c.get("greek:palaestra").reason(), "英雄建筑先说时代不够");
        assertEquals(ProjectPicker.REQUIRES, c.get("greek:winery").reason(), "缺前置先说缺前置");
        assertTrue(ProjectPicker.pick(input(Age.CLASSICAL, false, true, PLOTS, List.of(), ProjectPicker.Fit.ALWAYS)).isEmpty());
    }

    @Test
    void tc04_pickUnchanged() {
        assertEquals("greek:storehouse", ProjectPicker.pick(classical()).orElseThrow().building());
        assertTrue(ProjectPicker.pick(classical().withBusy(true)).isEmpty(), "已有进行中的项目");
        var shrine = ProjectPicker.pick(input(Age.CLASSICAL, false, false, PLOTS,
                List.of(new ProjectPicker.ShrineRequest(Age.CLASSICAL, "athena")), ProjectPicker.Fit.ALWAYS)).orElseThrow();
        assertEquals("greek:shrine_athena", shrine.building());
        // 没有空的仓储地块：大仓库放不下 → 谷仓升级（原地，不占地块）
        var up = ProjectPicker.pick(input(Age.CLASSICAL, false, false, List.of(PLOTS.get(0)), List.of(), ProjectPicker.Fit.ALWAYS))
                .orElseThrow();
        assertEquals("greek:granary_ii", up.building());
        assertEquals("", up.plot());
        assertEquals(new Pos(140, 64, 100), up.replaceAt());
    }

    @Test
    void tc04a_upgradeDoesNotTargetItself() {
        var in = new ProjectPicker.Input(Age.CLASSICAL, false, false, buildings(), PLOTS, Set.of(), Set.of(), List.of(), Set.of(),
                List.of(new ProjectPicker.Existing("greek:house_small_ii", at(HOUSE_II, new Pos(0, 64, 0), Turn.R0))), Set.of(),
                ProjectPicker.Fit.ALWAYS);
        assertTrue(byKey(ProjectPicker.candidates(in)).get("greek:house_small_ii") == null, "二级住居不会再「升级」成自己");
    }

    @Test
    void tc04b_uniqueInProgressHidden() {
        var in = new ProjectPicker.Input(Age.CLASSICAL, true, false, buildings(), PLOTS, Set.of(), Set.of(), List.of(), Set.of(),
                existing(), Set.of("greek:storehouse"), ProjectPicker.Fit.ALWAYS);
        var c = byKey(ProjectPicker.candidates(in));
        assertNull(c.get("greek:storehouse"), "正在盖的唯一建筑不再列出");
        assertTrue(c.get("greek:house_court").available(), "施工中照样能看候选（点名下一个）");
    }

    // ───────────── UpgradeFit ─────────────

    @Test
    void tc05_sameFootprintFits() {
        var old = at(HOUSE, new Pos(100, 64, 100), Turn.R0);
        Box neighbour = new Box(110, 64, 100, 118, 70, 109);
        assertEquals(UpgradeFit.OK, UpgradeFit.check(HOUSE_II, HOUSE, old, List.of(old.box(), neighbour), List.of(), p -> true),
                "同占地：没有多出来的格子，地面也不用看");
        assertEquals(new Pos(100, 64, 100), UpgradeFit.place(HOUSE_II, HOUSE, old).origin());
    }

    @Test
    void tc06_widerUpgradeBlockedByNeighbour() {
        // 新模板东西各宽 1 格（frame 向西挪 1）
        BuildingSpec wide = BuildingSpec.parse(JsonParser.parseString("""
                {"id": "greek:house_wide", "civ": "greek", "template": "t", "size": [11, 9, 10], "stage": "classical",
                 "frame": [-2, -1, -1], "plot_kinds": ["house"], "door": {"pos": [5, 1, 9], "facing": "south"},
                 "replace": ["greek:house_small"]}""").getAsJsonObject());
        var old = at(HOUSE, new Pos(100, 64, 100), Turn.R0);
        assertEquals(new Box(99, 64, 100, 109, 72, 109), UpgradeFit.place(wide, HOUSE, old).box());
        assertEquals(UpgradeFit.OK, UpgradeFit.check(wide, HOUSE, old, List.of(), List.of(), p -> false));
        assertEquals(UpgradeFit.OCCUPIED, UpgradeFit.check(wide, HOUSE, old, List.of(new Box(109, 64, 104, 112, 68, 106)),
                List.of(), p -> false), "东边贴着另一座建筑");
        assertEquals(UpgradeFit.OCCUPIED, UpgradeFit.check(wide, HOUSE, old, List.of(),
                List.of(new UpgradeFit.PlotArea("workshop", new Box(95, 64, 100, 99, 64, 104))), p -> false), "压到别的地块");
        assertEquals(UpgradeFit.OK, UpgradeFit.check(wide, HOUSE, old, List.of(),
                List.of(new UpgradeFit.PlotArea("house", new Box(95, 64, 100, 99, 64, 104))), p -> false), "同种地块不算占用");
        assertEquals(UpgradeFit.GROUND, UpgradeFit.check(wide, HOUSE, old, List.of(), List.of(), p -> p.x() == 99),
                "西边一列是水");
    }

    @Test
    void tc07_rotatedOldBuilding() {
        BuildingSpec wide = BuildingSpec.parse(JsonParser.parseString("""
                {"id": "greek:house_wide", "civ": "greek", "template": "t", "size": [11, 9, 10], "stage": "classical",
                 "frame": [-2, -1, -1], "plot_kinds": ["house"], "door": {"pos": [5, 1, 9], "facing": "south"},
                 "replace": ["greek:house_small"]}""").getAsJsonObject());
        var old = at(HOUSE, new Pos(100, 64, 100), Turn.R90);
        var n = UpgradeFit.place(wide, HOUSE, old);
        assertEquals(new Pos(100, 64, 99), n.origin(), "frame 差 (−1, 0, 0) 转 90° 成 (0, 0, −1)");
        assertEquals(Turn.R90, n.turn());
        Pos oldDoor = TemplateTransform.toWorld(old.origin(), HOUSE.door(), old.turn());
        Pos newDoor = TemplateTransform.toWorld(n.origin(), wide.door(), n.turn());
        assertEquals(oldDoor, newDoor, "门在同一格");
        assertEquals(Turn.R90.rotateFacing(HOUSE.doorFacing()), n.turn().rotateFacing(wide.doorFacing()), "门朝同一边");
        BuildingSpec turned = BuildingSpec.parse(JsonParser.parseString("""
                {"id": "greek:house_turned", "civ": "greek", "template": "t", "size": [9, 7, 10], "stage": "classical",
                 "frame": [-1, -1, -1], "plot_kinds": ["house"], "door": {"pos": [0, 1, 4], "facing": "west"},
                 "replace": ["greek:house_small"]}""").getAsJsonObject());
        assertEquals(UpgradeFit.DOOR, UpgradeFit.check(turned, HOUSE, old, List.of(), List.of(), p -> false));
    }

    @Test
    void tc07a_unfitUpgradeUnavailable() {
        ProjectPicker.Fit firstBlocked = (next, old) -> old.at() != null && old.at().origin().x() == 100 ? UpgradeFit.OCCUPIED : "";
        var c = byKey(ProjectPicker.candidates(input(Age.CLASSICAL, false, false, PLOTS, List.of(), firstBlocked)));
        assertEquals(new Pos(120, 64, 100), c.get("greek:house_small_ii").pick().replaceAt(), "第一座放不下，换第二座");
        ProjectPicker.Fit none = (next, old) -> UpgradeFit.OCCUPIED;
        var all = byKey(ProjectPicker.candidates(input(Age.CLASSICAL, false, false, PLOTS, List.of(), none)));
        assertEquals(ProjectPicker.NO_FIT, all.get("greek:house_small_ii").reason());
        assertTrue(all.get("greek:acropolis_classical").available(), "位置不明的旧神庙照 T19 不做原位判断");
    }

    // ───────────── 队列 ─────────────

    private static Optional<ProjectQueue.Next> next(String key) {
        return Optional.of(new ProjectQueue.Next(key, key, "", UUID.randomUUID(), "Alex", 3));
    }

    @Test
    void tc08_nextStartsAfterCompletion() {
        var d = ProjectQueue.decide(classical(), next("greek:house_court"), false);
        assertEquals("greek:house_court", d.pick().orElseThrow().building());
        assertEquals("classical_house_1", d.pick().get().plot());
        assertTrue(d.fromNext() && d.dropNext());
        assertTrue(ProjectQueue.decide(classical().withBusy(true), next("greek:house_court"), false).pick().isEmpty(),
                "施工中不动");
        assertFalse(ProjectQueue.decide(classical().withBusy(true), next("greek:house_court"), true).dropNext(), "也不清空");
    }

    @Test
    void tc09_shrineJumpsAheadOfNext() {
        var in = input(Age.CLASSICAL, false, false, PLOTS, List.of(new ProjectPicker.ShrineRequest(Age.CLASSICAL, "athena")),
                ProjectPicker.Fit.ALWAYS);
        var d = ProjectQueue.decide(in, next("greek:house_court"), false);
        assertEquals("greek:shrine_athena", d.pick().orElseThrow().building());
        assertFalse(d.fromNext());
        assertFalse(d.dropNext(), "「下一个」位保留");
    }

    @Test
    void tc10_villageDoesNotPickWhileNextHeld() {
        var none = ProjectQueue.decide(classical(), Optional.empty(), false);
        assertTrue(none.pick().isEmpty(), "不是黎明、没有点名：等黎明");
        assertEquals("greek:storehouse", ProjectQueue.decide(classical(), Optional.empty(), true).pick().orElseThrow().building());
        var held = ProjectQueue.decide(classical(), next("greek:house_small_ii"), true);
        assertEquals("greek:house_small_ii", held.pick().orElseThrow().building(), "黎明也先开工点名的，不选大仓库（D2）");
        var food = ProjectQueue.decide(input(Age.CLASSICAL, false, true, PLOTS, List.of(), ProjectPicker.Fit.ALWAYS),
                next("greek:house_small_ii"), true);
        assertTrue(food.pick().isEmpty() && !food.dropNext(), "缺粮：暂停，点名保留");
    }

    @Test
    void tc11_unavailableNextDropped() {
        var d = ProjectQueue.decide(input(Age.CLASSICAL, false, false, List.of(PLOTS.get(1)), List.of(), ProjectPicker.Fit.ALWAYS),
                next("greek:house_court"), false);
        assertTrue(d.dropNext());
        assertEquals(ProjectPicker.NO_PLOT, d.dropReason(), "地块被占");
        assertTrue(d.pick().isEmpty(), "不是黎明：等黎明村子自己选");
        var dawn = ProjectQueue.decide(input(Age.CLASSICAL, false, false, List.of(PLOTS.get(1)), List.of(),
                ProjectPicker.Fit.ALWAYS), next("greek:house_court"), true);
        assertEquals("greek:storehouse", dawn.pick().orElseThrow().building(), "黎明：清空后村子照常选");
        var gone = ProjectQueue.decide(classical(), next("greek:theater"), false);
        assertEquals(ProjectQueue.GONE, gone.dropReason());
    }

    @Test
    void tc12_contributionThreshold() {
        var s = ProjectQueue.Settings.DEFAULT;
        assertFalse(s.allows(63, false));
        assertTrue(s.allows(64, false));
        assertTrue(s.allows(32, true), "供奉者减半");
        assertFalse(s.allows(31, true));
        assertEquals(33, new ProjectQueue.Settings(65, 0.5).threshold(true), "向上取整");
        assertEquals(new ProjectQueue.Settings(10, 1), ProjectQueue.Settings.parse(JsonParser.parseString(
                "{\"minContribution\": 10, \"patronFactor\": 1}").getAsJsonObject()));
        assertEquals(s, ProjectQueue.Settings.parse(new com.google.gson.JsonObject()));
        assertThrows(IllegalArgumentException.class, () -> new ProjectQueue.Settings(-1, 0.5));
        assertThrows(IllegalArgumentException.class, () -> new ProjectQueue.Settings(64, 1.5));
    }
}
