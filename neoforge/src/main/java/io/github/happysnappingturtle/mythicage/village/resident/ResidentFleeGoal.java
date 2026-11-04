package io.github.happysnappingturtle.mythicage.village.resident;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

/**
 * 逃跑回屋（T19 设计文档 §3.2，11 号「告警 / 逃跑优先」）：12 格内有敌对生物时放下工作（托管退回），快步走回床位。
 * 敌人离开 16 格并且过了 100 刻后结束。
 */
final class ResidentFleeGoal extends Goal {
    public static final double DETECT = 12;
    public static final double CLEAR = 16;

    private final ResidentEntity resident;
    private int calmTicks;
    private int scanIn;

    ResidentFleeGoal(ResidentEntity resident) {
        this.resident = resident;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    private boolean threatened(double r) {
        AABB box = resident.getBoundingBox().inflate(r);
        List<LivingEntity> foes = resident.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e instanceof Enemy && e.isAlive() && !e.isSpectator());
        return !foes.isEmpty();
    }

    @Override
    public boolean canUse() {
        // 士兵不逃（T20 §5.6）：有兵装的接敌，没有的护送
        if (resident.isSoldier() || --scanIn > 0) {
            return false;
        }
        scanIn = 10;
        return threatened(DETECT);
    }

    @Override
    public void start() {
        resident.abortJob(false);
        resident.wakeUp();
        calmTicks = 0;
        BlockPos home = resident.home();
        if (home != null) {
            resident.getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, ResidentEntity.FLEE_SPEED);
        }
    }

    @Override
    public boolean canContinueToUse() {
        return calmTicks < 100 && !resident.isSoldier();
    }

    @Override
    public void tick() {
        if (resident.tickCount % 10 == 0) {
            if (threatened(CLEAR)) {
                calmTicks = 0;
                BlockPos home = resident.home();
                if (home != null && resident.getNavigation().isDone() && !resident.reached(home)) {
                    resident.getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, ResidentEntity.FLEE_SPEED);
                }
            } else {
                calmTicks += 10;
            }
        }
    }

    @Override
    public void stop() {
        resident.getNavigation().stop();
    }
}
