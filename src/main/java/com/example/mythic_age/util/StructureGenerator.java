package com.example.mythic_age.util;

import com.example.mythic_age.MythicAgeMod;
import com.example.mythic_age.block.ModBlocks;
import com.example.mythic_age.entity.CustomTempleGuardianEntity;
import com.example.mythic_age.entity.ModEntities;
import com.example.mythic_age.item.GodPowerCertificateItem;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 神殿结构生成器
 * 
 * 用于生成各文明神殿的.nbt结构文件，这些文件可以在世界生成时使用。
 * 每个神殿都有不同的大小和布局，体现各文明的特色。
 * 
 * 支持的神殿类型：
 * - 埃及神殿：金字塔风格，使用砂岩材料
 * - 中国神殿：传统庙宇风格，使用红色陶瓦
 * - 亚特兰蒂斯神殿：海洋风格，使用海晶石带发光效果
 * - 北欧神殿：维京风格，使用石砖和苔石
 * 
 * 每个神殿包含：
 * - 主体建筑（使用对应文明的建筑方块）
 * - 圣物基座（中央位置）
 * - 神像结构（用于兑换法术书）
 * - 神殿守护者生成点
 * - 装饰性元素
 */
public class StructureGenerator {
    
    /**
     * 生成埃及神殿结构
     * 
     * 创建一个11x7x11的金字塔风格神殿：
     * - 外墙使用埃及神殿砖
     * - 装饰使用埃及神殿浮雕
     * - 柱子使用埃及神殿柱
     * - 中央放置圣物基座和埃及神像
     * 
     * @param world 世界
     * @param pos 生成位置
     */
    public static void generateEgyptianTemple(World world, BlockPos pos) {
        generateEgyptianTemple(world, pos, true);
    }

