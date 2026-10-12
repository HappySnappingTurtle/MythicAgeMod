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

public class SunFalconEntity extends EgyptianMythEntity {
    private static final int SOLAR_DIVE_COOLDOWN = 85;
    private Vec3d flightTarget;

    public SunFalconEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.setNoGravity(true);
        this.experiencePoints = 10;
    }

    public static DefaultAttributeContainer.Builder createSunFalconAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 28.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.26D)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 0.48D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5.0D)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 0.35D)
                .add(EntityAttributes.GENERIC_ARMOR, 1.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 36.0D);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 12.0F));
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
            tickFlight();
            trySolarDive();
            if (this.age % 42 == 0) {
                this.playSound(SoundEvents.ENTITY_PHANTOM_FLAP, 0.35F, 1.12F + this.random.nextFloat() * 0.16F);
            }
        }
    }

    private void tickFlight() {
        LivingEntity target = this.getTarget();
        Vec3d destination;
        if (target != null && target.isAlive()) {
            destination = target.getPos().add(0.0D, 2.2D, 0.0D);
        } else {
            if (this.flightTarget == null || this.age % 90 == 0 || this.getPos().squaredDistanceTo(this.flightTarget) < 2.5D) {
                chooseFlightTarget();
            }
            destination = this.flightTarget;
        }

        Vec3d toTarget = destination.subtract(this.getPos());
        if (toTarget.lengthSquared() > 0.0001D) {
            Vec3d desired = toTarget.normalize().multiply(target == null ? 0.18D : 0.24D);
            Vec3d nextVelocity = this.getVelocity().multiply(0.84D).add(desired.multiply(0.35D));
            this.setVelocity(nextVelocity);
            this.move(MovementType.SELF, this.getVelocity());
            this.setVelocity(this.getVelocity().multiply(0.88D, 0.82D, 0.88D));
            faceHorizontalVelocity(nextVelocity);
        }
    }

    private void chooseFlightTarget() {
        double x = this.getX() + (this.random.nextDouble() - 0.5D) * 22.0D;
        double z = this.getZ() + (this.random.nextDouble() - 0.5D) * 22.0D;
        double y = this.getY() + 2.5D + this.random.nextDouble() * 6.0D;
        this.flightTarget = new Vec3d(x, MathHelper.clamp(y, this.getWorld().getBottomY() + 4.0D, this.getWorld().getTopY() - 6.0D), z);
    }

    private void faceHorizontalVelocity(Vec3d velocity) {
        if (velocity.horizontalLengthSquared() > 0.0001D) {
            float yaw = (float) (MathHelper.atan2(velocity.z, velocity.x) * 180.0D / Math.PI) - 90.0F;
            this.setYaw(yaw);
            this.setBodyYaw(yaw);
            this.headYaw = yaw;
        }
    }

    private void trySolarDive() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || !this.canUseSpecial() || !this.canSee(target)) {
            return;
        }

        double distance = this.squaredDistanceTo(target);
        if (distance < 7.0D || distance > 100.0D) {
            return;
        }

        this.getLookControl().lookAt(target, 35.0F, 35.0F);
        this.playSpecialSound(ModSounds.ENTITY_SUN_FALCON_SPECIAL);
        this.damageAndKnockback(target, 5.0F, 0.65F);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 90, 0), this);
        this.spawnParticles(ParticleTypes.ENCHANT, this.getX(), this.getBodyY(0.5D), this.getZ(), 18, 0.35D, 0.06D);
        this.spawnParticles(ParticleTypes.CRIT, target.getX(), target.getBodyY(0.55D), target.getZ(), 14, 0.35D, 0.08D);
        this.setSpecialCooldown(SOLAR_DIVE_COOLDOWN);
    }

    public boolean isFlying() {
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
        return ModSounds.ENTITY_SUN_FALCON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ENTITY_SUN_FALCON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ENTITY_SUN_FALCON_DEATH;
    }

    @Override
    protected SoundEvent getAttackSound() {
        return ModSounds.ENTITY_SUN_FALCON_ATTACK;
    }

    @Override
    protected void dropLoot(DamageSource damageSource, boolean causedByPlayer) {
        super.dropLoot(damageSource, causedByPlayer);
        this.dropStack(new ItemStack(Items.FEATHER, 1 + this.random.nextInt(3)));
        if (this.random.nextFloat() < 0.22F) {
            this.dropStack(new ItemStack(Items.PHANTOM_MEMBRANE, 1));
        }
    }
}
