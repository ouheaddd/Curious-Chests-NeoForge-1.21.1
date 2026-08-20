package com.overyourhead.curiouschests.common.sentinel;

import java.util.UUID;

public record SentinelLogEntry(
        UUID playerId,
        String playerName,
        SentinelIntrusionType action,
        long gameTime,
        int attempts
) {
    public SentinelLogEntry {
        attempts = Math.max(1, attempts);
    }

    public SentinelLogEntry(
            UUID playerId,
            String playerName,
            SentinelIntrusionType action,
            long gameTime
    ) {
        this(playerId, playerName, action, gameTime, 1);
    }
}
