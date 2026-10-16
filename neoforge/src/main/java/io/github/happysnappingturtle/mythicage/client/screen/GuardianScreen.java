package io.github.happysnappingturtle.mythicage.client.screen;

import io.github.happysnappingturtle.mythicage.client.ClientHooks;
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
import java.util.List;
import java.util.UUID;

/**
 * 祭司交易界面（T9 设计文档 §9，12 号 §4「NPC 交易」）：对话页 + 兑换页；也用于神庙兑换（只有兑换页）。
 * 只展示服务端算好的数据；点击后按钮置灰，收到结果再恢复；恩宠数字以服务端同步为准。
 * 与文明有关的文字都按 {@link TradeView#civ()} 与文明档案给的 key 取（T10b），不写死任何文明。
 */
public class GuardianScreen extends Screen {
    private static final int W = 360;
    private static final int H = 220;
    private static final int ROW_H = 24;
    private static final int ROWS = 5;
    private static final int PANEL = 0xE0201A14;
    private static final int BORDER = 0xFFB89A5A;
    private static final int TEXT = 0xFFEDE8DC;
    private static final int DIM = 0xFFB9B09C;
    private static final int OK = 0xFF7FD67F;
    private static final int BAD = 0xFFFF8A70;
    private static final int PURPLE = 0xFFC79BD8;

    private enum Tab { DIALOGUE, TRADE }

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

    public GuardianScreen(TradeView view) {
        super(titleOf(view));
        this.view = view;
        this.tab = view.dialogue() != TradeView.Dialogue.NONE ? Tab.DIALOGUE : Tab.TRADE;
    }

    /** 祭司处：祭司的名字；神庙处：「神庙名 · 兑换」。 */
    private static Component titleOf(TradeView view) {
        if (view.venue() == TradeOffer.Venue.GUARDIAN) {
            var level = net.minecraft.client.Minecraft.getInstance().level;
            var e = level == null ? null : level.getEntity(view.guardianEntityId());
            return e != null ? e.getDisplayName() : Component.translatable("mythic_age.guardian.title");
        }
        return Component.translatable("mythic_age.trade.shrine_title", Component.translatable(view.labels().shrine()));
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
            status = Component.translatable("mythic_age.trade.result.ok", itemName(r.itemName()), r.count());
            statusColor = OK;
            if (view.dialogue() == TradeView.Dialogue.NONE) {
                tab = Tab.TRADE;
            }
        } else {
            status = Component.translatable(r.reason().langKey(), r.arg(), shrineName());
            statusColor = r.reason() == TradeReason.DUPLICATE ? DIM : BAD;
        }
        rebuildWidgets();
    }

    public void closeByServer() {
        closedByServer = true;
        onClose();
    }

    private static Component itemName(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null ? Component.literal(id) : BuiltInRegistries.ITEM.get(rl).getDescription();
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
        }
        if (tab == Tab.DIALOGUE) {
            initDialogue();
        } else {
            initTrade(hasDialogue ? top + 48 : top + 28);
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
        }
        if (view.dialogue() != TradeView.Dialogue.FIRST) {
            addRenderableWidget(Button.builder(Component.translatable("mythic_age.guardian.to_trade"), b -> {
                tab = Tab.TRADE;
                rebuildWidgets();
            }).bounds(view.dialogue() == TradeView.Dialogue.NONE ? left + 8 : left + 164, y, 100, 20).build());
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
            int tx = tab == Tab.DIALOGUE ? left + 8 : left + 70;
            g.fill(tx + 2, top + 43, tx + 58, top + 45, BORDER);
        }
        if (tab == Tab.DIALOGUE) {
            renderDialogue(g);
        } else {
            renderTrade(g, mouseX, mouseY, view.venue() == TradeOffer.Venue.GUARDIAN ? top + 48 : top + 28);
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
            case NONE -> "greet";
        };
        int y = line(g, Component.translatable(key), left + 12, top + 52, W - 24, TEXT);
        if (!view.villagePatron().isEmpty()) {
            // 本村供奉的主神（T10a）；次神候选只在晋升页显示（18 号 §4）
            line(g, Component.translatable("mythic_age.village.patron_line",
                    Component.translatable("mythic_age.god." + view.villagePatron())), left + 12, y + 6, W - 24, PURPLE);
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
        if (!o.needsMinorGod().isEmpty()) {
            return Component.translatable("mythic_age.trade.state.needs_god",
                    Component.translatable("mythic_age.god." + o.needsMinorGod()));
        }
        if (o.locked()) {
            return Component.translatable("mythic_age.trade.state.locked", Component.translatable("mythic_age.age." + o.minAge().id()));
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
