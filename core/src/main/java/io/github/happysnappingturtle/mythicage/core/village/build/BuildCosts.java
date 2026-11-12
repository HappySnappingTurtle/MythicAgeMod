package io.github.happysnappingturtle.mythicage.core.village.build;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * 施工折算表（T19 设计文档 §1.4 E1）：方块 → 村库里的成品建材。施工扣的是成品（方石、陶瓦、木板……），不是逐方块同名物品。
 * <ul>
 *   <li>规则按顺序匹配（方块 id，可用 {@code *} 通配），第一条命中的生效；成本可以是小数；</li>
 *   <li>成本的键可以是物品组 {@code #<组名>}（如 {@code #planks} = 任意木板），组在 {@code groups} 里列，按顺序优先；</li>
 *   <li>{@code free} 里的方块不要钱（砂、砾石、水、泥土……）；什么都没命中也不要钱，由内容校验保证数据覆盖完整。</li>
 * </ul>
 * 文明数据 {@code village_civ/<civ>.json} 的 {@code buildCosts}：{@code {"free": [...], "groups": {...}, "rules": [{"match", "cost"}]}}。
 */
public final class BuildCosts {
    /** 浮点累加的容差：0.1 + 0.2 之类不该被向上取整成多 1 件。 */
    public static final double EPS = 1e-6;

    public record Rule(String match, Map<String, Double> cost) {
        public Rule {
            cost = Map.copyOf(cost);
        }
    }

    private final List<Rule> rules;
    private final List<String> free;
    private final Map<String, List<String>> groups;

    public BuildCosts(List<Rule> rules, List<String> free, Map<String, List<String>> groups) {
        this.rules = List.copyOf(rules);
        this.free = List.copyOf(free);
        Map<String, List<String>> g = new LinkedHashMap<>();
        groups.forEach((k, v) -> g.put(k, List.copyOf(v)));
        this.groups = java.util.Collections.unmodifiableMap(g);
    }

    public static BuildCosts none() {
        return new BuildCosts(List.of(), List.of(), Map.of());
    }

    public static BuildCosts parse(JsonObject o) {
        List<String> free = new ArrayList<>();
        if (o.has("free")) {
            o.getAsJsonArray("free").forEach(e -> free.add(e.getAsString()));
        }
        Map<String, List<String>> groups = new LinkedHashMap<>();
        if (o.has("groups")) {
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("groups").entrySet()) {
                List<String> items = new ArrayList<>();
                e.getValue().getAsJsonArray().forEach(x -> items.add(x.getAsString()));
                groups.put(e.getKey(), items);
            }
        }
        return new BuildCosts(rules(o.has("rules") ? o.getAsJsonArray("rules") : new JsonArray()), free, groups);
    }

    public static List<Rule> rules(JsonArray a) {
        List<Rule> out = new ArrayList<>();
        for (JsonElement e : a) {
            JsonObject r = e.getAsJsonObject();
            Map<String, Double> cost = new LinkedHashMap<>();
            if (r.has("cost")) {
                r.getAsJsonObject("cost").entrySet().forEach(c -> cost.put(c.getKey(), c.getValue().getAsDouble()));
            }
            out.add(new Rule(r.get("match").getAsString(), cost));
        }
        return out;
    }

    /** 在前面加上（某座建筑自己的）规则，组与 free 不变。 */
    public BuildCosts withLeading(List<Rule> leading) {
        if (leading.isEmpty()) {
            return this;
        }
        List<Rule> all = new ArrayList<>(leading);
        all.addAll(rules);
        return new BuildCosts(all, free, groups);
    }

    public List<Rule> rules() {
        return rules;
    }

    public Map<String, List<String>> groups() {
        return groups;
    }

    /** 方块的成本：命中 free 是空表；规则都没命中是 empty（内容校验用来找漏写的方块）。 */
    public Optional<Map<String, Double>> lookup(String block) {
        for (String f : free) {
            if (matches(f, block)) {
                return Optional.of(Map.of());
            }
        }
        for (Rule r : rules) {
            if (matches(r.match(), block)) {
                return Optional.of(r.cost());
            }
        }
        return Optional.empty();
    }

    public Map<String, Double> cost(String block) {
        return lookup(block).orElse(Map.of());
    }

    public boolean covers(String block) {
        return lookup(block).isPresent();
    }

    /** 一串方块的成本合计（空气不算）。 */
    public Map<String, Double> sum(Iterable<String> blocks) {
        Map<String, Double> out = new TreeMap<>();
        for (String b : blocks) {
            if (!"minecraft:air".equals(b)) {
                add(out, cost(b));
            }
        }
        return out;
    }

    /** 成本键对应的物品：物品组按组内顺序；普通物品就是它自己。 */
    public List<String> items(String key) {
        if (key.startsWith("#")) {
            return groups.getOrDefault(key.substring(1), List.of());
        }
        return List.of(key);
    }

    /** 物品组退回时给的物品（组里第一样）。 */
    public String representative(String key) {
        List<String> l = items(key);
        return l.isEmpty() ? key : l.get(0);
    }

    public static void add(Map<String, Double> into, Map<String, Double> what) {
        what.forEach((k, v) -> into.merge(k, v, Double::sum));
    }

    public static Map<String, Integer> ceil(Map<String, Double> m) {
        Map<String, Integer> out = new TreeMap<>();
        m.forEach((k, v) -> {
            int n = (int) Math.ceil(v - EPS);
            if (n > 0) {
                out.put(k, n);
            }
        });
        return out;
    }

    public static Map<String, Integer> floor(Map<String, Double> m) {
        Map<String, Integer> out = new TreeMap<>();
        m.forEach((k, v) -> {
            int n = (int) Math.floor(v + EPS);
            if (n > 0) {
                out.put(k, n);
            }
        });
        return out;
    }

    /** 方块 id 的通配匹配：只支持 {@code *}（任意长度）。 */
    public static boolean matches(String pattern, String id) {
        if (!pattern.contains("*")) {
            return pattern.equals(id);
        }
        String[] parts = pattern.split("\\*", -1);
        int at = 0;
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (i == 0) {
                if (!id.startsWith(p)) {
                    return false;
                }
                at = p.length();
            } else if (i == parts.length - 1) {
                return id.length() - p.length() >= at && id.endsWith(p);
            } else {
                int j = id.indexOf(p, at);
                if (j < 0) {
                    return false;
                }
                at = j + p.length();
            }
        }
        return true;
    }
}
