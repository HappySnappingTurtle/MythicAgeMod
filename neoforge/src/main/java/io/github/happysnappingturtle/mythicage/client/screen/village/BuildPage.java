package io.github.happysnappingturtle.mythicage.client.screen.village;

import io.github.happysnappingturtle.mythicage.core.village.map.VillageMapData;
import io.github.happysnappingturtle.mythicage.village.build.ProjectPayloads;
import io.github.happysnappingturtle.mythicage.village.economy.AffairsView;
import io.github.happysnappingturtle.mythicage.village.screen.VillageScreenPayloads;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 建设分页（GA-12 §3.2）：自上而下是项目卡（名字、进度条、已放 / 总数、工人、被挡格、还缺的材料与「捐出背包里的」）、
 * 「下一个」（谁点名的，清空）、候选列表（每行名字、类别、主要材料、状态、「点名」）、发展记录。
 */
final class BuildPage extends Page {
    BuildPage(VillageScreen s) {
        super(s);
    }

    @Override
    void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        ProjectPayloads.View pv = s.projects;
        int tx = innerX();
        int tw = innerW() - 4;
        int ty = beginScroll(g);
        if (pv == null) {
            ty = text(g, Component.translatable("mythic_age.village.map.loading"), tx, ty, tw, VillageScreen.INK_DIM);
            endScroll(g, ty);
            return;
        }
        ty = projectCard(g, pv, tx, ty, tw, mouseX, mouseY);
        ty = blockers(g, tx, ty + 6, tw);
        ty = next(g, pv, tx, ty + 6, tw, mouseX, mouseY);
        ty = candidates(g, pv, tx, ty + 6, tw, mouseX, mouseY);
        ty = log(g, tx, ty + 6, tw);
        endScroll(g, ty + 4);
    }

    private int projectCard(GuiGraphics g, ProjectPayloads.View pv, int tx, int ty, int tw, int mx, int my) {
        VillageScreenPayloads.Header hd = s.header;
        ty = heading(g, Component.translatable("mythic_age.village.build.current"), tx, ty, tw);
        if (hd.progress() < 0) {
            return text(g, Component.translatable("mythic_age.village.overview.no_project"), tx, ty, tw, VillageScreen.INK_DIM);
        }
        // 升级：旧 → 新（地图上找升级中的那座）
        Optional<VillageMapData.Building> up = s.map.buildings().stream()
                .filter(b -> b.underConstruction() && !b.target().isEmpty()).findFirst();
        MutableComponent title = up.isPresent()
                ? Component.translatable("mythic_age.village.build.upgrade_title", VillageScreen.buildingName(up.get().building(),
                up.get().god()), VillageScreen.buildingName(up.get().target(), ""))
                : Component.translatable("mythic_age.village.build.new_title", VillageScreen.line(hd.project()));
        ty = text(g, title, tx, ty, tw, VillageScreen.INK);
        VillageScreen.bar(g, tx, ty + 1, tw, 7, hd.progress() / 100.0, VillageScreen.AMBER);
        ty += 11;
        ty = text(g, Component.translatable("mythic_age.village.build.cells", pv.placed(), pv.total(), hd.progress(), pv.workers()), tx,
                ty, tw, VillageScreen.INK_DIM);
        if (pv.blocked() > 0) {
            Component blocked = Component.translatable("mythic_age.village.build.blocked", pv.blocked());
            g.drawString(font(), blocked, tx, ty + 3, VillageScreen.INK_WARN, false);
            button(g, tx + font().width(blocked) + 6, ty, Component.translatable("mythic_age.village.build.show_blocked"), true,
                    List.of(Component.translatable("mythic_age.village.build.show_blocked_tip")), mx, my,
                    () -> s.act(VillageScreenPayloads.Act.SHOW_BLOCKED, 0));
            ty += 16;
        }
        if (pv.missing().isEmpty()) {
            ty = text(g, Component.translatable("mythic_age.village.build.stocked"), tx, ty, tw, VillageScreen.INK_GOOD);
        } else {
            ty = text(g, Component.translatable("mythic_age.village.build.missing"), tx, ty, tw, VillageScreen.INK_WARN);
            int n = s.items(g, pv.missing(), tx, ty, tw, mx, my, top(), bottom());
            ty += 18;
            if (n < pv.missing().size()) {
                ty = text(g, Component.translatable("mythic_age.village.build.more", pv.missing().size() - n), tx, ty, tw,
                        VillageScreen.INK_DIM);
            }
            button(g, tx, ty + 1, Component.translatable("mythic_age.village.build.donate_missing"), true,
                    List.of(Component.translatable("mythic_age.village.build.donate_missing_tip")), mx, my,
                    () -> s.act(VillageScreenPayloads.Act.DONATE_MISSING, 0));
            ty += 17;
        }
        return ty;
    }

    /** 受阻（T24 §4.3 三分类，符号加文字前缀区分）：村子自己会解决 / 要稀有材料 / 被占地。 */
    private int blockers(GuiGraphics g, int tx, int ty, int tw) {
        AffairsView a = s.affairs;
        if (a == null || a.blockers().isEmpty()) {
            return ty - 6;
        }
        ty = heading(g, Component.translatable("mythic_age.village.overview.blockers"), tx, ty, tw);
        for (AffairsView.Blocker b : a.blockers()) {
            int color = switch (b.cls()) {
                case "rare_material", "occupied" -> VillageScreen.INK_WARN;
                default -> VillageScreen.INK_DIM;
            };
            ty = text(g, Component.translatable("mythic_age.village.blocker." + b.cls(), VillageScreen.line(b.line())), tx, ty, tw, color);
        }
        return ty;
    }

    private int next(GuiGraphics g, ProjectPayloads.View pv, int tx, int ty, int tw, int mx, int my) {
        ty = heading(g, Component.translatable("mythic_age.village.build.next_heading"), tx, ty, tw);
        boolean can = pv.contribution() >= pv.threshold();
        Component next = pv.nextKey().isEmpty() ? Component.translatable("mythic_age.village.build.next_none")
                : Component.translatable("mythic_age.village.build.next", MapPage.nextName(pv), pv.nextPlayer());
        ty = text(g, next, tx, ty, tw, VillageScreen.INK);
        ty = text(g, Component.translatable("mythic_age.village.build.contribution", pv.contribution(), pv.threshold()), tx, ty, tw,
                can ? VillageScreen.INK_DIM : VillageScreen.INK_WARN);
        if (!pv.nextKey().isEmpty()) {
            List<Component> tip = can ? List.of() : List.of(Component.translatable("mythic_age.village.project.nominate.low",
                    pv.contribution(), pv.threshold(), pv.threshold() - pv.contribution()));
            button(g, tx, ty + 1, Component.translatable("mythic_age.village.build.clear"), can, tip, mx, my,
                    () -> s.send(new ProjectPayloads.Nominate(s.village(), "")));
            ty += 17;
        }
        return ty;
    }

    private int candidates(GuiGraphics g, ProjectPayloads.View pv, int tx, int ty, int tw, int mx, int my) {
        ty = heading(g, Component.translatable("mythic_age.village.build.candidates"), tx, ty, tw);
        if (pv.entries().isEmpty()) {
            return text(g, Component.translatable("mythic_age.village.build.no_candidates"), tx, ty, tw, VillageScreen.INK_DIM);
        }
        boolean can = pv.contribution() >= pv.threshold();
        // 能盖的排前面（按村子自己的排序），不能盖的放后面、保持原顺序
        List<ProjectPayloads.Entry> entries = new ArrayList<>(pv.entries());
        entries.sort(java.util.Comparator.comparing((ProjectPayloads.Entry e) -> !e.reason().isEmpty())
                .thenComparingInt(e -> e.reason().isEmpty() ? e.rank() : 0));
        for (ProjectPayloads.Entry e : entries) {
            int rowTop = ty;
            Component pick = Component.translatable("mythic_age.village.build.nominate");
            int bw = s.buttonWidth(pick);
            MutableComponent name = VillageScreen.line(e.name()).copy().append("  ")
                    .append(Component.translatable("mythic_age.village.build.kind." + e.kind()).withStyle(net.minecraft.ChatFormatting.ITALIC));
            ty = text(g, name, tx, ty, tw - bw - 6, VillageScreen.INK);
            Component status;
            int color;
            if (e.key().equals(pv.nextKey())) {
                status = Component.translatable("mythic_age.village.build.nominated");
                color = VillageScreen.INK_GOOD;
            } else if (!e.reason().isEmpty()) {
                status = Component.translatable("mythic_age.village.project.reason." + e.reason());
                color = VillageScreen.INK_WARN;
            } else {
                status = Component.translatable("mythic_age.village.build.rank", e.rank() + 1);
                color = VillageScreen.INK_DIM;
            }
            ty = text(g, status, tx, ty, tw - bw - 6, color);
            if (!e.materials().isEmpty()) {
                s.items(g, e.materials(), tx, ty, tw - bw - 6, mx, my, top(), bottom());
                ty += 18;
            }
            boolean ok = can && e.reason().isEmpty() && !e.key().equals(pv.nextKey());
            List<Component> tip = new ArrayList<>();
            if (!can) {
                tip.add(Component.translatable("mythic_age.village.project.nominate.low", pv.contribution(), pv.threshold(),
                        pv.threshold() - pv.contribution()));
            } else if (!e.reason().isEmpty()) {
                tip.add(status);
            }
            button(g, tx + tw - bw, rowTop, pick, ok, tip, mx, my, () -> s.send(new ProjectPayloads.Nominate(s.village(), e.key())));
            g.fill(tx, ty + 1, tx + tw, ty + 2, 0x308A6A3A);
            ty += 4;
        }
        return ty;
    }

    private int log(GuiGraphics g, int tx, int ty, int tw) {
        ty = heading(g, Component.translatable("mythic_age.village.affairs.log"), tx, ty, tw);
        AffairsView a = s.affairs;
        if (a == null || a.log().isEmpty()) {
            return text(g, Component.translatable("mythic_age.village.build.no_log"), tx, ty, tw, VillageScreen.INK_DIM);
        }
        for (AffairsView.Line l : a.log()) {
            ty = text(g, VillageScreen.line(l), tx, ty, tw, VillageScreen.INK);
        }
        return ty;
    }
}
