package io.github.happysnappingturtle.mythicage.core.relic;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 圣物卡（T16 设计文档 §2.1，数据包 {@code relic/<civ>/<id>.json}）。数值在数据里，效果代码按效果组 id 分派。
 *
 * @param id   {@code <文明>:<文件名>}，如 {@code greek:rheia_crown}；绑定表的键
 * @param item 物品 id
 */
public record RelicCard(String id, String item, Civilization civ, List<EffectGroups.Effect> worn,
                        List<EffectGroups.Effect> offered) {
    public RelicCard {
        Objects.requireNonNull(id);
        Objects.requireNonNull(item);
        Objects.requireNonNull(civ);
        worn = List.copyOf(worn);
        offered = List.copyOf(offered);
    }

    /** 解析一张卡；不合格时抛出 {@link IllegalArgumentException}，消息指出字段（调用方记日志并跳过）。 */
    public static RelicCard parse(String civId, String name, JsonObject o) {
        String id = civId + ":" + name;
        Civilization civ = Civilization.byId(civId)
                .orElseThrow(() -> new IllegalArgumentException("圣物卡 " + id + "：未知文明（目录名）" + civId));
        if (!o.has("item") || !o.get("item").isJsonPrimitive()) {
            throw new IllegalArgumentException("圣物卡 " + id + "：缺少 item");
        }
        if (!o.has("civ") || !o.get("civ").isJsonPrimitive()) {
            throw new IllegalArgumentException("圣物卡 " + id + "：缺少 civ");
        }
        if (!civId.equals(o.get("civ").getAsString())) {
            throw new IllegalArgumentException("圣物卡 " + id + "：civ「" + o.get("civ").getAsString() + "」与目录不一致");
        }
        return new RelicCard(id, o.get("item").getAsString(), civ, effects(id, o, "worn"), effects(id, o, "offered"));
    }

    private static List<EffectGroups.Effect> effects(String id, JsonObject o, String key) {
        List<EffectGroups.Effect> out = new ArrayList<>();
        if (!o.has(key)) {
            return out;
        }
        for (JsonElement e : o.getAsJsonArray(key)) {
            JsonObject eo = e.getAsJsonObject();
            if (!eo.has("group") || !eo.has("value")) {
                throw new IllegalArgumentException("圣物卡 " + id + "：" + key + " 的条目缺少 group 或 value");
            }
            String group = eo.get("group").getAsString();
            double value = eo.get("value").getAsDouble();
            EffectGroups.Kind kind = EffectGroups.kindOf(group)
                    .orElseThrow(() -> new IllegalArgumentException("圣物卡 " + id + "：" + key + " 引用了未实现的效果组 " + group));
            if (value < 0) {
                throw new IllegalArgumentException("圣物卡 " + id + "：" + key + "." + group + " 数值为负");
            }
            if (kind != EffectGroups.Kind.FLAT && value > 1) {
                throw new IllegalArgumentException("圣物卡 " + id + "：" + key + "." + group + " 是百分比，不能超过 1");
            }
            out.add(new EffectGroups.Effect(group, value));
        }
        return out;
    }

    /**
     * 按读入顺序去重：两张卡指向同一个物品时，第二张报错并跳过。
     *
     * @return 圣物 ID → 卡（保持顺序）
     */
    public static java.util.Map<String, RelicCard> dedupe(List<RelicCard> cards, java.util.function.Consumer<String> errors) {
        java.util.Map<String, RelicCard> byId = new java.util.LinkedHashMap<>();
        java.util.Map<String, String> byItem = new java.util.HashMap<>();
        for (RelicCard c : cards) {
            String owner = byItem.putIfAbsent(c.item(), c.id());
            if (owner != null) {
                errors.accept("圣物卡 " + c.id() + "：物品 " + c.item() + " 已被 " + owner + " 使用，跳过");
                continue;
            }
            byId.put(c.id(), c);
        }
        return byId;
    }

    /** 某种模式下某组的数值；没有时为 0。 */
    public double value(RelicState mode, String group) {
        List<EffectGroups.Effect> list = mode == RelicState.WORN ? worn : mode == RelicState.OFFERED ? offered : List.of();
        double v = 0;
        for (EffectGroups.Effect e : list) {
            if (e.group().equals(group)) {
                v = Math.max(v, e.value());
            }
        }
        return v;
    }
}
