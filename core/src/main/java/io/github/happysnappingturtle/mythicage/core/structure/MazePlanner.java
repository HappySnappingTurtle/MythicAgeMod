package io.github.happysnappingturtle.mythicage.core.structure;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * 地下迷宫的网格规划（T22 设计文档 §5.2，D2）。纯函数，结果只由种子决定。
 *
 * <pre>
 *   x →  0 1 2 3 4 5 6 7
 * z 0    . . . . . . . .
 *   1    . . . V V . . .      V 宝库房（只从大厅北墙进，不属于迷宫）
 *   2    . . H H H H . .
 *   3    . . H H H H . .      H 中心大厅 4 × 4 格（31 × 31 方块）
 *   4    . . H H H H . .
 *   5    . . H H H H . .
 *   6    . . . A A . . .      A 牛首门前室（两格并成一间，北面是大厅的门）
 *   7    E . . . . . . .      E 入口格（南面接下行阶梯井）
 * </pre>
 * 步骤：先随机回溯找一条正好 {@value #PATH_MIN}–{@value #PATH_MAX} 段的主路径（入口 → 前室，步数有上限）→ 其余格子从已连上的格子
 * 随机长出去（随机 Prim，每格连通、无环、死胡同多）→ 打通 {@value #LOOPS_MIN}–{@value #LOOPS_MAX} 面墙成回路（不把主路径缩短到下限以下）
 * → 特殊房间挑主路径以外的死胡同（休整点、教学柱廊各 1，储物间 1–2）。不满足就换种子重算，最多 {@value #ATTEMPTS} 次，
 * 仍不行用固定布局（有界，不无限重试）。
 */
public final class MazePlanner {
    public static final int SIZE = 8;
    public static final int PATH_MIN = 8;
    public static final int PATH_MAX = 12;
    public static final int LOOPS_MIN = 3;
    public static final int LOOPS_MAX = 4;
    public static final int ATTEMPTS = 16;
    /** 找主路径的回溯步数上限（有界）。 */
    static final int SEARCH_BUDGET = 20_000;

    public enum Dir {
        N(0, -1), E(1, 0), S(0, 1), W(-1, 0);

        public final int dx;
        public final int dz;

        Dir(int dx, int dz) {
            this.dx = dx;
            this.dz = dz;
        }

        public Dir opposite() {
            return values()[(ordinal() + 2) % 4];
        }
    }

    public record Cell(int x, int z) {
        public Cell step(Dir d) {
            return new Cell(x + d.dx, z + d.dz);
        }

        boolean inGrid() {
            return x >= 0 && z >= 0 && x < SIZE && z < SIZE;
        }
    }

    public enum Room { CORRIDOR, ENTRANCE, ANTECHAMBER, REST, COLONNADE, STORAGE }

    public static final Cell ENTRANCE = new Cell(0, 7);
    public static final List<Cell> ANTECHAMBER = List.of(new Cell(3, 6), new Cell(4, 6));
    public static final List<Cell> VAULT = List.of(new Cell(3, 1), new Cell(4, 1));
    public static final int HALL_MIN = 2;
    public static final int HALL_MAX = 5;

    /**
     * 规划结果。
     *
     * @param open     每格开口的方向（入口格另有朝南通往阶梯井的口；前室两格之间互通）
     * @param rooms    每格的房间种类
     * @param mainPath 入口 → 前室的格子序列（含两端）
     * @param fallback 是不是用了固定布局
     */
    public record Plan(Map<Cell, Set<Dir>> open, Map<Cell, Room> rooms, List<Cell> mainPath, boolean fallback) {
        public int segments() {
            return mainPath.size() - 1;
        }

        public Set<Dir> openings(Cell c) {
            return open.getOrDefault(c, Set.of());
        }
    }

    private MazePlanner() {
    }

    public static boolean hall(Cell c) {
        return c.x() >= HALL_MIN && c.x() <= HALL_MAX && c.z() >= HALL_MIN && c.z() <= HALL_MAX;
    }

    /** 迷宫格（不含大厅、宝库房）。 */
    public static boolean node(Cell c) {
        return c.inGrid() && !hall(c) && !VAULT.contains(c);
    }

    private static Cell key(Cell c) {
        return ANTECHAMBER.contains(c) ? ANTECHAMBER.get(0) : c;
    }

    public static Plan plan(long seed) {
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            Plan p = tryPlan(new Random(seed + attempt * 0x9E3779B97F4A7C15L), false);
            if (p != null) {
                return p;
            }
        }
        return fallback();
    }

    private static volatile Plan fallback;

    /** 固定布局：从种子 0 起找到的第一个合格布局（只算一次）。 */
    public static Plan fallback() {
        Plan f = fallback;
        if (f == null) {
            for (long s = 0; s < 100_000 && f == null; s++) {
                f = tryPlan(new Random(s), true);
            }
            fallback = f;
        }
        return f;
    }

    static Plan tryPlan(Random random, boolean asFallback) {
        Map<Cell, Set<Dir>> open = new LinkedHashMap<>();
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                Cell c = new Cell(x, z);
                if (node(c)) {
                    open.put(c, EnumSet.noneOf(Dir.class));
                }
            }
        }
        connect(open, ANTECHAMBER.get(0), Dir.E);
        // 1. 主路径：随机回溯找一条正好 L 段（8–12）从入口到前室的路
        int length = PATH_MIN + random.nextInt(PATH_MAX - PATH_MIN + 1);
        List<Cell> route = new ArrayList<>();
        route.add(ENTRANCE);
        int[] budget = {SEARCH_BUDGET};
        if (!search(route, length, random, budget)) {
            return null;
        }
        Set<Cell> visited = new java.util.HashSet<>();
        for (int i = 0; i + 1 < route.size(); i++) {
            Cell a = route.get(i);
            Cell b = route.get(i + 1);
            for (Dir d : Dir.values()) {
                if (a.step(d).equals(b)) {
                    connect(open, a, d);
                }
            }
        }
        route.forEach(c -> visited.add(key(c)));
        visited.add(key(ANTECHAMBER.get(0)));
        // 2. 其余格子从已连上的格子随机长出去（随机 Prim：分支多、死胡同多）
        while (true) {
            List<Cell[]> frontier = new ArrayList<>();
            for (Cell c : open.keySet()) {
                if (!visited.contains(key(c))) {
                    continue;
                }
                for (Dir d : Dir.values()) {
                    Cell n = c.step(d);
                    if (node(n) && !visited.contains(key(n))) {
                        frontier.add(new Cell[]{c, new Cell(d.ordinal(), 0)});
                    }
                }
            }
            if (frontier.isEmpty()) {
                break;
            }
            Cell[] pick = frontier.get(random.nextInt(frontier.size()));
            Dir d = Dir.values()[pick[1].x()];
            connect(open, pick[0], d);
            visited.add(key(pick[0].step(d)));
        }
        // 3. 回路：打通几面还关着的墙，但不能把主路径缩短到下限以下
        int loops = LOOPS_MIN + random.nextInt(LOOPS_MAX - LOOPS_MIN + 1);
        List<Cell[]> walls = new ArrayList<>();
        for (Cell c : open.keySet()) {
            for (Dir d : List.of(Dir.E, Dir.S)) {
                Cell n = c.step(d);
                if (node(n) && !open.get(c).contains(d) && !key(c).equals(key(n))) {
                    walls.add(new Cell[]{c, new Cell(d.ordinal(), 0)});
                }
            }
        }
        Collections.shuffle(walls, random);
        int added = 0;
        for (Cell[] w : walls) {
            if (added >= loops) {
                break;
            }
            Dir d = Dir.values()[w[1].x()];
            connect(open, w[0], d);
            if (mainPath(open).size() - 1 < PATH_MIN) {
                disconnect(open, w[0], d);
            } else {
                added++;
            }
        }
        List<Cell> path = mainPath(open);
        if (path.size() - 1 < PATH_MIN || path.size() - 1 > PATH_MAX) {
            return null;
        }
        // 特殊房间：主路径以外的死胡同
        List<Cell> deadEnds = new ArrayList<>();
        for (Map.Entry<Cell, Set<Dir>> e : open.entrySet()) {
            Cell c = e.getKey();
            if (e.getValue().size() == 1 && !c.equals(ENTRANCE) && !ANTECHAMBER.contains(c) && !path.contains(c)) {
                deadEnds.add(c);
            }
        }
        if (deadEnds.size() < 2) {
            return null;
        }
        Collections.shuffle(deadEnds, random);
        Map<Cell, Room> rooms = new HashMap<>();
        open.keySet().forEach(c -> rooms.put(c, Room.CORRIDOR));
        rooms.put(ENTRANCE, Room.ENTRANCE);
        ANTECHAMBER.forEach(c -> rooms.put(c, Room.ANTECHAMBER));
        rooms.put(deadEnds.get(0), Room.REST);
        rooms.put(deadEnds.get(1), Room.COLONNADE);
        int storage = Math.min(deadEnds.size() - 2, 1 + random.nextInt(2));
        for (int i = 0; i < storage; i++) {
            rooms.put(deadEnds.get(2 + i), Room.STORAGE);
        }
        open.get(ENTRANCE).add(Dir.S); // 通往阶梯井
        Map<Cell, Set<Dir>> frozen = new LinkedHashMap<>();
        open.forEach((c, d) -> frozen.put(c, Collections.unmodifiableSet(EnumSet.copyOf((EnumSet<Dir>) d))));
        return new Plan(Collections.unmodifiableMap(frozen), Map.copyOf(rooms), List.copyOf(path), asFallback);
    }

    /** 回溯找一条从 route 末端出发、再走 remaining 步正好到前室的路（不重复经过格子）；共享步数预算，用完就放弃。 */
    private static boolean search(List<Cell> route, int remaining, Random random, int[] budget) {
        if (--budget[0] < 0) {
            return false;
        }
        Cell cur = route.get(route.size() - 1);
        if (remaining == 0) {
            return ANTECHAMBER.contains(cur);
        }
        if (ANTECHAMBER.contains(cur) || distanceToAntechamber(cur) > remaining) {
            return false;
        }
        List<Dir> dirs = new ArrayList<>(List.of(Dir.values()));
        Collections.shuffle(dirs, random);
        for (Dir d : dirs) {
            Cell n = cur.step(d);
            if (!node(n) || route.contains(n) || ANTECHAMBER.contains(n) && remaining != 1) {
                continue;
            }
            route.add(n);
            if (search(route, remaining - 1, random, budget)) {
                return true;
            }
            route.remove(route.size() - 1);
        }
        return false;
    }

    private static int distanceToAntechamber(Cell c) {
        int best = Integer.MAX_VALUE;
        for (Cell a : ANTECHAMBER) {
            best = Math.min(best, Math.abs(a.x() - c.x()) + Math.abs(a.z() - c.z()));
        }
        return best;
    }

    private static void disconnect(Map<Cell, Set<Dir>> open, Cell c, Dir d) {
        open.get(c).remove(d);
        open.get(c.step(d)).remove(d.opposite());
    }

    private static void connect(Map<Cell, Set<Dir>> open, Cell c, Dir d) {
        open.get(c).add(d);
        open.get(c.step(d)).add(d.opposite());
    }

    /** 入口 → 前室（任一格）的最短路径（广度优先，前室两格之间那一步不算一段）。 */
    static List<Cell> mainPath(Map<Cell, Set<Dir>> open) {
        Map<Cell, Cell> prev = new HashMap<>();
        ArrayDeque<Cell> q = new ArrayDeque<>();
        q.add(ENTRANCE);
        prev.put(ENTRANCE, ENTRANCE);
        while (!q.isEmpty()) {
            Cell c = q.poll();
            if (ANTECHAMBER.contains(c)) {
                List<Cell> path = new ArrayList<>();
                for (Cell x = c; ; x = prev.get(x)) {
                    path.add(0, x);
                    if (x.equals(ENTRANCE)) {
                        break;
                    }
                }
                return path;
            }
            for (Dir d : open.get(c)) {
                Cell n = c.step(d);
                if (open.containsKey(n) && !prev.containsKey(n)) {
                    prev.put(n, c);
                    q.add(n);
                }
            }
        }
        return List.of(ENTRANCE);
    }
}
