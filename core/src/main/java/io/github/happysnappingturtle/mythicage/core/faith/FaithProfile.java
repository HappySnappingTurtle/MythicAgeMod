package io.github.happysnappingturtle.mythicage.core.faith;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * 文明档案（T10b 设计文档 §2；19 号 U2：每个文明都有祭司，名字不同、作用相同）。
 * 来自数据包 {@code data/<ns>/mythic_age/faith/<文明>.json}。界面上与文明有关的名字都从这里取，
 * 通用代码里不写任何文明的名字。仪式一律用通用名（晋升 / 立祀 / 改奉，用户 2026-10-16：本土名玩家看不懂）。没有档案的文明视为「未开放」：不生成祭司、不能交易。
 *
 * @param guardian       祭司名的翻译 key
 * @param shrine         神庙名的翻译 key
 * @param favor          「X 恩宠」的翻译 key
 * @param hint           未接触该文明时的线索（信仰面板）；可为空
 * @param firstMeetGifts 第一次与祭司交谈时的赠礼
 * @param reissue        神庙全部丢失时的补领；为空表示该文明没有补领
 */
public record FaithProfile(Civilization civ, String guardian, String shrine, String favor, String hint,
                           List<Gift> firstMeetGifts, Optional<Reissue> reissue) {
    private static final Pattern ITEM = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Pattern KIND = Pattern.compile("[a-z]+:[a-z0-9_]+");
    private static final Set<String> KNOWN = Set.of("schemaVersion", "civilization", "guardian", "shrine", "favor",
            "hint", "firstMeetGifts", "reissue");

    public FaithProfile {
        firstMeetGifts = List.copyOf(firstMeetGifts);
    }

    public record Gift(String item, int count) {
    }

    /** @param shrineKind 判断「是否还有神庙」用的神庙种类 id（如 {@code greek:altar}） */
    public record Reissue(String item, String shrineKind) {
    }

    /** 只写了 civilization 时的档案：文本 key 按文明 id 生成，没有赠礼与补领。 */
    public static FaithProfile defaults(Civilization civ) {
        String c = civ.id();
        return new FaithProfile(civ, "mythic_age.faith." + c + ".guardian", "mythic_age.faith." + c + ".shrine",
                "mythic_age.favor." + c, "", List.of(), Optional.empty());
    }

    public static final class FaithProfileException extends RuntimeException {
        public FaithProfileException(String message) {
            super(message);
        }
    }

    /**
     * 解析一份档案。整份不可用（根不是对象、版本不对、文明未知）时抛出；
     * 单项不可用（某个赠礼的物品不存在、数量越界、未知字段）时跳过该项并记入 warnings。
     *
     * @param maxStack 物品 id → 最大堆叠；物品不存在时为空
     */
    public static FaithProfile parse(JsonElement rootEl, Function<String, OptionalInt> maxStack, List<String> warnings) {
        if (rootEl == null || !rootEl.isJsonObject()) {
            throw new FaithProfileException("(根): 缺失或不是对象");
        }
        JsonObject root = rootEl.getAsJsonObject();
        JsonElement v = root.get("schemaVersion");
        if (v == null || !v.isJsonPrimitive() || !v.getAsJsonPrimitive().isNumber() || v.getAsInt() != 1) {
            throw new FaithProfileException("schemaVersion: 只支持 1");
        }
        JsonElement civEl = root.get("civilization");
        String civId = civEl != null && civEl.isJsonPrimitive() ? civEl.getAsString() : "";
        Civilization civ = Civilization.byId(civId)
                .orElseThrow(() -> new FaithProfileException("civilization: 未知文明 " + civId));
        FaithProfile d = defaults(civ);
        for (String k : root.keySet()) {
            if (!KNOWN.contains(k)) {
                warnings.add(k + ": 未知字段，已忽略");
            }
        }
        String guardian = text(root, "guardian", d.guardian(), warnings);
        String shrine = text(root, "shrine", d.shrine(), warnings);
        String favor = text(root, "favor", d.favor(), warnings);
        String hint = text(root, "hint", d.hint(), warnings);
        List<Gift> gifts = new ArrayList<>();
        JsonElement g = root.get("firstMeetGifts");
        if (g != null) {
            if (!g.isJsonArray()) {
                warnings.add("firstMeetGifts: 应为数组，已视为没有赠礼");
            } else {
                JsonArray arr = g.getAsJsonArray();
                for (int i = 0; i < arr.size(); i++) {
                    gift(arr.get(i), "firstMeetGifts[" + i + "]", maxStack, warnings).ifPresent(gifts::add);
                }
            }
        }
        Optional<Reissue> reissue = Optional.empty();
        JsonElement re = root.get("reissue");
        if (re != null) {
            if (!re.isJsonObject()) {
                warnings.add("reissue: 应为对象，已视为没有补领");
            } else {
                JsonObject o = re.getAsJsonObject();
                String item = o.has("item") && o.get("item").isJsonPrimitive() ? o.get("item").getAsString() : "";
                String kind = o.has("shrineKind") && o.get("shrineKind").isJsonPrimitive()
                        ? o.get("shrineKind").getAsString() : "";
                if (!ITEM.matcher(item).matches() || maxStack.apply(item).isEmpty()) {
                    warnings.add("reissue.item: 物品不存在 " + item + "，已视为没有补领");
                } else if (!KIND.matcher(kind).matches() || !kind.startsWith(civ.id() + ":")) {
                    warnings.add("reissue.shrineKind: 应为本文明的神庙种类（" + civ.id() + ":<种类>），已视为没有补领");
                } else {
                    reissue = Optional.of(new Reissue(item, kind));
                }
            }
        }
        return new FaithProfile(civ, guardian, shrine, favor, hint, gifts, reissue);
    }

    public static FaithProfile parse(String json, Function<String, OptionalInt> maxStack, List<String> warnings) {
        try {
            return parse(JsonParser.parseString(json), maxStack, warnings);
        } catch (JsonSyntaxException e) {
            throw new FaithProfileException("(根): JSON 语法错误：" + e.getMessage());
        }
    }

    private static Optional<Gift> gift(JsonElement e, String path, Function<String, OptionalInt> maxStack,
                                       List<String> warnings) {
        if (!e.isJsonObject()) {
            warnings.add(path + ": 不是对象，已跳过");
            return Optional.empty();
        }
        JsonObject o = e.getAsJsonObject();
        String item = o.has("item") && o.get("item").isJsonPrimitive() ? o.get("item").getAsString() : "";
        OptionalInt max = ITEM.matcher(item).matches() ? maxStack.apply(item) : OptionalInt.empty();
        if (max.isEmpty()) {
            warnings.add(path + ".item: 物品不存在 " + item + "，已跳过");
            return Optional.empty();
        }
        JsonElement c = o.get("count");
        int count = c == null ? 1 : (c.isJsonPrimitive() && c.getAsJsonPrimitive().isNumber() ? c.getAsInt() : -1);
        if (count < 1 || count > max.getAsInt()) {
            warnings.add(path + ".count: 应在 1–" + max.getAsInt() + " 之间，已跳过");
            return Optional.empty();
        }
        return Optional.of(new Gift(item, count));
    }

    private static String text(JsonObject o, String key, String fallback, List<String> warnings) {
        return text(o, key, fallback, warnings, "");
    }

    private static String text(JsonObject o, String key, String fallback, List<String> warnings, String prefix) {
        JsonElement e = o.get(key);
        if (e == null) {
            return fallback;
        }
        if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString() && !e.getAsString().isEmpty()) {
            return e.getAsString();
        }
        warnings.add(prefix + key + ": 应为非空字符串，已使用默认值");
        return fallback;
    }
}
