package com.example.mythic_age.block;

import com.example.mythic_age.block.entity.StatueBaseBlockEntity;
import com.example.mythic_age.block.entity.ModBlockEntities;
import com.example.mythic_age.item.GodPowerCertificateItem;
import com.example.mythic_age.item.ModItems;
import com.example.mythic_age.system.GodPowerCertificateManager;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * 神像基座方块
 * 
 * 这是神话时代Mod中各个文明神像的基座方块，具有以下功能：
 * - 检测和储存村庄产生的神力证明
 * - 允许玩家点击获取储存的神力证明
 * - 显示当前储存的神力证明数量
 * - 根据文明类型提供不同的交互体验
 * 
 * 核心机制：
 * 1. 村庄机制会定期向附近的神像基座储存神力证明
 * 2. 玩家可以右键点击基座查看储存数量
 * 3. 玩家可以通过交互界面提取神力证明
 * 4. 不同文明的基座会储存对应文明的神力证明
 */
public class StatueBaseBlock extends BlockWithEntity {
    
    private final GodPowerCertificateItem.MythologyType mythologyType;
    
    // 方块形状 - 基座形状
    private static final VoxelShape SHAPE = VoxelShapes.union(
        Block.createCuboidShape(1, 0, 1, 15, 4, 15),   // 底座
        Block.createCuboidShape(2, 4, 2, 14, 8, 14),   // 中层
        Block.createCuboidShape(3, 8, 3, 13, 16, 13)   // 顶层
    );
    
    public StatueBaseBlock(Settings settings, GodPowerCertificateItem.MythologyType mythologyType) {
        super(settings);
        this.mythologyType = mythologyType;
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
        if (!(blockEntity instanceof StatueBaseBlockEntity baseEntity)) {
            return ActionResult.FAIL;
        }
        
        // 检查是否为正确的神像多方块结构
        if (!isValidStatueStructure(world, pos)) {
            player.sendMessage(Text.translatable("block.mythic_age.statue_base.incomplete_structure", 
                getCivilizationName()).formatted(Formatting.RED), true);
            return ActionResult.FAIL;
        }
        
        int storedAmount = baseEntity.getStoredCertificates();
        
        if (player.isSneaking()) {
            // 潜行+右键：获取储存的神力证明
            if (storedAmount > 0) {
                extractCertificates(player, baseEntity, storedAmount);
            } else {
                player.sendMessage(Text.translatable("block.mythic_age.statue_base.no_certificates", 
                    getCivilizationName()).formatted(Formatting.YELLOW), true);
                showHowToObtain(player);
            }
        } else {
            // 普通右键：显示详细信息和状态
            showDetailedInfo(player, storedAmount);
            if (storedAmount == 0) {
                showHowToObtain(player);
            }
        }
        
        return ActionResult.SUCCESS;
    }
    
    /**
     * 检查是否为有效的神像多方块结构
     */
    private boolean isValidStatueStructure(World world, BlockPos basePos) {
        // 检查基座上方的结构：腿部 -> 身体 -> 头部
        BlockPos legsPos = basePos.up();
        BlockPos bodyPos = basePos.up(2);
        BlockPos headPos = basePos.up(3);
        
        String expectedPrefix = "statue_" + mythologyType.name().toLowerCase();
        
        Block legsBlock = world.getBlockState(legsPos).getBlock();
        Block bodyBlock = world.getBlockState(bodyPos).getBlock();
        Block headBlock = world.getBlockState(headPos).getBlock();
        
        return legsBlock.toString().contains(expectedPrefix + "_legs") &&
               bodyBlock.toString().contains(expectedPrefix + "_body") &&
               headBlock.toString().contains(expectedPrefix + "_head");
    }
    
    /**
     * 显示详细信息
     */
    private void showDetailedInfo(PlayerEntity player, int storedAmount) {
        player.sendMessage(Text.empty(), false);
        player.sendMessage(Text.translatable("block.mythic_age.statue_base.title", 
            getCivilizationName()).formatted(Formatting.GOLD, Formatting.BOLD), false);
        player.sendMessage(Text.empty(), false);
        
        player.sendMessage(Text.translatable("block.mythic_age.statue_base.stored_amount", 
            storedAmount, getCertificateName()).formatted(Formatting.AQUA), false);
        
        player.sendMessage(Text.translatable("block.mythic_age.statue_base.source_info", 
            getCivilizationName()).formatted(Formatting.GRAY), false);
        
        player.sendMessage(Text.empty(), false);
        player.sendMessage(Text.translatable("block.mythic_age.statue_base.extraction_hint_sneak")
            .formatted(Formatting.GREEN), false);
        player.sendMessage(Text.translatable("block.mythic_age.statue_base.info_hint")
            .formatted(Formatting.DARK_GRAY), false);
    }
    
