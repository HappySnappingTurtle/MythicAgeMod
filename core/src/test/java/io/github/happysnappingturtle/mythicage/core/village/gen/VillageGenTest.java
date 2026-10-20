package io.github.happysnappingturtle.mythicage.core.village.gen;

import com.google.gson.JsonParser;
import io.github.happysnappingturtle.mythicage.core.config.ConfigIssue;
import io.github.happysnappingturtle.mythicage.core.config.ConfigLoadResult;
import io.github.happysnappingturtle.mythicage.core.config.ConfigLoader;
import io.github.happysnappingturtle.mythicage.core.config.villages.VillagesConfig;
import io.github.happysnappingturtle.mythicage.core.config.villages.VillagesSchema;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TC-T13b-01～31 中属于 core 的部分（阶段 ①）。 */
class VillageGenTest {
    // ───────────── 地形 ─────────────

    record Terrain(java.util.function.IntBinaryOperator s, java.util.function.IntBinaryOperator f) implements TerrainSampler {
        static int calls;

        @Override
        public int surface(int x, int z) {
            calls++;
            return s.applyAsInt(x, z);
        }

        @Override
        public int floor(int x, int z) {
            calls++;
            return f.applyAsInt(x, z);
        }

        static Terrain dry(java.util.function.IntBinaryOperator h) {
            return new Terrain(h, h);
        }
    }

    static final Terrain FLAT = Terrain.dry((x, z) -> 64);
    static final Terrain CONE = Terrain.dry((x, z) -> 64 + (int) Math.max(0, 30 - Math.hypot(x - 200, z) / 3));
    static final Terrain CLIFF = Terrain.dry((x, z) -> Math.floorMod(Math.floorDiv(x, 8) + Math.floorDiv(z, 8), 2) == 0 ? 64 : 84);

    static Map<String, BuildingSpec> specs() throws IOException {
        Path dir = Path.of(System.getProperty("mythicage.rootDir"))
                .resolve("neoforge/src/main/resources/data/mythic_age/mythic_age/village_building/greek");
        Map<String, BuildingSpec> out = new HashMap<>();
        try (Stream<Path> files = Files.list(dir)) {
            for (Path p : files.toList()) {
                BuildingSpec s = BuildingSpec.parse(JsonParser.parseString(Files.readString(p)).getAsJsonObject());
                out.put(s.id(), s);
            }
        }
        return out;
    }

    // ───────────── 2.1 配置 ─────────────

    @Test
    void tc01_defaults() {
        VillagesConfig c = VillagesSchema.INSTANCE.defaults();
        assertFalse(c.generateVanillaVillages());
        assertEquals(0.30, c.chineseRiverChance());
        assertEquals(3, VillagesSchema.INSTANCE.currentVersion());
    }

    @Test
    void tc02_03_migrateReplaceVanilla() {
        ConfigLoadResult<VillagesConfig> keep = ConfigLoader.load(VillagesSchema.INSTANCE,
                "{\"schemaVersion\":2,\"replaceVanillaInRegion\":false}", null);
        assertTrue(keep.value().generateVanillaVillages(), "明确要保留原版村庄 → 打开开关");
        assertTrue(keep.issues().stream().anyMatch(i -> i.path().contains("replaceVanillaInRegion")));
        assertFalse(ConfigLoader.load(VillagesSchema.INSTANCE,
                "{\"schemaVersion\":2,\"replaceVanillaInRegion\":true}", null).value().generateVanillaVillages());
        assertFalse(ConfigLoader.load(VillagesSchema.INSTANCE, "{\"schemaVersion\":2}", null).value()
                .generateVanillaVillages());
        assertTrue(ConfigLoader.load(VillagesSchema.INSTANCE,
                "{\"replaceVanillaInRegion\":true,\"generateVanillaVillages\":true}", null).value()
                .generateVanillaVillages(), "新字段优先");
    }

