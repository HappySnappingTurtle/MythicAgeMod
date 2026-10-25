package io.github.happysnappingturtle.mythicage.village.resident;

import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * 士兵 AI 的目标（T20 设计文档 §5）：转岗的居民值勤、补觉、接敌、撤退、换装时接管移动与朝向；不值勤又没有敌人时让出，
 * 由调度器像普通居民一样派单。
 */
final class SoldierGoal extends Goal {
    private final ResidentEntity resident;

    SoldierGoal(ResidentEntity resident) {
        this.resident = resident;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return resident.isSoldier() && resident.soldierBrain().wants();
    }

    @Override
    public boolean canContinueToUse() {
        return resident.isSoldier() && resident.soldierBrain().busy();
    }

    @Override
    public void start() {
        if (resident.job().isPresent()) {
            resident.abortJob(false);
        }
    }

    @Override
    public void stop() {
        resident.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        long t0 = System.nanoTime();
        resident.soldierBrain().tick();
        resident.village().ifPresent(v -> io.github.happysnappingturtle.mythicage.village.work.VillageScheduler.addPerf(v,
                System.nanoTime() - t0));
    }
}
