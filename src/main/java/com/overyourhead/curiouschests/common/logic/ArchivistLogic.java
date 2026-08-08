package com.overyourhead.curiouschests.common.logic;

import com.overyourhead.curiouschests.common.blockentity.SpecialChestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.List;

public final class ArchivistLogic {
    public static final int STORAGE_SLOTS = 54;
    public static final int INPUT_SLOT = 54;
    public static final int MAX_BOOKS_PER_ENTRY = 64;
    public static final int PROCESS_INTERVAL_TICKS = 10;

    private ArchivistLogic() {}

    public static boolean isProcessableBook(ItemStack stack) {
        if (!stack.is(Items.ENCHANTED_BOOK)) return false;
        ItemEnchantments enchantments = stack.getOrDefault(
                DataComponents.STORED_ENCHANTMENTS,
                ItemEnchantments.EMPTY
        );
        return !enchantments.isEmpty();
    }

    public static boolean processOne(ServerLevel level, BlockPos pos, SpecialChestBlockEntity chest) {
        ItemStack input = chest.getItem(INPUT_SLOT);
        if (!isProcessableBook(input)) return false;

        ItemEnchantments enchantments = input.getOrDefault(
                DataComponents.STORED_ENCHANTMENTS,
                ItemEnchantments.EMPTY
        );
        List<ItemStack> separatedBooks = new ArrayList<>(enchantments.size());
        for (var entry : enchantments.entrySet()) {
            separatedBooks.add(singleEnchantmentBook(entry.getKey(), entry.getIntValue()));
        }
        if (separatedBooks.isEmpty()) return false;

        NonNullList<ItemStack> simulated = NonNullList.withSize(STORAGE_SLOTS, ItemStack.EMPTY);
        for (int slot = 0; slot < STORAGE_SLOTS; slot++) {
            simulated.set(slot, chest.getItem(slot).copy());
        }
        for (ItemStack book : separatedBooks) {
            if (!insertOne(simulated, book)) return false;
        }

        for (int slot = 0; slot < STORAGE_SLOTS; slot++) {
            chest.setItem(slot, simulated.get(slot));
        }
        chest.removeItem(INPUT_SLOT, 1);
        chest.setChanged();

        level.playSound(
                null,
                pos,
                SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.BLOCKS,
                0.55F,
                1.15F
        );
        level.sendParticles(
                ParticleTypes.ENCHANT,
                pos.getX() + 0.5D,
                pos.getY() + 1.0D,
                pos.getZ() + 0.5D,
                Math.min(18, 5 + separatedBooks.size() * 3),
                0.35D,
                0.25D,
                0.35D,
                0.02D
        );
        return true;
    }

    private static ItemStack singleEnchantmentBook(Holder<Enchantment> enchantment, int level) {
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        mutable.set(enchantment, level);

        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        book.set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());
        return book;
    }

    private static boolean insertOne(NonNullList<ItemStack> storage, ItemStack offered) {
        for (int slot = 0; slot < storage.size(); slot++) {
            ItemStack existing = storage.get(slot);
            if (existing.isEmpty()) continue;
            if (!ItemStack.isSameItemSameComponents(existing, offered)) continue;
            if (existing.getCount() >= MAX_BOOKS_PER_ENTRY) continue;
            existing.grow(1);
            return true;
        }

        for (int slot = 0; slot < storage.size(); slot++) {
            if (!storage.get(slot).isEmpty()) continue;
            storage.set(slot, offered.copyWithCount(1));
            return true;
        }
        return false;
    }
}
