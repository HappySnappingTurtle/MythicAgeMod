package io.github.happysnappingturtle.mythicage.core.village.build;

import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 施工段（T19 设计文档 §8.3、§8.4、§1.4 E4）：纯函数，同一份蓝图总是切出同样的段（重启后按段号续上）。
 * <ul>
 *   <li>格子按 (y, z, x) 排序，每段最多 {@code max} 个<b>非空气</b>格；空气格（清掉草、雪等）跟着顺序落在各段里，
 *       只有空气的尾段并进前一段；</li>
 *   <li>两格方块（门、床的两半，{@link Cell#link()} 相同）放在同一段，可以因此多出一格；</li>
 *   <li>替换：先拆（自上而下），再放；标了「最后」的格子（神像、祭司站位、仪式位）的拆与放排在末尾。</li>
 * </ul>
 */
public final class BuildSegments {
    public static final int MAX = 16;
    public static final String AIR = "minecraft:air";

    public enum Kind { PLACE, DEMOLISH }

    /**
     * 一格。
     *
     * @param block  放置：目标方块 id；拆除：要拆的旧方块 id
     * @param expect 放置时原地应有的旧方块 id（替换：旧建筑的方块，放新方块时折算退回）；空串表示原地应是空地
     * @param link   两格方块的配对号（同号放同一段）；0 表示没有
     * @param last   排到最后（神像、祭司站位、仪式位附近）
     * @param index  在蓝图格子表里的序号（neoforge 用它找方块状态）
     */
    public record Cell(Pos pos, String block, String expect, int link, boolean last, int index) {
        public boolean air() {
            return AIR.equals(block);
        }

        public Cell withLast(boolean l) {
            return new Cell(pos, block, expect, link, l, index);
        }
    }

    public record Segment(int index, Kind kind, List<Cell> cells) {
        public Segment {
            cells = List.copyOf(cells);
        }

        /** 非空气格数（动作时长按它算）。 */
        public int solid() {
            return (int) cells.stream().filter(c -> !c.air()).count();
        }

        /** 这一段要放（放置段）或拆（拆除段）的方块 id。 */
        public List<String> blocks() {
            return cells.stream().filter(c -> !c.air()).map(Cell::block).toList();
        }

        /** 放置段里被换掉的旧方块 id（替换时退回村库）。 */
        public List<String> replaced() {
            return kind == Kind.PLACE ? cells.stream().filter(c -> !c.expect().isEmpty()).map(Cell::expect).toList() : List.of();
        }

        /** 水平中心（居民在建筑外圈离它最近的地方锤打）。 */
        public Pos center() {
            long x = 0, y = 0, z = 0;
            for (Cell c : cells) {
                x += c.pos().x();
                y += c.pos().y();
                z += c.pos().z();
            }
            int n = Math.max(1, cells.size());
            return new Pos((int) Math.floorDiv(x, n), (int) Math.floorDiv(y, n), (int) Math.floorDiv(z, n));
        }
    }

    private BuildSegments() {
    }

    static final Comparator<Cell> UP = Comparator.comparingInt((Cell c) -> c.pos().y()).thenComparingInt(c -> c.pos().z())
            .thenComparingInt(c -> c.pos().x());
    static final Comparator<Cell> DOWN = Comparator.comparingInt((Cell c) -> -c.pos().y()).thenComparingInt(c -> c.pos().z())
            .thenComparingInt(c -> c.pos().x());

    /** 新建：全部是放置段，自下而上。 */
    public static List<Segment> legacyCut(List<Cell> cells, int max) {
        List<Segment> out = new ArrayList<>();
        List<Cell> normal = cells.stream().filter(c -> !c.last()).sorted(UP).toList();
        List<Cell> late = cells.stream().filter(Cell::last).sorted(UP).toList();
        append(out, Kind.PLACE, normal, max);
        append(out, Kind.PLACE, late, max);
        return out;
    }

    /**
     * 替换（V3-希腊-建筑 §3.2）：比较新旧蓝图的同一格——
     * <ul>
     *   <li>完全相同：不动；</li>
     *   <li>旧有、新蓝图是空气或没写（且不在 {@code keepBelowY} 以下）：拆；</li>
     *   <li>新有、旧没有或不同：放（旧方块记在 {@link Cell#expect()}）；</li>
     *   <li>新蓝图的空气格、旧也没有：照样跟着放置段（清掉自然方块）。</li>
     * </ul>
     *
     * @param keepBelowY 旧模板在这一高度以下的地基，新模板没写就不拆（模板 y &lt; 0 的空格保留地形）
     * @param last       这一格排到最后（新旧蓝图的神像、祭司站位、仪式位附近）
     */
    public static List<Segment> legacyReplace(List<Cell> oldCells, List<Cell> newCells, int keepBelowY, Predicate<Pos> last, int max) {
        Map<Pos, Cell> olds = new HashMap<>();
        for (Cell c : oldCells) {
            if (!c.air()) {
                olds.put(c.pos(), c);
            }
        }
        Set<Pos> seen = new HashSet<>();
        List<Cell> place = new ArrayList<>();
        List<Cell> demolish = new ArrayList<>();
        for (Cell n : newCells) {
            seen.add(n.pos());
            Cell o = olds.get(n.pos());
            if (n.air()) {
                if (o != null) {
                    demolish.add(new Cell(o.pos(), o.block(), "", 0, last.test(o.pos()), o.index()));
                } else {
                    place.add(n.withLast(last.test(n.pos())));
                }
                continue;
            }
            if (o != null && o.block().equals(n.block())) {
                continue;
            }
            place.add(new Cell(n.pos(), n.block(), o == null ? "" : o.block(), n.link(), last.test(n.pos()), n.index()));
        }
        for (Cell o : olds.values()) {
            if (!seen.contains(o.pos()) && o.pos().y() >= keepBelowY) {
                demolish.add(new Cell(o.pos(), o.block(), "", 0, last.test(o.pos()), o.index()));
            }
        }
        // 两格方块：一半在「最后」区里，另一半也排到最后
        Map<Integer, Boolean> lateLinks = new HashMap<>();
        for (Cell c : place) {
            if (c.link() != 0 && c.last()) {
                lateLinks.put(c.link(), true);
            }
        }
        List<Cell> placeFixed = place.stream()
                .map(c -> c.link() != 0 && lateLinks.containsKey(c.link()) ? c.withLast(true) : c).toList();
        List<Segment> out = new ArrayList<>();
        append(out, Kind.DEMOLISH, demolish.stream().filter(c -> !c.last()).sorted(DOWN).toList(), max);
        append(out, Kind.PLACE, placeFixed.stream().filter(c -> !c.last()).sorted(UP).toList(), max);
        append(out, Kind.DEMOLISH, demolish.stream().filter(Cell::last).sorted(DOWN).toList(), max);
        append(out, Kind.PLACE, placeFixed.stream().filter(Cell::last).sorted(UP).toList(), max);
        return out;
    }

    /** Trip capacity counts physical blocks; template air follows the trip without consuming cargo capacity. */
    public static List<Segment> cut(List<Cell> cells, int max) {
        return recut(legacyCut(cells, max), max);
    }

    public static List<Segment> replace(List<Cell> oldCells, List<Cell> newCells, int keepBelowY, Predicate<Pos> last, int max) {
        Set<Pos> critical = new HashSet<>();
        oldCells.stream().filter(Cell::last).forEach(c -> critical.add(c.pos()));
        newCells.stream().filter(Cell::last).forEach(c -> critical.add(c.pos()));
        var source = legacyReplace(oldCells, newCells, keepBelowY, p -> last.test(p) || critical.contains(p), max);
        Map<Pos, Cell> old = new HashMap<>(); oldCells.forEach(c -> old.put(c.pos(), c));
        List<Segment> phases = new ArrayList<>();
        for (boolean late : new boolean[]{false, true}) {
            List<Cell> remove = new ArrayList<>(), place = new ArrayList<>();
            for (Segment segment : source) for (Cell c : segment.cells()) {
                if (c.last() != late) continue;
                if (segment.kind() == Kind.DEMOLISH) remove.add(c);
                else {
                    if (!c.expect().isEmpty()) {
                        Cell previous = old.get(c.pos());
                        remove.add(new Cell(c.pos(), previous.block(), "", 0, c.last(), previous.index()));
                    }
                    place.add(new Cell(c.pos(), c.block(), "", c.link(), c.last(), c.index()));
                }
            }
            if (!remove.isEmpty()) phases.add(new Segment(phases.size(), Kind.DEMOLISH, remove.stream().sorted(DOWN).toList()));
            if (!place.isEmpty()) phases.add(new Segment(phases.size(), Kind.PLACE, place.stream().sorted(UP).toList()));
        }
        return recut(phases, max);
    }

    private static List<Segment> recut(List<Segment> source, int max) {
        if (max < 1) throw new IllegalArgumentException("tripSize must be positive");
        List<Segment> result = new ArrayList<>();
        List<Cell> pending = new ArrayList<>();
        Kind kind = null;
        int solid = 0;
        boolean late = false;
        for (Segment segment : source) {
            boolean nextLate = segment.cells().stream().anyMatch(Cell::last);
            if (kind != null && (kind != segment.kind() || late != nextLate)) {
                if (!pending.isEmpty()) result.add(new Segment(result.size(), kind, pending));
                pending = new ArrayList<>(); solid = 0;
            }
            kind = segment.kind(); late = nextLate;
            for (int i = 0; i < segment.cells().size();) {
                Cell c = segment.cells().get(i);
                int count = 1;
                while (c.link() != 0 && i + count < segment.cells().size() && segment.cells().get(i + count).link() == c.link()) count++;
                int nextSolid = 0;
                for (int j = 0; j < count; j++) if (!segment.cells().get(i + j).air()) nextSolid++;
                if (nextSolid <= max && solid + nextSolid > max && !pending.isEmpty()) {
                    result.add(new Segment(result.size(), kind, pending)); pending = new ArrayList<>(); solid = 0;
                }
                for (int j = 0; j < count; j++) {
                    Cell next = segment.cells().get(i++);
                    if (!next.air() && solid == max) { result.add(new Segment(result.size(), kind, pending)); pending = new ArrayList<>(); solid = 0; }
                    pending.add(next);
                    if (!next.air()) solid++;
                }
            }
        }
        if (!pending.isEmpty()) result.add(new Segment(result.size(), kind, pending));
        return List.copyOf(result);
    }

    /** Stable topological order within a trip; cyclic/unavailable dependencies remain deferred by runtime. */
    public static Segment ordered(Segment segment, Predicate<Cell> attached,
                                  java.util.function.Function<Cell, java.util.Set<Pos>> dependencies) {
        if (segment.kind() == Kind.DEMOLISH) return segment;
        List<Cell> pending = new ArrayList<>(segment.cells());
        pending.sort(Comparator.comparing(attached::test));
        List<Cell> ordered = new ArrayList<>();
        while (!pending.isEmpty()) {
            Cell next = pending.stream().filter(c -> pending.stream().noneMatch(other -> other != c
                    && dependencies.apply(c).contains(other.pos()))).findFirst().orElse(pending.getFirst());
            ordered.add(next);
            pending.remove(next);
        }
        return new Segment(segment.index(), segment.kind(), ordered);
    }

    private static void append(List<Segment> out, Kind kind, List<Cell> sorted, int max) {
        Map<Integer, List<Cell>> links = new LinkedHashMap<>();
        for (Cell c : sorted) {
            if (c.link() != 0) {
                links.computeIfAbsent(c.link(), k -> new ArrayList<>()).add(c);
            }
        }
        Set<Integer> done = new HashSet<>();
        List<Cell> cur = new ArrayList<>();
        int solid = 0;
        int start = out.size();
        for (Cell c : sorted) {
            if (c.link() != 0) {
                if (!done.add(c.link())) {
                    continue;
                }
                if (solid >= max) {
                    out.add(new Segment(out.size(), kind, cur));
                    cur = new ArrayList<>();
                    solid = 0;
                }
                for (Cell m : links.get(c.link())) {
                    cur.add(m);
                    solid += m.air() ? 0 : 1;
                }
                continue;
            }
            if (!c.air() && solid >= max) {
                out.add(new Segment(out.size(), kind, cur));
                cur = new ArrayList<>();
                solid = 0;
            }
            cur.add(c);
            solid += c.air() ? 0 : 1;
        }
        if (!cur.isEmpty()) {
            if (solid == 0 && out.size() > start) {
                // 只有空气的尾段并进前一段
                Segment prev = out.remove(out.size() - 1);
                List<Cell> merged = new ArrayList<>(prev.cells());
                merged.addAll(cur);
                out.add(new Segment(prev.index(), kind, merged));
            } else {
                out.add(new Segment(out.size(), kind, cur));
            }
        }
    }
}
