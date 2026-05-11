package com.example.mythic_age.entity.ai.goal;

import com.example.mythic_age.entity.MinotaurEntity;
import com.example.mythic_age.sound.ModSounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import java.util.EnumSet;

public class MinotaurChargeAttackGoal extends Goal {
    private final MinotaurEntity minotaur;
    private final double speed;
    private LivingEntity target;
    private int chargeCooldown;
    private int chargeTicks;
    private static final int MAX_CHARGE_TICKS = 20; // How long the charge lasts
    private static final int CHARGE_COOLDOWN_TICKS = 100; // 5 seconds cooldown

    public MinotaurChargeAttackGoal(MinotaurEntity minotaur, double speed) {
        this.minotaur = minotaur;
        this.speed = speed;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        this.chargeCooldown = 0;
    }

    @Override
    public boolean canStart() {
        this.target = this.minotaur.getTarget();
        if (this.target == null || !this.target.isAlive()) {
            return false;
        }
        if (this.chargeCooldown > 0) {
            this.chargeCooldown--;
            return false;
        }
        double distanceToTarget = this.minotaur.squaredDistanceTo(this.target);
        // Start charge if target is within a certain range (e.g., 8-16 blocks) and not too close
        return distanceToTarget > 16.0D && distanceToTarget < 256.0D;
    }

    @Override
    public boolean shouldContinue() {
        return this.target != null && this.target.isAlive() && this.chargeTicks < MAX_CHARGE_TICKS && !this.minotaur.getNavigation().isIdle();
    }

    @Override
    public void start() {
        this.chargeTicks = 0;
        this.minotaur.getNavigation().startMovingTo(this.target, this.speed);
        this.minotaur.playSound(
            ModSounds.ENTITY_MINOTAUR_CHARGE,
            1.2F,
            0.85F + this.minotaur.getRandom().nextFloat() * 0.2F
        );
    }

    @Override
    public void stop() {
        this.chargeCooldown = CHARGE_COOLDOWN_TICKS;
        this.minotaur.getNavigation().stop();
    }

    @Override
    public void tick() {
        this.minotaur.getLookControl().lookAt(this.target, 30.0F, 30.0F);
        this.chargeTicks++;
        if (this.minotaur.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(
                ParticleTypes.CLOUD,
                this.minotaur.getX(),
                this.minotaur.getY() + 0.25D,
                this.minotaur.getZ(),
                4,
                0.28D,
                0.05D,
                0.28D,
                0.02D
            );
            if (this.chargeTicks % 4 == 0) {
                serverWorld.spawnParticles(
                    ParticleTypes.CRIT,
                    this.minotaur.getX(),
                    this.minotaur.getY() + 1.2D,
                    this.minotaur.getZ(),
                    3,
                    0.35D,
                    0.25D,
                    0.35D,
                    0.04D
                );
            }
        }
        if (this.minotaur.getBoundingBox().intersects(this.target.getBoundingBox())) {
            this.minotaur.tryAttack(this.target); // Perform attack
            this.target.takeKnockback(
                1.25F,
                this.minotaur.getX() - this.target.getX(),
                this.minotaur.getZ() - this.target.getZ()
            );
            this.chargeTicks = MAX_CHARGE_TICKS; // End charge after successful hit
        }
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }
}
