package com.overyourhead.curiouschests.common.chest.trapper;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/** Target eligibility, line-of-sight checks, immunity and lease ownership. */
final class TrapperTargeting {
    private static final String IMMUNITY_UNTIL_TAG = "CuriousChestsTrapperImmuneUntil";
    private static final String CLAIM_POS_TAG = "CuriousChestsTrapperClaimPos";
    private static final String CLAIM_DIMENSION_TAG = "CuriousChestsTrapperClaimDimension";
    private static final String CLAIM_UNTIL_TAG = "CuriousChestsTrapperClaimUntil";
    private static final int CLAIM_LEASE_TICKS = 40;

    private TrapperTargeting() {}

    static LivingEntity findAndClaimNearest(ServerLevel level, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        AABB box = new AABB(pos).inflate(TrapperLogic.CAPTURE_RADIUS);
        List<LivingEntity> candidates = level.getEntitiesOfClass(
                LivingEntity.class,
                box,
                TrapperTargeting::canCapture
        );
        candidates.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(center)));
        for (LivingEntity candidate : candidates) {
            // Do not even open / reserve a target through solid terrain. The physical
            // suction moves in a straight line, so a creature hidden behind a wall
            // would only get pinned against the obstacle and time out repeatedly.
            if (!hasCaptureLineOfSight(level, pos, candidate)) continue;

            // Server block-entity ticks are ordered, so claiming here makes target
            // selection effectively atomic: the next Trapper that scans this tick
            // sees the lease and can move on to the next available creature.
            if (tryClaimTarget(level, pos, candidate)) return candidate;
        }
        return null;
    }

    static boolean canCapture(LivingEntity entity) {
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

    static boolean isTrapperImmune(LivingEntity entity) {
        return entity.getPersistentData().getLong(IMMUNITY_UNTIL_TAG) > entity.level().getGameTime();
    }

    static void grantTrapperImmunity(LivingEntity entity, int ticks) {
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

    static boolean maintainTargetClaim(ServerLevel level, BlockPos pos, LivingEntity entity) {
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

    static void releaseClaimIfOwned(ServerLevel level, BlockPos pos, LivingEntity entity) {
        if (claimBelongsTo(level, pos, entity)) clearClaim(entity);
    }

    private static void clearClaim(LivingEntity entity) {
        var data = entity.getPersistentData();
        data.remove(CLAIM_POS_TAG);
        data.remove(CLAIM_DIMENSION_TAG);
        data.remove(CLAIM_UNTIL_TAG);
    }

    static boolean hasCaptureLineOfSight(ServerLevel level, BlockPos chestPos, LivingEntity target) {
        Vec3 mouth = TrapperCapture.mouthPoint(chestPos, level.getBlockState(chestPos));
        AABB box = target.getBoundingBox();
        Vec3 center = box.getCenter();
        Vec3 upperBody = new Vec3(target.getX(), box.minY + target.getBbHeight() * 0.82D, target.getZ());
        Vec3 eyes = target.getEyePosition();

        return isClearCaptureRay(level, chestPos, target, eyes, mouth)
                || isClearCaptureRay(level, chestPos, target, upperBody, mouth)
                || isClearCaptureRay(level, chestPos, target, center, mouth);
    }

    private static boolean isClearCaptureRay(
            ServerLevel level,
            BlockPos chestPos,
            LivingEntity target,
            Vec3 from,
            Vec3 to
    ) {
        BlockHitResult hit = level.clip(new ClipContext(
                from,
                to,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                target
        ));
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(chestPos);
    }

}
