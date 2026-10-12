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

public class SirenPriestessEntity extends AtlanteanMythEntity {
    private static final int HYMN_COOLDOWN = 95;
    private Vec3d driftTarget;

    public SirenPriestessEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.setNoGravity(true);
        this.experiencePoints = 14;
    }

    public static DefaultAttributeContainer.Builder createSirenPriestessAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 44.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.24D)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 0.34D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5.0D)
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
            tickSirenDrift();
            tryAbyssalHymn();
            if (this.age % 64 == 0) {
                this.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 0.26F, 1.05F + this.random.nextFloat() * 0.2F);
            }
        }
    }

    private void tickSirenDrift() {
        LivingEntity target = this.getTarget();
        Vec3d destination;
        if (target != null && target.isAlive()) {
            Vec3d away = this.getPos().subtract(target.getPos());
            if (away.horizontalLengthSquared() < 0.001D) {
                away = new Vec3d(1.0D, 0.0D, 0.0D);
            }
            destination = target.getPos().add(away.normalize().multiply(5.2D)).add(0.0D, 1.7D, 0.0D);
        } else {
            if (this.driftTarget == null || this.age % 112 == 0 || this.getPos().squaredDistanceTo(this.driftTarget) < 2.0D) {
                chooseDriftTarget();
            }
            destination = this.driftTarget;
        }

        Vec3d toTarget = destination.subtract(this.getPos());
        if (toTarget.lengthSquared() > 0.0001D) {
            Vec3d desired = toTarget.normalize().multiply(target == null ? 0.12D : 0.18D);
            Vec3d bob = new Vec3d(0.0D, MathHelper.sin(this.age * 0.1F) * 0.012D, 0.0D);
            Vec3d nextVelocity = this.getVelocity().multiply(0.86D).add(desired.multiply(0.32D)).add(bob);
            this.setVelocity(nextVelocity);
            this.move(MovementType.SELF, this.getVelocity());
            this.setVelocity(this.getVelocity().multiply(0.88D, 0.84D, 0.88D));
            faceHorizontalVelocity(nextVelocity);
        }
    }

    private void chooseDriftTarget() {
        double x = this.getX() + (this.random.nextDouble() - 0.5D) * 20.0D;
        double z = this.getZ() + (this.random.nextDouble() - 0.5D) * 20.0D;
        double y = this.getY() + (this.random.nextDouble() - 0.4D) * 4.5D;
        if (!this.isTouchingWater()) {
            y += 1.8D;
        }
        this.driftTarget = new Vec3d(x, MathHelper.clamp(y, this.getWorld().getBottomY() + 4.0D, this.getWorld().getTopY() - 6.0D), z);
    }

    private void faceHorizontalVelocity(Vec3d velocity) {
        if (velocity.horizontalLengthSquared() > 0.0001D) {
            float yaw = (float) (MathHelper.atan2(velocity.z, velocity.x) * 180.0D / Math.PI) - 90.0F;
            this.setYaw(yaw);
            this.setBodyYaw(yaw);
            this.headYaw = yaw;
        }
    }

    private void tryAbyssalHymn() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || !this.canUseSpecial() || !this.canSee(target)) {
            return;
        }

        double distance = this.squaredDistanceTo(target);
        if (distance < 12.0D || distance > 144.0D) {
            return;
        }

        this.getLookControl().lookAt(target, 30.0F, 30.0F);
        this.playSpecialSound(ModSounds.ENTITY_SIREN_PRIESTESS_SPECIAL);
        target.damage(this.getDamageSources().mobAttack(this), 4.5F);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 95, 1), this);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 80, 0), this);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 70, 0), this);
        this.spawnParticles(ParticleTypes.ENCHANT, this.getX(), this.getBodyY(0.65D), this.getZ(), 26, 0.45D, 0.05D);
        this.spawnParticles(ParticleTypes.BUBBLE_POP, target.getX(), target.getBodyY(0.6D), target.getZ(), 22, 0.5D, 0.06D);
        this.spawnParticles(ParticleTypes.SPLASH, target.getX(), target.getBodyY(0.4D), target.getZ(), 14, 0.45D, 0.08D);
        this.setSpecialCooldown(HYMN_COOLDOWN);
    }

    public boolean isDrifting() {
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
        return ModSounds.ENTITY_SIREN_PRIESTESS_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ENTITY_SIREN_PRIESTESS_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ENTITY_SIREN_PRIESTESS_DEATH;
    }

    @Override
    protected SoundEvent getAttackSound() {
        return ModSounds.ENTITY_SIREN_PRIESTESS_ATTACK;
    }

    @Override
    protected void dropLoot(DamageSource damageSource, boolean causedByPlayer) {
        super.dropLoot(damageSource, causedByPlayer);
        this.dropStack(new ItemStack(Items.PRISMARINE_CRYSTALS, 1 + this.random.nextInt(2)));
        if (this.random.nextFloat() < 0.25F) {
            this.dropStack(new ItemStack(Items.AMETHYST_SHARD, 1 + this.random.nextInt(2)));
        }
        if (this.random.nextFloat() < 0.16F) {
            this.dropStack(new ItemStack(Items.NAUTILUS_SHELL, 1));
        }
    }
}
