package com.overyourhead.curiouschests.common.logic;

import com.overyourhead.curiouschests.common.blockentity.SpecialChestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Automatic creature capture for the Trapper's Chest. */
public final class TrapperLogic {
    public static final int CAPACITY = 9;
    public static final double CAPTURE_RADIUS = 5.0D;
    public static final int CAPTURE_TICKS = 26;
    public static final int CAPTURE_TIMEOUT_TICKS = 80;
    public static final int POST_CAPTURE_COOLDOWN_TICKS = 18;
    public static final int SCAN_INTERVAL_TICKS = 4;
    public static final int RELEASE_IMMUNITY_TICKS = 200;
    public static final int ABORT_IMMUNITY_TICKS = 40;

    private static final String IMMUNITY_UNTIL_TAG = "CuriousChestsTrapperImmuneUntil";
    private static final String CLAIM_POS_TAG = "CuriousChestsTrapperClaimPos";
    private static final String CLAIM_DIMENSION_TAG = "CuriousChestsTrapperClaimDimension";
    private static final String CLAIM_UNTIL_TAG = "CuriousChestsTrapperClaimUntil";
    private static final int CLAIM_LEASE_TICKS = 40;

    private static final double ABORT_RADIUS = 7.0D;
    // The physical phase aims above the block, like a creature being pulled into
    // an opened lid. Final suction starts before the target can get stuck on the
    // the rendered chest's collision.
    private static final double MOUTH_HEIGHT = 1.18D;
    private static final double MOUTH_ENTRY_DISTANCE = 1.35D;
    private static final double INTAKE_HEIGHT = 0.70D;

    // Once the mob reaches the mouth, a short controlled phase moves its rendered
    // body through the collision and into the interior. This makes the effect
    // independent of the chest model's collision and also keeps it
    // easy to retune later when the final Trapper model is installed.
    private static final int FINAL_SUCTION_TICKS = 10;
    private static final double FINAL_SUCTION_START_SCALE = 1.00D;
    private static final double FINAL_SUCTION_END_SCALE = 0.08D;
    private static final double FINAL_SUCTION_LERP = 0.36D;

    private TrapperLogic() {}

    public static void tick(ServerLevel level, BlockPos pos, BlockState state, SpecialChestBlockEntity chest) {
        // A successful sneak-break removes the block synchronously. If the chest
        // survived until another tick, some later handler canceled that break, so
        // discard the transient packing intent and resume normal behavior.
        chest.clearTrapperPackBreakIntent();
        if (chest.getTrapperCaptureCooldown() > 0) {
            chest.setTrapperCaptureCooldown(chest.getTrapperCaptureCooldown() - 1);
        }

        UUID targetId = chest.getTrapperCaptureTargetId();
        if (targetId != null) {
            if (!(level.getEntity(targetId) instanceof LivingEntity target)
                    || !canCapture(target)
                    || target.distanceToSqr(Vec3.atCenterOf(pos)) > ABORT_RADIUS * ABORT_RADIUS) {
                abortCapture(level, pos, chest, targetId == null ? null : level.getEntity(targetId));
                return;
            }
            // A target is leased to exactly one Trapper at a time. Refreshing the
            // short lease every tick prevents a second nearby chest from opening,
            // pulling, or fighting over the same mob. If this chest stops ticking
            // unexpectedly, the lease expires by itself instead of becoming stale.
            if (!maintainTargetClaim(level, pos, target)) {
                abortCapture(level, pos, chest, target);
                return;
            }
            pullTarget(level, pos, state, chest, target);
            return;
        }

        if (chest.getTrappedEntityCount() >= CAPACITY
                || chest.getTrapperCaptureCooldown() > 0
                || chest.getWorkTicker() % SCAN_INTERVAL_TICKS != 0) {
            return;
        }

        LivingEntity nearest = findAndClaimNearest(level, pos);
        if (nearest != null) {
            beginCapture(level, pos, chest, nearest);
        }
    }

