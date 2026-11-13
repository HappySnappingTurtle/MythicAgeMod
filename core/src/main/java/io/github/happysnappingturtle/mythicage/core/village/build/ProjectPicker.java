package io.github.happysnappingturtle.mythicage.core.village.build;

import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.village.gen.BuildingSpec;
import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 选施工项目（T19 设计文档 §8.2、§1.4 E3）：纯函数，每 MC 日调用一次。
 * <ol>
 *   <li>已有进行中的项目、口粮低于底线、远古阶段（D7）：都不选；</li>
 *   <li>次神祠插队：本村登记过、还没有祠的次神，按登记时代先后；建在该时代的空 {@code shrine} 地块，没有就用更早开放的；
 *       有这位神专门的祠就用它，否则用通用祠（{@code god = "*"}）；</li>
 *   <li>其余按 {@code priority}（小的先）、id：时代已开放、唯一建筑没建过、前置条件满足；
 *       原地替换的建筑要本村有可替换的旧建筑、且新模板在旧建筑原位放得下（不占地块）；其余要有能放下它的空地块（地块的时代已开放，先用早开放的地块）。</li>
 * </ol>
 * GA-11：{@link #candidates} 列出全部候选（含不能开工的与原因），玩家从中点名「下一个」；{@link #pick} 取其中第一个能开工的。
 */
public final class ProjectPicker {

    /** 空地块（未建、未被占、没有进行中的项目）。w、d 是 x、z 方向的尺寸。 */
    public record Plot(String id, String kind, Age stage, int w, int d, String front) {
    }

    /** 一次立祀登记：哪个时代、哪位次神。 */
    public record ShrineRequest(Age age, String god) {
    }

    /** 本村一座能被升级的旧建筑（施工建成的直接取；生成的按锚点反推）。{@code at} 为 null 表示位置不明，不做原位判断。 */
    public record Existing(String building, BuildPlacement.Placed at) {
    }

    /** 升级候选在旧建筑原位放不放得下（GA-11 §2.4）：返回空串表示放得下，否则是原因（见 {@link UpgradeFit}）。 */
    @FunctionalInterface
    public interface Fit {
        Fit ALWAYS = (next, old) -> "";

        String check(BuildingSpec next, Existing old);
    }

    /**
     * @param buildings 候选建筑（施工建筑，已去掉主神变体；变体由调用方在选中后按村庄主神挑）
     * @param built     本村已有的建筑 id（施工建成的 + 能认出来的生成建筑）
     * @param builtGods 已经有祠的次神
     * @param satisfied 满足的前置条件（{@code anchor:x}、{@code building:y}）
     * @param existing  本村能被升级的旧建筑（每座一行；正在被替换的那座由调用方去掉）
     * @param inProgress 进行中的项目的候选键（{@link #key}），唯一建筑与次神祠不再列出
     * @param fit       升级的原位判断
     */
    public record Input(Age stage, boolean busy, boolean foodShort, List<BuildingSpec> buildings, List<Plot> plots,
                        Set<String> built, Set<String> builtGods, List<ShrineRequest> shrines, Set<String> satisfied,
                        List<Existing> existing, Set<String> inProgress, Fit fit) {
        public Input {
            buildings = List.copyOf(buildings);
            plots = List.copyOf(plots);
            built = Set.copyOf(built);
            builtGods = Set.copyOf(builtGods);
            shrines = List.copyOf(shrines);
            satisfied = Set.copyOf(satisfied);
            existing = List.copyOf(existing);
            inProgress = Set.copyOf(inProgress);
        }

        /** T19 的写法：可替换的旧建筑只给 id（位置不明，不做原位判断）。 */
        public Input(Age stage, boolean busy, boolean foodShort, List<BuildingSpec> buildings, List<Plot> plots,
                     Set<String> built, Set<String> builtGods, List<ShrineRequest> shrines, Set<String> satisfied,
                     Set<String> replaceable) {
            this(stage, busy, foodShort, buildings, plots, built, builtGods, shrines, satisfied,
                    replaceable.stream().sorted().map(id -> new Existing(id, null)).toList(), Set.of(), Fit.ALWAYS);
        }

        public Input withBusy(boolean b) {
            return new Input(stage, b, foodShort, buildings, plots, built, builtGods, shrines, satisfied, existing, inProgress, fit);
        }
    }

    /**
     * @param plot      地块 id；原地替换为空串
     * @param god       次神祠的神；其他为空串
     * @param replaces  被替换的旧建筑 id；其他为空串
     * @param replaceAt 被替换的那一座的原点；位置不明（T19 的写法）为 null，开工时取第一座
     */
    public record Pick(String building, String plot, String god, String replaces, Pos replaceAt) {
        public Pick(String building, String plot, String god, String replaces) {
            this(building, plot, god, replaces, null);
        }
    }

    /** 候选的种类：新建、原地升级、次神祠。 */
    public enum Kind { NEW, UPGRADE, SHRINE }

    /** 不能开工的原因（语言键后缀）。 */
    public static final String FOOD_SHORT = "food_short";
    public static final String STAGE = "stage";
    public static final String NO_PLOT = "no_plot";
    public static final String REQUIRES = "requires";
    public static final String NO_FIT = "no_fit";

    /**
     * 一项候选（GA-11 §2.1）。
     *
     * @param key       候选键：建筑 id，次神祠为 {@code shrine:<神>}；「下一个」位存它，开工时按它重新找
     * @param pick      能开工时的具体选择（地块、被替换的那一座）；不能开工时只有建筑 id
     * @param reason    不能开工的原因；能开工为空串
     * @param rank      村子自己选时排第几（0 = 下一个就选它）；不能开工为 −1
     * @param targets   升级：本村有几座能升级的旧建筑；其他为 0
     */
    public record Candidate(String key, Kind kind, Pick pick, String reason, int rank, int targets) {
        public boolean available() {
            return reason.isEmpty();
        }
    }

    private ProjectPicker() {
    }

    public static String key(String building, String god) {
        return god.isEmpty() ? building : "shrine:" + god;
    }

    /**
     * 本村全部候选，按村子自己选的顺序：先次神祠（登记先后），再按 {@code priority}、id。
     * 唯一建筑建过了、进行中的项目、没有旧建筑可升级的升级都不列出；进行中的项目不影响其余候选（玩家可以点名下一个）。
     */
    public static List<Candidate> candidates(Input in) {
        // 原因的先后：村子还在远古 → 这座建筑的时代、前置不够 → 缺粮（缺粮是暂时的，放最后，免得英雄建筑也写「缺粮」）
        String archaic = !in.stage().atLeast(Age.CLASSICAL) ? STAGE : "";
        String food = in.foodShort() ? FOOD_SHORT : "";
        List<Candidate> out = new ArrayList<>();
        Set<String> seenGods = new HashSet<>();
        for (ShrineRequest r : in.shrines()) {
            if (in.builtGods().contains(r.god()) || !seenGods.add(r.god()) || in.inProgress().contains(key("", r.god()))) {
                continue;
            }
            BuildingSpec s = in.buildings().stream().filter(b -> b.god().equals(r.god())).findFirst()
                    .or(() -> in.buildings().stream().filter(b -> b.god().equals("*")).findFirst()).orElse(null);
            if (s == null) {
                continue;
            }
            Optional<Plot> plot = in.plots().stream()
                    .filter(p -> s.plotKinds().contains(p.kind()) && in.stage().atLeast(p.stage()) && fits(s, p))
                    .min(Comparator.comparingInt((Plot p) -> p.stage() == r.age() ? 0 : 1)
                            .thenComparingInt(p -> p.stage().ordinal()).thenComparing(Plot::id));
            String reason = !archaic.isEmpty() ? archaic : !in.stage().atLeast(r.age()) ? STAGE : !food.isEmpty() ? food
                    : plot.isEmpty() ? NO_PLOT : "";
            out.add(new Candidate(key("", r.god()), Kind.SHRINE,
                    new Pick(s.id(), plot.map(Plot::id).orElse(""), r.god(), ""), reason, -1, 0));
        }
        List<BuildingSpec> sorted = in.buildings().stream()
                .filter(b -> b.god().isEmpty())
                .sorted(Comparator.comparingInt(BuildingSpec::priority).thenComparing(BuildingSpec::id)).toList();
        for (BuildingSpec s : sorted) {
            if ((s.unique() && in.built().contains(s.id())) || (s.unique() && in.inProgress().contains(s.id()))) {
                continue;
            }
            Age need = Age.byId(s.stage()).orElse(Age.ARCHAIC);
            String gate = !archaic.isEmpty() ? archaic : !in.stage().atLeast(need) ? STAGE
                    : !in.satisfied().containsAll(s.requires()) ? REQUIRES : food;
            if (!s.replace().isEmpty()) {
                List<Existing> targets = in.existing().stream()
                        .filter(e -> s.replace().stream().anyMatch(e.building()::startsWith) && !e.building().startsWith(s.id()))
                        .sorted(EXISTING_ORDER).toList();
                if (targets.isEmpty()) {
                    continue;
                }
                Optional<Existing> fit = targets.stream().filter(e -> e.at() == null || in.fit().check(s, e).isEmpty()).findFirst();
                String reason = !gate.isEmpty() ? gate : fit.isEmpty() ? NO_FIT : "";
                Existing t = fit.orElse(targets.get(0));
                out.add(new Candidate(s.id(), Kind.UPGRADE, new Pick(s.id(), "", "", t.building(),
                        t.at() == null ? null : t.at().origin()), reason, -1, targets.size()));
                continue;
            }
            Optional<Plot> plot = in.plots().stream()
                    .filter(p -> s.plotKinds().contains(p.kind()) && in.stage().atLeast(p.stage()) && fits(s, p))
                    .min(Comparator.comparingInt((Plot p) -> p.stage().ordinal()).thenComparing(Plot::id));
            String reason = !gate.isEmpty() ? gate : plot.isEmpty() ? NO_PLOT : "";
            out.add(new Candidate(s.id(), Kind.NEW, new Pick(s.id(), plot.map(Plot::id).orElse(""), "", ""), reason, -1, 0));
        }
        List<Candidate> ranked = new ArrayList<>();
        int rank = 0;
        for (Candidate c : out) {
            ranked.add(c.available() ? new Candidate(c.key(), c.kind(), c.pick(), c.reason(), rank++, c.targets()) : c);
        }
        return ranked;
    }

    private static final Comparator<Existing> EXISTING_ORDER = Comparator.comparing(Existing::building)
            .thenComparingInt(e -> e.at() == null ? Integer.MIN_VALUE : e.at().origin().x())
            .thenComparingInt(e -> e.at() == null ? Integer.MIN_VALUE : e.at().origin().z())
            .thenComparingInt(e -> e.at() == null ? Integer.MIN_VALUE : e.at().origin().y());

    /** 村子自己选：已有进行中的项目时不选，否则取 {@link #candidates} 里第一个能开工的。 */
    public static Optional<Pick> pick(Input in) {
        if (in.busy()) {
            return Optional.empty();
        }
        return candidates(in).stream().filter(Candidate::available).findFirst().map(Candidate::pick);
    }

    /**
     * 想建、但没有能放下它的预留地块的第一座（按 {@link #pick} 同样的顺序：先次神祠，再按优先级）——临时找地用（§1.13 K7）。
     * 原地替换的不算（它们不占地）。{@link #pick} 能选出项目时不要调用。
     */
    public static Optional<Pick> wanted(Input in) {
        if (in.busy() || in.foodShort() || !in.stage().atLeast(Age.CLASSICAL)) {
            return Optional.empty();
        }
        for (ShrineRequest r : in.shrines()) {
            if (in.builtGods().contains(r.god()) || !in.stage().atLeast(r.age())) {
                continue;
            }
            Optional<BuildingSpec> s = in.buildings().stream().filter(b -> b.god().equals(r.god())).findFirst()
                    .or(() -> in.buildings().stream().filter(b -> b.god().equals("*")).findFirst());
            if (s.isPresent()) {
                return Optional.of(new Pick(s.get().id(), "", r.god(), ""));
            }
        }
        return in.buildings().stream()
                .filter(b -> b.god().isEmpty() && b.replace().isEmpty())
                .sorted(Comparator.comparingInt(BuildingSpec::priority).thenComparing(BuildingSpec::id))
                .filter(s -> in.stage().atLeast(Age.byId(s.stage()).orElse(Age.ARCHAIC))
                        && !(s.unique() && in.built().contains(s.id())) && in.satisfied().containsAll(s.requires()))
                .findFirst().map(s -> new Pick(s.id(), "", "", ""));
    }

    static boolean fits(BuildingSpec s, Plot p) {
        return BuildPlacement.fits(s.sizeX(), s.sizeZ(), BuildPlacement.turnFor(s.doorFacing(), p.front()), p.w(), p.d());
    }
}
