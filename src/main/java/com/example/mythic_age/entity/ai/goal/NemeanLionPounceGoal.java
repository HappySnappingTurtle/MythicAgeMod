package com.example.mythic_age.entity.ai.goal;

import com.example.mythic_age.entity.NemeanLionEntity;
import com.example.mythic_age.sound.ModSounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;

public class NemeanLionPounceGoal extends MythicSpecialAttackGoal {

    private static final int COOLDOWN = 100;
    private static final float RANGE = 10.0F;
    private static final double MIN_DISTANCE_SQ = 10.0D;

    public NemeanLionPounceGoal(NemeanLionEntity lion) {
        super(lion, COOLDOWN, RANGE);
    }

    @Override
    public boolean canStart() {
        if (!super.canStart()) {
            return false;
        }
        LivingEntity target = mob.getTarget();
        return target != null && mob.squaredDistanceTo(target) >= MIN_DISTANCE_SQ;
    }

    @Override
    protected void performAttack(LivingEntity target) {
        mob.playSound(ModSounds.ENTITY_NEMEAN_LION_SPECIAL, 1.15F, 0.85F + mob.getRandom().nextFloat() * 0.25F);

        if (target.damage(mob.getDamageSources().mobAttack(mob), 7.0F)) {
            target.takeKnockback(1.35F,
                mob.getX() - target.getX(),
                mob.getZ() - target.getZ());
        }
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 70, 1), mob);

        if (mob.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.CLOUD,
                mob.getX(), mob.getBodyY(0.35D), mob.getZ(), 18, 0.55D, 0.275D, 0.55D, 0.08D);
            serverWorld.spawnParticles(ParticleTypes.CRIT,
                target.getX(), target.getBodyY(0.55D), target.getZ(), 14, 0.45D, 0.225D, 0.45D, 0.08D);
        }
    }
}