    public static void generateEgyptianTemple(World world, BlockPos pos, boolean spawnGuardian) {
        // 建筑尺寸：11x7x11 (宽x高x长)
        int width = 11;
        int height = 7;
        int length = 11;
        
        // 清空区域
        clearArea(world, pos, width, height, length);
        
        // 生成地基 (第1层)
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                world.setBlockState(pos.add(x, 0, z), ModBlocks.TEMPLE_EGYPTIAN_BRICKS.getDefaultState());
            }
        }
        
        // 生成墙壁和内部结构 (第2-6层)
        for (int y = 1; y < height - 1; y++) {
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < length; z++) {
                    BlockPos currentPos = pos.add(x, y, z);
                    
                    // 外墙
                    if (x == 0 || x == width - 1 || z == 0 || z == length - 1) {
                        if ((x == 2 || x == width - 3) && (z == 2 || z == length - 3) && y <= 4) {
                            // 四个角落的柱子
                            world.setBlockState(currentPos, ModBlocks.TEMPLE_EGYPTIAN_PILLAR.getDefaultState());
                        } else if ((x == width / 2 || z == length / 2) && y == 3) {
                            // 中央装饰浮雕
                            world.setBlockState(currentPos, ModBlocks.TEMPLE_EGYPTIAN_RELIEF.getDefaultState());
                        } else {
                            // 普通墙壁
                            world.setBlockState(currentPos, ModBlocks.TEMPLE_EGYPTIAN_BRICKS.getDefaultState());
                        }
                    }
                    // 内部地板
                    else if (y == 1) {
                        world.setBlockState(currentPos, ModBlocks.TEMPLE_EGYPTIAN_SLAB.getDefaultState());
                    }
                }
            }
        }
        
        // 生成屋顶 (第7层)
        for (int x = 1; x < width - 1; x++) {
            for (int z = 1; z < length - 1; z++) {
                world.setBlockState(pos.add(x, height - 1, z), ModBlocks.TEMPLE_EGYPTIAN_STAIRS.getDefaultState());
            }
        }
        
        // 放置圣物基座 (中央位置)
        BlockPos pedestalPos = pos.add(width / 2, 2, length / 2);
        world.setBlockState(pedestalPos, ModBlocks.RELIC_PEDESTAL.getDefaultState());
        
        // 放置埃及神像多方块结构 (靠近入口)
        BlockPos statuePos = pos.add(width / 2, 1, 2);
        placeStatueStructure(world, statuePos, GodPowerCertificateItem.MythologyType.EGYPTIAN);
        
        // 放置埃及神像基座 (另一侧)
        BlockPos egyptianPedestalPos = pos.add(width / 2, 1, length - 3);
        world.setBlockState(egyptianPedestalPos, ModBlocks.EGYPTIAN_STATUE_PEDESTAL.getDefaultState());
        
        if (spawnGuardian) {
            BlockPos guardianPos = pos.add(width / 2 + 1, 2, length / 2);
            spawnCustomTempleGuardian(world, guardianPos, GodPowerCertificateItem.MythologyType.EGYPTIAN, pos);
        }
    }
    
    /**
     * 生成中国神殿结构
     * 
     * 创建一个13x8x13的传统庙宇风格神殿：
     * - 使用红色陶瓦材料
     * - 传统的屋顶设计
     * - 龙纹装饰浮雕
     * 
     * @param world 世界
     * @param pos 生成位置
     */
    public static void generateChineseTemple(World world, BlockPos pos) {
        generateChineseTemple(world, pos, true);
    }

    public static void generateChineseTemple(World world, BlockPos pos, boolean spawnGuardian) {
        int width = 13;
        int height = 8;
        int length = 13;
        
        clearArea(world, pos, width, height, length);
        
        // 地基
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                world.setBlockState(pos.add(x, 0, z), ModBlocks.TEMPLE_CHINESE_BRICKS.getDefaultState());
            }
        }
        
        // 主体结构
        for (int y = 1; y < height - 2; y++) {
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < length; z++) {
                    BlockPos currentPos = pos.add(x, y, z);
                    
                    if (x == 0 || x == width - 1 || z == 0 || z == length - 1) {
                        if ((x == 3 || x == width - 4) && (z == 3 || z == length - 4) && y <= 5) {
                            world.setBlockState(currentPos, ModBlocks.TEMPLE_CHINESE_PILLAR.getDefaultState());
                        } else if ((x == width / 2 || z == length / 2) && y == 4) {
                            world.setBlockState(currentPos, ModBlocks.TEMPLE_CHINESE_RELIEF.getDefaultState());
                        } else {
                            world.setBlockState(currentPos, ModBlocks.TEMPLE_CHINESE_BRICKS.getDefaultState());
                        }
                    } else if (y == 1) {
                        world.setBlockState(currentPos, ModBlocks.TEMPLE_CHINESE_SLAB.getDefaultState());
                    }
                }
            }
        }
        
        // 传统屋顶
        for (int y = height - 2; y < height; y++) {
            for (int x = 1; x < width - 1; x++) {
                for (int z = 1; z < length - 1; z++) {
                    world.setBlockState(pos.add(x, y, z), ModBlocks.TEMPLE_CHINESE_STAIRS.getDefaultState());
                }
            }
        }
        
        // 功能方块
        BlockPos pedestalPos = pos.add(width / 2, 2, length / 2);
        world.setBlockState(pedestalPos, ModBlocks.RELIC_PEDESTAL.getDefaultState());
        
        BlockPos statuePos = pos.add(width / 2, 1, 3);
        placeStatueStructure(world, statuePos, GodPowerCertificateItem.MythologyType.CHINESE);
        
        if (spawnGuardian) {
            BlockPos guardianPos = pos.add(width / 2 - 1, 2, length / 2);
            spawnCustomTempleGuardian(world, guardianPos, GodPowerCertificateItem.MythologyType.CHINESE, pos);
        }
    }
    
    /**
     * 生成亚特兰蒂斯神殿结构
     * 
     * 创建一个15x6x15的海洋风格神殿：
     * - 使用海晶石材料，带发光效果
     * - 几何水纹装饰
     * - 神秘的海洋氛围
     * 
     * @param world 世界
     * @param pos 生成位置
     */
    public static void generateAtlanteanTemple(World world, BlockPos pos) {
        generateAtlanteanTemple(world, pos, true);
    }

    public static void generateAtlanteanTemple(World world, BlockPos pos, boolean spawnGuardian) {
        int width = 15;
        int height = 6;
        int length = 15;
        
        clearArea(world, pos, width, height, length);
        
        // 发光地基
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                world.setBlockState(pos.add(x, 0, z), ModBlocks.TEMPLE_ATLANTEAN_BRICKS.getDefaultState());
            }
        }
        
        // 主体结构
        for (int y = 1; y < height - 1; y++) {
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < length; z++) {
                    BlockPos currentPos = pos.add(x, y, z);
                    
                    if (x == 0 || x == width - 1 || z == 0 || z == length - 1) {
                        if ((x == 4 || x == width - 5) && (z == 4 || z == length - 5) && y <= 4) {
                            world.setBlockState(currentPos, ModBlocks.TEMPLE_ATLANTEAN_PILLAR.getDefaultState());
                        } else if ((x == width / 2 || z == length / 2) && y == 3) {
                            world.setBlockState(currentPos, ModBlocks.TEMPLE_ATLANTEAN_RELIEF.getDefaultState());
                        } else {
                            world.setBlockState(currentPos, ModBlocks.TEMPLE_ATLANTEAN_BRICKS.getDefaultState());
                        }
                    } else if (y == 1) {
                        world.setBlockState(currentPos, ModBlocks.TEMPLE_ATLANTEAN_SLAB.getDefaultState());
                    }
                }
            }
        }
        
        // 水晶屋顶
        for (int x = 2; x < width - 2; x++) {
            for (int z = 2; z < length - 2; z++) {
                world.setBlockState(pos.add(x, height - 1, z), ModBlocks.TEMPLE_ATLANTEAN_STAIRS.getDefaultState());
            }
        }
        
        // 功能方块
        BlockPos pedestalPos = pos.add(width / 2, 2, length / 2);
        world.setBlockState(pedestalPos, ModBlocks.RELIC_PEDESTAL.getDefaultState());
        
        BlockPos statuePos = pos.add(width / 2, 1, 4);
        placeStatueStructure(world, statuePos, GodPowerCertificateItem.MythologyType.ATLANTEAN);
        
        if (spawnGuardian) {
            BlockPos guardianPos = pos.add(width / 2, 2, length / 2 + 2);
            spawnCustomTempleGuardian(world, guardianPos, GodPowerCertificateItem.MythologyType.ATLANTEAN, pos);
        }
    }
    
    /**
     * 生成北欧神殿结构
     * 
     * 创建一个9x6x9的维京风格神殿：
     * - 使用石砖材料，粗犷风格
     * - 卢恩符文装饰
     * - 坚固的战士文化体现
     * 
     * @param world 世界
     * @param pos 生成位置
     */
    public static void generateNordicTemple(World world, BlockPos pos) {
        generateNordicTemple(world, pos, true);
    }

    public static void generateNordicTemple(World world, BlockPos pos, boolean spawnGuardian) {
        int width = 9;
        int height = 6;
        int length = 9;
        
        clearArea(world, pos, width, height, length);
        
        // 坚固地基
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                world.setBlockState(pos.add(x, 0, z), ModBlocks.TEMPLE_NORDIC_BRICKS.getDefaultState());
            }
        }
        
        // 主体结构
        for (int y = 1; y < height - 1; y++) {
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < length; z++) {
                    BlockPos currentPos = pos.add(x, y, z);
                    
                    if (x == 0 || x == width - 1 || z == 0 || z == length - 1) {
                        if ((x == 2 || x == width - 3) && (z == 2 || z == length - 3) && y <= 3) {
                            world.setBlockState(currentPos, ModBlocks.TEMPLE_NORDIC_PILLAR.getDefaultState());
                        } else if ((x == width / 2 || z == length / 2) && y == 3) {
                            world.setBlockState(currentPos, ModBlocks.TEMPLE_NORDIC_RELIEF.getDefaultState());
                        } else {
                            world.setBlockState(currentPos, ModBlocks.TEMPLE_NORDIC_BRICKS.getDefaultState());
                        }
                    }
                }
            }
        }
        
        // 屋顶
        for (int x = 1; x < width - 1; x++) {
            for (int z = 1; z < length - 1; z++) {
                world.setBlockState(pos.add(x, height - 1, z), ModBlocks.TEMPLE_NORDIC_STAIRS.getDefaultState());
            }
        }
        
        // 功能方块
        BlockPos pedestalPos = pos.add(width / 2, 2, length / 2);
        world.setBlockState(pedestalPos, ModBlocks.RELIC_PEDESTAL.getDefaultState());
        
        BlockPos statuePos = pos.add(width / 2, 1, 3);
        placeStatueStructure(world, statuePos, GodPowerCertificateItem.MythologyType.NORDIC);
        
        if (spawnGuardian) {
            BlockPos guardianPos = pos.add(width / 2 + 1, 2, length / 2 - 1);
            spawnCustomTempleGuardian(world, guardianPos, GodPowerCertificateItem.MythologyType.NORDIC, pos);
        }
    }

    /**
     * 生成希腊神殿结构。
     *
     * v1 世界生成优先使用代码结构，NBT 模板留作后续精修，因此希腊回退必须保持希腊方块语义。
     */
    public static void generateGreekTemple(World world, BlockPos pos) {
        generateGreekTemple(world, pos, true);
    }

    public static void generateGreekTemple(World world, BlockPos pos, boolean spawnGuardian) {
        int width = 11;
        int height = 7;
        int length = 11;

        clearArea(world, pos, width, height, length);

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                world.setBlockState(pos.add(x, 0, z), ModBlocks.TEMPLE_GREEK_BRICKS.getDefaultState());
            }
        }

        for (int y = 1; y < height - 1; y++) {
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < length; z++) {
                    BlockPos currentPos = pos.add(x, y, z);
                    boolean outer = x == 0 || x == width - 1 || z == 0 || z == length - 1;
                    boolean entry = z == 0 && x >= width / 2 - 1 && x <= width / 2 + 1 && y <= 3;

                    if (outer && !entry) {
                        if ((x == 2 || x == width - 3) && (z == 2 || z == length - 3) && y <= 4) {
                            world.setBlockState(currentPos, ModBlocks.TEMPLE_GREEK_PILLAR.getDefaultState());
                        } else if ((x == width / 2 || z == length / 2) && y == 3) {
                            world.setBlockState(currentPos, ModBlocks.TEMPLE_GREEK_RELIEF.getDefaultState());
                        } else {
                            world.setBlockState(currentPos, ModBlocks.TEMPLE_GREEK_BRICKS.getDefaultState());
                        }
                    } else if (!outer && y == 1) {
                        world.setBlockState(currentPos, ModBlocks.TEMPLE_GREEK_SLAB.getDefaultState());
                    }
                }
            }
        }

        for (int x = 1; x < width - 1; x++) {
            for (int z = 1; z < length - 1; z++) {
                world.setBlockState(pos.add(x, height - 1, z), ModBlocks.TEMPLE_GREEK_STAIRS.getDefaultState());
            }
        }

        BlockPos pedestalPos = pos.add(width / 2, 2, length / 2);
        world.setBlockState(pedestalPos, ModBlocks.RELIC_PEDESTAL.getDefaultState());

        BlockPos statuePos = pos.add(width / 2, 1, 2);
        placeStatueStructure(world, statuePos, GodPowerCertificateItem.MythologyType.GREEK);

        if (spawnGuardian) {
            BlockPos guardianPos = pos.add(width / 2 + 1, 2, length / 2);
            spawnCustomTempleGuardian(world, guardianPos, GodPowerCertificateItem.MythologyType.GREEK, pos);
        }
    }

    private static void placeStatueStructure(World world, BlockPos basePos, GodPowerCertificateItem.MythologyType mythologyType) {
        world.setBlockState(basePos, getStatueBaseBlock(mythologyType).getDefaultState());
        world.setBlockState(basePos.up(), getStatueLegsBlock(mythologyType).getDefaultState());
        world.setBlockState(basePos.up(2), getStatueBodyBlock(mythologyType).getDefaultState());
        world.setBlockState(basePos.up(3), getStatueHeadBlock(mythologyType).getDefaultState());
    }

    private static Block getStatueBaseBlock(GodPowerCertificateItem.MythologyType mythologyType) {
        return switch (mythologyType) {
            case GREEK -> ModBlocks.STATUE_GREEK_BASE;
            case EGYPTIAN -> ModBlocks.STATUE_EGYPTIAN_BASE;
            case CHINESE -> ModBlocks.STATUE_CHINESE_BASE;
            case ATLANTEAN -> ModBlocks.STATUE_ATLANTEAN_BASE;
            case NORDIC -> ModBlocks.STATUE_NORDIC_BASE;
        };
    }

    private static Block getStatueLegsBlock(GodPowerCertificateItem.MythologyType mythologyType) {
        return switch (mythologyType) {
            case GREEK -> ModBlocks.STATUE_GREEK_LEGS;
            case EGYPTIAN -> ModBlocks.STATUE_EGYPTIAN_LEGS;
            case CHINESE -> ModBlocks.STATUE_CHINESE_LEGS;
            case ATLANTEAN -> ModBlocks.STATUE_ATLANTEAN_LEGS;
            case NORDIC -> ModBlocks.STATUE_NORDIC_LEGS;
        };
    }

    private static Block getStatueBodyBlock(GodPowerCertificateItem.MythologyType mythologyType) {
        return switch (mythologyType) {
            case GREEK -> ModBlocks.STATUE_GREEK_BODY;
            case EGYPTIAN -> ModBlocks.STATUE_EGYPTIAN_BODY;
            case CHINESE -> ModBlocks.STATUE_CHINESE_BODY;
            case ATLANTEAN -> ModBlocks.STATUE_ATLANTEAN_BODY;
            case NORDIC -> ModBlocks.STATUE_NORDIC_BODY;
        };
    }

    private static Block getStatueHeadBlock(GodPowerCertificateItem.MythologyType mythologyType) {
        return switch (mythologyType) {
            case GREEK -> ModBlocks.STATUE_GREEK_HEAD;
            case EGYPTIAN -> ModBlocks.STATUE_EGYPTIAN_HEAD;
            case CHINESE -> ModBlocks.STATUE_CHINESE_HEAD;
            case ATLANTEAN -> ModBlocks.STATUE_ATLANTEAN_HEAD;
            case NORDIC -> ModBlocks.STATUE_NORDIC_HEAD;
        };
    }

    private static void spawnCustomTempleGuardian(World world, BlockPos guardianPos, GodPowerCertificateItem.MythologyType mythologyType, BlockPos templePos) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }

        var entityType = switch (mythologyType) {
            case GREEK -> ModEntities.CUSTOM_TEMPLE_GUARDIAN_GREEK;
            case EGYPTIAN -> ModEntities.CUSTOM_TEMPLE_GUARDIAN_EGYPTIAN;
            case CHINESE -> ModEntities.CUSTOM_TEMPLE_GUARDIAN_CHINESE;
            case ATLANTEAN -> ModEntities.CUSTOM_TEMPLE_GUARDIAN_ATLANTEAN;
            case NORDIC -> ModEntities.CUSTOM_TEMPLE_GUARDIAN_NORDIC;
        };

        CustomTempleGuardianEntity guardian = new CustomTempleGuardianEntity(entityType, world, mythologyType, templePos);
        guardian.setPosition(guardianPos.getX() + 0.5, guardianPos.getY(), guardianPos.getZ() + 0.5);
        guardian.setCustomName(guardian.getDisplayName());
        guardian.setCustomNameVisible(true);
        serverWorld.spawnEntity(guardian);
    }
    
    /**
     * 清空指定区域并适应地形
     * 神殿将直接覆盖现有方块，不需要完全平坦的地面
     * 
     * @param world 世界
     * @param pos 起始位置
     * @param width 宽度
     * @param height 高度
     * @param length 长度
     */
    private static void clearArea(World world, BlockPos pos, int width, int height, int length) {
        // 获取地面高度作为基准
        int baseGroundY = world.getTopY(Heightmap.Type.WORLD_SURFACE, pos.getX() + width/2, pos.getZ() + length/2);
        
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                // 获取当前位置的地面高度
                int localGroundY = world.getTopY(Heightmap.Type.WORLD_SURFACE, pos.getX() + x, pos.getZ() + z);
                
                // 清空从地面到神殿高度的所有方块
                int startY = Math.min(pos.getY(), localGroundY);
                int endY = Math.max(pos.getY() + height, localGroundY + height);
                
                for (int y = startY; y < endY; y++) {
                    BlockPos clearPos = new BlockPos(pos.getX() + x, y, pos.getZ() + z);
                    
                    // 不清空基岩和重要的村庄建筑
                    var blockState = world.getBlockState(clearPos);
                    if (!blockState.isOf(Blocks.BEDROCK) && 
                        !isImportantVillageBlock(world, clearPos)) {
                        world.setBlockState(clearPos, Blocks.AIR.getDefaultState());
                    }
                }
            }
        }
    }
    
    /**
     * 检查是否是重要的村庄建筑方块（与MythicVillageProcessor中的方法保持一致）
     */
    private static boolean isImportantVillageBlock(World world, BlockPos pos) {
        var blockState = world.getBlockState(pos);
        var block = blockState.getBlock();
        String blockName = block.toString().toLowerCase();
        
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
     * 保存结构为.nbt文件
     * 
     * 这个方法可以将生成的结构保存为.nbt文件，供数据包使用
     * 
     * @param world 世界
     * @param start 起始位置
     * @param end 结束位置
     * @param fileName 文件名
     */
    public static void saveStructureToNBT(World world, BlockPos start, BlockPos end, String fileName) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }
        
        StructureTemplate template = new StructureTemplate();
        template.saveFromWorld(world, start, new Vec3i(
            end.getX() - start.getX() + 1,
            end.getY() - start.getY() + 1,
            end.getZ() - start.getZ() + 1
        ), true, Blocks.STRUCTURE_VOID);
        
        try {
            Path structuresPath = Paths.get("src/main/resources/data/mythic_age/structures/village/");
            structuresPath.toFile().mkdirs();
            
            NbtCompound nbt = template.writeNbt(new NbtCompound());
            FileOutputStream fos = new FileOutputStream(structuresPath.resolve(fileName + ".nbt").toFile());
            NbtIo.writeCompressed(nbt, fos);
            fos.close();
            
            MythicAgeMod.LOGGER.info("Structure saved: " + fileName + ".nbt");
        } catch (IOException e) {
            MythicAgeMod.LOGGER.error("Failed to save structure: " + fileName, e);
        }
    }
}
