package com.example.mythic_age.block;

import com.example.mythic_age.block.entity.EgyptianStatuePedestalBlockEntity;
import com.example.mythic_age.block.entity.ModBlockEntities;
import com.example.mythic_age.system.GodPowerCertificateManager;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * 埃及神像基座方块
 * 
 * 可以放置不同等级的神像，周期性产出神力证明。
 * 神像等级：奴隶、市民、士兵、王后、国王、国王王后、法老
 */
public class EgyptianStatuePedestalBlock extends BlockWithEntity {
    
    // 神像状态属性
    public static final EnumProperty<StatueState> STATUE_STATE = EnumProperty.of("statue_state", StatueState.class);
    
    // 方块形状 - 基座形状
    private static final VoxelShape SHAPE = VoxelShapes.union(
        Block.createCuboidShape(2, 0, 2, 14, 2, 14),   // 底座
        Block.createCuboidShape(3, 2, 3, 13, 4, 13),   // 中层
        Block.createCuboidShape(4, 4, 4, 12, 16, 12)   // 柱子
    );
    
    public EgyptianStatuePedestalBlock(Settings settings) {
        super(settings);
        setDefaultState(stateManager.getDefaultState().with(STATUE_STATE, StatueState.EMPTY));
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(STATUE_STATE);
    }
    
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }
    
    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }
    
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (!(blockEntity instanceof EgyptianStatuePedestalBlockEntity pedestalEntity)) {
            return ActionResult.FAIL;
        }
        
        ItemStack handStack = player.getStackInHand(hand);
        
        // 如果玩家手中有神像物品，尝试放置
        if (isStatueItem(handStack)) {
            if (pedestalEntity.hasStatue()) {
                player.sendMessage(Text.translatable("block.mythic_age.egyptian_statue_pedestal.already_has_statue"), true);
                return ActionResult.FAIL;
            }
            
            GodPowerCertificateManager.EgyptianStatueType statueType = getStatueType(handStack);
            if (statueType != null) {
                // 放置神像
                pedestalEntity.setStatue(statueType);
                handStack.decrement(1);
                
                // 更新方块状态
                world.setBlockState(pos, state.with(STATUE_STATE, StatueState.fromStatueType(statueType)));
                
                // 注册到管理器
                GodPowerCertificateManager.getInstance().registerEgyptianStatue(pos, statueType);
                
                player.sendMessage(Text.translatable("block.mythic_age.egyptian_statue_pedestal.statue_placed", 
                    Text.translatable("item.mythic_age.egyptian_statue_" + statueType.getName())), true);
                
                return ActionResult.SUCCESS;
            }
        }
        // 如果基座上有神像且玩家是空手，移除神像
        else if (handStack.isEmpty() && pedestalEntity.hasStatue()) {
            GodPowerCertificateManager.EgyptianStatueType statueType = pedestalEntity.getStatueType();
            
            // 给玩家神像物品
            ItemStack statueItem = getStatueItemStack(statueType);
            if (!player.getInventory().insertStack(statueItem)) {
                player.dropItem(statueItem, false);
            }
            
            // 移除神像
            pedestalEntity.removeStatue();
            
            // 更新方块状态为空
            world.setBlockState(pos, state.with(STATUE_STATE, StatueState.EMPTY));
            
            // 从管理器移除
            GodPowerCertificateManager.getInstance().unregisterEgyptianStatue(pos);
            
            player.sendMessage(Text.translatable("block.mythic_age.egyptian_statue_pedestal.statue_removed"), true);
            
            return ActionResult.SUCCESS;
        }
        
        // 显示基座信息
        if (pedestalEntity.hasStatue()) {
            GodPowerCertificateManager.EgyptianStatueType statueType = pedestalEntity.getStatueType();
            player.sendMessage(Text.translatable("block.mythic_age.egyptian_statue_pedestal.info",
                Text.translatable("item.mythic_age.egyptian_statue_" + statueType.getName()),
                statueType.getProductionAmount(),
                statueType.getProductionInterval() / 20), false);
        } else {
            player.sendMessage(Text.translatable("block.mythic_age.egyptian_statue_pedestal.empty"), false);
        }
        
        return ActionResult.SUCCESS;
    }
    
    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            // 方块被破坏时，从管理器移除
            GodPowerCertificateManager.getInstance().unregisterEgyptianStatue(pos);
            
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof EgyptianStatuePedestalBlockEntity pedestalEntity && pedestalEntity.hasStatue()) {
                // 掉落神像物品
                ItemStack statueItem = getStatueItemStack(pedestalEntity.getStatueType());
                dropStack(world, pos, statueItem);
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }
    
    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new EgyptianStatuePedestalBlockEntity(pos, state);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return checkType(type, ModBlockEntities.EGYPTIAN_STATUE_PEDESTAL, EgyptianStatuePedestalBlockEntity::tick);
    }
    
    /**
     * 检查是否为神像物品
     */
    private boolean isStatueItem(ItemStack stack) {
        String itemName = stack.getItem().toString();
        return itemName.contains("egyptian_statue_");
    }
    
    /**
     * 根据物品获取神像类型
     */
    private GodPowerCertificateManager.EgyptianStatueType getStatueType(ItemStack stack) {
        String itemName = stack.getItem().toString();
        
        if (itemName.contains("egyptian_statue_slave")) {
            return GodPowerCertificateManager.EgyptianStatueType.SLAVE;
        } else if (itemName.contains("egyptian_statue_citizen")) {
            return GodPowerCertificateManager.EgyptianStatueType.CITIZEN;
        } else if (itemName.contains("egyptian_statue_soldier")) {
            return GodPowerCertificateManager.EgyptianStatueType.SOLDIER;
        } else if (itemName.contains("egyptian_statue_queen")) {
            return GodPowerCertificateManager.EgyptianStatueType.QUEEN;
        } else if (itemName.contains("egyptian_statue_king")) {
            return GodPowerCertificateManager.EgyptianStatueType.KING;
        } else if (itemName.contains("egyptian_statue_royal_couple")) {
            return GodPowerCertificateManager.EgyptianStatueType.ROYAL_COUPLE;
        } else if (itemName.contains("egyptian_statue_pharaoh")) {
            return GodPowerCertificateManager.EgyptianStatueType.PHARAOH;
        }
        
        return null;
    }
    
    /**
     * 根据神像类型获取对应的物品堆叠
     */
    private ItemStack getStatueItemStack(GodPowerCertificateManager.EgyptianStatueType type) {
        // 这里需要根据实际的神像物品来创建ItemStack
        // 暂时使用金锭作为占位符
        return new ItemStack(Items.GOLD_INGOT);
    }
    
    /**
     * 神像状态枚举 - 用于方块状态和纹理切换
     */
    public enum StatueState implements StringIdentifiable {
        EMPTY("empty"),
        SLAVE("slave"),
        CITIZEN("citizen"),
        SOLDIER("soldier"),
        QUEEN("queen"),
        KING("king"),
        ROYAL_COUPLE("royal_couple"),
        PHARAOH("pharaoh");
        
        private final String name;
        
        StatueState(String name) {
            this.name = name;
        }
        
        @Override
        public String asString() {
            return name;
        }
        
        public static StatueState fromStatueType(GodPowerCertificateManager.EgyptianStatueType type) {
            return switch (type) {
                case SLAVE -> SLAVE;
                case CITIZEN -> CITIZEN;
                case SOLDIER -> SOLDIER;
                case QUEEN -> QUEEN;
                case KING -> KING;
                case ROYAL_COUPLE -> ROYAL_COUPLE;
                case PHARAOH -> PHARAOH;
            };
        }
    }
} 