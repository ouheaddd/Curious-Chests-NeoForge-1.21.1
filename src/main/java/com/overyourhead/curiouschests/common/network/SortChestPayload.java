package com.overyourhead.curiouschests.common.network;

import com.overyourhead.curiouschests.CuriousChestsMod;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Requests a server-authoritative sort of the currently open Curious Chest. */
public record SortChestPayload(int containerId) implements CustomPacketPayload {
    public static final Type<SortChestPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CuriousChestsMod.MOD_ID, "sort_chest")
    );

    public static final StreamCodec<io.netty.buffer.ByteBuf, SortChestPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    SortChestPayload::containerId,
                    SortChestPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
