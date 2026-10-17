package io.github.happysnappingturtle.mythicage.core.power;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 一个神力的数据（T12 设计文档 §2.1）：数据包 {@code data/<ns>/mythic_age/god_power/<id>.json}。
 * 数值服主可改；效果代码在 Java 里按 id 注册。
 *
 * @param id            神力 id（与物品 id 相同的路径部分，如 {@code gp_gr_bolt}）
 * @param card          设计卡编号（如 GP-GR-01），只作对照
 * @param castTicks     吟唱刻数（0–60）
 * @param cooldownTicks 本神力的长冷却
 * @param range         施放距离（格）；缺省按 form 取默认
 * @param selfCentered  范围型以施法者为中心（不选点）
 * @param large         大型神力（受神力护盾约束，§2.6）
 * @param matrix        12 列 → 反应键；缺省 {@code none}
 * @param durationTicks 提交后到结束的上界（表现包用）
 * @param radius        作用半径（瞄准时的范围圈；效果代码也用它）；0 表示没有范围
 */
public record PowerSpec(String id, String card, Civilization civ, Form form, int castTicks, int cooldownTicks, double range,
                        TargetProfile profile, boolean selfCentered, boolean large, Map<TargetCategory, String> matrix,
                        int durationTicks, double radius) {

    public enum Form {
        SCROLL(32), PLACEABLE(4.5), THROWABLE(24);

        private final double defaultRange;

        Form(double defaultRange) {
            this.defaultRange = defaultRange;
        }

        /** 05 号 §6 的施放距离默认值。 */
        public double defaultRange() {
            return defaultRange;
        }
    }

    public static final int MAX_CAST_TICKS = 60;
    private static final Pattern ID = Pattern.compile("[a-z0-9_]+");
    private static final Pattern KEY = Pattern.compile("[a-z0-9_]+");
    private static final Set<String> KEYS = Set.of("schemaVersion", "id", "card", "civilization", "form", "castTicks",
            "cooldownTicks", "range", "targetProfile", "selfCentered", "large", "matrix", "durationTicks", "radius");

    public PowerSpec {
        matrix = Map.copyOf(matrix);
    }

    public String reaction(TargetCategory c) {
        return matrix.getOrDefault(c, TargetClassifier.NONE);
    }

    public static final class PowerSpecException extends RuntimeException {
        public PowerSpecException(String message) {
            super(message);
        }
    }

    private static PowerSpecException err(String path, String msg) {
        return new PowerSpecException(path + ": " + msg);
    }

    public static PowerSpec parse(String json, List<String> warnings) {
        try {
            return parse(JsonParser.parseString(json), warnings);
        } catch (JsonSyntaxException e) {
            throw new PowerSpecException("$: JSON 语法错误：" + e.getMessage());
        }
    }

    /** 解析并校验；结构错误抛出带路径的 {@link PowerSpecException}，多余字段只告警。 */
    public static PowerSpec parse(JsonElement rootEl, List<String> warnings) {
        if (!rootEl.isJsonObject()) {
            throw err("$", "应为对象");
        }
        JsonObject o = rootEl.getAsJsonObject();
        for (String k : o.keySet()) {
            if (!KEYS.contains(k)) {
                warnings.add("$." + k + ": 未知字段，已忽略");
            }
        }
        if (!o.has("schemaVersion") || o.get("schemaVersion").getAsInt() != 1) {
            throw err("$.schemaVersion", "只支持 1");
        }
        String id = str(o, "id");
        if (!ID.matcher(id).matches()) {
            throw err("$.id", "只能由小写字母、数字、下划线组成：" + id);
        }
        Civilization civ = Civilization.byId(str(o, "civilization"))
                .orElseThrow(() -> err("$.civilization", "未知文明：" + o.get("civilization")));
        Form form;
        try {
            form = Form.valueOf(str(o, "form").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw err("$.form", "应为 scroll / placeable / throwable");
        }
        int cast = intIn(o, "castTicks", 0, MAX_CAST_TICKS, 20);
        int cooldown = intIn(o, "cooldownTicks", 0, 24000 * 7, 0);
        double range = o.has("range") ? o.get("range").getAsDouble() : form.defaultRange();
        if (range <= 0 || range > 128) {
            throw err("$.range", "应在 (0, 128]：" + range);
        }
        TargetProfile profile = TargetProfile.byId(str(o, "targetProfile"))
                .orElseThrow(() -> err("$.targetProfile", "未知目标入口：" + o.get("targetProfile")));
        Map<TargetCategory, String> matrix = new EnumMap<>(TargetCategory.class);
        if (o.has("matrix")) {
            if (!o.get("matrix").isJsonObject()) {
                throw err("$.matrix", "应为对象");
            }
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("matrix").entrySet()) {
                TargetCategory c = TargetCategory.byId(e.getKey())
                        .orElseThrow(() -> err("$.matrix." + e.getKey(), "未知列"));
                String v = e.getValue().getAsString();
                if (!KEY.matcher(v).matches()) {
                    throw err("$.matrix." + e.getKey(), "反应键只能由小写字母、数字、下划线组成：" + v);
                }
                matrix.put(c, v);
            }
        }
        double radius = o.has("radius") ? o.get("radius").getAsDouble() : 0;
        if (radius < 0 || radius > 64) {
            throw err("$.radius", "应在 [0, 64]：" + radius);
        }
        return new PowerSpec(id, o.has("card") ? o.get("card").getAsString() : "", civ, form, cast, cooldown, range, profile,
                o.has("selfCentered") && o.get("selfCentered").getAsBoolean(),
                o.has("large") && o.get("large").getAsBoolean(), matrix, intIn(o, "durationTicks", 0, 24000, 0), radius);
    }

    private static String str(JsonObject o, String k) {
        if (!o.has(k) || !o.get(k).isJsonPrimitive()) {
            throw err("$." + k, "缺少或不是字符串");
        }
        return o.get(k).getAsString();
    }

    private static int intIn(JsonObject o, String k, int min, int max, int fallback) {
        if (!o.has(k)) {
            return fallback;
        }
        int v;
        try {
            v = o.get(k).getAsInt();
        } catch (RuntimeException e) {
            throw err("$." + k, "应为整数");
        }
        if (v < min || v > max) {
            throw err("$." + k, "应在 [" + min + ", " + max + "]：" + v);
        }
        return v;
    }
}
