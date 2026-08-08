package com.overyourhead.curiouschests.client.network;

import com.overyourhead.curiouschests.client.gui.SpecialChestScreen;
import com.overyourhead.curiouschests.common.network.SentinelLogPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientSentinelLogHandler {
    private ClientSentinelLogHandler() {}

    public static void handle(SentinelLogPayload payload, IPayloadContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof SpecialChestScreen screen) {
            screen.applySentinelLog(payload);
        }
    }
}
