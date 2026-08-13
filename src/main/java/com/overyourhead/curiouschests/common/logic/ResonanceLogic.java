package com.overyourhead.curiouschests.common.logic;

import com.overyourhead.curiouschests.common.blockentity.SpecialChestBlockEntity;
import com.overyourhead.curiouschests.common.chest.ChestKind;
import com.overyourhead.curiouschests.common.item.ResonanceCrystalItem;
import com.overyourhead.curiouschests.core.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
        NodeRef endpoint = data.getNode(targetId);
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
            NodeRef endpoint,
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
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(ResonanceNetworkData::new, ResonanceNetworkData::load),
                ResonanceNetworkData.DATA_NAME
        );
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

    private record NodeRef(ResourceKey<Level> dimension, BlockPos pos) {}

    /** Global resonance registry/mailbox, intentionally attached to the Overworld. */
    private static final class ResonanceNetworkData extends SavedData {
        private static final String DATA_NAME = "curiouschests_resonance_network";
        private static final String NODES_TAG = "Nodes";
        private static final String PENDING_TAG = "Pending";
        private static final String NODE_ID_TAG = "Node";
        private static final String DIMENSION_TAG = "Dimension";
        private static final String X_TAG = "X";
        private static final String Y_TAG = "Y";
        private static final String Z_TAG = "Z";
        private static final String QUEUE_SIZE_TAG = "QueueSize";

        private final Map<UUID, NodeRef> nodes = new HashMap<>();
        private final Map<UUID, List<ItemStack>> pending = new HashMap<>();

        private static ResonanceNetworkData load(CompoundTag tag, HolderLookup.Provider registries) {
            ResonanceNetworkData data = new ResonanceNetworkData();

            ListTag nodesTag = tag.getList(NODES_TAG, Tag.TAG_COMPOUND);
            for (int index = 0; index < nodesTag.size(); index++) {
                CompoundTag nodeTag = nodesTag.getCompound(index);
                if (!nodeTag.hasUUID(NODE_ID_TAG)) continue;

                ResourceLocation dimensionId = ResourceLocation.tryParse(nodeTag.getString(DIMENSION_TAG));
                if (dimensionId == null) continue;

                UUID nodeId = nodeTag.getUUID(NODE_ID_TAG);
                ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
                BlockPos pos = new BlockPos(
                        nodeTag.getInt(X_TAG),
                        nodeTag.getInt(Y_TAG),
                        nodeTag.getInt(Z_TAG)
                );
                data.nodes.put(nodeId, new NodeRef(dimension, pos));
            }

            ListTag pendingTag = tag.getList(PENDING_TAG, Tag.TAG_COMPOUND);
            for (int index = 0; index < pendingTag.size(); index++) {
                CompoundTag queueTag = pendingTag.getCompound(index);
                if (!queueTag.hasUUID(NODE_ID_TAG)) continue;

                int queueSize = Math.min(
                        Math.max(0, queueTag.getInt(QUEUE_SIZE_TAG)),
                        MAX_PENDING_STACKS
                );
                if (queueSize <= 0) continue;

                NonNullList<ItemStack> serialized = NonNullList.withSize(queueSize, ItemStack.EMPTY);
                ContainerHelper.loadAllItems(queueTag, serialized, registries);

                List<ItemStack> queue = new ArrayList<>();
                for (ItemStack stack : serialized) {
                    if (!stack.isEmpty()) queue.add(stack.copy());
                }
                if (!queue.isEmpty()) {
                    data.pending.put(queueTag.getUUID(NODE_ID_TAG), queue);
                }
            }

            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag nodesTag = new ListTag();
            for (Map.Entry<UUID, NodeRef> entry : nodes.entrySet()) {
                CompoundTag nodeTag = new CompoundTag();
                nodeTag.putUUID(NODE_ID_TAG, entry.getKey());
                nodeTag.putString(DIMENSION_TAG, entry.getValue().dimension().location().toString());
                nodeTag.putInt(X_TAG, entry.getValue().pos().getX());
                nodeTag.putInt(Y_TAG, entry.getValue().pos().getY());
                nodeTag.putInt(Z_TAG, entry.getValue().pos().getZ());
                nodesTag.add(nodeTag);
            }
            tag.put(NODES_TAG, nodesTag);

            ListTag pendingTag = new ListTag();
            for (Map.Entry<UUID, List<ItemStack>> entry : pending.entrySet()) {
                List<ItemStack> queue = entry.getValue();
                if (queue.isEmpty()) continue;

                int queueSize = Math.min(queue.size(), MAX_PENDING_STACKS);
                NonNullList<ItemStack> serialized = NonNullList.withSize(queueSize, ItemStack.EMPTY);
                for (int index = 0; index < queueSize; index++) {
                    serialized.set(index, queue.get(index).copy());
                }

                CompoundTag queueTag = new CompoundTag();
                queueTag.putUUID(NODE_ID_TAG, entry.getKey());
                queueTag.putInt(QUEUE_SIZE_TAG, queueSize);
                ContainerHelper.saveAllItems(queueTag, serialized, true, registries);
                pendingTag.add(queueTag);
            }
            tag.put(PENDING_TAG, pendingTag);

            return tag;
        }

        private void registerNode(UUID nodeId, ResourceKey<Level> dimension, BlockPos pos) {
            NodeRef next = new NodeRef(dimension, new BlockPos(pos.getX(), pos.getY(), pos.getZ()));
            NodeRef previous = nodes.put(nodeId, next);
            if (!next.equals(previous)) setDirty();
        }

        private void unregisterNode(UUID nodeId, ResourceKey<Level> dimension, BlockPos pos) {
            NodeRef current = nodes.get(nodeId);
            if (current == null
                    || !current.dimension().equals(dimension)
                    || !current.pos().equals(pos)) {
                return;
            }

            nodes.remove(nodeId);
            // Physical removal destroys this network identity. Pending mail is drained by
            // unregisterPlaced before the endpoint record is removed.
            setDirty();
        }

        private NodeRef getNode(UUID nodeId) {
            return nodes.get(nodeId);
        }

        private List<ItemStack> takePending(UUID nodeId) {
            List<ItemStack> queue = pending.remove(nodeId);
            if (queue == null || queue.isEmpty()) return List.of();

            List<ItemStack> result = new ArrayList<>(queue.size());
            for (ItemStack stack : queue) {
                if (!stack.isEmpty()) result.add(stack.copy());
            }
            setDirty();
            return result;
        }

        /** Returns the number of items accepted by the persistent mailbox. */
        private int enqueue(UUID targetId, ItemStack offered) {
            if (offered.isEmpty()) return 0;

            List<ItemStack> queue = pending.computeIfAbsent(targetId, ignored -> new ArrayList<>());
            ItemStack remaining = offered.copy();
            int originalCount = remaining.getCount();

            for (ItemStack existing : queue) {
                if (remaining.isEmpty()) break;
                if (!ItemStack.isSameItemSameComponents(existing, remaining)) continue;

                int room = existing.getMaxStackSize() - existing.getCount();
                if (room <= 0) continue;
                int moved = Math.min(room, remaining.getCount());
                existing.grow(moved);
                remaining.shrink(moved);
            }

            while (!remaining.isEmpty() && queue.size() < MAX_PENDING_STACKS) {
                int moved = Math.min(remaining.getMaxStackSize(), remaining.getCount());
                queue.add(remaining.copyWithCount(moved));
                remaining.shrink(moved);
            }

            int accepted = originalCount - remaining.getCount();
            if (accepted > 0) setDirty();
            if (queue.isEmpty()) pending.remove(targetId);
            return accepted;
        }

        /**
         * Attempts a bounded number of queued stack deliveries and returns item count inserted.
         * Blocked entries are rotated to the back so one incompatible/full stack cannot starve
         * later deliveries forever.
         */
        private int deliver(UUID targetId, SpecialChestBlockEntity target, int maxAttempts) {
            List<ItemStack> queue = pending.get(targetId);
            if (queue == null || queue.isEmpty() || maxAttempts <= 0) return 0;

            int delivered = 0;
            boolean removedEmpty = false;
            int attempts = Math.min(maxAttempts, queue.size());
            for (int attempt = 0; attempt < attempts; attempt++) {
                ItemStack queued = queue.remove(0);
                if (queued.isEmpty()) {
                    removedEmpty = true;
                    continue;
                }

                int inserted = target.insertResonanceReceived(queued.copy());
                if (inserted > 0) {
                    queued.shrink(Math.min(inserted, queued.getCount()));
                    delivered += inserted;
                }

                if (!queued.isEmpty()) {
                    queue.add(queued);
                }
            }

            if (queue.isEmpty()) pending.remove(targetId);
            if (delivered > 0 || removedEmpty) setDirty();
            return delivered;
        }
    }
}
