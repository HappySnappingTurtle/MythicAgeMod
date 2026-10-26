package io.github.happysnappingturtle.mythicage.client.power;

import io.github.happysnappingturtle.mythicage.client.geo.MythGeoModel;
import io.github.happysnappingturtle.mythicage.client.geo.MythGeoRenderer;
import io.github.happysnappingturtle.mythicage.client.power.greek.BoltFx;
import io.github.happysnappingturtle.mythicage.client.power.greek.BronzeFx;
import io.github.happysnappingturtle.mythicage.client.power.greek.LureFx;
import io.github.happysnappingturtle.mythicage.client.power.greek.RestorationFx;
import io.github.happysnappingturtle.mythicage.client.power.greek.SentinelFx;
import io.github.happysnappingturtle.mythicage.content.greek.power.GreekPowers;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.power.PowerRegistries;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

import java.util.Set;

/** 神力客户端的初始化（T13）：粒子工厂、外观叠层、各神力的专属表现。 */
@EventBusSubscriber(modid = MythicAgeIds.MOD_ID, value = Dist.CLIENT)
public final class PowerClientSetup {
    private PowerClientSetup() {
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        PowerFxRenderer.Registry.register(GreekPowers.BOLT, new BoltFx());
        PowerFxRenderer.Registry.register(GreekPowers.RESTORATION, new RestorationFx());
        PowerFxRenderer.Registry.register(GreekPowers.BRONZE, new BronzeFx());
        PowerFxRenderer.Registry.register(GreekPowers.PESTILENCE, new io.github.happysnappingturtle.mythicage.client.power.greek.PestilenceFx());
        PowerFxRenderer.Registry.register(GreekPowers.CURSE, new io.github.happysnappingturtle.mythicage.client.power.greek.CurseFx());
        PowerFxRenderer.Registry.register(GreekPowers.LURE, new LureFx());
        PowerFxRenderer.Registry.register(GreekPowers.SENTINEL, new SentinelFx());
    }

    @SubscribeEvent
    static void onParticles(RegisterParticleProvidersEvent event) {
        // 寿命、大小、重力、上浮、阻力、自发光、淡入、淡出、旋转、逐帧、透明度
        event.registerSpriteSet(PowerRegistries.SPARK.get(), s -> MythParticle.provider(s,
                new MythParticle.Style(4, 7, 0.08f, 0.16f, 0, 0, 0.82f, true, 0, 0.5f, 0, false, 1f)));
        event.registerSpriteSet(PowerRegistries.STORM_CLOUD.get(), s -> MythParticle.provider(s,
                new MythParticle.Style(22, 30, 0.9f, 1.6f, 0, 0, 0.9f, false, 0.2f, 0.35f, 0.01f, false, 0.92f)));
        event.registerSpriteSet(PowerRegistries.HEAL_MOTE.get(), s -> MythParticle.provider(s,
                new MythParticle.Style(16, 24, 0.04f, 0.08f, 0, 0.015f, 0.96f, true, 0.1f, 0.5f, 0, false, 0.9f)));
        event.registerSpriteSet(PowerRegistries.OLIVE_LEAF.get(), s -> MythParticle.provider(s,
                new MythParticle.Style(36, 50, 0.08f, 0.12f, 0.012f, 0, 0.96f, false, 0, 0.3f, 0.12f, false, 1f)));
        event.registerSpriteSet(PowerRegistries.BRONZE_FLAKE.get(), s -> MythParticle.provider(s,
                new MythParticle.Style(12, 20, 0.04f, 0.07f, 0.04f, 0, 0.92f, true, 0, 0.4f, 0.3f, false, 1f)));
        event.registerSpriteSet(PowerRegistries.CLEANSE.get(), s -> MythParticle.provider(s,
                new MythParticle.Style(26, 26, 0.18f, 0.18f, 0, 0.012f, 1f, true, 0.1f, 0.35f, 0, false, 1f)));
        event.registerSpriteSet(PowerRegistries.REPAIR.get(), s -> MythParticle.provider(s,
                new MythParticle.Style(26, 26, 0.16f, 0.16f, 0, 0.012f, 1f, true, 0.1f, 0.35f, 0, false, 1f)));
        event.registerSpriteSet(PowerRegistries.PLAGUE_MIST.get(), s -> MythParticle.provider(s,
                new MythParticle.Style(50, 80, 0.5f, 0.9f, 0, 0, 0.98f, false, 0.25f, 0.4f, 0.004f, false, 0.32f)));
        event.registerSpriteSet(PowerRegistries.ROSE_PUFF.get(), s -> MythParticle.provider(s,
                new MythParticle.Style(10, 16, 0.18f, 0.32f, 0, 0.01f, 0.9f, true, 0.1f, 0.5f, 0.05f, false, 0.85f)));
        event.registerSpriteSet(PowerRegistries.PLAGUE_MARK.get(), s -> MythParticle.provider(s,
                new MythParticle.Style(11, 11, 0.2f, 0.2f, 0, 0, 1f, true, 0.1f, 0.2f, 0, false, 1f)));
        event.registerSpriteSet(PowerRegistries.SEAL.get(), s -> MythParticle.provider(s,
                new MythParticle.Style(21, 21, 0.5f, 0.5f, 0, 0, 1f, true, 0.1f, 0.1f, 0, false, 1f)));
        event.registerSpriteSet(PowerRegistries.SEA_MOTE.get(), s -> MythParticle.provider(s,
                new MythParticle.Style(24, 36, 0.05f, 0.09f, 0, 0.012f, 0.96f, true, 0.15f, 0.45f, 0, false, 0.9f)));
        event.registerSpriteSet(PowerRegistries.RUNE_MOTE.get(), s -> MythParticle.provider(s,
                new MythParticle.Style(8, 14, 0.04f, 0.07f, 0, 0.004f, 0.9f, true, 0, 0.5f, 0, false, 1f)));
        event.registerSpriteSet(PowerRegistries.IMMUNE.get(), s -> MythParticle.provider(s,
                new MythParticle.Style(30, 30, 0.24f, 0.24f, 0, 0.015f, 1f, true, 0.05f, 0.35f, 0, false, 1f)));
    }

