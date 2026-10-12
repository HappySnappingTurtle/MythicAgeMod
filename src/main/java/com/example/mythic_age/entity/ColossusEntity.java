package com.example.mythic_age.entity;

import com.example.mythic_age.particle.ModParticles;
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
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

public class ColossusEntity extends GreekMythEntity {
    private static final int STOMP_COOLDOWN = 150;

    public ColossusEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 24;
    }

    public static DefaultAttributeContainer.Builder createColossusAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 120.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.18D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 14.0D)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 1.8D)
                .add(EntityAttributes.GENERIC_ARMOR, 12.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 38.0D);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 0.82D, false));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.55D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 12.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient && this.isAlive()) {
            tryStomp();
        }
    }

    private void tryStomp() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || !this.canUseSpecial() || this.squaredDistanceTo(target) > 42.0D) {
            return;
        }

        this.playSpecialSound(ModSounds.ENTITY_COLOSSUS_SPECIAL);
        Box area = this.getBoundingBox().expand(4.0D, 0.8D, 4.0D);
        List<LivingEntity> victims = this.getWorld().getEntitiesByClass(
                LivingEntity.class,
                area,
                entity -> entity != this && entity.isAlive() && !(entity instanceof ColossusEntity)
        );
        for (LivingEntity victim : victims) {
            this.damageAndKnockback(victim, 8.0F, 1.5F);
        }

        this.spawnParticles(ParticleTypes.POOF, this.getX(), this.getY() + 0.2D, this.getZ(), 42, 1.5D, 0.1D);
        this.spawnParticles(ModParticles.SHOCKWAVE, this.getX(), this.getY() + 0.1D, this.getZ(), 18, 1.75D, 0.05D);
        this.spawnParticles(ParticleTypes.CRIT, this.getX(), this.getBodyY(0.25D), this.getZ(), 24, 1.2D, 0.06D);
        this.setSpecialCooldown(STOMP_COOLDOWN);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ENTITY_COLOSSUS_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ENTITY_COLOSSUS_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ENTITY_COLOSSUS_DEATH;
    }

    @Override
    protected SoundEvent getAttackSound() {
        return ModSounds.ENTITY_COLOSSUS_ATTACK;
    }

    @Override
    protected void playStepSound(net.minecraft.util.math.BlockPos pos, net.minecraft.block.BlockState state) {
        this.playSound(ModSounds.ENTITY_COLOSSUS_STEP, 0.35F, 0.58F + this.random.nextFloat() * 0.08F);
    }

    @Override
    protected void dropLoot(DamageSource damageSource, boolean causedByPlayer) {
        super.dropLoot(damageSource, causedByPlayer);
        this.dropStack(new ItemStack(Items.COPPER_INGOT, 2 + this.random.nextInt(5)));
        if (this.random.nextFloat() < 0.45F) {
            this.dropStack(new ItemStack(Items.IRON_INGOT, 1 + this.random.nextInt(2)));
        }
        if (this.random.nextFloat() < 0.18F) {
            this.dropStack(new ItemStack(Items.GOLD_INGOT, 1));
        }
    }
}
