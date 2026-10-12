package com.example.mythic_age.entity;

import com.example.mythic_age.entity.ai.goal.NemeanLionPounceGoal;
import com.example.mythic_age.sound.ModSounds;
import net.minecraft.entity.EntityType;
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
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;

public class NemeanLionEntity extends GreekMythEntity {

    public NemeanLionEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 14;
    }

    public static DefaultAttributeContainer.Builder createNemeanLionAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 56.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.33D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8.0D)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 1.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 9.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 34.0D);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new NemeanLionPounceGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.15D, false));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.8D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 10.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        // Pounce is now handled by NemeanLionPounceGoal registered in initGoals()
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ENTITY_NEMEAN_LION_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ENTITY_NEMEAN_LION_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ENTITY_NEMEAN_LION_DEATH;
    }

    @Override
    protected SoundEvent getAttackSound() {
        return ModSounds.ENTITY_NEMEAN_LION_ATTACK;
    }

    @Override
    protected void playStepSound(net.minecraft.util.math.BlockPos pos, net.minecraft.block.BlockState state) {
        this.playSound(ModSounds.ENTITY_NEMEAN_LION_STEP, 0.18F, 0.9F + this.random.nextFloat() * 0.12F);
    }

    @Override
    protected void dropLoot(DamageSource damageSource, boolean causedByPlayer) {
        super.dropLoot(damageSource, causedByPlayer);
        this.dropStack(new ItemStack(Items.LEATHER, 1 + this.random.nextInt(3)));
        if (this.random.nextFloat() < 0.35F) {
            this.dropStack(new ItemStack(Items.GOLD_NUGGET, 2 + this.random.nextInt(4)));
        }
    }
}
