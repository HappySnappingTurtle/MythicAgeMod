package io.github.happysnappingturtle.mythicage.core.village;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 一个文明的主神与次神表（T10a 设计文档 §3），来自数据包 {@code data/<ns>/mythic_age/pantheon/<文明>.json}。
 * 次神列出它属于哪些主神（AoM「主神决定次神」）；{@code available} 表示该次神已有内容，候选只列已有内容的（R1）。
 */
public record Pantheon(Civilization civ, List<String> patrons, List<MinorGod> minorGods) {
    private static final Pattern ID = Pattern.compile("[a-z0-9_]+");

    public Pantheon {
        patrons = List.copyOf(patrons);
        minorGods = List.copyOf(minorGods);
    }

    public record MinorGod(String id, Age age, List<String> patrons, boolean available) {
        public MinorGod {
            patrons = List.copyOf(patrons);
        }
    }

    public static Pantheon empty(Civilization civ) {
        return new Pantheon(civ, List.of(), List.of());
    }

    /**
     * 在主神为 villagePatron 的村庄举行仪式时，某时代可选的次神（18 号 §4：候选看举行地村庄的主神），
     * 只列已有内容的（R1），按表顺序。
     */
    public List<MinorGod> candidates(String villagePatron, Age age) {
        return minorGods.stream().filter(g -> g.age() == age && g.available() && g.patrons().contains(villagePatron))
                .toList();
    }

    /** 本表中的全部神 id（主神 + 次神）。 */
    public List<String> godIds() {
        List<String> out = new ArrayList<>(patrons);
        minorGods.forEach(g -> out.add(g.id()));
        return out;
    }

    /** 在多个文明的表里重复出现的神 id（神名 key {@code mythic_age.god.<id>} 会冲突），按字母序。 */
    public static List<String> duplicateIds(Collection<Pantheon> tables) {
        Map<String, Integer> seen = new java.util.TreeMap<>();
        for (Pantheon p : tables) {
            for (String id : new HashSet<>(p.godIds())) {
                seen.merge(id, 1, Integer::sum);
            }
        }
        return seen.entrySet().stream().filter(e -> e.getValue() > 1).map(Map.Entry::getKey).toList();
    }

    /** 某位主神名下、某时代的次神（含尚未开放的），供祭司对话页展示。 */
    public List<MinorGod> underPatron(String patron, Age age) {
        return minorGods.stream().filter(g -> g.age() == age && g.patrons().contains(patron)).toList();
    }

    public boolean hasPatron(String patron) {
        return patrons.contains(patron);
    }

    public static final class PantheonException extends RuntimeException {
        public PantheonException(String message) {
            super(message);
        }
    }

    public static Pantheon parse(String json) {
        try {
            return parse(JsonParser.parseString(json));
        } catch (JsonSyntaxException e) {
            throw new PantheonException("(根): JSON 语法错误：" + e.getMessage());
        }
    }

    public static Pantheon parse(JsonElement rootEl) {
        if (rootEl == null || !rootEl.isJsonObject()) {
            throw new PantheonException("(根): 缺失或不是对象");
        }
        JsonObject root = rootEl.getAsJsonObject();
        JsonElement v = root.get("schemaVersion");
        if (v == null || !v.isJsonPrimitive() || v.getAsInt() != 1) {
            throw new PantheonException("schemaVersion: 只支持 1");
        }
        String civId = string(root, "civilization", "civilization");
        Civilization civ = Civilization.byId(civId).orElseThrow(() -> new PantheonException("civilization: 未知文明 " + civId));
        List<String> patrons = ids(root.get("patrons"), "patrons");
        if (patrons.isEmpty()) {
            throw new PantheonException("patrons: 至少需要一位主神");
        }
        JsonElement mgEl = root.get("minorGods");
        if (mgEl == null || !mgEl.isJsonArray()) {
            throw new PantheonException("minorGods: 缺失或不是数组");
        }
        Set<String> seen = new HashSet<>(patrons);
        List<MinorGod> gods = new ArrayList<>();
        JsonArray arr = mgEl.getAsJsonArray();
        for (int i = 0; i < arr.size(); i++) {
            String path = "minorGods[" + i + "]";
            if (!arr.get(i).isJsonObject()) {
                throw new PantheonException(path + ": 不是对象");
            }
            JsonObject o = arr.get(i).getAsJsonObject();
            String id = string(o, "id", path + ".id");
            if (!ID.matcher(id).matches() || !seen.add(id)) {
                throw new PantheonException(path + ".id: 非法或重复的 id " + id);
            }
            String ageId = string(o, "age", path + ".age");
            Age age = Age.byId(ageId).filter(a -> a != Age.NONE && a != Age.ARCHAIC)
                    .orElseThrow(() -> new PantheonException(path + ".age: 次神时代只能是 classical / heroic / mythic"));
            List<String> owners = ids(o.get("patrons"), path + ".patrons");
            for (String p : owners) {
                if (!patrons.contains(p)) {
                    throw new PantheonException(path + ".patrons: 未知主神 " + p);
                }
            }
            if (owners.isEmpty()) {
                throw new PantheonException(path + ".patrons: 至少属于一位主神");
            }
            JsonElement av = o.get("available");
            boolean available = av != null && av.isJsonPrimitive() && av.getAsJsonPrimitive().isBoolean() && av.getAsBoolean();
            gods.add(new MinorGod(id, age, owners, available));
        }
        return new Pantheon(civ, patrons, gods);
    }

    private static String string(JsonObject o, String key, String path) {
        JsonElement e = o.get(key);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString() || e.getAsString().isEmpty()) {
            throw new PantheonException(path + ": 缺失或不是非空字符串");
        }
        return e.getAsString();
    }

    private static List<String> ids(JsonElement e, String path) {
        if (e == null || !e.isJsonArray()) {
            throw new PantheonException(path + ": 缺失或不是数组");
        }
        List<String> out = new ArrayList<>();
        for (JsonElement x : e.getAsJsonArray()) {
            if (!x.isJsonPrimitive() || !ID.matcher(x.getAsString()).matches()) {
                throw new PantheonException(path + ": 非法 id " + x);
            }
            out.add(x.getAsString());
        }
        return out;
    }
}
