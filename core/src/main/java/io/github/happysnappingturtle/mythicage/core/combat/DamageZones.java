package io.github.happysnappingturtle.mythicage.core.combat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 伤害乘区（T25.1，五文明通用）：
 * <pre>
 * 最终伤害（护甲前）= 基础 × 装备区 × 弹药区 × 祝福区 × 招式区 × 窗口区 × 抗性区
 * </pre>
 * 同一个区里同一效果组只取最大，不同效果组相加（招式区相乘、窗口区只取最大、抗性区 1 − r 连乘）；不同区之间相乘。
 * 装备 × 弹药 × 祝福合计最多 ×{@value #BOOST_CAP}（D3）；对 Boss 的窗口最多 ×{@value #BOSS_WINDOW_CAP}（D4）；
 * 抗性合计最多减到 ×{@value #RESIST_FLOOR}（D5）。谁吃哪些区见 {@link #applies}（D6）。
 * <p>
 * 各内容只登记「我属于哪个区、哪个效果组、多少」（{@link Mod}），不自己乘。
 */
public final class DamageZones {
    public static final double BOOST_CAP = 2.5;
    public static final double BOSS_WINDOW_CAP = 2.0;
    public static final double RESIST_FLOOR = 0.1;

    /** 七个区（D1）。 */
    public enum Zone {
        /** 固定加值，加在原版算好的数值上。 */
        BASE,
        /** 玩家身上装备的百分比增伤。 */
        GEAR,
        /** 箭矢、投掷物本身的增伤。 */
        AMMO,
        /** 圣物、祝福、供奉、时代加成。 */
        BLESSING,
        /** 招式或神力对某类目标的系数（相乘）。 */
        MOVE,
        /** 目标的弱点窗口（只取最大）。 */
        WINDOW,
        /** 目标自带的减伤（1 − r 连乘）。 */
        RESIST
    }

    /** 伤害从哪来（§1.2）。 */
    public enum Source {
        /** 玩家近战、弓弩、投掷。 */
        PLAYER,
        /** 召唤单位。 */
        SUMMON,
        /** 神力。 */
        POWER,
        /** 村庄士兵、哨兵石像。 */
        SOLDIER,
        /** 野怪、精英、Boss，以及没有攻击者的伤害（摔落、中毒等）。 */
        MOB
    }

    /** 目标是什么（窗口封顶只对 Boss）。 */
    public enum Target {
        NORMAL,
        ELITE,
        BOSS
    }

    /**
     * 一条修正。value 的含义随区：基础区是加值；装备、弹药、祝福区是增伤比例（0.25 = +25%）；招式、窗口区是倍率（1.5 = ×1.5）；
     * 抗性区是减伤比例（0.8 = −80%）。
     *
     * @param sources 祝福区才看：这条对哪些来源生效（D6：祝福区只认写明对它们生效的）；空集表示只对玩家
     */
    public record Mod(Zone zone, String group, double value, Set<Source> sources) {
        public Mod {
            sources = sources.isEmpty() ? Set.of() : Set.copyOf(sources);
        }

        public static Mod of(Zone zone, String group, double value) {
            return new Mod(zone, group, value, Set.of());
        }

        public static Mod blessing(String group, double value, Source first, Source... more) {
            return new Mod(Zone.BLESSING, group, value, EnumSet.of(first, more));
        }
    }

    /** 明细里的一行：某个区合起来的倍率（基础区是加值）。 */
    public record Line(Zone zone, double factor) {
    }

    /**
     * @param base       基础（原版数值 + 基础区加值）
     * @param multiplier 各区相乘的总倍率
     * @param total      base × multiplier（护甲前）
     * @param lines      逐区明细（空区不列；增伤封顶时三区各自照列，另有 capped 标记）
     */
    public record Result(double base, double multiplier, double total, List<Line> lines, boolean capped) {
    }

    private DamageZones() {
    }

    /** 这条修正对这个来源算不算（§1.2，D6）。 */
    public static boolean applies(Mod m, Source source) {
        return switch (m.zone()) {
            case BASE, WINDOW, RESIST -> true;
            case GEAR, AMMO -> source == Source.PLAYER;
            case BLESSING -> source != Source.MOB && (m.sources().isEmpty() ? source == Source.PLAYER : m.sources().contains(source));
            case MOVE -> source != Source.PLAYER;
        };
    }

    public static Result compute(double amount, Source source, Target target, Collection<Mod> mods) {
        Map<Zone, Map<String, Double>> byGroup = new EnumMap<>(Zone.class);
        Map<Zone, List<Double>> moves = new EnumMap<>(Zone.class);
        for (Mod m : mods) {
            if (!applies(m, source) || !Double.isFinite(m.value())) {
                continue;
            }
            if (m.zone() == Zone.MOVE) {
                moves.computeIfAbsent(Zone.MOVE, k -> new ArrayList<>()).add(m.value());
            } else if (m.zone() == Zone.BASE) {
                // 基础区是固定加值，各条都加
                byGroup.computeIfAbsent(Zone.BASE, k -> new LinkedHashMap<>()).merge(m.group(), m.value(), Double::sum);
            } else {
                byGroup.computeIfAbsent(m.zone(), k -> new LinkedHashMap<>()).merge(m.group(), m.value(), Math::max);
            }
        }
        List<Line> lines = new ArrayList<>();
        double base = amount;
        Map<String, Double> flat = byGroup.get(Zone.BASE);
        if (flat != null) {
            double add = flat.values().stream().mapToDouble(Double::doubleValue).sum();
            base += add;
            lines.add(new Line(Zone.BASE, add));
        }
        double boost = 1;
        for (Zone z : List.of(Zone.GEAR, Zone.AMMO, Zone.BLESSING)) {
            Map<String, Double> g = byGroup.get(z);
            if (g == null) {
                continue;
            }
            double f = 1 + g.values().stream().mapToDouble(Double::doubleValue).sum();
            boost *= f;
            lines.add(new Line(z, f));
        }
        boolean capped = boost > BOOST_CAP;
        boost = Math.min(boost, BOOST_CAP);
        double move = 1;
        List<Double> mv = moves.get(Zone.MOVE);
        if (mv != null) {
            for (double v : mv) {
                move *= v;
            }
            lines.add(new Line(Zone.MOVE, move));
        }
        double window = 1;
        Map<String, Double> w = byGroup.get(Zone.WINDOW);
        if (w != null) {
            window = w.values().stream().mapToDouble(Double::doubleValue).max().orElse(1);
            if (target == Target.BOSS) {
                window = Math.min(window, BOSS_WINDOW_CAP);
            }
            lines.add(new Line(Zone.WINDOW, window));
        }
        double resist = 1;
        Map<String, Double> r = byGroup.get(Zone.RESIST);
        if (r != null) {
            for (double v : r.values()) {
                resist *= 1 - Math.max(0, Math.min(1, v));
            }
            resist = Math.max(RESIST_FLOOR, resist);
            lines.add(new Line(Zone.RESIST, resist));
        }
        double mult = boost * move * window * resist;
        return new Result(base, mult, Math.max(0, base * mult), List.copyOf(lines), capped);
    }

    /**
     * 明细一行，例如「9.0 ×1.25 装备 ×1.5 弹药 ×0.5 抗性 = 8.4」；有基础区加值时写成「6.0+3.0」，增伤封顶时补「≤×2.5」。
     * 区名由 name 给（平台层翻译）。
     */
    public static String describe(double amount, Result r, java.util.function.Function<Zone, String> name) {
        StringBuilder sb = new StringBuilder(fmt(amount));
        for (Line l : r.lines()) {
            if (l.zone() == Zone.BASE) {
                sb.append(l.factor() >= 0 ? "+" : "").append(fmt(l.factor()));
            } else {
                sb.append(" ×").append(fmt(l.factor())).append(' ').append(name.apply(l.zone()));
            }
        }
        if (r.capped()) {
            sb.append(" ≤×").append(fmt(BOOST_CAP));
        }
        return sb.append(" = ").append(fmt(r.total())).toString();
    }

    /** 明细里的数：保留到两位小数，整数写成「9.0」。 */
    public static String fmt(double v) {
        double rounded = Math.round(v * 100) / 100.0;
        return rounded == Math.rint(rounded) ? String.format(java.util.Locale.ROOT, "%.1f", rounded)
                : String.format(java.util.Locale.ROOT, "%s", rounded);
    }
}
