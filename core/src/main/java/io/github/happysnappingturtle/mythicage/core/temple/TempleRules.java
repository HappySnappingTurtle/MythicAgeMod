package io.github.happysnappingturtle.mythicage.core.temple;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import io.github.happysnappingturtle.mythicage.core.player.Age;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 自由神庙的等级规则（T8a 设计文档 §3.3），来自数据包 {@code data/<ns>/mythic_age/temple/<文明>.json}。
 * 要认定 k 级，必须满足第 1…k 级的全部要求（逐级累加）；认定等级再受主人时代封顶。
 */
public record TempleRules(Scan scan, Roof roof, int columnMinHeight, List<Tier> tiers) {
    /** 单次扫描红线（修订 02 §5）：扫描分刻进行，8192 格在默认每刻 2048 格下 4 刻完成。 */
    public static final int MAX_SCAN_VOLUME = 8192;

    public TempleRules {
        tiers = List.copyOf(tiers);
    }

    /**
     * 扫描体（修订 01 §2.1，修订 02 §5 加长到前方 32）：以神坛为参照，局部 +z 为神坛朝向（指向神庙）。
     * x ∈ [−halfWidth, halfWidth]，y ∈ [−below, above]，z ∈ [−back, front]。
     */
    public record Scan(int halfWidth, int back, int front, int below, int above) {
        public int sizeX() {
            return halfWidth * 2 + 1;
        }

        public int sizeY() {
            return below + above + 1;
        }

        public int sizeZ() {
            return back + front + 1;
        }

        public int volume() {
            return sizeX() * sizeY() * sizeZ();
        }
    }

    /** 屋顶：柱子围出的矩形两边都 ≥ minSide，其中有顶的列 ≥ minPercent%（向上取整）。 */
    public record Roof(int minPercent, int minSide) {
        public int need(int area) {
            int a = Math.max(area, minSide * minSide);
            return (a * minPercent + 99) / 100;
        }

        public boolean satisfied(int covered, int area) {
            return area >= minSide * minSide && covered >= need(area);
        }
    }

    /** 一级神庙的要求；0 / false 表示该项不要求。 */
    public record Requirement(int columns, boolean roof, boolean openAltar, boolean crepidoma, int litBraziers,
                              int statues, int pediments, int marbleFloor) {
        public static final Requirement NONE = new Requirement(0, false, false, false, 0, 0, 0, 0);
    }

    public record Tier(int level, String name, Age age, int prayerSlots, double coefficient, Requirement require) {
    }

    public Tier tier(int level) {
        return tiers.get(level);
    }

    public int maxLevel() {
        return tiers.size() - 1;
    }

    // ───────────── 判定 ─────────────

    public TempleVerdict evaluate(TempleSurvey s, Age ownerAge) {
        int structural = 0;
        for (int i = 1; i < tiers.size(); i++) {
            if (missing(s, tiers.get(i).require()).isEmpty()) {
                structural = i;
            } else {
                break;
            }
        }
        int ageCap = 0;
        for (Tier t : tiers) {
            if (ownerAge.atLeast(t.age())) {
                ageCap = Math.max(ageCap, t.level());
            }
        }
        int granted = Math.min(structural, ageCap);
        List<TempleVerdict.Missing> missing = structural < maxLevel()
                ? missing(s, tiers.get(structural + 1).require()) : List.of();
        Optional<Age> ageRequired = structural > granted ? Optional.of(tiers.get(granted + 1).age()) : Optional.empty();
        return new TempleVerdict(structural, granted, missing, ageRequired);
    }

    private List<TempleVerdict.Missing> missing(TempleSurvey s, Requirement r) {
        List<TempleVerdict.Missing> out = new ArrayList<>();
        check(out, "columns", s.columns(), r.columns());
        if (r.openAltar() && !s.openAltar()) {
            out.add(new TempleVerdict.Missing("openAltar", 0, 1));
        }
        if (r.roof() && !roof.satisfied(s.roofCovered(), s.roofArea())) {
            out.add(new TempleVerdict.Missing("roof", s.roofCovered(), roof.need(s.roofArea())));
        }
        if (r.crepidoma()) {
            check(out, "raisedColumns", s.raisedColumns(), r.columns());
            if (!s.platformReachable()) {
                out.add(new TempleVerdict.Missing("platformAccess", 0, 1));
            }
        }
        check(out, "litBraziers", s.litBraziers(), r.litBraziers());
        check(out, "statues", s.statues(), r.statues());
        check(out, "pediments", s.pediments(), r.pediments());
        check(out, "marbleFloor", s.marbleFloor(), r.marbleFloor());
        return out;
    }

    private static void check(List<TempleVerdict.Missing> out, String key, int have, int need) {
        if (have < need) {
            out.add(new TempleVerdict.Missing(key, have, need));
        }
    }

    // ───────────── 解析 ─────────────

    /** 解析错误，消息以字段路径开头（如 {@code tiers[2].prayerSlots: 必须为正整数}）。 */
    public static final class TempleRulesException extends RuntimeException {
        public TempleRulesException(String message) {
            super(message);
        }
    }

