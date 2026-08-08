package com.overyourhead.curiouschests.common.block;

import com.mojang.serialization.MapCodec;
import com.overyourhead.curiouschests.common.chest.ChestKind;
import net.minecraft.world.level.block.BaseEntityBlock;

public final class ArchivistChestBlock extends AbstractSpecialChestBlock {
    public static final MapCodec<ArchivistChestBlock> CODEC = BaseEntityBlock.simpleCodec(ArchivistChestBlock::new);

    public ArchivistChestBlock(Properties properties) {
        super(properties);
    }

    @Override
    public ChestKind kind() {
        return ChestKind.ARCHIVIST;
    }

    @Override
    protected MapCodec<ArchivistChestBlock> codec() {
        return CODEC;
    }
}
