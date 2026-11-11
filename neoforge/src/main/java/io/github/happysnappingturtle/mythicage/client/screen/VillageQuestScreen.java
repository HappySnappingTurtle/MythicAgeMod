package io.github.happysnappingturtle.mythicage.client.screen;

import io.github.happysnappingturtle.mythicage.village.quest.QuestPayloads;
import io.github.happysnappingturtle.mythicage.village.quest.QuestView;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * 委托页（T21 / GA-7）：标题与进度、状态、可交物品；名称按宽度换行，按钮单独放在条目底部。
 * 底部「你已接 N / 3」「你在本村的贡献：N」。是村务界面的第三个分页（T24 §4.1）。
 */
public class VillageQuestScreen extends Screen {
    private static final int W = VillageAffairsScreen.W;
    private static final int H = VillageAffairsScreen.H;
    private static final int LIST_TOP = 38;
    private static final int LIST_BOTTOM = H - 40;
    private static final int INNER = W - 16;
    private static final int PANEL = 0xF0201A14;
    private static final int BORDER = 0xFFB89A5A;
    private static final int TEXT = 0xFFEDE8DC;
    private static final int DIM = 0xFFB9B09C;
    private static final int GOLD = 0xFFF2D27A;
    private static final int WARN = 0xFFE59A6A;
    private static final int GOOD = 0xFF9AD07A;

    private QuestView view;
    private int left;
    private int top;
    private int scroll;
    private ItemStack hovered = ItemStack.EMPTY;
    private List<Component> hoveredList = List.of();

    public VillageQuestScreen(QuestView view) {
        super(Component.translatable("mythic_age.quest.screen.title"));
        this.view = view;
    }

    public void update(QuestView v) {
        this.view = v;
        scroll = Math.min(scroll, maxScroll());
        rebuildWidgets();
    }

