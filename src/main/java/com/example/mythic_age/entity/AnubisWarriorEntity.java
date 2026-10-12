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

public class AnubisWarriorEntity extends EgyptianMythEntity {
    private static final int JUDGEMENT_COOLDOWN = 140;

    public AnubisWarriorEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 12;
    }

    public static DefaultAttributeContainer.Builder createAnubisWarriorAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 44.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.27D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6.5D)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 0.85D)
                .add(EntityAttributes.GENERIC_ARMOR, 4.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.05D, false));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.72D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 10.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient && this.isAlive()) {
            tryJudgementMark();
        }
    }

    private void tryJudgementMark() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || !this.canUseSpecial() || !this.canSee(target)) {
            return;
        }

        double distance = this.squaredDistanceTo(target);
        if (distance < 9.0D || distance > 81.0D) {
            return;
        }

        this.getLookControl().lookAt(target, 35.0F, 35.0F);
        this.playSpecialSound(ModSounds.ENTITY_ANUBIS_WARRIOR_SPECIAL);
        this.damageAndKnockback(target, 4.0F, 0.55F);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 90, 0), this);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 70, 0), this);
        this.spawnParticles(ParticleTypes.SOUL, this.getX(), this.getEyeY(), this.getZ(), 18, 0.35D, 0.04D);
        this.spawnParticles(ParticleTypes.ENCHANT, target.getX(), target.getBodyY(0.65D), target.getZ(), 22, 0.45D, 0.06D);
        this.setSpecialCooldown(JUDGEMENT_COOLDOWN);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ENTITY_ANUBIS_WARRIOR_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ENTITY_ANUBIS_WARRIOR_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ENTITY_ANUBIS_WARRIOR_DEATH;
    }

    @Override
    protected SoundEvent getAttackSound() {
        return ModSounds.ENTITY_ANUBIS_WARRIOR_ATTACK;
    }

    @Override
    protected void playStepSound(net.minecraft.util.math.BlockPos pos, net.minecraft.block.BlockState state) {
        this.playSound(SoundEvents.ENTITY_HUSK_STEP, 0.18F, 0.92F + this.random.nextFloat() * 0.12F);
    }

    @Override
    protected void dropLoot(DamageSource damageSource, boolean causedByPlayer) {
        super.dropLoot(damageSource, causedByPlayer);
        this.dropStack(new ItemStack(Items.BONE, 1 + this.random.nextInt(2)));
        this.dropStack(new ItemStack(Items.ROTTEN_FLESH, 1 + this.random.nextInt(2)));
        if (this.random.nextFloat() < 0.35F) {
            this.dropStack(new ItemStack(Items.GOLD_NUGGET, 1 + this.random.nextInt(3)));
        }
    }
}
