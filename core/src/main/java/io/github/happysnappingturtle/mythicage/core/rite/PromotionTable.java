package io.github.happysnappingturtle.mythicage.core.rite;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.trade.TradeOffer;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * 一个文明的晋升表（T11a 设计文档 §3.3），来自数据包 {@code data/<ns>/mythic_age/promotion/<文明>.json}。
 * 整份不可用（版本、文明不对）时抛出；单项不可用时跳过该项并记入 warnings；
 * 某个时代引用了未注册的建筑条件、或试炼档非法时，该时代标为 {@link AgeEntry#broken()}（不可举行）。
 *
 * @param ages   classical / heroic / mythic → 该级的条件；缺少的时代表示该文明暂不能晋升到那一级
 * @param trials 试炼档（classical、heroic）→ 属于这一档的 Boss 实体 id
 */
public record PromotionTable(Civilization civ, Map<Age, AgeEntry> ages, Map<Age, List<String>> trials) {
    public static final int MAX_OFFERINGS = 6;
    public static final int MAX_OFFERING_COUNT = 999;
    private static final Pattern ITEM = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Pattern TYPE = Pattern.compile("[a-z0-9_]+:[a-z0-9_]+");
    private static final Set<String> ROOT_KEYS = Set.of("schemaVersion", "civilization", "ages", "trials");
    private static final Set<String> AGE_KEYS = Set.of("favor", "offerings", "trial", "requirements", "extraUnlocks");

    public PromotionTable {
        ages = Map.copyOf(ages);
        trials = Map.copyOf(trials);
    }

    /**
     * @param trial        需要消耗的 Boss 凭证档；NONE 表示不需要
     * @param requirements 建筑条件（类型 + 原样参数，由加载层的注册表解释）
     * @param broken       数据错误：不可举行
     */
    public record AgeEntry(long favor, List<TradeOffer.ItemAmount> offerings, Age trial,
                           List<RequirementSpec> requirements, List<ExtraUnlock> extraUnlocks, boolean broken) {
        public AgeEntry {
            offerings = List.copyOf(offerings);
            requirements = List.copyOf(requirements);
            extraUnlocks = List.copyOf(extraUnlocks);
        }
    }

    public record RequirementSpec(String type, JsonObject params) {
    }

    /** 成功界面上额外列出的解锁项。 */
    public record ExtraUnlock(String icon, String key) {
    }

    public static final class PromotionTableException extends RuntimeException {
        public PromotionTableException(String message) {
            super(message);
        }
    }

    public static PromotionTable empty(Civilization civ) {
        return new PromotionTable(civ, Map.of(), Map.of());
    }

    public Optional<AgeEntry> entry(Age age) {
        return Optional.ofNullable(ages.get(age));
    }

    /** 某个 Boss 属于哪一档试炼；不是本文明的试炼 Boss 时为空。 */
    public Optional<Age> trialTierOf(String bossId) {
        for (Map.Entry<Age, List<String>> e : trials.entrySet()) {
            if (e.getValue().contains(bossId)) {
                return Optional.of(e.getKey());
            }
        }
        return Optional.empty();
    }

    public static PromotionTable parse(String json, Function<String, OptionalInt> maxStack,
                                       Predicate<String> knownRequirement, List<String> warnings) {
        try {
            return parse(JsonParser.parseString(json), maxStack, knownRequirement, warnings);
        } catch (JsonSyntaxException e) {
            throw new PromotionTableException("(根): JSON 语法错误：" + e.getMessage());
        }
    }

    /**
     * @param maxStack         物品 id → 最大堆叠；物品不存在时为空
     * @param knownRequirement 建筑条件类型是否已注册
     */
    public static PromotionTable parse(JsonElement rootEl, Function<String, OptionalInt> maxStack,
                                       Predicate<String> knownRequirement, List<String> warnings) {
        if (rootEl == null || !rootEl.isJsonObject()) {
            throw new PromotionTableException("(根): 缺失或不是对象");
        }
        JsonObject root = rootEl.getAsJsonObject();
        JsonElement v = root.get("schemaVersion");
        if (v == null || !v.isJsonPrimitive() || !v.getAsJsonPrimitive().isNumber() || v.getAsInt() != 1) {
            throw new PromotionTableException("schemaVersion: 只支持 1");
        }
        JsonElement civEl = root.get("civilization");
        String civId = civEl != null && civEl.isJsonPrimitive() ? civEl.getAsString() : "";
        Civilization civ = Civilization.byId(civId)
                .orElseThrow(() -> new PromotionTableException("civilization: 未知文明 " + civId));
        for (String k : root.keySet()) {
            if (!ROOT_KEYS.contains(k)) {
                warnings.add(k + ": 未知字段，已忽略");
            }
        }
        Map<Age, AgeEntry> ages = new EnumMap<>(Age.class);
        JsonElement agesEl = root.get("ages");
        if (agesEl != null && !agesEl.isJsonObject()) {
            warnings.add("ages: 应为对象，已视为没有晋升");
        } else if (agesEl != null) {
            JsonObject o = agesEl.getAsJsonObject();
            for (String key : o.keySet()) {
                Optional<Age> age = Age.byId(key).filter(PromotionTable::promotable);
                if (age.isEmpty()) {
                    warnings.add("ages." + key + ": 只能是 classical / heroic / mythic，已跳过");
                    continue;
                }
                JsonElement e = o.get(key);
                if (!e.isJsonObject()) {
                    warnings.add("ages." + key + ": 应为对象，已跳过");
                    continue;
                }
                ages.put(age.get(), ageEntry(e.getAsJsonObject(), "ages." + key, maxStack, knownRequirement, warnings));
            }
        }
        Map<Age, List<String>> trials = new EnumMap<>(Age.class);
        JsonElement trialsEl = root.get("trials");
        if (trialsEl != null && !trialsEl.isJsonObject()) {
            warnings.add("trials: 应为对象，已忽略");
        } else if (trialsEl != null) {
            JsonObject o = trialsEl.getAsJsonObject();
            for (String key : o.keySet()) {
                Optional<Age> tier = Age.byId(key).filter(PromotionTable::trialTier);
                if (tier.isEmpty() || !o.get(key).isJsonArray()) {
                    warnings.add("trials." + key + ": 档只能是 classical / heroic，值应为数组，已跳过");
                    continue;
                }
                List<String> bosses = new ArrayList<>();
                for (JsonElement b : o.get(key).getAsJsonArray()) {
                    if (b.isJsonPrimitive() && ITEM.matcher(b.getAsString()).matches()) {
                        bosses.add(b.getAsString());
                    } else {
                        warnings.add("trials." + key + ": 非法实体 id " + b + "，已跳过");
                    }
                }
                trials.put(tier.get(), List.copyOf(bosses));
            }
        }
        return new PromotionTable(civ, ages, trials);
    }

    /** 晋升能到达的时代（远古来自初见祝福）。 */
    public static boolean promotable(Age a) {
        return a == Age.CLASSICAL || a == Age.HEROIC || a == Age.MYTHIC;
    }

    /** 试炼档：古典试炼（凭证用于英雄级）、英雄试炼（用于神话级）。 */
    public static boolean trialTier(Age a) {
        return a == Age.CLASSICAL || a == Age.HEROIC;
    }

    private static AgeEntry ageEntry(JsonObject o, String path, Function<String, OptionalInt> maxStack,
                                     Predicate<String> knownRequirement, List<String> warnings) {
        boolean broken = false;
        for (String k : o.keySet()) {
            if (!AGE_KEYS.contains(k)) {
                warnings.add(path + "." + k + ": 未知字段，已忽略");
            }
        }
        long favor = 0;
        JsonElement f = o.get("favor");
        if (f == null || !f.isJsonPrimitive() || !f.getAsJsonPrimitive().isNumber() || f.getAsLong() < 0
                || f.getAsLong() > 10_000) {
            warnings.add(path + ".favor: 应为 0–10000 的整数，该时代不可举行");
            broken = true;
        } else {
            favor = f.getAsLong();
        }
        List<TradeOffer.ItemAmount> offerings = new ArrayList<>();
        Map<String, Integer> merged = new LinkedHashMap<>();
        JsonElement off = o.get("offerings");
        if (off != null && !off.isJsonArray()) {
            warnings.add(path + ".offerings: 应为数组，已视为没有供品");
        } else if (off != null) {
            JsonArray arr = off.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                String ip = path + ".offerings[" + i + "]";
                if (!arr.get(i).isJsonObject()) {
                    warnings.add(ip + ": 不是对象，已跳过");
                    continue;
                }
                JsonObject it = arr.get(i).getAsJsonObject();
                String item = it.has("item") && it.get("item").isJsonPrimitive() ? it.get("item").getAsString() : "";
                if (!ITEM.matcher(item).matches() || maxStack.apply(item).isEmpty()) {
                    warnings.add(ip + ".item: 物品不存在 " + item + "，已跳过");
                    continue;
                }
                JsonElement c = it.get("count");
                int count = c != null && c.isJsonPrimitive() && c.getAsJsonPrimitive().isNumber() ? c.getAsInt() : -1;
                if (count < 1 || count > MAX_OFFERING_COUNT) {
                    warnings.add(ip + ".count: 应在 1–" + MAX_OFFERING_COUNT + " 之间，已跳过");
                    continue;
                }
                merged.merge(item, count, Integer::sum);
            }
        }
        if (merged.size() > MAX_OFFERINGS) {
            warnings.add(path + ".offerings: 最多 " + MAX_OFFERINGS + " 种物品，多出的已跳过");
        }
        merged.entrySet().stream().limit(MAX_OFFERINGS)
                .forEach(e -> offerings.add(new TradeOffer.ItemAmount(e.getKey(), e.getValue())));
        Age trial = Age.NONE;
        JsonElement t = o.get("trial");
        if (t != null && !(t.isJsonPrimitive() && t.getAsString().isEmpty())) {
            Optional<Age> tier = t.isJsonPrimitive() ? Age.byId(t.getAsString()).filter(PromotionTable::trialTier)
                    : Optional.empty();
            if (tier.isEmpty()) {
                warnings.add(path + ".trial: 只能是 classical / heroic / 空，该时代不可举行");
                broken = true;
            } else {
                trial = tier.get();
            }
        }
        List<RequirementSpec> requirements = new ArrayList<>();
        JsonElement req = o.get("requirements");
        if (req != null && !req.isJsonArray()) {
            warnings.add(path + ".requirements: 应为数组，该时代不可举行");
            broken = true;
        } else if (req != null) {
            JsonArray arr = req.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                String rp = path + ".requirements[" + i + "]";
                JsonElement r = arr.get(i);
                String type = r.isJsonObject() && r.getAsJsonObject().has("type")
                        && r.getAsJsonObject().get("type").isJsonPrimitive() ? r.getAsJsonObject().get("type").getAsString() : "";
                if (!TYPE.matcher(type).matches() || !knownRequirement.test(type)) {
                    warnings.add(rp + ".type: 未注册的建筑条件 " + type + "，该时代不可举行");
                    broken = true;
                    continue;
                }
                requirements.add(new RequirementSpec(type, r.getAsJsonObject().deepCopy()));
            }
        }
        List<ExtraUnlock> extras = new ArrayList<>();
        JsonElement ex = o.get("extraUnlocks");
        if (ex != null && ex.isJsonArray()) {
            JsonArray arr = ex.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                JsonElement e = arr.get(i);
                String icon = e.isJsonObject() && e.getAsJsonObject().has("icon") ? e.getAsJsonObject().get("icon").getAsString() : "";
                String key = e.isJsonObject() && e.getAsJsonObject().has("key") ? e.getAsJsonObject().get("key").getAsString() : "";
                if (!ITEM.matcher(icon).matches() || maxStack.apply(icon).isEmpty() || key.isEmpty()) {
                    warnings.add(path + ".extraUnlocks[" + i + "]: 需要已注册的 icon 物品与非空 key，已跳过");
                    continue;
                }
                extras.add(new ExtraUnlock(icon, key));
            }
        } else if (ex != null) {
            warnings.add(path + ".extraUnlocks: 应为数组，已忽略");
        }
        return new AgeEntry(favor, offerings, trial, requirements, extras, broken);
    }
}
