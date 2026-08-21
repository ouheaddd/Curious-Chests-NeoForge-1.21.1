package com.overyourhead.curiouschests.core;

import com.overyourhead.curiouschests.CuriousChestsMod;
import com.overyourhead.curiouschests.common.block.ArchivistChestBlock;
import com.overyourhead.curiouschests.common.block.BottomlessChestBlock;
import com.overyourhead.curiouschests.common.block.BuildersChestBlock;
import com.overyourhead.curiouschests.common.block.CollectorsChestBlock;
import com.overyourhead.curiouschests.common.block.EnderDispatchChestBlock;
import com.overyourhead.curiouschests.common.block.InfernalChestBlock;
import com.overyourhead.curiouschests.common.block.SculkSentinelChestBlock;
import com.overyourhead.curiouschests.common.block.ResonantChestBlock;
import com.overyourhead.curiouschests.common.block.WitchChestBlock;
import com.overyourhead.curiouschests.common.block.TrapperChestBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CuriousChestsMod.MOD_ID);

    private static BlockBehaviour.Properties props() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.CHEST).noOcclusion();
    }

    private static BlockBehaviour.Properties sentinelProps() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.CHEST)
                .noOcclusion()
                .strength(4.0F, 1200.0F)
                .pushReaction(PushReaction.BLOCK);
    }

    public static final DeferredBlock<BottomlessChestBlock> BOTTOMLESS_CHEST = BLOCKS.registerBlock("bottomless_chest", BottomlessChestBlock::new, props().sound(SoundType.WOOD));
    public static final DeferredBlock<InfernalChestBlock> INFERNAL_CHEST = BLOCKS.registerBlock("infernal_chest", InfernalChestBlock::new, props().sound(SoundType.NETHER_BRICKS).lightLevel(state -> 5));
    public static final DeferredBlock<EnderDispatchChestBlock> ENDER_DISPATCH_CHEST = BLOCKS.registerBlock("ender_dispatch_chest", EnderDispatchChestBlock::new, props().sound(SoundType.STONE));
    public static final DeferredBlock<BuildersChestBlock> BUILDERS_CHEST = BLOCKS.registerBlock("builders_chest", BuildersChestBlock::new, props());
    public static final DeferredBlock<CollectorsChestBlock> COLLECTORS_CHEST = BLOCKS.registerBlock("collectors_chest", CollectorsChestBlock::new, props());
    public static final DeferredBlock<SculkSentinelChestBlock> SCULK_SENTINEL_CHEST = BLOCKS.registerBlock("sculk_sentinel_chest", SculkSentinelChestBlock::new, sentinelProps().sound(SoundType.SCULK_CATALYST));
    public static final DeferredBlock<ResonantChestBlock> RESONANT_CHEST = BLOCKS.registerBlock("resonant_chest", ResonantChestBlock::new, props().sound(SoundType.AMETHYST).lightLevel(state -> 5));
    public static final DeferredBlock<ArchivistChestBlock> ARCHIVISTS_CHEST = BLOCKS.registerBlock("archivists_chest", ArchivistChestBlock::new, props());
    public static final DeferredBlock<WitchChestBlock> WITCHS_CHEST = BLOCKS.registerBlock("witchs_chest", WitchChestBlock::new, props().lightLevel(state -> 6));
    public static final DeferredBlock<TrapperChestBlock> TRAPPERS_CHEST = BLOCKS.registerBlock(
            "trappers_chest",
            TrapperChestBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.VAULT).noOcclusion().lightLevel(state -> state.hasProperty(TrapperChestBlock.OCCUPIED) && state.getValue(TrapperChestBlock.OCCUPIED) ? 4 : 0)
    );

    private ModBlocks() {}
}
