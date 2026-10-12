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

public class JiangshiEntity extends ChineseMythEntity {
    private static final int TALISMAN_BIND_COOLDOWN = 105;

    public JiangshiEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 12;
    }

    public static DefaultAttributeContainer.Builder createJiangshiAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 34.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.21D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5.0D)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 0.55D)
                .add(EntityAttributes.GENERIC_ARMOR, 2.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 28.0D);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 0.86D, false));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.42D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 10.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient && this.isAlive()) {
            tickStiffHop();
            tryTalismanBind();
        }
    }

    private void tickStiffHop() {
        if (this.isOnGround() && this.getVelocity().horizontalLengthSquared() > 0.0025D && this.age % 18 == 0) {
            this.setVelocity(this.getVelocity().x, 0.28D, this.getVelocity().z);
            this.velocityDirty = true;
            this.playSound(SoundEvents.ENTITY_ZOMBIE_STEP, 0.2F, 1.1F + this.random.nextFloat() * 0.08F);
        }
    }

    private void tryTalismanBind() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || !this.canUseSpecial() || !this.canSee(target)) {
            return;
        }

        double distance = this.squaredDistanceTo(target);
        if (distance < 4.0D || distance > 64.0D) {
            return;
        }

        this.getLookControl().lookAt(target, 24.0F, 20.0F);
        this.playSpecialSound(ModSounds.ENTITY_JIANGSHI_SPECIAL);
        this.damageAndKnockback(target, 4.0F, 0.35F);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 110, 2), this);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 100, 0), this);
        this.spawnParticles(ParticleTypes.ENCHANT, target.getX(), target.getBodyY(0.75D), target.getZ(), 24, 0.42D, 0.05D);
        this.spawnParticles(ParticleTypes.SOUL, target.getX(), target.getBodyY(0.45D), target.getZ(), 16, 0.35D, 0.04D);
        this.spawnParticles(ParticleTypes.CRIT, this.getX(), this.getBodyY(0.7D), this.getZ(), 10, 0.28D, 0.05D);
        this.setSpecialCooldown(TALISMAN_BIND_COOLDOWN);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ENTITY_JIANGSHI_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ENTITY_JIANGSHI_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ENTITY_JIANGSHI_DEATH;
    }

    @Override
    protected SoundEvent getAttackSound() {
        return ModSounds.ENTITY_JIANGSHI_ATTACK;
    }

    @Override
    protected void playStepSound(net.minecraft.util.math.BlockPos pos, net.minecraft.block.BlockState state) {
        this.playSound(SoundEvents.ENTITY_ZOMBIE_STEP, 0.2F, 1.06F + this.random.nextFloat() * 0.08F);
    }

    @Override
    protected void dropLoot(DamageSource damageSource, boolean causedByPlayer) {
        super.dropLoot(damageSource, causedByPlayer);
        if (this.random.nextFloat() < 0.5F) {
            this.dropStack(new ItemStack(Items.PAPER, 1 + this.random.nextInt(2)));
        }
        if (this.random.nextFloat() < 0.34F) {
            this.dropStack(new ItemStack(Items.ROTTEN_FLESH, 1 + this.random.nextInt(3)));
        }
        if (this.random.nextFloat() < 0.12F) {
            this.dropStack(new ItemStack(Items.GOLD_NUGGET, 1 + this.random.nextInt(2)));
        }
    }
}
