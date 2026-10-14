package io.github.happysnappingturtle.mythicage.content.greek.agri;

import com.mojang.serialization.MapCodec;
import io.github.happysnappingturtle.mythicage.content.greek.GreekAgriculture;
import io.github.happysnappingturtle.mythicage.growth.MythGrowth;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 橄榄树叶（T6b 设计文档 §3.1）：在原版树叶上增加结果状态 fruit（0 无果、1 青果、2 熟果）。
 * 只有自然树叶（非玩家放置）会结果；右键熟果采摘，树叶不破坏；默认不接受骨粉（§10.2-2）。
 */
public class OliveLeavesBlock extends LeavesBlock implements BonemealableBlock {
    public static final MapCodec<OliveLeavesBlock> CODEC = simpleCodec(OliveLeavesBlock::new);
    public static final IntegerProperty FRUIT = IntegerProperty.create("fruit", 0, 2);
    public static final int RIPE = 2;

    public OliveLeavesBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FRUIT, 0));
    }

    @Override
    public MapCodec<? extends LeavesBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FRUIT);
    }

    static boolean canFruit(BlockState state) {
        return !state.getValue(PERSISTENT) && state.getValue(FRUIT) < RIPE;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return super.isRandomlyTicking(state) || canFruit(state);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.randomTick(state, level, pos, random);
        BlockState now = level.getBlockState(pos);
        if (now.is(this) && canFruit(now)
                && MythGrowth.rollRandomTick(random, MythGrowth.config().oliveFruitTicks() / RIPE)) {
            MythGrowth.tryAdvance(level, pos, now, now.setValue(FRUIT, now.getValue(FRUIT) + 1),
                    MythGrowth.Source.RANDOM_TICK);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (state.getValue(FRUIT) != RIPE) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            popResource(level, pos, new ItemStack(GreekAgriculture.OLIVE.get(), 1 + level.random.nextInt(2)));
            level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0f,
                    0.8f + level.random.nextFloat() * 0.4f);
            level.setBlock(pos, state.setValue(FRUIT, 0), 2);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return MythGrowth.config().boneMeal().oliveLeaves() && canFruit(state);
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        MythGrowth.tryAdvance(level, pos, state, state.setValue(FRUIT, state.getValue(FRUIT) + 1),
                MythGrowth.Source.BONE_MEAL);
    }
}
