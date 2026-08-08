package com.overyourhead.curiouschests.core;

import com.overyourhead.curiouschests.CuriousChestsMod;
import com.overyourhead.curiouschests.common.chest.ChestKind;
import com.overyourhead.curiouschests.common.item.SpecialChestBlockItem;
import com.overyourhead.curiouschests.common.item.ResonanceCrystalItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CuriousChestsMod.MOD_ID);

    // Registration order is also used by the Curious Chests creative tab.
    public static final DeferredItem<SpecialChestBlockItem> BOTTOMLESS_CHEST_ITEM = ITEMS.registerItem(
            "bottomless_chest",
            properties -> new SpecialChestBlockItem(ModBlocks.BOTTOMLESS_CHEST.get(), ChestKind.BOTTOMLESS, properties)
    );
    public static final DeferredItem<SpecialChestBlockItem> INFERNAL_CHEST_ITEM = ITEMS.registerItem(
            "infernal_chest",
            properties -> new SpecialChestBlockItem(ModBlocks.INFERNAL_CHEST.get(), ChestKind.INFERNAL, properties.fireResistant())
    );
    public static final DeferredItem<SpecialChestBlockItem> ENDER_DISPATCH_CHEST_ITEM = ITEMS.registerItem(
            "ender_dispatch_chest",
            properties -> new SpecialChestBlockItem(ModBlocks.ENDER_DISPATCH_CHEST.get(), ChestKind.ENDER_DISPATCH, properties)
    );
    public static final DeferredItem<SpecialChestBlockItem> BUILDERS_CHEST_ITEM = ITEMS.registerItem(
            "builders_chest",
            properties -> new SpecialChestBlockItem(ModBlocks.BUILDERS_CHEST.get(), ChestKind.BUILDERS, properties)
    );
    public static final DeferredItem<SpecialChestBlockItem> COLLECTORS_CHEST_ITEM = ITEMS.registerItem(
            "collectors_chest",
            properties -> new SpecialChestBlockItem(ModBlocks.COLLECTORS_CHEST.get(), ChestKind.COLLECTORS, properties)
    );
    public static final DeferredItem<SpecialChestBlockItem> SCULK_SENTINEL_CHEST_ITEM = ITEMS.registerItem(
            "sculk_sentinel_chest",
            properties -> new SpecialChestBlockItem(ModBlocks.SCULK_SENTINEL_CHEST.get(), ChestKind.SCULK_SENTINEL, properties)
    );
    public static final DeferredItem<SpecialChestBlockItem> RESONANT_CHEST_ITEM = ITEMS.registerItem(
            "resonant_chest",
            properties -> new SpecialChestBlockItem(ModBlocks.RESONANT_CHEST.get(), ChestKind.RESONANT, properties)
    );
    public static final DeferredItem<SpecialChestBlockItem> ARCHIVISTS_CHEST_ITEM = ITEMS.registerItem(
            "archivists_chest",
            properties -> new SpecialChestBlockItem(ModBlocks.ARCHIVISTS_CHEST.get(), ChestKind.ARCHIVIST, properties)
    );
    public static final DeferredItem<SpecialChestBlockItem> WITCHS_CHEST_ITEM = ITEMS.registerItem(
            "witchs_chest",
            properties -> new SpecialChestBlockItem(ModBlocks.WITCHS_CHEST.get(), ChestKind.WITCH, properties)
    );
    public static final DeferredItem<ResonanceCrystalItem> RESONANCE_CRYSTAL = ITEMS.registerItem(
            "resonance_crystal",
            ResonanceCrystalItem::new
    );

    private ModItems() {}
}
