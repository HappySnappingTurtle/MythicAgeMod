package com.example.mythic_age.util;

import com.example.mythic_age.particle.MythicParticleEffects;
import com.example.mythic_age.world.MythicAgeWorldState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 闪电风暴管理器
 * 
 * 管理持续性闪电风暴效果的全局系统，处理天气控制和生物攻击。
 * 风暴期间会改变天气为下雨，并持续攻击非玩家生物。
 * 
 * 功能特性：
 * - 天气控制：开始时下雨，结束时晴天
 * - 生物搜索：每3秒搜索50格范围内的非玩家生物
 * - 闪电攻击：75%概率对生物产生闪电攻击
 * - 持续时间：风暴持续60秒
 * - 视觉效果：雷云和闪电效果
 * 
 * @author MythicAge Development Team
 * @version 1.0.0
 * @since 1.20.1
 */
public class LightningStormManager {
    
    /** 单例实例 */
    private static LightningStormManager instance;
    
    /** 活跃的闪电风暴数据 */
    private final Map<String, LightningStormData> activeStorms = new HashMap<>();
    
    /** 风暴持续时间 (30秒 = 600 ticks) */
    private static final int STORM_DURATION = 600;
    
    /** 闪电攻击间隔 (5秒 = 100 ticks) */
    private static final int LIGHTNING_INTERVAL = 100;
    
    /** 生物搜索范围 */
    private static final double CREATURE_SEARCH_RANGE = 50.0;
    
    /** 闪电命中概率 */
    private static final float LIGHTNING_HIT_CHANCE = 0.75f;
    
    private LightningStormManager() {}
    
    public static LightningStormManager getInstance() {
        if (instance == null) {
            instance = new LightningStormManager();
        }
        return instance;
    }
    
    /**
     * 启动一个新闪电风暴
     */
    public void startLightningStorm(World world, BlockPos center, String stormId) {
        MythicAgeWorldState worldState = null;
        if (world instanceof ServerWorld serverWorld) {
            worldState = MythicAgeWorldState.get(serverWorld.getServer());
        }
        activeStorms.put(stormId, new LightningStormData(world.getRegistryKey().getValue().toString(), center, 0, 0));
        
        // 改变天气为下雨
        if (!world.isClient) {
            ServerWorld serverWorld = (ServerWorld) world;
            serverWorld.setWeather(0, STORM_DURATION, true, true); // 雨天+雷暴
            if (worldState != null) {
                worldState.markDirty();
            }
        }
        
        // 立即进行第一轮闪电攻击
        performLightningAttack(world, center);
    }
    
