package io.github.happysnappingturtle.mythicage.content.greek.temple;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Locale;

/**
 * 三角楣石（T8a 设计文档 §2.2）：有朝向，按左右相邻的同向楣石自动切换形状。
 * 从正面看：左侧有楣石、右侧没有 → 右坡端（RIGHT）；两侧都有 → 中段（MIDDLE）；
 * 只有右侧有 → 左坡端（LEFT）；两侧都没有 → 单块山花（SINGLE，顶端）。
 * 逐行叠放、每行比下一行左右各收 1 格，两端的 45° 斜檐就连成一条直线；最底一行显示齿饰横檐。
 */
public class PedimentBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<PedimentBlock> CODEC = simpleCodec(PedimentBlock::new);
    public static final EnumProperty<Shape> SHAPE = EnumProperty.create("shape", Shape.class);
    /** 最底一行（下方不是楣石）：显示齿饰横檐。 */
    public static final BooleanProperty BOTTOM = BooleanProperty.create("bottom");

    public enum Shape implements StringRepresentable {
        SINGLE, LEFT, MIDDLE, RIGHT;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** 碰撞取下半格实体 + 上半格中间的台，近似三角形外廓。 */
    private static final VoxelShape OUTLINE = Shapes.or(Block.box(0, 0, 0, 16, 8, 16), Block.box(0, 8, 2, 16, 14, 16));

    public PedimentBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SHAPE, Shape.SINGLE)
                .setValue(BOTTOM, true));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SHAPE, BOTTOM);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return OUTLINE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState s = defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
        return s.setValue(SHAPE, shapeAt(ctx.getLevel(), ctx.getClickedPos(), s.getValue(FACING)))
                .setValue(BOTTOM, !ctx.getLevel().getBlockState(ctx.getClickedPos().below()).is(this));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        if (dir.getAxis().isHorizontal()) {
            return state.setValue(SHAPE, shapeAt(level, pos, state.getValue(FACING)));
        }
        if (dir == Direction.DOWN) {
            return state.setValue(BOTTOM, !neighbor.is(this));
        }
        return super.updateShape(state, dir, neighbor, level, pos, neighborPos);
    }

    /** facing 为楣石正面朝向；观察者面对正面时，左手方向是 facing 顺时针转 90°。 */
    private Shape shapeAt(BlockGetter level, BlockPos pos, Direction facing) {
        boolean left = same(level, pos.relative(facing.getClockWise()), facing);
        boolean right = same(level, pos.relative(facing.getCounterClockWise()), facing);
        if (left && right) {
            return Shape.MIDDLE;
        }
        if (left) {
            return Shape.RIGHT;
        }
        if (right) {
            return Shape.LEFT;
        }
        return Shape.SINGLE;
    }

    private boolean same(BlockGetter level, BlockPos pos, Direction facing) {
        BlockState s = level.getBlockState(pos);
        return s.is(this) && s.getValue(FACING) == facing;
    }
}
