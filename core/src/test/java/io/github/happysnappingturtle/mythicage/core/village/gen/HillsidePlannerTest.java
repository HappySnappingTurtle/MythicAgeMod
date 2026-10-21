package io.github.happysnappingturtle.mythicage.core.village.gen;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TC-T13b-18～24：山坡规划器（阶段 ②）。 */
class HillsidePlannerTest {
    static final VillageGenTest.Terrain FLAT = VillageGenTest.FLAT;
    static final VillageGenTest.Terrain CONE = VillageGenTest.Terrain.dry(
            (x, z) -> 64 + (int) Math.max(0, 14 - Math.hypot(x, z) / 5));
    static final VillageGenTest.Terrain SLOPE = VillageGenTest.Terrain.dry((x, z) -> 64 + Math.floorDiv(x, 5));
    /** x = 34..40 一条 7 格宽的河，水面比岸低 1 格。 */
    static final VillageGenTest.Terrain RIVER = new VillageGenTest.Terrain(
            (x, z) -> x >= 34 && x <= 40 ? 64 : 65, (x, z) -> x >= 34 && x <= 40 ? 60 : 65);
    /** z ≥ 40 是海，海面比陆地低 1 格。 */
    static final VillageGenTest.Terrain COAST = new VillageGenTest.Terrain(
            (x, z) -> z >= 40 ? 64 : 65, (x, z) -> z >= 40 ? 55 : 65);

    static Map<String, BuildingSpec> specs() throws IOException {
        return VillageGenTest.specs();
    }

    static VillagePlan plan(VillageGenTest.Terrain t, int x, int z, String patron) throws IOException {
        List<String> problems = new ArrayList<>();
        return HillsidePlanner.plan(x, z, t, specs(), "greek", patron, problems)
                .orElseThrow(() -> new AssertionError("规划失败：" + problems));
    }

    static long count(VillagePlan p, String building) {
        return p.buildings().stream().filter(b -> b.building().equals("greek:" + building)).count();
    }

    /** 卫城有按主神的变体（acropolis_archaic_zeus 等），按前缀找。 */
    static VillagePlan.Placement one(VillagePlan p, String building) {
        return p.buildings().stream().filter(b -> b.building().equals("greek:" + building)
                || building.equals("acropolis_archaic") && b.building().startsWith("greek:acropolis_archaic_"))
                .findFirst().orElseThrow();
    }

    @Test
    void tc18_cone() throws IOException {
        VillagePlan p = plan(CONE, 10, -5, "zeus");
        VillagePlan.Placement acro = one(p, "acropolis_archaic");
        assertTrue(Math.hypot(acro.box().centerX(), acro.box().centerZ()) <= 24, "神庙靠近山顶：" + acro.box());
        assertEquals(Turn.R0, acro.turn(), "门朝东（模板原朝向）");
        VillagePlan.Placement agora = one(p, "agora");
        assertTrue(agora.box().minY() <= acro.box().minY(), "广场不高于卫城");
        assertTrue(count(p, "house_small") >= 4, "≥ 4 座小屋");
        assertEquals(1, count(p, "granary"));
        assertEquals(1, count(p, "pottery"));
        Set<String> kinds = p.plots().stream().map(x -> x.stage() + ":" + x.kind()).collect(Collectors.toSet());
        for (HillsidePlanner.PlotSpec ps : HillsidePlanner.PLOTS) {
            if (!ps.optional()) {
                assertTrue(kinds.contains(ps.stage() + ":" + ps.kind()), "缺地块 " + ps);
            }
        }
        assertTrue(kinds.contains("classical:temple_ext") && kinds.contains("mythic:altar"));
        for (String a : HillsidePlanner.REQUIRED) {
            assertFalse(p.anchors(a).isEmpty(), a);
        }
    }

