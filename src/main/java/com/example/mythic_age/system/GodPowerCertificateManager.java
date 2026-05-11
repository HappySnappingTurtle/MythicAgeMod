package com.example.mythic_age.system;

import com.example.mythic_age.item.ModItems;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

/**
 * 神力证明系统管理器
 * 
 * 负责管理不同文明的神力证明获取方式。
 * 
 * 获取方式：
 * - 希腊：神庙周围对应文明村民数量
 * - 埃及：神像基座周期性产出
 * - 北欧：击杀怪物或其他玩家掉落
 * - 中国：基于村庄人口数量
 * - 亚特兰蒂斯：海晶碎片等海洋资源与神殿守护者交易
 * 
 * @author MythicAge Development Team
 * @version 1.0.0
 * @since 1.20.1
 */
public class GodPowerCertificateManager {
    
    private static final GodPowerCertificateManager INSTANCE = new GodPowerCertificateManager();
    
    // 埃及神像基座数据
    private final Map<BlockPos, EgyptianStatueData> egyptianStatues = new HashMap<>();
    
    public static GodPowerCertificateManager getInstance() {
        return INSTANCE;
    }

    /**
     * 北欧神力证明击杀入口。
     *
     * 只有玩家击杀敌对怪物或其他玩家时才触发，避免刷动物等非战斗行为产出证明。
     */
    public void handleNordicKill(ServerWorld world, PlayerEntity killer, LivingEntity killedEntity) {
        if (world == null || killer == null || killedEntity == null || killer == killedEntity) {
            return;
        }

        String entityType = Registries.ENTITY_TYPE.getId(killedEntity.getType()).getPath();
        if (killedEntity instanceof PlayerEntity) {
            dropNordicCertificate(world, killedEntity.getBlockPos(), calculateNordicDropAmount("player"));
            return;
        }

        if (!isNordicMonsterTarget(killedEntity, entityType)) {
            return;
        }

        int dropChance = calculateNordicDropChance(entityType);
        if (world.getRandom().nextInt(100) < dropChance) {
            dropNordicCertificate(world, killedEntity.getBlockPos(), calculateNordicDropAmount(entityType));
        }
    }

    private boolean isNordicMonsterTarget(LivingEntity killedEntity, String entityType) {
        return killedEntity instanceof HostileEntity ||
            entityType.equals("ender_dragon") ||
            entityType.equals("wither") ||
            entityType.equals("slime") ||
            entityType.equals("magma_cube");
    }

    private void dropNordicCertificate(World world, BlockPos pos, int amount) {
        if (amount <= 0) {
            return;
        }

        ItemStack certificate = new ItemStack(ModItems.GOD_POWER_CERTIFICATE_NORDIC, amount);
        world.spawnEntity(new net.minecraft.entity.ItemEntity(
            world,
            pos.getX() + 0.5,
            pos.getY() + 0.5,
            pos.getZ() + 0.5,
            certificate
        ));
    }
    
    /**
     * 计算北欧掉落概率
     */
    private int calculateNordicDropChance(String entityType) {
        return switch (entityType.toLowerCase()) {
            case "player" -> 100; // PvP胜利必定产出
            case "zombie", "skeleton" -> 5; // 5%
            case "creeper", "spider" -> 8; // 8%
            case "slime", "magma_cube", "drowned", "guardian" -> 10; // 10%
            case "enderman", "witch" -> 15; // 15%
            case "wither_skeleton", "blaze", "elder_guardian" -> 20; // 20%
            case "ender_dragon", "wither" -> 100; // 100% (BOSS)
            default -> 3; // 3% 默认
        };
    }
    
    /**
     * 计算北欧掉落数量
     */
    private int calculateNordicDropAmount(String entityType) {
        return switch (entityType.toLowerCase()) {
            case "player" -> 3;
            case "ender_dragon" -> 50;
            case "wither" -> 30;
            case "wither_skeleton", "blaze", "elder_guardian" -> 3;
            case "enderman", "witch" -> 2;
            default -> 1;
        };
    }
    
