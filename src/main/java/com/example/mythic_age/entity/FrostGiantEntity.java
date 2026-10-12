package com.example.mythic_age.entity;

import com.example.mythic_age.sound.ModSounds;
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
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

public class FrostGiantEntity extends NordicMythEntity {
    private static final int FROST_QUAKE_COOLDOWN = 115;

    public FrostGiantEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 18;
    }

    public static DefaultAttributeContainer.Builder createFrostGiantAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 72.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.22D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 10.0D)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 1.6D)
                .add(EntityAttributes.GENERIC_ARMOR, 7.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 36.0D);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 0.95D, false));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.58D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 12.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient && this.isAlive()) {
            tryFrostQuake();
        }
    }

    private void tryFrostQuake() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || !this.canUseSpecial() || !this.canSee(target)) {
            return;
        }

        double distance = this.squaredDistanceTo(target);
        if (distance < 9.0D || distance > 81.0D) {
            return;
        }

        this.getLookControl().lookAt(target, 28.0F, 28.0F);
        this.playSpecialSound(ModSounds.ENTITY_FROST_GIANT_SPECIAL);
        this.damageAndKnockback(target, 7.0F, 1.25F);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 1), this);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 80, 0), this);
        this.spawnParticles(ParticleTypes.SNOWFLAKE, this.getX(), this.getBodyY(0.25D), this.getZ(), 26, 0.9D, 0.06D);
        this.spawnParticles(ParticleTypes.CRIT, target.getX(), target.getBodyY(0.55D), target.getZ(), 16, 0.45D, 0.08D);
        this.setSpecialCooldown(FROST_QUAKE_COOLDOWN);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ENTITY_FROST_GIANT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ENTITY_FROST_GIANT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ENTITY_FROST_GIANT_DEATH;
    }

    @Override
    protected SoundEvent getAttackSound() {
        return ModSounds.ENTITY_FROST_GIANT_ATTACK;
    }

    @Override
    protected void playStepSound(net.minecraft.util.math.BlockPos pos, net.minecraft.block.BlockState state) {
        this.playSound(SoundEvents.ENTITY_IRON_GOLEM_STEP, 0.24F, 0.72F + this.random.nextFloat() * 0.1F);
    }

    @Override
    protected void dropLoot(DamageSource damageSource, boolean causedByPlayer) {
        super.dropLoot(damageSource, causedByPlayer);
        this.dropStack(new ItemStack(Items.BLUE_ICE, 1));
        if (this.random.nextFloat() < 0.45F) {
            this.dropStack(new ItemStack(Items.BONE, 1 + this.random.nextInt(3)));
        }
        if (this.random.nextFloat() < 0.28F) {
            this.dropStack(new ItemStack(Items.IRON_NUGGET, 2 + this.random.nextInt(4)));
        }
    }
}
