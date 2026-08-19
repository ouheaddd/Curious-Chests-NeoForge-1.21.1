package com.overyourhead.curiouschests.client.gui;

import com.overyourhead.curiouschests.CuriousChestsMod;
import com.overyourhead.curiouschests.common.chest.ChestKind;
import com.overyourhead.curiouschests.common.menu.SpecialChestMenu;
import com.overyourhead.curiouschests.common.network.RequestSentinelLogPayload;
import com.overyourhead.curiouschests.common.network.SentinelLogPayload;
import com.overyourhead.curiouschests.common.sentinel.SentinelLogEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Locale;

/**
 * Textured container screen shared by all Curious Chests.
 *
 * The Sentinel intrusion log is rendered as an opaque page inside this same
 * container screen. Keeping both pages in one screen gives them identical
 * dimensions and button coordinates, preserves vanilla container background
 * rendering, and prevents a second standalone screen from adding blur.
 */
public final class SpecialChestScreen extends AbstractContainerScreen<SpecialChestMenu> {
    private static final int SENTINEL_BUTTON_SIZE = 18;
    private static final int SENTINEL_BUTTON_GAP = 4;
    private static final int SENTINEL_BUTTON_Y = 18;

    private static final int RESONANCE_SLOT_PANEL_X = 180;
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

    private static final int LOG_ROWS_VISIBLE = 5;
    private static final int LOG_FIRST_ROW_Y = 34;
    private static final int LOG_ROW_HEIGHT = 27;
    private static final int LOG_ENTRY_WIDTH = 160;
    private static final int LOG_ENTRY_HEIGHT = 25;
    private static final int SENTINEL_LOG_WIDTH = 176;
    private static final int SENTINEL_LOG_HEIGHT = 186;
    private static final double LOG_OVERLAY_Z = 500.0D;

