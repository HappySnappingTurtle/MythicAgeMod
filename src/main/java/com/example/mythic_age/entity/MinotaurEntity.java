package com.example.mythic_age.entity;

import com.example.mythic_age.entity.ai.goal.MinotaurChargeAttackGoal;
import com.example.mythic_age.sound.ModSounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;

/**
 * 米诺陶实体类
 * 
 * 基于希腊神话中的米诺陶怪物，这是一个强大的敌对生物，具有独特的冲锋攻击能力。
 * 米诺陶是神话时代Mod中的标志性神话生物，代表了古代传说中的恐怖与力量。
 * 
 * 神话背景：
 * 米诺陶是希腊神话中的牛头人身怪物，被囚禁在克里特岛的迷宫中。
 * 它拥有强大的体魄和凶猛的性格，是古代英雄们面临的重大挑战。
 * 
 * 游戏特性：
 * 1. 高生命值：40点生命值(20颗心)，比大多数敌对生物更耐打
 * 2. 强攻击力：8点攻击伤害，配合击退效果
 * 3. 冲锋攻击：独特的AI行为，可以向目标冲锋造成额外伤害
 * 4. 智能AI：包含多种行为模式，从游荡到主动攻击
 * 5. 完整音效：环境音、受伤音、死亡音和脚步声
 * 
 * AI行为系统：
 * - 优先级0：游泳行为(防止溺水)
 * - 优先级1：冲锋攻击(独特的特殊攻击)
 * - 优先级2：近战攻击(标准的近距离攻击)
 * - 优先级5：远距离游荡(探索环境)
 * - 优先级6：观察玩家和环境
 * 
 * 目标选择系统：
 * - 优先级1：报复攻击(攻击伤害自己的实体)
 * - 优先级2：主动攻击玩家(搜索并攻击附近玩家)
 * 
 * 属性配置：
 * - 生命值：40.0 (比僵尸的20.0高一倍)
 * - 移动速度：0.25 (比僵尸的0.23稍快)
 * - 攻击伤害：8.0 (比僵尸的3.0高很多)
 * - 击退力：1.0 (标准击退效果)
 * - 跟踪范围：35.0 (较大的检测范围)
 * 
 * 平衡性考虑：
 * - 虽然攻击力和生命值很高，但移动速度相对较慢
 * - 冲锋攻击有冷却时间，不会连续使用
 * - 可以被正常的游戏机制(如剑、弓箭)击败
 * 
 * 未来扩展：
 * - 掉落物系统：可能掉落特殊的神话材料
 * - 视觉效果：冲锋时的粒子效果和动画
 * - 生成机制：在特定结构或生物群系中生成
 * 
 * @author MythicAge Development Team
 * @version 1.0.0
 * @since 1.20.1
 */
public class MinotaurEntity extends HostileEntity {

    public MinotaurEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createMinotaurAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8.0D)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 1.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 35.0D);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new MinotaurChargeAttackGoal(this, 1.2D));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.8D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(6, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ENTITY_MINOTAUR_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ENTITY_MINOTAUR_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ENTITY_MINOTAUR_DEATH;
    }

    @Override
    protected void playStepSound(net.minecraft.util.math.BlockPos pos, net.minecraft.block.BlockState state) {
        this.playSound(ModSounds.ENTITY_MINOTAUR_STEP, 0.15F, 1.0F);
    }

    @Override
    protected void dropLoot(DamageSource damageSource, boolean causedByPlayer) {
        super.dropLoot(damageSource, causedByPlayer);

        this.dropStack(new ItemStack(Items.BEEF, 1 + this.random.nextInt(3)));
        if (this.random.nextFloat() < 0.65F) {
            this.dropStack(new ItemStack(Items.LEATHER, 1 + this.random.nextInt(2)));
        }
        if (this.random.nextFloat() < 0.35F) {
            this.dropStack(new ItemStack(Items.BONE, 1 + this.random.nextInt(2)));
        }
    }
}
