package com.overyourhead.curiouschests.client.gui;

import com.overyourhead.curiouschests.CuriousChestsMod;
import com.overyourhead.curiouschests.common.chest.ChestKind;
import com.overyourhead.curiouschests.client.chest.trapper.TrapperScreenController;
import com.overyourhead.curiouschests.client.chest.sentinel.SentinelLogController;
import com.overyourhead.curiouschests.common.menu.SpecialChestMenu;
import com.overyourhead.curiouschests.common.network.SentinelLogPayload;
import com.overyourhead.curiouschests.common.network.TrapperContentsPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;


/**
 * Textured container screen shared by all Curious Chests.
 *
 * The Sentinel intrusion log is rendered as an opaque page inside this same
 * container screen. Keeping both pages in one screen gives them identical
 * dimensions and button coordinates, preserves vanilla container background
 * rendering, and prevents a second standalone screen from adding blur.
 */
public final class SpecialChestScreen extends AbstractContainerScreen<SpecialChestMenu> {

    private static final int RESONANCE_SLOT_PANEL_X = 192;
    private static final int RESONANCE_SLOT_PANEL_Y = 15;
    private static final int RESONANCE_SLOT_PANEL_SIZE = 22;

    private static final int ARCHIVIST_SLOT_PANEL_X = 180;
    private static final int ARCHIVIST_SLOT_PANEL_Y = 15;
    private static final int ARCHIVIST_SLOT_PANEL_SIZE = 22;

    private static final int BUILDERS_BASE_WIDTH = 176;
    private static final int BUILDERS_CRAFT_PANEL_X = 180;
    private static final int BUILDERS_CRAFT_PANEL_Y = 8;
    private static final int BUILDERS_CRAFT_PANEL_WIDTH = 80;
    private static final int BUILDERS_CRAFT_PANEL_HEIGHT = 116;

    // Per-GUI label tuning. These are intentionally kept as simple pixel
    // offsets/colors so the redesigned textures can be fine-tuned later.
    private static final int BUILDERS_LABEL_OFFSET_X = 1;
    private static final int BUILDERS_INVENTORY_LABEL_OFFSET_Y = -2;
    private static final int BUILDERS_LABEL_COLOR = 0xF2F2E8; // warm off-white main text
    private static final int BUILDERS_LABEL_SHADOW_COLOR = 0x555752; // darker stone-gray custom shadow

    private static final int COLLECTORS_LABEL_OFFSET_X = 0;
    private static final int COLLECTORS_LABEL_OFFSET_Y = -1;
    private static final int COLLECTORS_LABEL_COLOR = 0xE6D6A8; // birch-like beige
    private static final int COLLECTORS_LABEL_SHADOW_COLOR = 0x555752; // dark stone-gray custom shadow

    private static final int ARCHIVIST_LABEL_COLOR = 0xE1BC73; // warm beige/gold
    private static final int ARCHIVIST_LABEL_SHADOW_COLOR = 0x8A6428; // dark warm-gold custom shadow
    private static final int ARCHIVIST_TITLE_OFFSET_Y = 1;
    private static final int ARCHIVIST_INVENTORY_LABEL_OFFSET_Y = -1;

    private static final int WITCH_LABEL_COLOR = 0xC58BEA; // potion-like light purple
    // Move the complete Witch GUI content block together over the redesigned art.
    private static final int WITCH_CONTENT_OFFSET_X = 16;
    private static final int WITCH_CONTENT_OFFSET_Y = 16;
    private static final int WITCH_INVENTORY_LABEL_Y = 128;


