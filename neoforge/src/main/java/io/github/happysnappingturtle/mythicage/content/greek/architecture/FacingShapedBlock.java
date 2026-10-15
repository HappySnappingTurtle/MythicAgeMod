package io.github.happysnappingturtle.mythicage.content.greek.architecture;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 有正面朝向的檐部构件（额枋、三陇板饰带、瓦当）：正面朝向放置者。
 * alternating 为 true 时（三陇板饰带）按 (x + z) 的奇偶自动设定 ALT：三陇板与陇间板交替，连续摆放即为正确节奏。
 * collides 为 false 时（瓦当）没有碰撞箱。
 */
public class FacingShapedBlock extends HorizontalDirectionalBlock {
    public static final BooleanProperty ALT = BooleanProperty.create("alt");
    private final boolean alternating;
    private final VoxelShape shape;
    private final MapCodec<FacingShapedBlock> codec;

    public FacingShapedBlock(Properties properties, boolean alternating, VoxelShape shape) {
        super(properties);
        this.alternating = alternating;
        this.shape = shape;
        this.codec = simpleCodec(p -> new FacingShapedBlock(p, alternating, shape));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ALT, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ALT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shape;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return hasCollision ? shape : Shapes.empty();
    }

    /** 命令与结构放置不经过 getStateForPlacement：交替型方块放下后按坐标校正 ALT。 */
    @Override
    protected void onPlace(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState oldState,
                           boolean moving) {
        boolean alt = alternating && ((pos.getX() + pos.getZ()) & 1) == 1;
        if (!level.isClientSide && state.getValue(ALT) != alt) {
            level.setBlock(pos, state.setValue(ALT, alt), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockPos p = ctx.getClickedPos();
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite())
                .setValue(ALT, alternating && ((p.getX() + p.getZ()) & 1) == 1);
    }
}
