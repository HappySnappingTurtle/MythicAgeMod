package io.github.happysnappingturtle.mythicage.core.config.villages;

import com.google.gson.JsonObject;
import io.github.happysnappingturtle.mythicage.core.config.ConfigSchema;
import io.github.happysnappingturtle.mythicage.core.config.ObjectReader;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * {@code villages.json} 的 schema（T10a 设计文档 §8；T10b 设计文档 §7 升为第 2 版；T13b 升为第 3 版）。不合法的条目告警并回退到默认值。
 * <p>
 * 主神权重与群系加成都按文明与默认值合并：文件里只写了希腊时，默认里其他文明的条目保留（新版本加的文明不会丢）。
 * 第 1 版的 {@code coastalPoseidonBonus} 读作 {@code patronBiomeBonus.greek.poseidon.multiplier}；
 * 第 2 版的 {@code replaceVanillaInRegion} 由 {@code generateVanillaVillages} 取代：写过 {@code false}（明确要保留原版村庄）
 * 读作 {@code generateVanillaVillages: true}，其余读作默认 {@code false}（V0 §3.2）。按 T3 规则不改写文件。
 */
public final class VillagesSchema implements ConfigSchema<VillagesConfig> {
    public static final VillagesSchema INSTANCE = new VillagesSchema();

    private static final VillagesConfig DEFAULTS = new VillagesConfig(VillagesConfig.Mode.AFFILIATE,
            Map.of("plains", Map.of("greek", 1), "desert", Map.of("egyptian", 1), "savanna", Map.of("egyptian", 1),
                    "taiga", Map.of("norse", 1), "snowy", Map.of("norse", 1)),
            false, 0.30,
            Map.of("greek", orderedOf("zeus", 40, "poseidon", 35, "hades", 25)),
            Map.of("greek", Map.of("poseidon", new VillagesConfig.BiomeBonus(
                    List.of("#minecraft:is_ocean", "#minecraft:is_beach"), 32, 2.0))),
            true);
    /** 群系 id 或标签：可选 #，命名空间:路径。 */
    private static final Pattern BIOME = Pattern.compile("#?[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Pattern ID = Pattern.compile("[a-z0-9_]+");

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
        return 3;
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
        boolean generateVanilla = base.generateVanillaVillages();
        if (root.isPresent("replaceVanillaInRegion")) {
            generateVanilla = !root.bool("replaceVanillaInRegion", true);
            root.warn("replaceVanillaInRegion", "第 2 版字段：已由 generateVanillaVillages 取代，本次读作 generateVanillaVillages: "
                    + generateVanilla);
        }
        generateVanilla = root.bool("generateVanillaVillages", generateVanilla);
        double chinese = root.doubleIn("chineseRiverChance", base.chineseRiverChance(), 0.0, 1.0);
        Map<String, Map<String, Integer>> patrons = readPatrons(root.object("patronWeights"), base.patronWeights());
        Map<String, Map<String, VillagesConfig.BiomeBonus>> bonus = new LinkedHashMap<>(base.patronBiomeBonus());
        if (root.isPresent("coastalPoseidonBonus")) {
            Map<String, VillagesConfig.BiomeBonus> greek = new LinkedHashMap<>(bonus.getOrDefault("greek", Map.of()));
            VillagesConfig.BiomeBonus old = greek.getOrDefault("poseidon", DEFAULTS.biomeBonus("greek").get("poseidon"));
            double m = root.doubleIn("coastalPoseidonBonus", old.multiplier(), 1.0, 10.0);
            greek.put("poseidon", new VillagesConfig.BiomeBonus(old.biomes(), old.radius(), m));
            bonus.put("greek", greek);
            root.warn("coastalPoseidonBonus", "第 1 版字段：已改为 patronBiomeBonus.greek.poseidon.multiplier；本次按旧字段读取");
        }
        readBiomeBonus(root.object("patronBiomeBonus"), bonus);
        boolean title = root.bool("titleOnEnter", base.titleOnEnter());
        return new VillagesConfig(mode, typeToCiv, generateVanilla, chinese, patrons, bonus, title);
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

    /** 按文明合并：文件里写了的文明整表替换该文明，没写的文明保留 base。 */
    private static Map<String, Map<String, Integer>> readPatrons(ObjectReader p, Map<String, Map<String, Integer>> base) {
        Map<String, Map<String, Integer>> out = new LinkedHashMap<>(base);
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
        return out;
    }

    /** {@code patronBiomeBonus.<文明>.<主神>}：按文明、按主神与已有值合并。 */
    private static void readBiomeBonus(ObjectReader b, Map<String, Map<String, VillagesConfig.BiomeBonus>> out) {
        for (String civ : b.keys()) {
            ObjectReader c = b.object(civ);
            if (Civilization.byId(civ).isEmpty()) {
                b.warn(civ, "未知文明，已忽略");
                continue;
            }
            Map<String, VillagesConfig.BiomeBonus> perCiv = new LinkedHashMap<>(out.getOrDefault(civ, Map.of()));
            for (String patron : c.keys()) {
                if (!ID.matcher(patron).matches()) {
                    c.warn(patron, "主神 id 只能由小写字母、数字、下划线组成，已忽略");
                    continue;
                }
                ObjectReader o = c.object(patron);
                VillagesConfig.BiomeBonus old = perCiv.getOrDefault(patron,
                        new VillagesConfig.BiomeBonus(List.of(), 32, 1.0));
                List<String> biomes = o.strings("biomes", old.biomes());
                if (biomes.stream().anyMatch(x -> !BIOME.matcher(x).matches())) {
                    o.warn("biomes", "应为群系 id（minecraft:beach）或群系标签（#minecraft:is_ocean），已使用默认值");
                    biomes = old.biomes();
                }
                int radius = o.intIn("radius", old.radius(), 0, 96);
                double m = o.doubleIn("multiplier", old.multiplier(), 0.1, 10.0);
                perCiv.put(patron, new VillagesConfig.BiomeBonus(biomes, radius, m));
            }
            out.put(civ, perCiv);
        }
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
        root.addProperty("generateVanillaVillages", value.generateVanillaVillages());
        root.addProperty("chineseRiverChance", value.chineseRiverChance());
        JsonObject patrons = new JsonObject();
        value.patronWeights().forEach((civ, w) -> {
            JsonObject o = new JsonObject();
            w.forEach(o::addProperty);
            patrons.add(civ, o);
        });
        root.add("patronWeights", patrons);
        JsonObject bonus = new JsonObject();
        value.patronBiomeBonus().forEach((civ, m) -> {
            JsonObject c = new JsonObject();
            m.forEach((patron, b) -> {
                JsonObject o = new JsonObject();
                com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
                b.biomes().forEach(arr::add);
                o.add("biomes", arr);
                o.addProperty("radius", b.radius());
                o.addProperty("multiplier", b.multiplier());
                c.add(patron, o);
            });
            bonus.add(civ, c);
        });
        root.add("patronBiomeBonus", bonus);
        root.addProperty("titleOnEnter", value.titleOnEnter());
        return root;
    }
}
