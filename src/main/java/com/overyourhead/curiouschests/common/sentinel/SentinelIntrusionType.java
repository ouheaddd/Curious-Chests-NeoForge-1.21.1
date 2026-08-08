package com.overyourhead.curiouschests.common.sentinel;

public enum SentinelIntrusionType {
    OPEN,
    BREAK;

    public static SentinelIntrusionType byId(int id) {
        return id == BREAK.ordinal() ? BREAK : OPEN;
    }
}
