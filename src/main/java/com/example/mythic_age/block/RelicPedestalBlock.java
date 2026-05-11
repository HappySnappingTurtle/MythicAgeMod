package com.example.mythic_age.block;

import com.example.mythic_age.block.entity.ModBlockEntities;
import com.example.mythic_age.block.entity.RelicPedestalBlockEntity;
import com.example.mythic_age.item.RelicItem;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * 圣物基座方块类
 * 
 * 这是神话时代Mod中神殿系统的核心方块，用于放置和激活神话圣物。
 * 圣物基座是构建神殿多方块结构的关键组件，当正确建造时可以激活圣物的神殿效果。
 * 
 * 核心功能：
 * - 圣物存储：可以存放一个神话圣物物品
 * - 交互系统：玩家可以通过右键放置或取出圣物
 * - 结构检测：自动检测周围是否构成有效的神殿结构
 * - 效果激活：当神殿结构完整时激活圣物的区域效果
 * - 红石输出：根据是否放置圣物输出红石信号
 * 
 * 神殿结构要求：
 * - 基座下方必须有3x3的石砖平台
 * - 基座位于平台中央的上方一格
 * - 结构完整时会自动激活圣物的神殿效果
 * 
 * 交互逻辑：
 * - 空手右键：从基座取出圣物
 * - 手持圣物右键：将圣物放置在基座上
 * - 只能放置RelicItem类型的物品
 * - 破坏方块时会掉落存储的圣物
 * 
 * 视觉特性：
 * - 自定义的3D模型和碰撞箱
 * - 基座底部和顶部的双层设计
 * - 支持自定义纹理和渲染
 * 
 * 技术特性：
 * - 基于BlockEntity的数据存储
 * - 实时的结构检测和效果更新
 * - 完整的NBT数据持久化
 * - 红石兼容性支持
 * 
 * @author MythicAge Development Team
 * @version 1.0.0
 * @since 1.20.1
 */
public class RelicPedestalBlock extends BlockWithEntity implements BlockEntityProvider {
    /** 
     * 基座的3D碰撞箱形状
     * 由两部分组成：底部的宽基座和顶部的窄基座，形成经典的基座外观
     * 底部基座：12x4x12像素 (留2像素边距)
     * 顶部基座：8x8x8像素 (留4像素边距)
     */
    protected static final VoxelShape SHAPE = VoxelShapes.union(
        Block.createCuboidShape(2.0D, 0.0D, 2.0D, 14.0D, 4.0D, 14.0D), // 底部基座
        Block.createCuboidShape(4.0D, 4.0D, 4.0D, 12.0D, 12.0D, 12.0D)  // 顶部基座
    );

    public RelicPedestalBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL; // Use a custom model for this block
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new RelicPedestalBlockEntity(pos, state);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof RelicPedestalBlockEntity) {
                RelicPedestalBlockEntity pedestalEntity = (RelicPedestalBlockEntity) blockEntity;
                ItemStack heldStack = player.getStackInHand(hand);
                ItemStack pedestalStack = pedestalEntity.getRelicStack();

                if (heldStack.isEmpty() && !pedestalStack.isEmpty()) {
                    // Player has empty hand, take relic from pedestal
                    if (!player.getInventory().insertStack(pedestalStack.copy())) {
                        ItemScatterer.spawn(world, pos.getX(), pos.getY() + 1, pos.getZ(), pedestalStack);
                    }
                    pedestalEntity.setRelicStack(ItemStack.EMPTY);
                    world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 1.0f, 1.0f);
                    return ActionResult.SUCCESS;
                } else if (!heldStack.isEmpty() && heldStack.getItem() instanceof RelicItem && pedestalStack.isEmpty()) {
                    // Player is holding a relic, place it on pedestal
                    pedestalEntity.setRelicStack(heldStack.split(1)); // Take one item from stack
                    world.playSound(null, pos, SoundEvents.BLOCK_STONE_PLACE, SoundCategory.BLOCKS, 1.0f, 1.0f);
                    return ActionResult.SUCCESS;
                }
            }
        }
        return ActionResult.PASS;
    }

    // Called when the block is broken to drop its contents
    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof RelicPedestalBlockEntity) {
                ItemStack relicStack = ((RelicPedestalBlockEntity)blockEntity).getRelicStack();
                if (!relicStack.isEmpty()) {
                    ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), relicStack);
                }
                // Update comparators
                world.updateComparators(pos,this);
            }
            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return checkType(type, ModBlockEntities.RELIC_PEDESTAL_BLOCK_ENTITY, RelicPedestalBlockEntity::tick);
    }

     @Override
    public boolean hasComparatorOutput(BlockState state) {
        return true; // So it can output a redstone signal based on whether it holds an item
    }

    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof RelicPedestalBlockEntity) {
            RelicPedestalBlockEntity pedestalEntity = (RelicPedestalBlockEntity) blockEntity;
            return pedestalEntity.getRelicStack().isEmpty() ? 0 : 15;
        }
        return 0;
    }
}

