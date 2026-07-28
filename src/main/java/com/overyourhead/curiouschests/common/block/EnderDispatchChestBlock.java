package com.overyourhead.curiouschests.common.block;

import com.overyourhead.curiouschests.common.chest.ChestKind;
import com.mojang.serialization.MapCodec;

public final class EnderDispatchChestBlock extends AbstractSpecialChestBlock {
    public static final MapCodec<EnderDispatchChestBlock> CODEC = simpleCodec(EnderDispatchChestBlock::new);
    public EnderDispatchChestBlock(Properties properties) { super(properties); }
    @Override public ChestKind kind() { return ChestKind.ENDER_DISPATCH; }
    @Override protected MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
}
