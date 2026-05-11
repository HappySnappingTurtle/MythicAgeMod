package com.example.mythic_age.block.entity;

import com.example.mythic_age.block.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * 丰饶祭坛核心方块实体
 * 
 * 负责处理丰饶祭坛的核心逻辑，包括多方块结构验证和定期资源生成。
 * 这个方块实体是整个丰饶祭坛系统的大脑。
 * 
 * 主要功能：
 * - 多方块结构完整性检测
 * - 定时资源生成系统
 * - 资源类型和位置的智能选择
 * - 视觉和音效反馈系统
 * 
 * 多方块结构布局：
 * - 核心方块位于中心
 * - 4个柱子位于对角线位置（2格距离）
 * - 结构必须完整才能工作
 * 
 * 资源生成规则：
 * - 每30-60秒生成一次资源
 * - 在核心周围2格范围内选择位置
 * - 优先生成在空气方块位置
 * - 避免覆盖重要方块
 * 
 * 生成的资源类型：
 * - 25% 概率生成矿物（铁块、金块、钻石块等）
 * - 35% 概率生成一堆肉方块
 * - 30% 概率生成一堆木头方块  
 * - 10% 概率生成特殊珍贵方块
 * 
 * @author MythicAge Development Team
 * @version 1.0.0
 * @since 1.20.1
 */
public class AbundanceAltarCoreBlockEntity extends BlockEntity {

    /** 下次生成资源的时间（以游戏刻为单位） */
    private long nextGenerationTime = 0;
    
    /** 生成间隔时间（30-60秒，转换为游戏刻） */
    private static final int MIN_GENERATION_INTERVAL = 30 * 20; // 30秒 = 600刻
    private static final int MAX_GENERATION_INTERVAL = 60 * 20; // 60秒 = 1200刻
    
    /** 资源生成范围（核心周围的格数） */
    private static final int GENERATION_RANGE = 2;

