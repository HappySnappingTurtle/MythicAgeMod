package io.github.happysnappingturtle.mythicage.client.screen;

import io.github.happysnappingturtle.mythicage.village.found.FoundationPayloads;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * 选主神界面（T19 设计文档 §1.13 K2）：奠基石放下后给建立者三选一；每位主神一句作用说明与名下的次神。
 * 与村务界面同一风格。选定后发回服务端立村；关掉不选，奠基石保持「待选」，右键再打开。
 */
public class FoundationScreen extends Screen {
    private static final int W = 320;
    private static final int PANEL = 0xF0201A14;
    private static final int BORDER = 0xFFB89A5A;
    private static final int DIM = 0xFFB9B09C;
    private static final int GOLD = 0xFFF2D27A;
    private static final int TEXT = 0xFFE8E0CC;

    private final FoundationPayloads.Choose view;
    private int left;
    private int top;
    private int h;
    /** 每位主神一块：说明与次神两段，都按面板宽度折行。 */
    private final List<List<net.minecraft.util.FormattedCharSequence>> desc = new java.util.ArrayList<>();
    private final List<List<net.minecraft.util.FormattedCharSequence>> minors = new java.util.ArrayList<>();
    private final List<Integer> rowTop = new java.util.ArrayList<>();

    public FoundationScreen(FoundationPayloads.Choose view) {
        super(Component.translatable("mythic_age.foundation.title." + view.civ()));
        this.view = view;
    }

    @Override
    protected void init() {
        desc.clear();
        minors.clear();
        rowTop.clear();
        int inner = W - 16;
        int total = 0;
        for (FoundationPayloads.Option o : view.options()) {
            desc.add(font.split(Component.translatable("mythic_age.foundation.patron." + o.god()), inner));
            MutableComponent m = Component.translatable("mythic_age.foundation.minors");
            for (int i = 0; i < o.minors().size(); i++) {
                m.append(i == 0 ? Component.empty() : Component.literal("、")).append(Component.translatable("mythic_age.god." + o.minors().get(i)));
            }
            minors.add(o.minors().isEmpty() ? List.of() : font.split(m, inner));
            rowTop.add(total);
            total += 22 + (desc.get(desc.size() - 1).size() + minors.get(minors.size() - 1).size()) * 10 + 8;
        }
        List<net.minecraft.util.FormattedCharSequence> prompt = font.split(Component.translatable("mythic_age.foundation.prompt"), inner);
        int head = 20 + prompt.size() * 10 + 6;
        h = Math.min(height - 8, head + total + 4);
        left = (width - W) / 2;
        top = (height - h) / 2;
        for (int i = 0; i < view.options().size(); i++) {
            FoundationPayloads.Option o = view.options().get(i);
            addRenderableWidget(Button.builder(Component.translatable("mythic_age.foundation.choose"), b -> {
                PacketDistributor.sendToServer(new FoundationPayloads.Found(view.pos(), o.god()));
                onClose();
            }).bounds(left + W - 68, top + head + rowTop.get(i), 60, 16).build());
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderTransparentBackground(g);
        g.fill(left - 1, top - 1, left + W + 1, top + h + 1, BORDER);
        g.fill(left, top, left + W, top + h, PANEL);
        g.drawString(font, title, left + 8, top + 8, GOLD, false);
        int y = top + 20;
        for (var line : font.split(Component.translatable("mythic_age.foundation.prompt"), W - 16)) {
            g.drawString(font, line, left + 8, y, DIM, false);
            y += 10;
        }
        int head = y - top + 6;
        for (int i = 0; i < view.options().size(); i++) {
            int ry = top + head + rowTop.get(i);
            g.drawString(font, Component.translatable("mythic_age.god." + view.options().get(i).god()), left + 8, ry + 4, GOLD, false);
            int ly = ry + 22;
            for (var line : desc.get(i)) {
                g.drawString(font, line, left + 8, ly, TEXT, false);
                ly += 10;
            }
            for (var line : minors.get(i)) {
                g.drawString(font, line, left + 8, ly, DIM, false);
                ly += 10;
            }
        }
        super.render(g, mouseX, mouseY, partial);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
