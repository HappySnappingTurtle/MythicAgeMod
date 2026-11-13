package io.github.happysnappingturtle.mythicage.content.greek.temple;

import com.mojang.serialization.MapCodec;
import io.github.happysnappingturtle.mythicage.content.greek.architecture.FacingShapedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 村庄祭坛（GA-13）：外观同奥林匹斯祭坛。某个村子神庙的那一块（卫城 {@code ritual} 旁）可以献供、打开检视；
 * 其余（玩家合成放下的）只是装饰。交互见 {@link VillageAltarUse}。
 */
public class VillageAltarBlock extends FacingShapedBlock {
    private final MapCodec<VillageAltarBlock> codec;

    public VillageAltarBlock(Properties properties, VoxelShape shape) {
        super(properties, false, shape);
        this.codec = simpleCodec(p -> new VillageAltarBlock(p, shape));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return codec;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        return VillageAltarUse.useItemOn(stack, level, pos, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return VillageAltarUse.useWithoutItem(level, pos, player) ? InteractionResult.sidedSuccess(level.isClientSide)
                : InteractionResult.PASS;
    }
}
