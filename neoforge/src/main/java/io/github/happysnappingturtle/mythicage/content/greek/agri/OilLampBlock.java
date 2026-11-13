package io.github.happysnappingturtle.mythicage.content.greek.agri;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 希腊陶油灯（T6b 设计文档 §6）：扁平小灯，亮度 12（由方块属性给出），放在方块顶面；
 * 灯嘴处有蜡烛同频率的火焰粒子。不计为圣火盆。
 */
public class OilLampBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<OilLampBlock> CODEC = simpleCodec(OilLampBlock::new);
    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 3, 12);

    public OilLampBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    /**
     * 下方中心有支撑，或下面是陶瓮（陶瓮高 15 像素，顶面不算整面支撑；村里的灯常放在陶瓮上，
     * 依附表 architecture_attach.json 也把陶瓮列为灯的支撑——GA-11：村民施工放灯要过这道检查）。
     */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return canSupportCenter(level, pos.below(), Direction.UP)
                || level.getBlockState(pos.below()).getBlock() instanceof io.github.happysnappingturtle.mythicage.content.greek.village.PithosBlock;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        return dir == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, dir, neighbor, level, pos, neighborPos);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // 灯嘴在朝向一侧 3 像素处
        Direction f = state.getValue(FACING);
        double x = pos.getX() + 0.5 + f.getStepX() * 0.25;
        double z = pos.getZ() + 0.5 + f.getStepZ() * 0.25;
        double y = pos.getY() + 0.28;
        if (random.nextFloat() < 0.3f) {
            level.addParticle(ParticleTypes.SMALL_FLAME, x, y, z, 0, 0, 0);
        }
        if (random.nextFloat() < 0.08f) {
            level.addParticle(ParticleTypes.SMOKE, x, y + 0.05, z, 0, 0, 0);
        }
    }
}
