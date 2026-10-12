package com.example.mythic_age.entity;

import com.example.mythic_age.particle.ModParticles;
import com.example.mythic_age.sound.ModSounds;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import com.example.mythic_age.entity.ai.goal.PegasusFlightGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class PegasusEntity extends AnimalEntity {
    private static final double RIDER_FLIGHT_SPEED = 0.62D;
    private static final double AUTONOMOUS_FLIGHT_SPEED = 0.18D;
    private Vec3d flightTarget;

    public PegasusEntity(EntityType<? extends PegasusEntity> entityType, World world) {
        super(entityType, world);
        this.setNoGravity(true);
        this.experiencePoints = 4;
    }

    public static DefaultAttributeContainer.Builder createPegasusAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 36.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.31D)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 0.58D)
                .add(EntityAttributes.GENERIC_ARMOR, 2.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 28.0D);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new PegasusFlightGoal(this));
        this.goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 10.0F));
        this.goalSelector.add(6, new LookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        this.setNoGravity(true);
        this.fallDistance = 0.0F;

        if (!this.getWorld().isClient && this.isAlive() && !this.hasPassengers()) {
            tickAutonomousFlight();
        }
        if (!this.getWorld().isClient && this.isAlive() && this.isFlying() && this.age % 46 == 0) {
            this.playSound(ModSounds.ENTITY_PEGASUS_FLY, 0.35F, 1.0F + this.random.nextFloat() * 0.2F);
        }
        if (!this.getWorld().isClient && this.isAlive() && this.isFlying() && this.age % 12 == 0) {
            this.spawnFlightParticles();
        }
    }

    private void tickAutonomousFlight() {
        if (this.flightTarget == null || this.age % 80 == 0 || this.getPos().squaredDistanceTo(this.flightTarget) < 3.0D) {
            chooseFlightTarget();
        }

        Vec3d toTarget = this.flightTarget.subtract(this.getPos());
        if (toTarget.lengthSquared() > 0.0001D) {
            Vec3d desired = toTarget.normalize().multiply(AUTONOMOUS_FLIGHT_SPEED);
            Vec3d nextVelocity = this.getVelocity().multiply(0.86D).add(desired.multiply(0.24D));
            this.setVelocity(nextVelocity);
            faceHorizontalVelocity(nextVelocity);
        }
    }

    private void chooseFlightTarget() {
        double x = this.getX() + (this.random.nextDouble() - 0.5D) * 24.0D;
        double z = this.getZ() + (this.random.nextDouble() - 0.5D) * 24.0D;
        double y = this.getY() + 2.5D + this.random.nextDouble() * 7.0D;
        if (this.isOnGround()) {
            y += 4.0D;
        }
        this.flightTarget = new Vec3d(x, MathHelper.clamp(y, this.getWorld().getBottomY() + 4.0D, this.getWorld().getTopY() - 6.0D), z);
    }

    @Override
    public void travel(Vec3d movementInput) {
        LivingEntity rider = this.getControllingPassenger();
        if (this.isAlive() && rider instanceof PlayerEntity && this.isLogicalSideForUpdatingMovement()) {
            travelWithRider((PlayerEntity) rider);
            return;
        }

        super.travel(movementInput);
        this.setVelocity(this.getVelocity().multiply(0.92D, 0.88D, 0.92D));
        this.fallDistance = 0.0F;
    }

    private void travelWithRider(PlayerEntity rider) {
        this.setYaw(rider.getYaw());
        this.setPitch(rider.getPitch() * 0.45F);
        this.setBodyYaw(this.getYaw());
        this.headYaw = this.getYaw();

        float sideways = rider.sidewaysSpeed * 0.55F;
        float forward = rider.forwardSpeed;
        if (forward < 0.0F) {
            forward *= 0.28F;
        }

        Vec3d look = rider.getRotationVec(1.0F);
        Vec3d forwardVector = new Vec3d(look.x, 0.0D, look.z);
        if (forwardVector.lengthSquared() < 0.0001D) {
            double yawRadians = Math.toRadians(this.getYaw());
            forwardVector = new Vec3d(-Math.sin(yawRadians), 0.0D, Math.cos(yawRadians));
        } else {
            forwardVector = forwardVector.normalize();
        }

        Vec3d rightVector = new Vec3d(-forwardVector.z, 0.0D, forwardVector.x);
        Vec3d horizontal = forwardVector.multiply(forward * RIDER_FLIGHT_SPEED)
                .add(rightVector.multiply(sideways * RIDER_FLIGHT_SPEED));

        double vertical = look.y * (Math.abs(forward) > 0.01F ? Math.abs(forward) : 0.35D) * 0.58D;
        if (rider.isSneaking()) {
            vertical -= 0.34D;
        }
        if (Math.abs(forward) < 0.01F && Math.abs(sideways) < 0.01F && !rider.isSneaking()) {
            vertical += MathHelper.sin(this.age * 0.12F) * 0.018D;
        }

        Vec3d velocity = new Vec3d(horizontal.x, vertical, horizontal.z);
        this.setVelocity(velocity);
        this.move(MovementType.SELF, this.getVelocity());
        this.setVelocity(this.getVelocity().multiply(0.86D, 0.78D, 0.86D));
        this.fallDistance = 0.0F;
    }

    private void spawnFlightParticles() {
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(
                    ParticleTypes.CLOUD,
                    this.getX(),
                    this.getY() + 0.18D,
                    this.getZ(),
                    4,
                    0.32D,
                    0.04D,
                    0.32D,
                    0.015D
            );
            serverWorld.spawnParticles(
                    ModParticles.DIVINE_SPARKLE,
                    this.getX(),
                    this.getBodyY(0.52D),
                    this.getZ(),
                    5,
                    0.28D,
                    0.05D,
                    0.28D,
                    0.016D
            );
            serverWorld.spawnParticles(
                    ParticleTypes.END_ROD,
                    this.getX(),
                    this.getBodyY(0.52D),
                    this.getZ(),
                    3,
                    0.28D,
                    0.05D,
                    0.28D,
                    0.018D
            );
        }
    }

    private void faceHorizontalVelocity(Vec3d velocity) {
        if (velocity.horizontalLengthSquared() > 0.0001D) {
            float yaw = (float) (MathHelper.atan2(velocity.z, velocity.x) * 180.0D / Math.PI) - 90.0F;
            this.setYaw(yaw);
            this.setBodyYaw(yaw);
            this.headYaw = yaw;
        }
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (this.isBreedingItem(stack) || player.isSneaking()) {
            return super.interactMob(player, hand);
        }

        if (!this.getWorld().isClient && !this.hasPassengers()) {
            player.startRiding(this);
            this.playSound(ModSounds.ENTITY_PEGASUS_SPECIAL, 0.75F, 1.05F + this.random.nextFloat() * 0.12F);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengerList().isEmpty() && passenger instanceof PlayerEntity;
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        Entity firstPassenger = this.getFirstPassenger();
        return firstPassenger instanceof LivingEntity ? (LivingEntity) firstPassenger : null;
    }

    @Override
    public double getMountedHeightOffset() {
        return 1.18D;
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.isOf(Items.GOLDEN_CARROT) || stack.isOf(Items.APPLE);
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return ModEntities.PEGASUS.create(world);
    }

    public boolean isFlying() {
        return this.hasPassengers() || !this.isOnGround();
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
        return ModSounds.ENTITY_PEGASUS_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ENTITY_PEGASUS_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ENTITY_PEGASUS_DEATH;
    }

    @Override
    public int getMinAmbientSoundDelay() {
        return 160;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(ModSounds.ENTITY_PEGASUS_STEP, 0.12F, 1.15F + this.random.nextFloat() * 0.15F);
    }

    @Override
    protected void dropLoot(DamageSource damageSource, boolean causedByPlayer) {
        super.dropLoot(damageSource, causedByPlayer);
        if (this.random.nextFloat() < 0.55F) {
            this.dropStack(new ItemStack(Items.FEATHER, 1 + this.random.nextInt(3)));
        }
        if (this.random.nextFloat() < 0.24F) {
            this.dropStack(new ItemStack(Items.LEATHER, 1));
        }
    }
}
