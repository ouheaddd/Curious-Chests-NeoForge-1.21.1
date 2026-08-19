package com.overyourhead.curiouschests.common.chest;

import net.minecraft.network.chat.Component;

public enum ChestKind {
    BOTTOMLESS("bottomless", 63, 176, 240),
    INFERNAL("infernal", 27, 182, 223),
    ENDER_DISPATCH("ender_dispatch", 36, 176, 186),
    BUILDERS("builders", 45, 260, 204),
    COLLECTORS("collectors", 36, 176, 186),
    SCULK_SENTINEL("sculk_sentinel", 36, 176, 186),
    RESONANT("resonant", 28, 176, 168),
    ARCHIVIST("archivist", 55, 176, 222),
    WITCH("witch", 54, 176, 222);

    private final String id;
    private final int slots;
    private final int screenWidth;
    private final int screenHeight;

    ChestKind(String id, int slots, int screenWidth, int screenHeight) {
        this.id = id;
        this.slots = slots;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    public String id() {
        return id;
    }

    public int slots() {
        return slots;
    }

    public int storageSlots() {
        return switch (this) {
            case RESONANT -> 27;
            case ARCHIVIST -> 54;
            default -> slots;
        };
    }

    public int storageRows() {
        return (storageSlots() + 8) / 9;
    }

    public int screenWidth() {
        return screenWidth;
    }

    public int screenHeight() {
        return screenHeight;
    }

    public Component title() {
        return Component.translatable("container.curiouschests." + id);
    }
}
