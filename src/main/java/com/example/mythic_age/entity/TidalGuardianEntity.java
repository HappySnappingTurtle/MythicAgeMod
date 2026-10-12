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
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

public class TidalGuardianEntity extends AtlanteanMythEntity {
    private static final int CLAW_BREAKER_COOLDOWN = 125;

    public TidalGuardianEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 16;
    }

    public static DefaultAttributeContainer.Builder createTidalGuardianAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 74.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 9.0D)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 1.1D)
                .add(EntityAttributes.GENERIC_ARMOR, 9.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 0.86D, false));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.48D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 12.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient && this.isAlive()) {
            tryClawBreaker();
        }
    }

    private void tryClawBreaker() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || !this.canUseSpecial() || !this.canSee(target)) {
            return;
        }
        if (this.squaredDistanceTo(target) > 36.0D) {
            return;
        }

        this.getLookControl().lookAt(target, 24.0F, 24.0F);
        this.playSpecialSound(ModSounds.ENTITY_TIDAL_GUARDIAN_SPECIAL);
        Box area = this.getBoundingBox().expand(2.8D, 0.55D, 2.8D);
        List<LivingEntity> victims = this.getWorld().getEntitiesByClass(
                LivingEntity.class,
                area,
                entity -> entity != this && entity.isAlive() && !(entity instanceof TidalGuardianEntity)
        );
        for (LivingEntity victim : victims) {
            this.damageAndKnockback(victim, 6.5F, 1.05F);
            victim.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 80, 1), this);
        }

        this.spawnParticles(ParticleTypes.SPLASH, this.getX(), this.getBodyY(0.35D), this.getZ(), 26, 0.95D, 0.08D);
        this.spawnParticles(ParticleTypes.BUBBLE_POP, this.getX(), this.getBodyY(0.45D), this.getZ(), 24, 0.75D, 0.05D);
        this.spawnParticles(ParticleTypes.ENCHANT, this.getX(), this.getBodyY(0.7D), this.getZ(), 18, 0.65D, 0.04D);
        this.setSpecialCooldown(CLAW_BREAKER_COOLDOWN);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ENTITY_TIDAL_GUARDIAN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ENTITY_TIDAL_GUARDIAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ENTITY_TIDAL_GUARDIAN_DEATH;
    }

    @Override
    protected SoundEvent getAttackSound() {
        return ModSounds.ENTITY_TIDAL_GUARDIAN_ATTACK;
    }

    @Override
    protected void playStepSound(net.minecraft.util.math.BlockPos pos, net.minecraft.block.BlockState state) {
        this.playSound(SoundEvents.ENTITY_TURTLE_SHAMBLE, 0.26F, 0.72F + this.random.nextFloat() * 0.1F);
    }

    @Override
    protected void dropLoot(DamageSource damageSource, boolean causedByPlayer) {
        super.dropLoot(damageSource, causedByPlayer);
        this.dropStack(new ItemStack(Items.SCUTE, 1));
        if (this.random.nextFloat() < 0.36F) {
            this.dropStack(new ItemStack(Items.PRISMARINE_SHARD, 1 + this.random.nextInt(3)));
        }
        if (this.random.nextFloat() < 0.24F) {
            this.dropStack(new ItemStack(Items.COPPER_INGOT, 1));
        }
    }
}
