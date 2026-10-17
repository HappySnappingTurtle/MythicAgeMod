package io.github.happysnappingturtle.mythicage.statue;

import com.mojang.serialization.MapCodec;
import io.github.happysnappingturtle.mythicage.trade.TradeService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 主神像核心（T11a 设计文档 §5.2）：站在底座上、神像脚下的一格；整尊神像由它的方块实体渲染器画出。
 * 右键打开兑换（只对主人，且神像完整）；主人拆掉核心 → 整座神像的部件消失、道具返还。
 */
public class StatueCoreBlock extends BaseEntityBlock {
    public static final MapCodec<StatueCoreBlock> CODEC = simpleCodec(StatueCoreBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public StatueCoreBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.block();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StatueCoreBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server && player instanceof ServerPlayer sp) {
            open(server, pos, sp);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    static void open(ServerLevel level, BlockPos core, ServerPlayer player) {
        if (!(level.getBlockEntity(core) instanceof StatueCoreBlockEntity be)) {
            return;
        }
        if (!be.owner().map(player.getUUID()::equals).orElse(false)) {
            player.displayClientMessage(Component.translatable("mythic_age.statue.not_owner", be.ownerName()), true);
            return;
        }
        PatronStatues.check(level, be);
        if (!be.intact()) {
            player.displayClientMessage(Component.translatable("mythic_age.statue.damaged", be.missing().size()), true);
            PatronStatues.showMissing(player, be);
            return;
        }
        TradeService.openStatue(player, core);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel server
                && level.getBlockEntity(pos) instanceof StatueCoreBlockEntity be) {
            PatronStatues.onCoreRemoved(server, pos, be);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}
