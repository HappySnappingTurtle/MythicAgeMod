package io.github.happysnappingturtle.mythicage.core.config.villages;

import com.google.gson.JsonObject;
import io.github.happysnappingturtle.mythicage.core.config.ConfigSchema;
import io.github.happysnappingturtle.mythicage.core.config.ObjectReader;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;

import java.util.LinkedHashMap;
import java.util.Map;

/** {@code villages.json} 的 schema（T10a 设计文档 §8）。不合法的条目告警并回退到默认值。 */
public final class VillagesSchema implements ConfigSchema<VillagesConfig> {
    public static final VillagesSchema INSTANCE = new VillagesSchema();

    private static final VillagesConfig DEFAULTS = new VillagesConfig(VillagesConfig.Mode.AFFILIATE,
            Map.of("plains", Map.of("greek", 1), "desert", Map.of("egyptian", 1), "savanna", Map.of("egyptian", 1),
                    "taiga", Map.of("norse", 1), "snowy", Map.of("norse", 1)),
            true,
            Map.of("greek", orderedOf("zeus", 40, "poseidon", 35, "hades", 25)),
            2.0, true);

    private VillagesSchema() {
    }

    private static Map<String, Integer> orderedOf(Object... kv) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], (Integer) kv[i + 1]);
        }
        return m;
    }

    @Override
    public String name() {
        return "villages";
    }

    @Override
    public int currentVersion() {
        return 1;
    }

    @Override
    public VillagesConfig defaults() {
        return DEFAULTS;
    }

    @Override
    public VillagesConfig read(ObjectReader root, VillagesConfig base) {
        VillagesConfig.Mode mode = root.choice("vanillaVillages", base.vanillaVillages(),
                Map.of("affiliate", VillagesConfig.Mode.AFFILIATE, "neutral", VillagesConfig.Mode.NEUTRAL));
        Map<String, Map<String, Integer>> typeToCiv = readTypeToCiv(root, base.vanillaTypeToCiv());
        boolean replace = root.bool("replaceVanillaInRegion", base.replaceVanillaInRegion());
        Map<String, Map<String, Integer>> patrons = readPatrons(root.object("patronWeights"), base.patronWeights());
        double coastal = root.doubleIn("coastalPoseidonBonus", base.coastalPoseidonBonus(), 1.0, 10.0);
        boolean title = root.bool("titleOnEnter", base.titleOnEnter());
        return new VillagesConfig(mode, typeToCiv, replace, patrons, coastal, title);
    }

    private static Map<String, Map<String, Integer>> readTypeToCiv(ObjectReader root, Map<String, Map<String, Integer>> base) {
        ObjectReader t = root.object("vanillaTypeToCiv");
        Map<String, Map<String, Integer>> out = new LinkedHashMap<>(base);
        for (String type : t.keys()) {
            if (!VillagesConfig.VANILLA_TYPES.contains(type)) {
                t.warn(type, "未知的原版村庄类型，可选 " + VillagesConfig.VANILLA_TYPES);
                t.string(type, "");
                continue;
            }
            Map<String, Integer> weights;
            if (t.isObject(type)) {
                weights = readWeights(t.object(type), true);
            } else {
                String civ = t.string(type, "");
                weights = Civilization.byId(civ).isPresent() ? Map.of(civ, 1) : null;
                if (weights == null && !civ.isEmpty()) {
                    t.warn(type, "未知文明 " + civ + "，已使用默认值");
                }
            }
            if (weights != null && !weights.isEmpty()) {
                out.put(type, weights);
            }
        }
        return out;
    }

    private static Map<String, Map<String, Integer>> readPatrons(ObjectReader p, Map<String, Map<String, Integer>> base) {
        if (p.keys().isEmpty()) {
            return base;
        }
        Map<String, Map<String, Integer>> out = new LinkedHashMap<>();
        for (String civ : p.keys()) {
            if (Civilization.byId(civ).isEmpty()) {
                p.warn(civ, "未知文明，已忽略");
                p.object(civ);
                continue;
            }
            Map<String, Integer> w = readWeights(p.object(civ), false);
            if (!w.isEmpty()) {
                out.put(civ, w);
            }
        }
        return out.isEmpty() ? base : out;
    }

    /** 权重表：值为 1–1000 的整数；civKeys 时键必须是文明 id。 */
    private static Map<String, Integer> readWeights(ObjectReader o, boolean civKeys) {
        Map<String, Integer> w = new LinkedHashMap<>();
        for (String k : o.keys()) {
            if (civKeys && Civilization.byId(k).isEmpty()) {
                o.warn(k, "未知文明，已忽略");
                o.intIn(k, 0, 0, Integer.MAX_VALUE);
                continue;
            }
            int v = o.intIn(k, -1, 1, 1000);
            if (v > 0) {
                w.put(k, v);
            }
        }
        return w;
    }

    @Override
    public JsonObject toJson(VillagesConfig value) {
        JsonObject root = new JsonObject();
        root.addProperty("vanillaVillages", value.vanillaVillages().name().toLowerCase(java.util.Locale.ROOT));
        JsonObject types = new JsonObject();
        for (String type : VillagesConfig.VANILLA_TYPES) {
            Map<String, Integer> w = value.vanillaTypeToCiv().get(type);
            if (w == null) {
                continue;
            }
            if (w.size() == 1) {
                types.addProperty(type, w.keySet().iterator().next());
            } else {
                JsonObject o = new JsonObject();
                w.forEach(o::addProperty);
                types.add(type, o);
            }
        }
        root.add("vanillaTypeToCiv", types);
        root.addProperty("replaceVanillaInRegion", value.replaceVanillaInRegion());
        JsonObject patrons = new JsonObject();
        value.patronWeights().forEach((civ, w) -> {
            JsonObject o = new JsonObject();
            w.forEach(o::addProperty);
            patrons.add(civ, o);
        });
        root.add("patronWeights", patrons);
        root.addProperty("coastalPoseidonBonus", value.coastalPoseidonBonus());
        root.addProperty("titleOnEnter", value.titleOnEnter());
        return root;
    }
}
