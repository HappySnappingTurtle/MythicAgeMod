package io.github.happysnappingturtle.mythicage.village.soldier;

import io.github.happysnappingturtle.mythicage.core.village.soldier.AttackTimeline;
import io.github.happysnappingturtle.mythicage.core.village.soldier.DutyRules;
import io.github.happysnappingturtle.mythicage.core.village.soldier.InterceptRules;
import io.github.happysnappingturtle.mythicage.core.village.soldier.ShieldRules;
import io.github.happysnappingturtle.mythicage.core.village.soldier.SoldierProfile;
import io.github.happysnappingturtle.mythicage.core.village.soldier.SoldierTargetRules;
import io.github.happysnappingturtle.mythicage.core.village.soldier.ThrustGeometry;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import io.github.happysnappingturtle.mythicage.village.work.VillageScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 挂在居民身上的士兵 AI（T20 设计文档 §5、§8）：服务端、不存档。命令来自调度器（{@link SoldierOrders}），
 * 这里只按命令执行——找目标、站岗巡逻、接敌、举盾、护送、撤退、补觉、换装。
 */
public final class SoldierBrain {
    /** 士兵发现敌人时呼喊的半径（覆盖居民的 12 格警戒，§5.5）。 */
    public static final double SHOUT_RADIUS = 16;
    /** 站岗时离岗位最远多少格。 */
    public static final int POST_SPREAD = 4;
    /** 每隔多久换一个站位（刻）。 */
    public static final int RESPOT = 400;
    public static final int SCAN_INTERVAL = 10;
    /** 战斗后这么久才开始回血（§3.2）。 */
    public static final int CALM_TICKS = 600;
    static final double WALK = 0.55 / 0.5;
    static final double RUN = 0.85 / 0.5;

    private final ResidentEntity soldier;
    @Nullable
    private SoldierOrders orders;
    @Nullable
    private LivingEntity target;
    private SoldierTargetRules.Decision decision = SoldierTargetRules.Decision.IGNORE;
    private int scanIn;
    private long attackStart = Long.MIN_VALUE / 2;
    private boolean struck;
    private long raisedAt = Long.MIN_VALUE / 2;
    private long loweredAt = Long.MIN_VALUE / 2;
    private boolean blocking;
    private boolean retreating;
    private long lastCombat = Long.MIN_VALUE / 2;
    private long alertSerialSeen = -1;
    @Nullable
    private BlockPos spot;
    private long spotAt = Long.MIN_VALUE / 2;
    private int repathIn;
    private long alertAnimUntil;
    /** 调试统计：出手、命中、收手次数与最近一次的结果。 */
    private int starts, strikes, withholds;
    private String last = "";
    /** 正在护的人（拦截）：站到他与威胁之间，只在威胁进入触及时出手。 */
    @Nullable
    private LivingEntity ward;
    /** 被护者 → 护他的士兵（一个人同时只由一名士兵护，§9.1）。 */
    private static final java.util.Map<java.util.UUID, ResidentEntity> WARDS = new java.util.concurrent.ConcurrentHashMap<>();

    public SoldierBrain(ResidentEntity soldier) {
        this.soldier = soldier;
    }

    // ───────────── 命令与状态 ─────────────

    public void setOrders(@Nullable SoldierOrders o) {
        orders = o;
    }

    @Nullable
    public SoldierOrders orders() {
        SoldierOrders o = orders;
        if (o != null && soldier.level().getGameTime() - o.issuedAt() > SoldierOrders.STALE) {
            orders = null;
            return null;
        }
        return o;
    }

    @Nullable
    public LivingEntity target() {
        return target;
    }

    public SoldierTargetRules.Decision decision() {
        return decision;
    }

    public boolean retreating() {
        return retreating;
    }

    public boolean blocking() {
        return blocking;
    }

    @Nullable
    public LivingEntity ward() {
        return ward;
    }

    /** 开始护一个人：威胁就是目标（无兵装时只拦不打）。 */
    public void guard(LivingEntity who, LivingEntity threat) {
        SoldierOrders o = orders();
        if (target == null && o != null && soldier.level() instanceof ServerLevel level) {
            onFirstSight(o, level, level.getGameTime(), threat);
        }
        clearWard();
        ward = who;
        WARDS.put(who.getUUID(), soldier);
        target = threat;
        decision = o != null && o.tier() == SoldierProfile.Tier.NONE ? SoldierTargetRules.Decision.ESCORT
                : SoldierTargetRules.Decision.ENGAGE;
    }

