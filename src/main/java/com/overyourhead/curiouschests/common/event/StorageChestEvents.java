package com.overyourhead.curiouschests.common.event;

import com.overyourhead.curiouschests.common.block.BottomlessChestBlock;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Keeps the Storage Chest display-frame interaction available while sneaking. */
public final class StorageChestEvents {
    private StorageChestEvents() {}

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getEntity().isShiftKeyDown() || event.getItemStack().isEmpty()) return;
        if (!(event.getLevel().getBlockState(event.getPos()).getBlock() instanceof BottomlessChestBlock)) return;

        // Vanilla normally lets sneak + item bypass a block's own interaction so a
        // block can be placed against chests. Force only this Storage Chest action
        // back through useItemOn; a successful display insertion stops item use.
        event.setUseBlock(TriState.TRUE);
        event.setUseItem(TriState.FALSE);
    }
}
