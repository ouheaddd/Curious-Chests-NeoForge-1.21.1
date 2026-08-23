package com.overyourhead.curiouschests.common.chest.trapper;

import com.overyourhead.curiouschests.common.blockentity.SpecialChestBlockEntity;
import com.overyourhead.curiouschests.core.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Physical pull, final suction, capture completion and cancellation. */
final class TrapperCapture {
    private static final double MOUTH_HEIGHT = 1.18D;
    private static final double MOUTH_ENTRY_DISTANCE = 1.35D;
    private static final double LOWER_TARGET_LIFT_GAP = 0.48D;
    private static final double LOWER_TARGET_LIFT_MIN_SPEED = 0.30D;
    private static final double LOWER_TARGET_LIFT_MAX_SPEED = 0.42D;
    private static final double INTAKE_HEIGHT = 0.70D;
    private static final int FINAL_SUCTION_TICKS = 10;
    private static final double FINAL_SUCTION_START_SCALE = 1.00D;
    private static final double FINAL_SUCTION_END_SCALE = 0.08D;
    private static final double FINAL_SUCTION_LERP = 0.36D;

    private TrapperCapture() {}

    static void begin(ServerLevel level, BlockPos pos, SpecialChestBlockEntity chest, LivingEntity target) {
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
        // VAULT_CONNECTION expects a relative source offset. count=0 preserves
        // the supplied vector instead of randomizing it like a normal velocity particle.
        for (int i = 0; i < 6; i++) {
            double ox = (level.random.nextDouble() - 0.5D) * 0.60D;
            double oy = (level.random.nextDouble() - 0.5D) * 0.44D;
            double oz = (level.random.nextDouble() - 0.5D) * 0.60D;
            level.sendParticles(ModParticles.TRAPPER_LINK.get(), mouth.x, mouth.y, mouth.z, 0, ox, oy, oz, 1.0D);
        }
    }

    static void pull(
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
        float progress = Mth.clamp(ticks / (float) TrapperLogic.CAPTURE_TICKS, 0.0F, 1.0F);
        Vec3 mouth = mouthPoint(pos, state);
        Vec3 targetCenter = target.getBoundingBox().getCenter();
        Vec3 delta = mouth.subtract(targetCenter);
        double distance = delta.length();

        // Phase one is fully physical. If the creature is noticeably below the
        // mouth (for example one block down in a pit), first lift it vertically.
        // A direct diagonal pull can otherwise press the hitbox into the ledge and
        // leave it hovering there until the capture timeout. Once the creature is
        // high enough, the normal pull toward the mouth takes over.
        if (distance > 1.0E-4D) {
            double verticalGap = mouth.y - targetCenter.y;
            Vec3 currentMotion = target.getDeltaMovement();
            Vec3 next;

            if (verticalGap > LOWER_TARGET_LIFT_GAP) {
                double liftSpeed = Mth.lerp(
                        progress,
                        LOWER_TARGET_LIFT_MIN_SPEED,
                        LOWER_TARGET_LIFT_MAX_SPEED
                );
                // Damp horizontal movement while climbing so the creature clears
                // the wall/ledge instead of being dragged into it.
                next = new Vec3(
                        currentMotion.x * 0.28D,
                        Math.max(currentMotion.y * 0.35D + liftSpeed, liftSpeed),
                        currentMotion.z * 0.28D
                );
            } else {
                double speed = Mth.lerp(progress, 0.11D, 0.34D);
                Vec3 desired = delta.normalize().scale(speed);
                next = currentMotion.scale(0.38D).add(desired.scale(0.74D));
            }

            target.setDeltaMovement(next);
            target.hasImpulse = true;
            target.fallDistance = 0.0F;
        }

        // Keep the real hitbox at its original size during the physical approach.
        // Shrinking starts only after the target has reached the final suction phase.

        if ((ticks & 3) == 0) {
            Vec3 particleSource = new Vec3(
                    target.getX(),
                    target.getY() + target.getBbHeight() * 0.55D,
                    target.getZ()
            );
            Vec3 particleOffset = particleSource.subtract(mouth);
            for (int i = 0; i < 2; i++) {
                level.sendParticles(
                        ModParticles.TRAPPER_LINK.get(),
                        mouth.x, mouth.y, mouth.z,
                        0,
                        particleOffset.x, particleOffset.y, particleOffset.z,
                        1.0D
                );
            }
            level.sendParticles(
                    ModParticles.TRAPPER_ORBIT.get(),
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
                    ModParticles.TRAPPER_ORBIT.get(),
                    mouth.x, mouth.y, mouth.z,
                    12,
                    0.24D, 0.24D, 0.24D,
                    0.08D
            );
        } else if (ticks >= TrapperLogic.CAPTURE_TIMEOUT_TICKS) {
            // If terrain prevents the mob from ever reaching the external mouth,
            // give up rather than teleporting it through walls from a distance.
            abort(level, pos, chest, target);
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
                    ModParticles.TRAPPER_ORBIT.get(),
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
        TrapperTargeting.releaseClaimIfOwned(level, pos, target);
        if (chest.captureTrapperEntity(target)) {
            target.discard();
            chest.finishTrapperCapture(TrapperLogic.POST_CAPTURE_COOLDOWN_TICKS);
            level.playSound(null, pos, SoundEvents.VAULT_INSERT_ITEM, SoundSource.BLOCKS, 0.85F, 0.92F);
            level.playSound(null, pos, SoundEvents.VAULT_CLOSE_SHUTTER, SoundSource.BLOCKS, 0.48F, 0.96F);
            level.playSound(null, pos, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.34F, 0.94F);
            level.sendParticles(
                    ModParticles.TRAPPER_ORBIT.get(),
                    intake.x, intake.y, intake.z,
                    28,
                    0.40D, 0.35D, 0.40D,
                    0.12D
            );
        } else {
            chest.finishTrapperCapture(10);
        }
    }

    static void abort(
            ServerLevel level,
            BlockPos pos,
            SpecialChestBlockEntity chest,
            net.minecraft.world.entity.Entity entity
    ) {
        if (entity instanceof LivingEntity living) {
            restoreCaptureState(chest, living);
            TrapperTargeting.releaseClaimIfOwned(level, pos, living);
            TrapperTargeting.grantTrapperImmunity(living, TrapperLogic.ABORT_IMMUNITY_TICKS);
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

    /**
     * The Trapper only starts (and keeps) a capture while the creature has a
     * direct visual/physical route to the mouth. Several heights are sampled so
     * a mob standing one block lower can still be lifted over a ledge when its
     * head is visible, while mobs fully hidden behind a wall are ignored.
     */
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
    static Vec3 mouthPoint(BlockPos pos, BlockState state) {
        return new Vec3(
                pos.getX() + 0.5D,
                pos.getY() + MOUTH_HEIGHT,
                pos.getZ() + 0.5D
        );
    }

    /** Final visual destination inside the chest. */
    static Vec3 intakePoint(BlockPos pos, BlockState state) {
        return new Vec3(
                pos.getX() + 0.5D,
                pos.getY() + INTAKE_HEIGHT,
                pos.getZ() + 0.5D
        );
    }}
