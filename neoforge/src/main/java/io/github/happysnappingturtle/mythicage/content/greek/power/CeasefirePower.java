package io.github.happysnappingturtle.mythicage.content.greek.power;

import io.github.happysnappingturtle.mythicage.boss.TrialArenas;
import io.github.happysnappingturtle.mythicage.boss.TrialBoss;
import io.github.happysnappingturtle.mythicage.combat.DamagePipeline;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.combat.DamageZones;
import io.github.happysnappingturtle.mythicage.core.power.CeasefireRules;
import io.github.happysnappingturtle.mythicage.core.power.CeasefireRules.Party;
import io.github.happysnappingturtle.mythicage.power.CastContext;
import io.github.happysnappingturtle.mythicage.power.GodPower;
import io.github.happysnappingturtle.mythicage.power.PowerFx;
import io.github.happysnappingturtle.mythicage.power.PowerFxPayload;
import io.github.happysnappingturtle.mythicage.power.PowerZones;
import io.github.happysnappingturtle.mythicage.power.TargetFactsCollector;
import io.github.happysnappingturtle.mythicage.runtime.WorldClock;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 停火 GP-GR-05（赫尔墨斯，古典；T25a §2）：以施法者为中心、固定在施放点（D1）的 64 格休战区，30 秒。
 * <ul>
 *   <li>伤害：只要攻击方或受害方有一方按规则在区内（D18）就拦（T25.1 {@link DamagePipeline} 的判定，不进乘区）；环境伤害照常。</li>
 *   <li>破约：玩家挥砍、区内射出投射物、施放伤人的神力 → 涉及的休战区立即结束，这一击照常结算；聊天栏提示破约者。</li>
 *   <li>敌对生物失去目标、区内不刷敌对生物（D4，刷怪笼与试炼刷怪器一起停）；施法者移速 +30%（D2）。</li>
 *   <li>Boss：同一场战斗第一次施放停手 8 秒，到期后 Boss 与它的战斗参与者按「区外」算；第二次起不受影响（D3）。</li>
 * </ul>
 * 区域本身存在 {@link PowerZones}（重启续上）；Boss 停手与破约标记只在内存里（8 秒、一刻的事）。
 */
