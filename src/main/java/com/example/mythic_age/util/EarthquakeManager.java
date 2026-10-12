package com.example.mythic_age.util;

import com.example.mythic_age.particle.MythicParticleEffects;
import com.example.mythic_age.world.MythicAgeWorldState;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 地震管理器
 * 
 * 管理持续性地震效果的全局系统，处理多个同时进行的地震。
 * 每个地震都有自己的震中、持续时间和强度设置。
 * 
 * 功能特性：
 * - 周期性地震：每2秒产生一次地震效果
 * - 方块破坏：90%概率破坏范围内的方块
 * - 持续时间：地震持续60秒
 * - 视觉效果：爆炸粒子和振动效果
 * - 音效系统：真实的地震音效
 * 
 * @author MythicAge Development Team
 * @version 1.0.0
 * @since 1.20.1
 */
public class EarthquakeManager {
    
    /** 单例实例 */
    private static EarthquakeManager instance;
    
    /** 活跃的地震数据 */
    private final Map<String, EarthquakeData> activeEarthquakes = new HashMap<>();
    
    /** 地震持续时间 (30秒 = 600 ticks) */
    private static final int EARTHQUAKE_DURATION = 600;
    
    /** 地震间隔 (2.5秒 = 50 ticks) */
    private static final int EARTHQUAKE_INTERVAL = 50;
    
    /** 地震范围 - 长度 */
    private static final int EARTHQUAKE_LENGTH = 100;
    
    /** 地震范围 - 宽度 */
    private static final int EARTHQUAKE_WIDTH = 30;
    
    /** 方块破坏概率 */
    private static final float BLOCK_DESTRUCTION_CHANCE = 0.9f;
    
    private EarthquakeManager() {}
    
    public static EarthquakeManager getInstance() {
        if (instance == null) {
            instance = new EarthquakeManager();
        }
        return instance;
    }
    
    /**
     * 启动一个新地震
     */
    public void startEarthquake(World world, BlockPos epicenter, String earthquakeId, float playerYaw) {
        MythicAgeWorldState worldState = null;
        if (world instanceof ServerWorld serverWorld) {
            worldState = MythicAgeWorldState.get(serverWorld.getServer());
        }
        activeEarthquakes.put(earthquakeId, new EarthquakeData(world.getRegistryKey().getValue().toString(), epicenter, 0, 0, playerYaw));
        if (worldState != null) {
            worldState.markDirty();
        }
        
        // 立即进行第一次地震效果
        performEarthquakeEffect(world, epicenter, playerYaw);
    }
    
