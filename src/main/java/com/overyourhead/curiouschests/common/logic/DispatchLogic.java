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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class DispatchLogic {
    public static final int TRANSFER_DELAY_TICKS = 50;
    public static final int RETRY_DELAY_TICKS = 10;
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
            Set<BlockPos> preferredPositions = new HashSet<>();

            /*
             * A container that already contains the same ITEM ID is always a
             * sorting destination before a closer empty container. Components
             * are deliberately ignored for choosing the destination, so tools
             * such as two diamond swords still end up in the same chest. The
             * normal insertion rules remain component-safe: unlike stacks do
             * not merge, but they may occupy another empty slot in that chest.
             */
            for (Target target : targets) {
                if (!containsSameItem(target.container(), moving)) continue;

                preferredPositions.add(target.pos());
                int inserted = InventoryTransfer.insert(target.container(), moving, false);
                if (inserted > 0 && visualTarget == null) {
                    visualTarget = target;
                }
                if (moving.isEmpty()) break;
            }

            // Only after all matching storage is full do we use nearest free storage.
            if (!moving.isEmpty()) {
                for (Target target : targets) {
                    if (preferredPositions.contains(target.pos())) continue;

                    int inserted = InventoryTransfer.insert(target.container(), moving, false);
                    if (inserted > 0 && visualTarget == null) {
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

    private static boolean containsSameItem(Container container, ItemStack wanted) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack present = container.getItem(slot);
            if (!present.isEmpty() && present.is(wanted.getItem())) {
                return true;
            }
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
