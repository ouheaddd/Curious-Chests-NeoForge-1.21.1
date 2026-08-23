package com.overyourhead.curiouschests.client.chest.trapper;

import com.overyourhead.curiouschests.common.network.ReleaseTrapperEntityPayload;
import com.overyourhead.curiouschests.common.network.RequestTrapperContentsPayload;
import com.overyourhead.curiouschests.common.network.TrapperContentsPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Client-only creature row used by the Trapper container screen. */
public final class TrapperScreenController {
    public static final int SLOT_X = 8;
    public static final int SLOT_Y = 31;
    public static final int SLOT_SIZE = 18;
    public static final int SLOT_COUNT = 9;

    private List<CompoundTag> entityTags = List.of();
    private final List<LivingEntity> previewEntities = new ArrayList<>();
    private int refreshTicks;

    public void open(int containerId) {
        refreshTicks = 0;
        PacketDistributor.sendToServer(new RequestTrapperContentsPayload(containerId));
    }

    public void tick(int containerId) {
        refreshTicks++;
        if (refreshTicks >= 20) {
            refreshTicks = 0;
            PacketDistributor.sendToServer(new RequestTrapperContentsPayload(containerId));
        }
    }

    public void render(
            GuiGraphics graphics,
            Font font,
            int leftPos,
            int topPos,
            int mouseX,
            int mouseY
    ) {
        for (int index = 0; index < Math.min(SLOT_COUNT, previewEntities.size()); index++) {
            LivingEntity entity = previewEntities.get(index);
            int x = leftPos + SLOT_X + index * SLOT_SIZE;
            int y = topPos + SLOT_Y;
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

        int hovered = slotAt(mouseX, mouseY, leftPos, topPos);
        if (hovered >= 0 && hovered < previewEntities.size()) {
            graphics.renderTooltip(font, previewEntities.get(hovered).getDisplayName(), mouseX, mouseY);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, int leftPos, int topPos, int containerId) {
        if (button != 0 && button != 1) return false;
        int slot = slotAt(mouseX, mouseY, leftPos, topPos);
        if (slot < 0 || slot >= entityTags.size()) return false;
        PacketDistributor.sendToServer(new ReleaseTrapperEntityPayload(containerId, slot));
        return true;
    }

    public void apply(TrapperContentsPayload payload, int containerId) {
        if (payload.containerId() != containerId || entityTags.equals(payload.entities())) return;
        entityTags = payload.entities();
        previewEntities.clear();

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        for (CompoundTag tag : entityTags) {
            Entity loaded = EntityType.loadEntityRecursive(tag.copy(), minecraft.level, entity -> entity);
            if (loaded instanceof LivingEntity living) {
                living.setCustomNameVisible(false);
                previewEntities.add(living);
            }
        }
    }

    private static int slotAt(double mouseX, double mouseY, int leftPos, int topPos) {
        int localX = (int) mouseX - leftPos - SLOT_X;
        int localY = (int) mouseY - topPos - SLOT_Y;
        if (localX < 0 || localY < 0 || localY >= SLOT_SIZE) return -1;
        int slot = localX / SLOT_SIZE;
        if (slot < 0 || slot >= SLOT_COUNT) return -1;
        return localX % SLOT_SIZE < SLOT_SIZE ? slot : -1;
    }
}
