package io.github.happysnappingturtle.mythicage.client.screen;

import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.faith.FaithPayloads;
import io.github.happysnappingturtle.mythicage.faith.FaithView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 信仰面板（T11a 设计文档 §7.3；按 J 或祭司界面「信仰」打开）：顶部五个文明页签与全局上限；
 * 已接触的文明左栏是时代、次神、主神认可与下一次晋升的条件，右栏是你的村庄与恩宠来源；底部可重看晋升界面。
 */
public class FaithScreen extends Screen {
    private static final int W = 400;
    private static final int H = 244;
    private static final int PANEL = 0xF0201A14;
    private static final int BORDER = 0xFFB89A5A;
    private static final int TEXT = 0xFFEDE8DC;
    private static final int DIM = 0xFFB9B09C;
    private static final int GOLD = 0xFFF2D27A;
    private static final int OK = 0xFF7FD67F;
    private static final int BAD = 0xFFFF8A70;
    private static final int COL = 186;
    /** 底部两行按钮：上行重看晋升，下行神话单位、善恶与贡献、完成。 */
    private static final int BOTTOM = 46;
    private static final int REFRESH_TICKS = 40;
    private static final List<Age> MINOR_AGES = List.of(Age.CLASSICAL, Age.HEROIC, Age.MYTHIC);

    /** 上次看的文明，下次打开时还停在这里。 */
    @Nullable
    private static Civilization lastCiv;

    private FaithView view;
    @Nullable
    private final Screen previous;
    private Civilization civ;
    private int left;
    private int top;
    private int ticks;

    public FaithScreen(FaithView view, @Nullable Screen previous) {
        super(Component.translatable("mythic_age.faith.title"));
        this.view = view;
        this.previous = previous;
        this.civ = initialCiv(view);
    }

    private static Civilization initialCiv(FaithView view) {
        if (lastCiv != null && pageOf(view, lastCiv).status() != FaithView.Status.UNOPENED) {
            return lastCiv;
        }
        return view.pages().stream().filter(p -> p.status() == FaithView.Status.CONTACTED).findFirst()
                .or(() -> view.pages().stream().filter(p -> p.status() != FaithView.Status.UNOPENED).findFirst())
                .map(FaithView.Page::civ).orElse(Civilization.GREEK);
    }

    private static FaithView.Page pageOf(FaithView view, Civilization civ) {
        return view.pages().stream().filter(p -> p.civ() == civ).findFirst().orElseThrow();
    }

