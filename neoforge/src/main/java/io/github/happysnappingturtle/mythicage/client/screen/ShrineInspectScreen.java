package io.github.happysnappingturtle.mythicage.client.screen;

import io.github.happysnappingturtle.mythicage.client.ClientRelics;
import io.github.happysnappingturtle.mythicage.client.ShrineHighlightRenderer;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.faith.FaithPayloads;
import io.github.happysnappingturtle.mythicage.relic.RelicPayloads;
import io.github.happysnappingturtle.mythicage.shrine.inspect.ShrineSurvey;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 神坛检视（T8a 设计文档 §4，12 号 §4.1；T24 §5.1 改为五文明通用）：左栏身份与结构、中栏祈祷与产率及离开时的记账、
 * 右栏缺失项与高亮图例，底部供奉栏。只展示服务端发来的数字，不在客户端做判定。
 */
public class ShrineInspectScreen extends Screen {
    private static final int W = 420;
    /** 三栏的高度；下面是供奉栏（T16）。 */
    private static final int BODY = 192;
    private static final int STRIP = 44;
    private static final int H = BODY + STRIP;
    private static final int PANEL = 0xE0201A14;
    private static final int BORDER = 0xFFB89A5A;
    private static final int TEXT = 0xFFEDE8DC;
    private static final int DIM = 0xFFB9B09C;
    private static final int OK = 0xFF7FD67F;
    private static final int BAD = 0xFFFF8A70;

    private final ShrineSurvey p;
    private int left;
    private int top;

    public ShrineInspectScreen(ShrineSurvey payload) {
        super(titleOf(payload));
        this.p = payload;
    }

    /** 村庄神庙（GA-13）的标题带村名。 */
    private static Component titleOf(ShrineSurvey p) {
        if (!p.temple().present()) {
            return Component.translatable("mythic_age.altar.title");
        }
        Component village = p.temple().patron().isEmpty() ? Component.translatable("mythic_age.village.title.plain")
                : Component.translatable("mythic_age.village.title", Component.translatable("mythic_age.god." + p.temple().patron()));
        return Component.translatable("mythic_age.altar.title.village", village);
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        addRenderableWidget(Button.builder(Component.translatable("mythic_age.altar.highlight"), b -> {
            ShrineHighlightRenderer.show(p);
            onClose();
        }).bounds(left + W - 130, top + BODY - 26, 120, 18).build());
        // 重看晋升（12 号 §4.3 A：关掉晋升界面后能在神坛检视重看）
        if (p.viewerIsOwner() && p.ownerAge().atLeast(Age.CLASSICAL)) {
            Component replay = Component.translatable("mythic_age.altar.replay");
            int w = font.width(replay) + 12;
            addRenderableWidget(Button.builder(replay, b -> PacketDistributor.sendToServer(new FaithPayloads.Replay(p.civ(), p.ownerAge())))
                    .bounds(left + W - 8 - w, top + 4, w, 14).build());
        }
        // 村庄神庙：退出供奉（GA-13 D3）
        if (p.temple().present() && p.temple().devotee()) {
            Component leave = Component.translatable("mythic_age.altar.leave");
            int w = font.width(leave) + 12;
            addRenderableWidget(Button.builder(leave, b -> PacketDistributor.sendToServer(
                            new io.github.happysnappingturtle.mythicage.content.greek.temple.TempleMembership.Leave(p.pos())))
                    .bounds(left + W - 138 - w, top + BODY - 26, w, 18).build());
        }
        RelicPayloads.Shelf shelf = ClientRelics.at(p.pos());
        if (shelf != null && shelf.viewerIsOwner()) {
            for (int i = 0; i < shelf.relics().size(); i++) {
                int slot = i;
                addRenderableWidget(Button.builder(Component.translatable("mythic_age.altar.relics.take"),
                                b -> PacketDistributor.sendToServer(new RelicPayloads.Retrieve(p.pos(), slot)))
                        .bounds(relicX(i) + 20, top + BODY + 18, 48, 18).build());
            }
        }
    }

    public BlockPos pos() {
        return p.pos();
    }

    /** 收到新的供奉栏后重建按钮。 */
    public void relicsChanged() {
        rebuildWidgets();
    }

