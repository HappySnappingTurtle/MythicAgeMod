package io.github.happysnappingturtle.mythicage.core.village.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 村庄加工配方 {@code village_recipe/<civ>/<id>.json}（T19 设计文档 §11.1，V1 §5.2，V3-希腊-居民 §4.1）。
 * <pre>
 * {"station": "mythic_age:pottery_kiln", "professions": ["greek:potter"],
 *  "inputs": {"mythic_age:raw_roof_tile": 4}, "fuel": 1, "outputs": {"mythic_age:ceramic_component": 4},
 *  "stages": [{"anim": "kiln_load", "ticks": 80}, {"anim": "kiln_unload", "ticks": 160}],
 *  "unattendedDays": 0, "stage": "archaic", "outdoor": false}
 * </pre>
 * {@code unattendedDays > 0} 是无人值守段（H8：酒瓮发酵）：开始时托管输入并占用工作站，到期后由任何居民取出入库。
 *
 * @param station 工作站方块 id；采集点上的加工（粗打砌石）写 {@code point:<种类>}
 * @param fuel    燃料单位（见 {@link FuelTable}）
 * @param stage   村庄阶段（archaic / classical / heroic / mythic）
 */
public record VillageRecipe(String id, String civ, String station, Set<String> professions, Map<String, Integer> inputs,
                            int fuel, Map<String, Integer> outputs, List<Stage> stages, int unattendedDays, String stage,
                            boolean outdoor, double weight) {
    public record Stage(String anim, int ticks) {
    }

    public VillageRecipe {
        professions = Set.copyOf(professions);
        inputs = Map.copyOf(inputs);
        outputs = Map.copyOf(outputs);
        stages = List.copyOf(stages);
        if (outputs.isEmpty()) {
            throw new IllegalArgumentException("配方 " + id + " 没有产物");
        }
        if (stages.isEmpty() && unattendedDays <= 0) {
            throw new IllegalArgumentException("配方 " + id + " 没有动作段");
        }
    }

    public int totalTicks() {
        return stages.stream().mapToInt(Stage::ticks).sum();
    }

    public boolean unattended() {
        return unattendedDays > 0;
    }

    public static VillageRecipe parse(String id, String civ, JsonObject o) {
        Set<String> prof = new TreeSet<>();
        if (o.has("professions")) {
            o.getAsJsonArray("professions").forEach(e -> prof.add(e.getAsString()));
        }
        List<Stage> st = new ArrayList<>();
        if (o.has("stages")) {
            for (JsonElement e : o.getAsJsonArray("stages")) {
                JsonObject s = e.getAsJsonObject();
                st.add(new Stage(s.get("anim").getAsString(), s.get("ticks").getAsInt()));
            }
        }
        return new VillageRecipe(id, civ, o.get("station").getAsString(), prof, items(o, "inputs"),
                o.has("fuel") ? o.get("fuel").getAsInt() : 0, items(o, "outputs"), st,
                o.has("unattendedDays") ? o.get("unattendedDays").getAsInt() : 0,
                o.has("stage") ? o.get("stage").getAsString() : "archaic",
                o.has("outdoor") && o.get("outdoor").getAsBoolean(),
                o.has("weight") ? o.get("weight").getAsDouble() : 1);
    }

    public static Map<String, Integer> items(JsonObject o, String key) {
        Map<String, Integer> m = new LinkedHashMap<>();
        if (o.has(key)) {
            o.getAsJsonObject(key).entrySet().forEach(e -> m.put(e.getKey(), e.getValue().getAsInt()));
        }
        return m;
    }
}
