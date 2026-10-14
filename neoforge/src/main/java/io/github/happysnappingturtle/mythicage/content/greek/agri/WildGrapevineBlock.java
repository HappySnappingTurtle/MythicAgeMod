package io.github.happysnappingturtle.mythicage.content.greek.agri;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 野葡萄藤（T6b 设计文档 §4）：希腊地域的灌木，破坏时掉葡萄插条与葡萄（战利品表），剪刀得到本体。 */
public class WildGrapevineBlock extends BushBlock {
    public static final MapCodec<WildGrapevineBlock> CODEC = simpleCodec(WildGrapevineBlock::new);
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 13, 14);

    public WildGrapevineBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }
}
