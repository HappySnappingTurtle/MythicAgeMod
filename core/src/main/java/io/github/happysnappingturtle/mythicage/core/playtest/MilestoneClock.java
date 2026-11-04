package io.github.happysnappingturtle.mythicage.core.playtest;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * 一个玩家的站点用时与里程碑去重（GA-3 设计文档 §4.6）。纯计算：时间由调用方传入。
 */
public final class MilestoneClock {
    /**
     * 结束的一站。
     *
     * @param switched 没有 {@code done}，因为进了下一站而结束
     */
    public record Ended(String station, long seconds, long ticks, boolean switched) {
    }

    private String station;
    private long startMillis;
    private long startTick;
    private final Set<String> seen = new HashSet<>();

    /** 开始一站；上一站还没结束时先把它记为「切换」结束。 */
    public Optional<Ended> start(String id, long nowMillis, long gameTick) {
        Optional<Ended> prev = end(nowMillis, gameTick, true);
        station = id;
        startMillis = nowMillis;
        startTick = gameTick;
        seen.clear();
        return prev;
    }

    /** {@code playtest done}；没有进行中的站时为空。 */
    public Optional<Ended> done(long nowMillis, long gameTick) {
        return end(nowMillis, gameTick, false);
    }

    /** 开始准备下一站时先结束当前站（记为「切换」），准备期间发生的事件不算里程碑。 */
    public Optional<Ended> interrupt(long nowMillis, long gameTick) {
        return end(nowMillis, gameTick, true);
    }

    private Optional<Ended> end(long nowMillis, long gameTick, boolean switched) {
        if (station == null) {
            return Optional.empty();
        }
        Ended e = new Ended(station, Math.max(0, nowMillis - startMillis) / 1000, Math.max(0, gameTick - startTick), switched);
        station = null;
        seen.clear();
        return Optional.of(e);
    }

    /** 本站第一次出现这个里程碑时为 true；没在站里时一律 false（不记）。 */
    public boolean milestone(String key) {
        return station != null && seen.add(key);
    }

    public Optional<String> station() {
        return Optional.ofNullable(station);
    }

    /** 59 → 「59 秒」，612 → 「10 分 12 秒」，3700 → 「1 时 1 分 40 秒」。 */
    public static String format(long seconds) {
        long s = Math.max(0, seconds);
        long h = s / 3600;
        long m = s % 3600 / 60;
        long sec = s % 60;
        if (h > 0) {
            return h + " 时 " + m + " 分 " + sec + " 秒";
        }
        if (m > 0) {
            return m + " 分 " + sec + " 秒";
        }
        return sec + " 秒";
    }
}
