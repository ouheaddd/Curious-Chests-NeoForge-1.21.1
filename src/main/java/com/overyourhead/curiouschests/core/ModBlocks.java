package com.overyourhead.curiouschests.core;

import com.overyourhead.curiouschests.CuriousChestsMod;
import com.overyourhead.curiouschests.common.block.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CuriousChestsMod.MOD_ID);
    private static BlockBehaviour.Properties props() { return BlockBehaviour.Properties.ofFullCopy(Blocks.CHEST).noOcclusion(); }
    public static final DeferredBlock<BottomlessChestBlock> BOTTOMLESS_CHEST = BLOCKS.registerBlock("bottomless_chest", BottomlessChestBlock::new, props());
    public static final DeferredBlock<InfernalChestBlock> INFERNAL_CHEST = BLOCKS.registerBlock("infernal_chest", InfernalChestBlock::new, props());
    public static final DeferredBlock<EnderDispatchChestBlock> ENDER_DISPATCH_CHEST = BLOCKS.registerBlock("ender_dispatch_chest", EnderDispatchChestBlock::new, props());
    public static final DeferredBlock<BuildersChestBlock> BUILDERS_CHEST = BLOCKS.registerBlock("builders_chest", BuildersChestBlock::new, props());
    public static final DeferredBlock<CollectorsChestBlock> COLLECTORS_CHEST = BLOCKS.registerBlock("collectors_chest", CollectorsChestBlock::new, props());
    private ModBlocks() {}
}
