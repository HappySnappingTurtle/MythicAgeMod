package io.github.happysnappingturtle.mythicage.core.greek.prayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 信徒的纯判定（T10 设计文档 §4、§6、§7）：神坛选择顺序、同日一主、朝圣距离、补货上限。
 */
public final class BelieverRules {
    /** 朝圣距离：家到祈祷位的水平距离不小于它才计入。 */
    public static final double PILGRIMAGE_MIN = 8;
    /** 村庄范围外兜底神坛的最大水平距离（从村民量起）。 */
    public static final double FALLBACK_RANGE = 48;
    /** 原版每日补货上限与两次补货的最小间隔。 */
    public static final int VANILLA_RESTOCKS = 2;
    public static final long RESTOCK_GAP_TICKS = 2400;

    private BelieverRules() {
    }

    /**
     * 一座候选神坛。
     *
     * @param inVillage 在信徒所属村庄的范围内
     * @param distance  到信徒的水平距离
     */
    public record Candidate<T>(T altar, UUID owner, boolean inVillage, double distance) {
    }

    /**
     * 候选神坛的尝试顺序：村庄范围内有神坛时只考虑范围内的，按距离从近到远（T18 之前不看贡献）；
     * 范围内一座都没有时，才考虑范围外 {@link #FALLBACK_RANGE} 格内的，同样由近到远。
     * 调用方按顺序逐个尝试（同日一主、祈祷位是否空闲）。
     */
    public static <T> List<Candidate<T>> order(List<Candidate<T>> candidates) {
        boolean anyInside = candidates.stream().anyMatch(Candidate::inVillage);
        List<Candidate<T>> out = new ArrayList<>();
        for (Candidate<T> c : candidates) {
            if (anyInside ? c.inVillage() : c.distance() <= FALLBACK_RANGE) {
                out.add(c);
            }
        }
        out.sort(Comparator.comparingDouble(Candidate::distance));
        return out;
    }

    /** 同一个村民一天只为一位主人祈祷：今天还没计入过，或者计入的就是这位主人。 */
    public static boolean canPrayFor(long lastPrayDay, UUID lastOwner, long today, UUID owner) {
        return lastPrayDay != today || Objects.equals(lastOwner, owner);
    }

    /** 朝圣距离：家到祈祷位的水平距离 ≥ 8 格。 */
    public static boolean pilgrimageOk(double homeX, double homeZ, double slotX, double slotZ) {
        double dx = homeX - slotX;
        double dz = homeZ - slotZ;
        return dx * dx + dz * dz >= PILGRIMAGE_MIN * PILGRIMAGE_MIN;
    }

    /** 今天的补货次数上限：当天祈祷过的有职业村民用配置值（默认 1），否则原版的 2。 */
    public static int restockLimit(boolean prayedToday, int configuredLimit) {
        return prayedToday ? Math.max(0, Math.min(VANILLA_RESTOCKS, configuredLimit)) : VANILLA_RESTOCKS;
    }

    /**
     * 是否允许再补货一次。上限为 2 时与原版 {@code Villager#allowedToRestock} 完全一致：
     * 当天第一次随时可补，之后要距上次补货超过 2400 刻。
     */
    public static boolean restockAllowed(int restocksToday, int limit, long gameTime, long lastRestockGameTime) {
        return restocksToday < limit
                && (restocksToday == 0 || gameTime > lastRestockGameTime + RESTOCK_GAP_TICKS);
    }
}