    private static final ResourceLocation TRAPPER_GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CuriousChestsMod.MOD_ID,
            "textures/gui/container/trapper.png"
    );

    private static final ResourceLocation RESONANCE_SLOT_PANEL_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CuriousChestsMod.MOD_ID,
            "textures/gui/widget/resonance_crystal_slot.png"
    );
    private static final ResourceLocation ARCHIVIST_SLOT_PANEL_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CuriousChestsMod.MOD_ID,
            "textures/gui/widget/archivist_catalog_slot.png"
    );
    private static final ResourceLocation BUILDERS_CRAFT_PANEL_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CuriousChestsMod.MOD_ID,
            "textures/gui/widget/builders_crafting.png"
    );

    private final ResourceLocation texture;
    private final TrapperScreenController trapperController = new TrapperScreenController();
    private final SentinelLogController sentinelLogController = new SentinelLogController();



    public SpecialChestScreen(SpecialChestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = menu.kind().screenWidth();
        imageHeight = menu.kind().screenHeight();
        inventoryLabelY = imageHeight - 94;
        texture = menu.kind() == ChestKind.TRAPPER
                ? TRAPPER_GUI_TEXTURE
                : ResourceLocation.fromNamespaceAndPath(
                        CuriousChestsMod.MOD_ID,
                        "textures/gui/container/" + menu.kind().id() + ".png"
                );
    }

    @Override
    protected void init() {
        super.init();

        // Builder's crafting panel is an attached side module, not part of the
        // visual center of the chest GUI. Keep the 176px chest body centered
        // exactly like a normal container and let the crafting panel extend to
        // the right from that anchor. imageWidth intentionally stays 260 so the
        // attached panel and its slots remain inside the screen's interaction area.
        if (menu.kind() == ChestKind.BUILDERS) {
            leftPos = (width - BUILDERS_BASE_WIDTH) / 2;
        }
        if (menu.kind() == ChestKind.TRAPPER) trapperController.open(menu.containerId);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (menu.kind() == ChestKind.SCULK_SENTINEL) sentinelLogController.tick(menu.containerId);
        if (menu.kind() == ChestKind.TRAPPER) trapperController.tick(menu.containerId);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        if (menu.kind() == ChestKind.TRAPPER) {
            // Trapper now owns an editable 256x256 GUI asset like the other
            // Curious Chests. The visible vanilla-style placeholder occupies
            // the top-left 176x132 area and can be redrawn without touching code.
            graphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        } else if (menu.kind() == ChestKind.BUILDERS) {
            graphics.blit(
                    texture,
                    leftPos,
                    topPos,
                    0,
                    0,
                    BUILDERS_BASE_WIDTH,
                    imageHeight,
                    256,
                    256
            );
            graphics.blit(
                    BUILDERS_CRAFT_PANEL_TEXTURE,
                    leftPos + BUILDERS_CRAFT_PANEL_X,
                    topPos + BUILDERS_CRAFT_PANEL_Y,
                    0,
                    0,
                    BUILDERS_CRAFT_PANEL_WIDTH,
                    BUILDERS_CRAFT_PANEL_HEIGHT,
                    BUILDERS_CRAFT_PANEL_WIDTH,
                    BUILDERS_CRAFT_PANEL_HEIGHT
            );
        } else {
            graphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        }
        if (menu.kind() == ChestKind.RESONANT) {
            graphics.blit(
                    RESONANCE_SLOT_PANEL_TEXTURE,
                    leftPos + RESONANCE_SLOT_PANEL_X,
                    topPos + RESONANCE_SLOT_PANEL_Y,
                    0,
                    0,
                    RESONANCE_SLOT_PANEL_SIZE,
                    RESONANCE_SLOT_PANEL_SIZE,
                    RESONANCE_SLOT_PANEL_SIZE,
                    RESONANCE_SLOT_PANEL_SIZE
            );
        }
        if (menu.kind() == ChestKind.ARCHIVIST) {
            graphics.blit(
                    ARCHIVIST_SLOT_PANEL_TEXTURE,
                    leftPos + ARCHIVIST_SLOT_PANEL_X,
                    topPos + ARCHIVIST_SLOT_PANEL_Y,
                    0,
                    0,
                    ARCHIVIST_SLOT_PANEL_SIZE,
                    ARCHIVIST_SLOT_PANEL_SIZE,
                    ARCHIVIST_SLOT_PANEL_SIZE,
                    ARCHIVIST_SLOT_PANEL_SIZE
            );
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // These redesigned GUIs carry their visual identity in the texture itself.
        // Do not draw the vanilla chest title or the player "Inventory" label over them.
        if (menu.kind() == ChestKind.INFERNAL
                || menu.kind() == ChestKind.ENDER_DISPATCH
                || menu.kind() == ChestKind.RESONANT
                || menu.kind() == ChestKind.SCULK_SENTINEL
                || menu.kind() == ChestKind.TRAPPER) {
            return;
        }

        if (menu.kind() == ChestKind.BOTTOMLESS) {
            int storageLabelColor = 0xE6D6A8;
            graphics.drawString(font, title, titleLabelX, titleLabelY + 2, storageLabelColor, false);
            graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY + 1, storageLabelColor, false);
            return;
        }

        if (menu.kind() == ChestKind.BUILDERS) {
            int buildersTitleX = titleLabelX + BUILDERS_LABEL_OFFSET_X;
            int buildersTitleY = titleLabelY;
            int buildersInventoryX = inventoryLabelX + BUILDERS_LABEL_OFFSET_X;
            int buildersInventoryY = inventoryLabelY + BUILDERS_INVENTORY_LABEL_OFFSET_Y;

            // Use a custom light-gray shadow instead of Minecraft's default dark shadow.
            graphics.drawString(font, title, buildersTitleX + 1, buildersTitleY + 1, BUILDERS_LABEL_SHADOW_COLOR, false);
            graphics.drawString(font, title, buildersTitleX, buildersTitleY, BUILDERS_LABEL_COLOR, false);
            graphics.drawString(font, playerInventoryTitle, buildersInventoryX + 1, buildersInventoryY + 1, BUILDERS_LABEL_SHADOW_COLOR, false);
            graphics.drawString(font, playerInventoryTitle, buildersInventoryX, buildersInventoryY, BUILDERS_LABEL_COLOR, false);
            return;
        }

        if (menu.kind() == ChestKind.COLLECTORS) {
            Component collectorsTitle = Component.translatable("gui.curiouschests.collectors.title");
            int collectorsTitleX = titleLabelX + COLLECTORS_LABEL_OFFSET_X;
            int collectorsInventoryX = inventoryLabelX + COLLECTORS_LABEL_OFFSET_X;
            int collectorsTitleY = titleLabelY + COLLECTORS_LABEL_OFFSET_Y;
            int collectorsInventoryY = inventoryLabelY + COLLECTORS_LABEL_OFFSET_Y;

            graphics.drawString(font, collectorsTitle, collectorsTitleX + 1, collectorsTitleY + 1, COLLECTORS_LABEL_SHADOW_COLOR, false);
            graphics.drawString(font, collectorsTitle, collectorsTitleX, collectorsTitleY, COLLECTORS_LABEL_COLOR, false);
            graphics.drawString(font, playerInventoryTitle, collectorsInventoryX + 1, collectorsInventoryY + 1, COLLECTORS_LABEL_SHADOW_COLOR, false);
            graphics.drawString(font, playerInventoryTitle, collectorsInventoryX, collectorsInventoryY, COLLECTORS_LABEL_COLOR, false);
            return;
        }

        if (menu.kind() == ChestKind.ARCHIVIST) {
            Component archivistTitle = Component.translatable("gui.curiouschests.archivist.title");
            int archivistTitleY = titleLabelY + ARCHIVIST_TITLE_OFFSET_Y;
            int archivistInventoryY = inventoryLabelY + ARCHIVIST_INVENTORY_LABEL_OFFSET_Y;

            graphics.drawString(font, archivistTitle, titleLabelX + 1, archivistTitleY + 1, ARCHIVIST_LABEL_SHADOW_COLOR, false);
            graphics.drawString(font, archivistTitle, titleLabelX, archivistTitleY, ARCHIVIST_LABEL_COLOR, false);
            graphics.drawString(font, playerInventoryTitle, inventoryLabelX + 1, archivistInventoryY + 1, ARCHIVIST_LABEL_SHADOW_COLOR, false);
            graphics.drawString(font, playerInventoryTitle, inventoryLabelX, archivistInventoryY, ARCHIVIST_LABEL_COLOR, false);
            return;
        }

        if (menu.kind() == ChestKind.WITCH) {
            graphics.drawString(
                    font,
                    title,
                    titleLabelX + WITCH_CONTENT_OFFSET_X,
                    titleLabelY + WITCH_CONTENT_OFFSET_Y,
                    WITCH_LABEL_COLOR,
                    false
            );
            graphics.drawString(
                    font,
                    playerInventoryTitle,
                    inventoryLabelX + WITCH_CONTENT_OFFSET_X,
                    WITCH_INVENTORY_LABEL_Y + WITCH_CONTENT_OFFSET_Y,
                    WITCH_LABEL_COLOR,
                    false
            );
            return;
        }

        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // The log page has its own dimensions (176 x 186), while the redesigned
        // Sculk Sentinel container is 190 x 192. When the log is open, render it
        // directly instead of rendering the chest first and covering the size
        // mismatch with an opaque rectangle. This removes the black side/top/bottom
        // bands and also guarantees hidden slots/items cannot bleed through.
        if (menu.kind() == ChestKind.SCULK_SENTINEL && sentinelLogController.isOpen()) {
            renderBackground(graphics, mouseX, mouseY, partialTick);
            graphics.pose().pushPose();
            graphics.pose().translate(0.0D, 0.0D, SentinelLogController.OVERLAY_Z);
            sentinelLogController.renderPage(graphics, font, leftPos, topPos, imageWidth, imageHeight);
            sentinelLogController.renderButton(graphics, font, leftPos, topPos, imageWidth, mouseX, mouseY);
            graphics.pose().popPose();
            graphics.flush();
            return;
        }

        // Normal chest page.
        super.render(graphics, mouseX, mouseY, partialTick);

        if (menu.kind() == ChestKind.TRAPPER) {
            trapperController.render(graphics, font, leftPos, topPos, mouseX, mouseY);
            renderTooltip(graphics, mouseX, mouseY);
            return;
        }

        if (menu.kind() != ChestKind.SCULK_SENTINEL) {
            renderTooltip(graphics, mouseX, mouseY);
            return;
        }

        sentinelLogController.renderButton(graphics, font, leftPos, topPos, imageWidth, mouseX, mouseY);
        renderTooltip(graphics, mouseX, mouseY);
    }


    public void applyTrapperContents(TrapperContentsPayload payload) {
        if (menu.kind() == ChestKind.TRAPPER) trapperController.apply(payload, menu.containerId);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (menu.kind() == ChestKind.TRAPPER
                && trapperController.mouseClicked(mouseX, mouseY, button, leftPos, topPos, menu.containerId)) {
            return true;
        }

        if (menu.kind() == ChestKind.SCULK_SENTINEL
                && sentinelLogController.handleButtonClick(
                        mouseX, mouseY, button, leftPos, topPos, imageWidth, menu.containerId
                )) {
            return true;
        }

        // The log is informational. Swallow all remaining clicks so hidden
        // container slots cannot be moved accidentally through the overlay.
        if (sentinelLogController.isOpen()) return true;

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return sentinelLogController.isOpen() || super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return sentinelLogController.isOpen() || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // The journal intentionally stores and displays exactly five latest
        // records, so there is no hidden page or scroll state.
        return sentinelLogController.isOpen() || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (menu.kind() == ChestKind.SCULK_SENTINEL && sentinelLogController.handleKeyPressed(keyCode, scanCode)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public void applySentinelLog(SentinelLogPayload payload) {
        if (menu.kind() == ChestKind.SCULK_SENTINEL) sentinelLogController.apply(payload, menu.containerId);
    }

}
