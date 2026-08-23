package com.overyourhead.curiouschests.common.chest.enderdispatch;

import com.overyourhead.curiouschests.common.blockentity.SpecialChestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Owns delayed preview/transfer state for Ender Dispatch Chest. */
public final class DispatchChestRuntime {
    public static final String PREVIEW_TAG = "DispatchPreview";
    private static final String COOLDOWN_TAG = "DispatchCooldown";

    private final SpecialChestBlockEntity owner;
    private int cooldown = DispatchLogic.TRANSFER_DELAY_TICKS;
    private int previewTicks;
    private int previewSlot = -1;
    private ItemStack previewStack = ItemStack.EMPTY;
    private boolean internalMutation;

    public DispatchChestRuntime(SpecialChestBlockEntity owner) {
        this.owner = owner;
    }

    public ItemStack previewStack() {
        return previewStack;
    }

    public void onOwnerChanged() {
        Level level = owner.getLevel();
        if (!internalMutation && (level == null || !level.isClientSide)) {
            cooldown = DispatchLogic.TRANSFER_DELAY_TICKS;
        }
    }

    public void writeClientTag(CompoundTag tag, HolderLookup.Provider registries) {
        if (!previewStack.isEmpty()) tag.put(PREVIEW_TAG, previewStack.save(registries));
    }

    public void readClientTag(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.contains(PREVIEW_TAG, Tag.TAG_COMPOUND)) {
            previewStack = ItemStack.parseOptional(registries, tag.getCompound(PREVIEW_TAG));
        } else {
            previewStack = ItemStack.EMPTY;
        }
    }

    public void save(CompoundTag tag) {
        tag.putInt(COOLDOWN_TAG, cooldown);
    }

    public void load(CompoundTag tag) {
        cooldown = tag.contains(COOLDOWN_TAG)
                ? tag.getInt(COOLDOWN_TAG)
                : DispatchLogic.TRANSFER_DELAY_TICKS;
        previewTicks = 0;
        previewSlot = -1;
        previewStack = ItemStack.EMPTY;
        internalMutation = false;
    }

    public void clearPreview(boolean sync) {
        boolean hadPreview = !previewStack.isEmpty();
        previewSlot = -1;
        previewTicks = 0;
        previewStack = ItemStack.EMPTY;
        if (sync && hadPreview) syncPreview();
    }

    public void serverTick(Level level, BlockPos pos) {
        if (!previewStack.isEmpty()) {
            if (previewTicks > 0) previewTicks--;
            if (previewTicks > 0) return;

            boolean moved;
            internalMutation = true;
            try {
                moved = DispatchLogic.dispatchPreviewed(level, pos, owner, previewSlot, previewStack);
            } finally {
                internalMutation = false;
            }
            clearPreview(true);
            cooldown = moved ? DispatchLogic.POST_TRANSFER_GAP_TICKS : DispatchLogic.RETRY_DELAY_TICKS;
            return;
        }

        if (cooldown > 0) cooldown--;
        if (cooldown > 0) return;

        DispatchLogic.Preview preview = DispatchLogic.findPreview(level, pos, owner);
        if (preview != null) {
            beginPreview(preview);
        } else {
            cooldown = DispatchLogic.RETRY_DELAY_TICKS;
        }
    }

    private void beginPreview(DispatchLogic.Preview preview) {
        previewSlot = preview.sourceSlot();
        previewStack = preview.stack().copy();
        previewTicks = DispatchLogic.PREVIEW_TICKS;
        syncPreview();
    }

    private void syncPreview() {
        Level level = owner.getLevel();
        if (level == null || level.isClientSide) return;
        BlockState state = owner.getBlockState();
        level.sendBlockUpdated(owner.getBlockPos(), state, state, Block.UPDATE_CLIENTS);
    }
}
