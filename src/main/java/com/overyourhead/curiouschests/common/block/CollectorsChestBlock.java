package com.overyourhead.curiouschests.common.block;

import com.overyourhead.curiouschests.common.chest.ChestKind;
import com.mojang.serialization.MapCodec;

public final class CollectorsChestBlock extends AbstractSpecialChestBlock {
    public static final MapCodec<CollectorsChestBlock> CODEC = simpleCodec(CollectorsChestBlock::new);
    public CollectorsChestBlock(Properties properties) { super(properties); }
    @Override public ChestKind kind() { return ChestKind.COLLECTORS; }
    @Override protected MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
}
