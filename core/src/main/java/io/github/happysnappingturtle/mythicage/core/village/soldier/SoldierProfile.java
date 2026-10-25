package io.github.happysnappingturtle.mythicage.core.village.soldier;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 兵种数据：职业 JSON 的 {@code soldier} 段（T20 设计文档 §10.1）。近战 / 远程、数值、时间轴、可选的盾、弹药、兵装、独有机制。
 * <p>
 * 归因 {@code attribution} 是硬规则（01 号 §0.1，总纲 §3.13）：三项都必须是 0，写了别的值报警告并按 0 处理，不开放配置。
 *
 * @param ability 独有机制 id（{@code SoldierAbility} 注册表里的键）；空串表示没有
 * @param anims   动作名：{@code idle}（站岗）、{@code alert}、{@code attack}、{@code block}、{@code patrol}；没写的用通用名
 */
public record SoldierProfile(Kind kind, int health, int armor, int damage, Timeline timeline, Melee melee, Shield shield,
                             Ranged ranged, Ammo ammo, Kits kits, int durability, double retreatBelow, double returnAbove,
                             String ability, java.util.Map<String, String> anims) {
    /** 没写动作名时的通用名。 */
    public static final java.util.Map<String, String> DEFAULT_ANIMS = java.util.Map.of("idle", "guard_idle", "alert", "alert",
            "attack", "attack", "block", "block_hold", "throw", "throw");

    public SoldierProfile {
        anims = java.util.Map.copyOf(anims);
    }

    public String anim(String key) {
        return anims.getOrDefault(key, DEFAULT_ANIMS.getOrDefault(key, key));
    }

    public enum Kind { MELEE, RANGED }

    /** 兵装档：完整、简易、没有（无兵装模式：只报警与护送）。 */
    public enum Tier {
        FULL, SIMPLE, NONE;

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        public static Tier byId(String s) {
            for (Tier t : values()) {
                if (t.id().equals(s)) {
                    return t;
                }
            }
            return NONE;
        }
    }

    /**
     * 出手时间轴（刻）：前摇 [0, windup)，其中最后 {@code lock} 刻锁定朝向；有效 [windup, windup + active)；
     * 恢复到 windup + active + recover；从出手开始算第 {@code interval} 刻起才能再次出手。
     */
    public record Timeline(int windup, int lock, int active, int recover, int interval) {
    }

    /** 近战胶囊：沿朝向从 {@link #START} 到 {@code reach} 的线段、半径 {@code radius}，垂直中心离地 {@code height}、半高 {@code halfHeight}。 */
    public record Melee(double reach, double radius, double height, double halfHeight) {
        public static final double START = 0.5;
    }

    /** 盾：正面 {@code arc}° 扇形减伤 {@code reduction}；一次最多举 {@code maxHold} 刻，之后至少 {@code gap} 刻不能再举。 */
    public record Shield(double arc, double reduction, int maxHold, int gap, int raise, int lower) {
    }

    /**
     * 远程：射程、最短距离（近身后撤）、初速、重力、水阻、连发次数与间隔、投后撤步的距离。
     */
    public record Ranged(double range, double minRange, double speed, double gravity, double waterDrag, int burst,
                         int burstGap, double backstep) {
    }

    /** 弹药：身上带几支；按顺序先用前面的；{@code recover} 是战后回收的完好率；落地后最多留 {@code lifetime} 刻。 */
    public record Ammo(int carry, List<AmmoItem> items, double recover, int lifetime) {
        public Ammo {
            items = List.copyOf(items);
        }
    }

    /** 一种弹药：物品与伤害修正。 */
    public record AmmoItem(String item, int damage) {
    }

    /** 一档兵装：物品与数值修正（护甲、伤害）。 */
    public record Kit(String item, int armor, int damage) {
    }

    /** 兵种的三种兵装：完整、简易、破损（完整兵装损坏后）。 */
    public record Kits(Kit full, Kit simple, String broken) {
        public Kit of(Tier t) {
            return switch (t) {
                case FULL -> full;
                case SIMPLE -> simple;
                case NONE -> null;
            };
        }

        public String item(Tier t) {
            Kit k = of(t);
            return k == null ? "" : k.item();
        }
    }

    public boolean isMelee() {
        return kind == Kind.MELEE;
    }

    public boolean hasShield() {
        return shield != null;
    }

    /** 这一档兵装下的护甲（不低于 0）。 */
    public int armor(Tier t) {
        Kit k = kits.of(t);
        return Math.max(0, armor + (k == null ? 0 : k.armor()));
    }

    /** 这一档兵装下的单击伤害（不低于 1）。 */
    public int damage(Tier t) {
        Kit k = kits.of(t);
        return Math.max(1, damage + (k == null ? 0 : k.damage()));
    }

    /** 数据有误。 */
    public static final class SoldierDataException extends RuntimeException {
        public SoldierDataException(String message) {
            super(message);
        }
    }

    /**
     * 解析 {@code soldier} 段。缺必需字段抛 {@link SoldierDataException}（消息里带路径）；可修正的问题写进 {@code warnings}。
     */
    public static SoldierProfile parse(JsonObject o, String path, List<String> warnings) {
        String k = str(o, "kind", "melee");
        Kind kind = switch (k) {
            case "melee" -> Kind.MELEE;
            case "ranged" -> Kind.RANGED;
            default -> throw new SoldierDataException(path + ".kind: 只能是 melee 或 ranged");
        };
        int health = positive(o, "health", 20, path);
        int armor = Math.max(0, integer(o, "armor", 0));
        int damage = positive(o, "damage", 4, path);
        JsonObject tl = obj(o, "timeline", path, true);
        Timeline timeline = new Timeline(Math.max(1, integer(tl, "windup", 12)), Math.max(0, integer(tl, "lock", 4)),
                Math.max(1, integer(tl, "active", 3)), Math.max(0, integer(tl, "recover", 9)), 0);
        int interval = Math.max(timeline.windup() + timeline.active() + timeline.recover(), integer(tl, "interval", 32));
        timeline = new Timeline(timeline.windup(), Math.min(timeline.lock(), timeline.windup()), timeline.active(),
                timeline.recover(), interval);
        Melee melee = null;
        Ranged ranged = null;
        Ammo ammo = null;
        if (kind == Kind.MELEE) {
            JsonObject m = obj(o, "melee", path, true);
            melee = new Melee(dbl(m, "reach", 2.5), dbl(m, "radius", 0.25), dbl(m, "height", 1.15), dbl(m, "halfHeight", 0.5));
            if (melee.reach() <= Melee.START) {
                throw new SoldierDataException(path + ".melee.reach: 必须大于 " + Melee.START);
            }
        } else {
            JsonObject r = obj(o, "ranged", path, true);
            ranged = new Ranged(dbl(r, "range", 12), dbl(r, "minRange", 4), dbl(r, "speed", 1.1), dbl(r, "gravity", 0.05),
                    dbl(r, "waterDrag", 0.8), Math.max(1, integer(r, "burst", 1)), Math.max(0, integer(r, "burstGap", 0)),
                    dbl(r, "backstep", 0));
            JsonObject a = obj(o, "ammo", path, true);
            List<AmmoItem> items = new ArrayList<>();
            if (a.has("items")) {
                for (JsonElement e : a.getAsJsonArray("items")) {
                    JsonObject it = e.getAsJsonObject();
                    String item = str(it, "item", "");
                    if (item.isEmpty()) {
                        throw new SoldierDataException(path + ".ammo.items: 物品 id 为空");
                    }
                    items.add(new AmmoItem(item, integer(it, "damage", 0)));
                }
            }
            if (items.isEmpty()) {
                throw new SoldierDataException(path + ".ammo.items: 远程兵种至少要有一种弹药");
            }
            ammo = new Ammo(positive(a, "carry", 6, path + ".ammo"), items, clamp(dbl(a, "recover", 0.6), 0, 1),
                    Math.max(20, integer(a, "lifetime", 1200)));
        }
        Shield shield = null;
        if (o.has("shield")) {
            JsonObject s = o.getAsJsonObject("shield");
            shield = new Shield(clamp(dbl(s, "arc", 120), 1, 360), clamp(dbl(s, "reduction", 0.6), 0, 1),
                    Math.max(1, integer(s, "maxHold", 40)), Math.max(0, integer(s, "gap", 15)),
                    Math.max(0, integer(s, "raise", 6)), Math.max(0, integer(s, "lower", 8)));
        }
        JsonObject ks = obj(o, "kits", path, true);
        Kits kits = new Kits(kit(ks, "full", path), kit(ks, "simple", path), str(ks, "broken", ""));
        int durability = positive(o, "durability", 60, path);
        double below = clamp(dbl(o, "retreatBelow", 0.3), 0, 1);
        double above = clamp(dbl(o, "returnAbove", 0.6), below, 1);
        if (o.has("attribution")) {
            JsonObject at = o.getAsJsonObject("attribution");
            for (String key : List.of("favor", "conduct", "firstKill")) {
                if (at.has(key) && at.get(key).getAsDouble() != 0) {
                    warnings.add(path + ".attribution." + key + ": 士兵归因是硬规则（×0），不能改，已按 0 处理");
                }
            }
        }
        java.util.Map<String, String> anims = new java.util.LinkedHashMap<>();
        if (o.has("anims")) {
            o.getAsJsonObject("anims").entrySet().forEach(e -> anims.put(e.getKey(), e.getValue().getAsString()));
        }
        return new SoldierProfile(kind, health, armor, damage, timeline, melee, shield, ranged, ammo, kits, durability, below,
                above, str(o, "ability", ""), anims);
    }

    private static Kit kit(JsonObject ks, String key, String path) {
        if (!ks.has(key)) {
            throw new SoldierDataException(path + ".kits." + key + ": 缺失");
        }
        JsonObject k = ks.getAsJsonObject(key);
        String item = str(k, "item", "");
        if (item.isEmpty()) {
            throw new SoldierDataException(path + ".kits." + key + ".item: 物品 id 为空");
        }
        return new Kit(item, integer(k, "armor", 0), integer(k, "damage", 0));
    }

    private static JsonObject obj(JsonObject o, String k, String path, boolean required) {
        if (o.has(k) && o.get(k).isJsonObject()) {
            return o.getAsJsonObject(k);
        }
        if (required) {
            throw new SoldierDataException(path + "." + k + ": 缺失或不是对象");
        }
        return new JsonObject();
    }

    private static int positive(JsonObject o, String k, int d, String path) {
        int v = integer(o, k, d);
        if (v <= 0) {
            throw new SoldierDataException(path + "." + k + ": 必须大于 0");
        }
        return v;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static int integer(JsonObject o, String k, int d) {
        return o.has(k) ? o.get(k).getAsInt() : d;
    }

    private static double dbl(JsonObject o, String k, double d) {
        return o.has(k) ? o.get(k).getAsDouble() : d;
    }

    private static String str(JsonObject o, String k, String d) {
        return o.has(k) ? o.get(k).getAsString() : d;
    }
}
