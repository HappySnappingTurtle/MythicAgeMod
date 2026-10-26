package io.github.happysnappingturtle.mythicage.content.greek.knossos;

import com.mojang.serialization.MapCodec;
import io.github.happysnappingturtle.mythicage.boss.TrialArenas;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 牛首封门（T23 §5.2）：前室北墙 5 × 5 的门洞由 25 格本方块拼成（{@link #HEAD} 为真的那格画牛首浮雕）。
 * 开关全由竞技场负责（换成空气 / 写回）；右键交给竞技场：IDLE 时要手持阿里阿德涅线团，战中从大厅一侧可开门撤离。
 * 挖不动、炸不掉、推不动；没有掉落。
 */
public class BullGateBlock extends Block {
    public static final BooleanProperty HEAD = BooleanProperty.create("head");
    public static final MapCodec<BullGateBlock> CODEC = simpleCodec(BullGateBlock::new);

    public BullGateBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HEAD, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HEAD);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        use(level, pos, player, stack);
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        use(level, pos, player, ItemStack.EMPTY);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private static void use(Level level, BlockPos pos, Player player, ItemStack stack) {
        if (level instanceof ServerLevel sl && player instanceof ServerPlayer sp) {
            TrialArenas.get(sl.getServer()).byGate(sl, pos)
                    .flatMap(a -> a.useGate(sl, sp, stack))
                    .ifPresent(msg -> sp.displayClientMessage(msg, true));
        }
    }
}
