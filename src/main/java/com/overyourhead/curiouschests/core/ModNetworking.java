package com.overyourhead.curiouschests.core;

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

import java.util.Objects;
import java.util.function.BiConsumer;

public final class ModNetworking {
    private static volatile BiConsumer<SentinelLogPayload, IPayloadContext> sentinelLogClientHandler = (payload, context) -> {};
    private static volatile BiConsumer<ArchivistCatalogPayload, IPayloadContext> archivistCatalogClientHandler = (payload, context) -> {};
    private static volatile BiConsumer<TrapperContentsPayload, IPayloadContext> trapperContentsClientHandler = (payload, context) -> {};

    private ModNetworking() {}

    /**
     * Installed only from the Dist.CLIENT event subscriber. Keeping the actual
     * client handler classes out of this common class prevents dedicated-server
     * class loading from ever resolving Minecraft GUI classes.
     */
    public static void installClientHandlers(
            BiConsumer<SentinelLogPayload, IPayloadContext> sentinelHandler,
            BiConsumer<ArchivistCatalogPayload, IPayloadContext> archivistHandler,
            BiConsumer<TrapperContentsPayload, IPayloadContext> trapperHandler
    ) {
        sentinelLogClientHandler = Objects.requireNonNull(sentinelHandler);
        archivistCatalogClientHandler = Objects.requireNonNull(archivistHandler);
        trapperContentsClientHandler = Objects.requireNonNull(trapperHandler);
    }

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
                ModNetworking::handleSentinelLogClient
        );
        registrar.playToClient(
                ArchivistCatalogPayload.TYPE,
                ArchivistCatalogPayload.STREAM_CODEC,
                ModNetworking::handleArchivistCatalogClient
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
                ModNetworking::handleTrapperContentsClient
        );
    }

    private static void handleSentinelLogClient(SentinelLogPayload payload, IPayloadContext context) {
        sentinelLogClientHandler.accept(payload, context);
    }

    private static void handleArchivistCatalogClient(ArchivistCatalogPayload payload, IPayloadContext context) {
        archivistCatalogClientHandler.accept(payload, context);
    }

    private static void handleTrapperContentsClient(TrapperContentsPayload payload, IPayloadContext context) {
        trapperContentsClientHandler.accept(payload, context);
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
