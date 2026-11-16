package io.github.happysnappingturtle.mythicage.client.screen.village;

import io.github.happysnappingturtle.mythicage.village.economy.AffairsPayloads;
import io.github.happysnappingturtle.mythicage.village.economy.AffairsView;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 仓库分页（GA-12 §3.4，原「库存与捐献」）：容量、口粮、库存格（左上角金点 = 有托管）、缺口、「捐出主手物品」（要在谷仓管事身边）。
 */
final class StockPage extends Page {
    private static final int CELL = 24;

    StockPage(VillageScreen s) {
        super(s);
    }

    @Override
    void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        AffairsView a = s.affairs;
        int tx = innerX();
        int tw = innerW() - 4;
        int ty = beginScroll(g);
        if (a == null) {
            endScroll(g, text(g, Component.translatable("mythic_age.village.map.loading"), tx, ty, tw, VillageScreen.INK_DIM));
            return;
        }
        ty = text(g, Component.translatable("mythic_age.village.affairs.capacity", a.total(), a.capacity()), tx, ty, tw, VillageScreen.INK);
        if (!a.overview().food().key().isEmpty()) {
            ty = text(g, VillageScreen.line(a.overview().food()), tx, ty, tw, VillageScreen.INK);
        }
        boolean keeper = s.header.atKeeper();
        Component donate = Component.translatable("mythic_age.village.affairs.donate");
        button(g, tx, ty + 2, donate, keeper, keeper ? List.of() : List.of(Component.translatable("mythic_age.village.screen.at_keeper")),
                mouseX, mouseY, () -> s.send(new AffairsPayloads.Donate(s.village())));
        int below = ty + 20;
        if (!keeper) {
            Component hint = Component.translatable("mythic_age.village.screen.at_keeper");
            int hx = tx + s.buttonWidth(donate) + 6;
            if (hx + font().width(hint) <= tx + tw) {
                g.drawString(font(), hint, hx, ty + 5, VillageScreen.INK_DIM, false);
            } else {
                // 按钮右边放不下（英文、GUI 缩放 4）：写到按钮下面
                below = text(g, hint, tx, ty + 18, tw, VillageScreen.INK_DIM) + 2;
            }
        }
        ty = below;
        int gridW = Math.max(CELL, Math.min(tw, (tw * 3) / 5));
        int cols = Math.max(1, gridW / CELL);
        int gx = tx;
        int gy = ty;
        List<AffairsView.Entry> stock = a.stock();
        for (int i = 0; i < stock.size(); i++) {
            AffairsView.Entry e = stock.get(i);
            int cx = gx + (i % cols) * CELL;
            int cy = gy + (i / cols) * CELL;
            g.fill(cx, cy, cx + CELL - 2, cy + CELL - 2, 0x40FFFFFF);
            g.renderOutline(cx, cy, CELL - 2, CELL - 2, 0x408A6A3A);
            ItemStack st = VillageScreen.stack(e.item());
            g.renderItem(st, cx + 3, cy + 2);
            String n = e.count() >= 1000 ? (e.count() / 1000) + "k" : String.valueOf(e.count());
            // 羊皮纸底色浅，原版的白字带影看不清，改用墨色
            g.drawString(font(), n, cx + CELL - 3 - font().width(n), cy + CELL - 10, VillageScreen.INK, false);
            if (e.escrowed() > 0) {
                g.fill(cx + 1, cy + 1, cx + 5, cy + 5, 0xFFF2D27A);
            }
            if (mouseX >= cx && mouseX < cx + CELL - 2 && mouseY >= cy && mouseY < cy + CELL - 2 && visible(mouseY)) {
                s.tooltip(List.of(st.getHoverName(), Component.translatable("mythic_age.village.affairs.count", e.count(), e.escrowed())));
            }
        }
        int gridBottom = gy + ((stock.size() + cols - 1) / cols) * CELL;
        if (stock.isEmpty()) {
            gridBottom = text(g, Component.translatable("mythic_age.village.affairs.empty"), gx, gy, gridW, VillageScreen.INK_DIM);
        }
        int rx = gx + cols * CELL + 8;
        int rw = tx + tw - rx;
        int ry = heading(g, Component.translatable("mythic_age.village.affairs.shortages"), rx, gy, rw);
        if (a.blockers().isEmpty()) {
            ry = text(g, Component.translatable("mythic_age.village.affairs.no_shortage"), rx, ry, rw, VillageScreen.INK_DIM);
        }
        for (AffairsView.Blocker b : a.blockers()) {
            int color = b.cls().equals("self_resolving") ? VillageScreen.INK_DIM : VillageScreen.INK_WARN;
            ry = text(g, Component.translatable("mythic_age.village.blocker." + b.cls(), VillageScreen.line(b.line())), rx, ry, rw, color);
        }
        endScroll(g, Math.max(gridBottom, ry) + 4);
    }
}
