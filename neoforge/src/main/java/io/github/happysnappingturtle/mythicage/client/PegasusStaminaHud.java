package io.github.happysnappingturtle.mythicage.client;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.summon.greek.SummonPegasusEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * 骑天马时的飞行耐力条（T14 §11.4 R4）：画在经验条的位置（同原版骑马时的跳跃条），骑着天马时隐藏经验条。
 * 金色 = 剩余耐力；耗尽时整条变暗红。
 */
@EventBusSubscriber(modid = MythicAgeIds.MOD_ID, value = Dist.CLIENT)
public final class PegasusStaminaHud {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "pegasus_stamina");
    private static final int WIDTH = 182;

    private PegasusStaminaHud() {
    }

    @SubscribeEvent
    static void onRegisterLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.EXPERIENCE_BAR, ID, (g, delta) -> render(g));
    }

    /** 骑着天马时不画经验条（耐力条画在同一位置）。 */
    @SubscribeEvent
    static void onRenderLayer(RenderGuiLayerEvent.Pre event) {
        if (event.getName().equals(VanillaGuiLayers.EXPERIENCE_BAR) && mount() != null) {
            event.setCanceled(true);
        }
    }

    private static SummonPegasusEntity mount() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.getVehicle() instanceof SummonPegasusEntity p ? p : null;
    }

    private static void render(GuiGraphics g) {
        SummonPegasusEntity p = mount();
        Minecraft mc = Minecraft.getInstance();
        if (p == null || mc.options.hideGui) {
            return;
        }
        int x = (g.guiWidth() - WIDTH) / 2;
        int y = g.guiHeight() - 32 + 3;
        int max = Math.max(1, p.staminaMax());
        int fill = Math.round(WIDTH * (float) p.stamina() / max);
        g.fill(x - 1, y - 1, x + WIDTH + 1, y + 6, 0xA0101010);
        g.fill(x, y, x + WIDTH, y + 5, p.stamina() <= 0 ? 0xFF6A1F1A : 0xFF3A3328);
        if (fill > 0) {
            g.fill(x, y, x + fill, y + 5, 0xFFE3C98A);
            g.fill(x, y, x + fill, y + 1, 0xFFF4E6BC);
        }
    }
}
