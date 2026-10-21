package io.github.happysnappingturtle.mythicage.summon;

import io.github.happysnappingturtle.mythicage.core.summon.SummonMode;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.EnumSet;

/**
 * 召唤单位的通用 AI 目标（T14 设计文档 §4.1）：跟随、驻守回位、三种索敌。各单位在自己的 registerGoals 里按优先级挂上，
 * 普攻与特殊攻击由单位自己实现。
 */
public final class SummonGoals {
    private SummonGoals() {
    }

    private static SummonMode mode(net.minecraft.world.entity.Mob mob) {
        SummonMode m = ((MythSummon) mob).link().mode;
        // 非战斗单位的「自由攻击」视同跟随
        return m == SummonMode.FREE && !((MythSummon) mob).combatUnit() ? SummonMode.FOLLOW : m;
    }

    /** 跟随：离主人 &gt; 8 格开始走近，&lt; 3 格停；&gt; 24 格且同维度直接瞬移到主人身边。 */
    public static final class Follow extends Goal {
        private final PathfinderMob mob;
        private final double speed;
        private ServerPlayer owner;
        private int repath;

        public Follow(PathfinderMob mob, double speed) {
            this.mob = mob;
            this.speed = speed;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (mode(mob) != SummonMode.FOLLOW || mob.getTarget() != null) {
                return false;
            }
            owner = SummonTargets.owner(mob);
            return owner != null && owner.level() == mob.level() && !owner.isSpectator() && mob.distanceToSqr(owner) > 64;
        }

        @Override
        public boolean canContinueToUse() {
            return mode(mob) == SummonMode.FOLLOW && owner != null && owner.isAlive() && owner.level() == mob.level()
                    && mob.getTarget() == null && mob.distanceToSqr(owner) > 9;
        }

        @Override
        public void start() {
            repath = 0;
        }

        @Override
        public void stop() {
            owner = null;
            mob.getNavigation().stop();
        }

        @Override
        public void tick() {
            mob.getLookControl().setLookAt(owner, 10.0F, mob.getMaxHeadXRot());
            if (mob.distanceToSqr(owner) > (double) SummonMode.FOLLOW_TELEPORT * SummonMode.FOLLOW_TELEPORT) {
                SummonService.teleportNear(mob, owner);
                return;
            }
            if (--repath <= 0) {
                repath = 10;
                mob.getNavigation().moveTo(owner, speed);
            }
        }
    }

    /** 驻守：没有目标时回到驻守点 12 格内；追敌出了 24 格就放弃目标回来。 */
    public static final class Guard extends Goal {
        private final PathfinderMob mob;
        private final double speed;

        public Guard(PathfinderMob mob, double speed) {
            this.mob = mob;
            this.speed = speed;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        private BlockPos post() {
            return ((MythSummon) mob).link().guard;
        }

        @Override
        public boolean canUse() {
            if (mode(mob) != SummonMode.GUARD || post() == null) {
                return false;
            }
            double d = mob.blockPosition().distSqr(post());
            if (mob.getTarget() != null && d > (double) SummonMode.GUARD_LEASH * SummonMode.GUARD_LEASH) {
                mob.setTarget(null);
                ((MythSummon) mob).link().forcedTarget = null;
            }
            return mob.getTarget() == null && d > (double) SummonMode.GUARD_RADIUS * SummonMode.GUARD_RADIUS / 4;
        }

        @Override
        public boolean canContinueToUse() {
            return mode(mob) == SummonMode.GUARD && post() != null && mob.getTarget() == null && !mob.getNavigation().isDone();
        }

        @Override
        public void start() {
            BlockPos p = post();
            mob.getNavigation().moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, speed);
        }
    }

    /** 传令杖指定的攻击目标（最高优先级）。 */
    public static final class Commanded extends TargetGoal {
        public Commanded(PathfinderMob mob) {
            super(mob, false);
        }

        @Override
        public boolean canUse() {
            var id = ((MythSummon) mob).link().forcedTarget;
            if (id == null || !(mob.level() instanceof ServerLevel sl)) {
                return false;
            }
            Entity e = sl.getEntity(id);
            if (e instanceof LivingEntity le && SummonTargets.canAttack(mob, le) && mob.distanceToSqr(le) < 48 * 48) {
                targetMob = le;
                return true;
            }
            ((MythSummon) mob).link().forcedTarget = null;
            return false;
        }

        @Override
        public void start() {
            mob.setTarget(targetMob);
            super.start();
        }

        @Override
        public boolean canContinueToUse() {
            boolean ok = super.canContinueToUse();
            if (!ok) {
                ((MythSummon) mob).link().forcedTarget = null;
            }
            return ok;
        }
    }

    /** 保护主人：主人刚被谁打、刚打了谁（跟随与自由模式）。 */
    public static final class DefendOwner extends TargetGoal {
        private int lastHurtBy;
        private int lastHurt;

        public DefendOwner(PathfinderMob mob) {
            super(mob, false);
        }

        @Override
        public boolean canUse() {
            SummonMode m = mode(mob);
            if (m == SummonMode.GUARD || !((MythSummon) mob).combatUnit()) {
                return false;
            }
            ServerPlayer owner = SummonTargets.owner(mob);
            if (owner == null || owner.level() != mob.level()) {
                return false;
            }
            LivingEntity by = owner.getLastHurtByMob();
            if (by != null && owner.getLastHurtByMobTimestamp() != lastHurtBy && SummonTargets.canAttack(mob, by)) {
                lastHurtBy = owner.getLastHurtByMobTimestamp();
                targetMob = by;
                return true;
            }
            LivingEntity hit = owner.getLastHurtMob();
            if (hit != null && owner.getLastHurtMobTimestamp() != lastHurt && SummonTargets.canAttack(mob, hit)) {
                lastHurt = owner.getLastHurtMobTimestamp();
                targetMob = hit;
                return true;
            }
            return false;
        }

        @Override
        public void start() {
            mob.setTarget(targetMob);
            super.start();
        }
    }

    /** 主动索敌：按模式的半径找最近的合法目标；驻守时只看驻守点附近。每 10 刻找一次。 */
    public static final class NearestEnemy extends TargetGoal {
        private int cooldown;

        public NearestEnemy(PathfinderMob mob) {
            super(mob, false);
        }

        @Override
        public boolean canUse() {
            if (--cooldown > 0 || !((MythSummon) mob).combatUnit()) {
                return false;
            }
            cooldown = 10;
            SummonMode m = mode(mob);
            double r = m.aggroRange();
            BlockPos center = m == SummonMode.GUARD && ((MythSummon) mob).link().guard != null
                    ? ((MythSummon) mob).link().guard : mob.blockPosition();
            AABB box = new AABB(center).inflate(r, 6, r);
            targetMob = mob.level().getEntitiesOfClass(LivingEntity.class, box,
                            e -> e.distanceToSqr(center.getX() + 0.5, center.getY(), center.getZ() + 0.5) <= r * r
                                    && SummonTargets.canAttack(mob, e) && mob.getSensing().hasLineOfSight(e))
                    .stream().min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
            return targetMob != null;
        }

        @Override
        public void start() {
            mob.setTarget(targetMob);
            super.start();
        }
    }
}
