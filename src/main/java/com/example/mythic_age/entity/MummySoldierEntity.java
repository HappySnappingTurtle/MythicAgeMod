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
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

public class MummySoldierEntity extends EgyptianMythEntity {
    private static final int SPEAR_THRUST_COOLDOWN = 80;

    public MummySoldierEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 8;
    }

    public static DefaultAttributeContainer.Builder createMummySoldierAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.22D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5.0D)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 0.65D)
                .add(EntityAttributes.GENERIC_ARMOR, 2.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 28.0D);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 0.92D, false));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.6D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 9.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient && this.isAlive()) {
            trySpearThrust();
        }
    }

    private void trySpearThrust() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || !this.canUseSpecial() || !this.canSee(target)) {
            return;
        }

        double distance = this.squaredDistanceTo(target);
        if (distance < 4.0D || distance > 25.0D) {
            return;
        }

        this.getLookControl().lookAt(target, 30.0F, 30.0F);
        this.playSpecialSound(ModSounds.ENTITY_MUMMY_SOLDIER_SPECIAL);
        this.damageAndKnockback(target, 5.5F, 0.8F);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 100, 0), this);
        this.spawnParticles(ParticleTypes.CRIT, target.getX(), target.getBodyY(0.55D), target.getZ(), 12, 0.35D, 0.08D);
        this.setSpecialCooldown(SPEAR_THRUST_COOLDOWN);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ENTITY_MUMMY_SOLDIER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ENTITY_MUMMY_SOLDIER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ENTITY_MUMMY_SOLDIER_DEATH;
    }

    @Override
    protected SoundEvent getAttackSound() {
        return ModSounds.ENTITY_MUMMY_SOLDIER_ATTACK;
    }

    @Override
    protected void playStepSound(net.minecraft.util.math.BlockPos pos, net.minecraft.block.BlockState state) {
        this.playSound(SoundEvents.ENTITY_HUSK_STEP, 0.17F, 0.75F + this.random.nextFloat() * 0.12F);
    }

    @Override
    protected void dropLoot(DamageSource damageSource, boolean causedByPlayer) {
        super.dropLoot(damageSource, causedByPlayer);
        this.dropStack(new ItemStack(Items.ROTTEN_FLESH, 1 + this.random.nextInt(2)));
        if (this.random.nextFloat() < 0.25F) {
            this.dropStack(new ItemStack(Items.BONE, 1));
        }
    }
}