    /**
     * 每tick调用的更新方法
     */
    public boolean tick(World world) {
        if (world.isClient) return false;

        boolean dirty = false;
        
        Iterator<Map.Entry<String, EarthquakeData>> iterator = activeEarthquakes.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<String, EarthquakeData> entry = iterator.next();
            EarthquakeData data = entry.getValue();
            if (!data.dimension.equals(world.getRegistryKey().getValue().toString())) {
                continue;
            }
            
            data.ticksAlive++;
            data.intervalTimer++;
            
            // 检查是否超过持续时间
            if (data.ticksAlive >= EARTHQUAKE_DURATION) {
                // 地震结束
                performEarthquakeEndEffect(world, data.epicenter);
                iterator.remove();
                dirty = true;
                continue;
            }
            
            // 检查是否到了下次地震的时间
            if (data.intervalTimer >= EARTHQUAKE_INTERVAL) {
                data.intervalTimer = 0;
                performEarthquakeEffect(world, data.epicenter, data.playerYaw);
            }
            dirty = true;
        }
        return dirty;
    }

    public NbtCompound writePersistentState() {
        NbtCompound root = new NbtCompound();
        NbtList earthquakes = new NbtList();
        for (Map.Entry<String, EarthquakeData> entry : activeEarthquakes.entrySet()) {
            NbtCompound earthquake = new NbtCompound();
            EarthquakeData data = entry.getValue();
            earthquake.putString("Id", entry.getKey());
            earthquake.putString("Dimension", data.dimension);
            earthquake.putLong("Epicenter", data.epicenter.asLong());
            earthquake.putInt("TicksAlive", data.ticksAlive);
            earthquake.putInt("RemainingTicks", Math.max(0, EARTHQUAKE_DURATION - data.ticksAlive));
            earthquake.putInt("IntervalTimer", data.intervalTimer);
            earthquake.putFloat("PlayerYaw", data.playerYaw);
            earthquakes.add(earthquake);
        }
        root.put("ActiveEarthquakes", earthquakes);
        return root;
    }

    public void readPersistentState(NbtCompound nbt) {
        activeEarthquakes.clear();
        NbtList earthquakes = nbt.getList("ActiveEarthquakes", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < earthquakes.size(); i++) {
            NbtCompound earthquake = earthquakes.getCompound(i);
            String id = earthquake.getString("Id");
            String dimension = earthquake.getString("Dimension");
            if (id.isEmpty() || dimension.isEmpty() || !earthquake.contains("Epicenter", NbtElement.LONG_TYPE)) {
                continue;
            }
            int ticksAlive = earthquake.contains("TicksAlive", NbtElement.INT_TYPE)
                    ? earthquake.getInt("TicksAlive")
                    : Math.max(0, EARTHQUAKE_DURATION - earthquake.getInt("RemainingTicks"));
            if (ticksAlive >= EARTHQUAKE_DURATION) {
                continue;
            }
            int intervalTimer = earthquake.getInt("IntervalTimer");
            float playerYaw = earthquake.getFloat("PlayerYaw");
            activeEarthquakes.put(id, new EarthquakeData(dimension, BlockPos.fromLong(earthquake.getLong("Epicenter")), ticksAlive, intervalTimer, playerYaw));
        }
    }
    
    /**
     * 执行地震效果
     */
    private void performEarthquakeEffect(World world, BlockPos epicenter, float playerYaw) {
        ServerWorld serverWorld = (ServerWorld) world;
        int destroyedBlocks = 0;
        
        // 计算玩家朝向的单位向量
        double yawRad = Math.toRadians(playerYaw);
        double forwardX = -Math.sin(yawRad);
        double forwardZ = Math.cos(yawRad);
        double rightX = Math.cos(yawRad);
        double rightZ = Math.sin(yawRad);
        
        // 在30x100的矩形范围内破坏方块
        for (int forward = 0; forward < EARTHQUAKE_LENGTH; forward++) {
            for (int right = -EARTHQUAKE_WIDTH / 2; right <= EARTHQUAKE_WIDTH / 2; right++) {
                for (int y = -3; y <= 3; y++) {
                    // 计算相对于震中的实际位置
                    int offsetX = (int)(forward * forwardX + right * rightX);
                    int offsetZ = (int)(forward * forwardZ + right * rightZ);
                    BlockPos pos = epicenter.add(offsetX, y, offsetZ);
                    
                    BlockState state = world.getBlockState(pos);
                    
                    // 只影响特定方块（不是基岩、黑曜石等）
                    if (!state.isAir() && 
                        state.getBlock() != Blocks.BEDROCK && 
                        state.getBlock() != Blocks.OBSIDIAN &&
                        state.getBlock() != Blocks.BARRIER &&
                        state.getHardness(world, pos) >= 0) {
                        
                        // 90%概率破坏方块
                        if (world.random.nextFloat() < BLOCK_DESTRUCTION_CHANCE) {
                            world.breakBlock(pos, true);
                            destroyedBlocks++;
                        }
                    }
                }
            }
        }
        
        // 视觉效果 - 沿矩形范围的爆炸粒子
        for (int forward = 0; forward < EARTHQUAKE_LENGTH; forward += 5) {
            for (int right = -EARTHQUAKE_WIDTH / 2; right <= EARTHQUAKE_WIDTH / 2; right += 3) {
                int offsetX = (int)(forward * forwardX + right * rightX);
                int offsetZ = (int)(forward * forwardZ + right * rightZ);
                
                MythicParticleEffects.spawnAmbient(serverWorld, ParticleTypes.EXPLOSION,
                    epicenter.getX() + offsetX, epicenter.getY() + 1, epicenter.getZ() + offsetZ, 
                    2, 0.5, 0.5, 0.5, 0.1);
            }
        }
        
        // 震中强烈爆炸效果
        MythicParticleEffects.spawnUltimate(serverWorld, ParticleTypes.EXPLOSION_EMITTER,
            epicenter.getX() + 0.5, epicenter.getY() + 1, epicenter.getZ() + 0.5,
            1, 0, 0, 0, 0);
        
        // 地面裂纹效果 - 沿矩形范围
        for (int i = 0; i < 50; i++) {
            int randomForward = world.random.nextInt(EARTHQUAKE_LENGTH);
            int randomRight = world.random.nextInt(EARTHQUAKE_WIDTH) - EARTHQUAKE_WIDTH / 2;
            
            int offsetX = (int)(randomForward * forwardX + randomRight * rightX);
            int offsetZ = (int)(randomForward * forwardZ + randomRight * rightZ);
            
            MythicParticleEffects.spawnAmbient(serverWorld, ParticleTypes.CRIT,
                epicenter.getX() + offsetX, epicenter.getY(), epicenter.getZ() + offsetZ,
                3, 0.3, 0.1, 0.3, 0.1);
        }
        
        // 音效
        world.playSound(null, epicenter, SoundEvents.ENTITY_GENERIC_EXPLODE, 
            SoundCategory.BLOCKS, 2.0f, 0.3f + world.random.nextFloat() * 0.4f);
        
        // 如果破坏了很多方块，播放额外的崩塌音效
        if (destroyedBlocks > 20) {
            world.playSound(null, epicenter, SoundEvents.ENTITY_WITHER_BREAK_BLOCK, 
                SoundCategory.BLOCKS, 1.5f, 0.5f);
        }
    }
    
    /**
     * 地震结束效果
     */
    private void performEarthquakeEndEffect(World world, BlockPos epicenter) {
        ServerWorld serverWorld = (ServerWorld) world;
        
        // 地震平息效果
        MythicParticleEffects.spawnMajor(serverWorld, ParticleTypes.CLOUD,
            epicenter.getX() + 0.5, epicenter.getY() + 2, epicenter.getZ() + 0.5,
            50, EARTHQUAKE_WIDTH, 2.0, EARTHQUAKE_LENGTH / 2, 0.1);
        
        // 平息音效
        world.playSound(null, epicenter, SoundEvents.BLOCK_FIRE_EXTINGUISH, 
            SoundCategory.BLOCKS, 1.0f, 0.5f);
    }
    
    /**
     * 地震数据类
     */
    private static class EarthquakeData {
        public final String dimension;
        public final BlockPos epicenter;
        public final float playerYaw;
        public int ticksAlive;
        public int intervalTimer;
        
        public EarthquakeData(String dimension, BlockPos epicenter, int ticksAlive, int intervalTimer, float playerYaw) {
            this.dimension = dimension;
            this.epicenter = epicenter;
            this.ticksAlive = ticksAlive;
            this.intervalTimer = intervalTimer;
            this.playerYaw = playerYaw;
        }
    }
} 
