package io.github.happysnappingturtle.mythicage.core.trade;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 一个文明的交易表（T9 设计文档 §4），来自数据包 {@code data/<ns>/mythic_age/trade/<文明>.json}。
 * 校验失败时抛出以字段路径开头的 {@link TradeTableException}；由加载器决定保留上一版。
 */
public record TradeTable(Civilization civ, List<TradeOffer> offers) {
    public static final int SCHEMA_VERSION = 1;
    public static final long MAX_FAVOR_PRICE = 1000;
    public static final int MAX_PRICE_ITEMS = 3;
    private static final Pattern ID = Pattern.compile("[a-z0-9_]+");
    private static final Set<String> ROOT_KEYS = Set.of("schemaVersion", "civilization", "offers");
    private static final Set<String> OFFER_KEYS = Set.of("id", "category", "result", "price", "minAge", "venues", "dailyLimit");

    public TradeTable {
        offers = List.copyOf(offers);
    }

    public static TradeTable empty(Civilization civ) {
        return new TradeTable(civ, List.of());
    }

    public Optional<TradeOffer> offer(String id) {
        return offers.stream().filter(o -> o.id().equals(id)).findFirst();
    }

    public List<TradeOffer> offersAt(TradeOffer.Venue venue) {
        return offers.stream().filter(o -> o.venues().contains(venue)).toList();
    }

    /** 物品查询：已注册时返回最大堆叠，未注册时为空。由加载器接到物品注册表上。 */
    @FunctionalInterface
    public interface ItemLookup {
        OptionalInt maxStack(String itemId);

        /** 只校验语法、不查注册表（单元测试用）：任何 id 都当作最大堆叠 64。 */
        ItemLookup ANY = id -> OptionalInt.of(64);
    }

    public static final class TradeTableException extends RuntimeException {
        public TradeTableException(String message) {
            super(message);
        }
    }

    public static TradeTable parse(String json, ItemLookup items) {
        try {
            return parse(JsonParser.parseString(json), items);
        } catch (JsonSyntaxException e) {
            throw new TradeTableException("(根): JSON 语法错误：" + e.getMessage());
        }
    }

