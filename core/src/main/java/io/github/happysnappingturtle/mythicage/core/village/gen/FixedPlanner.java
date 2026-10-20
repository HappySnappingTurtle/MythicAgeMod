package io.github.happysnappingturtle.mythicage.core.village.gen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 固定组合的规划器（T13b 阶段 ①，只为验证生成管线）：广场居中，卫城在北（门朝东），
 * 广场东西各一座、南面两座坡地小屋（门都朝广场），村口在南。阶段 ② 由 {@code HillsidePlanner} 取代。
 */
public final class FixedPlanner {
    public static final String ID = "fixed";
    public static final int VERSION = 1;
    public static final List<String> REQUIRED = List.of("center", "guardian", "home", "entrance");

    private FixedPlanner() {
    }

    /** 失败原因放在 {@code problems} 里（调试与验收工具用）。 */
    public static Optional<VillagePlan> plan(int ax, int az, TerrainSampler terrain, Map<String, BuildingSpec> specs,
                                             String civ, String patron, List<String> problems) {
        BuildingSpec agora = spec(specs, civ + ":agora");
        BuildingSpec acropolis = spec(specs, civ + ":acropolis_archaic");
        BuildingSpec house = spec(specs, civ + ":house_small");
        PlanBuilder b = new PlanBuilder(terrain);
        try {
            b.place(agora, ax - 7, az - 7, Turn.R0);
            int[] acro = TemplateTransform.rotatedSize(acropolis.sizeX(), acropolis.sizeZ(), Turn.R0);
            b.place(acropolis, ax - acro[0] / 2, az - 7 - 5 - acro[1], Turn.R0);
            Turn west = Turn.between(house.doorFacing(), "east");
            Turn east = Turn.between(house.doorFacing(), "west");
            Turn south = Turn.between(house.doorFacing(), "north");
            int[] side = TemplateTransform.rotatedSize(house.sizeX(), house.sizeZ(), west);
            int[] front = TemplateTransform.rotatedSize(house.sizeX(), house.sizeZ(), south);
            b.place(house, ax - 7 - 4 - side[0], az - side[1] / 2, west);
            b.place(house, ax + 7 + 4, az - side[1] / 2, east);
            b.place(house, ax - 2 - front[0], az + 7 + 4, south);
            b.place(house, ax + 2, az + 7 + 4, south);
            int ez = az + 7 + 4 + front[1] + 4;
            b.anchor("entrance", new Pos(ax, b.surface(ax, ez), ez));
            VillagePlan plan = b.build(civ, patron, ID, VERSION);
            List<String> bad = PlanValidator.validate(plan, REQUIRED);
            if (!bad.isEmpty()) {
                problems.addAll(bad);
                return Optional.empty();
            }
            return Optional.of(plan);
        } catch (PlanBuilder.Rejected e) {
            problems.add(e.getMessage());
            return Optional.empty();
        }
    }

    private static BuildingSpec spec(Map<String, BuildingSpec> specs, String id) {
        BuildingSpec s = specs.get(id);
        if (s == null) {
            throw new IllegalStateException("缺少建筑数据：" + id);
        }
        return s;
    }

    /** 按选址结果的顺序最多试 3 个中心（V0 §3.8）。 */
    public static Optional<VillagePlan> planAny(List<int[]> centers, TerrainSampler terrain, Map<String, BuildingSpec> specs,
                                                String civ, String patron, List<String> problems) {
        List<String> all = new ArrayList<>();
        for (int i = 0; i < Math.min(3, centers.size()); i++) {
            List<String> p = new ArrayList<>();
            Optional<VillagePlan> plan = plan(centers.get(i)[0], centers.get(i)[1], terrain, specs, civ, patron, p);
            if (plan.isPresent()) {
                return plan;
            }
            all.addAll(p);
        }
        problems.addAll(all);
        return Optional.empty();
    }
}
