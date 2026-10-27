package io.github.happysnappingturtle.mythicage.client.screen;

import io.github.happysnappingturtle.mythicage.village.economy.AffairsPayloads;
import io.github.happysnappingturtle.mythicage.village.economy.AffairsView;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.UUID;

/**
 * 村务界面（T19 设计文档 §4.3；T24 §4 改为三个分页）：
 * <ul>
 *   <li>总览：人口与岗位、警报 / 口粮、在建、协作者 / 委托摘要与受阻三分类，底部发展记录与「显示村界」；</li>
 *   <li>库存与捐献：库存格、缺口、「捐出主手物品」；</li>
 *   <li>委托：切到 {@link VillageQuestScreen}。</li>
 * </ul>
 * 受阻用符号 ● ○ ■ 加文字前缀区分，不只靠颜色（12 号 §6）。
 */
public class VillageAffairsScreen extends Screen {
    static final int W = 400;
    static final int H = 236;
    private static final int COLS = 6;
    private static final int CELL = 26;
    private static final int ROWS = 6;
    static final int PANEL = 0xF0201A14;
    static final int BORDER = 0xFFB89A5A;
    static final int TEXT = 0xFFEDE8DC;
    static final int DIM = 0xFFB9B09C;
    static final int GOLD = 0xFFF2D27A;
    static final int WARN = 0xFFE59A6A;
    static final int TAB_QUESTS = 2;

    private AffairsView view;
    private int left;
    private int top;
    private int scroll;

    public VillageAffairsScreen(AffairsView view) {
        super(Component.translatable("mythic_age.village.affairs.title"));
        this.view = view;
    }

    public void update(AffairsView v) {
        boolean tabChanged = v.tab() != view.tab();
        this.view = v;
        scroll = Math.min(scroll, maxScroll());
        if (tabChanged) {
            rebuildWidgets();
        }
    }

    private int maxScroll() {
        return Math.max(0, (view.stock().size() + COLS - 1) / COLS - ROWS);
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        tabs(this::addRenderableWidget, font, left, top, view.village(), view.tab());
        if (view.tab() == AffairsView.TAB_STOCK) {
            Component donate = Component.translatable("mythic_age.village.affairs.donate");
            addRenderableWidget(Button.builder(donate, b -> PacketDistributor.sendToServer(new AffairsPayloads.Donate(view.village())))
                    .bounds(left + 8, top + H - 22, font.width(donate) + 16, 16).build());
        } else {
            Component bounds = Component.translatable("mythic_age.village.overview.bounds");
            addRenderableWidget(Button.builder(bounds, b -> {
                PacketDistributor.sendToServer(new AffairsPayloads.BoundsRequest(view.village()));
                onClose();
            }).bounds(left + 8, top + H - 22, font.width(bounds) + 16, 16).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + W - 58, top + H - 22, 50, 16).build());
    }

    /** 三个分页按钮（委托页也用）：当前页不可点。 */
    static void tabs(java.util.function.Consumer<Button> add, Font font, int left, int top, UUID village, int current) {
        int x = left + 8;
        String[] keys = {"overview", "stock", "quests"};
        for (int i = 0; i < keys.length; i++) {
            Component label = Component.translatable("mythic_age.village.tab." + keys[i]);
            int tab = i;
            Button b = Button.builder(label, btn -> PacketDistributor.sendToServer(tab == TAB_QUESTS
                    ? new io.github.happysnappingturtle.mythicage.village.quest.QuestPayloads.Open(village)
                    : new AffairsPayloads.Open(village, tab))).bounds(x, top + 19, font.width(label) + 14, 14).build();
            b.active = i != current;
            add.accept(b);
            x += font.width(label) + 18;
        }
    }

    /**
     * 参数以 {@code @} 开头的当语言键翻译；以 {@code @@} 开头的当语言键、并把后面所有参数都交给它（记录行：「第 N 日：」+ 一句带参数的话）。
     */
    static Component line(AffairsView.Line l) {
        return AffairsView.toComponent(l);
    }

    static Component blocker(AffairsView.Blocker b) {
        return Component.translatable("mythic_age.village.blocker." + b.cls(), line(b.line()));
    }

