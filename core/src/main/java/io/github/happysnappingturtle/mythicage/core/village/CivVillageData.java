package io.github.happysnappingturtle.mythicage.core.village;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.happysnappingturtle.mythicage.core.village.build.BuildCosts;
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
 * 行为模块、作息与历法、仓储、口粮、燃料、采集点种类、作物、初始居民与名字表、S0 初始库存、施工折算表（§1.4 E1）。
 * 职业与配方另有文件（{@code village_profession/<civ>/*}、{@code village_recipe/<civ>/*}）。
 */
public record CivVillageData(String civ, String behavior, Schedule schedule, VillageCalendar calendar, int baseCapacity,
                             int storageUnit, Rations rations, FuelTable fuel, Map<String, PointKind> points,
                             Map<String, CropKind> crops, Map<String, FruitKind> fruits, List<StationSearch> stations,
                             Woodland woodland, Map<String, NaturalPoint> naturalPoints, Residents residents,
                             Map<String, Integer> initialStock, Map<String, Profession> professions,
                             Map<String, VillageRecipe> recipes, BuildCosts buildCosts) {
    /**
     * 采集点种类（建筑锚点同名）。
     *
     * @param take   每次采集消耗的余量
     * @param perDay 一名居民每 MC 日能采几次（离线补算用）
     * @param anchor 登记在哪种建筑锚点上（默认与种类同名；如修枝登记在果树锚点上）
     */
    public record PointKind(String id, Map<String, Integer> outputs, int take, int max, int recover, String anim,
                            int ticks, Set<String> professions, double perDay, boolean outdoor, String anchor) {
    }

    /**
     * 作物（建筑锚点同名）：真实采收按方块掉落结算；{@code expected} 与 {@code perAnchorPerDay} 只给离线补算用。
     *
     * @param batch  一份工作单最多收几格
     * @param replant 补种扣掉的种子
     */
    public record CropKind(String id, String anim, int ticksPerCrop, int batch, String replant,
                           Map<String, Integer> expected, double perAnchorPerDay, Set<String> professions,
                           String thresh, String threshAnim, int threshTicks) {
        /** 割下之后要不要搬到打谷场扬场（V3-希腊-居民 §3.1）。 */
        public boolean threshed() {
            return !thresh.isEmpty();
        }
    }

    /**
     * 果树、葡萄架（建筑锚点同名）：哪些果子熟了、怎么采，由代码里登记的采果器（{@code harvester}）决定。
     *
     * @param ticks 每处（一棵树、一段葡萄架）的作业时长
     */
    public record FruitKind(String id, String harvester, String anim, int ticks, Set<String> professions,
                            Map<String, Integer> expected, double perAnchorPerDay, String carry) {
    }

    /**
     * 工作站搜索：在这些锚点周围 {@code radius} 格内找工作站方块（锚点通常是站位，工作站在旁边）。
     *
     * @param blocks 只认这些方块；空表示任何配方用到的工作站
     */
    public record StationSearch(String anchor, int radius, Set<String> blocks) {
    }

    /**
     * 登记林地（H5、D5）：村外 {@code radius} 格内的天然树木，最多 {@code max} 株；伐倒后补种树苗。
     *
     * @param perDay 一名居民每 MC 日能伐几棵（离线补算）
     */
    public record Woodland(int radius, int max, String anim, int ticks, Set<String> professions, double perDay,
                           int logsPerTree) {
    }

    /**
     * 村边天然资源点（D6）：先找露出地面的 {@code block}；找不到且 {@code outcrop} 时在资源区放一处 3×3×2 的露头。
     * 余量、产物等按 points 里同名的种类。
     */
    public record NaturalPoint(String kind, String block, int radius, boolean outcrop) {
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
        fruits = Map.copyOf(fruits);
        stations = List.copyOf(stations);
        naturalPoints = Map.copyOf(naturalPoints);
        initialStock = Map.copyOf(initialStock);
        professions = Map.copyOf(professions);
        recipes = Map.copyOf(recipes);
    }

    public CivVillageData withProfessions(Map<String, Profession> p, Map<String, VillageRecipe> r) {
        return new CivVillageData(civ, behavior, schedule, calendar, baseCapacity, storageUnit, rations, fuel, points, crops,
                fruits, stations, woodland, naturalPoints, residents, initialStock, p, r, buildCosts);
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
                        set(p, "professions"), dbl(p, "perDay", 4), !p.has("outdoor") || p.get("outdoor").getAsBoolean(),
                        str(p, "anchor", e.getKey())));
            }
        }
        Map<String, CropKind> crops = new LinkedHashMap<>();
        if (o.has("crops")) {
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("crops").entrySet()) {
                JsonObject c = e.getValue().getAsJsonObject();
                crops.put(e.getKey(), new CropKind(e.getKey(), str(c, "anim", "harvest_bend"), integer(c, "ticks", 10),
                        integer(c, "batch", 8), str(c, "replant", ""), VillageRecipe.items(c, "expected"),
                        dbl(c, "perAnchorPerDay", 0.5), set(c, "professions"), str(c, "thresh", ""),
                        str(c, "threshAnim", "winnow"), integer(c, "threshTicks", 200)));
            }
        }
        Map<String, FruitKind> fruits = new LinkedHashMap<>();
        if (o.has("fruits")) {
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("fruits").entrySet()) {
                JsonObject f = e.getValue().getAsJsonObject();
                fruits.put(e.getKey(), new FruitKind(e.getKey(), str(f, "harvester", e.getKey()), str(f, "anim", "pick"),
                        integer(f, "ticks", 40), set(f, "professions"), VillageRecipe.items(f, "expected"),
                        dbl(f, "perAnchorPerDay", 0.3), str(f, "carry", "basket")));
            }
        }
        List<StationSearch> stations = new ArrayList<>();
        if (o.has("stations")) {
            for (JsonElement e : o.getAsJsonArray("stations")) {
                JsonObject st = e.getAsJsonObject();
                stations.add(new StationSearch(str(st, "anchor", "workstation"), integer(st, "radius", 1), set(st, "blocks")));
            }
        } else {
            stations.add(new StationSearch("workstation", 1, Set.of()));
        }
        Woodland woodland = null;
        if (o.has("woodland")) {
            JsonObject w = o.getAsJsonObject("woodland");
            woodland = new Woodland(integer(w, "radius", 48), integer(w, "max", 24), str(w, "anim", "chop"),
                    integer(w, "ticks", 80), set(w, "professions"), dbl(w, "perDay", 2), integer(w, "logsPerTree", 5));
        }
        Map<String, NaturalPoint> natural = new LinkedHashMap<>();
        if (o.has("naturalPoints")) {
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("naturalPoints").entrySet()) {
                JsonObject n = e.getValue().getAsJsonObject();
                natural.put(e.getKey(), new NaturalPoint(e.getKey(), str(n, "block", ""), integer(n, "radius", 48),
                        n.has("outcrop") && n.get("outcrop").getAsBoolean()));
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
                new FuelTable(VillageRecipe.items(o, "fuel")), points, crops, fruits, stations, woodland, natural, residents,
                VillageRecipe.items(o, "initialStock"), Map.of(), Map.of(),
                o.has("buildCosts") ? BuildCosts.parse(o.getAsJsonObject("buildCosts")) : BuildCosts.none());
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
