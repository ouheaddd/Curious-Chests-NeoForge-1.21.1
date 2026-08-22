package com.overyourhead.curiouschests.common.block;

import com.mojang.serialization.MapCodec;
import com.overyourhead.curiouschests.common.chest.ChestKind;
import com.overyourhead.curiouschests.common.blockentity.SpecialChestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class WitchChestBlock extends AbstractSpecialChestBlock {
    public static final MapCodec<WitchChestBlock> CODEC = BaseEntityBlock.simpleCodec(WitchChestBlock::new);

    public WitchChestBlock(Properties properties) {
        super(properties);
    }

    @Override
    public ChestKind kind() {
        return ChestKind.WITCH;
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (player.isShiftKeyDown() && stack.is(Items.GLASS_BOTTLE)) {
            if (!level.isClientSide
                    && level.getBlockEntity(pos) instanceof SpecialChestBlockEntity chest) {
                chest.tryScoopWitchBrew(player, stack);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected MapCodec<WitchChestBlock> codec() {
        return CODEC;
    }
}
