package io.github.happysnappingturtle.mythicage.content.greek.village;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/**
 * 踩葡萄池（lenos，规划篇 §4.2）：两格长、有朝向。{@code foot} 是前格（出水口一侧，玩家点的那格，朝向放置者），
 * {@code head} 是后格（沿玩家视线往里一格）。前格为主体（只有前格掉落）；任一格被破坏时另一格一并移除，同原版床。
 * 碰撞是空心的（池底 + 四壁），人能站进池里踩葡萄。
 */
public class TreadingVatBlock extends HorizontalDirectionalBlock {
    public static final EnumProperty<BedPart> PART = BlockStateProperties.BED_PART;
    public static final MapCodec<TreadingVatBlock> CODEC = simpleCodec(TreadingVatBlock::new);

    private static final double WALL = 8.6;
    // 朝北时的形状（模型坐标）：前格北、西、东三面墙 + 出水口；后格南、西、东三面墙
    private static final VoxelShape FRONT_NORTH = Shapes.or(Block.box(0.5, 0, 0.5, 15.5, 1.5, 16),
            Block.box(0.5, 0, 0.5, 15.5, WALL, 2), Block.box(0.5, 0, 0.5, 2, WALL, 16),
            Block.box(14, 0, 0.5, 15.5, WALL, 16));
    private static final VoxelShape BACK_NORTH = Shapes.or(Block.box(0.5, 0, 0, 15.5, 1.5, 15.5),
            Block.box(0.5, 0, 14, 15.5, WALL, 15.5), Block.box(0.5, 0, 0, 2, WALL, 15.5),
            Block.box(14, 0, 0, 15.5, WALL, 15.5));
    private static final Map<Direction, VoxelShape> FRONT = rotations(FRONT_NORTH);
    private static final Map<Direction, VoxelShape> BACK = rotations(BACK_NORTH);

    public TreadingVatBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, BedPart.FOOT));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return (state.getValue(PART) == BedPart.FOOT ? FRONT : BACK).get(state.getValue(FACING));
    }

    /** 另一格的位置：前格往里（背离朝向）一格是后格，反之亦然。 */
    public static BlockPos other(BlockPos pos, BlockState state) {
        Direction back = state.getValue(FACING).getOpposite();
        return pos.relative(state.getValue(PART) == BedPart.FOOT ? back : back.getOpposite());
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction facing = ctx.getHorizontalDirection().getOpposite();
        BlockPos back = ctx.getClickedPos().relative(facing.getOpposite());
        Level level = ctx.getLevel();
        if (!level.getBlockState(back).canBeReplaced(ctx) || !level.getWorldBorder().isWithinBounds(back)) {
            return null;
        }
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide) {
            level.setBlock(other(pos, state), state.setValue(PART, BedPart.HEAD), Block.UPDATE_ALL);
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        Direction toOther = state.getValue(PART) == BedPart.FOOT
                ? state.getValue(FACING).getOpposite() : state.getValue(FACING);
        if (dir == toOther) {
            return neighbor.is(this) && neighbor.getValue(PART) != state.getValue(PART)
                    && neighbor.getValue(FACING) == state.getValue(FACING) ? state : Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, dir, neighbor, level, pos, neighborPos);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // 创造模式挖后格时，前格不掉落（同原版床）
        if (!level.isClientSide && player.isCreative() && state.getValue(PART) == BedPart.HEAD) {
            BlockPos front = other(pos, state);
            BlockState frontState = level.getBlockState(front);
            if (frontState.is(this) && frontState.getValue(PART) == BedPart.FOOT) {
                level.setBlock(front, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                level.levelEvent(player, 2001, front, Block.getId(frontState));
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    /** 朝北的形状转到四个朝向（俯视顺时针，同方块状态的 y 旋转）。 */
    public static Map<Direction, VoxelShape> rotations(VoxelShape north) {
        Map<Direction, VoxelShape> out = new EnumMap<>(Direction.class);
        for (Direction d : Direction.Plane.HORIZONTAL) {
            int turns = (d.get2DDataValue() + 2) % 4; // 南 0、西 1、北 2、东 3 → 北 0、东 1、南 2、西 3
            VoxelShape[] acc = {Shapes.empty()};
            north.forAllBoxes((x0, y0, z0, x1, y1, z1) -> {
                double ax = x0, az = z0, bx = x1, bz = z1;
                for (int i = 0; i < turns; i++) {
                    // (x, z) → (1 - z, x)
                    double nax = 1 - bz, nbx = 1 - az, naz = ax, nbz = bx;
                    ax = nax; bx = nbx; az = naz; bz = nbz;
                }
                acc[0] = Shapes.or(acc[0], Shapes.box(ax, y0, az, bx, y1, bz));
            });
            out.put(d, acc[0].optimize());
        }
        return out;
    }
}
