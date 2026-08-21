package com.overyourhead.curiouschests.core;

import com.overyourhead.curiouschests.CuriousChestsMod;
import com.overyourhead.curiouschests.common.blockentity.SpecialChestBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CuriousChestsMod.MOD_ID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SpecialChestBlockEntity>> SPECIAL_CHEST = BLOCK_ENTITIES.register("special_chest", () -> BlockEntityType.Builder.of(
            SpecialChestBlockEntity::new,
            ModBlocks.BOTTOMLESS_CHEST.get(),
            ModBlocks.INFERNAL_CHEST.get(),
            ModBlocks.ENDER_DISPATCH_CHEST.get(),
            ModBlocks.BUILDERS_CHEST.get(),
            ModBlocks.COLLECTORS_CHEST.get(),
            ModBlocks.SCULK_SENTINEL_CHEST.get(),
            ModBlocks.RESONANT_CHEST.get(),
            ModBlocks.ARCHIVISTS_CHEST.get(),
            ModBlocks.WITCHS_CHEST.get(),
            ModBlocks.TRAPPERS_CHEST.get()
    ).build(null));
    private ModBlockEntities() {}
}
