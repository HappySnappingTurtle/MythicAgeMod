package com.example.mythic_age.system;

import com.example.mythic_age.block.StatueBaseBlock;
import com.example.mythic_age.block.entity.StatueBaseBlockEntity;
import com.example.mythic_age.entity.CivilizedVillager;
import com.example.mythic_age.item.GodPowerCertificateItem;
import com.example.mythic_age.item.ModItems;
import net.minecraft.block.Block;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.List;

/**
 * 神力证明生成管理器
 * 
 * 负责管理基于村民数量的阶梯性神力证明生成系统：
 * 1. 检测神殿周围对应文明的村民数量
 * 2. 根据人数阶梯性生成神力证明
 * 3. 将生成的神力证明存储到对应文明的神像基座中
 * 4. 提供潜行右键获取神力证明的功能
 */
public class CertificateGenerationManager {
    
    // 神力证明生成阶梯配置
    private static final int[] VILLAGER_THRESHOLDS = {1, 3, 5, 8, 12, 16, 20}; // 村民数量阈值
    private static final int[] CERTIFICATE_AMOUNTS = {1, 2, 3, 5, 7, 10, 15}; // 对应的神力证明数量
    private static final int TEMPLE_RANGE = 32; // 神殿影响范围（格）
    private static final int GENERATION_INTERVAL = 2400; // 生成间隔（2分钟）
    
    /**
     * 检查并生成神力证明
     * 
     * @param world 服务器世界
     * @param templePos 希腊神殿位置
     * @param statueBasePos 神像基座位置
     */
    public static void checkAndGenerateCertificates(ServerWorld world, BlockPos templePos, BlockPos statueBasePos) {
        if (world == null || templePos == null || statueBasePos == null) {
            return;
        }
        
        // 统计神殿周围的希腊村民数量
        int greekVillagerCount = countCivilizedVillagersNearTemple(
            world, templePos, GodPowerCertificateItem.MythologyType.GREEK);
        
        if (greekVillagerCount > 0) {
            // 根据村民数量计算应生成的神力证明数量
            int certificateAmount = calculateCertificateAmount(greekVillagerCount);
            
            if (certificateAmount > 0) {
                // 生成神力证明并存储到神像基座
                generateAndStoreCertificates(
                    world,
                    statueBasePos,
                    certificateAmount,
                    greekVillagerCount,
                    GodPowerCertificateItem.MythologyType.GREEK
                );
            }
        }
    }
    
    /**
     * 统计神殿周围的希腊村民数量
     * 
     * @param world 服务器世界
     * @param templePos 神殿位置
     * @return 希腊村民数量
     */
    private static int countCivilizedVillagersNearTemple(ServerWorld world, BlockPos templePos,
                                                        GodPowerCertificateItem.MythologyType mythologyType) {
        // 搜索神殿周围的原版村民
        List<VillagerEntity> nearbyVillagers = world.getEntitiesByClass(
            VillagerEntity.class,
            net.minecraft.util.math.Box.of(
                net.minecraft.util.math.Vec3d.ofCenter(templePos),
                TEMPLE_RANGE * 2, TEMPLE_RANGE * 2, TEMPLE_RANGE * 2
            ),
            villager -> villager.isAlive() && 
                        villager instanceof CivilizedVillager civilizedVillager &&
                        civilizedVillager.mythicAge$getCivilization() == mythologyType
        );
        
        return nearbyVillagers.size();
    }
    
    /**
     * 为神殿生成神力证明
     * 
     * @param world 服务器世界
     * @param templePos 神殿位置
     * @param mythologyType 神话类型
     */
    public static void generateCertificatesForTemple(ServerWorld world, BlockPos templePos, 
                                                   GodPowerCertificateItem.MythologyType mythologyType) {
        
        if (mythologyType == null) {
            return;
        }
        
        // 统计神殿周围对应文明村民数量
        int civilizedVillagerCount = countCivilizedVillagersNearTemple(world, templePos, mythologyType);
        
        if (civilizedVillagerCount == 0) {
            return; // 没有对应文明村民，不生成证明
        }
        
        // 根据村民数量计算应生成的证明数量
        int certificatesToGenerate = calculateCertificateAmount(civilizedVillagerCount);
        
        if (certificatesToGenerate > 0) {
            // 寻找附近对应文明的神像基座
            BlockPos statueBasePos = findNearestStatueBase(world, templePos, mythologyType);
            
            if (statueBasePos != null) {
                // 将证明存储到神像基座
                storeCertificatesInStatueBase(world, statueBasePos, certificatesToGenerate);
                notifyNearbyPlayers(world, statueBasePos, certificatesToGenerate, civilizedVillagerCount, mythologyType);
                
                System.out.println(String.format(
                    "[神力证明系统] 在位置 %s 的%s神殿检测到 %d 个%s村民，生成 %d 个神力证明存储到基座 %s",
                    templePos, getCivilizationName(mythologyType), civilizedVillagerCount,
                    getCivilizationName(mythologyType), certificatesToGenerate, statueBasePos
                ));
            } else {
                dropCertificatesNearTemple(world, templePos, mythologyType, certificatesToGenerate);
                notifyNearbyPlayers(world, templePos, certificatesToGenerate, civilizedVillagerCount, mythologyType);
                System.out.println(String.format(
                    "[神力证明系统] 在位置 %s 的%s神殿附近未找到神像基座，已直接掉落 %d 个证明",
                    templePos, getCivilizationName(mythologyType), certificatesToGenerate
                ));
            }
        }
    }
    
