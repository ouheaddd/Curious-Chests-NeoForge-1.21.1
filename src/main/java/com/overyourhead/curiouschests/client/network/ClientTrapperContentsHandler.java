package com.overyourhead.curiouschests.client.network;

import com.overyourhead.curiouschests.client.gui.SpecialChestScreen;
import com.overyourhead.curiouschests.common.network.TrapperContentsPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientTrapperContentsHandler {
    private ClientTrapperContentsHandler() {}

    public static void handle(TrapperContentsPayload payload, IPayloadContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (minecraft.screen instanceof SpecialChestScreen screen) {
                screen.applyTrapperContents(payload);
            }
        });
    }
}
