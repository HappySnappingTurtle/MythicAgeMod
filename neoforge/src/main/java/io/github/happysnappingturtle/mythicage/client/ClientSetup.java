package io.github.happysnappingturtle.mythicage.client;

import io.github.happysnappingturtle.mythicage.client.geo.MythGeoModel;
import io.github.happysnappingturtle.mythicage.client.geo.MythGeoRenderer;
import io.github.happysnappingturtle.mythicage.client.screen.OlivePressScreen;
import io.github.happysnappingturtle.mythicage.content.greek.GreekAgriculture;
import io.github.happysnappingturtle.mythicage.content.wood.ModWoodTypes;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.entity.ModEntities;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import java.util.Set;

/** 客户端初始化：告示牌材质、实体渲染器、界面。 */
@EventBusSubscriber(modid = MythicAgeIds.MOD_ID, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            Sheets.addWoodType(ModWoodTypes.OLIVE);
            EquipmentItemProperties.register();
        });
    }

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.MINOTAUR.get(),
                ctx -> new MythGeoRenderer<>(ctx, new MythGeoModel<>("minotaur", Set.of("stone_r")), 1.1F).glowing());
        // 召唤版弥诺陶洛斯（T14）：与 T7 共用模型与动画
        event.registerEntityRenderer(io.github.happysnappingturtle.mythicage.summon.greek.GreekSummons.MINOTAUR.get(),
                ctx -> new MythGeoRenderer<>(ctx, new MythGeoModel<>("minotaur", Set.of("stone_r")), 1.1F).glowing());
        // 召唤版天马（T14 阶段 ②）：眼睛金光只在天眼时画
        event.registerEntityRenderer(io.github.happysnappingturtle.mythicage.summon.greek.GreekSummons.PEGASUS.get(),
                ctx -> {
                    var r = new MythGeoRenderer<>(ctx, new MythGeoModel<io.github.happysnappingturtle.mythicage.summon.greek.SummonPegasusEntity>(
                            "pegasus", Set.of("bridle", "saddle")), 0.9F);
                    r.addRenderLayer(new io.github.happysnappingturtle.mythicage.client.geo.SkyEyeGlowLayer(r));
                    return r;
                });
        // 召唤版半人马（T14 阶段 ③）：上身随视线转向；松树干投射物
        event.registerEntityRenderer(io.github.happysnappingturtle.mythicage.summon.greek.GreekSummons.CENTAUR.get(),
                ctx -> new MythGeoRenderer<>(ctx, new io.github.happysnappingturtle.mythicage.client.summon.CentaurGeoModel(Set.of()), 1.1F));
        event.registerEntityRenderer(io.github.happysnappingturtle.mythicage.summon.greek.GreekSummons.CENTAUR_PINE.get(),
                io.github.happysnappingturtle.mythicage.client.summon.CentaurPineRenderer::new);
        // 野外半人马（T15）：同一模型，不披豹皮
        event.registerEntityRenderer(io.github.happysnappingturtle.mythicage.wild.greek.GreekWild.CENTAUR.get(),
                ctx -> new MythGeoRenderer<>(ctx, new io.github.happysnappingturtle.mythicage.client.summon.CentaurGeoModel(Set.of("pelt")), 1.1F));
        // 斯廷法利斯怪鸟（T15 阶段 ②）：领头鸟才显示冠羽；青铜羽弹射物画成物品贴图
        event.registerEntityRenderer(io.github.happysnappingturtle.mythicage.wild.greek.GreekWild.STYMPHALIAN_BIRD.get(),
                ctx -> new MythGeoRenderer<>(ctx, new io.github.happysnappingturtle.mythicage.client.wild.StymphalianGeoModel(), 0.8F));
        // 希腊投矛兵的投矛（T20）：先画成物品贴图，阶段 ③ 换模型
        event.registerEntityRenderer(io.github.happysnappingturtle.mythicage.content.greek.village.soldier.GreekSoldiers.JAVELIN.get(),
                net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        // 文明村庄的居民（T19）
        event.registerEntityRenderer(ModEntities.RESIDENT.get(),
                ctx -> new MythGeoRenderer<>(ctx, new io.github.happysnappingturtle.mythicage.client.ResidentGeoModel(), 0.6F));
        // 树仙女与根刺（T15 阶段 ⑤）
        event.registerEntityRenderer(io.github.happysnappingturtle.mythicage.wild.greek.GreekWild.HAMADRYAD.get(),
                ctx -> new MythGeoRenderer<>(ctx, new MythGeoModel<>("hamadryad", Set.of()), 0.6F));
        event.registerEntityRenderer(io.github.happysnappingturtle.mythicage.wild.greek.GreekWild.ROOT_SPIKE.get(),
                ctx -> new MythGeoRenderer<>(ctx, new MythGeoModel<>("root_spike", Set.of()), 0.0F));
        // 卡尔基诺斯（T15 阶段 ④）
        event.registerEntityRenderer(io.github.happysnappingturtle.mythicage.wild.greek.GreekWild.KARKINOS.get(),
                ctx -> new MythGeoRenderer<>(ctx, new MythGeoModel<>("karkinos", Set.of()), 1.0F));
        // 哈耳庇厄（T15 阶段 ③）
        event.registerEntityRenderer(io.github.happysnappingturtle.mythicage.wild.greek.GreekWild.HARPY.get(),
                ctx -> new MythGeoRenderer<>(ctx, new MythGeoModel<>("harpy", Set.of()), 0.6F));
        event.registerEntityRenderer(io.github.happysnappingturtle.mythicage.wild.greek.GreekWild.BRONZE_FEATHER_SHOT.get(),
                net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        // 神庙祭司（T9b）：GeckoLib 模型，无隐藏骨骼
        event.registerEntityRenderer(ModEntities.GREEK_GUARDIAN.get(),
                ctx -> new MythGeoRenderer<>(ctx, new MythGeoModel<>("greek_guardian", Set.of()), 0.5F));
        // 主神像（T11a）
        event.registerBlockEntityRenderer(io.github.happysnappingturtle.mythicage.statue.PatronStatues.CORE_BE.get(),
                StatueCoreRenderer::new);
        // 神坛上的圣物陈列（T16）
        event.registerBlockEntityRenderer(io.github.happysnappingturtle.mythicage.content.greek.temple.GreekTemple.GREEK_ALTAR_BE.get(),
                ctx -> new ShrineRelicRenderer<>(ctx, 15 / 16F, 1.0F));
    }

    @SubscribeEvent
    static void onRegisterModels(net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional event) {
        ShrineRelicRenderer.registerModels(event);
    }

    @SubscribeEvent
    static void onLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(BelieverWreathLayer.LAYER, BelieverWreathLayer::createLayer);
        // 青铜甲的 3D 部件（T17）
        event.registerLayerDefinition(io.github.happysnappingturtle.mythicage.client.equipment.BronzeArmorModel.OUTER,
                io.github.happysnappingturtle.mythicage.client.equipment.BronzeArmorModel::outerLayer);
        event.registerLayerDefinition(io.github.happysnappingturtle.mythicage.client.equipment.BronzeArmorModel.INNER,
                io.github.happysnappingturtle.mythicage.client.equipment.BronzeArmorModel::innerLayer);
    }

    /** 希腊信徒的橄榄枝饰（T10）。 */
    @SubscribeEvent
    static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        if (event.getRenderer(EntityType.VILLAGER) instanceof VillagerRenderer villager) {
            villager.addLayer(new BelieverWreathLayer(villager, event.getEntityModels()));
        }
    }

    @SubscribeEvent
    static void onMenuScreens(RegisterMenuScreensEvent event) {
        event.register(GreekAgriculture.OLIVE_PRESS_MENU.get(), OlivePressScreen::new);
    }
}
