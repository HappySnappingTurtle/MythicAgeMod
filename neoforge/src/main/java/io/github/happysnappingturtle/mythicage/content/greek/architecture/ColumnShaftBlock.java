package io.github.happysnappingturtle.mythicage.content.greek.architecture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Locale;

/**
 * 大理石柱身（T8b 设计文档 §2.2）：按上下相邻的柱身自动切换 part（单段 / 底 / 中 / 顶），顶段更细，表现收分。
 * 柱础、柱头不算柱身，不影响 part。截面 12×12，顶面不是完整面（不能当地面、不算屋顶）。
 */
public class ColumnShaftBlock extends Block {
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 16, 14);

    public enum Part implements StringRepresentable {
        SINGLE, BOTTOM, MIDDLE, TOP;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        static Part of(boolean below, boolean above) {
            if (below && above) {
                return MIDDLE;
            }
            if (above) {
                return BOTTOM;
            }
            return below ? TOP : SINGLE;
        }
    }

    public ColumnShaftBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PART, Part.SINGLE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockPos p = ctx.getClickedPos();
        return defaultBlockState().setValue(PART, Part.of(isShaft(ctx.getLevel(), p.below()), isShaft(ctx.getLevel(), p.above())));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        if (dir.getAxis() == Direction.Axis.Y) {
            return state.setValue(PART, Part.of(isShaft(level, pos.below()), isShaft(level, pos.above())));
        }
        return state;
    }

    private boolean isShaft(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).is(this);
    }
}
