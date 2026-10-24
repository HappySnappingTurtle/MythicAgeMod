package io.github.happysnappingturtle.mythicage.village.resident;

import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** 执行调度器分到的工作单（T19 设计文档 §3.2）：走到 → 动作 → 结算。没有工作单时不占用。 */
final class ResidentJobGoal extends Goal {
    private final ResidentEntity resident;

    ResidentJobGoal(ResidentEntity resident) {
        this.resident = resident;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return resident.job().isPresent();
    }

    @Override
    public boolean canContinueToUse() {
        return resident.job().isPresent();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        resident.tickJob();
    }
}
