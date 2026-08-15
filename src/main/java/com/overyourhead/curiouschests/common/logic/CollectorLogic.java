package com.overyourhead.curiouschests.common.logic;

import com.overyourhead.curiouschests.common.chest.ChestRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CollectorLogic {
    public static final int RADIUS = 8;
    public static final int TICK_INTERVAL = 1;
    private static final int OUTPUT_INTERVAL_TICKS = 8;

    /*
     * Keep the same target speeds as v19, but steer toward them with a small
     * per-tick lerp. This makes horizontal motion feel less step-like while
     * preserving the physical hop/fall behavior.
     */
    private static final double MIN_CRUISE_SPEED = 0.080;
    private static final double MAX_CRUISE_SPEED = 0.180;
    private static final double CRUISE_STEERING = 0.24;

    private static final double FINAL_APPROACH_RADIUS = 1.40;
    private static final double FINAL_APPROACH_SPEED = 0.220;
    private static final double FINAL_STEERING = 0.32;
    // Start the lid hop only when the item is close enough that the 0.425 Y arc
    // reaches the collector intake near the horizontal center instead of sailing
    // over it while still too high.
    private static final double FINAL_JUMP_RADIUS = 0.82;
    private static final double FINAL_JUMP_VELOCITY = 0.425;

    private static final double STEP_PROBE_DISTANCE = 0.46;
    private static final double STEP_HEIGHT = 1.05;
    private static final double STEP_JUMP_VELOCITY = 0.425;

    private static final double ROUTE_SAMPLE_SPACING = 0.25;
    private static final double ROUTE_TARGET_MARGIN = 0.78;

    /*
     * A dropped item may sit inside the radius of multiple Collector Chests.
     * Without ownership, every chest writes a different velocity into the same
     * ItemEntity during the same server tick, which makes equidistant items
     * stall or visibly twitch. Keep a short-lived claim so only one collector
     * steers an item at a time.
     */
    private static final int CLAIM_TIMEOUT_TICKS = 6;
    private static final int CLAIM_CLEANUP_AGE_TICKS = 40;
    private static final double CLAIM_TAKEOVER_MARGIN = 0.50;
    private static final double CLAIM_TIE_EPSILON = 0.05;
    private static final Map<UUID, CollectorClaim> CLAIMS = new HashMap<>();
    private static long lastClaimCleanupTick = Long.MIN_VALUE;

    private CollectorLogic() {}

    public static boolean tick(ServerLevel level, BlockPos origin, Container target) {
        cleanupExpiredClaims(level.getGameTime());

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

        // Hopper-style lower output: Collector never extracts from another inventory.
        // It only pushes one item from its own storage into the block directly below,
        // once every eight ticks. Redstone power pauses this output without disabling
        // the Collector's normal attraction mechanic.
        if (level.getGameTime() % OUTPUT_INTERVAL_TICKS == 0L
                && !level.hasNeighborSignal(origin)
                && pushOneDown(level, origin, target)) {
            changed = true;
        }

        for (ItemEntity itemEntity : level.getEntitiesOfClass(
                ItemEntity.class,
                area,
                ItemEntity::isAlive
        )) {
            ItemStack stack = itemEntity.getItem();
            if (!ChestRules.canStore(stack) || !InventoryTransfer.canInsertAny(target, stack)) {
                releaseClaimIfOwned(level, origin, itemEntity);
                continue;
            }

            Vec3 itemCenter = itemEntity.position().add(0.0, itemEntity.getBbHeight() * 0.5, 0.0);
            Vec3 offset = destination.subtract(itemCenter);
            if (offset.lengthSqr() > RADIUS * RADIUS) {
                releaseClaimIfOwned(level, origin, itemEntity);
                continue;
            }

            /*
             * The sampled corridor accepts a free direct route or a route that
             * can be traversed by stepping up one block. A two-block wall still
             * blocks collection, while slabs and partial collision shapes are
             * handled by their real collision boxes.
             */
            if (!hasNavigableCorridor(level, itemEntity, destination)) {
                releaseClaimIfOwned(level, origin, itemEntity);
                continue;
            }

            if (!claimForCollector(level, origin, itemEntity, itemCenter, destination)) continue;

            if (itemEntity.getBoundingBox().intersects(intake)) {
                ItemStack moving = stack.copy();
                int moved = InventoryTransfer.insert(target, moving, false);
                if (moved <= 0) continue;

                changed = true;
                CLAIMS.remove(itemEntity.getUUID());
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

    private static boolean pushOneDown(ServerLevel level, BlockPos origin, Container source) {
        BlockPos below = origin.below();
        BlockState belowState = level.getBlockState(below);
        BlockEntity belowEntity = level.getBlockEntity(below);

        // Insert from the target's top face, exactly as a downward-facing hopper would.
        // Prefer the sided capability so modded inventories can enforce their own rules;
        // fall back to an unsided handler only when the block exposes no top-side view.
        IItemHandler target = level.getCapability(
                Capabilities.ItemHandler.BLOCK,
                below,
                belowState,
                belowEntity,
                Direction.UP
        );
        if (target == null) {
            target = level.getCapability(
                    Capabilities.ItemHandler.BLOCK,
                    below,
                    belowState,
                    belowEntity,
                    null
            );
        }
        if (target == null) return false;

        for (int sourceSlot = 0; sourceSlot < source.getContainerSize(); sourceSlot++) {
            ItemStack stored = source.getItem(sourceSlot);
            if (stored.isEmpty()) continue;

            ItemStack moving = stored.copyWithCount(1);
            if (!insertOne(target, moving)) continue;

            stored.shrink(1);
            if (stored.isEmpty()) {
                source.setItem(sourceSlot, ItemStack.EMPTY);
            } else {
                source.setItem(sourceSlot, stored);
            }
            return true;
        }
        return false;
    }

    private static boolean insertOne(IItemHandler target, ItemStack moving) {
        // Top off an existing matching stack first. This preserves Compression's
        // extended stacks (e.g. 69 -> 70) instead of opening a fresh slot.
        for (int slot = 0; slot < target.getSlots() && !moving.isEmpty(); slot++) {
            ItemStack present = target.getStackInSlot(slot);
            if (present.isEmpty() || !ItemStack.isSameItemSameComponents(present, moving)) continue;
            moving = target.insertItem(slot, moving, false);
        }

        for (int slot = 0; slot < target.getSlots() && !moving.isEmpty(); slot++) {
            ItemStack present = target.getStackInSlot(slot);
            if (!present.isEmpty() && ItemStack.isSameItemSameComponents(present, moving)) continue;
            moving = target.insertItem(slot, moving, false);
        }
        return moving.isEmpty();
    }

    private static boolean claimForCollector(
            ServerLevel level,
            BlockPos origin,
            ItemEntity itemEntity,
            Vec3 itemCenter,
            Vec3 destination
    ) {
        UUID itemId = itemEntity.getUUID();
        ResourceKey<Level> dimension = level.dimension();
        long now = level.getGameTime();
        CollectorClaim current = CLAIMS.get(itemId);

        if (current == null
                || !current.dimension.equals(dimension)
                || now - current.lastRefreshTick > CLAIM_TIMEOUT_TICKS) {
            CLAIMS.put(itemId, new CollectorClaim(dimension, origin.immutable(), now));
            return true;
        }

        if (current.owner.equals(origin)) {
            current.lastRefreshTick = now;
            return true;
        }

        Vec3 ownerDestination = new Vec3(
                current.owner.getX() + 0.5,
                current.owner.getY() + 1.10,
                current.owner.getZ() + 0.5
        );
        double candidateDistance = itemCenter.distanceTo(destination);
        double ownerDistance = itemCenter.distanceTo(ownerDestination);

        boolean clearlyCloser = candidateDistance + CLAIM_TAKEOVER_MARGIN < ownerDistance;
        boolean tied = Math.abs(candidateDistance - ownerDistance) <= CLAIM_TIE_EPSILON;
        boolean winsTie = tied && compareCollectorPositions(origin, current.owner) < 0;

        if (clearlyCloser || winsTie) {
            CLAIMS.put(itemId, new CollectorClaim(dimension, origin.immutable(), now));
            return true;
        }

        return false;
    }

    private static void releaseClaimIfOwned(
            ServerLevel level,
            BlockPos origin,
            ItemEntity itemEntity
    ) {
        CollectorClaim current = CLAIMS.get(itemEntity.getUUID());
        if (current != null
                && current.dimension.equals(level.dimension())
                && current.owner.equals(origin)) {
            CLAIMS.remove(itemEntity.getUUID());
        }
    }

    private static int compareCollectorPositions(BlockPos first, BlockPos second) {
        int x = Integer.compare(first.getX(), second.getX());
        if (x != 0) return x;
        int y = Integer.compare(first.getY(), second.getY());
        if (y != 0) return y;
        return Integer.compare(first.getZ(), second.getZ());
    }

    private static void cleanupExpiredClaims(long now) {
        if (lastClaimCleanupTick != Long.MIN_VALUE && now < lastClaimCleanupTick) {
            // Handles switching/reloading worlds in the same client/server JVM,
            // where the new world's game time may start below the old one.
            CLAIMS.clear();
            lastClaimCleanupTick = now;
            return;
        }
        if (lastClaimCleanupTick != Long.MIN_VALUE && now - lastClaimCleanupTick < 20L) return;
        lastClaimCleanupTick = now;
        CLAIMS.entrySet().removeIf(entry -> now - entry.getValue().lastRefreshTick > CLAIM_CLEANUP_AGE_TICKS);
    }

    private static final class CollectorClaim {
        private final ResourceKey<Level> dimension;
        private final BlockPos owner;
        private long lastRefreshTick;

        private CollectorClaim(ResourceKey<Level> dimension, BlockPos owner, long lastRefreshTick) {
            this.dimension = dimension;
            this.owner = owner;
            this.lastRefreshTick = lastRefreshTick;
        }
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
            // Once the item reaches the exact horizontal center, kill only the
            // X/Z drift. Keep its Y velocity so the existing hop/fall arc stays
            // intact and the item drops straight into the intake.
            itemEntity.setDeltaMovement(0.0, current.y, 0.0);
            itemEntity.hasImpulse = true;
            return;
        }

        Vec3 horizontalDirection = horizontalOffset.normalize();
        boolean finalApproach = horizontalDistance <= FINAL_APPROACH_RADIUS;

        double desiredSpeed;
        double steering;
        if (finalApproach) {
            desiredSpeed = FINAL_APPROACH_SPEED;
            steering = FINAL_STEERING;
        } else {
            desiredSpeed = Math.min(
                    MAX_CRUISE_SPEED,
                    MIN_CRUISE_SPEED + horizontalDistance * 0.006
            );
            steering = CRUISE_STEERING;
        }

        Vec3 desiredHorizontal = horizontalDirection.scale(desiredSpeed);
        double nextX = Mth.lerp(steering, current.x, desiredHorizontal.x);
        double nextZ = Mth.lerp(steering, current.z, desiredHorizontal.z);

        if (finalApproach) {
            /*
             * Never let the accelerated final approach step travel farther than
             * the remaining horizontal distance to the collector center. This
             * removes the fast-build overshoot without slowing normal travel.
             */
            double nextHorizontalSpeed = Math.sqrt(nextX * nextX + nextZ * nextZ);
            if (nextHorizontalSpeed > horizontalDistance) {
                nextX = horizontalDirection.x * horizontalDistance;
                nextZ = horizontalDirection.z * horizontalDistance;
            }
        }

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
            if (finalApproach && horizontalDistance <= FINAL_JUMP_RADIUS && belowLid) {
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
         * Collector steering is intentionally synchronized every tick. The
         * velocity itself is still changed gradually by the steering lerp,
         * so this removes the old four-tick visual stepping without turning
         * the item into a hard-snapping magnet. Jumps keep the same physics.
         */
        itemEntity.hasImpulse = true;
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
