package com.example.mythic_age.entity.ai.goal;

import com.example.mythic_age.entity.FenrirWolfEntity;
import com.example.mythic_age.sound.ModSounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;

import java.util.List;

public class FenrirHowlBuffGoal extends MythicSpecialAttackGoal {

    private static final int COOLDOWN = 120;
    private static final float RANGE = 9.0F;

    public FenrirHowlBuffGoal(FenrirWolfEntity fenrir) {
        super(fenrir, COOLDOWN, RANGE);
    }

    @Override
    protected void performAttack(LivingEntity target) {
        mob.playSound(ModSounds.ENTITY_FENRIR_WOLF_SPECIAL, 1.18F, 0.78F + mob.getRandom().nextFloat() * 0.2F);

        Box area = mob.getBoundingBox().expand(4.2D, 1.0D, 4.2D);
        List<LivingEntity> victims = mob.getWorld().getEntitiesByClass(
            LivingEntity.class, area,
            entity -> entity != mob && entity.isAlive() && !(entity instanceof FenrirWolfEntity));

        for (LivingEntity victim : victims) {
            victim.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 90, 1), mob);
            victim.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 90, 0), mob);
            if (mob.squaredDistanceTo(victim) < 16.0D) {
                if (victim.damage(mob.getDamageSources().mobAttack(mob), 3.5F)) {
                    victim.takeKnockback(0.8F,
                        mob.getX() - victim.getX(),
                        mob.getZ() - victim.getZ());
                }
            }
        }

        mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 80, 0), mob);

        if (mob.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.SOUL,
                mob.getX(), mob.getBodyY(0.75D), mob.getZ(), 34, 1.0D, 0.5D, 1.0D, 0.06D);
            serverWorld.spawnParticles(ParticleTypes.SMOKE,
                mob.getX(), mob.getBodyY(0.35D), mob.getZ(), 22, 0.9D, 0.45D, 0.9D, 0.04D);
            serverWorld.spawnParticles(ParticleTypes.CRIT,
                target.getX(), target.getBodyY(0.55D), target.getZ(), 16, 0.45D, 0.225D, 0.45D, 0.08D);
        }
    }
}
