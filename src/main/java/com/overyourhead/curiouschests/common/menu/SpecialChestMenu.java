package com.overyourhead.curiouschests.common.menu;

import com.overyourhead.curiouschests.common.blockentity.SpecialChestBlockEntity;
import com.overyourhead.curiouschests.common.chest.ChestKind;
import com.overyourhead.curiouschests.common.chest.ChestRules;
import com.overyourhead.curiouschests.common.logic.ArchivistLogic;
import com.overyourhead.curiouschests.common.logic.InfernalLogic;
import com.overyourhead.curiouschests.common.logic.ResonanceLogic;
import com.overyourhead.curiouschests.common.logic.WitchLogic;
import com.overyourhead.curiouschests.common.storage.BottomlessStorage;
import com.overyourhead.curiouschests.core.ModItems;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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
        if (kind == ChestKind.WITCH) {
            return new SimpleContainer(kind.slots()) {
                @Override
                public int getMaxStackSize() {
                    return WitchLogic.MAX_POTIONS_PER_SLOT;
                }

                @Override
                public int getMaxStackSize(ItemStack stack) {
                    return WitchLogic.maxPerSlot(stack);
                }
            };
        }
        if (kind == ChestKind.ARCHIVIST) {
            return new SimpleContainer(kind.slots()) {
                @Override
                public int getMaxStackSize() {
                    return ArchivistLogic.MAX_BOOKS_PER_ENTRY;
                }

                @Override
                public int getMaxStackSize(ItemStack stack) {
                    return stack.is(Items.ENCHANTED_BOOK)
                            ? ArchivistLogic.MAX_BOOKS_PER_ENTRY
                            : super.getMaxStackSize(stack);
                }
            };
        }
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

        if (kind == ChestKind.RESONANT) {
            for (int slot = 0; slot < ResonanceLogic.STORAGE_SLOTS; slot++) {
                int column = slot % 9;
                int row = slot / 9;
                addChestSlot(slot, 8 + column * 18, 18 + row * 18, true);
            }
            addSlot(new Slot(container, ResonanceLogic.CRYSTAL_SLOT, 183, 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.is(ModItems.RESONANCE_CRYSTAL.get())
                            && container.canPlaceItem(ResonanceLogic.CRYSTAL_SLOT, stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                @Override
                public int getMaxStackSize(ItemStack stack) {
                    return 1;
                }
            });
            return;
        }

        if (kind == ChestKind.WITCH) {
            for (int slot = 0; slot < WitchLogic.STORAGE_SLOTS; slot++) {
                int slotIndex = slot;
                int column = slotIndex % 9;
                int row = slotIndex / 9;
                addSlot(new Slot(container, slotIndex, 8 + column * 18, 18 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return WitchLogic.isSupported(stack)
                                && container.canPlaceItem(slotIndex, stack);
                    }

                    @Override
                    public int getMaxStackSize() {
                        return WitchLogic.MAX_POTIONS_PER_SLOT;
                    }

                    @Override
                    public int getMaxStackSize(ItemStack stack) {
                        return WitchLogic.maxPerSlot(stack);
                    }

                    @Override
                    public ItemStack remove(int amount) {
                        return super.remove(Math.min(1, amount));
                    }
                });
            }
            return;
        }

        if (kind == ChestKind.ARCHIVIST) {
            for (int slot = 0; slot < ArchivistLogic.STORAGE_SLOTS; slot++) {
                int slotIndex = slot;
                int column = slotIndex % 9;
                int row = slotIndex / 9;
                addSlot(new Slot(container, slotIndex, 8 + column * 18, 18 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return ArchivistLogic.isProcessableBook(stack)
                                && container.canPlaceItem(slotIndex, stack);
                    }

                    @Override
                    public int getMaxStackSize() {
                        return ArchivistLogic.MAX_BOOKS_PER_ENTRY;
                    }

                    @Override
                    public int getMaxStackSize(ItemStack stack) {
                        return ArchivistLogic.MAX_BOOKS_PER_ENTRY;
                    }

                    @Override
                    public ItemStack remove(int amount) {
                        if (getItem().isEmpty()) return ItemStack.EMPTY;
                        return super.remove(Math.min(1, amount));
                    }
                });
            }
            addSlot(new Slot(container, ArchivistLogic.INPUT_SLOT, 183, 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return ArchivistLogic.isProcessableBook(stack)
                            && container.canPlaceItem(ArchivistLogic.INPUT_SLOT, stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                @Override
                public int getMaxStackSize(ItemStack stack) {
                    return 1;
                }
            });
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
                return allowPlacement
                        && container.canPlaceItem(index, stack)
                        && ChestRules.canStore(stack)
                        && super.mayPlace(stack);
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
                return super.remove(Math.min(amount, getItem().getMaxStackSize()));
            }
        });
    }

    private void addPlayerSlots(Inventory inventory) {
        int yBase;
        if (kind == ChestKind.INFERNAL) {
            yBase = 130;
        } else if (kind == ChestKind.RESONANT) {
            yBase = 85;
        } else if (kind == ChestKind.ARCHIVIST || kind == ChestKind.WITCH) {
            yBase = 139;
        } else {
            yBase = 31 + kind.storageRows() * 18;
        }

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

    public SpecialChestBlockEntity blockEntity() {
        return container instanceof SpecialChestBlockEntity chest ? chest : null;
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
            boolean moved = kind == ChestKind.WITCH
                    ? moveWitchStackToPlayer(source)
                    : moveItemStackTo(source, chestSlots, slots.size(), true);
            if (!moved) return ItemStack.EMPTY;
        } else if (kind == ChestKind.RESONANT && source.is(ModItems.RESONANCE_CRYSTAL.get())) {
            if (!moveItemStackTo(source, ResonanceLogic.CRYSTAL_SLOT, ResonanceLogic.CRYSTAL_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (kind == ChestKind.ARCHIVIST) {
            if (!ArchivistLogic.isProcessableBook(source)
                    || !moveArchivistBooksToArchive(source)) {
                return ItemStack.EMPTY;
            }
        } else if (kind == ChestKind.WITCH) {
            if (!WitchLogic.isSupported(source)
                    || !moveWitchItemsToStorage(source)) {
                return ItemStack.EMPTY;
            }
        } else {
            int destinationEnd = switch (kind) {
                case INFERNAL -> InfernalLogic.INPUT_END;
                case RESONANT -> ResonanceLogic.STORAGE_SLOTS;
                case ARCHIVIST -> ArchivistLogic.STORAGE_SLOTS;
                default -> chestSlots;
            };
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


    private boolean moveWitchItemsToStorage(ItemStack source) {
        if (!WitchLogic.isSupported(source)) return false;

        boolean moved = false;

        for (int index = 0; index < WitchLogic.STORAGE_SLOTS && !source.isEmpty(); index++) {
            Slot target = slots.get(index);
            ItemStack existing = target.getItem();
            if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, source)) continue;

            int limit = target.getMaxStackSize(source);
            int transfer = Math.min(source.getCount(), limit - existing.getCount());
            if (transfer <= 0) continue;

            existing.grow(transfer);
            source.shrink(transfer);
            target.setChanged();
            moved = true;
        }

        for (int index = 0; index < WitchLogic.STORAGE_SLOTS && !source.isEmpty(); index++) {
            Slot target = slots.get(index);
            if (target.hasItem() || !target.mayPlace(source)) continue;

            int transfer = Math.min(source.getCount(), target.getMaxStackSize(source));
            if (transfer <= 0) continue;

            target.setByPlayer(source.copyWithCount(transfer));
            source.shrink(transfer);
            target.setChanged();
            moved = true;
        }

        return moved;
    }

    private boolean moveWitchStackToPlayer(ItemStack source) {
        if (!WitchLogic.isSupported(source)) return false;

        boolean moved = false;

        for (int index = slots.size() - 1; index >= chestSlots && !source.isEmpty(); index--) {
            Slot target = slots.get(index);
            ItemStack existing = target.getItem();
            if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, source)) continue;

            int limit = target.getMaxStackSize(source);
            int transfer = Math.min(source.getCount(), limit - existing.getCount());
            if (transfer <= 0) continue;

            existing.grow(transfer);
            source.shrink(transfer);
            target.setChanged();
            moved = true;
        }

        for (int index = slots.size() - 1; index >= chestSlots && !source.isEmpty(); index--) {
            Slot target = slots.get(index);
            if (target.hasItem() || !target.mayPlace(source)) continue;

            int transfer = Math.min(source.getCount(), target.getMaxStackSize(source));
            if (transfer <= 0) continue;

            target.setByPlayer(source.copyWithCount(transfer));
            source.shrink(transfer);
            target.setChanged();
            moved = true;
        }

        return moved;
    }

    private boolean moveArchivistBooksToArchive(ItemStack source) {
        if (!ArchivistLogic.isProcessableBook(source)) return false;

        boolean moved = false;

        // Enchanted books are normally non-stackable, so vanilla quick-move does not merge them.
        // Merge exact book/component matches manually up to the archive's 64-book limit.
        for (int index = 0; index < ArchivistLogic.STORAGE_SLOTS && !source.isEmpty(); index++) {
            Slot target = slots.get(index);
            ItemStack existing = target.getItem();
            if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, source)) continue;

            int limit = Math.min(ArchivistLogic.MAX_BOOKS_PER_ENTRY, target.getMaxStackSize(source));
            int transfer = Math.min(source.getCount(), limit - existing.getCount());
            if (transfer <= 0) continue;

            existing.grow(transfer);
            source.shrink(transfer);
            target.setChanged();
            moved = true;
        }

        // Put any remainder into empty archive slots. The floating cataloguing slot is intentionally skipped.
        for (int index = 0; index < ArchivistLogic.STORAGE_SLOTS && !source.isEmpty(); index++) {
            Slot target = slots.get(index);
            if (target.hasItem() || !target.mayPlace(source)) continue;

            int transfer = Math.min(source.getCount(), target.getMaxStackSize(source));
            if (transfer <= 0) continue;

            target.setByPlayer(source.copyWithCount(transfer));
            source.shrink(transfer);
            target.setChanged();
            moved = true;
        }

        return moved;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
        container.setChanged();
    }
}
