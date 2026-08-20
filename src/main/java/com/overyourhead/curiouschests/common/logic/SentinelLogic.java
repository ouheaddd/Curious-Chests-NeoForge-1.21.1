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
import net.minecraft.util.Unit;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.warden.WardenAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class SentinelLogic {
    public static final int DARKNESS_TICKS = 60;
    public static final int ALARM_TICKS = 40;
    public static final int WARDEN_COOLDOWN_TICKS = 600; // 30 seconds
    public static final int WARDEN_SEARCH_RADIUS = 48;
    public static final int WARDEN_GUARD_RADIUS = 48;
    public static final int WARDEN_INTRUDER_ESCAPE_RADIUS = 48;
    public static final int WARDEN_GUARD_LIFETIME_TICKS = 700; // 35 seconds
    public static final int MAX_LOG_ENTRIES = 5;
    public static final int LOG_DEDUPLICATION_TICKS = 100;

    private static final int WARDEN_DIG_FAILSAFE_TICKS = 200; // 10 seconds after digging is requested
    private static final String SENTINEL_GUARD_TAG = "curiouschests_sentinel_guard";
    private static final String SENTINEL_RETIRING_TAG = "curiouschests_sentinel_retiring";
    private static final float OPEN_DAMAGE = 4.0F; // two hearts
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
            // gives a sculk "zap", and clicks like a sensor. No shrieker scream.
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
            // Cooldown / no valid spawn position / a retiring Warden nearby: still
            // register the break attempt, but use the quieter warning instead.
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
        UUID trackedId = chest.getSentinelGuardWardenId();
        Warden tracked = trackedId == null ? null : getWarden(level, trackedId);

        // A Sentinel-spawned guard is always reused while it is still on duty.
        // Retargeting does not extend its 35-second lifetime.
        if (tracked != null && tracked.isAlive() && !chest.isSentinelGuardRetiring()) {
            tracked.increaseAngerAt(intruder, 150, true);
            chest.setSentinelGuardIntruder(intruder.getUUID());
            return true;
        }

        if (trackedId != null && (tracked == null || !tracked.isAlive())) {
            chest.clearSentinelGuard();
            trackedId = null;
        }

        List<Warden> nearby = level.getEntitiesOfClass(
                Warden.class,
                new AABB(chestPos).inflate(WARDEN_SEARCH_RADIUS),
                Entity::isAlive
        );

        List<Warden> unownedNearby = nearby.stream()
                // A guard spawned by another Sentinel belongs exclusively to that
                // chest. Ignore it completely here so nearby Sentinels cannot
                // steal its target or suppress each other's own guard spawn.
                .filter(warden -> !warden.getTags().contains(SENTINEL_GUARD_TAG))
                .toList();

        Warden existing = unownedNearby.stream()
                .filter(warden -> !warden.getTags().contains(SENTINEL_RETIRING_TAG))
                .min(Comparator.comparingDouble(warden -> warden.distanceToSqr(intruder)))
                .orElse(null);

        if (existing != null) {
            // Natural / already-existing Wardens are only borrowed for defense.
            // We never attach lifetime, leash, or despawn rules to them.
            existing.increaseAngerAt(intruder, 150, true);
            return true;
        }

        // A non-Sentinel Warden that is already digging away still counts as
        // "a Warden nearby" for anti-spam purposes, but must never be re-angered
        // out of its exit. Guards owned by other Sentinels were filtered above.
        if (!unownedNearby.isEmpty()) return false;

        if (chest.getSentinelWardenCooldown() > 0) return false;

        BlockPos spawnPos = findSpawnPosition(level, chestPos, intruder.blockPosition());
        if (spawnPos == null) return false;

        Warden warden = EntityType.WARDEN.spawn(level, spawnPos, MobSpawnType.TRIGGERED);
        if (warden == null) return false;

        // Keep the emerge animation, but make the memory finite. A permanent
        // IS_EMERGING memory can repeatedly push the Warden back into the emerge
        // activity instead of ever completing the animation.
        warden.getBrain().setMemoryWithExpiry(
                MemoryModuleType.IS_EMERGING,
                Unit.INSTANCE,
                WardenAi.EMERGE_DURATION
        );
        warden.setPose(Pose.EMERGING);
        WardenAi.updateActivity(warden);
        warden.increaseAngerAt(intruder, 150, true);
        warden.addTag(SENTINEL_GUARD_TAG);

        chest.trackSentinelGuard(
                warden.getUUID(),
                intruder.getUUID(),
                level.getGameTime() + WARDEN_GUARD_LIFETIME_TICKS
        );
        chest.setSentinelWardenCooldown(WARDEN_COOLDOWN_TICKS);
        return true;
    }

    /**
     * Ticks only the Warden that this exact Sentinel spawned. Existing natural
     * Wardens are deliberately never tracked or despawned by this code.
     */
    public static void tickGuard(ServerLevel level, BlockPos chestPos, SpecialChestBlockEntity chest) {
        UUID wardenId = chest.getSentinelGuardWardenId();
        if (wardenId == null) return;

        Warden warden = getWarden(level, wardenId);
        if (warden == null || !warden.isAlive()) {
            chest.clearSentinelGuard();
            return;
        }

        long gameTime = level.getGameTime();
        if (chest.isSentinelGuardRetiring()) {
            forceVanillaDigging(warden);

            // Normally the vanilla Digging behavior removes the Warden itself.
            // This is only a safety net against a third-party AI conflict leaving
            // an immortal, invisible, or permanently-digging guard behind.
            if (gameTime - chest.getSentinelGuardRetireStartedAt() >= WARDEN_DIG_FAILSAFE_TICKS) {
                warden.discard();
                chest.clearSentinelGuard();
            }
            return;
        }

        UUID intruderId = chest.getSentinelGuardIntruderId();
        Player intruder = intruderId == null ? null : level.getPlayerByUUID(intruderId);

        boolean lifetimeExpired = gameTime >= chest.getSentinelGuardExpiresAt();
        boolean wardenTooFar = warden.distanceToSqr(
                chestPos.getX() + 0.5,
                chestPos.getY() + 0.5,
                chestPos.getZ() + 0.5
        ) > (double) WARDEN_GUARD_RADIUS * WARDEN_GUARD_RADIUS;
        boolean intruderGone = intruder == null || !intruder.isAlive();
        boolean intruderEscaped = !intruderGone && intruder.distanceToSqr(
                chestPos.getX() + 0.5,
                chestPos.getY() + 0.5,
                chestPos.getZ() + 0.5
        ) > (double) WARDEN_INTRUDER_ESCAPE_RADIUS * WARDEN_INTRUDER_ESCAPE_RADIUS;

        if (lifetimeExpired || wardenTooFar || intruderGone || intruderEscaped) {
            beginGuardRetirement(level, chest, warden);
        }
    }

    /**
     * Called when the chest itself is removed. The guard leaves with its normal
     * Warden digging animation instead of being hard-deleted.
     */
    public static void retireGuardForRemovedChest(ServerLevel level, SpecialChestBlockEntity chest) {
        UUID wardenId = chest.getSentinelGuardWardenId();
        if (wardenId == null) return;

        Warden warden = getWarden(level, wardenId);
        if (warden == null || !warden.isAlive()) {
            chest.clearSentinelGuard();
            return;
        }

        beginGuardRetirement(level, chest, warden);
    }

    private static void beginGuardRetirement(
            ServerLevel level,
            SpecialChestBlockEntity chest,
            Warden warden
    ) {
        if (!chest.isSentinelGuardRetiring()) {
            chest.beginSentinelGuardRetirement(level.getGameTime());
            warden.addTag(SENTINEL_RETIRING_TAG);
        }
        forceVanillaDigging(warden);
    }

    /**
     * Vanilla Wardens enter the DIG activity when DIG_COOLDOWN is absent. We
     * clear combat/navigation state and let WardenAi perform the actual digging
     * animation, sounds, particles, and final removal. No manual DIGGING pose is
     * forced here, avoiding the emerge-loop class of bug we hit previously.
     */
    private static void forceVanillaDigging(Warden warden) {
        LivingEntity target = warden.getTarget();
        if (target != null) {
            warden.clearAnger(target);
        }
        warden.getEntityAngryAt().ifPresent(warden::clearAnger);
        warden.setTarget(null);
        warden.getNavigation().stop();

        warden.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        warden.getBrain().eraseMemory(MemoryModuleType.ROAR_TARGET);
        warden.getBrain().eraseMemory(MemoryModuleType.DISTURBANCE_LOCATION);
        warden.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        warden.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        warden.getBrain().eraseMemory(MemoryModuleType.DIG_COOLDOWN);
        WardenAi.updateActivity(warden);
    }

    private static Warden getWarden(ServerLevel level, UUID id) {
        Entity entity = level.getEntity(id);
        return entity instanceof Warden warden ? warden : null;
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
