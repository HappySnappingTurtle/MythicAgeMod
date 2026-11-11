package io.github.happysnappingturtle.mythicage.content.greek.architecture;

import com.mojang.serialization.MapCodec;
import io.github.happysnappingturtle.mythicage.content.greek.village.TreadingVatBlock;

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

import java.util.Map;

/**
 * 有正面朝向的建筑构件与摆件：默认正面朝向放置者，贴墙构件朝外。
 * alternating 为 true 时（三陇板饰带）按 (x + z) 的奇偶自动设定 ALT：三陇板与陇间板交替，连续摆放即为正确节奏。
 * 贴墙构件取被点击面的朝向，背面贴支撑；形状随正面朝向旋转。
 */
public class FacingShapedBlock extends HorizontalDirectionalBlock {
    public static final BooleanProperty ALT = BooleanProperty.create("alt");
    private final boolean alternating;
    private final Map<Direction, VoxelShape> shapes;
    private final boolean attached;
    private final MapCodec<FacingShapedBlock> codec;

    public FacingShapedBlock(Properties properties, boolean alternating, VoxelShape shape) {
        this(properties, alternating, shape, false);
    }

    public FacingShapedBlock(Properties properties, boolean alternating, VoxelShape shape, boolean attached) {
        super(properties);
        this.alternating = alternating;
        this.shapes = TreadingVatBlock.rotations(shape);
        this.attached = attached;
        this.codec = simpleCodec(p -> new FacingShapedBlock(p, alternating, shape, attached));
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
        return shapes.get(state.getValue(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return hasCollision ? getShape(state, level, pos, ctx) : Shapes.empty();
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
        // 点击面是从支撑指向新方块的法线；正面同向，依附方向与它相反。
        Direction face = ctx.getClickedFace();
        Direction facing = attached && face.getAxis().isHorizontal() ? face : ctx.getHorizontalDirection().getOpposite();
        return defaultBlockState().setValue(FACING, facing)
                .setValue(ALT, alternating && ((p.getX() + p.getZ()) & 1) == 1);
    }

    /** 实心但不是整格：原版默认当作「可走进去」，生物寻路会穿过它而卡住（T10 信徒绕神坛时发现）。 */
    @Override
    protected boolean isPathfindable(BlockState state, net.minecraft.world.level.pathfinder.PathComputationType type) {
        return false;
    }
}
