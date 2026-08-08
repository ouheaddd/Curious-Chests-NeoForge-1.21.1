package com.overyourhead.curiouschests.common.sentinel;

import java.util.UUID;

public record SentinelLogEntry(
        UUID playerId,
        String playerName,
        SentinelIntrusionType action,
        long gameTime
) {}