    /**
     * 寻找最近的对应文明神像基座
     */
    private static BlockPos findNearestStatueBase(ServerWorld world, BlockPos templePos,
                                                  GodPowerCertificateItem.MythologyType mythologyType) {
        BlockPos closestBase = null;
        double closestDistance = Double.MAX_VALUE;
        
        // 在指定范围内搜索神像基座
        for (int x = -48; x <= 48; x++) {
            for (int z = -48; z <= 48; z++) {
                for (int y = -12; y <= 16; y++) {
                    BlockPos checkPos = templePos.add(x, y, z);
                    
                    var blockState = world.getBlockState(checkPos);
                    if (blockState.getBlock() instanceof StatueBaseBlock statueBase) {
                        if (statueBase.getMythologyType() == mythologyType) {
                            double distance = templePos.getSquaredDistance(checkPos);
                            if (distance < closestDistance) {
                                closestDistance = distance;
                                closestBase = checkPos;
                            }
                        }
                    }
                }
            }
        }
        
        return closestBase;
    }
    
    /**
     * 将证明存储到神像基座
     */
    private static void storeCertificatesInStatueBase(ServerWorld world, BlockPos statueBasePos, int amount) {
        var blockState = world.getBlockState(statueBasePos);
        if (blockState.getBlock() instanceof StatueBaseBlock statueBase) {
            statueBase.addCertificates(world, statueBasePos, amount);
        }
    }

    private static void dropCertificatesNearTemple(ServerWorld world, BlockPos templePos,
                                                   GodPowerCertificateItem.MythologyType mythologyType, int amount) {
        ItemStack certificateStack = createCertificateStack(mythologyType, amount);
        world.spawnEntity(new net.minecraft.entity.ItemEntity(
            world,
            templePos.getX() + 0.5,
            templePos.getY() + 1.0,
            templePos.getZ() + 0.5,
            certificateStack
        ));
    }

    private static ItemStack createCertificateStack(GodPowerCertificateItem.MythologyType mythologyType, int amount) {
        return switch (mythologyType) {
            case GREEK -> new ItemStack(ModItems.GOD_POWER_CERTIFICATE_GREEK, amount);
            case EGYPTIAN -> new ItemStack(ModItems.GOD_POWER_CERTIFICATE_EGYPTIAN, amount);
            case CHINESE -> new ItemStack(ModItems.GOD_POWER_CERTIFICATE_CHINESE, amount);
            case ATLANTEAN -> new ItemStack(ModItems.GOD_POWER_CERTIFICATE_ATLANTEAN, amount);
            case NORDIC -> new ItemStack(ModItems.GOD_POWER_CERTIFICATE_NORDIC, amount);
        };
    }
    
    /**
     * 根据村民数量计算神力证明生成数量
     * 
     * @param villagerCount 村民数量
     * @return 应生成的神力证明数量
     */
    private static int calculateCertificateAmount(int villagerCount) {
        for (int i = VILLAGER_THRESHOLDS.length - 1; i >= 0; i--) {
            if (villagerCount >= VILLAGER_THRESHOLDS[i]) {
                return CERTIFICATE_AMOUNTS[i];
            }
        }
        return 0;
    }
    
