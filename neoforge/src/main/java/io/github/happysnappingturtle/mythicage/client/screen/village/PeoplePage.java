package io.github.happysnappingturtle.mythicage.client.screen.village;

import io.github.happysnappingturtle.mythicage.village.economy.AffairsView;
import io.github.happysnappingturtle.mythicage.village.screen.VillageScreenPayloads;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 村民分页（GA-12 §3.3）：人口 / 床位、士兵、警报、各岗位人数与空缺（点一行，地图页高亮这些人干活的建筑）、移民进度、协作者。
 */
final class PeoplePage extends Page {
    PeoplePage(VillageScreen s) {
        super(s);
    }

    @Override
    void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        VillageScreenPayloads.People p = s.people;
        int tx = innerX();
        int tw = innerW() - 4;
        int ty = beginScroll(g);
        if (p == null) {
            endScroll(g, text(g, Component.translatable("mythic_age.village.map.loading"), tx, ty, tw, VillageScreen.INK_DIM));
            return;
        }
        ty = heading(g, Component.translatable("mythic_age.village.overview.people"), tx, ty, tw);
        ty = text(g, Component.translatable("mythic_age.village.people.residents", p.residents(), p.beds()), tx, ty, tw, VillageScreen.INK);
        ty = text(g, Component.translatable("mythic_age.village.overview.soldiers", p.soldiers(), p.soldierCap()), tx, ty, tw,
                VillageScreen.INK);
        ty = text(g, VillageScreen.line(p.migration()), tx, ty, tw, VillageScreen.INK_DIM);
        AffairsView a = s.affairs;
        if (a != null && !a.overview().alerts().isEmpty()) {
            ty = heading(g, Component.translatable("mythic_age.village.overview.alerts"), tx, ty + 6, tw);
            for (AffairsView.Line l : a.overview().alerts()) {
                ty = text(g, Component.literal("⚠ ").append(VillageScreen.line(l)), tx, ty, tw, VillageScreen.INK_WARN);
            }
        }
        ty = heading(g, Component.translatable("mythic_age.village.people.jobs"), tx, ty + 6, tw);
        int barW = Math.min(90, tw / 3);
        for (VillageScreenPayloads.Job j : p.jobs()) {
            int rowY = ty;
            boolean hover = mouseX >= tx && mouseX < tx + tw && mouseY >= rowY && mouseY < rowY + 12 && visible(mouseY);
            if (hover) {
                g.fill(tx - 2, rowY - 1, tx + tw, rowY + 11, 0x30FFFFFF);
            }
            Component name = Component.translatable(j.key());
            s.drawClipped(g, name, tx, rowY + 1, tw - barW - 50, VillageScreen.INK);
            int bx = tx + tw - barW;
            if (j.slots() > 0) {
                String count = j.have() + " / " + j.slots();
                int cw = font().width(count);
                g.drawString(font(), count, bx - cw - 6, rowY + 1, j.slots() > j.have() ? VillageScreen.INK_WARN : VillageScreen.INK, false);
                VillageScreen.bar(g, bx, rowY + 2, barW, 6, (double) j.have() / j.slots(),
                        j.slots() > j.have() ? VillageScreen.AMBER : 0xFF5E8A3A);
            } else {
                // 没有固定岗位数的职业（卫兵、基础劳力）：只写人数
                Component count = Component.translatable("mythic_age.village.people.count", j.have());
                g.drawString(font(), count, bx - font().width(count) - 6, rowY + 1, VillageScreen.INK, false);
                g.drawString(font(), Component.translatable("mythic_age.village.people.no_slots"), bx, rowY + 1, VillageScreen.INK_DIM, false);
            }
            if (!j.buildings().isEmpty() && visible(rowY) && visible(rowY + 11)) {
                s.hit(tx - 2, rowY - 1, tw + 2, 12, () -> {
                    s.click();
                    s.mapPage().highlight(j.buildings());
                    s.switchTab(VillageScreenPayloads.TAB_MAP);
                });
                if (hover) {
                    s.tooltip(List.of(Component.translatable("mythic_age.village.people.show_on_map")));
                }
            }
            ty += 13;
        }
        if (p.jobs().isEmpty()) {
            ty = text(g, Component.translatable("mythic_age.village.people.no_jobs"), tx, ty, tw, VillageScreen.INK_DIM);
        }
        ty = heading(g, Component.translatable("mythic_age.village.overview.collaborators"), tx, ty + 6, tw);
        if (p.collaborators().isEmpty()) {
            ty = text(g, Component.translatable("mythic_age.village.overview.no_collaborators"), tx, ty, tw, VillageScreen.INK_DIM);
        }
        for (AffairsView.Count c : p.collaborators()) {
            ty = text(g, Component.translatable("mythic_age.village.overview.collaborator", c.key(), c.n()), tx, ty, tw, VillageScreen.INK);
        }
        endScroll(g, ty + 4);
    }
}
