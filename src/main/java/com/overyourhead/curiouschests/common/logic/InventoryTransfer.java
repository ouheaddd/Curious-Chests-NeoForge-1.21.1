package com.overyourhead.curiouschests.common.logic;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public final class InventoryTransfer {
    private InventoryTransfer() {}

    public static boolean canInsertAny(Container target, ItemStack source) {
        if (source.isEmpty()) return false;

        for (int slot = 0; slot < target.getContainerSize(); slot++) {
            ItemStack present = target.getItem(slot);
            if (!present.isEmpty() && ItemStack.isSameItemSameComponents(present, source)) {
                int limit = Math.min(target.getMaxStackSize(present), present.getMaxStackSize());
                if (present.getCount() < limit) return true;
            }
        }

        for (int slot = 0; slot < target.getContainerSize(); slot++) {
            if (target.getItem(slot).isEmpty() && target.canPlaceItem(slot, source)) return true;
        }
        return false;
    }

    public static int insert(Container target, ItemStack source, boolean existingOnly) {
        if (source.isEmpty()) return 0;
        int moved = 0;

        for (int i = 0; i < target.getContainerSize() && !source.isEmpty(); i++) {
            ItemStack present = target.getItem(i);
            if (!present.isEmpty() && ItemStack.isSameItemSameComponents(present, source)) {
                int room = Math.min(target.getMaxStackSize(present), present.getMaxStackSize()) - present.getCount();
                if (room > 0) {
                    int amount = Math.min(room, source.getCount());
                    present.grow(amount);
                    source.shrink(amount);
                    moved += amount;
                    target.setItem(i, present);
                }
            }
        }

        if (!existingOnly) {
            for (int i = 0; i < target.getContainerSize() && !source.isEmpty(); i++) {
                if (target.getItem(i).isEmpty() && target.canPlaceItem(i, source)) {
                    int amount = Math.min(
                            source.getCount(),
                            Math.min(target.getMaxStackSize(source), source.getMaxStackSize())
                    );
                    target.setItem(i, source.copyWithCount(amount));
                    source.shrink(amount);
                    moved += amount;
                }
            }
        }

        if (moved > 0) target.setChanged();
        return moved;
    }

    public static int insertIntoRange(Container target, ItemStack source, int start, int end) {
        int moved = 0;

        for (int i = start; i < end && !source.isEmpty(); i++) {
            ItemStack present = target.getItem(i);
            if (!present.isEmpty() && ItemStack.isSameItemSameComponents(present, source)) {
                int room = present.getMaxStackSize() - present.getCount();
                int amount = Math.min(room, source.getCount());
                if (amount > 0) {
                    present.grow(amount);
                    source.shrink(amount);
                    target.setItem(i, present);
                    moved += amount;
                }
            }
        }

        for (int i = start; i < end && !source.isEmpty(); i++) {
            if (target.getItem(i).isEmpty()) {
                int amount = Math.min(source.getCount(), source.getMaxStackSize());
                target.setItem(i, source.copyWithCount(amount));
                source.shrink(amount);
                moved += amount;
            }
        }

        if (moved > 0) target.setChanged();
        return moved;
    }
}
