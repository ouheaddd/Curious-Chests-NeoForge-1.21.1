package com.overyourhead.curiouschests.common.network;

import com.overyourhead.curiouschests.CuriousChestsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ReleaseTrapperEntityPayload(int containerId, int index) implements CustomPacketPayload {
    public static final Type<ReleaseTrapperEntityPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CuriousChestsMod.MOD_ID, "release_trapper_entity")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ReleaseTrapperEntityPayload> STREAM_CODEC =
            StreamCodec.ofMember(ReleaseTrapperEntityPayload::encode, ReleaseTrapperEntityPayload::decode);

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(containerId);
        buffer.writeVarInt(index);
    }

    private static ReleaseTrapperEntityPayload decode(RegistryFriendlyByteBuf buffer) {
        return new ReleaseTrapperEntityPayload(buffer.readVarInt(), buffer.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
