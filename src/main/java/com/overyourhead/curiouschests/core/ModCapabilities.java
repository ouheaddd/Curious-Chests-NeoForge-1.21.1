package com.overyourhead.curiouschests.core;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Exposes placed Curious Chests inventories through NeoForge's standard item-handler
 * capability so hoppers, pipes and storage mods can interact with them.
 */
public final class ModCapabilities {
    private ModCapabilities() {}

    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.SPECIAL_CHEST.get(),
                (blockEntity, side) -> blockEntity.getItemHandler()
        );
    }
}
