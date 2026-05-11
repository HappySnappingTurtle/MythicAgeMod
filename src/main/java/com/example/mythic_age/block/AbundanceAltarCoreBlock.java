package com.example.mythic_age.block;

import com.example.mythic_age.block.entity.AbundanceAltarCoreBlockEntity;
import com.example.mythic_age.block.entity.ModBlockEntities;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * 丰饶祭坛核心方块
 * 
 * 丰饶法术生成的多方块结构的核心组件，负责定期在周围生成各种资源。
 * 这个方块是整个丰饶祭坛的心脏，控制着资源生成的逻辑。
 * 
 * 核心功能：
 * - 多方块结构检测和验证
 * - 定期资源生成（30-60秒间隔）
 * - 在周围2格范围内生成各种资源
 * - 视觉效果和音效反馈
 * 
 * 多方块结构要求：
 * - 核心方块周围必须有特定的柱子配置
 * - 结构完整时才会开始生成资源
 * - 结构被破坏时停止工作
 * 
 * 生成的资源类型：
 * - 矿物方块（铁块、金块、钻石块等）
 * - 一堆肉方块（肉类资源）
 * - 一堆木头方块（木材资源）
 * - 其他珍贵物品
 * 
 * 技术特性：
 * - 基于方块实体的定时器系统
 * - 服务器端逻辑确保多人游戏一致性
 * - 智能位置检测避免覆盖现有方块
 * - 平衡的生成概率和冷却时间
 * 
 * @author MythicAge Development Team
 * @version 1.0.0
 * @since 1.20.1
 */
public class AbundanceAltarCoreBlock extends BlockWithEntity {

    public AbundanceAltarCoreBlock(Settings settings) {
        super(settings);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new AbundanceAltarCoreBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof AbundanceAltarCoreBlockEntity altarCore) {
                // 检查多方块结构
                if (altarCore.isStructureValid()) {
                    player.sendMessage(Text.literal("§a丰饶祭坛结构完整！正在运行中..."), false);
                    player.sendMessage(Text.literal("§7下次生成资源时间: " + altarCore.getTimeUntilNextGeneration() + " 秒"), false);
                } else {
                    player.sendMessage(Text.literal("§c丰饶祭坛结构不完整！"), false);
                    player.sendMessage(Text.literal("§7需要在核心周围放置丰饶祭坛柱子"), false);
                }
            }
        }
        return ActionResult.SUCCESS;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return checkType(type, ModBlockEntities.ABUNDANCE_ALTAR_CORE, 
            world.isClient ? null : AbundanceAltarCoreBlockEntity::serverTick);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && !world.isClient) {
            // 祭坛被破坏时给予提示
            if (world instanceof ServerWorld serverWorld) {
                serverWorld.getPlayers().forEach(player -> {
                    if (player.squaredDistanceTo(pos.getX(), pos.getY(), pos.getZ()) < 256) { // 16格范围内
                        player.sendMessage(Text.literal("§6丰饶祭坛被破坏了！"), false);
                    }
                });
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }
} 