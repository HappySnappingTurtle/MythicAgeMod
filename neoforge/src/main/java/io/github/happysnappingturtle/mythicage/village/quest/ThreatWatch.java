package io.github.happysnappingturtle.mythicage.village.quest;

import io.github.happysnappingturtle.mythicage.conduct.Responsibility;
import io.github.happysnappingturtle.mythicage.core.village.quest.RescueRules;
import io.github.happysnappingturtle.mythicage.power.OwnedSummon;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 盯住居民的敌对生物（T21 设计文档 §7.3、§8）：敌对生物把本村居民（不含在岗士兵）当目标、或伤了居民时开始记，
 * 记下它的来历（玩家刷出来的、有主人、之前打过它的玩家）、它以居民为目标期间受的伤害（按责任人分）、谁打出致死一击。
 * 救援（{@link RescueQuests}）与护送（{@link EscortQuests}）读这里的记录判定。
 *
 * <p>只在内存里：警戒、护送都只有几分钟，重启后清空（进行中的救援在警戒结束时判「没人救下」，护送丢掉这段的危险记录）。
 */
public final class ThreatWatch {
    /** 生成时打的标记：玩家造成的危险（§8.3）。 */
    public static final String PLAYER_MADE = "mythic_age:player_made";
    /** 「之前打过它」「伤过这名居民」往前看多久（刻）。 */
    static final long WINDOW = 600;
    /** 各张表的上限（防刷爆）。 */
    private static final int LIMIT = 512;
    private static final Set<MobSpawnType> PLAYER_SPAWNS = EnumSet.of(MobSpawnType.SPAWN_EGG, MobSpawnType.COMMAND,
            MobSpawnType.DISPENSER, MobSpawnType.SPAWNER, MobSpawnType.TRIAL_SPAWNER, MobSpawnType.BUCKET);

    /** 一只威胁。 */
    public static final class Threat {
        public final UUID id;
        public final long since;
        final boolean playerMade;
        final boolean owned;
        final Set<UUID> provokers;
        public final Map<UUID, Double> byPlayer = new LinkedHashMap<>();
        public double total;
        public boolean dead;
        public Optional<UUID> killer = Optional.empty();
        public long deadAt;
        /** 最近一次看到它还以居民为目标的刻。 */
        public long lastSeen;
        /** 救援已经按它判过谁救下。 */
        boolean judged;
        /** 护送已经按它判过危险。 */
        boolean escortSeen;

        Threat(UUID id, long since, boolean playerMade, boolean owned, Set<UUID> provokers) {
            this.id = id;
            this.since = since;
            this.lastSeen = since;
            this.playerMade = playerMade;
            this.owned = owned;
            this.provokers = Set.copyOf(provokers);
        }
    }

    /** 一名居民身上的威胁。 */
    public static final class Watch {
        public final UUID ward;
        public final UUID village;
        public final Map<UUID, Threat> threats = new LinkedHashMap<>();

        Watch(UUID ward, UUID village) {
            this.ward = ward;
            this.village = village;
        }
    }

    private static final Map<UUID, Watch> WATCHES = new ConcurrentHashMap<>();
    /** 敌对生物 → 玩家 → 最近一次被他打的刻（挑衅）。 */
    private static final Map<UUID, Map<UUID, Long>> HITS = new ConcurrentHashMap<>();
    /** 居民 → 玩家 → 最近一次被他伤的刻。 */
    private static final Map<UUID, Map<UUID, Long>> WARD_HARM = new ConcurrentHashMap<>();

    private ThreatWatch() {
    }

    // ───────────── 事件 ─────────────

    /** 生成方式是刷怪蛋、命令、发射器、刷怪笼、试炼刷怪笼、桶：打上「玩家造成」（D8：原版地牢的刷怪笼也算）。 */
    public static void onFinalizeSpawn(FinalizeSpawnEvent e) {
        if (PLAYER_SPAWNS.contains(e.getSpawnType())) {
            markPlayerMade(e.getEntity());
        }
    }

    public static void onChangeTarget(LivingChangeTargetEvent e) {
        if (e.isCanceled() || !(e.getEntity() instanceof Mob mob) || !(mob instanceof Enemy)
                || !(mob.level() instanceof ServerLevel level)) {
            return;
        }
        if (e.getNewAboutToBeSetTarget() instanceof ResidentEntity r) {
            watch(r, mob, level.getGameTime());
        }
    }

