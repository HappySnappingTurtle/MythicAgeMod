package io.github.happysnappingturtle.mythicage.client;

import io.github.happysnappingturtle.mythicage.boss.ArenaHudPayload;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * 试炼 Boss 血条下的一行（T24 §5.3）：「完整石柱 6 / 8 · 机关墙：A 组升起 · 首次挑战」。
 * 服务端每秒推送一次；离开大厅或战斗结束时清掉；3 秒没收到也清掉（断线等）。
 */
@EventBusSubscriber(modid = MythicAgeIds.MOD_ID, value = Dist.CLIENT)
public final class ArenaHud {
    private static final long STALE_MILLIS = 3000;
    private static ArenaHudPayload shown = ArenaHudPayload.OFF;
    private static long receivedAt;

    private ArenaHud() {
    }

    public static void accept(ArenaHudPayload p) {
        shown = p;
        receivedAt = System.currentTimeMillis();
    }

    @SubscribeEvent
    static void onRegisterLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.BOSS_OVERLAY, ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "arena_hud"),
                ArenaHud::render);
    }

    static Component line(ArenaHudPayload p) {
        MutableComponent c = Component.empty();
        if (p.total() > 0) {
            c.append(Component.translatable("mythic_age.arena.hud.pillars", p.intact(), p.total()));
        }
        if (!p.walls().isEmpty()) {
            c.append(" · ").append(Component.translatable("mythic_age.arena.hud.walls." + p.walls()));
        }
        c.append(" · ").append(Component.translatable(p.firstKill() ? "mythic_age.arena.hud.first_done"
                : "mythic_age.arena.hud.first_pending"));
        return c;
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (!shown.active() || mc.options.hideGui || System.currentTimeMillis() - receivedAt > STALE_MILLIS) {
            return;
        }
        Component text = line(shown);
        // 原版血条从 y = 12 起每条占 19；只有一条时文字在它正下方
        int y = 12 + 19 + 2;
        g.drawString(mc.font, text, (g.guiWidth() - mc.font.width(text)) / 2, y, 0xFFEDE8DC, true);
    }
}
