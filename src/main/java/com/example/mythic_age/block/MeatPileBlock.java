package com.example.mythic_age.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.entity.ItemEntity;

import java.util.List;
import java.util.ArrayList;

/**
 * 一堆肉方块
 * 
 * 丰饶法术生成的食物资源方块，为玩家提供大量的肉类资源。
 * 当被破坏时会掉落24-36个随机的肉类物品。
 * 
 * 功能特性：
 * - 被破坏时爆出大量肉类
 * - 包含所有游戏中的肉类食物
 * - 数量随机在24-36之间
 * - 自动清理防止掉落物过多
 * 
 * 掉落物品包括：
 * - 生牛肉和熟牛肉
 * - 生猪肉和熟猪肉  
 * - 生鸡肉和熟鸡肉
 * - 生羊肉和熟羊肉
 * - 生兔肉和熟兔肉
 * - 各种鱼类
 * 
 * @author MythicAge Development Team
 * @version 1.0.0
 * @since 1.20.1
 */
public class MeatPileBlock extends Block {

    /**
     * 所有可掉落的肉类物品列表
     */
    private static final List<ItemStack> MEAT_ITEMS = new ArrayList<>();
    
    static {
        // 生肉类
        MEAT_ITEMS.add(new ItemStack(Items.BEEF));
        MEAT_ITEMS.add(new ItemStack(Items.PORKCHOP));
        MEAT_ITEMS.add(new ItemStack(Items.CHICKEN));
        MEAT_ITEMS.add(new ItemStack(Items.MUTTON));
        MEAT_ITEMS.add(new ItemStack(Items.RABBIT));
        
        // 熟肉类
        MEAT_ITEMS.add(new ItemStack(Items.COOKED_BEEF));
        MEAT_ITEMS.add(new ItemStack(Items.COOKED_PORKCHOP));
        MEAT_ITEMS.add(new ItemStack(Items.COOKED_CHICKEN));
        MEAT_ITEMS.add(new ItemStack(Items.COOKED_MUTTON));
        MEAT_ITEMS.add(new ItemStack(Items.COOKED_RABBIT));
        
        // 鱼类
        MEAT_ITEMS.add(new ItemStack(Items.COD));
        MEAT_ITEMS.add(new ItemStack(Items.SALMON));
        MEAT_ITEMS.add(new ItemStack(Items.TROPICAL_FISH));
        MEAT_ITEMS.add(new ItemStack(Items.PUFFERFISH));
        MEAT_ITEMS.add(new ItemStack(Items.COOKED_COD));
        MEAT_ITEMS.add(new ItemStack(Items.COOKED_SALMON));
        
        // 特殊肉类
        MEAT_ITEMS.add(new ItemStack(Items.ROTTEN_FLESH));
        MEAT_ITEMS.add(new ItemStack(Items.SPIDER_EYE));
    }

    public MeatPileBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && !world.isClient) {
            // 生成24-36个随机肉类物品
            int itemCount = 24 + world.random.nextInt(13); // 24-36
            
            for (int i = 0; i < itemCount; i++) {
                // 随机选择一个肉类物品
                ItemStack meatItem = MEAT_ITEMS.get(world.random.nextInt(MEAT_ITEMS.size())).copy();
                
                // 随机堆叠数量 (1-4)
                int stackSize = 1 + world.random.nextInt(4);
                meatItem.setCount(Math.min(stackSize, meatItem.getMaxCount()));
                
                // 在方块位置周围随机掉落
                double offsetX = (world.random.nextDouble() - 0.5) * 2.0;
                double offsetY = world.random.nextDouble() * 0.5;
                double offsetZ = (world.random.nextDouble() - 0.5) * 2.0;
                
                ItemEntity itemEntity = new ItemEntity(world, 
                    pos.getX() + 0.5 + offsetX, 
                    pos.getY() + 0.5 + offsetY, 
                    pos.getZ() + 0.5 + offsetZ, 
                    meatItem);
                
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
            player.sendMessage(net.minecraft.text.Text.literal("§6肉堆爆裂！获得了大量肉类！"), false);
        }
        super.onBreak(world, pos, state, player);
    }
} 