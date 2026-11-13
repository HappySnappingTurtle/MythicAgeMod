package io.github.happysnappingturtle.mythicage.core.village.build;

import io.github.happysnappingturtle.mythicage.core.village.economy.VillageStock;
import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.core.village.gen.Turn;

import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * 一个施工项目（T19 设计文档 §8.3、§1.4）：状态、账单、托管、已放的段。存档在村庄运行数据里，段由蓝图重新切出（同一蓝图结果不变）。
 * <pre>
 * STOCKING ──料齐──▶ BUILDING ──全部落完、验收通过──▶ DONE
 *    ▲                  │  └──遇阻──▶ BLOCKED ──恢复──▶ BUILDING
 *    └──验收不过（修补）/ 料不够──┘
 * 任何状态 ──取消──▶ CANCELLED：只退还托管里剩下的料
 * </pre>
 * 账目（成本键可以是物品组 {@code #planks}，托管里是实际物品）：
 * <ul>
 *   <li>{@code planned}：要放的格子的成本合计（含修补追加）；账单 = 向上取整；</li>
 *   <li>{@code spent}：已放的段的成本合计；该扣 = 向上取整，{@code consumed} 记已扣的整数，每段扣「该扣 − 已扣」；</li>
 *   <li>{@code held}：托管里属于各成本键的实际物品（托管 id 为 {@link #escrowId()}）；</li>
 *   <li>{@code returned}：拆下、换下的旧方块折算，向下取整退回村库。</li>
 * </ul>
 */
public final class BuildProject {
    public static final String ESCROW_PREFIX = "project:";

    public enum State { STOCKING, BUILDING, BLOCKED, DONE, CANCELLED }

    public final long id;
    public final String building;
    /** 地块 id；原地替换为空串。 */
    public final String plot;
    public final String god;
    /** 被替换的旧建筑 id；新建为空串。 */
    public final String replaces;
    public final Pos origin;
    public final Turn turn;
    /** 旧建筑的原点与转向（替换时用来重新切段）。 */
    public final Pos oldOrigin;
    public final Turn oldTurn;
    public final long startDay;
    public State state = State.STOCKING;
    public int segments;
    public final BitSet placed = new BitSet();
    /** Segment-local bitmaps avoid a fixed stride (legacy segments can contain many air cells). */
    public final Map<Integer, BitSet> placedCells = new TreeMap<>();
    public final Map<Integer, BitSet> blockedCells = new TreeMap<>();
    public final Map<Integer, BitSet> missingCells = new TreeMap<>();
    /** Natural trunk cleanup remains incremental and survives a save between construction ticks. */
    public final java.util.Set<Pos> cleanupCells = new java.util.LinkedHashSet<>();
    /**
     * Persist the segmentation algorithm and trip size; datapack edits must not shift progress.
     * 0 = T19、1 = GA-10 逐块、2 = GA-11（依附在「最后放」格上的格也排到最后）。
     */
    public static final int LAYOUT = 2;
    public int layoutVersion = LAYOUT;
    public int tripSize = BuildPacing.Settings.DEFAULT.tripSize();

    public boolean cellPlaced(int segment, int cell) {
        BitSet bits = placedCells.get(segment);
        return bits != null && bits.get(cell);
    }

    public static void mark(Map<Integer, BitSet> cells, int segment, int cell, boolean value) {
        if (value) cells.computeIfAbsent(segment, k -> new BitSet()).set(cell);
        else if (cells.containsKey(segment)) {
            cells.get(segment).clear(cell);
            if (cells.get(segment).isEmpty()) cells.remove(segment);
        }
    }

    public static int count(Map<Integer, BitSet> cells) {
        return cells.values().stream().mapToInt(BitSet::cardinality).sum();
    }

    /** Also handles saves written after reading legacy NBT but before reconstructing the blueprint. */
    public void migrateCells(java.util.List<BuildSegments.Segment> layout) {
        for (BuildSegments.Segment seg : layout) {
            if (placed.get(seg.index())) {
                placedCells.computeIfAbsent(seg.index(), k -> new BitSet()).set(0, seg.cells().size());
            }
        }
    }

    public void placedCell(BuildSegments.Segment segment, int cell) {
        mark(placedCells, segment.index(), cell, true);
        mark(blockedCells, segment.index(), cell, false);
        mark(missingCells, segment.index(), cell, false);
        if (placedCells.get(segment.index()).cardinality() == segment.cells().size()) placed.set(segment.index());
    }

    public final Map<String, Double> planned = new TreeMap<>();
    public final Map<String, Double> spent = new TreeMap<>();
    public final Map<String, Integer> consumed = new TreeMap<>();
    public final Map<String, Map<String, Integer>> held = new TreeMap<>();
    public final Map<String, Double> returned = new TreeMap<>();
    public final Map<String, Integer> returnedGiven = new TreeMap<>();
    /** 停工的坐标与原因（语言键后缀）；没有停工为 null。 */
    public Pos blockedAt;
    public String blockedReason = "";

    public BuildProject(long id, String building, String plot, String god, String replaces, Pos origin, Turn turn,
                        Pos oldOrigin, Turn oldTurn, long startDay) {
        this.id = id;
        this.building = building;
        this.plot = plot;
        this.god = god;
        this.replaces = replaces;
        this.origin = origin;
        this.turn = turn;
        this.oldOrigin = oldOrigin;
        this.oldTurn = oldTurn;
        this.startDay = startDay;
    }

    public String escrowId() {
        return ESCROW_PREFIX + id;
    }

    public boolean active() {
        return state != State.DONE && state != State.CANCELLED;
    }

    public boolean complete() {
        return segments > 0 && placed.cardinality() >= segments;
    }

    public Map<String, Integer> bill() {
        return BuildCosts.ceil(planned);
    }

    public int heldCount(String key) {
        return held.getOrDefault(key, Map.of()).values().stream().mapToInt(Integer::intValue).sum();
    }

    /** 还缺的料：账单 − 已扣 − 托管中。 */
    public Map<String, Integer> missing() {
        Map<String, Integer> out = new TreeMap<>();
        bill().forEach((k, v) -> {
            int n = v - consumed.getOrDefault(k, 0) - heldCount(k);
            if (n > 0) {
                out.put(k, n);
            }
        });
        return out;
    }

    /**
     * 备料：缺的部分从村库能托管多少托管多少（物品组按组内顺序）。返回本次新托管的件数。
     * 「能托管的逐批托管」：村库不够也先锁一部分，生产补上之后下次再锁。
     */
    public int stockUp(VillageStock stock, BuildCosts costs) {
        int got = 0;
        for (Map.Entry<String, Integer> e : missing().entrySet()) {
            int need = e.getValue();
            for (String item : costs.items(e.getKey())) {
                if (need <= 0) {
                    break;
                }
                int n = Math.min(need, stock.count(item));
                if (n > 0 && stock.reserve(escrowId(), Map.of(item, n))) {
                    held.computeIfAbsent(e.getKey(), k -> new TreeMap<>()).merge(item, n, Integer::sum);
                    need -= n;
                    got += n;
                }
            }
        }
        return got;
    }

    /** 这一段的料够不够（扣料前检查）。 */
    public boolean canPay(Map<String, Double> segCost) {
        Map<String, Double> after = new TreeMap<>(spent);
        BuildCosts.add(after, segCost);
        for (Map.Entry<String, Integer> e : BuildCosts.ceil(after).entrySet()) {
            int due = e.getValue() - consumed.getOrDefault(e.getKey(), 0);
            if (due > heldCount(e.getKey())) {
                return false;
            }
        }
        return true;
    }

    /** 落下一段：按累计取整从托管里扣料（先确认 {@link #canPay}）。返回实际扣掉的物品。 */
    public Map<String, Integer> pay(VillageStock stock, Map<String, Double> segCost) {
        BuildCosts.add(spent, segCost);
        Map<String, Integer> used = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : BuildCosts.ceil(spent).entrySet()) {
            int due = e.getValue() - consumed.getOrDefault(e.getKey(), 0);
            Map<String, Integer> h = held.get(e.getKey());
            while (due > 0 && h != null && !h.isEmpty()) {
                Map.Entry<String, Integer> first = h.entrySet().iterator().next();
                int n = Math.min(due, first.getValue());
                stock.consumePart(escrowId(), first.getKey(), n);
                used.merge(first.getKey(), n, Integer::sum);
                if (n == first.getValue()) {
                    h.remove(first.getKey());
                } else {
                    h.put(first.getKey(), first.getValue() - n);
                }
                consumed.merge(e.getKey(), n, Integer::sum);
                due -= n;
            }
            if (h != null && h.isEmpty()) {
                held.remove(e.getKey());
            }
        }
        return used;
    }

    /** 拆下或换下的旧方块：按累计向下取整退回村库（不受容量限制）。返回退回的物品。 */
    public Map<String, Integer> refund(VillageStock stock, BuildCosts costs, Map<String, Double> cost) {
        BuildCosts.add(returned, cost);
        Map<String, Integer> out = new LinkedHashMap<>();
        BuildCosts.floor(returned).forEach((k, v) -> {
            int give = v - returnedGiven.getOrDefault(k, 0);
            if (give > 0) {
                String item = costs.representative(k);
                stock.putUnchecked(item, give);
                out.merge(item, give, Integer::sum);
                returnedGiven.merge(k, give, Integer::sum);
            }
        });
        return out;
    }

    /** 修补（验收不过）：这些段标回未放，账单加上它们的成本，回到备料。 */
    public void unplace(Map<Integer, Map<String, Double>> segCosts) {
        segCosts.forEach((seg, cost) -> {
            placed.clear(seg);
            placedCells.remove(seg);
            blockedCells.remove(seg);
            missingCells.remove(seg);
            BuildCosts.add(planned, cost);
        });
        state = State.STOCKING;
    }

    /** 取消：只退还托管里剩下的料（已放下的不拆、不退）。 */
    public Map<String, Integer> cancel(VillageStock stock) {
        held.clear();
        state = State.CANCELLED;
        return stock.release(escrowId());
    }

    public void block(Pos at, String reason) {
        state = State.BLOCKED;
        blockedAt = at;
        blockedReason = reason;
    }

    public void resume() {
        state = State.BUILDING;
        blockedAt = null;
        blockedReason = "";
    }
}
