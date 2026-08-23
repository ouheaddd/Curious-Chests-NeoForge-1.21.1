package com.overyourhead.curiouschests.common.chest.trapper;

import com.overyourhead.curiouschests.common.blockentity.SpecialChestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** Public Trapper facade and server-tick coordinator. */
public final class TrapperLogic {
    public static final int CAPACITY = 9;
    public static final double CAPTURE_RADIUS = 5.0D;
    public static final int CAPTURE_TICKS = 26;
    public static final int CAPTURE_TIMEOUT_TICKS = 80;
    public static final int POST_CAPTURE_COOLDOWN_TICKS = 18;
    public static final int SCAN_INTERVAL_TICKS = 4;
    public static final int RELEASE_IMMUNITY_TICKS = 200;
    public static final int ABORT_IMMUNITY_TICKS = 40;
    private static final double ABORT_RADIUS = 7.0D;

    private TrapperLogic() {}

    public static void tick(ServerLevel level, BlockPos pos, BlockState state, SpecialChestBlockEntity chest) {
        chest.clearTrapperPackBreakIntent();
        if (chest.getTrapperCaptureCooldown() > 0) {
            chest.setTrapperCaptureCooldown(chest.getTrapperCaptureCooldown() - 1);
        }

        UUID targetId = chest.getTrapperCaptureTargetId();
        if (targetId != null) {
            if (!(level.getEntity(targetId) instanceof LivingEntity target)
                    || !TrapperTargeting.canCapture(target)
                    || target.distanceToSqr(Vec3.atCenterOf(pos)) > ABORT_RADIUS * ABORT_RADIUS
                    || !TrapperTargeting.hasCaptureLineOfSight(level, pos, target)) {
                TrapperCapture.abort(level, pos, chest, level.getEntity(targetId));
                return;
            }
            if (!TrapperTargeting.maintainTargetClaim(level, pos, target)) {
                TrapperCapture.abort(level, pos, chest, target);
                return;
            }
            TrapperCapture.pull(level, pos, state, chest, target);
            return;
        }

        if (chest.getTrappedEntityCount() >= CAPACITY
                || chest.getTrapperCaptureCooldown() > 0
                || chest.getWorkTicker() % SCAN_INTERVAL_TICKS != 0) {
            return;
        }

        LivingEntity nearest = TrapperTargeting.findAndClaimNearest(level, pos);
        if (nearest != null) TrapperCapture.begin(level, pos, chest, nearest);
    }

    public static boolean canCapture(LivingEntity entity) {
        return TrapperTargeting.canCapture(entity);
    }

    public static boolean isTrapperImmune(LivingEntity entity) {
        return TrapperTargeting.isTrapperImmune(entity);
    }

    public static void grantTrapperImmunity(LivingEntity entity, int ticks) {
        TrapperTargeting.grantTrapperImmunity(entity, ticks);
    }

    public static void cancelCapture(ServerLevel level, SpecialChestBlockEntity chest) {
        UUID targetId = chest.getTrapperCaptureTargetId();
        if (targetId == null) return;
        TrapperCapture.abort(level, chest.getBlockPos(), chest, level.getEntity(targetId));
    }

    public static Vec3 mouthPoint(BlockPos pos, BlockState state) {
        return TrapperCapture.mouthPoint(pos, state);
    }

    public static Vec3 intakePoint(BlockPos pos, BlockState state) {
        return TrapperCapture.intakePoint(pos, state);
    }
}
