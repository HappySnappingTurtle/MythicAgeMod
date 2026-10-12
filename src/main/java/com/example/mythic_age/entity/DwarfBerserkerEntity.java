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

public class DwarfBerserkerEntity extends NordicMythEntity {
    private static final int BERSERK_FLURRY_COOLDOWN = 92;

    public DwarfBerserkerEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 13;
    }

    public static DefaultAttributeContainer.Builder createDwarfBerserkerAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 44.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.31D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6.5D)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 0.9D)
                .add(EntityAttributes.GENERIC_ARMOR, 5.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.35D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 28.0D);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.12D, false));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.58D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 12.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient && this.isAlive()) {
            tryBerserkFlurry();
            if (this.age % 36 == 0) {
                this.spawnParticles(ParticleTypes.CRIT, this.getX(), this.getBodyY(0.58D), this.getZ(), 6, 0.34D, 0.04D);
            }
        }
    }

    private void tryBerserkFlurry() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || !this.canUseSpecial() || !this.canSee(target)) {
            return;
        }
        if (this.squaredDistanceTo(target) > 12.25D) {
            return;
        }

        this.getLookControl().lookAt(target, 30.0F, 24.0F);
        this.playSpecialSound(ModSounds.ENTITY_DWARF_BERSERKER_SPECIAL);
        this.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 70, 0), this);
        this.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 70, 0), this);

        Box cleaveArea = this.getBoundingBox().expand(1.7D, 0.45D, 1.7D);
        List<LivingEntity> victims = this.getWorld().getEntitiesByClass(
                LivingEntity.class,
                cleaveArea,
                entity -> entity != this && entity.isAlive() && !(entity instanceof DwarfBerserkerEntity)
        );
        for (LivingEntity victim : victims) {
            this.damageAndKnockback(victim, 4.5F, 0.65F);
            victim.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 42, 0), this);
        }

        this.spawnParticles(ParticleTypes.CRIT, target.getX(), target.getBodyY(0.52D), target.getZ(), 20, 0.42D, 0.08D);
        this.spawnParticles(ParticleTypes.ENCHANT, this.getX(), this.getBodyY(0.7D), this.getZ(), 12, 0.5D, 0.05D);
        this.setSpecialCooldown(BERSERK_FLURRY_COOLDOWN);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ENTITY_DWARF_BERSERKER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ENTITY_DWARF_BERSERKER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ENTITY_DWARF_BERSERKER_DEATH;
    }

    @Override
    protected SoundEvent getAttackSound() {
        return ModSounds.ENTITY_DWARF_BERSERKER_ATTACK;
    }

    @Override
    protected void playStepSound(net.minecraft.util.math.BlockPos pos, net.minecraft.block.BlockState state) {
        this.playSound(SoundEvents.BLOCK_DEEPSLATE_STEP, 0.28F, 0.72F + this.random.nextFloat() * 0.12F);
    }

    @Override
    protected void dropLoot(DamageSource damageSource, boolean causedByPlayer) {
        super.dropLoot(damageSource, causedByPlayer);
        this.dropStack(new ItemStack(Items.IRON_NUGGET, 2 + this.random.nextInt(3)));
        if (this.random.nextFloat() < 0.28F) {
            this.dropStack(new ItemStack(Items.COPPER_INGOT, 1));
        }
        if (this.random.nextFloat() < 0.18F) {
            this.dropStack(new ItemStack(Items.GOLD_NUGGET, 1 + this.random.nextInt(2)));
        }
    }
}
