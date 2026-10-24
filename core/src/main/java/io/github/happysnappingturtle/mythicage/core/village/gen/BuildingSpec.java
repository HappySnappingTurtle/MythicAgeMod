package io.github.happysnappingturtle.mythicage.core.village.gen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 建筑数据 {@code village_building/<civ>/<id>.json}（V0 §6.2，T13b §1）：由 tools/buildings 从白模体素导出。
 * 锚点与门都是相对模板原点（体素包围盒最小角）的坐标。
 * <p>
 * 施工建筑（{@code generated = false}，T19 设计文档 §1.4 E3）另有：优先级、是否每村唯一、原地替换的建筑 id 前缀、
 * 次神（{@code "*"} 是通用祠）、前置条件、主神变体、本建筑的折算规则。
 *
 * @param floorY    体素 y = 0（地面上第一层）在模板里的高度；模板原点 y = 地表上第一格 − floorY
 * @param frame     模板原点在白模坐标系里的位置（原地替换时对齐新旧模板）
 * @param priority  越小越先建
 * @param replace   可被本建筑原地替换的建筑 id 前缀
 * @param requires  前置条件：{@code anchor:<锚点>}、{@code building:<建筑 id>}
 * @param patron    主神变体（不单独作为施工候选，按村庄主神挑）；空串表示通用
 * @param buildCosts 本建筑的折算规则（JSON 原样保留，排在文明折算表前面）
 * @param jobs      本建筑提供的岗位：职业 → 人数（T19 §1.6 H1，移民按空缺分职业）
 */
public record BuildingSpec(String id, String civ, String template, int sizeX, int sizeY, int sizeZ, int floorY,
                           String category, String stage, boolean generated, Pos door, String doorFacing,
                           Map<String, List<Pos>> anchors, Map<String, Integer> provides, List<String> plotKinds,
                           Pos frame, int priority, boolean unique, List<String> replace, String god, List<String> requires,
                           String patron, JsonArray buildCosts, Map<String, Integer> jobs) {
    public BuildingSpec {
        Map<String, List<Pos>> a = new LinkedHashMap<>();
        anchors.forEach((k, v) -> a.put(k, List.copyOf(v)));
        anchors = java.util.Collections.unmodifiableMap(a);
        provides = Map.copyOf(provides);
        plotKinds = List.copyOf(plotKinds);
        replace = List.copyOf(replace);
        requires = List.copyOf(requires);
        jobs = Map.copyOf(jobs);
    }

    public static BuildingSpec parse(JsonObject o) {
        JsonArray size = o.getAsJsonArray("size");
        Map<String, List<Pos>> anchors = new LinkedHashMap<>();
        if (o.has("anchors")) {
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("anchors").entrySet()) {
                List<Pos> pts = new ArrayList<>();
                for (JsonElement p : e.getValue().getAsJsonArray()) {
                    pts.add(pos(p.getAsJsonArray()));
                }
                anchors.put(e.getKey(), pts);
            }
        }
        Map<String, Integer> provides = new LinkedHashMap<>();
        if (o.has("provides")) {
            o.getAsJsonObject("provides").entrySet().forEach(e -> provides.put(e.getKey(), e.getValue().getAsInt()));
        }
        Pos door = new Pos(0, 0, 0);
        String facing = "south";
        if (o.has("door")) {
            JsonObject d = o.getAsJsonObject("door");
            door = pos(d.getAsJsonArray("pos"));
            facing = d.get("facing").getAsString();
        }
        return new BuildingSpec(o.get("id").getAsString(), o.get("civ").getAsString(), o.get("template").getAsString(),
                size.get(0).getAsInt(), size.get(1).getAsInt(), size.get(2).getAsInt(),
                o.has("floor_y") ? o.get("floor_y").getAsInt() : 0,
                o.has("category") ? o.get("category").getAsString() : "",
                o.has("stage") ? o.get("stage").getAsString() : "archaic",
                o.has("generated") && o.get("generated").getAsBoolean(), door, facing, anchors, provides,
                strings(o, "plot_kinds"), o.has("frame") ? pos(o.getAsJsonArray("frame")) : new Pos(0, 0, 0),
                o.has("priority") ? o.get("priority").getAsInt() : 100,
                !o.has("unique") || o.get("unique").getAsBoolean(), strings(o, "replace"),
                o.has("god") ? o.get("god").getAsString() : "", strings(o, "requires"),
                o.has("patron") ? o.get("patron").getAsString() : "",
                o.has("buildCosts") ? o.getAsJsonArray("buildCosts") : new JsonArray(), counts(o, "jobs"));
    }

    private static Pos pos(JsonArray a) {
        return new Pos(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt());
    }

    private static Map<String, Integer> counts(JsonObject o, String k) {
        Map<String, Integer> out = new LinkedHashMap<>();
        if (o.has(k)) {
            o.getAsJsonObject(k).entrySet().forEach(e -> out.put(e.getKey(), e.getValue().getAsInt()));
        }
        return out;
    }

    private static List<String> strings(JsonObject o, String k) {
        List<String> out = new ArrayList<>();
        if (o.has(k)) {
            o.getAsJsonArray(k).forEach(e -> out.add(e.getAsString()));
        }
        return out;
    }

    public List<Pos> anchors(String name) {
        return anchors.getOrDefault(name, List.of());
    }

    /** 不带主神后缀的短名：{@code greek:acropolis_classical_zeus} → {@code acropolis_classical}。 */
    public String shortBase() {
        String s = id.substring(id.indexOf(':') + 1);
        return patron.isEmpty() ? s : s.substring(0, s.length() - patron.length() - 1);
    }

    /**
     * 按主神挑建筑变体（T13b 阶段 ③：卫城山花楣饰随主神变）：有 {@code <civ>:<shortId>_<patron>} 就用它，
     * 否则用 {@code <civ>:<shortId>}；都没有返回 null。
     */
    public static BuildingSpec forPatron(java.util.Map<String, BuildingSpec> specs, String civ, String shortId,
                                         String patron) {
        BuildingSpec v = patron == null || patron.isEmpty() ? null : specs.get(civ + ":" + shortId + "_" + patron);
        return v != null ? v : specs.get(civ + ":" + shortId);
    }
}
