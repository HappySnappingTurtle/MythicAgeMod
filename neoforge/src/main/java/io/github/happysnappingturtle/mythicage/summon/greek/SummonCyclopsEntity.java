package io.github.happysnappingturtle.mythicage.summon.greek;

import io.github.happysnappingturtle.mythicage.core.power.ThrowRules;
import io.github.happysnappingturtle.mythicage.core.summon.HurlRules;
import io.github.happysnappingturtle.mythicage.entity.anim.MythAnimState;
import io.github.happysnappingturtle.mythicage.entity.cyclops.CyclopsEntity;
import io.github.happysnappingturtle.mythicage.entity.harpy.HarpyEntity;
import io.github.happysnappingturtle.mythicage.power.TargetFactsCollector;
import io.github.happysnappingturtle.mythicage.summon.MythSummon;
import io.github.happysnappingturtle.mythicage.summon.SummonGoals;
import io.github.happysnappingturtle.mythicage.summon.SummonLink;
import io.github.happysnappingturtle.mythicage.summon.SummonTargets;
import io.github.happysnappingturtle.mythicage.summon.SummonUnits;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;

/**
 * 召唤版独眼巨人（SU-GR-04，T25a 设计文档 §3）：重坦，挥橄榄木棒普攻；特殊攻击「抓投」——
 * 伸手 1 秒 → 抓起一个非神话、非 Boss 的敌人举过头顶 → 向前扔约 8 格（落点在墙里时沿抛线往回找空位），
 * 被扔者落地受 12，落点 2 格内其他敌人受 6；神话生物、Boss 抓空（冷却 5 秒）；玩家只击退（D6–D8）。
 * 规则在 core {@link HurlRules}；抓取免疫与哈耳庇厄共用（{@link HarpyEntity#IMMUNE_KEY}）。
 */
public class SummonCyclopsEntity extends CyclopsEntity implements MythSummon {
    /** 举起到松手的刻数（hurl 动画第 0.5 秒松手）。 */
    public static final int HOLD_TICKS = 10;
    /** 举起的前几刻被抓者从身前低处移到头顶。 */
    private static final int LIFT_TICKS = 6;
    /** 伸手结束时目标离得多远还抓得到（水平，格）。 */
    private static final double REACH = 4.0;
    /** 冷却好时自动对当前目标出手的距离（格）。 */
    private static final double AUTO_RANGE = 3.5;
    /** 传令杖点名时最远多少格（超出 {@link #AUTO_RANGE} 先走过去）。 */
    private static final double REQUEST_RANGE = 16.0;
    private static final int APPROACH_TICKS = 80;

    private final SummonLink link = new SummonLink();

    private enum Phase { NONE, APPROACH, REACH, HOLD }

    private Phase phase = Phase.NONE;
    private int phaseTimer;
    @Nullable
    private LivingEntity hurlTarget;
    private long hurlReadyAt;
    private float hurlYaw;

    // 飞行中的被扔者（松手后由本实体按固定抛线逐刻摆放）
    @Nullable
    private LivingEntity flying;
    private Vec3 flyFrom = Vec3.ZERO;
    private Vec3 flyTo = Vec3.ZERO;
    private int flyTick;
    private int flyTicks;

    private int windupTicks = HurlRules.WINDUP_TICKS;
    private int cooldownTicks = HurlRules.COOLDOWN;
    private int missCooldownTicks = HurlRules.MISS_COOLDOWN;
    private float hitDamage = HurlRules.HIT_DAMAGE;
    private float splashDamage = HurlRules.SPLASH_DAMAGE;

