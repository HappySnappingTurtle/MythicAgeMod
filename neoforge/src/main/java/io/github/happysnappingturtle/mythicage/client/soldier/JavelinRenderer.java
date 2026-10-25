package io.github.happysnappingturtle.mythicage.client.soldier;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.happysnappingturtle.mythicage.content.greek.village.soldier.JavelinEntity;
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
 * 飞行中、插在地上的投矛（T20 阶段 ③）：细木杆（长 1.4 格、粗 0.07 格）+ 矛头（投矛是青铜、粗投矛是燧石），沿飞行方向摆放。
 * 落地后保持最后的朝向（矛尖扎在地里）。
 */
public class JavelinRenderer extends EntityRenderer<JavelinEntity> {
    private static final BlockState SHAFT = Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
    private static final BlockState BRONZE = Blocks.WAXED_CUT_COPPER.defaultBlockState();
    private static final BlockState FLINT = Blocks.DEEPSLATE.defaultBlockState();
    private final BlockRenderDispatcher blocks;

    public JavelinRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.blocks = ctx.getBlockRenderDispatcher();
        this.shadowRadius = 0.1F;
    }

    @Override
    public void render(JavelinEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, e.yRotO, e.getYRot())));
        pose.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(partialTick, e.xRotO, e.getXRot())));
        // 实体位置在矛的中段偏前：杆从 −0.8 到 +0.5，矛头在 +0.5 到 +0.68
        block(pose, buffers, light, SHAFT, 0.07F, 0.07F, 1.3F, 0, 0, -0.15F);
        boolean rough = e.ammoItem().endsWith("rough_javelin");
        block(pose, buffers, light, rough ? FLINT : BRONZE, 0.1F, 0.1F, 0.18F, 0, 0, 0.59F);
        block(pose, buffers, light, rough ? FLINT : BRONZE, 0.05F, 0.05F, 0.08F, 0, 0, 0.71F);
        pose.popPose();
        super.render(e, yaw, partialTick, pose, buffers, light);
    }

    private void block(PoseStack pose, MultiBufferSource buffers, int light, BlockState state, float sx, float sy, float sz,
                       float cx, float cy, float cz) {
        pose.pushPose();
        pose.translate(cx - sx / 2, cy - sy / 2, cz - sz / 2);
        pose.scale(sx, sy, sz);
        blocks.renderSingleBlock(state, pose, buffers, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(JavelinEntity e) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
