package com.overyourhead.curiouschests.common.chest.layout;

import com.overyourhead.curiouschests.common.chest.ChestKind;
import com.overyourhead.curiouschests.common.chest.archivist.ArchivistLogic;
import com.overyourhead.curiouschests.common.chest.bottomless.BottomlessStorage;
import com.overyourhead.curiouschests.common.chest.witch.WitchLogic;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Client-side placeholder containers used before the server menu data arrives. */
public final class ChestClientContainers {
    private ChestClientContainers() {}

    public static Container create(ChestKind kind) {
        if (kind == ChestKind.WITCH) {
            return new SimpleContainer(kind.slots()) {
                @Override
                public int getMaxStackSize() {
                    return 64;
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
}