    public static TempleRules parse(String json) {
        try {
            return parse(JsonParser.parseString(json));
        } catch (JsonSyntaxException e) {
            throw new TempleRulesException("(根): JSON 语法错误：" + e.getMessage());
        }
    }

    public static TempleRules parse(JsonElement rootEl) {
        JsonObject root = object(rootEl, "(根)");
        JsonObject scanO = object(root.get("scan"), "scan");
        Scan scan = new Scan(intIn(scanO, "halfWidth", "scan.halfWidth", 1, 16), intIn(scanO, "back", "scan.back", 0, 16),
                intIn(scanO, "front", "scan.front", 1, 32), intIn(scanO, "below", "scan.below", 0, 16),
                intIn(scanO, "above", "scan.above", 1, 24));
        if (scan.volume() > MAX_SCAN_VOLUME) {
            throw new TempleRulesException("scan: 扫描体 " + scan.volume() + " 格超过单次扫描红线 " + MAX_SCAN_VOLUME);
        }
        JsonObject roofO = object(root.get("roof"), "roof");
        Roof roof = new Roof(intIn(roofO, "minPercent", "roof.minPercent", 1, 100),
                intIn(roofO, "minSide", "roof.minSide", 1, scan.sizeX()));
        int columnMinHeight = intIn(root, "columnMinHeight", "columnMinHeight", 1, scan.sizeY());

        JsonElement tiersEl = root.get("tiers");
        if (tiersEl == null || !tiersEl.isJsonArray() || tiersEl.getAsJsonArray().isEmpty()) {
            throw new TempleRulesException("tiers: 缺失或不是非空数组");
        }
        JsonArray arr = tiersEl.getAsJsonArray();
        List<Tier> tiers = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            String path = "tiers[" + i + "]";
            JsonObject t = object(arr.get(i), path);
            int level = intIn(t, "level", path + ".level", 0, 64);
            if (level != i) {
                throw new TempleRulesException(path + ".level: 等级必须从 0 起连续，期望 " + i + "，实际 " + level);
            }
            String name = string(t, "name", path + ".name");
            String ageId = string(t, "age", path + ".age");
            Age age = Age.byId(ageId).filter(a -> a != Age.NONE)
                    .orElseThrow(() -> new TempleRulesException(path + ".age: 未知时代 " + ageId));
            int slots = intIn(t, "prayerSlots", path + ".prayerSlots", 1, 64);
            double coefficient = number(t.get("coefficient"), path + ".coefficient");
            if (coefficient <= 0) {
                throw new TempleRulesException(path + ".coefficient: 必须为正数，实际 " + coefficient);
            }
            Requirement req = Requirement.NONE;
            if (t.has("require")) {
                JsonObject r = object(t.get("require"), path + ".require");
                String rp = path + ".require.";
                req = new Requirement(optInt(r, "columns", rp + "columns"), optBool(r, "roof", rp + "roof"),
                        optBool(r, "openAltar", rp + "openAltar"), optBool(r, "crepidoma", rp + "crepidoma"),
                        optInt(r, "litBraziers", rp + "litBraziers"), optInt(r, "statues", rp + "statues"),
                        optInt(r, "pediments", rp + "pediments"), optInt(r, "marbleFloor", rp + "marbleFloor"));
            } else if (i > 0) {
                throw new TempleRulesException(path + ".require: 0 级以上必须声明要求");
            }
            if (!tiers.isEmpty() && age.ordinal() < tiers.getLast().age().ordinal()) {
                throw new TempleRulesException(path + ".age: 不能早于上一级的时代");
            }
            tiers.add(new Tier(level, name, age, slots, coefficient, req));
        }
        return new TempleRules(scan, roof, columnMinHeight, tiers);
    }

    private static JsonObject object(JsonElement e, String path) {
        if (e == null || !e.isJsonObject()) {
            throw new TempleRulesException(path + ": 缺失或不是对象");
        }
        return e.getAsJsonObject();
    }

    private static String string(JsonObject o, String key, String path) {
        JsonElement e = o.get(key);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString() || e.getAsString().isEmpty()) {
            throw new TempleRulesException(path + ": 缺失或不是非空字符串");
        }
        return e.getAsString();
    }

    private static double number(JsonElement e, String path) {
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) {
            throw new TempleRulesException(path + ": 缺失或不是数字");
        }
        double d = e.getAsDouble();
        if (!Double.isFinite(d)) {
            throw new TempleRulesException(path + ": 不是有限数");
        }
        return d;
    }

    private static int intIn(JsonObject o, String key, String path, int min, int max) {
        double d = number(o.get(key), path);
        if (d != Math.rint(d) || d < min || d > max) {
            throw new TempleRulesException(path + ": 必须是 " + min + "–" + max + " 的整数，实际 " + d);
        }
        return (int) d;
    }

    private static int optInt(JsonObject o, String key, String path) {
        return o.has(key) ? intIn(o, key, path, 0, 4096) : 0;
    }

    private static boolean optBool(JsonObject o, String key, String path) {
        if (!o.has(key)) {
            return false;
        }
        JsonElement e = o.get(key);
        if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isBoolean()) {
            throw new TempleRulesException(path + ": 不是布尔值");
        }
        return e.getAsBoolean();
    }
}
