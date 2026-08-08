package com.overyourhead.curiouschests.common.network;

import com.overyourhead.curiouschests.CuriousChestsMod;
import com.overyourhead.curiouschests.common.sentinel.SentinelIntrusionType;
import com.overyourhead.curiouschests.common.sentinel.SentinelLogEntry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record SentinelLogPayload(
        int containerId,
        long serverGameTime,
        List<SentinelLogEntry> entries
) implements CustomPacketPayload {
    private static final int MAX_ENTRIES = 5;
    private static final int MAX_NAME_LENGTH = 64;

    public static final Type<SentinelLogPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CuriousChestsMod.MOD_ID, "sentinel_log")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, SentinelLogPayload> STREAM_CODEC =
            StreamCodec.ofMember(SentinelLogPayload::encode, SentinelLogPayload::decode);

    public SentinelLogPayload {
        entries = List.copyOf(entries.subList(0, Math.min(MAX_ENTRIES, entries.size())));
    }

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(containerId);
        buffer.writeVarLong(serverGameTime);
        buffer.writeVarInt(entries.size());
        for (SentinelLogEntry entry : entries) {
            buffer.writeUUID(entry.playerId());
            buffer.writeUtf(entry.playerName(), MAX_NAME_LENGTH);
            buffer.writeByte(entry.action().ordinal());
            buffer.writeVarLong(entry.gameTime());
        }
    }

    private static SentinelLogPayload decode(RegistryFriendlyByteBuf buffer) {
        int containerId = buffer.readVarInt();
        long serverGameTime = buffer.readVarLong();
        int encodedCount = Math.max(0, buffer.readVarInt());
        List<SentinelLogEntry> entries = new ArrayList<>(Math.min(MAX_ENTRIES, encodedCount));
        for (int index = 0; index < encodedCount; index++) {
            SentinelLogEntry entry = new SentinelLogEntry(
                    buffer.readUUID(),
                    buffer.readUtf(MAX_NAME_LENGTH),
                    SentinelIntrusionType.byId(buffer.readUnsignedByte()),
                    buffer.readVarLong()
            );
            if (index < MAX_ENTRIES) entries.add(entry);
        }
        return new SentinelLogPayload(containerId, serverGameTime, entries);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
