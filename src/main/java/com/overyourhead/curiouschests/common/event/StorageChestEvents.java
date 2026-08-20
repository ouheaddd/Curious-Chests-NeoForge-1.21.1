package com.overyourhead.curiouschests.common.event;

import com.overyourhead.curiouschests.common.block.BottomlessChestBlock;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Keeps the Storage Chest display-frame interaction available while sneaking. */
public final class StorageChestEvents {
    private StorageChestEvents() {}

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getEntity().isShiftKeyDown()) return;

        var state = event.getLevel().getBlockState(event.getPos());
        if (!(state.getBlock() instanceof BottomlessChestBlock)) return;

        // The built-in display frame belongs only to the chest's front face. Sneak-
        // clicking the top, sides or back keeps vanilla item use intact, so blocks
        // (including another Storage Chest) can still be placed against it normally.
        if (event.getFace() != state.getValue(BottomlessChestBlock.FACING)) return;

        // Vanilla normally lets sneak interaction bypass a block's own action so a
        // block can be placed against chests. Force only the front-face display action
        // back through the chest interaction pipeline. Held items can replace the
        // display; an empty hand falls through to useWithoutItem and removes it.
        event.setUseBlock(TriState.TRUE);
        event.setUseItem(TriState.FALSE);
    }
}
