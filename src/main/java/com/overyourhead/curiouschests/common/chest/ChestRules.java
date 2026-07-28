package com.overyourhead.curiouschests.common.chest;

import com.overyourhead.curiouschests.common.item.SpecialChestBlockItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;

public final class ChestRules {
    private ChestRules() {}

    public static boolean canStore(ItemStack stack) {
        if (stack.isEmpty()) return true;
        if (stack.getItem() instanceof SpecialChestBlockItem) return false;
        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock) return false;
        if (stack.has(DataComponents.BUNDLE_CONTENTS)) return false;
        return !stack.has(DataComponents.CONTAINER);
    }
}
