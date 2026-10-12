package com.example.mythic_age.worldgen;

import com.example.mythic_age.entity.CustomTempleGuardianEntity;
import com.example.mythic_age.entity.ModEntities;
import com.example.mythic_age.item.GodPowerCertificateItem;
import com.example.mythic_age.util.StructureGenerator;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

/**
 * 神话村庄生成处理器
 * 
 * 负责在村庄生成过程中：
 * 1. 检测神话村庄类型
 * 2. 在神殿位置生成自定义神殿守护者
 * 3. 设置守护者的神殿位置和文明类型
 * 4. 确保守护者与神殿正确绑定
 */
public class MythicVillageProcessor {
    
    /**
     * 处理神话村庄生成
     * 
     * @param world 世界
     * @param villageCenter 村庄中心
     * @param villageType 村庄类型
     */
    public static void processVillageGeneration(StructureWorldAccess world, BlockPos villageCenter, 
                                              MythicVillageTypes.VillageType villageType) {
        if (villageType == null || !(world instanceof ServerWorld serverWorld)) {
            return;
        }
        
        try {
            System.out.println("[神话村庄] 开始处理 " + villageType.getName() + " 村庄生成，位置: " + villageCenter);
            
            // 添加超时机制
            long startTime = System.currentTimeMillis();
            long timeout = 5000; // 5秒超时
            
            // 寻找合适的神殿生成位置
            BlockPos templePos = findSuitableTemplePosition(serverWorld, villageCenter);
            
            // 检查是否超时
            if (System.currentTimeMillis() - startTime > timeout) {
                System.err.println("[神话村庄] 处理村庄生成超时，跳过");
                return;
            }
            
            if (templePos == null) {
                System.out.println("[神话村庄] 未找到合适的神殿位置，跳过生成");
                return;
            }
            
            // 生成神殿
            boolean success = false;
            
            // 首先尝试从NBT文件加载
            if (loadTempleFromNBT(serverWorld, templePos, villageType)) {
                success = true;
                System.out.println("[神话村庄] 从NBT文件生成 " + villageType.getName() + " 神殿成功");
            }
            // 如果NBT加载失败，使用代码生成
            else if (generateTempleWithCode(serverWorld, templePos, villageType)) {
                success = true;
                System.out.println("[神话村庄] 代码生成 " + villageType.getName() + " 神殿成功");
            }
            
            if (success) {
                // 生成自定义神殿守护者
                spawnCustomTempleGuardian(serverWorld, templePos, villageType);
                System.out.println("[神话村庄] " + villageType.getName() + " 村庄处理完成");
            } else {
                System.err.println("[神话村庄] " + villageType.getName() + " 神殿生成失败");
            }
            
        } catch (Exception e) {
            System.err.println("[神话村庄] 处理村庄生成时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 生成神殿结构
     * 
     * @param world 世界
     * @param villageCenter 村庄中心
     * @param villageType 村庄类型
     * @return 神殿位置，如果生成失败则返回null
     */
    private static BlockPos generateTempleStructure(ServerWorld world, BlockPos villageCenter, 
                                                   MythicVillageTypes.VillageType villageType) {
        // 计算神殿生成位置（村庄中心附近的合适位置）
        BlockPos templePos = findSuitableTemplePosition(world, villageCenter);
        if (templePos == null) {
            return null;
        }
        
        // 尝试从NBT文件加载结构
        if (loadTempleFromNBT(world, templePos, villageType)) {
            System.out.println("[神话村庄] 成功从NBT文件加载 " + villageType.getName() + " 神殿");
            return templePos;
        }
        
        // 如果NBT加载失败，使用代码生成
        if (generateTempleWithCode(world, templePos, villageType)) {
            System.out.println("[神话村庄] 使用代码生成 " + villageType.getName() + " 神殿");
            return templePos;
        }
        
        return null;
    }
    
    /**
     * 查找合适的神殿生成位置
     * 按照村庄建筑生成方式，在村庄范围内寻找空闲位置
     * 
     * @param world 世界
     * @param villageCenter 村庄中心
     * @return 合适的位置，如果未找到则返回null
     */
    private static BlockPos findSuitableTemplePosition(ServerWorld world, BlockPos villageCenter) {
        // 添加搜索限制，防止无限循环
        int maxAttempts = 30;
        int attempts = 0;
        
        // 在村庄范围内寻找空闲位置（村庄半径通常为50-80格）
        int villageRadius = 60;
        
        for (int distance = 25; distance <= villageRadius && attempts < maxAttempts; distance += 8) {
            for (int angle = 0; angle < 360 && attempts < maxAttempts; angle += 30) {
                attempts++;
                
                double radians = Math.toRadians(angle);
                int x = (int) (villageCenter.getX() + distance * Math.cos(radians));
                int z = (int) (villageCenter.getZ() + distance * Math.sin(radians));
                
                // 获取地表高度
                int y = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z);
                BlockPos testPos = new BlockPos(x, y, z);
                
                // 检查是否可以在此位置生成神殿（不覆盖村庄建筑）
                if (canPlaceTempleAt(world, testPos)) {
                    System.out.println("[神话村庄] 找到合适位置: " + testPos + " (尝试次数: " + attempts + ")");
                    return testPos;
                }
            }
        }
        
        System.out.println("[神话村庄] 未找到合适位置，已尝试 " + attempts + " 次");
        return null;
    }
    
    /**
     * 检查是否可以在指定位置放置神殿
     * 按照村庄建筑生成方式，只检查是否与重要建筑冲突，不检查地形平坦度
     * 
     * @param world 世界
     * @param pos 位置
     * @return 是否可以放置
     */
    private static boolean canPlaceTempleAt(ServerWorld world, BlockPos pos) {
        try {
            // 神殿大小约为15x15x8，检查这个区域
            int templeWidth = 15;
            int templeLength = 15;
            int templeHeight = 8;
            
            // 只检查核心区域是否与重要建筑冲突（减少检查范围提高性能）
            for (int x = -templeWidth/3; x <= templeWidth/3; x += 2) {
                for (int z = -templeLength/3; z <= templeLength/3; z += 2) {
                    for (int y = 1; y < templeHeight - 1; y += 2) {
                        BlockPos checkPos = pos.add(x, y, z);
                        
                        // 检查是否是重要的村庄建筑方块
                        if (isImportantVillageBlock(world, checkPos)) {
                            return false;
                        }
                    }
                }
            }
            
            // 检查基础位置是否在合理的高度范围内（避免在虚空或过高处生成）
            int groundY = world.getTopY(Heightmap.Type.WORLD_SURFACE, pos.getX(), pos.getZ());
            if (pos.getY() < groundY - 10 || pos.getY() > groundY + 20) {
                return false;
            }
            
            return true;
        } catch (Exception e) {
            System.err.println("[神话村庄] 检查神殿位置时出错: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查是否是重要的村庄建筑方块
     * 
     * @param world 世界
     * @param pos 位置
     * @return 是否是重要建筑方块
     */
    private static boolean isImportantVillageBlock(ServerWorld world, BlockPos pos) {
        var blockState = world.getBlockState(pos);
        var block = blockState.getBlock();
        String blockName = block.toString().toLowerCase();
        
        // 检查是否是村庄重要建筑方块
        return blockName.contains("bed") ||           // 床
               blockName.contains("door") ||          // 门
               blockName.contains("bell") ||          // 钟
               blockName.contains("workstation") ||   // 工作站
               blockName.contains("lectern") ||       // 讲台
               blockName.contains("anvil") ||         // 铁砧
               blockName.contains("furnace") ||       // 熔炉
               blockName.contains("chest") ||         // 箱子
               blockName.contains("barrel") ||        // 桶
               blockName.contains("composter") ||     // 堆肥箱
               blockName.contains("cauldron") ||      // 炼药锅
               blockName.contains("brewing") ||       // 酿造台
               blockName.contains("smithing") ||      // 锻造台
               blockName.contains("grindstone") ||    // 砂轮
               blockName.contains("stonecutter") ||   // 切石机
               blockName.contains("loom") ||          // 织布机
               blockName.contains("cartography") ||   // 制图台
               blockName.contains("fletching") ||     // 制箭台
               blockName.contains("smoker") ||        // 烟熏炉
               blockName.contains("blast_furnace") || // 高炉
               blockName.contains("well") ||          // 井
               blockName.contains("lamp") ||          // 灯
               blockName.contains("torch") ||         // 火把
               blockName.contains("lantern");         // 灯笼
    }
    
    /**
     * 从NBT文件加载神殿结构
     * 
     * @param world 世界
     * @param pos 生成位置
     * @param villageType 村庄类型
     * @return 是否成功加载
     */
    private static boolean loadTempleFromNBT(ServerWorld world, BlockPos pos, MythicVillageTypes.VillageType villageType) {
        String nbtPath = getTempleNBTPath(villageType);
        if (nbtPath == null) {
            return false;
        }
        
        try {
            // 从资源包中加载NBT文件
            Identifier structureId = new Identifier("mythic_age", nbtPath);
            var resourceOpt = world.getServer().getResourceManager().getResource(structureId);
            
            if (resourceOpt.isEmpty()) {
                System.out.println("[神话村庄] 未找到NBT文件: " + nbtPath);
                return false;
            }
            
            // 读取NBT数据
            InputStream inputStream = resourceOpt.get().getInputStream();
            NbtCompound nbt = NbtIo.readCompressed(inputStream);
            inputStream.close();
            
            // 创建结构模板
            StructureTemplate template = new StructureTemplate();
            // 获取方块注册表查找器
            var blockLookup = world.getRegistryManager().get(net.minecraft.registry.RegistryKeys.BLOCK).getReadOnlyWrapper();
            template.readNbt(blockLookup, nbt);
            
            // 生成结构
            net.minecraft.structure.StructurePlacementData placementData = new net.minecraft.structure.StructurePlacementData();
            template.place(world, pos, pos, placementData, world.getRandom(), 2);
            
            return true;
            
        } catch (IOException e) {
            System.err.println("[神话村庄] 加载NBT文件失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取神殿NBT文件路径
     * 
     * @param villageType 村庄类型
     * @return NBT文件路径
     */
    private static String getTempleNBTPath(MythicVillageTypes.VillageType villageType) {
        return switch (villageType) {
            case GREEK -> "structures/village/greek/temple_small.nbt";
            case EGYPTIAN -> "structures/village/egyptian/temple_small.nbt";
            case CHINESE -> "structures/village/chinese/temple_small.nbt";
            case ATLANTEAN -> "structures/village/atlantean/temple_small.nbt";
            case NORDIC -> "structures/village/nordic/temple_small.nbt";
        };
    }
    
    /**
     * 使用代码生成神殿
     * 
     * @param world 世界
     * @param pos 生成位置
     * @param villageType 村庄类型
     * @return 是否成功生成
     */
    private static boolean generateTempleWithCode(ServerWorld world, BlockPos pos, MythicVillageTypes.VillageType villageType) {
        try {
            switch (villageType) {
                case GREEK -> StructureGenerator.generateGreekTemple(world, pos, false);
                case EGYPTIAN -> StructureGenerator.generateEgyptianTemple(world, pos, false);
                case CHINESE -> StructureGenerator.generateChineseTemple(world, pos, false);
                case ATLANTEAN -> StructureGenerator.generateAtlanteanTemple(world, pos, false);
                case NORDIC -> StructureGenerator.generateNordicTemple(world, pos, false);
                default -> {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            System.err.println("[神话村庄] 代码生成神殿失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查是否是神殿方块
     * 
     * @param state 方块状态
     * @param villageType 村庄类型
     * @return 是否是神殿方块
     */
    private static boolean isTempleBlock(BlockState state, MythicVillageTypes.VillageType villageType) {
        String blockId = state.getBlock().toString().toLowerCase();
        
        switch (villageType) {
            case GREEK:
                return blockId.contains("temple_greek") || blockId.contains("greek_temple");
            case EGYPTIAN:
                return blockId.contains("temple_egyptian") || blockId.contains("egyptian_temple");
            case CHINESE:
                return blockId.contains("temple_chinese") || blockId.contains("chinese_temple");
            case ATLANTEAN:
                return blockId.contains("temple_atlantean") || blockId.contains("atlantean_temple");
            case NORDIC:
                return blockId.contains("temple_nordic") || blockId.contains("nordic_temple");
            default:
                return false;
        }
    }
    
    /**
     * 生成自定义神殿守护者
     * 
     * @param world 服务器世界
     * @param templePos 神殿位置
     * @param villageType 村庄类型
     */
    private static void spawnCustomTempleGuardian(ServerWorld world, BlockPos templePos, 
                                                 MythicVillageTypes.VillageType villageType) {
        try {
            // 获取对应的实体类型
            var entityType = getCustomGuardianEntityType(villageType);
            if (entityType == null) {
                return;
            }
            
            // 找到合适的生成位置（神殿附近的地面）
            BlockPos spawnPos = findSpawnPosition(world, templePos);
            if (spawnPos == null) {
                return;
            }
            
            // 创建自定义神殿守护者
            CustomTempleGuardianEntity guardian = new CustomTempleGuardianEntity(
                entityType, world, villageType.getMythologyType(), templePos
            );
            
            // 设置位置
            guardian.setPosition(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
            
            // 设置自定义名称
            guardian.setCustomName(guardian.getDisplayName());
            guardian.setCustomNameVisible(true);
            
            // 生成实体
            if (world.spawnEntity(guardian)) {
                // 记录生成日志
                world.getServer().getPlayerManager().broadcast(
                    net.minecraft.text.Text.literal("在 " + villageType.getName() + 
                        " 神殿生成了自定义神殿守护者！")
                        .formatted(net.minecraft.util.Formatting.GOLD), false
                );
                
                System.out.println("[神话村庄] 在 " + templePos + " 生成了 " + 
                    villageType.getName() + " 自定义神殿守护者");
            }
            
        } catch (Exception e) {
            System.err.println("[神话村庄] 生成自定义神殿守护者失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 获取自定义守护者实体类型
     * 
     * @param villageType 村庄类型
     * @return 实体类型
     */
    private static net.minecraft.entity.EntityType<CustomTempleGuardianEntity> getCustomGuardianEntityType(
            MythicVillageTypes.VillageType villageType) {
        return switch (villageType) {
            case GREEK -> ModEntities.CUSTOM_TEMPLE_GUARDIAN_GREEK;
            case EGYPTIAN -> ModEntities.CUSTOM_TEMPLE_GUARDIAN_EGYPTIAN;
            case CHINESE -> ModEntities.CUSTOM_TEMPLE_GUARDIAN_CHINESE;
            case ATLANTEAN -> ModEntities.CUSTOM_TEMPLE_GUARDIAN_ATLANTEAN;
            case NORDIC -> ModEntities.CUSTOM_TEMPLE_GUARDIAN_NORDIC;
        };
    }
    
    /**
     * 查找合适的生成位置
     * 
     * @param world 世界
     * @param templePos 神殿位置
     * @return 生成位置
     */
    private static BlockPos findSpawnPosition(ServerWorld world, BlockPos templePos) {
        // 在神殿附近寻找合适的地面位置
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                BlockPos testPos = templePos.add(x, 0, z);
                int y = world.getTopY(Heightmap.Type.WORLD_SURFACE, testPos.getX(), testPos.getZ());
                BlockPos spawnPos = new BlockPos(testPos.getX(), y, testPos.getZ());
                
                // 检查位置是否合适
                if (isValidSpawnPosition(world, spawnPos)) {
                    return spawnPos;
                }
            }
        }
        
        return null;
    }
    
    /**
     * 检查是否是有效的生成位置
     * 
     * @param world 世界
     * @param pos 位置
     * @return 是否有效
     */
    private static boolean isValidSpawnPosition(ServerWorld world, BlockPos pos) {
        // 检查地面方块
        BlockState groundState = world.getBlockState(pos.down());
        if (!groundState.isSolidBlock(world, pos.down())) {
            return false;
        }
        
        // 检查生成位置
        BlockState spawnState = world.getBlockState(pos);
        if (!spawnState.isAir()) {
            return false;
        }
        
        // 检查上方空间
        BlockState aboveState = world.getBlockState(pos.up());
        if (!aboveState.isAir()) {
            return false;
        }
        
        return true;
    }
}
