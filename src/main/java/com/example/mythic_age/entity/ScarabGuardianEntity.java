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

public class ScarabGuardianEntity extends EgyptianMythEntity {
    private static final int HORN_RUSH_COOLDOWN = 90;

    public ScarabGuardianEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 9;
    }

    public static DefaultAttributeContainer.Builder createScarabGuardianAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 34.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.24D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5.5D)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 1.1D)
                .add(EntityAttributes.GENERIC_ARMOR, 8.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 0.96D, false));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.62D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient && this.isAlive()) {
            tryHornRush();
        }
    }

    private void tryHornRush() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || !this.canUseSpecial() || !this.canSee(target)) {
            return;
        }

        double distance = this.squaredDistanceTo(target);
        if (distance < 6.0D || distance > 49.0D) {
            return;
        }

        this.getLookControl().lookAt(target, 25.0F, 25.0F);
        this.playSpecialSound(ModSounds.ENTITY_SCARAB_GUARDIAN_SPECIAL);
        this.damageAndKnockback(target, 6.0F, 1.45F);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 55, 0), this);
        this.spawnParticles(ParticleTypes.CRIT, target.getX(), target.getBodyY(0.45D), target.getZ(), 18, 0.35D, 0.08D);
        this.spawnParticles(ParticleTypes.CLOUD, this.getX(), this.getBodyY(0.25D), this.getZ(), 18, 0.55D, 0.06D);
        this.setSpecialCooldown(HORN_RUSH_COOLDOWN);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ENTITY_SCARAB_GUARDIAN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ENTITY_SCARAB_GUARDIAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ENTITY_SCARAB_GUARDIAN_DEATH;
    }

    @Override
    protected SoundEvent getAttackSound() {
        return ModSounds.ENTITY_SCARAB_GUARDIAN_ATTACK;
    }

    @Override
    protected void playStepSound(net.minecraft.util.math.BlockPos pos, net.minecraft.block.BlockState state) {
        this.playSound(SoundEvents.ENTITY_SPIDER_STEP, 0.16F, 0.75F + this.random.nextFloat() * 0.12F);
    }

    @Override
    protected void dropLoot(DamageSource damageSource, boolean causedByPlayer) {
        super.dropLoot(damageSource, causedByPlayer);
        this.dropStack(new ItemStack(Items.STRING, 1 + this.random.nextInt(2)));
        if (this.random.nextFloat() < 0.38F) {
            this.dropStack(new ItemStack(Items.GOLD_NUGGET, 1 + this.random.nextInt(2)));
        }
    }
}
