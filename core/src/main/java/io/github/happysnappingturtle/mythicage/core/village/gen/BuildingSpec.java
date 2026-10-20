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
 *
 * @param floorY 体素 y = 0（地面上第一层）在模板里的高度；模板原点 y = 地表上第一格 − floorY
 */
public record BuildingSpec(String id, String civ, String template, int sizeX, int sizeY, int sizeZ, int floorY,
                           String category, String stage, boolean generated, Pos door, String doorFacing,
                           Map<String, List<Pos>> anchors, Map<String, Integer> provides) {
    public BuildingSpec {
        Map<String, List<Pos>> a = new LinkedHashMap<>();
        anchors.forEach((k, v) -> a.put(k, List.copyOf(v)));
        anchors = java.util.Collections.unmodifiableMap(a);
        provides = Map.copyOf(provides);
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
                o.has("generated") && o.get("generated").getAsBoolean(), door, facing, anchors, provides);
    }

    private static Pos pos(JsonArray a) {
        return new Pos(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt());
    }

    public List<Pos> anchors(String name) {
        return anchors.getOrDefault(name, List.of());
    }
}
