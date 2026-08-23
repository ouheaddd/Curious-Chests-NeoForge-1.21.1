package com.overyourhead.curiouschests.common.chest.shared;

import com.overyourhead.curiouschests.common.blockentity.SpecialChestBlockEntity;
import com.overyourhead.curiouschests.common.chest.ChestKind;
import com.overyourhead.curiouschests.common.chest.archivist.ArchivistLogic;
import com.overyourhead.curiouschests.common.chest.bottomless.BottomlessStorage;
import com.overyourhead.curiouschests.common.chest.infernal.InfernalLogic;
import com.overyourhead.curiouschests.common.chest.resonant.ResonanceLogic;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;

/** Capability-facing inventory views for a SpecialChestBlockEntity. */
public final class ChestItemHandlers {
    private final SpecialChestBlockEntity owner;
    private final InvWrapper full;
    private final IItemHandler bottomless;
    private final IItemHandler infernal;
    private final IItemHandler resonance;
    private final IItemHandler archivist;

    public ChestItemHandlers(SpecialChestBlockEntity owner) {
        this.owner = owner;
        this.full = new InvWrapper(owner);
        this.bottomless = createBottomless();
        this.infernal = createInfernal();
        this.resonance = new RangedWrapper(full, 0, ResonanceLogic.STORAGE_SLOTS);
        this.archivist = new RangedWrapper(full, ArchivistLogic.INPUT_SLOT, ArchivistLogic.INPUT_SLOT + 1);
    }

    public IItemHandler forKind(ChestKind kind) {
        return switch (kind) {
            case BOTTOMLESS -> bottomless;
            case INFERNAL -> infernal;
            case RESONANT -> resonance;
            case ARCHIVIST -> archivist;
            default -> full;
        };
    }

    private IItemHandler createBottomless() {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return owner.getContainerSize();
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return full.getStackInSlot(slot);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (slot < 0 || slot >= owner.getContainerSize() || stack.isEmpty() || !owner.canPlaceItem(slot, stack)) {
                    return stack;
                }

                ItemStack existing = owner.getItem(slot);
                int limit = BottomlessStorage.maxPerSlot(stack);
                int moved;
                if (existing.isEmpty()) {
                    moved = Math.min(limit, stack.getCount());
                    if (!simulate && moved > 0) owner.setItem(slot, stack.copyWithCount(moved));
                } else {
                    if (!ItemStack.isSameItemSameComponents(existing, stack)) return stack;
                    limit = BottomlessStorage.maxPerSlot(existing);
                    moved = Math.min(Math.max(0, limit - existing.getCount()), stack.getCount());
                    if (!simulate && moved > 0) {
                        existing.grow(moved);
                        owner.setChanged();
                    }
                }

                if (moved <= 0) return stack;
                if (moved >= stack.getCount()) return ItemStack.EMPTY;
                return stack.copyWithCount(stack.getCount() - moved);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return full.extractItem(slot, amount, simulate);
            }

            @Override
            public int getSlotLimit(int slot) {
                return BottomlessStorage.ABSOLUTE_SLOT_LIMIT;
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return slot >= 0 && slot < owner.getContainerSize() && owner.canPlaceItem(slot, stack);
            }
        };
    }

    private IItemHandler createInfernal() {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return InfernalLogic.OUTPUT_END;
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return full.getStackInSlot(slot);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (slot < InfernalLogic.INPUT_START || slot >= InfernalLogic.INPUT_END) return stack;
                return full.insertItem(slot, stack, simulate);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (slot < InfernalLogic.OUTPUT_START || slot >= InfernalLogic.OUTPUT_END) return ItemStack.EMPTY;
                return full.extractItem(slot, amount, simulate);
            }

            @Override
            public int getSlotLimit(int slot) {
                return full.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return slot >= InfernalLogic.INPUT_START
                        && slot < InfernalLogic.INPUT_END
                        && full.isItemValid(slot, stack);
            }
        };
    }
}
