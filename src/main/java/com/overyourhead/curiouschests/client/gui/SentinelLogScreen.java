package com.overyourhead.curiouschests.client.gui;

import com.overyourhead.curiouschests.CuriousChestsMod;
import com.overyourhead.curiouschests.common.network.RequestSentinelLogPayload;
import com.overyourhead.curiouschests.common.network.SentinelLogPayload;
import com.overyourhead.curiouschests.common.sentinel.SentinelLogEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Locale;

/**
 * Standalone, informational Sentinel log page.
 *
 * This deliberately does not extend AbstractContainerScreen: chest slots and
 * the player's inventory therefore cannot render or receive clicks beneath the
 * log texture. The original container menu remains open on the server and the
 * floating paper button returns to the parent chest screen.
 */
public final class SentinelLogScreen extends Screen {
    private static final int IMAGE_WIDTH = 214;
    private static final int IMAGE_HEIGHT = 186;
    private static final int BUTTON_SIZE = 18;
    private static final int BUTTON_GAP = 4;
    private static final int BUTTON_Y = 18;
    private static final int ROWS_VISIBLE = 5;
    private static final int FIRST_ROW_Y = 34;
    private static final int ROW_HEIGHT = 27;

    private static final ResourceLocation LOG_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CuriousChestsMod.MOD_ID,
            "textures/gui/container/sculk_sentinel_log.png"
    );
    private static final ResourceLocation BUTTON_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CuriousChestsMod.MOD_ID,
            "textures/gui/widget/sculk_log_button.png"
    );

    private final SpecialChestScreen parent;
    private final int containerId;
    private int leftPos;
    private int topPos;
    private boolean loading = true;
    private List<SentinelLogEntry> entries = List.of();
    private long serverGameTime;
    private int scroll;

    public SentinelLogScreen(SpecialChestScreen parent, int containerId) {
        super(Component.translatable("gui.curiouschests.sculk_sentinel.log_title"));
        this.parent = parent;
        this.containerId = containerId;
    }

    @Override
    protected void init() {
        leftPos = (width - IMAGE_WIDTH) / 2;
        topPos = (height - IMAGE_HEIGHT) / 2;
        requestLog();
    }

    @Override
    public void tick() {
        if (minecraft == null || minecraft.player == null) return;

        // The server may close the underlying chest menu while this standalone
        // page is visible (distance, block removal, etc.). Do not return to a
        // stale parent container screen in that case.
        if (minecraft.player.containerMenu.containerId != containerId) {
            minecraft.setScreen(null);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(LOG_TEXTURE, leftPos, topPos, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

        graphics.drawCenteredString(
                font,
                title,
                leftPos + IMAGE_WIDTH / 2,
                topPos + 10,
                0xD8E6E0
        );

        if (loading) {
            graphics.drawCenteredString(
                    font,
                    Component.translatable("gui.curiouschests.sculk_sentinel.log_loading"),
                    leftPos + IMAGE_WIDTH / 2,
                    topPos + 87,
                    0x93AAA4
            );
        } else if (entries.isEmpty()) {
            graphics.drawCenteredString(
                    font,
                    Component.translatable("gui.curiouschests.sculk_sentinel.log_empty"),
                    leftPos + IMAGE_WIDTH / 2,
                    topPos + 87,
                    0x93AAA4
            );
        } else {
            renderEntries(graphics);
        }

        renderFloatingButton(graphics, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderEntries(GuiGraphics graphics) {
        int end = Math.min(entries.size(), scroll + ROWS_VISIBLE);
        for (int index = scroll; index < end; index++) {
            int visibleIndex = index - scroll;
            int y = topPos + FIRST_ROW_Y + visibleIndex * ROW_HEIGHT;
            renderEntry(graphics, entries.get(index), leftPos + 12, y);
        }

        if (entries.size() > ROWS_VISIBLE) {
            String page = (scroll + 1) + "-" + end + " / " + entries.size();
            graphics.drawString(
                    font,
                    page,
                    leftPos + IMAGE_WIDTH - 54,
                    topPos + IMAGE_HEIGHT - 13,
                    0x789089,
                    false
            );
        }
    }

    private void renderEntry(GuiGraphics graphics, SentinelLogEntry entry, int x, int y) {
        PlayerInfo info = playerInfo(entry);
        if (info != null) {
            PlayerFaceRenderer.draw(graphics, info.getSkin(), x, y + 3, 18);
        } else {
            graphics.fill(x, y + 3, x + 18, y + 21, 0xFF163934);
            String initial = entry.playerName().isBlank()
                    ? "?"
                    : entry.playerName().substring(0, 1).toUpperCase(Locale.ROOT);
            graphics.drawCenteredString(font, initial, x + 9, y + 8, 0xD8E6E0);
        }

        graphics.drawString(font, trim(entry.playerName(), 124), x + 25, y + 2, 0xE3ECE8, false);
        graphics.drawString(
                font,
                Component.translatable(
                        "gui.curiouschests.sculk_sentinel.action." + entry.action().name().toLowerCase(Locale.ROOT)
                ),
                x + 25,
                y + 12,
                0x9DB3AD,
                false
        );

        long seconds = Math.max(0L, (serverGameTime - entry.gameTime()) / 20L);
        Component ago = Component.translatable("gui.curiouschests.sculk_sentinel.seconds_ago", seconds);
        int agoX = leftPos + IMAGE_WIDTH - 12 - font.width(ago);
        graphics.drawString(font, ago, agoX, y + 12, 0x657D77, false);
    }

    private void renderFloatingButton(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = buttonX();
        int y = buttonY();
        boolean hovered = inside(mouseX, mouseY, x, y, BUTTON_SIZE, BUTTON_SIZE);

        // The third sprite is the active/open state. The same floating button
        // works as a return button while the log page is visible.
        graphics.blit(BUTTON_TEXTURE, x, y, 36, 0, BUTTON_SIZE, BUTTON_SIZE, 54, 18);

        if (hovered) {
            graphics.renderTooltip(
                    font,
                    Component.translatable("gui.curiouschests.sculk_sentinel.log_back"),
                    mouseX,
                    mouseY
            );
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && inside(mouseX, mouseY, buttonX(), buttonY(), BUTTON_SIZE, BUTTON_SIZE)) {
            returnToParent();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (entries.size() > ROWS_VISIBLE) {
            int max = entries.size() - ROWS_VISIBLE;
            scroll = Math.max(0, Math.min(max, scroll + (scrollY < 0 ? 1 : -1)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 || (minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode))) {
            returnToParent();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        returnToParent();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void requestLog() {
        loading = true;
        entries = List.of();
        PacketDistributor.sendToServer(new RequestSentinelLogPayload(containerId));
    }

    public void applyLog(SentinelLogPayload payload) {
        if (payload.containerId() != containerId) return;
        loading = false;
        serverGameTime = payload.serverGameTime();
        entries = payload.entries();
        scroll = Math.min(scroll, Math.max(0, entries.size() - ROWS_VISIBLE));
    }

    private void returnToParent() {
        if (minecraft == null || minecraft.player == null) return;
        if (minecraft.player.containerMenu.containerId == containerId) {
            minecraft.setScreen(parent);
        } else {
            minecraft.setScreen(null);
        }
    }

    private PlayerInfo playerInfo(SentinelLogEntry entry) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        return connection == null ? null : connection.getPlayerInfo(entry.playerId());
    }

    private String trim(String value, int width) {
        return font.plainSubstrByWidth(value, width);
    }

    private int buttonX() {
        return leftPos + IMAGE_WIDTH + BUTTON_GAP;
    }

    private int buttonY() {
        return topPos + BUTTON_Y;
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
