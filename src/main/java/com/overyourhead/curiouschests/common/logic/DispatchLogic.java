package com.overyourhead.curiouschests.common.logic;

import com.overyourhead.curiouschests.common.blockentity.SpecialChestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class DispatchLogic {
    public static final int TICK_INTERVAL = 4;
    public static final int RADIUS = 8;

    private DispatchLogic() {}

    public static boolean dispatchOne(Level level, BlockPos origin, Container source) {
        List<Target> targets = scan(level, origin);
        if (targets.isEmpty()) return false;

        for (int sourceSlot = 0; sourceSlot < source.getContainerSize(); sourceSlot++) {
            ItemStack original = source.getItem(sourceSlot);
            if (original.isEmpty()) continue;

            ItemStack moving = original.copy();
            int before = moving.getCount();
            Target visualTarget = null;

            // Prefer storage that already contains the exact item.
            for (Target target : targets) {
                if (!contains(target.container(), moving)) continue;
                if (InventoryTransfer.insert(target.container(), moving, true) > 0 && visualTarget == null) {
                    visualTarget = target;
                }
                if (moving.isEmpty()) break;
            }

            // Then fill the nearest available storage.
            if (!moving.isEmpty()) {
                for (Target target : targets) {
                    if (InventoryTransfer.insert(target.container(), moving, false) > 0 && visualTarget == null) {
                        visualTarget = target;
                    }
                    if (moving.isEmpty()) break;
                }
            }

            int moved = before - moving.getCount();
            if (moved > 0) {
                original.shrink(moved);
                source.setItem(sourceSlot, original.isEmpty() ? ItemStack.EMPTY : original);
                source.setChanged();
                if (visualTarget != null) effects(level, origin, visualTarget.pos());
                return true;
            }
        }
        return false;
    }

    private static boolean contains(Container container, ItemStack wanted) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, wanted)) return true;
        }
        return false;
    }

    private static List<Target> scan(Level level, BlockPos origin) {
        List<Target> result = new ArrayList<>();
        BlockPos min = origin.offset(-RADIUS, -RADIUS, -RADIUS);
        BlockPos max = origin.offset(RADIUS, RADIUS, RADIUS);

        for (BlockPos cursor : BlockPos.betweenClosed(min, max)) {
            BlockPos pos = cursor.immutable();
            if (pos.equals(origin) || !level.hasChunkAt(pos) || pos.distSqr(origin) > RADIUS * RADIUS) continue;

            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof Container container)) continue;

            // Avoid loops and do not accidentally feed processing machines.
            if (blockEntity instanceof SpecialChestBlockEntity || blockEntity instanceof AbstractFurnaceBlockEntity) continue;
            result.add(new Target(pos, container));
        }

        result.sort(Comparator.comparingDouble(target -> target.pos().distSqr(origin)));
        return result;
    }

    private static void effects(Level level, BlockPos from, BlockPos to) {
        if (!(level instanceof ServerLevel server)) return;
        server.sendParticles(
                ParticleTypes.PORTAL,
                from.getX() + 0.5, from.getY() + 1.05, from.getZ() + 0.5,
                10, 0.25, 0.15, 0.25, 0.02
        );
        server.sendParticles(
                ParticleTypes.REVERSE_PORTAL,
                to.getX() + 0.5, to.getY() + 1.0, to.getZ() + 0.5,
                6, 0.2, 0.1, 0.2, 0.01
        );
        level.playSound(null, from, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 0.45F, 1.25F);
    }

    private record Target(BlockPos pos, Container container) {}
}
