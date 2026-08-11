package com.overyourhead.curiouschests.client.network;

import com.overyourhead.curiouschests.client.gui.ArchivistBookViewScreen;
import com.overyourhead.curiouschests.common.network.ArchivistCatalogPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class ClientArchivistCatalogHandler {
    private static final int BOOK_TEXT_WIDTH = 114;
    private static final int MAX_TEXT_LINES = 13;

    private ClientArchivistCatalogHandler() {}

    public static void handle(ArchivistCatalogPayload payload, IPayloadContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        HolderLookup.RegistryLookup<Enchantment> enchantments = minecraft.level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT);

        List<DisplayEntry> entries = new ArrayList<>();
        for (ArchivistCatalogPayload.Entry entry : payload.entries()) {
            ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, entry.enchantmentId());
            Holder.Reference<Enchantment> holder = enchantments.get(key).orElse(null);
            if (holder == null) continue;

            entries.add(new DisplayEntry(
                    holder,
                    entry.level(),
                    entry.count(),
                    holder.value().description().getString().toLowerCase(Locale.ROOT),
                    entry.enchantmentId().toString()
            ));
        }

        entries.sort(Comparator
                .comparingInt(DisplayEntry::count).reversed()
                .thenComparing(DisplayEntry::sortName)
                .thenComparingInt(DisplayEntry::level)
                .thenComparing(DisplayEntry::id));

        List<Component> pages = buildPages(minecraft.font, entries);
        BookViewScreen.BookAccess bookAccess = new BookViewScreen.BookAccess(pages);
        if (entries.isEmpty()) {
            minecraft.setScreen(new ArchivistBookViewScreen(
                    bookAccess,
                    Component.translatable("gui.curiouschests.archivist.catalog_empty")
                            .withStyle(ChatFormatting.BLACK)
            ));
        } else {
            minecraft.setScreen(new BookViewScreen(bookAccess));
        }
    }

    private static List<Component> buildPages(Font font, List<DisplayEntry> entries) {
        if (entries.isEmpty()) {
            return List.of(
                    Component.translatable("gui.curiouschests.archivist.catalog_title")
                            .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD)
            );
        }

        List<Component> pages = new ArrayList<>();
        Component page = Component.empty();
        int usedLines = 0;
        boolean hasEntryOnPage = false;

        Component title = Component.translatable("gui.curiouschests.archivist.catalog_title")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD);
        page = page.copy().append(title).append("\n\n");
        usedLines = 2;

        for (DisplayEntry entry : entries) {
            Component line = Component.empty()
                    .append(Enchantment.getFullname(entry.holder(), entry.level()))
                    .append(Component.literal("  ×" + entry.count()).withStyle(ChatFormatting.BLACK));
            int lineCount = Math.max(1, font.split(line, BOOK_TEXT_WIDTH).size());

            if (usedLines > 0 && usedLines + lineCount > MAX_TEXT_LINES) {
                pages.add(page);
                page = Component.empty();
                usedLines = 0;
                hasEntryOnPage = false;
            }

            if (hasEntryOnPage) page = page.copy().append("\n");
            page = page.copy().append(line);
            usedLines += lineCount;
            hasEntryOnPage = true;
        }

        if (!page.getString().isEmpty()) pages.add(page);
        return pages.isEmpty() ? List.of(Component.empty()) : pages;
    }

    private record DisplayEntry(
            Holder.Reference<Enchantment> holder,
            int level,
            int count,
            String sortName,
            String id
    ) {}
}
