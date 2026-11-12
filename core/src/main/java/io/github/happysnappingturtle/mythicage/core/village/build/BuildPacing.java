package io.github.happysnappingturtle.mythicage.core.village.build;

import com.google.gson.JsonObject;

/** GA-10: data driven timing and a server-wide, tick-scoped mutation budget. */
public final class BuildPacing {
    private BuildPacing() {}

    /**
     * @param fetchRange 仓储点离工地超过这个距离（格）就改到施工牌旁取料，免得每趟都跑远路
     */
    public record Settings(int ticksPerBlock, int catchUpTicksPerBlock, int maxBuilders, int tripSize,
                           int fetchTicks, int globalPerTick, int fetchRange) {
        public static final Settings DEFAULT = new Settings(15, 5, 3, 16, 20, 8, 32);
        public Settings {
            if (ticksPerBlock < 1 || catchUpTicksPerBlock < 1 || maxBuilders < 1 || tripSize < 1
                    || fetchTicks < 1 || globalPerTick < 1 || fetchRange < 1) {
                throw new IllegalArgumentException("Invalid construction settings");
            }
        }
        public static Settings parse(JsonObject o) {
            Settings d = DEFAULT;
            return new Settings(value(o, "ticksPerBlock", d.ticksPerBlock), value(o, "catchUpTicksPerBlock", d.catchUpTicksPerBlock),
                    value(o, "maxBuilders", d.maxBuilders), value(o, "tripSize", d.tripSize), value(o, "fetchTicks", d.fetchTicks),
                    value(o, "globalPerTick", d.globalPerTick), value(o, "fetchRange", d.fetchRange));
        }
        private static int value(JsonObject o, String key, int fallback) {
            return o.has(key) ? o.get(key).getAsInt() : fallback;
        }
    }

    public static int ticks(Settings settings, long bank, double speed) {
        if (!Double.isFinite(speed) || speed <= 0) throw new IllegalArgumentException("Invalid speed");
        return Math.max(1, (int) Math.ceil((bank > 0 ? settings.catchUpTicksPerBlock() : settings.ticksPerBlock()) / speed));
    }

    public static long spendBank(Settings settings, long bank) {
        return Math.max(0, bank - settings.catchUpTicksPerBlock());
    }

    public static long placementTicks(int blocks, int builders, int ticksPerBlock) {
        return Math.ceilDiv(blocks, builders) * (long) ticksPerBlock;
    }

    public static final class Budget {
        private long tick = Long.MIN_VALUE;
        private int used;
        public boolean available(long now, int limit) {
            if (now != tick) { tick = now; used = 0; }
            return used < limit;
        }
        public boolean take(long now, int limit) {
            if (!available(now, limit)) return false;
            used++;
            return true;
        }
    }
}
