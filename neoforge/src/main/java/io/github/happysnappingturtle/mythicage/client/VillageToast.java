package io.github.happysnappingturtle.mythicage.client;

import io.github.happysnappingturtle.mythicage.village.economy.AffairsPayloads;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** 村庄升级 toast（T24 设计文档 §4.5，12 号 §4.3 B）：图标 + 标题 + 最多两行正文，显示 6 秒。 */
public final class VillageToast implements Toast {
    private static final long DURATION_MS = 6000;
    private static final int WIDTH = 200;
    private static final int TEXT_W = WIDTH - 36;
    private final ItemStack icon;
    private final AffairsPayloads.Toast payload;
    private List<FormattedCharSequence> body;

    public VillageToast(AffairsPayloads.Toast payload) {
        this.payload = payload;
        this.icon = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(payload.icon())));
    }

    @Override
    public int width() {
        return WIDTH;
    }

    @Override
    public int height() {
        return 32 + Math.max(0, lines().size() - 1) * 10;
    }

    private List<FormattedCharSequence> lines() {
        if (body == null) {
            Font font = net.minecraft.client.Minecraft.getInstance().font;
            List<FormattedCharSequence> all = font.split(payload.body(), TEXT_W);
            body = all.size() > 2 ? all.subList(0, 2) : all;
        }
        return body;
    }

    @Override
    public Visibility render(GuiGraphics g, ToastComponent toasts, long timeSinceLastVisible) {
        Font font = toasts.getMinecraft().font;
        int h = height();
        g.fill(0, 0, WIDTH, h, 0xF0201A14);
        g.fill(0, 0, WIDTH, 1, 0xFFB89A5A);
        g.fill(0, h - 1, WIDTH, h, 0xFFB89A5A);
        g.fill(0, 0, 1, h, 0xFFB89A5A);
        g.fill(WIDTH - 1, 0, WIDTH, h, 0xFFB89A5A);
        g.renderFakeItem(icon, 8, 8);
        g.drawString(font, font.split(payload.title(), TEXT_W).get(0), 30, 7, 0xFFF2D27A, false);
        int y = 18;
        for (FormattedCharSequence l : lines()) {
            g.drawString(font, l, 30, y, 0xFFEDE8DC, false);
            y += 10;
        }
        return timeSinceLastVisible >= DURATION_MS * toasts.getNotificationDisplayTimeMultiplier() ? Visibility.HIDE : Visibility.SHOW;
    }
}
