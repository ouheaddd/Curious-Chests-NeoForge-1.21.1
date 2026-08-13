package com.overyourhead.curiouschests.common.logic;

import com.overyourhead.curiouschests.CuriousChestsMod;
import com.overyourhead.curiouschests.common.blockentity.SpecialChestBlockEntity;
import com.overyourhead.curiouschests.common.chest.ChestKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class DispatchLogic {
    public static final int PREVIEW_TICKS = 20;
    public static final int POST_TRANSFER_GAP_TICKS = 30;
    public static final int TRANSFER_DELAY_TICKS = PREVIEW_TICKS + POST_TRANSFER_GAP_TICKS;
    public static final int RETRY_DELAY_TICKS = 10;
    public static final int RADIUS = 8;

    /**
     * Explicit opt-out for inventories that should never be treated as Dispatch
     * storage. Vanilla processing/utility inventories are supplied by the mod's
     * datapack, and modpacks can add modded machines here without Java changes.
     */
    public static final TagKey<Block> TARGET_BLACKLIST = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(CuriousChestsMod.MOD_ID, "ender_dispatch_blacklist")
    );

    private DispatchLogic() {}

    /**
     * Finds the first source slot that can currently move at least one item into
     * some valid destination. Nothing is moved yet; this is only the visual
     * preview reservation used by the Ender Dispatch chest.
     */
    public static Preview findPreview(Level level, BlockPos origin, Container source) {
        List<Target> targets = scan(level, origin);
        if (targets.isEmpty()) return null;

        for (int sourceSlot = 0; sourceSlot < source.getContainerSize(); sourceSlot++) {
            ItemStack stack = source.getItem(sourceSlot);
            if (stack.isEmpty()) continue;

            // The two themed Curious Chests are intentional exceptions to the normal
            // "never dispatch into another special chest" rule. They get first refusal
            // only for the exact item family they are designed to store.
            for (Target target : targets) {
                if (target.isSpecializedMatch(stack) && target.canInsertAny(stack)) {
                    return new Preview(sourceSlot, stack.copyWithCount(1));
                }
            }

            for (Target target : targets) {
                if (!target.isSpecialized() && target.canInsertAny(stack)) {
                    // The floating render represents the item type, not a literal
                    // dropped stack count. Preserve every data component, but show one.
                    return new Preview(sourceSlot, stack.copyWithCount(1));
                }
            }
        }
        return null;
    }

    /**
     * Transfers only the slot that was previewed. The slot is validated again
     * immediately before moving anything so a player/hopper cannot leave a
     * stale visual that teleports a different item.
     */
    public static boolean dispatchPreviewed(
            Level level,
            BlockPos origin,
            Container source,
            int sourceSlot,
            ItemStack expected
    ) {
        if (sourceSlot < 0 || sourceSlot >= source.getContainerSize() || expected.isEmpty()) {
            return false;
        }

        ItemStack original = source.getItem(sourceSlot);
        if (original.isEmpty() || !ItemStack.isSameItemSameComponents(original, expected)) {
            return false;
        }

        List<Target> targets = scan(level, origin);
        if (targets.isEmpty()) return false;

        ItemStack moving = original.copy();
        int before = moving.getCount();
        Target visualTarget = null;
        Set<BlockPos> preferredPositions = new HashSet<>();

        /*
         * Specialized routing has absolute priority:
         *  - processable enchanted books -> Archivist
         *  - potion-content items -> Witch's Chest
         * No other item is ever offered to those special chests. If every matching
         * special chest is full/unavailable, normal Dispatch sorting takes over.
         */
        for (Target target : targets) {
            if (!target.isSpecializedMatch(moving)) continue;

            int inserted = target.insert(moving);
            if (inserted > 0 && visualTarget == null) {
                visualTarget = target;
            }
            if (moving.isEmpty()) break;
        }

        /*
         * A normal storage target that already contains the same ITEM ID is always a
         * sorting destination before a closer empty target. Components are
         * deliberately ignored for choosing the destination, while the actual
         * insertion remains component-safe and obeys the target's own handler.
         */
        if (!moving.isEmpty()) {
            for (Target target : targets) {
                if (target.isSpecialized() || !target.containsSameItem(moving)) continue;

                preferredPositions.add(target.pos());
                int inserted = target.insert(moving);
                if (inserted > 0 && visualTarget == null) {
                    visualTarget = target;
                }
                if (moving.isEmpty()) break;
            }
        }

        // Only after all matching normal storage is full do we use nearest free storage.
        if (!moving.isEmpty()) {
            for (Target target : targets) {
                if (target.isSpecialized() || preferredPositions.contains(target.pos())) continue;

                int inserted = target.insert(moving);
                if (inserted > 0 && visualTarget == null) {
                    visualTarget = target;
                }
                if (moving.isEmpty()) break;
            }
        }

        int moved = before - moving.getCount();
        if (moved <= 0) return false;

        original.shrink(moved);
        source.setItem(sourceSlot, original.isEmpty() ? ItemStack.EMPTY : original);
        source.setChanged();
        if (visualTarget != null) effects(level, origin, visualTarget.pos());
        return true;
    }

    /** Retained as a compatibility helper for any older call sites. */
    public static boolean dispatchOne(Level level, BlockPos origin, Container source) {
        Preview preview = findPreview(level, origin, source);
        return preview != null && dispatchPreviewed(level, origin, source, preview.sourceSlot(), preview.stack());
    }

    private static List<Target> scan(Level level, BlockPos origin) {
        List<Target> result = new ArrayList<>();
        BlockPos min = origin.offset(-RADIUS, -RADIUS, -RADIUS);
        BlockPos max = origin.offset(RADIUS, RADIUS, RADIUS);

        for (BlockPos cursor : BlockPos.betweenClosed(min, max)) {
            BlockPos pos = cursor.immutable();
            if (pos.equals(origin) || !level.hasChunkAt(pos) || pos.distSqr(origin) > RADIUS * RADIUS) continue;

            BlockState state = level.getBlockState(pos);
            if (state.is(TARGET_BLACKLIST)) continue;

            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof SpecialChestBlockEntity specialChest) {
                // Compression/Bottomless is ordinary storage from Dispatch's point
                // of view, so let it participate in the same sorting rules as chests,
                // barrels and modded storage. All of its own slot limits are still
                // enforced by the exposed item handler.
                if (specialChest.kind() == ChestKind.BOTTOMLESS) {
                    result.add(Target.forHandler(pos, specialChest.getItemHandler()));
                    continue;
                }

                // Archivist and Witch are the only themed routing exceptions. They
                // remain specialized so books/potions get first refusal, while no
                // unrelated item is ever offered to them.
                if (specialChest.kind() == ChestKind.ARCHIVIST
                        || specialChest.kind() == ChestKind.WITCH) {
                    result.add(Target.forSpecialHandler(
                            pos,
                            specialChest.getItemHandler(),
                            specialChest.kind()
                    ));
                }
                continue;
            }

            // Prefer NeoForge's capability so modded storage can enforce its own
            // slot/side/stack rules. Container remains a fallback for inventories
            // that do not expose an item-handler capability.
            IItemHandler handler = findItemHandler(level, pos, state, blockEntity);
            if (handler != null) {
                result.add(Target.forHandler(pos, handler));
                continue;
            }

            if (blockEntity instanceof Container container) {
                result.add(Target.forContainer(pos, container));
            }
        }

        result.sort(Comparator.comparingDouble(target -> target.pos().distSqr(origin)));
        return result;
    }

    private static IItemHandler findItemHandler(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        // Unsided handlers are the best representation of a magical remote insert.
        IItemHandler handler = level.getCapability(
                Capabilities.ItemHandler.BLOCK,
                pos,
                state,
                blockEntity,
                null
        );
        if (handler != null) return handler;

        // Some modded storage exposes only sided handlers. Fall back to whichever
        // side it actually permits instead of silently ignoring the storage.
        for (Direction side : Direction.values()) {
            handler = level.getCapability(
                    Capabilities.ItemHandler.BLOCK,
                    pos,
                    state,
                    blockEntity,
                    side
            );
            if (handler != null) return handler;
        }
        return null;
    }

    private static boolean canInsertAny(IItemHandler handler, ItemStack source) {
        if (source.isEmpty()) return false;

        ItemStack remainder = source.copy();
        for (int slot = 0; slot < handler.getSlots() && !remainder.isEmpty(); slot++) {
            int before = remainder.getCount();
            remainder = handler.insertItem(slot, remainder, true);
            if (remainder.getCount() < before) return true;
        }
        return false;
    }

    private static int insert(IItemHandler handler, ItemStack source) {
        if (source.isEmpty()) return 0;

        int before = source.getCount();
        ItemStack remainder = source.copy();

        // First top off stacks with the exact same item + components. This matters
        // especially for Compression: 5 + an incoming 64 should become 69 in that
        // slot instead of creating a fresh 64-stack in an earlier empty slot.
        for (int slot = 0; slot < handler.getSlots() && !remainder.isEmpty(); slot++) {
            ItemStack present = handler.getStackInSlot(slot);
            if (present.isEmpty() || !ItemStack.isSameItemSameComponents(present, remainder)) continue;
            remainder = handler.insertItem(slot, remainder, false);
        }

        // Then fall back to the handler's remaining slots in its normal order.
        // Standard inventories will use empty slots here; unusual modded handlers
        // still get the same insertion opportunities they had before this fix.
        for (int slot = 0; slot < handler.getSlots() && !remainder.isEmpty(); slot++) {
            ItemStack present = handler.getStackInSlot(slot);
            if (!present.isEmpty() && ItemStack.isSameItemSameComponents(present, remainder)) continue;
            remainder = handler.insertItem(slot, remainder, false);
        }

        int moved = before - remainder.getCount();
        if (moved > 0) source.shrink(moved);
        return moved;
    }

    private static boolean containsSameItem(IItemHandler handler, ItemStack wanted) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack present = handler.getStackInSlot(slot);
            if (!present.isEmpty() && present.is(wanted.getItem())) {
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

    private static void effects(Level level, BlockPos from, BlockPos to) {
        if (!(level instanceof ServerLevel server)) return;
        server.sendParticles(
                ParticleTypes.PORTAL,
                from.getX() + 0.5, from.getY() + 1.28, from.getZ() + 0.5,
                12, 0.22, 0.16, 0.22, 0.025
        );
        server.sendParticles(
                ParticleTypes.REVERSE_PORTAL,
                to.getX() + 0.5, to.getY() + 1.0, to.getZ() + 0.5,
                6, 0.2, 0.1, 0.2, 0.01
        );
        level.playSound(null, from, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 0.45F, 1.25F);
    }

    public record Preview(int sourceSlot, ItemStack stack) {}

    private record Target(
            BlockPos pos,
            IItemHandler handler,
            Container container,
            ChestKind specializedKind
    ) {
        private static Target forHandler(BlockPos pos, IItemHandler handler) {
            return new Target(pos, handler, null, null);
        }

        private static Target forContainer(BlockPos pos, Container container) {
            return new Target(pos, null, container, null);
        }

        private static Target forSpecialHandler(BlockPos pos, IItemHandler handler, ChestKind kind) {
            return new Target(pos, handler, null, kind);
        }

        private boolean isSpecialized() {
            return specializedKind != null;
        }

        private boolean isSpecializedMatch(ItemStack stack) {
            if (specializedKind == ChestKind.ARCHIVIST) {
                return ArchivistLogic.isProcessableBook(stack);
            }
            if (specializedKind == ChestKind.WITCH) {
                return WitchLogic.isSupported(stack);
            }
            return false;
        }

        private boolean canInsertAny(ItemStack stack) {
            return handler != null
                    ? DispatchLogic.canInsertAny(handler, stack)
                    : InventoryTransfer.canInsertAny(container, stack);
        }

        private boolean containsSameItem(ItemStack stack) {
            return handler != null
                    ? DispatchLogic.containsSameItem(handler, stack)
                    : DispatchLogic.containsSameItem(container, stack);
        }

        private int insert(ItemStack stack) {
            return handler != null
                    ? DispatchLogic.insert(handler, stack)
                    : InventoryTransfer.insert(container, stack, false);
        }
    }
}