    @Test
    void tc04_chanceOutOfRange() {
        for (String bad : List.of("1.5", "-0.1", "\"abc\"")) {
            ConfigLoadResult<VillagesConfig> r = ConfigLoader.load(VillagesSchema.INSTANCE,
                    "{\"chineseRiverChance\":" + bad + "}", null);
            assertEquals(0.30, r.value().chineseRiverChance(), bad);
            assertTrue(r.issues().stream().map(ConfigIssue::path).anyMatch(p -> p.contains("chineseRiverChance")), bad);
        }
    }

    // ───────────── 2.2 门控与文明选择 ─────────────

    @Test
    void tc05_07_gate() {
        assertEquals(GateRules.Decision.SKIP, GateRules.decide(false, true));
        assertEquals(GateRules.Decision.SKIP, GateRules.decide(false, false));
        assertEquals(GateRules.Decision.YIELD, GateRules.decide(true, true));
        assertEquals(GateRules.Decision.VANILLA, GateRules.decide(true, false));
    }

    static final BiomeView RIVER_EVERYWHERE = (id, x, z) -> id.equals("#mythic_age:region/greek") || id.equals(CivSelector.RIVER);
    static final List<CivRule> RULES = List.of(
            new CivRule("chinese", "", List.of("#mythic_age:region/egyptian"), 32, 1.0, "chineseRiverChance", "chinese"),
            CivRule.simple("greek", "#mythic_age:region/greek"));

    @Test
    void tc08_requiresTemplate() {
        assertEquals(Optional.of("greek"), CivSelector.select(RULES, RIVER_EVERYWHERE, 10, 10, Set.of("greek"),
                Map.of("chineseRiverChance", 1.0)));
    }

    @Test
    void tc09_chanceDeterministic() {
        Map<String, Double> ch = Map.of("chineseRiverChance", 0.30);
        Optional<String> first = CivSelector.select(RULES, RIVER_EVERYWHERE, 123, -456, Set.of("greek", "chinese"), ch);
        for (int i = 0; i < 100; i++) {
            assertEquals(first, CivSelector.select(RULES, RIVER_EVERYWHERE, 123, -456, Set.of("greek", "chinese"), ch));
        }
        int hits = 0;
        for (int i = 0; i < 1000; i++) {
            if (CivSelector.select(RULES, RIVER_EVERYWHERE, i * 977, i * -613, Set.of("greek", "chinese"), ch)
                    .orElse("").equals("chinese")) {
                hits++;
            }
        }
        assertTrue(hits >= 270 && hits <= 330, "命中 " + hits);
    }

    @Test
    void tc10_noMatch() {
        assertEquals(Optional.empty(), CivSelector.select(RULES, (id, x, z) -> false, 0, 0, Set.of("greek"), Map.of()));
    }

    // ───────────── 2.3 选址评分 ─────────────

    @Test
    void tc11_flat() {
        SiteScorer.Site s = SiteScorer.best(0, 0, FLAT, SiteScorer.Config.DEFAULT).orElseThrow();
        assertEquals(1.0, s.score(), 1e-9);
        assertEquals(64, s.y());
    }

    @Test
    void tc12_hillPrefersHighGround() {
        // 候选区块中心在山腰（离山顶 64 格），应选向山顶一侧的候选
        SiteScorer.Site s = SiteScorer.best(200 - 64, 0, CONE, SiteScorer.Config.DEFAULT).orElseThrow();
        assertTrue(s.x() > 200 - 64, "选中 " + s);
    }

    @Test
    void tc13_cliffRejected() {
        assertTrue(SiteScorer.best(0, 0, CLIFF, SiteScorer.Config.DEFAULT).isEmpty());
    }

    @Test
    void tc14_sampleBudget() {
        Terrain.calls = 0;
        SiteScorer.best(0, 0, FLAT, SiteScorer.Config.DEFAULT);
        assertTrue(Terrain.calls <= 1100, "采样 " + Terrain.calls);
    }