    /**
     * 提取神力证明
     */
    private void extractCertificates(PlayerEntity player, StatueBaseBlockEntity baseEntity, int storedAmount) {
        // 一次性提取所有神力证明
        ItemStack certificateStack = createCertificateStack(storedAmount);
        
        // 尝试添加到玩家背包
        if (player.getInventory().insertStack(certificateStack)) {
            // 成功添加到背包
            baseEntity.extractAllCertificates();
            
            player.sendMessage(Text.translatable("block.mythic_age.statue_base.extraction_success", 
                storedAmount, getCertificateName()).formatted(Formatting.GREEN), true);
            
            // 播放提取音效
            player.getWorld().playSound(null, baseEntity.getPos(), net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 
                net.minecraft.sound.SoundCategory.BLOCKS, 0.8f, 1.2f);
        } else {
            // 背包已满
            player.sendMessage(Text.translatable("block.mythic_age.statue_base.inventory_full")
                .formatted(Formatting.RED), true);
        }
    }
    
    /**
     * 显示如何获得神力证明的提示
     */
    private void showHowToObtain(PlayerEntity player) {
        switch (mythologyType) {
            case GREEK -> player.sendMessage(Text.translatable("block.mythic_age.statue_base.obtain_greek")
                .formatted(Formatting.GRAY), false);
            case EGYPTIAN -> player.sendMessage(Text.translatable("block.mythic_age.statue_base.obtain_egyptian")
                .formatted(Formatting.GRAY), false);
            case CHINESE -> player.sendMessage(Text.translatable("block.mythic_age.statue_base.obtain_chinese")
                .formatted(Formatting.GRAY), false);
            case ATLANTEAN -> player.sendMessage(Text.translatable("block.mythic_age.statue_base.obtain_atlantean")
                .formatted(Formatting.GRAY), false);
            case NORDIC -> player.sendMessage(Text.translatable("block.mythic_age.statue_base.obtain_nordic")
                .formatted(Formatting.GRAY), false);
        }
    }
    
    /**
     * 创建对应文明的神力证明物品堆叠
     */
    private ItemStack createCertificateStack(int amount) {
        return switch (mythologyType) {
            case GREEK -> new ItemStack(ModItems.GOD_POWER_CERTIFICATE_GREEK, amount);
            case EGYPTIAN -> new ItemStack(ModItems.GOD_POWER_CERTIFICATE_EGYPTIAN, amount);
            case CHINESE -> new ItemStack(ModItems.GOD_POWER_CERTIFICATE_CHINESE, amount);
            case ATLANTEAN -> new ItemStack(ModItems.GOD_POWER_CERTIFICATE_ATLANTEAN, amount);
            case NORDIC -> new ItemStack(ModItems.GOD_POWER_CERTIFICATE_NORDIC, amount);
        };
    }
    
    /**
     * 获取文明名称
     */
    private String getCivilizationName() {
        return switch (mythologyType) {
            case GREEK -> "希腊";
            case EGYPTIAN -> "埃及";
            case CHINESE -> "中国";
            case ATLANTEAN -> "亚特兰蒂斯";
            case NORDIC -> "北欧";
        };
    }
    
    /**
     * 获取神力证明名称
     */
    private String getCertificateName() {
        return getCivilizationName() + "神力证明";
    }
    
    /**
     * 村庄系统调用：添加神力证明到基座
     */
    public void addCertificates(World world, BlockPos pos, int amount) {
        if (world instanceof ServerWorld) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof StatueBaseBlockEntity baseEntity) {
                baseEntity.addCertificates(amount);
                
                // 播放添加音效
                world.playSound(null, pos, net.minecraft.sound.SoundEvents.BLOCK_BEACON_ACTIVATE, 
                    net.minecraft.sound.SoundCategory.BLOCKS, 0.5f, 1.8f);
            }
        }
    }
    
    /**
     * 获取文明类型
     */
    public GodPowerCertificateItem.MythologyType getMythologyType() {
        return mythologyType;
    }
    
    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new StatueBaseBlockEntity(pos, state, mythologyType);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        BlockEntityType<StatueBaseBlockEntity> expectedType = getBlockEntityType();
        return checkType(type, expectedType, StatueBaseBlockEntity::tick);
    }
    
    /**
     * 获取当前文明类型对应的 BlockEntityType
     */
    private BlockEntityType<StatueBaseBlockEntity> getBlockEntityType() {
        return switch (mythologyType) {
            case GREEK -> ModBlockEntities.STATUE_GREEK_BASE;
            case EGYPTIAN -> ModBlockEntities.STATUE_EGYPTIAN_BASE;
            case CHINESE -> ModBlockEntities.STATUE_CHINESE_BASE;
            case ATLANTEAN -> ModBlockEntities.STATUE_ATLANTEAN_BASE;
            case NORDIC -> ModBlockEntities.STATUE_NORDIC_BASE;
        };
    }
    
    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            // 方块被破坏时，掉落储存的神力证明
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof StatueBaseBlockEntity baseEntity) {
                int storedAmount = baseEntity.getStoredCertificates();
                if (storedAmount > 0) {
                    ItemStack certificates = createCertificateStack(storedAmount);
                    dropStack(world, pos, certificates);
                }
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }
}