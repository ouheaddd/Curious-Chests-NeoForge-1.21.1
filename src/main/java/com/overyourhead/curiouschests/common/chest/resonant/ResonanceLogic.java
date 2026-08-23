package com.overyourhead.curiouschests.common.chest.resonant;

import com.overyourhead.curiouschests.common.blockentity.SpecialChestBlockEntity;
import com.overyourhead.curiouschests.common.chest.ChestKind;
import com.overyourhead.curiouschests.common.item.ResonanceCrystalItem;
import com.overyourhead.curiouschests.core.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Directional item transport between Resonant Chests.
 *
 * Loaded targets receive items immediately. A registered target whose chunk is currently
 * unloaded receives items through a persistent mailbox stored in overworld SavedData.
 * Missing/broken targets are never sent items.
 */
public final class ResonanceLogic {
    public static final int STORAGE_SLOTS = 27;
    public static final int CRYSTAL_SLOT = 27;
    public static final int ATTUNEMENT_TICKS = 30;
    public static final int TRANSFER_DELAY_TICKS = 40;
    public static final int RETRY_DELAY_TICKS = 20;

    private static final int MAX_PENDING_STACKS = STORAGE_SLOTS;
    private static final int MAX_PENDING_ATTEMPTS_PER_TICK = 8;

    private static final Map<UUID, WeakReference<SpecialChestBlockEntity>> LOADED_NODES =
            new ConcurrentHashMap<>();

    private ResonanceLogic() {}

    /** Registers a loaded node and refreshes its persistent world location. */
    public static void register(SpecialChestBlockEntity chest) {
        UUID nodeId = chest.getResonanceNodeId();
        if (nodeId == null) return;

        LOADED_NODES.put(nodeId, new WeakReference<>(chest));
        if (chest.getLevel() instanceof ServerLevel serverLevel) {
            network(serverLevel).registerNode(nodeId, serverLevel.dimension(), chest.getBlockPos());
        }
    }

    /** Called immediately when a Resonant Chest is placed, before its first server tick. */
    public static void registerPlaced(ServerLevel level, SpecialChestBlockEntity chest) {
        UUID nodeId = chest.getResonanceNodeId();
        if (nodeId == null) return;
        LOADED_NODES.put(nodeId, new WeakReference<>(chest));
        network(level).registerNode(nodeId, level.dimension(), chest.getBlockPos());
    }

    /** Removes only the loaded weak reference. Chunk unloads must not remove the persistent endpoint. */
    public static void unregister(SpecialChestBlockEntity chest) {
        UUID nodeId = chest.getResonanceNodeId();
        if (nodeId == null) return;
        LOADED_NODES.computeIfPresent(nodeId, (id, reference) -> {
            SpecialChestBlockEntity registered = reference.get();
            return registered == null || registered == chest ? null : reference;
        });
    }