    public static void onDamagePost(LivingDamageEvent.Post e) {
        LivingEntity victim = e.getEntity();
        float dmg = e.getNewDamage();
        if (dmg <= 0 || !(victim.level() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        Responsibility.Blame blame = Responsibility.of(e.getSource());
        if (victim instanceof ResidentEntity r) {
            blame.player().ifPresent(p -> remember(WARD_HARM, r.getUUID(), p, now));
            if (e.getSource().getEntity() instanceof Mob mob && mob instanceof Enemy) {
                watch(r, mob, now);
            }
            return;
        }
        if (victim instanceof Enemy) {
            for (Watch w : WATCHES.values()) {
                Threat t = w.threats.get(victim.getUUID());
                if (t != null && !t.dead) {
                    t.total += dmg;
                    blame.player().ifPresent(p -> t.byPlayer.merge(p, (double) dmg, Double::sum));
                }
            }
            // 记在后面：这一下是在它被盯上之后打的，不算挑衅
            blame.player().ifPresent(p -> remember(HITS, victim.getUUID(), p, now));
        }
    }

    public static void onDeath(LivingDeathEvent e) {
        LivingEntity victim = e.getEntity();
        if (e.isCanceled() || victim.level().isClientSide() || !(victim instanceof Enemy)) {
            return;
        }
        Optional<UUID> killer = Responsibility.of(e.getSource()).player();
        for (Watch w : WATCHES.values()) {
            Threat t = w.threats.get(victim.getUUID());
            if (t != null) {
                t.dead = true;
                t.killer = killer;
                t.deadAt = victim.level().getGameTime();
            }
        }
        HITS.remove(victim.getUUID());
    }

    public static void onServerStopped(ServerStoppedEvent e) {
        WATCHES.clear();
        HITS.clear();
        WARD_HARM.clear();
    }

    // ───────────── 记录 ─────────────

    static void watch(ResidentEntity r, Mob mob, long now) {
        if (r.defending() || r.village().isEmpty()) {
            return;
        }
        if (WATCHES.size() >= LIMIT && !WATCHES.containsKey(r.getUUID())) {
            return;
        }
        Watch w = WATCHES.computeIfAbsent(r.getUUID(), k -> new Watch(k, r.village().get()));
        Threat t = w.threats.get(mob.getUUID());
        if (t != null) {
            t.lastSeen = now;
            return;
        }
        Set<UUID> provokers = new LinkedHashSet<>();
        Map<UUID, Long> hits = HITS.get(mob.getUUID());
        if (hits != null) {
            hits.forEach((p, at) -> {
                if (now - at <= WINDOW) {
                    provokers.add(p);
                }
            });
        }
        w.threats.put(mob.getUUID(), new Threat(mob.getUUID(), now, playerMade(mob), owned(mob), provokers));
    }

    private static void remember(Map<UUID, Map<UUID, Long>> table, UUID key, UUID player, long now) {
        if (table.size() >= LIMIT && !table.containsKey(key)) {
            table.entrySet().removeIf(x -> x.getValue().values().stream().allMatch(t -> now - t > WINDOW));
            if (table.size() >= LIMIT) {
                return;
            }
        }
        table.computeIfAbsent(key, k -> new ConcurrentHashMap<>()).put(player, now);
    }

    static boolean playerMade(Entity e) {
        return e.getPersistentData().getBoolean(PLAYER_MADE);
    }

    static boolean owned(Entity e) {
        return e instanceof OwnedSummon s && s.summonOwner() != null || e instanceof OwnableEntity o && o.getOwnerUUID() != null;
    }

    public static void markPlayerMade(Entity e) {
        e.getPersistentData().putBoolean(PLAYER_MADE, true);
    }

    /** 这只威胁对救援者 / 护送者的危险来历（§8.3）：伤过这名居民的玩家从它被盯上前 {@value #WINDOW} 刻算起。 */
    static RescueRules.Danger danger(Watch w, Threat t) {
        Set<UUID> harmers = new LinkedHashSet<>();
        Map<UUID, Long> harm = WARD_HARM.get(w.ward);
        if (harm != null) {
            harm.forEach((p, at) -> {
                if (at >= t.since - WINDOW) {
                    harmers.add(p);
                }
            });
        }
        return new RescueRules.Danger(t.playerMade, t.owned, t.provokers, harmers);
    }

    // ───────────── 查询 ─────────────

    static Optional<Watch> of(UUID ward) {
        return Optional.ofNullable(WATCHES.get(ward));
    }

    static List<Watch> inVillage(UUID village) {
        return WATCHES.values().stream().filter(w -> w.village.equals(village)).toList();
    }

    static void forget(UUID ward) {
        WATCHES.remove(ward);
    }

    /** 清掉没有任务在用、威胁也都不在了的记录（死了、不见了、{@value #WINDOW} 刻没再盯着居民）。 */
    static void prune(ServerLevel level, UUID village, Set<UUID> inUse, long now) {
        for (Watch w : inVillage(village)) {
            if (inUse.contains(w.ward)) {
                continue;
            }
            w.threats.values().removeIf(t -> {
                Entity e = level.getEntity(t.id);
                return t.dead || e == null || !e.isAlive() || now - t.lastSeen > WINDOW;
            });
            if (w.threats.isEmpty()) {
                WATCHES.remove(w.ward);
            }
        }
    }

    /** 只供 GameTest。 */
    public static Optional<Watch> watchForTest(UUID ward) {
        return of(ward);
    }

    public static void clearForTest(UUID village) {
        WATCHES.values().removeIf(w -> w.village.equals(village));
    }
}
