package com.example.mythic_age.entity.ai.goal;

import com.example.mythic_age.sound.ModSounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;

import java.util.EnumSet;

public class CyclopsStoneThrowGoal extends MythicSpecialAttackGoal {

    private static final float MIN_RANGE = 4.2F;
    private static final float MAX_RANGE = 14.0F;

    public CyclopsStoneThrowGoal(HostileEntity cyclops) {
        super(cyclops, 140, MAX_RANGE);
    }

    @Override
    public boolean canStart() {
        if (cooldownRemaining > 0) {
            cooldownRemaining--;
            return false;
        }
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive() || !mob.canSee(target)) return false;
        double dist = mob.distanceTo(target);
        return dist >= MIN_RANGE && dist <= MAX_RANGE;
    }

    @Override
    protected void performAttack(LivingEntity target) {
        mob.playSound(ModSounds.ENTITY_CYCLOPS_SPECIAL, 1.0F, 1.0F);
        target.damage(mob.getDamageSources().mobAttack(mob), 8.0F);
        target.takeKnockback(1.6F, mob.getX() - target.getX(), mob.getZ() - target.getZ());

        if (mob.getWorld() instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.POOF,
                target.getX(), target.getY() + 0.3D, target.getZ(), 18, 0.55D, 0.3D, 0.55D, 0.08D);
            sw.spawnParticles(ParticleTypes.CRIT,
                target.getX(), target.getBodyY(0.5D), target.getZ(), 12, 0.45D, 0.3D, 0.45D, 0.12D);
        }
    }
}
