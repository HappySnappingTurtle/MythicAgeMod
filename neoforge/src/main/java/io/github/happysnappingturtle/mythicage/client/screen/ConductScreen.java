package io.github.happysnappingturtle.mythicage.client.screen;

import io.github.happysnappingturtle.mythicage.conduct.ConductView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import javax.annotation.Nullable;

/**
 * 「善恶与贡献」（T24 设计文档 §3.2）：左栏善恶值（分值、档名、带刻度与档名的档位条、今日正分、最近 10 条），
 * 右栏本人有贡献的村庄（累计与最近 3 条来源），底部固定说明「贡献只是记录，不能兑换」。只显示自己的数据（D6）。
 */
public class ConductScreen extends Screen {
    private static final int W = 400;
    private static final int H = 236;
    private static final int PANEL = 0xF0201A14;
    private static final int BORDER = 0xFFB89A5A;
    private static final int TEXT = 0xFFEDE8DC;
    private static final int DIM = 0xFFB9B09C;
    private static final int GOLD = 0xFFF2D27A;
    private static final int GOOD = 0xFF9AD07A;
    private static final int BAD = 0xFFE59A6A;

    private ConductView view;
    @Nullable
    private final Screen previous;
    private int left;
    private int top;

    public ConductScreen(ConductView view, @Nullable Screen previous) {
        super(Component.translatable("mythic_age.conduct.screen.title"));
        this.view = view;
        this.previous = previous;
    }

    public void update(ConductView v) {
        this.view = v;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + W - 58, top + H - 22, 50, 16).build());
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(previous);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderTransparentBackground(g);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, BORDER);
        g.fill(left, top, left + W, top + H, PANEL);
        g.drawString(font, title, left + 8, top + 7, GOLD, false);
        int half = (W - 24) / 2;
        int lx = left + 8;
        int rx = left + 16 + half;
        g.fill(rx - 5, top + 20, rx - 4, top + H - 30, 0x60B89A5A);
        renderConduct(g, lx, top + 22, half);
        renderVillages(g, rx, top + 22, half);
        for (FormattedCharSequence seq : font.split(Component.translatable("mythic_age.conduct.screen.note"), W - 76)) {
            g.drawString(font, seq, left + 8, top + H - 20, DIM, false);
            break;
        }
        super.render(g, mouseX, mouseY, partial);
    }

    private void renderConduct(GuiGraphics g, int x, int y, int w) {
        g.drawString(font, Component.translatable("mythic_age.conduct.screen.score_header"), x, y, GOLD, false);
        y += 12;
        if (!view.enabled()) {
            g.drawString(font, Component.translatable("mythic_age.conduct.screen.disabled"), x, y, DIM, false);
            return;
        }
        String sign = view.score() > 0 ? "+" : "";
        Component score = Component.literal(sign + view.score() + "  ")
                .append(Component.translatable("mythic_age.conduct.tier." + view.tier()));
        g.drawString(font, score, x, y, view.score() > 0 ? GOOD : view.score() < 0 ? BAD : TEXT, false);
        y += 13;
        y = bar(g, x, y, w);
        g.drawString(font, Component.translatable("mythic_age.conduct.screen.today", view.positiveToday(), view.positiveCap()),
                x, y, DIM, false);
        y += 14;
        g.drawString(font, Component.translatable("mythic_age.conduct.screen.recent"), x, y, GOLD, false);
        y += 11;
        if (view.recent().isEmpty()) {
            g.drawString(font, Component.translatable("mythic_age.conduct.screen.no_events"), x, y, DIM, false);
        }
        for (ConductView.Event e : view.recent()) {
            if (y > top + H - 40) {
                break;
            }
            Component line = Component.literal((e.delta() > 0 ? "+" : "") + e.delta() + "  ")
                    .append(Component.translatable("mythic_age.conduct.reason." + e.reason()))
                    .append(Component.literal(" · ")).append(ago(e.daysAgo()));
            g.drawString(font, font.split(line, w).get(0), x, y, e.delta() > 0 ? GOOD : BAD, false);
            y += 10;
        }
    }

    /** 档位条：五段，分界处画刻度，当前位置画一个倒三角；条下写当前档名（不只靠颜色）。 */
    private int bar(GuiGraphics g, int x, int y, int w) {
        int bw = w - 4;
        g.fill(x, y, x + bw, y + 6, 0xFF3A322A);
        int range = view.max() - view.min();
        if (range <= 0) {
            return y + 10;
        }
        int pos = x + (int) Math.round((double) (view.score() - view.min()) / range * bw);
        int zero = x + (int) Math.round((double) -view.min() / range * bw);
        if (pos > zero) {
            g.fill(zero, y + 1, pos, y + 5, GOOD);
        } else if (pos < zero) {
            g.fill(pos, y + 1, zero, y + 5, BAD);
        }
        // 刻度：0 与正负两边的分档线（同 ConductDisplay：15% 与一半）
        int[] marks = {view.min() / 2, (int) Math.floor(view.min() * 0.15), 0, (int) Math.ceil(view.max() * 0.15), view.max() / 2};
        for (int m : marks) {
            int mx = x + (int) Math.round((double) (m - view.min()) / range * bw);
            g.fill(mx, y - 1, mx + 1, y + 7, m == 0 ? TEXT : DIM);
        }
        g.drawString(font, "▼", pos - font.width("▼") / 2, y - 9, GOLD, false);
        return y + 12;
    }

    private void renderVillages(GuiGraphics g, int x, int y, int w) {
        g.drawString(font, Component.translatable("mythic_age.conduct.screen.villages"), x, y, GOLD, false);
        y += 12;
        if (view.villages().isEmpty()) {
            for (FormattedCharSequence seq : font.split(Component.translatable("mythic_age.conduct.screen.no_villages"), w)) {
                g.drawString(font, seq, x, y, DIM, false);
                y += 10;
            }
            return;
        }
        for (ConductView.Village v : view.villages()) {
            if (y > top + H - 50) {
                break;
            }
            Component head = io.github.happysnappingturtle.mythicage.village.economy.AffairsView.toComponent(v.title()).copy().append("  " + v.points());
            g.drawString(font, font.split(head, w).get(0), x, y, TEXT, false);
            y += 11;
            for (ConductView.Event e : v.recent()) {
                Component line = Component.translatable("mythic_age.conduct.screen.contribution_line",
                        Component.translatable("mythic_age.contribution.reason." + e.reason()), e.delta(), ago(e.daysAgo()));
                g.drawString(font, font.split(line, w - 8).get(0), x + 8, y, DIM, false);
                y += 10;
            }
            y += 4;
        }
    }

    private static Component ago(long days) {
        return days <= 0 ? Component.translatable("mythic_age.conduct.screen.today_short")
                : Component.translatable("mythic_age.conduct.screen.days_ago", days);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
