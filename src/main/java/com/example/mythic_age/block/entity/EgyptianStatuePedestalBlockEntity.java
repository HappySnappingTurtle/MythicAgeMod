package com.example.mythic_age.block.entity;

import com.example.mythic_age.block.EgyptianStatuePedestalBlock;
import com.example.mythic_age.item.ModItems;
import com.example.mythic_age.system.GodPowerCertificateManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 埃及神像基座方块实体
 * 
 * 存储神像信息并处理神力证明产出逻辑
 */
public class EgyptianStatuePedestalBlockEntity extends BlockEntity {
    
    private GodPowerCertificateManager.EgyptianStatueType statueType;
    private boolean hasStatue = false;
    private int productionTick = 0;
    
    public EgyptianStatuePedestalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EGYPTIAN_STATUE_PEDESTAL, pos, state);
    }
    
    public boolean hasStatue() {
        return hasStatue;
    }
    
    public GodPowerCertificateManager.EgyptianStatueType getStatueType() {
        return statueType;
    }
    
    public void setStatue(GodPowerCertificateManager.EgyptianStatueType type) {
        this.statueType = type;
        this.hasStatue = true;
        this.productionTick = 0;
        markDirty();
        
        // 更新方块状态
        updateBlockState();
    }
    
    public void removeStatue() {
        this.statueType = null;
        this.hasStatue = false;
        this.productionTick = 0;
        markDirty();
        
        // 更新方块状态
        updateBlockState();
    }
    
    /**
     * 更新方块状态以匹配当前神像
     */
    private void updateBlockState() {
        if (world != null && !world.isClient) {
            BlockState currentState = world.getBlockState(pos);
            if (currentState.getBlock() instanceof EgyptianStatuePedestalBlock) {
                EgyptianStatuePedestalBlock.StatueState newState = hasStatue ? 
                    EgyptianStatuePedestalBlock.StatueState.fromStatueType(statueType) :
                    EgyptianStatuePedestalBlock.StatueState.EMPTY;
                
                world.setBlockState(pos, currentState.with(EgyptianStatuePedestalBlock.STATUE_STATE, newState));
            }
        }
    }
    
    public static void tick(World world, BlockPos pos, BlockState state, EgyptianStatuePedestalBlockEntity blockEntity) {
        if (world.isClient || !blockEntity.hasStatue) {
            return;
        }
        
        blockEntity.productionTick++;
        
        // 检查是否到达产出时间
        if (blockEntity.productionTick >= blockEntity.statueType.getProductionInterval()) {
            blockEntity.productionTick = 0;
            
            // 产出神力证明
            int amount = blockEntity.statueType.getProductionAmount();
            ItemStack certificate = new ItemStack(ModItems.GOD_POWER_CERTIFICATE_EGYPTIAN, amount);
            
            world.spawnEntity(new net.minecraft.entity.ItemEntity(world, 
                pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, certificate));
        }
    }
    
    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putBoolean("hasStatue", hasStatue);
        nbt.putInt("productionTick", productionTick);
        
        if (hasStatue && statueType != null) {
            nbt.putString("statueType", statueType.getName());
        }
    }
    
    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.hasStatue = nbt.getBoolean("hasStatue");
        this.productionTick = nbt.getInt("productionTick");
        
        if (hasStatue) {
            String typeName = nbt.getString("statueType");
            for (GodPowerCertificateManager.EgyptianStatueType type : GodPowerCertificateManager.EgyptianStatueType.values()) {
                if (type.getName().equals(typeName)) {
                    this.statueType = type;
                    break;
                }
            }
            
            // 重新加载时更新方块状态
            updateBlockState();
        }
    }
} 