package com.example.mythic_age.util;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.function.Predicate;

/**
 * 多方块结构检测工具类
 * 
 * 提供用于检测和验证多方块结构的实用方法。
 * 这个类是神殿系统的核心组件，负责确定玩家是否正确建造了神殿结构。
 * 
 * 主要功能：
 * 1. 简单平台检测：检测3x3石砖平台
 * 2. 通用结构检测：支持任意复杂的多方块结构
 * 3. 灵活的条件判断：使用谓词函数进行方块状态检查
 * 4. 性能优化：高效的坐标计算和状态检查
 * 
 * 当前支持的结构：
 * - 3x3x1 石砖平台：神殿的基础结构
 * - 可扩展的立体结构：支持任意形状的多方块建筑
 * 
 * 设计原则：
 * - 模块化：每个检测方法都是独立的
 * - 可重用：通用方法可以用于不同类型的结构
 * - 高效：避免不必要的世界访问和计算
 * - 健壮：完整的空值检查和错误处理
 * 
 * 使用场景：
 * - 神殿激活：检测圣物基座周围的结构
 * - 仪式场所：验证特殊法术的施放条件
 * - 建筑验证：确认玩家建造的结构符合要求
 * - 成就系统：检测特定的建筑成就
 * 
 * 性能考虑：
 * - 使用相对坐标减少计算量
 * - 谓词函数避免重复的类型检查
 * - 早期返回机制提高效率
 * - 缓存友好的遍历顺序
 * 
 * 未来扩展：
 * - 支持更复杂的3D结构
 * - 添加结构变体支持
 * - 实现结构旋转和镜像
 * - 支持部分匹配和修复建议
 * 
 * @author MythicAge Development Team
 * @version 1.0.0
 * @since 1.20.1
 */
public class MultiBlockStructureUtil {

    /**
     * 检测简单的3x3石砖平台
     * 
     * 这是神殿系统的基础检测方法，验证圣物基座下方是否有正确的3x3平台结构。
     * 平台必须由指定的方块类型组成，通常是石砖。
     * 
     * 结构要求：
     * - 尺寸：3x3x1 (长3格，宽3格，高1格)
     * - 位置：位于给定位置的正下方一格
     * - 材料：必须全部由指定的方块类型组成
     * - 完整性：所有9个位置都必须是正确的方块
     * 
     * 坐标系统：
     * ```
     * 平台布局 (从上往下看):
     * [-1,-1] [0,-1] [1,-1]
     * [-1, 0] [0, 0] [1, 0]  <- 中心位置
     * [-1, 1] [0, 1] [1, 1]
     * ```
     * 
     * 检测算法：
     * 1. 计算平台中心位置 (基座位置下方一格)
     * 2. 遍历3x3区域的所有9个位置
     * 3. 检查每个位置是否为指定的方块类型
     * 4. 如果任何位置不匹配，立即返回false
     * 5. 所有位置都匹配时返回true
     * 
     * @param world 世界实例，用于访问方块状态
     * @param centerPos 基座的位置，平台将在其下方一格
     * @param requiredBlock 平台必须由此方块类型组成
     * @return 如果找到完整的3x3平台则返回true，否则返回false
     */
    public static boolean checkForSimplePlatform(World world, BlockPos centerPos, Block requiredBlock) {
        // 计算平台中心位置：基座下方一格
        BlockPos platformCenterBase = centerPos.down();

        // 遍历3x3区域 (相对坐标从-1到1)
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos currentPos = platformCenterBase.add(x, 0, z);
                // 检查当前位置的方块是否为所需类型
                if (!world.getBlockState(currentPos).isOf(requiredBlock)) {
                    return false; // 发现不匹配的方块，结构不完整
                }
            }
        }
        return true; // 所有位置都匹配，结构完整
    }

    /**
     * A more generic checker for a cuboid structure defined by relative positions and a predicate for block states.
     * @param world The world.
     * @param origin The origin BlockPos from which relative positions are calculated.
     * @param relativePositions An array of BlockPos, relative to the origin, that must match the predicate.
     * @param statePredicate A predicate to test the BlockState at each relative position.
     * @return True if all specified relative positions match the predicate, false otherwise.
     */
    public static boolean checkStructure(World world, BlockPos origin, BlockPos[] relativePositions, Predicate<BlockState> statePredicate) {
        if (world == null || origin == null || relativePositions == null || statePredicate == null) {
            return false;
        }
        for (BlockPos relPos : relativePositions) {
            BlockState currentState = world.getBlockState(origin.add(relPos));
            if (!statePredicate.test(currentState)) {
                return false;
            }
        }
        return true;
    }

    // Example usage for a 3x3x3 hollow cube structure (walls only)
    // public static boolean isHollowCube(World world, BlockPos center, Block material) {
    //     Predicate<BlockState> materialPredicate = (state) -> state.isOf(material);
    //     BlockPos[] structureDefinition = new BlockPos[] {
    //         // Define all relative positions for the hollow cube walls
    //     };
    //     return checkStructure(world, center, structureDefinition, materialPredicate);
    // }
}

