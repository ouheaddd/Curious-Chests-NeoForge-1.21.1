package com.overyourhead.curiouschests.client.gui;

import com.overyourhead.curiouschests.CuriousChestsMod;
import com.overyourhead.curiouschests.common.menu.SpecialChestMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Every chest has its own editable 256x256 PNG background under:
 * assets/curiouschests/textures/gui/container/<chest_id>.png
 *
 * Slot positions remain menu-driven, while all panel, slot and decorative art
 * is now read from the texture instead of being painted with Java fill calls.
 */
public final class SpecialChestScreen extends AbstractContainerScreen<SpecialChestMenu> {
    private final ResourceLocation texture;

    public SpecialChestScreen(SpecialChestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = menu.kind().screenHeight();
        inventoryLabelY = imageHeight - 94;
        texture = ResourceLocation.fromNamespaceAndPath(
                CuriousChestsMod.MOD_ID,
                "textures/gui/container/" + menu.kind().id() + ".png"
        );
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(
                texture,
                leftPos,
                topPos,
                0,
                0,
                imageWidth,
                imageHeight
        );
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
