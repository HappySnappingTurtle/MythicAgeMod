package io.github.happysnappingturtle.mythicage.client.screen;

import io.github.happysnappingturtle.mythicage.village.economy.AffairsPayloads;
import io.github.happysnappingturtle.mythicage.village.economy.ResidentTradeView;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 居民交易界面（T19 设计文档 §1.8 I7）：「出售」「收购」两段，每行物品 × 数量、价格（绿宝石；打折时原价划线）、
 * 村库存量与今日剩余、按钮；不能成交时按钮灰掉并写原因。行多时滚轮翻页（GUI 比例大时屏幕很矮）。与村务界面同一风格。
 */
public class ResidentTradeScreen extends Screen {
    private static final int W = 300;
    private static final int ROW = 20;
    private static final int HEADER = 12;
    private static final int PANEL = 0xF0201A14;
    private static final int BORDER = 0xFFB89A5A;
    private static final int DIM = 0xFFB9B09C;
    private static final int GOLD = 0xFFF2D27A;
    private static final int WARN = 0xFFE59A6A;

    /** 列表的一行：段标题（row 为 null）或一条交易。 */
    private record Line(Component header, ResidentTradeView.Row row) {
        int height() {
            return row == null ? HEADER : ROW;
        }
    }

    private ResidentTradeView view;
    private final List<Line> lines = new ArrayList<>();
    private int left;
    private int top;
    private int h;
    private int listTop;
    private int listBottom;
    private int scroll;

    public ResidentTradeScreen(ResidentTradeView view) {
        super(Component.literal(view.name()));
        this.view = view;
    }

    public void update(ResidentTradeView v) {
        this.view = v;
        rebuildWidgets();
    }

    private void layout() {
        lines.clear();
        for (boolean sell : new boolean[]{true, false}) {
            List<ResidentTradeView.Row> rows = view.rows().stream().filter(r -> r.sell() == sell).toList();
            if (rows.isEmpty()) {
                continue;
            }
            lines.add(new Line(Component.translatable(sell ? "mythic_age.village.trade.sell" : "mythic_age.village.trade.buy"), null));
            rows.forEach(r -> lines.add(new Line(null, r)));
        }
    }

    private int contentHeight() {
        return lines.stream().mapToInt(Line::height).sum();
    }

    /** 从第 scroll 行起能放下的行数。 */
    private int visibleFrom(int from) {
        int y = 0;
        int n = 0;
        for (int i = from; i < lines.size(); i++) {
            y += lines.get(i).height();
            if (y > listBottom - listTop) {
                break;
            }
            n++;
        }
        return n;
    }

    private int maxScroll() {
        for (int s = 0; s < lines.size(); s++) {
            if (s + visibleFrom(s) >= lines.size()) {
                return s;
            }
        }
        return 0;
    }

    @Override
    protected void init() {
        layout();
        h = Math.min(height - 16, 20 + Math.max(HEADER, contentHeight()) + 26);
        left = (width - W) / 2;
        top = (height - h) / 2;
        listTop = top + 20;
        listBottom = top + h - 26;
        scroll = Math.min(scroll, maxScroll());
        int y = listTop;
        int n = visibleFrom(scroll);
        for (int i = scroll; i < scroll + n; i++) {
            Line l = lines.get(i);
            if (l.row() != null) {
                ResidentTradeView.Row r = l.row();
                Component label = Component.translatable(r.sell() ? "mythic_age.village.trade.do_buy" : "mythic_age.village.trade.do_sell");
                Button b = Button.builder(label, x -> PacketDistributor.sendToServer(new AffairsPayloads.Deal(view.resident(), r.id())))
                        .bounds(left + W - 48, y + 2, 40, 16).build();
                b.active = r.state() == ResidentTradeView.State.OK;
                addRenderableWidget(b);
            }
            y += l.height();
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + W - 58, top + h - 22, 50, 16).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderTransparentBackground(g);
        g.fill(left - 1, top - 1, left + W + 1, top + h + 1, BORDER);
        g.fill(left, top, left + W, top + h, PANEL);
        g.drawString(font, Component.literal(view.name()).append(" · ").append(Component.translatable(view.profession())),
                left + 8, top + 7, GOLD, false);
        if (lines.isEmpty()) {
            g.drawString(font, Component.translatable("mythic_age.village.trade.empty"), left + 8, listTop + 2, DIM, false);
        }
        ItemStack hovered = ItemStack.EMPTY;
        int y = listTop;
        int n = visibleFrom(scroll);
        for (int i = scroll; i < scroll + n; i++) {
            Line l = lines.get(i);
            if (l.row() == null) {
                g.drawString(font, l.header(), left + 8, y + 2, GOLD, false);
                y += HEADER;
                continue;
            }
            ResidentTradeView.Row r = l.row();
            ItemStack st = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(r.item())), r.count());
            ItemStack em = new ItemStack(Items.EMERALD, r.price());
            ItemStack give = r.sell() ? em : st;
            ItemStack get = r.sell() ? st : em;
            g.renderItem(give, left + 8, y + 2);
            g.renderItemDecorations(font, give, left + 8, y + 2);
            g.drawString(font, "→", left + 30, y + 6, DIM, false);
            g.renderItem(get, left + 42, y + 2);
            g.renderItemDecorations(font, get, left + 42, y + 2);
            if (r.sell() && r.price() < r.basePrice()) {
                String was = String.valueOf(r.basePrice());
                int wx = left + 26 - font.width(was);
                g.drawString(font, was, wx, y + 1, DIM, false);
                g.fill(wx - 1, y + 4, wx + font.width(was) + 1, y + 5, DIM);
            }
            Component info;
            int color = DIM;
            if (r.state() != ResidentTradeView.State.OK) {
                info = Component.translatable("mythic_age.village.trade.state." + r.state().name().toLowerCase(Locale.ROOT));
                color = WARN;
            } else if (r.sell()) {
                info = (r.stock() < 0 ? Component.translatable("mythic_age.village.trade.import")
                        : Component.translatable("mythic_age.village.trade.stock", r.stock()))
                        .append(" · ").append(Component.translatable("mythic_age.village.trade.remaining", r.remaining()));
            } else {
                info = Component.translatable("mythic_age.village.trade.remaining", r.remaining());
            }
            g.drawString(font, info, left + 66, y + 6, color, false);
            if (mouseY >= y && mouseY < y + 18) {
                if (mouseX >= left + 8 && mouseX < left + 26) {
                    hovered = give;
                } else if (mouseX >= left + 42 && mouseX < left + 60) {
                    hovered = get;
                }
            }
            y += ROW;
        }
        if (maxScroll() > 0) {
            String pos = (scroll + 1) + "/" + (maxScroll() + 1);
            g.drawString(font, "▲▼ " + pos, left + 8, top + h - 18, DIM, false);
        }
        super.render(g, mouseX, mouseY, partial);
        if (!hovered.isEmpty()) {
            g.renderTooltip(font, hovered, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseScrolled(double x, double y, double dx, double dy) {
        int next = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(dy)));
        if (next != scroll) {
            scroll = next;
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
