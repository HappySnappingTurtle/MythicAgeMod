package io.github.happysnappingturtle.mythicage.content.greek.power;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 诱兽石（GP-GR-02）：两格高的立石，只由神力作为两个临时方块放下、到期各自复原
 * （{@link io.github.happysnappingturtle.mythicage.power.TempBlocks}）。不可挖掘、不掉落、不能被活塞推动；
 * 村民与动物寻路把它当作障碍。
 * <p>上下两半不像门那样互相拆除：复原由临时方块登记逐格完成，若一半先变成空气、另一半跟着自拆，
 * 后复原的那格就认不出自己放下的方块而漏掉原方块。缺了一半时由诱兽石区域逐刻发现并整块复原（{@link LurePower}）。
 */
public class LureStoneBlock extends Block {
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    private static final VoxelShape LOWER = Block.box(2, 0, 3, 14, 16, 13);
    private static final VoxelShape UPPER = Block.box(4, 0, 4, 13, 15, 12);

    public LureStoneBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? LOWER : UPPER;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}
