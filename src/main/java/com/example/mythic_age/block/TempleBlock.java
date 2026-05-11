package com.example.mythic_age.block;

import com.example.mythic_age.item.GodPowerCertificateItem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 神殿方块类
 * 
 * 神殿是神话村庄中的特殊建筑，具有以下功能：
 * - 作为村庄的宗教中心建筑
 * - 内部包含圣物基座和神像
 * - 为神殿守护者提供工作场所
 * - 影响村庄的神话背景类型
 * 
 * 建筑特点：
 * - 使用特殊的建筑材料和装饰
 * - 具有独特的建筑风格
 * - 内部空间较大，可容纳多个功能区域
 * - 具有神圣的氛围效果
 */
public class TempleBlock extends Block {
    
    private final GodPowerCertificateItem.MythologyType mythologyType;
    
    /**
     * 构造函数
     * 
     * @param settings 方块设置
     * @param mythologyType 神话背景类型
     */
    public TempleBlock(Settings settings, GodPowerCertificateItem.MythologyType mythologyType) {
        super(settings);
        this.mythologyType = mythologyType;
    }
    
    /**
     * 获取神话背景类型
     * 
     * @return 神话背景类型
     */
    public GodPowerCertificateItem.MythologyType getMythologyType() {
        return mythologyType;
    }
    
    /**
     * 玩家右键点击神殿时的处理
     * 
     * 显示神殿信息和功能提示
     */
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            // 显示神殿信息
            player.sendMessage(Text.translatable("mythic_age.temple.info", mythologyType.getChineseName())
                .formatted(mythologyType.getColor()), false);
            
            player.sendMessage(Text.translatable("mythic_age.temple.description")
                .formatted(net.minecraft.util.Formatting.GRAY), false);
        }
        
        return ActionResult.SUCCESS;
    }
} 