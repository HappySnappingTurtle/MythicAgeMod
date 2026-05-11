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
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;

public class CyclopsEntity extends GreekMythEntity {
    private static final int STONE_THROW_COOLDOWN = 140;

    public CyclopsEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 12;
    }

    public static DefaultAttributeContainer.Builder createCyclopsAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 60.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.22D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 10.0D)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 1.5D)
                .add(EntityAttributes.GENERIC_ARMOR, 4.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 36.0D);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 0.95D, false));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.75D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 9.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient && this.isAlive()) {
            tryStoneThrow();
        }
    }

    private void tryStoneThrow() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || !this.canUseSpecial() || !this.canSee(target)) {
            return;
        }

        double distance = this.squaredDistanceTo(target);
        if (distance < 18.0D || distance > 196.0D) {
            return;
        }

        this.getLookControl().lookAt(target, 30.0F, 30.0F);
        this.playSpecialSound(ModSounds.ENTITY_CYCLOPS_SPECIAL);
        this.damageAndKnockback(target, 8.0F, 1.6F);
        this.spawnParticles(ParticleTypes.POOF, target.getX(), target.getY() + 0.3D, target.getZ(), 18, 0.55D, 0.08D);
        this.spawnParticles(ParticleTypes.CRIT, target.getX(), target.getBodyY(0.5D), target.getZ(), 12, 0.45D, 0.12D);
        this.setSpecialCooldown(STONE_THROW_COOLDOWN);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ENTITY_CYCLOPS_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ENTITY_CYCLOPS_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ENTITY_CYCLOPS_DEATH;
    }

    @Override
    protected SoundEvent getAttackSound() {
        return ModSounds.ENTITY_CYCLOPS_ATTACK;
    }

    @Override
    protected void playStepSound(net.minecraft.util.math.BlockPos pos, net.minecraft.block.BlockState state) {
        this.playSound(ModSounds.ENTITY_CYCLOPS_STEP, 0.28F, 0.75F + this.random.nextFloat() * 0.12F);
    }

    @Override
    protected void dropLoot(DamageSource damageSource, boolean causedByPlayer) {
        super.dropLoot(damageSource, causedByPlayer);
        this.dropStack(new ItemStack(Items.COBBLESTONE, 2 + this.random.nextInt(4)));
        if (this.random.nextFloat() < 0.45F) {
            this.dropStack(new ItemStack(Items.RAW_IRON, 1));
        }
        if (this.random.nextFloat() < 0.25F) {
            this.dropStack(new ItemStack(Items.LEATHER, 1 + this.random.nextInt(2)));
        }
    }
}
