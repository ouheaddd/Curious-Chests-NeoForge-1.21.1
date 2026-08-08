package com.overyourhead.curiouschests.common.block;

import com.mojang.serialization.MapCodec;
import com.overyourhead.curiouschests.common.chest.ChestKind;
import net.minecraft.world.level.block.BaseEntityBlock;

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
    protected MapCodec<WitchChestBlock> codec() {
        return CODEC;
    }
}
