package com.overyourhead.curiouschests;

import com.overyourhead.curiouschests.common.event.SentinelEvents;
import com.overyourhead.curiouschests.core.ModBlockEntities;
import com.overyourhead.curiouschests.core.ModBlocks;
import com.overyourhead.curiouschests.core.ModCapabilities;
import com.overyourhead.curiouschests.core.ModCreativeTabs;
import com.overyourhead.curiouschests.core.ModDataComponents;
import com.overyourhead.curiouschests.core.ModItems;
import com.overyourhead.curiouschests.core.ModMenus;
import com.overyourhead.curiouschests.core.ModParticles;
import com.overyourhead.curiouschests.core.ModNetworking;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(CuriousChestsMod.MOD_ID)
public final class CuriousChestsMod {
    public static final String MOD_ID = "curiouschests";

    public CuriousChestsMod(IEventBus modBus) {
        ModDataComponents.COMPONENTS.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModParticles.PARTICLES.register(modBus);

        modBus.addListener(ModCapabilities::register);
        modBus.addListener(ModNetworking::register);

        NeoForge.EVENT_BUS.addListener(SentinelEvents::onBreakSpeed);
        NeoForge.EVENT_BUS.addListener(SentinelEvents::onBreak);
    }
}
