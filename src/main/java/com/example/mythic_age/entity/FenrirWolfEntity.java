package com.example.mythic_age.entity;

import com.example.mythic_age.entity.ai.goal.FenrirHowlBuffGoal;
import com.example.mythic_age.sound.ModSounds;
import net.minecraft.entity.EntityType;
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
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

public class FenrirWolfEntity extends NordicMythEntity {

    public FenrirWolfEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 18;
    }

    public static DefaultAttributeContainer.Builder createFenrirWolfAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 66.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.31D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8.0D)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 1.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 5.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 38.0D);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new FenrirHowlBuffGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.08D, false));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.62D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 14.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient && this.isAlive()) {
            // Dread howl is now handled by FenrirHowlBuffGoal registered in initGoals()
            if (this.age % 48 == 0) {
                this.spawnParticles(ParticleTypes.SOUL, this.getX(), this.getBodyY(0.55D), this.getZ(), 8, 0.5D, 0.025D);
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ENTITY_FENRIR_WOLF_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ENTITY_FENRIR_WOLF_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ENTITY_FENRIR_WOLF_DEATH;
    }

    @Override
    protected SoundEvent getAttackSound() {
        return ModSounds.ENTITY_FENRIR_WOLF_ATTACK;
    }

    @Override
    protected void playStepSound(net.minecraft.util.math.BlockPos pos, net.minecraft.block.BlockState state) {
        this.playSound(SoundEvents.ENTITY_WOLF_STEP, 0.26F, 0.72F + this.random.nextFloat() * 0.1F);
    }

    @Override
    protected void dropLoot(DamageSource damageSource, boolean causedByPlayer) {
        super.dropLoot(damageSource, causedByPlayer);
        this.dropStack(new ItemStack(Items.BONE, 1 + this.random.nextInt(3)));
        if (this.random.nextFloat() < 0.32F) {
            this.dropStack(new ItemStack(Items.LEATHER, 1 + this.random.nextInt(2)));
        }
        if (this.random.nextFloat() < 0.16F) {
            this.dropStack(new ItemStack(Items.IRON_NUGGET, 2 + this.random.nextInt(3)));
        }
    }
}