    @Test
    void tc19_slopeTerraces() throws IOException {
        VillagePlan p = plan(SLOPE, 0, 0, "zeus");
        Set<Integer> levels = p.buildings().stream().map(b -> b.box().minY()).collect(Collectors.toSet());
        assertTrue(levels.size() >= 3, "坡地上建筑分在多个高度：" + levels);
        // 每座建筑占地的地面高差 ≤ 允许值（规划时已保证），这里抽查小屋：模板底面与网格地面相差不超过 6
        for (VillagePlan.Placement b : p.buildings()) {
            if (b.building().endsWith("house_small")) {
                int ground = SLOPE.surface(b.box().centerX(), b.box().centerZ());
                assertTrue(Math.abs(b.box().minY() + 1 - ground) <= 6, b.box() + " / " + ground);
            }
        }
    }

    @Test
    void tc20_flat() throws IOException {
        VillagePlan p = plan(FLAT, 0, 0, "hades");
        assertTrue(p.buildings().stream().allMatch(b -> b.box().minY() >= 63), "平地不造台地");
        assertTrue(count(p, "hades_cemetery") == 1, "哈迪斯墓园");
    }

    @Test
    void tc21_river() throws IOException {
        VillagePlan p = plan(RIVER, 0, 0, "hades");
        for (VillagePlan.Placement b : p.buildings()) {
            if (b.building().contains("bridge")) {
                continue;
            }
            boolean overRiver = b.box().maxX() >= 34 && b.box().minX() <= 40;
            assertFalse(overRiver && !b.building().contains("harbor"), "建筑压在河上：" + b.building() + " " + b.box());
        }
    }

    @Test
    void tc22_rangeAndOverlap() throws IOException {
        for (VillageGenTest.Terrain t : List.of(FLAT, CONE, SLOPE)) {
            VillagePlan p = plan(t, 0, 0, "zeus");
            assertTrue(PlanValidator.validate(p, HillsidePlanner.REQUIRED).isEmpty());
            List<Box> all = new ArrayList<>(p.builtBoxes());
            p.plots().forEach(x -> all.add(x.box()));
            for (int i = 0; i < all.size(); i++) {
                for (int j = i + 1; j < all.size(); j++) {
                    assertFalse(all.get(i).intersectsXZ(all.get(j)), all.get(i) + " / " + all.get(j));
                }
            }
            assertFalse(p.paths().isEmpty(), "有道路");
        }
    }

    @Test
    void tc23_patrons() throws IOException {
        assertEquals(1, count(plan(FLAT, 0, 0, "zeus"), "zeus_oak_archaic"));
        assertEquals(1, count(plan(FLAT, 0, 0, "poseidon"), "poseidon_spring"), "不临海：海神泉");
        VillagePlan coast = plan(COAST, 0, -10, "poseidon");
        assertEquals(1, count(coast, "poseidon_harbor") + count(coast, "poseidon_spring"));
        assertEquals(1, count(coast, "poseidon_harbor"), "临海：码头");
        VillagePlan hades = plan(FLAT, 0, 0, "hades");
        Pos e = hades.anchors("entrance").get(0);
        VillagePlan.Placement cem = one(hades, "hades_cemetery");
        assertTrue(Math.hypot(cem.box().centerX() - e.x(), cem.box().centerZ() - e.z()) <= 40, "墓园在村口附近");
    }

    @Test
    void tc24_deterministic() throws IOException {
        assertEquals(plan(CONE, 3, 7, "hades"), plan(CONE, 3, 7, "hades"));
    }

    @Test
    void sampleBudget() throws IOException {
        VillageGenTest.Terrain.calls = 0;
        plan(CONE, 0, 0, "zeus");
        assertTrue(VillageGenTest.Terrain.calls <= 1400, "规划采样 " + VillageGenTest.Terrain.calls); // 网格 19 × 19 × 2 + 候选位置的较准检查（每块地 5 点）
    }

    @Test
    void debugDump() throws IOException {
        for (String patron : List.of("hades", "poseidon", "zeus")) {
            List<String> problems = new ArrayList<>();
            var p = HillsidePlanner.plan(0, 0, FLAT, specs(), "greek", patron, problems);
            System.out.println("=== " + patron + " " + problems);
            p.ifPresent(x -> System.out.println(PlanAscii.render(x, 0, 0, 100)));
        }
    }
}