    /**
     * 别人报来的威胁（圣火烽号、呼喊，§9.2）：在岗、手上没有目标、岗位离威胁 ≤ 24 格、按敌我表该接敌的，就去接敌。
     * 返回是否接了。
     */
    public boolean notifyThreat(LivingEntity threat) {
        SoldierOrders o = orders();
        if (o == null || target != null || retreating || o.state() != DutyRules.State.ON_DUTY || threat == null || !threat.isAlive()
                || !(soldier.level() instanceof ServerLevel level)) {
            return false;
        }
        BlockPos c = leash(o);
        if (!SoldierTargetRules.keepChasing(Math.sqrt(threat.distanceToSqr(Vec3.atBottomCenterOf(c))),
                SoldierTargets.inVillage(level, threat.blockPosition(), o.village()))) {
            return false;
        }
        SoldierTargetRules.Decision d = SoldierTargetRules.decide(SoldierTargets.facts(soldier, threat, o.village(),
                reach(o.profile()), level.getGameTime()));
        if (d == SoldierTargetRules.Decision.IGNORE) {
            return false;
        }
        alertSerialSeen = Soldiers.alertSerial(o.village());
        target = threat;
        decision = o.tier() == SoldierProfile.Tier.NONE && d == SoldierTargetRules.Decision.ENGAGE ? SoldierTargetRules.Decision.ESCORT : d;
        soldier.playAnim(o.profile().anim("alert"));
        alertAnimUntil = level.getGameTime() + 16;
        return true;
    }

    public void clearWard() {
        if (ward != null) {
            WARDS.remove(ward.getUUID(), soldier);
            ward = null;
        }
    }

