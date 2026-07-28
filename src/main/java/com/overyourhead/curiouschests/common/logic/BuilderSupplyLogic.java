package com.overyourhead.curiouschests.common.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class BuilderSupplyLogic {
    public static final int RADIUS = 16;
    public static final int TICK_INTERVAL = 2;

    private BuilderSupplyLogic() {}

    public static void tick(
            ServerLevel level,
            BlockPos origin,
            Container source,
            Map<UUID, HeldSnapshot> snapshots
    ) {
        AABB area = new AABB(origin).inflate(RADIUS);
        Set<UUID> activePlayers = new HashSet<>();

        for (ServerPlayer player : level.getEntitiesOfClass(
                ServerPlayer.class,
                area,
                candidate -> !candidate.isSpectator() && candidate.blockPosition().distSqr(origin) <= RADIUS * RADIUS
        )) {
            UUID playerId = player.getUUID();
            activePlayers.add(playerId);

            int selectedSlot = player.getInventory().selected;
            ItemStack current = player.getInventory().getItem(selectedSlot);
            HeldSnapshot previous = snapshots.get(playerId);

            boolean keepPreviousPrototype = false;
            if (previous != null
                    && previous.slot() == selectedSlot
                    && current.isEmpty()
                    && !previous.prototype().isEmpty()) {
                boolean restocked = restock(player, source, selectedSlot, previous.prototype(), level, origin);
                current = player.getInventory().getItem(selectedSlot);
                keepPreviousPrototype = !restocked && current.isEmpty();
            }

            snapshots.put(playerId, keepPreviousPrototype
                    ? previous
                    : new HeldSnapshot(
                            selectedSlot,
                            current.isEmpty() ? ItemStack.EMPTY : current.copyWithCount(1)
                    ));
        }

        snapshots.keySet().removeIf(playerId -> !activePlayers.contains(playerId));
    }

    private static boolean restock(
            ServerPlayer player,
            Container source,
            int selectedSlot,
            ItemStack prototype,
            ServerLevel level,
            BlockPos origin
    ) {
        for (int slot = 0; slot < source.getContainerSize(); slot++) {
            ItemStack stored = source.getItem(slot);
            if (stored.isEmpty() || !ItemStack.isSameItemSameComponents(stored, prototype)) continue;

            int amount = Math.min(stored.getCount(), stored.getMaxStackSize());
            player.getInventory().setItem(selectedSlot, stored.copyWithCount(amount));
            player.getInventory().setChanged();

            stored.shrink(amount);
            source.setItem(slot, stored.isEmpty() ? ItemStack.EMPTY : stored);
            source.setChanged();

            level.sendParticles(
                    ParticleTypes.HAPPY_VILLAGER,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    4, 0.2, 0.25, 0.2, 0.0
            );
            level.playSound(
                    null,
                    origin,
                    SoundEvents.ITEM_PICKUP,
                    SoundSource.BLOCKS,
                    0.35F,
                    1.35F
            );
            return true;
        }
        return false;
    }

    public record HeldSnapshot(int slot, ItemStack prototype) {}
}
