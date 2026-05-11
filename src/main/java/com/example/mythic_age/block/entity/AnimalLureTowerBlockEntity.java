package com.example.mythic_age.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 动物招引塔方块实体
 * 
 * 动物招引法术生成的多方块结构的核心组件，负责：
 * - 周期性吸引周围20格范围内的动物到塔周围
 * - 在5格半径内快速生长草和草方块
 * - 维持法术效果持续5分钟
 * 
 * 结构组成：
 * - 第一层：黑曜石（基础层）
 * - 第二层：钻石块（核心层，此方块实体）
 * - 第三层：石头（顶层）
 * 
 * 功能特性：
 * - 动物吸引：每30秒吸引一次周围的动物
 * - 草地生长：每10秒在周围生长草和草方块
 * - 自动清理：5分钟后自动消失
 * - 视觉效果：周期性粒子效果和音效
 * 
 * @author MythicAge Development Team
 * @version 1.0.0
 * @since 1.20.1
 */
public class AnimalLureTowerBlockEntity extends BlockEntity {
    
    /** 塔的生存时间 (5分钟 = 6000 ticks) */
    private static final int TOWER_LIFETIME = 6000;
    
    /** 动物吸引间隔 (30秒 = 600 ticks) */
    private static final int LURE_INTERVAL = 600;
    
    /** 草地生长间隔 (10秒 = 200 ticks) */
    private static final int GRASS_GROWTH_INTERVAL = 200;
    
    /** 动物搜索范围 */
    private static final double ANIMAL_SEARCH_RANGE = 20.0;
    
    /** 草地生长范围 */
    private static final int GRASS_GROWTH_RANGE = 5;
    
    /** 当前存活时间 */
    private int ticksAlive = 0;
    
    /** 动物吸引计时器 */
    private int lureTimer = 0;
    
    /** 草地生长计时器 */
    private int grassTimer = 0;

