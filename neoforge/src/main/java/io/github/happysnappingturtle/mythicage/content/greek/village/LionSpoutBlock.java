package io.github.happysnappingturtle.mythicage.content.greek.village;

import io.github.happysnappingturtle.mythicage.content.greek.architecture.FacingShapedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 狮头出水口（泉屋、海神泉、浴室，规划篇 §4.2）：整块大理石墙面，正面伸出狮头；狮口不断往下淌水（只有粒子，不产生水方块）。
 */
public class LionSpoutBlock extends FacingShapedBlock {
    /** 狮口在模型里的位置：正面外 4.2 像素、离地 6 像素。 */
    private static final double MOUTH_OUT = 0.5 + 4.2 / 16.0;
    private static final double MOUTH_Y = 6.0 / 16.0;

    public LionSpoutBlock(Properties properties) {
        super(properties, false, Block.box(0, 0, 0, 16, 16, 16));
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Direction f = state.getValue(FACING);
        double x = pos.getX() + 0.5 + f.getStepX() * MOUTH_OUT + (random.nextDouble() - 0.5) * 0.08;
        double z = pos.getZ() + 0.5 + f.getStepZ() * MOUTH_OUT + (random.nextDouble() - 0.5) * 0.08;
        double y = pos.getY() + MOUTH_Y;
        for (int i = 0; i < 2; i++) {
            level.addParticle(ParticleTypes.FALLING_WATER, x, y - i * 0.1, z, 0, 0, 0);
        }
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.SPLASH, x, y - 0.2, z, f.getStepX() * 0.05, 0.05, f.getStepZ() * 0.05);
        }
    }
}
