package com.overyourhead.curiouschests.core;

import com.overyourhead.curiouschests.client.network.ClientSentinelLogHandler;
import com.overyourhead.curiouschests.common.blockentity.SpecialChestBlockEntity;
import com.overyourhead.curiouschests.common.chest.ChestKind;
import com.overyourhead.curiouschests.common.menu.SpecialChestMenu;
import com.overyourhead.curiouschests.common.network.RequestSentinelLogPayload;
import com.overyourhead.curiouschests.common.network.SentinelLogPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetworking {
    private ModNetworking() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                RequestSentinelLogPayload.TYPE,
                RequestSentinelLogPayload.STREAM_CODEC,
                ModNetworking::handleSentinelLogRequest
        );
        registrar.playToClient(
                SentinelLogPayload.TYPE,
                SentinelLogPayload.STREAM_CODEC,
                ClientSentinelLogHandler::handle
        );
    }

    private static void handleSentinelLogRequest(
            RequestSentinelLogPayload payload,
            IPayloadContext context
    ) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (!(player.containerMenu instanceof SpecialChestMenu menu)) return;
        if (menu.containerId != payload.containerId()) return;
        if (menu.kind() != ChestKind.SCULK_SENTINEL) return;

        SpecialChestBlockEntity chest = menu.blockEntity();
        if (chest == null || !chest.canSentinelAccess(player)) return;

        PacketDistributor.sendToPlayer(
                player,
                new SentinelLogPayload(
                        menu.containerId,
                        player.serverLevel().getGameTime(),
                        chest.getSentinelLogEntries()
                )
        );
    }
}
