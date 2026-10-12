package com.example.mythic_age.client.render.entity;

import com.example.mythic_age.MythicAgeMod;
import com.example.mythic_age.client.render.entity.model.DwarfBerserkerModel;
import com.example.mythic_age.client.render.entity.model.ModModelLayers;
import com.example.mythic_age.entity.DwarfBerserkerEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class DwarfBerserkerEntityRenderer extends MobEntityRenderer<DwarfBerserkerEntity, DwarfBerserkerModel> {
    private static final Identifier TEXTURE = new Identifier(MythicAgeMod.MOD_ID, "textures/entity/dwarf_berserker/dwarf_berserker.png");

    public DwarfBerserkerEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new DwarfBerserkerModel(context.getPart(ModModelLayers.DWARF_BERSERKER)), 0.48F);
    }

    @Override
    protected void scale(DwarfBerserkerEntity entity, MatrixStack matrices, float amount) {
        matrices.scale(1.02F, 1.02F, 1.02F);
    }

    @Override
    public Identifier getTexture(DwarfBerserkerEntity entity) {
        return TEXTURE;
    }
}