    /** Called only when the actual block is removed/replaced, not when its chunk unloads. */
    public static void unregisterPlaced(ServerLevel level, SpecialChestBlockEntity chest) {
        UUID nodeId = chest.getResonanceNodeId();
        if (nodeId == null) return;

        unregister(chest);
        ResonanceNetworkData data = network(level);
        for (ItemStack pending : data.takePending(nodeId)) {
            if (!pending.isEmpty()) {
                Containers.dropItemStack(
                        level,
                        chest.getBlockPos().getX() + 0.5D,
                        chest.getBlockPos().getY() + 0.5D,
                        chest.getBlockPos().getZ() + 0.5D,
                        pending
                );
            }
        }
        data.unregisterNode(nodeId, level.dimension(), chest.getBlockPos());
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

        // Pending deliveries are accepted even when this chest currently has no outgoing crystal.
        deliverPending(level, chest);

        ItemStack crystal = chest.getItem(CRYSTAL_SLOT);
        if (!crystal.is(ModItems.RESONANCE_CRYSTAL.get())) {
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
        if (target != null && target != chest) {
            transferToLoaded(chest, target);
            return;
        }

        ResonanceNetworkData data = network(level);
        ResonanceEndpoint endpoint = data.getNode(targetId);
        if (endpoint == null || !endpointStillExists(level, targetId, endpoint, data)) {
            // The crystal points to a chest that is not currently a registered placed endpoint.
            // Keep the source item exactly where it is.
            chest.setResonanceTransferCooldown(RETRY_DELAY_TICKS);
            return;
        }

        int sourceSlot = chest.findResonanceOutgoingSlot();
        if (sourceSlot < 0) {
            chest.setResonanceTransferCooldown(RETRY_DELAY_TICKS);
            return;
        }

        ItemStack source = chest.getItem(sourceSlot);
        int queued = data.enqueue(targetId, source.copy());
        if (queued <= 0) {
            // Mailbox safety cap reached: never delete or strand the source item.
            chest.setResonanceTransferCooldown(RETRY_DELAY_TICKS);
            return;
        }

        chest.shrinkResonanceOutgoing(sourceSlot, queued);
        chest.setResonanceTransferCooldown(TRANSFER_DELAY_TICKS);
        chest.setChanged();
        playQueuedTransfer(chest);
    }

    private static void transferToLoaded(
            SpecialChestBlockEntity sourceChest,
            SpecialChestBlockEntity targetChest
    ) {
        int sourceSlot = sourceChest.findResonanceOutgoingSlot();
        if (sourceSlot < 0) {
            sourceChest.setResonanceTransferCooldown(RETRY_DELAY_TICKS);
            return;
        }

        ItemStack source = sourceChest.getItem(sourceSlot);
        int moved = targetChest.insertResonanceReceived(source.copy());
        if (moved <= 0) {
            sourceChest.setResonanceTransferCooldown(RETRY_DELAY_TICKS);
            return;
        }

        sourceChest.shrinkResonanceOutgoing(sourceSlot, moved);
        sourceChest.setResonanceTransferCooldown(TRANSFER_DELAY_TICKS);
        targetChest.setChanged();
        sourceChest.setChanged();
        playTransfer(sourceChest, targetChest);
    }

    /**
     * If the endpoint's chunk happens to be loaded, validate the persistent registry without
     * loading any chunk. If the chunk is unloaded, the persistent placement record is trusted.
     */
    private static boolean endpointStillExists(
            ServerLevel sourceLevel,
            UUID targetId,
            ResonanceEndpoint endpoint,
            ResonanceNetworkData data
    ) {
        ServerLevel targetLevel = sourceLevel.getServer().getLevel(endpoint.dimension());
        if (targetLevel == null) return false;

        int chunkX = endpoint.pos().getX() >> 4;
        int chunkZ = endpoint.pos().getZ() >> 4;
        if (targetLevel.getChunkSource().getChunkNow(chunkX, chunkZ) == null) {
            return true;
        }

        BlockEntity blockEntity = targetLevel.getBlockEntity(endpoint.pos());
        if (blockEntity instanceof SpecialChestBlockEntity target
                && target.kind() == ChestKind.RESONANT
                && targetId.equals(target.getResonanceNodeId())) {
            register(target);
            return true;
        }

        // A loaded chunk proves the old location no longer contains the registered endpoint.
        data.unregisterNode(targetId, endpoint.dimension(), endpoint.pos());
        LOADED_NODES.remove(targetId);
        return false;
    }

    private static void deliverPending(ServerLevel level, SpecialChestBlockEntity target) {
        UUID nodeId = target.getResonanceNodeId();
        if (nodeId == null) return;

        int delivered = network(level).deliver(nodeId, target, MAX_PENDING_ATTEMPTS_PER_TICK);
        if (delivered > 0) {
            target.setChanged();
            playPendingArrival(target);
        }
    }

    private static ResonanceNetworkData network(ServerLevel level) {
        return ResonanceNetworkData.get(level);
    }

    private static void playAttunement(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8F, 1.25F);
        level.sendParticles(
                net.minecraft.core.particles.ParticleTypes.END_ROD,
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

    private static void playQueuedTransfer(SpecialChestBlockEntity source) {
        if (!(source.getLevel() instanceof ServerLevel sourceLevel)) return;
        BlockPos pos = source.getBlockPos();
        sourceLevel.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8F, 1.35F);
        sourceLevel.sendParticles(
                net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL,
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

    private static void playPendingArrival(SpecialChestBlockEntity target) {
        if (!(target.getLevel() instanceof ServerLevel targetLevel)) return;
        BlockPos pos = target.getBlockPos();
        targetLevel.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.75F, 0.9F);
        targetLevel.sendParticles(
                net.minecraft.core.particles.ParticleTypes.END_ROD,
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

    private static void playTransfer(SpecialChestBlockEntity source, SpecialChestBlockEntity target) {
        playQueuedTransfer(source);
        playPendingArrival(target);
    }

}
