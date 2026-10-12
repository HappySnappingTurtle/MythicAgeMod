package com.example.mythic_age.entity;

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
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

public class NineTailedFoxEntity extends ChineseMythEntity {
    private static final int TAIL_GLAMOUR_COOLDOWN = 115;

    public NineTailedFoxEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 14;
    }

    public static DefaultAttributeContainer.Builder createNineTailedFoxAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.34D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0D)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 0.35D)
                .add(EntityAttributes.GENERIC_ARMOR, 1.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 30.0D);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.18D, false));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.86D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 12.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient && this.isAlive()) {
            tryTailGlamour();
            if (this.age % 42 == 0) {
                this.spawnParticles(ParticleTypes.ENCHANT, this.getX(), this.getBodyY(0.55D), this.getZ(), 8, 0.42D, 0.025D);
            }
        }
    }

    private void tryTailGlamour() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || !this.canUseSpecial() || !this.canSee(target)) {
            return;
        }

        double distance = this.squaredDistanceTo(target);
        if (distance < 6.25D || distance > 100.0D) {
            return;
        }

        this.getLookControl().lookAt(target, 28.0F, 24.0F);
        this.playSpecialSound(ModSounds.ENTITY_NINE_TAILED_FOX_SPECIAL);
        this.damageAndKnockback(target, 3.0F, 0.22F);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 1), this);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 100, 0), this);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 70, 0), this);
        this.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 70, 0), this);

        Box charmArea = target.getBoundingBox().expand(2.4D, 0.8D, 2.4D);
        List<LivingEntity> nearby = this.getWorld().getEntitiesByClass(
                LivingEntity.class,
                charmArea,
                entity -> entity != this && entity != target && entity.isAlive() && !(entity instanceof NineTailedFoxEntity)
        );
        for (LivingEntity victim : nearby) {
            victim.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 70, 0), this);
            victim.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 45, 0), this);
        }

        this.spawnParticles(ParticleTypes.HEART, target.getX(), target.getBodyY(0.85D), target.getZ(), 8, 0.35D, 0.04D);
        this.spawnParticles(ParticleTypes.ENCHANT, target.getX(), target.getBodyY(0.55D), target.getZ(), 28, 0.65D, 0.05D);
        this.spawnParticles(ParticleTypes.WITCH, this.getX(), this.getBodyY(0.65D), this.getZ(), 20, 0.7D, 0.05D);
        this.setSpecialCooldown(TAIL_GLAMOUR_COOLDOWN);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ENTITY_NINE_TAILED_FOX_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ENTITY_NINE_TAILED_FOX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ENTITY_NINE_TAILED_FOX_DEATH;
    }

    @Override
    protected SoundEvent getAttackSound() {
        return ModSounds.ENTITY_NINE_TAILED_FOX_ATTACK;
    }

    @Override
    protected void dropLoot(DamageSource damageSource, boolean causedByPlayer) {
        super.dropLoot(damageSource, causedByPlayer);
        if (this.random.nextFloat() < 0.45F) {
            this.dropStack(new ItemStack(Items.RABBIT_HIDE, 1 + this.random.nextInt(2)));
        }
        if (this.random.nextFloat() < 0.32F) {
            this.dropStack(new ItemStack(Items.GLOW_BERRIES, 1 + this.random.nextInt(2)));
        }
        if (this.random.nextFloat() < 0.12F) {
            this.dropStack(new ItemStack(Items.RED_DYE, 1));
        }
    }
}
