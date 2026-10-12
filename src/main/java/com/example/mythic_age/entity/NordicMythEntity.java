package com.example.mythic_age.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/**
 * Shared behavior for Nordic myth creatures.
 */
public abstract class NordicMythEntity extends HostileEntity {
    private int specialCooldownTicks;

    protected NordicMythEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.specialCooldownTicks > 0) {
            this.specialCooldownTicks--;
        }
    }

    @Override
    public boolean tryAttack(Entity target) {
        boolean hit = super.tryAttack(target);
        if (hit) {
            SoundEvent attackSound = getAttackSound();
            if (attackSound != null) {
                this.playSound(attackSound, 1.0F, 0.82F + this.random.nextFloat() * 0.18F);
            }
            this.swingHand(Hand.MAIN_HAND);
        }
        return hit;
    }

    protected abstract SoundEvent getAttackSound();

    protected boolean canUseSpecial() {
        return this.specialCooldownTicks <= 0;
    }

    protected void setSpecialCooldown(int ticks) {
        this.specialCooldownTicks = ticks;
    }

    protected void playSpecialSound(SoundEvent sound) {
        if (sound != null) {
            this.playSound(sound, 1.18F, 0.78F + this.random.nextFloat() * 0.2F);
        }
        this.swingHand(Hand.MAIN_HAND);
    }

    protected void damageAndKnockback(LivingEntity target, float damage, float knockback) {
        if (target.damage(this.getDamageSources().mobAttack(this), damage)) {
            target.takeKnockback(
                    knockback,
                    this.getX() - target.getX(),
                    this.getZ() - target.getZ()
            );
        }
    }

    protected void spawnParticles(ParticleEffect particle, double x, double y, double z, int count, double spread, double speed) {
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(particle, x, y, z, count, spread, spread * 0.5D, spread, speed);
        }
    }
}
