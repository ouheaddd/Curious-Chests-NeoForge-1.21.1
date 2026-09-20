package com.overyourhead.curiouschests.common.chest.shared;

import com.overyourhead.curiouschests.common.blockentity.SpecialChestBlockEntity;
import com.overyourhead.curiouschests.common.chest.ChestKind;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Server-side sorting for the Curious Chests that opt into manual sorting. */
public final class ChestSorting {
    private ChestSorting() {}

    /**
     * Infernal, Ender Dispatch, Resonant and Trapper intentionally keep their
     * specialized layouts/behaviour and do not expose manual sorting.
     */
    public static boolean supports(ChestKind kind) {
        return switch (kind) {
            case BOTTOMLESS, BUILDERS, COLLECTORS, SCULK_SENTINEL, ARCHIVIST, WITCH -> true;
            case INFERNAL, ENDER_DISPATCH, RESONANT, TRAPPER -> false;
        };
    }

    /**
     * Merges exact item/component matches, orders item groups by registry id,
     * and leaves empty slots at the end. Returns true only when anything moved.
     */
    public static boolean sort(SpecialChestBlockEntity chest) {
        ChestKind kind = chest.kind();
        if (!supports(kind)) return false;

        int storageSlots = Math.min(kind.storageSlots(), chest.getContainerSize());
        if (storageSlots <= 1) return false;

        List<Group> groups = new ArrayList<>();
        for (int slot = 0; slot < storageSlots; slot++) {
            ItemStack stack = chest.getItem(slot);
            if (stack.isEmpty()) continue;

            Group group = findGroup(groups, stack);
            if (group == null) {
                groups.add(new Group(stack.copyWithCount(1), stack.getCount()));
            } else {
                group.count += stack.getCount();
            }
        }

        groups.sort(Comparator.comparing(group -> BuiltInRegistries.ITEM
                .getKey(group.prototype.getItem())
                .toString()));

        List<ItemStack> sorted = new ArrayList<>(storageSlots);
        for (Group group : groups) {
            int remaining = group.count;
            int perSlot = Math.max(1, chest.getMaxStackSize(group.prototype));
            while (remaining > 0) {
                int amount = Math.min(perSlot, remaining);
                sorted.add(group.prototype.copyWithCount(amount));
                remaining -= amount;
            }
        }
        // Defensive guard for malformed/legacy oversized stacks: never drop
        // items just because their normalized form would need extra slots.
        if (sorted.size() > storageSlots) return false;
        while (sorted.size() < storageSlots) {
            sorted.add(ItemStack.EMPTY);
        }

        boolean changed = false;
        for (int slot = 0; slot < storageSlots; slot++) {
            if (!sameStackAndCount(chest.getItem(slot), sorted.get(slot))) {
                changed = true;
                break;
            }
        }
        if (!changed) return false;

        for (int slot = 0; slot < storageSlots; slot++) {
            chest.setItem(slot, sorted.get(slot));
        }
        chest.setChanged();
        return true;
    }

    private static Group findGroup(List<Group> groups, ItemStack wanted) {
        for (Group group : groups) {
            if (ItemStack.isSameItemSameComponents(group.prototype, wanted)) {
                return group;
            }
        }
        return null;
    }

    private static boolean sameStackAndCount(ItemStack first, ItemStack second) {
        if (first.isEmpty() || second.isEmpty()) return first.isEmpty() && second.isEmpty();
        return first.getCount() == second.getCount()
                && ItemStack.isSameItemSameComponents(first, second);
    }

    private static final class Group {
        private final ItemStack prototype;
        private int count;

        private Group(ItemStack prototype, int count) {
            this.prototype = prototype;
            this.count = count;
        }
    }
}
