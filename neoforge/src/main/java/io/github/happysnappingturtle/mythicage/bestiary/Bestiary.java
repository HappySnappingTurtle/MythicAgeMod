package io.github.happysnappingturtle.mythicage.bestiary;

import io.github.happysnappingturtle.mythicage.advancement.MythEvents;
import io.github.happysnappingturtle.mythicage.conduct.Responsibility;
import io.github.happysnappingturtle.mythicage.core.bestiary.BestiaryRules;
import io.github.happysnappingturtle.mythicage.core.bestiary.BestiaryRules.Tier;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.player.OnlinePlayers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 图鉴点亮（T24 §6.3，D3）：
 * <ul>
 *   <li>目击：每 20 刻对每名玩家抽查 24 格内、视线可见的图鉴生物，最多 8 只；</li>
 *   <li>交战：玩家（含召唤单位、神力，按 {@link Responsibility} 归属）伤到它，或它伤到玩家；</li>
 *   <li>击败 / 驯服：击杀归属于玩家；试炼 Boss 按参战者（{@code TrialRewards}）；召唤出天马等召唤单位算驯服。</li>
 * </ul>
 * 每到一级授予隐藏进度 {@code mythic_age:bestiary/<条目>/<级别>}（事件 {@code bestiary:<条目>}，值为级别序号），手札按进度锁页。
 */
public final class Bestiary {
    public static final int CHECK_INTERVAL = 20;
    public static final String EVENT_PREFIX = "bestiary:";

    private Bestiary() {
    }

    public static Optional<String> entryOf(Entity e) {
        return BestiaryLoader.table().entryOf(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString());
    }

    /** 记一次：返回是否到了新的级别。 */
    public static boolean record(ServerPlayer p, Entity subject, Tier tier) {
        return entryOf(subject).map(entry -> record(p, entry, tier)).orElse(false);
    }

    public static boolean record(ServerPlayer p, String entry, Tier tier) {
        if (p.isSpectator()) {
            return false;
        }
        List<Tier> fresh = BestiaryRules.raise(MythData.get(p).discoveries(), entry, tier);
        if (fresh.isEmpty()) {
            return false;
        }
        MythData.changed(p);
        fire(p, entry);
        return true;
    }

    /** 按已到的最高级触发一次进度（进度条件是「至少到这一级」）。 */
    static void fire(ServerPlayer p, String entry) {
        Tier t = BestiaryRules.tier(MythData.get(p).discoveries(), entry);
        if (t != Tier.NONE) {
            MythEvents.fire(p, EVENT_PREFIX + entry, civOf(entry), t.ordinal());
        }
    }

    static Civilization civOf(String entry) {
        return Civilization.byId(entry.substring(0, entry.indexOf('/'))).orElse(Civilization.GREEK);
    }

    // ───────────── 事件 ─────────────

    public static void onServerTickPost(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        if (server.getTickCount() % CHECK_INTERVAL != 0 || BestiaryLoader.table().entries().isEmpty()) {
            return;
        }
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            sight(p);
        }
    }

    /** 抽查一次目击（GameTest 直接调用）。返回这次新点亮的条目数。 */
    public static int sight(ServerPlayer p) {
        if (p.isSpectator()) {
            return 0;
        }
        double r = BestiaryRules.SIGHT_RANGE;
        List<LivingEntity> near = p.level().getEntitiesOfClass(LivingEntity.class, new AABB(p.blockPosition()).inflate(r),
                le -> le != p && le.isAlive() && entryOf(le).isPresent());
        int n = 0;
        for (LivingEntity le : BestiaryRules.nearest(near, le -> le.distanceToSqr(p))) {
            if (p.hasLineOfSight(le) && record(p, le, Tier.SIGHTED)) {
                n++;
            }
        }
        return n;
    }

    public static void onDamagePost(LivingDamageEvent.Post e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide() || e.getNewDamage() <= 0) {
            return;
        }
        if (victim instanceof ServerPlayer p) {
            Entity attacker = e.getSource().getEntity();
            if (attacker != null) {
                record(p, attacker, Tier.ENGAGED);
            }
            return;
        }
        if (entryOf(victim).isEmpty()) {
            return;
        }
        blamed(victim, e.getSource()).ifPresent(p -> record(p, victim, Tier.ENGAGED));
    }

    public static void onDeath(LivingDeathEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide() || entryOf(victim).isEmpty()) {
            return;
        }
        blamed(victim, e.getSource()).ifPresent(p -> record(p, victim, Tier.DEFEATED));
    }

    private static Optional<ServerPlayer> blamed(LivingEntity victim, net.minecraft.world.damagesource.DamageSource source) {
        Optional<UUID> who = Responsibility.of(source).player();
        return who.map(id -> OnlinePlayers.find(victim.getServer(), id));
    }

    /** 登录时把已点亮的级别再触发一遍（进度表更新、换存档后保持一致）。 */
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            for (String entry : BestiaryLoader.table().entries()) {
                fire(p, entry);
            }
        }
    }
}
