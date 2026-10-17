package io.github.happysnappingturtle.mythicage.client.power;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * 吟唱条（T12 设计文档 §7）：准星下方，金色进度；提交后满格停留片刻，被打断时变红并淡出。
 * 冷却显示用原版物品冷却遮罩（D4），这里不画。
 */
@EventBusSubscriber(modid = MythicAgeIds.MOD_ID, value = Dist.CLIENT)
public final class PowerHud {
    private static final int WIDTH = 82;

    private PowerHud() {
    }

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR,
                ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "power_channel"), PowerHud::render);
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        ClientPowerFx.Channel c = ClientPowerFx.local();
        if (c == null || mc.level == null || mc.options.hideGui) {
            return;
        }
        double now = mc.level.getGameTime() + delta.getGameTimeDeltaPartialTick(false);
        double p = c.progress(now);
        float alpha = 1f;
        if (c.endedAt() >= 0) {
            alpha = (float) Math.max(0, 1 - (now - c.endedAt()) / ClientPowerFx.LINGER);
        }
        int a = (int) (alpha * 255) << 24;
        if ((a >>> 24) < 8) {
            return;
        }
        int x = (g.guiWidth() - WIDTH) / 2;
        int y = g.guiHeight() / 2 + 14;
        int fill = c.cancelled() ? 0xC23A2A : 0xE0B341;
        g.fill(x - 1, y - 1, x + WIDTH + 1, y + 4, (int) (alpha * 0xAA) << 24);
        g.fill(x, y, x + (int) Math.round(WIDTH * p), y + 3, a | fill);
        Component name = Component.translatableWithFallback("mythic_age.power." + c.powerId(), c.powerId());
        g.drawCenteredString(mc.font, name, g.guiWidth() / 2, y + 6, a | 0xE8D27A);
    }
}