public final class CeasefirePower implements GodPower {
    static final String KIND = "ceasefire";
    static final ResourceLocation SPEED_ID = ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "ceasefire_speed");

    /** Boss UUID → 停手到哪一刻。 */
    private static final Map<UUID, Long> HELD = new ConcurrentHashMap<>();
    /** 一场 Boss 战（D3：竞技场场次 / 原版 Boss 的 UUID）→ 这一场被施放过几次。 */
    private static final Map<String, Integer> CASTS = new ConcurrentHashMap<>();
    /** 破约结束的区域（结束表现用碎裂而不是淡出）。 */
    private static final Set<String> BROKEN = ConcurrentHashMap.newKeySet();
    /** 施法者（按 castId）：施放时记下，玩家列表里查不到的（GameTest 的假玩家）也能加、撤移速。 */
    private static final Map<String, ServerPlayer> CASTERS = new ConcurrentHashMap<>();
    /** 只供 GameTest：每个区域怎么结束的（break / fade）。 */
    private static final Map<String, String> ENDED = new ConcurrentHashMap<>();

    @Override
    public String id() {
        return GreekPowers.CEASEFIRE;
    }

    @Override
    public void execute(CastContext ctx) {
        ServerLevel level = ctx.level();
        long now = WorldClock.now(level.getServer());
        int duration = ctx.spec().durationTicks();
        Vec3 at = ctx.caster().position();
        PowerZones.Zone zone = new PowerZones.Zone(ctx.castId(), KIND, ctx.spec().id(), level.dimension(), at, ctx.spec().radius(),
                now + duration, ctx.caster().getUUID(), List.of());
        PowerZones.add(level.getServer(), zone);
        CASTERS.put(ctx.castId(), ctx.caster());
        GreekPowers.play(level, at, "gp.gr.ceasefire.begin", 1.4f, 1f);
        PowerFx.emit(ctx, PowerFxPayload.Phase.HIT, List.of(), at, duration, "zone:" + zone.radius());
        for (LivingEntity e : inside(level, zone)) {
            if (TargetFactsCollector.isBoss(e)) {
                String key = fightKey(level, e);
                int casts = CASTS.getOrDefault(key, 0);
                CASTS.put(key, casts + 1);
                int held = CeasefireRules.bossTicks(casts);
                if (held > 0) {
                    HELD.put(e.getUUID(), now + held);
                }
            }
        }
        disengage(level, zone);
        speed(level, zone, now);
    }

    // ───────────── 处境 ─────────────

    static List<LivingEntity> inside(ServerLevel level, PowerZones.Zone zone) {
        double r = zone.radius();
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(zone.center(), zone.center()).inflate(r, 16, r),
                x -> x.isAlive() && zone.covers(x.position()));
    }

    /** 这一处有哪些仍有效的休战区。 */
    static List<PowerZones.Zone> zonesAt(ServerLevel level, Vec3 p) {
        return PowerZones.at(level, p, KIND);
    }

    /** D3：竞技场 Boss 按「竞技场 id + 场次」，原版 Boss 按实体 UUID。 */
    static String fightKey(ServerLevel level, LivingEntity boss) {
        if (boss instanceof TrialBoss tb) {
            var arena = TrialArenas.get(level.getServer()).byId(tb.arenaId());
            if (arena.isPresent()) {
                return "arena:" + arena.get().id() + "#" + arena.get().fightId();
            }
        }
        return "uuid:" + boss.getUUID();
    }

    static boolean held(LivingEntity boss, long now) {
        Long until = HELD.get(boss.getUUID());
        return until != null && CeasefireRules.bossHeld(now, until);
    }

    /** 被放出休战：停手到期（或从没被停手）的 Boss；在这样一个 Boss 的竞技场大厅里的玩家。 */
    static boolean released(ServerLevel level, Entity e, long now) {
        if (e instanceof LivingEntity le && TargetFactsCollector.isBoss(le)) {
            return !held(le, now);
        }
        if (e instanceof Player) {
            return TrialArenas.get(level.getServer()).hallOf(e).flatMap(a -> a.boss(level)).map(b -> !held(b, now)).orElse(false);
        }
        return false;
    }

    static Party party(ServerLevel level, Entity e, long now) {
        if (e == null) {
            return Party.OUTSIDE;
        }
        boolean in = !zonesAt(level, e.position()).isEmpty();
        if (e instanceof LivingEntity le && TargetFactsCollector.isBoss(le)) {
            return held(le, now) ? new Party(in, false) : Party.freeBoss(in);
        }
        return new Party(in, in && released(level, e, now));
    }

    // ───────────── 伤害判定（T25.1 Gate） ─────────────

    /**
     * 拦不拦这次伤害。神力伤害（雷击……）发起方或目标在区内 → 破约，这一击照常（「施放伤人的神力」算攻击动作）。
     * 没有攻击者的伤害（岩浆、摔落、溺水、饥饿）照常。
     */
    static boolean gate(DamagePipeline.Hit hit) {
        if (!(hit.victim().level() instanceof ServerLevel level) || PowerZones.all(level.getServer()).isEmpty()) {
            return false;
        }
        Entity attacker = hit.attacker();
        boolean environmental = attacker == null;
        long now = WorldClock.now(level.getServer());
        Party a = party(level, attacker, now);
        Party v = party(level, hit.victim(), now);
        if (!CeasefireRules.blocks(a, v, environmental)) {
            return false;
        }
        if (hit.kind() == DamageZones.Source.POWER) {
            breach(level, attacker, hit.victim());
            return false;
        }
        return true;
    }

    // ───────────── 破约 ─────────────

    /** 发起方 attacker 对 target（可以为空）发起了攻击动作：涉及的休战区按规则结束。返回是否破约。 */
    static boolean breach(ServerLevel level, Entity attacker, Entity target) {
        long now = WorldClock.now(level.getServer());
        Party a = party(level, attacker, now);
        Party t = party(level, target, now);
        if (!CeasefireRules.breaches(a, t)) {
            return false;
        }
        List<PowerZones.Zone> hit = new ArrayList<>(zonesAt(level, attacker.position()));
        if (target != null) {
            for (PowerZones.Zone z : zonesAt(level, target.position())) {
                if (hit.stream().noneMatch(x -> x.castId().equals(z.castId()))) {
                    hit.add(z);
                }
            }
        }
        Entity who = attacker instanceof Projectile p && p.getOwner() != null ? p.getOwner() : attacker;
        for (PowerZones.Zone z : hit) {
            BROKEN.add(z.castId());
            for (Player p : level.players()) {
                if (z.covers(p.position())) {
                    p.displayClientMessage(Component.translatable("mythic_age.power.ceasefire.broken", who.getDisplayName()), false);
                }
            }
            PowerZones.end(level.getServer(), z);
        }
        return !hit.isEmpty();
    }

    /** 玩家挥砍。 */
    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntity().level() instanceof ServerLevel level && !PowerZones.all(level.getServer()).isEmpty()) {
            breach(level, event.getEntity(), event.getTarget());
        }
    }

    /** 区内射出投射物（读档加载的不算；末影珍珠、钓钩、经验瓶不算攻击）。 */
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() || !(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof Projectile p)
                || p instanceof ThrownEnderpearl || p instanceof FishingHook || p instanceof ThrownExperienceBottle
                || p instanceof Harmless || PowerZones.all(level.getServer()).isEmpty()) {
            return;
        }
        if (p.getOwner() instanceof LivingEntity owner) {
            breach(level, owner, null);
        }
    }

    /** 不算攻击动作的投射物（如马洛涅烈酒）。 */
    public interface Harmless {
    }

    // ───────────── 失去目标、不生成 ─────────────

    /** 区内（按规则）的生物不能锁定目标，也不能锁定区内的目标。 */
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() == null || !(event.getEntity().level() instanceof ServerLevel level)
                || PowerZones.all(level.getServer()).isEmpty()) {
            return;
        }
        long now = WorldClock.now(level.getServer());
        if (party(level, event.getEntity(), now).shielded() || party(level, event.getNewAboutToBeSetTarget(), now).shielded()) {
            event.setCanceled(true);
        }
    }

    /** 清掉区内生物当前的目标（施放时、每 20 刻）。 */
    static void disengage(ServerLevel level, PowerZones.Zone zone) {
        long now = WorldClock.now(level.getServer());
        for (LivingEntity e : inside(level, zone)) {
            if (e instanceof Mob m && party(level, m, now).shielded()) {
                if (m.getTarget() != null) {
                    m.setTarget(null);
                }
                if (m.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) {
                    m.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
                }
            }
        }
    }

    /** D4：区内敌对生物的自然、巡逻、刷怪笼、试炼刷怪器生成判失败；繁殖、命令、刷怪蛋放行。 */
    public static void onSpawnCheck(MobSpawnEvent.PositionCheck event) {
        if (event.getLevel() instanceof ServerLevel sl
                && CeasefireRules.blocksSpawn(event.getEntity().getType().getCategory() == MobCategory.MONSTER, event.getSpawnType().name())
                && !zonesAt(sl, new Vec3(event.getX(), event.getY(), event.getZ())).isEmpty()) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }

    // ───────────── 施法者移速（D2） ─────────────

    /** 区域的施法者：在线玩家优先，其次施放时记下的（重启后只认在线玩家）。 */
    static ServerPlayer caster(MinecraftServer server, PowerZones.Zone zone) {
        ServerPlayer online = server.getPlayerList().getPlayer(zone.owner());
        return online != null ? online : CASTERS.get(zone.castId());
    }

    static void speed(ServerLevel level, PowerZones.Zone zone, long now) {
        ServerPlayer caster = caster(level.getServer(), zone);
        if (caster == null || caster.isRemoved()) {
            return;
        }
        AttributeInstance a = caster.getAttribute(Attributes.MOVEMENT_SPEED);
        if (a == null) {
            return;
        }
        boolean on = caster.level() == level && CeasefireRules.casterBuff(zone.until() > now, zone.covers(caster.position()));
        if (on && !a.hasModifier(SPEED_ID)) {
            a.addTransientModifier(new AttributeModifier(SPEED_ID, CeasefireRules.CASTER_SPEED,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        } else if (!on) {
            a.removeModifier(SPEED_ID);
        }
    }

    static void clearSpeed(MinecraftServer server, PowerZones.Zone zone) {
        ServerPlayer caster = caster(server, zone);
        CASTERS.remove(zone.castId());
        if (caster != null && caster.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            caster.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(SPEED_ID);
        }
    }

    // ───────────── 区域行为 ─────────────

    static final PowerZones.Kind ZONE = new PowerZones.Kind() {
        @Override
        public void tick(ServerLevel level, PowerZones.Zone zone, long now) {
            disengage(level, zone);
            speed(level, zone, now);
            // Boss 停手到期：放出（播一声）
            HELD.entrySet().removeIf(e -> {
                if (now < e.getValue()) {
                    return false;
                }
                Entity boss = level.getEntity(e.getKey());
                if (boss != null && zone.covers(boss.position())) {
                    GreekPowers.play(level, boss.position(), "gp.gr.ceasefire.boss_release", 1f, 1f);
                }
                return true;
            });
        }

        @Override
        public void end(ServerLevel level, PowerZones.Zone zone) {
            clearSpeed(level.getServer(), zone);
            boolean broken = BROKEN.remove(zone.castId());
            if (ENDED.size() > 64) {
                ENDED.clear();
            }
            ENDED.put(zone.castId(), broken ? "break" : "fade");
            GreekPowers.play(level, zone.center(), broken ? "gp.gr.ceasefire.break" : "gp.gr.ceasefire.end", 1.2f, 1f);
            var p = new PowerFxPayload(zone.castId(), zone.powerId(), PowerFxPayload.Phase.END, -1, level.dimension().location(),
                    zone.center(), List.of(), WorldClock.now(level.getServer()), 0, 0, "", broken ? "break" : "fade");
            for (ServerPlayer to : PowerFx.recipients(level, null, zone.center())) {
                PowerFx.sendRaw(to, p);
            }
        }
    };

    /** Boss 死了：这一场的计数与停手清掉（竞技场复原后场次 id 会变，旧键自然作废）。 */
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity e = event.getEntity();
        if (e.level() instanceof ServerLevel level && TargetFactsCollector.isBoss(e)) {
            HELD.remove(e.getUUID());
            CASTS.remove(fightKey(level, e));
        }
    }

    public static void onServerStopped() {
        HELD.clear();
        CASTS.clear();
        BROKEN.clear();
        ENDED.clear();
        CASTERS.clear();
    }

    // ───────────── 只供 GameTest ─────────────

    public static boolean heldForTest(LivingEntity boss, ServerLevel level) {
        return held(boss, WorldClock.now(level.getServer()));
    }

    public static java.util.Optional<String> endedForTest(String castId) {
        return java.util.Optional.ofNullable(ENDED.get(castId));
    }

    public static boolean activeAt(ServerLevel level, Vec3 p) {
        return !zonesAt(level, p).isEmpty();
    }
}
