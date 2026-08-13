package com.overyourhead.curiouschests.common.chest;

import net.minecraft.world.item.ItemStack;

/** Shared baseline storage rule. Specialized chests still apply their own slot rules. */
public final class ChestRules {
    private ChestRules() {}

    public static boolean canStore(ItemStack stack) {
        return true;
    }
}
