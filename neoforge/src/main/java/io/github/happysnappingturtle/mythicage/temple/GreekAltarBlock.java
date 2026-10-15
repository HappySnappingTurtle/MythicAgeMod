package io.github.happysnappingturtle.mythicage.temple;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 奥林匹斯祭坛 Bomos（T8a 设计文档 §2.1、修订 01）：露天立在神庙正门前，朝向指向神庙。
 * 放置时绑定主人并检查数量上限与间距；右键检视神庙；
 * 移除时注销登记。没有玩家的放置不成立（由 {@link TempleService} 在下一刻移除）。
 */
public class GreekAltarBlock extends BaseEntityBlock {
    public static final MapCodec<GreekAltarBlock> CODEC = simpleCodec(GreekAltarBlock::new);
    /** 神坛朝向：指向神庙（修订 01 §2.1）。放置时取放置者的水平朝向。 */
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING =
            net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING;
    /** 台基 + 台身 + 顶沿。 */
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 3, 16), Block.box(1, 3, 1, 15, 12, 15),
            Block.box(0, 12, 0, 16, 15, 16));

    public GreekAltarBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(
            net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GreekAltarBlockEntity(pos, state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        if (ctx.getLevel() instanceof ServerLevel level && ctx.getPlayer() instanceof ServerPlayer player) {
            var error = TempleService.placementError(player, level, ctx.getClickedPos(), ctx.getHorizontalDirection());
            if (error.isPresent()) {
                player.displayClientMessage(error.get(), true);
                return null;
            }
        }
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (level instanceof ServerLevel server && placer instanceof ServerPlayer player
                && level.getBlockEntity(pos) instanceof GreekAltarBlockEntity be) {
            TempleService.onPlaced(server, pos, player, be);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level instanceof ServerLevel server && player instanceof ServerPlayer sp) {
            TempleService.inspect(sp, server, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            TempleService.onRemoved(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // 顶面火槽里的余烬：只是表现，与神庙等级无关（客户端不知道等级）
        if (random.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.3,
                    pos.getY() + 0.95, pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.3, 0, 0.02, 0);
        }
    }
}
