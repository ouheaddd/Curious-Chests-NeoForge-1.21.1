package com.overyourhead.curiouschests.common.chest.bottomless;

import com.overyourhead.curiouschests.common.blockentity.SpecialChestBlockEntity;
import com.overyourhead.curiouschests.common.chest.ChestKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Runtime state for the item displayed in the Bottomless Chest front frame. */
public final class BottomlessDisplayRuntime {
    public static final String TAG = "StorageDisplayItem";

    private final SpecialChestBlockEntity owner;
    private ItemStack item = ItemStack.EMPTY;

    public BottomlessDisplayRuntime(SpecialChestBlockEntity owner) {
        this.owner = owner;
    }

    public ItemStack item() {
        return item;
    }

    public boolean set(Player player, ItemStack heldStack) {
        if (owner.kind() != ChestKind.BOTTOMLESS || heldStack.isEmpty()) return false;

        ItemStack previous = item;
        ItemStack replacement = heldStack.copyWithCount(1);
        if (!player.getAbilities().instabuild) heldStack.shrink(1);

        item = replacement;
        if (!previous.isEmpty()) give(player, previous);
        sync();
        return true;
    }

    public boolean remove(Player player) {
        if (owner.kind() != ChestKind.BOTTOMLESS || item.isEmpty()) return false;
        ItemStack removed = item;
        item = ItemStack.EMPTY;
        give(player, removed);
        sync();
        return true;
    }

    public void write(CompoundTag tag, HolderLookup.Provider registries) {
        if (!item.isEmpty()) tag.put(TAG, item.save(registries));
    }

    public void read(CompoundTag tag, HolderLookup.Provider registries) {
        item = tag.contains(TAG, Tag.TAG_COMPOUND)
                ? ItemStack.parseOptional(registries, tag.getCompound(TAG))
                : ItemStack.EMPTY;
    }

    public void loadPersistent(CompoundTag tag, HolderLookup.Provider registries) {
        if (owner.kind() == ChestKind.BOTTOMLESS) read(tag, registries);
        else item = ItemStack.EMPTY;
    }

    public void drop(Level level, BlockPos pos) {
        if (owner.kind() != ChestKind.BOTTOMLESS || item.isEmpty()) return;
        Containers.dropItemStack(
                level,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                item.copy()
        );
        item = ItemStack.EMPTY;
    }

    private static void give(Player player, ItemStack stack) {
        if (!player.addItem(stack)) player.drop(stack, false);
    }

    private void sync() {
        owner.setChanged();
        Level level = owner.getLevel();
        if (level == null || level.isClientSide) return;
        BlockState state = owner.getBlockState();
        level.sendBlockUpdated(owner.getBlockPos(), state, state, Block.UPDATE_CLIENTS);
    }
}
