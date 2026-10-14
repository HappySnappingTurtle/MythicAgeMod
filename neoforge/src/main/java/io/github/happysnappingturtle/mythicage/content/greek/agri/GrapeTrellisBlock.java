package io.github.happysnappingturtle.mythicage.content.greek.agri;

import com.mojang.serialization.MapCodec;
import io.github.happysnappingturtle.mythicage.content.greek.GreekAgriculture;
import io.github.happysnappingturtle.mythicage.growth.MythGrowth;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 葡萄架（T6b 设计文档 §4）：stage 0 为空架子；对着空架子用葡萄插条种下后为 1–4（插条 → 抽藤 → 开花 → 结果）。
 * 成熟后右键采摘，回到 2（藤还在，不用重种）。骨粉按配置的几率推进 1 阶，成熟后不接受（§10.2-2）。
 */
public class GrapeTrellisBlock extends Block implements BonemealableBlock {
    public static final MapCodec<GrapeTrellisBlock> CODEC = simpleCodec(GrapeTrellisBlock::new);
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 4);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    public static final int MATURE = 4;
    private static final VoxelShape SHAPE_X = Block.box(0, 0, 7, 16, 16, 9);
    private static final VoxelShape SHAPE_Z = Block.box(7, 0, 0, 9, 16, 16);

    public GrapeTrellisBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(STAGE, 0).setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE, AXIS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(AXIS, ctx.getHorizontalDirection().getClockWise().getAxis());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(AXIS) == Direction.Axis.X ? SHAPE_X : SHAPE_Z;
    }

    /** 种植要求下方为泥土类方块或耕地。 */
    public static boolean plantableOn(BlockState below) {
        return below.is(BlockTags.DIRT) || below.is(Blocks.FARMLAND);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState b = level.getBlockState(below);
        return state.getValue(STAGE) == 0 ? b.isFaceSturdy(level, below, Direction.UP) : plantableOn(b);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        return dir == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, dir, neighbor, level, pos, neighborPos);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(GreekAgriculture.GRAPE_CUTTING.get()) || state.getValue(STAGE) != 0) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!plantableOn(level.getBlockState(pos.below()))) {
            return ItemInteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(STAGE, 1), 2);
            level.playSound(null, pos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0f, 1.0f);
            stack.consume(1, player);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (state.getValue(STAGE) != MATURE) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            harvest(level, pos, state);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** 采摘：掉 2–3 串葡萄，回到「抽藤」阶段。供玩家与村民（T19）共用。 */
    public static void harvest(Level level, BlockPos pos, BlockState state) {
        popResource(level, pos, new ItemStack(GreekAgriculture.GRAPE.get(), 2 + level.random.nextInt(2)));
        level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0f, 1.0f);
        level.setBlock(pos, state.setValue(STAGE, 2), 2);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        int s = state.getValue(STAGE);
        return s > 0 && s < MATURE;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // 生长加速类模组会直接调用 randomTick，不经过 isRandomlyTicking，这里必须自己再检查一次
        if (isRandomlyTicking(state) && MythGrowth.rollRandomTick(random, MythGrowth.config().grapeStageTicks())) {
            MythGrowth.tryAdvance(level, pos, state, state.setValue(STAGE, state.getValue(STAGE) + 1),
                    MythGrowth.Source.RANDOM_TICK);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return isRandomlyTicking(state);
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return random.nextDouble() < MythGrowth.config().boneMeal().grapeChance();
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        MythGrowth.tryAdvance(level, pos, state, state.setValue(STAGE, state.getValue(STAGE) + 1),
                MythGrowth.Source.BONE_MEAL);
    }
}
