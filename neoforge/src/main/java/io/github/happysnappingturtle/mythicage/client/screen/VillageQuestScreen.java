package io.github.happysnappingturtle.mythicage.client.screen;

import io.github.happysnappingturtle.mythicage.village.quest.QuestPayloads;
import io.github.happysnappingturtle.mythicage.village.quest.QuestView;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.UUID;

/**
 * 委托页（T21 设计文档 §9.1）：每条任务两行——标题与进度；状态、剩余日、范围、奖励类别——右边是接取 / 交付 / 放弃。
 * 底部「你已接 N / 3」「你在本村的贡献：N」。同村务界面一样的列表风格（统一的村庄总览在 T24）。
 */
public class VillageQuestScreen extends Screen {
    private static final int W = 380;
    private static final int H = 220;
    private static final int ROW = 30;
    private static final int ROWS = 5;
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
        return Math.max(0, view.rows().size() - ROWS);
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        List<QuestView.Row> rows = view.rows();
        for (int i = scroll; i < rows.size() && i < scroll + ROWS; i++) {
            QuestView.Row r = rows.get(i);
            int y = top + 30 + (i - scroll) * ROW + 6;
            int x = left + W - 8;
            if (r.state().equals("open") || r.state().equals("paused")) {
                if (r.accepted()) {
                    x = button(x, y, "abandon", r);  // 与 QuestService.ABANDON 等一致
                }
                if (r.canDeliver()) {
                    x = button(x, y, "deliver", r);
                } else if (!r.accepted() && r.refusal().isEmpty()) {
                    button(x, y, "accept", r);
                }
            }
        }
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
            g.drawString(font, Component.translatable("mythic_age.quest.screen.disabled"), left + 8, top + 19, WARN, false);
        }
        List<QuestView.Row> rows = view.rows();
        if (rows.isEmpty()) {
            g.drawString(font, Component.translatable("mythic_age.quest.screen.empty"), left + 8, top + 36, DIM, false);
        }
        for (int i = scroll; i < rows.size() && i < scroll + ROWS; i++) {
            QuestView.Row r = rows.get(i);
            int y = top + 30 + (i - scroll) * ROW;
            g.fill(left + 4, y, left + W - 4, y + ROW - 2, 0x30FFFFFF);
            boolean open = r.state().equals("open");
            Component title = VillageAffairsScreen.line(r.title());
            g.drawString(font, Component.translatable("mythic_age.quest.screen.progress", title, r.done(), r.need()),
                    left + 8, y + 4, open ? TEXT : DIM, false);
            g.drawString(font, detail(r), left + 8, y + 16, color(r), false);
        }
        g.drawString(font, Component.translatable("mythic_age.quest.screen.footer", view.active(), view.max(), view.contribution()),
                left + 8, top + H - 18, DIM, false);
        super.render(g, mouseX, mouseY, partial);
    }

    /** 第二行：状态（或剩余日）· 范围 · 奖励类别 · 我的份额 / 不能接的原因。 */
    private static Component detail(QuestView.Row r) {
        MutableComponent c = Component.empty();
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
        int s = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(dy)));
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