    @Test
    void waterRejected() {
        Terrain sea = new Terrain((x, z) -> 63, (x, z) -> 50);
        assertTrue(SiteScorer.best(0, 0, sea, SiteScorer.Config.DEFAULT).isEmpty());
    }

    // ───────────── 2.4 模板变换 ─────────────

    @Test
    void tc15_rotations() {
        // 7 × 9 的模板，门在 (3, 0, 8) 朝南
        Pos door = new Pos(3, 0, 8);
        assertEquals(new Pos(3, 0, 8), Turn.R0.apply(door));
        assertEquals(new Pos(-8, 0, 3), Turn.R90.apply(door));
        assertEquals(new Pos(-3, 0, -8), Turn.R180.apply(door));
        assertEquals(new Pos(8, 0, -3), Turn.R270.apply(door));
        assertEquals("west", Turn.R90.rotateFacing("south"));
        assertEquals("east", Turn.R270.rotateFacing("south"));
        assertEquals("north", Turn.R180.rotateFacing("south"));
        for (Turn t : Turn.values()) {
            assertEquals(t, Turn.between("south", t.rotateFacing("south")));
            int[] size = TemplateTransform.rotatedSize(7, 9, t);
            Pos origin = TemplateTransform.originFor(new Pos(0, 0, 0), 7, 9, t);
            Box box = TemplateTransform.worldBox(origin, 7, 5, 9, t);
            assertEquals(new Box(0, 0, 0, size[0] - 1, 4, size[1] - 1), box, t.name());
        }
    }

    @Test
    void tc16_translate() {
        Pos origin = TemplateTransform.originFor(new Pos(1000, 70, -2000), 7, 9, Turn.R90);
        Box box = TemplateTransform.worldBox(origin, 7, 5, 9, Turn.R90);
        assertEquals(1000, box.minX());
        assertEquals(-2000, box.minZ());
        Pos door = TemplateTransform.toWorld(origin, new Pos(3, 0, 8), Turn.R90);
        assertTrue(box.containsXZ(door.x(), door.z()));
        assertEquals(box.minX(), door.x(), "门朝西，在包围盒西边");
    }

    // ───────────── 2.5 固定规划器 ─────────────

    @Test
    void tc17_fixedPlannerFlat() throws IOException {
        List<String> problems = new ArrayList<>();
        VillagePlan p = FixedPlanner.plan(0, 0, FLAT, specs(), "greek", "zeus", problems).orElseThrow(
                () -> new AssertionError(problems.toString()));
        assertEquals(6, p.buildings().size());
        for (String a : FixedPlanner.REQUIRED) {
            assertFalse(p.anchors(a).isEmpty(), a);
        }
        assertEquals(8, p.anchors("home").size(), "4 座小屋 × 2 床");
        assertEquals(1, p.anchors("priest_home").size());
        assertEquals(66, p.center().y(), "钟在广场中央石座上：地表上第一格 64 + 2");
        assertTrue(PlanValidator.validate(p, FixedPlanner.REQUIRED).isEmpty());
        // 坡地小屋的门都朝广场
        Pos guardian = p.anchors("guardian").get(0);
        assertTrue(guardian.z() < p.center().z(), "卫城在广场北");
    }

    @Test
    void tc24_deterministic() throws IOException {
        Map<String, BuildingSpec> s = specs();
        assertEquals(FixedPlanner.plan(5, 7, FLAT, s, "greek", "hades", new ArrayList<>()),
                FixedPlanner.plan(5, 7, FLAT, s, "greek", "hades", new ArrayList<>()));
    }

    @Test
    void tc25_waterRejected() throws IOException {
        // 广场东侧 20 格外是水：东边的小屋泡水
        Terrain t = new Terrain((x, z) -> x > 14 ? 63 : 64, (x, z) -> x > 14 ? 58 : 64);
        List<String> problems = new ArrayList<>();
        assertTrue(FixedPlanner.plan(0, 0, t, specs(), "greek", "zeus", problems).isEmpty());
        assertTrue(problems.contains("泡水"), problems.toString());
    }

