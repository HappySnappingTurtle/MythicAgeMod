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

public class SphinxSentinelEntity extends EgyptianMythEntity {
    private static final int RIDDLE_GAZE_COOLDOWN = 130;

    public SphinxSentinelEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 15;
    }

    public static DefaultAttributeContainer.Builder createSphinxSentinelAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 58.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8.0D)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 1.1D)
                .add(EntityAttributes.GENERIC_ARMOR, 6.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 34.0D);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.02D, false));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.68D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 12.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient && this.isAlive()) {
            tryRiddleGaze();
        }
    }

    private void tryRiddleGaze() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || !this.canUseSpecial() || !this.canSee(target)) {
            return;
        }

        double distance = this.squaredDistanceTo(target);
        if (distance < 12.0D || distance > 100.0D) {
            return;
        }

        this.getLookControl().lookAt(target, 35.0F, 35.0F);
        this.playSpecialSound(ModSounds.ENTITY_SPHINX_SENTINEL_SPECIAL);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 110, 1), this);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 90, 1), this);
        this.spawnParticles(ParticleTypes.ENCHANT, this.getX(), this.getEyeY(), this.getZ(), 22, 0.45D, 0.06D);
        this.spawnParticles(ParticleTypes.SOUL, target.getX(), target.getBodyY(0.6D), target.getZ(), 18, 0.4D, 0.04D);
        this.setSpecialCooldown(RIDDLE_GAZE_COOLDOWN);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ENTITY_SPHINX_SENTINEL_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ENTITY_SPHINX_SENTINEL_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ENTITY_SPHINX_SENTINEL_DEATH;
    }

    @Override
    protected SoundEvent getAttackSound() {
        return ModSounds.ENTITY_SPHINX_SENTINEL_ATTACK;
    }

    @Override
    protected void playStepSound(net.minecraft.util.math.BlockPos pos, net.minecraft.block.BlockState state) {
        this.playSound(SoundEvents.ENTITY_RAVAGER_STEP, 0.2F, 0.82F + this.random.nextFloat() * 0.12F);
    }

    @Override
    protected void dropLoot(DamageSource damageSource, boolean causedByPlayer) {
        super.dropLoot(damageSource, causedByPlayer);
        this.dropStack(new ItemStack(Items.LEATHER, 1 + this.random.nextInt(2)));
        if (this.random.nextFloat() < 0.34F) {
            this.dropStack(new ItemStack(Items.GOLD_NUGGET, 2 + this.random.nextInt(3)));
        }
    }
}
