package com.overyourhead.curiouschests.client.chest.sentinel;

import com.overyourhead.curiouschests.CuriousChestsMod;
import com.overyourhead.curiouschests.common.network.RequestSentinelLogPayload;
import com.overyourhead.curiouschests.common.network.SentinelLogPayload;
import com.overyourhead.curiouschests.common.sentinel.SentinelLogEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Locale;

/** Client controller for the five-entry Sculk Sentinel intrusion journal. */
public final class SentinelLogController {
    public static final int BUTTON_SIZE = 18;
    public static final int BUTTON_GAP = 4;
    public static final int BUTTON_Y = 18;
    public static final int LOG_WIDTH = 176;
    public static final int LOG_HEIGHT = 186;
    public static final double OVERLAY_Z = 500.0D;

    private static final int ROWS_VISIBLE = 5;
    private static final int FIRST_ROW_Y = 34;
    private static final int ROW_HEIGHT = 27;
    private static final int ENTRY_WIDTH = 160;
    private static final int ENTRY_HEIGHT = 25;

    private static final ResourceLocation BUTTON_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CuriousChestsMod.MOD_ID,
            "textures/gui/widget/sculk_log_button.png"
    );
    private static final ResourceLocation LOG_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CuriousChestsMod.MOD_ID,
            "textures/gui/container/sculk_sentinel_log.png"
    );
    private static final ResourceLocation ENTRY_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CuriousChestsMod.MOD_ID,
            "textures/gui/widget/sculk_sentinel_log_entry.png"
    );

    private boolean open;
    private boolean loading;
    private List<SentinelLogEntry> entries = List.of();
    private long serverGameTime;
    private int refreshTicks;

    public boolean isOpen() {
        return open;
    }

    public void tick(int containerId) {
        if (!open) return;
        refreshTicks++;
        if (refreshTicks >= 20) {
            refreshTicks = 0;
            PacketDistributor.sendToServer(new RequestSentinelLogPayload(containerId));
        }
    }

    public void renderPage(GuiGraphics graphics, Font font, int leftPos, int topPos, int imageWidth, int imageHeight) {
        int logLeft = logLeft(leftPos, imageWidth);
        int logTop = logTop(topPos, imageHeight);
        graphics.blit(LOG_TEXTURE, logLeft, logTop, 0, 0, LOG_WIDTH, LOG_HEIGHT);

        Component logTitle = Component.translatable("gui.curiouschests.sculk_sentinel.log_title");
        int logTitleX = logLeft + (LOG_WIDTH - font.width(logTitle)) / 2;
        int logTitleY = logTop + 20;
        graphics.drawString(font, logTitle, logTitleX + 1, logTitleY + 1, 0x6F6F6F, false);
        graphics.drawString(font, logTitle, logTitleX, logTitleY, 0x2E5552, false);

        if (loading) {
            graphics.drawCenteredString(
                    font,
                    Component.translatable("gui.curiouschests.sculk_sentinel.log_loading"),
                    logLeft + LOG_WIDTH / 2,
                    logTop + 87,
                    0x93AAA4
            );
            return;
        }
        if (entries.isEmpty()) {
            graphics.drawCenteredString(
                    font,
                    Component.translatable("gui.curiouschests.sculk_sentinel.log_empty"),
                    logLeft + LOG_WIDTH / 2,
                    logTop + 87,
                    0x93AAA4
            );
            return;
        }

        int visible = Math.min(ROWS_VISIBLE, entries.size());
        for (int index = 0; index < visible; index++) {
            int y = logTop + FIRST_ROW_Y + index * ROW_HEIGHT;
            renderEntry(graphics, font, entries.get(index), logLeft + 6, y, logLeft);
        }
    }

    public void renderButton(
            GuiGraphics graphics,
            Font font,
            int leftPos,
            int topPos,
            int imageWidth,
            int mouseX,
            int mouseY
    ) {
        int x = buttonX(leftPos, imageWidth);
        int y = buttonY(topPos);
        boolean hovered = inside(mouseX, mouseY, x, y, BUTTON_SIZE, BUTTON_SIZE);
        int u = open ? 36 : (hovered ? 18 : 0);
        graphics.blit(BUTTON_TEXTURE, x, y, u, 0, BUTTON_SIZE, BUTTON_SIZE, 54, 18);
        if (hovered) {
            graphics.renderTooltip(
                    font,
                    Component.translatable(open
                            ? "gui.curiouschests.sculk_sentinel.log_back"
                            : "gui.curiouschests.sculk_sentinel.log_button"),
                    mouseX,
                    mouseY
            );
        }
    }

    public boolean handleButtonClick(
            double mouseX,
            double mouseY,
            int button,
            int leftPos,
            int topPos,
            int imageWidth,
            int containerId
    ) {
        if (button != 0 || !inside(mouseX, mouseY, buttonX(leftPos, imageWidth), buttonY(topPos), BUTTON_SIZE, BUTTON_SIZE)) {
            return false;
        }
        if (open) close(); else open(containerId);
        return true;
    }

    public boolean handleKeyPressed(int keyCode, int scanCode) {
        if (!open) return false;
        Minecraft minecraft = Minecraft.getInstance();
        if (keyCode == 256 || minecraft.options.keyInventory.matches(keyCode, scanCode)) close();
        return true;
    }

    public void apply(SentinelLogPayload payload, int containerId) {
        if (payload.containerId() != containerId) return;
        loading = false;
        serverGameTime = payload.serverGameTime();
        entries = List.copyOf(payload.entries().subList(0, Math.min(ROWS_VISIBLE, payload.entries().size())));
    }

    private void open(int containerId) {
        open = true;
        loading = true;
        entries = List.of();
        refreshTicks = 0;
        PacketDistributor.sendToServer(new RequestSentinelLogPayload(containerId));
    }

    private void close() {
        open = false;
        loading = false;
        refreshTicks = 0;
    }

    private void renderEntry(GuiGraphics graphics, Font font, SentinelLogEntry entry, int x, int y, int logLeft) {
        graphics.blit(ENTRY_TEXTURE, x, y, 0, 0, ENTRY_WIDTH, ENTRY_HEIGHT, ENTRY_WIDTH, ENTRY_HEIGHT);

        PlayerInfo info = SentinelAvatarCache.get(entry.playerId());
        if (info != null) {
            PlayerFaceRenderer.draw(graphics, info.getSkin(), x + 8, y + 5, 18);
        } else {
            graphics.fill(x + 8, y + 5, x + 26, y + 23, 0xFF163934);
            String initial = entry.playerName().isBlank()
                    ? "?"
                    : entry.playerName().substring(0, 1).toUpperCase(Locale.ROOT);
            graphics.drawCenteredString(font, initial, x + 17, y + 10, 0xD8E6E0);
        }

        graphics.drawString(font, trim(font, entry.playerName(), 83), x + 29, y + 5, 0x315D59, false);
        long seconds = Math.max(0L, (serverGameTime - entry.gameTime()) / 20L);
        Component ago = formatAge(seconds);
        int agoX = logLeft + LOG_WIDTH - 13 - font.width(ago);

        Component action = Component.translatable(
                "gui.curiouschests.sculk_sentinel.action." + entry.action().name().toLowerCase(Locale.ROOT)
        );
        String actionText = (entry.attempts() > 1 ? "×" + entry.attempts() + " " : "") + action.getString();
        int actionWidth = Math.max(0, agoX - (x + 29) - 4);
        graphics.drawString(font, trim(font, actionText, actionWidth), x + 29, y + 13, 0x52736E, false);
        graphics.drawString(font, ago, agoX, y + 13, 0x657D77, false);
    }

    private static Component formatAge(long totalSeconds) {
        long value;
        String unit;
        if (totalSeconds < 60L) {
            value = totalSeconds;
            unit = "s";
        } else if (totalSeconds < 60L * 60L) {
            value = totalSeconds / 60L;
            unit = "m";
        } else if (totalSeconds < 24L * 60L * 60L) {
            value = totalSeconds / (60L * 60L);
            unit = "h";
        } else if (totalSeconds < 7L * 24L * 60L * 60L) {
            value = totalSeconds / (24L * 60L * 60L);
            unit = "d";
        } else {
            value = totalSeconds / (7L * 24L * 60L * 60L);
            unit = "w";
        }
        return Component.translatable("gui.curiouschests.sculk_sentinel.time_ago", value, unit);
    }

    private static String trim(Font font, String value, int width) {
        return font.plainSubstrByWidth(value, width);
    }

    private static int logLeft(int leftPos, int imageWidth) {
        return leftPos + (imageWidth - LOG_WIDTH) / 2;
    }

    private static int logTop(int topPos, int imageHeight) {
        return topPos + (imageHeight - LOG_HEIGHT) / 2;
    }

    private static int buttonX(int leftPos, int imageWidth) {
        return leftPos + imageWidth + BUTTON_GAP;
    }

    private static int buttonY(int topPos) {
        return topPos + BUTTON_Y;
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
