package io.github.happysnappingturtle.mythicage.client.equipment;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

/**
 * 青铜重装步兵甲的 3D 部件（T17 §8.2，10 号合同 EQ-GR-08）。在原版护甲网格上加：
 * <ul>
 *   <li>头盔：横向马鬃冠（从左耳到右耳），只高出头顶 3u；T 形眼口与护鼻条画在贴图上</li>
 *   <li>胸甲：腰下一圈皮革垂条 pteruges，挂在躯干下沿</li>
 *   <li>护腿：左右小腿正面的青铜胫甲 knemides</li>
 *   <li>靴：踝部小青铜护片；交叉绑带画在贴图上</li>
 * </ul>
 * 贴图 64 × 64：上半部是原版护甲的 UV 布局，下半部放这些部件。外层（头、胸、靴）与内层（护腿）各一个模型，
 * 原版按部位隐藏不相关的部分，子部件跟着父部件一起隐藏。
 */
public final class BronzeArmorModel {
    public static final ModelLayerLocation OUTER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "bronze_armor"), "outer");
    public static final ModelLayerLocation INNER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "bronze_armor"), "inner");

    private static HumanoidModel<LivingEntity> outer;
    private static HumanoidModel<LivingEntity> inner;

    private BronzeArmorModel() {
    }

    public static LayerDefinition outerLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(new CubeDeformation(1.0F), 0);
        PartDefinition root = mesh.getRoot();
        root.getChild("head").addOrReplaceChild("crest", CubeListBuilder.create().texOffs(0, 32)
                .addBox(-5.5F, -12.0F, -1.0F, 11, 3, 2), PartPose.ZERO);
        root.getChild("head").addOrReplaceChild("crest_stand", CubeListBuilder.create().texOffs(28, 32)
                .addBox(-1.0F, -9.5F, -1.5F, 2, 1, 3), PartPose.ZERO);
        root.getChild("body").addOrReplaceChild("pteruges", CubeListBuilder.create().texOffs(0, 40)
                .addBox(-4.0F, 11.0F, -2.0F, 8, 5, 4, new CubeDeformation(1.2F)), PartPose.ZERO);
        for (String leg : new String[]{"right_leg", "left_leg"}) {
            root.getChild(leg).addOrReplaceChild("ankle", CubeListBuilder.create().texOffs(40, 32)
                    .addBox(-1.5F, 9.0F, -3.4F, 3, 2, 1), PartPose.ZERO);
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition innerLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(new CubeDeformation(0.5F), 0);
        PartDefinition root = mesh.getRoot();
        for (String leg : new String[]{"right_leg", "left_leg"}) {
            root.getChild(leg).addOrReplaceChild("greave", CubeListBuilder.create().texOffs(48, 32)
                    .addBox(-2.25F, 5.0F, -3.1F, 4.5F, 6.5F, 1.0F), PartPose.ZERO);
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** 按部位取模型（护腿用内层）。 */
    public static HumanoidModel<LivingEntity> forSlot(EquipmentSlot slot) {
        if (outer == null) {
            var models = Minecraft.getInstance().getEntityModels();
            outer = new HumanoidModel<>(models.bakeLayer(OUTER));
            inner = new HumanoidModel<>(models.bakeLayer(INNER));
        }
        return slot == EquipmentSlot.LEGS ? inner : outer;
    }
}
