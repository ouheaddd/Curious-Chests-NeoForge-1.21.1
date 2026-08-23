package com.overyourhead.curiouschests.common.chest.resonant;

import com.overyourhead.curiouschests.common.blockentity.SpecialChestBlockEntity;
import com.overyourhead.curiouschests.common.chest.ChestKind;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.BitSet;
import java.util.UUID;

/** Per-block state for Resonant identity, attunement, cooldown and received-slot bookkeeping. */
public final class ResonanceChestRuntime {
    public static final String NODE_TAG = "ResonanceNode";
    public static final String ATTUNEMENT_TAG = "ResonanceAttunement";
    public static final String TRANSFER_COOLDOWN_TAG = "ResonanceTransferCooldown";
    public static final String RECEIVED_TAG = "ResonanceReceivedSlots";

    private final SpecialChestBlockEntity owner;
    private final BitSet receivedSlots = new BitSet(ResonanceLogic.STORAGE_SLOTS);
    private UUID nodeId;
    private int attunementTicks;
    private int transferCooldown = ResonanceLogic.TRANSFER_DELAY_TICKS;

    public ResonanceChestRuntime(SpecialChestBlockEntity owner) {
        this.owner = owner;
    }

    public UUID nodeId() {
        return nodeId;
    }

    public void resetForPlacedChest() {
        nodeId = null;
        receivedSlots.clear();
        attunementTicks = 0;
        transferCooldown = ResonanceLogic.TRANSFER_DELAY_TICKS;
    }

    public void ensureInitialized() {
        if (owner.kind() != ChestKind.RESONANT || nodeId != null) return;
        nodeId = UUID.randomUUID();
        transferCooldown = ResonanceLogic.TRANSFER_DELAY_TICKS;
        owner.setChanged();
    }

    public void onSlotEdited(int slot) {
        if (slot >= 0 && slot < ResonanceLogic.STORAGE_SLOTS) receivedSlots.clear(slot);
        if (slot == ResonanceLogic.CRYSTAL_SLOT) attunementTicks = 0;
        transferCooldown = ResonanceLogic.TRANSFER_DELAY_TICKS;
        owner.setChanged();
    }

    public boolean advanceAttunement() {
        attunementTicks++;
        if (attunementTicks < ResonanceLogic.ATTUNEMENT_TICKS) return false;
        attunementTicks = 0;
        return true;
    }

    public void resetAttunement() {
        if (attunementTicks == 0) return;
        attunementTicks = 0;
        owner.setChanged();
    }

    public void setCrystalInternal(NonNullList<ItemStack> items, ItemStack crystal) {
        items.set(ResonanceLogic.CRYSTAL_SLOT, crystal.copyWithCount(1));
        attunementTicks = 0;
        transferCooldown = ResonanceLogic.TRANSFER_DELAY_TICKS;
        owner.setChanged();
    }

    public boolean tickTransferCooldown() {
        if (transferCooldown <= 0) return false;
        transferCooldown--;
        return true;
    }

    public void setTransferCooldown(int ticks) {
        transferCooldown = Math.max(0, ticks);
    }

    public int findOutgoingSlot(NonNullList<ItemStack> items) {
        for (int slot = 0; slot < ResonanceLogic.STORAGE_SLOTS; slot++) {
            if (!items.get(slot).isEmpty() && !receivedSlots.get(slot)) return slot;
        }
        return -1;
    }

    public int insertReceived(NonNullList<ItemStack> items, ItemStack offered) {
        if (owner.kind() != ChestKind.RESONANT || offered.isEmpty()) return 0;

        ItemStack remaining = offered.copy();
        int originalCount = remaining.getCount();

        for (int slot = 0; slot < ResonanceLogic.STORAGE_SLOTS && !remaining.isEmpty(); slot++) {
            if (!receivedSlots.get(slot)) continue;
            ItemStack existing = items.get(slot);
            if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, remaining)) continue;

            int limit = Math.min(existing.getMaxStackSize(), owner.getMaxStackSize(existing));
            int moved = Math.min(limit - existing.getCount(), remaining.getCount());
            if (moved <= 0) continue;
            existing.grow(moved);
            remaining.shrink(moved);
        }

        for (int slot = 0; slot < ResonanceLogic.STORAGE_SLOTS && !remaining.isEmpty(); slot++) {
            if (!items.get(slot).isEmpty()) continue;
            int moved = Math.min(remaining.getMaxStackSize(), remaining.getCount());
            items.set(slot, remaining.copyWithCount(moved));
            receivedSlots.set(slot);
            remaining.shrink(moved);
        }

        int inserted = originalCount - remaining.getCount();
        if (inserted > 0) owner.setChanged();
        return inserted;
    }

    public void shrinkOutgoing(NonNullList<ItemStack> items, int slot, int amount) {
        if (slot < 0 || slot >= ResonanceLogic.STORAGE_SLOTS || amount <= 0) return;
        ItemStack stack = items.get(slot);
        if (stack.isEmpty()) return;
        stack.shrink(Math.min(amount, stack.getCount()));
        if (stack.isEmpty()) items.set(slot, ItemStack.EMPTY);
        receivedSlots.clear(slot);
        owner.setChanged();
    }

    public void clearReceivedSlots() {
        receivedSlots.clear();
    }


    public void restoreLegacyComponentState(UUID legacyNodeId, Object receivedSlotsComponent) {
        nodeId = legacyNodeId;
        receivedSlots.clear();
        if (receivedSlotsComponent instanceof java.util.List<?> slots) {
            for (Object value : slots) {
                if (value instanceof Integer slot && slot >= 0 && slot < ResonanceLogic.STORAGE_SLOTS) {
                    receivedSlots.set(slot);
                }
            }
        }
        attunementTicks = 0;
        transferCooldown = ResonanceLogic.TRANSFER_DELAY_TICKS;
    }

    public void save(CompoundTag tag) {
        if (nodeId != null) tag.putUUID(NODE_TAG, nodeId);
        tag.putInt(ATTUNEMENT_TAG, attunementTicks);
        tag.putInt(TRANSFER_COOLDOWN_TAG, transferCooldown);
        tag.putLongArray(RECEIVED_TAG, receivedSlots.toLongArray());
    }

    public void load(CompoundTag tag) {
        nodeId = tag.hasUUID(NODE_TAG) ? tag.getUUID(NODE_TAG) : null;
        attunementTicks = tag.getInt(ATTUNEMENT_TAG);
        transferCooldown = tag.contains(TRANSFER_COOLDOWN_TAG)
                ? tag.getInt(TRANSFER_COOLDOWN_TAG)
                : ResonanceLogic.TRANSFER_DELAY_TICKS;
        receivedSlots.clear();
        receivedSlots.or(BitSet.valueOf(tag.getLongArray(RECEIVED_TAG)));
    }
}
