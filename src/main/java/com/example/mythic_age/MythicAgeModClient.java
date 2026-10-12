package com.example.mythic_age;

import com.example.mythic_age.client.render.entity.MinotaurEntityRenderer;
import com.example.mythic_age.client.render.entity.CentaurEntityRenderer;
import com.example.mythic_age.client.render.entity.CyclopsEntityRenderer;
import com.example.mythic_age.client.render.entity.HydraEntityRenderer;
import com.example.mythic_age.client.render.entity.MedusaEntityRenderer;
import com.example.mythic_age.client.render.entity.NemeanLionEntityRenderer;
import com.example.mythic_age.client.render.entity.ManticoreEntityRenderer;
import com.example.mythic_age.client.render.entity.ChimeraEntityRenderer;
import com.example.mythic_age.client.render.entity.ColossusEntityRenderer;
import com.example.mythic_age.client.render.entity.PegasusEntityRenderer;
import com.example.mythic_age.client.render.entity.AnubisWarriorEntityRenderer;
import com.example.mythic_age.client.render.entity.ScarabGuardianEntityRenderer;
import com.example.mythic_age.client.render.entity.MummySoldierEntityRenderer;
import com.example.mythic_age.client.render.entity.SphinxSentinelEntityRenderer;
import com.example.mythic_age.client.render.entity.SunFalconEntityRenderer;
import com.example.mythic_age.client.render.entity.FrostGiantEntityRenderer;
import com.example.mythic_age.client.render.entity.DraugrWarriorEntityRenderer;
import com.example.mythic_age.client.render.entity.ValkyrieEntityRenderer;
import com.example.mythic_age.client.render.entity.FenrirWolfEntityRenderer;
import com.example.mythic_age.client.render.entity.DwarfBerserkerEntityRenderer;
import com.example.mythic_age.client.render.entity.AzureDragonEntityRenderer;
import com.example.mythic_age.client.render.entity.QilinGuardianEntityRenderer;
import com.example.mythic_age.client.render.entity.JiangshiEntityRenderer;
import com.example.mythic_age.client.render.entity.NineTailedFoxEntityRenderer;
import com.example.mythic_age.client.render.entity.StoneLionGuardianEntityRenderer;
import com.example.mythic_age.client.render.entity.CrystalLeviathanEntityRenderer;
import com.example.mythic_age.client.render.entity.OrichalcumGolemEntityRenderer;
import com.example.mythic_age.client.render.entity.SirenPriestessEntityRenderer;
import com.example.mythic_age.client.render.entity.TidalGuardianEntityRenderer;
import com.example.mythic_age.client.render.entity.AbyssalRayEntityRenderer;
import com.example.mythic_age.client.render.entity.TempleGuardianEntityRenderer;
import com.example.mythic_age.client.render.entity.CustomTempleGuardianEntityRenderer;
import com.example.mythic_age.client.render.entity.WalkingTreeEntityRenderer;
import com.example.mythic_age.client.render.entity.model.ModModelLayers;
import com.example.mythic_age.client.render.entity.model.CentaurModel;
import com.example.mythic_age.client.render.entity.model.CyclopsModel;
import com.example.mythic_age.client.render.entity.model.HydraModel;
import com.example.mythic_age.client.render.entity.model.MedusaModel;
import com.example.mythic_age.client.render.entity.model.NemeanLionModel;
import com.example.mythic_age.client.render.entity.model.ManticoreModel;
import com.example.mythic_age.client.render.entity.model.ChimeraModel;
import com.example.mythic_age.client.render.entity.model.ColossusModel;
import com.example.mythic_age.client.render.entity.model.PegasusModel;
import com.example.mythic_age.client.render.entity.model.AnubisWarriorModel;
import com.example.mythic_age.client.render.entity.model.ScarabGuardianModel;
import com.example.mythic_age.client.render.entity.model.MummySoldierModel;
import com.example.mythic_age.client.render.entity.model.SphinxSentinelModel;
import com.example.mythic_age.client.render.entity.model.SunFalconModel;
import com.example.mythic_age.client.render.entity.model.FrostGiantModel;
import com.example.mythic_age.client.render.entity.model.DraugrWarriorModel;
import com.example.mythic_age.client.render.entity.model.ValkyrieModel;
import com.example.mythic_age.client.render.entity.model.FenrirWolfModel;
import com.example.mythic_age.client.render.entity.model.DwarfBerserkerModel;
import com.example.mythic_age.client.render.entity.model.AzureDragonModel;
import com.example.mythic_age.client.render.entity.model.QilinGuardianModel;
import com.example.mythic_age.client.render.entity.model.JiangshiModel;
import com.example.mythic_age.client.render.entity.model.NineTailedFoxModel;
import com.example.mythic_age.client.render.entity.model.StoneLionGuardianModel;
import com.example.mythic_age.client.render.entity.model.CrystalLeviathanModel;
import com.example.mythic_age.client.render.entity.model.OrichalcumGolemModel;
import com.example.mythic_age.client.render.entity.model.SirenPriestessModel;
import com.example.mythic_age.client.render.entity.model.TidalGuardianModel;
import com.example.mythic_age.client.render.entity.model.AbyssalRayModel;
import com.example.mythic_age.client.render.entity.model.MinotaurModel;
import com.example.mythic_age.client.render.entity.model.TempleGuardianModel;
import com.example.mythic_age.client.render.entity.model.WalkingTreeModel;
import com.example.mythic_age.client.render.MeteorEntityRenderer;
import com.example.mythic_age.client.particle.ModClientParticles;
import com.example.mythic_age.client.visual.VisualProbeServer;
import com.example.mythic_age.entity.ModEntities;
import com.example.mythic_age.entity.ModEntityTypes;
import com.example.mythic_age.network.ModNetworkClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/**
 * 神话时代 Mod 客户端初始化类
 * 
 * 负责处理所有仅在客户端运行的初始化工作，包括渲染、模型、纹理等视觉相关的内容。
 * 此类只在客户端环境中被调用，服务器端不会执行这些代码。
 * 
 * 主要职责：
 * - 注册实体渲染器：定义实体在游戏中的视觉外观
 * - 注册模型层：定义3D模型的结构和动画
 * - 注册客户端事件处理器：处理客户端特定的游戏事件
 * - 注册屏幕和GUI：自定义用户界面元素
 * - 注册粒子效果：视觉特效系统
 * 
 * 当前注册的内容：
 * - 米诺陶实体的渲染器和3D模型
 * - 神殿守护者实体的渲染器（原版和自定义）
 * - 实体模型层的纹理映射
 * 
 * @author MythicAge Development Team
 * @version 1.0.0
 * @since 1.20.1
 */