    public SummonCyclopsEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new HurlLockGoal());
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.addGoal(3, new SummonGoals.Follow(this, 1.15));
        goalSelector.addGoal(3, new SummonGoals.Guard(this, 1.0));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(0, new SummonGoals.Commanded(this));
        targetSelector.addGoal(1, new SummonGoals.DefendOwner(this));
        targetSelector.addGoal(2, new Retaliate());
        targetSelector.addGoal(3, new SummonGoals.NearestEnemy(this));
    }

    // ───────────── MythSummon ─────────────

    @Override
    public SummonLink link() {
        return link;
    }

    @Override
    public boolean combatUnit() {
        return true;
    }

    @Override
    public void applyStats(SummonUnits.Stats s) {
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(s.attr("max_health", 110));
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(s.attr("attack_damage", 10));
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(s.attr("movement_speed", 0.25));
        if (s.attributes().containsKey("armor")) {
            getAttribute(Attributes.ARMOR).setBaseValue(s.attr("armor", 0));
        }
        setHealth(getMaxHealth());
        windupTicks = s.specialInt("windup_ticks", windupTicks);
        cooldownTicks = s.specialInt("cooldown_ticks", cooldownTicks);
        missCooldownTicks = s.specialInt("miss_cooldown_ticks", missCooldownTicks);
        hitDamage = (float) s.specialDouble("hit_damage", hitDamage);
        splashDamage = (float) s.specialDouble("splash_damage", splashDamage);
    }

    @Override
    public boolean requestSpecial(LivingEntity target) {
        if (phase != Phase.NONE || level().getGameTime() < hurlReadyAt || distanceToSqr(target) > REQUEST_RANGE * REQUEST_RANGE
                || !SummonTargets.canAttack(this, target)) {
            return false;
        }
        start(target);
        return true;
    }

    /** 正在抓投（走近、伸手或举着）。 */
    public boolean hurling() {
        return phase != Phase.NONE;
    }

    public long hurlReadyAt() {
        return hurlReadyAt;
    }

    @Nullable
    public LivingEntity flying() {
        return flying;
    }

    // ───────────── 抓投 ─────────────

    private void start(LivingEntity target) {
        hurlTarget = target;
        setTarget(target);
        if (horizontalDist(target) > AUTO_RANGE) {
            phase = Phase.APPROACH;
            phaseTimer = APPROACH_TICKS;
            getNavigation().moveTo(target, 1.2);
        } else {
            beginReach();
        }
    }

    private void beginReach() {
        phase = Phase.REACH;
        phaseTimer = windupTicks;
        getNavigation().stop();
        playOnce(MythAnimState.GRAB, windupTicks + 2);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        LivingEntity t = getTarget();
        if (phase == Phase.NONE && flying == null && t != null && t.isAlive() && level().getGameTime() >= hurlReadyAt
                && horizontalDist(t) < AUTO_RANGE && SummonTargets.canAttack(this, t)) {
            start(t);
        }
        if (phase != Phase.NONE) {
            tickHurl();
        }
        if (flying != null) {
            tickFlight();
        }
    }

    private void tickHurl() {
        LivingEntity t = hurlTarget;
        if (t == null || !t.isAlive() || t.level() != level()) {
            if (phase == Phase.HOLD) {
                ejectPassengers();
            }
            finish(0);
            return;
        }
        switch (phase) {
            case APPROACH -> {
                if (horizontalDist(t) <= AUTO_RANGE) {
                    beginReach();
                } else if (--phaseTimer <= 0) {
                    finish(0);
                } else if (phaseTimer % 10 == 0) {
                    getNavigation().moveTo(t, 1.2);
                }
            }
            case REACH -> {
                faceTowards(t);
                setDeltaMovement(0, getDeltaMovement().y, 0);
                if (--phaseTimer <= 0) {
                    resolveGrab(t);
                }
            }
            case HOLD -> {
                lockYaw();
                setDeltaMovement(0, getDeltaMovement().y, 0);
                if (--phaseTimer <= 0) {
                    release(t);
                }
            }
            default -> {
            }
        }
    }

    private void resolveGrab(LivingEntity t) {
        long now = level().getGameTime();
        HurlRules.Outcome o = horizontalDist(t) > REACH ? HurlRules.Outcome.MISS
                : HurlRules.decide(TargetFactsCollector.isMythic(t), TargetFactsCollector.isBoss(t), t instanceof Player,
                now, t.getPersistentData().getLong(HarpyEntity.IMMUNE_KEY), t.getBbWidth(), t.getBbHeight());
        if (o == HurlRules.Outcome.GRAB && (t.isPassenger() || t.isVehicle() || !t.startRiding(this, true))) {
            o = HurlRules.Outcome.MISS;
        }
        switch (o) {
            case GRAB -> {
                t.getPersistentData().putLong(HarpyEntity.IMMUNE_KEY, now + io.github.happysnappingturtle.mythicage.core.wild.GrabRules.IMMUNE_TICKS);
                hurlYaw = yBodyRot;
                phase = Phase.HOLD;
                phaseTimer = HOLD_TICKS;
                playOnce(MythAnimState.HURL, 16);
                playSound(SoundEvents.RAVAGER_ATTACK, 1.0F, 0.8F);
                hurlReadyAt = now + cooldownTicks;
            }
            case KNOCKBACK -> {
                Vec3 d = flat(t.position().subtract(position()));
                t.knockback(1.6, -d.x, -d.z);
                t.hurtMarked = true;
                playOnce(MythAnimState.ATTACK_SWING, 14);
                playSound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0F, 0.7F);
                finish(cooldownTicks);
            }
            case MISS -> {
                // D7：伸手抓空、挥一下，冷却只算 5 秒
                playOnce(MythAnimState.ATTACK_SWING, 14);
                playSound(SoundEvents.PLAYER_ATTACK_NODAMAGE, 1.0F, 0.6F);
                finish(missCooldownTicks);
            }
        }
    }

    private void finish(int cooldown) {
        phase = Phase.NONE;
        hurlTarget = null;
        if (cooldown > 0) {
            hurlReadyAt = level().getGameTime() + cooldown;
        }
    }

    /** 松手：按 {@link HurlRules#landing} 找落点，之后逐刻沿抛线摆放被扔者。 */
    private void release(LivingEntity t) {
        Vec3 from = t.position();
        t.stopRiding();
        double yaw = Math.toRadians(hurlYaw);
        Vec3 dir = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3 origin = position();
        Vec3[] spot = new Vec3[1];
        double d = HurlRules.landing(dist -> {
            Vec3 p = standable(t, origin.add(dir.scale(dist)));
            if (p != null && clearLine(origin.add(0, 2.0, 0), p.add(0, 0.5, 0))) {
                spot[0] = p;
                return true;
            }
            return false;
        });
        Vec3 to = d > 0 && spot[0] != null ? spot[0] : origin.add(dir.scale(1.5));
        flying = t;
        flyFrom = from;
        flyTo = to;
        flyTick = 0;
        flyTicks = ThrowRules.flightTicks(from.distanceTo(to));
        t.setDeltaMovement(Vec3.ZERO);
        playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.6F);
        finish(0);
    }

    private void tickFlight() {
        LivingEntity t = flying;
        if (t == null || !t.isAlive() || t.level() != level() || t.isPassenger()) {
            flying = null;
            return;
        }
        flyTick++;
        double f = Math.min(1.0, flyTick / (double) flyTicks);
        double dist = flyFrom.distanceTo(flyTo);
        Vec3 p = flyFrom.lerp(flyTo, f).add(0, ThrowRules.arc(dist, f), 0);
        t.setPos(p.x, p.y, p.z);
        t.setDeltaMovement(Vec3.ZERO);
        t.fallDistance = 0;
        t.hurtMarked = true;
        if (f >= 1.0) {
            land(t);
            flying = null;
        }
    }

    private void land(LivingEntity t) {
        t.hurt(damageSources().mobAttack(this), hitDamage);
        AABB area = t.getBoundingBox().inflate(HurlRules.SPLASH_RADIUS, 1.0, HurlRules.SPLASH_RADIUS);
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e != t && e != this && e.isAlive() && e.distanceToSqr(t) <= (HurlRules.SPLASH_RADIUS + 0.5) * (HurlRules.SPLASH_RADIUS + 0.5))) {
            if (SummonTargets.canAttack(this, e)) {
                e.hurt(damageSources().mobAttack(this), splashDamage);
            }
        }
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, t.getBlockStateOn()), t.getX(), t.getY() + 0.1, t.getZ(),
                    24, 0.8, 0.1, 0.8, 0.15);
        }
        t.playSound(SoundEvents.GENERIC_BIG_FALL, 1.2F, 0.8F);
    }

    /** 那一列里被扔者能站住的位置（碰撞箱不压方块、脚下 0.2 格内有地面）；从高 2 格往下找到低 4 格。 */
    @Nullable
    private Vec3 standable(LivingEntity t, Vec3 at) {
        EntityDimensions dims = t.getDimensions(t.getPose());
        int base = Mth.floor(at.y);
        for (int dy = 2; dy >= -4; dy--) {
            double y = base + dy;
            AABB box = dims.makeBoundingBox(at.x, y, at.z);
            if (level().noCollision(t, box) && !level().noCollision(t, box.move(0, -0.2, 0))) {
                return new Vec3(at.x, y, at.z);
            }
        }
        return null;
    }

    private boolean clearLine(Vec3 a, Vec3 b) {
        return level().clip(new ClipContext(a, b, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS;
    }

    private void faceTowards(Entity t) {
        getLookControl().setLookAt(t, 30.0F, 30.0F);
        Vec3 to = t.position().subtract(position());
        float yaw = (float) Math.toDegrees(Math.atan2(-to.x, to.z));
        setYRot(yaw);
        yBodyRot = yaw;
    }

    private void lockYaw() {
        setYRot(hurlYaw);
        yBodyRot = hurlYaw;
        yHeadRot = hurlYaw;
    }

    private double horizontalDist(Entity t) {
        double dx = t.getX() - getX();
        double dz = t.getZ() - getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static Vec3 flat(Vec3 v) {
        Vec3 f = v.multiply(1, 0, 1);
        return f.lengthSqr() > 1.0E-4 ? f.normalize() : new Vec3(0, 0, 1);
    }

    // ───────────── 被抓者：从身前低处举到头顶 ─────────────

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dims, float scale) {
        double f = Mth.clamp(animAge() / (double) LIFT_TICKS, 0.0, 1.0);
        // 身前低处（左手抓腰）→ 头顶（双手高举处，被抓者脚底在 3.9 格）
        double y = Mth.lerp(f, 0.9, 3.9);
        double z = Mth.lerp(f, 2.0, 0.3);
        double x = Mth.lerp(f, 0.5, 0.2);
        Vec3 local = new Vec3(x, y + passenger.getVehicleAttachmentPoint(this).y, z);
        return local.yRot(-yBodyRot * Mth.DEG_TO_RAD);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        return passenger.position();
    }

    @Override
    @Nullable
    public LivingEntity getControllingPassenger() {
        return null;
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    /** 抓投进行中占住移动与视线（走近阶段让寻路接着走）。 */
    private final class HurlLockGoal extends Goal {
        HurlLockGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return phase == Phase.REACH || phase == Phase.HOLD;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }
    }

    /** 被谁打就打谁（只打合法目标）。 */
    private final class Retaliate extends TargetGoal {
        private int seen;

        Retaliate() {
            super(SummonCyclopsEntity.this, false);
        }

        @Override
        public boolean canUse() {
            LivingEntity by = getLastHurtByMob();
            if (by == null || getLastHurtByMobTimestamp() == seen || !SummonTargets.canAttack(mob, by)) {
                return false;
            }
            seen = getLastHurtByMobTimestamp();
            targetMob = by;
            return true;
        }

        @Override
        public void start() {
            mob.setTarget(targetMob);
            super.start();
        }
    }

    // ───────────── 普攻、阵营、存档 ─────────────

    @Override
    public boolean doHurtTarget(Entity target) {
        playOnce(MythAnimState.ATTACK_SWING, 14);
        return super.doHurtTarget(target);
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        if (link.owner != null && (other.getUUID().equals(link.owner)
                || (other instanceof MythSummon s && link.owner.equals(s.link().owner)))) {
            return true;
        }
        Player owner = link.owner == null ? null : level().getPlayerByUUID(link.owner);
        return owner != null && owner.isAlliedTo(other) || super.isAlliedTo(other);
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (flying != null) {
            flying.fallDistance = 0;
            flying = null;
        }
        for (Entity p : new ArrayList<>(getPassengers())) {
            p.stopRiding();
        }
        super.remove(reason);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        link.save(tag);
        tag.putLong("HurlReadyAt", hurlReadyAt);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        link.load(tag);
        hurlReadyAt = tag.getLong("HurlReadyAt");
        phase = Phase.NONE;
    }
}
