package io.github.happysnappingturtle.mythicage.client.screen.village;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.village.map.MapDelta;
import io.github.happysnappingturtle.mythicage.core.village.map.VillageMapData;
import io.github.happysnappingturtle.mythicage.village.build.ProjectPayloads;
import io.github.happysnappingturtle.mythicage.village.economy.AffairsView;
import io.github.happysnappingturtle.mythicage.village.quest.QuestView;
import io.github.happysnappingturtle.mythicage.village.screen.VillageScreenPayloads;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 村庄界面（GA-12 方案 A「告示牌墙」）：顶部状态栏（村名、时代·主神·神庙等级，警报 / 在建 / 口粮三块），左侧一列六块木牌
 * （地图、建设、村民、仓库、委托、信仰），右边羊皮纸内容区随木牌切换。各分页的数据由服务端推送（{@code ClientHooks}），
 * 切木牌只在本地换页，不必再请求。
 * <p>
 * 按 GUI 缩放后不小于 400 × 230 设计；内容超出时内容区内滚动，外框不滚。按钮是界面自己画的木签（{@link #button}），
 * 每帧登记命中区，点击时按上一帧的位置判断。
 */
public class VillageScreen extends Screen {
    static final int MIN_W = 400;
    static final int MIN_H = 230;
    static final int MAX_W = 560;
    static final int MAX_H = 320;
    static final int STATUS_H = 26;
    /** 木牌宽度：按最长的标签算（{@link #init}），夹在这个范围里。 */
    static final int BOARD_MIN_W = 64;
    static final int BOARD_MAX_W = 100;
    static final int PAD = 6;

    // 羊皮纸上的颜色
    static final int INK = 0xFF3B2A1A;
    static final int INK_DIM = 0xFF6B5A3A;
    static final int INK_HEAD = 0xFF6B3F06;
    static final int INK_WARN = 0xFFA0401A;
    static final int INK_GOOD = 0xFF2F6B2A;
    static final int AMBER = 0xFFB0701A;
    // 木头上的颜色
    static final int CREAM = 0xFFF4E7C9;
    static final int CREAM_DIM = 0xFFE2CFA6;

    static final ResourceLocation FRAME = sprite("frame");
    static final ResourceLocation STATUS = sprite("status");
    static final ResourceLocation BOARD = sprite("board");
    static final ResourceLocation BOARD_SELECTED = sprite("board_selected");
    static final ResourceLocation BOARD_HOVER = sprite("board_hover");
    static final ResourceLocation PARCHMENT = sprite("parchment");
    static final ResourceLocation INSET = sprite("inset");
    static final ResourceLocation BUTTON = sprite("button");
    static final ResourceLocation BUTTON_HOVER = sprite("button_hover");
    static final ResourceLocation BUTTON_DISABLED = sprite("button_disabled");

    static final String[] TAB_KEYS = {"map", "build", "people", "stock", "quests", "faith"};

    private final UUID village;
    private int tab;
    VillageScreenPayloads.Header header;
    AffairsView affairs;
    QuestView quests;
    ProjectPayloads.View projects;
    VillageScreenPayloads.People people;
    VillageScreenPayloads.Faith faith;
    VillageMapData map = VillageMapData.EMPTY;
    VillageScreenPayloads.Detail detail;

    int left;
    int top;
    int w;
    int h;
    int boardW = BOARD_MIN_W;
    private final Page[] pages;

    /** 一个可点的区域（按钮、木牌、状态块、物品格）。 */
    record Hit(int x, int y, int w, int h, Runnable action) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private final List<Hit> hits = new ArrayList<>();
    private final List<Hit> pending = new ArrayList<>();
    private List<Component> tooltip = List.of();
    private ItemStack tooltipItem = ItemStack.EMPTY;

    public VillageScreen(UUID village, int tab, VillageScreenPayloads.Header header) {
        super(Component.translatable("mythic_age.village.screen.title"));
        this.village = village;
        this.tab = Math.max(0, Math.min(VillageScreenPayloads.TABS - 1, tab));
        this.header = header;
        this.pages = new Page[]{new MapPage(this), new BuildPage(this), new PeoplePage(this), new StockPage(this), new QuestPage(this),
                new FaithPage(this)};
    }

    private static ResourceLocation sprite(String name) {
        return ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "village/" + name);
    }

    public UUID village() {
        return village;
    }

    public int tab() {
        return tab;
    }

    // ───────────── 数据 ─────────────

    public void header(VillageScreenPayloads.Header h) {
        this.header = h;
    }

    public void switchTab(int t) {
        if (t >= 0 && t < VillageScreenPayloads.TABS && t != tab) {
            tab = t;
            pages[tab].shown();
        }
    }

    public void affairs(AffairsView v) {
        this.affairs = v;
    }

    public void quests(QuestView v) {
        this.quests = v;
    }

    public void projects(ProjectPayloads.View v) {
        this.projects = v;
    }

    public void people(VillageScreenPayloads.People p) {
        this.people = p;
    }

    public void faith(VillageScreenPayloads.Faith f) {
        this.faith = f;
    }

    public void detail(VillageScreenPayloads.Detail d) {
        this.detail = d;
    }

    public void map(VillageScreenPayloads.MapPacket p) {
        try {
            if (p.full()) {
                map = VillageMapData.decode(p.data());
            } else {
                map = MapDelta.decode(p.data(), p.centerX(), p.centerZ()).apply(map);
            }
        } catch (RuntimeException e) {
            io.github.happysnappingturtle.mythicage.MythicAge.LOGGER.warn("村庄地图数据读不了：{}", e.getMessage());
        }
    }

    MapPage mapPage() {
        return (MapPage) pages[VillageScreenPayloads.TAB_MAP];
    }

    // ───────────── 布局 ─────────────

    @Override
    protected void init() {
        w = Math.max(Math.min(MIN_W, width - 8), Math.min(MAX_W, width - 16));
        h = Math.max(Math.min(MIN_H, height - 8), Math.min(MAX_H, height - 16));
        left = (width - w) / 2;
        top = (height - h) / 2;
        int widest = 0;
        for (int t = 0; t < VillageScreenPayloads.TABS; t++) {
            widest = Math.max(widest, font.width(boardLabel(t)));
        }
        boardW = Math.max(BOARD_MIN_W, Math.min(BOARD_MAX_W, widest + 24));
        for (Page p : pages) {
            p.layout(contentX(), contentY(), contentW(), contentH());
        }
    }

    int contentX() {
        return left + PAD + boardW + PAD;
    }

    int contentY() {
        return top + STATUS_H + PAD;
    }

    int contentW() {
        return left + w - PAD - contentX();
    }

    int contentH() {
        return top + h - PAD - contentY();
    }

    // ───────────── 绘制 ─────────────

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderTransparentBackground(g);
        pending.clear();
        tooltip = List.of();
        tooltipItem = ItemStack.EMPTY;
        g.blitSprite(FRAME, left, top, w, h);
        renderStatus(g, mouseX, mouseY);
        renderBoards(g, mouseX, mouseY);
        g.blitSprite(PARCHMENT, contentX(), contentY(), contentW(), contentH());
        pages[tab].render(g, mouseX, mouseY, partial);
        // 右上角关闭
        Component x = Component.literal("×");
        int cx = left + w - 12;
        int cy = top + 3;
        boolean hover = mouseX >= cx - 2 && mouseX < cx + 9 && mouseY >= cy - 1 && mouseY < cy + 10;
        g.drawString(font, x, cx, cy, hover ? 0xFFFFFFFF : CREAM_DIM, false);
        pending.add(new Hit(cx - 2, cy - 1, 11, 11, this::onClose));
        hits.clear();
        hits.addAll(pending);
        if (!tooltipItem.isEmpty()) {
            g.renderTooltip(font, tooltipItem, mouseX, mouseY);
        } else if (!tooltip.isEmpty()) {
            g.renderComponentTooltip(font, tooltip, mouseX, mouseY);
        }
    }

    private void renderStatus(GuiGraphics g, int mouseX, int mouseY) {
        int x = left + PAD;
        int y = top + 4;
        int sw = w - PAD * 2 - 12;
        g.blitSprite(STATUS, x, y, sw, STATUS_H - 6);
        VillageScreenPayloads.Header hd = header;
        // 右边三块：口粮、在建、警报（从右往左排）
        int right = x + sw - 3;
        int by = y + 3;
        int cap = Math.max(60, (sw - 120) / 3);
        MutableComponent food = hd.floor() >= 0
                ? Component.translatable("mythic_age.village.screen.food", hd.food(), hd.floor())
                : Component.translatable("mythic_age.village.screen.food_none");
        right = statusBlock(g, right, by, cap, food, hd.floor() >= 0 && hd.food() < hd.floor() ? 0xFF7A2A1A : 0xFF3F3A2C,
                VillageScreenPayloads.TAB_STOCK, mouseX, mouseY);
        if (hd.floor() >= 0 && mouseX >= right + 4 && mouseX < x + sw && mouseY >= by && mouseY < by + 14) {
            tooltip = List.of(Component.translatable("mythic_age.village.overview.food", hd.food(), hd.floor()));
        }
        MutableComponent build = hd.progress() >= 0
                ? Component.translatable("mythic_age.village.screen.building", line(hd.project()), hd.progress())
                : Component.translatable("mythic_age.village.screen.idle");
        right = statusBlock(g, right, by, cap, build, hd.progress() >= 0 ? 0xFF6B4A12 : 0xFF3F3A2C, VillageScreenPayloads.TAB_BUILD,
                mouseX, mouseY);
        boolean alarm = !hd.alert().key().isEmpty();
        MutableComponent alert = alarm ? Component.literal("⚠ ").append(line(hd.alert()))
                : Component.translatable("mythic_age.village.screen.no_alert");
        int alertTab = VillageScreenPayloads.TAB_PEOPLE;
        for (int t = 0; t < VillageScreenPayloads.TABS; t++) {
            if (hd.alerted(t)) {
                alertTab = t;
                break;
            }
        }
        right = statusBlock(g, right, by, cap, alert, alarm ? 0xFF7A2A1A : 0xFF2F5D3A, alertTab, mouseX, mouseY);
        // 左边村名与副标题
        int tw = right - x - 10;
        // 副标题由长到短试三种写法（英文在 GUI 缩放 4 下放不下全称），都放不下才截断，悬停看全文
        MutableComponent full = subtitle(hd, true, true);
        Component sub = full;
        for (MutableComponent c : List.of(full, subtitle(hd, true, false), subtitle(hd, false, false))) {
            sub = c;
            if (font.width(c) <= tw) {
                break;
            }
        }
        drawClipped(g, line(hd.title()), x + 6, y + 2, tw, CREAM);
        drawClipped(g, sub, x + 6, y + 11, tw, CREAM_DIM);
        if (sub != full && mouseX >= x && mouseX < x + 6 + tw && mouseY >= y && mouseY < y + 20) {
            tooltip = List.of(line(hd.title()), full);
        }
    }

    /** 状态栏副标题：时代 · 主神 · 神庙等级；withAge 为假时省掉时代，withTier 为假时神庙只写等级数。 */
    private static MutableComponent subtitle(VillageScreenPayloads.Header hd, boolean withAge, boolean withTier) {
        List<Component> parts = new ArrayList<>();
        if (withAge) {
            parts.add(Component.translatable("mythic_age.age." + hd.stage()));
        }
        if (!hd.patron().isEmpty()) {
            parts.add(Component.translatable("mythic_age.god." + hd.patron()));
        }
        if (!hd.tierKey().isEmpty()) {
            parts.add(withTier ? Component.translatable("mythic_age.village.screen.temple_level", hd.templeLevel(),
                    Component.translatable(hd.tierKey()))
                    : Component.translatable("mythic_age.village.screen.temple_short", hd.templeLevel()));
        }
        MutableComponent out = Component.empty();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                out.append(" · ");
            }
            out.append(parts.get(i));
        }
        return out;
    }

    /** 状态块：右对齐、超宽截断，点了跳到对应分页。返回左边界。 */
    private int statusBlock(GuiGraphics g, int right, int y, int cap, Component text, int bg, int target, int mouseX, int mouseY) {
        int tw = Math.min(cap - 8, font.width(text));
        int bw = tw + 8;
        int x = right - bw;
        boolean hover = mouseX >= x && mouseX < right && mouseY >= y && mouseY < y + 14;
        g.fill(x, y, right, y + 14, hover ? brighten(bg) : bg);
        g.renderOutline(x, y, bw, 14, 0x80000000);
        drawClipped(g, text, x + 4, y + 3, tw, 0xFFFFF6E0);
        pending.add(new Hit(x, y, bw, 14, () -> {
            click();
            switchTab(target);
        }));
        if (hover && font.width(text) > tw) {
            tooltip = List.of(text);
        }
        return x - 4;
    }

    private static int brighten(int c) {
        int r = Math.min(255, ((c >> 16) & 0xFF) + 24);
        int gg = Math.min(255, ((c >> 8) & 0xFF) + 24);
        int b = Math.min(255, (c & 0xFF) + 24);
        return 0xFF000000 | r << 16 | gg << 8 | b;
    }

    private void renderBoards(GuiGraphics g, int mouseX, int mouseY) {
        int x = left + PAD;
        int y0 = contentY();
        int gap = 4;
        int bh = Math.min(30, (contentH() - gap * (VillageScreenPayloads.TABS - 1)) / VillageScreenPayloads.TABS);
        for (int t = 0; t < VillageScreenPayloads.TABS; t++) {
            int y = y0 + t * (bh + gap);
            boolean hover = mouseX >= x && mouseX < x + boardW && mouseY >= y && mouseY < y + bh;
            g.blitSprite(t == tab ? BOARD_SELECTED : hover ? BOARD_HOVER : BOARD, x, y, boardW, bh);
            int ty = y + (bh - 8) / 2;
            drawClipped(g, boardLabel(t), x + 7, ty, boardW - 20, t == tab ? 0xFFFFF4DC : 0xFFF0E2C2);
            if (header.alerted(t)) {
                // 有警报：木牌右端画一个「!」，不只靠颜色
                g.fill(x + boardW - 13, ty - 2, x + boardW - 4, ty + 10, 0xFFB03A1A);
                g.drawString(font, "!", x + boardW - 10, ty, 0xFFFFFFFF, false);
            }
            int target = t;
            pending.add(new Hit(x, y, boardW, bh, () -> {
                click();
                switchTab(target);
            }));
        }
    }

    private MutableComponent boardLabel(int t) {
        MutableComponent label = Component.translatable("mythic_age.village.tab." + TAB_KEYS[t]);
        if (t == VillageScreenPayloads.TAB_QUESTS && header.quests() > 0) {
            label.append(" · " + header.quests());
        }
        return label;
    }

    // ───────────── 共用的画法 ─────────────

    static Component line(AffairsView.Line l) {
        return AffairsView.toComponent(l);
    }

    /** 一行，超宽用「…」截断。 */
    void drawClipped(GuiGraphics g, Component c, int x, int y, int maxW, int color) {
        if (font.width(c) <= maxW) {
            g.drawString(font, c, x, y, color, false);
            return;
        }
        String ell = "…";
        FormattedCharSequence cut = net.minecraft.locale.Language.getInstance().getVisualOrder(
                font.substrByWidth(c, Math.max(0, maxW - font.width(ell))));
        g.drawString(font, cut, x, y, color, false);
        g.drawString(font, ell, x + font.width(cut), y, color, false);
    }

    /** 自动折行；g 为空只量高度。clipTop / clipBottom 之外的行不画。返回下一行的 y。 */
    int text(GuiGraphics g, Component c, int x, int y, int maxW, int color) {
        for (FormattedCharSequence seq : font.split(c, Math.max(10, maxW))) {
            if (g != null) {
                g.drawString(font, seq, x, y, color, false);
            }
            y += 10;
        }
        return y;
    }

    /**
     * 木签按钮：画出来并登记命中区。不可用时变灰、不响应；tip 非空时悬停显示（不可用的原因）。
     * visibleTop / visibleBottom：滚动区域的可见范围，按钮露出不全时不登记。返回按钮宽度。
     */
    int button(GuiGraphics g, int x, int y, Component label, boolean active, List<Component> tip, int mouseX, int mouseY,
               int visibleTop, int visibleBottom, Runnable action) {
        int bw = font.width(label) + 12;
        int bh = 14;
        boolean hover = mouseX >= x && mouseX < x + bw && mouseY >= y && mouseY < y + bh
                && mouseY >= visibleTop && mouseY < visibleBottom;
        g.blitSprite(!active ? BUTTON_DISABLED : hover ? BUTTON_HOVER : BUTTON, x, y, bw, bh);
        g.drawString(font, label, x + 6, y + 3, active ? 0xFFFFF4DC : 0xFFCFC2AE, false);
        if (active && y >= visibleTop && y + bh <= visibleBottom) {
            pending.add(new Hit(x, y, bw, bh, () -> {
                click();
                action.run();
            }));
        }
        if (hover && !tip.isEmpty()) {
            tooltip = tip;
        }
        return bw;
    }

    int buttonWidth(Component label) {
        return font.width(label) + 12;
    }

    /** 登记一个不画按钮外观的可点区域（物品格、列表行）。 */
    void hit(int x, int y, int hw, int hh, Runnable action) {
        pending.add(new Hit(x, y, hw, hh, action));
    }

    void tooltip(List<Component> lines) {
        this.tooltip = lines;
    }

    void tooltip(ItemStack stack) {
        this.tooltipItem = stack;
    }

    /** 进度条：底色、填充、外框。 */
    static void bar(GuiGraphics g, int x, int y, int bw, int bh, double frac, int fill) {
        g.fill(x, y, x + bw, y + bh, 0xFFD6C49A);
        g.fill(x, y, x + (int) Math.round(bw * Math.max(0, Math.min(1, frac))), y + bh, fill);
        g.renderOutline(x, y, bw, bh, 0xFF8A6A3A);
    }

    static ItemStack stack(String item) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(item)));
    }

    /** 物品（或 {@code #组}）的名字。 */
    static Component itemName(String id) {
        if (id.startsWith("#")) {
            return Component.translatable("mythic_age.village.cost_group." + id.substring(1));
        }
        return stack(id).getHoverName();
    }

    /** 一行物品图标 + 件数（物品组画名字）。超出宽度就停。返回画了几项。 */
    int items(GuiGraphics g, java.util.Map<String, Integer> what, int x, int y, int maxW, int mouseX, int mouseY, int visibleTop,
              int visibleBottom) {
        int cx = x;
        int n = 0;
        for (var e : new java.util.TreeMap<>(what).entrySet()) {
            String count = "×" + e.getValue();
            int iw;
            if (e.getKey().startsWith("#")) {
                Component name = itemName(e.getKey()).copy().append(" " + count);
                iw = font.width(name) + 6;
                if (cx + iw > x + maxW) {
                    break;
                }
                g.drawString(font, name, cx, y + 4, INK, false);
            } else {
                ItemStack st = stack(e.getKey());
                iw = 18 + font.width(count) + 6;
                if (cx + iw > x + maxW) {
                    break;
                }
                g.renderItem(st, cx, y);
                g.drawString(font, count, cx + 18, y + 4, INK, false);
                if (mouseX >= cx && mouseX < cx + 16 && mouseY >= y && mouseY < y + 16 && mouseY >= visibleTop && mouseY < visibleBottom) {
                    tooltip(st);
                }
            }
            cx += iw;
            n++;
        }
        return n;
    }

    /** 建筑名：先找自己的语言键，再去掉主神后缀找；通用祠写「某神的祠」；认不出的写「建筑」。 */
    static Component buildingName(String id, String god) {
        if (id.isEmpty()) {
            return Component.translatable("mythic_age.village.map.unknown");
        }
        int colon = id.indexOf(':');
        String civ = colon < 0 ? "" : id.substring(0, colon);
        String base = colon < 0 ? id : id.substring(colon + 1);
        if (base.equals("shrine") && !god.isEmpty()) {
            return Component.translatable("mythic_age.building.shrine_of", Component.translatable("mythic_age.god." + god));
        }
        String key = "mythic_age.building." + civ + "." + base;
        while (!net.minecraft.client.resources.language.I18n.exists(key) && base.contains("_")) {
            base = base.substring(0, base.lastIndexOf('_'));
            key = "mythic_age.building." + civ + "." + base;
        }
        return Component.translatable(key);
    }

    void click() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    void send(net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    void act(String action, int arg) {
        send(new VillageScreenPayloads.Act(village, action, arg));
    }

    // ───────────── 输入 ─────────────

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            for (int i = hits.size() - 1; i >= 0; i--) {
                if (hits.get(i).contains(mx, my)) {
                    hits.get(i).action().run();
                    return true;
                }
            }
        }
        if (pages[tab].mouseClicked(mx, my, button)) {
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        pages[tab].mouseReleased(mx, my, button);
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (pages[tab].mouseDragged(mx, my, button, dx, dy)) {
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        return pages[tab].mouseScrolled(mx, my, dy) || super.mouseScrolled(mx, my, dx, dy);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        send(new VillageScreenPayloads.Close(village));
        super.removed();
    }

    Optional<VillageScreenPayloads.Header> headerView() {
        return Optional.ofNullable(header);
    }
}
