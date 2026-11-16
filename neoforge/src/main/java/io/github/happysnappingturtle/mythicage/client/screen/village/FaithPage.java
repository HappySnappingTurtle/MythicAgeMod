package io.github.happysnappingturtle.mythicage.client.screen.village;

import io.github.happysnappingturtle.mythicage.village.screen.VillageScreenPayloads;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 信仰分页（GA-12 §3.6，GA-13 的内容）：神庙等级、完好度、系数；主神与次神祠；你是不是这里的供奉者；今天为你祈祷的信徒与恩宠；
 * 献供；你在这个村子的圣物供奉位（点圣物取回，「放入主手圣物」）；「不再供奉此村」（点两次确认，GA-13 D3）。
 */
final class FaithPage extends Page {
    private static final int SLOT = 22;
    private long confirmUntil;

    FaithPage(VillageScreen s) {
        super(s);
    }

    @Override
    void shown() {
        confirmUntil = 0;
    }

    @Override
    void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        VillageScreenPayloads.Faith f = s.faith;
        int tx = innerX();
        int tw = innerW() - 4;
        int ty = beginScroll(g);
        if (f == null) {
            endScroll(g, text(g, Component.translatable("mythic_age.village.map.loading"), tx, ty, tw, VillageScreen.INK_DIM));
            return;
        }
        ty = heading(g, Component.translatable("mythic_age.village.faith.temple"), tx, ty, tw);
        if (!f.present()) {
            ty = text(g, Component.translatable("mythic_age.village.faith.no_temple"), tx, ty, tw, VillageScreen.INK_DIM);
        } else {
            ty = text(g, Component.translatable("mythic_age.village.faith.level", f.level(), Component.translatable(f.tierKey()),
                    String.format(java.util.Locale.ROOT, "%.2f", f.coefficient())), tx, ty, tw, VillageScreen.INK);
            boolean low = f.level() > 0 && f.integrity() < 80;
            ty = text(g, Component.translatable("mythic_age.village.faith.integrity", f.integrity()), tx, ty, tw,
                    low ? VillageScreen.INK_WARN : VillageScreen.INK);
            if (low) {
                ty = text(g, Component.translatable("mythic_age.village.faith.integrity_low"), tx, ty, tw, VillageScreen.INK_WARN);
            }
        }
        ty = heading(g, Component.translatable("mythic_age.village.faith.gods"), tx, ty + 6, tw);
        if (!f.patron().isEmpty()) {
            ty = text(g, Component.translatable("mythic_age.village.faith.patron", Component.translatable("mythic_age.god." + f.patron())),
                    tx, ty, tw, VillageScreen.INK);
        }
        if (f.shrines().isEmpty()) {
            ty = text(g, Component.translatable("mythic_age.village.faith.no_shrines"), tx, ty, tw, VillageScreen.INK_DIM);
        } else {
            MutableComponent list = Component.empty();
            for (int i = 0; i < f.shrines().size(); i++) {
                var sh = f.shrines().get(i);
                if (i > 0) {
                    list.append(Component.translatable("mythic_age.village.faith.sep"));
                }
                list.append(Component.translatable(sh.built() ? "mythic_age.village.faith.shrine_built" : "mythic_age.village.faith.shrine_planned",
                        Component.translatable("mythic_age.god." + sh.god())));
            }
            ty = text(g, Component.translatable("mythic_age.village.faith.shrines", list), tx, ty, tw, VillageScreen.INK);
        }
        if (!f.present()) {
            endScroll(g, ty + 4);
            return;
        }
        ty = heading(g, Component.translatable("mythic_age.village.faith.you"), tx, ty + 6, tw);
        if (!f.devotee()) {
            ty = text(g, Component.translatable("mythic_age.village.faith.not_devotee", f.devotees()), tx, ty, tw, VillageScreen.INK_DIM);
            endScroll(g, ty + 4);
            return;
        }
        ty = text(g, Component.translatable("mythic_age.village.faith.devotee", f.devotees()), tx, ty, tw, VillageScreen.INK_GOOD);
        ty = text(g, Component.translatable("mythic_age.village.faith.today", f.believers(),
                String.format(java.util.Locale.ROOT, "%.1f", f.favor())), tx, ty, tw, VillageScreen.INK);
        ty = text(g, Component.translatable(f.offered() ? "mythic_age.altar.offered" : "mythic_age.altar.offer_hint",
                "+" + f.offerBonus() + "%"), tx, ty, tw, f.offered() ? VillageScreen.INK_GOOD : VillageScreen.INK_DIM);
        // 圣物供奉位
        ty = heading(g, Component.translatable("mythic_age.village.faith.relics", f.relics().size(), f.relicCap()), tx, ty + 6, tw);
        int sx = tx;
        for (int i = 0; i < Math.max(f.relicCap(), f.relics().size()); i++) {
            if (sx + SLOT > tx + tw) {
                sx = tx;
                ty += SLOT + 2;
            }
            g.fill(sx, ty, sx + SLOT - 2, ty + SLOT - 2, 0x40FFFFFF);
            g.renderOutline(sx, ty, SLOT - 2, SLOT - 2, 0x808A6A3A);
            if (i < f.relics().size()) {
                ItemStack st = f.relics().get(i);
                g.renderItem(st, sx + 2, ty + 2);
                boolean hover = mouseX >= sx && mouseX < sx + SLOT - 2 && mouseY >= ty && mouseY < ty + SLOT - 2 && visible(mouseY);
                if (hover) {
                    g.renderOutline(sx, ty, SLOT - 2, SLOT - 2, 0xFFFFFFFF);
                    s.tooltip(List.of(st.getHoverName(), Component.translatable("mythic_age.village.faith.retrieve")));
                }
                if (visible(ty) && visible(ty + SLOT - 2)) {
                    int slot = i;
                    s.hit(sx, ty, SLOT - 2, SLOT - 2, () -> {
                        s.click();
                        s.act(VillageScreenPayloads.Act.RELIC_RETRIEVE, slot);
                    });
                }
            }
            sx += SLOT;
        }
        ty += SLOT + 2;
        boolean room = f.relics().size() < f.relicCap();
        int bw = button(g, tx, ty, Component.translatable("mythic_age.village.faith.offer_relic"), room,
                room ? List.of(Component.translatable("mythic_age.village.faith.offer_relic_tip"))
                        : List.of(Component.translatable("mythic_age.relic.full", f.relics().size(), f.relicCap())),
                mouseX, mouseY, () -> s.act(VillageScreenPayloads.Act.RELIC_OFFER, 0));
        // 退出供奉：点两次确认
        boolean confirming = System.currentTimeMillis() < confirmUntil;
        Component leave = Component.translatable(confirming ? "mythic_age.village.faith.leave_confirm" : "mythic_age.village.faith.leave");
        button(g, tx + bw + 8, ty, leave, true, List.of(Component.translatable("mythic_age.village.faith.leave_tip")), mouseX, mouseY,
                () -> {
                    if (System.currentTimeMillis() < confirmUntil) {
                        confirmUntil = 0;
                        s.act(VillageScreenPayloads.Act.LEAVE, 0);
                    } else {
                        confirmUntil = System.currentTimeMillis() + 4000;
                    }
                });
        endScroll(g, ty + 20);
    }
}
