package com.overyourhead.curiouschests.client.gui;

import com.overyourhead.curiouschests.CuriousChestsMod;
import com.overyourhead.curiouschests.common.chest.ChestKind;
import com.overyourhead.curiouschests.common.menu.SpecialChestMenu;
import com.overyourhead.curiouschests.common.network.RequestSentinelLogPayload;
import com.overyourhead.curiouschests.common.network.SentinelLogPayload;
import com.overyourhead.curiouschests.common.network.RequestTrapperContentsPayload;
import com.overyourhead.curiouschests.common.network.ReleaseTrapperEntityPayload;
import com.overyourhead.curiouschests.common.network.TrapperContentsPayload;
import com.overyourhead.curiouschests.common.sentinel.SentinelLogEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
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

    private static final int LOG_ROWS_VISIBLE = 5;
    private static final int LOG_FIRST_ROW_Y = 34;
    private static final int LOG_ROW_HEIGHT = 27;
    private static final int LOG_ENTRY_WIDTH = 160;
    private static final int LOG_ENTRY_HEIGHT = 25;
    private static final int SENTINEL_LOG_WIDTH = 176;
    private static final int SENTINEL_LOG_HEIGHT = 186;
    private static final double LOG_OVERLAY_Z = 500.0D;

    private static final int TRAPPER_ENTITY_SLOT_X = 8;
    private static final int TRAPPER_ENTITY_SLOT_Y = 18;
    private static final int TRAPPER_ENTITY_SLOT_SIZE = 18;
    private static final int TRAPPER_ENTITY_SLOTS = 9;
    private static final ResourceLocation TRAPPER_GUI_TEXTURE = ResourceLocation.withDefaultNamespace(
            "textures/gui/container/generic_54.png"
    );

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
    private int sentinelLogRefreshTicks;

    private List<CompoundTag> trapperEntityTags = List.of();
    private final List<LivingEntity> trapperPreviewEntities = new ArrayList<>();
    private int trapperRefreshTicks;

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
        if (menu.kind() == ChestKind.TRAPPER) {
            trapperRefreshTicks = 0;
            PacketDistributor.sendToServer(new RequestTrapperContentsPayload(menu.containerId));
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (menu.kind() == ChestKind.SCULK_SENTINEL && sentinelLogOpen) {
            sentinelLogRefreshTicks++;
            if (sentinelLogRefreshTicks >= 20) {
                sentinelLogRefreshTicks = 0;
                PacketDistributor.sendToServer(new RequestSentinelLogPayload(menu.containerId));
            }
        }
        if (menu.kind() == ChestKind.TRAPPER) {
            trapperRefreshTicks++;
            if (trapperRefreshTicks >= 20) {
                trapperRefreshTicks = 0;
                PacketDistributor.sendToServer(new RequestTrapperContentsPayload(menu.containerId));
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        if (menu.kind() == ChestKind.TRAPPER) {
            // Vanilla one-row container assembled from generic_54. The nine top
            // slots are visual entity cells, not ItemStack slots.
            int creatureAreaHeight = 17 + 18;
            graphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, creatureAreaHeight);
            graphics.blit(texture, leftPos, topPos + creatureAreaHeight, 0, 126, imageWidth, 96);
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
                || menu.kind() == ChestKind.SCULK_SENTINEL) {
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
        if (menu.kind() == ChestKind.SCULK_SENTINEL && sentinelLogOpen) {
            renderBackground(graphics, mouseX, mouseY, partialTick);
            graphics.pose().pushPose();
            graphics.pose().translate(0.0D, 0.0D, LOG_OVERLAY_Z);
            renderSentinelLog(graphics);
            renderSentinelButton(graphics, mouseX, mouseY);
            graphics.pose().popPose();
            graphics.flush();
            return;
        }

        // Normal chest page.
        super.render(graphics, mouseX, mouseY, partialTick);

        if (menu.kind() == ChestKind.TRAPPER) {
            renderTrapperEntities(graphics, mouseX, mouseY);
            renderTooltip(graphics, mouseX, mouseY);
            return;
        }

        if (menu.kind() != ChestKind.SCULK_SENTINEL) {
            renderTooltip(graphics, mouseX, mouseY);
            return;
        }

        renderSentinelButton(graphics, mouseX, mouseY);
        renderTooltip(graphics, mouseX, mouseY);
    }


    private void renderTrapperEntities(GuiGraphics graphics, int mouseX, int mouseY) {
        for (int index = 0; index < Math.min(TRAPPER_ENTITY_SLOTS, trapperPreviewEntities.size()); index++) {
            LivingEntity entity = trapperPreviewEntities.get(index);
            int x = leftPos + TRAPPER_ENTITY_SLOT_X + index * TRAPPER_ENTITY_SLOT_SIZE;
            int y = topPos + TRAPPER_ENTITY_SLOT_Y;
            float maxDimension = Math.max(0.6F, Math.max(entity.getBbWidth(), entity.getBbHeight()));
            int scale = Mth.clamp((int) (15.0F / maxDimension), 4, 14);

            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    graphics,
                    x + 1,
                    y + 1,
                    x + 17,
                    y + 17,
                    scale,
                    0.0F,
                    mouseX,
                    mouseY,
                    entity
            );
        }

        int hovered = trapperEntitySlotAt(mouseX, mouseY);
        if (hovered >= 0 && hovered < trapperPreviewEntities.size()) {
            LivingEntity entity = trapperPreviewEntities.get(hovered);
            graphics.renderTooltip(font, entity.getDisplayName(), mouseX, mouseY);
        }
    }

    private int trapperEntitySlotAt(double mouseX, double mouseY) {
        int localX = (int) mouseX - leftPos - TRAPPER_ENTITY_SLOT_X;
        int localY = (int) mouseY - topPos - TRAPPER_ENTITY_SLOT_Y;
        if (localX < 0 || localY < 0 || localY >= TRAPPER_ENTITY_SLOT_SIZE) return -1;
        int slot = localX / TRAPPER_ENTITY_SLOT_SIZE;
        if (slot < 0 || slot >= TRAPPER_ENTITY_SLOTS) return -1;
        int within = localX % TRAPPER_ENTITY_SLOT_SIZE;
        return within < TRAPPER_ENTITY_SLOT_SIZE ? slot : -1;
    }

    public void applyTrapperContents(TrapperContentsPayload payload) {
        if (payload.containerId() != menu.containerId || menu.kind() != ChestKind.TRAPPER) return;
        if (trapperEntityTags.equals(payload.entities())) return;
        trapperEntityTags = payload.entities();
        trapperPreviewEntities.clear();
        if (minecraft == null || minecraft.level == null) return;

        for (CompoundTag tag : trapperEntityTags) {
            Entity loaded = EntityType.loadEntityRecursive(tag.copy(), minecraft.level, entity -> entity);
            if (loaded instanceof LivingEntity living) {
                living.setCustomNameVisible(false);
                trapperPreviewEntities.add(living);
            }
        }
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
        // The log has its own 176 x 186 art and is centered independently inside
        // the 190 x 192 Sculk screen anchor. The normal container is not rendered
        // underneath while this page is open, so no opaque size-mismatch filler is needed.
        int logLeft = sentinelLogLeft();
        int logTop = sentinelLogTop();
        graphics.blit(
                SENTINEL_LOG_TEXTURE,
                logLeft,
                logTop,
                0,
                0,
                SENTINEL_LOG_WIDTH,
                SENTINEL_LOG_HEIGHT
        );

        Component logTitle = Component.translatable("gui.curiouschests.sculk_sentinel.log_title");
        int logTitleX = logLeft + (SENTINEL_LOG_WIDTH - font.width(logTitle)) / 2;
        int logTitleY = logTop + 20;
        // Draw the title shadow ourselves so it can use a neutral gray instead
        // of Minecraft's default dark text shadow.
        graphics.drawString(font, logTitle, logTitleX + 1, logTitleY + 1, 0x6F6F6F, false);
        graphics.drawString(font, logTitle, logTitleX, logTitleY, 0x2E5552, false);

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
            renderSentinelLogEntry(graphics, sentinelLogEntries.get(index), logLeft + 6, y);
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
            PlayerFaceRenderer.draw(graphics, info.getSkin(), x + 8, y + 5, 18);
        } else {
            graphics.fill(x + 8, y + 5, x + 26, y + 23, 0xFF163934);
            String initial = entry.playerName().isBlank()
                    ? "?"
                    : entry.playerName().substring(0, 1).toUpperCase(Locale.ROOT);
            graphics.drawCenteredString(font, initial, x + 17, y + 10, 0xD8E6E0);
        }

        // Keep the action baseline where it is; the nickname is nudged two pixels
        // lower to match the redesigned row art.
        graphics.drawString(font, trim(entry.playerName(), 83), x + 29, y + 5, 0x315D59, false);
        long seconds = Math.max(0L, (sentinelServerGameTime - entry.gameTime()) / 20L);
        Component ago = formatSentinelAge(seconds);
        int agoX = sentinelLogLeft() + SENTINEL_LOG_WIDTH - 13 - font.width(ago);

        Component action = Component.translatable(
                "gui.curiouschests.sculk_sentinel.action." + entry.action().name().toLowerCase(Locale.ROOT)
        );
        String actionText = (entry.attempts() > 1 ? "×" + entry.attempts() + " " : "") + action.getString();
        int actionWidth = Math.max(0, agoX - (x + 29) - 4);
        graphics.drawString(font, trim(actionText, actionWidth), x + 29, y + 13, 0x52736E, false);
        graphics.drawString(font, ago, agoX, y + 13, 0x657D77, false);
    }

    private Component formatSentinelAge(long totalSeconds) {
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (menu.kind() == ChestKind.TRAPPER && (button == 0 || button == 1)) {
            int slot = trapperEntitySlotAt(mouseX, mouseY);
            if (slot >= 0 && slot < trapperEntityTags.size()) {
                PacketDistributor.sendToServer(new ReleaseTrapperEntityPayload(menu.containerId, slot));
                return true;
            }
        }

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
        sentinelLogRefreshTicks = 0;
        PacketDistributor.sendToServer(new RequestSentinelLogPayload(menu.containerId));
    }

    private void closeSentinelLog() {
        sentinelLogOpen = false;
        sentinelLogLoading = false;
        sentinelLogRefreshTicks = 0;
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
        return SentinelAvatarCache.get(entry.playerId());
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
