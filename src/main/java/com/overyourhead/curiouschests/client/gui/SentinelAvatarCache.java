package com.overyourhead.curiouschests.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Small session-local cache for Sentinel log avatars.
 *
 * PlayerInfo entries disappear from the connection's live player list after a
 * player disconnects. Keeping the last PlayerInfo we actually rendered lets an
 * intrusion log continue to show that player's skin for the rest of the current
 * client connection instead of immediately falling back to an initial.
 */
public final class SentinelAvatarCache {
    private static final Map<UUID, PlayerInfo> CACHE = new HashMap<>();
    private static ClientPacketListener cachedConnection;

    private SentinelAvatarCache() {}

    public static PlayerInfo get(UUID playerId) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != cachedConnection) {
            CACHE.clear();
            cachedConnection = connection;
        }

        if (connection == null) return null;

        PlayerInfo liveInfo = connection.getPlayerInfo(playerId);
        if (liveInfo != null) {
            CACHE.put(playerId, liveInfo);
            return liveInfo;
        }

        return CACHE.get(playerId);
    }
}
