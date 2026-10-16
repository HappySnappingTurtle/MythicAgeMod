package io.github.happysnappingturtle.mythicage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;

/**
 * 希腊村庄村民头戴的橄榄枝饰（T10 设计文档 §1：表示信仰）。四片带透明缺口的叶带围在额头一圈，
 * 跟随头部（含跪拜时的低头）。只看客户端收到的信徒状态。
 */
public final class BelieverWreathLayer extends RenderLayer<Villager, VillagerModel<Villager>> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "villager"), "olive_wreath");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "textures/entity/villager/olive_wreath.png");
    /** 叶带离头部中心的距离：头半宽 4，帽子层外扩 0.51，再留一点空隙。 */
    private static final float RING = 4.75F;
    /** 叶带上沿高度（头顶在 −10）。 */
    private static final float TOP = -9.0F;

    private final ModelPart wreath;

    public BelieverWreathLayer(RenderLayerParent<Villager, VillagerModel<Villager>> parent, EntityModelSet models) {
        super(parent);
        this.wreath = models.bakeLayer(LAYER);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // 零厚度的面：正面取 (0,0) 10×4，背面取 (10,0) 10×4
        CubeListBuilder band = CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, 0.0F, 0.0F, 10.0F, 4.0F, 0.0F);
        float half = (float) (Math.PI / 2);
        root.addOrReplaceChild("front", band, PartPose.offset(0.0F, TOP, -RING));
        root.addOrReplaceChild("back", band, PartPose.offsetAndRotation(0.0F, TOP, RING, 0.0F, (float) Math.PI, 0.0F));
        root.addOrReplaceChild("left", band, PartPose.offsetAndRotation(RING, TOP, 0.0F, 0.0F, -half, 0.0F));
        root.addOrReplaceChild("right", band, PartPose.offsetAndRotation(-RING, TOP, 0.0F, 0.0F, half, 0.0F));
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, Villager villager, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (villager.isInvisible() || !ClientBelievers.believer(villager) || !getParentModel().getHead().visible) {
            return;
        }
        poseStack.pushPose();
        getParentModel().getHead().translateAndRotate(poseStack);
        wreath.render(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), packedLight,
                OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}
