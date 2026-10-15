package io.github.happysnappingturtle.mythicage.content.greek.architecture;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * 卵石马赛克（T8b 设计文档 §2.6）：每 2×2 方块组成一幅图案。放置时按世界坐标选象限，连续铺满就是连续图案。
 * quadrant = (x 的奇偶) + 2 × (z 的奇偶)。
 */
public class MosaicBlock extends Block {
    public static final IntegerProperty QUADRANT = IntegerProperty.create("quadrant", 0, 3);

    public MosaicBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(QUADRANT, 0));
    }

    public static int quadrantAt(BlockPos p) {
        return (p.getX() & 1) + 2 * (p.getZ() & 1);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(QUADRANT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(QUADRANT, quadrantAt(ctx.getClickedPos()));
    }

    /** 命令（/fill、/setblock）与结构放置不经过 getStateForPlacement：放下后按坐标校正象限。 */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        int q = quadrantAt(pos);
        if (!level.isClientSide && state.getValue(QUADRANT) != q) {
            level.setBlock(pos, state.setValue(QUADRANT, q), Block.UPDATE_CLIENTS);
        }
    }
}
