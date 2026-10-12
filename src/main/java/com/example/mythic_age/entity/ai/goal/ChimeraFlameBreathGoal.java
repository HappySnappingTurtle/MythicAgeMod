package com.example.mythic_age.entity.ai.goal;

import com.example.mythic_age.entity.ChimeraEntity;
import com.example.mythic_age.particle.ModParticles;
import com.example.mythic_age.sound.ModSounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;

import java.util.List;

public class ChimeraFlameBreathGoal extends MythicSpecialAttackGoal {

    private static final int COOLDOWN = 125;
    private static final float RANGE = 9.0F;

    public ChimeraFlameBreathGoal(ChimeraEntity chimera) {
        super(chimera, COOLDOWN, RANGE);
    }

    @Override
    protected void performAttack(LivingEntity target) {
        mob.playSound(ModSounds.ENTITY_CHIMERA_SPECIAL, 1.15F, 0.85F + mob.getRandom().nextFloat() * 0.25F);

        Box area = mob.getBoundingBox().expand(4.5D, 1.2D, 4.5D);
        List<LivingEntity> victims = mob.getWorld().getEntitiesByClass(
            LivingEntity.class, area,
            entity -> entity != mob && entity.isAlive() && !(entity instanceof ChimeraEntity));

        for (LivingEntity victim : victims) {
            if (mob.canSee(victim)) {
                victim.setOnFireFor(5);
                victim.damage(mob.getDamageSources().mobAttack(mob), 6.0F);
            }
        }

        if (mob.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ModParticles.MYTHIC_FLAME,
                target.getX(), target.getBodyY(0.45D), target.getZ(), 36, 0.75D, 0.375D, 0.75D, 0.08D);
            serverWorld.spawnParticles(ParticleTypes.FLAME,
                target.getX(), target.getBodyY(0.45D), target.getZ(), 18, 0.65D, 0.325D, 0.65D, 0.06D);
            serverWorld.spawnParticles(ParticleTypes.SMOKE,
                mob.getX(), mob.getEyeY(), mob.getZ(), 18, 0.45D, 0.225D, 0.45D, 0.05D);
        }
    }
}
