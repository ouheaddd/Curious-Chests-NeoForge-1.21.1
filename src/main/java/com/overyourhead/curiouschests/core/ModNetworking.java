package com.overyourhead.curiouschests.core;

import com.overyourhead.curiouschests.client.network.ClientArchivistCatalogHandler;
import com.overyourhead.curiouschests.client.network.ClientSentinelLogHandler;
import com.overyourhead.curiouschests.client.network.ClientTrapperContentsHandler;
import com.overyourhead.curiouschests.common.blockentity.SpecialChestBlockEntity;
import com.overyourhead.curiouschests.common.chest.ChestKind;
import com.overyourhead.curiouschests.common.chest.shared.ChestSorting;
import com.overyourhead.curiouschests.common.menu.SpecialChestMenu;
import com.overyourhead.curiouschests.common.network.ArchivistCatalogPayload;
import com.overyourhead.curiouschests.common.network.RequestSentinelLogPayload;
import com.overyourhead.curiouschests.common.network.SentinelLogPayload;
import com.overyourhead.curiouschests.common.network.SortChestPayload;
import com.overyourhead.curiouschests.common.network.RequestTrapperContentsPayload;
import com.overyourhead.curiouschests.common.network.ReleaseTrapperEntityPayload;
import com.overyourhead.curiouschests.common.network.TrapperContentsPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetworking {
    private ModNetworking() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        // Protocol 4 adds manual Curious Chest sorting.
        PayloadRegistrar registrar = event.registrar("4");
        registrar.playToServer(
                SortChestPayload.TYPE,
                SortChestPayload.STREAM_CODEC,
                ModNetworking::handleSortChest
        );
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
        registrar.playToClient(
                ArchivistCatalogPayload.TYPE,
                ArchivistCatalogPayload.STREAM_CODEC,
                ClientArchivistCatalogHandler::handle
        );
        registrar.playToServer(
                RequestTrapperContentsPayload.TYPE,
                RequestTrapperContentsPayload.STREAM_CODEC,
                ModNetworking::handleTrapperContentsRequest
        );
        registrar.playToServer(
                ReleaseTrapperEntityPayload.TYPE,
                ReleaseTrapperEntityPayload.STREAM_CODEC,
                ModNetworking::handleTrapperRelease
        );
        registrar.playToClient(
                TrapperContentsPayload.TYPE,
                TrapperContentsPayload.STREAM_CODEC,
                ClientTrapperContentsHandler::handle
        );
    }

    private static void handleSortChest(SortChestPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || player.isSpectator()) return;
        if (!(player.containerMenu instanceof SpecialChestMenu menu)) return;
        if (menu.containerId != payload.containerId() || !ChestSorting.supports(menu.kind())) return;
        if (!menu.stillValid(player)) return;

        SpecialChestBlockEntity chest = menu.blockEntity();
        if (chest == null || chest.kind() != menu.kind()) return;

        if (ChestSorting.sort(chest)) {
            menu.broadcastChanges();
            player.serverLevel().playSound(
                    null,
                    chest.getBlockPos(),
                    SoundEvents.BUNDLE_INSERT,
                    SoundSource.BLOCKS,
                    0.22F,
                    1.08F
            );
        }
    }

    private static void handleTrapperContentsRequest(
            RequestTrapperContentsPayload payload,
            IPayloadContext context
    ) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (!(player.containerMenu instanceof SpecialChestMenu menu)) return;
        if (menu.containerId != payload.containerId() || menu.kind() != ChestKind.TRAPPER) return;
        SpecialChestBlockEntity chest = menu.blockEntity();
        if (chest == null) return;
        sendTrapperContents(player, menu, chest);
    }

    private static void handleTrapperRelease(
            ReleaseTrapperEntityPayload payload,
            IPayloadContext context
    ) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (!(player.containerMenu instanceof SpecialChestMenu menu)) return;
        if (menu.containerId != payload.containerId() || menu.kind() != ChestKind.TRAPPER) return;
        SpecialChestBlockEntity chest = menu.blockEntity();
        if (chest == null) return;
        if (chest.releaseTrappedEntity(player.serverLevel(), payload.index())) {
            sendTrapperContents(player, menu, chest);
        }
    }

    private static void sendTrapperContents(ServerPlayer player, SpecialChestMenu menu, SpecialChestBlockEntity chest) {
        PacketDistributor.sendToPlayer(
                player,
                new TrapperContentsPayload(menu.containerId, chest.getTrappedEntityTags())
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
