package io.github.happysnappingturtle.mythicage.summon;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.core.summon.SummonUnitCard;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 神话单位卡（{@code data/<ns>/mythic_age/summon_unit/<文明>/<id>.json}）与单位数值
 * （{@code data/<ns>/mythic_age/entity_stats/<文明>/summon_<id>.json}）。T14 设计文档 §1。
 */
public final class SummonUnits {
    private static volatile Map<String, SummonUnitCard> cards = Map.of();
    private static volatile Map<String, Stats> stats = Map.of();

    /** 单位数值：属性（生命、普攻、移速……）与特殊攻击参数。 */
    public record Stats(Map<String, Double> attributes, JsonObject special) {
        public double attr(String key, double fallback) {
            return attributes.getOrDefault(key, fallback);
        }

        public int specialInt(String key, int fallback) {
            return special.has(key) ? special.get(key).getAsInt() : fallback;
        }

        public double specialDouble(String key, double fallback) {
            return special.has(key) ? special.get(key).getAsDouble() : fallback;
        }
    }

    private SummonUnits() {
    }

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new CardLoader());
        event.addListener(new StatsLoader());
    }

    /** 文明:id → 单位卡。 */
    public static Optional<SummonUnitCard> card(String key) {
        return Optional.ofNullable(cards.get(key));
    }

    public static Map<String, SummonUnitCard> all() {
        return cards;
    }

    /** 文明:id → 数值（缺失时返回空表，单位用自身默认值）。 */
    public static Stats stats(String key) {
        Stats s = stats.get(key);
        return s != null ? s : new Stats(Map.of(), new JsonObject());
    }

    /** 测试用：直接放一张卡（GameTest 不依赖数据包重载）。 */
    public static void putForTest(SummonUnitCard card) {
        Map<String, SummonUnitCard> next = new HashMap<>(cards);
        next.put(card.key(), card);
        cards = Map.copyOf(next);
    }

    /** {@code summon_unit/greek/minotaur} → {@code greek:minotaur}。 */
    private static String[] civAndId(ResourceLocation file) {
        String path = file.getPath();
        int slash = path.indexOf('/');
        if (slash < 0) {
            return null;
        }
        return new String[]{path.substring(0, slash), path.substring(slash + 1)};
    }

    private static final class CardLoader extends SimpleJsonResourceReloadListener {
        CardLoader() {
            super(new Gson(), "mythic_age/summon_unit");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
            Map<String, SummonUnitCard> next = new HashMap<>();
            files.forEach((file, json) -> {
                String[] ci = civAndId(file);
                if (ci == null) {
                    MythicAge.LOGGER.error("单位卡 {} 应放在 summon_unit/<文明>/ 下，已忽略", file);
                    return;
                }
                try {
                    SummonUnitCard c = SummonUnitCard.parse(ci[0], ci[1], json.getAsJsonObject());
                    next.put(c.key(), c);
                } catch (RuntimeException e) {
                    MythicAge.LOGGER.error("单位卡 {} 解析失败，已忽略：{}", file, e.getMessage());
                }
            });
            cards = Map.copyOf(next);
            MythicAge.LOGGER.info("神话单位卡：{} 张", next.size());
        }
    }

    private static final class StatsLoader extends SimpleJsonResourceReloadListener {
        StatsLoader() {
            super(new Gson(), "mythic_age/entity_stats");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
            Map<String, Stats> next = new HashMap<>();
            files.forEach((file, json) -> {
                String[] ci = civAndId(file);
                if (ci == null || !ci[1].startsWith("summon_")) {
                    return;
                }
                try {
                    JsonObject o = json.getAsJsonObject();
                    Map<String, Double> attrs = new HashMap<>();
                    for (var e : o.entrySet()) {
                        if (e.getValue().isJsonPrimitive() && e.getValue().getAsJsonPrimitive().isNumber()) {
                            attrs.put(e.getKey(), e.getValue().getAsDouble());
                        }
                    }
                    JsonObject special = o.has("special") ? o.getAsJsonObject("special") : new JsonObject();
                    next.put(ci[0] + ":" + ci[1].substring("summon_".length()), new Stats(Map.copyOf(attrs), special));
                } catch (RuntimeException e) {
                    MythicAge.LOGGER.error("单位数值 {} 解析失败，已忽略：{}", file, e.getMessage());
                }
            });
            stats = Map.copyOf(next);
        }
    }
}
