package com.overyourhead.curiouschests.common.storage;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

/**
 * Converts the 63 visible Bottomless Chest slots into four serialized
 * storage layers. The block entity may use oversized ItemStack counts while
 * it is loaded, but persistent item components and NBT contain bounded 64-count
 * fragments rather than the logical 256-count stack.
 */
public final class BottomlessStorage {
    public static final int VISIBLE_SLOTS = 63;
    public static final int LAYERS = 4;
    public static final int SERIALIZED_SLOTS = VISIBLE_SLOTS * LAYERS;
    public static final int ABSOLUTE_SLOT_LIMIT = 256;

    private BottomlessStorage() {}

    public static int maxPerSlot(ItemStack stack) {
        if (stack.isEmpty()) return ABSOLUTE_SLOT_LIMIT;
        return stack.getMaxStackSize() <= 1 ? 1 : ABSOLUTE_SLOT_LIMIT;
    }

    /**
     * Uses layer-major indexes: visibleSlot + layer * 63. The first layer is
     * therefore compatible with old 0.3.0 container components.
     */
    public static NonNullList<ItemStack> splitForSerialization(NonNullList<ItemStack> visibleItems) {
        NonNullList<ItemStack> serialized = NonNullList.withSize(SERIALIZED_SLOTS, ItemStack.EMPTY);

        for (int visibleSlot = 0; visibleSlot < Math.min(VISIBLE_SLOTS, visibleItems.size()); visibleSlot++) {
            ItemStack stored = visibleItems.get(visibleSlot);
            if (stored.isEmpty()) continue;

            int remaining = Math.min(stored.getCount(), maxPerSlot(stored));
            // The persistent representation uses bounded 64-count
            // fragments. Four fragments therefore represent one visible count of 256,
            // including items whose normal gameplay stack limit is 16.
            int serializedLayerSize = stored.getMaxStackSize() <= 1 ? 1 : 64;

            for (int layer = 0; layer < LAYERS && remaining > 0; layer++) {
                int amount = Math.min(serializedLayerSize, remaining);
                serialized.set(index(visibleSlot, layer), stored.copyWithCount(amount));
                remaining -= amount;
            }
        }

        return serialized;
    }

    public static NonNullList<ItemStack> mergeSerialized(NonNullList<ItemStack> serialized) {
        NonNullList<ItemStack> visible = NonNullList.withSize(VISIBLE_SLOTS, ItemStack.EMPTY);

        for (int visibleSlot = 0; visibleSlot < VISIBLE_SLOTS; visibleSlot++) {
            ItemStack prototype = ItemStack.EMPTY;
            int total = 0;

            for (int layer = 0; layer < LAYERS; layer++) {
                int index = index(visibleSlot, layer);
                if (index >= serialized.size()) break;

                ItemStack part = serialized.get(index);
                if (part.isEmpty()) continue;

                if (prototype.isEmpty()) {
                    prototype = part.copyWithCount(1);
                } else if (!ItemStack.isSameItemSameComponents(prototype, part)) {
                    // Corrupt or manually edited data: keep the first item type and
                    // ignore incompatible fragments instead of merging different items.
                    continue;
                }

                total += part.getCount();
            }

            if (!prototype.isEmpty() && total > 0) {
                visible.set(visibleSlot, prototype.copyWithCount(Math.min(total, maxPerSlot(prototype))));
            }
        }

        return visible;
    }

    private static int index(int visibleSlot, int layer) {
        return visibleSlot + layer * VISIBLE_SLOTS;
    }
}
