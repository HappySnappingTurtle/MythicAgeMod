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
import net.minecraft.world.World;

public class ManticoreEntity extends GreekMythEntity {
    private static final int TAIL_STING_COOLDOWN = 90;

    public ManticoreEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 13;
    }

    public static DefaultAttributeContainer.Builder createManticoreAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 48.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.28D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6.0D)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 0.5D)
                .add(EntityAttributes.GENERIC_ARMOR, 4.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 34.0D);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.02D, false));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.78D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 10.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient && this.isAlive()) {
            tryTailSting();
        }
    }

    private void tryTailSting() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || !this.canUseSpecial() || !this.canSee(target)) {
            return;
        }

        double distance = this.squaredDistanceTo(target);
        if (distance < 16.0D || distance > 144.0D) {
            return;
        }

        this.getLookControl().lookAt(target, 30.0F, 30.0F);
        this.playSpecialSound(ModSounds.ENTITY_MANTICORE_SPECIAL);
        target.damage(this.getDamageSources().mobAttack(this), 4.0F);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 140, 1), this);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 90, 0), this);
        this.spawnParticles(ParticleTypes.SNEEZE, target.getX(), target.getBodyY(0.55D), target.getZ(), 24, 0.45D, 0.06D);
        this.spawnParticles(ParticleTypes.CRIT, this.getX(), this.getEyeY(), this.getZ(), 10, 0.3D, 0.08D);
        this.setSpecialCooldown(TAIL_STING_COOLDOWN);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ENTITY_MANTICORE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ENTITY_MANTICORE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ENTITY_MANTICORE_DEATH;
    }

    @Override
    protected SoundEvent getAttackSound() {
        return ModSounds.ENTITY_MANTICORE_ATTACK;
    }

    @Override
    protected void playStepSound(net.minecraft.util.math.BlockPos pos, net.minecraft.block.BlockState state) {
        this.playSound(ModSounds.ENTITY_MANTICORE_STEP, 0.17F, 0.85F + this.random.nextFloat() * 0.16F);
    }

    @Override
    protected void dropLoot(DamageSource damageSource, boolean causedByPlayer) {
        super.dropLoot(damageSource, causedByPlayer);
        this.dropStack(new ItemStack(Items.LEATHER, 1 + this.random.nextInt(2)));
        if (this.random.nextFloat() < 0.45F) {
            this.dropStack(new ItemStack(Items.SPIDER_EYE, 1));
        }
        if (this.random.nextFloat() < 0.22F) {
            this.dropStack(new ItemStack(Items.PHANTOM_MEMBRANE, 1));
        }
    }
}
