package com.example.mythic_age.entity;

import com.example.mythic_age.entity.ai.goal.AzureDragonBreathGoal;
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
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class AzureDragonEntity extends ChineseMythEntity {
    private Vec3d flightTarget;

    public AzureDragonEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.setNoGravity(true);
        this.experiencePoints = 16;
    }

    public static DefaultAttributeContainer.Builder createAzureDragonAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 58.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.24D)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 0.52D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 7.0D)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 0.8D)
                .add(EntityAttributes.GENERIC_ARMOR, 5.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40.0D);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new AzureDragonBreathGoal(this));
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
            tickSerpentineFlight();
            // Dragon breath is now handled by AzureDragonBreathGoal registered in initGoals()
            if (this.age % 48 == 0) {
                this.playSound(SoundEvents.ENTITY_ENDER_DRAGON_FLAP, 0.28F, 1.25F + this.random.nextFloat() * 0.16F);
            }
        }
    }

    private void tickSerpentineFlight() {
        LivingEntity target = this.getTarget();
        Vec3d destination;
        if (target != null && target.isAlive()) {
            destination = target.getPos().add(0.0D, 2.6D, 0.0D);
        } else {
            if (this.flightTarget == null || this.age % 96 == 0 || this.getPos().squaredDistanceTo(this.flightTarget) < 3.0D) {
                chooseFlightTarget();
            }
            destination = this.flightTarget;
        }

        Vec3d toTarget = destination.subtract(this.getPos());
        if (toTarget.lengthSquared() > 0.0001D) {
            Vec3d desired = toTarget.normalize().multiply(target == null ? 0.17D : 0.23D);
            Vec3d drift = new Vec3d(MathHelper.sin(this.age * 0.12F) * 0.018D, MathHelper.cos(this.age * 0.09F) * 0.012D, 0.0D);
            Vec3d nextVelocity = this.getVelocity().multiply(0.84D).add(desired.multiply(0.34D)).add(drift);
            this.setVelocity(nextVelocity);
            this.move(MovementType.SELF, this.getVelocity());
            this.setVelocity(this.getVelocity().multiply(0.88D, 0.84D, 0.88D));
            faceHorizontalVelocity(nextVelocity);
        }
    }

    private void chooseFlightTarget() {
        double x = this.getX() + (this.random.nextDouble() - 0.5D) * 28.0D;
        double z = this.getZ() + (this.random.nextDouble() - 0.5D) * 28.0D;
        double y = this.getY() + 3.0D + this.random.nextDouble() * 7.0D;
        this.flightTarget = new Vec3d(x, MathHelper.clamp(y, this.getWorld().getBottomY() + 5.0D, this.getWorld().getTopY() - 7.0D), z);
    }

    private void faceHorizontalVelocity(Vec3d velocity) {
        if (velocity.horizontalLengthSquared() > 0.0001D) {
            float yaw = (float) (MathHelper.atan2(velocity.z, velocity.x) * 180.0D / Math.PI) - 90.0F;
            this.setYaw(yaw);
            this.setBodyYaw(yaw);
            this.headYaw = yaw;
        }
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
        return ModSounds.ENTITY_AZURE_DRAGON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ENTITY_AZURE_DRAGON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ENTITY_AZURE_DRAGON_DEATH;
    }

    @Override
    protected SoundEvent getAttackSound() {
        return ModSounds.ENTITY_AZURE_DRAGON_ATTACK;
    }

    @Override
    protected void dropLoot(DamageSource damageSource, boolean causedByPlayer) {
        super.dropLoot(damageSource, causedByPlayer);
        this.dropStack(new ItemStack(Items.PRISMARINE_SHARD, 1 + this.random.nextInt(3)));
        if (this.random.nextFloat() < 0.32F) {
            this.dropStack(new ItemStack(Items.GOLD_NUGGET, 2 + this.random.nextInt(3)));
        }
    }
}
