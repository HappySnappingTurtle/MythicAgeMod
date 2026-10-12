package com.example.mythic_age.entity.ai.goal;

import com.example.mythic_age.sound.ModSounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;

public class MedusaStoneGazeGoal extends MythicSpecialAttackGoal {

    public MedusaStoneGazeGoal(HostileEntity medusa) {
        super(medusa, 130, 10.5F);
    }

    @Override
    protected void performAttack(LivingEntity target) {
        mob.playSound(ModSounds.ENTITY_MEDUSA_SPECIAL, 1.0F, 1.0F);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, 4), mob);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 120, 1), mob);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 120, 1), mob);
        target.damage(mob.getDamageSources().mobAttack(mob), 2.0F);

        if (mob.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.WHITE_ASH,
                target.getX(), target.getBodyY(0.55D), target.getZ(), 28, 0.55D, 0.4D, 0.55D, 0.02D);
            serverWorld.spawnParticles(ParticleTypes.ENCHANT,
                mob.getX(), mob.getEyeY(), mob.getZ(), 16, 0.25D, 0.25D, 0.25D, 0.04D);
        }
    }
}
