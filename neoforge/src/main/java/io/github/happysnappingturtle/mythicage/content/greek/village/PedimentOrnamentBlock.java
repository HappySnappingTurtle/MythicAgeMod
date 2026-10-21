package io.github.happysnappingturtle.mythicage.content.greek.village;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Locale;
import java.util.Map;

/**
 * 三角楣饰（规划篇 §5 主神差异：宙斯雷霆、波塞冬三叉戟与海浪）：放在山花正面前一格、背面贴着山花的浮雕，
 * 朝向 = 浮雕正面（贴着山花点它的正面放下时取被点的面）。同种同向的楣饰横向相连时自动拼成一幅：
 * 两端 {@code start}/{@code end}、中间 {@code middle}、单独一格 {@code single}；{@code stacks} 为真时，
 * 叠在同种楣饰上面的那格是 {@code top}（三叉戟的叉头）。没有碰撞，只有选取框。
 */
public class PedimentOrnamentBlock extends HorizontalDirectionalBlock {
    public enum Part implements StringRepresentable {
        SINGLE, START, MIDDLE, END, TOP;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    private static final Map<Direction, VoxelShape> SHAPES =
            TreadingVatBlock.rotations(Block.box(0, 2, 13, 16, 16, 16));
    private final boolean stacks;
    private final MapCodec<PedimentOrnamentBlock> codec;

    public PedimentOrnamentBlock(Properties properties, boolean stacks) {
        super(properties);
        this.stacks = stacks;
        this.codec = simpleCodec(p -> new PedimentOrnamentBlock(p, stacks));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, Part.SINGLE));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction face = ctx.getClickedFace();
        Direction facing = face.getAxis().isHorizontal() ? face : ctx.getHorizontalDirection().getOpposite();
        return defaultBlockState().setValue(FACING, facing).setValue(PART, partAt(ctx.getLevel(), ctx.getClickedPos(), facing));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        return state.setValue(PART, partAt(level, pos, state.getValue(FACING)));
    }

    /** 命令与结构放置不经过 getStateForPlacement：放下后按邻居再算一次。 */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        if (!level.isClientSide) {
            BlockState fixed = state.setValue(PART, partAt(level, pos, state.getValue(FACING)));
            if (fixed != state) {
                level.setBlock(pos, fixed, Block.UPDATE_ALL);
            }
        }
    }

    /** 模型 +x 方向是朝向顺时针转 90°（站在正面看是左手边）。 */
    Part partAt(BlockGetter level, BlockPos pos, Direction facing) {
        if (stacks && same(level, pos.below(), facing)) {
            return Part.TOP;
        }
        boolean plus = same(level, pos.relative(facing.getClockWise()), facing);
        boolean minus = same(level, pos.relative(facing.getCounterClockWise()), facing);
        if (plus && minus) {
            return Part.MIDDLE;
        }
        return plus ? Part.START : minus ? Part.END : Part.SINGLE;
    }

    private boolean same(BlockGetter level, BlockPos p, Direction facing) {
        BlockState s = level.getBlockState(p);
        return s.is(this) && s.getValue(FACING) == facing && (!stacks || s.getValue(PART) != Part.TOP);
    }
}
