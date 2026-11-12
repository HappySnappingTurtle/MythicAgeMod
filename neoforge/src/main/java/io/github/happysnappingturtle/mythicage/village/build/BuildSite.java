package io.github.happysnappingturtle.mythicage.village.build;

import io.github.happysnappingturtle.mythicage.core.village.build.BuildCosts;
import io.github.happysnappingturtle.mythicage.core.village.build.BuildPlacement;
import io.github.happysnappingturtle.mythicage.core.village.build.BuildSegments;
import io.github.happysnappingturtle.mythicage.core.village.gen.Box;
import io.github.happysnappingturtle.mythicage.core.village.gen.BuildingSpec;
import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.core.village.gen.TemplateTransform;
import io.github.happysnappingturtle.mythicage.core.village.gen.Turn;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;

/**
 * 一个项目的工地（不存档，按蓝图重新算出来，同一蓝图结果不变）：每一格的世界坐标与方块状态、施工段、每段的成本。
 * 新建：全是放置段；原地替换：先拆后放，神像、祭司站位、仪式位附近排到最后（§1.4 E4）。
 */
public final class BuildSite {
    /** 替换时排到最后的锚点（神像、祭司床位、仪式位、祠的祭坛）及其周围范围。 */
    static final List<String> LEGACY_LAST_ANCHORS = List.of("guardian", "priest_home", "ritual", "shrine");
    static final List<String> LAST_ANCHORS = List.of("guardian", "priest_home", "ritual", "shrine", "home", "work", "workstation", "altar");
    static final int LAST_RADIUS = 2;

    public final BuildingSpec spec;
    public final BuildPlacement.Placed at;
    public final BuildCosts costs;
    /** 新蓝图各格的方块状态（放置段的 {@link BuildSegments.Cell#index()} 指向这里）。 */
    final List<BlockState> states;
    /** 旧蓝图各格的方块状态（拆除段指向这里）；新建为空。 */
    final List<BlockState> oldStates;
    public final List<BuildSegments.Segment> segments;
    /** 放置段的成本（拆除段为空表）。 */
    final List<Map<String, Double>> segCost;
    /** 地面层的世界高度（居民站的地方）。 */
    final int groundY;
    /** 被替换的旧建筑包围盒（新建为 null）：它外扩一圈里村庄生成时砌的挡土墙可以清掉。 */
    final Box oldBox;
    /** 各段的锤打站位（落下任一段后清空重找）。 */
    final Map<Integer, BlockPos> stands = new HashMap<>();

    private BuildSite(BuildingSpec spec, BuildPlacement.Placed at, BuildCosts costs, List<BlockState> states,
                      List<BlockState> oldStates, List<BuildSegments.Segment> segments, int groundY, Box oldBox) {
        this.spec = spec;
        this.at = at;
        this.costs = costs;
        this.states = states;
        this.oldStates = oldStates;
        this.segments = List.copyOf(segments);
        this.groundY = groundY;
        this.oldBox = oldBox;
        List<Map<String, Double>> c = new ArrayList<>();
        for (BuildSegments.Segment s : segments) {
            c.add(s.kind() == BuildSegments.Kind.PLACE ? costs.sum(s.blocks()) : Map.of());
        }
        this.segCost = List.copyOf(c);
    }

    /** 文明折算表加上建筑自己的规则。 */
    public static BuildCosts costsFor(BuildCosts civ, BuildingSpec spec) {
        return civ.withLeading(BuildCosts.rules(spec.buildCosts()));
    }

    /** 新建。 */
    public static Optional<BuildSite> create(ServerLevel level, BuildCosts civCosts, BuildingSpec spec, BuildPlacement.Placed at) {
        return create(level, civCosts, spec, at, 16, false);
    }

