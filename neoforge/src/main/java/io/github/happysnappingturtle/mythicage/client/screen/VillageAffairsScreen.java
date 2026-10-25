package io.github.happysnappingturtle.mythicage.client.screen;

import io.github.happysnappingturtle.mythicage.village.economy.AffairsPayloads;
import io.github.happysnappingturtle.mythicage.village.economy.AffairsView;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * 村务界面（T19 设计文档 §4.3）：左边库存格（图标、数量、托管中），右边缺口与发展记录，底部「捐出主手物品」。
 * 统一的村庄总览在 T24 做，这里只做能用的列表界面（与召唤管理界面同一风格）。
 */
public class VillageAffairsScreen extends Screen {
    private static final int W = 380;
    private static final int H = 220;
    private static final int COLS = 6;
    private static final int CELL = 26;
    private static final int ROWS = 6;
    private static final int PANEL = 0xF0201A14;
    private static final int BORDER = 0xFFB89A5A;
    private static final int TEXT = 0xFFEDE8DC;
    private static final int DIM = 0xFFB9B09C;
    private static final int GOLD = 0xFFF2D27A;
    private static final int WARN = 0xFFE59A6A;

    private AffairsView view;
    private int left;
    private int top;
    private int scroll;

    public VillageAffairsScreen(AffairsView view) {
        super(Component.translatable("mythic_age.village.affairs.title"));
        this.view = view;
    }

    public void update(AffairsView v) {
        this.view = v;
        scroll = Math.min(scroll, maxScroll());
    }

    private int maxScroll() {
        return Math.max(0, (view.stock().size() + COLS - 1) / COLS - ROWS);
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        Component donate = Component.translatable("mythic_age.village.affairs.donate");
        addRenderableWidget(Button.builder(donate, b -> PacketDistributor.sendToServer(new AffairsPayloads.Donate(view.village())))
                .bounds(left + 8, top + H - 22, font.width(donate) + 16, 16).build());
        // 委托页（T21 §9.1）
        Component quests = Component.translatable("mythic_age.quest.screen.open");
        addRenderableWidget(Button.builder(quests, b -> PacketDistributor.sendToServer(
                        new io.github.happysnappingturtle.mythicage.village.quest.QuestPayloads.Open(view.village())))
                .bounds(left + 8 + font.width(donate) + 20, top + H - 22, font.width(quests) + 16, 16).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + W - 58, top + H - 22, 50, 16).build());
    }

    /**
     * 参数以 {@code @} 开头的当语言键翻译；以 {@code @@} 开头的当语言键、并把后面所有参数都交给它（记录行：「第 N 日：」+ 一句带参数的话）。
     */
    static Component line(AffairsView.Line l) {
        return Component.translatable(l.key(), args(l.args()));
    }

    private static Object[] args(List<String> in) {
        List<Object> out = new java.util.ArrayList<>();
        for (int i = 0; i < in.size(); i++) {
            String a = in.get(i);
            if (a.startsWith("@@")) {
                out.add(Component.translatable(a.substring(2), args(in.subList(i + 1, in.size()))));
                break;
            }
            out.add(a.startsWith("@") ? Component.translatable(a.substring(1)) : a);
        }
        return out.toArray();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderTransparentBackground(g);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, BORDER);
        g.fill(left, top, left + W, top + H, PANEL);
        g.drawString(font, line(view.title()), left + 8, top + 7, GOLD, false);
        Component date = line(view.date());
        g.drawString(font, date, left + W - 8 - font.width(date), top + 7, DIM, false);
        g.drawString(font, Component.translatable("mythic_age.village.affairs.capacity", view.total(), view.capacity()),
                left + 8, top + 20, DIM, false);
        if (!view.notes().isEmpty()) {
            net.minecraft.network.chat.MutableComponent notes = Component.empty();
            for (int i = 0; i < view.notes().size(); i++) {
                notes.append(i == 0 ? Component.empty() : Component.literal(" · ")).append(line(view.notes().get(i)));
            }
            g.drawString(font, notes, left + W - 8 - font.width(notes), top + 20, GOLD, false);
        }
        int gx = left + 8;
        int gy = top + 34;
        List<AffairsView.Entry> stock = view.stock();
        ItemStack hovered = ItemStack.EMPTY;
        AffairsView.Entry hoveredEntry = null;
        for (int i = scroll * COLS; i < stock.size() && i < (scroll + ROWS) * COLS; i++) {
            AffairsView.Entry e = stock.get(i);
            int cx = gx + (i % COLS) * CELL;
            int cy = gy + (i / COLS - scroll) * CELL;
            g.fill(cx, cy, cx + CELL - 2, cy + CELL - 2, 0x40FFFFFF);
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(e.item()));
            ItemStack st = new ItemStack(item);
            g.renderItem(st, cx + 4, cy + 2);
            String n = e.count() >= 1000 ? (e.count() / 1000) + "k" : String.valueOf(e.count());
            g.drawString(font, n, cx + CELL - 3 - font.width(n), cy + CELL - 11, TEXT, true);
            if (e.escrowed() > 0) {
                g.fill(cx + 1, cy + 1, cx + 5, cy + 5, GOLD);
            }
            if (mouseX >= cx && mouseX < cx + CELL - 2 && mouseY >= cy && mouseY < cy + CELL - 2) {
                hovered = st;
                hoveredEntry = e;
            }
        }
        if (stock.isEmpty()) {
            g.drawString(font, Component.translatable("mythic_age.village.affairs.empty"), gx, gy + 4, DIM, false);
        }
        int rx = left + 8 + COLS * CELL + 10;
        int ry = top + 34;
        int rw = W - (rx - left) - 8;
        g.drawString(font, Component.translatable("mythic_age.village.affairs.shortages"), rx, ry, GOLD, false);
        ry += 12;
        if (view.shortages().isEmpty()) {
            g.drawString(font, Component.translatable("mythic_age.village.affairs.no_shortage"), rx, ry, DIM, false);
            ry += 11;
        }
        for (AffairsView.Line l : view.shortages()) {
            for (var seq : font.split(line(l), rw)) {
                g.drawString(font, seq, rx, ry, WARN, false);
                ry += 10;
            }
        }
        ry += 6;
        g.drawString(font, Component.translatable("mythic_age.village.affairs.log"), rx, ry, GOLD, false);
        ry += 12;
        for (AffairsView.Line l : view.log()) {
            for (var seq : font.split(line(l), rw)) {
                if (ry > top + H - 30) {
                    break;
                }
                g.drawString(font, seq, rx, ry, TEXT, false);
                ry += 10;
            }
        }
        super.render(g, mouseX, mouseY, partial);
        if (hoveredEntry != null) {
            g.renderComponentTooltip(font, List.of(hovered.getHoverName(),
                    Component.translatable("mythic_age.village.affairs.count", hoveredEntry.count(), hoveredEntry.escrowed())),
                    mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseScrolled(double x, double y, double dx, double dy) {
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(dy)));
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