    private int relicX(int i) {
        return left + 8 + i * 84;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private Component tier(int level) {
        return Component.translatable(p.tierKeys().get(level));
    }

    private static Component age(String id) {
        return Component.translatable("mythic_age.age." + id);
    }

    private static String num(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    /** 形状的字形（图例与线框同一形状）。 */
    static String glyph(ShrineSurvey.Shape s) {
        return switch (s) {
            case CELL -> "□";
            case CROSS -> "☒";
            case DASHED -> "⬚";
            case COLUMN -> "▮";
            case FLAT -> "▁";
        };
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.fill(left, top, left + W, top + H, PANEL);
        g.renderOutline(left, top, W, H, BORDER);
        g.drawString(font, title, left + 8, top + 7, BORDER, false);
        int colW = (W - 24) / 3;
        int c1 = left + 8;
        int c2 = c1 + colW + 4;
        int c3 = c2 + colW + 4;
        int y0 = top + 22;
        g.fill(c2 - 3, y0, c2 - 2, top + BODY - 8, 0x60B89A5A);
        g.fill(c3 - 3, y0, c3 - 2, top + BODY - 8, 0x60B89A5A);
        renderRelics(g);
        // 面板画在按钮上面了，按钮再画一遍
        for (var w : renderables) {
            if (w instanceof Button b) {
                b.render(g, mouseX, mouseY, partialTick);
            }
        }

        // 左栏：身份与结构
        int y = y0;
        ShrineSurvey.Temple temple = p.temple();
        if (temple.present()) {
            y = line(g, Component.translatable(temple.devotee() ? "mythic_age.altar.devotee.yes" : "mythic_age.altar.devotee.no"),
                    c1, y, colW, temple.devotee() ? OK : BAD);
            y = line(g, Component.translatable("mythic_age.altar.devotees", temple.devotees()), c1, y, colW, TEXT);
        } else {
            y = line(g, Component.translatable("mythic_age.altar.owner", p.ownerName(), age(p.ownerAge().id())), c1, y, colW, TEXT);
        }
        if (!p.hasResult()) {
            line(g, Component.translatable("mythic_age.altar.no_result"), c1, y, colW, DIM);
        } else {
            y = line(g, Component.translatable("mythic_age.altar.level", tier(p.grantedLevel()), num(p.coefficient())),
                    c1, y + 2, colW, BORDER);
            if (p.structuralLevel() > p.grantedLevel()) {
                y = line(g, Component.translatable("mythic_age.altar.structural", tier(p.structuralLevel())), c1, y, colW, DIM);
            }
            y += 4;
            boolean max = p.structuralLevel() >= p.tierKeys().size() - 1;
            y = line(g, max ? Component.translatable("mythic_age.altar.max")
                    : Component.translatable(temple.present() ? "mythic_age.altar.next.village" : "mythic_age.altar.next",
                    tier(p.structuralLevel() + 1)), c1, y, colW, DIM);
            for (ShrineSurvey.Row r : p.rows()) {
                y = progress(g, r, c1, y, colW);
            }
        }

        // 中栏：祈祷与产率、离开时的记账
        y = y0;
        y = line(g, Component.translatable("mythic_age.altar.slots", p.slotsFound(), p.slotsCap()), c2, y, colW, TEXT);
        if (p.hasResult() && p.slotsFound() < p.slotsCap()) {
            y = line(g, Component.translatable("mythic_age.altar.slots.short", p.slotsFound()), c2, y, colW, BAD);
        }
        ShrineSurvey.Prayer pr = p.prayer();
        y = line(g, Component.translatable("mythic_age.altar.praying", pr.praying()), c2, y, colW, TEXT);
        y = line(g, Component.translatable("mythic_age.altar.believers", pr.believers()), c2, y, colW, TEXT);
        y += 3;
        y = line(g, Component.translatable("mythic_age.altar.today", num(pr.today())), c2, y, colW, TEXT);
        y = line(g, Component.translatable("mythic_age.altar.estimate", num(p.estimate())), c2, y, colW, TEXT);
        y = line(g, Component.translatable("mythic_age.altar.estimate.note"), c2, y, colW, DIM);
        for (ShrineSurvey.Note n : pr.notes()) {
            y = line(g, Component.translatable(n.key(), n.args().toArray()), c2, y, colW, n.warn() ? BAD : BORDER);
        }
        y += 3;
        y = line(g, Component.translatable("mythic_age.altar.away.title"), c2, y, colW, BORDER);
        ShrineSurvey.Away a = p.away();
        if (!a.present()) {
            line(g, Component.translatable("mythic_age.altar.away.none"), c2, y, colW, DIM);
        } else if (!a.usable()) {
            y = line(g, Component.translatable("mythic_age.altar.away.stale"), c2, y, colW, BAD);
        } else if (a.ratePerDay() < 0.05) {
            y = line(g, Component.translatable("mythic_age.altar.away.zero"), c2, y, colW, DIM);
        } else {
            y = line(g, Component.translatable(a.verified() ? "mythic_age.altar.away.rate" : "mythic_age.altar.away.rate_unverified",
                    num(a.ratePerDay())), c2, y, colW, DIM);
        }
        if (a.present() && a.buffer() >= 0.05) {
            line(g, Component.translatable("mythic_age.altar.away.buffer", num(a.buffer())), c2, y, colW, OK);
        }

        // 右栏：缺失项与图例
        y = y0;
        y = line(g, Component.translatable("mythic_age.altar.missing.title"), c3, y, colW, BORDER);
        List<Component> missing = new ArrayList<>();
        if (temple.present() && !temple.devotee()) {
            missing.add(Component.translatable("mythic_age.altar.missing.devotee"));
        }
        if (p.unverified()) {
            missing.add(Component.translatable("mythic_age.altar.unverified"));
        }
        if (!p.ageRequired().isEmpty() && p.grantedLevel() + 1 < p.tierKeys().size()) {
            missing.add(Component.translatable("mythic_age.altar.age_required", age(p.ageRequired()), tier(p.grantedLevel() + 1)));
        }
        for (ShrineSurvey.Row r : p.rows()) {
            if (!r.ok()) {
                missing.add(r.bool() ? Component.translatable("mythic_age.altar.missing." + r.key())
                        : Component.translatable("mythic_age.altar.missing.row",
                        Component.translatable("mythic_age.altar.row." + r.key()), r.have(), r.need()));
            }
        }
        boolean allOk = missing.isEmpty();
        if (allOk) {
            missing.add(Component.translatable(temple.present() ? "mythic_age.altar.missing.none_village"
                    : p.structuralLevel() >= p.tierKeys().size() - 1 ? "mythic_age.altar.max" : "mythic_age.altar.missing.none"));
        }
        for (Component m : missing) {
            y = line(g, Component.literal("· ").append(m), c3, y, colW, allOk ? OK : BAD);
        }
        // 图例：只列本次有的组；形状字形 + 颜色 + 文字
        // 图例放在「高亮缺失格」按钮上方：标题 + 扫描范围 + 各组
        int ly = Math.max(y + 4, top + BODY - 30 - 10 * (p.highlights().size() + 2));
        ly = line(g, Component.translatable("mythic_age.altar.highlight.legend_title"), c3, ly, colW, DIM);
        ly = line(g, Component.literal("□ ").append(Component.translatable(temple.present()
                ? "mythic_age.altar.highlight.altar" : "mythic_age.altar.highlight.scan")), c3, ly, colW, TEXT);
        for (ShrineSurvey.Highlight h : p.highlights()) {
            ly = line(g, Component.literal(glyph(h.shape()) + " ").append(Component.translatable("mythic_age.altar.highlight." + h.kind())),
                    c3, ly, colW, h.color());
        }
    }

    /** 供奉栏：已用 / 上限（含其他神庙），架上的圣物与「取出」按钮（按钮在 init 里）。 */
    private void renderRelics(GuiGraphics g) {
        int y = top + BODY;
        g.fill(left + 4, y - 2, left + W - 4, y - 1, 0x60B89A5A);
        RelicPayloads.Shelf shelf = ClientRelics.at(p.pos());
        if (shelf == null) {
            return;
        }
        // 村庄神庙（GA-13）：每人每村的供奉位
        boolean village = p.temple().present();
        Component usage = Component.translatable(village ? "mythic_age.altar.relics.village" : "mythic_age.altar.relics",
                shelf.used(), shelf.cap());
        g.drawString(font, usage, left + 8, y + 4, BORDER, false);
        g.drawString(font, Component.translatable(village ? "mythic_age.altar.relics.how_village" : "mythic_age.altar.relics.how"),
                left + 20 + font.width(usage), y + 4, DIM, false);
        if (shelf.relics().isEmpty()) {
            g.drawString(font, Component.translatable("mythic_age.altar.relics.empty"), relicX(0), y + 24, DIM, false);
        }
        for (int i = 0; i < shelf.relics().size(); i++) {
            g.renderItem(shelf.relics().get(i), relicX(i), y + 19);
        }
        if (shelf.relics().size() == 1) {
            g.drawString(font, shelf.relics().get(0).getHoverName(), relicX(0) + 74, y + 24, TEXT, false);
        }
    }

    private int line(GuiGraphics g, Component text, int x, int y, int w, int color) {
        for (FormattedCharSequence seq : font.split(text, w)) {
            g.drawString(font, seq, x, y, color, false);
            y += 10;
        }
        return y;
    }

    private int progress(GuiGraphics g, ShrineSurvey.Row r, int x, int y, int w) {
        boolean ok = r.ok();
        String count = r.bool() ? (ok ? "✔" : "✘") : r.need() == 0 ? String.valueOf(r.have()) : r.have() + "/" + r.need();
        g.drawString(font, Component.translatable("mythic_age.altar.row." + r.key()), x, y, TEXT, false);
        g.drawString(font, count, x + w - font.width(count), y, ok ? OK : BAD, false);
        y += 10;
        if (r.need() > 0 && !r.bool()) {
            int filled = Math.min(w, (int) Math.round(w * (double) r.have() / r.need()));
            g.fill(x, y, x + w, y + 2, 0x40FFFFFF);
            g.fill(x, y, x + filled, y + 2, ok ? OK : BAD);
            y += 5;
        }
        return y;
    }
}
