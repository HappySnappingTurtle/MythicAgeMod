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
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

public class DraugrWarriorEntity extends NordicMythEntity {
    private static final int SHIELD_BASH_COOLDOWN = 95;

    public DraugrWarriorEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 12;
    }

    public static DefaultAttributeContainer.Builder createDraugrWarriorAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 36.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.23D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5.5D)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 0.65D)
                .add(EntityAttributes.GENERIC_ARMOR, 4.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.25D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 30.0D);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 0.9D, false));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.46D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 10.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient && this.isAlive()) {
            tryShieldBash();
            if (this.age % 52 == 0) {
                this.spawnParticles(ParticleTypes.SOUL, this.getX(), this.getBodyY(0.52D), this.getZ(), 7, 0.34D, 0.025D);
            }
        }
    }

    private void tryShieldBash() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || !this.canUseSpecial() || !this.canSee(target)) {
            return;
        }
        double distance = this.squaredDistanceTo(target);
        if (distance > 16.0D) {
            return;
        }

        this.getLookControl().lookAt(target, 24.0F, 18.0F);
        this.playSpecialSound(ModSounds.ENTITY_DRAUGR_WARRIOR_SPECIAL);
        this.damageAndKnockback(target, 4.5F, 1.05F);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 70, 1), this);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 60, 0), this);
        this.spawnParticles(ParticleTypes.CRIT, target.getX(), target.getBodyY(0.55D), target.getZ(), 16, 0.36D, 0.08D);
        this.spawnParticles(ParticleTypes.SOUL, this.getX(), this.getBodyY(0.55D), this.getZ(), 14, 0.42D, 0.04D);
        this.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, net.minecraft.block.Blocks.DEEPSLATE.getDefaultState()),
                this.getX(), this.getBodyY(0.18D), this.getZ(), 12, 0.45D, 0.04D);
        this.setSpecialCooldown(SHIELD_BASH_COOLDOWN);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ENTITY_DRAUGR_WARRIOR_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ENTITY_DRAUGR_WARRIOR_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ENTITY_DRAUGR_WARRIOR_DEATH;
    }

    @Override
    protected SoundEvent getAttackSound() {
        return ModSounds.ENTITY_DRAUGR_WARRIOR_ATTACK;
    }

    @Override
    protected void playStepSound(net.minecraft.util.math.BlockPos pos, net.minecraft.block.BlockState state) {
        this.playSound(SoundEvents.ENTITY_ZOMBIE_STEP, 0.18F, 0.78F + this.random.nextFloat() * 0.08F);
    }

    @Override
    protected void dropLoot(DamageSource damageSource, boolean causedByPlayer) {
        super.dropLoot(damageSource, causedByPlayer);
        if (this.random.nextFloat() < 0.48F) {
            this.dropStack(new ItemStack(Items.BONE, 1 + this.random.nextInt(2)));
        }
        if (this.random.nextFloat() < 0.34F) {
            this.dropStack(new ItemStack(Items.IRON_NUGGET, 1 + this.random.nextInt(3)));
        }
        if (this.random.nextFloat() < 0.1F) {
            this.dropStack(new ItemStack(Items.SHIELD, 1));
        }
    }
}
