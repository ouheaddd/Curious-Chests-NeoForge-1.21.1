package com.overyourhead.curiouschests.common.chest.shared;

import com.overyourhead.curiouschests.common.chest.ChestKind;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/** Shared open/close sound profile for all Curious Chests. */
public final class ChestSounds {
    private ChestSounds() {}

    public static void playBase(Level level, BlockPos pos, SoundEvent sound) {
        level.playSound(
                null,
                pos,
                sound,
                SoundSource.BLOCKS,
                0.5F,
                level.random.nextFloat() * 0.1F + 0.9F
        );
    }

    /** Quiet thematic layer over the normal chest open/close sound. */
    public static void playAccent(Level level, BlockPos pos, ChestKind kind, boolean opening) {
        SoundEvent sound;
        float volume;
        float pitch;

        switch (kind) {
            case RESONANT -> {
                sound = SoundEvents.LARGE_AMETHYST_BUD_BREAK;
                volume = opening ? 0.30F : 0.2F;
                pitch = opening ? 1.08F : 0.84F;
            }
            case SCULK_SENTINEL -> {
                sound = SoundEvents.SCULK_CLICKING;
                volume = 0.15F;
                pitch = opening ? 1.12F : 0.82F;
            }
            case ENDER_DISPATCH -> {
                sound = SoundEvents.ENDERMAN_TELEPORT;
                volume = opening ? 0.192F : 0.10F;
                pitch = opening ? 1.55F : 0.82F;
            }
            case INFERNAL -> {
                sound = SoundEvents.BLAZE_SHOOT;
                volume = opening ? 0.116F : 0.098F;
                pitch = opening ? 1.18F : 0.82F;
            }
            case BUILDERS, BOTTOMLESS -> {
                return;
            }
            case COLLECTORS -> {
                sound = SoundEvents.BUNDLE_INSERT;
                volume = opening ? 0.63F : 0.375F;
                pitch = opening ? 1.02F : 0.80F;
            }
            case ARCHIVIST -> {
                sound = SoundEvents.BOOK_PAGE_TURN;
                volume = opening ? 0.372F : 0.21F;
                pitch = opening ? 1.04F : 0.82F;
            }
            case WITCH -> {
                sound = SoundEvents.BREWING_STAND_BREW;
                volume = opening ? 0.266F : 0.11F;
                pitch = opening ? 1.08F : 0.84F;
            }
            case TRAPPER -> {
                sound = opening ? SoundEvents.VAULT_OPEN_SHUTTER : SoundEvents.VAULT_CLOSE_SHUTTER;
                volume = opening ? 0.28F : 0.22F;
                pitch = opening ? 1.04F : 0.94F;
            }
            default -> {
                return;
            }
        }

        level.playSound(null, pos, sound, SoundSource.BLOCKS, volume, pitch);
    }
}
