package com.overyourhead.curiouschests.common.logic;

import com.overyourhead.curiouschests.common.blockentity.SpecialChestBlockEntity;
import com.overyourhead.curiouschests.common.sentinel.SentinelIntrusionType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ShriekParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.warden.WardenAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Unit;

import java.util.Comparator;

public final class SentinelLogic {
    public static final int DARKNESS_TICKS = 60;
    public static final int ALARM_TICKS = 40;
    public static final int WARDEN_COOLDOWN_TICKS = 2400;
    public static final int WARDEN_SEARCH_RADIUS = 48;
    public static final int MAX_LOG_ENTRIES = 5;
    public static final int LOG_DEDUPLICATION_TICKS = 100;

    private static final float OPEN_DAMAGE = 1.0F; // half a heart
    private static final int SHRIEK_WAVE_COUNT = 5;
    private static final int SHRIEK_WAVE_DELAY_TICKS = 5;

    private SentinelLogic() {}

    public static void trigger(
            ServerLevel level,
            BlockPos pos,
            SpecialChestBlockEntity chest,
            Player intruder,
            SentinelIntrusionType action
    ) {
        chest.addSentinelLog(intruder, action, level.getGameTime());
        chest.pulseSentinelAlarm(ALARM_TICKS);

        intruder.addEffect(new MobEffectInstance(MobEffects.DARKNESS, DARKNESS_TICKS, 0, false, true, true));

        if (action == SentinelIntrusionType.OPEN) {
            // A normal trespass is only a warning: the chest notices the player,
            // gives a tiny sculk "zap", and clicks like a sensor. No shrieker scream.
            intruder.hurt(level.damageSources().generic(), OPEN_DAMAGE);
            playSensorWarning(level, pos);
            sendSculkSouls(level, pos, 4);
            return;
        }

        // BREAK is the serious alarm. Only scream like a shrieker when the Warden
        // defense actually activates (existing Warden angered or a new one spawned).
        boolean wardenDefenseActivated = summonOrAngerWarden(level, pos, chest, intruder);
        if (wardenDefenseActivated) {
            level.playSound(null, pos, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.BLOCKS, 1.1F, 0.9F);
            sendShriekerWaves(level, pos);
            sendSculkSouls(level, pos, 10);
        } else {
            // Cooldown / no valid spawn position: still register the break attempt,
            // but fall back to the quieter sensor warning instead of fake-shrieking.
            playSensorWarning(level, pos);
            sendSculkSouls(level, pos, 4);
        }
    }

    private static void playSensorWarning(ServerLevel level, BlockPos pos) {
        level.playSound(
                null,
                pos,
                SoundEvents.SCULK_CLICKING,
                SoundSource.BLOCKS,
                0.85F,
                1.0F
        );
    }

    private static void sendSculkSouls(ServerLevel level, BlockPos pos, int count) {
        level.sendParticles(
                ParticleTypes.SCULK_SOUL,
                pos.getX() + 0.5,
                pos.getY() + 1.0,
                pos.getZ() + 0.5,
                count,
                0.35,
                0.35,
                0.35,
                0.02
        );
    }

    private static void sendShriekerWaves(ServerLevel level, BlockPos pos) {
        for (int wave = 0; wave < SHRIEK_WAVE_COUNT; wave++) {
            level.sendParticles(
                    new ShriekParticleOption(wave * SHRIEK_WAVE_DELAY_TICKS),
                    pos.getX() + 0.5,
                    pos.getY() + 1.0,
                    pos.getZ() + 0.5,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0
            );
        }
    }

    /**
     * @return true when a Warden was actually used for the defense: either an
     * existing one was angered or a new one was successfully spawned.
     */
    private static boolean summonOrAngerWarden(
            ServerLevel level,
            BlockPos chestPos,
            SpecialChestBlockEntity chest,
            Player intruder
    ) {
        Warden existing = level.getEntitiesOfClass(
                        Warden.class,
                        new AABB(chestPos).inflate(WARDEN_SEARCH_RADIUS),
                        Entity::isAlive
                ).stream()
                .min(Comparator.comparingDouble(warden -> warden.distanceToSqr(intruder)))
                .orElse(null);

        if (existing != null) {
            existing.increaseAngerAt(intruder, 150, true);
            // Important anti-spam guard: using an already-existing Warden now starts
            // the same cooldown as spawning one. Killing it cannot immediately cause
            // this Sentinel to replace it on the next break attempt.
            chest.setSentinelWardenCooldown(WARDEN_COOLDOWN_TICKS);
            return true;
        }

        if (chest.getSentinelWardenCooldown() > 0) return false;

        BlockPos spawnPos = findSpawnPosition(level, chestPos, intruder.blockPosition());
        if (spawnPos == null) return false;

        Warden warden = EntityType.WARDEN.spawn(level, spawnPos, MobSpawnType.TRIGGERED);
        if (warden == null) return false;

        // A triggered entity spawn alone does not communicate the full sculk
        // shrieker state. Seed the same emerge memory and pose used by the
        // Warden brain so it climbs out of the ground before hunting.
        warden.getBrain().setMemory(MemoryModuleType.IS_EMERGING, Unit.INSTANCE);
        warden.setPose(Pose.EMERGING);
        WardenAi.updateActivity(warden);
        warden.increaseAngerAt(intruder, 150, true);
        chest.setSentinelWardenCooldown(WARDEN_COOLDOWN_TICKS);
        return true;
    }

    private static BlockPos findSpawnPosition(ServerLevel level, BlockPos chestPos, BlockPos intruderPos) {
        Direction preferred = Direction.getNearest(
                intruderPos.getX() - chestPos.getX(),
                0.0,
                intruderPos.getZ() - chestPos.getZ()
        );

        for (int radius = 2; radius <= 4; radius++) {
            BlockPos preferredPos = chestPos.relative(preferred, radius);
            BlockPos valid = findGroundAt(level, preferredPos);
            if (valid != null) return valid;

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    valid = findGroundAt(level, chestPos.offset(dx, 0, dz));
                    if (valid != null) return valid;
                }
            }
        }
        return null;
    }

    private static BlockPos findGroundAt(ServerLevel level, BlockPos horizontal) {
        for (int yOffset = 1; yOffset >= -2; yOffset--) {
            BlockPos candidate = horizontal.offset(0, yOffset, 0);
            BlockPos floor = candidate.below();
            if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) continue;
            if (!level.getFluidState(candidate).isEmpty()) continue;
            AABB spawnBox = EntityType.WARDEN.getSpawnAABB(
                    candidate.getX() + 0.5,
                    candidate.getY(),
                    candidate.getZ() + 0.5
            );
            if (level.noCollision(spawnBox)) return candidate;
        }
        return null;
    }
}
