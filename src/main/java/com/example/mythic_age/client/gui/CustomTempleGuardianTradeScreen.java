package com.example.mythic_age.client.gui;

import com.example.mythic_age.MythicAgeMod;
import com.example.mythic_age.entity.CustomTempleGuardianEntity;
import com.example.mythic_age.item.GodPowerCertificateItem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class CustomTempleGuardianTradeScreen extends Screen {
    private static final int GUI_WIDTH = 294;
    private static final int GUI_HEIGHT = 188;
    private static final int LIST_X = 12;
    private static final int LIST_Y = 38;
    private static final int LIST_WIDTH = 116;
    private static final int ROW_HEIGHT = 24;
    private static final int VISIBLE_ROWS = 5;
    private static final int DETAIL_X = 142;
    private static final int DETAIL_Y = 38;
    private static final int SLOT_SIZE = 22;
    private static final Identifier TEXTURE =
            new Identifier(MythicAgeMod.MOD_ID, "textures/gui/temple_guardian_trade.png");

    private final CustomTempleGuardianEntity guardian;
    private final PlayerEntity player;
    private final List<TradeEntry> tradeEntries = new ArrayList<>();

    private ButtonWidget executeButton;
    private ButtonWidget previousButton;
    private ButtonWidget nextButton;
    private int guiLeft;
    private int guiTop;
    private int selectedTradeIndex;
    private int scrollOffset;

    public CustomTempleGuardianTradeScreen(CustomTempleGuardianEntity guardian, PlayerEntity player) {
        super(Text.translatable("gui.mythic_age.temple_guardian_trade.title"));
        this.guardian = guardian;
        this.player = player;
        this.initTradeEntries();
    }

    private void initTradeEntries() {
        tradeEntries.clear();
        if (guardian != null) {
            guardian.getTradeOffers().entrySet().stream()
                    .sorted(Comparator
                            .comparing((Map.Entry<ItemStack, ItemStack> entry) -> entry.getKey().getItem().toString())
                            .thenComparing(entry -> entry.getValue().getItem().toString())
                            .thenComparingInt(entry -> entry.getKey().getCount()))
                    .forEach(entry -> tradeEntries.add(new TradeEntry(entry.getKey().copy(), entry.getValue().copy())));
        }
        if (tradeEntries.isEmpty()) {
            tradeEntries.add(new TradeEntry(new ItemStack(Items.BARRIER), new ItemStack(Items.BARRIER)));
        }
    }

    @Override
    protected void init() {
        super.init();
        this.guiLeft = (this.width - GUI_WIDTH) / 2;
        this.guiTop = (this.height - GUI_HEIGHT) / 2;
        int footerY = this.guiTop + GUI_HEIGHT - 28;

        this.previousButton = this.addDrawableChild(ButtonWidget.builder(
                        Text.literal("<"),
                        button -> selectTrade(this.selectedTradeIndex - 1))
                .tooltip(Tooltip.of(Text.translatable("gui.mythic_age.temple_guardian_trade.previous")))
                .dimensions(this.guiLeft + LIST_X, footerY, 28, 20)
                .build());

        this.nextButton = this.addDrawableChild(ButtonWidget.builder(
                        Text.literal(">"),
                        button -> selectTrade(this.selectedTradeIndex + 1))
                .tooltip(Tooltip.of(Text.translatable("gui.mythic_age.temple_guardian_trade.next")))
                .dimensions(this.guiLeft + LIST_X + 36, footerY, 28, 20)
                .build());

        this.executeButton = this.addDrawableChild(ButtonWidget.builder(
                        Text.translatable("gui.mythic_age.temple_guardian_trade.execute"),
                        button -> executeSelectedTrade())
                .dimensions(this.guiLeft + GUI_WIDTH - 128, footerY, 82, 20)
                .build());

        this.addDrawableChild(ButtonWidget.builder(
                        Text.translatable("gui.mythic_age.temple_guardian_trade.close"),
                        button -> this.close())
                .dimensions(this.guiLeft + GUI_WIDTH - 40, footerY, 28, 20)
                .build());

        updateButtonStates();
    }

    private void selectTrade(int index) {
        this.selectedTradeIndex = MathHelper.clamp(index, 0, Math.max(0, tradeEntries.size() - 1));
        if (this.selectedTradeIndex < this.scrollOffset) {
            this.scrollOffset = this.selectedTradeIndex;
        } else if (this.selectedTradeIndex >= this.scrollOffset + VISIBLE_ROWS) {
            this.scrollOffset = this.selectedTradeIndex - VISIBLE_ROWS + 1;
        }
        updateButtonStates();
    }

    private void updateButtonStates() {
        if (previousButton != null) {
            previousButton.active = selectedTradeIndex > 0;
        }
        if (nextButton != null) {
            nextButton.active = selectedTradeIndex < tradeEntries.size() - 1;
        }
        if (executeButton != null) {
            TradeEntry selected = getSelectedTrade();
            executeButton.active = selected != null && selected.canExecute(player, guardian);
            executeButton.setTooltip(Tooltip.of(selected == null
                    ? Text.translatable("gui.mythic_age.temple_guardian_trade.status.unavailable")
                    : selected.statusText(player, guardian)));
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        renderPanel(context);
        renderHeader(context);
        renderTradeList(context, mouseX, mouseY);
        renderTradeDetails(context, mouseX, mouseY);
        super.render(context, mouseX, mouseY, delta);
        renderHoveredItemTooltip(context, mouseX, mouseY);
    }

    private void renderPanel(DrawContext context) {
        context.drawTexture(TEXTURE, guiLeft, guiTop, 0, 0, 32, 32, 32, 32);
        context.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xE51D1A18);
        context.fill(guiLeft + 4, guiTop + 4, guiLeft + GUI_WIDTH - 4, guiTop + 30, 0xD02E2A24);
        context.fill(guiLeft + LIST_X - 4, guiTop + LIST_Y - 4,
                guiLeft + LIST_X + LIST_WIDTH + 4, guiTop + LIST_Y + ROW_HEIGHT * VISIBLE_ROWS + 4, 0xB0121110);
        context.fill(guiLeft + DETAIL_X - 4, guiTop + DETAIL_Y - 4,
                guiLeft + GUI_WIDTH - 12, guiTop + GUI_HEIGHT - 36, 0xB0121110);
        context.drawBorder(guiLeft, guiTop, GUI_WIDTH, GUI_HEIGHT, 0xFFB99654);
        context.drawBorder(guiLeft + LIST_X - 4, guiTop + LIST_Y - 4,
                LIST_WIDTH + 8, ROW_HEIGHT * VISIBLE_ROWS + 8, 0xAAE4C27A);
        context.drawBorder(guiLeft + DETAIL_X - 4, guiTop + DETAIL_Y - 4,
                GUI_WIDTH - DETAIL_X - 8, GUI_HEIGHT - DETAIL_Y - 32, 0xAAE4C27A);
    }

    private void renderHeader(DrawContext context) {
        context.drawTextWithShadow(textRenderer, title, guiLeft + 12, guiTop + 10, 0xFFEACB78);
        Text civ = Text.translatable(
                "gui.mythic_age.temple_guardian_trade.civilization",
                civilizationName()
        ).formatted(Formatting.GRAY);
        context.drawText(textRenderer, civ, guiLeft + GUI_WIDTH - textRenderer.getWidth(civ) - 12, guiTop + 11, 0xFFCFC6B2, false);
    }

    private void renderTradeList(DrawContext context, int mouseX, int mouseY) {
        int maxRows = Math.min(VISIBLE_ROWS, tradeEntries.size());
        for (int row = 0; row < maxRows; row++) {
            int index = scrollOffset + row;
            if (index >= tradeEntries.size()) {
                break;
            }
            TradeEntry entry = tradeEntries.get(index);
            int x = guiLeft + LIST_X;
            int y = guiTop + LIST_Y + row * ROW_HEIGHT;
            boolean selected = index == selectedTradeIndex;
            boolean hovered = isPointWithin(mouseX, mouseY, x, y, LIST_WIDTH, ROW_HEIGHT - 2);
            int fill = selected ? 0xD05E4630 : hovered ? 0xA0483A2B : 0x70241F1A;
            context.fill(x, y, x + LIST_WIDTH, y + ROW_HEIGHT - 2, fill);
            context.drawItem(entry.output, x + 4, y + 3);
            String name = entry.shortOutputName();
            context.drawText(textRenderer, trimToWidth(name, LIST_WIDTH - 29), x + 25, y + 5, 0xFFE8D7B0, false);
            context.drawText(textRenderer, entry.input.getCount() + "x", x + 25, y + 15, 0xFFBFB4A0, false);
        }
        if (tradeEntries.size() > VISIBLE_ROWS) {
            Text count = Text.translatable("gui.mythic_age.temple_guardian_trade.more_trades", tradeEntries.size() - VISIBLE_ROWS);
            context.drawText(textRenderer, count, guiLeft + LIST_X, guiTop + LIST_Y + ROW_HEIGHT * VISIBLE_ROWS + 5, 0xFFBFB4A0, false);
        }
    }

    private void renderTradeDetails(DrawContext context, int mouseX, int mouseY) {
        TradeEntry trade = getSelectedTrade();
        if (trade == null) {
            return;
        }

        int inputX = guiLeft + DETAIL_X + 14;
        int outputX = guiLeft + DETAIL_X + 112;
        int slotY = guiTop + DETAIL_Y + 24;
        renderSlot(context, inputX, slotY, trade.input);
        renderSlot(context, outputX, slotY, trade.output);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(">"), guiLeft + DETAIL_X + 83, slotY + 7, 0xFFEACB78);

        Text selected = Text.translatable(
                "gui.mythic_age.temple_guardian_trade.selected",
                trade.input.getName(),
                trade.output.getName()
        );
        context.drawText(textRenderer, trimToWidth(selected.getString(), GUI_WIDTH - DETAIL_X - 26),
                guiLeft + DETAIL_X + 8, guiTop + DETAIL_Y + 5, 0xFFE8D7B0, false);

        int owned = trade.countPlayerItems(player);
        int statusColor = trade.canExecute(player, guardian) ? 0xFF70D37E : 0xFFE08B72;
        context.drawText(textRenderer,
                Text.translatable("gui.mythic_age.temple_guardian_trade.inventory", owned, trade.input.getCount()),
                guiLeft + DETAIL_X + 8, guiTop + DETAIL_Y + 68, 0xFFCFC6B2, false);
        context.drawText(textRenderer, trade.statusText(player, guardian),
                guiLeft + DETAIL_X + 8, guiTop + DETAIL_Y + 82, statusColor, false);
        if (guardian != null && guardian.hasCooldown(trade.output)) {
            context.drawText(textRenderer,
                    Text.translatable("gui.mythic_age.temple_guardian_trade.cooldown", guardian.getTradeCooldownSeconds(trade.output)),
                    guiLeft + DETAIL_X + 8, guiTop + DETAIL_Y + 96, 0xFFB9D8FF, false);
        }

        if (isPointWithin(mouseX, mouseY, inputX, slotY, SLOT_SIZE, SLOT_SIZE)) {
            context.drawItemTooltip(textRenderer, trade.input, mouseX, mouseY);
        } else if (isPointWithin(mouseX, mouseY, outputX, slotY, SLOT_SIZE, SLOT_SIZE)) {
            context.drawItemTooltip(textRenderer, trade.output, mouseX, mouseY);
        }
    }

    private void renderSlot(DrawContext context, int x, int y, ItemStack stack) {
        context.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xFF2B241D);
        context.drawBorder(x, y, SLOT_SIZE, SLOT_SIZE, 0xFFC9A45F);
        context.drawItem(stack, x + 3, y + 3);
        context.drawItemInSlot(textRenderer, stack, x + 3, y + 3);
    }

    private void renderHoveredItemTooltip(DrawContext context, int mouseX, int mouseY) {
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int index = scrollOffset + row;
            if (index >= tradeEntries.size()) {
                break;
            }
            int x = guiLeft + LIST_X + 4;
            int y = guiTop + LIST_Y + row * ROW_HEIGHT + 3;
            if (isPointWithin(mouseX, mouseY, x, y, 16, 16)) {
                context.drawItemTooltip(textRenderer, tradeEntries.get(index).output, mouseX, mouseY);
                return;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int row = 0; row < VISIBLE_ROWS; row++) {
                int index = scrollOffset + row;
                int x = guiLeft + LIST_X;
                int y = guiTop + LIST_Y + row * ROW_HEIGHT;
                if (index < tradeEntries.size() && isPointWithin(mouseX, mouseY, x, y, LIST_WIDTH, ROW_HEIGHT - 2)) {
                    selectTrade(index);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (tradeEntries.size() > VISIBLE_ROWS && isPointWithin(mouseX, mouseY,
                guiLeft + LIST_X - 4, guiTop + LIST_Y - 4, LIST_WIDTH + 8, ROW_HEIGHT * VISIBLE_ROWS + 8)) {
            int maxScroll = Math.max(0, tradeEntries.size() - VISIBLE_ROWS);
            this.scrollOffset = MathHelper.clamp(this.scrollOffset - (int) Math.signum(amount), 0, maxScroll);
            if (selectedTradeIndex < scrollOffset || selectedTradeIndex >= scrollOffset + VISIBLE_ROWS) {
                selectTrade(scrollOffset);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    private void executeSelectedTrade() {
        TradeEntry trade = getSelectedTrade();
        if (trade == null || this.client == null || this.client.player == null || this.guardian == null || !trade.canExecute(player, guardian)) {
            return;
        }

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(this.guardian.getId());
        buf.writeItemStack(trade.input.copy());
        buf.writeItemStack(trade.output.copy());
        ClientPlayNetworking.send(
                com.example.mythic_age.network.ModNetworking.EXECUTE_CUSTOM_TEMPLE_GUARDIAN_TRADE,
                buf
        );
    }

    private TradeEntry getSelectedTrade() {
        if (selectedTradeIndex < 0 || selectedTradeIndex >= tradeEntries.size()) {
            return null;
        }
        return tradeEntries.get(selectedTradeIndex);
    }

    private String civilizationName() {
        GodPowerCertificateItem.MythologyType type = guardian != null ? guardian.getMythologyType() : GodPowerCertificateItem.MythologyType.GREEK;
        return switch (type) {
            case GREEK -> Text.translatable("mythic_age.civilization.greek").getString();
            case EGYPTIAN -> Text.translatable("mythic_age.civilization.egyptian").getString();
            case CHINESE -> Text.translatable("mythic_age.civilization.chinese").getString();
            case ATLANTEAN -> Text.translatable("mythic_age.civilization.atlantean").getString();
            case NORDIC -> Text.translatable("mythic_age.civilization.nordic").getString();
        };
    }

    private String trimToWidth(String text, int maxWidth) {
        return textRenderer.trimToWidth(text, maxWidth);
    }

    private boolean isPointWithin(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private static class TradeEntry {
        final ItemStack input;
        final ItemStack output;

        TradeEntry(ItemStack input, ItemStack output) {
            this.input = input;
            this.output = output;
        }

        boolean canExecute(PlayerEntity player, CustomTempleGuardianEntity guardian) {
            if (output.isOf(Items.BARRIER)) {
                return false;
            }
            if (countPlayerItems(player) < input.getCount()) {
                return false;
            }
            return guardian == null || guardian.getTradeCooldownSeconds(output) <= 0L;
        }

        Text statusText(PlayerEntity player, CustomTempleGuardianEntity guardian) {
            if (output.isOf(Items.BARRIER)) {
                return Text.translatable("gui.mythic_age.temple_guardian_trade.status.unavailable").formatted(Formatting.RED);
            }
            if (countPlayerItems(player) < input.getCount()) {
                return Text.translatable("gui.mythic_age.temple_guardian_trade.status.insufficient").formatted(Formatting.RED);
            }
            if (guardian != null && guardian.getTradeCooldownSeconds(output) > 0L) {
                return Text.translatable("gui.mythic_age.temple_guardian_trade.status.cooldown").formatted(Formatting.AQUA);
            }
            return Text.translatable("gui.mythic_age.temple_guardian_trade.status.available").formatted(Formatting.GREEN);
        }

        int countPlayerItems(PlayerEntity player) {
            int count = 0;
            if (player == null) {
                return count;
            }
            for (int i = 0; i < player.getInventory().size(); i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (stack.getItem() == input.getItem()) {
                    count += stack.getCount();
                }
            }
            return count;
        }

        String shortOutputName() {
            return output.getName().getString();
        }
    }
}
