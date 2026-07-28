package com.overyourhead.curiouschests;

import com.overyourhead.curiouschests.core.ModBlockEntities;
import com.overyourhead.curiouschests.core.ModBlocks;
import com.overyourhead.curiouschests.core.ModCapabilities;
import com.overyourhead.curiouschests.core.ModCreativeTabs;
import com.overyourhead.curiouschests.core.ModItems;
import com.overyourhead.curiouschests.core.ModMenus;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(CuriousChestsMod.MOD_ID)
public final class CuriousChestsMod {
    public static final String MOD_ID = "curiouschests";

    public CuriousChestsMod(IEventBus modBus) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModMenus.MENUS.register(modBus);

        modBus.addListener(ModCapabilities::register);
    }
}
