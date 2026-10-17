package io.github.happysnappingturtle.mythicage.client.screen;

import io.github.happysnappingturtle.mythicage.client.TempleHighlightRenderer;
import io.github.happysnappingturtle.mythicage.content.greek.temple.AltarSurveyPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 神坛检视界面（T8a 设计文档 §4，12 号 §4.1）：左栏身份与结构、中栏祈祷与产率、右栏缺失项与高亮按钮。
 * 只展示服务端发来的数字，不在客户端做判定。
 */
public class GreekAltarScreen extends Screen {
    private static final int W = 420;
    private static final int H = 200;
    private static final int PANEL = 0xE0201A14;
    private static final int BORDER = 0xFFB89A5A;
    private static final int TEXT = 0xFFEDE8DC;
    private static final int DIM = 0xFFB9B09C;
    private static final int OK = 0xFF7FD67F;
    private static final int BAD = 0xFFFF8A70;

    private final AltarSurveyPayload p;
    private int left;
    private int top;

    public GreekAltarScreen(AltarSurveyPayload payload) {
        super(Component.translatable("mythic_age.altar.title"));
        this.p = payload;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        addRenderableWidget(Button.builder(Component.translatable("mythic_age.altar.highlight"), b -> {
            TempleHighlightRenderer.show(p);
            onClose();
        }).bounds(left + W - 130, top + H - 28, 120, 20).build());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static Component tier(String name) {
        return Component.translatable("mythic_age.temple.tier." + name);
    }

    private static Component age(String id) {
        return Component.translatable("mythic_age.age." + id);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.fill(left, top, left + W, top + H, PANEL);
        g.renderOutline(left, top, W, H, BORDER);
        g.drawString(font, title, left + 8, top + 7, BORDER, false);
        int colW = (W - 24) / 3;
        int c1 = left + 8;
        int c2 = c1 + colW + 4;
        int c3 = c2 + colW + 4;
        int y0 = top + 22;
        g.fill(c2 - 3, y0, c2 - 2, top + H - 8, 0x60B89A5A);
        g.fill(c3 - 3, y0, c3 - 2, top + H - 8, 0x60B89A5A);

        // 左栏：身份与结构
        int y = y0;
        y = line(g, Component.translatable("mythic_age.altar.owner", p.ownerName(), age(p.ownerAge().id())), c1, y, colW, TEXT);
        if (!p.hasResult()) {
            line(g, Component.translatable("mythic_age.altar.no_result"), c1, y, colW, DIM);
        } else {
            String coef = String.format(Locale.ROOT, "%.1f", p.coefficient());
            y = line(g, Component.translatable("mythic_age.altar.level", tier(p.tierNames().get(p.grantedLevel())), coef),
                    c1, y + 2, colW, BORDER);
            if (p.structuralLevel() > p.grantedLevel()) {
                y = line(g, Component.translatable("mythic_age.altar.structural", tier(p.tierNames().get(p.structuralLevel()))),
                        c1, y, colW, DIM);
            }
            y += 4;
            boolean max = p.structuralLevel() >= p.tierNames().size() - 1;
            y = line(g, max ? Component.translatable("mythic_age.altar.max")
                    : Component.translatable("mythic_age.altar.next", tier(p.tierNames().get(p.structuralLevel() + 1))),
                    c1, y, colW, DIM);
            for (AltarSurveyPayload.Row r : p.rows()) {
                y = progress(g, r, c1, y, colW);
            }
        }

        // 中栏：祈祷与产率
        y = y0;
        y = line(g, Component.translatable("mythic_age.altar.slots", p.slotsFound(), p.slotsCap()), c2, y, colW, TEXT);
        if (p.hasResult() && p.slotsFound() < p.slotsCap()) {
            y = line(g, Component.translatable("mythic_age.altar.slots.short", p.slotsFound()), c2, y, colW, BAD);
        }
        AltarSurveyPayload.Prayer pr = p.prayer();
        y = line(g, Component.translatable("mythic_age.altar.praying", pr.praying()), c2, y, colW, TEXT);
        y = line(g, Component.translatable("mythic_age.altar.believers", pr.believers()), c2, y, colW, TEXT);
        y += 4;
        y = line(g, Component.translatable("mythic_age.altar.today", String.format(Locale.ROOT, "%.1f", pr.today())),
                c2, y, colW, TEXT);
        y = line(g, Component.translatable("mythic_age.altar.estimate", String.format(Locale.ROOT, "%.1f", p.estimate())),
                c2, y, colW, TEXT);
        y = line(g, Component.translatable("mythic_age.altar.estimate.note"), c2, y, colW, DIM);
        y += 4;
        if (pr.rainHalved()) {
            y = line(g, Component.translatable("mythic_age.altar.rain"), c2, y, colW, BAD);
        }
        if (pr.tooClose() > 0) {
            y = line(g, Component.translatable("mythic_age.altar.too_close", pr.tooClose()), c2, y, colW, BAD);
        }
        if (pr.unreachable()) {
            y = line(g, Component.translatable("mythic_age.altar.unreachable"), c2, y, colW, BAD);
        }
        if (pr.favorFull()) {
            line(g, Component.translatable("mythic_age.altar.favor_full"), c2, y, colW, BORDER);
        }

        // 右栏：缺失项
        y = y0;
        y = line(g, Component.translatable("mythic_age.altar.missing.title"), c3, y, colW, BORDER);
        List<Component> missing = new ArrayList<>();
        if (p.unverified()) {
            missing.add(Component.translatable("mythic_age.altar.unverified"));
        }
        if (!p.ageRequired().isEmpty() && p.grantedLevel() + 1 < p.tierNames().size()) {
            missing.add(Component.translatable("mythic_age.altar.age_required", age(p.ageRequired()),
                    tier(p.tierNames().get(p.grantedLevel() + 1))));
        }
        for (AltarSurveyPayload.Row r : p.rows()) {
            if (r.have() < r.need()) {
                missing.add(r.isBoolean() ? Component.translatable("mythic_age.altar.missing." + r.key())
                        : Component.translatable("mythic_age.altar.missing.row",
                        Component.translatable("mythic_age.altar.row." + r.key()), r.have(), r.need()));
            }
        }
        if (missing.isEmpty()) {
            missing.add(Component.translatable(p.structuralLevel() >= p.tierNames().size() - 1
                    ? "mythic_age.altar.max" : "mythic_age.altar.missing.none"));
        }
        for (Component m : missing) {
            y = line(g, Component.literal("· ").append(m), c3, y, colW, missing.size() == 1 && p.rows().stream()
                    .allMatch(r -> r.have() >= r.need()) && !p.unverified() ? OK : BAD);
        }
        line(g, Component.translatable("mythic_age.altar.highlight.legend").withStyle(ChatFormatting.ITALIC),
                c3, top + H - 72, colW, DIM);
    }

    private int line(GuiGraphics g, Component text, int x, int y, int w, int color) {
        for (FormattedCharSequence seq : font.split(text, w)) {
            g.drawString(font, seq, x, y, color, false);
            y += 10;
        }
        return y;
    }

    private int progress(GuiGraphics g, AltarSurveyPayload.Row r, int x, int y, int w) {
        boolean ok = r.need() == 0 || r.have() >= r.need();
        String count = r.isBoolean() ? (ok ? "✔" : "✘") : r.need() == 0 ? String.valueOf(r.have()) : r.have() + "/" + r.need();
        g.drawString(font, Component.translatable("mythic_age.altar.row." + r.key()), x, y, TEXT, false);
        g.drawString(font, count, x + w - font.width(count), y, ok ? OK : BAD, false);
        y += 10;
        if (r.need() > 0 && !r.isBoolean()) {
            int barW = w;
            int filled = Math.min(barW, (int) Math.round(barW * (double) r.have() / r.need()));
            g.fill(x, y, x + barW, y + 2, 0x40FFFFFF);
            g.fill(x, y, x + filled, y + 2, ok ? OK : BAD);
            y += 5;
        }
        return y;
    }
}