    /**
     * 生成神力证明并存储到神像基座
     * 
     * @param world 服务器世界
     * @param statueBasePos 神像基座位置
     * @param amount 生成数量
     * @param villagerCount 触发的村民数量
     */
    private static void generateAndStoreCertificates(ServerWorld world, BlockPos statueBasePos,
                                                   int amount, int villagerCount,
                                                   GodPowerCertificateItem.MythologyType mythologyType) {
        // 检查神像基座方块
        Block block = world.getBlockState(statueBasePos).getBlock();
        if (block instanceof StatueBaseBlock statueBase) {
            // 添加神力证明到基座存储
            statueBase.addCertificates(world, statueBasePos, amount);
            
            // 通知附近玩家
            notifyNearbyPlayers(world, statueBasePos, amount, villagerCount, mythologyType);
        }
    }
    
    /**
     * 通知附近玩家神力证明生成
     * 
     * @param world 服务器世界
     * @param pos 位置
     * @param amount 生成数量
     * @param villagerCount 村民数量
     */
    private static void notifyNearbyPlayers(ServerWorld world, BlockPos pos, int amount, int villagerCount,
                                            GodPowerCertificateItem.MythologyType mythologyType) {
        world.getPlayers().stream()
            .filter(player -> player.squaredDistanceTo(pos.getX(), pos.getY(), pos.getZ()) < 1024) // 32格范围
            .forEach(player -> {
                player.sendMessage(
                    Text.translatable("mythic_age.certificate.generated", 
                        villagerCount, amount, getCivilizationName(mythologyType))
                        .formatted(net.minecraft.util.Formatting.GOLD),
                    false
                );
            });
    }

    private static String getCivilizationName(GodPowerCertificateItem.MythologyType mythologyType) {
        return switch (mythologyType) {
            case GREEK -> "希腊";
            case EGYPTIAN -> "埃及";
            case CHINESE -> "中国";
            case ATLANTEAN -> "亚特兰蒂斯";
            case NORDIC -> "北欧";
        };
    }
    
    /**
     * 玩家潜行右键神像基座获取神力证明
     * 
     * @param world 世界
     * @param pos 神像基座位置
     * @param player 玩家
     * @return 是否成功获取
     */
    public static boolean tryExtractCertificates(World world, BlockPos pos, 
                                               net.minecraft.entity.player.PlayerEntity player) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return false;
        }
        
        // 检查玩家是否潜行
        if (!player.isSneaking()) {
            return false;
        }
        
        // 检查神像基座方块
        Block block = world.getBlockState(pos).getBlock();
        if (!(block instanceof StatueBaseBlock statueBase)) {
            return false;
        }
        
        // 获取存储的神力证明数量
        var blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof com.example.mythic_age.block.entity.StatueBaseBlockEntity baseEntity) {
            int storedAmount = baseEntity.getStoredCertificates();
            
            if (storedAmount > 0) {
                // 创建对应文明的神力证明物品
                ItemStack certificateStack = createCertificateStack(statueBase.getMythologyType(), storedAmount);
                
                // 尝试给玩家物品
                if (player.getInventory().insertStack(certificateStack)) {
                    // 清空基座存储
                    baseEntity.extractAllCertificates();
                    
                    // 播放音效
                    world.playSound(null, pos, net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                        net.minecraft.sound.SoundCategory.PLAYERS, 0.7f, 1.2f);
                    
                    // 发送消息
                    player.sendMessage(
                        Text.translatable("mythic_age.certificate.extracted", storedAmount)
                            .formatted(net.minecraft.util.Formatting.GREEN),
                        false
                    );
                    
                    return true;
                } else {
                    // 背包满了
                    player.sendMessage(
                        Text.translatable("mythic_age.certificate.inventory_full")
                            .formatted(net.minecraft.util.Formatting.RED),
                        false
                    );
                }
            } else {
                // 没有存储的神力证明
                player.sendMessage(
                    Text.translatable("mythic_age.certificate.none_stored")
                        .formatted(net.minecraft.util.Formatting.YELLOW),
                    false
                );
            }
        }
        
        return false;
    }
    
    /**
     * 获取生成间隔
     */
    public static int getGenerationInterval() {
        return GENERATION_INTERVAL;
    }
    
    /**
     * 获取神殿影响范围
     */
    public static int getTempleRange() {
        return TEMPLE_RANGE;
    }
    
    /**
     * 获取村民数量阈值配置（用于调试和显示）
     */
    public static String getThresholdInfo() {
        StringBuilder info = new StringBuilder("神力证明生成阶梯:\n");
        for (int i = 0; i < VILLAGER_THRESHOLDS.length; i++) {
            info.append(String.format("- %d名村民: %d个神力证明\n", 
                VILLAGER_THRESHOLDS[i], CERTIFICATE_AMOUNTS[i]));
        }
        return info.toString();
    }
}
