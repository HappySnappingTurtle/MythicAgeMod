package io.github.happysnappingturtle.mythicage.core.village;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.happysnappingturtle.mythicage.core.village.economy.Rations;
import io.github.happysnappingturtle.mythicage.core.village.recipe.FuelTable;
import io.github.happysnappingturtle.mythicage.core.village.recipe.VillageRecipe;
import io.github.happysnappingturtle.mythicage.core.village.time.Schedule;
import io.github.happysnappingturtle.mythicage.core.village.time.VillageCalendar;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 一个文明的村庄经济数据 {@code village_civ/<civ>.json}（T19 设计文档 §2、§6、§9、§10.1）：
 * 行为模块、作息与历法、仓储、口粮、燃料、采集点种类、作物、初始居民与名字表、S0 初始库存。
 * 职业与配方另有文件（{@code village_profession/<civ>/*}、{@code village_recipe/<civ>/*}）。
 */
public record CivVillageData(String civ, String behavior, Schedule schedule, VillageCalendar calendar, int baseCapacity,
                             int storageUnit, Rations rations, FuelTable fuel, Map<String, PointKind> points,
                             Map<String, CropKind> crops, Residents residents, Map<String, Integer> initialStock,
                             Map<String, Profession> professions, Map<String, VillageRecipe> recipes) {
    /**
     * 采集点种类（建筑锚点同名）。
     *
     * @param take   每次采集消耗的余量
     * @param perDay 一名居民每 MC 日能采几次（离线补算用）
     */
    public record PointKind(String id, Map<String, Integer> outputs, int take, int max, int recover, String anim,
                            int ticks, Set<String> professions, double perDay, boolean outdoor) {
    }

    /**
     * 作物（建筑锚点同名）：真实采收按方块掉落结算；{@code expected} 与 {@code perAnchorPerDay} 只给离线补算用。
     *
     * @param batch  一份工作单最多收几格
     * @param replant 补种扣掉的种子
     */
    public record CropKind(String id, String anim, int ticksPerCrop, int batch, String replant,
                           Map<String, Integer> expected, double perAnchorPerDay, Set<String> professions) {
    }

    /**
     * @param initial 生成时按职业分配的人数（其余为 {@code fallback}）
     * @param keeper  谷仓管事的职业（D4：村务界面入口）
     */
    public record Residents(Map<String, Integer> initial, String fallback, String keeper, List<String> male,
                            List<String> female) {
    }

    /** @param jobs 能做的工作单类型（基础工作另外任何人都能做） */
    public record Profession(String id, Set<String> jobs, String stage, boolean special) {
    }

    public CivVillageData {
        points = Map.copyOf(points);
        crops = Map.copyOf(crops);
        initialStock = Map.copyOf(initialStock);
        professions = Map.copyOf(professions);
        recipes = Map.copyOf(recipes);
    }

    public CivVillageData withProfessions(Map<String, Profession> p, Map<String, VillageRecipe> r) {
        return new CivVillageData(civ, behavior, schedule, calendar, baseCapacity, storageUnit, rations, fuel, points, crops,
                residents, initialStock, p, r);
    }

    /** 生成时 {@code homes} 张床的职业分配：先按 {@code initial} 的顺序，床不够就截断，多出来的是 {@code fallback}。 */
    public List<String> initialProfessions(int homes) {
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, Integer> e : residents.initial().entrySet()) {
            for (int i = 0; i < e.getValue() && out.size() < homes; i++) {
                out.add(e.getKey());
            }
        }
        while (out.size() < homes) {
            out.add(residents.fallback());
        }
        return out;
    }

    public int capacity(int storageBuildings) {
        return baseCapacity + storageUnit * Math.max(0, storageBuildings);
    }

    public static CivVillageData parse(String civ, JsonObject o) {
        Map<String, PointKind> points = new LinkedHashMap<>();
        if (o.has("points")) {
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("points").entrySet()) {
                JsonObject p = e.getValue().getAsJsonObject();
                points.put(e.getKey(), new PointKind(e.getKey(), VillageRecipe.items(p, "outputs"), integer(p, "take", 1),
                        integer(p, "max", 48), integer(p, "recover", 0), str(p, "anim", "dig"), integer(p, "ticks", 30),
                        set(p, "professions"), dbl(p, "perDay", 4), !p.has("outdoor") || p.get("outdoor").getAsBoolean()));
            }
        }
        Map<String, CropKind> crops = new LinkedHashMap<>();
        if (o.has("crops")) {
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("crops").entrySet()) {
                JsonObject c = e.getValue().getAsJsonObject();
                crops.put(e.getKey(), new CropKind(e.getKey(), str(c, "anim", "harvest_bend"), integer(c, "ticks", 10),
                        integer(c, "batch", 8), str(c, "replant", ""), VillageRecipe.items(c, "expected"),
                        dbl(c, "perAnchorPerDay", 0.5), set(c, "professions")));
            }
        }
        JsonObject r = o.has("residents") ? o.getAsJsonObject("residents") : new JsonObject();
        Map<String, Integer> initial = VillageRecipe.items(r, "initial");
        Residents residents = new Residents(initial, str(r, "fallback", civ + ":laborer"), str(r, "keeper", ""),
                list(r, "male"), list(r, "female"));
        JsonObject storage = o.has("storage") ? o.getAsJsonObject("storage") : new JsonObject();
        return new CivVillageData(civ, str(o, "behavior", ""),
                o.has("schedule") ? Schedule.parse(o.getAsJsonObject("schedule")) : Schedule.simple(),
                o.has("calendar") ? VillageCalendar.parse(o.getAsJsonObject("calendar")) : VillageCalendar.none(),
                integer(storage, "base", 256), integer(storage, "unit", 512),
                o.has("rations") ? Rations.parse(o.getAsJsonObject("rations")) : Rations.none(),
                new FuelTable(VillageRecipe.items(o, "fuel")), points, crops, residents,
                VillageRecipe.items(o, "initialStock"), Map.of(), Map.of());
    }

    public static Profession parseProfession(String id, JsonObject o) {
        return new Profession(id, set(o, "jobs"), str(o, "stage", "archaic"), o.has("special") && o.get("special").getAsBoolean());
    }

    private static int integer(JsonObject o, String k, int d) {
        return o.has(k) ? o.get(k).getAsInt() : d;
    }

    private static double dbl(JsonObject o, String k, double d) {
        return o.has(k) ? o.get(k).getAsDouble() : d;
    }

    private static String str(JsonObject o, String k, String d) {
        return o.has(k) ? o.get(k).getAsString() : d;
    }

    private static Set<String> set(JsonObject o, String k) {
        Set<String> s = new TreeSet<>();
        if (o.has(k)) {
            o.getAsJsonArray(k).forEach(e -> s.add(e.getAsString()));
        }
        return s;
    }

    private static List<String> list(JsonObject o, String k) {
        List<String> s = new ArrayList<>();
        if (o.has(k)) {
            o.getAsJsonArray(k).forEach(e -> s.add(e.getAsString()));
        }
        return List.copyOf(s);
    }
}
