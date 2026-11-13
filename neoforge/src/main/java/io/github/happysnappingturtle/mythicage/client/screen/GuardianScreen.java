package io.github.happysnappingturtle.mythicage.client.screen;

import io.github.happysnappingturtle.mythicage.client.ClientHooks;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.rite.RiteKind;
import io.github.happysnappingturtle.mythicage.rite.RitePayloads;
import io.github.happysnappingturtle.mythicage.rite.RiteService;
import io.github.happysnappingturtle.mythicage.rite.RitesView;
import io.github.happysnappingturtle.mythicage.core.trade.TradeOffer;
import io.github.happysnappingturtle.mythicage.core.trade.TradeReason;
import io.github.happysnappingturtle.mythicage.trade.TradePayloads;
import io.github.happysnappingturtle.mythicage.trade.TradeView;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 祭司交易界面（T9 设计文档 §9，12 号 §4「NPC 交易」）：对话页 + 兑换页 + 晋升页（T11a）；
 * 也用于主神像兑换（只有兑换页）。
 * 只展示服务端算好的数据；点击后按钮置灰，收到结果再恢复；恩宠数字以服务端同步为准。
 * 与文明有关的文字都按 {@link TradeView#civ()} 与文明档案给的 key 取（T10b），不写死任何文明。
 */
public class GuardianScreen extends Screen {
    private static final int W = 380;
    private static final int H = 236;
    private static final int ROW_H = 24;
    private static final int ROWS = 5;
    private static final int PANEL = 0xE0201A14;
    private static final int BORDER = 0xFFB89A5A;
    private static final int TEXT = 0xFFEDE8DC;
    private static final int DIM = 0xFFB9B09C;
    private static final int OK = 0xFF7FD67F;
    private static final int BAD = 0xFFFF8A70;
    private static final int PURPLE = 0xFFC79BD8;

    private enum Tab { DIALOGUE, TRADE, RITES }

    private TradeView view;
    private Tab tab;
    @Nullable
    private String pending;
    @Nullable
    private Component status;
    private int statusColor = TEXT;
    private int scroll;
    private int left;
    private int top;
    private boolean closedByServer;
    /** 晋升页：同一种仪式里选的哪一个（改奉按时代）与所选次神。 */
    private int option;
    private String chosenGod = "";

    public GuardianScreen(TradeView view) {
        super(titleOf(view));
        this.view = view;
        this.tab = view.dialogue() != TradeView.Dialogue.NONE ? Tab.DIALOGUE : Tab.TRADE;
    }

    /** 祭司处：祭司的名字；主神像处：「宙斯像 · 兑换」。 */
    private static Component titleOf(TradeView view) {
        if (view.venue() == TradeOffer.Venue.GUARDIAN) {
            var level = net.minecraft.client.Minecraft.getInstance().level;
            var e = level == null ? null : level.getEntity(view.guardianEntityId());
            return e != null ? e.getDisplayName() : Component.translatable("mythic_age.guardian.title");
        }
        if (view.venue() == TradeOffer.Venue.ORACLE) {
            return Component.translatable("mythic_age.oracle.title");
        }
        return Component.translatable("mythic_age.trade.statue_title",
                Component.translatable("mythic_age.god." + view.villagePatron()));
    }

    private Component shrineName() {
        return Component.translatable(view.labels().shrine());
    }

    /** 服务端推送了新的界面数据（再次右键或刷新）。 */
    public void update(TradeView next) {
        view = next;
        if (view.dialogue() == TradeView.Dialogue.NONE && tab == Tab.DIALOGUE && pending == null) {
            tab = Tab.TRADE;
        }
        rebuildWidgets();
    }

    /** 一次请求的结果。 */
    public void accept(TradePayloads.Result r) {
        if (r.requestId().equals(pending) || pending == null) {
            pending = null;
        }
        view = r.view();
        if (r.reason() == TradeReason.OK) {
            // 受祝福、在这里供奉不发物品（GA-13）
            status = r.given().isEmpty() ? Component.translatable("mythic_age.trade.result.devotee")
                    : Component.translatable("mythic_age.trade.result.ok", r.given().getHoverName(), r.given().getCount());
            statusColor = OK;
            if (view.dialogue() == TradeView.Dialogue.NONE) {
                tab = Tab.TRADE;
            }
        } else {
            status = Component.translatable(r.reason().langKey(), r.arg(), shrineName());
            statusColor = r.reason() == TradeReason.DUPLICATE || r.reason() == TradeReason.SEARCHING ? DIM : BAD;
        }
        rebuildWidgets();
    }

    /** 仪式结果（T11a）。 */
    public void acceptRite(RitePayloads.Result r) {
        if (r.requestId().equals(pending) || pending == null) {
            pending = null;
        }
        view = r.view();
        if (r.reason() == RiteService.Reason.OK) {
            status = Component.translatable("mythic_age.rite.result.ok");
            statusColor = OK;
            chosenGod = "";
        } else {
            Component why = Component.translatable("mythic_age.rite.result." + r.reason().name().toLowerCase(java.util.Locale.ROOT));
            status = r.issues().isEmpty() ? why : why.copy().append(" ").append(r.issues().getFirst());
            statusColor = r.reason() == RiteService.Reason.DUPLICATE ? DIM : BAD;
        }
        rebuildWidgets();
    }

    public void closeByServer() {
        closedByServer = true;
        onClose();
    }


    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        boolean hasDialogue = view.venue() == TradeOffer.Venue.GUARDIAN;
        if (hasDialogue) {
            addRenderableWidget(Button.builder(Component.translatable("mythic_age.trade.tab.talk"), b -> {
                tab = Tab.DIALOGUE;
                rebuildWidgets();
            }).bounds(left + 8, top + 26, 60, 16).build());
            addRenderableWidget(Button.builder(Component.translatable("mythic_age.trade.tab.trade"), b -> {
                tab = Tab.TRADE;
                rebuildWidgets();
            }).bounds(left + 70, top + 26, 60, 16).build());
            if (view.rites().isPresent()) {
                addRenderableWidget(Button.builder(Component.translatable("mythic_age.trade.tab.rites"), b -> {
                    tab = Tab.RITES;
                    rebuildWidgets();
                }).bounds(left + 132, top + 26, 60, 16).build());
            }
            // 信仰面板（D12）：在祭司界面之上打开，关闭后回到这里；不结束兑换会话
            addRenderableWidget(Button.builder(Component.translatable("mythic_age.faith.tab"),
                    b -> PacketDistributor.sendToServer(new io.github.happysnappingturtle.mythicage.faith.FaithPayloads.Request()))
                    .bounds(left + W - 68, top + 26, 60, 16).build());
        }
        if (tab == Tab.RITES && view.rites().isEmpty()) {
            tab = Tab.TRADE;
        }
        switch (tab) {
            case DIALOGUE -> initDialogue();
            case TRADE -> initTrade(hasDialogue ? top + 48 : top + 28);
            case RITES -> initRites(view.rites().get());
        }
    }

    // ───────────── 晋升页（T11a 设计文档 §7.1；2026-10-16 按用户意见简化） ─────────────

    private static final RiteKind[] KINDS = {RiteKind.PROMOTION, RiteKind.CONSECRATION, RiteKind.REDEDICATION};
    private static final int COL_W = 160;
    private static final int ROW = 14;
    private RiteKind kind = RiteKind.PROMOTION;

    private static Component riteName(RiteKind k) {
        return Component.translatable(switch (k) {
            case PROMOTION -> "mythic_age.rite.promotion";
            case CONSECRATION -> "mythic_age.rite.founding";
            case REDEDICATION -> "mythic_age.rite.rededication";
        });
    }

    private List<RitesView.Option> optionsOf(RiteKind k) {
        return view.rites().map(RitesView::options).orElse(List.of()).stream().filter(o -> o.kind() == k).toList();
    }

    /** 当前选中的仪式（改奉按时代有多个，option 是它们之间的下标）。 */
    private Optional<RitesView.Option> currentOption() {
        List<RitesView.Option> opts = optionsOf(kind);
        if (opts.isEmpty()) {
            return Optional.empty();
        }
        option = Math.max(0, Math.min(option, opts.size() - 1));
        return Optional.of(opts.get(option));
    }

    private int contentTop() {
        return top + 84 + (kind == RiteKind.REDEDICATION && optionsOf(kind).size() > 1 ? 18 : 0);
    }

    private void initRites(RitesView rv) {
        int x = left + 8;
        for (RiteKind k : KINDS) {
            Button b = Button.builder(riteName(k), btn -> {
                kind = k;
                option = 0;
                chosenGod = "";
                rebuildWidgets();
            }).bounds(x, top + 48, 56, 16).build();
            addRenderableWidget(b);
            x += 58;
        }
        List<RitesView.Option> opts = optionsOf(kind);
        if (kind == RiteKind.REDEDICATION && opts.size() > 1) {
            int ax = left + 40;
            for (int i = 0; i < opts.size(); i++) {
                int index = i;
                Button b = Button.builder(Component.translatable("mythic_age.age." + opts.get(i).target().id()), btn -> {
                    option = index;
                    chosenGod = "";
                    rebuildWidgets();
                }).bounds(ax, top + 82, 40, 14).build();
                b.active = i != option;
                addRenderableWidget(b);
                ax += 42;
            }
        }
        Optional<RitesView.Option> cur = currentOption();
        if (cur.isEmpty()) {
            return;
        }
        RitesView.Option o = cur.get();
        if (o.candidates().stream().noneMatch(c -> c.id().equals(chosenGod) && c.available())) {
            chosenGod = o.candidates().stream().filter(RitesView.Candidate::available)
                    .filter(c -> !c.id().equals(o.currentGod())).map(RitesView.Candidate::id).findFirst().orElse("");
        }
        Button perform = Button.builder(Component.translatable("mythic_age.rite.perform", riteName(o.kind())), b -> {
            pending = UUID.randomUUID().toString();
            status = Component.translatable("mythic_age.trade.pending");
            statusColor = DIM;
            PacketDistributor.sendToServer(new RitePayloads.Perform(o.kind(),
                    o.kind() == RiteKind.REDEDICATION ? o.target() : Age.NONE, chosenGod, o.favor(), pending));
            rebuildWidgets();
        }).bounds(left + W - 108, top + H - 46, 100, 20).build();
        perform.active = pending == null && o.ready() && !chosenGod.isEmpty();
        addRenderableWidget(perform);
    }

    /** 候选列表：点一行即选中（已开放、且不是当前供奉的才可选）。 */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (tab == Tab.RITES && button == 0) {
            Optional<RitesView.Option> cur = currentOption();
            int y0 = contentTop() + 12;
            if (cur.isPresent() && mouseX >= left + 8 && mouseX < left + 8 + COL_W && mouseY >= y0) {
                int i = (int) ((mouseY - y0) / ROW);
                List<RitesView.Candidate> cands = cur.get().candidates();
                if (i >= 0 && i < cands.size() && cands.get(i).available() && !cands.get(i).id().equals(cur.get().currentGod())) {
                    chosenGod = cands.get(i).id();
                    rebuildWidgets();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** 一条带 ✔/✘ 的说明：记号单独画，文字在记号右边换行。 */
    private int mark(GuiGraphics g, boolean ok, Component text, int x, int y, int w) {
        g.drawString(font, ok ? "✔" : "✘", x, y, ok ? OK : BAD, false);
        return line(g, text, x + 10, y, w - 10, ok ? OK : BAD);
    }

    private void renderRites(GuiGraphics g, int mouseX, int mouseY) {
        RitesView rv = view.rites().orElse(null);
        if (rv == null) {
            return;
        }
        // 选中的仪式：按钮下方一条金线
        int kx = left + 8 + java.util.Arrays.asList(KINDS).indexOf(kind) * 58;
        g.fill(kx + 2, top + 65, kx + 54, top + 66, BORDER);
        // 本村
        Component village = Component.translatable("mythic_age.rite.village",
                Component.translatable("mythic_age.god." + rv.patron()), Component.translatable("mythic_age.age." + rv.stage().id()));
        g.drawString(font, village, left + W - 8 - font.width(village), top + 52, PURPLE, false);
        // 当前仪式：一句说明
        g.drawString(font, Component.translatable("mythic_age.rite.explain." + kind.id()), left + 8, top + 70, DIM, false);
        if (kind == RiteKind.REDEDICATION && optionsOf(kind).size() > 1) {
            g.drawString(font, Component.translatable("mythic_age.rite.age_label"), left + 8, top + 85, DIM, false);
        }
        Optional<RitesView.Option> cur = currentOption();
        if (cur.isEmpty()) {
            return;
        }
        RitesView.Option o = cur.get();
        int y0 = contentTop();
        int lx = left + 8;
        int rx = left + 8 + COL_W + 16;
        int rw = W - COL_W - 32;
        if (o.candidates().isEmpty() && o.lines().isEmpty()) {
            // 根本性的阻碍：只说这一条
            for (Component b : o.blocking()) {
                mark(g, false, b, lx, y0 + 4, W - 24);
            }
            return;
        }
        // 左：次神
        g.drawString(font, Component.translatable("mythic_age.rite.choose_at",
                Component.translatable("mythic_age.age." + o.target().id())), lx, y0, BORDER, false);
        int y = y0 + 12;
        for (RitesView.Candidate c : o.candidates()) {
            boolean selected = c.id().equals(chosenGod);
            boolean current = c.id().equals(o.currentGod());
            boolean hover = mouseX >= lx && mouseX < lx + COL_W && mouseY >= y && mouseY < y + ROW;
            if (selected) {
                g.fill(lx - 2, y - 2, lx + COL_W, y + ROW - 3, 0x50B89A5A);
            } else if (hover && c.available() && !current) {
                g.fill(lx - 2, y - 2, lx + COL_W, y + ROW - 3, 0x20FFFFFF);
            }
            int color = !c.available() || current ? DIM : TEXT;
            g.drawString(font, (selected ? "● " : "○ "), lx, y, selected ? BORDER : DIM, false);
            g.drawString(font, Component.translatable("mythic_age.god." + c.id()), lx + 12, y, color, false);
            Component tag = current ? Component.translatable("mythic_age.rite.current")
                    : !c.available() ? Component.translatable("mythic_age.rite.unavailable") : Component.empty();
            g.drawString(font, tag, lx + COL_W - 4 - font.width(tag), y, DIM, false);
            y += ROW;
        }
        if (o.candidates().isEmpty()) {
            g.drawString(font, Component.translatable("mythic_age.rite.no_candidates"), lx, y, DIM, false);
        }
        // 所选次神可兑换的东西（有才显示）
        o.candidates().stream().filter(c -> c.id().equals(chosenGod)).findFirst().ifPresent(c -> {
            List<ItemStack> icons = new ArrayList<>(c.powers());
            icons.addAll(c.summons());
            icons.addAll(c.goods());
            if (icons.isEmpty()) {
                return;
            }
            int iy = y0 + 12 + o.candidates().size() * ROW + 4;
            g.drawString(font, Component.translatable("mythic_age.rite.offers"), lx, iy + 4, DIM, false);
            int ix = lx + 40;
            for (ItemStack st : icons) {
                if (ix > lx + COL_W - 16) {
                    break;
                }
                g.renderItem(st, ix, iy);
                if (mouseX >= ix && mouseX < ix + 16 && mouseY >= iy && mouseY < iy + 16) {
                    g.renderTooltip(font, st, mouseX, mouseY);
                }
                ix += 18;
            }
        });
        // 右：需要
        g.drawString(font, Component.translatable("mythic_age.rite.needs"), rx, y0, BORDER, false);
        int ry = y0 + 12;
        if (o.kind() == RiteKind.CONSECRATION && o.target() != Age.NONE) {
            ry = line(g, Component.translatable(o.approved() ? "mythic_age.rite.head.approved" : "mythic_age.rite.head.unapproved",
                    Component.translatable("mythic_age.god." + rv.patron()), Component.translatable("mythic_age.age." + o.target().id())),
                    rx, ry, rw, DIM);
        }
        for (RitesView.Line l : o.lines()) {
            g.drawString(font, l.met() ? "✔" : "✘", rx, ry, l.met() ? OK : BAD, false);
            g.drawString(font, l.label(), rx + 10, ry, l.met() ? OK : BAD, false);
            g.drawString(font, l.progress(), rx + rw - font.width(l.progress()), ry, l.met() ? OK : BAD, false);
            ry += 10;
        }
        for (Component b : o.blocking()) {
            ry = mark(g, false, b, rx, ry, rw);
        }
        // 下：仪式后（条件行多时整体下移，不压住上面的条件；T11b 实机发现：希腊古典晋升有 6 行条件）
        int leftBottom = y0 + 12 + o.candidates().size() * ROW + 20;
        int py = Math.max(top + H - 84, Math.max(ry, leftBottom) + 8);
        g.fill(left + 6, py - 4, left + W - 6, py - 3, 0x60B89A5A);
        g.drawString(font, Component.translatable("mythic_age.rite.preview.title"), lx, py, BORDER, false);
        py += 11;
        List<Component> preview = new ArrayList<>(o.preview());
        if (!chosenGod.isEmpty() && !preview.isEmpty()) {
            preview.set(preview.size() - 1, Component.translatable("mythic_age.rite.preview.shrine_of",
                    Component.translatable("mythic_age.god." + chosenGod)));
        }
        for (Component c : preview) {
            if (py > top + H - 30) {
                break;
            }
            py = line(g, Component.literal("· ").append(c), lx, py, W - 130, TEXT);
        }
    }

    private void initDialogue() {
        int y = top + H - 52;
        if (view.dialogue() == TradeView.Dialogue.FIRST) {
            addRenderableWidget(Button.builder(Component.translatable("mythic_age.guardian.accept_blessing"),
                    b -> sendBlessing(false)).bounds(left + 8, y, 150, 20).build()).active = pending == null;
        } else if (view.dialogue() == TradeView.Dialogue.REISSUE) {
            addRenderableWidget(Button.builder(Component.translatable("mythic_age.guardian.reissue", shrineName()),
                    b -> sendBlessing(true)).bounds(left + 8, y, 150, 20).build()).active = pending == null;
        } else if (view.dialogue() == TradeView.Dialogue.JOIN) {
            addRenderableWidget(Button.builder(Component.translatable("mythic_age.guardian.join"),
                    b -> sendBlessing(true)).bounds(left + 8, y, 150, 20).build()).active = pending == null;
        }
        if (view.dialogue() != TradeView.Dialogue.FIRST) {
            addRenderableWidget(Button.builder(Component.translatable("mythic_age.guardian.to_trade"), b -> {
                tab = Tab.TRADE;
                rebuildWidgets();
            }).bounds(view.dialogue() == TradeView.Dialogue.NONE ? left + 8 : left + 164, y, 100, 20).build());
        }
        // 本村祭司：「村务」打开村庄总览（T24 D8）
        if (view.dialogue() != TradeView.Dialogue.FIRST && !view.villagePatron().isEmpty()
                && view.venue() == io.github.happysnappingturtle.mythicage.core.trade.TradeOffer.Venue.GUARDIAN) {
            int x = view.dialogue() == TradeView.Dialogue.NONE ? left + 112 : left + 268;
            addRenderableWidget(Button.builder(Component.translatable("mythic_age.guardian.affairs"), b -> PacketDistributor.sendToServer(
                    new io.github.happysnappingturtle.mythicage.village.economy.AffairsPayloads.Open(
                            io.github.happysnappingturtle.mythicage.village.economy.AffairsPayloads.Open.FROM_PRIEST,
                            io.github.happysnappingturtle.mythicage.village.economy.AffairsView.TAB_OVERVIEW)))
                    .bounds(x, y, Math.min(100, left + W - 8 - x), 20).build());
        }
    }

    private void initTrade(int y0) {
        List<TradeView.OfferView> offers = view.offers();
        scroll = Math.max(0, Math.min(scroll, offers.size() - ROWS));
        for (int i = 0; i < ROWS && scroll + i < offers.size(); i++) {
            TradeView.OfferView o = offers.get(scroll + i);
            int y = y0 + i * ROW_H;
            Button buy = Button.builder(Component.translatable("mythic_age.trade.buy"), b -> sendBuy(o))
                    .bounds(left + W - 58, y + 2, 50, 18).build();
            buy.active = pending == null && !o.locked() && o.affordable() && o.remainingToday() != 0;
            addRenderableWidget(buy);
        }
    }

    private void sendBuy(TradeView.OfferView o) {
        pending = UUID.randomUUID().toString();
        status = Component.translatable("mythic_age.trade.pending");
        statusColor = DIM;
        PacketDistributor.sendToServer(new TradePayloads.Buy(o.id(), o.favorPrice(), pending));
        rebuildWidgets();
    }

    private void sendBlessing(boolean reissue) {
        pending = UUID.randomUUID().toString();
        status = Component.translatable("mythic_age.trade.pending");
        statusColor = DIM;
        PacketDistributor.sendToServer(new TradePayloads.Blessing(reissue, pending));
        rebuildWidgets();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double dx, double dy) {
        if (tab == Tab.TRADE && view.offers().size() > ROWS) {
            scroll = Math.max(0, Math.min(view.offers().size() - ROWS, scroll - (int) Math.signum(dy)));
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, dx, dy);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (!closedByServer) {
            PacketDistributor.sendToServer(new TradePayloads.Closed());
        }
        ClientHooks.clearTradeScreen(this);
        super.onClose();
    }

    /** 面板画在背景（模糊 + 压暗）之上、按钮之下。 */
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        g.fill(left, top, left + W, top + H, PANEL);
        g.renderOutline(left, top, W, H, BORDER);
        g.fill(left + 6, top + 20, left + W - 6, top + 21, 0x60B89A5A);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawString(font, title, left + 8, top + 8, BORDER, false);
        Component favor = view.age() == io.github.happysnappingturtle.mythicage.core.player.Age.NONE
                ? Component.translatable("mythic_age.trade.favor.none",
                Component.translatable("mythic_age.civ." + view.civ().id()))
                : Component.translatable("mythic_age.trade.favor", Component.translatable(view.labels().favor()),
                view.favor(), view.cap(), Component.translatable("mythic_age.age." + view.age().id()));
        g.drawString(font, favor, left + W - 8 - font.width(favor), top + 8, PURPLE, false);
        if (view.venue() == TradeOffer.Venue.GUARDIAN) {
            // 当前页签：按钮下方一条金线
            int tx = switch (tab) {
                case DIALOGUE -> left + 8;
                case TRADE -> left + 70;
                case RITES -> left + 132;
            };
            g.fill(tx + 2, top + 43, tx + 58, top + 45, BORDER);
        }
        switch (tab) {
            case DIALOGUE -> renderDialogue(g);
            case TRADE -> renderTrade(g, mouseX, mouseY, view.venue() == TradeOffer.Venue.GUARDIAN ? top + 48 : top + 28);
            case RITES -> renderRites(g, mouseX, mouseY);
        }
        if (status != null) {
            g.fill(left + 6, top + H - 22, left + W - 6, top + H - 21, 0x60B89A5A);
            line(g, status, left + 8, top + H - 16, W - 16, statusColor);
        }
    }

    private void renderDialogue(GuiGraphics g) {
        // 对话文本每个文明一套：mythic_age.guardian.<文明>.talk.<first|reissue|greet>
        String key = "mythic_age.guardian." + view.civ().id() + ".talk." + switch (view.dialogue()) {
            case FIRST -> "first";
            case REISSUE -> "reissue";
            case JOIN -> "join";
            case NONE -> "greet";
        };
        int y = line(g, Component.translatable(key), left + 12, top + 52, W - 24, TEXT);
        if (!view.villagePatron().isEmpty()) {
            // 本村供奉的主神（T10a）；次神候选只在晋升页显示（18 号 §4）
            y = line(g, Component.translatable("mythic_age.village.patron_line",
                    Component.translatable("mythic_age.god." + view.villagePatron())), left + 12, y + 6, W - 24, PURPLE);
        }
        for (Component note : view.villageNotes()) {
            y = line(g, note, left + 12, y + 6, W - 24, DIM);
        }
    }

    private void renderTrade(GuiGraphics g, int mouseX, int mouseY, int y0) {
        List<TradeView.OfferView> offers = view.offers();
        if (offers.isEmpty()) {
            line(g, Component.translatable("mythic_age.trade.empty"), left + 12, y0 + 6, W - 24, DIM);
            return;
        }
        for (int i = 0; i < ROWS && scroll + i < offers.size(); i++) {
            TradeView.OfferView o = offers.get(scroll + i);
            int y = y0 + i * ROW_H;
            g.fill(left + 6, y, left + W - 6, y + ROW_H - 2, 0x30FFFFFF);
            g.renderItem(o.result(), left + 10, y + 3);
            g.renderItemDecorations(font, o.result(), left + 10, y + 3);
            g.drawString(font, o.result().getHoverName(), left + 30, y + 3, TEXT, false);
            g.drawString(font, Component.translatable("mythic_age.trade.category." + o.category().id()), left + 30,
                    y + 13, DIM, false);
            int px = left + 150;
            if (o.favorPrice() > 0) {
                g.drawString(font, Component.translatable("mythic_age.trade.price.favor", o.favorPrice()), px, y + 8,
                        o.affordable() ? PURPLE : BAD, false);
            } else {
                for (ItemStack s : o.itemPrice()) {
                    g.renderItem(s, px, y + 3);
                    g.renderItemDecorations(font, s, px, y + 3);
                    px += 20;
                }
            }
            MutableComponent state = stateOf(o);
            int sx = left + W - 64 - font.width(state);
            g.drawString(font, state, sx, y + 8, o.locked() || o.remainingToday() == 0 || !o.affordable() ? BAD : DIM, false);
            if (mouseX >= left + 10 && mouseX < left + 26 && mouseY >= y + 3 && mouseY < y + 19) {
                g.renderTooltip(font, o.result(), mouseX, mouseY);
            }
        }
        if (offers.size() > ROWS) {
            String more = (scroll + 1) + "–" + Math.min(offers.size(), scroll + ROWS) + " / " + offers.size();
            g.drawString(font, more, left + W - 8 - font.width(more), y0 + ROWS * ROW_H, DIM, false);
        }
    }

    private static MutableComponent stateOf(TradeView.OfferView o) {
        if (o.locked()) {
            return switch (o.lock()) {
                case MINOR_GOD_LOCKED -> Component.translatable("mythic_age.trade.state.needs_god",
                        Component.translatable("mythic_age.god." + o.lockArg()));
                case STAGE_LOCKED -> Component.translatable("mythic_age.trade.state.stage",
                        Component.translatable("mythic_age.age." + o.lockArg()));
                case NO_SHRINE -> Component.translatable("mythic_age.trade.state.no_shrine",
                        Component.translatable("mythic_age.god." + o.lockArg()));
                case WRONG_PATRON -> Component.translatable("mythic_age.trade.state.wrong_patron",
                        Component.translatable("mythic_age.god." + o.lockArg()));
                case NEED_APPROVAL -> Component.translatable("mythic_age.trade.state.need_approval",
                        Component.translatable("mythic_age.god." + o.lockArg()));
                case HAS_STATUE -> Component.translatable("mythic_age.trade.state.has_statue");
                default -> Component.translatable("mythic_age.trade.state.locked",
                        Component.translatable("mythic_age.age." + o.lockArg()));
            };
        }
        if (o.remainingToday() == 0) {
            return Component.translatable("mythic_age.trade.state.sold_out");
        }
        if (!o.affordable()) {
            return Component.translatable(o.favorPrice() > 0 ? "mythic_age.trade.state.no_favor" : "mythic_age.trade.state.no_items");
        }
        return o.remainingToday() > 0 ? Component.translatable("mythic_age.trade.state.remaining", o.remainingToday())
                : Component.empty();
    }

    private int line(GuiGraphics g, Component text, int x, int y, int w, int color) {
        for (FormattedCharSequence seq : font.split(text, w)) {
            g.drawString(font, seq, x, y, color, false);
            y += 10;
        }
        return y;
    }
}
