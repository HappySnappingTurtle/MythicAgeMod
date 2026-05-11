package com.example.mythic_age.client.render;

import com.example.mythic_age.entity.MeteorEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/**
 * 陨石实体渲染器
 * 负责渲染下落的陨石实体
 */
public class MeteorEntityRenderer extends EntityRenderer<MeteorEntity> {
    private static final Identifier TEXTURE = new Identifier("mythic_age", "textures/entity/meteor.png");
    private static final RenderLayer LAYER = RenderLayer.getEntityCutoutNoCull(TEXTURE);
    
    public MeteorEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }
    
    @Override
    public void render(MeteorEntity entity, float yaw, float tickDelta, MatrixStack matrices, 
                      VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();
        
        // 旋转陨石以创建动态效果
        float rotation = (entity.age + tickDelta) * 10.0f;
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotation * 0.7f));
        
        // 缩放陨石
        float scale = 1.5f;
        matrices.scale(scale, scale, scale);
        
        // 渲染陨石立方体
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(LAYER);
        
        // 简单的立方体渲染
        renderCube(matrices, vertexConsumer, light);
        
        matrices.pop();
        
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }
    
    private void renderCube(MatrixStack matrices, VertexConsumer vertexConsumer, int light) {
        MatrixStack.Entry entry = matrices.peek();
        
        // 渲染一个完整的发光立方体
        float size = 0.5f;
        
        // 前面 (Z+)
        vertexConsumer.vertex(entry.getPositionMatrix(), -size, -size, size)
            .color(255, 100, 0, 255)
            .texture(0, 1)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(entry.getNormalMatrix(), 0, 0, 1)
            .next();
        vertexConsumer.vertex(entry.getPositionMatrix(), size, -size, size)
            .color(255, 100, 0, 255)
            .texture(1, 1)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(entry.getNormalMatrix(), 0, 0, 1)
            .next();
        vertexConsumer.vertex(entry.getPositionMatrix(), size, size, size)
            .color(255, 150, 50, 255)
            .texture(1, 0)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(entry.getNormalMatrix(), 0, 0, 1)
            .next();
        vertexConsumer.vertex(entry.getPositionMatrix(), -size, size, size)
            .color(255, 150, 50, 255)
            .texture(0, 0)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(entry.getNormalMatrix(), 0, 0, 1)
            .next();
            
        // 后面 (Z-)
        vertexConsumer.vertex(entry.getPositionMatrix(), size, -size, -size)
            .color(255, 100, 0, 255)
            .texture(0, 1)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(entry.getNormalMatrix(), 0, 0, -1)
            .next();
        vertexConsumer.vertex(entry.getPositionMatrix(), -size, -size, -size)
            .color(255, 100, 0, 255)
            .texture(1, 1)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(entry.getNormalMatrix(), 0, 0, -1)
            .next();
        vertexConsumer.vertex(entry.getPositionMatrix(), -size, size, -size)
            .color(255, 150, 50, 255)
            .texture(1, 0)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(entry.getNormalMatrix(), 0, 0, -1)
            .next();
        vertexConsumer.vertex(entry.getPositionMatrix(), size, size, -size)
            .color(255, 150, 50, 255)
            .texture(0, 0)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(entry.getNormalMatrix(), 0, 0, -1)
            .next();
            
        // 右面 (X+)
        vertexConsumer.vertex(entry.getPositionMatrix(), size, -size, size)
            .color(255, 120, 20, 255)
            .texture(0, 1)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(entry.getNormalMatrix(), 1, 0, 0)
            .next();
        vertexConsumer.vertex(entry.getPositionMatrix(), size, -size, -size)
            .color(255, 120, 20, 255)
            .texture(1, 1)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(entry.getNormalMatrix(), 1, 0, 0)
            .next();
        vertexConsumer.vertex(entry.getPositionMatrix(), size, size, -size)
            .color(255, 170, 70, 255)
            .texture(1, 0)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(entry.getNormalMatrix(), 1, 0, 0)
            .next();
        vertexConsumer.vertex(entry.getPositionMatrix(), size, size, size)
            .color(255, 170, 70, 255)
            .texture(0, 0)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(entry.getNormalMatrix(), 1, 0, 0)
            .next();
            
        // 左面 (X-)
        vertexConsumer.vertex(entry.getPositionMatrix(), -size, -size, -size)
            .color(255, 120, 20, 255)
            .texture(0, 1)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(entry.getNormalMatrix(), -1, 0, 0)
            .next();
        vertexConsumer.vertex(entry.getPositionMatrix(), -size, -size, size)
            .color(255, 120, 20, 255)
            .texture(1, 1)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(entry.getNormalMatrix(), -1, 0, 0)
            .next();
        vertexConsumer.vertex(entry.getPositionMatrix(), -size, size, size)
            .color(255, 170, 70, 255)
            .texture(1, 0)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(entry.getNormalMatrix(), -1, 0, 0)
            .next();
        vertexConsumer.vertex(entry.getPositionMatrix(), -size, size, -size)
            .color(255, 170, 70, 255)
            .texture(0, 0)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(entry.getNormalMatrix(), -1, 0, 0)
            .next();
            
        // 顶面 (Y+)
        vertexConsumer.vertex(entry.getPositionMatrix(), -size, size, size)
            .color(255, 200, 100, 255)
            .texture(0, 1)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(entry.getNormalMatrix(), 0, 1, 0)
            .next();
        vertexConsumer.vertex(entry.getPositionMatrix(), size, size, size)
            .color(255, 200, 100, 255)
            .texture(1, 1)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(entry.getNormalMatrix(), 0, 1, 0)
            .next();
        vertexConsumer.vertex(entry.getPositionMatrix(), size, size, -size)
            .color(255, 230, 150, 255)
            .texture(1, 0)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(entry.getNormalMatrix(), 0, 1, 0)
            .next();
        vertexConsumer.vertex(entry.getPositionMatrix(), -size, size, -size)
            .color(255, 230, 150, 255)
            .texture(0, 0)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(entry.getNormalMatrix(), 0, 1, 0)
            .next();
            
        // 底面 (Y-)
        vertexConsumer.vertex(entry.getPositionMatrix(), -size, -size, -size)
            .color(255, 80, 0, 255)
            .texture(0, 1)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(entry.getNormalMatrix(), 0, -1, 0)
            .next();
        vertexConsumer.vertex(entry.getPositionMatrix(), size, -size, -size)
            .color(255, 80, 0, 255)
            .texture(1, 1)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(entry.getNormalMatrix(), 0, -1, 0)
            .next();
        vertexConsumer.vertex(entry.getPositionMatrix(), size, -size, size)
            .color(255, 120, 30, 255)
            .texture(1, 0)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(entry.getNormalMatrix(), 0, -1, 0)
            .next();
        vertexConsumer.vertex(entry.getPositionMatrix(), -size, -size, size)
            .color(255, 120, 30, 255)
            .texture(0, 0)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(entry.getNormalMatrix(), 0, -1, 0)
            .next();
    }
    
    @Override
    public Identifier getTexture(MeteorEntity entity) {
        return TEXTURE;
    }
}