package com.overyourhead.curiouschests.common.network;

import com.overyourhead.curiouschests.CuriousChestsMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record TrapperContentsPayload(int containerId, List<CompoundTag> entities) implements CustomPacketPayload {
    private static final int MAX_ENTITIES = 9;

    public static final Type<TrapperContentsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CuriousChestsMod.MOD_ID, "trapper_contents")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, TrapperContentsPayload> STREAM_CODEC =
            StreamCodec.ofMember(TrapperContentsPayload::encode, TrapperContentsPayload::decode);

    public TrapperContentsPayload {
        List<CompoundTag> safe = new ArrayList<>();
        for (int i = 0; i < Math.min(MAX_ENTITIES, entities.size()); i++) {
            safe.add(entities.get(i).copy());
        }
        entities = List.copyOf(safe);
    }

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(containerId);
        buffer.writeVarInt(entities.size());
        for (CompoundTag entity : entities) {
            buffer.writeNbt(entity);
        }
    }

    private static TrapperContentsPayload decode(RegistryFriendlyByteBuf buffer) {
        int containerId = buffer.readVarInt();
        int encodedCount = Math.max(0, buffer.readVarInt());
        List<CompoundTag> entities = new ArrayList<>(Math.min(MAX_ENTITIES, encodedCount));
        for (int i = 0; i < encodedCount; i++) {
            CompoundTag tag = buffer.readNbt();
            if (i < MAX_ENTITIES && tag != null) entities.add(tag);
        }
        return new TrapperContentsPayload(containerId, entities);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
