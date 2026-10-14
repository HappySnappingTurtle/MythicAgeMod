package io.github.happysnappingturtle.mythicage.client;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.player.ClientMythData;
import io.github.happysnappingturtle.mythicage.player.PlayerSyncPayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * 恩宠条（T4 设计文档 §5）。默认不常驻：某个文明的恩宠变化后显示 {@link #SHOW_MILLIS} 毫秒。
 * 显示条件可扩展：T9/T12 再加入「手持神力道具」「靠近神坛」。
 */
@EventBusSubscriber(modid = MythicAgeIds.MOD_ID, value = Dist.CLIENT)
public final class FavorHud {
    public static final long SHOW_MILLIS = 5000;
    private static final int BAR_WIDTH = 60;
    private static final List<Predicate<Civilization>> EXTRA_CONDITIONS = new ArrayList<>();

    private FavorHud() {
    }

    /** 追加一个显示条件（任一条件成立即显示该文明的恩宠条）。 */
    public static void addCondition(Predicate<Civilization> condition) {
        EXTRA_CONDITIONS.add(condition);
    }

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR,
                ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "favor"), FavorHud::render);
    }

    static boolean visible(Civilization civ, long now) {
        if (ClientMythData.civ(civ) == null) {
            return false;
        }
        if (now - ClientMythData.lastChangeMillis(civ) < SHOW_MILLIS) {
            return true;
        }
        return EXTRA_CONDITIONS.stream().anyMatch(c -> c.test(civ));
    }

    private static void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) {
            return;
        }
        Font font = mc.font;
        long now = System.currentTimeMillis();
        // 屏幕左下角，快捷栏左侧，避开快捷栏上方的生命、护甲和经验条
        int x = 4;
        int y = graphics.guiHeight() - 14;
        for (Civilization civ : Civilization.values()) {
            if (!visible(civ, now)) {
                continue;
            }
            PlayerSyncPayload.CivEntry e = ClientMythData.civ(civ);
            Component text = Component.translatableWithFallback("mythic_age.hud.favor", "%s %s/%s",
                    Component.translatable("mythic_age.civ." + civ.id()), e.favor(), e.cap());
            graphics.drawString(font, text, x, y - 9, 0xFFE8D27A, true);
            int filled = e.cap() <= 0 ? 0 : (int) Math.min(BAR_WIDTH, BAR_WIDTH * e.favor() / e.cap());
            graphics.fill(x, y + 1, x + BAR_WIDTH, y + 4, 0xAA000000);
            graphics.fill(x, y + 1, x + filled, y + 4, 0xFFE0B341);
            y -= 18;
        }
    }
}
