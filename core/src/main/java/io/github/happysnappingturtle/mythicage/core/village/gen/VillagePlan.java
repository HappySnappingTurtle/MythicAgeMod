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
                          List<Placement> buildings, Map<String, List<Pos>> anchors) {
    public VillagePlan {
        buildings = List.copyOf(buildings);
        Map<String, List<Pos>> a = new LinkedHashMap<>();
        anchors.forEach((k, v) -> a.put(k, List.copyOf(v)));
        anchors = java.util.Collections.unmodifiableMap(a);
    }

    /** 一座建筑：建筑 id、模板原点、旋转、放置后的世界包围盒。 */
    public record Placement(String building, String template, Pos origin, Turn turn, Box box) {
    }

    /** 已建范围的原始包围盒（未外扩）：全部建筑。 */
    public List<Box> builtBoxes() {
        return buildings.stream().map(Placement::box).toList();
    }

    public List<Pos> anchors(String name) {
        return anchors.getOrDefault(name, List.of());
    }
}