    /** 护着某人的士兵（还活着、还在护他）。 */
    public static java.util.Optional<ResidentEntity> guardOf(java.util.UUID who) {
        ResidentEntity g = WARDS.get(who);
        if (g == null || !g.isAlive() || g.isRemoved() || !g.isSoldier() || g.soldierBrain().ward == null
                || !g.soldierBrain().ward.getUUID().equals(who)) {
            WARDS.remove(who);
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(g);
    }

    public long lastCombat() {
        return lastCombat;
    }

    /** 此刻是否由士兵 AI 接管（调度器不给他派单、警戒时不派回屋）。 */
    public boolean busy() {
        SoldierOrders o = orders();
        if (o == null) {
            return false;
        }
        return o.state() != DutyRules.State.OFF_DUTY || target != null || retreating || o.needSwap();
    }

    /** 举盾生效（举起的过渡做完了）。 */
    public boolean blockingEffective() {
        SoldierOrders o = orders();
        return blocking && o != null && o.profile().hasShield()
                && ShieldRules.effective(o.profile().shield(), soldier.level().getGameTime(), raisedAt);
    }

    // ───────────── 受伤 ─────────────

    /** 举盾时的伤害倍率（正面扇形、伤害有来源位置、不是绕盾伤害）。 */
    public float shieldMultiplier(DamageSource source) {
        SoldierOrders o = orders();
        if (o == null || !blockingEffective() || source.is(DamageTypeTags.BYPASSES_SHIELD)) {
            return 1.0F;
        }
        Vec3 from = source.getSourcePosition();
        if (from == null) {
            return 1.0F;
        }
        return (float) ShieldRules.multiplier(o.profile().shield(), true, soldier.getYRot(), from.x - soldier.getX(),
                from.z - soldier.getZ());
    }

    // ───────────── 每刻 ─────────────

    /**
     * 不值勤时也要偶尔看一眼（自己发现敌人就接敌，§5.4）。返回是否该由士兵 AI 接管。
     */
    public boolean wants() {
        SoldierOrders o = orders();
        if (o == null || soldier.inTransit()) {
            return false;
        }
        if (o.state() == DutyRules.State.OFF_DUTY && target == null && !retreating && !o.needSwap()
                && !soldier.resting() && --scanIn <= 0) {
            scanIn = SCAN_INTERVAL;
            scan(o);
        }
        return busy();
    }

    public void tick() {
        SoldierOrders o = orders();
        if (o == null || !(soldier.level() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        SoldierProfile p = o.profile();
        float hp = soldier.getHealth() / Math.max(1, soldier.getMaxHealth());
        if (!retreating && hp < p.retreatBelow() && target != null) {
            retreating = true;
        } else if (retreating && hp >= p.returnAbove()) {
            retreating = false;
        }
        if (--scanIn <= 0) {
            scanIn = SCAN_INTERVAL;
            scan(o);
        }
        validateTarget(o, level);
        updateShield(o, now);
        if (alertAnimUntil > 0 && now >= alertAnimUntil) {
            alertAnimUntil = 0;
            if (p.anim("alert").equals(soldier.anim())) {
                soldier.playAnim("");
            }
        }
        if (target != null) {
            lastCombat = now;
        }
        SoldierAbility ability = SoldierAbility.of(p.ability()).orElse(null);
        if (ability != null && ability.tick(soldier, this)) {
            return;
        }
        if (retreating) {
            retreat(o, now);
            return;
        }
        if (target != null) {
            if (soldier.resting()) {
                soldier.wakeUp();
            }
            if (decision == SoldierTargetRules.Decision.ENGAGE && o.tier() != SoldierProfile.Tier.NONE) {
                combat(o, now);
            } else {
                escort(o, now);
            }
            return;
        }
        endAttackAnim(p);
        if (o.needSwap()) {
            goSwap(o);
            return;
        }
        if (o.state() == DutyRules.State.ON_DUTY && ammoChores(o, now)) {
            return;
        }
        switch (o.state()) {
            case ON_DUTY -> guard(o, now);
            case SHIFT_SLEEP -> sleep();
            case OFF_DUTY -> {
            }
        }
    }

    // ───────────── 找目标 ─────────────

    /** 拴绳中心：在岗是当前岗位；不值勤是离自己最近的岗位；都没有就是自己。 */
    BlockPos leash(SoldierOrders o) {
        BlockPos post = o.postPos();
        if (post != null) {
            return post;
        }
        BlockPos best = null;
        double bd = Double.MAX_VALUE;
        for (BlockPos b : o.posts()) {
            double d = b.distToCenterSqr(soldier.position());
            if (d < bd) {
                bd = d;
                best = b;
            }
        }
        return best != null ? best : soldier.blockPosition();
    }

    private void scan(SoldierOrders o) {
        if (!(soldier.level() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        BlockPos c = leash(o);
        AABB box = new AABB(c).inflate(SoldierTargetRules.ENGAGE_RADIUS, 8, SoldierTargetRules.ENGAGE_RADIUS);
        double reach = reach(o.profile());
        LivingEntity best = null;
        SoldierTargetRules.Decision bestDec = SoldierTargetRules.Decision.IGNORE;
        double bestScore = Double.MAX_VALUE;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, x -> x != soldier && x.isAlive() && !x.isSpectator())) {
            SoldierTargetRules.Decision d = SoldierTargetRules.decide(SoldierTargets.facts(soldier, e, o.village(), reach, now));
            if (d == SoldierTargetRules.Decision.IGNORE) {
                continue;
            }
            if (o.tier() == SoldierProfile.Tier.NONE && d == SoldierTargetRules.Decision.ENGAGE) {
                d = SoldierTargetRules.Decision.ESCORT;
            }
            if (!SoldierTargetRules.canEngage(Math.sqrt(e.distanceToSqr(Vec3.atBottomCenterOf(c))),
                    SoldierTargets.inVillage(level, e.blockPosition(), o.village()))) {
                continue;
            }
            if (ward != null) {
                // 护人时不换目标
                break;
            }
            if (e != target && !soldier.hasLineOfSight(e)) {
                continue;
            }
            // 接敌优先于护送；同类里先打正在打本村人的，再按距离
            double score = soldier.distanceToSqr(e) + (d == SoldierTargetRules.Decision.ENGAGE ? 0 : 10000)
                    + (e instanceof Mob m && m.getTarget() instanceof ResidentEntity ? 0 : 400);
            if (score < bestScore) {
                bestScore = score;
                best = e;
                bestDec = d;
            }
        }
        if (best != null && target == null) {
            onFirstSight(o, level, now, best);
        }
        if (best != null) {
            target = best;
            decision = bestDec;
        }
    }

    private void onFirstSight(SoldierOrders o, ServerLevel level, long now, LivingEntity threat) {
        if (soldier.resting()) {
            soldier.wakeUp();
        }
        if (soldier.job().isPresent()) {
            soldier.abortJob(false);
        }
        long serial = VillageScheduler.raiseAlarm(o.village(), now);
        soldier.playAnim(o.profile().anim("alert"));
        alertAnimUntil = now + 16;
        if (serial != alertSerialSeen) {
            alertSerialSeen = serial;
            // 新一场警戒：告诉本村所有士兵的独有机制（圣火烽号等，每场一次）
            Soldiers.broadcastAlert(level, o.village(), serial, threat);
        }
    }

    private void validateTarget(SoldierOrders o, ServerLevel level) {
        if (target == null) {
            return;
        }
        if (!target.isAlive() || target.isRemoved() || target.level() != level || target.isSpectator()) {
            dropTarget();
            return;
        }
        BlockPos c = leash(o);
        boolean in = SoldierTargets.inVillage(level, target.blockPosition(), o.village());
        if (!SoldierTargetRules.keepChasing(Math.sqrt(target.distanceToSqr(Vec3.atBottomCenterOf(c))), in)) {
            dropTarget();
            return;
        }
        if (soldier.tickCount % SCAN_INTERVAL == 0) {
            SoldierTargetRules.Decision d = SoldierTargetRules.decide(SoldierTargets.facts(soldier, target, o.village(),
                    reach(o.profile()), level.getGameTime()));
            if (d == SoldierTargetRules.Decision.IGNORE) {
                dropTarget();
            } else {
                decision = o.tier() == SoldierProfile.Tier.NONE && d == SoldierTargetRules.Decision.ENGAGE
                        ? SoldierTargetRules.Decision.ESCORT : d;
            }
        }
    }

    private void dropTarget() {
        clearWard();
        target = null;
        decision = SoldierTargetRules.Decision.IGNORE;
        soldier.getNavigation().stop();
        spot = null;
    }

    static double reach(SoldierProfile p) {
        return p.isMelee() ? p.melee().reach() : p.ranged().range();
    }

    // ───────────── 接敌（近战） ─────────────

    private void combat(SoldierOrders o, long now) {
        SoldierProfile p = o.profile();
        if (!p.isMelee()) {
            ranged(o, now);
            return;
        }
        int t = (int) Math.min(Integer.MAX_VALUE, Math.max(-1, now - attackStart));
        AttackTimeline.Phase phase = AttackTimeline.phase(p.timeline(), t);
        double edge = soldier.distanceTo(target) - target.getBbWidth() / 2;
        if (ward != null && phase == AttackTimeline.Phase.READY
                && !(edge <= p.melee().reach() && soldier.hasLineOfSight(target))) {
            // 拦截：站到被护者与威胁的连线上，面向威胁举盾，等它进入触及
            double[] at = InterceptRules.stand(ward.getX(), ward.getZ(), target.getX(), target.getZ());
            Vec3 dest = new Vec3(at[0], ward.getY(), at[1]);
            if (soldier.position().distanceToSqr(dest) > 0.5) {
                approach(dest, RUN);
                if (!p.anim("intercept").equals(soldier.anim()) && !blocking) {
                    soldier.playAnim(p.anim("intercept"));
                }
            } else {
                soldier.getNavigation().stop();
            }
            face(target);
            raise(o, now);
            return;
        }
        if (phase == AttackTimeline.Phase.READY) {
            if (edge <= p.melee().reach() && soldier.hasLineOfSight(target)) {
                face(target);
                soldier.getNavigation().stop();
                ThrustGeometry.Outcome pre = sweep(o, now);
                if (pre instanceof ThrustGeometry.Withhold w) {
                    withholds++;
                    last = "withhold-pre:" + w.blocker();
                    sidestep();
                    return;
                }
                lower(o, now);
                starts++;
                attackStart = now;
                struck = false;
                soldier.playAnim(p.anim("attack"));
            } else {
                approach(target.position(), RUN);
                if (p.hasShield() && edge <= 6 && target instanceof RangedAttackMob) {
                    raise(o, now);
                }
            }
            return;
        }
        if (!AttackTimeline.locked(p.timeline(), t)) {
            face(target);
        }
        soldier.getNavigation().stop();
        if (phase == AttackTimeline.Phase.ACTIVE && !struck) {
            ThrustGeometry.Outcome out = sweep(o, now);
            last = out.toString();
            if (out instanceof ThrustGeometry.Strike s && s.who() instanceof LivingEntity hit) {
                hit.hurt(soldier.damageSources().mobAttack(soldier), p.damage(o.tier()));
                strikes++;
                struck = true;
            } else if (out instanceof ThrustGeometry.Withhold) {
                withholds++;
                // 友方挡在矛线上：收手，时间轴直接进冷却
                struck = true;
                soldier.playAnim("");
                sidestep();
            }
        }
        if (t >= AttackTimeline.animationEnd(p.timeline())) {
            endAttackAnim(p);
            if (p.hasShield() && phase == AttackTimeline.Phase.COOLDOWN && edge <= 3) {
                raise(o, now);
            }
        }
    }

    // ───────────── 接敌（远程，§8.2） ─────────────

    private boolean fired;
    private boolean backstepPending;

    private void ranged(SoldierOrders o, long now) {
        SoldierProfile p = o.profile();
        SoldierProfile.Ranged r = p.ranged();
        ServerLevel level = (ServerLevel) soldier.level();
        Optional<SoldierRecord> rec = Soldiers.record(level, soldier);
        Optional<SoldierProfile.AmmoItem> ammo = rec.flatMap(x -> p.ammo().items().stream()
                .filter(a -> x.ammo().getOrDefault(a.item(), 0) > 0 && RangedAttacks.launcher(a.item()).isPresent()).findFirst());
        int t = (int) Math.min(Integer.MAX_VALUE, Math.max(-1, now - attackStart));
        AttackTimeline.Phase phase = AttackTimeline.phase(p.timeline(), t);
        double dist = soldier.distanceTo(target);
        if (phase == AttackTimeline.Phase.READY) {
            if (ammo.isEmpty()) {
                // 没有弹药：护送居民（§9.2）
                escort(o, now);
                return;
            }
            if (backstepPending || dist < r.minRange()) {
                backstepPending = false;
                backstep(Math.max(r.backstep(), r.minRange() - dist + 1));
                face(target);
                return;
            }
            if (dist > r.range() - 0.5 || !soldier.hasLineOfSight(target)) {
                approach(target.position(), RUN);
                return;
            }
            Optional<LivingEntity> blocker = lineBlocker(o, now);
            if (blocker.isPresent()) {
                withholds++;
                last = "withhold-line:" + blocker.get().getName().getString();
                sidestep();
                return;
            }
            soldier.getNavigation().stop();
            face(target);
            starts++;
            attackStart = now;
            fired = false;
            soldier.playAnim(p.anim("attack"));
            return;
        }
        if (!AttackTimeline.locked(p.timeline(), t)) {
            face(target);
        }
        soldier.getNavigation().stop();
        if (phase == AttackTimeline.Phase.ACTIVE && !fired && ammo.isPresent()) {
            fired = true;
            SoldierProfile.AmmoItem a = ammo.get();
            Vec3 from = new Vec3(soldier.getX(), soldier.getY() + soldier.getEyeHeight() * 0.95, soldier.getZ());
            Vec3 aim = target.position().add(0, target.getBbHeight() * 0.6, 0);
            double[] v = io.github.happysnappingturtle.mythicage.core.village.soldier.Ballistics.launch(aim.x - from.x, aim.y - from.y,
                    aim.z - from.z, r.speed(), r.gravity()).orElseGet(() -> {
                        Vec3 d = aim.subtract(from).multiply(1, 0, 1).normalize().scale(r.speed() * Math.cos(Math.PI / 4));
                        return new double[]{d.x, r.speed() * Math.sin(Math.PI / 4), d.z};
                    });
            float dmg = Math.max(1, p.damage(o.tier()) + a.damage());
            RangedAttacks.launcher(a.item()).ifPresent(l -> l.launch(level, soldier, o.village(), a.item(), dmg, from,
                    new Vec3(v[0], v[1], v[2]), p));
            Soldiers.useAmmo(level, soldier, a.item());
            strikes++;
            last = "throw:" + a.item();
            backstepPending = r.backstep() > 0;
        }
        if (t >= AttackTimeline.animationEnd(p.timeline())) {
            endAttackAnim(p);
        }
    }

    /** 投射线上（出手点到目标胸口）0.6 格内有不能打的人（§8.2）。 */
    private Optional<LivingEntity> lineBlocker(SoldierOrders o, long now) {
        Vec3 a = soldier.getEyePosition();
        Vec3 b = target.position().add(0, target.getBbHeight() * 0.6, 0);
        Vec3 ab = b.subtract(a);
        double len2 = ab.lengthSqr();
        for (LivingEntity e : soldier.level().getEntitiesOfClass(LivingEntity.class, new AABB(a, b).inflate(1),
                x -> x != soldier && x != target && x.isAlive())) {
            Vec3 c = e.position().add(0, e.getBbHeight() * 0.5, 0);
            double k = len2 < 1.0E-6 ? 0 : Math.max(0, Math.min(1, c.subtract(a).dot(ab) / len2));
            if (k <= 0 || k >= 1) {
                continue;
            }
            Vec3 p = a.add(ab.scale(k));
            if (p.distanceTo(c) <= 0.6 + e.getBbWidth() / 2 && SoldierTargetRules.decide(SoldierTargets.facts(soldier, e, o.village(),
                    o.profile().ranged().range(), now)) != SoldierTargetRules.Decision.ENGAGE) {
                return Optional.of(e);
            }
        }
        return Optional.empty();
    }

    /** 面朝目标往后退 {@code dist} 格（投后撤步、近身后撤）。 */
    private void backstep(double dist) {
        Vec3 away = soldier.position().subtract(target.position()).multiply(1, 0, 1);
        if (away.lengthSqr() < 1.0E-4) {
            away = new Vec3(1, 0, 0);
        }
        Vec3 dest = soldier.position().add(away.normalize().scale(dist));
        soldier.getNavigation().moveTo(dest.x, soldier.getY(), dest.z, WALK);
        repathIn = 10;
    }

    // ───────────── 远程兵的弹药：捡回与补给 ─────────────

    /** 不在警戒时，捡回岗位附近落地的弹药，或回谷仓补满（§9.2）。返回是否接管了这一刻。 */
    private boolean ammoChores(SoldierOrders o, long now) {
        SoldierProfile p = o.profile();
        if (p.isMelee() || p.ammo() == null || VillageScheduler.runtime(o.village()).alarmed(now)) {
            return false;
        }
        ServerLevel level = (ServerLevel) soldier.level();
        BlockPos c = leash(o);
        Optional<net.minecraft.world.entity.Entity> fallen = level.getEntities((net.minecraft.world.entity.Entity) null,
                new AABB(c).inflate(SoldierTargetRules.CHASE_RADIUS, 8, SoldierTargetRules.CHASE_RADIUS),
                e -> e instanceof SoldierAmmo a && a.landed() && o.village().equals(a.ammoVillage())).stream()
                .min(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(soldier)));
        if (fallen.isPresent()) {
            net.minecraft.world.entity.Entity f = fallen.get();
            if (soldier.distanceToSqr(f) <= 2.25) {
                soldier.getNavigation().stop();
                soldier.playAnim("collect_pick");
                Soldiers.recoverAmmo(level, (SoldierAmmo) f, p);
                f.discard();
            } else {
                approach(f.position(), WALK);
            }
            return true;
        }
        int total = Soldiers.record(level, soldier).map(SoldierRecord::ammoTotal).orElse(0);
        if (total < p.ammo().carry() && Soldiers.ammoInStock(level, o.village(), p) > 0) {
            if (soldier.reached(o.keeper())) {
                soldier.getNavigation().stop();
                Soldiers.resupply(level, soldier, p);
            } else {
                approach(Vec3.atBottomCenterOf(o.keeper()), WALK);
            }
            return true;
        }
        return false;
    }

    private void endAttackAnim(SoldierProfile p) {
        if (p.anim("attack").equals(soldier.anim())) {
            soldier.playAnim("");
        }
    }

    /** 本刻胶囊里的所有对象（§8.1）。 */
    private ThrustGeometry.Outcome sweep(SoldierOrders o, long now) {
        SoldierProfile.Melee m = o.profile().melee();
        double yaw = soldier.getYRot();
        double fx = -Math.sin(Math.toRadians(yaw));
        double fz = Math.cos(Math.toRadians(yaw));
        Vec3 from = new Vec3(soldier.getX(), soldier.getY() + m.height(), soldier.getZ());
        Vec3 to = from.add(fx * m.reach(), 0, fz * m.reach());
        HitResult clip = soldier.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, soldier));
        double blocked = clip.getType() == HitResult.Type.MISS ? Double.MAX_VALUE : clip.getLocation().distanceTo(from);
        List<ThrustGeometry.Hit> hits = new ArrayList<>();
        AABB area = soldier.getBoundingBox().inflate(m.reach() + 1);
        for (LivingEntity e : soldier.level().getEntitiesOfClass(LivingEntity.class, area, x -> x != soldier && x.isAlive())) {
            AABB b = e.getBoundingBox();
            var along = ThrustGeometry.hit(soldier.getX(), soldier.getY(), soldier.getZ(), yaw, m, blocked,
                    new ThrustGeometry.Box(b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ));
            if (along.isEmpty()) {
                continue;
            }
            boolean attackable = e == target ? decision == SoldierTargetRules.Decision.ENGAGE
                    : SoldierTargetRules.decide(SoldierTargets.facts(soldier, e, o.village(), m.reach(), now))
                    == SoldierTargetRules.Decision.ENGAGE;
            hits.add(new ThrustGeometry.Hit(e, along.get(), attackable));
        }
        return ThrustGeometry.resolve(hits);
    }

