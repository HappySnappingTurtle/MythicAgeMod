package io.github.happysnappingturtle.mythicage.summon.greek;

import io.github.happysnappingturtle.mythicage.entity.MinotaurEntity;
import io.github.happysnappingturtle.mythicage.entity.anim.MythAnimState;
import io.github.happysnappingturtle.mythicage.power.TargetFactsCollector;
import io.github.happysnappingturtle.mythicage.summon.MythSummon;
import io.github.happysnappingturtle.mythicage.summon.SummonGoals;
import io.github.happysnappingturtle.mythicage.summon.SummonLink;
import io.github.happysnappingturtle.mythicage.summon.SummonService;
import io.github.happysnappingturtle.mythicage.summon.SummonTargets;
import io.github.happysnappingturtle.mythicage.summon.SummonUnits;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * 召唤版弥诺陶洛斯（T14 设计文档 §6.1）：复用 T7 的模型与动画框架；近战挥斧，特殊攻击「顶飞」——
 * 刨地 0.8 秒 → 冲向目标 → 顶起约 4 格并击退，被顶的生物 3 秒内失去仇恨；Boss 与玩家只击退。
 */
public class SummonMinotaurEntity extends MinotaurEntity implements MythSummon {
    private final SummonLink link = new SummonLink();

    // 顶飞：0 无、1 刨地、2 冲顶
    private int tossPhase;
    private int tossTimer;
    private LivingEntity tossTarget;
    private long tossReadyAt;

    private int windupTicks = 16;
    private int cooldownTicks = 240;
    private double tossLift = 0.9;
    private double tossPush = 0.8;
    private int disengageTicks = 60;

    public SummonMinotaurEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createSummonAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 80.0)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.ATTACK_KNOCKBACK, 0.4)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new TossLockGoal());
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, true));
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
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(s.attr("max_health", 80));
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(s.attr("attack_damage", 8));
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(s.attr("movement_speed", 0.3));
        if (s.attributes().containsKey("armor")) {
            getAttribute(Attributes.ARMOR).setBaseValue(s.attr("armor", 0));
        }
        setHealth(getMaxHealth());
        windupTicks = s.specialInt("windup_ticks", windupTicks);
        cooldownTicks = s.specialInt("cooldown_ticks", cooldownTicks);
        tossLift = s.specialDouble("lift", tossLift);
        tossPush = s.specialDouble("push", tossPush);
        disengageTicks = s.specialInt("disengage_ticks", disengageTicks);
    }

    @Override
    public boolean requestSpecial(LivingEntity target) {
        if (tossPhase != 0 || level().getGameTime() < tossReadyAt || distanceToSqr(target) > 10 * 10) {
            return false;
        }
        startToss(target);
        return true;
    }

    public boolean tossing() {
        return tossPhase != 0;
    }

    public long tossReadyAt() {
        return tossReadyAt;
    }

    // ───────────── 顶飞 ─────────────

    private void startToss(LivingEntity target) {
        tossTarget = target;
        tossPhase = 1;
        tossTimer = windupTicks;
        tossReadyAt = level().getGameTime() + cooldownTicks;
        getNavigation().stop();
        setTarget(target);
        playOnce(MythAnimState.PAW, windupTicks + 4);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        LivingEntity t = getTarget();
        if (tossPhase == 0 && t != null && t.isAlive() && level().getGameTime() >= tossReadyAt
                && distanceToSqr(t) < 4.5 * 4.5 && SummonTargets.canAttack(this, t)) {
            startToss(t);
        }
        if (tossPhase != 0) {
            tickToss();
        }
    }

    private void tickToss() {
        if (tossTarget == null || !tossTarget.isAlive() || tossTarget.level() != level()) {
            tossPhase = 0;
            return;
        }
        getLookControl().setLookAt(tossTarget, 30.0F, 30.0F);
        Vec3 to = tossTarget.position().subtract(position()).multiply(1, 0, 1);
        float yaw = (float) Math.toDegrees(Math.atan2(-to.x, to.z));
        setYRot(yaw);
        yBodyRot = yaw;
        if (tossPhase == 1) {
            setDeltaMovement(0, getDeltaMovement().y, 0);
            if (tossTimer % 4 == 0 && level() instanceof ServerLevel sl) {
                sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, getBlockStateOn()), getX(), getY() + 0.1, getZ(),
                        6, 0.4, 0.05, 0.4, 0.1);
            }
            if (--tossTimer <= 0) {
                tossPhase = 2;
                tossTimer = 12;
            }
            return;
        }
        // 冲顶：最多 12 刻冲向目标，进入 2.6 格就顶
        if (distanceToSqr(tossTarget) < 2.6 * 2.6) {
            hitToss(tossTarget);
            tossPhase = 0;
            return;
        }
        Vec3 dir = to.lengthSqr() > 1.0E-4 ? to.normalize() : Vec3.ZERO;
        setDeltaMovement(dir.x * 0.6, getDeltaMovement().y, dir.z * 0.6);
        if (--tossTimer <= 0) {
            tossPhase = 0;
        }
    }

    private void hitToss(LivingEntity target) {
        playOnce(MythAnimState.TOSS, 12);
        target.hurt(damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
        Vec3 dir = target.position().subtract(position()).multiply(1, 0, 1);
        dir = dir.lengthSqr() > 1.0E-4 ? dir.normalize() : getLookAngle().multiply(1, 0, 1).normalize();
        boolean boss = TargetFactsCollector.isBoss(target);
        if (boss || target instanceof Player) {
            // Boss 与玩家（PvP）只击退：不顶飞、不清仇恨（07 号边界用例、§9 D6）
            target.knockback(1.2, -dir.x, -dir.z);
        } else {
            target.setDeltaMovement(dir.x * tossPush, tossLift, dir.z * tossPush);
            target.hurtMarked = true;
            target.hasImpulse = true;
            if (target instanceof Mob m) {
                m.setTarget(null);
                SummonService.disengage(m, disengageTicks);
            }
        }
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1, target.getZ(), 12, 0.4, 0.4, 0.4, 0.2);
        }
    }

    /** 顶飞进行中占住移动与视线。 */
    private final class TossLockGoal extends Goal {
        TossLockGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return tossPhase != 0;
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
            super(SummonMinotaurEntity.this, false);
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
        playOnce(MythAnimState.ATTACK_SWING, 10);
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
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        link.save(tag);
        tag.putLong("TossReadyAt", tossReadyAt);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        link.load(tag);
        tossReadyAt = tag.getLong("TossReadyAt");
        tossPhase = 0;
    }
}
