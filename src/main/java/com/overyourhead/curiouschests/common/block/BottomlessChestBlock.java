package com.overyourhead.curiouschests.common.block;

import com.overyourhead.curiouschests.common.chest.ChestKind;
import com.mojang.serialization.MapCodec;

public final class BottomlessChestBlock extends AbstractSpecialChestBlock {
    public static final MapCodec<BottomlessChestBlock> CODEC = simpleCodec(BottomlessChestBlock::new);
    public BottomlessChestBlock(Properties properties) { super(properties); }
    @Override public ChestKind kind() { return ChestKind.BOTTOMLESS; }
    @Override protected MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
}
