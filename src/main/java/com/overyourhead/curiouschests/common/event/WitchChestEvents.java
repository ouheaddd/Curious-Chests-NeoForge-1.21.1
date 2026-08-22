package com.overyourhead.curiouschests.common.event;

import com.overyourhead.curiouschests.common.block.WitchChestBlock;
import com.overyourhead.curiouschests.common.blockentity.SpecialChestBlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Handles Witch Chest brew scooping directly so sneaking cannot bypass it. */
public final class WitchChestEvents {
    private WitchChestEvents() {}

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getEntity().isShiftKeyDown()) return;
        if (!event.getItemStack().is(Items.GLASS_BOTTLE)) return;

        var state = event.getLevel().getBlockState(event.getPos());
        if (!(state.getBlock() instanceof WitchChestBlock)) return;

        // In the 1.21 interaction pipeline a sneaking player can bypass
        // Block#useItemOn entirely. Handle this exact action here instead of
        // trying to force the normal block/item pipeline back on.
        if (!event.getLevel().isClientSide
                && event.getLevel().getBlockEntity(event.getPos()) instanceof SpecialChestBlockEntity chest) {
            chest.tryScoopWitchBrew(event.getEntity(), event.getItemStack());
        }

        // Consume the interaction on both sides so the bottle cannot continue
        // into another use action and the chest GUI does not open underneath it.
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        event.setCanceled(true);
    }
}
