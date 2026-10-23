package io.github.happysnappingturtle.mythicage.core.equipment;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 文明装备数值表（T17 设计文档 §1）：{@code mythic_age/equipment/<文明>.json}，物品注册与校验脚本共用。
 * 伤害、攻速是武器面板上的数值（原版口径）；注册属性修饰时用 {@link #damageModifier} / {@link #speedModifier} 换算。
 */
public record EquipmentTable(Map<String, ArmorSet> armor, Map<String, Weapon> weapons, Map<String, SetBonus> sets) {
    /** 玩家基础攻击伤害与攻速（原版）。 */
    public static final double BASE_DAMAGE = 1.0;
    public static final double BASE_SPEED = 4.0;

    /**
     * 一套护甲。
     *
     * @param points     头 / 胸 / 腿 / 靴的护甲点
     * @param durability 每件的绝对耐久（倍率可能不是整数，所以直接写每件）
     */
    public record ArmorSet(String id, String source, int[] points, double toughness, int[] durability, int enchantability) {
        public int total() {
            int t = 0;
            for (int p : points) {
                t += p;
            }
            return t;
        }
    }

    /** 一件武器或盾、弓；没有的字段为 0 / false。 */
    public record Weapon(String id, String source, double damage, double speed, double reach, int durability,
                         boolean sweep, boolean blunt, double thrustMultiplier, int counterTicks, double counterBonus,
                         int drawTicks) {
    }

    /** 套装效果：穿满 pieces 件 armor 套，半径 radius 内每名友方 +perAlly，最多 max。 */
    public record SetBonus(String id, String source, String armor, int pieces, double radius, int perAlly, int max) {
    }

    public static double damageModifier(double panelDamage) {
        return panelDamage - BASE_DAMAGE;
    }

    public static double speedModifier(double panelSpeed) {
        return panelSpeed - BASE_SPEED;
    }

    /** 每件耐久 = 部位基数（头 11、胸 16、腿 15、靴 13）× 倍率，四舍五入（10 号合同 §0）。 */
    public static int[] durability(double multiplier) {
        int[] base = {11, 16, 15, 13};
        int[] out = new int[4];
        for (int i = 0; i < 4; i++) {
            out[i] = (int) Math.round(base[i] * multiplier);
        }
        return out;
    }

    public Optional<Weapon> weapon(String id) {
        return Optional.ofNullable(weapons.get(id));
    }

    public static EquipmentTable parse(String json) {
        return parse(JsonParser.parseString(json).getAsJsonObject());
    }

    /** 解析；不合格时抛出 {@link IllegalArgumentException}，消息指出字段。 */
    public static EquipmentTable parse(JsonObject root) {
        Map<String, ArmorSet> armor = new LinkedHashMap<>();
        Map<String, Weapon> weapons = new LinkedHashMap<>();
        Map<String, SetBonus> sets = new LinkedHashMap<>();
        if (root.has("armor")) {
            for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("armor").entrySet()) {
                JsonObject o = e.getValue().getAsJsonObject();
                String id = e.getKey();
                int[] points = ints(o, "points", id);
                int[] dur = ints(o, "durability", id);
                for (int i = 0; i < 4; i++) {
                    if (points[i] < 0 || dur[i] <= 0) {
                        throw new IllegalArgumentException("护甲 " + id + "：points / durability 不能为负或 0");
                    }
                }
                double tough = num(o, "toughness", id, 0);
                armor.put(id, new ArmorSet(id, str(o, "source"), points, tough, dur, (int) num(o, "enchantability", id, 0)));
            }
        }
        if (root.has("weapons")) {
            for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("weapons").entrySet()) {
                JsonObject o = e.getValue().getAsJsonObject();
                String id = e.getKey();
                double damage = num(o, "damage", id, 0);
                double speed = num(o, "speed", id, 0);
                if (damage < 0) {
                    throw new IllegalArgumentException("武器 " + id + "：damage 为负");
                }
                if (o.has("speed") && speed <= 0) {
                    throw new IllegalArgumentException("武器 " + id + "：speed 必须大于 0");
                }
                int durability = (int) num(o, "durability", id, 0);
                if (durability <= 0) {
                    throw new IllegalArgumentException("武器 " + id + "：缺少 durability");
                }
                weapons.put(id, new Weapon(id, str(o, "source"), damage, speed, num(o, "reach", id, 0), durability,
                        o.has("sweep") && o.get("sweep").getAsBoolean(), o.has("blunt") && o.get("blunt").getAsBoolean(),
                        num(o, "thrust_multiplier", id, 0), (int) num(o, "counter_ticks", id, 0),
                        num(o, "counter_bonus", id, 0), (int) num(o, "draw_ticks", id, 0)));
            }
        }
        if (root.has("sets")) {
            for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("sets").entrySet()) {
                JsonObject o = e.getValue().getAsJsonObject();
                String id = e.getKey();
                String set = str(o, "armor");
                if (!armor.containsKey(set)) {
                    throw new IllegalArgumentException("套装 " + id + "：armor「" + set + "」不在表里");
                }
                sets.put(id, new SetBonus(id, str(o, "source"), set, (int) num(o, "pieces", id, 4), num(o, "radius", id, 0),
                        (int) num(o, "per_ally", id, 0), (int) num(o, "max", id, 0)));
            }
        }
        return new EquipmentTable(Map.copyOf(armor), Map.copyOf(weapons), Map.copyOf(sets));
    }

    private static int[] ints(JsonObject o, String key, String id) {
        if (!o.has(key) || !o.get(key).isJsonArray()) {
            throw new IllegalArgumentException("护甲 " + id + "：缺少 " + key);
        }
        JsonArray a = o.getAsJsonArray(key);
        if (a.size() != 4) {
            throw new IllegalArgumentException("护甲 " + id + "：" + key + " 应有 4 个（头 / 胸 / 腿 / 靴），实际 " + a.size());
        }
        int[] out = new int[4];
        for (int i = 0; i < 4; i++) {
            out[i] = a.get(i).getAsInt();
        }
        return out;
    }

    private static double num(JsonObject o, String key, String id, double def) {
        if (!o.has(key)) {
            return def;
        }
        try {
            return o.get(key).getAsDouble();
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(id + "：" + key + " 不是数字");
        }
    }

    private static String str(JsonObject o, String key) {
        return o.has(key) ? o.get(key).getAsString() : "";
    }
}