    public static Optional<BuildSite> create(ServerLevel level, BuildCosts civCosts, BuildingSpec spec, BuildPlacement.Placed at,
                                              int tripSize, boolean legacy) {
        Optional<List<Blueprint.Cell>> bp = Blueprint.load(level, spec.template());
        if (bp.isEmpty()) {
            return Optional.empty();
        }
        List<BlockState> states = new ArrayList<>();
        List<BuildSegments.Cell> cells = cells(bp.get(), at, states, p -> false);
        if (legacy) cells = cells.stream().map(c -> c.withLast(false)).toList();
        return Optional.of(new BuildSite(spec, at, costsFor(civCosts, spec), states, List.of(),
                (legacy ? BuildSegments.legacyCut(cells, 16) : ordered(level, BuildSegments.cut(cells, tripSize), states)), at.origin().y() + spec.floorY(), null));
    }

    /** 原地替换旧建筑。 */
    public static Optional<BuildSite> replace(ServerLevel level, BuildCosts civCosts, BuildingSpec spec, BuildPlacement.Placed at,
                                              BuildingSpec oldSpec, BuildPlacement.Placed oldAt) {
        return replace(level, civCosts, spec, at, oldSpec, oldAt, 16, false);
    }

    public static Optional<BuildSite> replace(ServerLevel level, BuildCosts civCosts, BuildingSpec spec, BuildPlacement.Placed at,
                                              BuildingSpec oldSpec, BuildPlacement.Placed oldAt, int tripSize, boolean legacy) {
        Optional<List<Blueprint.Cell>> bp = Blueprint.load(level, spec.template());
        Optional<List<Blueprint.Cell>> old = Blueprint.load(level, oldSpec.template());
        if (bp.isEmpty() || old.isEmpty()) {
            return Optional.empty();
        }
        List<Pos> keep = new ArrayList<>();
        for (String a : legacy ? LEGACY_LAST_ANCHORS : LAST_ANCHORS) {
            keep.addAll(BuildPlacement.worldAnchors(spec, at).getOrDefault(a, List.of()));
            keep.addAll(BuildPlacement.worldAnchors(oldSpec, oldAt).getOrDefault(a, List.of()));
        }
        Predicate<Pos> last = p -> keep.stream().anyMatch(k -> Math.abs(k.x() - p.x()) <= LAST_RADIUS
                && Math.abs(k.z() - p.z()) <= LAST_RADIUS && p.y() - k.y() >= -1 && p.y() - k.y() <= 3);
        List<BlockState> states = new ArrayList<>();
        List<BlockState> oldStates = new ArrayList<>();
        List<BuildSegments.Cell> nu = cells(bp.get(), at, states, p -> false);
        List<BuildSegments.Cell> ol = cells(old.get(), oldAt, oldStates, p -> false);
        if (!legacy) {
            java.util.Set<String> stations = new java.util.HashSet<>();
            io.github.happysnappingturtle.mythicage.village.work.VillageData.civ(spec.civ()).ifPresent(data -> {
                data.stations().forEach(search -> stations.addAll(search.blocks()));
                data.recipes().values().stream().map(r -> r.station()).filter(id -> !id.startsWith("anchor:") && !id.startsWith("point:"))
                        .forEach(stations::add);
            });
            nu = nu.stream().map(c -> c.withLast(c.last() || stations.contains(c.block()))).toList();
            ol = ol.stream().map(c -> c.withLast(c.last() || stations.contains(c.block()))).toList();
        }
        int ground = oldAt.origin().y() + oldSpec.floorY();
        return Optional.of(new BuildSite(spec, at, costsFor(civCosts, spec), states, oldStates,
                (legacy ? BuildSegments.legacyReplace(ol, nu, ground, last, 16)
                        : ordered(level, BuildSegments.replace(ol, nu, ground, last, tripSize), states)), at.origin().y() + spec.floorY(), oldAt.box()));
    }

    private static io.github.happysnappingturtle.mythicage.core.greek.temple.ArchitectureAttachments attachments;

