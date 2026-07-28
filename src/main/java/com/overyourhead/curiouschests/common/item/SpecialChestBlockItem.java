package com.overyourhead.curiouschests.common.item;

import com.overyourhead.curiouschests.common.chest.ChestKind;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

public final class SpecialChestBlockItem extends BlockItem {
    private final ChestKind kind;

    public SpecialChestBlockItem(Block block, ChestKind kind, Item.Properties properties) {
        super(block, properties.stacksTo(1));
        this.kind = kind;
    }

    public ChestKind kind() {
        return kind;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String prefix = "tooltip.curiouschests." + kind.id();
        tooltip.add(Component.translatable(prefix + ".lore_1")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable(prefix + ".lore_2")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable(prefix + ".ability")
                .withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
