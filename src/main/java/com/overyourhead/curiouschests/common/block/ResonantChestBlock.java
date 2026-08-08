package com.overyourhead.curiouschests.common.block;

import com.mojang.serialization.MapCodec;
import com.overyourhead.curiouschests.common.chest.ChestKind;
import net.minecraft.world.level.block.BaseEntityBlock;

public final class ResonantChestBlock extends AbstractSpecialChestBlock {
    public static final MapCodec<ResonantChestBlock> CODEC = BaseEntityBlock.simpleCodec(ResonantChestBlock::new);

    public ResonantChestBlock(Properties properties) {
        super(properties);
    }

    @Override
    public ChestKind kind() {
        return ChestKind.RESONANT;
    }

    @Override
    protected MapCodec<ResonantChestBlock> codec() {
        return CODEC;
    }
}
