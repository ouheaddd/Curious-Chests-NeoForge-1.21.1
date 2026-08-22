package com.overyourhead.curiouschests.common.event;

import com.overyourhead.curiouschests.common.block.TrapperChestBlock;
import com.overyourhead.curiouschests.common.blockentity.SpecialChestBlockEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Special harvest rule for safely carrying captured creatures inside a Trapper. */
public final class TrapperEvents {
    private TrapperEvents() {}

    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getState().getBlock() instanceof TrapperChestBlock)) return;
        Player player = event.getPlayer();
        if (!player.isShiftKeyDown()) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(level.getBlockEntity(event.getPos()) instanceof SpecialChestBlockEntity chest)) return;

        // Do not cancel the break. Vanilla/NeoForge still handles mining time, tool
        // wear, stats and block removal. onRemove sees this transient intent and does
        // not release the prisoners; BlockDropsEvent then swaps the normal empty
        // chest drop for a single packed stack carrying their NBT.
        chest.armTrapperPackBreak(level);
    }

    public static void onDrops(BlockDropsEvent event) {
        if (!(event.getState().getBlock() instanceof TrapperChestBlock)) return;
        if (!(event.getBlockEntity() instanceof SpecialChestBlockEntity chest)) return;
        if (!chest.isTrapperPackOnBreak()) return;

        ItemStack packed = chest.createPackedTrapperStack();
        if (packed.isEmpty()) return;

        event.getDrops().clear();
        event.getDrops().add(new ItemEntity(
                event.getLevel(),
                event.getPos().getX() + 0.5D,
                event.getPos().getY() + 0.5D,
                event.getPos().getZ() + 0.5D,
                packed
        ));

        event.getLevel().playSound(null, event.getPos(), SoundEvents.VAULT_CLOSE_SHUTTER, SoundSource.BLOCKS, 0.72F, 0.92F);
        event.getLevel().sendParticles(
                ParticleTypes.VAULT_CONNECTION,
                event.getPos().getX() + 0.5D,
                event.getPos().getY() + 0.65D,
                event.getPos().getZ() + 0.5D,
                14,
                0.42D, 0.38D, 0.42D,
                0.06D
        );
    }
}
