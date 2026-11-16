package io.github.happysnappingturtle.mythicage.core.village.map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 村庄地图的类别配色（GA-12 §3.1、§4）：{@code assets/mythic_age/village_map/<文明>.json}。
 * <pre>
 * { "categories": { "temple": { "fill": "#D4A72C", "border": "#8A6A12", "symbol": "Π" }, … },
 *   "buildings":  { "greek:acropolis": "temple", "greek:house": "dwelling", … },
 *   "plots":      { "shrine": "shrine", "house": "dwelling", … } }
 * </pre>
 * 建筑归哪类按 id 前缀，取最长的那条；都不匹配归 {@value #FALLBACK}。每类除颜色外还有一个符号，不只靠颜色区分。
 * 缺了必需的类别用默认色并给一条警告。空地块按种类（{@code plots}）归类，地图上画该类的淡色符号、不写字；没登记的种类不画符号。
 */
public record MapPalette(Map<String, Style> categories, List<Map.Entry<String, String>> prefixes, Map<String, String> plots) {
    /** 必需的类别（§3.1 表）。 */
    public static final List<String> REQUIRED = List.of("temple", "dwelling", "workshop", "storage", "farm", "shrine", "public");
    public static final String FALLBACK = "public";
    public static final Style DEFAULT = new Style(0xFF9A8F7A, 0xFF5A5040, "?");

    /** @param fill 填充色 ARGB；border 边框色；symbol 画在矩形中间的符号 */
    public record Style(int fill, int border, String symbol) {
    }

    public MapPalette {
        categories = Map.copyOf(categories);
        List<Map.Entry<String, String>> p = new ArrayList<>(prefixes);
        p.sort(Comparator.comparingInt((Map.Entry<String, String> e) -> e.getKey().length()).reversed());
        prefixes = List.copyOf(p);
        plots = Map.copyOf(plots);
    }

    public static final MapPalette EMPTY = new MapPalette(Map.of(), List.of(), Map.of());

    public static MapPalette parse(JsonObject o, Consumer<String> warn) {
        Map<String, Style> cats = new LinkedHashMap<>();
        JsonObject c = o.has("categories") ? o.getAsJsonObject("categories") : new JsonObject();
        for (Map.Entry<String, JsonElement> e : c.entrySet()) {
            JsonObject s = e.getValue().getAsJsonObject();
            try {
                cats.put(e.getKey(), new Style(color(s.get("fill").getAsString()),
                        s.has("border") ? color(s.get("border").getAsString()) : DEFAULT.border(),
                        s.has("symbol") ? s.get("symbol").getAsString() : ""));
            } catch (RuntimeException ex) {
                warn.accept("村庄地图配色：类别 " + e.getKey() + " 写错了（" + ex.getMessage() + "），用默认色");
            }
        }
        for (String r : REQUIRED) {
            if (!cats.containsKey(r)) {
                warn.accept("村庄地图配色：缺类别 " + r + "，用默认色");
            }
        }
        List<Map.Entry<String, String>> prefixes = new ArrayList<>();
        if (o.has("buildings")) {
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("buildings").entrySet()) {
                String cat = e.getValue().getAsString();
                if (!REQUIRED.contains(cat) && !cats.containsKey(cat)) {
                    warn.accept("村庄地图配色：建筑 " + e.getKey() + " 归到未定义的类别 " + cat);
                }
                prefixes.add(Map.entry(e.getKey(), cat));
            }
        }
        Map<String, String> plots = new LinkedHashMap<>();
        if (o.has("plots")) {
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("plots").entrySet()) {
                String cat = e.getValue().getAsString();
                if (!REQUIRED.contains(cat) && !cats.containsKey(cat)) {
                    warn.accept("村庄地图配色：空地块 " + e.getKey() + " 归到未定义的类别 " + cat);
                }
                plots.put(e.getKey(), cat);
            }
        }
        return new MapPalette(cats, prefixes, plots);
    }

    /** {@code #RRGGBB} 或 {@code #AARRGGBB}。 */
    static int color(String s) {
        String h = s.startsWith("#") ? s.substring(1) : s;
        if (h.length() == 6) {
            return 0xFF000000 | Integer.parseUnsignedInt(h, 16);
        }
        if (h.length() == 8) {
            return Integer.parseUnsignedInt(h, 16);
        }
        throw new IllegalArgumentException("颜色要写成 #RRGGBB：" + s);
    }

    public String categoryOf(String building) {
        for (Map.Entry<String, String> e : prefixes) {
            if (building.startsWith(e.getKey())) {
                return e.getValue();
            }
        }
        return FALLBACK;
    }

    public Style style(String category) {
        return categories.getOrDefault(category, DEFAULT);
    }

    public Style styleOf(String building) {
        return style(categoryOf(building));
    }

    /** 空地块种类归哪类；没登记为空。 */
    public java.util.Optional<String> plotCategory(String kind) {
        return java.util.Optional.ofNullable(plots.get(kind));
    }
}