    // ───────────── 盾 ─────────────

    private void updateShield(SoldierOrders o, long now) {
        if (!blocking) {
            return;
        }
        SoldierProfile p = o.profile();
        if (!p.hasShield() || ShieldRules.mustLower(p.shield(), now, raisedAt)) {
            lower(o, now);
        }
    }

    void raise(SoldierOrders o, long now) {
        SoldierProfile p = o.profile();
        if (blocking || !p.hasShield() || !ShieldRules.canRaise(p.shield(), now, loweredAt)) {
            return;
        }
        blocking = true;
        raisedAt = now;
        soldier.playAnim(p.anim("block"));
    }

    void lower(SoldierOrders o, long now) {
        if (!blocking) {
            return;
        }
        blocking = false;
        loweredAt = now;
        if (o.profile().anim("block").equals(soldier.anim())) {
            soldier.playAnim("");
        }
    }

    // ───────────── 护送、撤退 ─────────────

    /** 站到威胁与离它最近的本村居民之间（§5.6）；有盾就举盾。 */
    private void escort(SoldierOrders o, long now) {
        LivingEntity threat = target;
        ResidentEntity ward = null;
        double bd = Double.MAX_VALUE;
        for (ResidentEntity r : soldier.level().getEntitiesOfClass(ResidentEntity.class, threat.getBoundingBox().inflate(16),
                r -> r != soldier && !r.isSoldier() && r.village().map(o.village()::equals).orElse(false))) {
            double d = r.distanceToSqr(threat) - (threat instanceof Mob m && m.getTarget() == r ? 1000 : 0);
            if (d < bd) {
                bd = d;
                ward = r;
            }
        }
        if (ward != null) {
            double[] at = InterceptRules.stand(ward.getX(), ward.getZ(), threat.getX(), threat.getZ());
            Vec3 dest = new Vec3(at[0], ward.getY(), at[1]);
            if (soldier.position().distanceToSqr(dest) > 1.0) {
                approach(dest, RUN);
            } else {
                soldier.getNavigation().stop();
            }
        }
        face(threat);
        raise(o, now);
    }

