package com.overyourhead.curiouschests.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.Component;

/** Vanilla written-book view with only the Archivist empty-state line rendered slightly smaller. */
public final class ArchivistBookViewScreen extends BookViewScreen {
    private static final int BOOK_WIDTH = 192;
    private static final int BOOK_TEXT_X = 36;
    private static final int EMPTY_TEXT_Y = 54;
    private static final int BOOK_TEXT_WIDTH = 114;
    private static final float EMPTY_TEXT_SCALE = 0.85F;

    private final Component emptyText;

    public ArchivistBookViewScreen(BookAccess bookAccess, Component emptyText) {
        super(bookAccess);
        this.emptyText = emptyText;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int bookLeft = (this.width - BOOK_WIDTH) / 2;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(bookLeft + BOOK_TEXT_X, EMPTY_TEXT_Y, 0.0F);
        guiGraphics.pose().scale(EMPTY_TEXT_SCALE, EMPTY_TEXT_SCALE, 1.0F);
        guiGraphics.drawWordWrap(
                this.font,
                emptyText,
                0,
                0,
                Math.round(BOOK_TEXT_WIDTH / EMPTY_TEXT_SCALE),
                0x000000
        );
        guiGraphics.pose().popPose();
    }
}
