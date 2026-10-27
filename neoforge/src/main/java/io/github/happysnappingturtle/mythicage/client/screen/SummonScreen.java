package io.github.happysnappingturtle.mythicage.client.screen;

import io.github.happysnappingturtle.mythicage.core.summon.SummonMode;
import io.github.happysnappingturtle.mythicage.summon.SummonPayloads;
import io.github.happysnappingturtle.mythicage.summon.SummonView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;

/**
 * 召唤管理界面（T14 设计文档 §7、§11.2；传令杖右键空气或信仰面板「神话单位」打开）：
 * 顶部「占用 / 上限」与两个群体指令；每个单位一行——召唤符图标、名字、所在、生命条、模式（点击循环）、召回、遣散。
 */
public class SummonScreen extends Screen {
    private static final int W = 360;
    private static final int ROW = 24;
    /** 临时效果区占了地方：常驻单位一页 5 行（GUI 缩放 4 时整屏放得下），多的翻页。 */
    private static final int ROWS = 5;
    private static final int LIST_TOP = 44;
    private static final int BOTTOM = 26;
    /** 临时效果区（T24 §5.2）：标题一行 + 最多 3 行。 */
    private static final int TEMP = 44;
    private static final int H = LIST_TOP + ROW * ROWS + TEMP + BOTTOM + 4;
    private static final int PANEL = 0xF0201A14;
    private static final int BORDER = 0xFFB89A5A;
    private static final int TEXT = 0xFFEDE8DC;
    private static final int DIM = 0xFFB9B09C;
    private static final int GOLD = 0xFFF2D27A;
    private static final int HP = 0xFF7FD67F;
    private static final int HP_BG = 0xFF3A3328;
    private static final int REFRESH_TICKS = 20;

    private SummonView view;
    @Nullable
    private final Screen previous;
    private int left;
    private int top;
    private int scroll;
    private int ticks;

    public SummonScreen(SummonView view, @Nullable Screen previous) {
        super(Component.translatable("mythic_age.summon.title"));
        this.view = view;
        this.previous = previous;
    }

    public void update(SummonView view) {
        this.view = view;
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
        rebuildWidgets();
    }

    private int maxScroll() {
        return Math.max(0, view.rows().size() - ROWS);
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        Component all = Component.translatable("mythic_age.summon.all_follow");
        Component guard = Component.translatable("mythic_age.summon.all_guard");
        int bw1 = font.width(all) + 14;
        int bw2 = font.width(guard) + 14;
        Button b1 = Button.builder(all, b -> send(SummonPayloads.Command.all(SummonPayloads.Action.ALL_FOLLOW)))
                .bounds(left + 8, top + 22, bw1, 16).build();
        Button b2 = Button.builder(guard, b -> send(SummonPayloads.Command.all(SummonPayloads.Action.ALL_GUARD_HERE)))
                .bounds(left + 12 + bw1, top + 22, bw2, 16).build();
        b1.active = b2.active = !view.rows().isEmpty();
        addRenderableWidget(b1);
        addRenderableWidget(b2);
        for (int i = 0; i < ROWS && scroll + i < view.rows().size(); i++) {
            SummonView.Row r = view.rows().get(scroll + i);
            int y = top + LIST_TOP + i * ROW + 4;
            boolean alive = r.where() != SummonView.Where.RESPAWNING;
            Button mode = Button.builder(Component.translatable("mythic_age.summon.mode." + r.mode().id()),
                    b -> send(new SummonPayloads.Command(SummonPayloads.Action.MODE, r.unitId(), next(r)))).bounds(left + 210, y, 62, 16).build();
            mode.active = alive;
            addRenderableWidget(mode);
            Button recall = Button.builder(Component.translatable("mythic_age.summon.recall"),
                    b -> send(new SummonPayloads.Command(SummonPayloads.Action.RECALL, r.unitId(), SummonMode.FOLLOW)))
                    .bounds(left + 274, y, 38, 16).build();
            recall.active = r.recallable();
            addRenderableWidget(recall);
            addRenderableWidget(Button.builder(Component.translatable("mythic_age.summon.dismiss"), b -> confirmDismiss(r))
                    .bounds(left + 314, y, 38, 16).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + W - 58, top + H - BOTTOM + 4, 50, 16).build());
    }

    /** 模式循环：跟随 → 驻守 → 自由攻击；非战斗单位只在跟随与驻守间切换。 */
    private static SummonMode next(SummonView.Row r) {
        return switch (r.mode()) {
            case FOLLOW -> SummonMode.GUARD;
            case GUARD -> r.combat() ? SummonMode.FREE : SummonMode.FOLLOW;
            case FREE -> SummonMode.FOLLOW;
        };
    }

