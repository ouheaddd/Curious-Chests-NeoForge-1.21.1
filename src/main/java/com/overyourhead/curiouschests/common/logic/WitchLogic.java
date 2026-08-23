package com.overyourhead.curiouschests.common.logic;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.util.RandomSource;

import java.util.List;

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

    public static boolean isPotionStack(ItemStack stack) {
        return !stack.isEmpty()
                && stack.has(DataComponents.POTION_CONTENTS)
                && !(stack.getItem() instanceof ArrowItem);
    }

    public static boolean isSupported(ItemStack stack) {
        return isPotionStack(stack)
                || stack.is(Items.GLASS_BOTTLE)
                || stack.is(Items.DRAGON_BREATH);
    }

    public static int maxPerSlot(ItemStack stack) {
        return isPotionStack(stack) ? MAX_POTIONS_PER_SLOT : stack.getMaxStackSize();
    }

    /**
     * Draws one completely vanilla potion variant. This intentionally includes
     * the full minecraft potion registry (water/base, normal, long, strong and
     * the 1.21 trial-effect potions) while excluding modded potion entries.
     * Because the resulting stack uses the vanilla Potion holder unchanged, it
     * stacks normally with an identical potion already stored in the Witch Chest.
     */
    public static ItemStack randomVanillaPotion(RandomSource random) {
        List<Holder.Reference<Potion>> vanilla = BuiltInRegistries.POTION.holders()
                .filter(holder -> holder.unwrapKey()
                        .map(key -> "minecraft".equals(key.location().getNamespace()))
                        .orElse(false))
                .toList();
        if (vanilla.isEmpty()) return ItemStack.EMPTY;
        Holder<Potion> selected = vanilla.get(random.nextInt(vanilla.size()));
        return PotionContents.createItemStack(Items.POTION, selected);
    }
}
