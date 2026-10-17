package io.github.happysnappingturtle.mythicage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekTemple;
import io.github.happysnappingturtle.mythicage.content.greek.temple.StatueBlock;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.statue.StatueCoreBlock;
import io.github.happysnappingturtle.mythicage.statue.StatueCoreBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 主神像核心的渲染器（T11a 设计文档 §5.2；T11b §6）：有该主神的神像模型
 * （{@code geo/block/statue_<主神>.geo.json} + {@code textures/statue/<主神>.png}）时用 GeckoLib 画整尊神像，
 * 朝向随核心方块；没有时退回 T11a 的占位（T8a 神像放大 1.5 倍）。
 */
public class StatueCoreRenderer implements BlockEntityRenderer<StatueCoreBlockEntity> {
    private static final float SCALE = 1.5f;
    private final BlockRenderDispatcher blocks;
    private final GeoBlockRenderer<StatueCoreBlockEntity> geo = new GeoBlockRenderer<>(new Model());
    /** 主神 → 是否有神像模型（资源重载后客户端会重建渲染器，缓存随之清空）。 */
    private final Map<String, Boolean> hasModel = new ConcurrentHashMap<>();

    public StatueCoreRenderer(BlockEntityRendererProvider.Context ctx) {
        this.blocks = ctx.getBlockRenderDispatcher();
    }

    static ResourceLocation geoOf(String patron) {
        return ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "geo/block/statue_" + patron + ".geo.json");
    }

    static ResourceLocation textureOf(String patron) {
        return ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "textures/statue/" + patron + ".png");
    }

    private boolean modelFor(String patron) {
        return !patron.isEmpty() && hasModel.computeIfAbsent(patron, p -> {
            var rm = Minecraft.getInstance().getResourceManager();
            return rm.getResource(geoOf(p)).isPresent() && rm.getResource(textureOf(p)).isPresent();
        });
    }

    @Override
    public void render(StatueCoreBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light,
                       int overlay) {
        if (modelFor(be.patron())) {
            geo.render(be, partialTick, pose, buffers, light, overlay);
            return;
        }
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
        return new AABB(be.getBlockPos()).inflate(2).expandTowards(0, 4, 0);
    }

    /** 按核心记录的主神取资源；没有动画（共用一个空动画文件）。 */
    private static final class Model extends GeoModel<StatueCoreBlockEntity> {
        private static final ResourceLocation ANIMATION =
                ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "animations/block/statue.animation.json");

        @Override
        public ResourceLocation getModelResource(StatueCoreBlockEntity be) {
            return geoOf(be.patron());
        }

        @Override
        public ResourceLocation getTextureResource(StatueCoreBlockEntity be) {
            return textureOf(be.patron());
        }

        @Override
        public ResourceLocation getAnimationResource(StatueCoreBlockEntity be) {
            return ANIMATION;
        }
    }
}
