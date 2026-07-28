package com.overyourhead.curiouschests.common.chest;

import net.minecraft.network.chat.Component;

public enum ChestKind {
    BOTTOMLESS("bottomless", 63),
    INFERNAL("infernal", 27),
    ENDER_DISPATCH("ender_dispatch", 36),
    BUILDERS("builders", 45),
    COLLECTORS("collectors", 36);

    private final String id;
    private final int slots;

    ChestKind(String id, int slots) {
        this.id = id;
        this.slots = slots;
    }

    public String id() {
        return id;
    }

    public int slots() {
        return slots;
    }

    public Component title() {
        return Component.translatable("container.curiouschests." + id);
    }
}
