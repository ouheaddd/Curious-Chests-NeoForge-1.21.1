package com.overyourhead.curiouschests.client;

import com.overyourhead.curiouschests.CuriousChestsMod;
import com.overyourhead.curiouschests.client.gui.SpecialChestScreen;
import com.overyourhead.curiouschests.client.model.BuildersChestModel;
import com.overyourhead.curiouschests.client.model.CollectorsChestModel;
import com.overyourhead.curiouschests.client.model.EnderDispatchChestModel;
import com.overyourhead.curiouschests.client.model.ResonantChestModel;
import com.overyourhead.curiouschests.client.model.SculkSentinelChestModel;
import com.overyourhead.curiouschests.client.model.WitchLiquidModel;
import com.overyourhead.curiouschests.client.model.WitchsChestModel;
import com.overyourhead.curiouschests.client.particle.WitchBurstParticle;
import com.overyourhead.curiouschests.client.particle.WitchSparkParticle;
import com.overyourhead.curiouschests.client.particle.WitchSteamParticle;
import com.overyourhead.curiouschests.client.render.SpecialChestRenderer;
import com.overyourhead.curiouschests.core.ModBlockEntities;
import com.overyourhead.curiouschests.core.ModMenus;
import com.overyourhead.curiouschests.core.ModParticles;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

public final class CuriousChestsClient {
    private CuriousChestsClient() {}

    @EventBusSubscriber(modid = CuriousChestsMod.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {
        private ModEvents() {}

        @SubscribeEvent
        public static void modelLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(
                    BuildersChestModel.LAYER_LOCATION,
                    BuildersChestModel::createBodyLayer
            );
            event.registerLayerDefinition(
                    CollectorsChestModel.LAYER_LOCATION,
                    CollectorsChestModel::createBodyLayer
            );
            event.registerLayerDefinition(
                    EnderDispatchChestModel.LAYER_LOCATION,
                    EnderDispatchChestModel::createBodyLayer
            );
            event.registerLayerDefinition(
                    ResonantChestModel.LAYER_LOCATION,
                    ResonantChestModel::createBodyLayer
            );
            event.registerLayerDefinition(
                    SculkSentinelChestModel.LAYER_LOCATION,
                    SculkSentinelChestModel::createBodyLayer
            );
            event.registerLayerDefinition(
                    WitchsChestModel.LAYER_LOCATION,
                    WitchsChestModel::createBodyLayer
            );
            event.registerLayerDefinition(
                    WitchLiquidModel.LAYER_LOCATION,
                    WitchLiquidModel::createBodyLayer
            );
        }

        @SubscribeEvent
        public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(
                    ModBlockEntities.SPECIAL_CHEST.get(),
                    SpecialChestRenderer::new
            );
        }


        @SubscribeEvent
        public static void particles(RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(ModParticles.WITCH_STEAM.get(), WitchSteamParticle.Provider::new);
            event.registerSpriteSet(ModParticles.WITCH_SPARK.get(), WitchSparkParticle.Provider::new);
            event.registerSpriteSet(ModParticles.WITCH_BURST.get(), WitchBurstParticle.Provider::new);
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
