package com.overyourhead.curiouschests.common.logic;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;

/**
 * Shared storage rules for the Witch's Chest.
 *
 * Any non-arrow item carrying the standard potion-contents component is
 * accepted. This covers vanilla drinkable, splash and lingering potions, plus
 * compatible modded potion containers that use Minecraft's normal component.
 */
public final class WitchLogic {
    public static final int STORAGE_SLOTS = 54;
    public static final int MAX_POTIONS_PER_SLOT = 16;

    private WitchLogic() {}

    public static boolean isSupported(ItemStack stack) {
        return !stack.isEmpty()
                && stack.has(DataComponents.POTION_CONTENTS)
                && !(stack.getItem() instanceof ArrowItem);
    }

    public static int maxPerSlot(ItemStack stack) {
        return isSupported(stack) ? MAX_POTIONS_PER_SLOT : stack.getMaxStackSize();
    }
}
