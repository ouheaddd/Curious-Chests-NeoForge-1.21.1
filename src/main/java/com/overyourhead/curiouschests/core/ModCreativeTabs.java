package com.overyourhead.curiouschests.core;

import com.overyourhead.curiouschests.CuriousChestsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CuriousChestsMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CURIOUS_CHESTS = TABS.register(
            "curiouschests",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.curiouschests"))
                    .icon(() -> ModItems.BUILDERS_CHEST_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) ->
                            ModItems.ITEMS.getEntries().forEach(item -> output.accept(item.get())))
                    .build()
    );

    private ModCreativeTabs() {}
}
