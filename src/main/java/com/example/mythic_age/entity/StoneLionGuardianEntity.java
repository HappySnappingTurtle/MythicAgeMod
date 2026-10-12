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
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

public class StoneLionGuardianEntity extends ChineseMythEntity {
    private static final int STONE_PAW_COOLDOWN = 125;

    public StoneLionGuardianEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 18;
    }

    public static DefaultAttributeContainer.Builder createStoneLionGuardianAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 62.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 7.0D)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 1.1D)
                .add(EntityAttributes.GENERIC_ARMOR, 9.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.7D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 28.0D);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 0.84D, false));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.42D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 12.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient && this.isAlive()) {
            tryStonePawSuppression();
            if (this.age % 64 == 0) {
                this.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, net.minecraft.block.Blocks.STONE.getDefaultState()),
                        this.getX(), this.getBodyY(0.28D), this.getZ(), 8, 0.45D, 0.02D);
            }
        }
    }

    private void tryStonePawSuppression() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || !this.canUseSpecial() || !this.canSee(target)) {
            return;
        }
        if (this.squaredDistanceTo(target) > 25.0D) {
            return;
        }

        this.getLookControl().lookAt(target, 24.0F, 18.0F);
        this.playSpecialSound(ModSounds.ENTITY_STONE_LION_GUARDIAN_SPECIAL);
        Box area = this.getBoundingBox().expand(2.4D, 0.7D, 2.4D);
        List<LivingEntity> victims = this.getWorld().getEntitiesByClass(
                LivingEntity.class,
                area,
                entity -> entity != this && entity.isAlive() && !(entity instanceof StoneLionGuardianEntity)
        );
        for (LivingEntity victim : victims) {
            this.damageAndKnockback(victim, 6.0F, 1.15F);
            victim.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 90, 2), this);
            victim.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 90, 0), this);
        }

        this.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 90, 0), this);
        this.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, net.minecraft.block.Blocks.STONE.getDefaultState()),
                this.getX(), this.getBodyY(0.2D), this.getZ(), 34, 1.0D, 0.08D);
        this.spawnParticles(ParticleTypes.CRIT, target.getX(), target.getBodyY(0.45D), target.getZ(), 18, 0.45D, 0.08D);
        this.spawnParticles(ParticleTypes.ENCHANT, this.getX(), this.getBodyY(0.62D), this.getZ(), 12, 0.55D, 0.04D);
        this.setSpecialCooldown(STONE_PAW_COOLDOWN);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ENTITY_STONE_LION_GUARDIAN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ENTITY_STONE_LION_GUARDIAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ENTITY_STONE_LION_GUARDIAN_DEATH;
    }

    @Override
    protected SoundEvent getAttackSound() {
        return ModSounds.ENTITY_STONE_LION_GUARDIAN_ATTACK;
    }

    @Override
    protected void playStepSound(net.minecraft.util.math.BlockPos pos, net.minecraft.block.BlockState state) {
        this.playSound(state.isIn(BlockTags.WOOL) ? SoundEvents.BLOCK_STONE_STEP : SoundEvents.BLOCK_DEEPSLATE_STEP,
                0.28F, 0.78F + this.random.nextFloat() * 0.08F);
    }

    @Override
    protected void dropLoot(DamageSource damageSource, boolean causedByPlayer) {
        super.dropLoot(damageSource, causedByPlayer);
        this.dropStack(new ItemStack(Items.STONE, 1 + this.random.nextInt(3)));
        if (this.random.nextFloat() < 0.36F) {
            this.dropStack(new ItemStack(Items.RED_DYE, 1 + this.random.nextInt(2)));
        }
        if (this.random.nextFloat() < 0.12F) {
            this.dropStack(new ItemStack(Items.GOLD_NUGGET, 1 + this.random.nextInt(2)));
        }
    }
}
