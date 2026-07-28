package com.overyourhead.curiouschests.common.block;

import com.overyourhead.curiouschests.common.chest.ChestKind;
import com.mojang.serialization.MapCodec;

public final class BuildersChestBlock extends AbstractSpecialChestBlock {
    public static final MapCodec<BuildersChestBlock> CODEC = simpleCodec(BuildersChestBlock::new);
    public BuildersChestBlock(Properties properties) { super(properties); }
    @Override public ChestKind kind() { return ChestKind.BUILDERS; }
    @Override protected MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
}
