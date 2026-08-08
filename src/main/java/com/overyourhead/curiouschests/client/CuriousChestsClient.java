package com.overyourhead.curiouschests.client;

import com.overyourhead.curiouschests.CuriousChestsMod;
import com.overyourhead.curiouschests.client.gui.SpecialChestScreen;
import com.overyourhead.curiouschests.client.render.SpecialChestRenderer;
import com.overyourhead.curiouschests.core.ModBlockEntities;
import com.overyourhead.curiouschests.core.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class CuriousChestsClient {
    private CuriousChestsClient() {}

    @EventBusSubscriber(modid = CuriousChestsMod.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {
        private ModEvents() {}

        @SubscribeEvent
        public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(
                    ModBlockEntities.SPECIAL_CHEST.get(),
                    SpecialChestRenderer::new
            );
        }

        @SubscribeEvent
        public static void screens(RegisterMenuScreensEvent event) {
            event.register(ModMenus.BOTTOMLESS.get(), SpecialChestScreen::new);
            event.register(ModMenus.INFERNAL.get(), SpecialChestScreen::new);
            event.register(ModMenus.ENDER_DISPATCH.get(), SpecialChestScreen::new);
            event.register(ModMenus.BUILDERS.get(), SpecialChestScreen::new);
            event.register(ModMenus.COLLECTORS.get(), SpecialChestScreen::new);
            event.register(ModMenus.SCULK_SENTINEL.get(), SpecialChestScreen::new);
            event.register(ModMenus.RESONANT.get(), SpecialChestScreen::new);
            event.register(ModMenus.ARCHIVIST.get(), SpecialChestScreen::new);
            event.register(ModMenus.WITCH.get(), SpecialChestScreen::new);
        }
    }
}