    public static io.github.happysnappingturtle.mythicage.core.greek.temple.ArchitectureAttachments attachments(ServerLevel level) {
        if (attachments == null) {
            var id = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mythic_age", "mythic_age/architecture_attach.json");
            try (var in = level.getServer().getResourceManager().getResourceOrThrow(id).open()) {
                attachments = io.github.happysnappingturtle.mythicage.core.greek.temple.ArchitectureAttachments.parse(
                        new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            } catch (java.io.IOException e) { throw new IllegalStateException("Missing architecture attachments", e); }
        }
        return attachments;
    }

    static void clearAttachments() { attachments = null; }

    private static boolean attached(BlockState state, ServerLevel level) {
        return attachments(level).requiresSupport(id(state)) || state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                || state.hasProperty(BlockStateProperties.BED_PART) || state.getBlock() instanceof net.minecraft.world.level.block.TorchBlock
                || state.getBlock() instanceof net.minecraft.world.level.block.LadderBlock;
    }

    private static List<BuildSegments.Segment> ordered(ServerLevel level, List<BuildSegments.Segment> segments, List<BlockState> states) {
        Map<io.github.happysnappingturtle.mythicage.core.greek.temple.ArchitectureAttachments.Pos,
                io.github.happysnappingturtle.mythicage.core.greek.temple.ArchitectureAttachments.State> blueprint = new HashMap<>();
        for (var seg : segments) if (seg.kind() == BuildSegments.Kind.PLACE) for (var c : seg.cells()) {
            blueprint.put(new io.github.happysnappingturtle.mythicage.core.greek.temple.ArchitectureAttachments.Pos(
                    c.pos().x(), c.pos().y(), c.pos().z()), attachmentState(states.get(c.index())));
        }
        return segments.stream().map(seg -> BuildSegments.ordered(seg, c -> attached(states.get(c.index()), level), c -> {
            java.util.Set<Pos> dependencies = new java.util.HashSet<>();
            BlockState state = states.get(c.index());
            if (attached(state, level)) dependencies.add(new Pos(c.pos().x(), c.pos().y() - 1, c.pos().z()));
            var rules = attachments(level);
            var at = new io.github.happysnappingturtle.mythicage.core.greek.temple.ArchitectureAttachments.Pos(c.pos().x(), c.pos().y(), c.pos().z());
            for (var support : rules.supports(at, attachmentState(state), blueprint::get)) {
                dependencies.add(new Pos(support.x(), support.y(), support.z()));
            }
            return dependencies;
        })).toList();
    }

    static io.github.happysnappingturtle.mythicage.core.greek.temple.ArchitectureAttachments.State attachmentState(BlockState state) {
        Map<String, String> props = new HashMap<>();
        state.getValues().forEach((k, v) -> props.put(k.getName(), v.toString().toLowerCase(java.util.Locale.ROOT)));
        return new io.github.happysnappingturtle.mythicage.core.greek.temple.ArchitectureAttachments.State(
                id(state), props.getOrDefault("facing", "north"), props);
    }

    boolean supported(ServerLevel level, BlockPos at, BlockState state) {
        var rules = attachments(level);
        if (rules.requiresSupport(id(state))) {
            var pos = new io.github.happysnappingturtle.mythicage.core.greek.temple.ArchitectureAttachments.Pos(at.getX(), at.getY(), at.getZ());
            if (rules.supports(pos, attachmentState(state), p -> attachmentState(level.getBlockState(new BlockPos(p.x(), p.y(), p.z())))).isEmpty()) return false;
        }
        return state.canSurvive(level, at);
    }

    /** Bounded candidate search; path computation uses the shared resident budget. */
    public Optional<BlockPos> standFor(ServerLevel level, BlockPos target,
                                      io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity worker, boolean force) {
        // 先只花一个名额问「能不能走到目标 2 格以内」，走得到就站在路径终点；不行再逐个试候选站位
        if (io.github.happysnappingturtle.mythicage.village.life.PathBudget.take() || force) {
            var direct = worker.getNavigation().createPath(target, 6);
            if (direct != null && direct.canReach() && direct.getEndNode() != null) {
                BlockPos end = direct.getEndNode().asBlockPos();
                if (end.distSqr(target) <= 49 && standable(level, end)) {
                    return Optional.of(end);
                }
            }
        }
        List<BlockPos> candidates = new ArrayList<>();
        for (int y = target.getY() - 7; y <= target.getY() + 1; y++) {
            for (int x = target.getX() - 6; x <= target.getX() + 6; x++) {
                for (int z = target.getZ() - 6; z <= target.getZ() + 6; z++) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (p.equals(target) || !level.isLoaded(p) || !standable(level, p)) continue;
                    if (p.distSqr(target) <= 49) candidates.add(p);
                }
            }
        }
        candidates.sort(java.util.Comparator.comparingDouble(p -> worker.distanceToSqr(p.getX() + .5, p.getY(), p.getZ() + .5)
                + (box().containsXZ(p.getX(), p.getZ()) ? 2 : 0)));
        for (BlockPos p : candidates) {
            if (!io.github.happysnappingturtle.mythicage.village.life.PathBudget.take()) break;
            var path = worker.getNavigation().createPath(p, 0);
            if (path != null && path.canReach()) return Optional.of(p);
        }
        return Optional.empty();
    }

