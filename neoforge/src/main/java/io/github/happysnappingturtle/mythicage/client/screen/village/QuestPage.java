package io.github.happysnappingturtle.mythicage.client.screen.village;

import io.github.happysnappingturtle.mythicage.village.quest.QuestPayloads;
import io.github.happysnappingturtle.mythicage.village.quest.QuestView;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * 委托分页（GA-12 §3.5，原委托页 T21 / GA-7 搬过来）：每条写标题与进度、状态、可交物品；按钮在条目底部右侧。
 * 接取、交付、放弃要在谷仓管事身边（T21），不在时页底提示。
 */
final class QuestPage extends Page {
    QuestPage(VillageScreen s) {
        super(s);
    }

    @Override
    void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        QuestView v = s.quests;
        int tx = innerX();
        int tw = innerW() - 4;
        int ty = beginScroll(g);
        if (v == null) {
            endScroll(g, text(g, Component.translatable("mythic_age.village.map.loading"), tx, ty, tw, VillageScreen.INK_DIM));
            return;
        }
        if (s.affairs != null) {
            ty = text(g, Component.translatable("mythic_age.village.overview.quests", s.affairs.overview().quests(),
                    s.affairs.overview().publicQuests()), tx, ty, tw, VillageScreen.INK);
        }
        ty = text(g, Component.translatable("mythic_age.quest.screen.footer", v.active(), v.max(), v.contribution()), tx, ty, tw,
                VillageScreen.INK_DIM);
        if (!s.header.atKeeper()) {
            ty = text(g, Component.translatable("mythic_age.village.screen.quest_keeper"), tx, ty, tw, VillageScreen.INK_DIM);
        }
        if (!v.enabled()) {
            ty = text(g, Component.translatable("mythic_age.quest.screen.disabled"), tx, ty, tw, VillageScreen.INK_WARN);
        }
        if (v.rows().isEmpty()) {
            ty = text(g, Component.translatable("mythic_age.quest.screen.empty"), tx, ty + 4, tw, VillageScreen.INK_DIM);
        }
        for (QuestView.Row r : v.rows()) {
            ty += 4;
            int rowTop = ty;
            ty = text(g, heading(r), tx + 2, ty + 2, tw - 4, r.state().equals("open") ? VillageScreen.INK : VillageScreen.INK_DIM);
            ty = text(g, detail(r), tx + 2, ty + 1, tw - 4, color(r));
            ty = accepts(g, r, tx + 2, ty + 3, tw - 4, mouseX, mouseY);
            if (hasActions(r)) {
                int right = tx + tw - 2;
                if (r.accepted()) {
                    right = action(g, right, ty, "abandon", r, mouseX, mouseY);
                }
                if (r.canDeliver()) {
                    action(g, right, ty, "deliver", r, mouseX, mouseY);
                } else if (!r.accepted() && r.refusal().isEmpty()) {
                    action(g, right, ty, "accept", r, mouseX, mouseY);
                }
                ty += 18;
            }
            g.renderOutline(tx, rowTop, tw, ty - rowTop + 2, 0x608A6A3A);
            ty += 2;
        }
        endScroll(g, ty + 4);
    }

    /** 右对齐放一个按钮，返回下一个按钮的右边界。 */
    private int action(GuiGraphics g, int right, int y, String action, QuestView.Row r, int mx, int my) {
        Component label = Component.translatable("mythic_age.quest.button." + action);
        int bw = s.buttonWidth(label);
        button(g, right - bw, y, label, true, List.of(), mx, my,
                () -> s.send(new QuestPayloads.Act(s.village(), r.id(), action, UUID.randomUUID().toString())));
        return right - bw - 4;
    }

    private static boolean food(QuestView.Row r) {
        return r.title().key().equals("mythic_age.quest.title.supply.food");
    }

    private static Component heading(QuestView.Row r) {
        return Component.translatable("mythic_age.quest.screen.progress", VillageScreen.line(r.title()), r.done(), r.need());
    }

    private static boolean hasActions(QuestView.Row r) {
        return (r.state().equals("open") || r.state().equals("paused"))
                && (r.accepted() || r.canDeliver() || r.refusal().isEmpty());
    }

    private static Component amount(QuestView.Row r, QuestView.Accept a) {
        String n = BigDecimal.valueOf(a.value()).stripTrailingZeros().toPlainString();
        return Component.translatable(food(r) ? "mythic_age.quest.screen.portions" : "mythic_age.quest.screen.units", n);
    }

    /** 可交清单：口粮全部列出；物品组最多六项，末尾悬停查看完整清单（GA-7）。 */
    private int accepts(GuiGraphics g, QuestView.Row r, int x0, int y, int width, int mx, int my) {
        if (r.accepts().isEmpty()) {
            return y;
        }
        Component label = Component.translatable("mythic_age.quest.screen.accepts").append(" · ")
                .append(Component.translatable(food(r) ? "mythic_age.quest.screen.remaining_food"
                        : "mythic_age.quest.screen.remaining_items", Math.max(0, r.need() - r.done())));
        y = text(g, label, x0, y, width, VillageScreen.INK_HEAD) + 2;
        int x = x0;
        int limit = food(r) ? r.accepts().size() : Math.min(6, r.accepts().size());
        for (int i = 0; i < limit; i++) {
            QuestView.Accept a = r.accepts().get(i);
            ItemStack item = VillageScreen.stack(a.item());
            Component name = item.getHoverName().copy().append(" ").append(amount(r, a));
            int iw = Math.min(width, 20 + font().width(name));
            if (x > x0 && x + iw > x0 + width) {
                x = x0;
                y += 18;
            }
            var lines = font().split(name, width - 20);
            int ih = Math.max(18, lines.size() * 10 + 2);
            g.renderItem(item, x, y);
            int ly = y + 4;
            for (var l : lines) {
                g.drawString(font(), l, x + 20, ly, VillageScreen.INK, false);
                ly += 10;
            }
            if (mx >= x && mx < x + 16 && my >= y && my < y + 16 && visible(my)) {
                s.tooltip(item);
            }
            x += iw + 8;
            if (ih > 18) {
                x = x0;
                y += ih;
            }
        }
        if (limit < r.accepts().size()) {
            Component more = Component.translatable("mythic_age.quest.screen.accepts_more", r.accepts().size());
            int mw = font().width(more);
            if (x + mw > x0 + width) {
                x = x0;
                y += 18;
            }
            g.drawString(font(), more, x, y + 4, VillageScreen.INK_HEAD, false);
            if (mx >= x && mx < x + mw && my >= y && my < y + 16 && visible(my)) {
                s.tooltip(r.accepts().stream().map(a -> (Component) VillageScreen.stack(a.item()).getHoverName().copy()
                        .append(" ").append(amount(r, a))).toList());
            }
        }
        return y + 18;
    }

    /** 第二行：来源 · 状态（或剩余日）· 范围 · 奖励类别 · 我的份额 / 不能接的原因。 */
    private static Component detail(QuestView.Row r) {
        MutableComponent c = Component.empty();
        if (!r.sourceKey().isEmpty()) {
            c.append(Component.translatable(r.sourceKey())).append(Component.literal(" · "));
        }
        if (r.state().equals("open")) {
            c.append(r.daysLeft() >= 0 ? Component.translatable("mythic_age.quest.screen.days", r.daysLeft())
                    : Component.translatable("mythic_age.quest.state.open"));
        } else {
            c.append(Component.translatable("mythic_age.quest.state." + r.state()));
            if (!r.reason().key().isEmpty()) {
                c.append(Component.literal("（")).append(VillageScreen.line(r.reason())).append(Component.literal("）"));
            }
        }
        c.append(Component.literal(" · ")).append(Component.translatable("mythic_age.quest.scope." + r.scope()));
        for (String reward : r.rewards()) {
            c.append(Component.literal(" · ")).append(Component.translatable("mythic_age.quest.reward." + reward));
        }
        if (r.mine() > 0) {
            c.append(Component.literal(" · ")).append(Component.translatable("mythic_age.quest.screen.mine", r.mine()));
        } else if (!r.accepted() && !r.refusal().isEmpty() && r.state().equals("open") && !r.refusal().equals("event")) {
            c.append(Component.literal(" · ")).append(Component.translatable("mythic_age.quest.act." + r.refusal()));
        }
        return c;
    }

    private static int color(QuestView.Row r) {
        return switch (r.state()) {
            case "open" -> r.accepted() ? VillageScreen.INK_HEAD : VillageScreen.INK_DIM;
            case "complete", "settled" -> VillageScreen.INK_GOOD;
            case "failed", "expired", "invalid", "paused" -> VillageScreen.INK_WARN;
            default -> VillageScreen.INK_DIM;
        };
    }
}
