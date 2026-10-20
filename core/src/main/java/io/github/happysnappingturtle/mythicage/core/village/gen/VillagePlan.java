package io.github.happysnappingturtle.mythicage.core.village.gen;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 规划器的输出（V0 §3.5）：放哪些建筑、锚点在哪、已建范围。地块（plots）在阶段 ② 加入。
 *
 * @param center 村庄中心（聚会钟所在格）
 */
public record VillagePlan(String civ, String patron, String layout, int layoutVersion, Pos center,
                          List<Placement> buildings, Map<String, List<Pos>> anchors, List<Plot> plots, List<Path> paths) {
    public VillagePlan(String civ, String patron, String layout, int layoutVersion, Pos center, List<Placement> buildings,
                       Map<String, List<Pos>> anchors) {
        this(civ, patron, layout, layoutVersion, center, buildings, anchors, List.of(), List.of());
    }

    public VillagePlan {
        buildings = List.copyOf(buildings);
        plots = List.copyOf(plots);
        paths = List.copyOf(paths);
        Map<String, List<Pos>> a = new LinkedHashMap<>();
        anchors.forEach((k, v) -> a.put(k, List.copyOf(v)));
        anchors = java.util.Collections.unmodifiableMap(a);
    }

    /** 一座建筑：建筑 id、模板原点、旋转、放置后的世界包围盒。 */
    public record Placement(String building, String template, Pos origin, Turn turn, Box box) {
    }

    /**
     * 预留地块（V0 §5.2）：生成时整平、四角立界桩，未建成前不算村里。
     *
     * @param id     村内唯一，如 {@code classical_shrine_1}
     * @param kind   地块类别（shrine、market、workshop、house、storage、training、theater、altar、feature、temple_ext）
     * @param stage  最早可用的村庄阶段（classical / heroic / mythic）
     * @param box    占地；y 是整平后地面上第一格
     * @param front  正面朝向（朝广场）
     */
    public record Plot(String id, String kind, String stage, Box box, String front) {
    }

    /** 道路：折线的拐点（y 是规划时的地面估计，铺路时以实际地表为准），宽度 2 或 3。 */
    public record Path(int width, List<Pos> points) {
        public Path {
            points = List.copyOf(points);
        }
    }

    /** 已建范围的原始包围盒（未外扩）：全部建筑（不含预留地块与道路）。 */
    public List<Box> builtBoxes() {
        return buildings.stream().map(Placement::box).toList();
    }

    public VillagePlan withPatron(String p) {
        return new VillagePlan(civ, p, layout, layoutVersion, center, buildings, anchors, plots, paths);
    }

    public List<Pos> anchors(String name) {
        return anchors.getOrDefault(name, List.of());
    }
}
