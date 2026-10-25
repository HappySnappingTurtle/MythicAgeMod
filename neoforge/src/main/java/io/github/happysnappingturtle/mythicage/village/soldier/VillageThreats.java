package io.github.happysnappingturtle.mythicage.village.soldier;

import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 谁打过本村的居民或士兵（T20 设计文档 §5.4）：村庄 → 攻击者 → 最近一次的世界刻。只在内存里，不存档（30 秒的反击窗口、
 * 100 刻的中立生物记忆都很短，重启后清空正合适，不形成永久仇恨）。
 */
public final class VillageThreats {
    private static final Map<UUID, Map<UUID, Long>> HARMS = new ConcurrentHashMap<>();
    /** 一村最多记这么多个攻击者（防止被刷爆）。 */
    private static final int LIMIT = 64;

    private VillageThreats() {
    }

    public static void onDamagePost(LivingDamageEvent.Post e) {
        if (e.getNewDamage() <= 0 || !(e.getEntity() instanceof ResidentEntity r) || !(r.level() instanceof ServerLevel level)) {
            return;
        }
        Entity attacker = e.getSource().getEntity();
        if (attacker == null || attacker == r) {
            return;
        }
        r.village().ifPresent(v -> note(v, attacker.getUUID(), level.getGameTime()));
    }

    public static void note(UUID village, UUID attacker, long now) {
        Map<UUID, Long> m = HARMS.computeIfAbsent(village, k -> new ConcurrentHashMap<>());
        if (m.size() >= LIMIT && !m.containsKey(attacker)) {
            m.entrySet().stream().min(Map.Entry.comparingByValue()).ifPresent(x -> m.remove(x.getKey()));
        }
        m.put(attacker, now);
    }

    /** 它上一次打本村的人是多少刻前；没打过返回 -1。 */
    public static long ticksSince(UUID village, UUID attacker, long now) {
        Map<UUID, Long> m = HARMS.get(village);
        Long t = m == null ? null : m.get(attacker);
        return t == null ? -1 : Math.max(0, now - t);
    }

    /** 村庄 → 这场警戒最先被发现的威胁（独有机制用来通知全村，弱引用）。 */
    private static final Map<UUID, java.lang.ref.WeakReference<net.minecraft.world.entity.LivingEntity>> ALERT = new ConcurrentHashMap<>();

    public static void setAlertThreat(UUID village, net.minecraft.world.entity.LivingEntity threat) {
        if (threat != null) {
            ALERT.put(village, new java.lang.ref.WeakReference<>(threat));
        }
    }

    /** 这场警戒的威胁（还活着才返回）。 */
    public static java.util.Optional<net.minecraft.world.entity.LivingEntity> alertThreat(UUID village) {
        var ref = ALERT.get(village);
        var e = ref == null ? null : ref.get();
        return e != null && e.isAlive() && !e.isRemoved() ? java.util.Optional.of(e) : java.util.Optional.empty();
    }

    public static void onServerStopped(ServerStoppedEvent e) {
        HARMS.clear();
        ALERT.clear();
    }

    public static void clearForTest(UUID village) {
        HARMS.remove(village);
        ALERT.remove(village);
    }
}
