package io.github.happysnappingturtle.mythicage.content.greek.knossos;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 线团祭坛（T23 §5.3）：迷宫入口格中心的矮大理石祭坛。右键：背包里没有阿里阿德涅线团就给一个，并从这里重新开始记回程线
 * （「每次进入可取 1 个」，D1、D2）。属于结构，挖不动。
 */
public class ThreadAltarBlock extends Block {
    public static final MapCodec<ThreadAltarBlock> CODEC = simpleCodec(ThreadAltarBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(1, 0, 1, 15, 3, 15), Block.box(3, 3, 3, 13, 10, 13),
            Block.box(2, 10, 2, 14, 12, 14));

    public ThreadAltarBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            take(sp);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /** 取线：已有线团就只提示。 */
    public static boolean take(ServerPlayer player) {
        if (player.getInventory().hasAnyMatching(s -> s.is(GreekKnossos.ARIADNE_THREAD.get()))) {
            player.displayClientMessage(Component.translatable("mythic_age.knossos.thread.have"), true);
            return false;
        }
        ItemStack thread = new ItemStack(GreekKnossos.ARIADNE_THREAD.get());
        if (!player.getInventory().add(thread)) {
            player.drop(thread, false);
        }
        ThreadTrails.restart(player);
        player.displayClientMessage(Component.translatable("mythic_age.knossos.thread.taken"), true);
        return true;
    }
}