    @SubscribeEvent
    static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(GreekPowers.CURSED_PIG.get(), CursedPigRenderer::new);
        // 哨兵石像：GeckoLib 模型 + 眼睛发光层；视线只叠加在头上（升起、碎化期间不叠加）
        event.registerEntityRenderer(GreekPowers.SENTINEL_ENTITY.get(),
                ctx -> new MythGeoRenderer<>(ctx, new MythGeoModel<>("sentinel", Set.of()), 0.8F).glowing());
        // 废弃神殿的敌对哨兵（T22）：同一套模型与动画
        event.registerEntityRenderer(io.github.happysnappingturtle.mythicage.content.greek.ruin.GreekRuins.RUIN_SENTINEL.get(),
                ctx -> new MythGeoRenderer<>(ctx, new MythGeoModel<>("sentinel", Set.of()), 0.8F).glowing());
    }

    @SubscribeEvent
    static void onLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(AuraLayer.SHELL_LAYER, AuraLayer::createShell);
    }

    /** 外观叠层加到所有活体渲染器与两种玩家皮肤上；人形的另给一个外扩的人形外壳模型。 */
    @SubscribeEvent
    @SuppressWarnings({"unchecked", "rawtypes"})
    static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (var type : event.getEntityTypes()) {
            EntityRenderer<?> r = event.getRenderer(type);
            if (r instanceof LivingEntityRenderer lr) {
                lr.addLayer(new AuraLayer(lr, shell(event, lr)));
            }
        }
        for (PlayerSkin.Model skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof LivingEntityRenderer lr) {
                lr.addLayer(new AuraLayer(lr, shell(event, lr)));
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static net.minecraft.client.model.HumanoidModel shell(EntityRenderersEvent.AddLayers event, LivingEntityRenderer lr) {
        return lr.getModel() instanceof net.minecraft.client.model.HumanoidModel
                ? new net.minecraft.client.model.HumanoidModel(event.getEntityModels().bakeLayer(AuraLayer.SHELL_LAYER)) : null;
    }
}
