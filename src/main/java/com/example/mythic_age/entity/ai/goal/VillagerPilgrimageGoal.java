package com.example.mythic_age.entity.ai.goal;

import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.EnumSet;

/**
 * 村民朝圣AI Goal
 * 
 * 为希腊村民添加朝圣行为，在没有工作或其他任务时随机前往最近的希腊神殿。
 * 这个Goal直接作用于原版VillagerEntity，不需要创建新的实体类型。
 */
public class VillagerPilgrimageGoal extends Goal {
    
    private final VillagerEntity villager;
    private final double speed;
    private BlockPos templePos;
    private int pilgrimageTimer = 0;
    private int gatheringTicks = 0;
    private boolean isOnPilgrimage = false;
    
    private static final int PILGRIMAGE_INTERVAL = 2400; // 2分钟 (20 ticks/秒 * 120秒)
    private static final int TEMPLE_SEARCH_RANGE = 64; // 神殿搜索范围
    private static final double IDLE_PILGRIMAGE_CHANCE = 0.25; // 空闲检查通过后25%概率开始聚集
    private static final double ARRIVAL_RADIUS = 5.0D;
    private static final int GATHERING_DURATION = 1200; // 到达神庙后停留1分钟，形成可见聚集
    
    public VillagerPilgrimageGoal(VillagerEntity villager, double speed) {
        this.villager = villager;
        this.speed = speed;
        this.setControls(EnumSet.of(Control.MOVE));
    }
    
    @Override
    public boolean canStart() {
        // 只在服务器端执行
        if (villager.getWorld().isClient) {
            return false;
        }
        
        // 如果已经在朝圣中，继续执行
        if (isOnPilgrimage && templePos != null) {
            return true;
        }
        
        // 检查朝圣计时器
        pilgrimageTimer++;
        if (pilgrimageTimer < PILGRIMAGE_INTERVAL) {
            return false;
        }
        
        // 重置计时器
        pilgrimageTimer = 0;
        
        // 只有在村民没有工作、交易、恐慌、繁殖、移动等任务时才触发随机聚集。
        if (!isIdleForPilgrimageStart()) {
            return false;
        }
        
        if (villager.getRandom().nextFloat() > IDLE_PILGRIMAGE_CHANCE) {
            return false;
        }
        
        // 寻找最近的希腊神殿
        templePos = findNearestGreekTemple();
        if (templePos != null) {
            isOnPilgrimage = true;
            gatheringTicks = 0;
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean shouldContinue() {
        if (!isOnPilgrimage || templePos == null) {
            return false;
        }
        
        if (villager.getBlockPos().isWithinDistance(templePos, ARRIVAL_RADIUS)) {
            return gatheringTicks < GATHERING_DURATION && isStillAvailableForGathering();
        }
        
        return isStillAvailableForGathering();
    }

    private boolean isIdleForPilgrimageStart() {
        if (villager.isBaby() || villager.isSleeping() || villager.hasCustomer()) {
            return false;
        }

        Activity activity = villager.getBrain().getFirstPossibleNonCoreActivity().orElse(Activity.IDLE);
        if (activity != Activity.IDLE) {
            return false;
        }

        if (villager.getNavigation().isFollowingPath()) {
            return false;
        }

        return !hasActiveTaskMemory();
    }

    private boolean isStillAvailableForGathering() {
        if (villager.isBaby() || villager.isSleeping() || villager.hasCustomer()) {
            return false;
        }

        Activity activity = villager.getBrain().getFirstPossibleNonCoreActivity().orElse(Activity.IDLE);
        return activity == Activity.IDLE && !hasActiveTaskMemory();
    }

    private boolean hasActiveTaskMemory() {
        return villager.getBrain().hasMemoryModule(MemoryModuleType.WALK_TARGET) ||
            villager.getBrain().hasMemoryModule(MemoryModuleType.INTERACTION_TARGET) ||
            villager.getBrain().hasMemoryModule(MemoryModuleType.ATTACK_TARGET) ||
            villager.getBrain().hasMemoryModule(MemoryModuleType.AVOID_TARGET) ||
            villager.getBrain().hasMemoryModule(MemoryModuleType.BREED_TARGET) ||
            villager.getBrain().hasMemoryModule(MemoryModuleType.IS_PANICKING);
    }
    
    @Override
    public void start() {
        if (templePos != null) {
            villager.getNavigation().startMovingTo(templePos.getX(), templePos.getY(), templePos.getZ(), speed);
        }
    }
    
    @Override
    public void tick() {
        if (templePos == null) {
            return;
        }

        if (villager.getBlockPos().isWithinDistance(templePos, ARRIVAL_RADIUS)) {
            gatheringTicks++;
            villager.getNavigation().stop();
            villager.getLookControl().lookAt(
                templePos.getX() + 0.5D,
                templePos.getY() + 1.0D,
                templePos.getZ() + 0.5D
            );
        } else if (!villager.getNavigation().isFollowingPath()) {
            // 如果导航停止，重新开始导航
            villager.getNavigation().startMovingTo(templePos.getX(), templePos.getY(), templePos.getZ(), speed);
        }
    }
    
    @Override
    public void stop() {
        villager.getNavigation().stop();
        isOnPilgrimage = false;
        templePos = null;
        gatheringTicks = 0;
    }
    
    /**
     * 寻找最近的希腊神殿
     * 优化版本：减少搜索范围，避免性能问题
     */
    private BlockPos findNearestGreekTemple() {
        World world = villager.getWorld();
        if (!(world instanceof ServerWorld)) {
            return null;
        }
        
        BlockPos villagerPos = villager.getBlockPos();
        BlockPos nearestTemple = null;
        double nearestDistance = Double.MAX_VALUE;
        
        // 优化：减少搜索范围，使用更高效的搜索策略
        int reducedRange = TEMPLE_SEARCH_RANGE;
        
        // 分层搜索，优先搜索同一高度和附近高度
        for (int y = -8; y <= 8; y++) { // 只搜索上下8格
            for (int x = -reducedRange; x <= reducedRange; x += 4) { // 每4格搜索一次，减少计算量
                for (int z = -reducedRange; z <= reducedRange; z += 4) {
                    BlockPos checkPos = villagerPos.add(x, y, z);
                    
                    // 确保位置在世界边界内
                    if (!world.isOutOfHeightLimit(checkPos)) {
                        if (isGreekTempleBlock(world, checkPos)) {
                            double distance = villagerPos.getSquaredDistance(checkPos);
                            if (distance < nearestDistance) {
                                nearestDistance = distance;
                                nearestTemple = checkPos;
                            }
                        }
                    }
                }
            }
        }
        
        return nearestTemple;
    }

    private boolean isGreekTempleBlock(World world, BlockPos pos) {
        Identifier blockId = Registries.BLOCK.getId(world.getBlockState(pos).getBlock());
        if (!"mythic_age".equals(blockId.getNamespace())) {
            return false;
        }

        String path = blockId.getPath();
        return path.startsWith("temple_greek") || path.startsWith("statue_greek");
    }
    
    /**
     * 检查村民是否正在朝圣
     */
    public boolean isOnPilgrimage() {
        return isOnPilgrimage;
    }
    
    /**
     * 获取目标神殿位置
     */
    public BlockPos getTemplePos() {
        return templePos;
    }
}
