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
 * 缓坡 1:2（T8a 修订 02，真实神庙屋顶坡度约 12–15°，45° 会像民居尖顶）：逐行叠放、每行左右各收 2 格，
 * 两端的斜檐连成一条直线；最底一行显示齿饰横檐。形状规则见 {@link #shapeAt}。
 */
public class PedimentBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<PedimentBlock> CODEC = simpleCodec(PedimentBlock::new);
    public static final EnumProperty<Shape> SHAPE = EnumProperty.create("shape", Shape.class);
    /** 最底一行（下方不是楣石）：显示齿饰横檐。 */
    public static final BooleanProperty BOTTOM = BooleanProperty.create("bottom");

    public enum Shape implements StringRepresentable {
        SINGLE, LEFT, LEFT_HIGH, MIDDLE, RIGHT_HIGH, RIGHT;

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

    /**
     * facing 为楣石正面朝向；观察者面对正面时，左手方向是 facing 顺时针转 90°。
     * 缓坡 1:2（修订 02）：每行两端各有两块斜坡块——最外一块 LEFT/RIGHT（斜檐从 0 升到半格），
     * 第二块 LEFT_HIGH/RIGHT_HIGH（从半格升到整格）。行宽每行少 4，奇数宽（1、5、9、13…）最顶一块为 SINGLE。
     */
    private Shape shapeAt(BlockGetter level, BlockPos pos, Direction facing) {
        Direction lDir = facing.getClockWise();
        Direction rDir = facing.getCounterClockWise();
        boolean l1 = same(level, pos.relative(lDir), facing);
        boolean l2 = l1 && same(level, pos.relative(lDir, 2), facing);
        boolean r1 = same(level, pos.relative(rDir), facing);
        boolean r2 = r1 && same(level, pos.relative(rDir, 2), facing);
        if (!l1 && !r1) {
            return Shape.SINGLE;
        }
        if (!l1) {
            return Shape.LEFT;
        }
        if (!r1) {
            return Shape.RIGHT;
        }
        if (!l2) {
            return Shape.LEFT_HIGH;
        }
        if (!r2) {
            return Shape.RIGHT_HIGH;
        }
        return Shape.MIDDLE;
    }

    /** 命令与结构放置时邻居不一定逐个更新：放下后按两侧再算一次形状。 */
    @Override
    protected void onPlace(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState old,
                           boolean moving) {
        if (level.isClientSide) {
            return;
        }
        BlockState fixed = state.setValue(SHAPE, shapeAt(level, pos, state.getValue(FACING)))
                .setValue(BOTTOM, !level.getBlockState(pos.below()).is(this));
        if (fixed != state) {
            level.setBlock(pos, fixed, net.minecraft.world.level.block.Block.UPDATE_ALL);
        }
    }

    private boolean same(BlockGetter level, BlockPos pos, Direction facing) {
        BlockState s = level.getBlockState(pos);
        return s.is(this) && s.getValue(FACING) == facing;
    }

    /** 实心但不是整格：原版默认当作「可走进去」，生物寻路会穿过它而卡住（T10 信徒绕神坛时发现）。 */
    @Override
    protected boolean isPathfindable(BlockState state, net.minecraft.world.level.pathfinder.PathComputationType type) {
        return false;
    }
}
