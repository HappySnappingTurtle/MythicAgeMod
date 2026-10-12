package com.example.mythic_age.entity;

import com.example.mythic_age.sound.ModSounds;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class AbyssalRayEntity extends AtlanteanMythEntity {
    private static final int TAIL_STING_COOLDOWN = 95;
    private Vec3d glideTarget;

    public AbyssalRayEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.setNoGravity(true);
        this.experiencePoints = 14;
    }

    public static DefaultAttributeContainer.Builder createAbyssalRayAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 38.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.24D)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 0.44D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5.5D)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 0.45D)
                .add(EntityAttributes.GENERIC_ARMOR, 2.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 34.0D);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 14.0F));
        this.goalSelector.add(6, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        this.setNoGravity(true);
        this.fallDistance = 0.0F;
        if (!this.getWorld().isClient && this.isAlive()) {
            tickAbyssalGlide();
            tryTailSting();
            if (this.age % 62 == 0) {
                this.playSound(SoundEvents.ENTITY_PHANTOM_FLAP, 0.28F, 0.74F + this.random.nextFloat() * 0.16F);
            }
        }
    }

    private void tickAbyssalGlide() {
        LivingEntity target = this.getTarget();
        Vec3d destination;
        if (target != null && target.isAlive()) {
            destination = target.getPos().add(0.0D, 1.1D, 0.0D);
        } else {
            if (this.glideTarget == null || this.age % 96 == 0 || this.getPos().squaredDistanceTo(this.glideTarget) < 2.8D) {
                chooseGlideTarget();
            }
            destination = this.glideTarget;
        }

        Vec3d toTarget = destination.subtract(this.getPos());
        if (toTarget.lengthSquared() <= 0.0001D) {
            return;
        }

        double targetSpeed = target == null ? 0.12D : 0.2D;
        if (this.isTouchingWater()) {
            targetSpeed += 0.06D;
        }

        Vec3d desired = toTarget.normalize().multiply(targetSpeed);
        Vec3d wingDrift = new Vec3d(
                MathHelper.sin(this.age * 0.08F) * 0.018D,
                MathHelper.cos(this.age * 0.1F) * 0.014D,
                MathHelper.cos(this.age * 0.06F) * 0.012D
        );
        Vec3d nextVelocity = this.getVelocity().multiply(0.84D).add(desired.multiply(0.36D)).add(wingDrift);
        this.setVelocity(nextVelocity);
        this.move(MovementType.SELF, this.getVelocity());
        this.setVelocity(this.getVelocity().multiply(0.88D, 0.82D, 0.88D));
        faceHorizontalVelocity(nextVelocity);
    }

    private void chooseGlideTarget() {
        double x = this.getX() + (this.random.nextDouble() - 0.5D) * 26.0D;
        double z = this.getZ() + (this.random.nextDouble() - 0.5D) * 26.0D;
        double y = this.getY() + (this.random.nextDouble() - 0.4D) * 4.5D;
        if (!this.isTouchingWater()) {
            y += 1.4D;
        }
        this.glideTarget = new Vec3d(
                x,
                MathHelper.clamp(y, this.getWorld().getBottomY() + 4.0D, this.getWorld().getTopY() - 6.0D),
                z
        );
    }

    private void faceHorizontalVelocity(Vec3d velocity) {
        if (velocity.horizontalLengthSquared() > 0.0001D) {
            float yaw = (float) (MathHelper.atan2(velocity.z, velocity.x) * 180.0D / Math.PI) - 90.0F;
            this.setYaw(yaw);
            this.setBodyYaw(yaw);
            this.headYaw = yaw;
        }
    }

    private void tryTailSting() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || !this.canUseSpecial() || !this.canSee(target)) {
            return;
        }

        double distance = this.squaredDistanceTo(target);
        if (distance < 6.0D || distance > 90.0D) {
            return;
        }

        this.getLookControl().lookAt(target, 32.0F, 28.0F);
        this.playSpecialSound(ModSounds.ENTITY_ABYSSAL_RAY_SPECIAL);
        this.damageAndKnockback(target, 5.5F, 0.62F);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 80, 0), this);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 70, 0), this);
        this.spawnParticles(ParticleTypes.CRIT, target.getX(), target.getBodyY(0.55D), target.getZ(), 18, 0.42D, 0.08D);
        this.spawnParticles(ParticleTypes.BUBBLE_POP, target.getX(), target.getBodyY(0.55D), target.getZ(), 20, 0.5D, 0.05D);
        this.spawnParticles(ParticleTypes.ENCHANT, this.getX(), this.getBodyY(0.42D), this.getZ(), 18, 0.52D, 0.04D);
        this.setSpecialCooldown(TAIL_STING_COOLDOWN);
    }

    public boolean isGliding() {
        return !this.isOnGround();
    }

    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    protected void fall(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition) {
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ENTITY_ABYSSAL_RAY_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ENTITY_ABYSSAL_RAY_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ENTITY_ABYSSAL_RAY_DEATH;
    }

    @Override
    protected SoundEvent getAttackSound() {
        return ModSounds.ENTITY_ABYSSAL_RAY_ATTACK;
    }

    @Override
    protected void dropLoot(DamageSource damageSource, boolean causedByPlayer) {
        super.dropLoot(damageSource, causedByPlayer);
        if (this.random.nextFloat() < 0.62F) {
            this.dropStack(new ItemStack(Items.PRISMARINE_SHARD, 1 + this.random.nextInt(2)));
        }
        if (this.random.nextFloat() < 0.28F) {
            this.dropStack(new ItemStack(Items.PHANTOM_MEMBRANE, 1));
        }
        if (this.random.nextFloat() < 0.24F) {
            this.dropStack(new ItemStack(Items.INK_SAC, 1 + this.random.nextInt(2)));
        }
    }
}
