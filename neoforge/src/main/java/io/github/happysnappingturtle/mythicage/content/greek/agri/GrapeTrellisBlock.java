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
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
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
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 葡萄架（T6b 设计文档 §4，修订 01）：门式的三格整体（part = bottom / middle / top）。
 * stage 0 为空架子；对着空架子用葡萄插条种下后为 1–4（插条 → 抽藤 → 开花 → 结果）。
 * 成熟后右键采摘，回到 2（藤还在，不用重种）。骨粉按配置的几率推进 1 阶，成熟后不接受（§10.2-2）。
 * stage 与 axis 以底格为准：只改底格，中格、顶格在 updateShape 中从下方一格复制；种植、采摘、骨粉对任意一格都作用到底格。
 */
public class GrapeTrellisBlock extends Block implements BonemealableBlock {
    public static final MapCodec<GrapeTrellisBlock> CODEC = simpleCodec(GrapeTrellisBlock::new);
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 4);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    public static final int MATURE = 4;
    private static final VoxelShape SHAPE_X = Block.box(0, 0, 7, 16, 16, 9);
    private static final VoxelShape SHAPE_Z = Block.box(7, 0, 0, 9, 16, 16);

    public GrapeTrellisBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(STAGE, 0).setValue(AXIS, Direction.Axis.X)
                .setValue(PART, Part.BOTTOM));
    }

    /** 三格中的位置；offset 为距底格的格数。 */
    public enum Part implements StringRepresentable {
        BOTTOM("bottom", 0), MIDDLE("middle", 1), TOP("top", 2);

        private final String name;
        public final int offset;

        Part(String name, int offset) {
            this.name = name;
            this.offset = offset;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    /** 任意一格对应的底格位置。 */
    public static BlockPos bottomOf(BlockPos pos, BlockState state) {
        return pos.below(state.getValue(PART).offset);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE, AXIS, PART);
    }

    /** 放在底格；上方两格必须可替换且不超出建筑高度，否则放不下。 */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockPos pos = ctx.getClickedPos();
        Level level = ctx.getLevel();
        if (level.isOutsideBuildHeight(pos.above(2)) || !level.getBlockState(pos.above()).canBeReplaced(ctx)
                || !level.getBlockState(pos.above(2)).canBeReplaced(ctx)) {
            return null;
        }
        return defaultBlockState().setValue(AXIS, ctx.getHorizontalDirection().getClockWise().getAxis());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(PART, Part.MIDDLE), 3);
        level.setBlock(pos.above(2), state.setValue(PART, Part.TOP), 3);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(AXIS) == Direction.Axis.X ? SHAPE_X : SHAPE_Z;
    }

    /** 种植要求下方为泥土类方块或耕地。 */
    public static boolean plantableOn(BlockState below) {
        return below.is(BlockTags.DIRT) || below.is(Blocks.FARMLAND);
    }

    /** neighbor 是否为本方块的指定一格。 */
    private boolean isPart(BlockState neighbor, Part part) {
        return neighbor.is(this) && neighbor.getValue(PART) == part;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState b = level.getBlockState(below);
        return switch (state.getValue(PART)) {
            case BOTTOM -> state.getValue(STAGE) == 0 ? b.isFaceSturdy(level, below, Direction.UP) : plantableOn(b);
            case MIDDLE -> isPart(b, Part.BOTTOM);
            case TOP -> isPart(b, Part.MIDDLE);
        };
    }

    /** 任何一格缺失，其余格子随之变为空气（由底格在连锁中掉落）；中格、顶格从下方一格同步 stage 与 axis。 */
    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        Part part = state.getValue(PART);
        if (dir == Direction.DOWN) {
            if (!canSurvive(state, level, pos)) {
                return Blocks.AIR.defaultBlockState();
            }
            if (part != Part.BOTTOM) {
                return state.setValue(STAGE, neighbor.getValue(STAGE)).setValue(AXIS, neighbor.getValue(AXIS));
            }
        } else if (dir == Direction.UP && part != Part.TOP
                && !isPart(neighbor, part == Part.BOTTOM ? Part.MIDDLE : Part.TOP)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, dir, neighbor, level, pos, neighborPos);
    }

    /** 创造模式破坏中格或顶格时先静默清掉底格，避免连锁掉落（同原版 DoublePlantBlock.preventDropFromBottomPart）。 */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.isCreative() && state.getValue(PART) != Part.BOTTOM) {
            BlockPos bottom = bottomOf(pos, state);
            BlockState b = level.getBlockState(bottom);
            if (isPart(b, Part.BOTTOM)) {
                level.setBlock(bottom, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                level.levelEvent(player, 2001, bottom, Block.getId(b));
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(GreekAgriculture.GRAPE_CUTTING.get()) || state.getValue(STAGE) != 0) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        BlockPos bottom = bottomOf(pos, state);
        BlockState b = level.getBlockState(bottom);
        if (!isPart(b, Part.BOTTOM) || !plantableOn(level.getBlockState(bottom.below()))) {
            return ItemInteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            level.setBlock(bottom, b.setValue(STAGE, 1), 2);
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

    /** 采摘：在 pos（任意一格）弹出 2–3 串葡萄，整架回到「抽藤」阶段。供玩家与村民（T19）共用。 */
    public static void harvest(Level level, BlockPos pos, BlockState state) {
        BlockPos bottom = bottomOf(pos, state);
        BlockState b = level.getBlockState(bottom);
        if (!(b.getBlock() instanceof GrapeTrellisBlock) || b.getValue(STAGE) != MATURE) {
            return;
        }
        popResource(level, pos, new ItemStack(GreekAgriculture.GRAPE.get(), 2 + level.random.nextInt(2)));
        level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0f, 1.0f);
        level.setBlock(bottom, b.setValue(STAGE, 2), 2);
    }

    private static boolean growing(BlockState state) {
        int s = state.getValue(STAGE);
        return s > 0 && s < MATURE;
    }

    /** 只有底格接受随机刻。 */
    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(PART) == Part.BOTTOM && growing(state);
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
        return growing(state);
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return random.nextDouble() < MythGrowth.config().boneMeal().grapeChance();
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        BlockPos bottom = bottomOf(pos, state);
        BlockState b = level.getBlockState(bottom);
        if (isPart(b, Part.BOTTOM) && growing(b)) {
            MythGrowth.tryAdvance(level, bottom, b, b.setValue(STAGE, b.getValue(STAGE) + 1), MythGrowth.Source.BONE_MEAL);
        }
    }
}
