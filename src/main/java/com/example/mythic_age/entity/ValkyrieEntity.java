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

public class ValkyrieEntity extends NordicMythEntity {
    private static final int SPEAR_DIVE_COOLDOWN = 85;
    private Vec3d flightTarget;

    public ValkyrieEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.setNoGravity(true);
        this.experiencePoints = 18;
    }

    public static DefaultAttributeContainer.Builder createValkyrieAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 44.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.28D)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 0.54D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6.5D)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 0.7D)
                .add(EntityAttributes.GENERIC_ARMOR, 5.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 38.0D);
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
            tickWingedFlight();
            trySpearDive();
            if (this.age % 44 == 0) {
                this.playSound(SoundEvents.ENTITY_PHANTOM_FLAP, 0.32F, 1.35F + this.random.nextFloat() * 0.14F);
                this.spawnParticles(ParticleTypes.END_ROD, this.getX(), this.getBodyY(0.55D), this.getZ(), 6, 0.38D, 0.02D);
            }
        }
    }

    private void tickWingedFlight() {
        LivingEntity target = this.getTarget();
        Vec3d destination;
        if (target != null && target.isAlive()) {
            destination = target.getPos().add(0.0D, 2.4D, 0.0D);
        } else {
            if (this.flightTarget == null || this.age % 90 == 0 || this.getPos().squaredDistanceTo(this.flightTarget) < 2.5D) {
                chooseFlightTarget();
            }
            destination = this.flightTarget;
        }

        Vec3d toTarget = destination.subtract(this.getPos());
        if (toTarget.lengthSquared() > 0.0001D) {
            Vec3d desired = toTarget.normalize().multiply(target == null ? 0.18D : 0.25D);
            Vec3d lift = new Vec3d(0.0D, MathHelper.sin(this.age * 0.12F) * 0.012D, 0.0D);
            Vec3d nextVelocity = this.getVelocity().multiply(0.84D).add(desired.multiply(0.36D)).add(lift);
            this.setVelocity(nextVelocity);
            this.move(MovementType.SELF, this.getVelocity());
            this.setVelocity(this.getVelocity().multiply(0.88D, 0.84D, 0.88D));
            faceHorizontalVelocity(nextVelocity);
        }
    }

    private void chooseFlightTarget() {
        double x = this.getX() + (this.random.nextDouble() - 0.5D) * 24.0D;
        double z = this.getZ() + (this.random.nextDouble() - 0.5D) * 24.0D;
        double y = this.getY() + 3.0D + this.random.nextDouble() * 6.5D;
        this.flightTarget = new Vec3d(x, MathHelper.clamp(y, this.getWorld().getBottomY() + 5.0D, this.getWorld().getTopY() - 6.0D), z);
    }

    private void faceHorizontalVelocity(Vec3d velocity) {
        if (velocity.horizontalLengthSquared() > 0.0001D) {
            float yaw = (float) (MathHelper.atan2(velocity.z, velocity.x) * 180.0D / Math.PI) - 90.0F;
            this.setYaw(yaw);
            this.setBodyYaw(yaw);
            this.headYaw = yaw;
        }
    }

    private void trySpearDive() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || !this.canUseSpecial() || !this.canSee(target)) {
            return;
        }

        double distance = this.squaredDistanceTo(target);
        if (distance < 6.25D || distance > 121.0D) {
            return;
        }

        this.getLookControl().lookAt(target, 36.0F, 32.0F);
        this.playSpecialSound(ModSounds.ENTITY_VALKYRIE_SPECIAL);
        this.damageAndKnockback(target, 6.5F, 0.8F);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 90, 0), this);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 55, 0), this);
        this.spawnParticles(ParticleTypes.END_ROD, this.getX(), this.getBodyY(0.62D), this.getZ(), 18, 0.45D, 0.06D);
        this.spawnParticles(ParticleTypes.CRIT, target.getX(), target.getBodyY(0.62D), target.getZ(), 18, 0.4D, 0.1D);
        this.spawnParticles(ParticleTypes.ENCHANT, target.getX(), target.getBodyY(0.8D), target.getZ(), 14, 0.45D, 0.05D);
        this.setSpecialCooldown(SPEAR_DIVE_COOLDOWN);
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
        return ModSounds.ENTITY_VALKYRIE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ENTITY_VALKYRIE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ENTITY_VALKYRIE_DEATH;
    }

    @Override
    protected SoundEvent getAttackSound() {
        return ModSounds.ENTITY_VALKYRIE_ATTACK;
    }

    @Override
    protected void dropLoot(DamageSource damageSource, boolean causedByPlayer) {
        super.dropLoot(damageSource, causedByPlayer);
        this.dropStack(new ItemStack(Items.FEATHER, 1 + this.random.nextInt(3)));
        if (this.random.nextFloat() < 0.4F) {
            this.dropStack(new ItemStack(Items.IRON_NUGGET, 2 + this.random.nextInt(4)));
        }
        if (this.random.nextFloat() < 0.16F) {
            this.dropStack(new ItemStack(Items.GOLD_NUGGET, 1 + this.random.nextInt(2)));
        }
    }
}
