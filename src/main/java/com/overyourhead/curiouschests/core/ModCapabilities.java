package com.overyourhead.curiouschests.core;

import com.overyourhead.curiouschests.common.chest.ChestKind;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/** Exposes storage slots while keeping Sentinel private and Resonant crystal control internal. */
public final class ModCapabilities {
    private ModCapabilities() {}

    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.SPECIAL_CHEST.get(),
                (blockEntity, side) -> (blockEntity.kind() == ChestKind.SCULK_SENTINEL
                        || blockEntity.kind() == ChestKind.TRAPPER)
                        ? null
                        : blockEntity.getItemHandler()
        );
    }
}
