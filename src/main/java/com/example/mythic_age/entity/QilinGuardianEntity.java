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

public class QilinGuardianEntity extends ChineseMythEntity {
    private static final int AUSPICIOUS_STOMP_COOLDOWN = 120;

    public QilinGuardianEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 16;
    }

    public static DefaultAttributeContainer.Builder createQilinGuardianAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 56.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.26D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 7.0D)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 0.95D)
                .add(EntityAttributes.GENERIC_ARMOR, 6.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 34.0D);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.62D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 12.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient && this.isAlive()) {
            tryAuspiciousStomp();
        }
    }

    private void tryAuspiciousStomp() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || !this.canUseSpecial() || !this.canSee(target)) {
            return;
        }
        if (this.squaredDistanceTo(target) > 49.0D) {
            return;
        }

        this.getLookControl().lookAt(target, 26.0F, 22.0F);
        this.playSpecialSound(ModSounds.ENTITY_QILIN_GUARDIAN_SPECIAL);
        Box area = this.getBoundingBox().expand(2.7D, 0.8D, 2.7D);
        List<LivingEntity> victims = this.getWorld().getEntitiesByClass(
                LivingEntity.class,
                area,
                entity -> entity != this && entity.isAlive() && !(entity instanceof QilinGuardianEntity)
        );
        for (LivingEntity victim : victims) {
            this.damageAndKnockback(victim, 5.5F, 1.0F);
            victim.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 80, 1), this);
            victim.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 70, 0), this);
        }

        this.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 80, 0), this);
        this.spawnParticles(ParticleTypes.ENCHANT, this.getX(), this.getBodyY(0.55D), this.getZ(), 28, 0.85D, 0.05D);
        this.spawnParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getBodyY(0.75D), this.getZ(), 18, 0.65D, 0.05D);
        this.spawnParticles(ParticleTypes.CRIT, target.getX(), target.getBodyY(0.55D), target.getZ(), 16, 0.42D, 0.08D);
        this.setSpecialCooldown(AUSPICIOUS_STOMP_COOLDOWN);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ENTITY_QILIN_GUARDIAN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ENTITY_QILIN_GUARDIAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ENTITY_QILIN_GUARDIAN_DEATH;
    }

    @Override
    protected SoundEvent getAttackSound() {
        return ModSounds.ENTITY_QILIN_GUARDIAN_ATTACK;
    }

    @Override
    protected void playStepSound(net.minecraft.util.math.BlockPos pos, net.minecraft.block.BlockState state) {
        this.playSound(SoundEvents.ENTITY_HORSE_STEP, 0.24F, 0.95F + this.random.nextFloat() * 0.12F);
    }

    @Override
    protected void dropLoot(DamageSource damageSource, boolean causedByPlayer) {
        super.dropLoot(damageSource, causedByPlayer);
        this.dropStack(new ItemStack(Items.GOLD_NUGGET, 2 + this.random.nextInt(4)));
        if (this.random.nextFloat() < 0.36F) {
            this.dropStack(new ItemStack(Items.AMETHYST_SHARD, 1 + this.random.nextInt(2)));
        }
        if (this.random.nextFloat() < 0.08F) {
            this.dropStack(new ItemStack(Items.EMERALD, 1));
        }
    }
}
