package com.overyourhead.curiouschests.common.network;

import com.overyourhead.curiouschests.CuriousChestsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record ArchivistCatalogPayload(List<Entry> entries) implements CustomPacketPayload {
    private static final int MAX_ENTRIES = 256;
    private static final int MAX_ID_LENGTH = 128;

    public static final Type<ArchivistCatalogPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CuriousChestsMod.MOD_ID, "archivist_catalog")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ArchivistCatalogPayload> STREAM_CODEC =
            StreamCodec.ofMember(ArchivistCatalogPayload::encode, ArchivistCatalogPayload::decode);

    public ArchivistCatalogPayload {
        entries = List.copyOf(entries.subList(0, Math.min(MAX_ENTRIES, entries.size())));
    }

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(entries.size());
        for (Entry entry : entries) {
            buffer.writeUtf(entry.enchantmentId().toString(), MAX_ID_LENGTH);
            buffer.writeVarInt(entry.level());
            buffer.writeVarInt(entry.count());
        }
    }

    private static ArchivistCatalogPayload decode(RegistryFriendlyByteBuf buffer) {
        int encodedCount = Math.max(0, buffer.readVarInt());
        List<Entry> entries = new ArrayList<>(Math.min(MAX_ENTRIES, encodedCount));
        for (int index = 0; index < encodedCount; index++) {
            ResourceLocation enchantmentId = ResourceLocation.parse(buffer.readUtf(MAX_ID_LENGTH));
            int level = Math.max(1, buffer.readVarInt());
            int count = Math.max(0, buffer.readVarInt());
            if (index < MAX_ENTRIES && count > 0) {
                entries.add(new Entry(enchantmentId, level, count));
            }
        }
        return new ArchivistCatalogPayload(entries);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Entry(ResourceLocation enchantmentId, int level, int count) {}
}