    public void update(FaithView view) {
        this.view = view;
        rebuildWidgets();
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        int tw = (W - 16) / Civilization.values().length;
        for (int i = 0; i < Civilization.values().length; i++) {
            Civilization c = Civilization.values()[i];
            Button b = Button.builder(Component.translatable("mythic_age.civ." + c.id()), btn -> {
                civ = c;
                lastCiv = c;
                rebuildWidgets();
            }).bounds(left + 8 + i * tw, top + 22, tw - 2, 16).build();
            b.active = pageOf(view, c).status() != FaithView.Status.UNOPENED;
            addRenderableWidget(b);
        }
        FaithView.Page p = pageOf(view, civ);
        if (p.status() == FaithView.Status.CONTACTED) {
            int x = left + 8 + font.width(Component.translatable("mythic_age.faith.replay")) + 6;
            for (Age a : MINOR_AGES) {
                if (!p.age().atLeast(a)) {
                    break;
                }
                Component label = Component.translatable("mythic_age.age." + a.id());
                int bw = font.width(label) + 12;
                addRenderableWidget(Button.builder(label, btn -> PacketDistributor.sendToServer(new FaithPayloads.Replay(civ, a)))
                        .bounds(x, top + H - BOTTOM + 4, bw, 16).build());
                x += bw + 4;
            }
        }
        int row2 = top + H - 22;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + W - 58, row2, 50, 16).build());
        // 神话单位（T14 D8）：打开召唤管理界面，关闭后回到这里
        Component units = Component.translatable("mythic_age.summon.title");
        int uw = font.width(units) + 12;
        addRenderableWidget(Button.builder(units, b -> PacketDistributor.sendToServer(
                        new io.github.happysnappingturtle.mythicage.summon.SummonPayloads.Request()))
                .bounds(left + W - 62 - uw, row2, uw, 16).build());
        // 《神话手札》（T24 §6.1，D2）：J 面板讲数据，手札讲规则
        Component book = Component.translatable("mythic_age.handbook.open");
        int bw2 = font.width(book) + 12;
        addRenderableWidget(Button.builder(book, b -> vazkii.patchouli.api.PatchouliAPI.get().openBookGUI(
                        io.github.happysnappingturtle.mythicage.handbook.Handbook.ID))
                .bounds(left + 8, row2, bw2, 16).build());
        // 善恶与贡献（T24 §3.1）：只看自己
        Component conduct = Component.translatable("mythic_age.conduct.screen.title");
        int cw = font.width(conduct) + 12;
        addRenderableWidget(Button.builder(conduct, b -> PacketDistributor.sendToServer(
                        new io.github.happysnappingturtle.mythicage.conduct.ConductPayloads.Request()))
                .bounds(left + W - 66 - uw - cw, row2, cw, 16).build());
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(previous);
    }

    /** 打开期间每 40 刻向服务端要一次新数据（恩宠、来源的今日产出会变）。 */
    @Override
    public void tick() {
        if (++ticks % REFRESH_TICKS == 0) {
            PacketDistributor.sendToServer(new FaithPayloads.Request());
        }
    }

    /** 再按一次 J（或背包键）关闭，与原版背包一致。 */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (io.github.happysnappingturtle.mythicage.client.FaithKeys.OPEN.matches(keyCode, scanCode)
                || Minecraft.getInstance().options.keyInventory.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        g.fill(left, top, left + W, top + H, PANEL);
        g.renderOutline(left, top, W, H, BORDER);
        g.fill(left + 6, top + H - BOTTOM, left + W - 6, top + H - BOTTOM + 1, 0x60B89A5A);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawString(font, title, left + 8, top + 8, GOLD, false);
        Component caps = Component.translatable("mythic_age.faith.caps", view.mythicUnits(), view.relicSlots());
        g.drawString(font, caps, left + W - 8 - font.width(caps), top + 8, DIM, false);
        // 选中的文明：页签下方一条金线
        int tw = (W - 16) / Civilization.values().length;
        int tx = left + 8 + civ.ordinal() * tw;
        g.fill(tx + 2, top + 39, tx + tw - 4, top + 40, BORDER);
        FaithView.Page p = pageOf(view, civ);
        switch (p.status()) {
            case UNOPENED -> centered(g, Component.translatable("mythic_age.faith.unopened"), top + 90, DIM);
            case UNCONTACTED -> {
                int y = centered(g, Component.translatable("mythic_age.faith.uncontacted"), top + 80, TEXT);
                centered(g, Component.translatable(p.hint()), y + 6, DIM);
            }
            case CONTACTED -> {
                renderLeft(g, p, left + 10, top + 46);
                renderRight(g, p, left + W - 10 - COL, top + 46);
                g.drawString(font, Component.translatable("mythic_age.faith.replay"), left + 8, top + H - BOTTOM + 8, DIM, false);
            }
        }
    }

    private void renderLeft(GuiGraphics g, FaithView.Page p, int x, int y) {
        int limit = top + H - BOTTOM - 10;
        Component age = Component.translatable("mythic_age.faith.age", Component.translatable("mythic_age.age." + p.age().id()));
        g.drawString(font, age, x, y, GOLD, false);
        Component favor = Component.translatable(p.favorKey()).copy().append(" " + p.favor() + " / " + p.cap());
        g.drawString(font, favor, x + COL - font.width(favor), y, TEXT, false);
        y += 14;
        g.drawString(font, Component.translatable("mythic_age.faith.minor_gods"), x, y, BORDER, false);
        y += 11;
        for (int i = 0; i < MINOR_AGES.size(); i++) {
            Age a = MINOR_AGES.get(i);
            String god = i < p.minorGods().size() ? p.minorGods().get(i) : "";
            Component name = god.isEmpty() ? Component.literal("—") : Component.translatable("mythic_age.god." + god);
            g.drawString(font, Component.translatable("mythic_age.age." + a.id()), x + 4, y, DIM, false);
            g.drawString(font, name, x + 44, y, god.isEmpty() ? DIM : TEXT, false);
            y += 10;
        }
        y += 4;
        g.drawString(font, Component.translatable("mythic_age.faith.approvals"), x, y, BORDER, false);
        y += 11;
        for (FaithView.Approval a : p.approvals()) {
            boolean ok = a.age() != Age.NONE;
            g.drawString(font, Component.translatable("mythic_age.god." + a.patron()), x + 4, y, TEXT, false);
            Component v = ok ? Component.translatable("mythic_age.age." + a.age().id()) : Component.translatable("mythic_age.faith.not_approved");
            g.drawString(font, v, x + COL - font.width(v), y, ok ? OK : DIM, false);
            y += 10;
        }
        y += 4;
        if (p.nextAge() == Age.NONE) {
            g.drawString(font, Component.translatable(p.age() == Age.MYTHIC ? "mythic_age.age_unlocked.top" : "mythic_age.faith.no_next"),
                    x, y, DIM, false);
            return;
        }
        g.drawString(font, Component.translatable("mythic_age.faith.next", Component.translatable("mythic_age.age." + p.nextAge().id())),
                x, y, BORDER, false);
        y += 11;
        for (FaithView.Line l : p.next()) {
            if (y > limit) {
                break;
            }
            int lx = x + 4;
            if (!l.icon().isEmpty()) {
                g.pose().pushPose();
                g.pose().translate(lx, y - 1, 0);
                g.pose().scale(0.625F, 0.625F, 1);
                g.renderItem(l.icon(), 0, 0);
                g.pose().popPose();
            } else {
                g.drawString(font, l.met() ? "✔" : "✘", lx, y, l.met() ? OK : BAD, false);
            }
            g.drawString(font, l.label(), lx + 12, y, l.met() ? TEXT : BAD, false);
            g.drawString(font, l.progress(), x + COL - font.width(l.progress()), y, l.met() ? OK : BAD, false);
            y += 10;
        }
    }

    private void renderRight(GuiGraphics g, FaithView.Page p, int x, int y) {
        int limit = top + H - BOTTOM - 10;
        y = section(g, Component.translatable("mythic_age.faith.villages"), p.villages(), "mythic_age.faith.no_villages", x, y, limit);
        y += 4;
        y = section(g, Component.translatable("mythic_age.faith.sources"), p.sources(), "mythic_age.faith.no_sources", x, y, limit);
        for (Component f : p.features()) {
            if (y > limit) {
                break;
            }
            y += 4;
            for (FormattedCharSequence seq : font.split(f, COL)) {
                g.drawString(font, seq, x, y, TEXT, false);
                y += 10;
            }
        }
    }

    /** 一段：标题，然后每行「名称 …… 右侧数字」+ 一行灰字（说明 · 坐标）。 */
    private int section(GuiGraphics g, Component head, List<FaithView.Row> rows, String emptyKey, int x, int y, int limit) {
        g.drawString(font, head, x, y, BORDER, false);
        y += 11;
        if (rows.isEmpty()) {
            g.drawString(font, Component.translatable(emptyKey), x + 4, y, DIM, false);
            return y + 10;
        }
        for (int i = 0; i < rows.size(); i++) {
            if (y + 20 > limit) {
                g.drawString(font, Component.translatable("mythic_age.faith.more", rows.size() - i), x + 4, y, DIM, false);
                return y + 10;
            }
            FaithView.Row r = rows.get(i);
            int vw = font.width(r.value());
            g.drawString(font, font.substrByWidth(r.name(), COL - vw - 10).getString(), x + 4, y, TEXT, false);
            g.drawString(font, r.value(), x + COL - vw, y, DIM, false);
            Component pos = Component.translatable("mythic_age.faith.pos", r.pos().getX(), r.pos().getY(), r.pos().getZ());
            Component sub = !r.hasPos() ? r.note()
                    : r.note().getString().isEmpty() ? pos : Component.translatable("mythic_age.faith.row_sub", r.note(), pos);
            // 说明较长时折成两行（如神坛：等级 · 信徒 · 今日 · 坐标）
            List<FormattedCharSequence> subs = font.split(sub, COL - 10);
            for (int k = 0; k < Math.min(2, subs.size()); k++) {
                g.drawString(font, subs.get(k), x + 10, y + 10 + k * 10, DIM, false);
            }
            y += 12 + 10 * Math.min(2, subs.size());
        }
        return y;
    }

    private int centered(GuiGraphics g, Component text, int y, int color) {
        for (FormattedCharSequence seq : font.split(text, W - 40)) {
            g.drawString(font, seq, left + (W - font.width(seq)) / 2, y, color, false);
            y += 10;
        }
        return y;
    }
}
