package io.github.happysnappingturtle.mythicage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekTemple;
import io.github.happysnappingturtle.mythicage.content.greek.temple.StatueBlock;
import io.github.happysnappingturtle.mythicage.statue.StatueCoreBlock;
import io.github.happysnappingturtle.mythicage.statue.StatueCoreBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;

/**
 * 主神像核心的渲染器（T11a 设计文档 §5.2）：在核心上画出整尊神像（3 格高）。
 * T11a 先用 T8a 的神像模型放大 1.5 倍作为占位；各主神的正式神像模型在 T11b。
 */
public class StatueCoreRenderer implements BlockEntityRenderer<StatueCoreBlockEntity> {
    private static final float SCALE = 1.5f;
    private final BlockRenderDispatcher blocks;

    public StatueCoreRenderer(BlockEntityRendererProvider.Context ctx) {
        this.blocks = ctx.getBlockRenderDispatcher();
    }

    @Override
    public void render(StatueCoreBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light,
                       int overlay) {
        BlockState core = be.getBlockState();
        Direction facing = core.hasProperty(StatueCoreBlock.FACING) ? core.getValue(StatueCoreBlock.FACING) : Direction.NORTH;
        BlockState lower = GreekTemple.GREEK_STATUE.get().defaultBlockState().setValue(StatueBlock.FACING, facing)
                .setValue(StatueBlock.HALF, DoubleBlockHalf.LOWER);
        BlockState upper = lower.setValue(StatueBlock.HALF, DoubleBlockHalf.UPPER);
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.scale(SCALE, SCALE, SCALE);
        pose.translate(-0.5, 0, -0.5);
        blocks.renderSingleBlock(lower, pose, buffers, light, overlay);
        pose.translate(0, 1, 0);
        blocks.renderSingleBlock(upper, pose, buffers, light, overlay);
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(StatueCoreBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(1).expandTowards(0, 3, 0);
    }
}
