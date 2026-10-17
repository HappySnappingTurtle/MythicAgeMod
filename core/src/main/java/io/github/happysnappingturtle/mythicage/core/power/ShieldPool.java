package io.github.happysnappingturtle.mythicage.core.power;

import java.util.ArrayList;
import java.util.List;

/**
 * 神力护盾（05 号 §1.2；T12 设计文档 §2.6）：大型神力生效期间，原点水平半径 r 内不能再放任何大型神力；
 * 五个文明合为一个护盾池。到期的护盾自动失效。
 */
public final class ShieldPool {
    /** @param dim 维度 id；@param until 到期时刻（世界时钟刻） */
    public record Shield(String dim, double x, double z, double radius, long until) {
        boolean covers(String d, double px, double pz) {
            double dx = px - x;
            double dz = pz - z;
            return dim.equals(d) && dx * dx + dz * dz <= radius * radius;
        }
    }

    private final List<Shield> shields = new ArrayList<>();

    public synchronized void add(Shield s) {
        shields.add(s);
    }

    /** 这个点还要等多少刻才能放大型神力；0 = 可以。重叠时取最长的。 */
    public synchronized long remaining(String dim, double x, double z, long now) {
        shields.removeIf(s -> s.until() <= now);
        long best = 0;
        for (Shield s : shields) {
            if (s.covers(dim, x, z)) {
                best = Math.max(best, s.until() - now);
            }
        }
        return best;
    }

    public synchronized int size() {
        return shields.size();
    }

    public synchronized void clear() {
        shields.clear();
    }
}
