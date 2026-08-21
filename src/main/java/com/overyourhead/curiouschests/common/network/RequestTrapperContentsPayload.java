package com.overyourhead.curiouschests.common.network;

import com.overyourhead.curiouschests.CuriousChestsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RequestTrapperContentsPayload(int containerId) implements CustomPacketPayload {
    public static final Type<RequestTrapperContentsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CuriousChestsMod.MOD_ID, "request_trapper_contents")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestTrapperContentsPayload> STREAM_CODEC =
            StreamCodec.ofMember(RequestTrapperContentsPayload::encode, RequestTrapperContentsPayload::decode);

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(containerId);
    }

    private static RequestTrapperContentsPayload decode(RegistryFriendlyByteBuf buffer) {
        return new RequestTrapperContentsPayload(buffer.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
