package io.github.happysnappingturtle.mythicage.core.boss;

import java.util.ArrayList;
import java.util.List;

/**
 * 阿里阿德涅线团的回程线（T23 §5.1）：玩家每走 ≥ {@value #STEP} 格记一个点；一次跳得远就补点，相邻点距离 ≤ {@value #MAX_GAP}；
 * 最多 {@value #MAX_POINTS} 个点，超出丢最早的；走回到较早的某个点附近（&lt; {@value #BACKTRACK} 格）时，截掉它之后的点——线只留一条。
 * 截断或清空时 {@link #version()} 加一，客户端据此整条重画；否则按 {@link #since(int)} 只取新增的点。
 */
public final class ThreadTrail {
    public static final double STEP = 2.0;
    public static final double MAX_GAP = 3.0;
    public static final int MAX_POINTS = 400;
    public static final double BACKTRACK = 1.5;
    /** 回溯判定不看最近的这么多个点（刚走过的不算折返）。 */
    public static final int RECENT = 3;

    public record Point(double x, double y, double z) {
        double dist(Point o) {
            double dx = x - o.x;
            double dy = y - o.y;
            double dz = z - o.z;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
    }

    private final List<Point> points = new ArrayList<>();
    /** 已丢掉的最早点数（绝对编号 = dropped + 下标）。 */
    private int dropped;
    private int version;

    public void add(double x, double y, double z) {
        Point p = new Point(x, y, z);
        if (points.isEmpty()) {
            points.add(p);
            return;
        }
        Point last = points.get(points.size() - 1);
        double d = last.dist(p);
        if (d < STEP) {
            return;
        }
        for (int i = points.size() - 1 - RECENT; i >= 0; i--) {
            if (points.get(i).dist(p) < BACKTRACK) {
                points.subList(i + 1, points.size()).clear();
                version++;
                return;
            }
        }
        int extra = (int) Math.ceil(d / MAX_GAP) - 1;
        for (int k = 1; k <= extra; k++) {
            double t = (double) k / (extra + 1);
            push(new Point(last.x + (p.x - last.x) * t, last.y + (p.y - last.y) * t, last.z + (p.z - last.z) * t));
        }
        push(p);
    }

    private void push(Point p) {
        points.add(p);
        if (points.size() > MAX_POINTS) {
            points.remove(0);
            dropped++;
        }
    }

    public void clear() {
        points.clear();
        dropped = 0;
        version++;
    }

    public List<Point> points() {
        return List.copyOf(points);
    }

    /** 绝对编号 ≥ n 的点（编号从开始记线算起，丢掉的最早点也占编号）。 */
    public List<Point> since(int n) {
        int from = Math.max(0, n - dropped);
        return from >= points.size() ? List.of() : List.copyOf(points.subList(from, points.size()));
    }

    /** 下一个点的绝对编号。 */
    public int end() {
        return dropped + points.size();
    }

    public int version() {
        return version;
    }

    public int size() {
        return points.size();
    }
}