    private int maxScroll() {
        return Math.max(0, view.rows().stream().mapToInt(this::rowHeight).sum() - (LIST_BOTTOM - LIST_TOP));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        scroll = Math.min(scroll, maxScroll());
        int y = top + LIST_TOP - scroll;
        for (QuestView.Row r : view.rows()) {
            int h = rowHeight(r);
            int actions = y + h - 22;
            // 只给完整露出的按钮建立命中区，滚动时不会点到页签或页脚下面的按钮。
            if (actions >= top + LIST_TOP && actions + 16 <= top + LIST_BOTTOM && hasActions(r)) {
                int x = left + W - 8;
                if (r.accepted()) {
                    x = button(x, actions, "abandon", r);
                }
                if (r.canDeliver()) {
                    button(x, actions, "deliver", r);
                } else if (!r.accepted() && r.refusal().isEmpty()) {
                    button(x, actions, "accept", r);
                }
            }
            y += h;
        }
        VillageAffairsScreen.tabs(this::addRenderableWidget, font, left, top, view.village(), VillageAffairsScreen.TAB_QUESTS);
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + W - 58, top + H - 22, 50, 16).build());
    }

    /** 右对齐放一个按钮，返回下一个按钮的右边界。 */
    private int button(int right, int y, String action, QuestView.Row r) {
        Component label = Component.translatable("mythic_age.quest.button." + action);
        int w = font.width(label) + 12;
        addRenderableWidget(Button.builder(label, b -> PacketDistributor.sendToServer(
                        new QuestPayloads.Act(view.village(), r.id(), action, UUID.randomUUID().toString())))
                .bounds(right - w, y, w, 16).build());
        return right - w - 4;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderTransparentBackground(g);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, BORDER);
        g.fill(left, top, left + W, top + H, PANEL);
        g.drawString(font, Component.translatable("mythic_age.quest.screen.heading", VillageAffairsScreen.line(view.title())),
                left + 8, top + 8, GOLD, false);
        if (!view.enabled()) {
            g.drawString(font, Component.translatable("mythic_age.quest.screen.disabled"), left + 8, top + H - 32, WARN, false);
        }
        List<QuestView.Row> rows = view.rows();
        if (rows.isEmpty()) {
            g.drawString(font, Component.translatable("mythic_age.quest.screen.empty"), left + 8, top + 42, DIM, false);
        }
        hovered = ItemStack.EMPTY;
        hoveredList = List.of();
        g.enableScissor(left + 4, top + LIST_TOP, left + W - 4, top + LIST_BOTTOM);
        int y = top + LIST_TOP - scroll;
        for (QuestView.Row r : rows) {
            int h = rowHeight(r);
            if (y + h > top + LIST_TOP && y < top + LIST_BOTTOM) {
                g.fill(left + 4, y, left + W - 4, y + h - 2, 0x30FFFFFF);
                int next = text(g, heading(r), y + 4, r.state().equals("open") ? TEXT : DIM);
                next = text(g, detail(r), next + 2, color(r));
                accepts(g, r, next + 4, mouseX, mouseY);
            }
            y += h;
        }
        g.disableScissor();
        int max = maxScroll();
        if (max > 0) {
            int track = LIST_BOTTOM - LIST_TOP;
            int thumb = Math.max(12, track * track / (track + max));
            int thumbY = top + LIST_TOP + (track - thumb) * scroll / max;
            g.fill(left + W - 3, thumbY, left + W - 1, thumbY + thumb, BORDER);
        }
        g.drawString(font, Component.translatable("mythic_age.quest.screen.footer", view.active(), view.max(), view.contribution()),
                left + 8, top + H - 18, DIM, false);
        super.render(g, mouseX, mouseY, partial);
        if (!hovered.isEmpty()) {
            g.renderTooltip(font, hovered, mouseX, mouseY);
        } else if (!hoveredList.isEmpty()) {
            g.renderComponentTooltip(font, hoveredList, mouseX, mouseY);
        }
    }

    private static boolean food(QuestView.Row r) {
        return r.title().key().equals("mythic_age.quest.title.supply.food");
    }

    private static Component heading(QuestView.Row r) {
        return Component.translatable("mythic_age.quest.screen.progress", VillageAffairsScreen.line(r.title()), r.done(), r.need());
    }

    private static boolean hasActions(QuestView.Row r) {
        return (r.state().equals("open") || r.state().equals("paused"))
                && (r.accepted() || r.canDeliver() || r.refusal().isEmpty());
    }

    /** 测量与绘制共用同一套换行，三语长名称不会盖住按钮。 */
    private int text(GuiGraphics g, Component c, int y, int color) {
        for (var line : font.split(c, INNER)) {
            if (g != null) {
                g.drawString(font, line, left + 8, y, color, false);
            }
            y += font.lineHeight;
        }
        return y;
    }

    private int rowHeight(QuestView.Row r) {
        int y = text(null, heading(r), 4, TEXT);
        y = text(null, detail(r), y + 2, DIM);
        return accepts(null, r, y + 4, 0, 0) + (hasActions(r) ? 24 : 4);
    }

    private static ItemStack stack(QuestView.Accept a) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(a.item())));
    }

    private static Component amount(QuestView.Row r, QuestView.Accept a) {
        String n = BigDecimal.valueOf(a.value()).stripTrailingZeros().toPlainString();
        return Component.translatable(food(r) ? "mythic_age.quest.screen.portions" : "mythic_age.quest.screen.units", n);
    }

    /** 可交清单：口粮全部列出；物品组最多六项，末尾悬停查看完整清单。 */
    private int accepts(GuiGraphics g, QuestView.Row r, int y, int mouseX, int mouseY) {
        if (r.accepts().isEmpty()) {
            return y;
        }
        Component label = Component.translatable("mythic_age.quest.screen.accepts").append(" · ")
                .append(Component.translatable(food(r) ? "mythic_age.quest.screen.remaining_food"
                        : "mythic_age.quest.screen.remaining_items", Math.max(0, r.need() - r.done())));
        y = text(g, label, y, GOLD) + 2;
        int x = left + 8;
        int limit = food(r) ? r.accepts().size() : Math.min(6, r.accepts().size());
        for (int i = 0; i < limit; i++) {
            QuestView.Accept a = r.accepts().get(i);
            ItemStack item = stack(a);
            Component name = item.getHoverName().copy().append(" ").append(amount(r, a));
            int w = Math.min(INNER, 20 + font.width(name));
            if (x > left + 8 && x + w > left + W - 8) {
                x = left + 8;
                y += 18;
            }
            // 单个名称也可能很长，图标保留 16 像素，文字继续换行。
            var lines = font.split(name, INNER - 20);
            int height = Math.max(18, lines.size() * font.lineHeight + 2);
            if (g != null) {
                g.renderItem(item, x, y);
                int ty = y + 3;
                for (var line : lines) {
                    g.drawString(font, line, x + 20, ty, TEXT, false);
                    ty += font.lineHeight;
                }
                if (inside(mouseX, mouseY, x, y, 16, 16)) {
                    hovered = item;
                }
            }
            x += w + 8;
            if (height > 18) {
                x = left + 8;
                y += height;
            }
        }
        if (limit < r.accepts().size()) {
            Component more = Component.translatable("mythic_age.quest.screen.accepts_more", r.accepts().size());
            int w = font.width(more);
            if (x + w > left + W - 8) {
                x = left + 8;
                y += 18;
            }
            if (g != null) {
                g.drawString(font, more, x, y + 3, GOLD, false);
                if (inside(mouseX, mouseY, x, y, w, 16)) {
                    hoveredList = r.accepts().stream().map(a -> (Component) stack(a).getHoverName().copy()
                            .append(" ").append(amount(r, a))).toList();
                }
            }
        }
        return y + 18;
    }

    private boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h
                && my >= top + LIST_TOP && my < top + LIST_BOTTOM;
    }

    /** 第二行：状态（或剩余日）· 范围 · 奖励类别 · 我的份额 / 不能接的原因。 */
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
                c.append(Component.literal("（")).append(VillageAffairsScreen.line(r.reason())).append(Component.literal("）"));
            }
        }
        c.append(Component.literal(" · ")).append(Component.translatable("mythic_age.quest.scope." + r.scope()));
        for (String reward : r.rewards()) {
            c.append(Component.literal(" · ")).append(Component.translatable("mythic_age.quest.reward." + reward));
        }
        if (r.mine() > 0) {
            c.append(Component.literal(" · ")).append(Component.translatable("mythic_age.quest.screen.mine", r.mine()));
        } else if (!r.accepted() && !r.refusal().isEmpty() && r.state().equals("open")
                && !r.refusal().equals("event")) {
            c.append(Component.literal(" · ")).append(Component.translatable("mythic_age.quest.act." + r.refusal()));
        }
        return c;
    }

    private static int color(QuestView.Row r) {
        return switch (r.state()) {
            case "open" -> r.accepted() ? GOLD : DIM;
            case "complete", "settled" -> GOOD;
            case "failed", "expired", "invalid", "paused" -> WARN;
            default -> DIM;
        };
    }

    @Override
    public boolean mouseScrolled(double x, double y, double dx, double dy) {
        int s = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(dy) * 18));
        if (s != scroll) {
            scroll = s;
            rebuildWidgets();
        }
        return true;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
