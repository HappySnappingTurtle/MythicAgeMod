package com.example.mythic_age.entity;

import com.example.mythic_age.sound.ModSounds;
import java.util.List;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

public class HydraEntity extends GreekMythEntity {
    private static final int POISON_SWEEP_COOLDOWN = 120;

    public HydraEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 16;
    }

    public static DefaultAttributeContainer.Builder createHydraAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 82.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.21D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 7.0D)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 0.8D)
                .add(EntityAttributes.GENERIC_ARMOR, 6.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new MeleeAttackGoal(this, 0.9D, false));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.65D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 9.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient && this.isAlive()) {
            tryPoisonSweep();
        }
    }

    private void tryPoisonSweep() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || !this.canUseSpecial() || this.squaredDistanceTo(target) > 30.0D) {
            return;
        }

        this.playSpecialSound(ModSounds.ENTITY_HYDRA_SPECIAL);
        Box area = this.getBoundingBox().expand(4.2D, 1.0D, 4.2D);
        List<LivingEntity> victims = this.getWorld().getEntitiesByClass(
                LivingEntity.class,
                area,
                entity -> entity != this && entity.isAlive() && !(entity instanceof HydraEntity)
        );

        for (LivingEntity victim : victims) {
            if (this.canSee(victim)) {
                this.damageAndKnockback(victim, 5.0F, 0.75F);
                victim.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                        net.minecraft.entity.effect.StatusEffects.POISON,
                        100,
                        0
                ), this);
            }
        }

        this.spawnParticles(ParticleTypes.SNEEZE, this.getX(), this.getBodyY(0.55D), this.getZ(), 32, 1.25D, 0.05D);
        this.spawnParticles(ParticleTypes.SWEEP_ATTACK, this.getX(), this.getBodyY(0.45D), this.getZ(), 3, 0.8D, 0.0D);
        this.setSpecialCooldown(POISON_SWEEP_COOLDOWN);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ENTITY_HYDRA_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ENTITY_HYDRA_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ENTITY_HYDRA_DEATH;
    }

    @Override
    protected SoundEvent getAttackSound() {
        return ModSounds.ENTITY_HYDRA_ATTACK;
    }

    @Override
    protected void playStepSound(net.minecraft.util.math.BlockPos pos, net.minecraft.block.BlockState state) {
        this.playSound(ModSounds.ENTITY_HYDRA_STEP, 0.23F, 0.72F + this.random.nextFloat() * 0.12F);
    }

    @Override
    protected void dropLoot(DamageSource damageSource, boolean causedByPlayer) {
        super.dropLoot(damageSource, causedByPlayer);
        this.dropStack(new ItemStack(Items.ROTTEN_FLESH, 1 + this.random.nextInt(3)));
        if (this.random.nextFloat() < 0.55F) {
            this.dropStack(new ItemStack(Items.SLIME_BALL, 1 + this.random.nextInt(2)));
        }
        if (this.random.nextFloat() < 0.22F) {
            this.dropStack(new ItemStack(Items.POISONOUS_POTATO, 1));
        }
    }
}