    static Rotation rotation(Turn t) {
        return switch (t) {
            case R0 -> Rotation.NONE;
            case R90 -> Rotation.CLOCKWISE_90;
            case R180 -> Rotation.CLOCKWISE_180;
            case R270 -> Rotation.COUNTERCLOCKWISE_90;
        };
    }

    static String id(BlockState s) {
        return BuiltInRegistries.BLOCK.getKey(s.getBlock()).toString();
    }

    /** 蓝图 → 世界格子；两格方块（上下半、床头床尾）配上同一个配对号。 */
    private static List<BuildSegments.Cell> cells(List<Blueprint.Cell> bp, BuildPlacement.Placed at, List<BlockState> states,
                                                  Predicate<Pos> last) {
        Rotation rot = rotation(at.turn());
        List<Pos> world = new ArrayList<>();
        Map<Pos, Integer> paired = new HashMap<>();
        for (Blueprint.Cell c : bp) {
            BlockState s = c.state().rotate(rot);
            Pos w = TemplateTransform.toWorld(at.origin(), new Pos(c.rel().getX(), c.rel().getY(), c.rel().getZ()), at.turn());
            states.add(s);
            world.add(w);
            if (s.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) || s.hasProperty(BlockStateProperties.BED_PART)) {
                paired.put(w, states.size() - 1);
            }
        }
        int[] link = new int[states.size()];
        for (Map.Entry<Pos, Integer> e : paired.entrySet()) {
            if (link[e.getValue()] != 0) {
                continue;
            }
            // 连通的一组（门的两半、床的两半）用组里第一格的序号 + 1
            Deque<Integer> todo = new ArrayDeque<>();
            List<Integer> group = new ArrayList<>();
            todo.add(e.getValue());
            link[e.getValue()] = -1;
            while (!todo.isEmpty()) {
                int i = todo.poll();
                group.add(i);
                Pos p = world.get(i);
                for (Direction d : Direction.values()) {
                    Integer j = paired.get(p.offset(d.getStepX(), d.getStepY(), d.getStepZ()));
                    if (j != null && link[j] == 0 && states.get(j).getBlock() == states.get(i).getBlock() && halves(states.get(i), states.get(j))) {
                        link[j] = -1;
                        todo.add(j);
                    }
                }
            }
            int id = group.stream().min(Integer::compare).orElseThrow() + 1;
            group.forEach(i -> link[i] = id);
        }
        List<BuildSegments.Cell> out = new ArrayList<>();
        for (int i = 0; i < states.size(); i++) {
            out.add(new BuildSegments.Cell(world.get(i), id(states.get(i)), "", Math.max(0, link[i]), last.test(world.get(i)) || states.get(i).hasProperty(BlockStateProperties.BED_PART), i));
        }
        return out;
    }

    private static boolean halves(BlockState a, BlockState b) {
        if (a.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            return a.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) != b.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
        }
        return a.getValue(BlockStateProperties.BED_PART) != b.getValue(BlockStateProperties.BED_PART);
    }

    /** 格子的目标方块状态（放置段：新蓝图；拆除段：旧蓝图里的旧方块）。 */
    BlockState state(BuildSegments.Segment seg, BuildSegments.Cell c) {
        return seg.kind() == BuildSegments.Kind.PLACE ? states.get(c.index()) : oldStates.get(c.index());
    }

    /** 全部放置段的成本合计（项目的账单）。 */
    public Map<String, Double> totalCost() {
        Map<String, Double> out = new TreeMap<>();
        segCost.forEach(m -> BuildCosts.add(out, m));
        return out;
    }

    public Map<String, Double> cost(int seg) {
        return segCost.get(seg);
    }

    public Box box() {
        return at.box();
    }

    /**
     * 居民锤打的位置：先在段中心 ±3 格、地面层上下找能站的格子（施工到一半时屋里的地面也能站）；
     * 找不到就站到建筑外圈离这一段最近的地面上。
     */
    public BlockPos stand(ServerLevel level, int seg) {
        return stands.computeIfAbsent(seg, k -> findStand(level, k));
    }

    private BlockPos findStand(ServerLevel level, int seg) {
        Pos c = segments.get(seg).center();
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        int lo = Math.min(groundY, c.y()) - 1;
        for (int x = c.x() - 3; x <= c.x() + 3; x++) {
            for (int z = c.z() - 3; z <= c.z() + 3; z++) {
                for (int y = lo; y <= c.y() + 2; y++) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (!level.isLoaded(p) || !standable(level, p)) {
                        continue;
                    }
                    double d = p.distSqr(new BlockPos(c.x(), c.y(), c.z()));
                    if (d < bestD) {
                        bestD = d;
                        best = p;
                    }
                }
            }
        }
        if (best != null) {
            return best;
        }
        Box b = box();
        int x = Math.clamp(c.x(), b.minX() - 1, b.maxX() + 1);
        int z = Math.clamp(c.z(), b.minZ() - 1, b.maxZ() + 1);
        int[] d = {x - (b.minX() - 1), (b.maxX() + 1) - x, z - (b.minZ() - 1), (b.maxZ() + 1) - z};
        int k = 0;
        for (int i = 1; i < 4; i++) {
            if (d[i] < d[k]) {
                k = i;
            }
        }
        switch (k) {
            case 0 -> x = b.minX() - 1;
            case 1 -> x = b.maxX() + 1;
            case 2 -> z = b.minZ() - 1;
            default -> z = b.maxZ() + 1;
        }
        return new BlockPos(x, groundY, z);
    }

    /**
     * 生成时整地留下的挡土墙、矮墙与压顶（{@code VillageBuildingPiece} 外扩 {@code MARGIN} 格；卫城台地四周也有）：原地替换时，
     * 新旧建筑包围盒外扩这一圈里的多边形砌石（含矮墙）、压顶方石当作可清的地形（实机：中神庙的角上压着远古卫城的挡土墙与坡上的矮墙）。
     */
    boolean oldTerrain(BlockPos p, BlockState s) {
        int m = io.github.happysnappingturtle.mythicage.village.gen.VillageBuildingPiece.MARGIN;
        return oldBox != null
                && (oldBox.inflateXZ(m).containsXZ(p.getX(), p.getZ()) || at.box().inflateXZ(m).containsXZ(p.getX(), p.getZ()))
                && s.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mythic_age", "construction_terrain")));
    }

    static boolean standable(ServerLevel level, BlockPos p) {
        return level.getBlockState(p).getCollisionShape(level, p).isEmpty()
                && level.getBlockState(p.above()).getCollisionShape(level, p.above()).isEmpty()
                && !level.getBlockState(p.below()).getCollisionShape(level, p.below()).isEmpty();
    }
}