    public AnimalLureTowerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ANIMAL_LURE_TOWER, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, AnimalLureTowerBlockEntity blockEntity) {
        if (world.isClient) return;
        
        blockEntity.ticksAlive++;
        blockEntity.lureTimer++;
        blockEntity.grassTimer++;
        
        // 检查是否超过生存时间
        if (blockEntity.ticksAlive >= TOWER_LIFETIME) {
            blockEntity.destroyTower(world, pos);
            return;
        }
        
        // 动物吸引逻辑
        if (blockEntity.lureTimer >= LURE_INTERVAL) {
            blockEntity.lureTimer = 0;
            blockEntity.lureAnimals(world, pos);
        }
        
        // 草地生长逻辑
        if (blockEntity.grassTimer >= GRASS_GROWTH_INTERVAL) {
            blockEntity.grassTimer = 0;
            blockEntity.growGrass(world, pos);
        }
        
        // 每5秒显示一次粒子效果
        if (blockEntity.ticksAlive % 100 == 0) {
            blockEntity.showParticleEffects(world, pos);
        }
        
        blockEntity.markDirty();
    }
    
    /**
     * 吸引周围的动物
     */
    private void lureAnimals(World world, BlockPos pos) {
        ServerWorld serverWorld = (ServerWorld) world;
        
        // 搜索周围的动物
        List<AnimalEntity> animals = world.getEntitiesByClass(AnimalEntity.class,
            Box.of(pos.toCenterPos(), ANIMAL_SEARCH_RANGE, ANIMAL_SEARCH_RANGE, ANIMAL_SEARCH_RANGE),
            animal -> true);
        
        for (AnimalEntity animal : animals) {
            // 让动物移向塔的位置
            animal.getNavigation().startMovingTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.0);
            
            // 给动物一个临时的速度提升
            animal.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                net.minecraft.entity.effect.StatusEffects.SPEED, 200, 0)); // 10秒速度I
        }
        
        if (!animals.isEmpty()) {
            // 播放吸引音效
            world.playSound(null, pos, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 
                SoundCategory.BLOCKS, 1.0f, 1.2f);
            
            // 显示吸引粒子效果
            serverWorld.spawnParticles(ParticleTypes.HEART, 
                pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 0.5,
                animals.size() * 2, 2.0, 1.0, 2.0, 0.1);
        }
    }
    
    /**
     * 在周围生长草和草方块
     */
    private void growGrass(World world, BlockPos pos) {
        ServerWorld serverWorld = (ServerWorld) world;
        int grownBlocks = 0;
        
        // 在5x5范围内随机生长草
        for (int x = -GRASS_GROWTH_RANGE; x <= GRASS_GROWTH_RANGE; x++) {
            for (int z = -GRASS_GROWTH_RANGE; z <= GRASS_GROWTH_RANGE; z++) {
                BlockPos checkPos = pos.add(x, -1, z); // 检查塔下方的地面
                
                // 30%概率生长
                if (world.random.nextFloat() < 0.3f) {
                    BlockState currentState = world.getBlockState(checkPos);
                    BlockState aboveState = world.getBlockState(checkPos.up());
                    
                    // 将泥土转换为草方块
                    if (currentState.isOf(Blocks.DIRT) && aboveState.isAir()) {
                        world.setBlockState(checkPos, Blocks.GRASS_BLOCK.getDefaultState());
                        grownBlocks++;
                    }
                    // 在草方块上生长草
                    else if (currentState.isOf(Blocks.GRASS_BLOCK) && aboveState.isAir()) {
                        world.setBlockState(checkPos.up(), Blocks.GRASS.getDefaultState());
                        grownBlocks++;
                    }
                }
            }
        }
        
        if (grownBlocks > 0) {
            // 显示生长粒子效果
            serverWorld.spawnParticles(ParticleTypes.HAPPY_VILLAGER, 
                pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5,
                grownBlocks * 3, GRASS_GROWTH_RANGE, 1.0, GRASS_GROWTH_RANGE, 0.1);
            
            // 播放生长音效
            world.playSound(null, pos, SoundEvents.ITEM_BONE_MEAL_USE, 
                SoundCategory.BLOCKS, 0.5f, 1.0f + world.random.nextFloat() * 0.4f);
        }
    }
    
    /**
     * 显示粒子效果
     */
    private void showParticleEffects(World world, BlockPos pos) {
        ServerWorld serverWorld = (ServerWorld) world;
        
        // 围绕塔显示魔法粒子
        for (int i = 0; i < 8; i++) {
            double angle = (i / 8.0) * 2 * Math.PI;
            double radius = 2.0;
            double x = pos.getX() + 0.5 + Math.cos(angle) * radius;
            double z = pos.getZ() + 0.5 + Math.sin(angle) * radius;
            
            serverWorld.spawnParticles(ParticleTypes.ENCHANT,
                x, pos.getY() + 1.5, z, 1, 0, 0, 0, 0);
        }
        
        // 塔顶闪烁效果
        serverWorld.spawnParticles(ParticleTypes.ENCHANTED_HIT,
            pos.getX() + 0.5, pos.getY() + 3, pos.getZ() + 0.5,
            5, 0.5, 0.5, 0.5, 0.1);
    }
    
    /**
     * 销毁塔结构 - 5层十字形结构
     */
    private void destroyTower(World world, BlockPos pos) {
        ServerWorld serverWorld = (ServerWorld) world;
        
        // 定义十字形相对位置 (中心点 + 四个方向)
        BlockPos[] crossPositions = {
            new BlockPos(0, 0, 0),    // 中心
            new BlockPos(1, 0, 0),    // 东
            new BlockPos(-1, 0, 0),   // 西
            new BlockPos(0, 0, 1),    // 南
            new BlockPos(0, 0, -1)    // 北
        };
        
        // 显示消失效果
        serverWorld.spawnParticles(ParticleTypes.POOF,
            pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
            30, 2.0, 2.0, 2.0, 0.1);
        
        // 播放消失音效
        world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_EXTINGUISH_FIRE, 
            SoundCategory.BLOCKS, 1.0f, 0.8f);
        
        // 移除5层十字形结构
        // 当前方块位置是第3层的核心方块位置（不一定是中心）
        // 需要找到第1层的中心位置
        BlockState currentState = world.getBlockState(pos);
        Direction facing = currentState.get(com.example.mythic_age.block.AnimalLureTowerBlock.FACING);
        
        // 从核心方块位置计算回第1层中心位置
        BlockPos layerCenterOffset = new BlockPos(-facing.getOffsetX(), 0, -facing.getOffsetZ());
        BlockPos basePos = pos.down(2).add(layerCenterOffset); // 第1层的中心位置
        
        // 移除第1-4层的十字形结构
        for (int layer = 0; layer < 4; layer++) {
            for (BlockPos relPos : crossPositions) {
                BlockPos targetPos = basePos.up(layer).add(relPos);
                if (world.getBlockState(targetPos).isOf(com.example.mythic_age.block.ModBlocks.ANIMAL_LURE_TOWER) ||
                    world.getBlockState(targetPos).isOf(Blocks.MOSSY_COBBLESTONE)) {
                    world.setBlockState(targetPos, Blocks.AIR.getDefaultState());
                }
            }
        }
        
        // 移除第5层的中心方块
        BlockPos topPos = basePos.up(4);
        if (world.getBlockState(topPos).isOf(Blocks.MOSSY_COBBLESTONE)) {
            world.setBlockState(topPos, Blocks.AIR.getDefaultState());
        }
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.ticksAlive = nbt.getInt("ticksAlive");
        this.lureTimer = nbt.getInt("lureTimer");
        this.grassTimer = nbt.getInt("grassTimer");
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putInt("ticksAlive", this.ticksAlive);
        nbt.putInt("lureTimer", this.lureTimer);
        nbt.putInt("grassTimer", this.grassTimer);
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }
} 