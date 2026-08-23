package com.overyourhead.curiouschests.common.chest.resonant;

import com.overyourhead.curiouschests.common.blockentity.SpecialChestBlockEntity;
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
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Persistent placed-node registry and unloaded-target mailbox for Resonant Chests. */
final class ResonanceNetworkData extends SavedData {
    private static final String DATA_NAME = "curiouschests_resonance_network";
    private static final String NODES_TAG = "Nodes";
    private static final String PENDING_TAG = "Pending";
    private static final String NODE_ID_TAG = "Node";
    private static final String DIMENSION_TAG = "Dimension";
    private static final String X_TAG = "X";
    private static final String Y_TAG = "Y";
    private static final String Z_TAG = "Z";
    private static final String QUEUE_SIZE_TAG = "QueueSize";
    private static final int MAX_PENDING_STACKS = ResonanceLogic.STORAGE_SLOTS;

    private final Map<UUID, ResonanceEndpoint> nodes = new HashMap<>();
    private final Map<UUID, List<ItemStack>> pending = new HashMap<>();

    static ResonanceNetworkData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(ResonanceNetworkData::new, ResonanceNetworkData::load),
                DATA_NAME
        );
    }

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
            data.nodes.put(nodeId, new ResonanceEndpoint(dimension, pos));
        }

        ListTag pendingTag = tag.getList(PENDING_TAG, Tag.TAG_COMPOUND);
        for (int index = 0; index < pendingTag.size(); index++) {
            CompoundTag queueTag = pendingTag.getCompound(index);
            if (!queueTag.hasUUID(NODE_ID_TAG)) continue;

            int queueSize = Math.min(Math.max(0, queueTag.getInt(QUEUE_SIZE_TAG)), MAX_PENDING_STACKS);
            if (queueSize <= 0) continue;

            NonNullList<ItemStack> serialized = NonNullList.withSize(queueSize, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(queueTag, serialized, registries);

            List<ItemStack> queue = new ArrayList<>();
            for (ItemStack stack : serialized) {
                if (!stack.isEmpty()) queue.add(stack.copy());
            }
            if (!queue.isEmpty()) data.pending.put(queueTag.getUUID(NODE_ID_TAG), queue);
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag nodesTag = new ListTag();
        for (Map.Entry<UUID, ResonanceEndpoint> entry : nodes.entrySet()) {
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
            for (int index = 0; index < queueSize; index++) serialized.set(index, queue.get(index).copy());

            CompoundTag queueTag = new CompoundTag();
            queueTag.putUUID(NODE_ID_TAG, entry.getKey());
            queueTag.putInt(QUEUE_SIZE_TAG, queueSize);
            ContainerHelper.saveAllItems(queueTag, serialized, true, registries);
            pendingTag.add(queueTag);
        }
        tag.put(PENDING_TAG, pendingTag);
        return tag;
    }

    void registerNode(UUID nodeId, ResourceKey<Level> dimension, BlockPos pos) {
        ResonanceEndpoint next = new ResonanceEndpoint(dimension, new BlockPos(pos.getX(), pos.getY(), pos.getZ()));
        ResonanceEndpoint previous = nodes.put(nodeId, next);
        if (!next.equals(previous)) setDirty();
    }

    void unregisterNode(UUID nodeId, ResourceKey<Level> dimension, BlockPos pos) {
        ResonanceEndpoint current = nodes.get(nodeId);
        if (current == null || !current.dimension().equals(dimension) || !current.pos().equals(pos)) return;
        nodes.remove(nodeId);
        setDirty();
    }

    ResonanceEndpoint getNode(UUID nodeId) {
        return nodes.get(nodeId);
    }

    List<ItemStack> takePending(UUID nodeId) {
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
    int enqueue(UUID targetId, ItemStack offered) {
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

    /** Bounded delivery; blocked entries rotate to the back of the queue. */
    int deliver(UUID targetId, SpecialChestBlockEntity target, int maxAttempts) {
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
            if (!queued.isEmpty()) queue.add(queued);
        }

        if (queue.isEmpty()) pending.remove(targetId);
        if (delivered > 0 || removedEmpty) setDirty();
        return delivered;
    }
}

record ResonanceEndpoint(ResourceKey<Level> dimension, BlockPos pos) {}
