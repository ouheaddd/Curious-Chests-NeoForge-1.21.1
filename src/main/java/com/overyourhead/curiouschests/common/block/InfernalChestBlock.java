package com.overyourhead.curiouschests.common.block;

import com.overyourhead.curiouschests.common.chest.ChestKind;
import com.mojang.serialization.MapCodec;

public final class InfernalChestBlock extends AbstractSpecialChestBlock {
    public static final MapCodec<InfernalChestBlock> CODEC = simpleCodec(InfernalChestBlock::new);
    public InfernalChestBlock(Properties properties) { super(properties); }
    @Override public ChestKind kind() { return ChestKind.INFERNAL; }
    @Override protected MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
}
