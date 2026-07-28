package com.overyourhead.curiouschests.common.logic;

import com.overyourhead.curiouschests.common.block.AbstractSpecialChestBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
    public static final int TICKS_PER_ITEM = 100;
    public static final int INPUT_START = 0;
    public static final int INPUT_END = 9;
    public static final int OUTPUT_START = 9;
    public static final int OUTPUT_END = 27;

    private InfernalLogic() {}

    public static boolean smeltOne(Level level, BlockPos pos, BlockState state, Container container) {
        for (int slot = INPUT_START; slot < INPUT_END; slot++) {
            ItemStack inputStack = container.getItem(slot);
            if (inputStack.isEmpty()) continue;

            SingleRecipeInput input = new SingleRecipeInput(inputStack);
            Optional<RecipeHolder<SmeltingRecipe>> found =
                    level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, input, level);
            if (found.isEmpty()) continue;

            ItemStack result = found.get().value().assemble(input, level.registryAccess());
            if (result.isEmpty()) continue;

            ItemStack moving = result.copy();
            InventoryTransfer.insertIntoRange(container, moving, OUTPUT_START, OUTPUT_END);
            if (!moving.isEmpty()) eject(level, pos, state, moving);

            inputStack.shrink(1);
            container.setItem(slot, inputStack.isEmpty() ? ItemStack.EMPTY : inputStack);
            container.setChanged();
            return true;
        }
        return false;
    }

    private static void eject(Level level, BlockPos pos, BlockState state, ItemStack stack) {
        Direction facing = state.getValue(AbstractSpecialChestBlock.FACING);
        double x = pos.getX() + 0.5 + facing.getStepX() * 0.8;
        double y = pos.getY() + 0.65;
        double z = pos.getZ() + 0.5 + facing.getStepZ() * 0.8;

        ItemEntity dropped = new ItemEntity(level, x, y, z, stack.copy());
        dropped.setDeltaMovement(
                facing.getStepX() * 0.16,
                0.10,
                facing.getStepZ() * 0.16
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