    public static TradeTable parse(JsonElement rootEl, ItemLookup items) {
        JsonObject root = object(rootEl, "(根)");
        unknownKeys(root, ROOT_KEYS, "");
        int version = intIn(root, "schemaVersion", "schemaVersion", 1, 1000);
        if (version != SCHEMA_VERSION) {
            throw new TradeTableException("schemaVersion: 只支持 " + SCHEMA_VERSION + "，实际 " + version);
        }
        String civId = string(root, "civilization", "civilization");
        Civilization civ = Civilization.byId(civId)
                .orElseThrow(() -> new TradeTableException("civilization: 未知文明 " + civId));
        JsonElement arrEl = root.get("offers");
        if (arrEl == null || !arrEl.isJsonArray()) {
            throw new TradeTableException("offers: 缺失或不是数组");
        }
        JsonArray arr = arrEl.getAsJsonArray();
        List<TradeOffer> offers = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < arr.size(); i++) {
            String path = "offers[" + i + "]";
            TradeOffer offer = offer(object(arr.get(i), path), path, items);
            if (!ids.add(offer.id())) {
                throw new TradeTableException(path + ".id: 重复的商品 id " + offer.id());
            }
            offers.add(offer);
        }
        return new TradeTable(civ, offers);
    }

    private static TradeOffer offer(JsonObject o, String path, ItemLookup items) {
        unknownKeys(o, OFFER_KEYS, path + ".");
        String id = string(o, "id", path + ".id");
        if (!ID.matcher(id).matches()) {
            throw new TradeTableException(path + ".id: 只能用小写字母、数字和下划线，实际 " + id);
        }
        String catId = string(o, "category", path + ".category");
        TradeOffer.Category category = TradeOffer.Category.byId(catId)
                .orElseThrow(() -> new TradeTableException(path + ".category: 未知分类 " + catId));
        TradeOffer.ItemAmount result = itemAmount(object(o.get("result"), path + ".result"), path + ".result", items);
        TradeOffer.Price price = price(o.get("price"), path + ".price", items);
        String ageId = string(o, "minAge", path + ".minAge");
        Age minAge = Age.byId(ageId).filter(a -> a != Age.NONE)
                .orElseThrow(() -> new TradeTableException(path + ".minAge: 未知时代 " + ageId));
        JsonElement vEl = o.get("venues");
        if (vEl == null || !vEl.isJsonArray() || vEl.getAsJsonArray().isEmpty()) {
            throw new TradeTableException(path + ".venues: 缺失或不是非空数组");
        }
        EnumSet<TradeOffer.Venue> venues = EnumSet.noneOf(TradeOffer.Venue.class);
        JsonArray vArr = vEl.getAsJsonArray();
        for (int i = 0; i < vArr.size(); i++) {
            JsonElement v = vArr.get(i);
            String vp = path + ".venues[" + i + "]";
            if (!v.isJsonPrimitive() || !v.getAsJsonPrimitive().isString()) {
                throw new TradeTableException(vp + ": 不是字符串");
            }
            venues.add(TradeOffer.Venue.byId(v.getAsString())
                    .orElseThrow(() -> new TradeTableException(vp + ": 未知地点 " + v.getAsString())));
        }
        int dailyLimit = o.has("dailyLimit") ? intIn(o, "dailyLimit", path + ".dailyLimit", 1, 1000) : 0;
        return new TradeOffer(id, category, result, price, minAge, venues, dailyLimit);
    }

    private static TradeOffer.Price price(JsonElement el, String path, ItemLookup items) {
        JsonObject p = object(el, path);
        unknownKeys(p, Set.of("favor", "items"), path + ".");
        boolean hasFavor = p.has("favor");
        boolean hasItems = p.has("items");
        if (hasFavor == hasItems) {
            throw new TradeTableException(path + ": favor 与 items 必须二选一");
        }
        if (hasFavor) {
            return TradeOffer.Price.ofFavor(intIn(p, "favor", path + ".favor", 1, (int) MAX_FAVOR_PRICE));
        }
        JsonElement arrEl = p.get("items");
        if (!arrEl.isJsonArray() || arrEl.getAsJsonArray().isEmpty() || arrEl.getAsJsonArray().size() > MAX_PRICE_ITEMS) {
            throw new TradeTableException(path + ".items: 必须是 1–" + MAX_PRICE_ITEMS + " 种物品的数组");
        }
        Map<String, TradeOffer.ItemAmount> merged = new LinkedHashMap<>();
        JsonArray arr = arrEl.getAsJsonArray();
        for (int i = 0; i < arr.size(); i++) {
            String ip = path + ".items[" + i + "]";
            TradeOffer.ItemAmount a = itemAmount(object(arr.get(i), ip), ip, items);
            if (merged.put(a.item(), a) != null) {
                throw new TradeTableException(ip + ".item: 重复的物品 " + a.item());
            }
        }
        return TradeOffer.Price.ofItems(List.copyOf(merged.values()));
    }

    private static TradeOffer.ItemAmount itemAmount(JsonObject o, String path, ItemLookup items) {
        unknownKeys(o, Set.of("item", "count"), path + ".");
        String item = string(o, "item", path + ".item");
        OptionalInt max = items.maxStack(item);
        if (max.isEmpty()) {
            throw new TradeTableException(path + ".item: 未注册的物品 " + item);
        }
        int count = intIn(o, "count", path + ".count", 1, max.getAsInt());
        return new TradeOffer.ItemAmount(item, count);
    }

    private static void unknownKeys(JsonObject o, Set<String> allowed, String prefix) {
        for (String k : o.keySet()) {
            if (!allowed.contains(k)) {
                throw new TradeTableException(prefix + k + ": 未知字段");
            }
        }
    }

    private static JsonObject object(JsonElement e, String path) {
        if (e == null || !e.isJsonObject()) {
            throw new TradeTableException(path + ": 缺失或不是对象");
        }
        return e.getAsJsonObject();
    }

    private static String string(JsonObject o, String key, String path) {
        JsonElement e = o.get(key);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString() || e.getAsString().isEmpty()) {
            throw new TradeTableException(path + ": 缺失或不是非空字符串");
        }
        return e.getAsString();
    }

    private static int intIn(JsonObject o, String key, String path, int min, int max) {
        JsonElement e = o.get(key);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) {
            throw new TradeTableException(path + ": 缺失或不是数字");
        }
        double d = e.getAsDouble();
        if (!Double.isFinite(d) || d != Math.rint(d) || d < min || d > max) {
            throw new TradeTableException(path + ": 必须是 " + min + "–" + max + " 的整数，实际 " + e.getAsString());
        }
        return (int) d;
    }
}
