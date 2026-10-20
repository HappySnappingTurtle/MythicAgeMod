package io.github.happysnappingturtle.mythicage.content.greek.village;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 陶瓮（pithos，规划篇 §4.2）：仓储建筑里的货堆显示位（V1 §5.1），按村庄逻辑库存的存量显示空 / 半 / 满。
 * 存量由 T19 的村庄库存驱动；玩家放置时默认「满」（封口陶盖），可当装饰用。
 */
public class PithosBlock extends Block {
    public enum Fill implements StringRepresentable {
        EMPTY, HALF, FULL;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final EnumProperty<Fill> FILL = EnumProperty.create("fill", Fill.class);
    public static final MapCodec<PithosBlock> CODEC = simpleCodec(PithosBlock::new);
    private static final VoxelShape SHAPE = Block.box(2.5, 0, 2.5, 13.5, 15, 13.5);

    public PithosBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FILL, Fill.FULL));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FILL);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }
}