    private static LivingEntity findAndClaimNearest(ServerLevel level, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        AABB box = new AABB(pos).inflate(CAPTURE_RADIUS);
        List<LivingEntity> candidates = level.getEntitiesOfClass(
                LivingEntity.class,
                box,
                TrapperLogic::canCapture
        );
        candidates.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(center)));
        for (LivingEntity candidate : candidates) {
            // Server block-entity ticks are ordered, so claiming here makes target
            // selection effectively atomic: the next Trapper that scans this tick
            // sees the lease and can move on to the next available creature.
            if (tryClaimTarget(level, pos, candidate)) return candidate;
        }
        return null;
    }

    public static boolean canCapture(LivingEntity entity) {
        if (!entity.isAlive() || entity.isRemoved() || isTrapperImmune(entity)) return false;
        if (entity instanceof Player
                || entity instanceof EnderDragon
                || entity instanceof WitherBoss
                || entity instanceof Warden) {
            return false;
        }
        return entity.getType() != EntityType.ENDER_DRAGON
                && entity.getType() != EntityType.WITHER
                && entity.getType() != EntityType.WARDEN;
    }

    public static boolean isTrapperImmune(LivingEntity entity) {
        return entity.getPersistentData().getLong(IMMUNITY_UNTIL_TAG) > entity.level().getGameTime();
    }

    public static void grantTrapperImmunity(LivingEntity entity, int ticks) {
        if (ticks <= 0) return;
        long until = entity.level().getGameTime() + ticks;
        long current = entity.getPersistentData().getLong(IMMUNITY_UNTIL_TAG);
        entity.getPersistentData().putLong(IMMUNITY_UNTIL_TAG, Math.max(current, until));
    }

    private static boolean tryClaimTarget(ServerLevel level, BlockPos pos, LivingEntity entity) {
        long now = level.getGameTime();
        var data = entity.getPersistentData();
        long claimUntil = data.getLong(CLAIM_UNTIL_TAG);
        if (claimUntil > now && !claimBelongsTo(level, pos, entity)) return false;

        data.putLong(CLAIM_POS_TAG, pos.asLong());
        data.putString(CLAIM_DIMENSION_TAG, level.dimension().location().toString());
        data.putLong(CLAIM_UNTIL_TAG, now + CLAIM_LEASE_TICKS);
        return true;
    }

    private static boolean maintainTargetClaim(ServerLevel level, BlockPos pos, LivingEntity entity) {
        long now = level.getGameTime();
        var data = entity.getPersistentData();
        long claimUntil = data.getLong(CLAIM_UNTIL_TAG);
        if (claimUntil <= now) {
            clearClaim(entity);
            return tryClaimTarget(level, pos, entity);
        }
        if (!claimBelongsTo(level, pos, entity)) return false;
        data.putLong(CLAIM_UNTIL_TAG, now + CLAIM_LEASE_TICKS);
        return true;
    }

    private static boolean claimBelongsTo(ServerLevel level, BlockPos pos, LivingEntity entity) {
        var data = entity.getPersistentData();
        return data.getLong(CLAIM_POS_TAG) == pos.asLong()
                && data.getString(CLAIM_DIMENSION_TAG).equals(level.dimension().location().toString());
    }

    private static void releaseClaimIfOwned(ServerLevel level, BlockPos pos, LivingEntity entity) {
        if (claimBelongsTo(level, pos, entity)) clearClaim(entity);
    }

    private static void clearClaim(LivingEntity entity) {
        var data = entity.getPersistentData();
        data.remove(CLAIM_POS_TAG);
        data.remove(CLAIM_DIMENSION_TAG);
        data.remove(CLAIM_UNTIL_TAG);
    }

    private static void beginCapture(ServerLevel level, BlockPos pos, SpecialChestBlockEntity chest, LivingEntity target) {
        AttributeInstance scale = target.getAttribute(Attributes.SCALE);
        double originalScale = scale == null ? 1.0D : scale.getBaseValue();
        // A mounted creature cannot be steered independently. Capture only the
        // selected creature; riders/passengers stay in the world.
        target.stopRiding();
        target.ejectPassengers();
        chest.beginTrapperCapture(target.getUUID(), originalScale, target.isInvulnerable());

        level.playSound(null, pos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.36F, 1.0F);
        level.playSound(null, pos, SoundEvents.VAULT_ACTIVATE, SoundSource.BLOCKS, 0.72F, 1.0F);
        level.playSound(null, pos, SoundEvents.VAULT_OPEN_SHUTTER, SoundSource.BLOCKS, 0.46F, 1.05F);
        Vec3 mouth = mouthPoint(pos, chest.getBlockState());
        level.sendParticles(
                ParticleTypes.VAULT_CONNECTION,
                mouth.x, mouth.y, mouth.z,
                6,
                0.30D, 0.22D, 0.30D,
                0.05D
        );
    }

    private static void pullTarget(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            SpecialChestBlockEntity chest,
            LivingEntity target
    ) {
        if (chest.isTrapperFinalSuction()) {
            finalSuction(level, pos, state, chest, target);
            return;
        }

        int ticks = chest.advanceTrapperCaptureTicks();
        float progress = Mth.clamp(ticks / (float) CAPTURE_TICKS, 0.0F, 1.0F);
        Vec3 mouth = mouthPoint(pos, state);
        Vec3 targetCenter = target.getBoundingBox().getCenter();
        Vec3 delta = mouth.subtract(targetCenter);
        double distance = delta.length();

        // Phase one is fully physical. The target is pulled toward the opening
        // above the block instead of toward a point buried in the collision.
        if (distance > 1.0E-4D) {
            double speed = Mth.lerp(progress, 0.11D, 0.34D);
            Vec3 desired = delta.normalize().scale(speed);
            Vec3 next = target.getDeltaMovement().scale(0.38D).add(desired.scale(0.74D));
            target.setDeltaMovement(next);
            target.hasImpulse = true;
            target.fallDistance = 0.0F;
        }

        // Keep the real hitbox at its original size during the physical approach.
        // Shrinking starts only after the target has reached the final suction phase.

        if ((ticks & 3) == 0) {
            level.sendParticles(
                    ParticleTypes.VAULT_CONNECTION,
                    target.getX(), target.getY() + target.getBbHeight() * 0.55D, target.getZ(),
                    2,
                    0.08D, 0.08D, 0.08D,
                    0.02D
            );
            level.sendParticles(
                    ParticleTypes.REVERSE_PORTAL,
                    mouth.x, mouth.y, mouth.z,
                    7,
                    0.45D, 0.35D, 0.45D,
                    0.07D
            );
        }
        if (ticks % 8 == 0) {
            level.playSound(null, pos, SoundEvents.VAULT_AMBIENT, SoundSource.BLOCKS, 0.23F, 1.08F);
        }

        if (distance <= MOUTH_ENTRY_DISTANCE && canBeginFinalSuction(level, pos, target, mouth)) {
            chest.beginTrapperFinalSuction();
            // The final visual pass intentionally moves the creature through the
            // Trapper block. Make that brief transition damage-proof so a full-cube
            // current/future model cannot deal suffocation damage or leave a
            // red hurt flash frozen into the captured preview.
            target.setInvulnerable(true);
            target.setDeltaMovement(Vec3.ZERO);
            target.fallDistance = 0.0F;
            level.playSound(null, pos, SoundEvents.VAULT_INSERT_ITEM, SoundSource.BLOCKS, 0.34F, 1.18F);
            level.sendParticles(
                    ParticleTypes.REVERSE_PORTAL,
                    mouth.x, mouth.y, mouth.z,
                    12,
                    0.24D, 0.24D, 0.24D,
                    0.08D
            );
        } else if (ticks >= CAPTURE_TIMEOUT_TICKS) {
            // If terrain prevents the mob from ever reaching the external mouth,
            // give up rather than teleporting it through walls from a distance.
            abortCapture(level, pos, chest, target);
        }
    }

    private static void finalSuction(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            SpecialChestBlockEntity chest,
            LivingEntity target
    ) {
        int ticks = chest.advanceTrapperFinalSuctionTicks();
        float progress = Mth.clamp(ticks / (float) FINAL_SUCTION_TICKS, 0.0F, 1.0F);
        Vec3 intake = intakePoint(pos, state);
        Vec3 currentCenter = target.getBoundingBox().getCenter();

        // This is intentionally position-controlled instead of collision-driven.
        // It only starts after the creature has physically reached the mouth, so it
        // cannot pull mobs through walls, but the last few pixels can pass through
        // the chest collision and visibly disappear inside.
        Vec3 nextCenter = currentCenter.lerp(intake, FINAL_SUCTION_LERP);
        double halfHeight = Math.max(0.02D, target.getBbHeight() * 0.5D);
        target.setPos(nextCenter.x, nextCenter.y - halfHeight, nextCenter.z);
        target.setDeltaMovement(Vec3.ZERO);
        target.hasImpulse = true;
        target.fallDistance = 0.0F;

        AttributeInstance scale = target.getAttribute(Attributes.SCALE);
        if (scale != null) {
            double scaleFactor = Mth.lerp(progress, FINAL_SUCTION_START_SCALE, FINAL_SUCTION_END_SCALE);
            scale.setBaseValue(Math.max(0.0625D, chest.getTrapperCaptureOriginalScale() * scaleFactor));
        }

        if ((ticks & 1) == 0) {
            level.sendParticles(
                    ParticleTypes.REVERSE_PORTAL,
                    currentCenter.x, currentCenter.y, currentCenter.z,
                    5,
                    0.10D, 0.10D, 0.10D,
                    0.08D
            );
        }

        if (ticks >= FINAL_SUCTION_TICKS || currentCenter.distanceToSqr(intake) <= 0.018D) {
            completeCapture(level, pos, chest, target, intake);
        }
    }

    private static void completeCapture(
            ServerLevel level,
            BlockPos pos,
            SpecialChestBlockEntity chest,
            LivingEntity target,
            Vec3 intake
    ) {
        // Restore transient capture-only state before serializing so the captured
        // NBT keeps the creature's real scale/invulnerability, not our suction effect.
        restoreCaptureState(chest, target);
        // The reservation is runtime coordination only; never bake it into the
        // stored entity NBT, otherwise a released creature could look claimed.
        releaseClaimIfOwned(level, pos, target);
        if (chest.captureTrapperEntity(target)) {
            target.discard();
            chest.finishTrapperCapture(POST_CAPTURE_COOLDOWN_TICKS);
            level.playSound(null, pos, SoundEvents.VAULT_INSERT_ITEM, SoundSource.BLOCKS, 0.85F, 0.92F);
            level.playSound(null, pos, SoundEvents.VAULT_CLOSE_SHUTTER, SoundSource.BLOCKS, 0.48F, 0.96F);
            level.playSound(null, pos, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.34F, 0.94F);
            level.sendParticles(
                    ParticleTypes.REVERSE_PORTAL,
                    intake.x, intake.y, intake.z,
                    28,
                    0.40D, 0.35D, 0.40D,
                    0.12D
            );
        } else {
            chest.finishTrapperCapture(10);
        }
    }

    public static void cancelCapture(ServerLevel level, SpecialChestBlockEntity chest) {
        UUID targetId = chest.getTrapperCaptureTargetId();
        if (targetId == null) return;
        net.minecraft.world.entity.Entity entity = level.getEntity(targetId);
        abortCapture(level, chest.getBlockPos(), chest, entity);
    }

    private static void abortCapture(
            ServerLevel level,
            BlockPos pos,
            SpecialChestBlockEntity chest,
            net.minecraft.world.entity.Entity entity
    ) {
        if (entity instanceof LivingEntity living) {
            restoreCaptureState(chest, living);
            releaseClaimIfOwned(level, pos, living);
            grantTrapperImmunity(living, ABORT_IMMUNITY_TICKS);
        }
        chest.finishTrapperCapture(8);
        level.playSound(null, pos, SoundEvents.VAULT_CLOSE_SHUTTER, SoundSource.BLOCKS, 0.28F, 0.90F);
        level.playSound(null, pos, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.20F, 0.90F);
    }

    private static void restoreCaptureState(SpecialChestBlockEntity chest, LivingEntity target) {
        AttributeInstance scale = target.getAttribute(Attributes.SCALE);
        if (scale != null) {
            scale.setBaseValue(chest.getTrapperCaptureOriginalScale());
        }
        target.setInvulnerable(chest.wasTrapperCaptureOriginallyInvulnerable());
    }

    private static boolean canBeginFinalSuction(
            ServerLevel level,
            BlockPos chestPos,
            LivingEntity target,
            Vec3 mouth
    ) {
        Vec3 from = target.getBoundingBox().getCenter();
        BlockHitResult hit = level.clip(new ClipContext(
                from,
                mouth,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                target
        ));
        // A clear route is fine. If the first collision is the Trapper itself,
        // that is also fine: the controlled final phase exists specifically to
        // pass through the chest's own temporary/full-cube collision.
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(chestPos);
    }

    /** Reachable visual mouth just above the Trapper chest. */
    public static Vec3 mouthPoint(BlockPos pos, BlockState state) {
        return new Vec3(
                pos.getX() + 0.5D,
                pos.getY() + MOUTH_HEIGHT,
                pos.getZ() + 0.5D
        );
    }

    /** Final visual destination inside the chest. */
    public static Vec3 intakePoint(BlockPos pos, BlockState state) {
        return new Vec3(
                pos.getX() + 0.5D,
                pos.getY() + INTAKE_HEIGHT,
                pos.getZ() + 0.5D
        );
    }
}
