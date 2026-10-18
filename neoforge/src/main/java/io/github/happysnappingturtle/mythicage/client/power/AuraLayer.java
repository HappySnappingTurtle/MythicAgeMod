package io.github.happysnappingturtle.mythicage.client.power;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * 外观状态的模型叠层（T13 设计文档 §1.4）。目前一种：青铜之躯——放大后罩在盔甲外的半透明青铜甲片，
 * 加一层向上流动的青铜流光（加色混合，与原版闪电苦力怕的护盾层同一种画法）：前 1 秒亮起，之后保持较低亮度的流光，最后 2 秒褪色。
 */
public class AuraLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    static final ResourceLocation BRONZE = ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID,
            "textures/fx/bronze_swirl.png");
    static final ResourceLocation PLATE = ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID,
            "textures/fx/bronze_plate.png");
    private static final int BRONZE_RGB = 0xC8702A;
    /** 非人形模型的外壳放大比例：水平多、竖直少。 */
    private static final float SHELL_XZ = 1.12f;
    private static final float SHELL_Y = 1.05f;

    /** 人形外壳的模型层：比盔甲外层（外扩 1.0）再大一点，罩住盔甲。 */
    public static final net.minecraft.client.model.geom.ModelLayerLocation SHELL_LAYER =
            new net.minecraft.client.model.geom.ModelLayerLocation(
                    ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "bronze_shell"), "main");

    @javax.annotation.Nullable
    private final net.minecraft.client.model.HumanoidModel<T> shell;

    /**
     * @param shell 人形渲染器传外扩的人形模型（姿态每帧从本体复制）；其它渲染器传 null，按比例放大本体模型
     */
    public AuraLayer(RenderLayerParent<T, M> parent, @javax.annotation.Nullable net.minecraft.client.model.HumanoidModel<T> shell) {
        super(parent);
        this.shell = shell;
    }

    public static net.minecraft.client.model.geom.builders.LayerDefinition createShell() {
        return net.minecraft.client.model.geom.builders.LayerDefinition.create(
                net.minecraft.client.model.HumanoidModel.createMesh(new net.minecraft.client.model.geom.builders.CubeDeformation(1.3f), 0f),
                64, 32);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        ClientAuras.State s = ClientAuras.get(entity.getId(), "bronze");
        if (s == null || Minecraft.getInstance().level == null) {
            return;
        }
        long now = Minecraft.getInstance().level.getGameTime();
        float k = bronzeIntensity(s.age(now) + partialTick, s.left(now) - partialTick);
        if (k <= 0.01f) {
            return;
        }
        float t = ageInTicks * 0.01f;
        int r = (int) ((BRONZE_RGB >> 16 & 255) * k);
        int g = (int) ((BRONZE_RGB >> 8 & 255) * k);
        int b = (int) ((BRONZE_RGB & 255) * k);
        EntityModel<T> model;
        pose.pushPose();
        if (shell != null && getParentModel() instanceof net.minecraft.client.model.HumanoidModel<T> body) {
            // 人形：外扩的人形模型照抄本体姿态，罩在盔甲外
            body.copyPropertiesTo(shell);
            model = shell;
        } else {
            // 其它模型：放大本体（脚底不动；原版模型的原点在脚底上方 24 像素 = 1.5 格）
            pose.translate(0, 1.5 * (1 - SHELL_Y), 0);
            pose.scale(SHELL_XZ, SHELL_Y, SHELL_XZ);
            model = getParentModel();
        }
        // 半透明青铜甲片（不论底下是皮肤还是钻石甲都读得出青铜色）+ 加色的向上流光
        int plateAlpha = (int) (Math.min(1f, k) * 0.62f * 255);
        model.renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucent(PLATE)), light,
                OverlayTexture.NO_OVERLAY, plateAlpha << 24 | 0xFFFFFF);
        model.renderToBuffer(pose, buffers.getBuffer(RenderType.energySwirl(BRONZE, t * 0.5f % 1f, -t % 1f)),
                light, OverlayTexture.NO_OVERLAY, 0xFF000000 | r << 16 | g << 8 | b);
        pose.popPose();
    }

    /** 亮度：0–20 刻从 1 降到 0.6（亮起），之后保持 0.6，最后 40 刻降到 0。颜色偏深，叠加后才是青铜色而不发白。 */
    static float bronzeIntensity(float age, float left) {
        float k = age < 20 ? Mth.lerp(age / 20f, 1f, 0.6f) : 0.6f;
        if (left < 40) {
            k *= Math.max(0, left / 40f);
        }
        return Math.min(1f, k);
    }
}