    public AbundanceAltarCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ABUNDANCE_ALTAR_CORE, pos, state);
        // 初始化下次生成时间
        scheduleNextGeneration();
    }

    /**
     * 服务器端定时器
     * 每游戏刻调用一次，负责检测结构完整性和触发资源生成
     */
    public static void serverTick(World world, BlockPos pos, BlockState state, AbundanceAltarCoreBlockEntity blockEntity) {
        if (world.isClient) return;

        long currentTime = world.getTime();
        
        // 检查是否到了生成资源的时间
        if (currentTime >= blockEntity.nextGenerationTime) {
            // 验证多方块结构
            if (blockEntity.isStructureValid()) {
                blockEntity.generateResources((ServerWorld) world);
                blockEntity.scheduleNextGeneration();
            } else {
                // 结构不完整，延迟一段时间再检查
                blockEntity.nextGenerationTime = currentTime + 200; // 10秒后再检查
            }
        }
    }

    /**
     * 检查多方块结构是否完整
     * 
     * 新的10层结构验证：
     * - 检查关键层的关键方块
     * - 验证结构的完整性
     * 
     * @return 结构是否完整
     */
    public boolean isStructureValid() {
        if (world == null) return false;

        // 核心方块应该在第七层（y+6）
        
        // 检查第一层的边框是否为带金线的砖 (纵向边缘A类型，横向边缘E类型)
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                if (Math.abs(x) == 5 || Math.abs(z) == 5) {
                    BlockPos checkPos = pos.add(x, -6, z); // 相对于核心位置向下6层
                    BlockState state = world.getBlockState(checkPos);
                    
                    // 根据位置检查对应的方块类型
                    if (Math.abs(x) == 5) {
                        // 纵向边缘（东西边界，x=±5）应该是A类型（上下前后带金线）
                        if (!state.isOf(com.example.mythic_age.block.ModBlocks.ABUNDANCE_ALTAR_BRICK_GOLD_H)) {
                            return false;
                        }
                    } else {
                        // 横向边缘（南北边界，z=±5）应该是E类型（前后左右带金线）
                        if (!state.isOf(com.example.mythic_age.block.ModBlocks.ABUNDANCE_ALTAR_BRICK_GOLD_V)) {
                            return false;
                        }
                    }
                }
            }
        }
        
        // 检查关键的柱子位置 (第二、三、四、六、七、八层)
        BlockPos[] pillarPositions = {
            pos.add(2, -5, 2),   // 第二层的角落柱子
            pos.add(2, -5, -2),
            pos.add(-2, -5, 2),
            pos.add(-2, -5, -2),
            pos.add(2, -1, 2),   // 第六层的角落柱子
            pos.add(2, -1, -2),
            pos.add(-2, -1, 2),
            pos.add(-2, -1, -2)
        };
        
        for (BlockPos pillarPos : pillarPositions) {
            BlockState pillarState = world.getBlockState(pillarPos);
            if (!pillarState.isOf(com.example.mythic_age.block.ModBlocks.ABUNDANCE_ALTAR_PILLAR)) {
                return false;
            }
        }

        // 检查储藏室是否在正确位置 (第三层)
        BlockPos[] storagePositions = {
            pos.add(-3, -4, -4),  // 矿物储藏室
            pos.add(3, -4, -4),   // 木材储藏室
            pos.add(0, -4, -4),   // 食物储藏室
            pos.add(0, -4, 4),    // 食物储藏室
            pos.add(-4, -4, 0),   // 食物储藏室
            pos.add(4, -4, 0)     // 食物储藏室
        };
        
        // 简化验证，只检查几个关键储藏室
        Block[] expectedBlocks = {
            com.example.mythic_age.block.ModBlocks.ABUNDANCE_STORAGE_MINERAL,
            com.example.mythic_age.block.ModBlocks.ABUNDANCE_STORAGE_WOOD,
            com.example.mythic_age.block.ModBlocks.ABUNDANCE_STORAGE_FOOD,
            com.example.mythic_age.block.ModBlocks.ABUNDANCE_STORAGE_FOOD,
            com.example.mythic_age.block.ModBlocks.ABUNDANCE_STORAGE_FOOD,
            com.example.mythic_age.block.ModBlocks.ABUNDANCE_STORAGE_FOOD
        };
        
        for (int i = 0; i < Math.min(storagePositions.length, expectedBlocks.length); i++) {
            BlockState state = world.getBlockState(storagePositions[i]);
            if (!state.isOf(expectedBlocks[i])) {
                return false;
            }
        }

        return true;
    }

    /**
     * 生成资源
     * 在第一层祭坛砖的上方生成资源
     */
    private void generateResources(ServerWorld world) {
        // 获取第一层带金线砖的上方位置
        List<BlockPos> availablePositions = getFirstLayerTopPositions();
        
        if (availablePositions.isEmpty()) {
            // 如果没有可用位置，播放失败音效
            world.playSound(null, pos, SoundEvents.BLOCK_BEACON_ACTIVATE, 
                SoundCategory.BLOCKS, 0.5f, 0.8f);
            return;
        }

        // 随机选择一个位置
        BlockPos targetPos = availablePositions.get(world.random.nextInt(availablePositions.size()));
        
        // 随机选择资源类型
        Block resourceBlock = selectRandomResource(world);
        
        // 放置资源方块
        world.setBlockState(targetPos, resourceBlock.getDefaultState());
        
        // 播放成功音效
        world.playSound(null, targetPos, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 
            SoundCategory.BLOCKS, 1.0f, 1.2f);
        
        // 生成粒子效果
        world.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
            targetPos.getX() + 0.5, targetPos.getY() + 1.0, targetPos.getZ() + 0.5,
            10, 0.5, 0.5, 0.5, 0.1);
        
        // 在核心位置也生成一些粒子效果
        world.spawnParticles(ParticleTypes.ENCHANT,
            pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
            5, 0.3, 0.3, 0.3, 0.1);
    }

    /**
     * 获取第一层带金线砖上方的可用位置
     * 只在第一层边框砖的上方生成资源
     */
    private List<BlockPos> getFirstLayerTopPositions() {
        List<BlockPos> positions = new ArrayList<>();
        
        // 检查第一层边框砖的上方位置
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                if (Math.abs(x) == 5 || Math.abs(z) == 5) {
                    BlockPos firstLayerPos = pos.add(x, -6, z); // 第一层位置
                    BlockPos topPos = firstLayerPos.up(); // 第一层上方
                    
                    // 检查第一层是否是正确的砖（根据位置判断类型）
                    BlockState firstLayerState = world.getBlockState(firstLayerPos);
                    boolean isCorrectBlock = false;
                    
                    if (Math.abs(x) == 5) {
                        // 纵向边缘（东西边界，x=±5）应该是A类型（上下前后带金线）
                        isCorrectBlock = firstLayerState.isOf(com.example.mythic_age.block.ModBlocks.ABUNDANCE_ALTAR_BRICK_GOLD_H);
                    } else {
                        // 横向边缘（南北边界，z=±5）应该是E类型（前后左右带金线）
                        isCorrectBlock = firstLayerState.isOf(com.example.mythic_age.block.ModBlocks.ABUNDANCE_ALTAR_BRICK_GOLD_V);
                    }
                    
                    if (isCorrectBlock) {
                        // 检查上方是否可以放置
                        if (isValidGenerationPosition(topPos)) {
                            positions.add(topPos);
                        }
                    }
                }
            }
        }
        
        return positions;
    }

    /**
     * 检查位置是否适合生成资源
     */
    private boolean isValidGenerationPosition(BlockPos checkPos) {
        if (world == null) return false;
        
        BlockState state = world.getBlockState(checkPos);
        
        // 只在空气方块中生成
        if (!state.isAir()) return false;
        
        // 确保下方有实体方块支撑
        BlockState belowState = world.getBlockState(checkPos.down());
        if (!belowState.isSolidBlock(world, checkPos.down())) return false;
        
        // 避免在柱子位置生成
        if (world.getBlockState(checkPos).isOf(ModBlocks.ABUNDANCE_ALTAR_PILLAR)) return false;
        
        return true;
    }

    /**
     * 随机选择要生成的资源类型
     */
    private Block selectRandomResource(ServerWorld world) {
        float random = world.random.nextFloat();
        
        if (random < 0.35f) {
            // 35% 概率生成一堆肉
            return ModBlocks.MEAT_PILE;
        } else if (random < 0.65f) {
            // 30% 概率生成一堆木头
            return ModBlocks.WOOD_PILE;
        } else if (random < 0.90f) {
            // 25% 概率生成矿物方块
            return selectRandomOreBlock(world);
        } else {
            // 10% 概率生成特殊珍贵方块
            return selectRandomPreciousBlock(world);
        }
    }

    /**
     * 随机选择矿物方块
     */
    private Block selectRandomOreBlock(ServerWorld world) {
        Block[] oreBlocks = {
            Blocks.IRON_BLOCK,
            Blocks.GOLD_BLOCK,
            Blocks.DIAMOND_BLOCK,
            Blocks.EMERALD_BLOCK,
            Blocks.COAL_BLOCK,
            Blocks.COPPER_BLOCK,
            Blocks.LAPIS_BLOCK,
            Blocks.REDSTONE_BLOCK
        };
        
        return oreBlocks[world.random.nextInt(oreBlocks.length)];
    }

    /**
     * 随机选择珍贵方块
     */
    private Block selectRandomPreciousBlock(ServerWorld world) {
        Block[] preciousBlocks = {
            Blocks.NETHERITE_BLOCK,
            Blocks.BEACON,
            Blocks.ENCHANTING_TABLE,
            Blocks.ANVIL,
            Blocks.ENDER_CHEST,
            Blocks.SHULKER_BOX
        };
        
        return preciousBlocks[world.random.nextInt(preciousBlocks.length)];
    }

    /**
     * 安排下次资源生成的时间
     */
    private void scheduleNextGeneration() {
        if (world != null) {
            long currentTime = world.getTime();
            int interval = MIN_GENERATION_INTERVAL + 
                         world.random.nextInt(MAX_GENERATION_INTERVAL - MIN_GENERATION_INTERVAL);
            nextGenerationTime = currentTime + interval;
        }
    }

    /**
     * 获取距离下次生成还有多少秒
     */
    public int getTimeUntilNextGeneration() {
        if (world == null) return 0;
        
        long remainingTicks = nextGenerationTime - world.getTime();
        return Math.max(0, (int)(remainingTicks / 20)); // 转换为秒
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putLong("NextGenerationTime", nextGenerationTime);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        nextGenerationTime = nbt.getLong("NextGenerationTime");
    }
} 