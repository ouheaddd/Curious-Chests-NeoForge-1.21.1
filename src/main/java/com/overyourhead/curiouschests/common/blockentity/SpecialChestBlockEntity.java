package com.overyourhead.curiouschests.common.blockentity;

import com.overyourhead.curiouschests.common.block.AbstractSpecialChestBlock;
import com.overyourhead.curiouschests.common.chest.ChestKind;
import com.overyourhead.curiouschests.common.chest.ChestRules;
import com.overyourhead.curiouschests.common.logic.BuilderSupplyLogic;
import com.overyourhead.curiouschests.common.logic.CollectorLogic;
import com.overyourhead.curiouschests.common.logic.DispatchLogic;
import com.overyourhead.curiouschests.common.logic.InfernalLogic;
import com.overyourhead.curiouschests.common.menu.SpecialChestMenu;
import com.overyourhead.curiouschests.common.storage.BottomlessStorage;
import com.overyourhead.curiouschests.core.ModBlockEntities;
import com.overyourhead.curiouschests.core.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SpecialChestBlockEntity extends BaseContainerBlockEntity {
    private static final String BOTTOMLESS_DEEP_FORMAT_TAG = "BottomlessDeepFormat";

    private NonNullList<ItemStack> items;
    private int workTicker;
    private int dispatchCooldown = DispatchLogic.TRANSFER_DELAY_TICKS;
    private final IItemHandler itemHandler = new InvWrapper(this);
    private final Map<UUID, BuilderSupplyLogic.HeldSnapshot> builderSnapshots = new HashMap<>();

    public SpecialChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SPECIAL_CHEST.get(), pos, state);
        items = NonNullList.withSize(kindFromState(state).slots(), ItemStack.EMPTY);
    }

    public ChestKind kind() {
        return kindFromState(getBlockState());
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    private static ChestKind kindFromState(BlockState state) {
        return state.getBlock() instanceof AbstractSpecialChestBlock chest
                ? chest.kind()
                : ChestKind.COLLECTORS;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (kind() == ChestKind.ENDER_DISPATCH) {
            dispatchCooldown = DispatchLogic.TRANSFER_DELAY_TICKS;
        }
    }

    @Override
    public int getContainerSize() {
        return kind().slots();
    }

    @Override
    public int getMaxStackSize() {
        return kind() == ChestKind.BOTTOMLESS
                ? BottomlessStorage.ABSOLUTE_SLOT_LIMIT
                : super.getMaxStackSize();
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return kind() == ChestKind.BOTTOMLESS
                ? BottomlessStorage.maxPerSlot(stack)
                : super.getMaxStackSize(stack);
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (kind() != ChestKind.BOTTOMLESS) {
            super.setItem(slot, stack);
            if (kind() == ChestKind.ENDER_DISPATCH) {
                dispatchCooldown = DispatchLogic.TRANSFER_DELAY_TICKS;
            }
            return;
        }

        ItemStack stored = stack;
        int limit = BottomlessStorage.maxPerSlot(stack);
        if (!stack.isEmpty() && stack.getCount() > limit) {
            stored = stack.copyWithCount(limit);
        }
        items.set(slot, stored);
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (kind() == ChestKind.INFERNAL && slot >= InfernalLogic.OUTPUT_START) return false;
        return ChestRules.canStore(stack) && super.canPlaceItem(slot, stack);
    }

    @Override
    protected Component getDefaultName() {
        return kind().title();
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return SpecialChestMenu.server(
                ModMenus.forKind(kind()),
                containerId,
                inventory,
                this,
                kind(),
                this::stillValid
        );
    }

    public void loadFromPlacedStack(ItemStack stack) {
        applyComponentsFromItemStack(stack);
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        if (kind() == ChestKind.BOTTOMLESS) {
            ContainerHelper.saveAllItems(tag, BottomlessStorage.splitForSerialization(items), registries);
            tag.putBoolean(BOTTOMLESS_DEEP_FORMAT_TAG, true);
        } else {
            ContainerHelper.saveAllItems(tag, items, registries);
        }

        tag.putInt("WorkTicker", workTicker);
        tag.putInt("DispatchCooldown", dispatchCooldown);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        if (kind() == ChestKind.BOTTOMLESS && tag.getBoolean(BOTTOMLESS_DEEP_FORMAT_TAG)) {
            NonNullList<ItemStack> serialized = NonNullList.withSize(
                    BottomlessStorage.SERIALIZED_SLOTS,
                    ItemStack.EMPTY
            );
            ContainerHelper.loadAllItems(tag, serialized, registries);
            items = BottomlessStorage.mergeSerialized(serialized);
        } else {
            // Also reads old 0.3.0 Bottomless Chest data where every visible
            // slot still held a normal vanilla-sized stack.
            items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag, items, registries);
        }

        workTicker = tag.getInt("WorkTicker");
        dispatchCooldown = tag.contains("DispatchCooldown")
                ? tag.getInt("DispatchCooldown")
                : DispatchLogic.TRANSFER_DELAY_TICKS;
    }

    @Override
    protected void collectImplicitComponents(net.minecraft.core.component.DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        NonNullList<ItemStack> componentItems = kind() == ChestKind.BOTTOMLESS
                ? BottomlessStorage.splitForSerialization(items)
                : items;
        builder.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(componentItems));
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        ItemContainerContents contents = input.getOrDefault(
                DataComponents.CONTAINER,
                ItemContainerContents.EMPTY
        );

        if (kind() == ChestKind.BOTTOMLESS) {
            List<ItemStack> stored = contents.stream().toList();
            NonNullList<ItemStack> serialized = NonNullList.withSize(
                    BottomlessStorage.SERIALIZED_SLOTS,
                    ItemStack.EMPTY
            );
            for (int index = 0; index < Math.min(serialized.size(), stored.size()); index++) {
                serialized.set(index, stored.get(index).copy());
            }
            items = BottomlessStorage.mergeSerialized(serialized);
        } else {
            contents.copyInto(items);
        }
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("Items");
        tag.remove(BOTTOMLESS_DEEP_FORMAT_TAG);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SpecialChestBlockEntity chest) {
        chest.workTicker++;

        if (chest.kind() == ChestKind.INFERNAL
                && chest.workTicker % InfernalLogic.TICKS_PER_ITEM == 0) {
            if (InfernalLogic.smeltOne(level, pos, state, chest)) chest.setChanged();
        }

        if (chest.kind() == ChestKind.ENDER_DISPATCH) {
            if (chest.dispatchCooldown > 0) {
                chest.dispatchCooldown--;
            } else {
                boolean moved = DispatchLogic.dispatchOne(level, pos, chest);
                chest.dispatchCooldown = moved
                        ? DispatchLogic.TRANSFER_DELAY_TICKS
                        : DispatchLogic.RETRY_DELAY_TICKS;
                if (moved) chest.setChanged();
            }
        }

        if (level instanceof ServerLevel serverLevel
                && chest.kind() == ChestKind.BUILDERS
                && chest.workTicker % BuilderSupplyLogic.TICK_INTERVAL == 0) {
            BuilderSupplyLogic.tick(serverLevel, pos, chest, chest.builderSnapshots);
        }

        if (level instanceof ServerLevel serverLevel
                && chest.kind() == ChestKind.COLLECTORS
                && chest.workTicker % CollectorLogic.TICK_INTERVAL == 0) {
            if (CollectorLogic.tick(serverLevel, pos, chest)) chest.setChanged();
        }
    }
}
