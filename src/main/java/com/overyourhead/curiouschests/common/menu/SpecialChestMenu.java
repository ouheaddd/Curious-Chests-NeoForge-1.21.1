package com.overyourhead.curiouschests.common.menu;

import com.overyourhead.curiouschests.common.chest.ChestKind;
import com.overyourhead.curiouschests.common.chest.ChestRules;
import com.overyourhead.curiouschests.common.logic.InfernalLogic;
import com.overyourhead.curiouschests.common.storage.BottomlessStorage;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

public final class SpecialChestMenu extends AbstractContainerMenu {
    private final Container container;
    private final ChestKind kind;
    private final Predicate<Player> validity;
    private final int chestSlots;

    public static SpecialChestMenu client(MenuType<SpecialChestMenu> type, int id, Inventory inventory, ChestKind kind) {
        return new SpecialChestMenu(
                type,
                id,
                inventory,
                createClientContainer(kind),
                kind,
                player -> true
        );
    }


    private static Container createClientContainer(ChestKind kind) {
        if (kind != ChestKind.BOTTOMLESS) return new SimpleContainer(kind.slots());

        return new SimpleContainer(kind.slots()) {
            @Override
            public int getMaxStackSize() {
                return BottomlessStorage.ABSOLUTE_SLOT_LIMIT;
            }

            @Override
            public int getMaxStackSize(ItemStack stack) {
                return BottomlessStorage.maxPerSlot(stack);
            }
        };
    }

    public static SpecialChestMenu server(
            MenuType<SpecialChestMenu> type,
            int id,
            Inventory inventory,
            Container container,
            ChestKind kind,
            Predicate<Player> validity
    ) {
        return new SpecialChestMenu(type, id, inventory, container, kind, validity);
    }

    private SpecialChestMenu(
            MenuType<SpecialChestMenu> type,
            int id,
            Inventory playerInventory,
            Container container,
            ChestKind kind,
            Predicate<Player> validity
    ) {
        super(type, id);
        checkContainerSize(container, kind.slots());
        this.container = container;
        this.kind = kind;
        this.validity = validity;
        this.chestSlots = kind.slots();

        container.startOpen(playerInventory.player);
        addChestSlots();
        addPlayerSlots(playerInventory);
    }

    private void addChestSlots() {
        if (kind == ChestKind.INFERNAL) {
            for (int slot = InfernalLogic.INPUT_START; slot < InfernalLogic.INPUT_END; slot++) {
                addChestSlot(slot, 8 + slot * 18, 18, true);
            }
            for (int slot = InfernalLogic.OUTPUT_START; slot < InfernalLogic.OUTPUT_END; slot++) {
                int outputIndex = slot - InfernalLogic.OUTPUT_START;
                int column = outputIndex % 9;
                int row = outputIndex / 9;
                addChestSlot(slot, 8 + column * 18, 72 + row * 18, false);
            }
            return;
        }

        for (int slot = 0; slot < kind.slots(); slot++) {
            int column = slot % 9;
            int row = slot / 9;
            addChestSlot(slot, 8 + column * 18, 18 + row * 18, true);
        }
    }

    private void addChestSlot(int index, int x, int y, boolean allowPlacement) {
        addSlot(new Slot(container, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return allowPlacement && ChestRules.canStore(stack) && super.mayPlace(stack);
            }

            @Override
            public int getMaxStackSize() {
                return kind == ChestKind.BOTTOMLESS
                        ? BottomlessStorage.ABSOLUTE_SLOT_LIMIT
                        : super.getMaxStackSize();
            }

            @Override
            public int getMaxStackSize(ItemStack stack) {
                return kind == ChestKind.BOTTOMLESS
                        ? BottomlessStorage.maxPerSlot(stack)
                        : super.getMaxStackSize(stack);
            }

            @Override
            public ItemStack remove(int amount) {
                if (kind != ChestKind.BOTTOMLESS || getItem().isEmpty()) {
                    return super.remove(amount);
                }

                // Never place an oversized stack on the player's cursor. A normal
                // click takes at most one ordinary stack while the deep remainder
                // stays in the chest. Shift-click can still distribute more across
                // several player inventory slots through quickMoveStack.
                return super.remove(Math.min(amount, getItem().getMaxStackSize()));
            }
        });
    }

    private void addPlayerSlots(Inventory inventory) {
        int rows = kind == ChestKind.INFERNAL ? 5 : (kind.slots() + 8) / 9;
        int yBase = kind == ChestKind.INFERNAL ? 130 : 31 + rows * 18;

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(
                        inventory,
                        column + row * 9 + 9,
                        8 + column * 18,
                        yBase + row * 18
                ));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, yBase + 58));
        }
    }

    public ChestKind kind() {
        return kind;
    }

    public int chestSlots() {
        return chestSlots;
    }

    @Override
    public boolean stillValid(Player player) {
        return validity.test(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return result;

        ItemStack source = slot.getItem();
        result = source.copy();

        if (index < chestSlots) {
            if (!moveItemStackTo(source, chestSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            int destinationEnd = kind == ChestKind.INFERNAL ? InfernalLogic.INPUT_END : chestSlots;
            if (!ChestRules.canStore(source) || !moveItemStackTo(source, 0, destinationEnd, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (source.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (source.getCount() == result.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, source);
        return result;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
        container.setChanged();
    }
}
