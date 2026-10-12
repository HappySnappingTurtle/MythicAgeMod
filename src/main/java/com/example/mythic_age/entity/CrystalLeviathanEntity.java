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

public class CrystalLeviathanEntity extends AtlanteanMythEntity {
    private static final int PRISM_SURGE_COOLDOWN = 105;
    private Vec3d swimTarget;

    public CrystalLeviathanEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.setNoGravity(true);
        this.experiencePoints = 18;
    }

    public static DefaultAttributeContainer.Builder createCrystalLeviathanAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 64.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.23D)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 0.46D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8.0D)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 0.85D)
                .add(EntityAttributes.GENERIC_ARMOR, 6.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40.0D);
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
            tickTidalGlide();
            tryPrismSurge();
            if (this.age % 56 == 0) {
                this.playSound(SoundEvents.ENTITY_GUARDIAN_FLOP, 0.28F, 0.82F + this.random.nextFloat() * 0.18F);
            }
        }
    }

    private void tickTidalGlide() {
        LivingEntity target = this.getTarget();
        Vec3d destination;
        if (target != null && target.isAlive()) {
            destination = target.getPos().add(0.0D, 1.8D, 0.0D);
        } else {
            if (this.swimTarget == null || this.age % 104 == 0 || this.getPos().squaredDistanceTo(this.swimTarget) < 3.2D) {
                chooseSwimTarget();
            }
            destination = this.swimTarget;
        }

        Vec3d toTarget = destination.subtract(this.getPos());
        if (toTarget.lengthSquared() > 0.0001D) {
            double targetSpeed = target == null ? 0.15D : 0.22D;
            if (this.isTouchingWater()) {
                targetSpeed += 0.05D;
            }
            Vec3d desired = toTarget.normalize().multiply(targetSpeed);
            Vec3d wave = new Vec3d(MathHelper.sin(this.age * 0.1F) * 0.015D, MathHelper.cos(this.age * 0.07F) * 0.012D, 0.0D);
            Vec3d nextVelocity = this.getVelocity().multiply(0.85D).add(desired.multiply(0.34D)).add(wave);
            this.setVelocity(nextVelocity);
            this.move(MovementType.SELF, this.getVelocity());
            this.setVelocity(this.getVelocity().multiply(0.88D, 0.84D, 0.88D));
            faceHorizontalVelocity(nextVelocity);
        }
    }

    private void chooseSwimTarget() {
        double x = this.getX() + (this.random.nextDouble() - 0.5D) * 30.0D;
        double z = this.getZ() + (this.random.nextDouble() - 0.5D) * 30.0D;
        double y = this.getY() + (this.random.nextDouble() - 0.35D) * 5.0D;
        if (!this.isTouchingWater()) {
            y += 2.5D;
        }
        this.swimTarget = new Vec3d(x, MathHelper.clamp(y, this.getWorld().getBottomY() + 4.0D, this.getWorld().getTopY() - 6.0D), z);
    }

    private void faceHorizontalVelocity(Vec3d velocity) {
        if (velocity.horizontalLengthSquared() > 0.0001D) {
            float yaw = (float) (MathHelper.atan2(velocity.z, velocity.x) * 180.0D / Math.PI) - 90.0F;
            this.setYaw(yaw);
            this.setBodyYaw(yaw);
            this.headYaw = yaw;
        }
    }

    private void tryPrismSurge() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || !this.canUseSpecial() || !this.canSee(target)) {
            return;
        }

        double distance = this.squaredDistanceTo(target);
        if (distance < 9.0D || distance > 144.0D) {
            return;
        }

        this.getLookControl().lookAt(target, 35.0F, 35.0F);
        this.playSpecialSound(ModSounds.ENTITY_CRYSTAL_LEVIATHAN_SPECIAL);
        this.damageAndKnockback(target, 6.5F, 0.75F);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 90, 1), this);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 80, 0), this);
        this.spawnParticles(ParticleTypes.BUBBLE_POP, target.getX(), target.getBodyY(0.55D), target.getZ(), 26, 0.55D, 0.06D);
        this.spawnParticles(ParticleTypes.SPLASH, target.getX(), target.getBodyY(0.65D), target.getZ(), 18, 0.5D, 0.08D);
        this.spawnParticles(ParticleTypes.ENCHANT, this.getX(), this.getBodyY(0.55D), this.getZ(), 20, 0.45D, 0.06D);
        this.setSpecialCooldown(PRISM_SURGE_COOLDOWN);
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
        return ModSounds.ENTITY_CRYSTAL_LEVIATHAN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ENTITY_CRYSTAL_LEVIATHAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ENTITY_CRYSTAL_LEVIATHAN_DEATH;
    }

    @Override
    protected SoundEvent getAttackSound() {
        return ModSounds.ENTITY_CRYSTAL_LEVIATHAN_ATTACK;
    }

    @Override
    protected void dropLoot(DamageSource damageSource, boolean causedByPlayer) {
        super.dropLoot(damageSource, causedByPlayer);
        this.dropStack(new ItemStack(Items.PRISMARINE_CRYSTALS, 1 + this.random.nextInt(3)));
        if (this.random.nextFloat() < 0.28F) {
            this.dropStack(new ItemStack(Items.PRISMARINE_SHARD, 2 + this.random.nextInt(4)));
        }
        if (this.random.nextFloat() < 0.05F) {
            this.dropStack(new ItemStack(Items.HEART_OF_THE_SEA, 1));
        }
    }
}
