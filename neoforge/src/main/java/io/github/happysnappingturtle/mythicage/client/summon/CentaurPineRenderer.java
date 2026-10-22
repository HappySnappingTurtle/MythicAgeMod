package io.github.happysnappingturtle.mythicage.client.summon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.happysnappingturtle.mythicage.summon.greek.CentaurPineEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 掷出的松树干（T14 §11.5）：三段云杉原木首尾相接（粗 0.5 格、长 1.5 格），尾端一团带根的泥土、前端一簇云杉叶；
 * 沿飞行方向摆放，并绕横轴翻滚。
 */
public class CentaurPineRenderer extends EntityRenderer<CentaurPineEntity> {
    private static final BlockState LOG = Blocks.SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
    private static final BlockState ROOTS = Blocks.ROOTED_DIRT.defaultBlockState();
    private static final BlockState NEEDLES = Blocks.SPRUCE_LEAVES.defaultBlockState();
    private final BlockRenderDispatcher blocks;

    public CentaurPineRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.blocks = ctx.getBlockRenderDispatcher();
        this.shadowRadius = 0.3F;
    }

    @Override
    public void render(CentaurPineEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, e.yRotO, e.getYRot())));
        pose.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(partialTick, e.xRotO, e.getXRot())));
        // 翻滚：每刻转 30°
        pose.mulPose(Axis.XP.rotationDegrees((e.age() + partialTick) * 30.0F));
        for (int i = -1; i <= 1; i++) {
            block(pose, buffers, light, LOG, 0.5F, 0.5F, 0.5F, 0, 0, i * 0.5F);
        }
        block(pose, buffers, light, ROOTS, 0.7F, 0.7F, 0.5F, 0, 0, -1.0F);
        block(pose, buffers, light, NEEDLES, 0.9F, 0.9F, 0.6F, 0, 0, 1.05F);
        pose.popPose();
        super.render(e, yaw, partialTick, pose, buffers, light);
    }

    /** 画一个缩放过的方块，中心在 (cx, cy, cz)。 */
    private void block(PoseStack pose, MultiBufferSource buffers, int light, BlockState state, float sx, float sy, float sz,
                       float cx, float cy, float cz) {
        pose.pushPose();
        pose.translate(cx - sx / 2, cy - sy / 2, cz - sz / 2);
        pose.scale(sx, sy, sz);
        blocks.renderSingleBlock(state, pose, buffers, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(CentaurPineEntity e) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
