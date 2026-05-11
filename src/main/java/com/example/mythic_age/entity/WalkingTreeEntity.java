package com.example.mythic_age.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.EnumSet;
import java.util.UUID;

/**
 * 行走树实体类
 * 
 * 这是一个由北欧神力"行走的森林"召唤的特殊怪物，具有以下特性：
 * 1. 固守召唤位置附近的小范围区域
 * 2. 拥有较高的生命值
 * 3. 受到火焰伤害时伤害翻倍
 * 4. 移动速度较慢但攻击力强
 * 5. 作为友方召唤物保护召唤者和守护区域
 * 
 * 设计理念：
 * - 作为召唤生物，它们应该保护特定区域而不是四处游荡
 * - 高生命值使其成为可靠的防御单位
 * - 火焰弱点增加了战术深度
 * - 树木主题的外观和行为
 */
public class WalkingTreeEntity extends HostileEntity {
    private static final TrackedData<Integer> VARIANT =
        DataTracker.registerData(WalkingTreeEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> ATTACK_ANIMATION_TICKS =
        DataTracker.registerData(WalkingTreeEntity.class, TrackedDataHandlerRegistry.INTEGER);
    
    private BlockPos guardPos; // 守护位置
    private static final int GUARD_RANGE = 8; // 守护范围
    private static final double FIRE_DAMAGE_MULTIPLIER = 2.0; // 火焰伤害倍数
    private static final int LIFETIME_TICKS = 6000; // 5分钟
    private static final int ATTACK_ANIMATION_DURATION = 32;
    private UUID summonerUuid;
    private int lifetimeTicks;
    
    public WalkingTreeEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.guardPos = this.getBlockPos(); // 默认守护当前位置
    }
    
    /**
     * 设置守护位置
     */
    public void setGuardPos(BlockPos pos) {
        this.guardPos = pos;
    }

    public void setSummoner(PlayerEntity summoner) {
        this.summonerUuid = summoner != null ? summoner.getUuid() : null;
    }

    public PlayerEntity getSummoner() {
        return this.summonerUuid != null ? this.getWorld().getPlayerByUuid(this.summonerUuid) : null;
    }
    
    /**
     * 获取守护位置
     */
    public BlockPos getGuardPos() {
        return this.guardPos != null ? this.guardPos : this.getBlockPos();
    }
    
    /**
     * 获取守护范围
     */
    public int getGuardRange() {
        return GUARD_RANGE;
    }

    public void setVariant(WalkingTreeVariant variant) {
        this.dataTracker.set(VARIANT, variant.ordinal());
    }

    public WalkingTreeVariant getVariant() {
        return WalkingTreeVariant.fromOrdinal(this.dataTracker.get(VARIANT));
    }
    
