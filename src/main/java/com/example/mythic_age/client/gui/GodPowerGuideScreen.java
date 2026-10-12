package com.example.mythic_age.client.gui;

import com.example.mythic_age.item.GodPowerCertificateItem;
import com.example.mythic_age.item.ModItems;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GodPowerGuideScreen extends Screen {
    private static final int PANEL_WIDTH = 318;
    private static final int PANEL_HEIGHT = 216;
    private static final int NAV_WIDTH = 84;
    private static final int ROW_HEIGHT = 20;
    private static final int SPELLS_PER_PAGE = 6;

    private final String civilizationId;
    private final GuideCivilization civilization;
    private final PlayerEntity player;
    private final List<GuideTab> tabs = List.of(GuideTab.OVERVIEW, GuideTab.SPELLS, GuideTab.TEMPLE_STATUS, GuideTab.CERTIFICATES);

    private ButtonWidget previousPageButton;
    private ButtonWidget nextPageButton;
    private int guiLeft;
    private int guiTop;
    private int selectedTabIndex;
    private int spellPage;

    public GodPowerGuideScreen(String civilizationId, PlayerEntity player) {
        super(Text.translatable("gui.mythic_age.god_power_guide.title"));
        this.civilizationId = normalizeCivilization(civilizationId);
        this.civilization = GuideCivilization.byId(this.civilizationId);
        this.player = player;
    }

    @Override
    protected void init() {
        super.init();
        this.guiLeft = (this.width - PANEL_WIDTH) / 2;
        this.guiTop = (this.height - PANEL_HEIGHT) / 2;
        this.clearChildren();

        int navX = guiLeft + 10;
        int navY = guiTop + 34;
        for (int i = 0; i < tabs.size(); i++) {
            final int tabIndex = i;
            this.addDrawableChild(ButtonWidget.builder(tabs.get(i).title(), button -> {
                        this.selectedTabIndex = tabIndex;
                        this.spellPage = 0;
                        updatePageButtons();
                    })
                    .tooltip(Tooltip.of(tabs.get(i).tooltip()))
                    .dimensions(navX, navY + i * 24, NAV_WIDTH, 20)
                    .build());
        }

        int footerY = guiTop + PANEL_HEIGHT - 28;
        this.previousPageButton = this.addDrawableChild(ButtonWidget.builder(
                        Text.literal("<"),
                        button -> {
                            this.spellPage = Math.max(0, this.spellPage - 1);
                            updatePageButtons();
                        })
                .tooltip(Tooltip.of(Text.translatable("gui.mythic_age.god_power_guide.previous_page")))
                .dimensions(guiLeft + PANEL_WIDTH - 118, footerY, 26, 20)
                .build());
        this.nextPageButton = this.addDrawableChild(ButtonWidget.builder(
                        Text.literal(">"),
                        button -> {
                            this.spellPage = Math.min(maxSpellPage(), this.spellPage + 1);
                            updatePageButtons();
                        })
                .tooltip(Tooltip.of(Text.translatable("gui.mythic_age.god_power_guide.next_page")))
                .dimensions(guiLeft + PANEL_WIDTH - 88, footerY, 26, 20)
                .build());
        this.addDrawableChild(ButtonWidget.builder(
                        Text.translatable("gui.mythic_age.god_power_guide.close"),
                        button -> this.close())
                .dimensions(guiLeft + PANEL_WIDTH - 56, footerY, 44, 20)
                .build());
        updatePageButtons();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        renderPanel(context);
        renderHeader(context);
        renderNavigation(context);
        renderSelectedContent(context, mouseX, mouseY);
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderPanel(DrawContext context) {
        context.fill(guiLeft, guiTop, guiLeft + PANEL_WIDTH, guiTop + PANEL_HEIGHT, 0xEA151817);
        context.fill(guiLeft + 4, guiTop + 4, guiLeft + PANEL_WIDTH - 4, guiTop + 28, 0xD02B302C);
        context.fill(guiLeft + 8, guiTop + 32, guiLeft + NAV_WIDTH + 18, guiTop + PANEL_HEIGHT - 34, 0xB00F1110);
        context.fill(guiLeft + NAV_WIDTH + 24, guiTop + 32, guiLeft + PANEL_WIDTH - 10, guiTop + PANEL_HEIGHT - 34, 0xB0121413);
        context.drawBorder(guiLeft, guiTop, PANEL_WIDTH, PANEL_HEIGHT, civilization.color());
        context.drawBorder(guiLeft + 8, guiTop + 32, NAV_WIDTH + 10, PANEL_HEIGHT - 66, 0x886F7A6B);
        context.drawBorder(guiLeft + NAV_WIDTH + 24, guiTop + 32, PANEL_WIDTH - NAV_WIDTH - 34, PANEL_HEIGHT - 66, 0x886F7A6B);
    }

    private void renderHeader(DrawContext context) {
        context.drawTextWithShadow(textRenderer, title, guiLeft + 12, guiTop + 10, 0xFFF0E5C0);
        Text civ = Text.translatable("gui.mythic_age.god_power_guide.civilization", civilization.displayName())
                .formatted(Formatting.GRAY);
        context.drawText(textRenderer, civ, guiLeft + PANEL_WIDTH - textRenderer.getWidth(civ) - 12, guiTop + 11, 0xFFC9D0C4, false);
    }

    private void renderNavigation(DrawContext context) {
        for (int i = 0; i < tabs.size(); i++) {
            int y = guiTop + 34 + i * 24;
            if (i == selectedTabIndex) {
                context.fill(guiLeft + 12, y + 1, guiLeft + NAV_WIDTH + 14, y + 19, 0x665D7B5B);
            }
        }
    }

    private void renderSelectedContent(DrawContext context, int mouseX, int mouseY) {
        int x = guiLeft + NAV_WIDTH + 34;
        int y = guiTop + 42;
        int width = PANEL_WIDTH - NAV_WIDTH - 52;
        switch (tabs.get(selectedTabIndex)) {
            case OVERVIEW -> renderOverview(context, x, y, width);
            case SPELLS -> renderSpells(context, x, y, width, mouseX, mouseY);
            case TEMPLE_STATUS -> renderTempleStatus(context, x, y, width);
            case CERTIFICATES -> renderCertificates(context, x, y, width);
        }
    }

    private void renderOverview(DrawContext context, int x, int y, int width) {
        drawSectionTitle(context, Text.translatable("gui.mythic_age.god_power_guide.overview.title"), x, y);
        drawWrapped(context, civilization.overview(), x, y + 18, width, 0xFFD8D2C4);
        drawSectionTitle(context, Text.translatable("gui.mythic_age.god_power_guide.acquisition.title"), x, y + 76);
        drawWrapped(context, civilization.acquisition(), x, y + 94, width, 0xFFC9D0C4);
        context.drawText(textRenderer,
                Text.translatable("gui.mythic_age.god_power_guide.spell_count", civilization.spells().size()),
                x, y + 140, 0xFFE6C36A, false);
    }

    private void renderSpells(DrawContext context, int x, int y, int width, int mouseX, int mouseY) {
        drawSectionTitle(context, Text.translatable("gui.mythic_age.god_power_guide.spells.title"), x, y);
        int start = spellPage * SPELLS_PER_PAGE;
        int end = Math.min(civilization.spells().size(), start + SPELLS_PER_PAGE);
        for (int i = start; i < end; i++) {
            GuideSpell spell = civilization.spells().get(i);
            int rowY = y + 20 + (i - start) * ROW_HEIGHT;
            context.fill(x, rowY - 2, x + width, rowY + ROW_HEIGHT - 3, i % 2 == 0 ? 0x44202620 : 0x30202620);
            context.drawItem(new ItemStack(spell.item()), x + 2, rowY);
            context.drawText(textRenderer, spell.name(), x + 23, rowY + 1, 0xFFEAD9AA, false);
            context.drawText(textRenderer,
                    Text.translatable("gui.mythic_age.god_power_guide.cost", spell.cost()),
                    x + width - 62, rowY + 1, 0xFF9ED6FF, false);
            if (isPointWithin(mouseX, mouseY, x + 2, rowY, 16, 16)) {
                context.drawItemTooltip(textRenderer, new ItemStack(spell.item()), mouseX, mouseY);
            }
        }
        context.drawText(textRenderer,
                Text.translatable("gui.mythic_age.god_power_guide.page", spellPage + 1, maxSpellPage() + 1),
                x, guiTop + PANEL_HEIGHT - 24, 0xFFBFC8BC, false);
    }

    private void renderTempleStatus(DrawContext context, int x, int y, int width) {
        drawSectionTitle(context, Text.translatable("gui.mythic_age.god_power_guide.temple_status.title"), x, y);
        String[] checklist = {
                "gui.mythic_age.god_power_guide.temple_status.guardian",
                "gui.mythic_age.god_power_guide.temple_status.certificate_source",
                "gui.mythic_age.god_power_guide.temple_status.trade_screen",
                "gui.mythic_age.god_power_guide.temple_status.statue_or_focus"
        };
        for (int i = 0; i < checklist.length; i++) {
            int rowY = y + 22 + i * 26;
            context.drawText(textRenderer, Text.literal(String.valueOf(i + 1)).formatted(Formatting.GOLD), x + 2, rowY, 0xFFE6C36A, false);
            drawWrapped(context, Text.translatable(checklist[i]), x + 20, rowY, width - 22, 0xFFD8D2C4);
        }
    }

    private void renderCertificates(DrawContext context, int x, int y, int width) {
        drawSectionTitle(context, Text.translatable("gui.mythic_age.god_power_guide.certificates.title"), x, y);
        int count = countPlayerItems(civilization.certificateItem());
        context.drawItem(new ItemStack(civilization.certificateItem()), x + 2, y + 24);
        context.drawText(textRenderer,
                Text.translatable("gui.mythic_age.god_power_guide.certificates.balance", count),
                x + 25, y + 27, 0xFFEAD9AA, false);
        drawWrapped(context, Text.translatable("gui.mythic_age.god_power_guide.certificates.hint"),
                x, y + 56, width, 0xFFC9D0C4);
        drawSectionTitle(context, Text.translatable("gui.mythic_age.god_power_guide.progress.title"), x, y + 104);
        int ownedAffordable = 0;
        for (GuideSpell spell : civilization.spells()) {
            if (count >= spell.cost()) {
                ownedAffordable++;
            }
        }
        context.drawText(textRenderer,
                Text.translatable("gui.mythic_age.god_power_guide.progress.affordable", ownedAffordable, civilization.spells().size()),
                x, y + 124, 0xFF9ED6FF, false);
    }

    private void drawSectionTitle(DrawContext context, Text text, int x, int y) {
        context.drawTextWithShadow(textRenderer, text, x, y, civilization.color());
    }

    private void drawWrapped(DrawContext context, Text text, int x, int y, int width, int color) {
        List<String> lines = wrap(text.getString(), width);
        for (int i = 0; i < lines.size(); i++) {
            context.drawText(textRenderer, lines.get(i), x, y + i * 11, color, false);
        }
    }

    private List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (textRenderer.getWidth(candidate) > width && !line.isEmpty()) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    private int countPlayerItems(Item item) {
        if (player == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private boolean isPointWithin(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private void updatePageButtons() {
        boolean spellsTab = tabs.get(selectedTabIndex) == GuideTab.SPELLS;
        if (previousPageButton != null) {
            previousPageButton.visible = spellsTab;
            previousPageButton.active = spellsTab && spellPage > 0;
        }
        if (nextPageButton != null) {
            nextPageButton.visible = spellsTab;
            nextPageButton.active = spellsTab && spellPage < maxSpellPage();
        }
    }

    private int maxSpellPage() {
        return Math.max(0, MathHelper.ceil(civilization.spells().size() / (float) SPELLS_PER_PAGE) - 1);
    }

    private static String normalizeCivilization(String id) {
        if (id == null || id.isBlank()) {
            return "general";
        }
        return id.toLowerCase(Locale.ROOT);
    }

    private enum GuideTab {
        OVERVIEW("overview"),
        SPELLS("spells"),
        TEMPLE_STATUS("temple_status"),
        CERTIFICATES("certificates");

        private final String key;

        GuideTab(String key) {
            this.key = key;
        }

        Text title() {
            return Text.translatable("gui.mythic_age.god_power_guide.tab." + key);
        }

        Text tooltip() {
            return Text.translatable("gui.mythic_age.god_power_guide.tab." + key + ".tooltip");
        }
    }

    private record GuideSpell(Item item, int cost) {
        Text name() {
            return item.getName();
        }
    }

    private record GuideCivilization(
            String id,
            Text displayName,
            Text overview,
            Text acquisition,
            Item certificateItem,
            int color,
            List<GuideSpell> spells
    ) {
        static GuideCivilization byId(String id) {
            return switch (id) {
                case "egyptian" -> egyptian();
                case "chinese" -> chinese();
                case "atlantean" -> atlantean();
                case "nordic" -> nordic();
                case "greek" -> greek();
                default -> general();
            };
        }

        static GuideCivilization greek() {
            return new GuideCivilization(
                    "greek",
                    Text.translatable("mythic_age.civilization.greek"),
                    Text.translatable("gui.mythic_age.god_power_guide.overview.greek"),
                    Text.translatable("gui.mythic_age.god_power_guide.acquisition.greek"),
                    ModItems.GOD_POWER_CERTIFICATE_GREEK,
                    0xFF6CA8FF,
                    List.of(
                            new GuideSpell(ModItems.SPELLBOOK_RESTORATION, 2),
                            new GuideSpell(ModItems.SPELLBOOK_LURE, 2),
                            new GuideSpell(ModItems.SPELLBOOK_LIGHTNING_STORM, 3),
                            new GuideSpell(ModItems.SPELLBOOK_PESTILENCE, 3),
                            new GuideSpell(ModItems.SPELLBOOK_BRONZE, 3),
                            new GuideSpell(ModItems.SPELLBOOK_CEASEFIRE, 3),
                            new GuideSpell(ModItems.SPELLBOOK_CURSE, 4),
                            new GuideSpell(ModItems.SPELLBOOK_LIGHTNING_STORM_HERA, 4),
                            new GuideSpell(ModItems.SPELLBOOK_UNDERWORLD_ARMY, 5),
                            new GuideSpell(ModItems.SPELLBOOK_PLENTY, 5),
                            new GuideSpell(ModItems.SPELLBOOK_EARTHQUAKE, 5),
                            new GuideSpell(ModItems.SPELLBOOK_UNDERWORLD_PASSAGE, 7)
                    ));
        }

        static GuideCivilization egyptian() {
            return new GuideCivilization(
                    "egyptian",
                    Text.translatable("mythic_age.civilization.egyptian"),
                    Text.translatable("gui.mythic_age.god_power_guide.overview.egyptian"),
                    Text.translatable("gui.mythic_age.god_power_guide.acquisition.egyptian"),
                    ModItems.GOD_POWER_CERTIFICATE_EGYPTIAN,
                    0xFFFFC85A,
                    List.of(
                            new GuideSpell(ModItems.SPELLBOOK_RA_RADIANCE, 2),
                            new GuideSpell(ModItems.SPELLBOOK_DESERT_STORM, 3),
                            new GuideSpell(ModItems.SPELLBOOK_EYE_OF_HORUS, 3),
                            new GuideSpell(ModItems.SPELLBOOK_SUN_RAIN, 4),
                            new GuideSpell(ModItems.SPELLBOOK_PROSPERITY, 4),
                            new GuideSpell(ModItems.SPELLBOOK_ANUBIS_JUDGMENT, 5),
                            new GuideSpell(ModItems.SPELLBOOK_LOCUST_SWARM, 5),
                            new GuideSpell(ModItems.SPELLBOOK_TORNADO, 6),
                            new GuideSpell(ModItems.SPELLBOOK_ISIS_RESURRECTION, 7),
                            new GuideSpell(ModItems.SPELLBOOK_METEOR, 8)
                    ));
        }

        static GuideCivilization chinese() {
            return new GuideCivilization(
                    "chinese",
                    Text.translatable("mythic_age.civilization.chinese"),
                    Text.translatable("gui.mythic_age.god_power_guide.overview.chinese"),
                    Text.translatable("gui.mythic_age.god_power_guide.acquisition.chinese"),
                    ModItems.GOD_POWER_CERTIFICATE_CHINESE,
                    0xFFFF6B5E,
                    List.of(
                            new GuideSpell(ModItems.SPELLBOOK_RAIN_PRAYER, 2),
                            new GuideSpell(ModItems.SPELLBOOK_EARTH_GOD_BLESSING, 3),
                            new GuideSpell(ModItems.SPELLBOOK_FIVE_THUNDER, 4),
                            new GuideSpell(ModItems.SPELLBOOK_ENLIGHTENMENT, 4),
                            new GuideSpell(ModItems.SPELLBOOK_ABUNDANCE, 5),
                            new GuideSpell(ModItems.SPELLBOOK_CRANE_BLESSING, 5),
                            new GuideSpell(ModItems.SPELLBOOK_FLOOD, 6),
                            new GuideSpell(ModItems.SPELLBOOK_CALL_TO_ARMS, 6),
                            new GuideSpell(ModItems.SPELLBOOK_TAIJI_SHIELD, 7),
                            new GuideSpell(ModItems.SPELLBOOK_RECREATION, 8)
                    ));
        }

        static GuideCivilization nordic() {
            return new GuideCivilization(
                    "nordic",
                    Text.translatable("mythic_age.civilization.nordic"),
                    Text.translatable("gui.mythic_age.god_power_guide.overview.nordic"),
                    Text.translatable("gui.mythic_age.god_power_guide.acquisition.nordic"),
                    ModItems.GOD_POWER_CERTIFICATE_NORDIC,
                    0xFFB8C4D8,
                    List.of(
                            new GuideSpell(ModItems.SPELLBOOK_FENRIR_HOWL, 2),
                            new GuideSpell(ModItems.SPELLBOOK_ODIN_EYE, 3),
                            new GuideSpell(ModItems.SPELLBOOK_THOR_WRATH, 4),
                            new GuideSpell(ModItems.SPELLBOOK_GREAT_HUNT, 4),
                            new GuideSpell(ModItems.SPELLBOOK_DWARVEN_MINE, 5),
                            new GuideSpell(ModItems.SPELLBOOK_VALHALLA_CALL, 5),
                            new GuideSpell(ModItems.SPELLBOOK_FROST, 6),
                            new GuideSpell(ModItems.SPELLBOOK_WALKING_WOODS, 6),
                            new GuideSpell(ModItems.SPELLBOOK_RAGNAROK, 7),
                            new GuideSpell(ModItems.SPELLBOOK_FIMBULWINTER, 8)
                    ));
        }

        static GuideCivilization atlantean() {
            return new GuideCivilization(
                    "atlantean",
                    Text.translatable("mythic_age.civilization.atlantean"),
                    Text.translatable("gui.mythic_age.god_power_guide.overview.atlantean"),
                    Text.translatable("gui.mythic_age.god_power_guide.acquisition.atlantean"),
                    ModItems.GOD_POWER_CERTIFICATE_ATLANTEAN,
                    0xFF62E0F0,
                    List.of(
                            new GuideSpell(ModItems.SPELLBOOK_TIDAL_SURGE, 2),
                            new GuideSpell(ModItems.SPELLBOOK_ABYSSAL_CURRENT, 3),
                            new GuideSpell(ModItems.SPELLBOOK_CRYSTAL_BARRIER, 4),
                            new GuideSpell(ModItems.SPELLBOOK_PEARL_HARVEST, 4),
                            new GuideSpell(ModItems.SPELLBOOK_ORICHALCUM_FORGE, 5),
                            new GuideSpell(ModItems.SPELLBOOK_SIREN_SONG, 6),
                            new GuideSpell(ModItems.SPELLBOOK_LEVIATHAN_CALL, 7),
                            new GuideSpell(ModItems.SPELLBOOK_ABYSSAL_RIFT, 7),
                            new GuideSpell(ModItems.SPELLBOOK_CHRONO_STASIS, 8),
                            new GuideSpell(ModItems.SPELLBOOK_OCEANIC_WRATH, 8)
                    ));
        }

        static GuideCivilization general() {
            return new GuideCivilization(
                    "general",
                    Text.translatable("gui.mythic_age.god_power_guide.general_civilization"),
                    Text.translatable("gui.mythic_age.god_power_guide.overview.general"),
                    Text.translatable("gui.mythic_age.god_power_guide.acquisition.general"),
                    ModItems.GOD_POWER_CERTIFICATE_GREEK,
                    0xFFE6C36A,
                    List.of(
                            new GuideSpell(ModItems.SPELLBOOK_RESTORATION, 2),
                            new GuideSpell(ModItems.SPELLBOOK_RAIN_PRAYER, 2),
                            new GuideSpell(ModItems.SPELLBOOK_RA_RADIANCE, 2),
                            new GuideSpell(ModItems.SPELLBOOK_FENRIR_HOWL, 2),
                            new GuideSpell(ModItems.SPELLBOOK_TIDAL_SURGE, 2)
                    ));
        }
    }
}
