package com.example.mythic_age.entity.ai.goal;

import com.example.mythic_age.entity.HydraEntity;
import com.example.mythic_age.sound.ModSounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;

import java.util.List;

public class HydraPoisonSweepGoal extends MythicSpecialAttackGoal {

    private static final int COOLDOWN = 120;
    private static final float RANGE = 5.5F;

    public HydraPoisonSweepGoal(HydraEntity hydra) {
        super(hydra, COOLDOWN, RANGE);
    }

    @Override
    protected void performAttack(LivingEntity target) {
        mob.playSound(ModSounds.ENTITY_HYDRA_SPECIAL, 1.0F, 1.0F);

        Box area = mob.getBoundingBox().expand(4.2D, 1.0D, 4.2D);
        List<LivingEntity> victims = mob.getWorld().getEntitiesByClass(
            LivingEntity.class, area,
            entity -> entity != mob && entity.isAlive() && !(entity instanceof HydraEntity));

        for (LivingEntity victim : victims) {
            if (mob.canSee(victim)) {
                victim.damage(mob.getDamageSources().mobAttack(mob), 5.0F);
                victim.takeKnockback(0.75F, mob.getX() - victim.getX(), mob.getZ() - victim.getZ());
                victim.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 100, 0), mob);
            }
        }

        if (mob.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.SNEEZE,
                mob.getX(), mob.getBodyY(0.55D), mob.getZ(), 32, 1.25D, 0.5D, 1.25D, 0.05D);
            serverWorld.spawnParticles(ParticleTypes.SWEEP_ATTACK,
                mob.getX(), mob.getBodyY(0.45D), mob.getZ(), 3, 0.8D, 0.2D, 0.8D, 0.0D);
        }
    }
}
