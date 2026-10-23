package io.github.happysnappingturtle.mythicage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.happysnappingturtle.mythicage.relic.RelicItem;
import io.github.happysnappingturtle.mythicage.shrine.ShrineBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.List;

/**
 * 神庙供奉架的陈列（T16 设计文档 §9.2）：架上的圣物按供奉模型（{@code models/relic/<物品名>.json}，真实大小）
 * 摆在神庙顶面。希腊一座神坛只放一件，按 1:1 居中；为允许多件的文明保留了左右排开（scale 由各文明给）。
 * 模型正面朝 +Z；神庙朝向（facing）指向神庙建筑，所以模型正面落在 facing 的反方向，朝着走近神坛的人。静止不转（06 号卡片：缓转默认关闭）。
 */
public class ShrineRelicRenderer<T extends ShrineBlockEntity> implements BlockEntityRenderer<T> {
    /** 顶面高度（奥林匹斯祭坛的顶沿上表面 y = 15/16）。 */
    private final float top;
    private final float scale;
    /** 多件时的左右位置（以模型宽度为单位，乘 scale）。 */
    private static final float[][] SLOTS = {{0}, {-0.3125F, 0.3125F}, {-0.52F, 0, 0.52F}};

    public ShrineRelicRenderer(BlockEntityRendererProvider.Context ctx, float top, float scale) {
        this.top = top;
        this.scale = scale;
    }

    /** 物品 → 供奉小模型的 id（mythic_age:relic/rheia_crown）。 */
    public static ModelResourceLocation modelOf(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "relic/" + id.getPath()));
    }

    /** 登记所有圣物物品的供奉小模型。 */
    public static void registerModels(ModelEvent.RegisterAdditional event) {
        for (var item : BuiltInRegistries.ITEM) {
            if (item instanceof RelicItem) {
                event.register(modelOf(new ItemStack(item)));
            }
        }
    }

    @Override
    public void render(T be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        List<ItemStack> list = be.relics().stacks();
        if (list.isEmpty() || be.getLevel() == null) {
            return;
        }
        BlockState state = be.getBlockState();
        Direction facing = state.hasProperty(HorizontalDirectionalBlock.FACING)
                ? state.getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
        // 方块内部的光照是 0，用顶上那一格的
        int lit = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().above());
        Minecraft mc = Minecraft.getInstance();
        float[] xs = SLOTS[Math.min(list.size(), SLOTS.length) - 1];
        pose.pushPose();
        pose.translate(0.5, top, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(180 - facing.toYRot()));
        for (int i = 0; i < xs.length; i++) {
            BakedModel model = mc.getModelManager().getModel(modelOf(list.get(i)));
            pose.pushPose();
            pose.translate(xs[i] * scale, 0, 0);
            pose.scale(scale, scale, scale);
            pose.translate(-0.5, 0, -0.5);
            mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffers.getBuffer(RenderType.cutout()), null,
                    model, 1, 1, 1, lit, overlay);
            pose.popPose();
        }
        pose.popPose();
    }
}
