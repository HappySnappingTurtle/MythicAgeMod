package io.github.happysnappingturtle.mythicage.client.screen;

import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.rite.AgeUnlockedPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 晋升成功界面（12 号 §4.3-A；T11a 设计文档 §7.2）：顶部文明与新时代、一句引言；中部四栏——神力、召唤单位、
 * 配方与建筑、上限变化；底部「下一时代需要」（只预告）。关闭后回到之前的界面。
 */
public class AgeUnlockedScreen extends Screen {
    private static final int W = 400;
    private static final int H = 230;
    private static final int PANEL = 0xF0201A14;
    private static final int BORDER = 0xFFB89A5A;
    private static final int TEXT = 0xFFEDE8DC;
    private static final int DIM = 0xFFB9B09C;
    private static final int GOLD = 0xFFF2D27A;
    private static final int OK = 0xFF7FD67F;

    private final AgeUnlockedPayload p;
    @Nullable
    private final Screen previous;
    private int left;
    private int top;

    public AgeUnlockedScreen(AgeUnlockedPayload payload, @Nullable Screen previous) {
        super(Component.translatable("mythic_age.age_unlocked.title",
                Component.translatable("mythic_age.civ." + payload.civ().id()),
                Component.translatable("mythic_age.age." + payload.age().id())));
        this.p = payload;
        this.previous = previous;
    }

    /** 打开成功界面之前的界面（关闭后回到它）。 */
    @Nullable
    public Screen previous() {
        return previous;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + W / 2 - 40, top + H - 26, 80, 20).build());
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(previous);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        g.fill(left, top, left + W, top + H, PANEL);
        g.renderOutline(left, top, W, H, BORDER);
        g.renderOutline(left + 2, top + 2, W - 4, H - 4, 0x60B89A5A);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title, left + W / 2, top + 10, GOLD);
        Component quote = Component.translatable("mythic_age.age_unlocked." + p.civ().id() + "." + p.age().id() + ".quote");
        int y = centered(g, quote, top + 24, W - 40, DIM);
        if (!p.minorGod().isEmpty()) {
            g.drawCenteredString(font, Component.translatable("mythic_age.age_unlocked.god",
                    Component.translatable("mythic_age.god." + p.minorGod())), left + W / 2, y + 2, TEXT);
        }
        int colW = (W - 20) / 4;
        int cy = top + 62;
        column(g, mouseX, mouseY, left + 10, cy, colW, "powers", p.powers());
        column(g, mouseX, mouseY, left + 10 + colW, cy, colW, "summons", p.summons());
        List<ItemStack> crafts = new java.util.ArrayList<>(p.recipes());
        p.extras().forEach(e -> crafts.add(e.icon()));
        column(g, mouseX, mouseY, left + 10 + colW * 2, cy, colW, "recipes", crafts);
        int x = left + 10 + colW * 3;
        g.drawString(font, Component.translatable("mythic_age.age_unlocked.caps"), x, cy, BORDER, false);
        int yy = cy + 12;
        for (AgeUnlockedPayload.Cap c : p.caps()) {
            Component line = Component.translatable("mythic_age.age_unlocked.cap." + c.key()).copy()
                    .append(" " + c.before() + " → " + c.after());
            yy = wrap(g, line, x, yy, colW - 4, OK);
        }
        for (AgeUnlockedPayload.Extra e : p.extras()) {
            yy = wrap(g, Component.translatable(e.key()), x, yy, colW - 4, TEXT);
        }
        // 下一时代
        int ny = top + H - 54;
        g.fill(left + 8, ny - 4, left + W - 8, ny - 3, 0x60B89A5A);
        p.next().ifPresentOrElse(n -> {
            int nx = left + 10;
            Component head = Component.translatable("mythic_age.age_unlocked.next",
                    Component.translatable("mythic_age.age." + n.age().id()), n.favor());
            g.drawString(font, head, nx, ny, BORDER, false);
            nx += font.width(head) + 6;
            for (ItemStack s : n.offerings()) {
                g.renderItem(s, nx, ny - 4);
                g.renderItemDecorations(font, s, nx, ny - 4);
                nx += 18;
            }
            if (n.trial() != Age.NONE) {
                Component trial = Component.translatable("mythic_age.age_unlocked.next_trial",
                        Component.translatable("mythic_age.trial." + n.trial().id()));
                g.drawString(font, trial, nx + 4, ny, DIM, false);
            }
            // 建筑条件（如神庙等级）另起一行，只预告不判定
            int rx = left + 10;
            for (Component r : n.requirements()) {
                Component line = Component.literal("+ ").append(r);
                g.drawString(font, line, rx, ny + 14, DIM, false);
                rx += font.width(line) + 10;
            }
        }, () -> g.drawString(font, Component.translatable("mythic_age.age_unlocked.top"), left + 10, ny, DIM, false));
    }

    private void column(GuiGraphics g, int mouseX, int mouseY, int x, int y, int w, String key, List<ItemStack> items) {
        g.drawString(font, Component.translatable("mythic_age.age_unlocked." + key), x, y, BORDER, false);
        if (items.isEmpty()) {
            g.drawString(font, "—", x, y + 14, DIM, false);
            return;
        }
        int ix = x;
        int iy = y + 12;
        for (ItemStack s : items) {
            if (ix + 16 > x + w) {
                ix = x;
                iy += 18;
            }
            g.renderItem(s, ix, iy);
            if (mouseX >= ix && mouseX < ix + 16 && mouseY >= iy && mouseY < iy + 16) {
                g.renderTooltip(font, s, mouseX, mouseY);
            }
            ix += 18;
        }
    }

    private int centered(GuiGraphics g, Component text, int y, int w, int color) {
        for (FormattedCharSequence seq : font.split(text, w)) {
            g.drawString(font, seq, left + (W - font.width(seq)) / 2, y, color, false);
            y += 10;
        }
        return y;
    }

    private int wrap(GuiGraphics g, Component text, int x, int y, int w, int color) {
        for (FormattedCharSequence seq : font.split(text, w)) {
            g.drawString(font, seq, x, y, color, false);
            y += 10;
        }
        return y;
    }
}
