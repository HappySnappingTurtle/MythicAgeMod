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
        event.enqueueWork(() -> Sheets.addWoodType(ModWoodTypes.OLIVE));
    }

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.MINOTAUR.get(),
                ctx -> new MythGeoRenderer<>(ctx, new MythGeoModel<>("minotaur", Set.of("stone_r")), 1.1F).glowing());
        // 神庙祭司（T9b）：GeckoLib 模型，无隐藏骨骼
        event.registerEntityRenderer(ModEntities.GREEK_GUARDIAN.get(),
                ctx -> new MythGeoRenderer<>(ctx, new MythGeoModel<>("greek_guardian", Set.of()), 0.5F));
    }

    @SubscribeEvent
    static void onLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(BelieverWreathLayer.LAYER, BelieverWreathLayer::createLayer);
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