public class MythicAgeModClient implements ClientModInitializer {
    /**
     * 客户端初始化方法
     * 
     * 在客户端启动时调用，负责注册所有客户端特定的内容。
     * 这些内容只在客户端环境中存在，服务器端不会加载。
     * 
     * 注册内容包括：
     * - 实体渲染器：将服务器端的实体数据转换为客户端的视觉表现
     * - 模型层：定义3D模型的几何结构和纹理坐标
     * - 未来可能添加：自定义GUI、粒子效果、着色器等
     */
    @Override
    public void onInitializeClient() {
        ModClientParticles.registerClientParticles();

        // 注册米诺陶实体的渲染器
        // 将服务器端的米诺陶实体与客户端的视觉渲染器绑定
        EntityRendererRegistry.register(ModEntities.MINOTAUR, MinotaurEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.CYCLOPS, CyclopsEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.CENTAUR, CentaurEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.HYDRA, HydraEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.MEDUSA, MedusaEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.NEMEAN_LION, NemeanLionEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.MANTICORE, ManticoreEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.CHIMERA, ChimeraEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.COLOSSUS, ColossusEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.PEGASUS, PegasusEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.ANUBIS_WARRIOR, AnubisWarriorEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.SCARAB_GUARDIAN, ScarabGuardianEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.MUMMY_SOLDIER, MummySoldierEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.SPHINX_SENTINEL, SphinxSentinelEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.SUN_FALCON, SunFalconEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.FROST_GIANT, FrostGiantEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.DRAUGR_WARRIOR, DraugrWarriorEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.VALKYRIE, ValkyrieEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.FENRIR_WOLF, FenrirWolfEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.DWARF_BERSERKER, DwarfBerserkerEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.AZURE_DRAGON, AzureDragonEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.QILIN_GUARDIAN, QilinGuardianEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.JIANGSHI, JiangshiEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.NINE_TAILED_FOX, NineTailedFoxEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.STONE_LION_GUARDIAN, StoneLionGuardianEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.CRYSTAL_LEVIATHAN, CrystalLeviathanEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.ORICHALCUM_GOLEM, OrichalcumGolemEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.SIREN_PRIESTESS, SirenPriestessEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.TIDAL_GUARDIAN, TidalGuardianEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.ABYSSAL_RAY, AbyssalRayEntityRenderer::new);
        
        // 注册米诺陶的3D模型层
        // 定义模型的几何结构、动画骨骼和纹理映射
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.MINOTAUR, MinotaurModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.CYCLOPS, CyclopsModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.CENTAUR, CentaurModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.HYDRA, HydraModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.MEDUSA, MedusaModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.NEMEAN_LION, NemeanLionModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.MANTICORE, ManticoreModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.CHIMERA, ChimeraModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.COLOSSUS, ColossusModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.PEGASUS, PegasusModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.ANUBIS_WARRIOR, AnubisWarriorModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.SCARAB_GUARDIAN, ScarabGuardianModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.MUMMY_SOLDIER, MummySoldierModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.SPHINX_SENTINEL, SphinxSentinelModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.SUN_FALCON, SunFalconModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.FROST_GIANT, FrostGiantModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.DRAUGR_WARRIOR, DraugrWarriorModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.VALKYRIE, ValkyrieModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.FENRIR_WOLF, FenrirWolfModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.DWARF_BERSERKER, DwarfBerserkerModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.AZURE_DRAGON, AzureDragonModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.QILIN_GUARDIAN, QilinGuardianModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.JIANGSHI, JiangshiModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.NINE_TAILED_FOX, NineTailedFoxModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.STONE_LION_GUARDIAN, StoneLionGuardianModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.CRYSTAL_LEVIATHAN, CrystalLeviathanModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.ORICHALCUM_GOLEM, OrichalcumGolemModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.SIREN_PRIESTESS, SirenPriestessModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.TIDAL_GUARDIAN, TidalGuardianModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.ABYSSAL_RAY, AbyssalRayModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.TEMPLE_GUARDIAN, TempleGuardianModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.WALKING_TREE, WalkingTreeModel::getTexturedModelData);

