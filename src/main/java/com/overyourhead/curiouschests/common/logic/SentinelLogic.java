package com.overyourhead.curiouschests.common.logic;

import com.overyourhead.curiouschests.common.blockentity.SpecialChestBlockEntity;
import com.overyourhead.curiouschests.common.sentinel.SentinelIntrusionType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
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
    public static final int MAX_LOG_ENTRIES = 5;
    public static final int LOG_DEDUPLICATION_TICKS = 100;

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
        level.playSound(null, pos, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.BLOCKS, 1.1F, 0.9F);
        level.sendParticles(
                ParticleTypes.SCULK_SOUL,
                pos.getX() + 0.5,
                pos.getY() + 1.0,
                pos.getZ() + 0.5,
                10,
                0.35,
                0.35,
                0.35,
                0.02
        );

        if (action == SentinelIntrusionType.BREAK) {
            summonOrAngerWarden(level, pos, chest, intruder);
        }
    }

    private static void summonOrAngerWarden(
            ServerLevel level,
            BlockPos chestPos,
            SpecialChestBlockEntity chest,
            Player intruder
    ) {
        Warden existing = level.getEntitiesOfClass(
                        Warden.class,
                        new AABB(chestPos).inflate(32.0),
                        Entity::isAlive
                ).stream()
                .min(Comparator.comparingDouble(warden -> warden.distanceToSqr(intruder)))
                .orElse(null);

        if (existing != null) {
            existing.increaseAngerAt(intruder, 150, true);
            return;
        }

        if (chest.getSentinelWardenCooldown() > 0) return;

        BlockPos spawnPos = findSpawnPosition(level, chestPos, intruder.blockPosition());
        if (spawnPos == null) return;

        Warden warden = EntityType.WARDEN.spawn(level, spawnPos, MobSpawnType.TRIGGERED);
        if (warden != null) {
            // A triggered entity spawn alone does not communicate the full sculk
            // shrieker state. Seed the same emerge memory and pose used by the
            // Warden brain so it climbs out of the ground before hunting.
            warden.getBrain().setMemory(MemoryModuleType.IS_EMERGING, Unit.INSTANCE);
            warden.setPose(Pose.EMERGING);
            WardenAi.updateActivity(warden);
            warden.increaseAngerAt(intruder, 150, true);
            chest.setSentinelWardenCooldown(WARDEN_COOLDOWN_TICKS);
        }
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
