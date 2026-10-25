package io.github.happysnappingturtle.mythicage.core.village.life;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 村庄生活数据 {@code village_civ/<civ>.json} 的 {@code life} 段（T19 设计文档 §1.10 J1）：三餐、装饰性活动、傍晚聚会、
 * 祭司差事、节庆、打鱼、神谕。全部是通用框架的数据，各文明只写数据（多文明优先）。
 */
public record LifeData(Meals meals, List<Activity> activities, Meet meet, List<PriestDuty> priest,
                       Map<String, Festival> festivals, Fishing fishing, Oracle oracle) {
    public static final Set<String> PHASE_KINDS = Set.of("relay", "procession", "games", "theater", "gather");

    public LifeData {
        activities = List.copyOf(activities);
        priest = List.copyOf(priest);
        festivals = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(festivals));
    }

    /** 一段动作：动画名与时长。 */
    public record Act(String anim, int ticks) {
        static Act parse(String s) {
            int i = s.indexOf(':');
            return i < 0 ? new Act(s, 40) : new Act(s.substring(0, i), Integer.parseInt(s.substring(i + 1)));
        }
    }

    /** 黎明、晚饭（在家）与午饭（原地）的动作序列。 */
    public record Meals(List<Act> dawn, List<Act> supper, List<Act> lunch) {
        public static final Meals NONE = new Meals(List.of(), List.of(), List.of());

        public Meals {
            dawn = List.copyOf(dawn);
            supper = List.copyOf(supper);
            lunch = List.copyOf(lunch);
        }
    }

    /** 一天里的时段 [from, to)（刻，0 = 日出）。 */
    public record Window(int from, int to) {
        public boolean contains(int t) {
            return from <= to ? t >= from && t < to : t >= from || t < to;
        }
    }

    /**
     * 装饰性活动（打水、闲逛、扫墓……）。
     *
     * @param min / max     每段派几人（按村庄、日子、时段派生）
     * @param chance        这一段有没有活动的概率（1 = 每段都有）
     * @param carry         去的路上手里的东西；{@code carryHome}：做完带回家的东西（空 = 不回家）
     * @param queueAct      排队等候的动作（第 k 个人先等 40k 刻）
     * @param prefer        {@code female}：优先派女性居民
     * @param special       只在带这个标签的日子（空 = 每天；{@code !tag} = 不在这天）
     * @param specialMax    特殊日派全村空闲居民时的上限（0 = 按 min/max）
     */
    public record Activity(String id, String anchor, List<Window> windows, int min, int max, double chance, Act act,
                           String carry, String carryHome, String queueAct, String prefer, String patron, String special,
                           int specialMax, String minAge, Set<String> professions) {
        public Activity {
            windows = List.copyOf(windows);
            professions = Set.copyOf(professions);
        }

        public java.util.Optional<Window> windowAt(int tod) {
            return windows.stream().filter(w -> w.contains(tod)).findFirst();
        }
    }

    /** 傍晚聚会：地点锚点（空 = 钟）、下雨时的避雨锚点（空 = 下雨不聚）。 */
    public record Meet(String at, String shelter) {
        public static final Meet NONE = new Meet("", "");
    }

    /**
     * 祭司差事：{@code at} 刻起去 {@code anchor}（{@code all}：同名锚点按远近依次都去），播 {@code gesture} {@code ticks} 刻。
     * {@code until} > 0 时一直坐到这一刻（神谕坐诊）。
     */
    public record PriestDuty(String id, int at, int until, String anchor, String gesture, int ticks, boolean all,
                             boolean smoke, String patron, String special, String minAge) {
    }

    /**
     * 节庆的一段。
     *
     * @param kind      {@link #PHASE_KINDS}
     * @param eve       在节日前一天的这一时段（火炬接力）
     * @param at        集合 / 举行的锚点（{@code center} 等；{@code altar} = 游行终点祭坛）
     * @param track     接力、赛跑、游行的起点与终点锚点；{@code fallback}：没有这两个锚点时用的
     * @param count     接力棒数、每轮赛跑人数、演员人数
     * @param route     游行路线：{@code altar}（走到祭坛）或 {@code loop}（绕村回到起点）
     * @param leader    领头者手里的东西（空 = 没有领头者）
     * @param carries   游行队伍前几人手里的东西（按顺序）
     * @param offering  到达祭坛时的祭品（依次找村库里有的第一种，消耗 1）
     * @param acts      聚集时的动作（内圈、外圈……）
     * @param consume   宴饮消耗的物品（空 = 不消耗）；{@code per}：每几人 1 份
     * @param otherwise 没有场地（剧场）时改为哪种段
     */
    public record Phase(String kind, int from, int to, boolean eve, String at, List<String> track, List<String> fallback,
                        int count, String route, String leader, List<String> carries, List<String> offering,
                        List<String> acts, String consume, int per, String otherwise) {
        public Phase {
            track = List.copyOf(track);
            fallback = List.copyOf(fallback);
            carries = List.copyOf(carries);
            offering = List.copyOf(offering);
            acts = List.copyOf(acts);
        }

        public boolean contains(int tod) {
            return tod >= from && tod < to;
        }
    }

    /** 一个节日：日历上的标签就是 id。 */
    public record Festival(String id, String minAge, double rations, List<Phase> phases) {
        public Festival {
            phases = List.copyOf(phases);
        }
    }

    /** 打鱼：每村每日最多结算 {@code daily} 次；{@code alt} 以 {@code altChance} 代替主产物。 */
    public record Fishing(String anchor, Set<String> professions, String patron, int daily, String item, String alt,
                          double altChance, int castTicks, int reelTicks) {
        public static final Fishing NONE = new Fishing("", Set.of(), "", 0, "", "", 0, 40, 60);

        public boolean enabled() {
            return daily > 0 && !anchor.isEmpty() && !item.isEmpty();
        }
    }

    /** 神谕：锚点、日历标签、闭门的期、开放阶段、坐诊时段。 */
    public record Oracle(String anchor, String special, String closedPeriod, String minAge, int from, int until) {
        public static final Oracle NONE = new Oracle("", "", "", "", 0, 0);

        public boolean enabled() {
            return !anchor.isEmpty() && !special.isEmpty();
        }
    }

    public static LifeData none() {
        return new LifeData(Meals.NONE, List.of(), Meet.NONE, List.of(), Map.of(), Fishing.NONE, Oracle.NONE);
    }

    public static LifeData parse(JsonObject o) {
        Meals meals = Meals.NONE;
        if (o.has("meals")) {
            JsonObject m = o.getAsJsonObject("meals");
            meals = new Meals(acts(m, "dawn"), acts(m, "supper"), acts(m, "lunch"));
        }
        List<Activity> activities = new ArrayList<>();
        if (o.has("activities")) {
            for (JsonElement e : o.getAsJsonArray("activities")) {
                JsonObject a = e.getAsJsonObject();
                int[] people = pair(a, "people", 1, 1);
                activities.add(new Activity(str(a, "id", ""), str(a, "anchor", ""), windows(a), people[0], people[1],
                        dbl(a, "chance", 1), Act.parse(str(a, "act", "idle:40")), str(a, "carry", ""), str(a, "carryHome", ""),
                        str(a, "queueAct", ""), str(a, "prefer", ""), str(a, "patron", ""), str(a, "special", ""),
                        integer(a, "specialMax", 0), str(a, "minAge", ""), set(a, "professions")));
            }
        }
        Meet meet = Meet.NONE;
        if (o.has("meet")) {
            JsonObject m = o.getAsJsonObject("meet");
            meet = new Meet(str(m, "at", ""), str(m, "shelter", ""));
        }
        List<PriestDuty> priest = new ArrayList<>();
        if (o.has("priest")) {
            for (JsonElement e : o.getAsJsonArray("priest")) {
                JsonObject p = e.getAsJsonObject();
                priest.add(new PriestDuty(str(p, "id", ""), integer(p, "at", 0), integer(p, "until", 0), str(p, "anchor", ""),
                        str(p, "gesture", ""), integer(p, "ticks", 60), bool(p, "all"), bool(p, "smoke"), str(p, "patron", ""),
                        str(p, "special", ""), str(p, "minAge", "")));
            }
        }
        Map<String, Festival> festivals = new LinkedHashMap<>();
        if (o.has("festivals")) {
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("festivals").entrySet()) {
                JsonObject f = e.getValue().getAsJsonObject();
                List<Phase> phases = new ArrayList<>();
                for (JsonElement pe : f.getAsJsonArray("phases")) {
                    phases.add(phase(pe.getAsJsonObject()));
                }
                festivals.put(e.getKey(), new Festival(e.getKey(), str(f, "minAge", ""), dbl(f, "rations", 1), phases));
            }
        }
        Fishing fishing = Fishing.NONE;
        if (o.has("fishing")) {
            JsonObject f = o.getAsJsonObject("fishing");
            fishing = new Fishing(str(f, "anchor", ""), set(f, "professions"), str(f, "patron", ""), integer(f, "daily", 8),
                    str(f, "item", ""), str(f, "alt", ""), dbl(f, "altChance", 0), integer(f, "castTicks", 40),
                    integer(f, "reelTicks", 60));
        }
        Oracle oracle = Oracle.NONE;
        if (o.has("oracle")) {
            JsonObject r = o.getAsJsonObject("oracle");
            oracle = new Oracle(str(r, "anchor", ""), str(r, "special", ""), str(r, "closedPeriod", ""), str(r, "minAge", ""),
                    integer(r, "from", 1500), integer(r, "until", 11000));
        }
        return new LifeData(meals, activities, meet, priest, festivals, fishing, oracle);
    }

    static Phase phase(JsonObject p) {
        String kind = str(p, "kind", "");
        if (!PHASE_KINDS.contains(kind)) {
            throw new IllegalArgumentException("未知的节庆段种类：" + kind);
        }
        return new Phase(kind, integer(p, "from", 0), integer(p, "to", 0), bool(p, "eve"), str(p, "at", ""),
                list(p, "track"), list(p, "fallback"), integer(p, "count", 0), str(p, "route", "altar"), str(p, "leader", ""),
                list(p, "carries"), list(p, "offering"), list(p, "acts"), str(p, "consume", ""), integer(p, "per", 4),
                str(p, "otherwise", ""));
    }

    private static List<Act> acts(JsonObject o, String k) {
        List<Act> r = new ArrayList<>();
        if (o.has(k)) {
            o.getAsJsonArray(k).forEach(e -> r.add(Act.parse(e.getAsString())));
        }
        return r;
    }

    private static List<Window> windows(JsonObject o) {
        List<Window> r = new ArrayList<>();
        if (o.has("windows")) {
            for (JsonElement e : o.getAsJsonArray("windows")) {
                JsonArray w = e.getAsJsonArray();
                r.add(new Window(w.get(0).getAsInt(), w.get(1).getAsInt()));
            }
        }
        return r;
    }

    private static int[] pair(JsonObject o, String k, int a, int b) {
        if (!o.has(k)) {
            return new int[]{a, b};
        }
        JsonArray arr = o.getAsJsonArray(k);
        return new int[]{arr.get(0).getAsInt(), arr.get(1).getAsInt()};
    }

    private static int integer(JsonObject o, String k, int d) {
        return o.has(k) ? o.get(k).getAsInt() : d;
    }

    private static double dbl(JsonObject o, String k, double d) {
        return o.has(k) ? o.get(k).getAsDouble() : d;
    }

    private static boolean bool(JsonObject o, String k) {
        return o.has(k) && o.get(k).getAsBoolean();
    }

    private static String str(JsonObject o, String k, String d) {
        return o.has(k) ? o.get(k).getAsString() : d;
    }

    private static Set<String> set(JsonObject o, String k) {
        Set<String> s = new TreeSet<>();
        if (o.has(k)) {
            o.getAsJsonArray(k).forEach(e -> s.add(e.getAsString()));
        }
        return s;
    }

    private static List<String> list(JsonObject o, String k) {
        List<String> s = new ArrayList<>();
        if (o.has(k)) {
            o.getAsJsonArray(k).forEach(e -> s.add(e.getAsString()));
        }
        return s;
    }
}
