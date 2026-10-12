package com.example.mythic_age.entity;

import com.example.mythic_age.entity.ai.goal.CentaurRangedAttackGoal;
import com.example.mythic_age.sound.ModSounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.ProjectileAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class CentaurEntity extends GreekMythEntity implements RangedAttackMob {
    public CentaurEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 8;
    }

    public static DefaultAttributeContainer.Builder createCentaurAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 38.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.34D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5.0D)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 0.65D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 34.0D);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new CentaurRangedAttackGoal(this));
        this.goalSelector.add(2, new ProjectileAttackGoal(this, 1.05D, 34, 18.0F));
        this.goalSelector.add(3, new MeleeAttackGoal(this, 1.12D, false));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.9D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 10.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void attack(LivingEntity target, float pullProgress) {
        ArrowEntity arrow = new ArrowEntity(this.getWorld(), this);
        double x = target.getX() - this.getX();
        double y = target.getBodyY(0.45D) - arrow.getY();
        double z = target.getZ() - this.getZ();
        double horizontal = Math.sqrt(x * x + z * z);
        arrow.setVelocity(x, y + horizontal * 0.18D, z, 1.65F, 14.0F - this.getWorld().getDifficulty().getId() * 3.0F);
        arrow.setDamage(5.0D);

        this.playSpecialSound(ModSounds.ENTITY_CENTAUR_SPECIAL);
        this.swingHand(Hand.MAIN_HAND);
        this.spawnParticles(ParticleTypes.CRIT, this.getX(), this.getBodyY(0.78D), this.getZ(), 8, 0.35D, 0.04D);
        this.getWorld().spawnEntity(arrow);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ENTITY_CENTAUR_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ENTITY_CENTAUR_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ENTITY_CENTAUR_DEATH;
    }

    @Override
    protected SoundEvent getAttackSound() {
        return ModSounds.ENTITY_CENTAUR_ATTACK;
    }

    @Override
    protected void playStepSound(net.minecraft.util.math.BlockPos pos, net.minecraft.block.BlockState state) {
        this.playSound(ModSounds.ENTITY_CENTAUR_STEP, 0.18F, 0.95F + this.random.nextFloat() * 0.18F);
    }

    @Override
    protected void dropLoot(DamageSource damageSource, boolean causedByPlayer) {
        super.dropLoot(damageSource, causedByPlayer);
        this.dropStack(new ItemStack(Items.ARROW, 1 + this.random.nextInt(4)));
        if (this.random.nextFloat() < 0.35F) {
            this.dropStack(new ItemStack(Items.LEATHER, 1));
        }
        if (this.random.nextFloat() < 0.12F) {
            this.dropStack(new ItemStack(Items.BOW, 1));
        }
    }
}
