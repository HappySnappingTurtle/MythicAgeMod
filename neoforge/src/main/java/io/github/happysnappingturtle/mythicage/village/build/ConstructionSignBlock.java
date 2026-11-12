package io.github.happysnappingturtle.mythicage.village.build;

import io.github.happysnappingturtle.mythicage.content.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.neoforged.neoforge.registries.DeferredBlock;

public final class ConstructionSignBlock extends Block {
    public static final DeferredBlock<ConstructionSignBlock> SIGN = Content.block("construction_sign", null, ConstructionSignBlock::new)
            .properties(() -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(-1, 3600000).noOcclusion())
            .model(BlockModelSpec.manual()).loot(LootSpec.none())
            .lang("施工牌", "Construction Sign", "工事看板").register();
    public static void init() {}
    public ConstructionSignBlock(Properties properties) { super(properties); }
    @Override protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.or(Block.box(7, 0, 7, 9, 10, 9), Block.box(1, 8, 6, 15, 16, 10));
    }
    /** 牌子不挡路：工人到牌子旁取料，挡路会把工地边的窄角堵死（GA-10 实机）。 */
    @Override protected VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server) SiteMarkers.describe(server, pos).ifPresent(message -> player.displayClientMessage(message, false));
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