    private static final ResourceLocation SENTINEL_BUTTON_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CuriousChestsMod.MOD_ID,
            "textures/gui/widget/sculk_log_button.png"
    );
    private static final ResourceLocation SENTINEL_LOG_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CuriousChestsMod.MOD_ID,
            "textures/gui/container/sculk_sentinel_log.png"
    );
    private static final ResourceLocation SENTINEL_LOG_ENTRY_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CuriousChestsMod.MOD_ID,
            "textures/gui/widget/sculk_sentinel_log_entry.png"
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

    private boolean sentinelLogOpen;
    private boolean sentinelLogLoading;
    private List<SentinelLogEntry> sentinelLogEntries = List.of();
    private long sentinelServerGameTime;

    public SpecialChestScreen(SpecialChestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = menu.kind().screenWidth();
        imageHeight = menu.kind().screenHeight();
        inventoryLabelY = imageHeight - 94;
        texture = ResourceLocation.fromNamespaceAndPath(
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
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        if (menu.kind() == ChestKind.BUILDERS) {
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
                || menu.kind() == ChestKind.SCULK_SENTINEL) {
            return;
        }

        if (menu.kind() == ChestKind.BOTTOMLESS) {
            int storageLabelColor = 0xE6D6A8;
            graphics.drawString(font, title, titleLabelX, titleLabelY + 2, storageLabelColor, false);
            graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY + 1, storageLabelColor, false);
            return;
        }

        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Let the normal container screen perform its single vanilla background
        // pass. The menu stays open while the owner views the intrusion page.
        super.render(graphics, mouseX, mouseY, partialTick);

        if (menu.kind() != ChestKind.SCULK_SENTINEL) {
            renderTooltip(graphics, mouseX, mouseY);
            return;
        }

        if (sentinelLogOpen) {
            // Item icons are submitted through a different GUI render buffer than
            // textured panels. Flush those commands first, then draw the opaque log
            // page at a higher Z layer. This prevents chest, inventory, hotbar and
            // carried items from appearing above the intrusion page.
            graphics.flush();
            graphics.pose().pushPose();
            graphics.pose().translate(0.0D, 0.0D, LOG_OVERLAY_Z);
            renderSentinelLog(graphics);
            renderSentinelButton(graphics, mouseX, mouseY);
            graphics.pose().popPose();
            graphics.flush();
            return;
        }

        renderSentinelButton(graphics, mouseX, mouseY);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderSentinelButton(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = sentinelButtonX();
        int y = sentinelButtonY();
        boolean hovered = inside(mouseX, mouseY, x, y, SENTINEL_BUTTON_SIZE, SENTINEL_BUTTON_SIZE);
        int u = sentinelLogOpen ? 36 : (hovered ? 18 : 0);

        graphics.blit(
                SENTINEL_BUTTON_TEXTURE,
                x,
                y,
                u,
                0,
                SENTINEL_BUTTON_SIZE,
                SENTINEL_BUTTON_SIZE,
                54,
                18
        );

        if (hovered) {
            graphics.renderTooltip(
                    font,
                    Component.translatable(sentinelLogOpen
                            ? "gui.curiouschests.sculk_sentinel.log_back"
                            : "gui.curiouschests.sculk_sentinel.log_button"),
                    mouseX,
                    mouseY
            );
        }
    }

    private void renderSentinelLog(GuiGraphics graphics) {
        // The chest GUI is now 190 x 192, while the existing intrusion-log art is 176 x 186.
        // Cover the whole resized container first, then center the old log panel inside it so
        // hidden slots/items cannot bleed through the newly exposed border area.
        int logLeft = sentinelLogLeft();
        int logTop = sentinelLogTop();
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF071311);
        graphics.blit(
                SENTINEL_LOG_TEXTURE,
                logLeft,
                logTop,
                0,
                0,
                SENTINEL_LOG_WIDTH,
                SENTINEL_LOG_HEIGHT
        );

        graphics.drawCenteredString(
                font,
                Component.translatable("gui.curiouschests.sculk_sentinel.log_title"),
                logLeft + SENTINEL_LOG_WIDTH / 2,
                logTop + 10,
                0xD8E6E0
        );

        if (sentinelLogLoading) {
            graphics.drawCenteredString(
                    font,
                    Component.translatable("gui.curiouschests.sculk_sentinel.log_loading"),
                    logLeft + SENTINEL_LOG_WIDTH / 2,
                    logTop + 87,
                    0x93AAA4
            );
        } else if (sentinelLogEntries.isEmpty()) {
            graphics.drawCenteredString(
                    font,
                    Component.translatable("gui.curiouschests.sculk_sentinel.log_empty"),
                    logLeft + SENTINEL_LOG_WIDTH / 2,
                    logTop + 87,
                    0x93AAA4
            );
        } else {
            renderSentinelLogEntries(graphics);
        }
    }

    private void renderSentinelLogEntries(GuiGraphics graphics) {
        int visible = Math.min(LOG_ROWS_VISIBLE, sentinelLogEntries.size());
        int logLeft = sentinelLogLeft();
        int logTop = sentinelLogTop();
        for (int index = 0; index < visible; index++) {
            int y = logTop + LOG_FIRST_ROW_Y + index * LOG_ROW_HEIGHT;
            renderSentinelLogEntry(graphics, sentinelLogEntries.get(index), logLeft + 8, y);
        }
    }

    private void renderSentinelLogEntry(GuiGraphics graphics, SentinelLogEntry entry, int x, int y) {
        // The card itself is a separate editable texture and is drawn only for a
        // real record. The base log PNG therefore remains completely empty when
        // there are no intrusions.
        graphics.blit(
                SENTINEL_LOG_ENTRY_TEXTURE,
                x,
                y,
                0,
                0,
                LOG_ENTRY_WIDTH,
                LOG_ENTRY_HEIGHT,
                LOG_ENTRY_WIDTH,
                LOG_ENTRY_HEIGHT
        );

        PlayerInfo info = playerInfo(entry);
        if (info != null) {
            PlayerFaceRenderer.draw(graphics, info.getSkin(), x + 4, y + 3, 18);
        } else {
            graphics.fill(x + 4, y + 3, x + 22, y + 21, 0xFF163934);
            String initial = entry.playerName().isBlank()
                    ? "?"
                    : entry.playerName().substring(0, 1).toUpperCase(Locale.ROOT);
            graphics.drawCenteredString(font, initial, x + 13, y + 8, 0xD8E6E0);
        }

        graphics.drawString(font, trim(entry.playerName(), 83), x + 29, y + 2, 0xE3ECE8, false);
        graphics.drawString(
                font,
                Component.translatable(
                        "gui.curiouschests.sculk_sentinel.action." + entry.action().name().toLowerCase(Locale.ROOT)
                ),
                x + 29,
                y + 12,
                0x9DB3AD,
                false
        );

        long seconds = Math.max(0L, (sentinelServerGameTime - entry.gameTime()) / 20L);
        Component ago = Component.translatable("gui.curiouschests.sculk_sentinel.seconds_ago", seconds);
        int agoX = sentinelLogLeft() + SENTINEL_LOG_WIDTH - 8 - font.width(ago);
        graphics.drawString(font, ago, agoX, y + 12, 0x657D77, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (menu.kind() == ChestKind.SCULK_SENTINEL
                && button == 0
                && inside(
                        mouseX,
                        mouseY,
                        sentinelButtonX(),
                        sentinelButtonY(),
                        SENTINEL_BUTTON_SIZE,
                        SENTINEL_BUTTON_SIZE
                )) {
            if (sentinelLogOpen) {
                closeSentinelLog();
            } else {
                openSentinelLog();
            }
            return true;
        }

        // The log is informational. Swallow all remaining clicks so hidden
        // container slots cannot be moved accidentally through the overlay.
        if (sentinelLogOpen) {
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return sentinelLogOpen || super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return sentinelLogOpen || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // The journal intentionally stores and displays exactly five latest
        // records, so there is no hidden page or scroll state.
        return sentinelLogOpen || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (sentinelLogOpen) {
            if (keyCode == 256
                    || (minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode))) {
                closeSentinelLog();
            }
            // Do not let hotbar, drop or recipe-book keys interact with the
            // container slots hidden behind the opaque log page.
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void openSentinelLog() {
        sentinelLogOpen = true;
        sentinelLogLoading = true;
        sentinelLogEntries = List.of();
        PacketDistributor.sendToServer(new RequestSentinelLogPayload(menu.containerId));
    }

    private void closeSentinelLog() {
        sentinelLogOpen = false;
        sentinelLogLoading = false;
    }

    public void applySentinelLog(SentinelLogPayload payload) {
        if (payload.containerId() != menu.containerId) return;
        sentinelLogLoading = false;
        sentinelServerGameTime = payload.serverGameTime();
        sentinelLogEntries = List.copyOf(
                payload.entries().subList(0, Math.min(LOG_ROWS_VISIBLE, payload.entries().size()))
        );
    }

    private PlayerInfo playerInfo(SentinelLogEntry entry) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        return connection == null ? null : connection.getPlayerInfo(entry.playerId());
    }

    private String trim(String value, int width) {
        return font.plainSubstrByWidth(value, width);
    }

    private int sentinelLogLeft() {
        return leftPos + (imageWidth - SENTINEL_LOG_WIDTH) / 2;
    }

    private int sentinelLogTop() {
        return topPos + (imageHeight - SENTINEL_LOG_HEIGHT) / 2;
    }

    private int sentinelButtonX() {
        return leftPos + imageWidth + SENTINEL_BUTTON_GAP;
    }

    private int sentinelButtonY() {
        return topPos + SENTINEL_BUTTON_Y;
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