    private void confirmDismiss(SummonView.Row r) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new ConfirmScreen(yes -> {
            if (yes) {
                send(new SummonPayloads.Command(SummonPayloads.Action.DISMISS, r.unitId(), SummonMode.FOLLOW));
            }
            mc.setScreen(this);
        }, Component.translatable("mythic_age.summon.dismiss_confirm", name(r)),
                Component.translatable("mythic_age.summon.dismiss_confirm_body")));
    }

    private static void send(SummonPayloads.Command c) {
        PacketDistributor.sendToServer(c);
    }

    private static Component name(SummonView.Row r) {
        return r.entity().isEmpty() ? Component.literal(r.card()) : Component.translatable("entity." + r.entity().replace(':', '.'));
    }

    private static Component where(SummonView.Row r) {
        return switch (r.where()) {
            case NEAR -> Component.translatable("mythic_age.summon.where_distance", r.value());
            case UNLOADED -> Component.translatable("mythic_age.summon.where_unloaded");
            case DIMENSION -> Component.translatable("mythic_age.summon.where_dimension",
                    Component.translatableWithFallback("mythic_age.summon.dim." + r.dimension().replace(':', '.'), r.dimension()));
            case RESPAWNING -> Component.translatable("mythic_age.summon.where_respawning",
                    String.format("%d:%02d", r.value() / 60, r.value() % 60));
        };
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int s = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(scrollY)));
        if (s != scroll) {
            scroll = s;
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(previous);
    }

    /** 开着时每秒向服务端要一次新数据（生命、距离、重生倒计时会变）。 */
    @Override
    public void tick() {
        if (++ticks % REFRESH_TICKS == 0) {
            PacketDistributor.sendToServer(new SummonPayloads.Request());
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (Minecraft.getInstance().options.keyInventory.matches(keyCode, scanCode)) {
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
        g.fill(left + 6, top + LIST_TOP - 2, left + W - 6, top + LIST_TOP - 1, 0x60B89A5A);
        g.fill(left + 6, top + H - BOTTOM, left + W - 6, top + H - BOTTOM + 1, 0x60B89A5A);
        g.fill(left + 6, top + LIST_TOP + ROW * ROWS + 1, left + W - 6, top + LIST_TOP + ROW * ROWS + 2, 0x30B89A5A);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawString(font, title, left + 8, top + 8, GOLD, false);
        Component cap = Component.translatable("mythic_age.summon.cap", view.occupied(), view.cap());
        g.drawString(font, cap, left + W - 8 - font.width(cap), top + 8, view.occupied() >= view.cap() ? GOLD : DIM, false);
        renderTemps(g, top + LIST_TOP + ROW * ROWS + 5);
        if (view.rows().isEmpty()) {
            centered(g, Component.translatable("mythic_age.summon.empty"), top + LIST_TOP + 50, TEXT);
            centered(g, Component.translatable("mythic_age.summon.empty_hint"), top + LIST_TOP + 64, DIM);
            return;
        }
        for (int i = 0; i < ROWS && scroll + i < view.rows().size(); i++) {
            renderRow(g, view.rows().get(scroll + i), top + LIST_TOP + i * ROW);
        }
        if (maxScroll() > 0) {
            Component page = Component.translatable("mythic_age.summon.page", scroll + 1,
                    Math.min(scroll + ROWS, view.rows().size()), view.rows().size());
            g.drawString(font, page, left + 8, top + H - BOTTOM + 8, DIM, false);
        }
    }

    private void renderRow(GuiGraphics g, SummonView.Row r, int y) {
        int x = left + 8;
        if (!r.token().isEmpty()) {
            ItemStack icon = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(r.token())));
            g.renderItem(icon, x, y + 4);
        }
        boolean waiting = r.where() == SummonView.Where.RESPAWNING;
        g.drawString(font, name(r), x + 22, y + 3, waiting ? DIM : TEXT, false);
        g.drawString(font, where(r), x + 22, y + 13, DIM, false);
        // 生命条：未载入或等待重生时只画底
        int bx = left + 130;
        int bw = 70;
        g.fill(bx, y + 6, bx + bw, y + 11, HP_BG);
        if (r.health() >= 0 && r.maxHealth() > 0) {
            int fill = Math.round(bw * Math.min(1, r.health() / r.maxHealth()));
            g.fill(bx, y + 6, bx + fill, y + 11, HP);
            String hp = Math.round(r.health()) + " / " + Math.round(r.maxHealth());
            g.drawString(font, hp, bx + (bw - font.width(hp)) / 2, y + 13, DIM, false);
        } else {
            Component dash = Component.literal("—");
            g.drawString(font, dash, bx + (bw - font.width(dash)) / 2, y + 13, DIM, false);
        }
    }

    /** 临时效果：不占常驻槽位、只读，到期自然消失；未载入的写「未载入」，不当作消失。 */
    private void renderTemps(GuiGraphics g, int y) {
        g.drawString(font, Component.translatable("mythic_age.summon.temps"), left + 8, y, GOLD, false);
        Component note = Component.translatable("mythic_age.summon.temps_note");
        g.drawString(font, note, left + W - 8 - font.width(note), y, DIM, false);
        y += 11;
        if (view.temps().isEmpty()) {
            g.drawString(font, Component.translatable("mythic_age.summon.temps_none"), left + 14, y, DIM, false);
            return;
        }
        for (SummonView.Temp t : view.temps()) {
            Component name = t.entity().isEmpty() ? Component.translatable("mythic_age.summon.temp_unknown")
                    : BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(t.entity())).getDescription();
            Component where = switch (t.where()) {
                case NEAR -> Component.translatable("mythic_age.summon.where_distance", t.value());
                case DIMENSION -> Component.translatable("mythic_age.summon.where_dimension",
                        Component.translatableWithFallback("mythic_age.summon.dim." + t.dimension().replace(':', '.'), t.dimension()));
                default -> Component.translatable("mythic_age.summon.where_unloaded");
            };
            net.minecraft.network.chat.MutableComponent line = Component.empty().append(name).append(" ×" + t.count())
                    .append(" · ").append(where);
            if (t.secondsLeft() >= 0) {
                line.append(" · ").append(Component.translatable("mythic_age.summon.temp_left", t.secondsLeft()));
            }
            g.drawString(font, line, left + 14, y, TEXT, false);
            y += 10;
        }
    }

    private void centered(GuiGraphics g, Component text, int y, int color) {
        g.drawString(font, text, left + (W - font.width(text)) / 2, y, color, false);
    }
}
