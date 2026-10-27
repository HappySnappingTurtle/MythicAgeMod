package io.github.happysnappingturtle.mythicage.player;

import io.github.happysnappingturtle.mythicage.core.player.Civilization;

import java.util.EnumMap;
import java.util.Map;

/**
 * 客户端缓存：只保存服务端同步来的数据，不做任何修改（T4 设计文档 §4）。
 * 不引用客户端专属类，服务端加载本类也不会出问题。
 */
public final class ClientMythData {
    private static volatile long dataVersion = -1;
    private static volatile Map<Civilization, PlayerSyncPayload.CivEntry> civs = Map.of();
    /** 每个文明恩宠最近一次变化的时间（System.currentTimeMillis），供 HUD 决定是否显示。 */
    private static final Map<Civilization, Long> lastChange = new EnumMap<>(Civilization.class);
    /** 每个文明的最近变化（合并 3 秒内的连续变化，T24 §5.4）。 */
    private static final Map<Civilization, io.github.happysnappingturtle.mythicage.core.player.FavorDelta> deltas =
            new EnumMap<>(Civilization.class);

    private ClientMythData() {
    }

    /** 收到同步包：丢弃比已缓存版本更旧的包。 */
    public static synchronized void accept(PlayerSyncPayload payload) {
        if (payload.dataVersion() < dataVersion) {
            return;
        }
        Map<Civilization, PlayerSyncPayload.CivEntry> next = new EnumMap<>(Civilization.class);
        long now = System.currentTimeMillis();
        boolean first = dataVersion < 0;
        for (PlayerSyncPayload.CivEntry e : payload.civs()) {
            next.put(e.civ(), e);
            PlayerSyncPayload.CivEntry old = civs.get(e.civ());
            if (!first && (old == null || old.favor() != e.favor() || old.cap() != e.cap())) {
                lastChange.put(e.civ(), now);
                long change = e.favor() - (old == null ? 0 : old.favor());
                deltas.put(e.civ(), delta(e.civ()).add(change, now, ""));
            }
        }
        civs = Map.copyOf(next);
        dataVersion = payload.dataVersion();
    }

    /** 退出世界时清空。 */
    public static synchronized void reset() {
        dataVersion = -1;
        civs = Map.of();
        lastChange.clear();
        deltas.clear();
    }

    /** 来源包（与数值包先后不定）。 */
    public static synchronized void source(Civilization c, String key) {
        deltas.put(c, delta(c).withSource(key, System.currentTimeMillis()));
        if (delta(c).delta() == 0) {
            // 来源先到：先记一个 0 变化的来源，数值到了再合并
            deltas.put(c, new io.github.happysnappingturtle.mythicage.core.player.FavorDelta(0, System.currentTimeMillis(), key));
        }
    }

    public static synchronized io.github.happysnappingturtle.mythicage.core.player.FavorDelta delta(Civilization c) {
        return deltas.getOrDefault(c, io.github.happysnappingturtle.mythicage.core.player.FavorDelta.NONE);
    }

    public static long dataVersion() {
        return dataVersion;
    }

    public static PlayerSyncPayload.CivEntry civ(Civilization c) {
        return civs.get(c);
    }

    public static synchronized long lastChangeMillis(Civilization c) {
        return lastChange.getOrDefault(c, 0L);
    }
}
