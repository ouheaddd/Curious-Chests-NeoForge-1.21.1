package com.overyourhead.curiouschests.common.chest;

import net.minecraft.network.chat.Component;

public enum ChestKind {
    BOTTOMLESS("bottomless", 63, 240),
    INFERNAL("infernal", 27, 214),
    ENDER_DISPATCH("ender_dispatch", 36, 186),
    BUILDERS("builders", 45, 204),
    COLLECTORS("collectors", 36, 186);

    private final String id;
    private final int slots;
    private final int screenHeight;

    ChestKind(String id, int slots, int screenHeight) {
        this.id = id;
        this.slots = slots;
        this.screenHeight = screenHeight;
    }

    public String id() {
        return id;
    }

    public int slots() {
        return slots;
    }

    public int screenHeight() {
        return screenHeight;
    }

    public Component title() {
        return Component.translatable("container.curiouschests." + id);
    }
}