    /**
     * 中国神力证明基于人口产出
     * 
     * @param world 世界
     * @param villageCenter 村庄中心
     * @param populationCount 人口数量
     */
    public void handleChinesePopulationGeneration(ServerWorld world, BlockPos villageCenter, int populationCount) {
        // 每20个村民产出1个神力证明，每分钟检查一次
        int baseAmount = populationCount / 20;
        int bonusChance = (populationCount % 20) * 5; // 剩余人口提供概率奖励
        
        int totalAmount = baseAmount;
        if (world.getRandom().nextInt(100) < bonusChance) {
            totalAmount++;
        }
        
        if (totalAmount > 0) {
            ItemStack certificate = new ItemStack(ModItems.GOD_POWER_CERTIFICATE_CHINESE, totalAmount);
            
            // 在村庄中心附近掉落
            world.spawnEntity(new net.minecraft.entity.ItemEntity(world, 
                villageCenter.getX() + 0.5, villageCenter.getY() + 1, villageCenter.getZ() + 0.5, certificate));
        }
    }
    
    /**
     * 注册埃及神像基座
     */
    public void registerEgyptianStatue(BlockPos pos, EgyptianStatueType type) {
        egyptianStatues.put(pos, new EgyptianStatueData(type));
    }
    
    /**
     * 移除埃及神像基座
     */
    public void unregisterEgyptianStatue(BlockPos pos) {
        egyptianStatues.remove(pos);
    }
    
    /**
     * 埃及神像周期性产出神力证明
     */
    public void tickEgyptianStatues(ServerWorld world) {
        egyptianStatues.entrySet().removeIf(entry -> {
            BlockPos pos = entry.getKey();
            EgyptianStatueData data = entry.getValue();
            
            // 检查方块是否还存在
            if (!world.getBlockState(pos).getBlock().toString().contains("egyptian_statue")) {
                return true; // 移除不存在的基座
            }
            
            data.tickCount++;
            if (data.tickCount >= data.type.getProductionInterval()) {
                data.tickCount = 0;
                
                // 产出神力证明
                int amount = data.type.getProductionAmount();
                ItemStack certificate = new ItemStack(ModItems.GOD_POWER_CERTIFICATE_EGYPTIAN, amount);
                
                world.spawnEntity(new net.minecraft.entity.ItemEntity(world, 
                    pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, certificate));
            }
            
            return false;
        });
    }
    
    /**
     * 埃及神像类型枚举
     */
    public enum EgyptianStatueType {
        SLAVE("slave", 1, 2400),      // 奴隶：1个/2分钟
        CITIZEN("citizen", 2, 2400),   // 市民：2个/2分钟
        SOLDIER("soldier", 3, 2400),   // 士兵：3个/2分钟
        QUEEN("queen", 5, 1800),       // 王后：5个/1.5分钟
        KING("king", 7, 1800),         // 国王：7个/1.5分钟
        ROYAL_COUPLE("royal_couple", 12, 1200), // 国王王后：12个/1分钟
        PHARAOH("pharaoh", 20, 1200);  // 法老：20个/1分钟
        
        private final String name;
        private final int productionAmount;
        private final int productionInterval; // 以tick为单位
        
        EgyptianStatueType(String name, int amount, int interval) {
            this.name = name;
            this.productionAmount = amount;
            this.productionInterval = interval;
        }
        
        public String getName() { return name; }
        public int getProductionAmount() { return productionAmount; }
        public int getProductionInterval() { return productionInterval; }
        
        public int getGoldCost() {
            return switch (this) {
                case SLAVE -> 5;
                case CITIZEN -> 10;
                case SOLDIER -> 20;
                case QUEEN -> 35;
                case KING -> 50;
                case ROYAL_COUPLE -> 80;
                case PHARAOH -> 120;
            };
        }
    }
    
    /**
     * 埃及神像数据
     */
    private static class EgyptianStatueData {
        final EgyptianStatueType type;
        int tickCount = 0;
        
        EgyptianStatueData(EgyptianStatueType type) {
            this.type = type;
        }
    }
}
