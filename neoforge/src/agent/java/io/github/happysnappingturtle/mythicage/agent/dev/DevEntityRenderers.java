package io.github.happysnappingturtle.mythicage.agent.dev;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** 开发实体的渲染器。 */
@EventBusSubscriber(modid = MythicAgeIds.MOD_ID, value = Dist.CLIENT)
public final class DevEntityRenderers {
    private DevEntityRenderers() {
    }

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        if (DevEntities.calibration != null) {
            event.registerEntityRenderer(DevEntities.calibration,
                    ctx -> new GeoEntityRenderer<>(ctx, new DefaultedEntityGeoModel<>(DevEntities.CALIBRATION_ID)));
        }
    }
}