    /**
     * 每tick调用的更新方法
     */
    public boolean tick(World world) {
        if (world.isClient) return false;

        boolean dirty = false;
        
        Iterator<Map.Entry<String, LightningStormData>> iterator = activeStorms.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<String, LightningStormData> entry = iterator.next();
            LightningStormData data = entry.getValue();
            if (!data.dimension.equals(world.getRegistryKey().getValue().toString())) {
                continue;
            }
            
            data.ticksAlive++;
            data.lightningTimer++;
            
            // 检查是否超过持续时间
            if (data.ticksAlive >= STORM_DURATION) {
                // 风暴结束
                performStormEndEffect(world, data.center);
                iterator.remove();
                dirty = true;
                continue;
            }
            
            // 检查是否到了下次闪电攻击的时间
            if (data.lightningTimer >= LIGHTNING_INTERVAL) {
                data.lightningTimer = 0;
                performLightningAttack(world, data.center);
            }
            dirty = true;
        }
        return dirty;
    }

    public NbtCompound writePersistentState() {
        NbtCompound root = new NbtCompound();
        NbtList storms = new NbtList();
        for (Map.Entry<String, LightningStormData> entry : activeStorms.entrySet()) {
            NbtCompound storm = new NbtCompound();
            LightningStormData data = entry.getValue();
            storm.putString("Id", entry.getKey());
            storm.putString("Dimension", data.dimension);
            storm.putLong("Center", data.center.asLong());
            storm.putInt("TicksAlive", data.ticksAlive);
            storm.putInt("RemainingTicks", Math.max(0, STORM_DURATION - data.ticksAlive));
            storm.putInt("LightningTimer", data.lightningTimer);
            storms.add(storm);
        }
        root.put("ActiveStorms", storms);
        return root;
    }

    public void readPersistentState(NbtCompound nbt) {
        activeStorms.clear();
        NbtList storms = nbt.getList("ActiveStorms", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < storms.size(); i++) {
            NbtCompound storm = storms.getCompound(i);
            String id = storm.getString("Id");
            String dimension = storm.getString("Dimension");
            if (id.isEmpty() || dimension.isEmpty() || !storm.contains("Center", NbtElement.LONG_TYPE)) {
                continue;
            }
            int ticksAlive = storm.contains("TicksAlive", NbtElement.INT_TYPE)
                    ? storm.getInt("TicksAlive")
                    : Math.max(0, STORM_DURATION - storm.getInt("RemainingTicks"));
            if (ticksAlive >= STORM_DURATION) {
                continue;
            }
            int lightningTimer = storm.getInt("LightningTimer");
            activeStorms.put(id, new LightningStormData(dimension, BlockPos.fromLong(storm.getLong("Center")), ticksAlive, lightningTimer));
        }
    }
    
    /**
     * 执行闪电攻击
     */
    private void performLightningAttack(World world, BlockPos center) {
        ServerWorld serverWorld = (ServerWorld) world;
        int struckCreatures = 0;
        
        // 搜索范围内的所有非玩家生物
        List<LivingEntity> creatures = world.getEntitiesByClass(LivingEntity.class,
            Box.of(center.toCenterPos(), CREATURE_SEARCH_RANGE, CREATURE_SEARCH_RANGE, CREATURE_SEARCH_RANGE),
            entity -> !(entity instanceof PlayerEntity) && entity.isAlive());
        
        for (LivingEntity creature : creatures) {
            // 75%概率命中
            if (world.random.nextFloat() < LIGHTNING_HIT_CHANCE) {
                BlockPos creaturePos = creature.getBlockPos();
                
                // 创建闪电实体
                LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, world);
                lightning.refreshPositionAfterTeleport(
                    creaturePos.getX() + 0.5, 
                    creaturePos.getY(), 
                    creaturePos.getZ() + 0.5
                );
                world.spawnEntity(lightning);
                
                // 直接杀死生物
                creature.damage(world.getDamageSources().lightningBolt(), Float.MAX_VALUE);
                
                // 死亡粒子效果
                MythicParticleEffects.spawnNormal(serverWorld, ParticleTypes.SOUL,
                    creaturePos.getX() + 0.5, creaturePos.getY() + 1, creaturePos.getZ() + 0.5,
                    10, 0.5, 0.5, 0.5, 0.1);
                
                struckCreatures++;
            }
        }
        
        // 如果没有生物被击中，在中心位置产生一些闪电效果
        if (struckCreatures == 0 && !creatures.isEmpty()) {
            // 随机在范围内产生装饰性闪电
            for (int i = 0; i < 3; i++) {
                double offsetX = (world.random.nextDouble() - 0.5) * 20;
                double offsetZ = (world.random.nextDouble() - 0.5) * 20;
                BlockPos lightningPos = center.add((int)offsetX, 0, (int)offsetZ);
                
                LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, world);
                lightning.refreshPositionAfterTeleport(
                    lightningPos.getX() + 0.5, 
                    lightningPos.getY(), 
                    lightningPos.getZ() + 0.5
                );
                world.spawnEntity(lightning);
            }
        }
        
        // 风暴中心的雷云效果
        MythicParticleEffects.spawnMajor(serverWorld, ParticleTypes.CLOUD,
            center.getX() + 0.5, center.getY() + 20, center.getZ() + 0.5,
            15, 10.0, 3.0, 10.0, 0.1);
        
        // 雷声音效
        world.playSound(null, center, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 
            SoundCategory.WEATHER, 1.5f, 0.8f + world.random.nextFloat() * 0.4f);
        
        // 如果击中了生物，播放额外的音效
        if (struckCreatures > 0) {
            world.playSound(null, center, SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, 
                SoundCategory.HOSTILE, 1.0f, 1.0f);
        }
    }
    
    /**
     * 风暴结束效果
     */
    private void performStormEndEffect(World world, BlockPos center) {
        ServerWorld serverWorld = (ServerWorld) world;
        
        // 改变天气为晴天
        serverWorld.setWeather(6000, 0, false, false); // 晴天持续5分钟
        
        // 风暴散去效果
        MythicParticleEffects.spawnMajor(serverWorld, ParticleTypes.SWEEP_ATTACK,
            center.getX() + 0.5, center.getY() + 10, center.getZ() + 0.5,
            20, 15.0, 5.0, 15.0, 0.2);
        
        // 风暴结束音效
        world.playSound(null, center, SoundEvents.ITEM_TRIDENT_THUNDER, 
            SoundCategory.WEATHER, 1.0f, 1.2f);
    }
    
    /**
     * 闪电风暴数据类
     */
    private static class LightningStormData {
        public final String dimension;
        public final BlockPos center;
        public int ticksAlive;
        public int lightningTimer;
        
        public LightningStormData(String dimension, BlockPos center, int ticksAlive, int lightningTimer) {
            this.dimension = dimension;
            this.center = center;
            this.ticksAlive = ticksAlive;
            this.lightningTimer = lightningTimer;
        }
    }
} 
