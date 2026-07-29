package com.overyourhead.curiouschests.common.logic;

import com.overyourhead.curiouschests.common.chest.ChestRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class CollectorLogic {
    public static final int RADIUS = 8;
    public static final int TICK_INTERVAL = 1;

    /*
     * Horizontal movement uses capped acceleration rather than replacing the
     * velocity with a new direction every tick. This keeps the travel speed
     * from the previous build while removing most small direction corrections.
     */
    private static final double MIN_CRUISE_SPEED = 0.035;
    private static final double MAX_CRUISE_SPEED = 0.085;
    private static final double GROUND_ACCELERATION = 0.0065;
    private static final double AIR_ACCELERATION = 0.0045;

    private static final double FINAL_APPROACH_RADIUS = 1.40;
    private static final double FINAL_APPROACH_SPEED = 0.105;
    private static final double FINAL_ACCELERATION = 0.011;
    private static final double FINAL_JUMP_VELOCITY = 0.425;

    private static final double STEP_PROBE_DISTANCE = 0.46;
    private static final double STEP_HEIGHT = 1.05;
    private static final double STEP_JUMP_VELOCITY = 0.425;

    private static final double ROUTE_SAMPLE_SPACING = 0.25;
    private static final double ROUTE_TARGET_MARGIN = 0.78;

    private CollectorLogic() {}

    public static boolean tick(ServerLevel level, BlockPos origin, Container target) {
        Vec3 destination = new Vec3(
                origin.getX() + 0.5,
                origin.getY() + 1.10,
                origin.getZ() + 0.5
        );
        AABB intake = new AABB(
                origin.getX() + 0.08,
                origin.getY() + 0.88,
                origin.getZ() + 0.08,
                origin.getX() + 0.92,
                origin.getY() + 1.48,
                origin.getZ() + 0.92
        );
        AABB area = new AABB(origin).inflate(RADIUS);
        boolean changed = false;

        for (ItemEntity itemEntity : level.getEntitiesOfClass(
                ItemEntity.class,
                area,
                ItemEntity::isAlive
        )) {
            ItemStack stack = itemEntity.getItem();
            if (!ChestRules.canStore(stack) || !InventoryTransfer.canInsertAny(target, stack)) continue;

            Vec3 itemCenter = itemEntity.position().add(0.0, itemEntity.getBbHeight() * 0.5, 0.0);
            Vec3 offset = destination.subtract(itemCenter);
            if (offset.lengthSqr() > RADIUS * RADIUS) continue;

            /*
             * The sampled corridor accepts a free direct route or a route that
             * can be traversed by stepping up one block. A two-block wall still
             * blocks collection, while slabs and partial collision shapes are
             * handled by their real collision boxes.
             */
            if (!hasNavigableCorridor(level, itemEntity, destination)) continue;

            if (itemEntity.getBoundingBox().intersects(intake)) {
                ItemStack moving = stack.copy();
                int moved = InventoryTransfer.insert(target, moving, false);
                if (moved <= 0) continue;

                changed = true;
                if (moving.isEmpty()) {
                    itemEntity.discard();
                } else {
                    itemEntity.setItem(moving);
                    itemEntity.setDeltaMovement(Vec3.ZERO);
                    itemEntity.hasImpulse = true;
                }

                level.sendParticles(
                        ParticleTypes.HAPPY_VILLAGER,
                        destination.x, destination.y, destination.z,
                        5, 0.18, 0.10, 0.18, 0.0
                );
                level.playSound(
                        null,
                        origin,
                        SoundEvents.ITEM_PICKUP,
                        SoundSource.BLOCKS,
                        0.3F,
                        0.8F + level.random.nextFloat() * 0.25F
                );
                continue;
            }

            steerItem(level, origin, itemEntity, itemCenter, offset);

            if (level.getGameTime() % 10L == 0L) {
                level.sendParticles(
                        ParticleTypes.HAPPY_VILLAGER,
                        itemEntity.getX(), itemEntity.getY() + 0.08, itemEntity.getZ(),
                        1, 0.01, 0.01, 0.01, 0.0
                );
            }
        }

        return changed;
    }

    private static void steerItem(
            ServerLevel level,
            BlockPos origin,
            ItemEntity itemEntity,
            Vec3 itemCenter,
            Vec3 offset
    ) {
        double horizontalDistance = Math.sqrt(offset.x * offset.x + offset.z * offset.z);
        Vec3 current = itemEntity.getDeltaMovement();
        Vec3 horizontalOffset = new Vec3(offset.x, 0.0, offset.z);

        if (horizontalOffset.lengthSqr() < 1.0E-7) {
            return;
        }

        Vec3 horizontalDirection = horizontalOffset.normalize();
        boolean finalApproach = horizontalDistance <= FINAL_APPROACH_RADIUS;

        double desiredSpeed;
        double acceleration;
        if (finalApproach) {
            desiredSpeed = FINAL_APPROACH_SPEED;
            acceleration = FINAL_ACCELERATION;
        } else {
            desiredSpeed = Math.min(
                    MAX_CRUISE_SPEED,
                    MIN_CRUISE_SPEED + horizontalDistance * 0.006
            );
            acceleration = itemEntity.onGround()
                    ? GROUND_ACCELERATION
                    : AIR_ACCELERATION;
        }

        Vec3 desiredHorizontal = horizontalDirection.scale(desiredSpeed);
        double nextX = approach(current.x, desiredHorizontal.x, acceleration);
        double nextZ = approach(current.z, desiredHorizontal.z, acceleration);
        double nextY = current.y;
        boolean jumped = false;

        if (itemEntity.onGround()) {
            /*
             * Do not add a small positive Y value during ordinary travel. The
             * item stays on the surface and only receives a real jump when a
             * one-block step or the collector lid is directly ahead.
             */
            nextY = Math.min(current.y, 0.0);

            boolean belowLid = itemCenter.y < origin.getY() + 0.98;
            if (finalApproach && belowLid) {
                nextY = FINAL_JUMP_VELOCITY;
                jumped = true;
            } else if (needsStepUp(level, itemEntity, horizontalDirection)) {
                nextY = STEP_JUMP_VELOCITY;
                jumped = true;
            }
        }

        Vec3 next = new Vec3(nextX, nextY, nextZ);
        applyVelocity(itemEntity, current, next, jumped);
    }

    private static double approach(double current, double target, double maximumChange) {
        return current + Mth.clamp(target - current, -maximumChange, maximumChange);
    }

    private static boolean needsStepUp(
            ServerLevel level,
            ItemEntity itemEntity,
            Vec3 horizontalDirection
    ) {
        Vec3 probe = horizontalDirection.scale(STEP_PROBE_DISTANCE);
        AABB box = itemEntity.getBoundingBox();

        boolean blockedAhead = !level.noCollision(
                itemEntity,
                box.move(probe.x, 0.0, probe.z)
        );
        if (!blockedAhead) return false;

        boolean canRise = level.noCollision(
                itemEntity,
                box.move(0.0, STEP_HEIGHT, 0.0)
        );
        boolean clearAboveObstacle = level.noCollision(
                itemEntity,
                box.move(probe.x, STEP_HEIGHT, probe.z)
        );
        return canRise && clearAboveObstacle;
    }

    private static void applyVelocity(
            ItemEntity itemEntity,
            Vec3 previous,
            Vec3 next,
            boolean forceSync
    ) {
        itemEntity.setDeltaMovement(next);

        /*
         * Sending a velocity packet every tick can create visible micro
         * corrections. Normal gliding is synchronized in small batches, while
         * jumps are sent immediately so the client starts the arc at once.
         */
        double changeSqr = next.subtract(previous).lengthSqr();
        if (forceSync || itemEntity.tickCount % 4 == 0 || changeSqr > 0.0016) {
            itemEntity.hasImpulse = true;
        }
    }

    private static boolean hasNavigableCorridor(
            ServerLevel level,
            ItemEntity itemEntity,
            Vec3 destination
    ) {
        Vec3 start = itemEntity.position();
        Vec3 horizontal = new Vec3(
                destination.x - start.x,
                0.0,
                destination.z - start.z
        );
        double horizontalDistance = horizontal.length();
        if (horizontalDistance <= ROUTE_TARGET_MARGIN) return true;

        Vec3 direction = horizontal.scale(1.0 / horizontalDistance);
        AABB originalBox = itemEntity.getBoundingBox();
        double checkedDistance = horizontalDistance - ROUTE_TARGET_MARGIN;

        for (double travelled = ROUTE_SAMPLE_SPACING;
             travelled <= checkedDistance;
             travelled += ROUTE_SAMPLE_SPACING) {
            double moveX = direction.x * travelled;
            double moveZ = direction.z * travelled;
            AABB lowBox = originalBox.move(moveX, 0.0, moveZ);

            if (level.noCollision(itemEntity, lowBox)) {
                continue;
            }

            AABB steppedBox = lowBox.move(0.0, STEP_HEIGHT, 0.0);
            if (!level.noCollision(itemEntity, steppedBox)) {
                return false;
            }
        }

        return true;
    }
}
