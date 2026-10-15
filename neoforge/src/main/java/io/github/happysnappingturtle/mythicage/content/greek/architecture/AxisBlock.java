package io.github.happysnappingturtle.mythicage.content.greek.architecture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 只有水平轴向的模型方块（爱奥尼柱头、屋脊）：放置时取放置者朝向的轴。
 * 爱奥尼柱头：axis = z 时涡卷面朝南北（玩家朝南放置，正对涡卷）；屋脊：axis 为脊线方向的垂直方向（顺着玩家视线铺坡）。
 */
public class AxisBlock extends Block {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    private final VoxelShape shapeX;
    private final VoxelShape shapeZ;

    public AxisBlock(Properties properties, VoxelShape shapeX, VoxelShape shapeZ) {
        super(properties);
        this.shapeX = shapeX;
        this.shapeZ = shapeZ;
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.Z));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(AXIS) == Direction.Axis.X ? shapeX : shapeZ;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(AXIS, ctx.getHorizontalDirection().getAxis());
    }
}
