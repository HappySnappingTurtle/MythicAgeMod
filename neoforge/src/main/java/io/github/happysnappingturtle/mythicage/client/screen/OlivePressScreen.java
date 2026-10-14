package io.github.happysnappingturtle.mythicage.client.screen;

import io.github.happysnappingturtle.mythicage.content.greek.agri.OlivePressMenu;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** 压榨器界面：大理石底、木纹边框（自绘 C 类贴图），一支随进度填充的箭头。 */
public class OlivePressScreen extends AbstractContainerScreen<OlivePressMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "textures/gui/olive_press.png");

    public OlivePressScreen(OlivePressMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
        int w = Math.round(menu.progress() * 24);
        if (w > 0) {
            // 箭头填充图在贴图 (176, 0)，24×17
            g.blit(TEXTURE, x + 79, y + 37, 176, 0, w, 17);
        }
    }
}
