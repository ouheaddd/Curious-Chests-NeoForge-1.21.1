package com.overyourhead.curiouschests.common.event;

import com.overyourhead.curiouschests.common.block.SculkSentinelChestBlock;
import com.overyourhead.curiouschests.common.blockentity.SpecialChestBlockEntity;
import com.overyourhead.curiouschests.common.logic.SentinelLogic;
import com.overyourhead.curiouschests.common.sentinel.SentinelIntrusionType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

public final class SentinelEvents {
    private SentinelEvents() {}

    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!(event.getState().getBlock() instanceof SculkSentinelChestBlock)) return;

        Player player = event.getEntity();
        BlockPos pos = event.getPosition().orElse(null);
        if (pos == null) return;
        if (!(player.level().getBlockEntity(pos) instanceof SpecialChestBlockEntity chest)) return;
        if (chest.canSentinelAccess(player)) return;

        event.setNewSpeed(event.getNewSpeed() * 0.25F);
    }

    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getState().getBlock() instanceof SculkSentinelChestBlock)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(level.getBlockEntity(event.getPos()) instanceof SpecialChestBlockEntity chest)) return;

        Player player = event.getPlayer();
        if (chest.canSentinelAccess(player)) return;

        event.setCanceled(true);
        SentinelLogic.trigger(
                level,
                event.getPos(),
                chest,
                player,
                SentinelIntrusionType.BREAK
        );
    }
}
