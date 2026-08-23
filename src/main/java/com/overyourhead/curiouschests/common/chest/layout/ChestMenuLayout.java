package com.overyourhead.curiouschests.common.chest.layout;

import com.overyourhead.curiouschests.common.chest.ChestKind;

/** Pixel layout for the shared menu. Visual tuning lives here, away from slot behavior. */
public final class ChestMenuLayout {
    public static final int BUILDERS_CRAFT_X = 193;
    public static final int BUILDERS_CRAFT_Y = 18;
    public static final int BUILDERS_RESULT_X = 212;
    public static final int BUILDERS_RESULT_Y = 99;

    public static final int RESONANT_CRYSTAL_SLOT_X = 195;
    public static final int RESONANT_CRYSTAL_SLOT_Y = 18;
    public static final int WITCH_CONTENT_OFFSET_X = 16;
    public static final int WITCH_CONTENT_OFFSET_Y = 16;

    private ChestMenuLayout() {}

    public static int chestOffsetX(ChestKind kind) {
        return switch (kind) {
            case ENDER_DISPATCH -> 0;
            case SCULK_SENTINEL -> 6;
            case RESONANT -> 8;
            default -> 0;
        };
    }

    public static int chestOffsetY(ChestKind kind) {
        return switch (kind) {
            case ENDER_DISPATCH -> -7;
            case SCULK_SENTINEL -> 5;
            case RESONANT -> 2;
            default -> 0;
        };
    }

    public static int inventoryOffsetX(ChestKind kind) {
        return switch (kind) {
            case ENDER_DISPATCH -> 0;
            case SCULK_SENTINEL -> 7;
            case RESONANT -> 8;
            case WITCH -> WITCH_CONTENT_OFFSET_X;
            default -> 0;
        };
    }

    public static int inventoryOffsetY(ChestKind kind) {
        return switch (kind) {
            case ENDER_DISPATCH -> -2;
            case SCULK_SENTINEL -> 2;
            case RESONANT -> 14;
            case BUILDERS -> -2;
            case WITCH -> WITCH_CONTENT_OFFSET_Y;
            default -> 0;
        };
    }

    public static int hotbarOffsetX(ChestKind kind) {
        return switch (kind) {
            case ENDER_DISPATCH -> 0;
            case SCULK_SENTINEL -> 7;
            case RESONANT -> 8;
            case WITCH -> WITCH_CONTENT_OFFSET_X;
            default -> 0;
        };
    }

    public static int hotbarOffsetY(ChestKind kind) {
        return switch (kind) {
            case ENDER_DISPATCH -> 1;
            case SCULK_SENTINEL -> 7;
            case RESONANT -> 20;
            case WITCH -> WITCH_CONTENT_OFFSET_Y;
            default -> 0;
        };
    }

    public static int playerInventoryBaseY(ChestKind kind) {
        return switch (kind) {
            case INFERNAL -> 130;
            case RESONANT -> 85;
            case ARCHIVIST, WITCH -> 139;
            case TRAPPER -> 62;
            default -> 31 + kind.storageRows() * 18;
        };
    }

    public static int playerInventoryBaseX(ChestKind kind) {
        return kind == ChestKind.INFERNAL ? 10 : 8;
    }
}
