package io.github.happysnappingturtle.mythicage.client.screen.village;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 村庄界面的一个分页。内容区是一张羊皮纸；需要滚动的分页用 {@link #beginScroll} / {@link #endScroll} 包住内容：
 * 内容按「滚动后的 y」往下画，超出部分裁掉，右边画滚动条。
 */
abstract class Page {
    final VillageScreen s;
    int x;
    int y;
    int w;
    int h;
    int scroll;
    private int contentHeight;

    Page(VillageScreen s) {
        this.s = s;
    }

    void layout(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    /** 切到这一页时。 */
    void shown() {
    }

    abstract void render(GuiGraphics g, int mouseX, int mouseY, float partial);

    Font font() {
        return s.getMinecraft().font;
    }

    int innerX() {
        return x + 8;
    }

    int innerW() {
        return w - 16;
    }

    int top() {
        return y + 6;
    }

    int bottom() {
        return y + h - 6;
    }

    /** 开始画滚动内容：返回内容起点的 y（已减去滚动量）。 */
    int beginScroll(GuiGraphics g) {
        scroll = Math.max(0, Math.min(scroll, Math.max(0, contentHeight - (bottom() - top()))));
        g.enableScissor(x + 2, top(), x + w - 2, bottom());
        return top() - scroll;
    }

    /** 结束：记下内容总高（下一帧用来夹滚动量），画滚动条。endY 是内容最后一行之后的 y。 */
    void endScroll(GuiGraphics g, int endY) {
        g.disableScissor();
        contentHeight = endY + scroll - top();
        int view = bottom() - top();
        int max = contentHeight - view;
        if (max > 0) {
            int thumb = Math.max(12, view * view / contentHeight);
            int ty = top() + (view - thumb) * scroll / max;
            g.fill(x + w - 5, top(), x + w - 3, bottom(), 0x30000000);
            g.fill(x + w - 5, ty, x + w - 3, ty + thumb, 0xFF8A6A3A);
        }
    }

    boolean mouseScrolled(double mx, double my, double dy) {
        if (mx >= x && mx < x + w && my >= y && my < y + h) {
            scroll = Math.max(0, scroll - (int) Math.signum(dy) * 20);
            return true;
        }
        return false;
    }

    boolean mouseClicked(double mx, double my, int button) {
        return false;
    }

    boolean mouseReleased(double mx, double my, int button) {
        return false;
    }

    boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        return false;
    }

    /** 小标题：深棕色，下面一条细线。返回下一行的 y。 */
    int heading(GuiGraphics g, Component c, int x0, int y0, int width) {
        g.drawString(font(), c, x0, y0, VillageScreen.INK_HEAD, false);
        g.fill(x0, y0 + 10, x0 + width, y0 + 11, 0x608A6A3A);
        return y0 + 14;
    }

    int text(GuiGraphics g, Component c, int x0, int y0, int width, int color) {
        return s.text(g, c, x0, y0, width, color);
    }

    int button(GuiGraphics g, int bx, int by, Component label, boolean active, List<Component> tip, int mx, int my, Runnable action) {
        return s.button(g, bx, by, label, active, tip, mx, my, top(), bottom(), action);
    }

    boolean visible(int my) {
        return my >= top() && my < bottom();
    }
}
