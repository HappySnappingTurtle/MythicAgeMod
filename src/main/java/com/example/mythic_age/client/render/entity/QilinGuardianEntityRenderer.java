package com.example.mythic_age.client.render.entity;

import com.example.mythic_age.MythicAgeMod;
import com.example.mythic_age.client.render.entity.model.ModModelLayers;
import com.example.mythic_age.client.render.entity.model.QilinGuardianModel;
import com.example.mythic_age.entity.QilinGuardianEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class QilinGuardianEntityRenderer extends MobEntityRenderer<QilinGuardianEntity, QilinGuardianModel> {
    private static final Identifier TEXTURE = new Identifier(MythicAgeMod.MOD_ID, "textures/entity/qilin_guardian/qilin_guardian.png");

    public QilinGuardianEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new QilinGuardianModel(context.getPart(ModModelLayers.QILIN_GUARDIAN)), 0.62F);
    }

    @Override
    protected void scale(QilinGuardianEntity entity, MatrixStack matrices, float amount) {
        matrices.scale(1.08F, 1.08F, 1.08F);
    }

    @Override
    public Identifier getTexture(QilinGuardianEntity entity) {
        return TEXTURE;
    }
}
