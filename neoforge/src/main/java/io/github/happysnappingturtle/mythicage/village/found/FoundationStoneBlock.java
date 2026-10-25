package io.github.happysnappingturtle.mythicage.village.found;

import com.mojang.serialization.MapCodec;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.village.FoundationService;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Supplier;

/**
 * 奠基石（T19 设计文档 §1.13，17 号 §3）：五文明通用的方块类，每个文明注册一种（希腊：城邦奠基石）。
 * 放下时检查限额与间距，放下后给建立者打开选主神界面，选好才立村；只有建立者能拆，拆后进入 3 日宽限。
 * 逻辑都在 {@link FoundationService}。
 */
public class FoundationStoneBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Block.box(0.4, 0, 0.4, 15.6, 5, 15.6);
    private final Civilization civ;
    private final Supplier<BlockEntityType<FoundationStoneBlockEntity>> type;

    public FoundationStoneBlock(Properties p, Civilization civ, Supplier<BlockEntityType<FoundationStoneBlockEntity>> type) {
        super(p);
        this.civ = civ;
        this.type = type;
    }

    public Civilization civ() {
        return civ;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        // 每个文明一个实例（文明与方块实体类型在构造时给定），编解码直接返回这个实例
        return MapCodec.unit(this);
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
        return new FoundationStoneBlockEntity(type.get(), pos, state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        if (ctx.getLevel() instanceof ServerLevel level && ctx.getPlayer() instanceof ServerPlayer player) {
            var error = FoundationService.placementError(player, level, ctx.getClickedPos(), civ);
            if (error.isPresent()) {
                player.displayClientMessage(error.get(), true);
                return null;
            }
        }
        return defaultBlockState();
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (level instanceof ServerLevel server && placer instanceof ServerPlayer player
                && level.getBlockEntity(pos) instanceof FoundationStoneBlockEntity be) {
            FoundationService.onPlaced(server, pos, player, be, civ);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server && player instanceof ServerPlayer sp
                && level.getBlockEntity(pos) instanceof FoundationStoneBlockEntity be) {
            FoundationService.use(server, sp, pos, be, civ);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel server
                && level.getBlockEntity(pos) instanceof FoundationStoneBlockEntity be) {
            FoundationService.onRemoved(server, pos, be);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** 圣火：火星与青烟（只是表现）。 */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 0.5;
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.FLAME, x + (random.nextDouble() - 0.5) * 0.4, pos.getY() + 0.5,
                    z + (random.nextDouble() - 0.5) * 0.4, 0, 0.02, 0);
        }
        if (random.nextInt(5) == 0) {
            level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, pos.getY() + 1.0, z, 0, 0.04, 0);
        }
    }

    @Override
    protected boolean isPathfindable(BlockState state, net.minecraft.world.level.pathfinder.PathComputationType type) {
        return false;
    }
}
