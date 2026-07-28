package com.overyourhead.curiouschests.common.logic;

import com.overyourhead.curiouschests.common.chest.ChestRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class CollectorLogic {
    public static final int RADIUS = 8;
    public static final int TICK_INTERVAL = 2;
    private static final double ABSORB_DISTANCE = 0.8;
    private static final double PULL_SPEED = 0.14;

    private CollectorLogic() {}

    public static boolean tick(ServerLevel level, BlockPos origin, Container target) {
        Vec3 destination = new Vec3(origin.getX() + 0.5, origin.getY() + 1.15, origin.getZ() + 0.5);
        AABB area = new AABB(origin).inflate(RADIUS);
        boolean changed = false;

        for (ItemEntity itemEntity : level.getEntitiesOfClass(
                ItemEntity.class,
                area,
                ItemEntity::isAlive
        )) {
            ItemStack stack = itemEntity.getItem();
            if (!ChestRules.canStore(stack) || !InventoryTransfer.canInsertAny(target, stack)) continue;

            Vec3 offset = destination.subtract(itemEntity.position());
            if (offset.lengthSqr() > RADIUS * RADIUS) continue;
            double distance = offset.length();

            if (distance <= ABSORB_DISTANCE) {
                ItemStack moving = stack.copy();
                int moved = InventoryTransfer.insert(target, moving, false);
                if (moved <= 0) continue;

                changed = true;
                if (moving.isEmpty()) {
                    itemEntity.discard();
                } else {
                    itemEntity.setItem(moving);
                }

                level.sendParticles(
                        ParticleTypes.HAPPY_VILLAGER,
                        destination.x, destination.y, destination.z,
                        5, 0.18, 0.12, 0.18, 0.0
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

            Vec3 pull = offset.normalize().scale(PULL_SPEED);
            itemEntity.setDeltaMovement(itemEntity.getDeltaMovement().scale(0.65).add(pull));

            if (level.getGameTime() % 6L == 0L) {
                level.sendParticles(
                        ParticleTypes.HAPPY_VILLAGER,
                        itemEntity.getX(), itemEntity.getY() + 0.1, itemEntity.getZ(),
                        1, 0.01, 0.01, 0.01, 0.0
                );
            }
        }

        return changed;
    }
}
