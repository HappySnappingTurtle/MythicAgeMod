package io.github.happysnappingturtle.mythicage.core.rite;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 村庄阶段表（T11a 设计文档 §3.4），来自 {@code data/<ns>/mythic_age/village_stage/<文明>.json}：
 * 每个阶段带来的、不需要施工的变化。缺文件或缺阶段时加成为 0、不吸纳移民。
 */
public record VillageStageTable(Civilization civ, Map<Age, Stage> stages) {
    /** 卫兵上限的基础硬上限与人口比例（01 号居民士兵提案 §0.2）。 */
    public static final int GUARD_HARD_CAP = 6;
    private static final Set<String> STAGE_KEYS = Set.of("population", "guards", "migrationDays", "preview");

    public VillageStageTable {
        stages = Map.copyOf(stages);
    }

    /**
     * @param population    本阶段新增的人口目标（累加）
     * @param guards        卫兵上限加成（取当前阶段的值，不累加）
     * @param migrationDays 吸纳移民的间隔（MC 日）；0 = 不吸纳
     * @param preview       晋升页「村庄变化预告」里的文本键
     */
    public record Stage(int population, int guards, int migrationDays, List<String> preview) {
        public static final Stage NONE = new Stage(0, 0, 0, List.of());

        public Stage {
            preview = List.copyOf(preview);
        }
    }

    public static VillageStageTable empty(Civilization civ) {
        return new VillageStageTable(civ, Map.of());
    }

    public Stage stage(Age age) {
        return stages.getOrDefault(age, Stage.NONE);
    }

    /** 移民目标：基线 + 已达到的各阶段人口加成之和。 */
    public int populationTarget(int baseline, Age stage) {
        int total = Math.max(0, baseline);
        for (Map.Entry<Age, Stage> e : stages.entrySet()) {
            if (e.getKey() != Age.NONE && stage.atLeast(e.getKey())) {
                total += e.getValue().population();
            }
        }
        return total;
    }

    /** 卫兵上限：min(6, ⌊人口 × 0.25⌋，人口 ≥ 4 时至少 1) + 当前阶段的加成。 */
    public int guardCap(int population, Age stage) {
        int base = Math.min(GUARD_HARD_CAP, population / 4);
        if (population >= 4) {
            base = Math.max(1, base);
        }
        return base + stage(stage).guards();
    }

    public static VillageStageTable parse(String json, List<String> warnings) {
        try {
            return parse(JsonParser.parseString(json), warnings);
        } catch (JsonSyntaxException e) {
            throw new PromotionTable.PromotionTableException("(根): JSON 语法错误：" + e.getMessage());
        }
    }

    public static VillageStageTable parse(JsonElement rootEl, List<String> warnings) {
        if (rootEl == null || !rootEl.isJsonObject()) {
            throw new PromotionTable.PromotionTableException("(根): 缺失或不是对象");
        }
        JsonObject root = rootEl.getAsJsonObject();
        JsonElement v = root.get("schemaVersion");
        if (v == null || !v.isJsonPrimitive() || !v.getAsJsonPrimitive().isNumber() || v.getAsInt() != 1) {
            throw new PromotionTable.PromotionTableException("schemaVersion: 只支持 1");
        }
        JsonElement civEl = root.get("civilization");
        String civId = civEl != null && civEl.isJsonPrimitive() ? civEl.getAsString() : "";
        Civilization civ = Civilization.byId(civId)
                .orElseThrow(() -> new PromotionTable.PromotionTableException("civilization: 未知文明 " + civId));
        Map<Age, Stage> stages = new EnumMap<>(Age.class);
        JsonElement s = root.get("stages");
        if (s != null && s.isJsonObject()) {
            JsonObject o = s.getAsJsonObject();
            for (String key : o.keySet()) {
                Optional<Age> age = Age.byId(key).filter(a -> a != Age.NONE);
                if (age.isEmpty() || !o.get(key).isJsonObject()) {
                    warnings.add("stages." + key + ": 应为 archaic / classical / heroic / mythic 的对象，已跳过");
                    continue;
                }
                stages.put(age.get(), stage(o.get(key).getAsJsonObject(), "stages." + key, warnings));
            }
        } else if (s != null) {
            warnings.add("stages: 应为对象，已忽略");
        }
        return new VillageStageTable(civ, stages);
    }

    private static Stage stage(JsonObject o, String path, List<String> warnings) {
        for (String k : o.keySet()) {
            if (!STAGE_KEYS.contains(k)) {
                warnings.add(path + "." + k + ": 未知字段，已忽略");
            }
        }
        List<String> preview = new ArrayList<>();
        JsonElement p = o.get("preview");
        if (p != null && p.isJsonArray()) {
            p.getAsJsonArray().forEach(e -> {
                if (e.isJsonPrimitive() && !e.getAsString().isEmpty()) {
                    preview.add(e.getAsString());
                }
            });
        }
        return new Stage(nonNegative(o, "population", path, warnings), nonNegative(o, "guards", path, warnings),
                nonNegative(o, "migrationDays", path, warnings), preview);
    }

    private static int nonNegative(JsonObject o, String key, String path, List<String> warnings) {
        JsonElement e = o.get(key);
        if (e == null) {
            return 0;
        }
        if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber() || e.getAsInt() < 0 || e.getAsInt() > 1000) {
            warnings.add(path + "." + key + ": 应为 0–1000 的整数，已按 0");
            return 0;
        }
        return e.getAsInt();
    }
}
