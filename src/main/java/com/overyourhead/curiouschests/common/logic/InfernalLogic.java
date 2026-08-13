package com.overyourhead.curiouschests.common.logic;

import com.overyourhead.curiouschests.common.block.AbstractSpecialChestBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public final class InfernalLogic {
    public static final int TICKS_PER_ITEM = 150;
    public static final int INPUT_START = 0;
    public static final int INPUT_END = 9;
    public static final int OUTPUT_START = 9;
    public static final int OUTPUT_END = 27;

    private InfernalLogic() {}

    public static boolean smeltOne(Level level, BlockPos pos, BlockState state, Container container) {
        for (int slot = INPUT_START; slot < INPUT_END; slot++) {
            ItemStack inputStack = container.getItem(slot);
            if (inputStack.isEmpty()) continue;

            // Infernal processes the first occupied input slot in order. It never
            // skips "bad" input to reach something smeltable behind it.
            SingleRecipeInput input = new SingleRecipeInput(inputStack);
            Optional<RecipeHolder<SmeltingRecipe>> found =
                    level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, input, level);

            if (found.isPresent()) {
                ItemStack result = found.get().value().assemble(input, level.registryAccess());
                if (!result.isEmpty()) {
                    ItemStack moving = result.copy();
                    InventoryTransfer.insertIntoRange(container, moving, OUTPUT_START, OUTPUT_END);
                    if (!moving.isEmpty()) eject(level, pos, state, moving);

                    consumeOne(container, slot, inputStack);
                    return true;
                }
            }

            // No usable smelting result: the furnace simply burns one item away.
            consumeOne(container, slot, inputStack);
            incinerateFeedback(level, pos, state);
            return true;
        }
        return false;
    }

    private static void consumeOne(Container container, int slot, ItemStack stack) {
        stack.shrink(1);
        container.setItem(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);
        container.setChanged();
    }

    private static void incinerateFeedback(Level level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(AbstractSpecialChestBlock.FACING);
        double x = pos.getX() + 0.5D + facing.getStepX() * 0.54D;
        double y = pos.getY() + 0.34D;
        double z = pos.getZ() + 0.5D + facing.getStepZ() * 0.54D;

        level.playSound(
                null,
                pos,
                SoundEvents.FIRE_EXTINGUISH,
                SoundSource.BLOCKS,
                0.42F,
                0.88F + level.random.nextFloat() * 0.16F
        );

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE, x, y, z, 6, 0.12D, 0.08D, 0.12D, 0.015D);
            serverLevel.sendParticles(ParticleTypes.FLAME, x, y, z, 3, 0.10D, 0.06D, 0.10D, 0.010D);
        }
    }

    private static void eject(Level level, BlockPos pos, BlockState state, ItemStack stack) {
        Direction facing = state.getValue(AbstractSpecialChestBlock.FACING);
        // Overflow drops low and close to the furnace face, like hot output
        // falling out of the front rather than being launched like a dropper.
        double x = pos.getX() + 0.5 + facing.getStepX() * 0.58;
        double y = pos.getY() + 0.28;
        double z = pos.getZ() + 0.5 + facing.getStepZ() * 0.58;

        ItemEntity dropped = new ItemEntity(level, x, y, z, stack.copy());
        dropped.setDeltaMovement(
                facing.getStepX() * 0.07,
                0.055,
                facing.getStepZ() * 0.07
        );
        level.addFreshEntity(dropped);
        level.playSound(
                null,
                pos,
                SoundEvents.FIRECHARGE_USE,
                SoundSource.BLOCKS,
                0.45F,
                1.15F
        );
    }
}
