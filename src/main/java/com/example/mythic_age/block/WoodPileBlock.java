package com.example.mythic_age.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.entity.ItemEntity;

import java.util.List;
import java.util.ArrayList;

/**
 * 一堆木头方块
 * 
 * 丰饶法术生成的木材资源方块，为玩家提供大量的木材资源。
 * 当被破坏时会掉落24-36个随机的木材物品。
 * 
 * 功能特性：
 * - 被破坏时爆出大量木材
 * - 包含所有游戏中的木材类型
 * - 数量随机在24-36之间
 * - 包含原木和木板两种形式
 * 
 * 掉落物品包括：
 * - 各种原木：橡木、桦木、云杉木、丛林木、金合欢木、深色橡木、红树木、樱花木
 * - 各种木板：对应上述原木的木板形式
 * - 竹子和竹木板
 * - 其他木质材料
 * 
 * @author MythicAge Development Team
 * @version 1.0.0
 * @since 1.20.1
 */
public class WoodPileBlock extends Block {

    /**
     * 所有可掉落的木材物品列表
     */
    private static final List<ItemStack> WOOD_ITEMS = new ArrayList<>();
    
    static {
        // 原木类
        WOOD_ITEMS.add(new ItemStack(Items.OAK_LOG));
        WOOD_ITEMS.add(new ItemStack(Items.BIRCH_LOG));
        WOOD_ITEMS.add(new ItemStack(Items.SPRUCE_LOG));
        WOOD_ITEMS.add(new ItemStack(Items.JUNGLE_LOG));
        WOOD_ITEMS.add(new ItemStack(Items.ACACIA_LOG));
        WOOD_ITEMS.add(new ItemStack(Items.DARK_OAK_LOG));
        WOOD_ITEMS.add(new ItemStack(Items.MANGROVE_LOG));
        WOOD_ITEMS.add(new ItemStack(Items.CHERRY_LOG));
        
        // 木板类
        WOOD_ITEMS.add(new ItemStack(Items.OAK_PLANKS));
        WOOD_ITEMS.add(new ItemStack(Items.BIRCH_PLANKS));
        WOOD_ITEMS.add(new ItemStack(Items.SPRUCE_PLANKS));
        WOOD_ITEMS.add(new ItemStack(Items.JUNGLE_PLANKS));
        WOOD_ITEMS.add(new ItemStack(Items.ACACIA_PLANKS));
        WOOD_ITEMS.add(new ItemStack(Items.DARK_OAK_PLANKS));
        WOOD_ITEMS.add(new ItemStack(Items.MANGROVE_PLANKS));
        WOOD_ITEMS.add(new ItemStack(Items.CHERRY_PLANKS));
        
        // 剥皮原木
        WOOD_ITEMS.add(new ItemStack(Items.STRIPPED_OAK_LOG));
        WOOD_ITEMS.add(new ItemStack(Items.STRIPPED_BIRCH_LOG));
        WOOD_ITEMS.add(new ItemStack(Items.STRIPPED_SPRUCE_LOG));
        WOOD_ITEMS.add(new ItemStack(Items.STRIPPED_JUNGLE_LOG));
        WOOD_ITEMS.add(new ItemStack(Items.STRIPPED_ACACIA_LOG));
        WOOD_ITEMS.add(new ItemStack(Items.STRIPPED_DARK_OAK_LOG));
        WOOD_ITEMS.add(new ItemStack(Items.STRIPPED_MANGROVE_LOG));
        WOOD_ITEMS.add(new ItemStack(Items.STRIPPED_CHERRY_LOG));
        
        // 竹子和其他
        WOOD_ITEMS.add(new ItemStack(Items.BAMBOO));
        WOOD_ITEMS.add(new ItemStack(Items.BAMBOO_PLANKS));
        WOOD_ITEMS.add(new ItemStack(Items.STICK));
    }

    public WoodPileBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && !world.isClient) {
            // 生成24-36个随机木材物品
            int itemCount = 24 + world.random.nextInt(13); // 24-36
            
            for (int i = 0; i < itemCount; i++) {
                // 随机选择一个木材物品
                ItemStack woodItem = WOOD_ITEMS.get(world.random.nextInt(WOOD_ITEMS.size())).copy();
                
                // 随机堆叠数量 (1-8，木材堆叠更多)
                int stackSize = 1 + world.random.nextInt(8);
                woodItem.setCount(Math.min(stackSize, woodItem.getMaxCount()));
                
                // 在方块位置周围随机掉落
                double offsetX = (world.random.nextDouble() - 0.5) * 2.0;
                double offsetY = world.random.nextDouble() * 0.5;
                double offsetZ = (world.random.nextDouble() - 0.5) * 2.0;
                
                ItemEntity itemEntity = new ItemEntity(world, 
                    pos.getX() + 0.5 + offsetX, 
                    pos.getY() + 0.5 + offsetY, 
                    pos.getZ() + 0.5 + offsetZ, 
                    woodItem);
                
                // 添加随机速度
                itemEntity.setVelocity(
                    (world.random.nextDouble() - 0.5) * 0.2,
                    world.random.nextDouble() * 0.2,
                    (world.random.nextDouble() - 0.5) * 0.2
                );
                
                world.spawnEntity(itemEntity);
            }
        }
        
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient) {
            // 向玩家发送提示消息
            player.sendMessage(net.minecraft.text.Text.literal("§6木材堆分散！获得了大量木材！"), false);
        }
        super.onBreak(world, pos, state, player);
    }
} 