package com.overyourhead.curiouschests.common.logic;

import com.overyourhead.curiouschests.common.blockentity.SpecialChestBlockEntity;
import com.overyourhead.curiouschests.common.chest.ChestKind;
import com.overyourhead.curiouschests.common.item.ResonanceCrystalItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Directional item transport between loaded Resonant Chests. */
public final class ResonanceLogic {
    public static final int STORAGE_SLOTS = 27;
    public static final int CRYSTAL_SLOT = 27;
    public static final int ATTUNEMENT_TICKS = 30;
    public static final int TRANSFER_DELAY_TICKS = 40;
    public static final int RETRY_DELAY_TICKS = 20;

    private static final Map<UUID, WeakReference<SpecialChestBlockEntity>> LOADED_NODES =
            new ConcurrentHashMap<>();

    private ResonanceLogic() {}

    public static void register(SpecialChestBlockEntity chest) {
        UUID nodeId = chest.getResonanceNodeId();
        if (nodeId != null) {
            LOADED_NODES.put(nodeId, new WeakReference<>(chest));
        }
    }

    public static void unregister(SpecialChestBlockEntity chest) {
        UUID nodeId = chest.getResonanceNodeId();
        if (nodeId == null) return;
        LOADED_NODES.computeIfPresent(nodeId, (id, reference) -> {
            SpecialChestBlockEntity registered = reference.get();
            return registered == null || registered == chest ? null : reference;
        });
    }

    public static Optional<SpecialChestBlockEntity> findLoaded(UUID nodeId) {
        WeakReference<SpecialChestBlockEntity> reference = LOADED_NODES.get(nodeId);
        if (reference == null) return Optional.empty();

        SpecialChestBlockEntity chest = reference.get();
        if (chest == null
                || chest.isRemoved()
                || chest.getLevel() == null
                || chest.kind() != ChestKind.RESONANT
                || !nodeId.equals(chest.getResonanceNodeId())) {
            LOADED_NODES.remove(nodeId, reference);
            return Optional.empty();
        }
        return Optional.of(chest);
    }

    public static void tick(ServerLevel level, BlockPos pos, BlockState state, SpecialChestBlockEntity chest) {
        chest.ensureResonanceInitialized();
        register(chest);

        ItemStack crystal = chest.getItem(CRYSTAL_SLOT);
        if (!crystal.is(com.overyourhead.curiouschests.core.ModItems.RESONANCE_CRYSTAL.get())) {
            chest.resetResonanceAttunement();
            chest.tickResonanceTransferCooldown();
            return;
        }

        UUID targetId = ResonanceCrystalItem.getTarget(crystal);
        if (targetId == null) {
            if (chest.advanceResonanceAttunement()) {
                ItemStack attuned = crystal.copyWithCount(1);
                ResonanceCrystalItem.attune(attuned, chest.getResonanceNodeId());
                chest.setResonanceCrystalInternal(attuned);
                playAttunement(level, pos);
            }
            return;
        }

        chest.resetResonanceAttunement();
        if (targetId.equals(chest.getResonanceNodeId())) {
            chest.tickResonanceTransferCooldown();
            return;
        }

        if (chest.tickResonanceTransferCooldown()) return;

        SpecialChestBlockEntity target = findLoaded(targetId).orElse(null);
        if (target == null || target == chest) {
            chest.setResonanceTransferCooldown(RETRY_DELAY_TICKS);
            return;
        }

        int sourceSlot = chest.findResonanceOutgoingSlot();
        if (sourceSlot < 0) {
            chest.setResonanceTransferCooldown(RETRY_DELAY_TICKS);
            return;
        }

        ItemStack source = chest.getItem(sourceSlot);
        int moved = target.insertResonanceReceived(source.copy());
        if (moved <= 0) {
            chest.setResonanceTransferCooldown(RETRY_DELAY_TICKS);
            return;
        }

        chest.shrinkResonanceOutgoing(sourceSlot, moved);
        chest.setResonanceTransferCooldown(TRANSFER_DELAY_TICKS);
        target.setChanged();
        chest.setChanged();
        playTransfer(chest, target);
    }

    private static void playAttunement(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8F, 1.25F);
        level.sendParticles(
                ParticleTypes.END_ROD,
                pos.getX() + 0.5D,
                pos.getY() + 1.0D,
                pos.getZ() + 0.5D,
                12,
                0.25D,
                0.2D,
                0.25D,
                0.015D
        );
    }

    private static void playTransfer(SpecialChestBlockEntity source, SpecialChestBlockEntity target) {
        if (source.getLevel() instanceof ServerLevel sourceLevel) {
            BlockPos pos = source.getBlockPos();
            sourceLevel.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8F, 1.35F);
            sourceLevel.sendParticles(
                    ParticleTypes.REVERSE_PORTAL,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.9D,
                    pos.getZ() + 0.5D,
                    18,
                    0.3D,
                    0.25D,
                    0.3D,
                    0.02D
            );
        }

        if (target.getLevel() instanceof ServerLevel targetLevel) {
            BlockPos pos = target.getBlockPos();
            targetLevel.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.75F, 0.9F);
            targetLevel.sendParticles(
                    ParticleTypes.END_ROD,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.9D,
                    pos.getZ() + 0.5D,
                    14,
                    0.3D,
                    0.25D,
                    0.3D,
                    0.015D
            );
        }
    }
}
