package com.example.mythic_age.client.render.entity;

import com.example.mythic_age.client.render.entity.model.ModModelLayers;
import com.example.mythic_age.client.render.entity.model.TempleGuardianModel;
import com.example.mythic_age.entity.CustomTempleGuardianEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

/**
 * 自定义神殿守护者实体渲染器
 * 
 * 这个渲染器负责在游戏中显示 CustomTempleGuardianEntity。
 * 与原版守护者共用同一套几何，仅通过文明贴图区分。
 */
public class CustomTempleGuardianEntityRenderer extends MobEntityRenderer<CustomTempleGuardianEntity, TempleGuardianModel<CustomTempleGuardianEntity>> {
    
    /**
     * 构造函数
     * 
     * @param context 实体渲染器工厂上下文
     */
    public CustomTempleGuardianEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new TempleGuardianModel<>(context.getPart(ModModelLayers.TEMPLE_GUARDIAN)), 0.55F);
    }
    
    /**
     * 获取实体纹理
     * 
     * 根据不同的神话文明类型返回对应的纹理。
     * 如果没有专门的纹理，则使用默认的村民纹理。
     * 
     * @param entity 自定义神殿守护者实体
     * @return 纹理标识符
     */
    @Override
    public Identifier getTexture(CustomTempleGuardianEntity entity) {
        return TempleGuardianTextures.forType(entity.getMythologyType());
    }
}
