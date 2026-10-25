package io.github.happysnappingturtle.mythicage.core.village.soldier;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 文明 JSON 的 {@code soldiers} 段（T20 设计文档 §10.2）：兵种补员顺序、岗位锚点类别、训练锚点、转岗后至少留几名劳力、
 * 昼夜班、生成村庄的初始兵装档。
 *
 * @param order       兵种职业 id，按补员顺序
 * @param posts       岗位锚点类别，按重要性
 * @param training    训练锚点类别（不是岗位，只用于不值勤时训练）
 * @param minLaborers 转岗后基础劳力至少留几人（01 号 §0.2「必要在岗配额」）
 * @param seedKit     生成村庄的初始士兵（{@code residents.initial} 里的兵种）穿哪一档兵装
 */
public record SoldierCivRules(List<String> order, List<String> posts, String training, int minLaborers, Shifts shifts,
                              Map<String, SoldierProfile.Tier> seedKit) {
    public static final SoldierCivRules NONE = new SoldierCivRules(List.of(), List.of(), "", 2, Shifts.DEFAULT, Map.of());

    public SoldierCivRules {
        order = List.copyOf(order);
        posts = List.copyOf(posts);
        seedKit = Map.copyOf(seedKit);
    }

    /**
     * 昼夜班（一日内的时刻，0–24000，左闭右开、可跨午夜）。夜班士兵白天先睡 {@code nightSleep}。
     */
    public record Shifts(int dayFrom, int dayTo, int nightFrom, int nightTo, int sleepFrom, int sleepTo) {
        public static final Shifts DEFAULT = new Shifts(23000, 12000, 12000, 23000, 23000, 6000);
    }

    public boolean enabled() {
        return !order.isEmpty();
    }

    public static SoldierCivRules parse(JsonObject o) {
        Shifts s = Shifts.DEFAULT;
        if (o.has("shifts")) {
            JsonObject sh = o.getAsJsonObject("shifts");
            int[] day = pair(sh, "day", s.dayFrom(), s.dayTo());
            int[] night = pair(sh, "night", s.nightFrom(), s.nightTo());
            int[] sleep = pair(sh, "nightSleep", s.sleepFrom(), s.sleepTo());
            s = new Shifts(day[0], day[1], night[0], night[1], sleep[0], sleep[1]);
        }
        Map<String, SoldierProfile.Tier> seed = new LinkedHashMap<>();
        if (o.has("seedKit")) {
            o.getAsJsonObject("seedKit").entrySet().forEach(e -> seed.put(e.getKey(), SoldierProfile.Tier.byId(e.getValue().getAsString())));
        }
        return new SoldierCivRules(list(o, "order"), list(o, "posts"), o.has("training") ? o.get("training").getAsString() : "",
                o.has("minLaborers") ? Math.max(0, o.get("minLaborers").getAsInt()) : 2, s, seed);
    }

    private static int[] pair(JsonObject o, String k, int a, int b) {
        if (!o.has(k)) {
            return new int[]{a, b};
        }
        var arr = o.getAsJsonArray(k);
        return new int[]{Math.floorMod(arr.get(0).getAsInt(), 24000), Math.floorMod(arr.get(1).getAsInt(), 24000)};
    }

    private static List<String> list(JsonObject o, String k) {
        List<String> out = new ArrayList<>();
        if (o.has(k)) {
            o.getAsJsonArray(k).forEach(e -> out.add(e.getAsString()));
        }
        return out;
    }
}
