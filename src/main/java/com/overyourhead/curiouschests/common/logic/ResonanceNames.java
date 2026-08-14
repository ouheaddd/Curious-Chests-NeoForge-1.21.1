package com.overyourhead.curiouschests.common.logic;

import java.util.UUID;

/**
 * Stable, display-only names for Resonant Chest node UUIDs.
 *
 * The UUID remains the real routing identity. The visible name is derived from it,
 * so no extra data needs to be stored or synchronized.
 */
public final class ResonanceNames {
    private static final String[] FIRST = {
            "Violet", "Silent", "Hollow", "Distant", "Fading", "Shimmering", "Wandering", "Sleeping",
            "Twisted", "Hidden", "Pale", "Deep", "Soft", "Bright", "Lost", "Crooked",
            "Tiny", "Odd", "Strange", "Muffled", "Broken", "Restless", "Gentle", "Ancient",
            "Quiet", "Secret", "Lunar", "Stellar", "Velvet", "Dusky", "Echoing", "Glassy",
            "Singing", "Humming", "Ringing", "Drifting", "Purple", "Mystic", "Faint", "Shifting",
            "Dreaming", "Wayward", "Veiled", "Low", "High", "Warm", "Cold", "Softened",
            "Crystalline", "Gleaming", "Muted", "Slight", "Patient", "Wobbly", "Suspicious", "Curious",
            "Unstable", "Polite", "Local", "Forbidden", "Definitely", "Very", "Drowsy", "Awkward"
    };

    private static final String[] SECOND = {
            "Echo", "Chime", "Geode", "Prism", "Chorus", "Hum", "Tone", "Frequency",
            "Ring", "Facet", "Wake", "Star", "Bell", "Note", "Pulse", "Song",
            "Whisper", "Murmur", "Shard", "Spark", "Glow", "Wave", "Signal", "Resonance",
            "Refrain", "Orbit", "Bloom", "Gleam", "Veil", "Halo", "Rift", "Pebble",
            "Bonk", "Ping", "Ding", "Noise", "Rock", "Rattle", "Tinkle", "Wobble",
            "Buzz", "Clink", "Knock", "Flicker", "Glint", "Ripple", "Thread", "Beacon",
            "Lantern", "Crown", "Vale", "Dream", "Secret", "Memory", "Path", "Door",
            "Key", "Shrine", "Choir", "Comet", "Pocket", "Jingle", "Whistle", "Thrum"
    };

    // Rare complete names. These are deliberately a little sillier than the normal combinations.
    private static final String[] RARE = {
            "Forbidden Ding", "Very Purple", "Do Not Ring", "The Purple One",
            "Definitely Amethyst", "Suspicious Bonk", "Echo McEcho", "Distant Bonk",
            "Lost Ping", "Loud Rock", "Wrong Frequency", "Tiny Cathedral",
            "One More Chime", "Probably Fine", "Unscheduled Resonance", "Not A Geode",
            "Certified Crystal", "Slightly Haunted", "Please Hold", "Emergency Ding",
            "Purple Business", "Local Frequency", "Oddly Specific Hum", "Polite Screaming",
            "The Other Geode", "Very Normal Crystal", "Absolutely A Rock", "Quietly Concerned",
            "Pending Resonance", "No Signal", "Crystal Maybe", "Please Do Not Bonk"
    };

    private ResonanceNames() {
    }

    public static String forNode(UUID nodeId) {
        long hash = mix64(nodeId.getMostSignificantBits()
                ^ Long.rotateLeft(nodeId.getLeastSignificantBits(), 27));

        // About 1 in 32 nodes gets a hand-written rare name.
        if ((hash & 31L) == 0L) {
            int rareIndex = Math.floorMod((int) (hash >>> 32), RARE.length);
            return RARE[rareIndex];
        }

        int firstIndex = (int) (hash & 63L);
        int secondIndex = (int) ((hash >>> 6) & 63L);
        return FIRST[firstIndex] + " " + SECOND[secondIndex];
    }

    private static long mix64(long value) {
        value ^= value >>> 30;
        value *= 0xbf58476d1ce4e5b9L;
        value ^= value >>> 27;
        value *= 0x94d049bb133111ebL;
        value ^= value >>> 31;
        return value;
    }
}
