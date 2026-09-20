package com.overyourhead.curiouschests.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/** Client key mappings used by Curious Chests container screens. */
public final class ModKeyMappings {
    public static final String SORT_CHEST_KEY = "key.curiouschests.sort_chest";

    public static final KeyMapping SORT_CHEST = new KeyMapping(
            SORT_CHEST_KEY,
            KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_S,
            "key.categories.curiouschests"
    );

    private ModKeyMappings() {}
}