        // ============ 原版神殿守护者渲染器 ============
        
        // 注册神殿守护者实体的渲染器
        // 使用自定义渲染器，根据文明类型使用不同纹理
        EntityRendererRegistry.register(ModEntities.TEMPLE_GUARDIAN, 
            TempleGuardianEntityRenderer::new);

        // 注册各文明特定的神殿守护者渲染器
        EntityRendererRegistry.register(ModEntities.TEMPLE_GUARDIAN_GREEK, 
            TempleGuardianEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.TEMPLE_GUARDIAN_EGYPTIAN, 
            TempleGuardianEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.TEMPLE_GUARDIAN_CHINESE, 
            TempleGuardianEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.TEMPLE_GUARDIAN_ATLANTEAN, 
            TempleGuardianEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.TEMPLE_GUARDIAN_NORDIC, 
            TempleGuardianEntityRenderer::new);

        // ============ 自定义神殿守护者渲染器 ============
        
        // 注册自定义神殿守护者实体的渲染器
        // 这些实体使用完全自定义的交易系统，不受原版村民限制
        EntityRendererRegistry.register(ModEntities.CUSTOM_TEMPLE_GUARDIAN_GREEK, 
            CustomTempleGuardianEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.CUSTOM_TEMPLE_GUARDIAN_EGYPTIAN, 
            CustomTempleGuardianEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.CUSTOM_TEMPLE_GUARDIAN_CHINESE, 
            CustomTempleGuardianEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.CUSTOM_TEMPLE_GUARDIAN_ATLANTEAN, 
            CustomTempleGuardianEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.CUSTOM_TEMPLE_GUARDIAN_NORDIC, 
            CustomTempleGuardianEntityRenderer::new);

        // ============ 行走树实体渲染器 ============
        
        // 注册行走树实体的渲染器
        EntityRendererRegistry.register(ModEntities.WALKING_TREE, 
            WalkingTreeEntityRenderer::new);

        // ============ 文明村民渲染器 ============
        
        // 村民文明系统通过Mixin增强原版村民，无需额外渲染器注册

        // 未来扩展：可以在这里注册更多客户端内容
        // 例如：GUI界面、粒子效果、自定义渲染等
        
        // 注册陨石实体渲染器
        try {
            // 先检查实体类型是否存在
            if (ModEntityTypes.METEOR == null) {
                MythicAgeMod.LOGGER.error("Cannot register renderer: ModEntityTypes.METEOR is null!");
            } else {
                MythicAgeMod.LOGGER.info("Registering renderer for Meteor entity: " + ModEntityTypes.METEOR.toString());
                // 使用安全的渲染器工厂方法，添加空值检查
                EntityRendererRegistry.register(ModEntityTypes.METEOR, (context) -> {
                    if (context == null) {
                        MythicAgeMod.LOGGER.error("EntityRendererFactory.Context is null for Meteor entity!");
                        return null;
                    }
                    try {
                        return new MeteorEntityRenderer(context);
                    } catch (Exception e) {
                        MythicAgeMod.LOGGER.error("Failed to create MeteorEntityRenderer: " + e.getMessage());
                        e.printStackTrace();
                        return null;
                    }
                });
                MythicAgeMod.LOGGER.info("Successfully registered Meteor entity renderer");
                
                // 额外验证渲染器是否正确注册
                MythicAgeMod.LOGGER.info("Meteor entity class: " + ModEntityTypes.METEOR.getClass().getName());
                MythicAgeMod.LOGGER.info("Meteor entity ID: " + ModEntityTypes.METEOR.toString());
            }
        } catch (Exception e) {
            MythicAgeMod.LOGGER.error("Failed to register Meteor entity renderer: " + e.getMessage());
            e.printStackTrace();
        }
        
        // 注册客户端网络处理器
        ModNetworkClient.registerClientNetworkHandlers();

        // 默认关闭；仅在本地视觉验收时通过 -Dmythic.visualProbe=true 开启。
        VisualProbeServer.startIfEnabled();
    }
}