    private void retreat(SoldierOrders o, long now) {
        BlockPos home = soldier.home();
        if (home != null && !soldier.reached(home)) {
            approach(Vec3.atBottomCenterOf(home), RUN);
        } else {
            soldier.getNavigation().stop();
            if (target != null && o.tier() != SoldierProfile.Tier.NONE && o.profile().isMelee()
                    && decision == SoldierTargetRules.Decision.ENGAGE && soldier.distanceTo(target) < 3) {
                combat(o, now);
                return;
            }
        }
        if (target != null && soldier.distanceTo(target) < 8) {
            face(target);
            raise(o, now);
        }
    }

    // ───────────── 站岗、补觉、换装 ─────────────

    private void guard(SoldierOrders o, long now) {
        BlockPos post = o.postPos();
        if (post == null) {
            return;
        }
        if (soldier.resting()) {
            soldier.wakeUp();
        }
        if (spot == null || now - spotAt >= RESPOT || spot.distManhattan(post) > POST_SPREAD * 2) {
            spot = pickSpot(post);
            spotAt = now;
        }
        String idle = o.profile().anim("idle");
        if (!soldier.reached(spot) && soldier.position().distanceToSqr(Vec3.atBottomCenterOf(spot)) > 1.0) {
            if (idle.equals(soldier.anim())) {
                soldier.playAnim("");
            }
            approach(Vec3.atBottomCenterOf(spot), WALK);
        } else {
            soldier.getNavigation().stop();
            if (soldier.anim().isEmpty()) {
                soldier.playAnim(idle);
            }
            // 朝外：从村钟看向岗位的方向
            BlockPos bell = o.bell();
            double dx = post.getX() - bell.getX();
            double dz = post.getZ() - bell.getZ();
            if (dx * dx + dz * dz > 1) {
                soldier.setYRot((float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90F);
                soldier.yBodyRot = soldier.getYRot();
                soldier.yHeadRot = soldier.getYRot();
            }
        }
    }

    private BlockPos pickSpot(BlockPos post) {
        for (int i = 0; i < 6; i++) {
            BlockPos p = post.offset(soldier.getRandom().nextInt(POST_SPREAD * 2 + 1) - POST_SPREAD, 0,
                    soldier.getRandom().nextInt(POST_SPREAD * 2 + 1) - POST_SPREAD);
            BlockPos s = soldier.standSpot(p);
            if (!s.equals(p) || soldier.standable(p)) {
                return s;
            }
        }
        return soldier.standSpot(post);
    }

    private void sleep() {
        if (soldier.resting()) {
            return;
        }
        BlockPos home = soldier.home();
        if (home == null) {
            return;
        }
        if (soldier.reached(home)) {
            soldier.getNavigation().stop();
            soldier.playAnim("");
            soldier.trySleepAtHome();
        } else {
            approach(Vec3.atBottomCenterOf(home), WALK);
        }
    }

    private void goSwap(SoldierOrders o) {
        BlockPos k = o.keeper();
        if (soldier.reached(k)) {
            soldier.getNavigation().stop();
            Soldiers.swapKit((ServerLevel) soldier.level(), soldier);
        } else {
            approach(Vec3.atBottomCenterOf(k), WALK);
        }
    }

    /** 状态变了（换班、降回）：清掉战斗与站岗的临时状态。 */
    public void reset() {
        dropTarget();
        retreating = false;
        blocking = false;
        attackStart = Long.MIN_VALUE / 2;
        if (!soldier.anim().isEmpty() && !"sleep".equals(soldier.anim())) {
            soldier.playAnim("");
        }
    }

    // ───────────── 移动 ─────────────

    private void approach(Vec3 dest, double speed) {
        if (--repathIn <= 0 || soldier.getNavigation().isDone()) {
            repathIn = 10;
            soldier.getNavigation().moveTo(dest.x, dest.y, dest.z, speed);
        }
    }

    private void sidestep() {
        double yaw = Math.toRadians(soldier.getYRot() + (soldier.getRandom().nextBoolean() ? 90 : -90));
        double x = soldier.getX() - Math.sin(yaw) * 1.5;
        double z = soldier.getZ() + Math.cos(yaw) * 1.5;
        soldier.getNavigation().moveTo(x, soldier.getY(), z, WALK);
        repathIn = 10;
    }

    void face(LivingEntity e) {
        double dx = e.getX() - soldier.getX();
        double dz = e.getZ() - soldier.getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90F;
        soldier.setYRot(yaw);
        soldier.yBodyRot = yaw;
        soldier.yHeadRot = yaw;
        soldier.getLookControl().setLookAt(e, 30, 30);
    }

    // ───────────── 测试 ─────────────

    /** 因友方挡在矛线上而收手的次数（GameTest 用）。 */
    public int withholds() {
        return withholds;
    }

    /** 调试：出手 / 命中 / 收手次数、最近结果、当前阶段。 */
    public String debug() {
        SoldierOrders o = orders();
        String phase = o == null || !o.profile().isMelee() ? "-" : AttackTimeline.phase(o.profile().timeline(),
                (int) Math.min(Integer.MAX_VALUE, Math.max(-1, soldier.level().getGameTime() - attackStart))).name();
        return "出手 " + starts + " 命中 " + strikes + " 收手 " + withholds + " 最近 " + last + " 阶段 " + phase
                + " 目标 " + (target == null ? "-" : target.getName().getString() + "@" + target.blockPosition().toShortString()
                + " 距 " + String.format("%.2f", soldier.distanceTo(target))) + " 决定 " + decision + " 命令 "
                + (o == null ? "无" : o.state() + "/岗" + o.post());
    }

    /** 只供 GameTest：直接指定目标（跳过发现）。 */
    public void targetForTest(LivingEntity e, SoldierTargetRules.Decision d) {
        target = e;
        decision = d;
    }

    /** 只供 GameTest：举盾并跳过举起的过渡。 */
    public void blockForTest(boolean on) {
        long now = soldier.level().getGameTime();
        blocking = on;
        if (on) {
            loweredAt = now - 200;
            raisedAt = now - 6;
        } else {
            loweredAt = now;
        }
    }
}