    /**
     * 设置实体属性
     * 高生命值、中等攻击力、较慢移动速度
     */
    public static DefaultAttributeContainer.Builder createWalkingTreeAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 60.0D) // 高生命值
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2D) // 较慢移动速度
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 10.0D) // 强攻击力
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 1.5D) // 较强击退
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16.0D) // 中等跟踪范围
                .add(EntityAttributes.GENERIC_ARMOR, 8.0D); // 较高护甲
    }
    
    @Override
    protected void initGoals() {
        super.initGoals();
        // 游泳行为（防止溺水）
        this.goalSelector.add(0, new SwimGoal(this));
        // 守护区域行为（最高优先级的移动行为）
        this.goalSelector.add(1, new GuardAreaGoal());
        // 近战攻击
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.0D, false));
        // 在守护范围内游荡
        this.goalSelector.add(5, new WanderAroundPointOfInterestGoal(this, 0.6D, false));
        // 观察行为
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(6, new LookAroundGoal(this));
        
        // 目标选择
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, HostileEntity.class, 10, true, false, this::isValidEnemy));
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(VARIANT, WalkingTreeVariant.OAK.ordinal());
        this.dataTracker.startTracking(ATTACK_ANIMATION_TICKS, 0);
    }
    
    @Override
    public boolean damage(DamageSource source, float amount) {
        // 火焰伤害翻倍
        if (source.isOf(DamageTypes.IN_FIRE) || 
            source.isOf(DamageTypes.ON_FIRE) || 
            source.isOf(DamageTypes.LAVA) ||
            source.isOf(DamageTypes.HOT_FLOOR)) {
            amount *= FIRE_DAMAGE_MULTIPLIER;
        }
        return super.damage(source, amount);
    }
    
    @Override
    protected SoundEvent getAmbientSound() {
        // 使用树叶沙沙声作为环境音
        return SoundEvents.BLOCK_GRASS_STEP;
    }
    
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        // 使用木头破裂声作为受伤音
        return SoundEvents.BLOCK_WOOD_BREAK;
    }
    
    @Override
    protected SoundEvent getDeathSound() {
        // 使用树木倒下的声音作为死亡音
        return SoundEvents.BLOCK_WOOD_FALL;
    }
    
    @Override
    protected void playStepSound(BlockPos pos, net.minecraft.block.BlockState state) {
        // 使用木头脚步声
        this.playSound(SoundEvents.BLOCK_WOOD_STEP, 0.15F, 1.0F);
    }

    @Override
    protected void dropLoot(DamageSource damageSource, boolean causedByPlayer) {
        WalkingTreeVariant variant = this.getVariant();
        this.dropStack(new ItemStack(getLogDropItem(variant), 2 + this.random.nextInt(4)));
        this.dropStack(new ItemStack(Items.STICK, 1 + this.random.nextInt(4)));

        switch (variant) {
            case OAK -> {
                if (this.random.nextFloat() < 0.35F) {
                    this.dropStack(new ItemStack(Items.APPLE, 1 + this.random.nextInt(2)));
                }
            }
            case DARK_OAK -> {
                if (this.random.nextFloat() < 0.45F) {
                    this.dropStack(new ItemStack(Items.APPLE, 1 + this.random.nextInt(2)));
                }
            }
            case JUNGLE -> {
                if (this.random.nextFloat() < 0.25F) {
                    this.dropStack(new ItemStack(Items.COCOA_BEANS, 1 + this.random.nextInt(3)));
                }
            }
            case MANGROVE -> {
                if (this.random.nextFloat() < 0.25F) {
                    this.dropStack(new ItemStack(Items.MANGROVE_PROPAGULE));
                }
            }
            case CHERRY -> {
                if (this.random.nextFloat() < 0.30F) {
                    this.dropStack(new ItemStack(Items.PINK_PETALS, 1 + this.random.nextInt(3)));
                }
            }
            default -> {
            }
        }
    }

    private static Item getLogDropItem(WalkingTreeVariant variant) {
        return switch (variant) {
            case OAK -> Items.OAK_LOG;
            case SPRUCE -> Items.SPRUCE_LOG;
            case BIRCH -> Items.BIRCH_LOG;
            case JUNGLE -> Items.JUNGLE_LOG;
            case ACACIA -> Items.ACACIA_LOG;
            case DARK_OAK -> Items.DARK_OAK_LOG;
            case MANGROVE -> Items.MANGROVE_LOG;
            case CHERRY -> Items.CHERRY_LOG;
        };
    }
    
    /**
     * 检查是否应该返回守护位置
     */
    public boolean shouldReturnToGuardPos() {
        if (this.guardPos == null) {
            return false;
        }
        return this.squaredDistanceTo(this.guardPos.getX(), this.guardPos.getY(), this.guardPos.getZ()) > GUARD_RANGE * GUARD_RANGE;
    }
    
    /**
     * 检查目标是否在守护范围内
     */
    public boolean isTargetInGuardRange(BlockPos targetPos) {
        if (this.guardPos == null) {
            return true;
        }
        return this.guardPos.isWithinDistance(targetPos, GUARD_RANGE);
    }
    
    @Override
    public void tick() {
        super.tick();

        if (!this.getWorld().isClient) {
            this.lifetimeTicks++;
            if (this.lifetimeTicks >= LIFETIME_TICKS) {
                this.discard();
                return;
            }

            int attackAnimationTicks = this.dataTracker.get(ATTACK_ANIMATION_TICKS);
            if (attackAnimationTicks > 0) {
                this.dataTracker.set(ATTACK_ANIMATION_TICKS, attackAnimationTicks - 1);
            }

            if (this.getTarget() == null) {
                this.assistSummoner();
            }
        }

        // 如果距离守护位置太远，清除当前目标并返回
        if (shouldReturnToGuardPos() && this.getTarget() != null) {
            this.setTarget(null);
        }
    }

    @Override
    public boolean tryAttack(Entity target) {
        if (this.isFriendlyEntity(target)) {
            return false;
        }
        boolean attacked = super.tryAttack(target);
        if (attacked) {
            this.swingHand(Hand.MAIN_HAND);
            this.dataTracker.set(ATTACK_ANIMATION_TICKS, ATTACK_ANIMATION_DURATION);
        }
        return attacked;
    }

    public float getAttackAnimationProgress(float tickDelta) {
        float remainingTicks = Math.max(0.0F, this.dataTracker.get(ATTACK_ANIMATION_TICKS) - tickDelta);
        float progress = 1.0F - (remainingTicks / (float) ATTACK_ANIMATION_DURATION);
        return MathHelper.clamp(progress, 0.0F, 1.0F);
    }

    @Override
    public void setTarget(LivingEntity target) {
        if (target != null && !this.isValidEnemy(target)) {
            super.setTarget(null);
            return;
        }
        super.setTarget(target);
    }

    @Override
    public boolean isTeammate(Entity other) {
        if (this.isFriendlyEntity(other)) {
            return true;
        }
        return super.isTeammate(other);
    }

    @Override
    public boolean cannotDespawn() {
        return true;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putString("walkingTreeVariant", this.getVariant().getSerializedName());
        nbt.putInt("walkingTreeLifetimeTicks", this.lifetimeTicks);
        if (this.summonerUuid != null) {
            nbt.putUuid("walkingTreeSummoner", this.summonerUuid);
        }
        if (this.guardPos != null) {
            nbt.putInt("guardPosX", this.guardPos.getX());
            nbt.putInt("guardPosY", this.guardPos.getY());
            nbt.putInt("guardPosZ", this.guardPos.getZ());
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("walkingTreeVariant")) {
            this.setVariant(WalkingTreeVariant.fromSerializedName(nbt.getString("walkingTreeVariant")));
        }
        if (nbt.contains("walkingTreeLifetimeTicks")) {
            this.lifetimeTicks = nbt.getInt("walkingTreeLifetimeTicks");
        }
        if (nbt.containsUuid("walkingTreeSummoner")) {
            this.summonerUuid = nbt.getUuid("walkingTreeSummoner");
        }
        if (nbt.contains("guardPosX")) {
            this.guardPos = new BlockPos(
                nbt.getInt("guardPosX"),
                nbt.getInt("guardPosY"),
                nbt.getInt("guardPosZ")
            );
        }
    }

    private void assistSummoner() {
        PlayerEntity summoner = this.getSummoner();
        if (summoner == null || !summoner.isAlive()) {
            return;
        }

        LivingEntity preferredTarget = null;
        if (summoner.getAttacker() instanceof LivingEntity) {
            preferredTarget = (LivingEntity) summoner.getAttacker();
        } else if (summoner.getAttacking() instanceof LivingEntity) {
            preferredTarget = (LivingEntity) summoner.getAttacking();
        }

        if (preferredTarget != null && this.isValidEnemy(preferredTarget)) {
            this.setTarget(preferredTarget);
        }
    }

    private boolean isValidEnemy(LivingEntity entity) {
        return entity != null
            && entity.isAlive()
            && !this.isFriendlyEntity(entity)
            && this.isTargetInGuardRange(entity.getBlockPos());
    }

    private boolean isFriendlyEntity(Entity entity) {
        if (entity == null) {
            return false;
        }

        if (this.summonerUuid != null && this.summonerUuid.equals(entity.getUuid())) {
            return true;
        }

        if (entity instanceof WalkingTreeEntity) {
            WalkingTreeEntity otherTree = (WalkingTreeEntity) entity;
            return this.summonerUuid != null && this.summonerUuid.equals(otherTree.summonerUuid);
        }

        if (entity instanceof TameableEntity) {
            TameableEntity tameable = (TameableEntity) entity;
            if (tameable.getOwnerUuid() != null) {
                return this.summonerUuid != null && this.summonerUuid.equals(tameable.getOwnerUuid());
            }
        }

        return false;
    }
    
    /**
     * 守护区域AI目标内部类
     * 实现行走树固守召唤位置的行为
     */
    private class GuardAreaGoal extends Goal {
        private final double speed = 1.0D;
        private Path returnPath;
        
        public GuardAreaGoal() {
            this.setControls(EnumSet.of(Goal.Control.MOVE));
        }
        
        @Override
        public boolean canStart() {
            // 当实体距离守护位置太远时启动返回行为
            return WalkingTreeEntity.this.shouldReturnToGuardPos() && WalkingTreeEntity.this.getTarget() == null;
        }
        
        @Override
        public boolean shouldContinue() {
            // 继续执行直到回到守护范围内
            return WalkingTreeEntity.this.shouldReturnToGuardPos() && this.returnPath != null;
        }
        
        @Override
        public void start() {
            BlockPos guardPos = WalkingTreeEntity.this.getGuardPos();
            if (guardPos != null) {
                this.returnPath = WalkingTreeEntity.this.getNavigation().findPathTo(guardPos, 1);
                if (this.returnPath != null) {
                    WalkingTreeEntity.this.getNavigation().startMovingAlong(this.returnPath, this.speed);
                }
            }
        }
        
        @Override
        public void stop() {
            this.returnPath = null;
            WalkingTreeEntity.this.getNavigation().stop();
        }
        
        @Override
        public void tick() {
            BlockPos guardPos = WalkingTreeEntity.this.getGuardPos();
            if (guardPos != null && WalkingTreeEntity.this.shouldReturnToGuardPos()) {
                WalkingTreeEntity.this.getNavigation().startMovingTo(guardPos.getX(), guardPos.getY(), guardPos.getZ(), this.speed);
            }
        }
    }
}