    static int blockerColor(AffairsView.Blocker b) {
        return switch (b.cls()) {
            case "rare_material" -> WARN;
            case "occupied" -> 0xFFE8B04A;
            default -> DIM;
        };
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderTransparentBackground(g);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, BORDER);
        g.fill(left, top, left + W, top + H, PANEL);
        MutableComponent title = line(view.title()).copy();
        if (!view.overview().stage().equals("none")) {
            title.append(" · ").append(Component.translatable("mythic_age.age." + view.overview().stage()));
        }
        g.drawString(font, title, left + 8, top + 7, GOLD, false);
        Component date = line(view.date());
        g.drawString(font, date, left + W - 8 - font.width(date), top + 7, DIM, false);
        if (!view.notes().isEmpty()) {
            MutableComponent notes = Component.empty();
            for (int i = 0; i < view.notes().size(); i++) {
                notes.append(i == 0 ? Component.empty() : Component.literal(" · ")).append(line(view.notes().get(i)));
            }
            g.drawString(font, notes, left + W - 8 - font.width(notes), top + 22, GOLD, false);
        }
        if (view.tab() == AffairsView.TAB_STOCK) {
            renderStock(g, mouseX, mouseY, partial);
        } else {
            renderOverview(g);
            super.render(g, mouseX, mouseY, partial);
        }
    }

    // ───────────── 总览 ─────────────

    private void renderOverview(GuiGraphics g) {
        AffairsView.Overview o = view.overview();
        int colW = (W - 16 - 16) / 3;
        int y0 = top + 40;
        int limit = top + H - 76;
        // 左：人口与岗位、警报
        int x = left + 8;
        int y = header(g, "mythic_age.village.overview.people", x, y0);
        y = text(g, Component.translatable("mythic_age.village.overview.residents", o.residents(), o.freeBeds()), x, y, colW, TEXT, limit);
        y = text(g, Component.translatable("mythic_age.village.overview.soldiers", o.soldiers(), o.soldierCap()), x, y, colW, TEXT, limit);
        for (int i = 0; i < o.jobs().size() && i < 5; i++) {
            AffairsView.Count c = o.jobs().get(i);
            y = text(g, Component.translatable("mythic_age.village.overview.job", Component.translatable(c.key()), c.n()), x, y, colW,
                    DIM, limit);
        }
        if (!o.alerts().isEmpty()) {
            y = header(g, "mythic_age.village.overview.alerts", x, y + 4);
            for (AffairsView.Line l : o.alerts()) {
                y = text(g, Component.literal("⚠ ").append(line(l)), x, y, colW, WARN, limit);
            }
        }
        // 中：口粮、在建、协作者
        x = left + 8 + colW + 8;
        y = header(g, "mythic_age.village.overview.stock", x, y0);
        if (!o.food().key().isEmpty()) {
            y = text(g, line(o.food()), x, y, colW, TEXT, limit);
        }
        y = text(g, Component.translatable("mythic_age.village.affairs.capacity", view.total(), view.capacity()), x, y, colW, DIM, limit);
        if (o.project().isEmpty()) {
            y = text(g, Component.translatable("mythic_age.village.overview.no_project"), x, y, colW, DIM, limit);
        }
        for (AffairsView.Line l : o.project()) {
            y = text(g, line(l), x, y, colW, TEXT, limit);
        }
        y = header(g, "mythic_age.village.overview.collaborators", x, y + 4);
        if (o.collaborators().isEmpty()) {
            y = text(g, Component.translatable("mythic_age.village.overview.no_collaborators"), x, y, colW, DIM, limit);
        }
        for (AffairsView.Count c : o.collaborators()) {
            y = text(g, Component.translatable("mythic_age.village.overview.collaborator", c.key(), c.n()), x, y, colW, TEXT, limit);
        }
        // 右：委托摘要、受阻
        x = left + 8 + (colW + 8) * 2;
        y = header(g, "mythic_age.village.overview.needs", x, y0);
        y = text(g, Component.translatable("mythic_age.village.overview.quests", o.quests(), o.publicQuests()), x, y, colW, TEXT, limit);
        y = header(g, "mythic_age.village.overview.blockers", x, y + 4);
        if (view.blockers().isEmpty()) {
            y = text(g, Component.translatable("mythic_age.village.affairs.no_shortage"), x, y, colW, DIM, limit);
        }
        for (AffairsView.Blocker b : view.blockers()) {
            y = text(g, blocker(b), x, y, colW, blockerColor(b), limit);
        }
        // 底：发展记录
        int ly = top + H - 70;
        g.fill(left + 8, ly - 4, left + W - 8, ly - 3, 0x60B89A5A);
        ly = header(g, "mythic_age.village.affairs.log", left + 8, ly);
        for (AffairsView.Line l : view.log()) {
            ly = text(g, line(l), left + 8, ly, W - 16, TEXT, top + H - 26);
        }
    }

    private int header(GuiGraphics g, String key, int x, int y) {
        g.drawString(font, Component.translatable(key), x, y, GOLD, false);
        return y + 11;
    }

    /** 自动折行；超过 limit 就不再画。返回下一行的 y。 */
    private int text(GuiGraphics g, Component c, int x, int y, int w, int color, int limit) {
        for (FormattedCharSequence seq : font.split(c, w)) {
            if (y > limit) {
                return y;
            }
            g.drawString(font, seq, x, y, color, false);
            y += 10;
        }
        return y;
    }

    // ───────────── 库存与捐献 ─────────────

    private void renderStock(GuiGraphics g, int mouseX, int mouseY, float partial) {
        g.drawString(font, Component.translatable("mythic_age.village.affairs.capacity", view.total(), view.capacity()),
                left + 8, top + 38, DIM, false);
        int gx = left + 8;
        int gy = top + 50;
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
        int rw = W - (rx - left) - 8;
        int ry = header(g, "mythic_age.village.affairs.shortages", rx, top + 38);
        if (view.blockers().isEmpty()) {
            ry = text(g, Component.translatable("mythic_age.village.affairs.no_shortage"), rx, ry, rw, DIM, top + H - 30);
        }
        for (AffairsView.Blocker b : view.blockers()) {
            ry = text(g, blocker(b), rx, ry, rw, blockerColor(b), top + H - 30);
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
        if (view.tab() == AffairsView.TAB_STOCK) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(dy)));
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
