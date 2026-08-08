package com.overyourhead.curiouschests.common.item;

import com.overyourhead.curiouschests.core.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.UUID;

/**
 * One physical crystal item with two states:
 * - no resonance id: dormant;
 * - resonance id present: attuned to a specific Resonant Chest.
 */
public final class ResonanceCrystalItem extends Item {
    public ResonanceCrystalItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    public static UUID getTarget(ItemStack stack) {
        return stack.get(ModDataComponents.RESONANCE_ID.get());
    }

    public static boolean isAttuned(ItemStack stack) {
        return getTarget(stack) != null;
    }

    public static void attune(ItemStack stack, UUID nodeId) {
        stack.set(ModDataComponents.RESONANCE_ID.get(), nodeId);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(isAttuned(stack)
                ? "item.curiouschests.resonance_crystal.attuned"
                : "item.curiouschests.resonance_crystal.dormant");
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isAttuned(stack) || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        UUID target = getTarget(stack);
        if (target == null) {
            tooltip.add(Component.translatable("tooltip.curiouschests.resonance_crystal.dormant")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            String shortId = target.toString().substring(0, 8).toUpperCase();
            tooltip.add(Component.translatable("tooltip.curiouschests.resonance_crystal.attuned", shortId)
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
