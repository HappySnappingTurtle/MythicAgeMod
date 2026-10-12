package com.example.mythic_age.entity.ai.goal;

import com.example.mythic_age.entity.AzureDragonEntity;
import com.example.mythic_age.sound.ModSounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;

public class AzureDragonBreathGoal extends MythicSpecialAttackGoal {

    private static final int COOLDOWN = 95;
    private static final float RANGE = 11.0F;
    private static final double MIN_DISTANCE_SQ = 9.0D;

    public AzureDragonBreathGoal(AzureDragonEntity dragon) {
        super(dragon, COOLDOWN, RANGE);
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
        mob.playSound(ModSounds.ENTITY_AZURE_DRAGON_SPECIAL, 1.15F, 0.92F + mob.getRandom().nextFloat() * 0.2F);

        target.damage(mob.getDamageSources().mobAttack(mob), 6.0F);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 28, 0), mob);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 70, 0), mob);

        if (mob.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.DRAGON_BREATH,
                target.getX(), target.getBodyY(0.65D), target.getZ(), 26, 0.55D, 0.275D, 0.55D, 0.05D);
            serverWorld.spawnParticles(ParticleTypes.ENCHANT,
                mob.getX(), mob.getBodyY(0.5D), mob.getZ(), 18, 0.45D, 0.225D, 0.45D, 0.05D);
        }
    }
}
