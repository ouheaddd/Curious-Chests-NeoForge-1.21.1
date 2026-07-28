package com.overyourhead.curiouschests.client.gui;

import com.overyourhead.curiouschests.common.chest.ChestKind;
import com.overyourhead.curiouschests.common.menu.SpecialChestMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Vanilla-like placeholder screen. Final art can replace this renderer later
 * without changing menu coordinates or registry IDs.
 */
public final class SpecialChestScreen extends AbstractContainerScreen<SpecialChestMenu> {
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_DARK = 0xFF555555;
    private static final int SLOT_BORDER = 0xFF373737;
    private static final int SLOT_INNER = 0xFF8B8B8B;
    private final int rows;

    public SpecialChestScreen(SpecialChestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        rows = menu.kind() == ChestKind.INFERNAL ? 5 : (menu.chestSlots() + 8) / 9;
        imageHeight = menu.kind() == ChestKind.INFERNAL ? 214 : 114 + rows * 18;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        graphics.fill(x, y, x + imageWidth, y + imageHeight, PANEL_DARK);
        graphics.fill(x + 3, y + 3, x + imageWidth - 3, y + imageHeight - 3, PANEL);

        for (Slot slot : menu.slots) {
            drawSlot(graphics, x + slot.x, y + slot.y);
        }

        if (menu.kind() == ChestKind.INFERNAL) {
            graphics.fill(x + 7, y + 41, x + 169, y + 62, 0xFF3A1D18);
            graphics.fill(x + 10, y + 46, x + 166, y + 58, 0xFFB73514);
            graphics.fill(x + 14, y + 49, x + 162, y + 55, 0xFFFF8C24);
            graphics.drawString(
                    font,
                    Component.translatable("screen.curiouschests.infernal_inputs"),
                    x + 8,
                    y + 7,
                    0x6B2412,
                    false
            );
            graphics.drawString(
                    font,
                    Component.translatable("screen.curiouschests.infernal_outputs"),
                    x + 8,
                    y + 61,
                    0x6B2412,
                    false
            );
        }
    }

    private static void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, SLOT_BORDER);
        graphics.fill(x, y, x + 16, y + 16, SLOT_INNER);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
