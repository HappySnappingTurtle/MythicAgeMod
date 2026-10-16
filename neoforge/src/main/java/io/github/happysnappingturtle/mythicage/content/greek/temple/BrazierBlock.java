package io.github.happysnappingturtle.mythicage.content.greek.temple;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 基础圣火盆 Escharon（T8a 设计文档 §2.2）：放下时熄灭；打火石、火焰弹点燃，铲子或水桶熄灭。
 * 点燃时亮度 15（由方块属性给出）、有火焰粒子，不伤害实体。神庙判定只计点燃的圣火盆。
 */
public class BrazierBlock extends Block {
    public static final MapCodec<BrazierBlock> CODEC = simpleCodec(BrazierBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    /** 四腿木架 + 火盆。 */
    private static final VoxelShape SHAPE = Shapes.or(Block.box(3, 0, 3, 13, 10, 13), Block.box(2, 10, 2, 14, 14, 14));

    public BrazierBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        boolean lit = state.getValue(LIT);
        if (!lit && (stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE))) {
            setLit(level, pos, state, player, true);
            if (stack.is(Items.FLINT_AND_STEEL)) {
                stack.hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(hand));
                level.playSound(player, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            } else {
                stack.consume(1, player);
                level.playSound(player, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (lit && (stack.is(ItemTags.SHOVELS) || stack.is(Items.WATER_BUCKET))) {
            setLit(level, pos, state, player, false);
            level.playSound(player, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 1.0F);
            if (stack.is(ItemTags.SHOVELS)) {
                stack.hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(hand));
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private static void setLit(Level level, BlockPos pos, BlockState state, Player player, boolean lit) {
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(LIT, lit), Block.UPDATE_ALL_IMMEDIATE);
            level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 0.5;
        double y = pos.getY() + 0.9;
        for (int i = 0; i < 2; i++) {
            level.addParticle(ParticleTypes.FLAME, x + (random.nextDouble() - 0.5) * 0.4, y,
                    z + (random.nextDouble() - 0.5) * 0.4, 0, 0.01, 0);
        }
        if (random.nextFloat() < 0.25f) {
            level.addParticle(ParticleTypes.SMOKE, x, y + 0.3, z, 0, 0.03, 0);
        }
        if (random.nextInt(24) == 0) {
            level.playLocalSound(x, y, z, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.6F, 1.0F, false);
        }
    }

    /** 实心但不是整格：原版默认当作「可走进去」，生物寻路会穿过它而卡住（T10 信徒绕神坛时发现）。 */
    @Override
    protected boolean isPathfindable(BlockState state, net.minecraft.world.level.pathfinder.PathComputationType type) {
        return false;
    }
}
