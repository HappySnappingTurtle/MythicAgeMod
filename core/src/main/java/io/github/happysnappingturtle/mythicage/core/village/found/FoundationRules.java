package io.github.happysnappingturtle.mythicage.core.village.found;

import io.github.happysnappingturtle.mythicage.core.village.gen.Box;

import java.util.List;

/**
 * 奠基石的规则（T19 设计文档 §13.1、§1.13；17 号 §3）：五文明通用。
 * <ul>
 *   <li>限额：每位玩家每个文明最多 {@link #PER_PLAYER} 个村庄，拆除宽限中的也算；</li>
 *   <li>间距：{@link #MIN_GAP} 格内不能有别的村庄的已建范围；</li>
 *   <li>拆除宽限：拆后 {@link #GRACE_DAYS} 个 MC 日内放回原处恢复，超过就注销。</li>
 * </ul>
 */
public final class FoundationRules {
    public static final int PER_PLAYER = 2;
    public static final int MIN_GAP = 32;
    public static final int GRACE_DAYS = 3;
    /** 放回「原处」的容差（格）。 */
    public static final int RESTORE_RADIUS = 2;

    private FoundationRules() {
    }

    public enum Grace { ACTIVE, RESTORABLE, EXPIRED }

    /** 还能再立一个吗：在用的 + 宽限中的 < 上限。 */
    public static boolean canFound(int active, int inGrace, int limit) {
        return active + inGrace < limit;
    }

    /** (x, z) 到包围盒的水平距离（在盒内为 0）。 */
    public static double distanceXZ(Box b, int x, int z) {
        int dx = Math.max(0, Math.max(b.minX() - x, x - b.maxX()));
        int dz = Math.max(0, Math.max(b.minZ() - z, z - b.maxZ()));
        return Math.sqrt((double) dx * dx + (double) dz * dz);
    }

    /** 别的村庄的已建范围有没有在 {@code gap} 格内。 */
    public static boolean tooClose(List<Box> others, int x, int z, int gap) {
        return others.stream().anyMatch(b -> distanceXZ(b, x, z) <= gap);
    }

    /** 拆除后的状态：{@code removedDay} 为 −1 表示没拆。 */
    public static Grace grace(long removedDay, long today) {
        if (removedDay < 0) {
            return Grace.ACTIVE;
        }
        return today - removedDay <= GRACE_DAYS - 1 ? Grace.RESTORABLE : Grace.EXPIRED;
    }
}