    @Test
    void tc27_missingAnchor() throws IOException {
        VillagePlan p = FixedPlanner.plan(0, 0, FLAT, specs(), "greek", "zeus", new ArrayList<>()).orElseThrow();
        List<String> bad = PlanValidator.validate(p, List.of("center", "pottery"));
        assertEquals(List.of("缺件：锚点 pottery"), bad);
    }

    @Test
    void tc28_atMostThreeAttempts() throws IOException {
        Terrain.calls = 0;
        List<int[]> centers = List.of(new int[]{0, 0}, new int[]{100, 0}, new int[]{200, 0}, new int[]{300, 0});
        List<String> problems = new ArrayList<>();
        assertTrue(FixedPlanner.planAny(centers, CLIFF, specs(), "greek", "zeus", problems).isEmpty());
        assertEquals(3, problems.size(), problems.toString());
    }

    @Test
    void overlapAndRangeDetected() throws IOException {
        VillagePlan p = FixedPlanner.plan(0, 0, FLAT, specs(), "greek", "zeus", new ArrayList<>()).orElseThrow();
        List<VillagePlan.Placement> bs = new ArrayList<>(p.buildings());
        VillagePlan.Placement a = bs.get(0);
        bs.add(new VillagePlan.Placement("dup", a.template(), a.origin(), a.turn(), a.box()));
        bs.add(new VillagePlan.Placement("far", a.template(), a.origin(), a.turn(), new Box(200, 0, 0, 210, 5, 10)));
        VillagePlan q = new VillagePlan(p.civ(), p.patron(), p.layout(), p.layoutVersion(), p.center(), bs, p.anchors());
        List<String> bad = PlanValidator.validate(q, List.of());
        assertTrue(bad.stream().anyMatch(s -> s.startsWith("重叠")), bad.toString());
        assertTrue(bad.contains("出界：far"), bad.toString());
    }

    // ───────────── 2.6 已建范围 ─────────────

    @Test
    void tc29_builtArea() {
        BuiltArea a = new BuiltArea(List.of(new Box(0, 60, 0, 9, 70, 9), new Box(20, 60, 0, 29, 70, 9)));
        assertTrue(a.contains(5, 5));
        assertTrue(a.contains(13, 5), "外扩 4 格");
        assertTrue(a.contains(-4, -4));
        assertFalse(a.contains(-5, 5));
        assertFalse(a.contains(15, 15));
        assertTrue(a.within(-15, 5, 16));
    }

    @Test
    void tc30_chunkIndex() {
        // 外扩后 x、z 都覆盖 −4..43：区块 −1..2 → 16 个
        BuiltArea a = new BuiltArea(List.of(new Box(0, 0, 0, 39, 5, 39)));
        assertEquals(16, a.chunks().size());
        SpaceIndex<String> idx = new SpaceIndex<>();
        idx.put("v", a, new Pos(20, 64, 20));
        assertEquals(Optional.of("v"), idx.at(40, 40));
        assertEquals(Optional.empty(), idx.at(60, 60));
        assertEquals(Optional.empty(), idx.at(44, 44), "区块内但不在范围内");
        idx.remove("v");
        assertEquals(Optional.empty(), idx.at(10, 10));
    }

    @Test
    void tc31_overlapPicksNearer() {
        SpaceIndex<String> idx = new SpaceIndex<>();
        idx.put("a", new BuiltArea(List.of(new Box(0, 0, 0, 30, 5, 30))), new Pos(0, 64, 0));
        idx.put("b", new BuiltArea(List.of(new Box(20, 0, 0, 60, 5, 30))), new Pos(60, 64, 0));
        assertEquals(Optional.of("a"), idx.at(25, 5));
        assertEquals(Optional.of("b"), idx.at(31, 5));
    }
}
