package com.overyourhead.curiouschests.client.render;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/** One shared client extension / BEWLR for all Curious Chests block items. */
public final class SpecialChestItemExtensions implements IClientItemExtensions {
    private final SpecialChestItemRenderer renderer = new SpecialChestItemRenderer();

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        return renderer;
    }
}
