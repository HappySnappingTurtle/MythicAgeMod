package io.github.happysnappingturtle.mythicage.core.village.build;

import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.village.gen.BuildingSpec;

import java.util.Comparator;
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
 *       原地替换的建筑要本村有可替换的旧建筑（不占地块）；其余要有能放下它的空地块（地块的时代已开放，先用早开放的地块）。</li>
 * </ol>
 */
public final class ProjectPicker {

    /** 空地块（未建、未被占、没有进行中的项目）。w、d 是 x、z 方向的尺寸。 */
    public record Plot(String id, String kind, Age stage, int w, int d, String front) {
    }

    /** 一次立祀登记：哪个时代、哪位次神。 */
    public record ShrineRequest(Age age, String god) {
    }

    /**
     * @param buildings 候选建筑（施工建筑，已去掉主神变体；变体由调用方在选中后按村庄主神挑）
     * @param built     本村已有的建筑 id（施工建成的 + 能认出来的生成建筑）
     * @param builtGods 已经有祠的次神
     * @param satisfied 满足的前置条件（{@code anchor:x}、{@code building:y}）
     * @param replaceable 本村可被替换的旧建筑 id
     */
    public record Input(Age stage, boolean busy, boolean foodShort, List<BuildingSpec> buildings, List<Plot> plots,
                        Set<String> built, Set<String> builtGods, List<ShrineRequest> shrines, Set<String> satisfied,
                        Set<String> replaceable) {
        public Input {
            buildings = List.copyOf(buildings);
            plots = List.copyOf(plots);
            built = Set.copyOf(built);
            builtGods = Set.copyOf(builtGods);
            shrines = List.copyOf(shrines);
            satisfied = Set.copyOf(satisfied);
            replaceable = Set.copyOf(replaceable);
        }
    }

    /**
     * @param plot     地块 id；原地替换为空串
     * @param god      次神祠的神；其他为空串
     * @param replaces 被替换的旧建筑 id；其他为空串
     */
    public record Pick(String building, String plot, String god, String replaces) {
    }

    private ProjectPicker() {
    }

    public static Optional<Pick> pick(Input in) {
        if (in.busy() || in.foodShort() || !in.stage().atLeast(Age.CLASSICAL)) {
            return Optional.empty();
        }
        for (ShrineRequest r : in.shrines()) {
            if (in.builtGods().contains(r.god()) || !in.stage().atLeast(r.age())) {
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
            if (plot.isPresent()) {
                return Optional.of(new Pick(s.id(), plot.get().id(), r.god(), ""));
            }
        }
        List<BuildingSpec> sorted = in.buildings().stream()
                .filter(b -> b.god().isEmpty())
                .sorted(Comparator.comparingInt(BuildingSpec::priority).thenComparing(BuildingSpec::id)).toList();
        for (BuildingSpec s : sorted) {
            Age need = Age.byId(s.stage()).orElse(Age.ARCHAIC);
            if (!in.stage().atLeast(need) || (s.unique() && in.built().contains(s.id()))
                    || !in.satisfied().containsAll(s.requires())) {
                continue;
            }
            if (!s.replace().isEmpty()) {
                Optional<String> old = in.replaceable().stream()
                        .filter(id -> s.replace().stream().anyMatch(id::startsWith)).sorted().findFirst();
                if (old.isPresent()) {
                    return Optional.of(new Pick(s.id(), "", "", old.get()));
                }
                continue;
            }
            Optional<Plot> plot = in.plots().stream()
                    .filter(p -> s.plotKinds().contains(p.kind()) && in.stage().atLeast(p.stage()) && fits(s, p))
                    .min(Comparator.comparingInt((Plot p) -> p.stage().ordinal()).thenComparing(Plot::id));
            if (plot.isPresent()) {
                return Optional.of(new Pick(s.id(), plot.get().id(), "", ""));
            }
        }
        return Optional.empty();
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
