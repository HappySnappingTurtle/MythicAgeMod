package com.example.mythic_age.entity.ai.goal;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Base class for mythic creature signature attack goals.
 * Handles cooldown management, range checks, and the standard canStart/stop lifecycle.
 */
public abstract class MythicSpecialAttackGoal extends Goal {

    protected final HostileEntity mob;
    protected final int cooldownTicks;
    protected final float range;
    protected int cooldownRemaining;

    protected MythicSpecialAttackGoal(HostileEntity mob, int cooldownTicks, float range) {
        this.mob = mob;
        this.cooldownTicks = cooldownTicks;
        this.range = range;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (cooldownRemaining > 0) {
            cooldownRemaining--;
            return false;
        }
        LivingEntity target = mob.getTarget();
        return target != null
            && target.isAlive()
            && mob.squaredDistanceTo(target) <= (double) range * range
            && mob.canSee(target);
    }

    @Override
    public void start() {
        LivingEntity target = mob.getTarget();
        if (target != null) {
            mob.getLookControl().lookAt(target, 30.0F, 30.0F);
            performAttack(target);
        }
        cooldownRemaining = cooldownTicks;
    }

    @Override
    public boolean shouldContinue() {
        return false;
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    protected abstract void performAttack(LivingEntity target);
}
